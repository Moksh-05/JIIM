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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val workouts by viewModel.allWorkouts.collectAsState()
  val activeExercises by viewModel.activeExercises.collectAsState()
  val isParsingRant by viewModel.isParsingRant.collectAsState()
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
  var isExerciseSearchOpen by remember { mutableStateOf(false) }
  var showAddCustomExerciseDialog by remember { mutableStateOf(false) }

  // Inspecting active or completed exercises
  var inspectingActiveExerciseIndex by remember { mutableStateOf<Int?>(null) }
  var inspectingWorkoutExercise by remember {
    mutableStateOf<Pair<com.example.model.ExerciseWithSets, Long>?>(null)
  }

  var ramblerInput by remember { mutableStateOf("") }

  // Streak calculation
  val (currStreak, _) = remember(workouts) { viewModel.computeStreaks(workouts) }

  // DIALOGS
  if (parsedRant != null) {
    RamblerReviewDialog(
      parsed = parsedRant!!,
      clarifications = clarifications,
      useLbs = useLbs,
      onUpdateClarification = { idx, wt, reps ->
        viewModel.updateClarification(idx, wt, reps)
      },
      onConfirm = { confirmedRant ->
        viewModel.saveLoggedWorkout(confirmedRant)
        ramblerInput = ""
      },
      onDismiss = { viewModel.clearParsedRant() }
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
      onDeleteExercise = {
        viewModel.deleteExerciseFromWorkout(exerciseWithSets.exercise.id, sessionId)
        inspectingWorkoutExercise = null
      },
      onDismiss = { inspectingWorkoutExercise = null }
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

          // Streak & JIIM Brand Badge
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

        // Tactical In-Gym Rest Timer (if enabled in settings)
        if (dashboardPrefs.showRestTimer) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isRestTimerActive) Color(0xFF1B2230) else CardDark,
            border = androidx.compose.foundation.BorderStroke(
              1.dp,
              if (isRestTimerActive) TitaniumWhite else BorderSubtle
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, BorderHighlight, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (isRestTimerActive) TitaniumWhite else TitaniumSilver,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = if (isRestTimerActive) "REST TIMER RUNNING" else "GYM REST TIMER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = if (isRestTimerActive) TitaniumWhite else TextSecondary
                  )
                  Text(
                    text = if (isRestTimerActive) "${restTimerRemaining}s remaining" else "Default: ${dashboardPrefs.restTimerSeconds}s",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRestTimerActive) {
                  Surface(
                    onClick = { viewModel.stopRestTimer() },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF331C1C),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                  ) {
                    Text(
                      text = "Reset",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFFCA5A5),
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                } else {
                  Surface(
                    onClick = { viewModel.startRestTimer() },
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight)
                  ) {
                    Text(
                      text = "Start ${dashboardPrefs.restTimerSeconds}s",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = TitaniumWhite,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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
    // LOG EXERCISE (MAIN HEADING & TWO SELECTABLE METHODS)
    // -------------------------------------------------------------
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "LOG EXERCISE",
              fontSize = 16.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 1.5.sp,
              color = TextPrimary
            )
            Text(
              text = "Choose your preferred logging method",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }

          // Inbuilt Add Custom Exercise Button
          Surface(
            onClick = { showAddCustomExerciseDialog = true },
            shape = RoundedCornerShape(10.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.testTag("inbuilt_add_exercise_button")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Add Lift",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TitaniumWhite
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Two Selectable Method Cards (Method 1: Set-by-Set vs Method 2: The Rambler)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Method 1: Set Logger Card
          val isMethod1 = selectedLoggingMethod == "SET_LOGGER"
          Surface(
            onClick = { viewModel.setLoggingMethod("SET_LOGGER") },
            shape = RoundedCornerShape(16.dp),
            color = if (isMethod1) CardElevated else CardDark,
            border = androidx.compose.foundation.BorderStroke(
              1.5.dp,
              if (isMethod1) TitaniumWhite else BorderSubtle
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("method_set_logger_card")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMethod1) TitaniumWhite else SurfaceDark),
                  contentAlignment = Alignment.Center
                ) {
                  MinimalDumbbellIcon(
                    tint = if (isMethod1) MatteBlack else TitaniumSilver,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isMethod1) TitaniumWhite.copy(alpha = 0.15f) else SurfaceDark)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "METHOD 1",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMethod1) TitaniumWhite else TextSecondary
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "Set-by-Set",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Tactile weights & rep wheel",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          // Method 2: The Rambler AI Card
          val isMethod2 = selectedLoggingMethod == "RAMBLER"
          Surface(
            onClick = { viewModel.setLoggingMethod("RAMBLER") },
            shape = RoundedCornerShape(16.dp),
            color = if (isMethod2) CardElevated else CardDark,
            border = androidx.compose.foundation.BorderStroke(
              1.5.dp,
              if (isMethod2) TitaniumWhite else BorderSubtle
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("method_rambler_card")
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isMethod2) TitaniumWhite else SurfaceDark),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isMethod2) MatteBlack else TitaniumSilver,
                    modifier = Modifier.size(18.dp)
                  )
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isMethod2) TitaniumWhite.copy(alpha = 0.15f) else SurfaceDark)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "METHOD 2",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMethod2) TitaniumWhite else TextSecondary
                  )
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              Text(
                text = "The Rambler",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Stream of thoughts via AI",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
        }
      }
    }

    // -------------------------------------------------------------
    // ACTIVE METHOD INTERFACE (EXPANDED BASED ON SELECTION)
    // -------------------------------------------------------------
    item {
      if (selectedLoggingMethod == "SET_LOGGER") {
        // ---------------------------------------------------------
        // METHOD 1: QUICK SET LOGGER
        // ---------------------------------------------------------
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
          modifier = Modifier.fillMaxWidth().testTag("exercise_logger_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Header with Exercise Picker & Inbuilt Add Button
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "SELECT EXERCISE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TitaniumSilver
              )

              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                  onClick = { showAddCustomExerciseDialog = true },
                  shape = RoundedCornerShape(8.dp),
                  color = CardElevated,
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                  Text(
                    text = "+ New",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitaniumWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }

                Surface(
                  onClick = { isExerciseSearchOpen = true },
                  shape = RoundedCornerShape(8.dp),
                  color = CardElevated,
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                  modifier = Modifier.testTag("change_exercise_button")
                ) {
                  Text(
                    text = "Browse All",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TitaniumSilver,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Currently Selected Exercise Bar
            Surface(
              onClick = { isExerciseSearchOpen = true },
              shape = RoundedCornerShape(12.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  MinimalDumbbellIcon(size = 20.dp, tint = TitaniumWhite, accentTint = PlatinumSteel)
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = selectedExerciseName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TitaniumWhite
                  )
                }

                Text(
                  text = "Tap to change",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            // Quick Split-Specific Exercise Chips
            if (splitExercises.isNotEmpty()) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                splitExercises.forEach { exName ->
                  val isSelected = selectedExerciseName.equals(exName, ignoreCase = true)
                  Surface(
                    onClick = {
                      selectedExerciseName = exName
                      val tgt = viewModel.getTargetForExercise(exName, currentActiveSplit)
                      if (tgt.lastWeightKg > 0) {
                        selectedWeightKg = tgt.lastWeightKg
                        selectedReps = tgt.lastReps.coerceAtLeast(1)
                      }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) TitaniumWhite else CardElevated,
                    border = androidx.compose.foundation.BorderStroke(
                      1.dp,
                      if (isSelected) TitaniumWhite else BorderHighlight
                    )
                  ) {
                    Text(
                      text = exName,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) MatteBlack else TitaniumSilver,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }

            // Real-Time Overload Target for the currently selected exercise
            val currentExerciseTarget = remember(selectedExerciseName, currentActiveSplit, workouts) {
              viewModel.getTargetForExercise(selectedExerciseName, currentActiveSplit)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF14171E),
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = TitaniumWhite,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text(
                      text = "TARGET PROGRESSION",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 1.sp,
                      color = TitaniumSilver
                    )
                    Text(
                      text = currentExerciseTarget.targetRepsProgression,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = TitaniumWhite
                    )
                  }
                }

                if (currentExerciseTarget.lastWeightKg > 0) {
                  Surface(
                    onClick = {
                      selectedWeightKg = currentExerciseTarget.lastWeightKg
                      selectedReps = currentExerciseTarget.lastReps.coerceAtLeast(1)
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = CardElevated,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle)
                  ) {
                    Text(
                      text = "Match Last",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = TitaniumSilver,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weight Controls Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = if (useLbs) "WEIGHT (LBS)" else "WEIGHT (KG)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  letterSpacing = 1.sp,
                  color = TextSecondary
                )
                val displayWeight = if (useLbs) (selectedWeightKg * 2.20462).toInt() else selectedWeightKg.toInt()
                Text(
                  text = "$displayWeight ${if (useLbs) "lbs" else "kg"}",
                  fontSize = 26.sp,
                  fontWeight = FontWeight.Black,
                  color = TitaniumWhite
                )
              }

              // Quick weight adjustment buttons
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(-5.0, -2.5, 2.5, 5.0).forEach { delta ->
                  val label = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}"
                  Surface(
                    onClick = {
                      selectedWeightKg = (selectedWeightKg + delta).coerceAtLeast(0.0)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = CardElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.testTag("weight_btn_$label")
                  ) {
                    Text(
                      text = label,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary,
                      modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rep Wheel Picker
            Text(
              text = "REPETITIONS",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 1.sp,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            RepWheelPicker(
              reps = selectedReps,
              onRepsChange = { selectedReps = it },
              modifier = Modifier.fillMaxWidth().testTag("rep_wheel_picker")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Log Set Button
            Button(
              onClick = {
                viewModel.addSetToActiveSession(
                  exerciseName = selectedExerciseName,
                  weightKg = selectedWeightKg,
                  reps = selectedReps
                )
                if (dashboardPrefs.showRestTimer) {
                  viewModel.startRestTimer()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = TitaniumWhite,
                contentColor = MatteBlack
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("log_set_button")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "LOG SET (${if (useLbs) (selectedWeightKg * 2.20462).toInt() else selectedWeightKg.toInt()} ${if (useLbs) "lbs" else "kg"} × $selectedReps reps)",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  letterSpacing = 0.5.sp
                )
              }
            }
          }
        }
      } else {
        // ---------------------------------------------------------
        // METHOD 2: THE RAMBLER (AI VOICE/TEXT STREAM)
        // ---------------------------------------------------------
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = SurfaceDark),
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
          modifier = Modifier.fillMaxWidth().testTag("rambler_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "THE RAMBLER",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Black,
                  letterSpacing = 1.sp,
                  color = TitaniumWhite
                )
                Text(
                  text = "Type or dictate whatever you did in any order",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isOnline) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFD48B54).copy(alpha = 0.15f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = if (isOnline) "ONLINE AI" else "OFFLINE PARSER",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isOnline) Color(0xFF10B981) else Color(0xFFD48B54)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
              value = ramblerInput,
              onValueChange = { ramblerInput = it },
              placeholder = {
                Text(
                  text = "e.g. Bench press 80kg 3 sets of 8 reps, then did lateral raises with 12kg dumbbells for 15 reps, felt great pump",
                  color = TextSecondary,
                  fontSize = 12.sp,
                  lineHeight = 17.sp
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .testTag("rambler_text_input"),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardDark,
                unfocusedContainerColor = CardDark,
                focusedBorderColor = TitaniumWhite,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
              onClick = {
                if (ramblerInput.isNotBlank()) {
                  viewModel.parseGymRant(ramblerInput)
                }
              },
              enabled = ramblerInput.isNotBlank() && !isParsingRant,
              colors = ButtonDefaults.buttonColors(
                containerColor = TitaniumWhite,
                contentColor = MatteBlack
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("submit_rambler_button")
            ) {
              if (isParsingRant) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MatteBlack, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI is extracting workout...", fontWeight = FontWeight.Bold)
              } else {
                Text("PARSE WORKOUT WITH AI", fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.5.sp)
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              activeExercises.forEachIndexed { exIndex, ex ->
                Surface(
                  onClick = { inspectingActiveExerciseIndex = exIndex },
                  shape = RoundedCornerShape(10.dp),
                  color = CardDark,
                  border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = ex.exerciseName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                      )
                      val setsText = ex.sets.map {
                        val wt = if (useLbs) (it.weightKg * 2.20462).toInt() else it.weightKg.toInt()
                        "${wt}${if (useLbs) "lb" else "kg"}×${it.reps}"
                      }.joinToString("  •  ")
                      Text(
                        text = setsText,
                        fontSize = 12.sp,
                        color = TextSecondary
                      )
                    }

                    Icon(
                      Icons.Default.Delete,
                      contentDescription = "Manage Exercise",
                      tint = TextSecondary,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                .fillMaxWidth()
                .height(48.dp)
                .testTag("finish_active_session_button")
            ) {
              Text("COMPLETE & SAVE WORKOUT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
  onDeleteExercise: () -> Unit,
  onDismiss: () -> Unit
) {
  var showConfirmDelete by remember { mutableStateOf(false) }

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
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Set ${idx + 1}:  $wt ${if (useLbs) "lbs" else "kg"} × ${s.reps} reps",
                  fontSize = 13.sp,
                  color = TextPrimary
                )
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
          Text("Delete From Saved Workout")
        }
      }
    }
  }
}

// -------------------------------------------------------------
// RAMBLER REVIEW & CLARIFICATION DIALOG
// -------------------------------------------------------------
@Composable
private fun RamblerReviewDialog(
  parsed: ParsedWorkoutRant,
  clarifications: List<com.example.viewmodel.RamblerClarification>,
  useLbs: Boolean,
  onUpdateClarification: (Int, Double, Int) -> Unit,
  onConfirm: (ParsedWorkoutRant) -> Unit,
  onDismiss: () -> Unit
) {
  var workoutTitle by remember { mutableStateOf(parsed.workoutTitle) }
  val exercisesList by remember { mutableStateOf(parsed.exercises) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
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
          Text(
            text = "REVIEW LOGGED SESSION",
            fontWeight = FontWeight.Bold,
            color = TitaniumWhite,
            fontSize = 13.sp,
            letterSpacing = 1.sp
          )

          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                Text("Clarification Needed", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumWhite)
              }
              Spacer(modifier = Modifier.height(6.dp))
              clarifications.forEach { cl ->
                var inputVal by remember { mutableStateOf(if (cl.initialWeightKg > 0) cl.initialWeightKg.toString() else "20.0") }
                Text(cl.question, fontSize = 11.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  OutlinedTextField(
                    value = inputVal,
                    onValueChange = { inputVal = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = CardDark,
                      unfocusedContainerColor = CardDark,
                      focusedTextColor = TextPrimary,
                      unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.width(100.dp).height(48.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                    onClick = {
                      val wt = inputVal.toDoubleOrNull() ?: 20.0
                      onUpdateClarification(cl.exerciseIndex, wt, cl.initialReps)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TitaniumWhite, contentColor = MatteBlack),
                    shape = RoundedCornerShape(8.dp)
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

        OutlinedTextField(
          value = workoutTitle,
          onValueChange = { workoutTitle = it },
          label = { Text("Workout Title", color = TextSecondary, fontSize = 11.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Detected Exercises (${exercisesList.size}):",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(exercisesList) { ex ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = ex.exerciseName,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite,
                  fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val setsSummary = ex.sets.mapIndexed { idx, s ->
                  val wt = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
                  "S${idx + 1}: ${wt}${if (useLbs) "lb" else "kg"} × ${s.reps}"
                }.joinToString(", ")
                Text(
                  text = setsSummary,
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
            }
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
              onConfirm(
                parsed.copy(
                  workoutTitle = workoutTitle,
                  exercises = exercisesList
                )
              )
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = TitaniumWhite,
              contentColor = MatteBlack
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1.3f)
              .testTag("confirm_save_workout_button")
          ) {
            Text("CONFIRM LOG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  }
}
