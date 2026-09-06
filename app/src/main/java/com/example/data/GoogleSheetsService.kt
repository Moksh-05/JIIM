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
    fun extractSpreadsheetId(input: String): String {
      val trimmed = input.trim()
      // Pattern: /spreadsheets/d/([a-zA-Z0-9-_]+)
      val regex = Regex("""/spreadsheets/d/([a-zA-Z0-9-_]+)""")
      val match = regex.find(trimmed)
      if (match != null) {
        return match.groupValues[1]
      }
      // Or if user provided just the raw spreadsheet ID (typically 20-50 chars of alphanum, dash, underscore)
      if (trimmed.matches(Regex("""^[a-zA-Z0-9-_]{20,60}$"""))) {
        return trimmed
      }
      return trimmed
    }
  }

  suspend fun fetchSpreadsheetMetadata(
    spreadsheetId: String
  ): Result<GoogleSheetMetadata> = withContext(Dispatchers.IO) {
    try {
      val cleanId = extractSpreadsheetId(spreadsheetId)
      val token = authManager.effectiveAccessToken
      val apiKey = authManager.effectiveApiKey

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

      if (!response.isSuccessful) {
        val errorMsg = try {
          val json = JSONObject(body)
          json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
        } catch (_: Exception) {
          "HTTP ${response.code}: $body"
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

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

      Result.success(
        GoogleSheetMetadata(
          spreadsheetId = cleanId,
          title = title,
          sheetTabs = tabs
        )
      )
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
      val cleanId = extractSpreadsheetId(spreadsheetId)
      val token = authManager.effectiveAccessToken
      val apiKey = authManager.effectiveApiKey

      // Format range: if it's just tab title, fetch e.g. "'Tab Title'!A1:ZZ5000"
      val fullRange = if (!rangeOrTabTitle.contains("!")) {
        "'$rangeOrTabTitle'!A1:ZZ5000"
      } else {
        rangeOrTabTitle
      }

      val encodedRange = URLEncoder.encode(fullRange, "UTF-8").replace("+", "%20")
      val urlBuilder = StringBuilder("https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values/$encodedRange")
      urlBuilder.append("?valueRenderOption=FORMATTED_VALUE")
      if (token.isNullOrBlank() && apiKey.isNotBlank()) {
        urlBuilder.append("&key=").append(apiKey)
      }

      val requestBuilder = Request.Builder().url(urlBuilder.toString())
      if (!token.isNullOrBlank()) {
        requestBuilder.addHeader("Authorization", "Bearer $token")
      }

      val response = client.newCall(requestBuilder.build()).execute()
      val body = response.body?.string() ?: ""

      if (!response.isSuccessful) {
        val errorMsg = try {
          val json = JSONObject(body)
          json.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
        } catch (_: Exception) {
          "HTTP ${response.code}: $body"
        }
        return@withContext Result.failure(Exception(errorMsg))
      }

      val json = JSONObject(body)
      val valuesArray = json.optJSONArray("values") ?: return@withContext Result.success(emptyList())

      val rows = mutableListOf<List<String>>()
      for (r in 0 until valuesArray.length()) {
        val rowArr = valuesArray.optJSONArray(r) ?: continue
        val row = mutableListOf<String>()
        for (c in 0 until rowArr.length()) {
          row.add(rowArr.optString(c, ""))
        }
        rows.add(row)
      }

      Result.success(rows)
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
