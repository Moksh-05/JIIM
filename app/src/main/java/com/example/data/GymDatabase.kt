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
  version = 5,
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

      suspend fun populateInitialData(db: GymDatabase) {
        val routineDao = db.routineDao()
        if (routineDao.countRoutines() == 0) {
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
        }
      }
    }
  }
}


