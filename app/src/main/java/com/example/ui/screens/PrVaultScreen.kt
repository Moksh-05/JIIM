package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ExerciseLibrary
import com.example.model.ExercisePr
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GoldPr
import com.example.ui.theme.VoltLime
import com.example.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PrVaultScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val prs by viewModel.allPrs.collectAsStateWithLifecycle()
  val isLbs by viewModel.useLbs.collectAsStateWithLifecycle()

  var showAddPrDialog by remember { mutableStateOf(false) }

  // 1RM Calculator interactive states
  var calcWeight by remember { mutableStateOf("80") }
  var calcReps by remember { mutableStateOf("5") }

  val weightNum = calcWeight.toDoubleOrNull() ?: 0.0
  val repsNum = calcReps.toIntOrNull() ?: 0
  val calculated1Rm = remember(weightNum, repsNum) {
    viewModel.calculate1Rm(weightNum, repsNum)
  }

  // Key compound PRs
  val benchPr = prs.find { it.exerciseName.contains("Bench", ignoreCase = true) }
  val squatPr = prs.find { it.exerciseName.contains("Squat", ignoreCase = true) }
  val deadliftPr = prs.find { it.exerciseName.contains("Deadlift", ignoreCase = true) }
  val ohpPr = prs.find { it.exerciseName.contains("Overhead", ignoreCase = true) || it.exerciseName.contains("Shoulder Press", ignoreCase = true) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(12.dp))
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "PR VAULT",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
          )
          Text(
            text = "Personal records & 1RM projection",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
          )
        }

        Button(
          onClick = { showAddPrDialog = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E2232),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2C3246)),
          modifier = Modifier.testTag("add_custom_pr_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("LOG PR", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }
      }
    }

    // BIG FOUR COMPOUNDS SHOWCASE
    item {
      Text(
        text = "COMPOUND BENCHMARKS",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BigLiftCard(
          title = "Bench Press",
          pr = benchPr,
          isLbs = isLbs,
          modifier = Modifier.weight(1f)
        )
        BigLiftCard(
          title = "Back Squat",
          pr = squatPr,
          isLbs = isLbs,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        BigLiftCard(
          title = "Deadlift",
          pr = deadliftPr,
          isLbs = isLbs,
          modifier = Modifier.weight(1f)
        )
        BigLiftCard(
          title = "Overhead Press",
          pr = ohpPr,
          isLbs = isLbs,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // 1RM CALCULATOR CARD
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .background(Color(0xFF181B26), CircleShape)
                .border(0.5.dp, Color(0xFF282D3E), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Calculate, contentDescription = null, tint = VoltLime, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "1RM CALCULATOR",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
              )
              Text(
                text = "Brzycki formula projection",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = calcWeight,
              onValueChange = { calcWeight = it },
              label = { Text("Weight (${if (isLbs) "lbs" else "kg"})", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoltLime,
                unfocusedBorderColor = Color(0xFF262B3B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
              value = calcReps,
              onValueChange = { calcReps = it },
              label = { Text("Reps", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoltLime,
                unfocusedBorderColor = Color(0xFF262B3B),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // 1RM Result Banner
          val display1Rm = if (isLbs) (calculated1Rm * 2.20462).roundToInt() else calculated1Rm.roundToInt()
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF161924),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("ESTIMATED 1RM", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                Text(
                  text = "$display1Rm ${if (isLbs) "lbs" else "kg"}",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = VoltLime
                )
              }

              Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF181B26),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF282D3E))
              ) {
                Text(
                  text = "Brzycki Model",
                  color = Color(0xFF94A3B8),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Training Percentages Table
          Text("TRAINING PERCENTAGES", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
          Spacer(modifier = Modifier.height(6.dp))

          val percentages = listOf(
            Pair("95%", (display1Rm * 0.95).roundToInt() to "2 reps"),
            Pair("90%", (display1Rm * 0.90).roundToInt() to "3-4 reps"),
            Pair("85%", (display1Rm * 0.85).roundToInt() to "5-6 reps"),
            Pair("80%", (display1Rm * 0.80).roundToInt() to "7-8 reps"),
            Pair("75%", (display1Rm * 0.75).roundToInt() to "9-10 reps"),
            Pair("70%", (display1Rm * 0.70).roundToInt() to "11-12 reps")
          )

          percentages.chunked(3).forEach { rowList ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              rowList.forEach { (pct, data) ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFF161924),
                  border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
                  modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp)
                ) {
                  Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pct, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VoltLime)
                    Text("${data.first} ${if (isLbs) "lbs" else "kg"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(data.second, fontSize = 9.sp, color = Color(0xFF64748B))
                  }
                }
              }
            }
          }
        }
      }
    }

    // ALL PERSONAL RECORDS LIST
    item {
      Text(
        text = "LOGGED PERSONAL RECORDS (${prs.size})",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        letterSpacing = 1.sp
      )
    }

    items(prs) { pr ->
      PrItemRow(pr = pr, isLbs = isLbs)
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }

  // Add Custom PR Dialog
  if (showAddPrDialog) {
    AddPrDialog(
      onDismiss = { showAddPrDialog = false },
      onSave = { name, weight, reps ->
        viewModel.recordCustomPr(name, weight, reps)
        showAddPrDialog = false
      }
    )
  }
}

