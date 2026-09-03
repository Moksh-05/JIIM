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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import com.example.data.UserProfile
import com.example.ui.components.AddCustomExerciseDialog
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite
import com.example.viewmodel.GymViewModel

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

  var showEditProfileDialog by remember { mutableStateOf(false) }
  var showAddExerciseDialog by remember { mutableStateOf(false) }

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
