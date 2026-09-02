package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.example.model.AiProgressAnalysis
import com.example.model.ExercisePr
import com.example.model.ParsedExerciseLog
import com.example.model.ParsedSetLog
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
import java.util.concurrent.TimeUnit

class GeminiService {

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  private val apiKey: String
    get() = try {
      BuildConfig.GEMINI_API_KEY
    } catch (_: Throwable) {
      ""
    }

  private val isKeyConfigured: Boolean
    get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

  suspend fun parseGymRant(
    rantText: String,
    isOnline: Boolean
  ): ParsedWorkoutRant = withContext(Dispatchers.IO) {
    if (!isOnline || !isKeyConfigured) {
      // Offline fallback: Use the smart local heuristic regex parser
      return@withContext OfflineRantParser.parseRant(rantText)
    }

    try {
      val prompt = """
        You are an expert gym logger assistant for a lifter.
        Extract the workout title and all exercises with their sets, weights in kg, and reps from this gym rant:
        "$rantText"
        
        Respond ONLY with a JSON object in this exact schema, without markdown formatting or code fences:
        {
          "workoutTitle": "Chest & Triceps",
          "exercises": [
            {
              "exerciseName": "Barbell Bench Press",
              "sets": [
                {"weightKg": 80.0, "reps": 8},
                {"weightKg": 80.0, "reps": 8}
              ]
            }
          ],
          "notes": "Felt good pump"
        }
      """.trimIndent()

      val jsonResponse = callGemini(prompt)
      if (jsonResponse != null) {
        val parsed = parseRantJson(jsonResponse, rantText)
        if (parsed != null && parsed.exercises.isNotEmpty()) {
          return@withContext parsed
        }
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Online rant parsing failed, falling back to offline parser", e)
    }

    // Fallback to offline parser
    OfflineRantParser.parseRant(rantText)
  }

  suspend fun analyzeProgress(
    workouts: List<WorkoutWithExercises>,
    prs: List<ExercisePr>,
    isOnline: Boolean
  ): AiProgressAnalysis = withContext(Dispatchers.IO) {
    if (!isOnline || !isKeyConfigured || workouts.isEmpty()) {
      return@withContext OfflineProgressAnalyzer.analyze(workouts, prs)
    }

    try {
      val sessionSummaries = workouts.take(8).joinToString("\n") { w ->
        val exSummary = w.exercises.joinToString("; ") { ex ->
          val setsInfo = ex.sets.joinToString(",") { "${it.weightKg}kgx${it.reps}" }
          "${ex.exercise.exerciseName} ($setsInfo)"
        }
        "- Session '${w.session.name}' on ${java.util.Date(w.session.startTimeMillis)}: Volume: ${w.session.totalVolumeKg.toInt()}kg: $exSummary"
      }

      val prompt = """
        You are an elite bodybuilding & strength coach specializing in hypertrophy and progressive overload.
        Analyze the lifter's recent workout sessions:
        $sessionSummaries
        
        Provide an analytical evaluation. Respond ONLY with a valid JSON object matching this exact schema without markdown backticks:
        {
          "overallScore": "Elite Hypertrophy (9.2/10)",
          "hypertrophyStatus": "Summary of hypertrophy volume and muscle group balance",
          "progressiveOverloadVerdict": "Evaluation of weight and rep progressions across recent weeks",
          "recommendations": [
            "Recommendation 1",
            "Recommendation 2",
            "Recommendation 3"
          ],
          "detectedSplitName": "Detected custom split name (e.g. 4-Day Antagonist Split)",
          "detectedSplitBreakdown": [
            "Day 1: Chest & Triceps",
            "Day 2: Biceps & Shoulders",
            "Day 3: Abs, Cardio & Legs"
          ],
          "stagnationAlerts": [
            "Plateau notice if any exercise hasn't progressed in weight or reps"
          ]
        }
      """.trimIndent()

      val jsonResponse = callGemini(prompt)
      if (jsonResponse != null) {
        val parsed = parseAnalysisJson(jsonResponse)
        if (parsed != null) {
          return@withContext parsed
        }
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Online progress analysis failed, falling back to offline analyzer", e)
    }

    OfflineProgressAnalyzer.analyze(workouts, prs)
  }

  private fun callGemini(prompt: String): String? {
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

    val requestBodyJson = JSONObject().apply {
      val contentsArray = JSONArray().apply {
        val contentObj = JSONObject().apply {
          val partsArray = JSONArray().apply {
            put(JSONObject().apply { put("text", prompt) })
          }
          put("parts", partsArray)
        }
        put(contentObj)
      }
      put("contents", contentsArray)
    }

    val request = Request.Builder()
      .url(url)
      .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        Log.w("GeminiService", "Gemini HTTP call failed with code: ${response.code}")
        return null
      }
      val responseText = response.body?.string() ?: return null
      val root = JSONObject(responseText)
      val candidates = root.optJSONArray("candidates") ?: return null
      if (candidates.length() == 0) return null
      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content") ?: return null
      val parts = content.optJSONArray("parts") ?: return null
      if (parts.length() == 0) return null
      val text = parts.getJSONObject(0).optString("text")
      return cleanJsonText(text)
    }
  }

