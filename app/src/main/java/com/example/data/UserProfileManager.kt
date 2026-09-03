package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ExerciseDefinition
import com.example.model.ExerciseLibrary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt

data class UserProfile(
  val name: String = "Lifter",
  val birthDate: String = "2000-05-15",
  val gender: String = "Male", // "Male", "Female", "Other"
  val heightCm: Double = 178.0,
  val weightKg: Double = 78.0,
  val activityLevel: String = "Moderate (3-4 gym days/wk)", // Sedentary, Light, Moderate, Heavy, Athlete
  val fitnessGoal: String = "Hypertrophy & Muscle Mass", // Hypertrophy, Strength & Power, Fat Loss / Cut, Recomposition
  val customBodyFatPercent: Double? = null // Optional override
) {
  val age: Int
    get() {
      return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dob = sdf.parse(birthDate) ?: return 24
        val dobCal = Calendar.getInstance().apply { time = dob }
        val nowCal = Calendar.getInstance()
        var age = nowCal.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
        if (nowCal.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
          age--
        }
        age.coerceAtLeast(15)
      } catch (_: Exception) {
        24
      }
    }

  val bmi: Double
    get() {
      val heightM = heightCm / 100.0
      if (heightM <= 0.0) return 22.0
      val raw = weightKg / (heightM * heightM)
      return (raw * 10).roundToInt() / 10.0
    }

  val bmiCategory: String
    get() = when {
      bmi < 18.5 -> "Underweight"
      bmi < 25.0 -> "Normal Weight"
      bmi < 30.0 -> "Athletic / Overweight"
      else -> "High Mass / Obese"
    }

  val estimatedBodyFatPercent: Double
    get() {
      if (customBodyFatPercent != null && customBodyFatPercent > 0) {
        return customBodyFatPercent
      }
      // Adult Body Fat % via Deurenberg formula
      val sexFactor = if (gender.equals("Female", ignoreCase = true)) 0 else 1
      val raw = (1.20 * bmi) + (0.23 * age) - (10.8 * sexFactor) - 5.4
      return (raw.coerceIn(5.0, 50.0) * 10).roundToInt() / 10.0
    }

  // Basal Metabolic Rate (Mifflin-St Jeor)
  val bmrCalories: Int
    get() {
      val isFemale = gender.equals("Female", ignoreCase = true)
      val base = (10 * weightKg) + (6.25 * heightCm) - (5 * age)
      val result = if (isFemale) base - 161 else base + 5
      return result.roundToInt()
    }

  // Total Daily Energy Expenditure (Maintenance)
  val tdeeCalories: Int
    get() {
      val multiplier = when {
        activityLevel.contains("Sedentary") -> 1.2
        activityLevel.contains("Light") -> 1.375
        activityLevel.contains("Moderate") -> 1.55
        activityLevel.contains("Heavy") -> 1.725
        else -> 1.9
      }
      return (bmrCalories * multiplier).roundToInt()
    }

  val dailyProteinTargetGrams: Int
    get() {
      // 2.0g per kg for lifters
      return (weightKg * 2.0).roundToInt()
    }

  val dailyWaterTargetLiters: Double
    get() = ((weightKg * 0.038) * 10).roundToInt() / 10.0
}

data class DashboardPreferences(
  val showOverloadTargets: Boolean = true,
  val showSplitBanner: Boolean = true,
  val showLastWorkout: Boolean = true,
  val showRestTimer: Boolean = true,
  val preferredSplit: String = "Push Pull Legs",
  val restTimerSeconds: Int = 90
)

class UserProfileManager(context: Context) {
  private val prefs: SharedPreferences =
    context.getSharedPreferences("jiim_user_profile_prefs", Context.MODE_PRIVATE)

  private val _profile = MutableStateFlow(loadProfile())
  val profile: StateFlow<UserProfile> = _profile.asStateFlow()

  private val _dashboardPrefs = MutableStateFlow(loadDashboardPreferences())
  val dashboardPrefs: StateFlow<DashboardPreferences> = _dashboardPrefs.asStateFlow()

  private val _customExercises = MutableStateFlow(loadCustomExercises())
  val customExercises: StateFlow<List<ExerciseDefinition>> = _customExercises.asStateFlow()

  fun updateProfile(newProfile: UserProfile) {
    _profile.value = newProfile
    saveProfile(newProfile)
  }

  fun updateDashboardPreferences(newPrefs: DashboardPreferences) {
    _dashboardPrefs.value = newPrefs
    saveDashboardPreferences(newPrefs)
  }

