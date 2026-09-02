package com.example

import com.example.data.OfflineRantParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testOfflineRantParserBasic() {
    val rant = "Hit chest today: Bench press 85kg 3x8, Incline DB 32kg 10,10,8 reps, Lateral raises 14kg 4x15"
    val parsed = OfflineRantParser.parse(rant)

    assertTrue(parsed.exercises.isNotEmpty())
    val bench = parsed.exercises.find { it.exerciseName.contains("Bench", ignoreCase = true) }
    assertTrue("Bench press should be detected", bench != null)
    assertEquals(3, bench?.sets?.size)
    assertEquals(85.0, bench?.sets?.first()?.weightKg ?: 0.0, 0.1)
    assertEquals(8, bench?.sets?.first()?.reps ?: 0)

    val latRaises = parsed.exercises.find { it.exerciseName.contains("Lateral", ignoreCase = true) }
    assertTrue("Lateral raises should be detected", latRaises != null)
    assertEquals(4, latRaises?.sets?.size)
    assertEquals(14.0, latRaises?.sets?.first()?.weightKg ?: 0.0, 0.1)
  }
}
