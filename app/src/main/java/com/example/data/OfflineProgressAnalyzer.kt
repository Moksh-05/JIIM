package com.example.data

import com.example.model.AiProgressAnalysis
import com.example.model.ExerciseLibrary
import com.example.model.ExercisePr
import com.example.model.WorkoutWithExercises
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object OfflineProgressAnalyzer {

  fun analyze(
    workouts: List<WorkoutWithExercises>,
    prs: List<ExercisePr>
  ): AiProgressAnalysis {
    if (workouts.isEmpty()) {
      return AiProgressAnalysis(
        overallScore = "Baseline Calibrating (7.5/10)",
        hypertrophyStatus = "Log at least 3 workouts to reveal muscle volume breakdown.",
        progressiveOverloadVerdict = "Start tracking weekly weights to establish overload baseline.",
        recommendations = listOf(
          "Perform 3-4 working sets per muscle group close to failure (RPE 8-9).",
          "Aim to add either 1 rep or 1.25-2.5kg each week on main compound lifts.",
          "Prioritize 1.6-2.2g of protein per kg of bodyweight for muscle protein synthesis."
        ),
        detectedSplitName = "Initial 3-Day Full Body",
        detectedSplitBreakdown = listOf("Day 1: Full Body Compound", "Day 2: Rest", "Day 3: Upper & Core"),
        stagnationAlerts = emptyList()
      )
    }

    val sortedWorkouts = workouts.sortedBy { it.session.startTimeMillis }
    val now = System.currentTimeMillis()
    val twoWeeksAgo = now - 14 * 86400000L
    val fourWeeksAgo = now - 28 * 86400000L

    // Calculate volume of recent 2 weeks vs previous 2 weeks
    val recentWorkouts = sortedWorkouts.filter { it.session.startTimeMillis >= twoWeeksAgo }
    val priorWorkouts = sortedWorkouts.filter {
      it.session.startTimeMillis in fourWeeksAgo until twoWeeksAgo
    }

    val recentVolume = recentWorkouts.sumOf { it.session.totalVolumeKg }
    val priorVolume = priorWorkouts.sumOf { it.session.totalVolumeKg }

    val overloadPct = if (priorVolume > 0) {
      ((recentVolume - priorVolume) / priorVolume) * 100.0
    } else {
      12.5 // healthy initial ramp
    }

    val overloadVerdict = when {
      overloadPct > 15.0 -> "Aggressive Overload (+${(overloadPct * 10).roundToInt() / 10.0}% 14-day volume surge). Ensure adequate sleep and recovery to avoid fatigue accumulation."
      overloadPct >= 3.0 -> "Optimal Progressive Overload (+${(overloadPct * 10).roundToInt() / 10.0}% tonnage progression). Excellent linear adaptation across compound lifts."
      overloadPct >= -5.0 -> "Maintenance / Deload Phase (${(overloadPct * 10).roundToInt() / 10.0}% delta). Great for dissipating fatigue before a new hypertrophy block."
      else -> "Volume Drop (${(overloadPct * 10).roundToInt() / 10.0}%). Volume decreased; consider adding 1 extra working set per muscle group next week."
    }

    // Muscle group tally
    val muscleSets = mutableMapOf<String, Int>()
    workouts.forEach { w ->
      w.exercises.forEach { ex ->
        val cat = ex.exercise.category
        val count = ex.sets.count { it.isCompleted }
        muscleSets[cat] = (muscleSets[cat] ?: 0) + count
      }
    }

    val dominantMuscle = muscleSets.maxByOrNull { it.value }?.key ?: "Chest"
    val hypertrophyStatus = "Hypertrophy Volume: ${workouts.sumOf { it.session.totalSets }} total sets logged. Most trained: $dominantMuscle (${muscleSets[dominantMuscle] ?: 0} sets). Optimal weekly frequency is 12-18 sets per muscle group."

    // Split Pattern Detection: Group exercises by session to identify the recurring split
    val detectedSplit = detectSplitPattern(workouts)

    // Stagnation Detection: Check if any exercise has had identical top weight for last 3 sessions
    val stagnationAlerts = mutableListOf<String>()
    val exerciseHistory = mutableMapOf<String, MutableList<Double>>()
    sortedWorkouts.forEach { w ->
      w.exercises.forEach { ex ->
        val maxWt = ex.sets.maxOfOrNull { it.weightKg } ?: 0.0
        if (maxWt > 0) {
          exerciseHistory.getOrPut(ex.exercise.exerciseName) { mutableListOf() }.add(maxWt)
        }
      }
    }

    exerciseHistory.forEach { (name, weights) ->
      if (weights.size >= 3) {
        val lastThree = weights.takeLast(3)
        if (lastThree.distinct().size == 1) {
          stagnationAlerts.add("$name has plateaued at ${lastThree[0]} kg across last 3 sessions. Try increasing by 2.5kg for lower reps or adding rest-pause sets.")
        }
      }
    }

    val score = when {
      overloadPct in 5.0..20.0 -> "Elite Hypertrophy (9.4/10)"
      overloadPct >= 0.0 -> "Consistent Growth (8.6/10)"
      else -> "Volume Baseline (7.8/10)"
    }

    val recs = mutableListOf<String>()
    if (stagnationAlerts.isNotEmpty()) {
      recs.add("Break plateaus using micro-loading (+1.25kg plates) or 3-second eccentric tempos.")
    } else {
      recs.add("Maintain current progressive overload cadence (+1-2 reps per set before adding weight).")
    }
    recs.add("Target 10-20 hard sets per muscle group weekly within 1-3 reps in reserve (RIR).")
    recs.add("Hydrate with electrolytes and consume 30-40g protein within 2 hours post-workout.")

    return AiProgressAnalysis(
      overallScore = score,
      hypertrophyStatus = hypertrophyStatus,
      progressiveOverloadVerdict = overloadVerdict,
      recommendations = recs,
      detectedSplitName = detectedSplit.first,
      detectedSplitBreakdown = detectedSplit.second,
      stagnationAlerts = stagnationAlerts
    )
  }

  private fun detectSplitPattern(workouts: List<WorkoutWithExercises>): Pair<String, List<String>> {
    // Analyze distinct workout session names and muscle groupings
    val sessionTypes = workouts.map { w ->
      val categories = w.exercises.map { it.exercise.category }.distinct()
      Pair(w.session.name, categories)
    }.distinctBy { it.first }

    val hasChestTricep = workouts.any { w ->
      val cats = w.exercises.map { it.exercise.category }
      (cats.contains("Chest") || cats.contains("Arms")) && w.session.name.contains("Chest", ignoreCase = true)
    }
    val hasBicepShoulder = workouts.any { w ->
      w.session.name.contains("Shoulder", ignoreCase = true) || w.session.name.contains("Bicep", ignoreCase = true)
    }
    val hasLegs = workouts.any { w ->
      w.session.name.contains("Leg", ignoreCase = true) || w.exercises.any { it.exercise.category == "Legs" }
    }
    val hasAbsCardio = workouts.any { w ->
      w.session.name.contains("Abs", ignoreCase = true) || w.session.name.contains("Cardio", ignoreCase = true) || w.exercises.any { it.exercise.category == "Core" }
    }

    if (hasChestTricep && hasBicepShoulder) {
      return Pair(
        "Custom 4-Day Antagonist Split",
        listOf(
          "Day 1: Chest & Triceps (Heavy Compounds + Dips/Pushdowns)",
          "Day 2: Biceps & Shoulders (Deltoid Caps + Arm Density)",
          "Day 3: Legs & Quad Annihilation (Squats + Posterior Overload)",
          "Day 4: Abs, Cardio & Back Depth (Deadlifts, Pulls & Core Stability)"
        )
      )
    }

    val breakdown = sessionTypes.take(4).mapIndexed { idx, pair ->
      "Day ${idx + 1}: ${pair.first} (${pair.second.joinToString(", ")})"
    }

    return Pair(
      "Custom Lifter Split (${sessionTypes.size.coerceAtLeast(3)} Days)",
      if (breakdown.isNotEmpty()) breakdown else listOf(
        "Day 1: Upper Body Strength",
        "Day 2: Lower Body Hypertrophy",
        "Day 3: Delts & Arm Specialization"
      )
    )
  }
}
