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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.BodyWeightLog
import com.example.model.ExerciseLibrary
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
import com.example.viewmodel.ExerciseSessionPoint
import com.example.viewmodel.GymViewModel
import com.example.viewmodel.PlateauInsight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressScreen(
  viewModel: GymViewModel,
  modifier: Modifier = Modifier
) {
  val workouts by viewModel.allWorkouts.collectAsState()
  val bodyWeights by viewModel.allBodyWeights.collectAsState()
  val aiAnalysis by viewModel.aiAnalysis.collectAsState()
  val isAnalyzing by viewModel.isAnalyzing.collectAsState()
  val isOnline by viewModel.isOnline.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()
  val customExercises by viewModel.customExercises.collectAsState()

  // Tracked Exercise for Metric 1
  var selectedExercise by remember { mutableStateOf("Barbell Bench Press") }
  val exerciseHistory = remember(workouts, selectedExercise) {
    viewModel.getExerciseHistory(selectedExercise)
  }

  // Metric 3: Plateau Insights for past 2-3 weeks
  val plateauInsights = remember(workouts) {
    viewModel.getPlateauInsights()
  }

  // Modal for logging body weight
  var showLogWeightDialog by remember { mutableStateOf(false) }

  if (showLogWeightDialog) {
    LogBodyWeightDialog(
      useLbs = useLbs,
      onSaveWeight = { wt ->
        viewModel.logBodyWeight(wt)
        showLogWeightDialog = false
      },
      onDismiss = { showLogWeightDialog = false }
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(MatteBlack)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Header
    item {
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "ANALYTICS & METRICS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = PlatinumSteel
          )
          Text(
            text = "Progress Tracking",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TitaniumWhite
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = CardElevated,
          border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
          Text(
            text = "3 Key Metrics",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TitaniumSilver,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
    }

    // =============================================================
    // METRIC 1: PROGRESSIVE OVERLOAD PER EXERCISE GRAPH
    // =============================================================
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth().testTag("metric_1_progressive_overload_card")
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
                  .size(32.dp)
                  .background(CardElevated, CircleShape)
                  .border(1.dp, BorderHighlight, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.TrendingUp,
                  contentDescription = null,
                  tint = TitaniumWhite,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "METRIC 1",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = PlatinumSteel
                )
                Text(
                  text = "Progressive Overload",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
              }
            }

            // Overload status badge
            val latestPoint = exerciseHistory.lastOrNull()
            val isOverloadNow = latestPoint?.isOverloadComparedToPrevious == true
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isOverloadNow) Color(0xFF142217) else CardElevated,
              border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (isOverloadNow) Color(0xFF5BA872) else BorderSubtle
              )
            ) {
              Text(
                text = if (isOverloadNow) "Overload: UP" else "Tracking Active",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOverloadNow) Color(0xFF86EFAC) else TitaniumSilver,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Exercise Selector Horizontal Carousel
          Text(
            text = "Select Exercise to Inspect:",
            fontSize = 11.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(6.dp))

          val commonExercises = remember(customExercises) {
            listOf(
              "Barbell Bench Press",
              "Barbell Back Squat",
              "Incline Dumbbell Press",
              "Barbell Deadlift",
              "Overhead Barbell Press",
              "Lat Pulldown",
              "Dumbbell Lateral Raise",
              "Romanian Deadlift"
            ) + customExercises.map { it.name }
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(commonExercises) { exName ->
              val isSelected = exName.equals(selectedExercise, ignoreCase = true)
              Surface(
                onClick = { selectedExercise = exName },
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) TitaniumWhite else CardDark,
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (isSelected) TitaniumWhite else BorderSubtle
                )
              ) {
                Text(
                  text = exName,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) MatteBlack else TextSecondary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Key Stats Summary for this exercise
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val maxWeightLogged = exerciseHistory.maxOfOrNull { it.topWeightKg } ?: 0.0
            val latestTop = exerciseHistory.lastOrNull()?.topWeightKg ?: 0.0
            val est1Rm = exerciseHistory.lastOrNull()?.estimated1Rm ?: 0.0

            val peakDisplay = if (useLbs) "${(maxWeightLogged * 2.20462).toInt()} lbs" else "${maxWeightLogged.toInt()} kg"
            val currentDisplay = if (useLbs) "${(latestTop * 2.20462).toInt()} lbs" else "${latestTop.toInt()} kg"
            val est1RmDisplay = if (useLbs) "${(est1Rm * 2.20462).toInt()} lbs" else "${est1Rm.toInt()} kg"

            MetricSummaryPill(label = "Current Load", value = currentDisplay, modifier = Modifier.weight(1f))
            MetricSummaryPill(label = "All-Time Best", value = peakDisplay, modifier = Modifier.weight(1f))
            MetricSummaryPill(label = "Estimated 1RM", value = est1RmDisplay, modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Progressive Overload Canvas Graph
          if (exerciseHistory.size >= 2) {
            ProgressiveOverloadCanvasChart(
              points = exerciseHistory,
              useLbs = useLbs,
              modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
            )
          } else {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CardDark,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
            ) {
              Text(
                text = "Log at least 2 sessions of $selectedExercise to view the progressive overload trajectory line.",
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(14.dp)
              )
            }
          }
        }
      }
    }

    // =============================================================
    // METRIC 2: BODY WEIGHT TRACKER
    // =============================================================
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth().testTag("metric_2_body_weight_card")
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
                  .size(32.dp)
                  .background(CardElevated, CircleShape)
                  .border(1.dp, BorderHighlight, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.MonitorWeight,
                  contentDescription = null,
                  tint = TitaniumWhite,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "METRIC 2",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = PlatinumSteel
                )
                Text(
                  text = "Body Weight Trend",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
              }
            }

            Surface(
              onClick = { showLogWeightDialog = true },
              shape = RoundedCornerShape(8.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
              modifier = Modifier.testTag("log_body_weight_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = TitaniumWhite, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Log Weight",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TitaniumWhite
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Body Weight Stats Row
          val latestBw = bodyWeights.lastOrNull()?.weightKg ?: 78.0
          val initialBw = bodyWeights.firstOrNull()?.weightKg ?: latestBw
          val delta = latestBw - initialBw

          val latestStr = if (useLbs) "${((latestBw * 2.20462) * 10).toInt() / 10.0} lbs" else "${latestBw} kg"
          val deltaStr = if (useLbs) {
            val dLbs = ((delta * 2.20462) * 10).toInt() / 10.0
            if (dLbs > 0) "+$dLbs lbs" else "$dLbs lbs"
          } else {
            val dKg = ((delta) * 10).toInt() / 10.0
            if (dKg > 0) "+$dKg kg" else "$dKg kg"
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            MetricSummaryPill(label = "Current Weight", value = latestStr, modifier = Modifier.weight(1f))
            MetricSummaryPill(label = "Net Delta", value = deltaStr, modifier = Modifier.weight(1f))
            MetricSummaryPill(label = "Entries", value = "${bodyWeights.size} logs", modifier = Modifier.weight(1f))
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Body Weight Canvas Chart
          if (bodyWeights.size >= 2) {
            BodyWeightCanvasChart(
              logs = bodyWeights,
              useLbs = useLbs,
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
            )
          } else {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CardDark,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
            ) {
              Text(
                text = "Tap 'Log Weight' to begin tracking daily body mass trends.",
                color = TextTertiary,
                fontSize = 11.sp,
                modifier = Modifier.padding(12.dp)
              )
            }
          }
        }
      }
    }

    // =============================================================
    // METRIC 3: AI & DATA ANALYSIS: PLATEAUS & FORM FIXES (PAST 2-3 WEEKS)
    // User requested: "3. The AI, or just the data, being analyzed and seeing
    // what exercises I'm plateauing on and what form I need to fix based on my
    // previous two to three weeks' suggestions."
    // =============================================================
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth().testTag("metric_3_plateau_analysis_card")
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
                  .size(32.dp)
                  .background(CardElevated, CircleShape)
                  .border(1.dp, BorderHighlight, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.AutoAwesome,
                  contentDescription = null,
                  tint = TitaniumWhite,
                  modifier = Modifier.size(17.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "METRIC 3",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = PlatinumSteel
                )
                Text(
                  text = "Plateau & Form Analysis",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TitaniumWhite
                )
              }
            }

            Surface(
              onClick = { viewModel.runProgressAnalysis() },
              shape = RoundedCornerShape(8.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.testTag("run_ai_analysis_button")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (isAnalyzing) {
                  CircularProgressIndicator(modifier = Modifier.size(12.dp), color = TitaniumWhite, strokeWidth = 1.5.dp)
                } else {
                  Icon(Icons.Default.Refresh, contentDescription = null, tint = TitaniumSilver, modifier = Modifier.size(12.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isAnalyzing) "Auditing..." else "2-3 Wk Audit",
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TitaniumSilver
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Diagnostic evaluation of stalled lifts and biomechanical form cues:",
            fontSize = 11.5.sp,
            color = TextSecondary
          )

          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            plateauInsights.forEach { insight ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = insight.exerciseName,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = TitaniumWhite
                    )

                    val stalledWtStr = if (useLbs) "${(insight.stalledWeightKg * 2.20462).toInt()} lbs" else "${insight.stalledWeightKg.toInt()} kg"
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = Color(0xFF261D15),
                      border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFD48B54).copy(alpha = 0.5f))
                    ) {
                      Text(
                        text = "Stalled at $stalledWtStr (${insight.sessionCount} sessions)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFDBA74),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Form Fix Cue
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F1117),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                      Text(
                        text = "BIOMECHANICAL FORM FIX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = PlatinumSteel
                      )
                      Spacer(modifier = Modifier.height(3.dp))
                      Text(
                        text = insight.formFixCue,
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 17.sp
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  // Recommended Accessory
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F1117),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                      Text(
                        text = "RECOMMENDED ACCESSORY TO UNSTICK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = PlatinumSteel
                      )
                      Spacer(modifier = Modifier.height(3.dp))
                      Text(
                        text = insight.recommendedAccessory,
                        fontSize = 12.sp,
                        color = TitaniumSilver,
                        lineHeight = 17.sp
                      )
                    }
                  }
                }
              }
            }
          }

          // Summary Verdict from Gemini or Offline Analyzer
          aiAnalysis?.let { analysis ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = CardElevated,
              border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "AI PROGRESSION VERDICT",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  color = PlatinumSteel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = analysis.progressiveOverloadVerdict,
                  fontSize = 12.sp,
                  color = TextPrimary,
                  lineHeight = 17.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}

