package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LifeTrackerTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.BatteryOptimizationDialog
import com.example.util.BatteryOptimizationHelper

class MainActivity : ComponentActivity() {
  private val viewModel: LifeTrackerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    if (intent?.getBooleanExtra("INTERCEPTED_TAMPER_ATTEMPT", false) == true) {
      android.widget.Toast.makeText(this, "Peewee Block: Protected setting access blocked.", android.widget.Toast.LENGTH_SHORT).show()
    } else if (intent?.getBooleanExtra("OPEN_PROTECTION_HEALTH", false) == true) {
      viewModel.setTab(MainTab.PROTECTION_HEALTH)
    } else if (intent?.getBooleanExtra("NAVIGATE_TO_SETTINGS", false) == true || intent?.getBooleanExtra("SHOW_RESTORE_PROTECTION", false) == true) {
      viewModel.setTab(MainTab.SETTINGS)
    }

    // Ensure all notification channels are registered with exact silent/alert policies
    com.example.focus.AppNotificationChannels.registerAllChannels(this)
    com.example.focus.FocusModeManager.onBootOrScheduleChange(this)
    com.example.focus.StrictLockWatchdog.checkAndEnforce(this)

    // Start Protection Health dual-watchdog & heartbeat
    com.example.focus.ProtectionHealthManager.scheduleWorkManagerHeartbeat(this)
    com.example.focus.ProtectionHealthManager.scheduleNextWatchdogExactAlarm(this)
    com.example.focus.ProtectionWatchdogService.start(this)

    setContent {
      LifeTrackerTheme {
        LifeTrackerApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra("INTERCEPTED_TAMPER_ATTEMPT", false)) {
      android.widget.Toast.makeText(this, "Peewee Block: Protected setting access blocked.", android.widget.Toast.LENGTH_SHORT).show()
    } else if (intent.getBooleanExtra("OPEN_PROTECTION_HEALTH", false)) {
      viewModel.setTab(MainTab.PROTECTION_HEALTH)
    } else if (intent.getBooleanExtra("NAVIGATE_TO_SETTINGS", false) || intent.getBooleanExtra("SHOW_RESTORE_PROTECTION", false)) {
      viewModel.setTab(MainTab.SETTINGS)
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.onAppResume()
    com.example.focus.StrictLockWatchdog.checkAndEnforce(this)
  }

  override fun onPause() {
    super.onPause()
    viewModel.onAppPause()
  }
}

@Composable
fun LifeTrackerApp(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val currentTab by viewModel.currentTab.collectAsState()
  val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { _ -> }
  )

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  if (isFocusModeActive) {
    com.example.ui.screens.FocusRestrictionScreen(viewModel = viewModel)
  } else {
    BackHandler(enabled = currentTab != MainTab.TODAY) {
      viewModel.setTab(MainTab.TODAY)
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF090D16)),
      contentAlignment = Alignment.TopCenter
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
      ) {
        Crossfade(
          targetState = currentTab,
          animationSpec = tween(durationMillis = 200),
          label = "dashboard_crossfade"
        ) { tab ->
          when (tab) {
            MainTab.PROTECTION_HEALTH -> com.example.ui.screens.ProtectionHealthScreen(
              onNavigateBack = { viewModel.setTab(MainTab.TODAY) }
            )
            MainTab.SETTINGS -> SettingsScreen(
              viewModel = viewModel
            )
            else -> com.example.ui.screens.PeeweeBlockDashboardScreen(
              onOpenSettings = { viewModel.setTab(MainTab.SETTINGS) },
              onOpenHealthLog = { viewModel.setTab(MainTab.PROTECTION_HEALTH) }
            )
          }
        }
      }
    }
  }
}
