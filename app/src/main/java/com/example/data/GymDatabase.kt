package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.BodyWeightLog
import com.example.model.ExerciseLog
import com.example.model.ExercisePr
import com.example.model.RoutineTemplate
import com.example.model.SetLog
import com.example.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    WorkoutSession::class,
    ExerciseLog::class,
    SetLog::class,
    ExercisePr::class,
    RoutineTemplate::class,
    BodyWeightLog::class
  ],
  version = 3,
  exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
  abstract fun workoutDao(): WorkoutDao
  abstract fun exercisePrDao(): ExercisePrDao
  abstract fun routineDao(): RoutineDao
  abstract fun bodyWeightDao(): BodyWeightDao

  companion object {
    @Volatile
    private var INSTANCE: GymDatabase? = null

    fun getDatabase(context: Context, scope: CoroutineScope): GymDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          GymDatabase::class.java,
          "gym_tracker.db"
        )
          .addCallback(GymDatabaseCallback(scope))
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }

    private class GymDatabaseCallback(
      private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateInitialData(database)
          }
        }
      }

      override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            if (database.routineDao().countRoutines() == 0) {
              populateInitialData(database)
            }
          }
        }
      }

      suspend fun populateInitialData(db: GymDatabase) {
        val routineDao = db.routineDao()
        val defaultRoutines = listOf(
          RoutineTemplate(
            title = "Chest & Triceps Hypertrophy",
            splitGroup = "Custom Split",
            description = "Heavy compound pressing paired with tricep isolation",
            exercisesCsv = "Barbell Bench Press,Incline Dumbbell Press,Dips,Tricep Rope Pushdown,Skull Crushers"
          ),
          RoutineTemplate(
            title = "Biceps & Boulder Shoulders",
            splitGroup = "Custom Split",
            description = "Strict deltoid isolation and peak bicep contraction",
            exercisesCsv = "Overhead Barbell Press,Dumbbell Lateral Raise,Face Pulls,Barbell Bicep Curl,Incline Dumbbell Curl,Hammer Curl"
          ),
          RoutineTemplate(
            title = "Abs, Cardio & Back Depth",
            splitGroup = "Custom Split",
            description = "Core stability, pull compounds, and conditioning",
            exercisesCsv = "Barbell Deadlift,Lat Pulldown,Seated Cable Row,Hanging Leg Raise,Plank"
          ),
          RoutineTemplate(
            title = "Legs & Quad Annihilation",
            splitGroup = "Custom Split",
            description = "High intensity quad drive, posterior chain, and calf overload",
            exercisesCsv = "Barbell Back Squat,Romanian Deadlift,Leg Press,Leg Extension,Standing Calf Raise"
          )
        )
        routineDao.insertRoutines(defaultRoutines)

        val prDao = db.exercisePrDao()
        val bwDao = db.bodyWeightDao()
        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        bwDao.insertBodyWeight(BodyWeightLog(dateMillis = now - oneDay * 14, weightKg = 79.2))
        bwDao.insertBodyWeight(BodyWeightLog(dateMillis = now - oneDay * 10, weightKg = 78.8))
        bwDao.insertBodyWeight(BodyWeightLog(dateMillis = now - oneDay * 7, weightKg = 78.5))
        bwDao.insertBodyWeight(BodyWeightLog(dateMillis = now - oneDay * 4, weightKg = 78.3))
        bwDao.insertBodyWeight(BodyWeightLog(dateMillis = now - oneDay * 1, weightKg = 78.0))

        prDao.upsertPr(
          ExercisePr(
            exerciseName = "Barbell Bench Press",
            weightKg = 85.0,
            reps = 6,
            estimated1RmKg = 98.6,
            dateAchieved = now - oneDay * 3
          )
        )
        prDao.upsertPr(
          ExercisePr(
            exerciseName = "Barbell Back Squat",
            weightKg = 115.0,
            reps = 5,
            estimated1RmKg = 133.4,
            dateAchieved = now - oneDay * 5
          )
        )
        prDao.upsertPr(
          ExercisePr(
            exerciseName = "Barbell Deadlift",
            weightKg = 145.0,
            reps = 4,
            estimated1RmKg = 163.8,
            dateAchieved = now - oneDay * 7
          )
        )
        prDao.upsertPr(
          ExercisePr(
            exerciseName = "Overhead Barbell Press",
            weightKg = 55.0,
            reps = 5,
            estimated1RmKg = 63.8,
            dateAchieved = now - oneDay * 10
          )
        )

        // Seed 7 recent sessions spanning the past 14 days so Calendar & Graphs have vivid data immediately
        val workoutDao = db.workoutDao()

        // Session 1: 12 days ago - Chest & Triceps
        seedSession(
          workoutDao = workoutDao,
          name = "Chest & Triceps Overload",
          timeMillis = now - oneDay * 12,
          notes = "First session of cycle, solid bench speed",
          exercises = listOf(
            Triple("Barbell Bench Press", "Chest", listOf(Pair(77.5, 8), Pair(77.5, 8), Pair(80.0, 6))),
            Triple("Incline Dumbbell Press", "Chest", listOf(Pair(28.0, 10), Pair(28.0, 10), Pair(30.0, 8))),
            Triple("Tricep Rope Pushdown", "Arms", listOf(Pair(30.0, 12), Pair(32.5, 10), Pair(32.5, 10)))
          )
        )

        // Session 2: 10 days ago - Biceps & Shoulders
        seedSession(
          workoutDao = workoutDao,
          name = "Biceps & Delts Blast",
          timeMillis = now - oneDay * 10,
          notes = "Strict lateral raises, mind muscle connection high",
          exercises = listOf(
            Triple("Overhead Barbell Press", "Shoulders", listOf(Pair(50.0, 6), Pair(52.5, 6), Pair(55.0, 5))),
            Triple("Dumbbell Lateral Raise", "Shoulders", listOf(Pair(12.0, 15), Pair(12.0, 15), Pair(14.0, 12))),
            Triple("Barbell Bicep Curl", "Arms", listOf(Pair(32.5, 10), Pair(35.0, 8), Pair(35.0, 8)))
          )
        )

        // Session 3: 8 days ago - Legs
        seedSession(
          workoutDao = workoutDao,
          name = "Leg Day Quad Drive",
          timeMillis = now - oneDay * 8,
          notes = "Deep squat depth, felt great in the hole",
          exercises = listOf(
            Triple("Barbell Back Squat", "Legs", listOf(Pair(105.0, 6), Pair(110.0, 5), Pair(115.0, 5))),
            Triple("Romanian Deadlift", "Legs", listOf(Pair(90.0, 8), Pair(95.0, 8), Pair(95.0, 8))),
            Triple("Leg Extension", "Legs", listOf(Pair(60.0, 12), Pair(65.0, 12), Pair(70.0, 10)))
          )
        )

        // Session 4: 6 days ago - Abs & Cardio & Pull
        seedSession(
          workoutDao = workoutDao,
          name = "Abs, Cardio & Back",
          timeMillis = now - oneDay * 6,
          notes = "Heavy deadlift pull, strong grip",
          exercises = listOf(
            Triple("Barbell Deadlift", "Back", listOf(Pair(135.0, 5), Pair(140.0, 4), Pair(145.0, 4))),
            Triple("Lat Pulldown", "Back", listOf(Pair(65.0, 10), Pair(70.0, 8), Pair(70.0, 8))),
            Triple("Hanging Leg Raise", "Core", listOf(Pair(0.0, 15), Pair(0.0, 15), Pair(0.0, 12)))
          )
        )

        // Session 5: 4 days ago - Chest & Triceps (Progressive Overload!)
        seedSession(
          workoutDao = workoutDao,
          name = "Chest & Triceps Overload",
          timeMillis = now - oneDay * 4,
          notes = "Progressed bench from 80kg to 82.5kg! Nice pump",
          exercises = listOf(
            Triple("Barbell Bench Press", "Chest", listOf(Pair(80.0, 8), Pair(82.5, 6), Pair(85.0, 6))),
            Triple("Incline Dumbbell Press", "Chest", listOf(Pair(30.0, 10), Pair(30.0, 10), Pair(32.0, 8))),
            Triple("Tricep Rope Pushdown", "Arms", listOf(Pair(32.5, 12), Pair(35.0, 10), Pair(35.0, 10)))
          )
        )

        // Session 6: 2 days ago - Biceps & Shoulders
        seedSession(
          workoutDao = workoutDao,
          name = "Biceps & Delts Blast",
          timeMillis = now - oneDay * 2,
          notes = "Huge bicep pump, shoulder striations visible",
          exercises = listOf(
            Triple("Overhead Barbell Press", "Shoulders", listOf(Pair(52.5, 6), Pair(55.0, 5), Pair(55.0, 5))),
            Triple("Dumbbell Lateral Raise", "Shoulders", listOf(Pair(14.0, 15), Pair(14.0, 15), Pair(14.0, 14))),
            Triple("Barbell Bicep Curl", "Arms", listOf(Pair(35.0, 10), Pair(35.0, 9), Pair(37.5, 7)))
          )
        )

        // Session 7: Yesterday - Legs & Core
        seedSession(
          workoutDao = workoutDao,
          name = "Leg Day Quad Drive",
          timeMillis = now - oneDay * 1,
          notes = "Volume PR on squats! Quad burn intense",
          exercises = listOf(
            Triple("Barbell Back Squat", "Legs", listOf(Pair(110.0, 6), Pair(115.0, 5), Pair(117.5, 5))),
            Triple("Romanian Deadlift", "Legs", listOf(Pair(95.0, 8), Pair(100.0, 8), Pair(100.0, 8))),
            Triple("Standing Calf Raise", "Legs", listOf(Pair(70.0, 15), Pair(75.0, 15), Pair(80.0, 12)))
          )
        )
      }

      private suspend fun seedSession(
        workoutDao: WorkoutDao,
        name: String,
        timeMillis: Long,
        notes: String,
        exercises: List<Triple<String, String, List<Pair<Double, Int>>>>
      ) {
        var totalVol = 0.0
        var totalSets = 0

        exercises.forEach { (_, _, sets) ->
          sets.forEach { (wt, reps) ->
            totalSets++
            totalVol += (wt * reps)
          }
        }

        val sessionId = workoutDao.insertWorkoutSession(
          WorkoutSession(
            name = name,
            startTimeMillis = timeMillis,
            endTimeMillis = timeMillis + 3600000L,
            totalVolumeKg = totalVol,
            totalSets = totalSets,
            prCount = 0,
            notes = notes,
            isCompleted = true
          )
        )

        exercises.forEachIndexed { exIdx, (exName, cat, sets) ->
          val exId = workoutDao.insertExerciseLog(
            ExerciseLog(
              workoutSessionId = sessionId,
              exerciseName = exName,
              category = cat,
              orderIndex = exIdx
            )
          )
          sets.forEachIndexed { setIdx, (wt, reps) ->
            workoutDao.insertSetLog(
              SetLog(
                exerciseLogId = exId,
                setNumber = setIdx + 1,
                weightKg = wt,
                reps = reps,
                setKind = "NORMAL",
                isCompleted = true
              )
            )
          }
        }
      }
    }
  }
}
