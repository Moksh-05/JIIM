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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyWorkoutSummary
import com.example.model.WorkoutWithExercises
import com.example.ui.theme.VoltLime
import com.example.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val workouts by viewModel.allWorkouts.collectAsState()
  val monthOffset by viewModel.monthOffset.collectAsState()
  val selectedDateMillis by viewModel.selectedCalendarDateMillis.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()

  val (monthCal, dailySummaries) = remember(monthOffset, workouts) {
    viewModel.getMonthCalendarInfo(monthOffset)
  }

  val streaks = remember(workouts) {
    viewModel.computeStreaks(workouts)
  }

  val monthTitleFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
  val currentMonthTitle = monthTitleFormat.format(monthCal.time)

  // Determine selected day's workouts
  val selectedDayWorkouts = remember(selectedDateMillis, workouts) {
    if (selectedDateMillis == null) emptyList()
    else {
      val dayStart = selectedDateMillis!!
      val dayEnd = dayStart + 86400000L - 1
      workouts.filter { it.session.startTimeMillis in dayStart..dayEnd }
    }
  }

  val totalTrainedDaysThisMonth = dailySummaries.count { it.workoutCount > 0 }
  val totalDaysInMonth = dailySummaries.size

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(12.dp))
      // Screen Title
      Column {
        Text(
          text = "CALENDAR",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = 2.sp
        )
        Text(
          text = "Session history & streak tracking",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF94A3B8)
        )
      }
    }

    // Streak & Consistency Summary Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("streak_summary_card")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Current streak
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF181B26), CircleShape)
                .border(0.5.dp, Color(0xFF282D3E), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = VoltLime,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "${streaks.first} DAYS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Text(
                text = "Current streak",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }

          // Best streak & monthly consistency
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Best: ${streaks.second} days",
              fontWeight = FontWeight.SemiBold,
              color = VoltLime,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "$totalTrainedDaysThisMonth / $totalDaysInMonth days active",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
        }
      }
    }

    // Month Navigation Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.prevMonth() },
          modifier = Modifier.testTag("prev_month_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Previous Month",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }

        Text(
          text = currentMonthTitle.uppercase(Locale.getDefault()),
          fontWeight = FontWeight.Bold,
          color = Color.White,
          fontSize = 14.sp,
          letterSpacing = 1.sp
        )

        IconButton(
          onClick = { viewModel.nextMonth() },
          modifier = Modifier.testTag("next_month_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Next Month",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // Weekday names
    item {
      val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        dayNames.forEach { day ->
          Text(
            text = day,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    // Calendar Grid
    item {
      CalendarMonthGrid(
        monthCal = monthCal,
        dailySummaries = dailySummaries,
        selectedDateMillis = selectedDateMillis,
        onSelectDay = { summary ->
          viewModel.selectCalendarDate(summary?.dateMillis)
        }
      )
    }

    // Selected Day Inspection Card
    item {
      SelectedDayInspectionCard(
        selectedDateMillis = selectedDateMillis,
        workouts = selectedDayWorkouts,
        useLbs = useLbs
      )
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun CalendarMonthGrid(
  monthCal: Calendar,
  dailySummaries: List<DailyWorkoutSummary>,
  selectedDateMillis: Long?,
  onSelectDay: (DailyWorkoutSummary?) -> Unit
) {
  val firstDayCal = Calendar.getInstance().apply {
    timeInMillis = monthCal.timeInMillis
    set(Calendar.DAY_OF_MONTH, 1)
  }
  val javaDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
  val startOffset = when (javaDayOfWeek) {
    Calendar.MONDAY -> 0
    Calendar.TUESDAY -> 1
    Calendar.WEDNESDAY -> 2
    Calendar.THURSDAY -> 3
    Calendar.FRIDAY -> 4
    Calendar.SATURDAY -> 5
    Calendar.SUNDAY -> 6
    else -> 0
  }

  val totalSlots = startOffset + dailySummaries.size
  val rows = (totalSlots + 6) / 7

  val todayCal = Calendar.getInstance()
  val todayYear = todayCal.get(Calendar.YEAR)
  val todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR)

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    for (r in 0 until rows) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        for (c in 0 until 7) {
          val slotIndex = r * 7 + c
          val dayIndex = slotIndex - startOffset

          if (dayIndex in dailySummaries.indices) {
            val summary = dailySummaries[dayIndex]
            val isSelected = selectedDateMillis == summary.dateMillis

            val cellCal = Calendar.getInstance().apply { timeInMillis = summary.dateMillis }
            val isToday = cellCal.get(Calendar.YEAR) == todayYear &&
                cellCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear

            val hasWorkout = summary.workoutCount > 0

            Box(
              modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .background(
                  color = when {
                    isSelected -> VoltLime
                    hasWorkout -> Color(0xFF141A14)
                    else -> Color(0xFF12141C)
                  },
                  shape = RoundedCornerShape(8.dp)
                )
                .border(
                  width = when {
                    isSelected -> 1.dp
                    isToday -> 1.dp
                    hasWorkout -> 0.5.dp
                    else -> 0.5.dp
                  },
                  color = when {
                    isSelected -> VoltLime
                    isToday -> Color(0xFF38BDF8)
                    hasWorkout -> VoltLime.copy(alpha = 0.4f)
                    else -> Color(0xFF222636)
                  },
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { onSelectDay(summary) }
                .testTag("calendar_day_${dayIndex + 1}"),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Text(
                  text = "${dayIndex + 1}",
                  fontWeight = if (hasWorkout || isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 12.sp,
                  color = when {
                    isSelected -> Color(0xFF0B0C10)
                    hasWorkout -> Color.White
                    isToday -> Color(0xFF38BDF8)
                    else -> Color(0xFF7A8196)
                  }
                )

                if (hasWorkout) {
                  Box(
                    modifier = Modifier
                      .padding(top = 2.dp)
                      .size(4.dp)
                      .background(if (isSelected) Color(0xFF0B0C10) else VoltLime, CircleShape)
                  )
                }
              }
            }
          } else {
            Box(
              modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SelectedDayInspectionCard(
  selectedDateMillis: Long?,
  workouts: List<WorkoutWithExercises>,
  useLbs: Boolean
) {
  val dateTitle = if (selectedDateMillis != null) {
    val fmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    fmt.format(Date(selectedDateMillis))
  } else {
    "Select a date on the calendar"
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("selected_day_inspection_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "DATE DETAILS",
            fontWeight = FontWeight.SemiBold,
            color = VoltLime,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
          )
          Text(
            text = dateTitle,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp
          )
        }
        Icon(
          imageVector = Icons.Default.CalendarToday,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(16.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (selectedDateMillis == null) {
        Text(
          text = "Tap any date on the calendar above to view workouts and performance details.",
          color = Color(0xFF94A3B8),
          fontSize = 11.sp
        )
      } else if (workouts.isEmpty()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .background(Color(0xFF181B26), CircleShape)
              .border(0.5.dp, Color(0xFF282D3E), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Nightlight,
              contentDescription = null,
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Rest Day",
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 13.sp
            )
            Text(
              text = "No workout recorded. Time for recovery and hypertrophy synthesis.",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
        }
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          workouts.forEach { w ->
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFF161924),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.FitnessCenter,
                      contentDescription = null,
                      tint = VoltLime,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = w.session.name,
                      fontWeight = FontWeight.Bold,
                      color = Color.White,
                      fontSize = 13.sp
                    )
                  }

                  val volStr = if (useLbs) "${(w.session.totalVolumeKg * 2.20462).toInt()} lbs"
                  else "${w.session.totalVolumeKg.toInt()} kg"
                  Text(
                    text = volStr,
                    fontWeight = FontWeight.Bold,
                    color = VoltLime,
                    fontSize = 12.sp
                  )
                }

                if (w.session.notes.isNotBlank()) {
                  Spacer(modifier = Modifier.height(3.dp))
                  Text(
                    text = "\"${w.session.notes}\"",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                w.exercises.forEach { ex ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(
                      text = ex.exercise.exerciseName,
                      color = Color(0xFFCBD5E1),
                      fontSize = 11.sp
                    )
                    val setsSummary = ex.sets.joinToString(", ") {
                      val wt = if (useLbs) (it.weightKg * 2.20462).toInt() else it.weightKg.toInt()
                      "${wt}x${it.reps}"
                    }
                    Text(
                      text = "$setsSummary (${if (useLbs) "lbs" else "kg"})",
                      color = Color(0xFF7B829A),
                      fontSize = 10.sp
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
