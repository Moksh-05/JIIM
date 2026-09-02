package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ParsedExerciseLog
import com.example.model.ParsedSetLog
import com.example.model.ParsedWorkoutRant
import com.example.model.WorkoutWithExercises
import com.example.ui.theme.GoldPr
import com.example.ui.theme.VoltLime
import com.example.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkoutScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val workouts by viewModel.allWorkouts.collectAsState()
  val isParsingRant by viewModel.isParsingRant.collectAsState()
  val parsedRant by viewModel.parsedRant.collectAsState()
  val aiAnalysis by viewModel.aiAnalysis.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()

  var rantInput by remember { mutableStateOf("") }
  var showSplitDialog by remember { mutableStateOf(false) }

  // Parsed Rant Confirmation Dialog
  if (parsedRant != null) {
    ParsedRantReviewDialog(
      parsed = parsedRant!!,
      useLbs = useLbs,
      onConfirm = { confirmedRant ->
        viewModel.saveLoggedWorkout(confirmedRant)
        rantInput = ""
      },
      onDismiss = { viewModel.clearParsedRant() }
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(12.dp))
      // Screen Title & Subtitle
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "JIIM",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
          )
          Text(
            text = "Intelligent session logger & volume tracking",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
          )
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF141620),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF262B3B))
        ) {
          Text(
            text = "${workouts.size} Sessions",
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
          )
        }
      }
    }

    // Hero: Quick Natural Session Logger Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF272B3C)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("gym_rant_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .background(Color(0xFF1A1D29), CircleShape)
                  .border(0.5.dp, Color(0xFF2D3246), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.FitnessCenter,
                  contentDescription = "Quick Log",
                  tint = VoltLime,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "QUICK SESSION LOG",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = Color.White,
                  letterSpacing = 0.5.sp
                )
                Text(
                  text = "Type or paste your sets in natural text",
                  fontSize = 11.sp,
                  color = Color(0xFF94A3B8)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isOnline) Color(0xFF131F17) else Color(0xFF221E14),
              border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (isOnline) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
              )
            ) {
              Text(
                text = if (isOnline) "JIIM AI" else "Offline",
                color = if (isOnline) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Text area for rant
          OutlinedTextField(
            value = rantInput,
            onValueChange = { rantInput = it },
            placeholder = {
              Text(
                text = "e.g., Bench press 85kg 3x8, Incline dumbbell 32kg 10,10,8, Overhead press 55kg 5,5,5, Lateral raises 14kg 4x15",
                color = Color(0xFF555A6E),
                fontSize = 13.sp,
                lineHeight = 18.sp
              )
            },
            minLines = 3,
            maxLines = 6,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF0E1017),
              unfocusedContainerColor = Color(0xFF0E1017),
              focusedBorderColor = VoltLime.copy(alpha = 0.7f),
              unfocusedBorderColor = Color(0xFF242838),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("gym_rant_input")
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Preset Chips (Clean, no emojis)
          Text(
            text = "Quick templates:",
            color = Color(0xFF71778E),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(6.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            item {
              PresetRantChip("Push") {
                rantInput = "Push day: Bench press 85kg 3x8, Incline DB 32kg 10, 10, 8 reps, Overhead press 55kg 3x6, Lateral raises 14kg 4x15, Tricep pushdowns 35kg 12 reps."
              }
            }
            item {
              PresetRantChip("Legs") {
                rantInput = "Leg day: Barbell squat 115kg 5x5, Romanian deadlift 100kg 3x8, Leg press 220kg 3x10, Standing calf raise 80kg 4x15."
              }
            }
            item {
              PresetRantChip("Upper & Arms") {
                rantInput = "Upper and arms: Overhead barbell press 55kg 3x6, Lateral raises 14kg 4x15, Barbell bicep curl 37.5kg 3x8, Incline dumbbell curl 16kg 3x10, Hammer curl 18kg 3x10."
              }
            }
            item {
              PresetRantChip("Back & Core") {
                rantInput = "Back and core: Barbell deadlift 145kg 3x4, Lat pulldown 70kg 3x8, Seated cable row 65kg 3x10, Hanging leg raise 3x15, Plank 3 sets."
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Parse & Log Action Button
          Button(
            onClick = { viewModel.parseGymRant(rantInput) },
            enabled = rantInput.isNotBlank() && !isParsingRant,
            colors = ButtonDefaults.buttonColors(
              containerColor = VoltLime,
              contentColor = Color(0xFF0B0C10),
              disabledContainerColor = Color(0xFF1E212D),
              disabledContentColor = Color(0xFF5A6076)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("parse_rant_button")
          ) {
            if (isParsingRant) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Color.Black,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Analyzing with JIIM AI...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            } else {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "LOG SESSION",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
              )
            }
          }
        }
      }
    }

    // Recommended Routine Card
    item {
      val splitName = aiAnalysis?.detectedSplitName ?: "4-Day Antagonist Split"
      val splitDays = aiAnalysis?.detectedSplitBreakdown ?: listOf(
        "Day 1: Chest & Triceps (Compounds + Isolations)",
        "Day 2: Biceps & Shoulders (Overhead Press & Arm Density)",
        "Day 3: Legs & Quad Overload (Squats & RDLs)",
        "Day 4: Abs, Cardio & Back Depth (Deadlifts & Core)"
      )

      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF262B3B)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("detected_split_card")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "ROUTINE STRUCTURE",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
              )
              Text(
                text = splitName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }

            Surface(
              onClick = {
                viewModel.saveCustomSplit(
                  title = splitName,
                  description = "Custom split based on tracked frequency",
                  exercisesCsv = "Barbell Bench Press,Overhead Barbell Press,Barbell Back Squat,Barbell Deadlift"
                )
              },
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF1A1D29),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2D3246)),
              modifier = Modifier.testTag("save_split_button")
            ) {
              Text(
                text = "Save as Split",
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          splitDays.forEach { dayLine ->
            Text(
              text = dayLine,
              color = Color(0xFFCBD5E1),
              fontSize = 12.sp,
              modifier = Modifier.padding(vertical = 2.dp)
            )
          }
        }
      }
    }

    // Workout History Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "RECENT SESSIONS",
          fontWeight = FontWeight.Bold,
          color = Color.White,
          fontSize = 12.sp,
          letterSpacing = 0.5.sp
        )
        Text(
          text = "History",
          color = Color(0xFF64748B),
          fontSize = 11.sp
        )
      }
    }

    // List of Workout Sessions
    items(workouts) { sessionWithExercises ->
      WorkoutSessionCard(
        workout = sessionWithExercises,
        useLbs = useLbs,
        onDelete = { viewModel.deleteWorkout(sessionWithExercises.session.id) }
      )
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun PresetRantChip(
  label: String,
  onClick: () -> Unit
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(8.dp),
    color = Color(0xFF161822),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF262B3B))
  ) {
    Text(
      text = label,
      color = Color(0xFFE2E8F0),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    )
  }
}

