package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.model.BodyWeightLog
import com.example.model.ExerciseLog
import com.example.model.ExercisePr
import com.example.model.RoutineTemplate
import com.example.model.SetLog
import com.example.model.WorkoutSession
import com.example.model.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWorkoutSession(session: WorkoutSession): Long

  @Update
  suspend fun updateWorkoutSession(session: WorkoutSession)

  @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
  suspend fun deleteWorkoutSession(sessionId: Long)

  @Query("DELETE FROM set_logs WHERE exerciseLogId IN (SELECT id FROM exercise_logs WHERE workoutSessionId = :sessionId)")
  suspend fun deleteSetsForWorkoutSession(sessionId: Long)

  @Query("DELETE FROM exercise_logs WHERE workoutSessionId = :sessionId")
  suspend fun deleteExercisesForWorkoutSession(sessionId: Long)

  @Query("DELETE FROM set_logs")
  suspend fun deleteAllSetLogs()

  @Query("DELETE FROM exercise_logs")
  suspend fun deleteAllExerciseLogs()

  @Query("DELETE FROM workout_sessions")
  suspend fun deleteAllWorkoutSessions()

  @Transaction
  @Query("SELECT * FROM workout_sessions ORDER BY startTimeMillis DESC")
  fun getAllWorkouts(): Flow<List<WorkoutWithExercises>>

  @Transaction
  @Query("SELECT * FROM workout_sessions WHERE startTimeMillis >= :startMillis AND startTimeMillis <= :endMillis ORDER BY startTimeMillis ASC")
  fun getWorkoutsBetween(startMillis: Long, endMillis: Long): Flow<List<WorkoutWithExercises>>

  @Transaction
  @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
  suspend fun getWorkoutById(sessionId: Long): WorkoutWithExercises?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

  @Query("DELETE FROM exercise_logs WHERE id = :exerciseLogId")
  suspend fun deleteExerciseLog(exerciseLogId: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSetLog(setLog: SetLog): Long

  @Update
  suspend fun updateSetLog(setLog: SetLog)

  @Query("DELETE FROM set_logs WHERE id = :setId")
  suspend fun deleteSetLog(setId: Long)
}

@Dao
interface ExercisePrDao {
  @Query("SELECT * FROM exercise_prs ORDER BY dateAchieved DESC")
  fun getAllPrs(): Flow<List<ExercisePr>>

  @Query("SELECT * FROM exercise_prs WHERE exerciseName = :exerciseName LIMIT 1")
  suspend fun getPrForExercise(exerciseName: String): ExercisePr?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPr(pr: ExercisePr)

  @Query("DELETE FROM exercise_prs WHERE exerciseName = :exerciseName")
  suspend fun deletePr(exerciseName: String)

  @Query("DELETE FROM exercise_prs")
  suspend fun deleteAllPrs()
}

@Dao
interface RoutineDao {
  @Query("SELECT * FROM routine_templates ORDER BY id ASC")
  fun getAllRoutines(): Flow<List<RoutineTemplate>>

  @Query("SELECT COUNT(*) FROM routine_templates")
  suspend fun countRoutines(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoutines(routines: List<RoutineTemplate>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoutine(routine: RoutineTemplate): Long

  @Query("DELETE FROM routine_templates WHERE id = :id")
  suspend fun deleteRoutine(id: Long)

  @Query("DELETE FROM routine_templates")
  suspend fun deleteAllRoutines()
}

@Dao
interface BodyWeightDao {
  @Query("SELECT * FROM body_weight_logs ORDER BY dateMillis ASC")
  fun getAllBodyWeights(): Flow<List<BodyWeightLog>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertBodyWeight(log: BodyWeightLog): Long

  @Query("DELETE FROM body_weight_logs WHERE id = :id")
  suspend fun deleteBodyWeight(id: Long)

  @Query("DELETE FROM body_weight_logs")
  suspend fun deleteAllBodyWeights()
}
