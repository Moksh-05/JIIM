package com.example.data

import android.util.Log
import com.example.model.ParsedExerciseLog
import com.example.model.ParsedWorkoutRant
import com.example.model.WorkoutWithExercises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class GoogleSheetMetadata(
  val spreadsheetId: String,
  val title: String,
  val sheetTabs: List<GoogleSheetTab>
)

data class GoogleSheetTab(
  val sheetId: Int,
  val title: String,
  val rowCount: Int = 0,
  val columnCount: Int = 0
)

class GoogleSheetsService(
  private val authManager: GoogleAuthManager
) {

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  companion object {
    data class ParsedSpreadsheetTarget(
      val cleanId: String,
      val isWebPublished: Boolean = false,
      val gid: String? = null,
      val isNameOnly: Boolean = false,
      val originalInput: String = ""
    )

    fun parseSpreadsheetTarget(input: String): ParsedSpreadsheetTarget {
      val trimmed = input.trim()
      if (trimmed.isBlank()) {
        return ParsedSpreadsheetTarget("", isNameOnly = true, originalInput = input)
      }

      // 1. Check if user typed a plain name like "Gym" or "Workout Log"
      if (!trimmed.contains("/") && !trimmed.contains(".") && (trimmed.length < 18 || trimmed.contains(" "))) {
        return ParsedSpreadsheetTarget(trimmed, isNameOnly = true, originalInput = trimmed)
      }

      // Extract GID if present
      val gidMatch = Regex("""(?:[#&?]gid=)(\d+)""").find(trimmed)
      val gid = gidMatch?.groupValues?.get(1)

      // 2. Web published link: /spreadsheets/d/e/(...)/...
      val pubMatch = Regex("""/spreadsheets/d/e/([a-zA-Z0-9-_]+)""").find(trimmed)
      if (pubMatch != null) {
        return ParsedSpreadsheetTarget(pubMatch.groupValues[1], isWebPublished = true, gid = gid, originalInput = trimmed)
      }

      // 3. Standard Google Sheets URL: /spreadsheets/d/(...)/...
      val stdMatch = Regex("""/spreadsheets/d/([a-zA-Z0-9-_]+)""").find(trimmed)
      if (stdMatch != null) {
        return ParsedSpreadsheetTarget(stdMatch.groupValues[1], isWebPublished = false, gid = gid, originalInput = trimmed)
      }

      // 4. Drive file URL: /file/d/(...) or ?id=(...)
      val driveMatch = Regex("""/file/d/([a-zA-Z0-9-_]+)""").find(trimmed)
      if (driveMatch != null) {
        return ParsedSpreadsheetTarget(driveMatch.groupValues[1], isWebPublished = false, gid = gid, originalInput = trimmed)
      }

      val idParamMatch = Regex("""[?&]id=([a-zA-Z0-9-_]+)""").find(trimmed)
      if (idParamMatch != null) {
        return ParsedSpreadsheetTarget(idParamMatch.groupValues[1], isWebPublished = false, gid = gid, originalInput = trimmed)
      }

      // 5. Raw alphanumeric spreadsheet ID
      if (trimmed.matches(Regex("""^[a-zA-Z0-9-_]{20,70}$"""))) {
        return ParsedSpreadsheetTarget(trimmed, isWebPublished = false, gid = gid, originalInput = trimmed)
      }

      return ParsedSpreadsheetTarget(trimmed, isNameOnly = true, originalInput = trimmed)
    }

    fun extractSpreadsheetId(input: String): String {
      return parseSpreadsheetTarget(input).cleanId.ifBlank { input.trim() }
    }
  }

  suspend fun fetchSpreadsheetMetadata(
    spreadsheetId: String
  ): Result<GoogleSheetMetadata> = withContext(Dispatchers.IO) {
    try {
      val target = parseSpreadsheetTarget(spreadsheetId)
      if (target.isNameOnly) {
        return@withContext Result.failure(
          Exception("\"${target.originalInput}\" appears to be a sheet name rather than a link. Please open Google Sheets, tap Share → Copy link, and paste the full link here.")
        )
      }

      val cleanId = target.cleanId
      val token = authManager.effectiveAccessToken
      val apiKey = authManager.effectiveApiKey

      // If user provided a web-published sheet
      if (target.isWebPublished) {
        val pubUrl = "https://docs.google.com/spreadsheets/d/e/$cleanId/pub?output=csv"
        val req = Request.Builder().url(pubUrl).build()
        val resp = client.newCall(req).execute()
        if (resp.isSuccessful) {
          val csvData = resp.body?.string() ?: ""
          val rows = GoogleSheetWorkoutParser.parseCsvString(csvData)
          return@withContext Result.success(
            GoogleSheetMetadata(
              spreadsheetId = cleanId,
              title = "Published Gym Sheet",
              sheetTabs = listOf(GoogleSheetTab(sheetId = 0, title = "Sheet1", rowCount = rows.size, columnCount = rows.firstOrNull()?.size ?: 0))
            )
          )
        }
      }

      // Strategy 1: Google Sheets API v4
      var apiV4Failed = false
      var apiV4Error = ""
      if (!token.isNullOrBlank() || apiKey.isNotBlank()) {
        try {
          val urlBuilder = StringBuilder("https://sheets.googleapis.com/v4/spreadsheets/$cleanId")
          urlBuilder.append("?fields=properties.title,sheets.properties(sheetId,title,gridProperties(rowCount,columnCount))")
          if (token.isNullOrBlank() && apiKey.isNotBlank()) {
            urlBuilder.append("&key=").append(apiKey)
          }

          val requestBuilder = Request.Builder().url(urlBuilder.toString())
          if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
          }

          val response = client.newCall(requestBuilder.build()).execute()
          val body = response.body?.string() ?: ""

          if (response.isSuccessful) {
            val json = JSONObject(body)
            val title = json.optJSONObject("properties")?.optString("title") ?: "Google Spreadsheet"
            val sheetsArray = json.optJSONArray("sheets")
            val tabs = mutableListOf<GoogleSheetTab>()

            if (sheetsArray != null) {
              for (i in 0 until sheetsArray.length()) {
                val sheetObj = sheetsArray.getJSONObject(i)
                val props = sheetObj.optJSONObject("properties") ?: continue
                val tabId = props.optInt("sheetId", i)
                val tabTitle = props.optString("title", "Sheet${i + 1}")
                val grid = props.optJSONObject("gridProperties")
                val rowCount = grid?.optInt("rowCount", 0) ?: 0
                val colCount = grid?.optInt("columnCount", 0) ?: 0
                tabs.add(GoogleSheetTab(sheetId = tabId, title = tabTitle, rowCount = rowCount, columnCount = colCount))
              }
            }

            return@withContext Result.success(
              GoogleSheetMetadata(
                spreadsheetId = cleanId,
                title = title,
                sheetTabs = tabs.ifEmpty { listOf(GoogleSheetTab(sheetId = 0, title = "Sheet1")) }
              )
            )
          } else {
            apiV4Failed = true
            apiV4Error = try {
              val json = JSONObject(body)
              json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
            } catch (_: Exception) {
              "HTTP ${response.code}"
            }
          }
        } catch (e: Exception) {
          apiV4Failed = true
          apiV4Error = e.message ?: "Network error"
        }
      }

      // Strategy 2: Direct Google Visualization API (GViz) + HTML Export
      // Works for any spreadsheet where General Access is "Anyone with the link can view",
      // with ZERO Google Cloud API keys and NO OAuth configuration needed!
      val gvizUrl = "https://docs.google.com/spreadsheets/d/$cleanId/gviz/tq?tqx=out:csv"
      val gvizReq = Request.Builder()
        .url(gvizUrl)
        .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
        .build()

      val gvizResp = client.newCall(gvizReq).execute()
      val finalUrl = gvizResp.request.url.toString()
      val gvizBody = gvizResp.body?.string() ?: ""

      // Check if redirected to Google sign-in (meaning the sheet is private)
      if (finalUrl.contains("accounts.google.com") || gvizResp.code in listOf(401, 403)) {
        return@withContext Result.failure(
          Exception("Your sheet is currently set to Private. In Google Sheets, tap Share (top right) → under General Access, change 'Restricted' to 'Anyone with the link' (Viewer), then tap Fetch again.")
        )
      }

      if (gvizResp.code == 404) {
        return@withContext Result.failure(
          Exception("Spreadsheet not found (404). Please verify that the Google Sheets URL or ID is correct.")
        )
      }

      if (gvizResp.isSuccessful && gvizBody.isNotBlank()) {
        // Fetch HTML view to extract document title & any tab buttons
        var sheetTitle = "Gym Spreadsheet"
        val discoveredTabs = mutableListOf<GoogleSheetTab>()

        try {
          val htmlUrl = "https://docs.google.com/spreadsheets/d/$cleanId/htmlview"
          val htmlReq = Request.Builder()
            .url(htmlUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
            .build()
          val htmlResp = client.newCall(htmlReq).execute()
          if (htmlResp.isSuccessful) {
            val htmlContent = htmlResp.body?.string() ?: ""
            // Extract <title>...</title>
            val titleMatch = Regex("""<title>(.*?)(?: - Google (?:Sheets|Docs|Drive))?</title>""", RegexOption.IGNORE_CASE).find(htmlContent)
            if (titleMatch != null) {
              val extracted = titleMatch.groupValues[1].trim()
              if (extracted.isNotBlank() && !extracted.equals("Google Sheets", ignoreCase = true)) {
                sheetTitle = extracted
              }
            }
            // Extract tabs: <li id="sheet-button-..."><a>TabName</a></li>
            val tabRegex = Regex("""<li[^>]*id="sheet-button-(\d+)"[^>]*>.*?<a[^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val tabMatches = tabRegex.findAll(htmlContent)
            for (m in tabMatches) {
              val tId = m.groupValues[1].toIntOrNull() ?: 0
              val tName = m.groupValues[2].trim()
              if (tName.isNotBlank()) {
                discoveredTabs.add(GoogleSheetTab(sheetId = tId, title = tName))
              }
            }
          }
        } catch (_: Exception) {}

        val finalTabs = if (discoveredTabs.isNotEmpty()) discoveredTabs else listOf(GoogleSheetTab(sheetId = 0, title = "Sheet1"))

        return@withContext Result.success(
          GoogleSheetMetadata(
            spreadsheetId = cleanId,
            title = sheetTitle,
            sheetTabs = finalTabs
          )
        )
      }

      // If both strategies failed
      val finalErrMsg = if (apiV4Error.isNotBlank()) {
        apiV4Error
      } else {
        "HTTP ${gvizResp.code}: Verify your spreadsheet link and ensure 'Anyone with the link can view' is enabled."
      }
      Result.failure(Exception(finalErrMsg))
    } catch (e: Exception) {
      Log.e("GoogleSheetsService", "Failed to fetch spreadsheet metadata", e)
      Result.failure(e)
    }
  }

  suspend fun fetchSheetValues(
    spreadsheetId: String,
    rangeOrTabTitle: String
  ): Result<List<List<String>>> = withContext(Dispatchers.IO) {
    try {
      val target = parseSpreadsheetTarget(spreadsheetId)
      val cleanId = target.cleanId.ifBlank { spreadsheetId.trim() }
      val token = authManager.effectiveAccessToken
      val apiKey = authManager.effectiveApiKey

      // If user provided a web published link
      if (target.isWebPublished) {
        val pubUrl = "https://docs.google.com/spreadsheets/d/e/$cleanId/pub?output=csv"
        val req = Request.Builder().url(pubUrl).build()
        val resp = client.newCall(req).execute()
        if (resp.isSuccessful) {
          val csv = resp.body?.string() ?: ""
          return@withContext Result.success(GoogleSheetWorkoutParser.parseCsvString(csv))
        }
      }

      // Strategy 1: Sheets API v4
      if (!token.isNullOrBlank()) {
        try {
          val fullRange = if (!rangeOrTabTitle.contains("!")) {
            "'$rangeOrTabTitle'!A1:ZZ5000"
          } else {
            rangeOrTabTitle
          }
          val encodedRange = URLEncoder.encode(fullRange, "UTF-8").replace("+", "%20")
          val url = "https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values/$encodedRange?valueRenderOption=FORMATTED_VALUE"

          val requestBuilder = Request.Builder().url(url).addHeader("Authorization", "Bearer $token")
          val response = client.newCall(requestBuilder.build()).execute()
          val body = response.body?.string() ?: ""

          if (response.isSuccessful) {
            val json = JSONObject(body)
            val valuesArray = json.optJSONArray("values")
            if (valuesArray != null) {
              val rows = mutableListOf<List<String>>()
              for (r in 0 until valuesArray.length()) {
                val rowArr = valuesArray.optJSONArray(r) ?: continue
                val row = mutableListOf<String>()
                for (c in 0 until rowArr.length()) {
                  row.add(rowArr.optString(c, ""))
                }
                rows.add(row)
              }
              return@withContext Result.success(rows)
            }
          }
        } catch (_: Exception) {}
      }

      // Strategy 2: Google Visualization API (GViz) CSV
      // Try with specific sheet name:
      val gvizUrlWithSheet = "https://docs.google.com/spreadsheets/d/$cleanId/gviz/tq?tqx=out:csv&sheet=${URLEncoder.encode(rangeOrTabTitle, "UTF-8")}"
      val reqWithSheet = Request.Builder()
        .url(gvizUrlWithSheet)
        .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
        .build()

      val respWithSheet = client.newCall(reqWithSheet).execute()
      if (respWithSheet.isSuccessful) {
        val csv = respWithSheet.body?.string() ?: ""
        val rows = GoogleSheetWorkoutParser.parseCsvString(csv)
        if (rows.isNotEmpty()) {
          return@withContext Result.success(rows)
        }
      }

      // Strategy 3: Standard CSV export (e.g. if gid is known)
      val exportUrl = if (!target.gid.isNullOrBlank()) {
        "https://docs.google.com/spreadsheets/d/$cleanId/export?format=csv&gid=${target.gid}"
      } else {
        "https://docs.google.com/spreadsheets/d/$cleanId/export?format=csv"
      }
      val expReq = Request.Builder()
        .url(exportUrl)
        .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
        .build()
      val expResp = client.newCall(expReq).execute()
      if (expResp.isSuccessful) {
        val csv = expResp.body?.string() ?: ""
        val rows = GoogleSheetWorkoutParser.parseCsvString(csv)
        return@withContext Result.success(rows)
      }

      Result.failure(Exception("Could not fetch data rows. Please ensure General Access in Google Sheets is set to 'Anyone with the link can view'."))
    } catch (e: Exception) {
      Log.e("GoogleSheetsService", "Failed to fetch sheet values", e)
      Result.failure(e)
    }
  }

  suspend fun appendWorkoutRows(
    spreadsheetId: String,
    tabTitle: String,
    rows: List<List<String>>
  ): Result<Int> = withContext(Dispatchers.IO) {
    try {
      val cleanId = extractSpreadsheetId(spreadsheetId)
      val token = authManager.effectiveAccessToken
      if (token.isNullOrBlank()) {
        return@withContext Result.failure(Exception("Google Sign-In / OAuth Token required to write to Google Sheets."))
      }

      val fullRange = if (!tabTitle.contains("!")) {
        "'$tabTitle'!A:G"
      } else {
        tabTitle
      }

      val encodedRange = URLEncoder.encode(fullRange, "UTF-8").replace("+", "%20")
      val url = "https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values/$encodedRange:append?valueInputOption=USER_ENTERED&insertDataOption=INSERT_ROWS"

      val valuesArray = JSONArray()
      for (row in rows) {
        val rowArr = JSONArray()
        for (cell in row) {
          rowArr.put(cell)
        }
        valuesArray.put(rowArr)
      }

      val bodyJson = JSONObject().apply {
        put("range", fullRange)
        put("majorDimension", "ROWS")
        put("values", valuesArray)
      }

      val requestBody = bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
      val request = Request.Builder()
        .url(url)
        .addHeader("Authorization", "Bearer $token")
        .post(requestBody)
        .build()

      val response = client.newCall(request).execute()
      val respBody = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          val json = JSONObject(respBody)
          json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
        } catch (_: Exception) {
          "HTTP ${response.code}: $respBody"
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(respBody)
      val updates = json.optJSONObject("updates")
      val updatedRows = updates?.optInt("updatedRows", rows.size) ?: rows.size
      Result.success(updatedRows)
    } catch (e: Exception) {
      Log.e("GoogleSheetsService", "Failed to append rows to spreadsheet", e)
      Result.failure(e)
    }
  }

  fun formatWorkoutForSheet(
    workoutName: String,
    dateMillis: Long,
    exercises: List<ParsedExerciseLog>,
    notes: String = "",
    useLbs: Boolean = false
  ): List<List<String>> {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateMillis))
    val unitStr = if (useLbs) "lbs" else "kg"
    val rows = mutableListOf<List<String>>()

    for (ex in exercises) {
      for ((setIdx, s) in ex.sets.withIndex()) {
        val weightVal = if (useLbs) (s.weightKg * 2.20462).roundToInt() else s.weightKg
        val formattedWeight = if (weightVal.toDouble() % 1.0 == 0.0) weightVal.toInt().toString() else "%.1f".format(weightVal)
        val repsVal = if (s.reps % 1.0 == 0.0) s.reps.toInt().toString() else "%.1f".format(s.reps)
        val notesParts = mutableListOf<String>()
        if (s.side != "BOTH") notesParts.add("Side: ${s.side}")
        if (s.setKind != "NORMAL") notesParts.add(s.setKind)
        if (s.failurePoint.isNotBlank()) notesParts.add("Failed: ${s.failurePoint}")
        if (s.tempo.isNotBlank()) notesParts.add("Tempo: ${s.tempo}")
        if (notes.isNotBlank() && setIdx == 0) notesParts.add(notes)

        rows.add(
          listOf(
            dateStr,
            workoutName,
            ex.exerciseName,
            formattedWeight,
            unitStr,
            repsVal,
            notesParts.joinToString("; ")
          )
        )
      }
    }
    return rows
  }

  fun formatCompletedWorkoutForSheet(
    workout: WorkoutWithExercises,
    useLbs: Boolean = false
  ): List<List<String>> {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(workout.session.startTimeMillis))
    val unitStr = if (useLbs) "lbs" else "kg"
    val rows = mutableListOf<List<String>>()

    for (exWithSets in workout.exercises) {
      for ((setIdx, s) in exWithSets.sets.withIndex()) {
        val weightVal = if (useLbs) (s.weightKg * 2.20462).roundToInt() else s.weightKg
        val formattedWeight = if (weightVal.toDouble() % 1.0 == 0.0) weightVal.toInt().toString() else "%.1f".format(weightVal)
        val repsVal = if (s.reps % 1.0 == 0.0) s.reps.toInt().toString() else "%.1f".format(s.reps)
        val notesParts = mutableListOf<String>()
        if (s.side != "BOTH") notesParts.add("Side: ${s.side}")
        if (s.setKind != "NORMAL") notesParts.add(s.setKind)
        if (s.biofeedbackTags.isNotBlank()) notesParts.add(s.biofeedbackTags)
        if (s.tempo.isNotBlank()) notesParts.add("Tempo: ${s.tempo}")
        if (s.failurePoint.isNotBlank()) notesParts.add("Failed: ${s.failurePoint}")
        if (!workout.session.notes.isNullOrBlank() && setIdx == 0) notesParts.add(workout.session.notes)

        rows.add(
          listOf(
            dateStr,
            workout.session.name,
            exWithSets.exercise.exerciseName,
            formattedWeight,
            unitStr,
            repsVal,
            notesParts.joinToString("; ")
          )
        )
      }
    }
    return rows
  }
}
