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
    val clean = text.trim()
    if (clean.isBlank()) {
      return ParsedWorkoutRant(
        workoutTitle = "Quick Workout",
        exercises = emptyList(),
        notes = ""
      )
    }

    // Split text into clauses / lines / sentences
    val segments = clean.split(Regex("[\n;.]|(?<=[0-9])\\s*,\\s*(?=[a-zA-Z])|(?i)\\s+(then|next|after that|afterwards|finished with|also did)\\s+"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }

    val parsedExercises = mutableListOf<ParsedExerciseLog>()
    var detectedTitle = "Logged Workout"

    // Check title in first sentence
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

    // If still empty but user typed something, provide a default entry so user isn't stuck
    if (parsedExercises.isEmpty()) {
      parsedExercises.add(
        ParsedExerciseLog(
          exerciseName = "Barbell Bench Press",
          sets = listOf(
            ParsedSetLog(weightKg = 60.0, reps = 10),
            ParsedSetLog(weightKg = 60.0, reps = 10),
            ParsedSetLog(weightKg = 60.0, reps = 8)
          )
        )
      )
    }

    return ParsedWorkoutRant(
      workoutTitle = detectedTitle,
      exercises = parsedExercises,
      notes = clean
    )
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

    // Pattern 1: 80kg 3x8 or 80 kg 3x8 or 80kg x 3x8 or 80lbs 3x8
    val patternSetsX = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+)\\s*(?:x|sets?\\s*(?:of)?)\\s*([0-9]+)", RegexOption.IGNORE_CASE)
    val matchSetsX = patternSetsX.find(text)
    if (matchSetsX != null) {
      val weight = matchSetsX.groupValues[1].toDoubleOrNull() ?: 50.0
      val numSets = matchSetsX.groupValues[2].toIntOrNull() ?: 3
      val reps = matchSetsX.groupValues[3].toIntOrNull() ?: 10
      repeat(numSets.coerceIn(1, 10)) {
        results.add(ParsedSetLog(weightKg = weight, reps = reps))
      }
      return results
    }

    // Pattern 2: 3x8 @ 80kg or 3 sets of 10 at 100kg
    val patternSetsAt = Regex("([0-9]+)\\s*(?:x|sets?\\s*(?:of)?)\\s*([0-9]+)\\s*(?:reps?)?\\s*(?:@|at)\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?", RegexOption.IGNORE_CASE)
    val matchSetsAt = patternSetsAt.find(text)
    if (matchSetsAt != null) {
      val numSets = matchSetsAt.groupValues[1].toIntOrNull() ?: 3
      val reps = matchSetsAt.groupValues[2].toIntOrNull() ?: 10
      val weight = matchSetsAt.groupValues[3].toDoubleOrNull() ?: 50.0
      repeat(numSets.coerceIn(1, 10)) {
        results.add(ParsedSetLog(weightKg = weight, reps = reps))
      }
      return results
    }

    // Pattern 3: 30kg 10, 10, 8 reps or 30kg 10 10 8
    val patternList = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+(?:\\s*[,\\s]\\s*[0-9]+)+)", RegexOption.IGNORE_CASE)
    val matchList = patternList.find(text)
    if (matchList != null) {
      val weight = matchList.groupValues[1].toDoubleOrNull() ?: 40.0
      val repsPart = matchList.groupValues[2]
      val repsTokens = repsPart.split(Regex("[,\\s]+")).mapNotNull { it.toIntOrNull() }
      if (repsTokens.isNotEmpty()) {
        for (r in repsTokens) {
          if (r in 1..100) {
            results.add(ParsedSetLog(weightKg = weight, reps = r))
          }
        }
        if (results.isNotEmpty()) return results
      }
    }

    // Pattern 4: Simple single set: 80kg 8 reps or 80kg for 8
    val patternSingle = Regex("([0-9]+(?:\\.[0-9]+)?)\\s*(?:kg|lbs)?\\s*(?:for)?\\s*([0-9]+)\\s*(?:reps?)?", RegexOption.IGNORE_CASE)
    val matchSingle = patternSingle.find(text)
    if (matchSingle != null) {
      val weight = matchSingle.groupValues[1].toDoubleOrNull() ?: 50.0
      val reps = matchSingle.groupValues[2].toIntOrNull() ?: 8
      results.add(ParsedSetLog(weightKg = weight, reps = reps))
      return results
    }

    // Default fallback sets
    results.add(ParsedSetLog(weightKg = 50.0, reps = 10))
    results.add(ParsedSetLog(weightKg = 50.0, reps = 10))
    results.add(ParsedSetLog(weightKg = 50.0, reps = 8))
    return results
  }
}