// -------------------------------------------------------------
// HELPER PILL
// -------------------------------------------------------------
@Composable
private fun MetricSummaryPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = CardDark,
    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = value,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TitaniumWhite
      )
    }
  }
}

// -------------------------------------------------------------
// PROGRESSIVE OVERLOAD CANVAS GRAPH
// -------------------------------------------------------------
@Composable
private fun ProgressiveOverloadCanvasChart(
  points: List<ExerciseSessionPoint>,
  useLbs: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(Color(0xFF0C0D12), RoundedCornerShape(12.dp))
      .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val weights = points.map { if (useLbs) it.topWeightKg * 2.20462 else it.topWeightKg }
      val minVal = weights.minOrNull() ?: 0.0
      val maxVal = weights.maxOrNull() ?: 100.0
      val span = (maxVal - minVal).coerceAtLeast(5.0)

      val stepX = w / (points.size - 1).coerceAtLeast(1)

      // Draw subtle background grid lines
      for (i in 0..3) {
        val y = h * (i / 3f)
        drawLine(
          color = Color(0xFF1F222B),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1f
        )
      }

      val path = Path()
      val coords = mutableListOf<Offset>()

      points.forEachIndexed { i, p ->
        val weight = if (useLbs) p.topWeightKg * 2.20462 else p.topWeightKg
        val x = i * stepX
        val y = h - (((weight - minVal) / span) * (h * 0.8f) + (h * 0.1f)).toFloat()
        val offset = Offset(x, y)
        coords.add(offset)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }

      // Draw connection line
      drawPath(
        path = path,
        color = Color(0xFFD4D8E2),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
      )

      // Draw points
      coords.forEachIndexed { idx, pt ->
        val isOverload = points[idx].isOverloadComparedToPrevious
        drawCircle(
          color = if (isOverload) Color(0xFF86EFAC) else Color(0xFFCBD2DE),
          radius = 4.5.dp.toPx(),
          center = pt
        )
        drawCircle(
          color = Color(0xFF0C0D12),
          radius = 2.dp.toPx(),
          center = pt
        )
      }
    }
  }
}

