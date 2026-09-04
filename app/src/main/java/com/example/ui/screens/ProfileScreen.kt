package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DashboardPreferences
import com.example.data.UpdateCheckState
import com.example.data.UserProfile
import com.example.ui.components.AddCustomExerciseDialog
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
import com.example.viewmodel.GymViewModel

sealed class DeleteTarget {
  object AllWorkouts : DeleteTarget()
  object AllBodyWeights : DeleteTarget()
  object AllPrs : DeleteTarget()
  object AllRoutines : DeleteTarget()
  object AllCustomExercises : DeleteTarget()
  object WipeEverything : DeleteTarget()
  data class SingleWorkout(val id: Long, val name: String) : DeleteTarget()
  data class SingleBodyWeight(val id: Long, val weightStr: String) : DeleteTarget()
  data class SinglePr(val exerciseName: String) : DeleteTarget()
  data class SingleRoutine(val id: Long, val name: String) : DeleteTarget()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val profile by viewModel.userProfile.collectAsState()
  val dashboardPrefs by viewModel.dashboardPrefs.collectAsState()
  val customExercises by viewModel.customExercises.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()
  val workouts by viewModel.allWorkouts.collectAsState()
  val prs by viewModel.allPrs.collectAsState()
  val bodyWeights by viewModel.allBodyWeights.collectAsState()
  val routines by viewModel.allRoutines.collectAsState()
  val updateCheckState by viewModel.updateCheckState.collectAsState()
  val currentAppVersion = viewModel.currentAppVersion

  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showAddExerciseDialog by remember { mutableStateOf(false) }
  var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }
  var statusNotice by remember { mutableStateOf<String?>(null) }

  var expandWorkoutsList by remember { mutableStateOf(false) }
  var expandWeightsList by remember { mutableStateOf(false) }
  var expandPrsList by remember { mutableStateOf(false) }
  var expandRoutinesList by remember { mutableStateOf(false) }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MatteBlack)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .testTag("profile_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Profile Hero Card
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(
                    Brush.linearGradient(
                      listOf(Color(0xFF2C3240), Color(0xFF1B1E26))
                    )
                  )
                  .border(1.5.dp, TitaniumSilver, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = profile.name.take(1).uppercase(),
                  fontSize = 24.sp,
                  fontWeight = FontWeight.Black,
                  color = TitaniumWhite
                )
              }

              Spacer(modifier = Modifier.width(14.dp))

              Column {
                Text(
                  text = profile.name,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "${profile.age} yrs • ${profile.gender} • ${profile.heightCm.toInt()} cm",
                  fontSize = 13.sp,
                  color = TextSecondary
                )
              }
            }

            IconButton(
              onClick = { showEditProfileDialog = true },
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceDark)
                .border(1.dp, BorderSubtle, CircleShape)
                .testTag("edit_profile_btn")
            ) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = TitaniumSilver, modifier = Modifier.size(18.dp))
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Goal pill & activity
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(SurfaceDark)
              .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Text(
              text = "GOAL: ${profile.fitnessGoal.uppercase()}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TitaniumWhite,
              letterSpacing = 0.5.sp
            )
          }
        }
      }

      // Live Health & Body Composition Metrics (BMI & Body Fat)
      Text(
        text = "BODY COMPOSITION & METRICS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TitaniumSilver,
        letterSpacing = 1.2.sp
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // BMI Card
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier
            .weight(1f)
            .testTag("bmi_metric_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "BMI INDEX",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${profile.bmi}",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            val bmiBadgeColor = when (profile.bmiCategory) {
              "Normal Weight" -> Color(0xFF10B981)
              "Underweight" -> Color(0xFFF59E0B)
              else -> Color(0xFF38BDF8)
            }
            Text(
              text = profile.bmiCategory,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = bmiBadgeColor
            )
          }
        }

        // Body Fat % Card
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier
            .weight(1f)
            .testTag("body_fat_metric_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "BODY FAT %",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = TextSecondary,
              letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "${profile.estimatedBodyFatPercent}%",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = if (profile.customBodyFatPercent != null) "Manual Override" else "Deurenberg Est.",
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = TextSecondary
            )
          }
        }
      }

      // Caloric Expenditure & Protein Targets
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "DAILY METABOLISM & NUTRITION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TitaniumSilver,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Maintenance TDEE", fontSize = 12.sp, color = TextSecondary)
              Text("${profile.tdeeCalories} kcal/day", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column {
              Text("Target Protein", fontSize = 12.sp, color = TextSecondary)
              Text("${profile.dailyProteinTargetGrams}g / day", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Column {
              Text("Water Target", fontSize = 12.sp, color = TextSecondary)
              Text("${profile.dailyWaterTargetLiters} L / day", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
          }
        }
      }

      // Dashboard Customization & Settings
      Text(
        text = "DASHBOARD CUSTOMIZATION",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TitaniumSilver,
        letterSpacing = 1.2.sp
      )

      Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Toggle: Overload targets
          SettingToggleRow(
            title = "Progressive Overload Targets",
            subtitle = "Show target reps and weight based on previous session",
            isChecked = dashboardPrefs.showOverloadTargets,
            onCheckedChange = {
              viewModel.updateDashboardPrefs(dashboardPrefs.copy(showOverloadTargets = it))
            }
          )

          HorizontalDivider(color = BorderSubtle)

          // Toggle: Split Banner
          SettingToggleRow(
            title = "Today's Split Banner",
            subtitle = "Show planned split and target muscle groups",
            isChecked = dashboardPrefs.showSplitBanner,
            onCheckedChange = {
              viewModel.updateDashboardPrefs(dashboardPrefs.copy(showSplitBanner = it))
            }
          )

          HorizontalDivider(color = BorderSubtle)

          // Toggle: Last Workout Card
          SettingToggleRow(
            title = "Last Workout Summary",
            subtitle = "Display last logged workout volume and exercise list",
            isChecked = dashboardPrefs.showLastWorkout,
            onCheckedChange = {
              viewModel.updateDashboardPrefs(dashboardPrefs.copy(showLastWorkout = it))
            }
          )

          HorizontalDivider(color = BorderSubtle)

          // Toggle: Rest Timer Widget
          SettingToggleRow(
            title = "In-Gym Rest Timer",
            subtitle = "Display tactical rest countdown between sets",
            isChecked = dashboardPrefs.showRestTimer,
            onCheckedChange = {
              viewModel.updateDashboardPrefs(dashboardPrefs.copy(showRestTimer = it))
            }
          )

          HorizontalDivider(color = BorderSubtle)

          // Preferred Split selector
          Column {
            Text(
              text = "Preferred Routine Split",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf("Push Pull Legs", "Upper Lower", "Arnold Split", "Bro Split", "Full Body").forEach { split ->
                val isSelected = dashboardPrefs.preferredSplit == split
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) TitaniumWhite else SurfaceDark)
                    .border(1.dp, if (isSelected) TitaniumWhite else BorderSubtle, RoundedCornerShape(8.dp))
                    .clickable {
                      viewModel.updateDashboardPrefs(dashboardPrefs.copy(preferredSplit = split))
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = split,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MatteBlack else TextSecondary
                  )
                }
              }
            }
          }

          HorizontalDivider(color = BorderSubtle)

          // Weight Units
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Weight Unit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("Switch between Kilograms (kg) and Pounds (lbs)", fontSize = 12.sp, color = TextSecondary)
            }
            Surface(
              onClick = { viewModel.toggleUnits() },
              shape = RoundedCornerShape(8.dp),
              color = SurfaceDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight)
            ) {
              Text(
                text = if (useLbs) "LBS" else "KG",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = TitaniumWhite,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
              )
            }
          }
        }
      }

      // Inbuilt Custom Exercises Manager
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "MY CUSTOM EXERCISES (${customExercises.size})",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = TitaniumSilver,
          letterSpacing = 1.2.sp
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (customExercises.isNotEmpty()) {
            Surface(
              onClick = { deleteTarget = DeleteTarget.AllCustomExercises },
              shape = RoundedCornerShape(8.dp),
              color = SurfaceDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
              modifier = Modifier.testTag("clear_custom_exercises_btn")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
              }
            }
          }

          Surface(
            onClick = { showAddExerciseDialog = true },
            shape = RoundedCornerShape(8.dp),
            color = SurfaceDark,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.testTag("profile_add_exercise_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TitaniumWhite)
            }
          }
        }
      }

      if (customExercises.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = CardDark,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "No custom exercises added yet. Tap \"Add\" or add exercises directly from the workout tab.",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(14.dp)
          )
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          customExercises.forEach { ex ->
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = CardDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = ex.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                  Text(text = "${ex.category} • ${ex.primaryMuscle}", fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(
                  onClick = { viewModel.deleteCustomExercise(ex.name) },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                }
              }
            }
          }
        }
      }

      // -------------------------------------------------------------
      // PAST LOGS & DATA MANAGEMENT (Clear past logs for everything)
      // -------------------------------------------------------------
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "DATA MANAGEMENT & CLEAR LOGS",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = TitaniumSilver,
          letterSpacing = 1.2.sp
        )
      }

      if (statusNotice != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(0xFF1E293B),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = statusNotice ?: "",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF10B981)
            )
            IconButton(
              onClick = { statusNotice = null },
              modifier = Modifier.size(24.dp)
            ) {
              Text("✕", color = TitaniumSilver, fontSize = 12.sp)
            }
          }
        }
      }

      // Master Wipe Option
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("wipe_all_data_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF7F1D1D).copy(alpha = 0.4f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.DeleteForever,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Wipe All Past Logs & Data",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFCA5A5)
              )
              Text(
                text = "Permanently erases all workout logs, sets, weight history, and PRs.",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = { deleteTarget = DeleteTarget.WipeEverything },
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFFEF4444),
              contentColor = TitaniumWhite
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("wipe_all_data_button")
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Delete All Logs & Reset", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }

      // 1. Past Workout Logs
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TitaniumSilver, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Past Workout Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                  text = "${workouts.size} sessions recorded • ${workouts.sumOf { it.session.totalSets }} total sets",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            if (workouts.isNotEmpty()) {
              Surface(
                onClick = { deleteTarget = DeleteTarget.AllWorkouts },
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier.testTag("clear_all_workouts_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
              }
            }
          }

          if (workouts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .clickable { expandWorkoutsList = !expandWorkoutsList }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (expandWorkoutsList) "Hide workout sessions list" else "Manage individual workouts (${workouts.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitaniumSilver
              )
              Icon(
                if (expandWorkoutsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TitaniumSilver,
                modifier = Modifier.size(16.dp)
              )
            }

            if (expandWorkoutsList) {
              Spacer(modifier = Modifier.height(8.dp))
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                workouts.forEach { w ->
                  val sessionDate = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(w.session.startTimeMillis))
                  val weightUnit = if (useLbs) "lbs" else "kg"
                  val displayVol = if (useLbs) (w.session.totalVolumeKg * 2.20462).toInt() else w.session.totalVolumeKg.toInt()
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
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
                      Column(modifier = Modifier.weight(1f)) {
                        Text(w.session.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                          text = "$sessionDate • $displayVol $weightUnit • ${w.session.totalSets} sets",
                          fontSize = 11.sp,
                          color = TextSecondary
                        )
                      }
                      IconButton(
                        onClick = {
                          deleteTarget = DeleteTarget.SingleWorkout(w.session.id, w.session.name)
                        },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          Icons.Default.DeleteOutline,
                          contentDescription = "Delete workout",
                          tint = Color(0xFFEF4444),
                          modifier = Modifier.size(16.dp)
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

      // 2. Body Weight Logs
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = TitaniumSilver, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Body Weight Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                  text = "${bodyWeights.size} weight entries recorded",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            if (bodyWeights.isNotEmpty()) {
              Surface(
                onClick = { deleteTarget = DeleteTarget.AllBodyWeights },
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier.testTag("clear_all_weights_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
              }
            }
          }

          if (bodyWeights.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .clickable { expandWeightsList = !expandWeightsList }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (expandWeightsList) "Hide weight logs list" else "Manage individual weight entries (${bodyWeights.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitaniumSilver
              )
              Icon(
                if (expandWeightsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TitaniumSilver,
                modifier = Modifier.size(16.dp)
              )
            }

            if (expandWeightsList) {
              Spacer(modifier = Modifier.height(8.dp))
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                bodyWeights.forEach { b ->
                  val dateStr = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(b.dateMillis))
                  val displayWeight = if (useLbs) "${(b.weightKg * 2.20462 * 10).roundToInt() / 10.0} lbs" else "${b.weightKg} kg"
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
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
                      Column(modifier = Modifier.weight(1f)) {
                        Text(displayWeight, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(dateStr, fontSize = 11.sp, color = TextSecondary)
                      }
                      IconButton(
                        onClick = {
                          deleteTarget = DeleteTarget.SingleBodyWeight(b.id, displayWeight)
                        },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          Icons.Default.DeleteOutline,
                          contentDescription = "Delete weight entry",
                          tint = Color(0xFFEF4444),
                          modifier = Modifier.size(16.dp)
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

      // 3. Personal Records (PR Vault)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = TitaniumSilver, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Personal Records (PR Vault)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                  text = "${prs.size} lifetime max lifts recorded",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            if (prs.isNotEmpty()) {
              Surface(
                onClick = { deleteTarget = DeleteTarget.AllPrs },
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier.testTag("clear_all_prs_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
              }
            }
          }

          if (prs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .clickable { expandPrsList = !expandPrsList }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (expandPrsList) "Hide PR vault list" else "Manage individual PRs (${prs.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitaniumSilver
              )
              Icon(
                if (expandPrsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TitaniumSilver,
                modifier = Modifier.size(16.dp)
              )
            }

            if (expandPrsList) {
              Spacer(modifier = Modifier.height(8.dp))
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                prs.forEach { pr ->
                  val unit = if (useLbs) "lbs" else "kg"
                  val displayWeight = if (useLbs) (pr.weightKg * 2.20462).toInt() else pr.weightKg.toInt()
                  val display1Rm = if (useLbs) (pr.estimated1RmKg * 2.20462).toInt() else pr.estimated1RmKg.toInt()
                  val prDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(pr.dateAchieved))
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
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
                      Column(modifier = Modifier.weight(1f)) {
                        Text(pr.exerciseName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                          text = "1RM: $display1Rm $unit • Best: $displayWeight $unit × ${pr.reps} reps • $prDate",
                          fontSize = 11.sp,
                          color = TextSecondary
                        )
                      }
                      IconButton(
                        onClick = {
                          deleteTarget = DeleteTarget.SinglePr(pr.exerciseName)
                        },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          Icons.Default.DeleteOutline,
                          contentDescription = "Delete PR",
                          tint = Color(0xFFEF4444),
                          modifier = Modifier.size(16.dp)
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

      // 4. Custom Routine Templates
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = TitaniumSilver, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("Custom Routine Templates", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                  text = "${routines.size} routine splits created",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            if (routines.isNotEmpty()) {
              Surface(
                onClick = { deleteTarget = DeleteTarget.AllRoutines },
                shape = RoundedCornerShape(8.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                modifier = Modifier.testTag("clear_all_routines_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Clear All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
              }
            }
          }

          if (routines.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
                .clickable { expandRoutinesList = !expandRoutinesList }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (expandRoutinesList) "Hide routines list" else "Manage individual routines (${routines.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitaniumSilver
              )
              Icon(
                if (expandRoutinesList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = TitaniumSilver,
                modifier = Modifier.size(16.dp)
              )
            }

            if (expandRoutinesList) {
              Spacer(modifier = Modifier.height(8.dp))
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                routines.forEach { r ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDark,
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
                      Column(modifier = Modifier.weight(1f)) {
                        Text(r.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${r.splitGroup} • ${r.description}", fontSize = 11.sp, color = TextSecondary)
                      }
                      IconButton(
                        onClick = {
                          deleteTarget = DeleteTarget.SingleRoutine(r.id, r.title)
                        },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          Icons.Default.DeleteOutline,
                          contentDescription = "Delete routine",
                          tint = Color(0xFFEF4444),
                          modifier = Modifier.size(16.dp)
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

      // JIIM AI's Recommendations for User
      Text(
        text = "JIIM AI'S PERSONALIZED COACH SUGGESTIONS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TitaniumSilver,
        letterSpacing = 1.2.sp
      )

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SuggestionBullet(
            title = "Target Rep Range",
            desc = if (profile.fitnessGoal.contains("Strength")) "3 to 6 reps with 80-88% 1RM for maximum neural drive." else "8 to 12 reps with 2-3s eccentric for maximum muscle hypertrophy."
          )
          SuggestionBullet(
            title = "Caloric Strategy",
            desc = if (profile.bmi > 25) "Eat at a slight deficit (-300 kcal/day) while keeping protein at ${profile.dailyProteinTargetGrams}g to retain muscle." else "Lean surplus (+250 kcal/day) targeting 0.25kg gain per week to maximize dry tissue."
          )
          SuggestionBullet(
            title = "Rest Periods",
            desc = "Rest 90-120s between compound movements (Bench, Squat, Pull-up) to replenish ATP-CP reserves."
          )
        }
      }

      // -------------------------------------------------------------
      // IN-APP UPDATE SECTION (GitHub: Moksh-05/JIIM)
      // -------------------------------------------------------------
      Text(
        text = "APP UPDATES & RELEASES",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = TitaniumSilver,
        letterSpacing = 1.2.sp
      )

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
        modifier = Modifier.fillMaxWidth().testTag("app_update_card")
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                  .background(SurfaceDark),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.SystemUpdate,
                  contentDescription = null,
                  tint = TitaniumWhite,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "JIIM Workout Tracker",
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                  color = TitaniumWhite
                )
                Text(
                  text = "Current: v$currentAppVersion • Moksh-05/JIIM",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = SurfaceDark,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
              Text(
                text = "v$currentAppVersion",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = PlatinumSteel,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              )
            }
          }

          when (val state = updateCheckState) {
            UpdateCheckState.Idle -> {
              Text(
                text = "Check for newer builds and features published directly on GitHub.",
                fontSize = 12.sp,
                color = TextSecondary
              )
              Button(
                onClick = { viewModel.checkForAppUpdates() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = TitaniumWhite,
                  contentColor = MatteBlack
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("check_updates_btn")
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Check for Updates", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            UpdateCheckState.Checking -> {
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TitaniumWhite, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Checking Moksh-05/JIIM releases...",
                  fontSize = 12.sp,
                  color = TextSecondary
                )
              }
            }

            is UpdateCheckState.UpToDate -> {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF10B981).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "App is up to date! You are running the latest version (${state.currentVersion}).",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Medium
                  )
                }
              }
              Button(
                onClick = { viewModel.checkForAppUpdates() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = CardElevated,
                  contentColor = TitaniumWhite
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
              ) {
                Text("Check Again", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
            }

            is UpdateCheckState.Available -> {
              val info = state.updateInfo
              val apkSizeStr = if (info.apkSizeBytes > 0) {
                "${(info.apkSizeBytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} MB"
              } else "APK"

              Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "New Update Available: ${info.tagName}",
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      color = TitaniumWhite
                    )
                    Text(
                      text = apkSizeStr,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF3B82F6)
                    )
                  }
                  if (info.releaseNotes.isNotBlank()) {
                    Text(
                      text = info.releaseNotes.take(200),
                      fontSize = 11.sp,
                      color = TextSecondary,
                      maxLines = 4
                    )
                  }
                }
              }

              Button(
                onClick = { viewModel.downloadAndInstallUpdate(info) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF3B82F6),
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("download_update_btn")
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("DOWNLOAD & INSTALL UPDATE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            is UpdateCheckState.Downloading -> {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Downloading Update APK...", fontSize = 12.sp, color = TextPrimary)
                  Text("${state.progressPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TitaniumWhite)
                }
                LinearProgressIndicator(
                  progress = { state.progressPercent / 100f },
                  modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                  color = Color(0xFF3B82F6),
                  trackColor = SurfaceDark
                )
                val downloadedMb = (state.downloadedBytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0
                val totalMb = (state.totalBytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0
                Text(
                  text = if (state.totalBytes > 0) "$downloadedMb MB of $totalMb MB" else "$downloadedMb MB downloaded",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            is UpdateCheckState.ReadyToInstall -> {
              Button(
                onClick = { viewModel.installDownloadedApk(state.apkFile) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF10B981),
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp).testTag("install_update_btn")
              ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("INSTALL UPDATE NOW", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            is UpdateCheckState.Error -> {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = state.message,
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444)
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = { viewModel.resetUpdateState() },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = CardElevated,
                    contentColor = TextSecondary
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(40.dp)
                ) {
                  Text("Dismiss", fontSize = 11.sp)
                }

                Button(
                  onClick = { viewModel.checkForAppUpdates() },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = TitaniumWhite,
                    contentColor = MatteBlack
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).height(40.dp)
                ) {
                  Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // Edit Profile Dialog
  if (showEditProfileDialog) {
    EditProfileModal(
      initialProfile = profile,
      onDismiss = { showEditProfileDialog = false },
      onSave = { updated ->
        viewModel.updateProfile(updated)
        showEditProfileDialog = false
      }
    )
  }

  // Add Custom Exercise Dialog
  if (showAddExerciseDialog) {
    AddCustomExerciseDialog(
      onDismiss = { showAddExerciseDialog = false },
      onSave = { name, category, muscle ->
        val success = viewModel.addCustomExercise(name, category, muscle)
        success
      }
    )
  }

  // Delete Logs Confirmation Dialog
  deleteTarget?.let { target ->
    val (dialogTitle, dialogDesc) = when (target) {
      is DeleteTarget.AllWorkouts -> "Clear All Past Workout Logs?" to "This will permanently delete all ${workouts.size} workout sessions and set logs. This action cannot be undone."
      is DeleteTarget.AllBodyWeights -> "Clear All Body Weight Logs?" to "This will permanently delete all ${bodyWeights.size} weight tracking entries."
      is DeleteTarget.AllPrs -> "Clear All Personal Records?" to "This will permanently delete all ${prs.size} exercise personal records and estimated 1RMs."
      is DeleteTarget.AllRoutines -> "Clear All Custom Routines?" to "This will permanently delete all ${routines.size} custom routine templates."
      is DeleteTarget.AllCustomExercises -> "Clear All Custom Exercises?" to "This will permanently delete all ${customExercises.size} custom exercises you created."
      is DeleteTarget.WipeEverything -> "⚠️ Wipe All Logs & Data?" to "This will permanently erase ALL workout logs, weight entries, PR vault records, and AI chat history. Your app will reset to a clean slate."
      is DeleteTarget.SingleWorkout -> "Delete Workout Session?" to "Delete \"${target.name}\"? All recorded sets from this session will be permanently removed."
      is DeleteTarget.SingleBodyWeight -> "Delete Weight Entry?" to "Delete weight log entry of ${target.weightStr}?"
      is DeleteTarget.SinglePr -> "Delete Personal Record?" to "Delete PR record for \"${target.exerciseName}\"?"
      is DeleteTarget.SingleRoutine -> "Delete Custom Routine?" to "Delete custom routine \"${target.name}\"?"
    }

    AlertDialog(
      onDismissRequest = { deleteTarget = null },
      containerColor = CardDark,
      titleContentColor = TitaniumWhite,
      textContentColor = TextSecondary,
      icon = {
        Icon(
          Icons.Default.Warning,
          contentDescription = null,
          tint = Color(0xFFEF4444),
          modifier = Modifier.size(28.dp)
        )
      },
      title = {
        Text(dialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        Text(dialogDesc, fontSize = 13.sp, color = TextSecondary)
      },
      confirmButton = {
        Button(
          onClick = {
            when (target) {
              is DeleteTarget.AllWorkouts -> {
                viewModel.deleteAllWorkouts()
                statusNotice = "All past workout logs have been deleted."
              }
              is DeleteTarget.AllBodyWeights -> {
                viewModel.deleteAllBodyWeights()
                statusNotice = "All body weight logs have been deleted."
              }
              is DeleteTarget.AllPrs -> {
                viewModel.deleteAllPrs()
                statusNotice = "All personal records have been deleted."
              }
              is DeleteTarget.AllRoutines -> {
                viewModel.deleteAllRoutines()
                statusNotice = "All custom routines have been deleted."
              }
              is DeleteTarget.AllCustomExercises -> {
                viewModel.deleteAllCustomExercises()
                statusNotice = "All custom exercises have been deleted."
              }
              is DeleteTarget.WipeEverything -> {
                viewModel.clearAllData(includeCustomExercises = true)
                statusNotice = "All logs and data have been wiped."
              }
              is DeleteTarget.SingleWorkout -> {
                viewModel.deleteWorkout(target.id)
                statusNotice = "Deleted workout \"${target.name}\"."
              }
              is DeleteTarget.SingleBodyWeight -> {
                viewModel.deleteBodyWeight(target.id)
                statusNotice = "Deleted weight log entry."
              }
              is DeleteTarget.SinglePr -> {
                viewModel.deletePr(target.exerciseName)
                statusNotice = "Deleted PR for \"${target.exerciseName}\"."
              }
              is DeleteTarget.SingleRoutine -> {
                viewModel.deleteRoutine(target.id)
                statusNotice = "Deleted routine \"${target.name}\"."
              }
            }
            deleteTarget = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEF4444),
            contentColor = TitaniumWhite
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("confirm_delete_btn")
        ) {
          Text("Delete Permanently", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { deleteTarget = null }
        ) {
          Text("Cancel", color = TitaniumSilver)
        }
      }
    )
  }
}

@Composable
private fun SettingToggleRow(
  title: String,
  subtitle: String,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
      Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(
      checked = isChecked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = MatteBlack,
        checkedTrackColor = TitaniumWhite,
        uncheckedThumbColor = TitaniumSilver,
        uncheckedTrackColor = SurfaceDark
      )
    )
  }
}

@Composable
private fun SuggestionBullet(title: String, desc: String) {
  Row(modifier = Modifier.fillMaxWidth()) {
    Text(text = "• ", fontWeight = FontWeight.Black, color = TitaniumSilver)
    Column {
      Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
      Text(text = desc, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileModal(
  initialProfile: UserProfile,
  onDismiss: () -> Unit,
  onSave: (UserProfile) -> Unit
) {
  var name by remember { mutableStateOf(initialProfile.name) }
  var dob by remember { mutableStateOf(initialProfile.birthDate) }
  var gender by remember { mutableStateOf(initialProfile.gender) }
  var heightStr by remember { mutableStateOf(initialProfile.heightCm.toInt().toString()) }
  var weightStr by remember { mutableStateOf(initialProfile.weightKg.toString()) }
  var goal by remember { mutableStateOf(initialProfile.fitnessGoal) }
  var customBfStr by remember { mutableStateOf(initialProfile.customBodyFatPercent?.toString() ?: "") }

  val goals = listOf(
    "Hypertrophy & Muscle Mass",
    "Strength & Power",
    "Fat Loss / Cut",
    "Recomposition"
  )

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = CardDark,
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Text(
          text = "Edit Lifter Profile",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        // Name
        Column {
          Text("NAME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SurfaceDark,
              unfocusedContainerColor = SurfaceDark,
              focusedBorderColor = TitaniumWhite,
              unfocusedBorderColor = BorderSubtle,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }

        // Date of Birth
        Column {
          Text("DATE OF BIRTH (YYYY-MM-DD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = dob,
            onValueChange = { dob = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SurfaceDark,
              unfocusedContainerColor = SurfaceDark,
              focusedBorderColor = TitaniumWhite,
              unfocusedBorderColor = BorderSubtle,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }

        // Gender Choice
        Column {
          Text("GENDER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
          Spacer(modifier = Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Male", "Female", "Other").forEach { g ->
              val isSel = gender == g
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) TitaniumWhite else SurfaceDark)
                  .border(1.dp, if (isSel) TitaniumWhite else BorderSubtle, RoundedCornerShape(8.dp))
                  .clickable { gender = g }
                  .padding(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Text(
                  text = g,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSel) MatteBlack else TextSecondary,
                  fontSize = 12.sp
                )
              }
            }
          }
        }

        // Height & Weight
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Column(modifier = Modifier.weight(1f)) {
            Text("HEIGHT (CM)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = heightStr,
              onValueChange = { heightStr = it },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = TitaniumWhite,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text("WEIGHT (KG)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = weightStr,
              onValueChange = { weightStr = it },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = TitaniumWhite,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              )
            )
          }
        }

        // Fitness Goal
        Column {
          Text("FITNESS GOAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            goals.forEach { g ->
              val isSel = goal == g
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSel) TitaniumWhite else SurfaceDark)
                  .border(1.dp, if (isSel) TitaniumWhite else BorderSubtle, RoundedCornerShape(8.dp))
                  .clickable { goal = g }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = g,
                  fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSel) MatteBlack else TextSecondary,
                  fontSize = 11.sp
                )
              }
            }
          }
        }

        // Custom Body Fat % (Optional)
        Column {
          Text("BODY FAT % (OPTIONAL OVERRIDE)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TitaniumSilver)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = customBfStr,
            onValueChange = { customBfStr = it },
            placeholder = { Text("Leave blank to auto-calculate", color = TextSecondary, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SurfaceDark,
              unfocusedContainerColor = SurfaceDark,
              focusedBorderColor = TitaniumWhite,
              unfocusedBorderColor = BorderSubtle,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            )
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
          onClick = {
            val h = heightStr.toDoubleOrNull() ?: initialProfile.heightCm
            val w = weightStr.toDoubleOrNull() ?: initialProfile.weightKg
            val bf = customBfStr.toDoubleOrNull()
            val updated = initialProfile.copy(
              name = name.ifBlank { "Lifter" },
              birthDate = dob.ifBlank { "2000-05-15" },
              gender = gender,
              heightCm = h,
              weightKg = w,
              fitnessGoal = goal,
              customBodyFatPercent = bf
            )
            onSave(updated)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = TitaniumWhite,
            contentColor = MatteBlack
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Text("Save Profile Changes", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
