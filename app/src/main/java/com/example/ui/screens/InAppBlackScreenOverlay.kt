package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackscreen.BlackScreenGestureDetector
import com.example.blackscreen.BlackScreenGestureType
import com.example.blackscreen.BlackScreenManager
import com.example.ui.viewmodel.LifeTrackerViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure OLED Black screen composable.
 * Displays pure #000000 with optional subtle dim clock, and requires deliberate
 * 3-finger gesture to exit. Audio/music plays uninterrupted in background.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InAppBlackScreenOverlay(
  viewModel: LifeTrackerViewModel,
  onExit: () -> Unit
) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()

  val gestureType = remember(userSettings?.blackScreenExitGesture) {
    BlackScreenGestureType.fromId(userSettings?.blackScreenExitGesture ?: "THREE_FINGER_TAP")
  }
  val sensitivity = userSettings?.blackScreenGestureSensitivity ?: 1.0f

  val showClock = userSettings?.blackScreenShowClock ?: true
  val is24Hour = userSettings?.blackScreenClockFormat24 ?: false

  var currentTimeText by remember { mutableStateOf("") }
  var burnInShiftX by remember { mutableFloatStateOf(0f) }

  // Clock ticker with burn-in shift
  LaunchedEffect(showClock, is24Hour) {
    while (showClock) {
      val now = Calendar.getInstance()
      val pattern = if (is24Hour) "HH:mm" else "h:mm a"
      val sdf = SimpleDateFormat(pattern, Locale.getDefault())
      currentTimeText = sdf.format(Date())

      val minute = now.get(Calendar.MINUTE)
      burnInShiftX = (minute % 10 - 5) * 4f
      delay(15000L)
    }
  }

  val detector = remember(gestureType, sensitivity) {
    BlackScreenGestureDetector(
      targetGesture = gestureType,
      sensitivity = sensitivity,
      onGestureRecognized = {
        try {
          val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
          } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(45L)
          }
        } catch (_: Exception) {}

        viewModel.deactivateBlackScreenOverlay()
        onExit()
      }
    )
  }

  // Prevent accidental hardware back button exit
  BackHandler(enabled = true) {
    // Deliberately consumed: user must use the 3-finger gesture to exit Black Screen Mode
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .pointerInteropFilter { event ->
        detector.onTouchEvent(event)
        true
      }
      .testTag("in_app_black_screen_overlay"),
    contentAlignment = Alignment.BottomCenter
  ) {
    if (showClock) {
      Text(
        text = currentTimeText,
        color = Color(0xFF2C2C2C), // Very dim subtle gray
        fontSize = 13.sp,
        modifier = Modifier
          .padding(bottom = 54.dp)
          .padding(start = burnInShiftX.dp)
      )
    }
  }
}
