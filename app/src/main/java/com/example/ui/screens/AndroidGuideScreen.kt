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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.VoltLime

@Composable
fun AndroidGuideScreen(
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0C10))
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Spacer(modifier = Modifier.height(12.dp))
      // Header
      Column {
        Text(
          text = "SETUP GUIDE",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Black,
          color = Color.White,
          letterSpacing = 2.sp
        )
        Text(
          text = "Installation & Android deployment roadmap",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF94A3B8)
        )
      }
    }

    // Step 1: Running in Browser / Streaming Preview
    item {
      ExpandableStepCard(
        stepNumber = "1",
        title = "Test & Use in AI Studio",
        subtitle = "Zero setup needed to test tracking and AI features",
        accentColor = VoltLime,
        initialExpanded = true,
        content = {
          Text(
            text = "• The app is active in the interactive Android emulator on your screen.\n" +
              "• Log workouts by quick rant, track custom splits, check off sets, and inspect hypertrophy progress.\n" +
              "• Local persistence runs on Room Database so sessions remain intact across sessions.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      )
    }

    // Step 2: Export & Build APK
    item {
      ExpandableStepCard(
        stepNumber = "2",
        title = "Download APK or Export Code",
        subtitle = "Generate an installer package for Android devices",
        accentColor = VoltLime,
        initialExpanded = true,
        content = {
          Text(
            text = "• In the top-right menu of Google AI Studio, select 'Download Project as ZIP' or push to GitHub.\n" +
              "• To build an installable APK on your computer, run in the root directory:\n" +
              "   ./gradlew assembleDebug\n" +
              "• The output installer file is generated at:\n" +
              "   app/build/outputs/apk/debug/app-debug.apk",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      )
    }

    // Step 3: Install directly on personal Android phone
    item {
      ExpandableStepCard(
        stepNumber = "3",
        title = "Install On Your Personal Phone",
        subtitle = "Transfer the APK to your mobile device for gym sessions",
        accentColor = VoltLime,
        initialExpanded = true,
        content = {
          Text(
            text = "Method 1 (Direct APK install):\n" +
              "1. Send 'app-debug.apk' to your phone via Google Drive, Telegram, or email.\n" +
              "2. Tap the APK file on your phone.\n" +
              "3. Enable 'Allow from this source' in Android Settings if prompted.\n" +
              "4. Tap 'Install' and launch JIIM.\n\n" +
              "Method 2 (Android Studio USB Debugging):\n" +
              "1. Enable Developer Options & USB Debugging on your phone.\n" +
              "2. Connect your phone via USB and press 'Run' in Android Studio.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      )
    }

    // Step 4: Publishing to Google Play Console
    item {
      ExpandableStepCard(
        stepNumber = "4",
        title = "Upload to Google Play Store",
        subtitle = "Publish JIIM to the Play Store for wider release",
        accentColor = VoltLime,
        initialExpanded = false,
        content = {
          Text(
            text = "1. Register a Google Play Developer Account at play.google.com/console.\n" +
              "2. Build a release App Bundle:\n" +
              "   ./gradlew bundleRelease\n" +
              "   Output: app/build/outputs/bundle/release/app-release.aab\n" +
              "3. Create a new app named 'JIIM' under 'Health & Fitness'.\n" +
              "4. Upload the .aab bundle to Closed Testing or Production.\n" +
              "5. Complete the Store Listing details and privacy declaration.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      )
    }

    // FAQ & Custom Split Tip Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("lifter_questions_card")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF181B26), CircleShape)
                .border(0.5.dp, Color(0xFF282D3E), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.HelpOutline, contentDescription = null, tint = VoltLime, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "CUSTOM SPLIT & AI ANALYSIS",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              letterSpacing = 0.5.sp
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "• JIIM runs offline by default for seamless logging anywhere.\n" +
              "• Over 1-2 weeks, JIIM AI identifies workout trends (e.g. chest/triceps, biceps/shoulders, legs/abs) and codifies them into your personalized split.\n" +
              "• In the Progress tab, your weekly hypertrophy volumes and progressive overload trajectories are calculated automatically.",
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
fun ExpandableStepCard(
  stepNumber: String,
  title: String,
  subtitle: String,
  accentColor: Color,
  initialExpanded: Boolean = false,
  content: @Composable () -> Unit
) {
  var expanded by remember { mutableStateOf(initialExpanded) }

  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF12141C)),
    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { expanded = !expanded }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
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
            Text(
              text = stepNumber,
              fontWeight = FontWeight.Bold,
              color = accentColor,
              fontSize = 13.sp
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 13.sp
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
        }

        Icon(
          imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
          contentDescription = if (expanded) "Collapse" else "Expand",
          tint = Color(0xFF94A3B8),
          modifier = Modifier.size(20.dp)
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF161924),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242838)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Box(modifier = Modifier.padding(12.dp)) {
              content()
            }
          }
        }
      }
    }
  }
}
