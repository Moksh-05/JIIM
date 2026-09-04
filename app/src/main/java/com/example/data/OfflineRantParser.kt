package com.example.data

import com.example.model.ExerciseLibrary
import com.example.model.ParsedExerciseLog
import com.example.model.ParsedSetLog
import com.example.model.ParsedWorkoutRant
import java.util.Locale

object OfflineRantParser {

  // Aliases mapping colloquial gym slang to canonical exercise names
  private val exerciseAliases = mapOf(
    "bench" to "Barbell Bench Press",
    "bench press" to "Barbell Bench Press",
    "flat bench" to "Barbell Bench Press",
    "barbell bench" to "Barbell Bench Press",
    "incline db" to "Incline Dumbbell Press",
    "incline dumbbell" to "Incline Dumbbell Press",
    "incline dumbbell press" to "Incline Dumbbell Press",
    "incline bench" to "Incline Dumbbell Press",
    "cable fly" to "Cable Chest Fly",
    "chest fly" to "Cable Chest Fly",
    "dips" to "Dips",
    "pushups" to "Push Ups",
    "push ups" to "Push Ups",
    "squat" to "Barbell Back Squat",
    "squats" to "Barbell Back Squat",
    "back squat" to "Barbell Back Squat",
    "barbell squat" to "Barbell Back Squat",
    "leg press" to "Leg Press",
    "leg extension" to "Leg Extension",
    "leg extensions" to "Leg Extension",
    "leg curl" to "Hamstring Leg Curl",
    "hamstring curl" to "Hamstring Leg Curl",
    "rdl" to "Romanian Deadlift",
    "rdls" to "Romanian Deadlift",
    "romanian deadlift" to "Romanian Deadlift",
    "deadlift" to "Barbell Deadlift",
    "deadlifts" to "Barbell Deadlift",
    "calves" to "Standing Calf Raise",
    "calf raise" to "Standing Calf Raise",
    "calf raises" to "Standing Calf Raise",
    "ohp" to "Overhead Barbell Press",
    "overhead press" to "Overhead Barbell Press",
    "shoulder press" to "Dumbbell Shoulder Press",
    "military press" to "Overhead Barbell Press",
    "lateral raise" to "Dumbbell Lateral Raise",
    "lateral raises" to "Dumbbell Lateral Raise",
    "side raises" to "Dumbbell Lateral Raise",
    "face pulls" to "Face Pulls",
    "facepulls" to "Face Pulls",
    "lat pulldown" to "Lat Pulldown",
    "lat pulldowns" to "Lat Pulldown",
    "pull down" to "Lat Pulldown",
    "pulldown" to "Lat Pulldown",
    "cable row" to "Seated Cable Row",
    "seated row" to "Seated Cable Row",
    "barbell row" to "Barbell Bent-Over Row",
    "bent over row" to "Barbell Bent-Over Row",
    "bent over rows" to "Barbell Bent-Over Row",
    "pullups" to "Pull-Ups",
    "pull ups" to "Pull-Ups",
    "chin ups" to "Pull-Ups",
    "bicep curl" to "Barbell Bicep Curl",
    "bicep curls" to "Barbell Bicep Curl",
    "curls" to "Barbell Bicep Curl",
    "barbell curl" to "Barbell Bicep Curl",
    "hammer curl" to "Hammer Curl",
    "hammer curls" to "Hammer Curl",
    "incline curl" to "Incline Dumbbell Curl",
    "incline curls" to "Incline Dumbbell Curl",
    "tricep pushdown" to "Tricep Rope Pushdown",
    "tricep pushdowns" to "Tricep Rope Pushdown",
    "pushdown" to "Tricep Rope Pushdown",
    "pushdowns" to "Tricep Rope Pushdown",
    "rope pushdown" to "Tricep Rope Pushdown",
    "skull crushers" to "Skull Crushers",
    "skullcrushers" to "Skull Crushers",
    "hanging leg raise" to "Hanging Leg Raise",
    "leg raises" to "Hanging Leg Raise",
    "plank" to "Plank",
    "woodchoppers" to "Cable Woodchoppers"
  )

  fun parseRant(text: String): ParsedWorkoutRant {
    val results = parseMultiWorkoutRant(text)
    return results.firstOrNull() ?: ParsedWorkoutRant(
      workoutTitle = "Quick Workout",
      exercises = emptyList(),
      notes = text.trim()
    )
  }