@Composable
private fun WorkoutSessionCard(
  workout: WorkoutWithExercises,
  useLbs: Boolean,
  onDelete: () -> Unit
) {
  var isExpanded by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
  val formattedDate = dateFormat.format(Date(workout.session.startTimeMillis))

  val volumeStr = if (useLbs) {
    "${(workout.session.totalVolumeKg * 2.20462).toInt()} lbs"
  } else {
    "${workout.session.totalVolumeKg.toInt()} kg"
  }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("workout_session_card_${workout.session.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = workout.session.name,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp
          )
          Text(
            text = formattedDate,
            color = Color(0xFF7B829A),
            fontSize = 11.sp
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (workout.session.prCount > 0) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFF2B2313),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldPr.copy(alpha = 0.5f)),
              modifier = Modifier.padding(end = 6.dp)
            ) {
              Text(
                text = "${workout.session.prCount} PR",
                color = GoldPr,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(26.dp)
          ) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Delete workout",
              tint = Color(0xFF71778E),
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Volume & Sets Summary Pill Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFF181B26),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF262B3B)),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.FitnessCenter,
              contentDescription = null,
              tint = VoltLime,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = volumeStr,
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFF181B26),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF262B3B)),
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Check,
              contentDescription = null,
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${workout.session.totalSets} Sets",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Surface(
          onClick = { isExpanded = !isExpanded },
          shape = RoundedCornerShape(6.dp),
          color = Color(0xFF181B26),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF262B3B))
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isExpanded) "Hide" else "Details",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
            Icon(
              imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      // Notes snippet
      if (workout.session.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = workout.session.notes,
          color = Color(0xFF94A3B8),
          fontSize = 11.sp
        )
      }

      // Expanded exercises & sets breakdown
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          workout.exercises.forEach { ex ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF161924),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF252A3B)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = ex.exercise.exerciseName,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                  fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val setsStr = ex.sets.joinToString("  •  ") { s ->
                  val w = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
                  "Set ${s.setNumber}: ${w}${if (useLbs) "lb" else "kg"} × ${s.reps}"
                }
                Text(
                  text = setsStr,
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ParsedRantReviewDialog(
  parsed: ParsedWorkoutRant,
  useLbs: Boolean,
  onConfirm: (ParsedWorkoutRant) -> Unit,
  onDismiss: () -> Unit
) {
  var workoutTitle by remember { mutableStateOf(parsed.workoutTitle) }
  var exercisesList by remember { mutableStateOf(parsed.exercises) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C3246)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("parsed_rant_review_dialog")
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
            text = "REVIEW SESSION LOG",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
          )

          IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title edit
        OutlinedTextField(
          value = workoutTitle,
          onValueChange = { workoutTitle = it },
          label = { Text("Session Name", color = Color(0xFF94A3B8), fontSize = 11.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0E1017),
            unfocusedContainerColor = Color(0xFF0E1017),
            focusedBorderColor = VoltLime,
            unfocusedBorderColor = Color(0xFF242838),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Detected Exercises (${exercisesList.size}):",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(exercisesList) { ex ->
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF161924),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = ex.exerciseName,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                  fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val setsSummary = ex.sets.mapIndexed { idx, s ->
                  val wt = if (useLbs) (s.weightKg * 2.20462).toInt() else s.weightKg.toInt()
                  "S${idx + 1}: ${wt}${if (useLbs) "lb" else "kg"} × ${s.reps}"
                }.joinToString(", ")
                Text(
                  text = setsSummary,
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF1A1D28),
              contentColor = Color(0xFF94A3B8)
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
              containerColor = VoltLime,
              contentColor = Color(0xFF0B0C10)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1.3f)
              .testTag("confirm_save_workout_button")
          ) {
            Text("CONFIRM", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  }
}