  fun addCustomExercise(name: String, category: String, primaryMuscle: String): Boolean {
    val trimmed = name.trim()
    if (trimmed.isBlank()) return false

    val current = _customExercises.value.toMutableList()
    val exists = current.any { it.name.equals(trimmed, ignoreCase = true) } ||
      ExerciseLibrary.allExercises.any { it.name.equals(trimmed, ignoreCase = true) }

    if (exists) return false

    val newDef = ExerciseDefinition(
      name = trimmed,
      category = category.ifBlank { "Full Body" },
      primaryMuscle = primaryMuscle.ifBlank { category }
    )
    current.add(0, newDef)
    _customExercises.value = current
    saveCustomExercises(current)
    return true
  }

  fun deleteCustomExercise(name: String) {
    val current = _customExercises.value.toMutableList()
    current.removeAll { it.name.equals(name, ignoreCase = true) }
    _customExercises.value = current
    saveCustomExercises(current)
  }

  fun getAllExercises(): List<ExerciseDefinition> {
    val custom = _customExercises.value
    val builtIn = ExerciseLibrary.allExercises
    val combined = mutableListOf<ExerciseDefinition>()
    combined.addAll(custom)
    builtIn.forEach { b ->
      if (custom.none { it.name.equals(b.name, ignoreCase = true) }) {
        combined.add(b)
      }
    }
    return combined
  }

  private fun loadProfile(): UserProfile {
    return UserProfile(
      name = prefs.getString("user_name", "Alex") ?: "Alex",
      birthDate = prefs.getString("user_dob", "2000-05-15") ?: "2000-05-15",
      gender = prefs.getString("user_gender", "Male") ?: "Male",
      heightCm = prefs.getFloat("user_height_cm", 178.0f).toDouble(),
      weightKg = prefs.getFloat("user_weight_kg", 78.0f).toDouble(),
      activityLevel = prefs.getString("user_activity", "Moderate (3-4 gym days/wk)") ?: "Moderate (3-4 gym days/wk)",
      fitnessGoal = prefs.getString("user_goal", "Hypertrophy & Muscle Mass") ?: "Hypertrophy & Muscle Mass",
      customBodyFatPercent = if (prefs.contains("user_custom_bf")) prefs.getFloat("user_custom_bf", 0f).toDouble() else null
    )
  }

  private fun saveProfile(p: UserProfile) {
    prefs.edit().apply {
      putString("user_name", p.name)
      putString("user_dob", p.birthDate)
      putString("user_gender", p.gender)
      putFloat("user_height_cm", p.heightCm.toFloat())
      putFloat("user_weight_kg", p.weightKg.toFloat())
      putString("user_activity", p.activityLevel)
      putString("user_goal", p.fitnessGoal)
      if (p.customBodyFatPercent != null) {
        putFloat("user_custom_bf", p.customBodyFatPercent.toFloat())
      } else {
        remove("user_custom_bf")
      }
      apply()
    }
  }

  private fun loadDashboardPreferences(): DashboardPreferences {
    return DashboardPreferences(
      showOverloadTargets = prefs.getBoolean("dash_overload", true),
      showSplitBanner = prefs.getBoolean("dash_split", true),
      showLastWorkout = prefs.getBoolean("dash_last_workout", true),
      showRestTimer = prefs.getBoolean("dash_rest_timer", true),
      preferredSplit = prefs.getString("dash_preferred_split", "Push Pull Legs") ?: "Push Pull Legs",
      restTimerSeconds = prefs.getInt("dash_rest_seconds", 90)
    )
  }

  private fun saveDashboardPreferences(d: DashboardPreferences) {
    prefs.edit().apply {
      putBoolean("dash_overload", d.showOverloadTargets)
      putBoolean("dash_split", d.showSplitBanner)
      putBoolean("dash_last_workout", d.showLastWorkout)
      putBoolean("dash_rest_timer", d.showRestTimer)
      putString("dash_preferred_split", d.preferredSplit)
      putInt("dash_rest_seconds", d.restTimerSeconds)
      apply()
    }
  }

  private fun loadCustomExercises(): List<ExerciseDefinition> {
    val jsonStr = prefs.getString("custom_exercises_json", null) ?: return emptyList()
    val list = mutableListOf<ExerciseDefinition>()
    try {
      val arr = JSONArray(jsonStr)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          ExerciseDefinition(
            name = obj.getString("name"),
            category = obj.getString("category"),
            primaryMuscle = obj.getString("primaryMuscle")
          )
        )
      }
    } catch (_: Exception) {}
    return list
  }

  private fun saveCustomExercises(list: List<ExerciseDefinition>) {
    val arr = JSONArray()
    list.forEach { item ->
      val obj = JSONObject().apply {
        put("name", item.name)
        put("category", item.category)
        put("primaryMuscle", item.primaryMuscle)
      }
      arr.put(obj)
    }
    prefs.edit().putString("custom_exercises_json", arr.toString()).apply()
  }
}
