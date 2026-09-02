package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.WorkoutSession
import com.example.ui.theme.GoldPr
import com.example.ui.theme.VoltLime

@Composable
fun PrBannerAlert(
  message: String?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = message != null,
    enter = slideInVertically() + fadeIn(),
    exit = slideOutVertically() + fadeOut(),
    modifier = modifier
  ) {
    if (message != null) {
      Surface(
        color = Color(0xFF1C2210),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, VoltLime),
        tonalElevation = 10.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .background(VoltLime, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = "PR Trophy",
              tint = Color.Black,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "PERSONAL RECORD!",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Black,
              color = VoltLime,
              letterSpacing = 1.sp
            )
            Text(
              text = message,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White,
              fontWeight = FontWeight.SemiBold
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Dismiss PR alert",
              tint = Color.LightGray,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun WorkoutCompletedSummaryDialog(
  session: WorkoutSession,
  isLbs: Boolean,
  onDismiss: () -> Unit
) {
  val durationMinutes = ((session.endTimeMillis - session.startTimeMillis) / 60000L).coerceAtLeast(1L)
  val volumeDisplay = if (isLbs) {
    "${(session.totalVolumeKg * 2.20462).toInt()} lbs"
  } else {
    "${session.totalVolumeKg.toInt()} kg"
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26364E)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("workout_summary_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Trophy Icon
        Box(
          modifier = Modifier
            .size(72.dp)
            .background(VoltLime.copy(alpha = 0.15f), CircleShape)
            .border(2.dp, VoltLime, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Workout Complete",
            tint = VoltLime,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "WORKOUT CRUSHED!",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = 1.sp
        )

        Text(
          text = session.name,
          style = MaterialTheme.typography.bodyMedium,
          color = VoltLime,
          fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats Grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            label = "Total Volume",
            value = volumeDisplay,
            icon = Icons.Default.FitnessCenter,
            tint = VoltLime,
            modifier = Modifier.weight(1f)
          )
          StatCard(
            label = "Completed Sets",
            value = "${session.totalSets}",
            icon = Icons.Default.Check,
            tint = Color(0xFF00E5FF),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            label = "Duration",
            value = "$durationMinutes min",
            icon = Icons.Default.Timer,
            tint = Color(0xFFFFA726),
            modifier = Modifier.weight(1f)
          )
          StatCard(
            label = "PRs Smashed",
            value = "${session.prCount}",
            icon = Icons.Default.LocalFireDepartment,
            tint = if (session.prCount > 0) GoldPr else Color.Gray,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(
            containerColor = VoltLime,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("dismiss_summary_button")
        ) {
          Text(
            text = "LOG TO HISTORY",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            letterSpacing = 1.sp
          )
        }
      }
    }
  }
}

@Composable
private fun StatCard(
  label: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = Color(0xFF192333),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26354D)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF94A3B8),
          fontSize = 11.sp
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }
  }
}
