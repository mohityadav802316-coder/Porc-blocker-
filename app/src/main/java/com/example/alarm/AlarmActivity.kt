package com.example.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AlarmActivity : ComponentActivity() {

  private var configuredSnoozeMinutes by mutableStateOf(10)

  private val dismissReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      finishAndRemoveTask()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    turnScreenOnAndKeyguard()

    // Ensure audio & vibration are active when alarm activity launches
    if (!AlarmAudioPlayer.isPlaying()) {
      AlarmAudioPlayer.startWithSavedSettings(this)
    }

    // Register broadcast listener for external dismissal (e.g. from notification)
    ContextCompat.registerReceiver(
      this,
      dismissReceiver,
      IntentFilter(AlarmScheduler.ACTION_STOP_ALARM),
      ContextCompat.RECEIVER_NOT_EXPORTED
    )

    // Hardware/gesture back button handling: immediately dismiss the alarm safely
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        stopAlarm()
      }
    })

    // Load configured snooze duration from settings
    lifecycleScope.launch(Dispatchers.IO) {
      try {
        val db = com.example.data.db.LifeTrackerDatabase.getDatabase(this@AlarmActivity)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          withContext(Dispatchers.Main) {
            configuredSnoozeMinutes = settings.snoozeMinutes
          }
        }
      } catch (_: Exception) {}
    }

    setContent {
      AlarmScreen(
        snoozeMinutes = configuredSnoozeMinutes,
        onStop = {
          stopAlarm()
        },
        onSnooze = {
          snoozeAlarm()
        }
      )
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      unregisterReceiver(dismissReceiver)
    } catch (_: Exception) {}
    AlarmAudioPlayer.stop()
  }

  private fun turnScreenOnAndKeyguard() {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
      val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
      keyguardManager?.requestDismissKeyguard(this, null)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
      )
    }
  }

  private fun stopAlarm() {
    AlarmScheduler.dismissRingingAlarm(this)
    finishAndRemoveTask()
  }

  private fun snoozeAlarm() {
    AlarmAudioPlayer.stop()
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.cancel(AlarmScheduler.ALARM_NOTIFICATION_ID)

    lifecycleScope.launch(Dispatchers.IO) {
      val db = com.example.data.db.LifeTrackerDatabase.getDatabase(this@AlarmActivity)
      val settings = db.userSettingsDao().getSettingsSync()
      val snoozeMins = settings?.snoozeMinutes ?: configuredSnoozeMinutes
      AlarmScheduler.scheduleSnooze(this@AlarmActivity, snoozeMins)
      withContext(Dispatchers.Main) {
        finishAndRemoveTask()
      }
    }
  }
}

@Composable
fun AlarmScreen(
  snoozeMinutes: Int = 10,
  onStop: () -> Unit,
  onSnooze: () -> Unit
) {
  val cal = Calendar.getInstance()
  val currentTimeMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  val formatted12Hour = TimeUtils.minutesTo12Hour(currentTimeMinutes)
  val hindiTime = TimeUtils.minutesToHindiTime(currentTimeMinutes)

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            Color(0xFF030712),
            Color(0xFF0A1026),
            Color(0xFF02040A)
          )
        )
      )
      .padding(28.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth()
    ) {
      // Glowing Animated Alarm Orb
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
      ) {
        Box(
          modifier = Modifier
            .size(130.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(CyanNeon.copy(alpha = 0.15f))
            .border(2.dp, CyanNeon.copy(alpha = 0.4f), CircleShape)
        )
        Box(
          modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(CyanNeon.copy(alpha = 0.4f), Color.Transparent)
              )
            )
        )
        Icon(
          imageVector = Icons.Default.Alarm,
          contentDescription = "Alarm Active",
          tint = CyanNeon,
          modifier = Modifier.size(46.dp)
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      Text(
        text = "LIFE TRACKER • WAKE-UP ALARM",
        color = CyanNeon,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Current 12-Hour Time Display
      Text(
        text = formatted12Hour,
        color = TextPrimary,
        fontSize = 54.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = hindiTime,
        color = CyanNeon.copy(alpha = 0.9f),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Rise and execute your daily routine. Today is yours to conquer.",
        color = TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(44.dp))

      // Large Prominent DISMISS / STOP ALARM Button
      Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFDC2626), // Vibrant Alert Red
          contentColor = Color.White
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .testTag("alarm_stop_button"),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
      ) {
        Icon(Icons.Default.Stop, contentDescription = "Dismiss or Stop Alarm", modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "DISMISS / STOP ALARM",
          fontSize = 18.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Snooze Action Button
      OutlinedButton(
        onClick = onSnooze,
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = VioletNeon
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
          brush = Brush.linearGradient(listOf(VioletNeon.copy(alpha = 0.6f), CyanNeon.copy(alpha = 0.4f)))
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("alarm_snooze_button"),
        shape = RoundedCornerShape(18.dp)
      ) {
        Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "SNOOZE (+$snoozeMinutes MIN)",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )
      }
    }
  }
}
