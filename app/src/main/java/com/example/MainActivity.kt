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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HelpOutline
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
import com.example.ui.screens.AndroidGuideScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.PrVaultScreen
import com.example.ui.screens.WorkoutScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoltLime
import com.example.viewmodel.GymViewModel
import com.example.viewmodel.GymViewModelFactory

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
  WORKOUTS("Sessions", Icons.Default.FitnessCenter, "nav_workouts_tab"),
  PROGRESS("Progress", Icons.Default.TrendingUp, "nav_progress_tab"),
  CALENDAR("Calendar", Icons.Default.CalendarMonth, "nav_calendar_tab"),
  PR_VAULT("Records", Icons.Default.EmojiEvents, "nav_pr_vault_tab")
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
          containerColor = Color(0xFF0B0C10),
          titleContentColor = Color.White
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "JIIM",
              fontWeight = FontWeight.Black,
              fontSize = 20.sp,
              letterSpacing = 2.sp,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .size(6.dp)
                .background(VoltLime, CircleShape)
            )
          }
        },
        actions = {
          // Subtle connection badge
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isOnline) Color(0xFF131F17) else Color(0xFF221E14),
            border = androidx.compose.foundation.BorderStroke(
              0.5.dp,
              if (isOnline) Color(0xFF22C55E).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
            ),
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
                    if (isOnline) Color(0xFF22C55E) else Color(0xFFF59E0B),
                    CircleShape
                  )
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = if (isOnline) "JIIM AI" else "Offline",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOnline) Color(0xFF86EFAC) else Color(0xFFFDE68A)
              )
            }
          }

          // KG / LBS unit toggle
          Surface(
            onClick = { viewModel.toggleUnits() },
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF161924),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF2B3045)),
            modifier = Modifier
              .padding(end = 6.dp)
              .testTag("unit_toggle_button")
          ) {
            Text(
              text = if (useLbs) "LBS" else "KG",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }

          // Guide button
          IconButton(
            onClick = { showGuideDialog = true },
            modifier = Modifier.testTag("open_guide_button")
          ) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = "Install & Upload Guide",
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = Color(0xFF0F1118),
        tonalElevation = 4.dp,
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
                tint = if (isSelected) VoltLime else Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
              )
            },
            label = {
              Text(
                text = tab.title,
                color = if (isSelected) Color.White else Color(0xFF64748B),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 11.sp
              )
            },
            colors = NavigationBarItemDefaults.colors(
              indicatorColor = Color(0xFF1D2214)
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
      AppTab.PROGRESS -> ProgressScreen(viewModel = viewModel, modifier = contentModifier)
      AppTab.CALENDAR -> CalendarScreen(viewModel = viewModel, modifier = contentModifier)
      AppTab.PR_VAULT -> PrVaultScreen(viewModel = viewModel, modifier = contentModifier)
    }
  }

  // Guide Dialog
  if (showGuideDialog) {
    Dialog(onDismissRequest = { showGuideDialog = false }) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F1118),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262B3B)),
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 24.dp)
      ) {
        AndroidGuideScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }
}
