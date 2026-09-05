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

enum class GeminiChatModel(val modelId: String, val displayName: String, val badge: String) {
  FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "Standard"),
  PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Deep Reasoning"),
  FLASH_LITE("gemini-3.1-flash-lite-preview", "Gemini 3.1 Flash-Lite", "Fast Cues")
}

class GeminiService(
  private val customApiKeyProvider: (() -> String)? = null
) {

  private val client = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  val effectiveApiKey: String
    get() {
      val custom = customApiKeyProvider?.invoke()?.trim()
      if (!custom.isNullOrBlank() && custom != "MY_GEMINI_API_KEY") {
        return custom
      }
      return try {
        val bKey = BuildConfig.GEMINI_API_KEY.trim()
        if (bKey.isNotBlank() && bKey != "MY_GEMINI_API_KEY") bKey else ""
      } catch (_: Throwable) {
        ""
      }
    }

  val isKeyConfigured: Boolean
    get() = effectiveApiKey.isNotBlank() && effectiveApiKey != "MY_GEMINI_API_KEY"

  suspend fun parseGymRant(
    rantText: String,
    isOnline: Boolean = true
  ): ParsedWorkoutRant = withContext(Dispatchers.IO) {
    parseGymRants(rantText, isOnline).firstOrNull() ?: OfflineRantParser.parseRant(rantText)
  }

  suspend fun parseGymRants(
    rantText: String,
    isOnline: Boolean = true
  ): List<ParsedWorkoutRant> = withContext(Dispatchers.IO) {
    if (!isKeyConfigured) {
      Log.i("GeminiService", "Gemini API key is not configured; using offline parser.")
      return@withContext OfflineRantParser.parseMultiWorkoutRant(rantText)
    }

    try {
      Log.i("GeminiService", "Analyzing workout notes using Gemini 3.5 Flash...")
      val prompt = """
        You are an expert bodybuilding and strength training parser and biomechanics extraction engine.
        You map unstructured, rant-style lifter notes into a structured workout schema.
        
        CRITICAL PARSING RULES:
        1. Set-by-Set Tracking & Detailed Logs: Break down lifts into distinct, individual sets!
           - "incline db 60s 10, 8, 8" -> 3 sets: Set 1: 60 lbs x 10, Set 2: 60 lbs x 8, Set 3: 60 lbs x 8.
           - "3 sets of 10 at 185" -> 3 sets: 185 lbs x 10 reps each.
           - "heavy single 225, then 185 3x8" -> Set 1: 225 lbs x 1, Set 2: 185 lbs x 8, Set 3: 185 lbs x 8, Set 4: 185 lbs x 8.
        2. Smart Rep Inference (NEVER ask the lifter what reps they did in the past):
           - If reps are not specified for an exercise (e.g., "incline dumbbell press 60s 3 sets" or "did bench with 185"):
             Intelligently infer standard hypertrophy reps based on the exercise type:
             * Heavy Barbell Compounds (Bench, Squat, Deadlift, Barbell Row, OHP): 6-8 reps
             * Dumbbell Presses, Dumbbell Rows, Lunges: 8-10 reps
             * Cables, Machines, Curls, Tricep Pushdowns, Lateral Raises: 10-12 reps
             * Bodyweight, Abs, Calves: 12-15 reps
           - NEVER output 0 reps or ask "How many reps did you complete on [date]?". Lifters do not remember historical reps from weeks ago. Deduce them seamlessly!
        3. Automatic Workout Title Analysis:
           - Analyze the collection of exercises in each session and assign an intuitive, descriptive workout name:
             * Chest, Upper Chest, Triceps, Shoulders -> "Push Day" or "Chest & Triceps"
             * Back, Lat Pulldown, Rows, Bicep Curls -> "Pull Day" or "Back & Biceps"
             * Squats, Leg Press, Leg Extension, Hamstrings, Calves -> "Leg Day"
             * Chest and Back together -> "Upper Body"
             * Legs and Core -> "Lower Body"
             * Biceps, Triceps, Delts -> "Arms & Shoulders"
             * Full body spectrum -> "Full Body"
           - If the user explicitly provided a title or headline (e.g. "Push Day 1", "Heavy Leg Day", "Chest & Back"), PRIORITIZE THEIR EXACT TITLE!
           - NEVER return generic "Logged Workout" or leave the title empty!
        4. Accurate Canonical Exercise Names:
           - Correct typos and gym slang to clean canonical exercise names:
             "bench" / "flat bench" -> "Barbell Bench Press"
             "inc db" / "incline dumbbell" -> "Incline Dumbbell Press"
             "lat pulldown" / "pulldowns" -> "Lat Pulldown"
             "seated row" / "cable row" -> "Seated Cable Row"
             "db curl" / "curls" / "bicep curl" -> "Barbell Bicep Curl" or "Dumbbell Curl"
             "rope pushdown" / "pushdown" -> "Tricep Rope Pushdown"
             "lateral raise" / "lat raise" / "side raises" -> "Dumbbell Lateral Raise"
             "squats" -> "Barbell Back Squat"
             "leg press" -> "Leg Press"
             "rdl" -> "Romanian Deadlift"
             "hamstring curl" / "leg curl" -> "Hamstring Leg Curl"
             "calf raises" -> "Standing Calf Raise"
             "ohp" / "military press" -> "Overhead Barbell Press"
           - DO NOT mix up muscle groups! Bicep curls are biceps, pushdowns are triceps, lateral raises are delts, lat pulldowns are back.
        5. Pounds (LBS) vs Kilograms (KG) Unit Detection:
           - Analyze whether the lifter's weights are in pounds (LBS) or kilograms (KG).
           - If the lifter mentions "lb", "lbs", "pounds", or uses typical American dumbbell sizes (e.g. 35s, 40s, 50s, 60s, 70s, 80s) or barbell plate numbers (e.g. 95, 135, 155, 185, 205, 225, 245, 275, 315) without explicitly specifying "kg":
             Treat the unit as "LBS"!
           - If weights are in LBS, convert them to kilograms for storage: `weightKg = weightLbs / 2.20462`, rounded to 1 decimal place (e.g., 60 lbs -> 27.2 kg, 185 lbs -> 83.9 kg, 225 lbs -> 102.1 kg, 50 lbs -> 22.7 kg).
           - Output "detectedUnit": "LBS" (or "KG" if explicitly in kg).
        6. Fractional & Partial Reps: Recognize decimal points in reps (e.g., "6.5 reps failure", "failed at 6.5") as exact decimal numbers in "reps" (e.g. 6.5). Never round up to an integer!
        7. Muscular Failure Points: If the lifter mentions where they failed (e.g. "preacher curl machine stuck at 90 degrees", "failed at 6.5 reps"), record this in "failurePoint".
        8. Biofeedback Translation: Map subjective notes into standardized tags in "biofeedbackTags" array:
           - "fingers hurt", "grip gave out", "straps slipped" -> "grip_fatigue"
           - "felt awkward", "form breakdown", "lost arch" -> "form_breakdown"
           - "left arm weaker", "discrepancy" -> "asymmetry"
           - "armpit discomfort", "shoulder pinch", "knee twinge" -> "joint_discomfort"
           - "insane burn", "mind-muscle peak", "crazy pump" -> "peak_burn"
           - "gassed out", "out of breath", "cardio fatigue" -> "cardio_fatigue"
        9. Asymmetric Unilateral Discrepancies: If left and right sides performed different reps or weights, split them into dual exercise entries or separate sets marked with side "LEFT" and "RIGHT".
        10. Mid-Set Adjustments & Drop Sets: If the lifter drops weight mid-set, capture "dropWeightKg", "dropReps", and setKind="DROP".
        11. Bodyweight Exercises: For pull-ups, push-ups, dips, planks, and hanging leg raises, set weightKg=0.0.
        12. Date Recognition: Identify if notes span multiple sessions/dates (e.g. "Aug 20", "Yesterday", "3 days ago"). Create a distinct workout object for each session!
        13. Keep "clarificationQuestions" empty `[]`. Do NOT nag the user with unanswerable questions about past dates or reps.
        
        Text to parse:
        "$rantText"
        
        Respond ONLY with a valid JSON array of objects with this schema:
        [
          {
            "workoutTitle": "Push Day",
            "detectedUnit": "LBS",
            "dateDisplay": "Yesterday",
            "workoutDateMillis": ${System.currentTimeMillis()},
            "notes": "Original notes",
            "exercises": [
              {
                "exerciseName": "Incline Dumbbell Press",
                "isUnilateral": false,
                "sets": [
                  {
                    "weightKg": 27.2,
                    "reps": 10.0,
                    "setKind": "NORMAL",
                    "side": "BOTH",
                    "failurePoint": "",
                    "biofeedbackTags": [],
                    "tempo": "controlled negative",
                    "dropWeightKg": 0.0,
                    "dropReps": 0.0
                  }
                ]
              }
            ],
            "clarificationQuestions": []
          }
        ]
      """.trimIndent()

      val jsonResponse = callGemini(prompt)
      if (jsonResponse != null) {
        val parsedList = parseRantsJson(jsonResponse, rantText)
        if (parsedList.isNotEmpty()) {
          Log.i("GeminiService", "Successfully parsed ${parsedList.size} workout session(s) via Gemini 3.5 Flash")
          return@withContext parsedList
        }
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Gemini online multi-rant parsing failed, falling back to offline parser", e)
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
    val key = effectiveApiKey
    if (key.isBlank() || key == "MY_GEMINI_API_KEY") {
      Log.w("GeminiService", "Gemini API key is not configured or is placeholder")
      return null
    }
    val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$key"

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
      put("generationConfig", JSONObject().apply {
        put("responseMimeType", "application/json")
        put("temperature", 0.1)
      })
    }

    val request = Request.Builder()
      .url(url)
      .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        val errBody = response.body?.string() ?: ""
        Log.w("GeminiService", "Gemini HTTP call failed code=${response.code} message=${response.message} body=$errBody")
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

      val detectedUnit = obj.optString("detectedUnit", "LBS")

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
          val rawReps = sObj.optDouble("reps", 10.0)
          val reps = if (rawReps <= 0.0) 10.0 else rawReps
          val setKind = sObj.optString("setKind", "NORMAL")
          val side = sObj.optString("side", "BOTH")
          val failurePoint = sObj.optString("failurePoint", "")
          val tempo = sObj.optString("tempo", "")
          val dropWeight = sObj.optDouble("dropWeightKg", 0.0)
          val dropReps = sObj.optDouble("dropReps", 0.0)

          val bioTagsList = mutableListOf<String>()
          val bioArr = sObj.optJSONArray("biofeedbackTags")
          if (bioArr != null) {
            for (b in 0 until bioArr.length()) {
              val tag = bioArr.optString(b)
              if (tag.isNotBlank()) bioTagsList.add(tag)
            }
          }
          val biofeedbackTags = bioTagsList.joinToString(",")

          setsList.add(
            ParsedSetLog(
              weightKg = weight,
              reps = reps,
              setKind = setKind,
              side = side,
              biofeedbackTags = biofeedbackTags,
              tempo = tempo,
              failurePoint = failurePoint,
              dropWeightKg = dropWeight,
              dropReps = dropReps
            )
          )
        }

        val isUnilateral = exObj.optBoolean("isUnilateral", false) ||
          name.contains("(Left)", ignoreCase = true) ||
          name.contains("(Right)", ignoreCase = true)

        if (setsList.isNotEmpty()) {
          exercisesList.add(
            ParsedExerciseLog(
              exerciseName = name,
              sets = setsList,
              isUnilateral = isUnilateral
            )
          )
        }
      }

      val finalTitle = if (title.isBlank() || title.equals("Logged Workout", ignoreCase = true) || title.equals("Workout", ignoreCase = true)) {
        OfflineRantParser.suggestWorkoutTitle(exercisesList)
      } else {
        title
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
        workoutTitle = finalTitle,
        exercises = exercisesList,
        notes = notes,
        workoutDateMillis = dateMillis,
        dateDisplay = dateDisplay,
        clarificationQuestions = questions,
        detectedUnit = detectedUnit
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
  ): Pair<String, List<String>> {
    return chatWithJim(
      historyTurns = emptyList(),
      userMessage = userMessage,
      lifterProfile = lifterProfile,
      recentWorkoutSummary = recentWorkoutSummary,
      model = GeminiChatModel.FLASH,
      isOnline = isOnline
    )
  }

  suspend fun chatWithJim(
    historyTurns: List<Pair<String, String>>,
    userMessage: String,
    lifterProfile: String,
    recentWorkoutSummary: String,
    model: GeminiChatModel = GeminiChatModel.FLASH,
    isOnline: Boolean
  ): Pair<String, List<String>> = withContext(Dispatchers.IO) {
    if (!isOnline || !isKeyConfigured) {
      return@withContext getOfflineJimResponse(userMessage, lifterProfile)
    }

    try {
      val systemPrompt = """
        You are "Jim", an elite evidence-based bodybuilding coach, biomechanics specialist, and progressive overload strategist.
        Your name is Jim.
        
        CRITICAL ROLE MANDATES:
        - Your name is Jim. Speak directly to the lifter as their coach Jim.
        - Tone: Motivating, sharp, scientific, and athletic with zero fluff and practical biomechanics (hypertrophy, mechanical tension, lengthening partials, progressive overload, fatigue management).
        - Multi-Turn Conversation: Actively maintain conversational context and remember previous instructions and logs discussed with the lifter.
        - Analyze granular failure points, unilateral imbalances, volume ramps, and recovery markers.
        - ALWAYS end your coaching response with 1-2 targeted, thought-provoking questions to guide the lifter's next action or assess their fatigue.
        
        Lifter Profile:
        $lifterProfile
        
        Recent Workouts History:
        $recentWorkoutSummary
        
        Format your response as a JSON object:
        {
          "jimReply": "Your direct coaching analysis followed by 1-2 probing coach questions",
          "suggestedFollowUps": ["Short quick reply chip 1", "Short quick reply chip 2", "Short quick reply chip 3"]
        }
      """.trimIndent()

      val url = "https://generativelanguage.googleapis.com/v1beta/models/${model.modelId}:generateContent?key=$effectiveApiKey"

      val requestBodyJson = JSONObject().apply {
        // System instruction
        val sysInstObj = JSONObject().apply {
          val partsArr = JSONArray().apply {
            put(JSONObject().apply { put("text", systemPrompt) })
          }
          put("parts", partsArr)
        }
        put("systemInstruction", sysInstObj)

        // Contents array for multi-turn chat
        val contentsArray = JSONArray()

        // Add previous history turns (up to last 10 turns to stay optimal)
        historyTurns.takeLast(10).forEach { (sender, text) ->
          if (text.isNotBlank()) {
            val role = if (sender.equals("USER", ignoreCase = true)) "user" else "model"
            contentsArray.put(JSONObject().apply {
              put("role", role)
              put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", text) })
              })
            })
          }
        }

        // Add current user turn
        contentsArray.put(JSONObject().apply {
          put("role", "user")
          put("parts", JSONArray().apply {
            put(JSONObject().apply { put("text", userMessage) })
          })
        })

        put("contents", contentsArray)

        // Generation config
        val genConfig = JSONObject().apply {
          put("temperature", 0.7)
          put("responseMimeType", "application/json")
        }
        put("generationConfig", genConfig)
      }

      val request = Request.Builder()
        .url(url)
        .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val responseText = response.body?.string()
          if (!responseText.isNullOrBlank()) {
            val root = JSONObject(responseText)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
              val firstCandidate = candidates.getJSONObject(0)
              val content = firstCandidate.optJSONObject("content")
              val parts = content?.optJSONArray("parts")
              if (parts != null && parts.length() > 0) {
                val rawText = parts.getJSONObject(0).optString("text")
                val clean = cleanJsonText(rawText)
                try {
                  val obj = JSONObject(clean)
                  val reply = obj.optString("jimReply", obj.optString("coachReply", ""))
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
                } catch (_: Exception) {
                  if (clean.isNotBlank()) {
                    return@withContext Pair(
                      clean,
                      listOf("Got it, Jim", "How should I structure my next set?", "Check my recovery")
                    )
                  }
                }
              }
            }
          }
        } else {
          Log.w("GeminiService", "Jim chat failed with HTTP code: ${response.code} on model ${model.modelId}")
        }
      }
    } catch (e: Exception) {
      Log.e("GeminiService", "Jim chat error", e)
    }

    getOfflineJimResponse(userMessage, lifterProfile)
  }

  private fun getOfflineTrainerResponse(userMessage: String, lifterProfile: String): Pair<String, List<String>> {
    return getOfflineJimResponse(userMessage, lifterProfile)
  }

  private fun getOfflineJimResponse(userMessage: String, lifterProfile: String): Pair<String, List<String>> {
    val lower = userMessage.lowercase()
    return when {
      lower.contains("sleep") || lower.contains("recover") || lower.contains("rest") -> {
        Pair(
          "Sleep is your primary anabolic window—growth hormone peak occurs in slow-wave sleep. If you get under 7 hours, your nervous system recovery drops by 30%, which directly impacts 1RM bench and squat stability.\n\nJim's Question: How many hours did you log last night, and which muscle group feels the most sore or inflamed right now?",
          listOf("Slept 7-8 hours, ready", "Under 6 hours, fatigued", "Chest & front delts are sore", "Legs feel completely fried")
        )
      }
      lower.contains("bench") || lower.contains("chest") -> {
        Pair(
          "For bench overload: Keep shoulder blades depressed and retracted into the pad, create full leg drive with heels planted, and tuck elbows at ~45° to recruit sternal pecs while protecting the rotator cuff. When stuck, adding a 1-second pause on the chest builds explosive bottom-end power.\n\nJim's Question: What is your current working weight on bench press, and where in the movement do you usually fail (off the chest, or mid-way at lockout)?",
          listOf("Stuck off the chest", "Lockout failure (triceps)", "Current bench is 80kg", "Shoulder hurts during press")
        )
      }
      lower.contains("squat") || lower.contains("leg") -> {
        Pair(
          "On squats, ensure your ribcage is stacked over your pelvis with 360-degree intra-abdominal bracing. Drive knees out over your middle toes and maintain equal foot pressure across tripod contact points (big toe, pinky toe, heel).\n\nJim's Question: Are you doing low-bar or high-bar squats, and do you feel any knee or hip tightness after leg day?",
          listOf("High-bar Olympic style", "Low-bar powerlifting style", "Knees feel slightly stiff", "Quads are super sore")
        )
      }
      lower.contains("protein") || lower.contains("diet") || lower.contains("calorie") || lower.contains("eat") -> {
        Pair(
          "To optimize muscle protein synthesis (MPS), aim for 1.8g to 2.2g of protein per kg of body weight, split across 3 to 5 meals. Each feeding should contain at least 2.5g to 3g of leucine to trigger mTOR activation.\n\nJim's Question: Are you currently in a caloric surplus (bulking), a deficit (cutting), or eating at maintenance?",
          listOf("Caloric surplus (bulking)", "Caloric deficit (cutting)", "Maintenance recomp", "Track my daily protein")
        )
      }
      lower.contains("plateau") || lower.contains("stuck") -> {
        Pair(
          "Plateaus are usually caused by accumulated systemic fatigue or lack of targeted accessory stimulus. We can break it with micro-loading (+1kg per side), wave periodization (shifting from 3x8 to 5x5), or a deload week.\n\nJim's Question: Which specific exercise has been stalled, and for how many weeks have the weights or reps stayed the same?",
          listOf("Barbell Bench Press", "Overhead Barbell Press", "Barbell Back Squat", "Stalled for 3 weeks")
        )
      }
      else -> {
        Pair(
          "I'm locked in as your coach, Jim. My goal is to optimize your biomechanics, volume management, and progressive overload so you make steady, injury-free gains every single week.\n\nJim's Question: To dial in your program today: How is your energy level right now (1-10), and what muscle group are you hitting today?",
          listOf("Energy is 8/10, hitting Chest", "Feeling 6/10, hitting Back/Pull", "Leg Day today", "Need a 10-minute warm-up")
        )
      }
    }
  }
}