  private fun cleanJsonText(raw: String): String {
    var cleaned = raw.trim()
    if (cleaned.startsWith("```json")) {
      cleaned = cleaned.removePrefix("```json")
    } else if (cleaned.startsWith("```")) {
      cleaned = cleaned.removePrefix("```")
    }
    if (cleaned.endsWith("```")) {
      cleaned = cleaned.removeSuffix("```")
    }
    return cleaned.trim()
  }

  private fun parseRantJson(jsonStr: String, originalText: String): ParsedWorkoutRant? {
    return try {
      val obj = JSONObject(jsonStr)
      val title = obj.optString("workoutTitle", "Logged Workout")
      val notes = obj.optString("notes", originalText)
      val exercisesArr = obj.optJSONArray("exercises") ?: JSONArray()
      val exercisesList = mutableListOf<ParsedExerciseLog>()

      for (i in 0 until exercisesArr.length()) {
        val exObj = exercisesArr.getJSONObject(i)
        val name = exObj.optString("exerciseName", "Exercise")
        val setsArr = exObj.optJSONArray("sets") ?: JSONArray()
        val setsList = mutableListOf<ParsedSetLog>()

        for (j in 0 until setsArr.length()) {
          val sObj = setsArr.getJSONObject(j)
          val weight = sObj.optDouble("weightKg", 0.0)
          val reps = sObj.optInt("reps", 10)
          setsList.add(ParsedSetLog(weightKg = weight, reps = reps))
        }

        if (setsList.isNotEmpty()) {
          exercisesList.add(ParsedExerciseLog(exerciseName = name, sets = setsList))
        }
      }

      ParsedWorkoutRant(workoutTitle = title, exercises = exercisesList, notes = notes)
    } catch (_: Exception) {
      null
    }
  }

  private fun parseAnalysisJson(jsonStr: String): AiProgressAnalysis? {
    return try {
      val obj = JSONObject(jsonStr)
      val score = obj.optString("overallScore", "Hypertrophy Progress (8.8/10)")
      val hypertrophy = obj.optString("hypertrophyStatus", "Solid weekly hypertrophy stimulus.")
      val overload = obj.optString("progressiveOverloadVerdict", "Positive progressive overload trajectory.")

      val recs = mutableListOf<String>()
      val recsArr = obj.optJSONArray("recommendations")
      if (recsArr != null) {
        for (i in 0 until recsArr.length()) {
          recs.add(recsArr.getString(i))
        }
      }

      val splitName = obj.optString("detectedSplitName", "Lifter Custom Split")
      val breakdown = mutableListOf<String>()
      val breakArr = obj.optJSONArray("detectedSplitBreakdown")
      if (breakArr != null) {
        for (i in 0 until breakArr.length()) {
          breakdown.add(breakArr.getString(i))
        }
      }

      val stags = mutableListOf<String>()
      val stagArr = obj.optJSONArray("stagnationAlerts")
      if (stagArr != null) {
        for (i in 0 until stagArr.length()) {
          stags.add(stagArr.getString(i))
        }
      }

      AiProgressAnalysis(
        overallScore = score,
        hypertrophyStatus = hypertrophy,
        progressiveOverloadVerdict = overload,
        recommendations = if (recs.isNotEmpty()) recs else listOf("Maintain current volume ramp."),
        detectedSplitName = splitName,
        detectedSplitBreakdown = breakdown,
        stagnationAlerts = stags
      )
    } catch (_: Exception) {
      null
    }
  }
}
