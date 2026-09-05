package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.ui.components.MinimalDumbbellIcon
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.PlatinumSteel
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite
import com.example.viewmodel.GymViewModel
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier,
  onFinish: () -> Unit = {}
) {
  val existingProfile by viewModel.userProfile.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()

  var step by remember { mutableIntStateOf(0) } // 0: Intro, 1: Biometrics, 2: Goals, 3: Blueprint

  // Editable fields
  var name by remember(existingProfile) { mutableStateOf(existingProfile.name) }
  var ageText by remember(existingProfile) { mutableStateOf(existingProfile.age.toString()) }
  var gender by remember(existingProfile) { mutableStateOf(existingProfile.gender) }

  // Height & Weight units
  var inputInLbs by remember { mutableStateOf(useLbs) }
  var inputWeightText by remember(existingProfile, inputInLbs) {
    val displayed = if (inputInLbs) existingProfile.weightKg * 2.20462 else existingProfile.weightKg
    mutableStateOf(displayed.roundToInt().toString())
  }
  var inputHeightCmText by remember(existingProfile) {
    mutableStateOf(existingProfile.heightCm.roundToInt().toString())
  }

  // Goals
  var selectedGoal by remember(existingProfile) { mutableStateOf(existingProfile.fitnessGoal) }
  var targetWeightText by remember(existingProfile, inputInLbs) {
    val target = existingProfile.targetWeightKg
    val displayed = if (inputInLbs) target * 2.20462 else target
    mutableStateOf(displayed.roundToInt().toString())
  }
  var trainingDays by remember(existingProfile) { mutableIntStateOf(existingProfile.trainingDaysPerWeek) }
  var activityLevel by remember(existingProfile) { mutableStateOf(existingProfile.activityLevel) }

  // Computed UserProfile in real-time
  val parsedWeightKg = remember(inputWeightText, inputInLbs) {
    val raw = inputWeightText.toDoubleOrNull() ?: 75.0
    if (inputInLbs) raw / 2.20462 else raw
  }
  val parsedTargetWeightKg = remember(targetWeightText, inputInLbs) {
    val raw = targetWeightText.toDoubleOrNull() ?: parsedWeightKg
    if (inputInLbs) raw / 2.20462 else raw
  }
  val parsedHeightCm = remember(inputHeightCmText) {
    inputHeightCmText.toDoubleOrNull() ?: 178.0
  }
  val parsedAge = remember(ageText) {
    ageText.toIntOrNull()?.coerceIn(12, 100) ?: 24
  }

  val birthYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - parsedAge
  val calculatedBirthDate = "$birthYear-01-15"

  val temporaryProfile = remember(
    name, calculatedBirthDate, gender, parsedHeightCm, parsedWeightKg,
    parsedTargetWeightKg, trainingDays, activityLevel, selectedGoal
  ) {
    UserProfile(
      name = name.ifBlank { "Lifter" },
      birthDate = calculatedBirthDate,
      gender = gender,
      heightCm = parsedHeightCm,
      weightKg = parsedWeightKg,
      targetWeightKg = parsedTargetWeightKg,
      trainingDaysPerWeek = trainingDays,
      activityLevel = activityLevel,
      fitnessGoal = selectedGoal
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MatteBlack)
  ) {
    // Top Bar with step indicator & back button
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      if (step > 0) {
        IconButton(
          onClick = { step-- },
          modifier = Modifier.testTag("onboarding_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowBack,
            contentDescription = "Back",
            tint = TitaniumWhite
          )
        }
      } else {
        Spacer(modifier = Modifier.size(48.dp))
      }

      // Progress Steps Dots
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        (0..3).forEach { index ->
          Box(
            modifier = Modifier
              .size(width = if (step == index) 24.dp else 8.dp, height = 8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(
                if (step == index) TitaniumWhite else SurfaceDark
              )
          )
        }
      }

      if (step < 3) {
        TextButton(
          onClick = {
            viewModel.completeOnboarding(temporaryProfile)
            onFinish()
          }
        ) {
          Text("Skip", color = TextTertiary, fontSize = 13.sp)
        }
      } else {
        Spacer(modifier = Modifier.size(48.dp))
      }
    }

    AnimatedContent(
      targetState = step,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      modifier = Modifier.weight(1f),
      label = "onboarding_step_transition"
    ) { targetStep ->
      when (targetStep) {
        0 -> StepIntro(
          onNext = { step = 1 }
        )
        1 -> StepBiometrics(
          name = name,
          onNameChange = { name = it },
          ageText = ageText,
          onAgeChange = { ageText = it },
          gender = gender,
          onGenderChange = { gender = it },
          inputInLbs = inputInLbs,
          onToggleLbs = {
            val curKg = parsedWeightKg
            inputInLbs = !inputInLbs
            inputWeightText = if (inputInLbs) (curKg * 2.20462).roundToInt().toString() else curKg.roundToInt().toString()
          },
          weightText = inputWeightText,
          onWeightChange = { inputWeightText = it },
          heightCmText = inputHeightCmText,
          onHeightChange = { inputHeightCmText = it },
          onNext = { step = 2 }
        )
        2 -> StepGoals(
          selectedGoal = selectedGoal,
          onSelectGoal = { selectedGoal = it },
          targetWeightText = targetWeightText,
          onTargetWeightChange = { targetWeightText = it },
          trainingDays = trainingDays,
          onTrainingDaysChange = { trainingDays = it },
          activityLevel = activityLevel,
          onActivityLevelChange = { activityLevel = it },
          useLbs = inputInLbs,
          onNext = { step = 3 }
        )
        3 -> StepBlueprint(
          profile = temporaryProfile,
          useLbs = inputInLbs,
          onFinish = {
            viewModel.completeOnboarding(temporaryProfile)
            onFinish()
          }
        )
      }
    }
  }
}

