package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GeminiService
import com.example.data.UserProfile
import com.example.data.UserProfileManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JIIM", appName)
  }

  @Test
  fun `user profile manager calculates metrics and handles custom exercises`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = UserProfileManager(context)

    val profile = manager.profile.value
    assertNotNull(profile)
    assertTrue("BMI should be positive", profile.bmi > 0.0)
    assertTrue("TDEE should be positive", profile.tdeeCalories > 0)
    assertTrue("Protein target should be positive", profile.dailyProteinTargetGrams > 0)

    // Add custom exercise
    val added = manager.addCustomExercise("Incline Cable Flye", "Chest", "Pectoralis Major")
    assertTrue("Custom exercise should be added", added)

    val customList = manager.customExercises.value
    assertTrue("Custom list should contain added exercise", customList.any { it.name == "Incline Cable Flye" })

    // Verify all exercises list merges inbuilt + custom
    val allEx = manager.getAllExercises()
    assertTrue("All exercises should contain custom exercise", allEx.any { it.name == "Incline Cable Flye" })

    // Delete custom exercise
    manager.deleteCustomExercise("Incline Cable Flye")
    val updatedList = manager.customExercises.value
    assertTrue("Custom list should no longer contain deleted exercise", updatedList.none { it.name == "Incline Cable Flye" })
  }

  @Test
  fun `JJ AI offline trainer delivers structured cues and probing questions`() = runBlocking {
    val service = GeminiService()
    val reply = service.chatWithTrainerJJ(
      userMessage = "My chest is sore and I have bench press today",
      historyContext = "Lifter: Hello Coach",
      lifterProfile = "Goal: Hypertrophy, Weight: 80kg",
      recentWorkoutSummary = "Last workout: Push Day",
      isOnline = false
    )

    assertNotNull("JJ response should not be null", reply)
    assertTrue("Coach reply should not be blank", reply.first.isNotBlank())
    assertTrue("JJ should provide follow-up options", reply.second.isNotEmpty())
  }

  @Test
  fun `offline rant parser parses multi-session historical notes with qualitative exercises`() {
    val sampleNotes = """
      ### July 28, 2026: Shoulders and Biceps
      * You performed the Machine Shoulder Press and Leg Extension utilizing machines.
      * You executed Zottman Curls, Lean-in Lateral Raises, Incline Curls, and Reverse DB Flys using dumbbells.
      * You completed Calf Raises utilizing weights.

      ### July 30, 2026: Chest and Triceps
      * You completed Zottman Curls, Flat Bench Chest Press, Incline Bench Press, DB Tricep Extensions, Back Extensions, and Dumbbell Shrugs using dumbbells.
      * You performed Tricep Pushdowns utilizing a flat bar attachment on a pulley.
    """.trimIndent()

    val parsed = com.example.data.OfflineRantParser.parseMultiWorkoutRant(sampleNotes)
    assertEquals("Should detect 2 distinct workout sessions", 2, parsed.size)

    val session1 = parsed[0]
    assertEquals("Shoulders and Biceps", session1.workoutTitle)
    assertTrue("Session 1 should have exercises", session1.exercises.size >= 5)
    assertTrue("Should include Machine Shoulder Press", session1.exercises.any { it.exerciseName == "Machine Shoulder Press" })
    assertTrue("Should include Zottman Curl", session1.exercises.any { it.exerciseName == "Zottman Curl" })

    val session2 = parsed[1]
    assertEquals("Chest and Triceps", session2.workoutTitle)
    assertTrue("Session 2 should have exercises", session2.exercises.size >= 5)
    assertTrue("Should include Barbell Bench Press", session2.exercises.any { it.exerciseName == "Barbell Bench Press" })
    assertTrue("Should include Tricep Rope Pushdown", session2.exercises.any { it.exerciseName == "Tricep Rope Pushdown" })
  }
}