  fun parseMultiWorkoutRant(text: String): List<ParsedWorkoutRant> {
    val clean = text.trim()
    if (clean.isBlank()) return emptyList()

    val blocks = splitIntoWorkoutBlocks(clean)
    val parsedWorkouts = mutableListOf<ParsedWorkoutRant>()

    for ((idx, block) in blocks.withIndex()) {
      val parsed = parseWorkoutBlock(block, fallbackDayOffset = (blocks.size - 1 - idx))
      parsedWorkouts.add(parsed)
    }

    return parsedWorkouts.ifEmpty {
      listOf(parseWorkoutBlock(clean, fallbackDayOffset = 0))
    }
  }

  private fun splitIntoWorkoutBlocks(text: String): List<String> {
    val lines = text.lines()
    val blocks = mutableListOf<String>()
    var currentBlock = StringBuilder()

    val dateHeaderRegex = Regex(
      """(?i)^\s*(?:[-*•#>]+\s*)?(?:(?:workout|session|day)\s*\d*[:\s-]+)?(?:on\s+)?(today|yesterday|\d+\s+days?\s+ago|last\s+\w+|\w+day|(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s+\d{1,2}(?:st|nd|rd|th)?(?:,?\s*\d{4})?|\d{1,2}(?:st|nd|rd|th)?\s+(?:of\s+)?(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*(?:,?\s*\d{4})?|\d{1,2}[/-]\d{1,2}(?:[/-]\d{2,4})?)(?:\s*[:,–-].*)?$"""
    )

    val dividerRegex = Regex("""^\s*[-=_*]{3,}\s*$""")

    for (line in lines) {
      val trimmed = line.trim()
      if (dividerRegex.matches(trimmed)) {
        if (currentBlock.isNotBlank()) {
          blocks.add(currentBlock.toString().trim())
          currentBlock = StringBuilder()
        }
        continue
      }

      if (dateHeaderRegex.matches(trimmed) && currentBlock.isNotBlank()) {
        blocks.add(currentBlock.toString().trim())
        currentBlock = StringBuilder()
      }

      currentBlock.append(line).append("\n")
    }

    if (currentBlock.isNotBlank()) {
      blocks.add(currentBlock.toString().trim())
    }

    return blocks
  }

