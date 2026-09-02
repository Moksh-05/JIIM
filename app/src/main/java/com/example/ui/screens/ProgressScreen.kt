package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiProgressAnalysis
import com.example.model.ExercisePr
import com.example.model.WorkoutWithExercises
import com.example.ui.theme.VoltLime
import com.example.viewmodel.GymViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val workouts by viewModel.allWorkouts.collectAsState()
  val prs by viewModel.allPrs.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val isAnalyzing by viewModel.isAnalyzing.collectAsState()
  val aiAnalysis by viewModel.aiAnalysis.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()

  var timeRangeFilter by remember { mutableStateOf("14D") } // "14D", "30D", "ALL"
  var showFullAiBreakdown by remember { mutableStateOf(true) }

  val filteredWorkouts = remember(workouts, timeRangeFilter) {
    val now = System.currentTimeMillis()
    when (timeRangeFilter) {
      "14D" -> workouts.filter { it.session.startTimeMillis >= now - 14 * 86400000L }
      "30D" -> workouts.filter { it.session.startTimeMillis >= now - 30 * 86400000L }
      else -> workouts
    }.sortedBy { it.session.startTimeMillis }
  }

  val totalVolume = remember(filteredWorkouts) {
    filteredWorkouts.sumOf { it.session.totalVolumeKg }
  }
  val totalSets = remember(filteredWorkouts) {
    filteredWorkouts.sumOf { it.session.totalSets }
  }
  val totalWorkoutsCount = filteredWorkouts.size

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(12.dp))
      // Top Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "PROGRESS",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
          )
          Text(
            text = "Volume load & progressive overload trends",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
          )
        }

        // Status Badge
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isOnline) Color(0xFF131F17) else Color(0xFF221E14),
          border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isOnline) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(
                  if (isOnline) Color(0xFF22C55E) else Color(0xFFF59E0B),
                  CircleShape
                )
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = if (isOnline) "JIIM AI" else "Offline",
              color = if (isOnline) Color(0xFF86EFAC) else Color(0xFFFDE68A),
              fontWeight = FontWeight.SemiBold,
              fontSize = 10.sp
            )
          }
        }
      }
    }

    // Time filter pills
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf("14D" to "14 Days", "30D" to "30 Days", "ALL" to "All Time").forEach { (key, label) ->
          val isSelected = timeRangeFilter == key
          Surface(
            onClick = { timeRangeFilter = key },
            shape = RoundedCornerShape(8.dp),
            color = if (isSelected) VoltLime else Color(0xFF141620),
            border = androidx.compose.foundation.BorderStroke(
              0.5.dp,
              if (isSelected) VoltLime else Color(0xFF262B3B)
            ),
            modifier = Modifier.weight(1f)
          ) {
            Text(
              text = label,
              color = if (isSelected) Color(0xFF0B0C10) else Color(0xFF94A3B8),
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 11.sp,
              modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    }

    // Summary Metric Cards
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val volumeDisplay = if (useLbs) {
          "${(totalVolume * 2.20462).toInt()} lbs"
        } else {
          "${totalVolume.toInt()} kg"
        }

        MetricStatBox(
          label = "Total Volume",
          value = volumeDisplay,
          subtitle = "Tonnage moved",
          accentColor = VoltLime,
          modifier = Modifier.weight(1f)
        )
        MetricStatBox(
          label = "Working Sets",
          value = "$totalSets",
          subtitle = "$totalWorkoutsCount sessions",
          accentColor = Color(0xFF38BDF8),
          modifier = Modifier.weight(1f)
        )
      }
    }

    // JIIM AI Progressive Overload & Hypertrophy Coach Card
    item {
      AiCoachCard(
        analysis = aiAnalysis,
        isAnalyzing = isAnalyzing,
        isOnline = isOnline,
        isExpanded = showFullAiBreakdown,
        onToggleExpand = { showFullAiBreakdown = !showFullAiBreakdown },
        onRunAnalysis = { viewModel.runProgressAnalysis() }
      )
    }

    // Chart 1: Volume Progression Canvas Curve
    item {
      VolumeProgressionChart(
        workouts = filteredWorkouts,
        useLbs = useLbs
      )
    }

    // Chart 2: Hypertrophy Muscle Group Distribution
    item {
      HypertrophyMuscleDistributionCard(workouts = filteredWorkouts)
    }

    // Chart 3: Estimated 1RM Progression
    item {
      PrProgressionCard(prs = prs, useLbs = useLbs)
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun MetricStatBox(
  label: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFF12141C),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = label,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        color = Color(0xFF64748B),
        fontSize = 10.sp
      )
    }
  }
}

