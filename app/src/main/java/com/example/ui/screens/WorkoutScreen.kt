package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Description
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ExerciseDefinition
import com.example.model.ParsedExerciseLog
import com.example.model.ParsedSetLog
import com.example.model.ParsedWorkoutRant
import com.example.model.WorkoutWithExercises
import com.example.ui.components.AddCustomExerciseDialog
import com.example.ui.components.ExerciseSelectorDialog
import com.example.ui.components.MinimalDumbbellIcon
import com.example.ui.components.RepWheelPicker
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.PlatinumSteel
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite
import com.example.viewmodel.ActiveExerciseLog
import com.example.viewmodel.GymViewModel
import com.example.viewmodel.OverloadTarget
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekDayAdherenceItem(
  val letter: String,
  val name: String,
  val dayNumber: Int,
  val trained: Boolean,
  val today: Boolean,
  val sets: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val workouts by viewModel.allWorkouts.collectAsState()
  val activeExercises by viewModel.activeExercises.collectAsState()
  val isParsingRant by viewModel.isParsingRant.collectAsState()
  val parsedRants by viewModel.parsedRants.collectAsState()
  val parsedRant by viewModel.parsedRant.collectAsState()
  val clarifications by viewModel.clarifications.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()

  val userProfile by viewModel.userProfile.collectAsState()
  val dashboardPrefs by viewModel.dashboardPrefs.collectAsState()
  val allAvailableExercises = viewModel.getAllExercises()

  val probableSplit = remember(workouts) { viewModel.getProbableSplitToday() }
  var customTodaySplit by remember { mutableStateOf<String?>(null) }
  val currentActiveSplit = customTodaySplit ?: probableSplit
  val previousWorkoutForCurrentSplit = remember(workouts, currentActiveSplit) {
    viewModel.getPreviousWorkoutForSplit(currentActiveSplit)
  }
  val overloadTargets = remember(previousWorkoutForCurrentSplit) {
    viewModel.getProgressiveOverloadTargets(previousWorkoutForCurrentSplit)
  }
  val splitExercises = remember(currentActiveSplit) {
    viewModel.getExercisesForSplit(currentActiveSplit)
  }
  var showSplitPicker by remember { mutableStateOf(false) }

  // Logging Method State ("SET_LOGGER" vs "RAMBLER")
  val selectedLoggingMethod by viewModel.selectedLoggingMethod.collectAsState()

  // Rest Timer State
  val restTimerRemaining by viewModel.restTimerSecondsRemaining.collectAsState()
  val isRestTimerActive by viewModel.isRestTimerActive.collectAsState()

  // Exercise Logger States
  var selectedExerciseName by remember { mutableStateOf("Barbell Bench Press") }
  var selectedWeightKg by remember { mutableDoubleStateOf(80.0) }
  var selectedReps by remember { mutableIntStateOf(8) }
  var hasFractionalRep by remember { mutableStateOf(false) } // +0.5 rep failure point
  var isUnilateralMode by remember { mutableStateOf(false) }
  var selectedSide by remember { mutableStateOf("BOTH") } // "BOTH", "LEFT", "RIGHT"
  var selectedBiofeedbackTags by remember { mutableStateOf<Set<String>>(emptySet()) }
  var selectedTempo by remember { mutableStateOf("") }
  var selectedFailurePoint by remember { mutableStateOf("") }
  var isDropSetExpanded by remember { mutableStateOf(false) }
  var dropWeightKg by remember { mutableDoubleStateOf(0.0) }
  var dropReps by remember { mutableDoubleStateOf(4.0) }
  var isExerciseSearchOpen by remember { mutableStateOf(false) }
  var showAddCustomExerciseDialog by remember { mutableStateOf(false) }

  // Inspecting active or completed exercises
  var inspectingActiveExerciseIndex by remember { mutableStateOf<Int?>(null) }
  var inspectingWorkoutExercise by remember {
    mutableStateOf<Pair<com.example.model.ExerciseWithSets, Long>?>(null)
  }

  var showDirectWeightDialog by remember { mutableStateOf(false) }
  var showRamblerDialog by remember { mutableStateOf(false) }
  var showSetLoggerDialog by remember { mutableStateOf(false) }
  var ramblerInput by remember { mutableStateOf("") }

  // Streak calculation
  val (currStreak, _) = remember(workouts) { viewModel.computeStreaks(workouts) }

  // DIALOGS
  if (parsedRants.isNotEmpty()) {
    MultiWorkoutRamblerDialog(
      rants = parsedRants,
      clarifications = clarifications,
      useLbs = useLbs,
      onToggleUnits = { viewModel.toggleUnits() },
      onUpdateClarification = { idx, wt, reps ->
        viewModel.updateClarification(idx, wt, reps)
      },
      onUpdateRant = { idx, updated ->
        viewModel.updateRantAt(idx, updated)
      },
      onRemoveRant = { idx ->
        viewModel.removeRantAt(idx)
      },
      onConfirmAll = { confirmedList ->
        viewModel.saveAllLoggedWorkouts(confirmedList)
        ramblerInput = ""
      },
      onDismiss = { viewModel.clearParsedRant() }
    )
  }

  if (showDirectWeightDialog) {
    val displayVal = if (useLbs) (selectedWeightKg * 2.20462) else selectedWeightKg
    val initialText = if (displayVal % 1.0 == 0.0) displayVal.toInt().toString() else "%.1f".format(displayVal)
    var weightInputText by remember { mutableStateOf(initialText) }

    AlertDialog(
      onDismissRequest = { showDirectWeightDialog = false },
      containerColor = CardElevated,
      titleContentColor = TitaniumWhite,
      title = {
        Text(
          text = if (useLbs) "SET WEIGHT (LBS)" else "SET WEIGHT (KG)",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          letterSpacing = 1.sp
        )
      },
      text = {
        Column {
          Text(
            text = "Enter precise load for $selectedExerciseName:",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = weightInputText,
            onValueChange = { weightInputText = it },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardDark,
              unfocusedContainerColor = CardDark,
              focusedBorderColor = TitaniumWhite,
              unfocusedBorderColor = BorderSubtle,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val num = weightInputText.toDoubleOrNull()
            if (num != null && num >= 0) {
              selectedWeightKg = if (useLbs) (num / 2.20462) else num
            }
            showDirectWeightDialog = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = TitaniumWhite,
            contentColor = MatteBlack
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("APPLY", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDirectWeightDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  if (showSplitPicker) {
    SplitPickerDialog(
      currentSplit = customTodaySplit ?: probableSplit,
      onSelectSplit = {
        customTodaySplit = it
        showSplitPicker = false
      },
      onDismiss = { showSplitPicker = false }
    )
  }

  if (isExerciseSearchOpen) {
    ExerciseSelectorDialog(
      allAvailableExercises = allAvailableExercises,
      onSelectExercise = { def ->
        selectedExerciseName = def.name
        isExerciseSearchOpen = false
      },
      onAddNewCustomExercise = {
        isExerciseSearchOpen = false
        showAddCustomExerciseDialog = true
      },
      onDismiss = { isExerciseSearchOpen = false }
    )
  }

  if (showAddCustomExerciseDialog) {
    AddCustomExerciseDialog(
      onDismiss = { showAddCustomExerciseDialog = false },
      onSave = { name, category, muscle ->
        val ok = viewModel.addCustomExercise(name, category, muscle)
        if (ok) {
          selectedExerciseName = name
        }
        ok
      }
    )
  }

  inspectingActiveExerciseIndex?.let { index ->
    if (index in activeExercises.indices) {
      ActiveExerciseDetailDialog(
        exercise = activeExercises[index],
        useLbs = useLbs,
        onDeleteExercise = {
          viewModel.removeExerciseFromActiveSession(index)
          inspectingActiveExerciseIndex = null
        },
        onDeleteSet = { setIndex ->
          viewModel.removeSetFromActiveSession(index, setIndex)
        },
        onDismiss = { inspectingActiveExerciseIndex = null }
      )
    } else {
      inspectingActiveExerciseIndex = null
    }
  }

  inspectingWorkoutExercise?.let { (exerciseWithSets, sessionId) ->
    CompletedExerciseDetailDialog(
      exerciseWithSets = exerciseWithSets,
      useLbs = useLbs,
      onDeleteSet = { setId ->
        viewModel.deleteSetFromWorkout(setId, sessionId)
      },
      onDeleteExercise = {
        viewModel.deleteExerciseFromWorkout(exerciseWithSets.exercise.id, sessionId)
        inspectingWorkoutExercise = null
      },
      onDismiss = { inspectingWorkoutExercise = null }
    )
  }

  if (showRamblerDialog) {
    Dialog(
      onDismissRequest = { showRamblerDialog = false },
      properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = false,
        dismissOnClickOutside = false
      )
    ) {
      RamblerInputCard(
        isOnline = isOnline,
        isGeminiConfigured = viewModel.isGeminiConfigured,
        initialApiKey = viewModel.getGeminiApiKey(),
        onSaveGeminiKey = { key -> viewModel.setGeminiApiKey(key) },
        isParsingRant = isParsingRant,
        onParseRant = { text ->
          showRamblerDialog = false
          viewModel.parseGymRant(text)
        },
        onDismiss = { showRamblerDialog = false }
      )
    }
  }

  if (showSetLoggerDialog) {
    QuickSetLoggerDialog(
      allAvailableExercises = allAvailableExercises,
      splitExercises = splitExercises,
      selectedExerciseName = selectedExerciseName,
      onSelectExercise = { exName ->
        selectedExerciseName = exName
        val tgt = viewModel.getTargetForExercise(exName, currentActiveSplit)
        if (tgt.lastWeightKg > 0) {
          selectedWeightKg = tgt.lastWeightKg
          selectedReps = tgt.lastReps.coerceAtLeast(1)
        }
      },
      selectedWeightKg = selectedWeightKg,
      onWeightChange = { selectedWeightKg = it },
      selectedReps = selectedReps,
      onRepsChange = { selectedReps = it },
      hasFractionalRep = hasFractionalRep,
      onFractionalRepChange = { hasFractionalRep = it },
      isUnilateralMode = isUnilateralMode,
      onUnilateralModeChange = { isUnilateralMode = it },
      selectedSide = selectedSide,
      onSideChange = { selectedSide = it },
      selectedBiofeedbackTags = selectedBiofeedbackTags,
      onToggleBiofeedbackTag = { tag ->
        selectedBiofeedbackTags = if (selectedBiofeedbackTags.contains(tag)) {
          selectedBiofeedbackTags - tag
        } else {
          selectedBiofeedbackTags + tag
        }
      },
      selectedTempo = selectedTempo,
      onTempoChange = { selectedTempo = it },
      selectedFailurePoint = selectedFailurePoint,
      onFailurePointChange = { selectedFailurePoint = it },
      useLbs = useLbs,
      onOpenDirectWeightDialog = { showDirectWeightDialog = true },
      onOpenExerciseSearch = { isExerciseSearchOpen = true },
      onOpenAddCustomExercise = { showAddCustomExerciseDialog = true },
      currentExerciseTarget = viewModel.getTargetForExercise(selectedExerciseName, currentActiveSplit),
      onMatchLastTarget = { wt, reps ->
        selectedWeightKg = wt
        selectedReps = reps
      },
      onLogSet = {
        val finalReps = if (hasFractionalRep) selectedReps + 0.5 else selectedReps.toDouble()
        viewModel.addSetToActiveSession(
          exerciseName = selectedExerciseName,
          weightKg = selectedWeightKg,
          reps = finalReps,
          setKind = "NORMAL",
          side = if (isUnilateralMode) selectedSide else "BOTH",
          biofeedbackTags = selectedBiofeedbackTags.toList(),
          tempo = selectedTempo,
          failurePoint = selectedFailurePoint,
          isUnilateral = isUnilateralMode
        )
        showSetLoggerDialog = false
      },
      onDismiss = { showSetLoggerDialog = false }
    )
  }

  val todayDayFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
  val todayDateFormat = remember { SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()) }
  val now = remember { Date() }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MatteBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // -------------------------------------------------------------
    // TOP SECTION: SLEEK ATHLETIC HEADER & REST TIMER
    // -------------------------------------------------------------
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = todayDayFormat.format(now).uppercase(),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.sp,
              color = PlatinumSteel
            )
            Text(
              text = todayDateFormat.format(now),
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
          }

          // Streak & MacroFactor Consistency Badge
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "🔥 ${currStreak.coerceAtLeast(1)}D",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Black,
                  color = TitaniumWhite
                )
              }
            }
          }
        }

        // MacroFactor-Style Weekly Consistency & Volume Bar
        val weekAdherence = remember(workouts) {
          val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          }
          val todayCal = Calendar.getInstance()
          val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
          val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

          (0..6).map { offset ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val startMs = dayCal.timeInMillis
            val endMs = startMs + 86400000L
            val dayWorkouts = workouts.filter { it.session.startTimeMillis in startMs until endMs }
            val isTrained = dayWorkouts.isNotEmpty()
            val isToday = dayCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) &&
                          dayCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
            val daySets = dayWorkouts.sumOf { it.session.totalSets }
            WeekDayAdherenceItem(
              letter = dayLetters[offset],
              name = dayNames[offset],
              dayNumber = dayCal.get(Calendar.DAY_OF_MONTH),
              trained = isTrained,
              today = isToday,
              sets = daySets
            )
          }
        }
        val trainedDaysCount = remember(weekAdherence) { weekAdherence.count { it.trained } }
        val weekSetsCount = remember(weekAdherence) { weekAdherence.sumOf { it.sets } }

        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SurfaceDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth().testTag("workout_weekly_calendar_card")
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "WEEKLY CONSISTENCY",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.2.sp,
                  color = PlatinumSteel
                )
                Text(
                  text = "$trainedDaysCount of 7 Days Logged",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
              }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight)
              ) {
                Text(
                  text = "$weekSetsCount Sets This Week",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF05DF72),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-day pill row (Aesthetic MacroFactor design)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              weekAdherence.forEach { day ->
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.weight(1f)
                ) {
                  Text(
                    text = day.letter,
                    fontSize = 11.sp,
                    fontWeight = if (day.today) FontWeight.Black else FontWeight.SemiBold,
                    color = if (day.today) TitaniumWhite else TextSecondary
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Box(
                    modifier = Modifier
                      .size(width = 32.dp, height = 26.dp)
                      .background(
                        color = if (day.trained) Color(0xFF05DF72) else CardElevated,
                        shape = RoundedCornerShape(8.dp)
                      )
                      .border(
                        width = if (day.today) 1.5.dp else 0.5.dp,
                        color = if (day.today) TitaniumWhite else BorderSubtle,
                        shape = RoundedCornerShape(8.dp)
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    if (day.trained) {
                      Icon(
                        Icons.Default.Check,
                        contentDescription = "Trained",
                        tint = MatteBlack,
                        modifier = Modifier.size(13.dp)
                      )
                    } else {
                      Text(
                        text = "${day.dayNumber}",
                        fontSize = 10.sp,
                        fontWeight = if (day.today) FontWeight.Bold else FontWeight.Normal,
                        color = if (day.today) TitaniumWhite else TextSecondary
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Daily Split Banner (if enabled)
        if (dashboardPrefs.showSplitBanner) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth().testTag("today_plan_banner")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "TODAY'S SPLIT",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 1.sp,
                  color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = customTodaySplit ?: probableSplit,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
              }

              Surface(
                onClick = { showSplitPicker = true },
                shape = RoundedCornerShape(8.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.testTag("switch_split_button")
              ) {
                Text(
                  text = "Switch",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  color = TitaniumSilver,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // PROGRESSIVE OVERLOAD TARGETS (Configurable via Settings)
    // -------------------------------------------------------------
    if (dashboardPrefs.showOverloadTargets && previousWorkoutForCurrentSplit != null) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth().testTag("previous_workout_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "LAST SESSION TARGETS • ${currentActiveSplit.uppercase()}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = TitaniumSilver
                )
                Text(
                  text = previousWorkoutForCurrentSplit.session.name,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }

              val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(previousWorkoutForCurrentSplit.session.startTimeMillis))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
              ) {
                Text(
                  text = dateStr,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TitaniumSilver,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              overloadTargets.take(3).forEach { target ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = CardDark,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      selectedExerciseName = target.exerciseName
                      if (target.lastWeightKg > 0) {
                        selectedWeightKg = target.lastWeightKg
                        selectedReps = target.lastReps.coerceAtLeast(1)
                      }
                    }
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = target.exerciseName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                      )
                      Text(
                        text = "Last: ${target.lastWeightKg.toInt()}kg × ${target.lastReps} reps",
                        fontSize = 11.sp,
                        color = TextSecondary
                      )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF191C24),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderHighlight),
                        modifier = Modifier.weight(1f)
                      ) {
                        Text(
                          text = "Goal A: ${target.targetRepsProgression}",
                          fontSize = 10.sp,
                          color = TitaniumSilver,
                          fontWeight = FontWeight.Medium,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                      }
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF191C24),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderHighlight),
                        modifier = Modifier.weight(1f)
                      ) {
                        Text(
                          text = "Goal B: ${target.targetWeightProgression}",
                          fontSize = 10.sp,
                          color = TitaniumSilver,
                          fontWeight = FontWeight.Medium,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // LOG WORKOUT (MINIMAL INTERFACE)
    // -------------------------------------------------------------
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("minimal_log_exercise_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "LOG WORKOUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = PlatinumSteel
              )
              Text(
                text = "Voice rant or set-by-set entry",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TitaniumWhite
              )
            }

            Surface(
              onClick = { showAddCustomExerciseDialog = true },
              shape = RoundedCornerShape(8.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
              modifier = Modifier.testTag("inbuilt_add_exercise_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Custom",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // The Rambler Button
            Surface(
              onClick = { showRamblerDialog = true },
              shape = RoundedCornerShape(12.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
              modifier = Modifier
                .weight(1f)
                .testTag("open_rambler_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.Notes,
                    contentDescription = null,
                    tint = TitaniumWhite,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text("The Rambler", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TitaniumWhite)
                  Text("Voice & Notes", fontSize = 10.sp, color = TextSecondary)
                }
              }
            }

            // Quick Set Logger Button
            Surface(
              onClick = { showSetLoggerDialog = true },
              shape = RoundedCornerShape(12.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier
                .weight(1f)
                .testTag("open_set_logger_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark),
                  contentAlignment = Alignment.Center
                ) {
                  MinimalDumbbellIcon(
                    tint = TitaniumWhite,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text("Log Set", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TitaniumWhite)
                  Text("Pick & Record", fontSize = 10.sp, color = TextSecondary)
                }
              }
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // CURRENT ACTIVE WORKOUT SESSION (IF SETS LOGGED)
    // -------------------------------------------------------------
    if (activeExercises.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = CardElevated),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderHighlight),
          modifier = Modifier.fillMaxWidth().testTag("active_workout_session_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "CURRENT SESSION IN PROGRESS",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = TitaniumSilver
                )
                val totalSets = activeExercises.sumOf { it.sets.size }
                Text(
                  text = "${activeExercises.size} Exercises • $totalSets Sets Logged",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }

              Surface(
                onClick = { viewModel.clearActiveSession() },
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
              ) {
                Text(
                  text = "Discard",
                  fontSize = 11.sp,
                  color = Color(0xFFEF4444),
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              activeExercises.forEachIndexed { exIndex, ex ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = CardDark,
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    // Exercise Header
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                          text = ex.exerciseName,
                          fontSize = 15.sp,
                          fontWeight = FontWeight.Bold,
                          color = TextPrimary
                        )
                        if (ex.isUnilateral) {
                          Spacer(modifier = Modifier.width(6.dp))
                          Box(
                            modifier = Modifier
                              .clip(RoundedCornerShape(4.dp))
                              .background(TitaniumWhite.copy(alpha = 0.15f))
                              .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                            Text(
                              text = "UNILATERAL",
                              fontSize = 8.sp,
                              fontWeight = FontWeight.Bold,
                              color = TitaniumWhite
                            )
                          }
                        }
                      }

                      IconButton(
                        onClick = { viewModel.removeExerciseFromActiveSession(exIndex) },
                        modifier = Modifier.size(26.dp)
                      ) {
                        Icon(
                          Icons.Default.Delete,
                          contentDescription = "Delete Exercise",
                          tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Individual sets with granular details and quick delete
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                      ex.sets.forEachIndexed { sIndex, s ->
                        val wt = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
                        val repsFormatted = if (s.reps % 1.0 == 0.0) s.reps.toInt().toString() else s.reps.toString()
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = CardElevated,
                          border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                          modifier = Modifier.fillMaxWidth()
                        ) {
                          Row(
                            modifier = Modifier
                              .fillMaxWidth()
                              .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Column(modifier = Modifier.weight(1f)) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                  text = "S${s.setNumber}:  $wt ${if (useLbs) "lbs" else "kg"} × $repsFormatted reps",
                                  fontSize = 13.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = TitaniumWhite
                                )

                                if (s.side == "LEFT") {
                                  Spacer(modifier = Modifier.width(6.dp))
                                  Text(
                                    text = "[L]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                  )
                                } else if (s.side == "RIGHT") {
                                  Spacer(modifier = Modifier.width(6.dp))
                                  Text(
                                    text = "[R]",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFB7185)
                                  )
                                }
                              }

                              // Granular Failure / Biofeedback / Drop / Tempo cues
                              val details = mutableListOf<String>()
                              if (s.failurePoint.isNotBlank()) details.add("⚠️ ${s.failurePoint}")
                              if (s.biofeedbackTags.isNotEmpty()) details.add("⚡ ${s.biofeedbackTags.joinToString(", ")}")
                              if (s.dropWeightKg > 0.0) {
                                val dWt = if (useLbs) (s.dropWeightKg * 2.20462).toInt() else s.dropWeightKg.toInt()
                                val dReps = if (s.dropReps % 1.0 == 0.0) s.dropReps.toInt().toString() else s.dropReps.toString()
                                details.add("↳ Drop: $dWt ${if (useLbs) "lbs" else "kg"} × $dReps")
                              }
                              if (s.tempo.isNotBlank()) details.add("⏱ ${s.tempo}")

                              if (details.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                  text = details.joinToString(" • "),
                                  fontSize = 10.sp,
                                  color = Color(0xFFCBD5E1),
                                  lineHeight = 14.sp
                                )
                              }
                            }

                            IconButton(
                              onClick = { viewModel.removeSetFromActiveSession(exIndex, sIndex) },
                              modifier = Modifier.size(24.dp)
                            ) {
                              Icon(
                                Icons.Default.Close,
                                contentDescription = "Delete Set",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = { showSetLoggerDialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TitaniumWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("active_session_add_set_button")
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Set", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              Button(
                onClick = {
                  val workoutName = customTodaySplit ?: probableSplit
                  viewModel.saveActiveSession(workoutName)
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = TitaniumWhite,
                  contentColor = MatteBlack
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1.4f)
                  .height(48.dp)
                  .testTag("finish_active_session_button")
              ) {
                Text("SAVE WORKOUT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // RECENT WORKOUT HISTORY (WITH DELETION SAFETY)
    // -------------------------------------------------------------
    if (dashboardPrefs.showLastWorkout && workouts.isNotEmpty()) {
      item {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "RECENT SESSIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = TitaniumSilver
          )
          Spacer(modifier = Modifier.height(10.dp))

          workouts.take(4).forEach { w ->
            var isExpanded by remember { mutableStateOf(false) }
            val dateStr = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()).format(Date(w.session.startTimeMillis))

            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = CardDark),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = w.session.name,
                      fontWeight = FontWeight.Bold,
                      fontSize = 15.sp,
                      color = TextPrimary
                    )
                    Text(
                      text = "$dateStr • ${w.session.totalVolumeKg.toInt()} kg Vol",
                      fontSize = 12.sp,
                      color = TextSecondary
                    )
                  }

                  IconButton(onClick = { isExpanded = !isExpanded }, modifier = Modifier.size(28.dp)) {
                    Icon(
                      if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                      contentDescription = null,
                      tint = TitaniumSilver
                    )
                  }
                }

                if (isExpanded) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    w.exercises.forEach { ex ->
                      Surface(
                        onClick = { inspectingWorkoutExercise = Pair(ex, w.session.id) },
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                      ) {
                        Row(
                          modifier = Modifier.padding(10.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text(text = ex.exercise.exerciseName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            val setsInfo = ex.sets.joinToString(", ") { "${it.weightKg.toInt()}kg × ${it.reps}" }
                            Text(text = setsInfo, fontSize = 11.sp, color = TextSecondary)
                          }
                          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

// -------------------------------------------------------------
// SPLIT PICKER DIALOG
// -------------------------------------------------------------
@Composable
private fun SplitPickerDialog(
  currentSplit: String,
  onSelectSplit: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val splits = listOf(
    "Push Day • Chest, Shoulders & Triceps",
    "Pull Day • Back, Rear Delts & Biceps",
    "Legs Day • Quads, Hamstrings & Calves",
    "Upper Body Hypertrophy",
    "Lower Body & Core",
    "Full Body Power",
    "Arms & Weak Points Specialization",
    "Active Recovery & Cardio"
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("split_picker_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "SELECT WORKOUT SPLIT",
            fontWeight = FontWeight.Bold,
            color = TitaniumWhite,
            fontSize = 13.sp,
            letterSpacing = 1.sp
          )

          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          splits.forEach { split ->
            val isSelected = split.equals(currentSplit, ignoreCase = true)
            Surface(
              onClick = { onSelectSplit(split) },
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) CardElevated else CardDark,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) TitaniumWhite else BorderSubtle
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = split,
                  color = if (isSelected) TitaniumWhite else TextPrimary,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  fontSize = 13.sp
                )
                if (isSelected) {
                  Icon(Icons.Default.Check, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// ACTIVE EXERCISE DETAIL DIALOG WITH SAFE DELETION
// -------------------------------------------------------------
@Composable
private fun ActiveExerciseDetailDialog(
  exercise: ActiveExerciseLog,
  useLbs: Boolean,
  onDeleteExercise: () -> Unit,
  onDeleteSet: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  var showConfirmDeleteExercise by remember { mutableStateOf(false) }

  if (showConfirmDeleteExercise) {
    AlertDialog(
      onDismissRequest = { showConfirmDeleteExercise = false },
      title = { Text("Delete Exercise?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to remove \"${exercise.exerciseName}\" and all its sets from this active session?", color = TextSecondary) },
      containerColor = SurfaceDark,
      confirmButton = {
        Button(
          onClick = {
            showConfirmDeleteExercise = false
            onDeleteExercise()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDeleteExercise = false }) {
          Text("Cancel", color = TitaniumSilver)
        }
      }
    )
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("active_exercise_detail_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = exercise.exerciseName,
            fontWeight = FontWeight.Bold,
            color = TitaniumWhite,
            fontSize = 16.sp
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "LOGGED SETS (${exercise.sets.size}):",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          exercise.sets.forEachIndexed { setIdx, s ->
            val wt = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Set ${setIdx + 1}:  $wt ${if (useLbs) "lbs" else "kg"} × ${s.reps} reps",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )

                IconButton(
                  onClick = { onDeleteSet(setIdx) },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(Icons.Default.Delete, contentDescription = "Delete Set", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
          onClick = { showConfirmDeleteExercise = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("delete_active_exercise_btn")
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete Entire Exercise")
        }
      }
    }
  }
}

// -------------------------------------------------------------
// COMPLETED EXERCISE DETAIL DIALOG WITH SAFE DELETION
// -------------------------------------------------------------
@Composable
private fun CompletedExerciseDetailDialog(
  exerciseWithSets: com.example.model.ExerciseWithSets,
  useLbs: Boolean,
  onDeleteSet: (Long) -> Unit,
  onDeleteExercise: () -> Unit,
  onDismiss: () -> Unit
) {
  var showConfirmDelete by remember { mutableStateOf(false) }
  var setPendingDeleteId by remember { mutableStateOf<Long?>(null) }

  if (showConfirmDelete) {
    AlertDialog(
      onDismissRequest = { showConfirmDelete = false },
      title = { Text("Delete Logged Exercise?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to permanently delete \"${exerciseWithSets.exercise.exerciseName}\" and its sets from this completed session?", color = TextSecondary) },
      containerColor = SurfaceDark,
      confirmButton = {
        Button(
          onClick = {
            showConfirmDelete = false
            onDeleteExercise()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmDelete = false }) {
          Text("Cancel", color = TitaniumSilver)
        }
      }
    )
  }

  setPendingDeleteId?.let { setId ->
    AlertDialog(
      onDismissRequest = { setPendingDeleteId = null },
      title = { Text("Delete Logged Set?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete this set from the completed workout?", color = TextSecondary) },
      containerColor = SurfaceDark,
      confirmButton = {
        Button(
          onClick = {
            onDeleteSet(setId)
            setPendingDeleteId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White)
        ) {
          Text("Delete Set")
        }
      },
      dismissButton = {
        TextButton(onClick = { setPendingDeleteId = null }) {
          Text("Cancel", color = TitaniumSilver)
        }
      }
    )
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("completed_exercise_detail_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = exerciseWithSets.exercise.exerciseName,
            fontWeight = FontWeight.Bold,
            color = TitaniumWhite,
            fontSize = 16.sp
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "COMPLETED SETS (${exerciseWithSets.sets.size}):",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          exerciseWithSets.sets.forEachIndexed { idx, s ->
            val wt = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
            val repsFormatted = if (s.reps % 1.0 == 0.0) s.reps.toInt().toString() else s.reps.toString()
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Set ${idx + 1}:  $wt ${if (useLbs) "lbs" else "kg"} × $repsFormatted reps",
                  fontSize = 13.sp,
                  color = TextPrimary
                )

                IconButton(
                  onClick = { setPendingDeleteId = s.id },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Set",
                    tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedButton(
          onClick = { showConfirmDelete = true },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete Entire Exercise")
        }
      }
    }
  }
}

// -------------------------------------------------------------
// MULTI-WORKOUT RAMBLER REVIEW & CLARIFICATION DIALOG
// -------------------------------------------------------------
@Composable
private fun MultiWorkoutRamblerDialog(
  rants: List<ParsedWorkoutRant>,
  clarifications: List<com.example.viewmodel.RamblerClarification>,
  useLbs: Boolean,
  onToggleUnits: () -> Unit = {},
  onUpdateClarification: (Int, Double, Int) -> Unit,
  onUpdateRant: (Int, ParsedWorkoutRant) -> Unit,
  onRemoveRant: (Int) -> Unit,
  onConfirmAll: (List<ParsedWorkoutRant>) -> Unit,
  onDismiss: () -> Unit
) {
  var workoutList by remember(rants) { mutableStateOf(rants) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = false,
      dismissOnClickOutside = false
    )
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth(0.96f)
        .fillMaxHeight(0.88f)
        .testTag("rambler_review_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (workoutList.size > 1) "REVIEW ${workoutList.size} SESSIONS" else "REVIEW LOGGED SESSION",
              fontWeight = FontWeight.Bold,
              color = TitaniumWhite,
              fontSize = 14.sp,
              letterSpacing = 1.sp
            )
            Text(
              text = "Extracted past workouts with dates & sets",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF262A35),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
              modifier = Modifier.clickable { onToggleUnits() }
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (useLbs) "LBS" else "KG",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(Icons.Default.Refresh, contentDescription = "Toggle Unit", tint = PlatinumSteel, modifier = Modifier.size(12.dp))
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clarifications section if missing weight or reps
        if (clarifications.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1E2129),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = PlatinumSteel, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clarifications Needed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumWhite)
              }
              Spacer(modifier = Modifier.height(6.dp))
              clarifications.forEach { cl ->
                var inputVal by remember(cl.exerciseIndex, cl.question, useLbs) {
                  val initNum = if (useLbs) (cl.initialWeightKg * 2.20462).toInt() else cl.initialWeightKg.toInt()
                  mutableStateOf(if (initNum > 0) initNum.toString() else (if (useLbs) "45" else "20"))
                }
                Text(cl.question, fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    singleLine = true,
                    trailingIcon = {
                      Text(
                        text = if (useLbs) "lbs" else "kg",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.padding(end = 8.dp)
                      )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = CardDark,
                      unfocusedContainerColor = CardDark,
                      focusedTextColor = TextPrimary,
                      unfocusedTextColor = TextPrimary,
                      focusedBorderColor = TitaniumWhite,
                      unfocusedBorderColor = BorderHighlight
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .weight(1f)
                      .height(52.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                    onClick = {
                      val entered = inputVal.toDoubleOrNull() ?: (if (useLbs) 45.0 else 20.0)
                      val wtKg = if (useLbs) (entered / 2.20462) else entered
                      onUpdateClarification(cl.exerciseIndex, wtKg, cl.initialReps)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TitaniumWhite, contentColor = MatteBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(52.dp)
                  ) {
                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
                Spacer(modifier = Modifier.height(8.dp))
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // List of all parsed workout sessions
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          itemsIndexed(
            items = workoutList,
            key = { index, r -> "rant_${index}_${r.workoutDateMillis}_${r.workoutTitle}" }
          ) { index, rant ->
            WorkoutRantReviewItem(
              rant = rant,
              index = index,
              totalCount = workoutList.size,
              useLbs = useLbs,
              onTitleChange = { newTitle ->
                val currentList = workoutList.toMutableList()
                if (index in currentList.indices) {
                  currentList[index] = currentList[index].copy(workoutTitle = newTitle)
                  workoutList = currentList
                }
              },
              onRemove = {
                val currentList = workoutList.toMutableList()
                if (index in currentList.indices) {
                  currentList.removeAt(index)
                  workoutList = currentList
                  onRemoveRant(index)
                }
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
              containerColor = CardElevated,
              contentColor = TextSecondary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", fontWeight = FontWeight.Medium, fontSize = 12.sp)
          }

          Button(
            onClick = {
              onConfirmAll(workoutList)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = TitaniumWhite,
              contentColor = MatteBlack
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1.4f)
              .testTag("confirm_save_workout_button")
          ) {
            Text(
              text = if (workoutList.size > 1) "SYNC ALL (${workoutList.size})" else "SYNC WORKOUT",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RamblerReviewDialog(
  parsed: ParsedWorkoutRant,
  clarifications: List<com.example.viewmodel.RamblerClarification>,
  useLbs: Boolean,
  onUpdateClarification: (Int, Double, Int) -> Unit,
  onConfirm: (ParsedWorkoutRant) -> Unit,
  onDismiss: () -> Unit
) {
  MultiWorkoutRamblerDialog(
    rants = listOf(parsed),
    clarifications = clarifications,
    useLbs = useLbs,
    onUpdateClarification = onUpdateClarification,
    onUpdateRant = { _, updated -> onConfirm(updated) },
    onRemoveRant = { onDismiss() },
    onConfirmAll = { list -> list.firstOrNull()?.let { onConfirm(it) } },
    onDismiss = onDismiss
  )
}

@Composable
private fun WorkoutRantReviewItem(
  rant: ParsedWorkoutRant,
  index: Int,
  totalCount: Int,
  useLbs: Boolean,
  onTitleChange: (String) -> Unit,
  onRemove: () -> Unit
) {
  var titleText by rememberSaveable(rant.workoutDateMillis) { mutableStateOf(rant.workoutTitle) }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = CardDark,
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Date Pill
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = SurfaceDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = PlatinumSteel, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = rant.dateDisplay ?: "Past Workout",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TitaniumWhite
            )
          }
        }

        if (totalCount > 1) {
          IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Remove workout", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "WORKOUT NAME",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = PlatinumSteel
      )
      Spacer(modifier = Modifier.height(4.dp))
      OutlinedTextField(
        value = titleText,
        onValueChange = {
          titleText = it
          onTitleChange(it)
        },
        placeholder = { Text("e.g. Push Hypertrophy A", color = TextSecondary, fontSize = 12.sp) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = SurfaceDark,
          unfocusedContainerColor = SurfaceDark,
          focusedBorderColor = TitaniumWhite,
          unfocusedBorderColor = BorderSubtle,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "${rant.exercises.size} Exercises Detected:",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(4.dp))

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rant.exercises.forEach { ex ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = ex.exerciseName,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TitaniumWhite
            )
            val setsSummary = ex.sets.mapIndexed { _, s ->
              val wt = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
              val repsFormatted = if (s.reps % 1.0 == 0.0) s.reps.toInt().toString() else s.reps.toString()
              val sideIndicator = if (s.side == "LEFT") " [L]" else if (s.side == "RIGHT") " [R]" else ""
              "${wt}${if (useLbs) "lb" else "kg"}×$repsFormatted$sideIndicator"
            }.joinToString(", ")
            Text(
              text = setsSummary,
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RamblerInputCard(
  isOnline: Boolean,
  isGeminiConfigured: Boolean,
  initialApiKey: String,
  onSaveGeminiKey: (String) -> Unit,
  isParsingRant: Boolean,
  onParseRant: (String) -> Unit,
  onDismiss: (() -> Unit)? = null
) {
  var ramblerText by rememberSaveable { mutableStateOf("") }
  var showKeyDialog by remember { mutableStateOf(false) }
  var apiKeyDraft by remember { mutableStateOf(initialApiKey) }
  val clipboardManager = LocalClipboardManager.current

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
    border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
    modifier = Modifier
      .fillMaxWidth(0.96f)
      .fillMaxHeight(0.88f)
      .testTag("rambler_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(18.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CardDark),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              Icons.Default.FitnessCenter,
              contentDescription = null,
              tint = TitaniumWhite,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "THE RAMBLER",
              fontSize = 15.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.sp,
              color = TitaniumWhite
            )
            Text(
              text = "Lifter's Notepad • Gemini 3.5 Flash",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Gemini status badge / trigger
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isGeminiConfigured) Color(0xFF0F2E1E) else Color(0xFF2A2415),
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isGeminiConfigured) Color(0xFF05DF72).copy(alpha = 0.6f) else Color(0xFFE5A83B).copy(alpha = 0.6f)
            ),
            modifier = Modifier.clickable { showKeyDialog = !showKeyDialog }
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                if (isGeminiConfigured) Icons.Default.AutoAwesome else Icons.Default.Key,
                contentDescription = null,
                tint = if (isGeminiConfigured) Color(0xFF05DF72) else Color(0xFFE5A83B),
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isGeminiConfigured) "Gemini Active" else "Setup Key",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGeminiConfigured) Color(0xFF05DF72) else Color(0xFFE5A83B)
              )
            }
          }

          if (onDismiss != null) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
              Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Inline Gemini Key setup banner (if opened)
      AnimatedVisibility(visible = showKeyDialog) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Gemini API Key (Google AI Studio)",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TitaniumWhite
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Free key from aistudio.google.com for high-accuracy workout analysis and workout title categorization.",
              fontSize = 10.sp,
              color = TextSecondary,
              lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
              value = apiKeyDraft,
              onValueChange = { apiKeyDraft = it },
              placeholder = { Text("AIzaSy...", color = TextSecondary, fontSize = 11.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = TitaniumWhite,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              TextButton(onClick = { showKeyDialog = false }) {
                Text("Close", color = TextSecondary, fontSize = 11.sp)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = {
                  onSaveGeminiKey(apiKeyDraft)
                  showKeyDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = TitaniumWhite, contentColor = MatteBlack),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Save Key", fontWeight = FontWeight.Bold, fontSize = 11.sp)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Quick action helper chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.clickable {
            val clip = clipboardManager.getText()?.text
            if (!clip.isNullOrBlank()) {
              ramblerText = if (ramblerText.isBlank()) clip else "$ramblerText\n$clip"
            }
          }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Paste Clipboard", fontSize = 11.sp, color = TitaniumWhite)
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.clickable {
            val example = "Push Day\nBarbell Bench Press 80kg 3x8\nIncline DB Press 30kg 10, 8, 8\nCable Chest Flyes 15kg 12, 12\nTricep Rope Pushdowns 25kg 3x12"
            ramblerText = if (ramblerText.isBlank()) example else "$ramblerText\n\n$example"
          }
        ) {
          Text("+ Push Day", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.clickable {
            val example = "Pull Day\nLat Pulldowns 65kg 3x10\nSeated Cable Row 60kg 10, 10, 8\nIncline DB Curl 14kg 3x10\nFace Pulls 20kg 15, 15"
            ramblerText = if (ramblerText.isBlank()) example else "$ramblerText\n\n$example"
          }
        ) {
          Text("+ Pull Day", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.clickable {
            val example = "Leg Day\nBarbell Back Squats 100kg 3x6\nLeg Press 180kg 10, 10, 10\nRomanian Deadlift 80kg 3x8\nStanding Calf Raises 50kg 15, 15"
            ramblerText = if (ramblerText.isBlank()) example else "$ramblerText\n\n$example"
          }
        ) {
          Text("+ Leg Day", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }

        if (ramblerText.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.clickable { ramblerText = "" }
          ) {
            Text("Clear", fontSize = 11.sp, color = Color(0xFFEF5350), modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Spacious Lifter's Notepad Text Box (dynamic weight fills the dialog!)
      OutlinedTextField(
        value = ramblerText,
        onValueChange = { ramblerText = it },
        placeholder = {
          Text(
            text = "Type or paste your workout notes freely, e.g.:\n\nPush Day\nBarbell Bench Press 80kg 3x8\nIncline DB Press 32kg 10, 8, 8\nCable Chest Flyes 15kg 12, 12\nTricep Rope Pushdown 25kg 3x12 (insane pump)\nLateral Raises 12kg 15, 12\n\nTip: You can paste your entire session! Gemini will intelligently analyze all exercises, sets, weights, reps, and workout names.",
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 13.sp,
            lineHeight = 19.sp
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("rambler_text_input"),
        shape = RoundedCornerShape(14.dp),
        singleLine = false,
        minLines = 8,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          autoCorrectEnabled = true,
          keyboardType = KeyboardType.Text,
          imeAction = ImeAction.Default
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = CardDark,
          unfocusedContainerColor = CardDark,
          focusedBorderColor = TitaniumWhite,
          unfocusedBorderColor = BorderSubtle,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Character / Line counter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val lineCount = ramblerText.lines().count { it.isNotBlank() }
        Text(
          text = if (ramblerText.isBlank()) "Ready for notes" else "$lineCount line${if (lineCount == 1) "" else "s"} • ${ramblerText.length} chars",
          fontSize = 11.sp,
          color = TextSecondary
        )
        Text(
          text = if (isGeminiConfigured) "✨ AI Analysis Enabled" else "⚡ Local Rule Engine",
          fontSize = 11.sp,
          color = if (isGeminiConfigured) Color(0xFF05DF72) else TextSecondary
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Action Button
      Button(
        onClick = {
          if (ramblerText.isNotBlank()) {
            onParseRant(ramblerText)
          }
        },
        enabled = ramblerText.isNotBlank() && !isParsingRant,
        colors = ButtonDefaults.buttonColors(
          containerColor = TitaniumWhite,
          contentColor = MatteBlack,
          disabledContainerColor = CardElevated,
          disabledContentColor = TextSecondary
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("submit_rambler_button")
      ) {
        if (isParsingRant) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MatteBlack, strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Gemini is analyzing your notes...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("ANALYZE WITH GEMINI", fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 0.5.sp)
        }
      }
    }
  }
}

@Composable
private fun QuickSetLoggerDialog(
  allAvailableExercises: List<com.example.model.ExerciseDefinition>,
  splitExercises: List<String>,
  selectedExerciseName: String,
  onSelectExercise: (String) -> Unit,
  selectedWeightKg: Double,
  onWeightChange: (Double) -> Unit,
  selectedReps: Int,
  onRepsChange: (Int) -> Unit,
  hasFractionalRep: Boolean,
  onFractionalRepChange: (Boolean) -> Unit,
  isUnilateralMode: Boolean,
  onUnilateralModeChange: (Boolean) -> Unit,
  selectedSide: String,
  onSideChange: (String) -> Unit,
  selectedBiofeedbackTags: Set<String>,
  onToggleBiofeedbackTag: (String) -> Unit,
  selectedTempo: String,
  onTempoChange: (String) -> Unit,
  selectedFailurePoint: String,
  onFailurePointChange: (String) -> Unit,
  useLbs: Boolean,
  onOpenDirectWeightDialog: () -> Unit,
  onOpenExerciseSearch: () -> Unit,
  onOpenAddCustomExercise: () -> Unit,
  currentExerciseTarget: OverloadTarget,
  onMatchLastTarget: (Double, Int) -> Unit,
  onLogSet: () -> Unit,
  onDismiss: () -> Unit
) {
  androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .testTag("quick_set_logger_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "LOG NEW SET",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp,
              color = PlatinumSteel
            )
            Text(
              text = selectedExerciseName,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TitaniumWhite
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Exercise Picker Strip
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("EXERCISE", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TextSecondary)
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
              onClick = onOpenAddCustomExercise,
              shape = RoundedCornerShape(6.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
            ) {
              Text("+ New", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TitaniumWhite, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
            Surface(
              onClick = onOpenExerciseSearch,
              shape = RoundedCornerShape(6.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderHighlight)
            ) {
              Text("Browse All", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Split exercise quick chips
        if (splitExercises.isNotEmpty()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            splitExercises.forEach { exName ->
              val isSelected = selectedExerciseName.equals(exName, ignoreCase = true)
              Surface(
                onClick = { onSelectExercise(exName) },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) TitaniumWhite else CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TitaniumWhite else BorderHighlight)
              ) {
                Text(
                  text = exName,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MatteBlack else TitaniumSilver,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        // Target Progression Card
        if (currentExerciseTarget.targetRepsProgression.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF14171E),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                  Text("PROGRESSION TARGET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
                  Text(currentExerciseTarget.targetRepsProgression, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TitaniumWhite)
                }
              }
              if (currentExerciseTarget.lastWeightKg > 0) {
                Surface(
                  onClick = { onMatchLastTarget(currentExerciseTarget.lastWeightKg, currentExerciseTarget.lastReps.coerceAtLeast(1)) },
                  shape = RoundedCornerShape(6.dp),
                  color = CardElevated,
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                ) {
                  Text("Match Last", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(14.dp))
        }

        // WEIGHT CONTROLS
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .clickable { onOpenDirectWeightDialog() }
              .padding(4.dp)
          ) {
            Text(if (useLbs) "WEIGHT (LBS)" else "WEIGHT (KG)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            val displayWeight = if (useLbs) (selectedWeightKg * 2.20462).toInt() else selectedWeightKg.toInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("$displayWeight ${if (useLbs) "lbs" else "kg"}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TitaniumWhite)
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.Default.Edit, contentDescription = "Edit weight", tint = PlatinumSteel, modifier = Modifier.size(12.dp))
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(-5.0, -2.5, 2.5, 5.0).forEach { delta ->
              val label = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}"
              Surface(
                onClick = { onWeightChange((selectedWeightKg + delta).coerceAtLeast(0.0)) },
                shape = RoundedCornerShape(8.dp),
                color = CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
              ) {
                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // REPS CONTROLS
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("REPETITIONS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
          Surface(
            onClick = { onFractionalRepChange(!hasFractionalRep) },
            shape = RoundedCornerShape(6.dp),
            color = if (hasFractionalRep) Color(0xFFEF4444).copy(alpha = 0.2f) else CardElevated,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, if (hasFractionalRep) Color(0xFFEF4444) else BorderSubtle)
          ) {
            Text(
              text = if (hasFractionalRep) "+0.5 Rep (Failure Point)" else "+0.5 Rep",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (hasFractionalRep) Color(0xFFFCA5A5) else TextSecondary,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Rep Chips (-1, +1, and standard counts)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { onRepsChange((selectedReps - 1).coerceAtLeast(1)) },
            modifier = Modifier.size(36.dp).clip(CircleShape).background(CardElevated)
          ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TitaniumWhite, modifier = Modifier.size(16.dp))
          }

          Row(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "$selectedReps${if (hasFractionalRep) ".5" else ""} REPS",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color = TitaniumWhite
            )
          }

          IconButton(
            onClick = { onRepsChange(selectedReps + 1) },
            modifier = Modifier.size(36.dp).clip(CircleShape).background(CardElevated)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = TitaniumWhite, modifier = Modifier.size(16.dp))
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // UNILATERAL MODE TOGGLE
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("UNILATERAL (L / R)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("BOTH", "LEFT", "RIGHT").forEach { side ->
              val isSel = if (!isUnilateralMode) side == "BOTH" else (selectedSide == side && side != "BOTH")
              Surface(
                onClick = {
                  if (side == "BOTH") {
                    onUnilateralModeChange(false)
                    onSideChange("BOTH")
                  } else {
                    onUnilateralModeChange(true)
                    onSideChange(side)
                  }
                },
                shape = RoundedCornerShape(6.dp),
                color = if (isSel) TitaniumWhite else CardElevated,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) TitaniumWhite else BorderSubtle)
              ) {
                Text(side, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) MatteBlack else TextSecondary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LOG SET BUTTON
        val displayWeight = if (useLbs) (selectedWeightKg * 2.20462).toInt() else selectedWeightKg.toInt()
        val repsLabel = "$selectedReps${if (hasFractionalRep) ".5" else ""}"
        Button(
          onClick = onLogSet,
          colors = ButtonDefaults.buttonColors(containerColor = TitaniumWhite, contentColor = MatteBlack),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("submit_log_set_button")
        ) {
          Text(
            text = "LOG SET ($displayWeight ${if (useLbs) "lbs" else "kg"} × $repsLabel reps)",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
          )
        }
      }
    }
  }
}