// -------------------------------------------------------------
// BODY WEIGHT CANVAS CHART
// -------------------------------------------------------------
@Composable
private fun BodyWeightCanvasChart(
  logs: List<BodyWeightLog>,
  useLbs: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .background(Color(0xFF0C0D12), RoundedCornerShape(12.dp))
      .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
      .padding(horizontal = 14.dp, vertical = 12.dp)
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height

      val weights = logs.map { if (useLbs) it.weightKg * 2.20462 else it.weightKg }
      val minVal = weights.minOrNull() ?: 60.0
      val maxVal = weights.maxOrNull() ?: 90.0
      val span = (maxVal - minVal).coerceAtLeast(1.0)

      val stepX = w / (logs.size - 1).coerceAtLeast(1)

      for (i in 0..3) {
        val y = h * (i / 3f)
        drawLine(
          color = Color(0xFF1F222B),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1f
        )
      }

      val path = Path()
      val coords = mutableListOf<Offset>()

      logs.forEachIndexed { i, l ->
        val weight = if (useLbs) l.weightKg * 2.20462 else l.weightKg
        val x = i * stepX
        val y = h - (((weight - minVal) / span) * (h * 0.75f) + (h * 0.12f)).toFloat()
        val offset = Offset(x, y)
        coords.add(offset)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
      }

      drawPath(
        path = path,
        color = Color(0xFF9CA3AF),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
      )

      coords.forEach { pt ->
        drawCircle(
          color = Color.White,
          radius = 4.dp.toPx(),
          center = pt
        )
        drawCircle(
          color = Color(0xFF0C0D12),
          radius = 1.8.dp.toPx(),
          center = pt
        )
      }
    }
  }
}

// -------------------------------------------------------------
// LOG BODY WEIGHT DIALOG
// -------------------------------------------------------------
@Composable
private fun LogBodyWeightDialog(
  useLbs: Boolean,
  onSaveWeight: (Double) -> Unit,
  onDismiss: () -> Unit
) {
  var weightInput by remember { mutableStateOf(if (useLbs) "172.0" else "78.0") }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = SurfaceDark),
      border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
        .testTag("log_body_weight_dialog")
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "LOG BODY WEIGHT",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TitaniumWhite,
            letterSpacing = 1.sp
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = weightInput,
          onValueChange = { weightInput = it },
          label = { Text("Weight (${if (useLbs) "lbs" else "kg"})", color = TextSecondary, fontSize = 11.sp) },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardDark,
            unfocusedContainerColor = CardDark,
            focusedBorderColor = TitaniumWhite,
            unfocusedBorderColor = BorderSubtle,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
          onClick = {
            val parsed = weightInput.toDoubleOrNull() ?: 75.0
            onSaveWeight(parsed)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = TitaniumWhite,
            contentColor = MatteBlack
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
          Text("SAVE WEIGHT ENTRY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