@Composable
private fun AiCoachCard(
  analysis: AiProgressAnalysis?,
  isAnalyzing: Boolean,
  isOnline: Boolean,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onRunAnalysis: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF272B3C)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("ai_coach_card")
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
              .background(Color(0xFF181B26), CircleShape)
              .border(0.5.dp, Color(0xFF282D3E), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.TrendingUp,
              contentDescription = "JIIM AI",
              tint = VoltLime,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "JIIM AI",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
            Text(
              text = if (isOnline) "Adaptive hypertrophy & overload analysis" else "Offline diagnostic engine",
              fontSize = 11.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF181B26),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF282D3E))
        ) {
          Text(
            text = analysis?.overallScore ?: "Active",
            color = VoltLime,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Progressive Overload Verdict Card
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161924),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "PROGRESSION EVALUATION",
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = analysis?.progressiveOverloadVerdict ?: "Calculating overload trajectory across sessions...",
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Toggle for full breakdown
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onToggleExpand() }
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (isExpanded) "Hide details" else "View breakdown & recommendations",
          color = Color(0xFF94A3B8),
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp
        )
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(16.dp)
        )
      }

      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier.padding(top = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Hypertrophy status
          if (analysis?.hypertrophyStatus != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF161924),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "VOLUME BREAKDOWN",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                  color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = analysis.hypertrophyStatus,
                  color = Color(0xFFCBD5E1),
                  fontSize = 12.sp,
                  lineHeight = 16.sp
                )
              }
            }
          }

          // Detected Split
          if (analysis?.detectedSplitName != null) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF161924),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "DETECTED ROUTINE: ${analysis.detectedSplitName}",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                  color = Color(0xFFCBD5E1)
                )
                Spacer(modifier = Modifier.height(4.dp))
                analysis.detectedSplitBreakdown.forEach { dayLine ->
                  Text(
                    text = dayLine,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                  )
                }
              }
            }
          }

          // Plateau / Stagnation warning
          if (!analysis?.stagnationAlerts.isNullOrEmpty()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF1F1717),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "PLATEAU ADVISORY",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                  color = Color(0xFFF87171)
                )
                Spacer(modifier = Modifier.height(3.dp))
                analysis?.stagnationAlerts?.forEach { alert ->
                  Text(
                    text = alert,
                    color = Color(0xFFFCA5A5),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                  )
                }
              }
            }
          }

          // Tactical recommendations
          if (!analysis?.recommendations.isNullOrEmpty()) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF161924),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "RECOMMENDATIONS",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 11.sp,
                  color = VoltLime
                )
                Spacer(modifier = Modifier.height(4.dp))
                analysis?.recommendations?.forEach { rec ->
                  Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Box(
                      modifier = Modifier
                        .padding(top = 5.dp)
                        .size(4.dp)
                        .background(VoltLime, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = rec,
                      color = Color(0xFFCBD5E1),
                      fontSize = 11.sp,
                      lineHeight = 16.sp
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Refresh Analysis Button
      Button(
        onClick = onRunAnalysis,
        enabled = !isAnalyzing,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF1E2232),
          contentColor = Color.White,
          disabledContainerColor = Color(0xFF161822),
          disabledContentColor = Color(0xFF5A6076)
        ),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2C3246)),
        modifier = Modifier
          .fillMaxWidth()
          .height(42.dp)
          .testTag("run_ai_analysis_button")
      ) {
        if (isAnalyzing) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color.White,
            strokeWidth = 2.dp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Analyzing...", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        } else {
          Text(
            text = "ANALYZE PROGRESSION",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
          )
        }
      }
    }
  }
}

