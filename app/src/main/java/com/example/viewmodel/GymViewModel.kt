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
import com.example.model.DailyWorkoutSummary
import com.example.model.ExercisePr
import com.example.model.ParsedWorkoutRant
import com.example.model.RoutineTemplate
import com.example.model.WorkoutWithExercises
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GymViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: GymRepository
  private val geminiService = GeminiService()
  private val networkMonitor = NetworkMonitor(application)

  val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.checkCurrentOnline())

  init {
    val database = GymDatabase.getDatabase(application, viewModelScope)
    repository = GymRepository(
      database.workoutDao(),
      database.exercisePrDao(),
      database.routineDao()
    )
  }

  // Completed workout history (all logged sessions)
  val allWorkouts: StateFlow<List<WorkoutWithExercises>> = repository.allWorkouts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Personal Records
  val allPrs: StateFlow<List<ExercisePr>> = repository.allPrs
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Routine templates and custom splits
  val allRoutines: StateFlow<List<RoutineTemplate>> = repository.allRoutines
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Unit toggle: kg vs lbs
  private val _useLbs = MutableStateFlow(false)
  val useLbs: StateFlow<Boolean> = _useLbs.asStateFlow()

  fun toggleUnits() {
    _useLbs.value = !_useLbs.value
  }

  // Rant parsing state
  private val _isParsingRant = MutableStateFlow(false)
  val isParsingRant: StateFlow<Boolean> = _isParsingRant.asStateFlow()

  private val _parsedRant = MutableStateFlow<ParsedWorkoutRant?>(null)
  val parsedRant: StateFlow<ParsedWorkoutRant?> = _parsedRant.asStateFlow()

  // AI Progress Analysis state
  private val _isAnalyzing = MutableStateFlow(false)
  val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

  private val _aiAnalysis = MutableStateFlow<AiProgressAnalysis?>(null)
  val aiAnalysis: StateFlow<AiProgressAnalysis?> = _aiAnalysis.asStateFlow()

  // Selected calendar day for inspection
  private val _selectedCalendarDateMillis = MutableStateFlow<Long?>(null)
  val selectedCalendarDateMillis: StateFlow<Long?> = _selectedCalendarDateMillis.asStateFlow()

  // Month offset for calendar view (0 = current month, -1 = previous, etc.)
  private val _monthOffset = MutableStateFlow(0)
  val monthOffset: StateFlow<Int> = _monthOffset.asStateFlow()

  fun nextMonth() {
    _monthOffset.value += 1
  }

  fun prevMonth() {
    _monthOffset.value -= 1
  }

  fun selectCalendarDate(dateMillis: Long?) {
    _selectedCalendarDateMillis.value = dateMillis
  }

  init {
    // Automatically trigger initial local analysis when workouts load
    viewModelScope.launch {
      allWorkouts.collect { workouts ->
        if (workouts.isNotEmpty() && _aiAnalysis.value == null) {
          _aiAnalysis.value = OfflineProgressAnalyzer.analyze(workouts, allPrs.value)
        }
      }
    }
  }

  fun parseGymRant(rantText: String) {
    if (rantText.isBlank()) return
    viewModelScope.launch {
      _isParsingRant.value = true
      try {
        val result = geminiService.parseGymRant(rantText, isOnline.value)
        _parsedRant.value = result
      } finally {
        _isParsingRant.value = false
      }
    }
  }

  fun clearParsedRant() {
    _parsedRant.value = null
  }

  fun saveLoggedWorkout(rant: ParsedWorkoutRant) {
    viewModelScope.launch {
      repository.saveLoggedWorkout(rant)
      _parsedRant.value = null
      // Re-trigger analysis after new workout
      runProgressAnalysis()
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

  fun saveCustomSplit(title: String, description: String, exercisesCsv: String) {
    viewModelScope.launch {
      repository.saveCustomSplit(title, description, exercisesCsv)
    }
  }

  fun deleteWorkout(workoutId: Long) {
    viewModelScope.launch {
      repository.deleteWorkout(workoutId)
    }
  }

  fun recordCustomPr(exerciseName: String, weight: Double, reps: Int) {
    viewModelScope.launch {
      repository.recordCustomPr(exerciseName, weight, reps)
    }
  }

  fun deletePr(exerciseName: String) {
    viewModelScope.launch {
      repository.deletePr(exerciseName)
    }
  }

  fun calculate1Rm(weight: Double, reps: Int): Double {
    return repository.calculate1Rm(weight, reps)
  }

  // Calendar metrics: Current month start and days
  fun getMonthCalendarInfo(offset: Int): Pair<Calendar, List<DailyWorkoutSummary>> {
    val cal = Calendar.getInstance().apply {
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.MONTH, offset)
    }

    val monthStart = cal.timeInMillis
    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val workouts = allWorkouts.value

    val summaries = mutableListOf<DailyWorkoutSummary>()
    val dayCal = Calendar.getInstance()

    for (d in 1..maxDays) {
      cal.set(Calendar.DAY_OF_MONTH, d)
      val dayStart = cal.timeInMillis
      val dayEnd = dayStart + 86400000L - 1

      val matchingWorkouts = workouts.filter {
        it.session.startTimeMillis in dayStart..dayEnd
      }

      val vol = matchingWorkouts.sumOf { it.session.totalVolumeKg }
      val names = matchingWorkouts.map { it.session.name }

      summaries.add(
        DailyWorkoutSummary(
          dateMillis = dayStart,
          dateString = dateFormat.format(Date(dayStart)),
          workoutCount = matchingWorkouts.size,
          totalVolumeKg = vol,
          workoutNames = names
        )
      )
    }

    return Pair(cal, summaries)
  }

  // Calculate streaks
  fun computeStreaks(workouts: List<WorkoutWithExercises>): Pair<Int, Int> {
    if (workouts.isEmpty()) return Pair(0, 0)

    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sortedDates = workouts.map {
      dayFormat.format(Date(it.session.startTimeMillis))
    }.distinct().sortedDescending()

    val cal = Calendar.getInstance()
    val todayStr = dayFormat.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = dayFormat.format(cal.time)

    var currentStreak = 0
    var checkDate = Calendar.getInstance()

    // Determine if today or yesterday was trained to start streak
    val startsToday = sortedDates.contains(todayStr)
    val startsYesterday = sortedDates.contains(yesterdayStr)

    if (startsToday || startsYesterday) {
      if (!startsToday) {
        checkDate.add(Calendar.DAY_OF_YEAR, -1)
      }
      while (true) {
        val dateStr = dayFormat.format(checkDate.time)
        if (sortedDates.contains(dateStr)) {
          currentStreak++
          checkDate.add(Calendar.DAY_OF_YEAR, -1)
        } else {
          break
        }
      }
    }

    // Longest streak
    var maxStreak = currentStreak
    var tempStreak = 0
    // Simple contiguous check
    val allUniqueDates = workouts.map {
      val c = Calendar.getInstance().apply { timeInMillis = it.session.startTimeMillis }
      c.set(Calendar.HOUR_OF_DAY, 0)
      c.set(Calendar.MINUTE, 0)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MILLISECOND, 0)
      c.timeInMillis
    }.distinct().sorted()

    for (i in 0 until allUniqueDates.size) {
      if (i == 0) {
        tempStreak = 1
      } else {
        val diff = allUniqueDates[i] - allUniqueDates[i - 1]
        if (diff in 86000000L..86800000L) {
          tempStreak++
        } else {
          tempStreak = 1
        }
      }
      if (tempStreak > maxStreak) maxStreak = tempStreak
    }

    return Pair(currentStreak, maxStreak)
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
