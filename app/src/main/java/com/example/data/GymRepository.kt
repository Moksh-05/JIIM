package com.example.data

import com.example.model.BodyWeightLog
import com.example.model.ExerciseLibrary
import com.example.model.ExerciseLog
import com.example.model.ExercisePr
import com.example.model.ParsedWorkoutRant
import com.example.model.RoutineTemplate
import com.example.model.SetLog
import com.example.model.WorkoutSession
import com.example.model.WorkoutWithExercises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class GymRepository(
  private val workoutDao: WorkoutDao,
  private val prDao: ExercisePrDao,
  private val routineDao: RoutineDao,
  private val bodyWeightDao: BodyWeightDao
) {
  val allWorkouts: Flow<List<WorkoutWithExercises>> = workoutDao.getAllWorkouts()
  val allPrs: Flow<List<ExercisePr>> = prDao.getAllPrs()
  val allRoutines: Flow<List<RoutineTemplate>> = routineDao.getAllRoutines()
  val allBodyWeights: Flow<List<BodyWeightLog>> = bodyWeightDao.getAllBodyWeights()

  fun getWorkoutsBetween(startMillis: Long, endMillis: Long): Flow<List<WorkoutWithExercises>> {
    return workoutDao.getWorkoutsBetween(startMillis, endMillis)
  }

  // Calculate 1RM via Brzycki: Weight / (1.0278 - 0.0278 * Reps) supporting fractional reps
  fun calculate1Rm(weight: Double, reps: Double): Double {
    if (reps <= 0.0 || weight <= 0.0) return 0.0
    if (reps == 1.0) return weight
    val effectiveReps = reps.coerceAtMost(30.0)
    val brzycki = weight / (1.0278 - (0.0278 * effectiveReps))
    return (brzycki * 10).roundToInt() / 10.0
  }

  fun calculate1Rm(weight: Double, reps: Int): Double = calculate1Rm(weight, reps.toDouble())

  suspend fun saveLoggedWorkout(
    rant: ParsedWorkoutRant,
    timestamp: Long = System.currentTimeMillis()
  ): Long = withContext(Dispatchers.IO) {
    val sessionTime = if (rant.workoutDateMillis > 0L) rant.workoutDateMillis else timestamp
    var totalVolume = 0.0
    var totalSets = 0
    var prsCount = 0

    rant.exercises.forEach { ex ->
      ex.sets.forEach { s ->
        totalSets++
        totalVolume += (s.weightKg * s.reps)
      }
    }

    val sessionId = workoutDao.insertWorkoutSession(
      WorkoutSession(
        name = if (rant.workoutTitle.isNotBlank()) rant.workoutTitle else "Logged Workout",
        startTimeMillis = sessionTime,
        endTimeMillis = sessionTime + 3600000L,
        totalVolumeKg = totalVolume,
        totalSets = totalSets,
        prCount = 0,
        notes = rant.notes,
        isCompleted = true
      )
    )

    rant.exercises.forEachIndexed { exIndex, ex ->
      val category = ExerciseLibrary.allExercises
        .find { it.name.equals(ex.exerciseName, ignoreCase = true) }?.category ?: "Strength"

      val exId = workoutDao.insertExerciseLog(
        ExerciseLog(
          workoutSessionId = sessionId,
          exerciseName = ex.exerciseName,
          category = category,
          orderIndex = exIndex
        )
      )

      ex.sets.forEachIndexed { setIndex, s ->
        var isPr = false
        if (s.weightKg > 0 && s.reps > 0) {
          val current1Rm = calculate1Rm(s.weightKg, s.reps)
          val existingPr = prDao.getPrForExercise(ex.exerciseName)
          if (existingPr == null || current1Rm > existingPr.estimated1RmKg) {
            prDao.upsertPr(
              ExercisePr(
                exerciseName = ex.exerciseName,
                weightKg = s.weightKg,
                reps = s.reps,
                estimated1RmKg = current1Rm,
                dateAchieved = sessionTime
              )
            )
            isPr = true
            prsCount++
          }
        }

        workoutDao.insertSetLog(
          SetLog(
            exerciseLogId = exId,
            setNumber = setIndex + 1,
            weightKg = s.weightKg,
            reps = s.reps,
            setKind = s.setKind,
            isCompleted = true,
            isPr = isPr,
            side = s.side,
            biofeedbackTags = s.biofeedbackTags,
            tempo = s.tempo,
            failurePoint = s.failurePoint,
            dropWeightKg = s.dropWeightKg,
            dropReps = s.dropReps
          )
        )
      }
    }

    // Update PR count if new PRs were made
    if (prsCount > 0) {
      val existing = workoutDao.getWorkoutById(sessionId)
      if (existing != null) {
        workoutDao.updateWorkoutSession(existing.session.copy(prCount = prsCount))
      }
    }

    sessionId
  }

  suspend fun deleteWorkout(workoutId: Long) = withContext(Dispatchers.IO) {
    workoutDao.deleteSetsForWorkoutSession(workoutId)
    workoutDao.deleteExercisesForWorkoutSession(workoutId)
    workoutDao.deleteWorkoutSession(workoutId)
  }

  suspend fun deleteAllWorkouts() = withContext(Dispatchers.IO) {
    workoutDao.deleteAllSetLogs()
    workoutDao.deleteAllExerciseLogs()
    workoutDao.deleteAllWorkoutSessions()
  }

  suspend fun recordCustomPr(exerciseName: String, weight: Double, reps: Double) = withContext(Dispatchers.IO) {
    val oneRm = calculate1Rm(weight, reps)
    prDao.upsertPr(
      ExercisePr(
        exerciseName = exerciseName,
        weightKg = weight,
        reps = reps,
        estimated1RmKg = oneRm,
        dateAchieved = System.currentTimeMillis()
      )
    )
  }

  suspend fun recordCustomPr(exerciseName: String, weight: Double, reps: Int) = recordCustomPr(exerciseName, weight, reps.toDouble())

  suspend fun deletePr(exerciseName: String) = withContext(Dispatchers.IO) {
    prDao.deletePr(exerciseName)
  }

  suspend fun deleteAllPrs() = withContext(Dispatchers.IO) {
    prDao.deleteAllPrs()
  }

  suspend fun saveCustomSplit(
    title: String,
    description: String,
    exercisesCsv: String
  ) = withContext(Dispatchers.IO) {
    routineDao.insertRoutine(
      RoutineTemplate(
        title = title,
        splitGroup = "Custom Split",
        description = description,
        exercisesCsv = exercisesCsv
      )
    )
  }

  suspend fun deleteRoutine(id: Long) = withContext(Dispatchers.IO) {
    routineDao.deleteRoutine(id)
  }

  suspend fun deleteAllRoutines() = withContext(Dispatchers.IO) {
    routineDao.deleteAllRoutines()
  }

  suspend fun logBodyWeight(weightKg: Double, dateMillis: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
    bodyWeightDao.insertBodyWeight(
      BodyWeightLog(
        dateMillis = dateMillis,
        weightKg = weightKg
      )
    )
  }

  suspend fun deleteBodyWeight(id: Long) = withContext(Dispatchers.IO) {
    bodyWeightDao.deleteBodyWeight(id)
  }

  suspend fun deleteAllBodyWeights() = withContext(Dispatchers.IO) {
    bodyWeightDao.deleteAllBodyWeights()
  }

  suspend fun clearAllData() = withContext(Dispatchers.IO) {
    workoutDao.deleteAllSetLogs()
    workoutDao.deleteAllExerciseLogs()
    workoutDao.deleteAllWorkoutSessions()
    prDao.deleteAllPrs()
    bodyWeightDao.deleteAllBodyWeights()
  }

  suspend fun deleteExerciseFromWorkout(exerciseLogId: Long, workoutSessionId: Long) = withContext(Dispatchers.IO) {
    workoutDao.deleteExerciseLog(exerciseLogId)
    val sessionWithEx = workoutDao.getWorkoutById(workoutSessionId)
    if (sessionWithEx != null) {
      if (sessionWithEx.exercises.isEmpty()) {
        workoutDao.deleteWorkoutSession(workoutSessionId)
      } else {
        var totalVolume = 0.0
        var totalSets = 0
        sessionWithEx.exercises.forEach { ex ->
          ex.sets.forEach { s ->
            totalSets++
            totalVolume += (s.weightKg * s.reps)
          }
        }
        workoutDao.updateWorkoutSession(
          sessionWithEx.session.copy(
            totalVolumeKg = totalVolume,
            totalSets = totalSets
          )
        )
      }
    }
  }

  suspend fun deleteSetFromWorkout(setId: Long, workoutSessionId: Long) = withContext(Dispatchers.IO) {
    workoutDao.deleteSetLog(setId)
    val sessionWithEx = workoutDao.getWorkoutById(workoutSessionId)
    if (sessionWithEx != null) {
      var totalVolume = 0.0
      var totalSets = 0
      sessionWithEx.exercises.forEach { ex ->
        ex.sets.forEach { s ->
          totalSets++
          totalVolume += (s.weightKg * s.reps)
        }
      }
      workoutDao.updateWorkoutSession(
        sessionWithEx.session.copy(
          totalVolumeKg = totalVolume,
          totalSets = totalSets
        )
      )
    }
  }
}