@Composable
private fun VolumeProgressionChart(
  workouts: List<WorkoutWithExercises>,
  useLbs: Boolean
) {
  val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("volume_progression_chart")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "VOLUME OVER TIME",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
          )
          Text(
            text = if (useLbs) "Total weight moved per session (lbs)" else "Total weight moved per session (kg)",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
          )
        }
        Icon(Icons.Default.ShowChart, contentDescription = null, tint = VoltLime, modifier = Modifier.size(18.dp))
      }

      Spacer(modifier = Modifier.height(18.dp))

      if (workouts.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
          contentAlignment = Alignment.Center
        ) {
          Text("No sessions recorded in this period", color = Color(0xFF64748B), fontSize = 12.sp)
        }
      } else {
        val volumes = workouts.map {
          if (useLbs) it.session.totalVolumeKg * 2.20462 else it.session.totalVolumeKg
        }
        val dates = workouts.map { dateFormat.format(Date(it.session.startTimeMillis)) }
        val maxVol = (volumes.maxOrNull() ?: 1000.0).coerceAtLeast(500.0)

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val paddingBottom = 30f
            val chartHeight = h - paddingBottom
            val n = volumes.size

            // Guide lines
            val gridLines = 3
            for (i in 0..gridLines) {
              val y = chartHeight * (1f - (i.toFloat() / gridLines))
              drawLine(
                color = Color(0xFF242838),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
              )
            }

            if (n >= 1) {
              val points = mutableListOf<Offset>()
              val stepX = if (n > 1) w / (n - 1) else w / 2

              for (i in 0 until n) {
                val x = if (n > 1) i * stepX else w / 2
                val ratio = (volumes[i] / maxVol).toFloat().coerceIn(0.05f, 1f)
                val y = chartHeight * (1f - ratio)
                points.add(Offset(x, y))
              }

              val fillPath = Path().apply {
                moveTo(points.first().x, chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, chartHeight)
                close()
              }

              drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                  colors = listOf(VoltLime.copy(alpha = 0.2f), Color.Transparent),
                  startY = 0f,
                  endY = chartHeight
                )
              )

              val strokePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                  lineTo(points[i].x, points[i].y)
                }
              }

              drawPath(
                path = strokePath,
                color = VoltLime,
                style = Stroke(width = 3f)
              )

              points.forEach { pt ->
                drawCircle(
                  color = Color(0xFF0B0C10),
                  radius = 5f,
                  center = pt
                )
                drawCircle(
                  color = VoltLime,
                  radius = 3.5f,
                  center = pt
                )
              }
            }
          }
        }

        // X-axis labels
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val labelsToShow = if (dates.size <= 5) dates else listOf(
            dates.first(),
            dates[dates.size / 2],
            dates.last()
          )
          labelsToShow.forEach { d ->
            Text(text = d, color = Color(0xFF64748B), fontSize = 10.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun HypertrophyMuscleDistributionCard(
  workouts: List<WorkoutWithExercises>
) {
  val muscleTallies = remember(workouts) {
    val counts = mutableMapOf(
      "Chest" to 0,
      "Back" to 0,
      "Legs" to 0,
      "Shoulders" to 0,
      "Arms" to 0,
      "Core" to 0
    )
    workouts.forEach { w ->
      w.exercises.forEach { ex ->
        val cat = when (ex.exercise.category.lowercase(Locale.ROOT)) {
          "chest" -> "Chest"
          "back" -> "Back"
          "legs" -> "Legs"
          "shoulders" -> "Shoulders"
          "arms" -> "Arms"
          "core" -> "Core"
          else -> "Chest"
        }
        val sets = ex.sets.count { it.isCompleted }
        counts[cat] = (counts[cat] ?: 0) + sets
      }
    }
    counts
  }

  val maxSets = (muscleTallies.values.maxOrNull() ?: 20).coerceAtLeast(15)

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hypertrophy_distribution_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "MUSCLE GROUP VOLUME",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
          )
          Text(
            text = "Working sets completed per target area",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp
          )
        }
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
      }

      Spacer(modifier = Modifier.height(14.dp))

      muscleTallies.forEach { (muscle, setCount) ->
        val fraction = (setCount.toFloat() / maxSets).coerceIn(0f, 1f)
        val inOptimalRange = setCount in 10..22

        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = muscle,
              fontWeight = FontWeight.Medium,
              color = Color.White,
              fontSize = 12.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "$setCount sets",
                fontWeight = FontWeight.SemiBold,
                color = if (inOptimalRange) VoltLime else Color(0xFF94A3B8),
                fontSize = 12.sp
              )
              if (inOptimalRange) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF1B2313)
                ) {
                  Text(
                    text = "Optimal",
                    color = VoltLime,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .background(Color(0xFF1A1D29), RoundedCornerShape(3.dp))
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(
                  color = if (inOptimalRange) VoltLime else Color(0xFF38BDF8),
                  shape = RoundedCornerShape(3.dp)
                )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PrProgressionCard(
  prs: List<ExercisePr>,
  useLbs: Boolean
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("pr_progression_card")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = "ESTIMATED 1RM BENCHMARKS",
        fontWeight = FontWeight.Bold,
        color = Color.White,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp
      )
      Text(
        text = "Calculated strength ceiling via Brzycki formula",
        color = Color(0xFF94A3B8),
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      val keyLifts = listOf(
        "Barbell Bench Press",
        "Barbell Back Squat",
        "Barbell Deadlift",
        "Overhead Barbell Press"
      )

      keyLifts.forEach { liftName ->
        val pr = prs.find { it.exerciseName.equals(liftName, ignoreCase = true) }
        val display1Rm = if (pr != null) {
          if (useLbs) "${(pr.estimated1RmKg * 2.20462).roundToInt()} lbs"
          else "${pr.estimated1RmKg} kg"
        } else "—"

        val displayActual = if (pr != null) {
          if (useLbs) "${(pr.weightKg * 2.20462).roundToInt()} lbs × ${pr.reps}"
          else "${pr.weightKg} kg × ${pr.reps}"
        } else "No record"

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF161924),
          border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = liftName,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 12.sp
              )
              Text(
                text = "Best: $displayActual",
                color = Color(0xFF7B829A),
                fontSize = 11.sp
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = display1Rm,
                fontWeight = FontWeight.Bold,
                color = VoltLime,
                fontSize = 14.sp
              )
              Text(
                text = "Est. 1RM",
                color = Color(0xFF64748B),
                fontSize = 9.sp
              )
            }
          }
        }
      }
    }
  }
}