@Composable
fun BigLiftCard(
  title: String,
  pr: ExercisePr?,
  isLbs: Boolean,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      if (pr != null) {
        val w = if (isLbs) (pr.weightKg * 2.20462).toInt() else pr.weightKg.toInt()
        val oneRm = if (isLbs) (pr.estimated1RmKg * 2.20462).toInt() else pr.estimated1RmKg.toInt()
        Text(
          text = "$w ${if (isLbs) "lbs" else "kg"}",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "x ${pr.reps} (1RM: $oneRm)",
          fontSize = 11.sp,
          color = VoltLime,
          fontWeight = FontWeight.Medium
        )
      } else {
        Text(
          text = "—",
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF64748B)
        )
        Text(
          text = "No entry",
          fontSize = 11.sp,
          color = Color(0xFF64748B)
        )
      }
    }
  }
}

@Composable
fun PrItemRow(
  pr: ExercisePr,
  isLbs: Boolean
) {
  val dateFormatted = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(pr.dateAchieved))
  val w = if (isLbs) (pr.weightKg * 2.20462).roundToInt() else pr.weightKg.roundToInt()
  val oneRm = if (isLbs) (pr.estimated1RmKg * 2.20462).roundToInt() else pr.estimated1RmKg.roundToInt()

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .background(Color(0xFF181B26), CircleShape)
            .border(0.5.dp, Color(0xFF282D3E), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = VoltLime, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = pr.exerciseName,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 13.sp
          )
          Text(
            text = "$dateFormatted • 1RM: $oneRm ${if (isLbs) "lbs" else "kg"}",
            fontSize = 11.sp,
            color = Color(0xFF7A8196)
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = "$w ${if (isLbs) "lbs" else "kg"}",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "x ${pr.reps} reps",
          fontSize = 11.sp,
          color = VoltLime,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}

@Composable
fun AddPrDialog(
  onDismiss: () -> Unit,
  onSave: (name: String, weight: Double, reps: Int) -> Unit
) {
  var exerciseName by remember { mutableStateOf("") }
  var weight by remember { mutableStateOf("") }
  var reps by remember { mutableStateOf("1") }

  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF141622),
    shape = RoundedCornerShape(16.dp),
    title = { Text("Log Personal Record", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = exerciseName,
          onValueChange = { exerciseName = it },
          label = { Text("Exercise Name", fontSize = 11.sp) },
          placeholder = { Text("e.g. Incline Dumbbell Press", fontSize = 12.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoltLime,
            unfocusedBorderColor = Color(0xFF262B3B),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = weight,
          onValueChange = { weight = it },
          label = { Text("Weight (kg)", fontSize = 11.sp) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoltLime,
            unfocusedBorderColor = Color(0xFF262B3B),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = reps,
          onValueChange = { reps = it },
          label = { Text("Reps Completed", fontSize = 11.sp) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoltLime,
            unfocusedBorderColor = Color(0xFF262B3B),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (exerciseName.isNotBlank() && weight.isNotBlank()) {
            onSave(exerciseName.trim(), weight.toDoubleOrNull() ?: 0.0, reps.toIntOrNull() ?: 1)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = VoltLime, contentColor = Color(0xFF0B0C10)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("SAVE PR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("CANCEL", color = Color(0xFF94A3B8), fontSize = 12.sp)
      }
    }
  )
}