// -------------------------------------------------------------------------------------
// STEP 0: INTRODUCTION & PRODUCT TOUR
// -------------------------------------------------------------------------------------
@Composable
private fun StepIntro(onNext: () -> Unit) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Spacer(modifier = Modifier.height(20.dp))

    // Bold App Emblem matching uploaded icon
    Box(
      modifier = Modifier
        .size(90.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.linearGradient(listOf(Color(0xFF1E222A), Color(0xFF0F1115)))
        )
        .border(1.5.dp, BorderHighlight, RoundedCornerShape(24.dp)),
      contentAlignment = Alignment.Center
    ) {
      MinimalDumbbellIcon(size = 46.dp, tint = TitaniumWhite, accentTint = PlatinumSteel)
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(
      text = "JIIM",
      fontSize = 32.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 4.sp,
      color = TitaniumWhite
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Precision Strength & Progression Architecture",
      fontSize = 14.sp,
      color = PlatinumSteel,
      fontWeight = FontWeight.Medium
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Value Pillars
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      IntroFeatureCard(
        icon = Icons.Default.Mic,
        title = "Voice & Free-Form Rambler",
        description = "Simply talk or type: \"Did 3 sets of bench with 225 for 8, felt solid\". The AI parses exercises, weights, and sets automatically."
      )

      IntroFeatureCard(
        icon = Icons.Default.Timeline,
        title = "Calculated Progressive Overload",
        description = "Never guess your next session. JIIM computes dynamic weight and rep targets tailored to your historical performance."
      )

      IntroFeatureCard(
        icon = Icons.Default.ElectricBolt,
        title = "MacroFactor-Grade Analytics",
        description = "Volume adherence heatmaps, metabolic TDEE estimation, target protein thresholds, and Plateau Radar detection."
      )

      IntroFeatureCard(
        icon = Icons.Default.Security,
        title = "100% Offline & Private",
        description = "Your workout telemetry stays strictly on your phone. Works seamlessly in basements and gyms with zero cell service."
      )
    }

    Spacer(modifier = Modifier.height(36.dp))

    Button(
      onClick = onNext,
      colors = ButtonDefaults.buttonColors(
        containerColor = TitaniumWhite,
        contentColor = MatteBlack
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("onboarding_intro_continue_button")
    ) {
      Text("Set Up My Profile & Goals", fontWeight = FontWeight.Black, fontSize = 15.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

@Composable
private fun IntroFeatureCard(
  icon: ImageVector,
  title: String,
  description: String
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = CardDark,
    border = BorderStroke(1.dp, BorderSubtle),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
        .background(SurfaceDark)
        .border(1.dp, BorderHighlight, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(20.dp))
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column {
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = TitaniumWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = description,
          fontSize = 12.sp,
          lineHeight = 17.sp,
          color = TextSecondary
        )
      }
    }
  }
}

// -------------------------------------------------------------------------------------
// STEP 1: BIOMETRICS & PERSONAL STATS
// -------------------------------------------------------------------------------------
@Composable
private fun StepBiometrics(
  name: String,
  onNameChange: (String) -> Unit,
  ageText: String,
  onAgeChange: (String) -> Unit,
  gender: String,
  onGenderChange: (String) -> Unit,
  inputInLbs: Boolean,
  onToggleLbs: () -> Unit,
  weightText: String,
  onWeightChange: (String) -> Unit,
  heightCmText: String,
  onHeightChange: (String) -> Unit,
  onNext: () -> Unit
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "STEP 1 OF 3",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
      color = PlatinumSteel
    )
    Text(
      text = "Your Biometrics",
      fontSize = 24.sp,
      fontWeight = FontWeight.Black,
      color = TitaniumWhite
    )
    Text(
      text = "Used to calibrate your baseline metabolic rate and strength curves.",
      fontSize = 13.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Name Field
    Text("What should we call you?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = name,
      onValueChange = onNameChange,
      singleLine = true,
      placeholder = { Text("e.g. Alex", color = TextTertiary) },
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TitaniumWhite,
        unfocusedBorderColor = BorderSubtle,
        focusedContainerColor = CardDark,
        unfocusedContainerColor = CardDark,
        focusedTextColor = TitaniumWhite,
        unfocusedTextColor = TitaniumWhite
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("onboarding_name_input")
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Age Field
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Age", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = ageText,
          onValueChange = onAgeChange,
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TitaniumWhite,
            unfocusedBorderColor = BorderSubtle,
            focusedContainerColor = CardDark,
            unfocusedContainerColor = CardDark,
            focusedTextColor = TitaniumWhite,
            unfocusedTextColor = TitaniumWhite
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_age_input")
        )
      }

      // Gender Selector
      Column(modifier = Modifier.weight(1.5f)) {
        Text("Biological Sex", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          listOf("Male", "Female").forEach { g ->
            val isSelected = gender.equals(g, ignoreCase = true)
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) CardElevated else Color.Transparent)
                .clickable { onGenderChange(g) },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = g,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TitaniumWhite else TextSecondary
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Weight Input with LBS / KG toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Current Weight", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
      Surface(
        onClick = onToggleLbs,
        shape = RoundedCornerShape(8.dp),
        color = CardElevated,
        border = BorderStroke(1.dp, BorderHighlight)
      ) {
        Text(
          text = if (inputInLbs) "Unit: LBS" else "Unit: KG",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = TitaniumWhite,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
      }
    }
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = weightText,
      onValueChange = onWeightChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      trailingIcon = {
        Text(if (inputInLbs) "lbs" else "kg", color = PlatinumSteel, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
      },
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TitaniumWhite,
        unfocusedBorderColor = BorderSubtle,
        focusedContainerColor = CardDark,
        unfocusedContainerColor = CardDark,
        focusedTextColor = TitaniumWhite,
        unfocusedTextColor = TitaniumWhite
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_weight_input")
    )

    Spacer(modifier = Modifier.height(18.dp))

    // Height Input
    Text("Height (cm)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = heightCmText,
      onValueChange = onHeightChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      trailingIcon = {
        Text("cm", color = PlatinumSteel, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
      },
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TitaniumWhite,
        unfocusedBorderColor = BorderSubtle,
        focusedContainerColor = CardDark,
        unfocusedContainerColor = CardDark,
        focusedTextColor = TitaniumWhite,
        unfocusedTextColor = TitaniumWhite
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_height_input")
    )

    Spacer(modifier = Modifier.height(36.dp))

    Button(
      onClick = onNext,
      colors = ButtonDefaults.buttonColors(
        containerColor = TitaniumWhite,
        contentColor = MatteBlack
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_step1_next_button")
    ) {
      Text("Next: Select Goals", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

// -------------------------------------------------------------------------------------
// STEP 2: GOALS & TRAINING FREQUENCY
// -------------------------------------------------------------------------------------
@Composable
private fun StepGoals(
  selectedGoal: String,
  onSelectGoal: (String) -> Unit,
  targetWeightText: String,
  onTargetWeightChange: (String) -> Unit,
  trainingDays: Int,
  onTrainingDaysChange: (Int) -> Unit,
  activityLevel: String,
  onActivityLevelChange: (String) -> Unit,
  useLbs: Boolean,
  onNext: () -> Unit
) {
  val scrollState = rememberScrollState()

  val goals = listOf(
    Triple("Hypertrophy & Muscle Mass", "Maximize hypertrophy with progressive overload volume.", Icons.Default.FitnessCenter),
    Triple("Strength & Power", "Prioritize compound 1RM strength and neuromuscular adaptation.", Icons.Default.Timeline),
    Triple("Fat Loss / Cut", "Shed body fat while preserving lean muscle mass.", Icons.Default.LocalFireDepartment),
    Triple("Body Recomposition", "Simultaneously build strength while leaning out body composition.", Icons.Default.ElectricBolt)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "STEP 2 OF 3",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
      color = PlatinumSteel
    )
    Text(
      text = "Define Your Goals",
      fontSize = 24.sp,
      fontWeight = FontWeight.Black,
      color = TitaniumWhite
    )
    Text(
      text = "Customizes progressive overload algorithms and caloric targets.",
      fontSize = 13.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Goal Cards
    Text("Primary Objective", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      goals.forEach { (title, desc, icon) ->
        val isSelected = selectedGoal.equals(title, ignoreCase = true)
        Surface(
          onClick = { onSelectGoal(title) },
          shape = RoundedCornerShape(14.dp),
          color = if (isSelected) CardElevated else CardDark,
          border = BorderStroke(1.5.dp, if (isSelected) TitaniumWhite else BorderSubtle),
          modifier = Modifier.fillMaxWidth().testTag("goal_card_$title")
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) TitaniumWhite else SurfaceDark),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MatteBlack else TitaniumSilver,
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TitaniumWhite
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary
              )
            }

            if (isSelected) {
              Icon(Icons.Default.Check, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(20.dp))
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Target Goal Weight
    Text("Target Goal Weight (${if (useLbs) "lbs" else "kg"})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = targetWeightText,
      onValueChange = onTargetWeightChange,
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TitaniumWhite,
        unfocusedBorderColor = BorderSubtle,
        focusedContainerColor = CardDark,
        unfocusedContainerColor = CardDark,
        focusedTextColor = TitaniumWhite,
        unfocusedTextColor = TitaniumWhite
      ),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().testTag("onboarding_target_weight_input")
    )

    Spacer(modifier = Modifier.height(22.dp))

    // Weekly Training Frequency (3, 4, 5, 6 days)
    Text("Planned Weekly Training Sessions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(3, 4, 5, 6).forEach { days ->
        val isSelected = trainingDays == days
        Surface(
          onClick = { onTrainingDaysChange(days) },
          shape = RoundedCornerShape(12.dp),
          color = if (isSelected) TitaniumWhite else CardDark,
          border = BorderStroke(1.dp, if (isSelected) TitaniumWhite else BorderSubtle),
          modifier = Modifier.weight(1f).height(46.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "$days Days",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = if (isSelected) MatteBlack else TitaniumWhite
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(36.dp))

    Button(
      onClick = onNext,
      colors = ButtonDefaults.buttonColors(
        containerColor = TitaniumWhite,
        contentColor = MatteBlack
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("onboarding_step2_next_button")
    ) {
      Text("View My Blueprint", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

// -------------------------------------------------------------------------------------
// STEP 3: BLUEPRINT & SUMMARY (MacroFactor Aesthetic)
// -------------------------------------------------------------------------------------
@Composable
private fun StepBlueprint(
  profile: UserProfile,
  useLbs: Boolean,
  onFinish: () -> Unit
) {
  val scrollState = rememberScrollState()

  val displayWeight = if (useLbs) "${(profile.weightKg * 2.20462).roundToInt()} lbs" else "${profile.weightKg.roundToInt()} kg"
  val displayTarget = if (useLbs) "${(profile.targetWeightKg * 2.20462).roundToInt()} lbs" else "${profile.targetWeightKg.roundToInt()} kg"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = "STEP 3 OF 3",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.5.sp,
      color = PlatinumSteel
    )
    Text(
      text = "Your Blueprint",
      fontSize = 24.sp,
      fontWeight = FontWeight.Black,
      color = TitaniumWhite
    )
    Text(
      text = "Tailored to ${profile.name} • ${profile.fitnessGoal}",
      fontSize = 13.sp,
      color = TextSecondary
    )

    Spacer(modifier = Modifier.height(20.dp))

    // MacroFactor-style Main Metrics Grid
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = SurfaceDark,
      border = BorderStroke(1.dp, BorderHighlight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("TARGET DAILY ENERGY", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = PlatinumSteel)
            Text(
              text = "${profile.targetCalories} kcal",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = TitaniumWhite
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardElevated,
            border = BorderStroke(1.dp, BorderHighlight)
          ) {
            Text(
              text = if (profile.targetCalories > profile.tdeeCalories) "+Surplus" else if (profile.targetCalories < profile.tdeeCalories) "-Deficit" else "Maintenance",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF05DF72),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdown Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          BlueprintStatCard(
            label = "MAINTENANCE",
            value = "${profile.tdeeCalories} kcal",
            modifier = Modifier.weight(1f)
          )
          BlueprintStatCard(
            label = "PROTEIN GOAL",
            value = "${profile.dailyProteinTargetGrams}g/day",
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          BlueprintStatCard(
            label = "WEIGHT TARGET",
            value = "$displayWeight → $displayTarget",
            modifier = Modifier.weight(1f)
          )
          BlueprintStatCard(
            label = "WEEKLY LIFTING",
            value = "${profile.trainingDaysPerWeek} Days/Wk",
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Guidance Note Card
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = CardDark),
      border = BorderStroke(0.5.dp, BorderSubtle),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "🎯 PROGRESSIVE OVERLOAD ACTIVATED",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = PlatinumSteel
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "As you log workouts using either The Rambler (speech/text) or the Set Logger, JIIM continuously adapts your overload curves to match your actual training velocity.",
          fontSize = 12.sp,
          lineHeight = 17.sp,
          color = TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Button(
      onClick = onFinish,
      colors = ButtonDefaults.buttonColors(
        containerColor = TitaniumWhite,
        contentColor = MatteBlack
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("onboarding_enter_dashboard_button")
    ) {
      Text("Enter Dashboard", fontWeight = FontWeight.Black, fontSize = 16.sp)
      Spacer(modifier = Modifier.width(8.dp))
      Icon(Icons.AutoMirrored.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}

@Composable
private fun BlueprintStatCard(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = CardDark,
    border = BorderStroke(0.5.dp, BorderSubtle),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = PlatinumSteel)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = TitaniumWhite)
    }
  }
}
