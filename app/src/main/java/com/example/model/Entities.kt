package com.example.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val startTimeMillis: Long,
  val endTimeMillis: Long = 0,
  val totalVolumeKg: Double = 0.0,
  val totalSets: Int = 0,
  val prCount: Int = 0,
  val notes: String = "",
  val isCompleted: Boolean = true
)

@Entity(
  tableName = "exercise_logs",
  indices = [Index(value = ["workoutSessionId"])]
)
data class ExerciseLog(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val workoutSessionId: Long,
  val exerciseName: String,
  val category: String,
  val orderIndex: Int
)

@Entity(
  tableName = "set_logs",
  indices = [Index(value = ["exerciseLogId"])]
)
data class SetLog(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val exerciseLogId: Long,
  val setNumber: Int,
  val weightKg: Double,
  val reps: Double,
  val setKind: String = "NORMAL", // NORMAL, WARMUP, WORKING, DROP, PARTIAL, FAILURE
  val isCompleted: Boolean = true,
  val isPr: Boolean = false,
  val side: String = "BOTH", // BOTH, LEFT, RIGHT (Unilateral tracking)
  val biofeedbackTags: String = "", // e.g. "Grip Failure,Form Breakdown,Asymmetry"
  val tempo: String = "", // e.g. "3-1-1-0" or "Explosive positive, controlled negative"
  val failurePoint: String = "", // e.g. "Failed at 6.5 reps (mid-concentric)"
  val dropWeightKg: Double = 0.0,
  val dropReps: Double = 0.0
)

data class ExerciseWithSets(
  @Embedded val exercise: ExerciseLog,
  @Relation(
    parentColumn = "id",
    entityColumn = "exerciseLogId"
  )
  val sets: List<SetLog>
)

data class WorkoutWithExercises(
  @Embedded val session: WorkoutSession,
  @Relation(
    entity = ExerciseLog::class,
    parentColumn = "id",
    entityColumn = "workoutSessionId"
  )
  val exercises: List<ExerciseWithSets>
)

@Entity(tableName = "exercise_prs")
data class ExercisePr(
  @PrimaryKey val exerciseName: String,
  val weightKg: Double,
  val reps: Double,
  val estimated1RmKg: Double,
  val dateAchieved: Long
)

@Entity(tableName = "body_weight_logs")
data class BodyWeightLog(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val dateMillis: Long,
  val weightKg: Double,
  val notes: String = ""
)

@Entity(tableName = "routine_templates")
data class RoutineTemplate(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val splitGroup: String, // "Push Pull Legs", "Upper Lower", "Arnold Split", "Smart Split", "Custom Split"
  val description: String,
  val exercisesCsv: String // e.g. "Barbell Bench Press,Incline Dumbbell Press,Overhead Press"
)

data class ParsedWorkoutRant(
  val id: String = java.util.UUID.randomUUID().toString(),
  val workoutTitle: String,
  val exercises: List<ParsedExerciseLog>,
  val notes: String = "",
  val workoutDateMillis: Long = System.currentTimeMillis(),
  val dateDisplay: String = "Today",
  val clarificationQuestions: List<String> = emptyList(),
  val detectedUnit: String = "LBS"
)

data class ParsedExerciseLog(
  val exerciseName: String,
  val sets: List<ParsedSetLog>,
  val isUnilateral: Boolean = false,
  val notes: String = ""
)

data class ParsedSetLog(
  val weightKg: Double,
  val reps: Double,
  val setKind: String = "NORMAL",
  val side: String = "BOTH", // BOTH, LEFT, RIGHT
  val biofeedbackTags: String = "",
  val tempo: String = "",
  val failurePoint: String = "",
  val dropWeightKg: Double = 0.0,
  val dropReps: Double = 0.0
)

data class AiProgressAnalysis(
  val timestamp: Long = System.currentTimeMillis(),
  val overallScore: String,
  val hypertrophyStatus: String,
  val progressiveOverloadVerdict: String,
  val recommendations: List<String>,
  val detectedSplitName: String? = null,
  val detectedSplitBreakdown: List<String> = emptyList(),
  val stagnationAlerts: List<String> = emptyList()
)

data class DailyWorkoutSummary(
  val dateMillis: Long,
  val dateString: String,
  val workoutCount: Int,
  val totalVolumeKg: Double,
  val workoutNames: List<String>
)

data class ExerciseDefinition(
  val name: String,
  val category: String,
  val primaryMuscle: String
)

object ExerciseLibrary {
  val allExercises = listOf(
    ExerciseDefinition("Barbell Bench Press", "Chest", "Chest & Triceps"),
    ExerciseDefinition("Incline Dumbbell Press", "Chest", "Upper Chest"),
    ExerciseDefinition("Cable Chest Fly", "Chest", "Pectorals"),
    ExerciseDefinition("Dips", "Chest", "Lower Chest & Triceps"),
    ExerciseDefinition("Push Ups", "Chest", "Chest & Core"),
    ExerciseDefinition("Barbell Deadlift", "Back", "Erectors & Lats"),
    ExerciseDefinition("Barbell Bent-Over Row", "Back", "Upper & Mid Back"),
    ExerciseDefinition("Lat Pulldown", "Back", "Latissimus Dorsi"),
    ExerciseDefinition("Seated Cable Row", "Back", "Rhomboids & Lats"),
    ExerciseDefinition("Pull-Ups", "Back", "Lats & Biceps"),
    ExerciseDefinition("Barbell Back Squat", "Legs", "Quadriceps & Glutes"),
    ExerciseDefinition("Romanian Deadlift", "Legs", "Hamstrings & Glutes"),
    ExerciseDefinition("Leg Press", "Legs", "Quadriceps"),
    ExerciseDefinition("Leg Extension", "Legs", "Quadriceps"),
    ExerciseDefinition("Hamstring Leg Curl", "Legs", "Hamstrings"),
    ExerciseDefinition("Standing Calf Raise", "Legs", "Calves"),
    ExerciseDefinition("Overhead Barbell Press", "Shoulders", "Front Deltoids"),
    ExerciseDefinition("Dumbbell Lateral Raise", "Shoulders", "Side Deltoids"),
    ExerciseDefinition("Face Pulls", "Shoulders", "Rear Deltoids"),
    ExerciseDefinition("Dumbbell Shoulder Press", "Shoulders", "Deltoids"),
    ExerciseDefinition("Barbell Bicep Curl", "Arms", "Biceps"),
    ExerciseDefinition("Incline Dumbbell Curl", "Arms", "Biceps Long Head"),
    ExerciseDefinition("Hammer Curl", "Arms", "Brachialis & Forearms"),
    ExerciseDefinition("Tricep Rope Pushdown", "Arms", "Triceps Lateral Head"),
    ExerciseDefinition("Skull Crushers", "Arms", "Triceps Long Head"),
    ExerciseDefinition("Hanging Leg Raise", "Core", "Abdominals"),
    ExerciseDefinition("Cable Woodchoppers", "Core", "Obliques"),
    ExerciseDefinition("Plank", "Core", "Core Stability")
  )
}
