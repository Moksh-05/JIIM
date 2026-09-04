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
    parseGymRants(rantText, isOnline).firstOrNull() ?: OfflineRantParser.parseRant(rantText)
  }

  suspend fun parseGymRants(
    rantText: String,
    isOnline: Boolean
  ): List<ParsedWorkoutRant> = withContext(Dispatchers.IO) {
    if (!isOnline || !isKeyConfigured) {
      return@withContext OfflineRantParser.parseMultiWorkoutRant(rantText)
    }

    try {
      val prompt = """
        You are an expert gym logger assistant for a lifter.
        The user has provided a text containing ONE or MULTIPLE past gym workouts, separated by dates, lines, or sections.
        Extract every workout separately. For each workout, determine:
        1. "workoutTitle": Title (e.g., "Chest & Triceps", "Push Day", "Leg Day", or default based on exercises)
        2. "dateDisplay": Date string mentioned (e.g., "Aug 20, 2026", "Yesterday", "3 days ago", or "Today")
        3. "workoutDateMillis": Approximate timestamp in epoch milliseconds (for reference, current time is ${System.currentTimeMillis()})
        4. "exercises": Array of exercises with "exerciseName" and "sets" (each set with "weightKg": double and "reps": int)
        5. "notes": Notes or original description for that specific workout
        6. "clarificationQuestions": Array of question strings to ask the lifter if anything is ambiguous, missing (like weight or reps), or if a date is unclear.
        
        Gym text:
        "$rantText"
        
        Respond ONLY with a valid JSON array of objects in this exact schema, without markdown formatting or code fences:
        [
          {
            "workoutTitle": "Chest & Triceps",
            "dateDisplay": "Aug 20, 2026",
            "workoutDateMillis": 1724148000000,
            "exercises": [
              {
                "exerciseName": "Barbell Bench Press",
                "sets": [
                  {"weightKg": 80.0, "reps": 8}
                ]
              }
            ],
            "notes": "Felt good pump",
            "clarificationQuestions": []
          }
        ]
      """.trimIndent()

      val jsonResponse = callGemini(prompt)
      if (jsonResponse != null) {
        val parsedList = parseRantsJson(jsonResponse, rantText)
        if (parsedList.isNotEmpty()) {
          return@withContext parsedList
        }
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Online multi-rant parsing failed, falling back to offline parser", e)
    }

    // Fallback to offline multi-parser
    OfflineRantParser.parseMultiWorkoutRant(rantText)
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

  private fun parseRantsJson(jsonStr: String, originalText: String): List<ParsedWorkoutRant> {
    val clean = cleanJsonText(jsonStr)
    val results = mutableListOf<ParsedWorkoutRant>()

    try {
      if (clean.startsWith("[")) {
        val arr = JSONArray(clean)
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          parseWorkoutObject(obj, originalText)?.let { results.add(it) }
        }
      } else if (clean.startsWith("{")) {
        val obj = JSONObject(clean)
        // Check if top-level has a "workouts" array
        val workoutsArr = obj.optJSONArray("workouts")
        if (workoutsArr != null) {
          for (i in 0 until workoutsArr.length()) {
            val wObj = workoutsArr.getJSONObject(i)
            parseWorkoutObject(wObj, originalText)?.let { results.add(it) }
          }
        } else {
          parseWorkoutObject(obj, originalText)?.let { results.add(it) }
        }
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Failed to parse rants JSON", e)
    }

    return results
  }

  private fun parseWorkoutObject(obj: JSONObject, originalText: String): ParsedWorkoutRant? {
    return try {
      val title = obj.optString("workoutTitle", "Logged Workout")
      val notes = obj.optString("notes", originalText)
      val dateDisplay = obj.optString("dateDisplay", "Today")
      val dateMillis = obj.optLong("workoutDateMillis", System.currentTimeMillis())

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

      val questions = mutableListOf<String>()
      val qArr = obj.optJSONArray("clarificationQuestions")
      if (qArr != null) {
        for (k in 0 until qArr.length()) {
          val q = qArr.optString(k, "")
          if (q.isNotBlank()) questions.add(q)
        }
      }

      ParsedWorkoutRant(
        workoutTitle = title,
        exercises = exercisesList,
        notes = notes,
        workoutDateMillis = dateMillis,
        dateDisplay = dateDisplay,
        clarificationQuestions = questions
      )
    } catch (_: Exception) {
      null
    }
  }

  private fun parseRantJson(jsonStr: String, originalText: String): ParsedWorkoutRant? {
    return parseRantsJson(jsonStr, originalText).firstOrNull()
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

  suspend fun chatWithTrainerJJ(
    userMessage: String,
    historyContext: String,
    lifterProfile: String,
    recentWorkoutSummary: String,
    isOnline: Boolean
  ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
    if (!isOnline || !isKeyConfigured) {
      return@withContext getOfflineTrainerResponse(userMessage, lifterProfile)
    }

    try {
      val prompt = """
        You are "JIIM AI", an elite gym coach, biomechanics specialist, and hypertrophy trainer inside the JIIM app.
        Your tone is motivating, sharp, scientific, and athletic. You talk like a seasoned lifter and coach (concise, high impact, no fluff).
        
        CRITICAL MANDATE:
        - The lifter specifically expects you to ask questions often to gather as much insight as possible into their recovery, sleep, muscle soreness, joint health, and lifting goals.
        - Give direct, actionable lifting/nutrition advice.
        - ALWAYS end your response with 1-2 targeted questions to extract more insights about their training, fatigue, or form.
        
        Lifter Profile:
        $lifterProfile
        
        Recent Workouts History:
        $recentWorkoutSummary
        
        Previous Conversation:
        $historyContext
        
        Lifter says:
        "$userMessage"
        
        Respond with a JSON object:
        {
          "coachReply": "your direct answer followed by 1-2 probing coach questions",
          "suggestedFollowUps": ["Short quick reply chip 1", "Short quick reply chip 2", "Short quick reply chip 3"]
        }
      """.trimIndent()

      val rawJson = callGemini(prompt)
      if (rawJson != null) {
        try {
          val obj = JSONObject(rawJson)
          val reply = obj.optString("coachReply", "")
          val followUps = mutableListOf<String>()
          val arr = obj.optJSONArray("suggestedFollowUps")
          if (arr != null) {
            for (i in 0 until arr.length()) {
              followUps.add(arr.getString(i))
            }
          }
          if (reply.isNotBlank()) {
            return@withContext Pair(reply, followUps)
          }
        } catch (_: Exception) {}
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "JIIM AI chat error", e)
    }

    getOfflineTrainerResponse(userMessage, lifterProfile)
  }

  private fun getOfflineTrainerResponse(userMessage: String, lifterProfile: String): Pair<String, List<String>> {
    val lower = userMessage.lowercase()
    return when {
      lower.contains("sleep") || lower.contains("recover") || lower.contains("rest") -> {
        Pair(
          "Sleep is your primary anabolic window—growth hormone peak occurs in slow-wave sleep. If you get under 7 hours, your nervous system recovery drops by 30%, which directly impacts 1RM bench and squat stability.\n\nJIIM AI's Insight Question: How many hours did you log last night, and which muscle group feels the most sore or inflamed right now?",
          listOf("Slept 7-8 hours, ready", "Under 6 hours, fatigued", "Chest & front delts are sore", "Legs feel completely fried")
        )
      }
      lower.contains("bench") || lower.contains("chest") -> {
        Pair(
          "For bench overload: Keep shoulder blades depressed and retracted into the pad, create full leg drive with heels planted, and tuck elbows at ~45° to recruit sternal pecs while protecting the rotator cuff. When stuck, adding a 1-second pause on the chest builds explosive bottom-end power.\n\nJIIM AI's Insight Question: What is your current working weight on bench press, and where in the movement do you usually fail (off the chest, or mid-way at lockout)?",
          listOf("Stuck off the chest", "Lockout failure (triceps)", "Current bench is 80kg", "Shoulder hurts during press")
        )
      }
      lower.contains("squat") || lower.contains("leg") -> {
        Pair(
          "On squats, ensure your ribcage is stacked over your pelvis with 360-degree intra-abdominal bracing. Drive knees out over your middle toes and maintain equal foot pressure across tripod contact points (big toe, pinky toe, heel).\n\nJIIM AI's Insight Question: Are you doing low-bar or high-bar squats, and do you feel any knee or hip tightness after leg day?",
          listOf("High-bar Olympic style", "Low-bar powerlifting style", "Knees feel slightly stiff", "Quads are super sore")
        )
      }
      lower.contains("protein") || lower.contains("diet") || lower.contains("calorie") || lower.contains("eat") -> {
        Pair(
          "To optimize muscle protein synthesis (MPS), aim for 1.8g to 2.2g of protein per kg of body weight, split across 3 to 5 meals. Each feeding should contain at least 2.5g to 3g of leucine to trigger mTOR activation.\n\nJIIM AI's Insight Question: Are you currently in a caloric surplus (bulking), a deficit (cutting), or eating at maintenance?",
          listOf("Caloric surplus (bulking)", "Caloric deficit (cutting)", "Maintenance recomp", "Track my daily protein")
        )
      }
      lower.contains("plateau") || lower.contains("stuck") -> {
        Pair(
          "Plateaus are usually caused by accumulated systemic fatigue or lack of targeted accessory stimulus. We can break it with micro-loading (+1kg per side), wave periodization (shifting from 3x8 to 5x5), or a deload week.\n\nJIIM AI's Insight Question: Which specific exercise has been stalled, and for how many weeks have the weights or reps stayed the same?",
          listOf("Barbell Bench Press", "Overhead Barbell Press", "Barbell Back Squat", "Stalled for 3 weeks")
        )
      }
      else -> {
        Pair(
          "I'm locked in as your coach. My goal is to optimize your biomechanics, volume management, and progressive overload so you make steady, injury-free gains every single week.\n\nJIIM AI's Insight Question: To dial in your program today: How is your energy level right now (1-10), and what muscle group are you hitting today?",
          listOf("Energy is 8/10, hitting Chest", "Feeling 6/10, hitting Back/Pull", "Leg Day today", "Need a 10-minute warm-up")
        )
      }
    }
  }
}
