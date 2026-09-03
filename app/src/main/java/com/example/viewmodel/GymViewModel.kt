package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GeminiService
import com.example.data.GymDatabase
import com.example.data.GymRepository
import com.example.data.NetworkMonitor
import com.example.data.OfflineProgressAnalyzer
import com.example.model.AiProgressAnalysis
import com.example.model.BodyWeightLog
import com.example.model.DailyWorkoutSummary
import com.example.model.ExerciseLibrary
import com.example.model.ExercisePr
import com.example.model.ParsedExerciseLog
import com.example.model.ParsedSetLog
import com.example.model.ParsedWorkoutRant
import com.example.model.RoutineTemplate
import com.example.model.WorkoutWithExercises
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class ActiveSetLog(
  val setNumber: Int,
  val weightKg: Double,
  val reps: Int
)

data class ActiveExerciseLog(
  val exerciseName: String,
  val sets: List<ActiveSetLog>
)

data class OverloadTarget(
  val exerciseName: String,
  val lastWeightKg: Double,
  val lastReps: Int,
  val lastSetsCount: Int,
  val targetRepsProgression: String,
  val targetWeightProgression: String
)

data class ExerciseSessionPoint(
  val dateMillis: Long,
  val sessionName: String,
  val topWeightKg: Double,
  val totalReps: Int,
  val totalSets: Int,
  val estimated1Rm: Double,
  val isOverloadComparedToPrevious: Boolean
)

data class PlateauInsight(
  val exerciseName: String,
  val stalledWeightKg: Double,
  val sessionCount: Int,
  val formFixCue: String,
  val recommendedAccessory: String
)

data class RamblerClarification(
  val exerciseIndex: Int,
  val exerciseName: String,
  val question: String,
  val initialWeightKg: Double,
  val initialReps: Int
)

data class TrainerMessage(
  val id: Long = System.currentTimeMillis(),
  val sender: String, // "JIIM AI" or "USER"
  val text: String,
  val timestamp: Long = System.currentTimeMillis(),
  val promptFollowUps: List<String> = emptyList()
)

class GymViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: GymRepository
  private val geminiService = GeminiService()
  private val networkMonitor = NetworkMonitor(application)
  private val userProfileManager = com.example.data.UserProfileManager(application)

  val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.checkCurrentOnline())

  // User Profile, Custom Exercises & Dashboard Customization
  val userProfile: StateFlow<com.example.data.UserProfile> = userProfileManager.profile
  val dashboardPrefs: StateFlow<com.example.data.DashboardPreferences> = userProfileManager.dashboardPrefs
  val customExercises: StateFlow<List<com.example.model.ExerciseDefinition>> = userProfileManager.customExercises

  fun updateProfile(profile: com.example.data.UserProfile) {
    userProfileManager.updateProfile(profile)
  }

  fun updateDashboardPrefs(prefs: com.example.data.DashboardPreferences) {
    userProfileManager.updateDashboardPreferences(prefs)
  }

  fun addCustomExercise(name: String, category: String, primaryMuscle: String): Boolean {
    return userProfileManager.addCustomExercise(name, category, primaryMuscle)
  }

  fun deleteCustomExercise(name: String) {
    userProfileManager.deleteCustomExercise(name)
  }

  fun getAllExercises(): List<com.example.model.ExerciseDefinition> {
    return userProfileManager.getAllExercises()
  }

  // -------------------------------------------------------------
  // REST TIMER STATE
  // -------------------------------------------------------------
  private val _restTimerSecondsRemaining = MutableStateFlow(0)
  val restTimerSecondsRemaining: StateFlow<Int> = _restTimerSecondsRemaining.asStateFlow()

  private val _isRestTimerActive = MutableStateFlow(false)
  val isRestTimerActive: StateFlow<Boolean> = _isRestTimerActive.asStateFlow()

  private var timerJob: kotlinx.coroutines.Job? = null

  fun startRestTimer(seconds: Int? = null) {
    val duration = seconds ?: dashboardPrefs.value.restTimerSeconds
    timerJob?.cancel()
    _restTimerSecondsRemaining.value = duration
    _isRestTimerActive.value = true

    timerJob = viewModelScope.launch {
      while (_restTimerSecondsRemaining.value > 0) {
        kotlinx.coroutines.delay(1000)
        _restTimerSecondsRemaining.value--
      }
      _isRestTimerActive.value = false
    }
  }

  fun stopRestTimer() {
    timerJob?.cancel()
    _isRestTimerActive.value = false
    _restTimerSecondsRemaining.value = 0
  }

  // -------------------------------------------------------------
  // LOGGING METHOD SELECTION (Method 1: SET_LOGGER vs Method 2: RAMBLER)
  // -------------------------------------------------------------
  private val _selectedLoggingMethod = MutableStateFlow("SET_LOGGER") // "SET_LOGGER" or "RAMBLER"
  val selectedLoggingMethod: StateFlow<String> = _selectedLoggingMethod.asStateFlow()

  fun setLoggingMethod(method: String) {
    _selectedLoggingMethod.value = method
  }

  // -------------------------------------------------------------
  // JIIM AI TRAINER CONVERSATION & INSIGHT GATHERING
  // -------------------------------------------------------------
  private val _trainerMessages = MutableStateFlow<List<TrainerMessage>>(
    listOf(
      TrainerMessage(
        id = 1L,
        sender = "JIIM AI",
        text = "Yo! I'm JIIM AI, your AI Gym Coach & Biomechanics Specialist. I'm here to analyze your lifting, dial in progressive overload, and break through any plateaus.\n\nTo optimize today's session: How did you sleep last night, and which muscle group feels the most sore or tight right now?",
        promptFollowUps = listOf(
          "Slept 7-8h, feeling recovered",
          "Chest & shoulders are sore",
          "Legs are still fatigued",
          "What split should I run today?",
          "How to hit a new Bench PR?"
        )
      )
    )
  )
  val trainerMessages: StateFlow<List<TrainerMessage>> = _trainerMessages.asStateFlow()

  private val _isTrainerTyping = MutableStateFlow(false)
  val isTrainerTyping: StateFlow<Boolean> = _isTrainerTyping.asStateFlow()

  fun sendTrainerMessage(messageText: String) {
    if (messageText.isBlank()) return
    val userMsg = TrainerMessage(sender = "USER", text = messageText)
    val current = _trainerMessages.value + userMsg
    _trainerMessages.value = current

    viewModelScope.launch {
      _isTrainerTyping.value = true
      try {
        val p = userProfile.value
        val profileSummary = "Lifter Name: ${p.name}, Age: ${p.age}, Gender: ${p.gender}, Weight: ${p.weightKg}kg, Height: ${p.heightCm}cm, BMI: ${p.bmi} (${p.bmiCategory}), Goal: ${p.fitnessGoal}, Activity: ${p.activityLevel}"
        val recentWorkouts = allWorkouts.value.take(3).joinToString("\n") {
          "- ${it.session.name} (${it.session.totalVolumeKg.toInt()}kg, ${it.session.totalSets} sets)"
        }
        val conversationHistory = current.takeLast(6).joinToString("\n") { "${it.sender}: ${it.text}" }

        val (reply, followUps) = geminiService.chatWithTrainerJJ(
          userMessage = messageText,
          historyContext = conversationHistory,
          lifterProfile = profileSummary,
          recentWorkoutSummary = recentWorkouts,
          isOnline = isOnline.value
        )

        val coachMsg = TrainerMessage(
          sender = "JIIM AI",
          text = reply,
          promptFollowUps = followUps
        )
        _trainerMessages.value = _trainerMessages.value + coachMsg
      } catch (e: Exception) {
        val fallback = TrainerMessage(
          sender = "JIIM AI",
          text = "Form and progressive overload come first. Make sure your eccentric control is at 2-3 seconds to maximize mechanical tension.\n\nJIIM AI's Insight Question: What specific lift are you tackling next?",
          promptFollowUps = listOf("Barbell Bench Press", "Barbell Back Squat", "Barbell Deadlift", "Overhead Press")
        )
        _trainerMessages.value = _trainerMessages.value + fallback
      } finally {
        _isTrainerTyping.value = false
      }
    }
  }

  fun resetTrainerChat() {
    _trainerMessages.value = listOf(
      TrainerMessage(
        id = System.currentTimeMillis(),
        sender = "JIIM AI",
        text = "Fresh workout session! I'm JIIM AI, your AI Coach. How is your energy level right now, and what are we training today?",
        promptFollowUps = listOf(
          "Energy is 9/10, ready to PR",
          "A bit fatigued, need a warm-up",
          "Push Day • Chest & Triceps",
          "Pull Day • Back & Biceps"
        )
      )
    )
  }

  init {
    val database = GymDatabase.getDatabase(application, viewModelScope)
    repository = GymRepository(
      database.workoutDao(),
      database.exercisePrDao(),
      database.routineDao(),
      database.bodyWeightDao()
    )
  }

  // Completed workout history
  val allWorkouts: StateFlow<List<WorkoutWithExercises>> = repository.allWorkouts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Personal Records
  val allPrs: StateFlow<List<ExercisePr>> = repository.allPrs
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Routine templates
  val allRoutines: StateFlow<List<RoutineTemplate>> = repository.allRoutines
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Body weight logs
  val allBodyWeights: StateFlow<List<BodyWeightLog>> = repository.allBodyWeights
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Unit toggle: kg vs lbs
  private val _useLbs = MutableStateFlow(false)
  val useLbs: StateFlow<Boolean> = _useLbs.asStateFlow()

  fun toggleUnits() {
    _useLbs.value = !_useLbs.value
  }

  // -------------------------------------------------------------
  // ACTIVE SESSION EXERCISE-BY-EXERCISE LOGGER
  // -------------------------------------------------------------
  private val _activeExercises = MutableStateFlow<List<ActiveExerciseLog>>(emptyList())
  val activeExercises: StateFlow<List<ActiveExerciseLog>> = _activeExercises.asStateFlow()

  fun addSetToActiveSession(exerciseName: String, weightKg: Double, reps: Int) {
    val current = _activeExercises.value.toMutableList()
    val existingIndex = current.indexOfFirst { it.exerciseName.equals(exerciseName, ignoreCase = true) }

    if (existingIndex >= 0) {
      val existing = current[existingIndex]
      val newSetNumber = existing.sets.size + 1
      val updatedSets = existing.sets + ActiveSetLog(
        setNumber = newSetNumber,
        weightKg = weightKg,
        reps = reps
      )
      current[existingIndex] = existing.copy(sets = updatedSets)
    } else {
      current.add(
        ActiveExerciseLog(
          exerciseName = exerciseName,
          sets = listOf(
            ActiveSetLog(
              setNumber = 1,
              weightKg = weightKg,
              reps = reps
            )
          )
        )
      )
    }
    _activeExercises.value = current
  }

  fun removeSetFromActiveSession(exerciseIndex: Int, setIndex: Int) {
    val current = _activeExercises.value.toMutableList()
    if (exerciseIndex in current.indices) {
      val ex = current[exerciseIndex]
      val newSets = ex.sets.toMutableList()
      if (setIndex in newSets.indices) {
        newSets.removeAt(setIndex)
        if (newSets.isEmpty()) {
          current.removeAt(exerciseIndex)
        } else {
          val reindexed = newSets.mapIndexed { i, s -> s.copy(setNumber = i + 1) }
          current[exerciseIndex] = ex.copy(sets = reindexed)
        }
        _activeExercises.value = current
      }
    }
  }

  fun removeExerciseFromActiveSession(exerciseIndex: Int) {
    val current = _activeExercises.value.toMutableList()
    if (exerciseIndex in current.indices) {
      current.removeAt(exerciseIndex)
      _activeExercises.value = current
    }
  }

  fun clearActiveSession() {
    _activeExercises.value = emptyList()
  }

  fun saveActiveSession(sessionTitle: String, notes: String = "") {
    val current = _activeExercises.value
    if (current.isEmpty()) return

    val parsedExercises = current.map { ex ->
      ParsedExerciseLog(
        exerciseName = ex.exerciseName,
        sets = ex.sets.map { s ->
          ParsedSetLog(weightKg = s.weightKg, reps = s.reps)
        }
      )
    }

    val rant = ParsedWorkoutRant(
      workoutTitle = sessionTitle.ifBlank { "Logged Workout" },
      exercises = parsedExercises,
      notes = notes
    )

    saveLoggedWorkout(rant)
    clearActiveSession()
  }

  // -------------------------------------------------------------
  // DATABASE DELETE METHODS (Safely protected behind modals)
  // -------------------------------------------------------------
  fun deleteExerciseFromWorkout(exerciseLogId: Long, workoutSessionId: Long) {
    viewModelScope.launch {
      repository.deleteExerciseFromWorkout(exerciseLogId, workoutSessionId)
      runProgressAnalysis()
    }
  }

  fun deleteWorkout(workoutId: Long) {
    viewModelScope.launch {
      repository.deleteWorkout(workoutId)
      runProgressAnalysis()
    }
  }

  fun deleteBodyWeight(id: Long) {
    viewModelScope.launch {
      repository.deleteBodyWeight(id)
    }
  }

  fun logBodyWeight(weightValue: Double) {
    viewModelScope.launch {
      val weightKg = if (_useLbs.value) (weightValue / 2.20462) else weightValue
      repository.logBodyWeight(weightKg = (weightKg * 10).roundToInt() / 10.0)
    }
  }

  // -------------------------------------------------------------
  // RAMBLER PARSING & CLARIFICATION
  // -------------------------------------------------------------
  private val _isParsingRant = MutableStateFlow(false)
  val isParsingRant: StateFlow<Boolean> = _isParsingRant.asStateFlow()

  private val _parsedRant = MutableStateFlow<ParsedWorkoutRant?>(null)
  val parsedRant: StateFlow<ParsedWorkoutRant?> = _parsedRant.asStateFlow()

  private val _clarifications = MutableStateFlow<List<RamblerClarification>>(emptyList())
  val clarifications: StateFlow<List<RamblerClarification>> = _clarifications.asStateFlow()

  fun parseGymRant(rantText: String) {
    if (rantText.isBlank()) return
    viewModelScope.launch {
      _isParsingRant.value = true
      try {
        val result = geminiService.parseGymRant(rantText, isOnline.value)
        _parsedRant.value = result

        val needed = mutableListOf<RamblerClarification>()
        result.exercises.forEachIndexed { idx, ex ->
          val zeroWeightSets = ex.sets.filter { it.weightKg <= 0.0 }
          val zeroRepSets = ex.sets.filter { it.reps <= 0 }

          if (zeroWeightSets.isNotEmpty()) {
            needed.add(
              RamblerClarification(
                exerciseIndex = idx,
                exerciseName = ex.exerciseName,
                question = "What weight did you use for ${ex.exerciseName}?",
                initialWeightKg = 0.0,
                initialReps = ex.sets.firstOrNull()?.reps ?: 10
              )
            )
          } else if (zeroRepSets.isNotEmpty()) {
            needed.add(
              RamblerClarification(
                exerciseIndex = idx,
                exerciseName = ex.exerciseName,
                question = "How many reps did you complete for ${ex.exerciseName}?",
                initialWeightKg = ex.sets.firstOrNull()?.weightKg ?: 20.0,
                initialReps = 10
              )
            )
          }
        }
        _clarifications.value = needed
      } finally {
        _isParsingRant.value = false
      }
    }
  }

  fun updateClarification(exerciseIndex: Int, newWeightKg: Double, newReps: Int) {
    val rant = _parsedRant.value ?: return
    val updatedExercises = rant.exercises.toMutableList()
    if (exerciseIndex in updatedExercises.indices) {
      val ex = updatedExercises[exerciseIndex]
      val updatedSets = ex.sets.map { s ->
        s.copy(
          weightKg = if (s.weightKg <= 0) newWeightKg else s.weightKg,
          reps = if (s.reps <= 0) newReps else s.reps
        )
      }
      updatedExercises[exerciseIndex] = ex.copy(sets = updatedSets)
      _parsedRant.value = rant.copy(exercises = updatedExercises)
    }
    _clarifications.value = _clarifications.value.filterNot { it.exerciseIndex == exerciseIndex }
  }

  fun clearParsedRant() {
    _parsedRant.value = null
    _clarifications.value = emptyList()
  }

  fun saveLoggedWorkout(rant: ParsedWorkoutRant) {
    viewModelScope.launch {
      repository.saveLoggedWorkout(rant)
      _parsedRant.value = null
      _clarifications.value = emptyList()
      runProgressAnalysis()
    }
  }

  // -------------------------------------------------------------
  // AI PROGRESS ANALYSIS
  // -------------------------------------------------------------
  private val _isAnalyzing = MutableStateFlow(false)
  val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

  private val _aiAnalysis = MutableStateFlow<AiProgressAnalysis?>(null)
  val aiAnalysis: StateFlow<AiProgressAnalysis?> = _aiAnalysis.asStateFlow()

  init {
    viewModelScope.launch {
      allWorkouts.collect { workouts ->
        if (workouts.isNotEmpty() && _aiAnalysis.value == null) {
          _aiAnalysis.value = OfflineProgressAnalyzer.analyze(workouts, allPrs.value)
        }
      }
    }
  }

  fun runProgressAnalysis() {
    viewModelScope.launch {
      _isAnalyzing.value = true
      try {
        val result = geminiService.analyzeProgress(
          workouts = allWorkouts.value,
          prs = allPrs.value,
          isOnline = isOnline.value
        )
        _aiAnalysis.value = result
      } finally {
        _isAnalyzing.value = false
      }
    }
  }

  // -------------------------------------------------------------
  // PREVIOUS WORKOUT & OVERLOAD TARGETS (FRONT PAGE TOP & SPLIT-AWARE)
  // -------------------------------------------------------------
  fun getPreviousWorkout(): WorkoutWithExercises? {
    val workouts = allWorkouts.value
    if (workouts.isEmpty()) return null
    return workouts.maxByOrNull { it.session.startTimeMillis }
  }

  fun getPreviousWorkoutForSplit(splitName: String): WorkoutWithExercises? {
    val workouts = allWorkouts.value.sortedByDescending { it.session.startTimeMillis }
    if (workouts.isEmpty()) return null

    val targetLower = splitName.lowercase()
    val isPush = targetLower.contains("push") || targetLower.contains("chest") || targetLower.contains("tricep")
    val isPull = targetLower.contains("pull") || targetLower.contains("back") || targetLower.contains("bicep")
    val isLegs = targetLower.contains("leg") || targetLower.contains("quad") || targetLower.contains("hamstring") || targetLower.contains("calf")
    val isUpper = targetLower.contains("upper")
    val isLower = targetLower.contains("lower")
    val isArms = targetLower.contains("arm")

    // 1. Search by session title match
    val matchedByName = workouts.firstOrNull { w ->
      val sName = w.session.name.lowercase()
      when {
        isPush -> sName.contains("push") || sName.contains("chest") || sName.contains("tricep")
        isPull -> sName.contains("pull") || sName.contains("back") || sName.contains("bicep")
        isLegs -> sName.contains("leg") || sName.contains("quad") || sName.contains("squat")
        isUpper -> sName.contains("upper") || sName.contains("chest") || sName.contains("back")
        isLower -> sName.contains("lower") || sName.contains("leg")
        isArms -> sName.contains("arm") || sName.contains("bicep") || sName.contains("delt")
        else -> sName.contains(targetLower.take(5))
      }
    }
    if (matchedByName != null) return matchedByName

    // 2. Search by muscle category of exercises inside the session
    val matchedByExercises = workouts.firstOrNull { w ->
      val categories = w.exercises.map { it.exercise.category.lowercase() }
      when {
        isPush -> categories.any { it.contains("chest") || it.contains("shoulder") || it.contains("tricep") }
        isPull -> categories.any { it.contains("back") || it.contains("bicep") }
        isLegs -> categories.any { it.contains("leg") || it.contains("quad") }
        isUpper -> categories.any { it.contains("chest") || it.contains("back") }
        isLower -> categories.any { it.contains("leg") }
        isArms -> categories.any { it.contains("arm") }
        else -> false
      }
    }
    if (matchedByExercises != null) return matchedByExercises

    // Fallback to most recent session
    return workouts.firstOrNull()
  }

  fun getTargetForExercise(exerciseName: String, splitName: String): OverloadTarget {
    // Check previous session of the targeted split
    val lastSplitWorkout = getPreviousWorkoutForSplit(splitName)
    val exerciseInLastSplit = lastSplitWorkout?.exercises?.find {
      it.exercise.exerciseName.equals(exerciseName, ignoreCase = true)
    }

    if (exerciseInLastSplit != null && exerciseInLastSplit.sets.isNotEmpty()) {
      val sets = exerciseInLastSplit.sets
      val maxWeight = sets.maxOfOrNull { it.weightKg } ?: 0.0
      val avgReps = (sets.map { it.reps }.average()).roundToInt().coerceAtLeast(1)
      val setCount = sets.size
      val nextRepTarget = avgReps + 1
      val nextWeightTarget = maxWeight + 2.5

      return OverloadTarget(
        exerciseName = exerciseName,
        lastWeightKg = maxWeight,
        lastReps = avgReps,
        lastSetsCount = setCount,
        targetRepsProgression = "$maxWeight kg × $nextRepTarget reps (+1 rep)",
        targetWeightProgression = "$nextWeightTarget kg × ${avgReps - 2}-${avgReps} reps (+2.5 kg)"
      )
    }

    // Check all workouts history for this exercise
    val allHistory = getExerciseHistory(exerciseName)
    if (allHistory.isNotEmpty()) {
      val lastPoint = allHistory.last()
      val maxWeight = lastPoint.topWeightKg
      val avgReps = (lastPoint.totalReps.toDouble() / lastPoint.totalSets.coerceAtLeast(1)).roundToInt().coerceAtLeast(1)
      val nextRepTarget = avgReps + 1
      val nextWeightTarget = maxWeight + 2.5

      return OverloadTarget(
        exerciseName = exerciseName,
        lastWeightKg = maxWeight,
        lastReps = avgReps,
        lastSetsCount = lastPoint.totalSets,
        targetRepsProgression = "$maxWeight kg × $nextRepTarget reps (+1 rep)",
        targetWeightProgression = "$nextWeightTarget kg × ${avgReps - 2}-${avgReps} reps (+2.5 kg)"
      )
    }

    // Baseline if fresh exercise
    return OverloadTarget(
      exerciseName = exerciseName,
      lastWeightKg = 0.0,
      lastReps = 0,
      lastSetsCount = 0,
      targetRepsProgression = "Baseline: 3 sets × 8-10 reps",
      targetWeightProgression = "Find working weight with 2 warm-up sets"
    )
  }

  fun getExercisesForSplit(splitName: String): List<String> {
    val lower = splitName.lowercase()
    return when {
      lower.contains("push") || lower.contains("chest") -> listOf(
        "Barbell Bench Press",
        "Incline Dumbbell Press",
        "Overhead Barbell Press",
        "Tricep Rope Pushdown",
        "Dips"
      )
      lower.contains("pull") || lower.contains("back") -> listOf(
        "Barbell Deadlift",
        "Lat Pulldown",
        "Seated Cable Row",
        "Barbell Bicep Curl",
        "Face Pulls"
      )
      lower.contains("leg") || lower.contains("quad") -> listOf(
        "Barbell Back Squat",
        "Romanian Deadlift",
        "Leg Press",
        "Leg Extension",
        "Standing Calf Raise"
      )
      lower.contains("upper") -> listOf(
        "Barbell Bench Press",
        "Lat Pulldown",
        "Overhead Barbell Press",
        "Barbell Bicep Curl",
        "Tricep Rope Pushdown"
      )
      lower.contains("arm") -> listOf(
        "Barbell Bicep Curl",
        "Incline Dumbbell Curl",
        "Tricep Rope Pushdown",
        "Skull Crushers",
        "Hammer Curl"
      )
      else -> listOf(
        "Barbell Bench Press",
        "Barbell Back Squat",
        "Barbell Deadlift",
        "Overhead Barbell Press"
      )
    }
  }

  fun getProbableSplitToday(): String {
    val last = getPreviousWorkout() ?: return "Push Day • Chest, Shoulders & Triceps"
    val name = last.session.name.lowercase()
    return when {
      name.contains("push") || name.contains("chest") -> "Pull Day • Back, Rear Delts & Biceps"
      name.contains("pull") || name.contains("back") -> "Leg Day • Quads, Hamstrings & Calves"
      name.contains("leg") || name.contains("quad") -> "Push Day • Chest, Shoulders & Triceps"
      name.contains("bicep") || name.contains("shoulder") -> "Legs & Core Overload"
      else -> "Upper Body Hypertrophy"
    }
  }

  fun getProgressiveOverloadTargets(previous: WorkoutWithExercises?): List<OverloadTarget> {
    if (previous == null) return emptyList()

    return previous.exercises.map { ex ->
      val sets = ex.sets
      val maxWeight = sets.maxOfOrNull { it.weightKg } ?: 0.0
      val avgReps = if (sets.isNotEmpty()) (sets.map { it.reps }.average()).roundToInt() else 8
      val setCount = sets.size

      val nextRepTarget = avgReps + 1
      val nextWeightTarget = (maxWeight + 2.5)

      OverloadTarget(
        exerciseName = ex.exercise.exerciseName,
        lastWeightKg = maxWeight,
        lastReps = avgReps,
        lastSetsCount = setCount,
        targetRepsProgression = "$maxWeight kg × $nextRepTarget reps (+1 rep)",
        targetWeightProgression = "$nextWeightTarget kg × ${avgReps - 2}-${avgReps} reps (+2.5 kg)"
      )
    }
  }

  // -------------------------------------------------------------
  // METRIC 1: PROGRESSIVE OVERLOAD PER EXERCISE GRAPH
  // -------------------------------------------------------------
  fun getExerciseHistory(exerciseName: String): List<ExerciseSessionPoint> {
    val workouts = allWorkouts.value.sortedBy { it.session.startTimeMillis }
    val points = mutableListOf<ExerciseSessionPoint>()
    var prevTopWeight = 0.0

    workouts.forEach { w ->
      val matchingEx = w.exercises.find { it.exercise.exerciseName.equals(exerciseName, ignoreCase = true) }
      if (matchingEx != null && matchingEx.sets.isNotEmpty()) {
        val topWeight = matchingEx.sets.maxOfOrNull { it.weightKg } ?: 0.0
        val totalReps = matchingEx.sets.sumOf { it.reps }
        val setCount = matchingEx.sets.size
        val topSet = matchingEx.sets.maxByOrNull { it.weightKg }
        val est1Rm = if (topSet != null) repository.calculate1Rm(topSet.weightKg, topSet.reps) else 0.0

        val isOverload = topWeight > prevTopWeight || (topWeight == prevTopWeight && totalReps > (points.lastOrNull()?.totalReps ?: 0))

        points.add(
          ExerciseSessionPoint(
            dateMillis = w.session.startTimeMillis,
            sessionName = w.session.name,
            topWeightKg = topWeight,
            totalReps = totalReps,
            totalSets = setCount,
            estimated1Rm = est1Rm,
            isOverloadComparedToPrevious = isOverload && prevTopWeight > 0
          )
        )
        if (topWeight > 0) prevTopWeight = topWeight
      }
    }
    return points
  }

  // -------------------------------------------------------------
  // METRIC 3: PLATEAU & FORM ANALYSIS (PAST 2-3 WEEKS)
  // -------------------------------------------------------------
  fun getPlateauInsights(): List<PlateauInsight> {
    val now = System.currentTimeMillis()
    val threeWeeksAgo = now - 21 * 86400000L
    val recentWorkouts = allWorkouts.value.filter { it.session.startTimeMillis >= threeWeeksAgo }

    val exerciseAppearances = mutableMapOf<String, MutableList<Double>>()
    recentWorkouts.forEach { w ->
      w.exercises.forEach { ex ->
        val maxWeight = ex.sets.maxOfOrNull { it.weightKg } ?: 0.0
        if (maxWeight > 0) {
          exerciseAppearances.getOrPut(ex.exercise.exerciseName) { mutableListOf() }.add(maxWeight)
        }
      }
    }

    val plateaus = mutableListOf<PlateauInsight>()
    exerciseAppearances.forEach { (name, weights) ->
      if (weights.size >= 2) {
        val lastWeights = weights.takeLast(3)
        val isStalled = lastWeights.all { it == lastWeights[0] }
        if (isStalled) {
          val stalledWt = lastWeights[0]
          val formCue = getFormFixCue(name)
          val accessory = getAccessoryRecommendation(name)
          plateaus.add(
            PlateauInsight(
              exerciseName = name,
              stalledWeightKg = stalledWt,
              sessionCount = lastWeights.size,
              formFixCue = formCue,
              recommendedAccessory = accessory
            )
          )
        }
      }
    }

    if (plateaus.isEmpty()) {
      val primaryEx = exerciseAppearances.keys.firstOrNull() ?: "Barbell Bench Press"
      plateaus.add(
        PlateauInsight(
          exerciseName = primaryEx,
          stalledWeightKg = exerciseAppearances[primaryEx]?.lastOrNull() ?: 80.0,
          sessionCount = 2,
          formFixCue = getFormFixCue(primaryEx),
          recommendedAccessory = getAccessoryRecommendation(primaryEx)
        )
      )
    }

    return plateaus
  }

  private fun getFormFixCue(name: String): String {
    val lower = name.lowercase()
    return when {
      lower.contains("bench") ->
        "Retract and depress scapulae into the pad. Maintain leg drive with feet rooted flat. Tuck elbows at ~45-60° to eliminate anterior shoulder shear and recruit sternal pecs."
      lower.contains("squat") ->
        "Brace with 360° intra-abdominal pressure before descent. Push knees outward in line with toes, and ensure hips and chest ascend at identical tempo out of the hole."
      lower.contains("deadlift") ->
        "Pull the slack out of the barbell until your lats engage before lifting. Wedge hips forward and drive the floor away through mid-foot rather than hyperextending lumbar."
      lower.contains("overhead") || lower.contains("ohp") || lower.contains("shoulder") ->
        "Squeeze glutes and brace core to prevent lumbar arch. Keep forearms strictly vertical under the bar and push head through the window once bar clears forehead."
      lower.contains("incline") ->
        "Set bench to 30° incline to isolate clavicular head. Pause 1s at bottom stretch to eliminate bounce momentum and trigger deeper stretch-mediated hypertrophy."
      lower.contains("lat pulldown") || lower.contains("pull down") ->
        "Depress shoulder blades before pulling with arms. Pull elbows down and back towards your hip crease with a controlled 2s eccentric negative."
      lower.contains("row") ->
        "Hinge at 45° with neutral spine. Pull through the elbows into lower ribs, holding peak scapular squeeze for 1 second."
      lower.contains("lateral raise") ->
        "Slight forward torso lean (10-15°). Lead with elbows in the scapular plane with thumbs slightly lower than pinkies to directly isolate lateral delts."
      lower.contains("curl") ->
        "Keep elbows pinned to your sides. Supinate wrists hard at the top contraction and take 3 seconds to lower the weight under complete tension."
      else ->
        "Slow down eccentric phase to 3 seconds. Eliminate momentum and emphasize full range of motion with a 1-second pause at maximum stretch."
    }
  }

  private fun getAccessoryRecommendation(name: String): String {
    val lower = name.lowercase()
    return when {
      lower.contains("bench") -> "Spoto Press (pause 1-inch off chest) & Close-Grip Bench"
      lower.contains("squat") -> "Pause Squats & Bulgarian Split Squats for quad drive"
      lower.contains("deadlift") -> "Deficit Deadlifts & Romanian Deadlifts for hamstring/hip hinge"
      lower.contains("overhead") || lower.contains("shoulder") -> "Seated Pin Press & Heavy Dumbbell Lateral Raises"
      lower.contains("incline") -> "Weighted Dips & Low-to-High Cable Flyes"
      lower.contains("lat pulldown") || lower.contains("row") -> "Single-Arm Dumbbell Row & Straight-Arm Cable Pullover"
      else -> "Rest-pause sets or a 10% deload drop set to accumulate clean volume"
    }
  }

  // -------------------------------------------------------------
  // HELPER METHODS FOR 1RM & PRs
  // -------------------------------------------------------------
  fun calculate1Rm(weightKg: Double, reps: Int): Double {
    return repository.calculate1Rm(weightKg, reps)
  }

  fun recordCustomPr(exerciseName: String, weightKg: Double, reps: Int) {
    viewModelScope.launch {
      repository.recordCustomPr(exerciseName, weightKg, reps)
    }
  }

  // -------------------------------------------------------------
  // CALENDAR HELPER METHODS
  // -------------------------------------------------------------
  private val _monthOffset = MutableStateFlow(0)
  val monthOffset: StateFlow<Int> = _monthOffset.asStateFlow()

  private val _selectedCalendarDateMillis = MutableStateFlow<Long?>(null)
  val selectedCalendarDateMillis: StateFlow<Long?> = _selectedCalendarDateMillis.asStateFlow()

  fun prevMonth() {
    _monthOffset.value--
  }

  fun nextMonth() {
    _monthOffset.value++
  }

  fun selectCalendarDate(dateMillis: Long?) {
    _selectedCalendarDateMillis.value = dateMillis
  }

  fun getMonthCalendarInfo(offset: Int): Pair<Calendar, List<DailyWorkoutSummary>> {
    val cal = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MONTH, offset)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val summaries = (1..daysInMonth).map { day ->
      val dayCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
      val start = dayCal.timeInMillis
      val end = start + 86400000L - 1
      val matchingWorkouts = allWorkouts.value.filter { it.session.startTimeMillis in start..end }

      DailyWorkoutSummary(
        dateMillis = start,
        dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dayCal.time),
        workoutCount = matchingWorkouts.size,
        totalVolumeKg = matchingWorkouts.sumOf { it.session.totalVolumeKg },
        workoutNames = matchingWorkouts.map { it.session.name }
      )
    }

    return Pair(cal, summaries)
  }

  fun computeStreaks(workouts: List<WorkoutWithExercises>): Pair<Int, Int> {
    if (workouts.isEmpty()) return Pair(0, 0)
    val sortedDates = workouts.map {
      val c = Calendar.getInstance().apply {
        timeInMillis = it.session.startTimeMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      c.timeInMillis
    }.distinct().sorted()

    var maxStreak = 1
    var currStreak = 1

    for (i in 1 until sortedDates.size) {
      val diffDays = (sortedDates[i] - sortedDates[i - 1]) / 86400000L
      if (diffDays == 1L) {
        currStreak++
        if (currStreak > maxStreak) maxStreak = currStreak
      } else if (diffDays > 1L) {
        currStreak = 1
      }
    }

    return Pair(currStreak, maxStreak)
  }
}

class GymViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(GymViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return GymViewModel(application) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
