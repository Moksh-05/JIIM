package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MinimalDumbbellIcon
import com.example.ui.screens.AndroidGuideScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.TrainerScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardDark
import com.example.ui.theme.CardElevated
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlatinumSteel
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.TitaniumSilver
import com.example.ui.theme.TitaniumWhite
import com.example.viewmodel.GymViewModel
import com.example.viewmodel.GymViewModelFactory

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
  WORKOUTS("Workouts", Icons.Default.FitnessCenter, "nav_workouts_tab"),
  TRAINER("Jim", Icons.Default.Psychology, "nav_trainer_tab"),
  PROGRESS("Progress", Icons.Default.TrendingUp, "nav_progress_tab"),
  PROFILE("Profile", Icons.Default.Person, "nav_profile_tab")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        GymTrackerApp()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymTrackerApp() {
  val context = LocalContext.current
  val application = context.applicationContext as android.app.Application
  val viewModel: GymViewModel = viewModel(factory = GymViewModelFactory(application))

  var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
  var showGuideDialog by remember { mutableStateOf(false) }

  val tabs = AppTab.values()
  val isOnline by viewModel.isOnline.collectAsState()
  val useLbs by viewModel.useLbs.collectAsState()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MatteBlack,
          titleContentColor = TitaniumWhite
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            MinimalDumbbellIcon(
              size = 22.dp,
              tint = TitaniumWhite,
              accentTint = PlatinumSteel
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "JIIM",
              fontWeight = FontWeight.Black,
              fontSize = 20.sp,
              letterSpacing = 2.5.sp,
              color = TitaniumWhite
            )
          }
        },
        actions = {
          // Subtle connectivity indicator
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = CardElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(
                    if (isOnline) Color(0xFF86EFAC) else Color(0xFFD48B54),
                    CircleShape
                  )
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = if (isOnline) "Connected" else "Local",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TitaniumSilver
              )
            }
          }

          // KG / LBS unit toggle
          Surface(
            onClick = { viewModel.toggleUnits() },
            shape = RoundedCornerShape(8.dp),
            color = CardElevated,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier
              .padding(end = 6.dp)
              .testTag("unit_toggle_button")
          ) {
            Text(
              text = if (useLbs) "LBS" else "KG",
              color = TitaniumWhite,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }

          // Help / Guide button
          IconButton(
            onClick = { showGuideDialog = true },
            modifier = Modifier.testTag("open_guide_button")
          ) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = "Install & Upload Guide",
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 2.dp,
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("main_bottom_navigation")
      ) {
        tabs.forEachIndexed { index, tab ->
          val isSelected = selectedTabIndex == index
          NavigationBarItem(
            selected = isSelected,
            onClick = { selectedTabIndex = index },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) TitaniumWhite else TextTertiary,
                modifier = Modifier.size(20.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                color = if (isSelected) TitaniumWhite else TextTertiary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
              )
            },
            colors = NavigationBarItemDefaults.colors(
              indicatorColor = CardElevated
            ),
            modifier = Modifier.testTag(tab.tag)
          )
        }
      }
    }
  ) { innerPadding ->
    val contentModifier = Modifier.padding(innerPadding)
    when (tabs[selectedTabIndex]) {
      AppTab.WORKOUTS -> WorkoutScreen(viewModel = viewModel, modifier = contentModifier)
      AppTab.TRAINER -> TrainerScreen(viewModel = viewModel, modifier = contentModifier)
      AppTab.PROGRESS -> ProgressScreen(viewModel = viewModel, modifier = contentModifier)
      AppTab.PROFILE -> ProfileScreen(viewModel = viewModel, modifier = contentModifier)
    }
  }

  // Guide Dialog
  if (showGuideDialog) {
    Dialog(onDismissRequest = { showGuideDialog = false }) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 24.dp)
      ) {
        AndroidGuideScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