  private fun parseWorkoutBlock(block: String, fallbackDayOffset: Int): ParsedWorkoutRant {
    val clean = block.trim()
    val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val (dateMillis, dateDisplay, dateClarification) = extractDateFromBlock(clean, fallbackDayOffset)

    // Detect title from first line or exercises
    var detectedTitle = "Logged Workout"
    val firstLine = lines.firstOrNull() ?: ""
    val lowerFirst = firstLine.lowercase(Locale.ROOT)
    val lowerFull = clean.lowercase(Locale.ROOT)

    when {
      lowerFull.contains("push") -> detectedTitle = "Push Day"
      lowerFull.contains("pull") -> detectedTitle = "Pull Day"
      lowerFull.contains("leg") -> detectedTitle = "Leg Day"
      lowerFull.contains("chest") && lowerFull.contains("tricep") -> detectedTitle = "Chest & Triceps"
      lowerFull.contains("bicep") && lowerFull.contains("shoulder") -> detectedTitle = "Biceps & Shoulders"
      lowerFull.contains("arm") -> detectedTitle = "Arms Workout"
      lowerFull.contains("back") -> detectedTitle = "Back Workout"
      lowerFull.contains("upper") -> detectedTitle = "Upper Body"
      lowerFull.contains("lower") -> detectedTitle = "Lower Body"
      lowerFull.contains("shoulder") -> detectedTitle = "Shoulders Workout"
    }

    // Split text into clauses / lines / sentences for exercise parsing
    val segments = clean.split(Regex("[\n;.]|(?<=[0-9])\\s*,\\s*(?=[a-zA-Z])|(?i)\\s+(then|next|after that|afterwards|finished with|also did)\\s+"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }

    val parsedExercises = mutableListOf<ParsedExerciseLog>()
    val clarifications = mutableListOf<String>()

    if (dateClarification != null) {
      clarifications.add(dateClarification)
    }

    for (segment in segments) {
      val (matchedName, sets) = parseSegment(segment)
      if (matchedName != null && sets.isNotEmpty()) {
        parsedExercises.add(
          ParsedExerciseLog(
            exerciseName = matchedName,
            sets = sets
          )
        )
      }
    }

    // Fallback: If no segments separated by punct, scan for known exercises across whole text
    if (parsedExercises.isEmpty()) {
      for ((alias, canonical) in exerciseAliases.entries.sortedByDescending { it.key.length }) {
        val idx = lowerFull.indexOf(alias)
        if (idx != -1) {
          val sub = clean.substring(idx)
          val (_, sets) = parseSegment(sub)
          if (sets.isNotEmpty() && parsedExercises.none { it.exerciseName == canonical }) {
            parsedExercises.add(
              ParsedExerciseLog(
                exerciseName = canonical,
                sets = sets
              )
            )
          }
        }
      }
    }

    // Check for missing weights or reps to ask user clarifications
    for (ex in parsedExercises) {
      val missingWeight = ex.sets.any { it.weightKg <= 0.0 }
      val missingReps = ex.sets.any { it.reps <= 0 }
      if (missingWeight) {
        clarifications.add("What weight was used for ${ex.exerciseName}?")
      }
      if (missingReps) {
        clarifications.add("How many reps were performed for ${ex.exerciseName}?")
      }
    }

    if (parsedExercises.isEmpty()) {
      clarifications.add("No specific lifts detected in this section. Please review exercises.")
    }

    return ParsedWorkoutRant(
      workoutTitle = detectedTitle,
      exercises = parsedExercises,
      notes = clean,
      workoutDateMillis = dateMillis,
      dateDisplay = dateDisplay,
      clarificationQuestions = clarifications
    )
  }

  private fun extractDateFromBlock(text: String, fallbackDayOffset: Int): Triple<Long, String, String?> {
    val cal = java.util.Calendar.getInstance()
    val now = cal.timeInMillis
    val oneDay = 86400000L

    val lower = text.lowercase(Locale.ROOT)

    if (lower.contains("yesterday")) {
      cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
      cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
      cal.set(java.util.Calendar.MINUTE, 0)
      return Triple(cal.timeInMillis, "Yesterday", null)
    }

    if (lower.contains("today")) {
      cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
      cal.set(java.util.Calendar.MINUTE, 0)
      return Triple(cal.timeInMillis, "Today", null)
    }

    val daysAgoMatch = Regex("""(\d+)\s+days?\s+ago""").find(lower)
    if (daysAgoMatch != null) {
      val days = daysAgoMatch.groupValues[1].toIntOrNull() ?: 1
      cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
      cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
      cal.set(java.util.Calendar.MINUTE, 0)
      val sdf = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
      return Triple(cal.timeInMillis, sdf.format(cal.time), null)
    }

    // Month + Day: e.g. "aug 20", "august 20", "sep 4th", "september 15, 2026"
    val monthRegex = Regex("""(?i)\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?(?:,?\s*(\d{4}))?\b""")
    val monthMatch = monthRegex.find(text)
    if (monthMatch != null) {
      val monthStr = monthMatch.groupValues[1].lowercase(Locale.ROOT)
      val day = monthMatch.groupValues[2].toIntOrNull() ?: 1
      val year = monthMatch.groupValues[3].toIntOrNull() ?: cal.get(java.util.Calendar.YEAR)

      val monthIndex = when {
        monthStr.startsWith("jan") -> 0
        monthStr.startsWith("feb") -> 1
        monthStr.startsWith("mar") -> 2
        monthStr.startsWith("apr") -> 3
        monthStr.startsWith("may") -> 4
        monthStr.startsWith("jun") -> 5
        monthStr.startsWith("jul") -> 6
        monthStr.startsWith("aug") -> 7
        monthStr.startsWith("sep") -> 8
        monthStr.startsWith("oct") -> 9
        monthStr.startsWith("nov") -> 10
        monthStr.startsWith("dec") -> 11
        else -> cal.get(java.util.Calendar.MONTH)
      }

      cal.set(java.util.Calendar.YEAR, year)
      cal.set(java.util.Calendar.MONTH, monthIndex)
      cal.set(java.util.Calendar.DAY_OF_MONTH, day)
      cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
      cal.set(java.util.Calendar.MINUTE, 0)

      val sdf = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
      return Triple(cal.timeInMillis, sdf.format(cal.time), null)
    }

    // Slash format: e.g. "8/20" or "8/20/2026"
    val slashRegex = Regex("""\b(\d{1,2})[/.-](\d{1,2})(?:[/.-](\d{2,4}))?\b""")
    val slashMatch = slashRegex.find(text)
    if (slashMatch != null) {
      val m = slashMatch.groupValues[1].toIntOrNull() ?: 1
      val d = slashMatch.groupValues[2].toIntOrNull() ?: 1
      val y = slashMatch.groupValues[3].toIntOrNull() ?: cal.get(java.util.Calendar.YEAR)
      val normalizedYear = if (y < 100) 2000 + y else y

      cal.set(java.util.Calendar.YEAR, normalizedYear)
      cal.set(java.util.Calendar.MONTH, (m - 1).coerceIn(0, 11))
      cal.set(java.util.Calendar.DAY_OF_MONTH, d.coerceIn(1, 31))
      cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
      cal.set(java.util.Calendar.MINUTE, 0)

      val sdf = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
      return Triple(cal.timeInMillis, sdf.format(cal.time), null)
    }

    // Default fallback
    if (fallbackDayOffset > 0) {
      cal.add(java.util.Calendar.DAY_OF_YEAR, -fallbackDayOffset)
      cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
      cal.set(java.util.Calendar.MINUTE, 0)
      val sdf = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
      return Triple(cal.timeInMillis, sdf.format(cal.time), "Date not specified; set to $fallbackDayOffset day(s) ago.")
    }

    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return Triple(now, "Today", null)
  }

  private fun parseSegment(segment: String): Pair<String?, List<ParsedSetLog>> {
    val lower = segment.lowercase(Locale.ROOT)

    // Find best matching exercise
    var matchedCanonical: String? = null
    var bestMatchLength = 0

    // Check full exercise library first
    for (ex in ExerciseLibrary.allExercises) {
      if (lower.contains(ex.name.lowercase(Locale.ROOT)) && ex.name.length > bestMatchLength) {
        matchedCanonical = ex.name
        bestMatchLength = ex.name.length
      }
    }

    // Check aliases
    if (matchedCanonical == null) {
      for ((alias, canonical) in exerciseAliases) {
        if (lower.contains(alias) && alias.length > bestMatchLength) {
          matchedCanonical = canonical
          bestMatchLength = alias.length
        }
      }
    }

    if (matchedCanonical == null) {
      return Pair(null, emptyList())
    }

    // Extract sets, weight and reps
    val sets = extractSetsFromText(segment)
    return Pair(matchedCanonical, sets)
  }

  private fun extractSetsFromText(text: String): List<ParsedSetLog> {
    val results = mutableListOf<ParsedSetLog>()
    val lower = text.lowercase(Locale.ROOT)

    // Biofeedback tags extraction
    val bioTags = mutableListOf<String>()
    if (lower.contains("grip") || lower.contains("fingers hurt") || lower.contains("forearm")) bioTags.add("grip_fatigue")
    if (lower.contains("form breakdown") || lower.contains("awkward") || lower.contains("sloppy")) bioTags.add("form_breakdown")
    if (lower.contains("asymmetr") || lower.contains("left weaker") || lower.contains("right weaker")) bioTags.add("asymmetry")
    if (lower.contains("burn") || lower.contains("pump") || lower.contains("tension")) bioTags.add("peak_burn")
    if (lower.contains("joint") || lower.contains("hurt") || lower.contains("pinch") || lower.contains("discomfort")) bioTags.add("joint_discomfort")
    if (lower.contains("gassed") || lower.contains("breath") || lower.contains("cardio")) bioTags.add("cardio_fatigue")
    val bioString = bioTags.joinToString(",")

    // Tempo detection
    var tempo = ""
    if (lower.contains("controlled negative") || lower.contains("slow eccentric")) tempo = "controlled negative"
    if (lower.contains("pause") || lower.contains("paused")) tempo = "paused rep"
    if (lower.contains("explosive")) tempo = "explosive concentric"

    // Failure point detection
    var failurePoint = ""
    val failMatch = Regex("""failed(?:\s+at)?\s+([0-9]+(?:\.[0-9]+)?\s*reps?)""", RegexOption.IGNORE_CASE).find(text)
    if (failMatch != null) {
      failurePoint = failMatch.value
    }

    // Drop set detection: e.g. "dropped to 15 lbs for 4 reps" or "drops to 15"
    var dropWeight = 0.0
    var dropReps = 0.0
    val dropMatch = Regex("""(?:drop(?:ped)?(?:\s+to)?)\s+([0-9]+(?:\.[0-9]+)?)\s*(?:kg|lbs)?(?:\s*(?:for)?\s*([0-9]+(?:\.[0-9]+)?)\s*reps?)?""", RegexOption.IGNORE_CASE).find(text)
    if (dropMatch != null) {
      dropWeight = dropMatch.groupValues[1].toDoubleOrNull() ?: 0.0
      dropReps = dropMatch.groupValues.getOrNull(2)?.toDoubleOrNull() ?: 4.0
    }

    // Pattern 1: 80kg 3x8 or 80 kg 3x8.5 or 80kg x 3x8 or 80lbs 3x8
    val patternSetsX = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+)\\s*(?:x|sets?\\s*(?:of)?)\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
    val matchSetsX = patternSetsX.find(text)
    if (matchSetsX != null) {
      val weight = matchSetsX.groupValues[1].toDoubleOrNull() ?: 50.0
      val numSets = matchSetsX.groupValues[2].toIntOrNull() ?: 3
      val reps = matchSetsX.groupValues[3].toDoubleOrNull() ?: 10.0
      repeat(numSets.coerceIn(1, 10)) {
        results.add(
          ParsedSetLog(
            weightKg = weight,
            reps = reps,
            biofeedbackTags = bioString,
            tempo = tempo,
            failurePoint = failurePoint,
            dropWeightKg = dropWeight,
            dropReps = dropReps
          )
        )
      }
      return results
    }

    // Pattern 2: 3x8.5 @ 80kg or 3 sets of 10 at 100kg
    val patternSetsAt = Regex("([0-9]+)\\s*(?:x|sets?\\s*(?:of)?)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:reps?)?\\s*(?:@|at)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?", RegexOption.IGNORE_CASE)
    val matchSetsAt = patternSetsAt.find(text)
    if (matchSetsAt != null) {
      val numSets = matchSetsAt.groupValues[1].toIntOrNull() ?: 3
      val reps = matchSetsAt.groupValues[2].toDoubleOrNull() ?: 10.0
      val weight = matchSetsAt.groupValues[3].toDoubleOrNull() ?: 50.0
      repeat(numSets.coerceIn(1, 10)) {
        results.add(
          ParsedSetLog(
            weightKg = weight,
            reps = reps,
            biofeedbackTags = bioString,
            tempo = tempo,
            failurePoint = failurePoint,
            dropWeightKg = dropWeight,
            dropReps = dropReps
          )
        )
      }
      return results
    }

    // Pattern 3: 30kg 10, 10, 8.5 reps or 30kg 10 10 8.5
    val patternList = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+(?:\\.[0-9]+)?(?:\\s*[,\\s]\\s*[0-9]+(?:\\.[0-9]+)?)+)", RegexOption.IGNORE_CASE)
    val matchList = patternList.find(text)
    if (matchList != null) {
      val weight = matchList.groupValues[1].toDoubleOrNull() ?: 40.0
      val repsPart = matchList.groupValues[2]
      val repsTokens = repsPart.split(Regex("[,\\s]+")).mapNotNull { it.toDoubleOrNull() }
      if (repsTokens.isNotEmpty()) {
        for (r in repsTokens) {
          if (r in 0.5..100.0) {
            results.add(
              ParsedSetLog(
                weightKg = weight,
                reps = r,
                biofeedbackTags = bioString,
                tempo = tempo,
                failurePoint = failurePoint,
                dropWeightKg = dropWeight,
                dropReps = dropReps
              )
            )
          }
        }
        if (results.isNotEmpty()) return results
      }
    }

    // Pattern 4: Simple single set: 80kg 8.5 reps or 80kg for 6.5
    val patternSingle = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:reps?)?", RegexOption.IGNORE_CASE)
    val matchSingle = patternSingle.find(text)
    if (matchSingle != null) {
      val weight = matchSingle.groupValues[1].toDoubleOrNull() ?: 50.0
      val reps = matchSingle.groupValues[2].toDoubleOrNull() ?: 8.0
      results.add(
        ParsedSetLog(
          weightKg = weight,
          reps = reps,
          biofeedbackTags = bioString,
          tempo = tempo,
          failurePoint = failurePoint,
          dropWeightKg = dropWeight,
          dropReps = dropReps
        )
      )
      return results
    }

    // Default fallback sets
    results.add(ParsedSetLog(weightKg = 50.0, reps = 10.0, biofeedbackTags = bioString, tempo = tempo))
    results.add(ParsedSetLog(weightKg = 50.0, reps = 10.0, biofeedbackTags = bioString, tempo = tempo))
    results.add(ParsedSetLog(weightKg = 50.0, reps = 8.0, biofeedbackTags = bioString, tempo = tempo))
    return results
  }
}
