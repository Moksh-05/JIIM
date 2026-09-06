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
    "flat bench chest press" to "Barbell Bench Press",
    "barbell bench" to "Barbell Bench Press",
    "chest press" to "Machine Chest Press",
    "chest press machine" to "Machine Chest Press",
    "machine chest press" to "Machine Chest Press",
    "incline db" to "Incline Dumbbell Press",
    "incline db press" to "Incline Dumbbell Press",
    "incline db / bench press" to "Incline Dumbbell Press",
    "incline dumbbell" to "Incline Dumbbell Press",
    "incline dumbbell press" to "Incline Dumbbell Press",
    "incline bench" to "Incline Barbell Bench Press",
    "incline bench press" to "Incline Barbell Bench Press",
    "cable fly" to "Cable Chest Fly",
    "chest fly" to "Cable Chest Fly",
    "db butterfly" to "Dumbbell Flyes",
    "butterfly" to "Dumbbell Flyes",
    "db flys" to "Dumbbell Flyes",
    "db flyes" to "Dumbbell Flyes",
    "dumbbell flyes" to "Dumbbell Flyes",
    "dips" to "Dips",
    "pushups" to "Push Ups",
    "push ups" to "Push Ups",
    "push-ups" to "Push Ups",
    "pike pushups" to "Pike Push-Ups",
    "pike push-ups" to "Pike Push-Ups",

    // Back & Traps
    "deadlift" to "Barbell Deadlift",
    "deadlifts" to "Barbell Deadlift",
    "rdl" to "Romanian Deadlift",
    "rdls" to "Romanian Deadlift",
    "romanian deadlift" to "Romanian Deadlift",
    "romanian deadlifts" to "Romanian Deadlift",
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
    "back extensions" to "Back Extensions",
    "back extension" to "Back Extensions",
    "shrugs" to "Dumbbell Shrug",
    "shrug" to "Dumbbell Shrug",
    "dumbbell shrugs" to "Dumbbell Shrug",
    "db shrugs" to "Dumbbell Shrug",

    // Shoulders
    "machine shoulder press" to "Machine Shoulder Press",
    "shoulder press" to "Dumbbell Shoulder Press",
    "db shoulder press" to "Dumbbell Shoulder Press",
    "left hand db shoulder press" to "Dumbbell Shoulder Press",
    "ohp" to "Overhead Barbell Press",
    "overhead press" to "Overhead Barbell Press",
    "military press" to "Overhead Barbell Press",
    "lateral raise" to "Dumbbell Lateral Raise",
    "lateral raises" to "Dumbbell Lateral Raise",
    "side raises" to "Dumbbell Lateral Raise",
    "side raise" to "Dumbbell Lateral Raise",
    "db lateral raises" to "Dumbbell Lateral Raise",
    "db lateral raise" to "Dumbbell Lateral Raise",
    "lean-in lateral raises" to "Dumbbell Lateral Raise",
    "lean in lateral raises" to "Dumbbell Lateral Raise",
    "bilateral lateral raises" to "Dumbbell Lateral Raise",
    "single hand lateral raises" to "Cable Lateral Raise",
    "left hand cable lateral raises" to "Cable Lateral Raise",
    "cable lateral raise" to "Cable Lateral Raise",
    "cable lateral raises" to "Cable Lateral Raise",
    "front raise" to "Front Dumbbell Raise",
    "front raises" to "Front Dumbbell Raise",
    "reverse db flys" to "Reverse Dumbbell Flyes",
    "reverse db flyes" to "Reverse Dumbbell Flyes",
    "reverse flys" to "Reverse Dumbbell Flyes",
    "reverse flyes" to "Reverse Dumbbell Flyes",
    "rear delt raises" to "Reverse Dumbbell Flyes",
    "rear delt flys" to "Reverse Dumbbell Flyes",
    "face pulls" to "Face Pulls",
    "facepulls" to "Face Pulls",

    // Arms - Biceps
    "bicep curl" to "Barbell Bicep Curl",
    "bicep curls" to "Barbell Bicep Curl",
    "curls" to "Barbell Bicep Curl",
    "barbell curl" to "Barbell Bicep Curl",
    "db bicep curl" to "Dumbbell Bicep Curl",
    "db bicep curls" to "Dumbbell Bicep Curl",
    "regular bicep curls" to "Dumbbell Bicep Curl",
    "regular bicep curl" to "Dumbbell Bicep Curl",
    "seated unilateral bicep curls" to "Seated Dumbbell Curl",
    "seated bicep curl" to "Seated Dumbbell Curl",
    "incline curl" to "Incline Dumbbell Curl",
    "incline curls" to "Incline Dumbbell Curl",
    "incline db curls" to "Incline Dumbbell Curl",
    "incline db curl" to "Incline Dumbbell Curl",
    "db bench curl" to "Incline Dumbbell Curl",
    "hammer curl" to "Hammer Curl",
    "hammer curls" to "Hammer Curl",
    "db hammer curl" to "Hammer Curl",
    "db hammer curls" to "Hammer Curl",
    "cable hammer curls" to "Cable Hammer Curl",
    "cable hammer curl" to "Cable Hammer Curl",
    "zottman curl" to "Zottman Curl",
    "zottman curls" to "Zottman Curl",
    "preacher curl machine" to "Machine Preacher Curl",
    "preacher curl" to "Machine Preacher Curl",

    // Arms - Triceps
    "tricep pushdown" to "Tricep Rope Pushdown",
    "tricep pushdowns" to "Tricep Rope Pushdown",
    "pushdown" to "Tricep Rope Pushdown",
    "pushdowns" to "Tricep Rope Pushdown",
    "rope pushdown" to "Tricep Rope Pushdown",
    "one hand cable pushdown" to "Single Arm Cable Tricep Pushdown",
    "cable pushdown" to "Tricep Rope Pushdown",
    "db tricep extensions" to "Dumbbell Tricep Extension",
    "db tricep extension" to "Dumbbell Tricep Extension",
    "overhead tricep extension" to "Cable Overhead Tricep Extension",
    "tricep extension machine" to "Machine Tricep Extension",
    "tricep extension" to "Machine Tricep Extension",
    "tricep extensions" to "Machine Tricep Extension",
    "skull crushers" to "Skull Crushers",
    "skullcrushers" to "Skull Crushers",

    // Legs
    "squat" to "Barbell Back Squat",
    "squats" to "Barbell Back Squat",
    "back squat" to "Barbell Back Squat",
    "barbell squat" to "Barbell Back Squat",
    "jump squats" to "Bodyweight Squats",
    "leg press" to "Leg Press",
    "leg extension" to "Leg Extension",
    "leg extensions" to "Leg Extension",
    "leg curl" to "Hamstring Leg Curl",
    "hamstring curl" to "Hamstring Leg Curl",
    "calves" to "Standing Calf Raise",
    "calf raise" to "Standing Calf Raise",
    "calf raises" to "Standing Calf Raise",

    // Core
    "hanging leg raise" to "Hanging Leg Raise",
    "leg raises" to "Hanging Leg Raise",
    "plank" to "Plank",
    "woodchoppers" to "Cable Woodchoppers",
    "upper ab crunches" to "Ab Crunches",
    "weighted upper ab crunches" to "Ab Crunches",
    "reverse ab crunches" to "Reverse Crunches",
    "bench reverse crunches" to "Reverse Crunches",
    "crunches" to "Ab Crunches",
    "ab crunches" to "Ab Crunches"
  )

  fun resolveCanonicalExerciseName(rawName: String): String {
    val trimmed = rawName.trim().removePrefix("-").removePrefix("*").removePrefix("•").trim()
    if (trimmed.isBlank()) return "General Exercise"
    val directMatch = ExerciseLibrary.allExercises.find { it.name.equals(trimmed, ignoreCase = true) }
    if (directMatch != null) return directMatch.name

    val lower = trimmed.lowercase(Locale.ROOT)
    val aliasMatch = exerciseAliases[lower]
    if (aliasMatch != null) return aliasMatch

    for ((alias, canonical) in exerciseAliases.entries.sortedByDescending { it.key.length }) {
      if (lower.contains(alias)) {
        return canonical
      }
    }

    return trimmed.split(" ").joinToString(" ") { word ->
      word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }
  }

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

    // Filter out trailing chat prompt questions or markdown dividers
    return blocks.filter { rawBlock ->
      val b = rawBlock.trim()
      b.isNotBlank() &&
        !b.startsWith("---") &&
        !b.startsWith("Are there any specific", ignoreCase = true) &&
        !b.startsWith("Let me know", ignoreCase = true) &&
        !b.startsWith("Would you like", ignoreCase = true)
    }
  }

  private fun parseWorkoutBlock(block: String, fallbackDayOffset: Int): ParsedWorkoutRant {
    val clean = block.trim()
    val lines = clean.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val (dateMillis, dateDisplay, dateClarification) = extractDateFromBlock(clean, fallbackDayOffset)

    // Detect title from first line or exercises
    var detectedTitle = ""
    val firstLine = lines.firstOrNull() ?: ""
    val cleanFirst = firstLine.trim().trimStart('#', '*', '-', '•', '>', ' ')
    val lowerFull = clean.lowercase(Locale.ROOT)

    // 1. If first line has a colon after the date (e.g. "July 28, 2026: Shoulders and Biceps")
    val colonIdx = cleanFirst.indexOf(':')
    if (colonIdx != -1) {
      val afterColon = cleanFirst.substring(colonIdx + 1).trim()
      if (afterColon.isNotBlank() && afterColon.length in 2..60 &&
        !afterColon.contains("yesterday", ignoreCase = true) &&
        !afterColon.contains("today", ignoreCase = true)) {
        detectedTitle = afterColon
      }
    }

    // 2. If first line is a title without sets/numbers, use the lifter's title directly
    if (detectedTitle.isBlank()) {
      val hasSetNumbersInFirstLine = Regex("""\b\d+(?:kg|lb|lbs)?\b|\b\d+\s*[x×]\s*\d+\b""").containsMatchIn(cleanFirst)
      if (!hasSetNumbersInFirstLine && cleanFirst.length in 3..60 &&
        !cleanFirst.contains("yesterday", ignoreCase = true) &&
        !cleanFirst.contains("today", ignoreCase = true)) {
        val candidateTitle = cleanFirst
          .removePrefix("Workout:")
          .removePrefix("Session:")
          .removePrefix("Day:")
          .trim()
        if (candidateTitle.isNotBlank()) {
          detectedTitle = candidateTitle
        }
      }
    }

    if (detectedTitle.isBlank()) {
      when {
        Regex("""\bpush\s+day\b""").containsMatchIn(lowerFull) -> detectedTitle = "Push Day"
        Regex("""\bpull\s+day\b""").containsMatchIn(lowerFull) -> detectedTitle = "Pull Day"
        Regex("""\bleg\s+day\b""").containsMatchIn(lowerFull) -> detectedTitle = "Leg Day"
        Regex("""\bupper\s+body\b""").containsMatchIn(lowerFull) -> detectedTitle = "Upper Body"
        Regex("""\blower\s+body\b""").containsMatchIn(lowerFull) -> detectedTitle = "Lower Body"
        lowerFull.contains("chest") && lowerFull.contains("tricep") -> detectedTitle = "Chest & Triceps"
        lowerFull.contains("back") && lowerFull.contains("bicep") -> detectedTitle = "Back & Biceps"
        lowerFull.contains("bicep") && lowerFull.contains("tricep") -> detectedTitle = "Arm Day"
        lowerFull.contains("abs") && lowerFull.contains("shoulder") -> detectedTitle = "Abs & Shoulders"
        lowerFull.contains("shoulder") && lowerFull.contains("bicep") -> detectedTitle = "Shoulders & Biceps"
        Regex("""\barm\s+day\b""").containsMatchIn(lowerFull) -> detectedTitle = "Arms Workout"
        Regex("""\bback\s+day\b""").containsMatchIn(lowerFull) -> detectedTitle = "Back Workout"
        Regex("""\bshoulder\s+day\b""").containsMatchIn(lowerFull) -> detectedTitle = "Shoulders Workout"
      }
    }

    val isLbs = clean.contains("lb", ignoreCase = true) || clean.contains("pound", ignoreCase = true) || hasTypicalPoundNumbers(clean)

    // Split text into clauses / lines / sentences for exercise parsing
    val segments = clean.split(Regex("[\n;.]|(?<=[0-9])\\s*,\\s*(?=[a-zA-Z])|(?i)\\s+(then|next|after that|afterwards|finished with|also did)\\s+"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }

    val parsedExercises = mutableListOf<ParsedExerciseLog>()
    val clarifications = mutableListOf<String>()

    if (dateClarification != null) {
      clarifications.add(dateClarification)
    }

    // Step 1: Parse segments that have explicit weights/sets/reps
    for (segment in segments) {
      val (matchedName, sets) = parseSegment(segment, isLbs)
      if (matchedName != null && sets.isNotEmpty()) {
        parsedExercises.add(
          ParsedExerciseLog(
            exerciseName = matchedName,
            sets = sets
          )
        )
      }
    }

    // Step 2: Comprehensive scan for all exercises mentioned in narrative text or bullet points
    // (e.g., "You executed Zottman Curls, Lean-in Lateral Raises, Incline Curls, and Reverse DB Flys")
    val mentionedExercisesInOrder = mutableListOf<Pair<Int, String>>()
    val sortedAliases = exerciseAliases.entries.sortedByDescending { it.key.length }

    for (ex in ExerciseLibrary.allExercises) {
      val pattern = Regex("""\b${Regex.escape(ex.name.lowercase(Locale.ROOT))}\b""")
      val matches = pattern.findAll(lowerFull)
      for (m in matches) {
        mentionedExercisesInOrder.add(Pair(m.range.first, ex.name))
      }
    }

    for ((alias, canonical) in sortedAliases) {
      val pattern = Regex("""\b${Regex.escape(alias)}\b""")
      val matches = pattern.findAll(lowerFull)
      for (m in matches) {
        mentionedExercisesInOrder.add(Pair(m.range.first, canonical))
      }
    }

    // Sort by order of appearance in the notes and deduplicate
    val distinctMentions = mutableListOf<String>()
    for ((_, canonical) in mentionedExercisesInOrder.sortedBy { it.first }) {
      if (!distinctMentions.contains(canonical)) {
        distinctMentions.add(canonical)
      }
    }

    // For any mentioned exercise that doesn't have sets yet, create smart default working sets
    for (canonical in distinctMentions) {
      if (parsedExercises.none { it.exerciseName == canonical }) {
        val defaultSets = createSmartDefaultSets(canonical, isLbs)
        parsedExercises.add(
          ParsedExerciseLog(
            exerciseName = canonical,
            sets = defaultSets
          )
        )
      }
    }

    // If title was not explicitly in the notes, automatically deduce it from the exercises!
    if (detectedTitle.isBlank() || detectedTitle == "Logged Workout") {
      detectedTitle = suggestWorkoutTitle(parsedExercises)
    }

    // Ensure all sets have valid reps (never 0)
    val sanitizedExercises = parsedExercises.map { ex ->
      val fixedSets = ex.sets.map { s ->
        if (s.reps <= 0.0) s.copy(reps = 10.0) else s
      }
      ex.copy(sets = fixedSets)
    }

    if (sanitizedExercises.isEmpty()) {
      clarifications.add("No specific lifts detected in this section. Please review exercises.")
    }

    return ParsedWorkoutRant(
      workoutTitle = detectedTitle,
      exercises = sanitizedExercises,
      notes = clean,
      workoutDateMillis = dateMillis,
      dateDisplay = dateDisplay,
      clarificationQuestions = clarifications,
      detectedUnit = if (isLbs) "LBS" else "KG"
    )
  }

  fun createSmartDefaultSets(exerciseName: String, isLbs: Boolean): List<ParsedSetLog> {
    val lower = exerciseName.lowercase(Locale.ROOT)
    val isBodyweight = isBodyweightExercise(exerciseName)
    val isCore = lower.contains("crunch") || lower.contains("plank") || lower.contains("leg raise") || lower.contains("woodchopper")
    val isHeavyCompound = (lower.contains("squat") && !lower.contains("bodyweight")) ||
      lower.contains("deadlift") || lower.contains("bench press") || lower.contains("barbell row") ||
      lower.contains("overhead barbell") || lower.contains("leg press")

    val defaultWeightKg = when {
      isBodyweight || isCore -> 0.0
      isHeavyCompound -> if (isLbs) (135.0 / 2.20462) else 60.0
      lower.contains("curl") || lower.contains("lateral raise") || lower.contains("fly") -> if (isLbs) (25.0 / 2.20462) else 12.0
      lower.contains("shoulder press") || lower.contains("incline") || lower.contains("chest press") -> if (isLbs) (45.0 / 2.20462) else 20.0
      lower.contains("pulldown") || lower.contains("cable row") || lower.contains("pushdown") || lower.contains("extension") -> if (isLbs) (50.0 / 2.20462) else 25.0
      else -> if (isLbs) (35.0 / 2.20462) else 15.0
    }

    val defaultReps = when {
      isCore || lower.contains("push up") || lower.contains("push-up") -> 15.0
      isHeavyCompound -> 8.0
      else -> 10.0
    }

    return listOf(
      ParsedSetLog(weightKg = defaultWeightKg, reps = defaultReps),
      ParsedSetLog(weightKg = defaultWeightKg, reps = defaultReps),
      ParsedSetLog(weightKg = defaultWeightKg, reps = defaultReps)
    )
  }

  fun suggestWorkoutTitle(exercises: List<ParsedExerciseLog>): String {
    if (exercises.isEmpty()) return "Gym Workout"

    var chestCount = 0
    var backCount = 0
    var legsCount = 0
    var shouldersCount = 0
    var armsCount = 0
    var coreCount = 0

    for (ex in exercises) {
      val nameLower = ex.exerciseName.lowercase(Locale.ROOT)
      when {
        nameLower.contains("bench") || nameLower.contains("chest") || nameLower.contains("fly") || nameLower.contains("push up") || nameLower.contains("dip") -> chestCount++
        nameLower.contains("row") || nameLower.contains("pull") || nameLower.contains("deadlift") || nameLower.contains("lat") -> backCount++
        nameLower.contains("squat") || nameLower.contains("leg") || nameLower.contains("calf") || nameLower.contains("calves") || nameLower.contains("rdl") || nameLower.contains("hamstring") -> legsCount++
        nameLower.contains("shoulder") || nameLower.contains("overhead") || nameLower.contains("military") || nameLower.contains("lateral") || nameLower.contains("delt") || nameLower.contains("face pull") -> shouldersCount++
        nameLower.contains("curl") || nameLower.contains("tricep") || nameLower.contains("pushdown") || nameLower.contains("skull") || nameLower.contains("arm") -> armsCount++
        nameLower.contains("plank") || nameLower.contains("ab") || nameLower.contains("crunch") -> coreCount++
      }
    }

    val total = exercises.size
    val pushCount = chestCount + shouldersCount
    val pullCount = backCount + (if (armsCount > 0) 1 else 0)

    return when {
      legsCount >= 2 && legsCount >= (total / 2) -> "Leg Day"
      chestCount >= 1 && (shouldersCount >= 1 || armsCount >= 1) && backCount == 0 -> "Push Day"
      backCount >= 1 && armsCount >= 1 && chestCount == 0 && legsCount == 0 -> "Pull Day"
      chestCount >= 2 && backCount == 0 && legsCount == 0 -> "Chest Day"
      backCount >= 2 && chestCount == 0 && legsCount == 0 -> "Back Day"
      legsCount >= 1 && chestCount == 0 && backCount == 0 -> "Leg Day"
      chestCount >= 1 && backCount >= 1 && legsCount == 0 -> "Upper Body"
      legsCount >= 1 && coreCount >= 1 && chestCount == 0 && backCount == 0 -> "Lower Body"
      shouldersCount >= 1 && armsCount >= 1 && chestCount == 0 && backCount == 0 && legsCount == 0 -> "Shoulders & Arms"
      shouldersCount >= 2 && chestCount == 0 && backCount == 0 -> "Shoulder Day"
      armsCount >= 2 && chestCount == 0 && backCount == 0 -> "Arm Day"
      pushCount >= 2 && pullCount == 0 -> "Push Day"
      pullCount >= 2 && pushCount == 0 -> "Pull Day"
      chestCount >= 1 && legsCount >= 1 && backCount >= 1 -> "Full Body"
      else -> {
        val top = listOf(
          "Chest & Triceps" to chestCount,
          "Back & Biceps" to backCount,
          "Leg Day" to legsCount,
          "Shoulders" to shouldersCount,
          "Arms" to armsCount
        ).maxByOrNull { it.second }
        if (top != null && top.second > 0) top.first else "Gym Workout"
      }
    }
  }

  private fun isBodyweightExercise(name: String): Boolean {
    val lower = name.lowercase(Locale.ROOT)
    return lower.contains("pull-up") || lower.contains("pull up") ||
      lower.contains("chin up") || lower.contains("chin-up") ||
      lower.contains("dip") || lower.contains("push up") ||
      lower.contains("push-up") || lower.contains("hanging leg raise") ||
      lower.contains("plank") || lower.contains("bodyweight")
  }

  private fun hasTypicalPoundNumbers(text: String): Boolean {
    val lower = text.lowercase(Locale.ROOT)
    if (lower.contains("lb") || lower.contains("pound")) return true
    if (Regex("""\b(35|40|45|50|55|60|65|70|75|80|85|90|95|100)s\b""").containsMatchIn(lower)) return true
    if (Regex("""\b(135|155|185|205|225|245|275|315|365|405)\b""").containsMatchIn(lower)) return true
    return false
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

  private fun parseSegment(segment: String, isLbs: Boolean): Pair<String?, List<ParsedSetLog>> {
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
    val isBodyweight = isBodyweightExercise(matchedCanonical)
    val sets = extractSetsFromText(segment, isLbs = isLbs, isBodyweight = isBodyweight)
    return Pair(matchedCanonical, sets)
  }

  private fun extractSetsFromText(text: String, isLbs: Boolean, isBodyweight: Boolean): List<ParsedSetLog> {
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
      val rawDrop = dropMatch.groupValues[1].toDoubleOrNull() ?: 0.0
      dropWeight = if (isLbs) (rawDrop / 2.20462) else rawDrop
      dropReps = dropMatch.groupValues.getOrNull(2)?.toDoubleOrNull() ?: 4.0
    }

    fun toKg(raw: Double): Double = if (isLbs) (raw / 2.20462) else raw

    // Pattern 1: 80kg 3x8 or 80 kg 3x8.5 or 80kg x 3x8 or 80lbs 3x8
    val patternSetsX = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+)\\s*(?:x|sets?\\s*(?:of)?)\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE)
    val matchSetsX = patternSetsX.find(text)
    if (matchSetsX != null) {
      val rawWeight = matchSetsX.groupValues[1].toDoubleOrNull() ?: 50.0
      val numSets = matchSetsX.groupValues[2].toIntOrNull() ?: 3
      val reps = matchSetsX.groupValues[3].toDoubleOrNull() ?: 10.0
      val weight = toKg(rawWeight)
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
      val rawWeight = matchSetsAt.groupValues[3].toDoubleOrNull() ?: 50.0
      val weight = toKg(rawWeight)
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
      val rawWeight = matchList.groupValues[1].toDoubleOrNull() ?: 40.0
      val repsPart = matchList.groupValues[2]
      val repsTokens = repsPart.split(Regex("[,\\s]+")).mapNotNull { it.toDoubleOrNull() }
      if (repsTokens.isNotEmpty()) {
        val weight = toKg(rawWeight)
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
      val rawWeight = matchSingle.groupValues[1].toDoubleOrNull() ?: 50.0
      val reps = matchSingle.groupValues[2].toDoubleOrNull() ?: 10.0
      val weight = toKg(rawWeight)
      repeat(3) {
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

    // Pattern 5: Weight only with 's' or 'lbs' (e.g. "60s" or "60 lbs" or "with 60")
    val patternWeightOnly = Regex("""(?:with\s+|at\s+)?([0-9]+(?:\.[0-9]+)?)\s*(?:s|lbs?|kg)?""", RegexOption.IGNORE_CASE)
    val matchWeightOnly = patternWeightOnly.find(text)
    if (matchWeightOnly != null && !isBodyweight) {
      val rawWeight = matchWeightOnly.groupValues[1].toDoubleOrNull()
      if (rawWeight != null && rawWeight > 0.0) {
        val weight = toKg(rawWeight)
        repeat(3) {
          results.add(
            ParsedSetLog(
              weightKg = weight,
              reps = 10.0,
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
    }

    // Bodyweight default (dips, pullups, pushups)
    if (isBodyweight) {
      repeat(3) {
        results.add(ParsedSetLog(weightKg = 0.0, reps = 10.0, biofeedbackTags = bioString, tempo = tempo))
      }
      return results
    }

    // Default fallback sets: sensible working weight
    val defaultWeight = if (isLbs) (50.0 / 2.20462) else 25.0
    results.add(ParsedSetLog(weightKg = defaultWeight, reps = 10.0, biofeedbackTags = bioString, tempo = tempo))
    results.add(ParsedSetLog(weightKg = defaultWeight, reps = 10.0, biofeedbackTags = bioString, tempo = tempo))
    results.add(ParsedSetLog(weightKg = defaultWeight, reps = 8.0, biofeedbackTags = bioString, tempo = tempo))
    return results
  }
}
