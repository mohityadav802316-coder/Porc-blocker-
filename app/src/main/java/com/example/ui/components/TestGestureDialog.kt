package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.blackscreen.BlackScreenGestureDetector
import com.example.blackscreen.BlackScreenGestureType
import com.example.blackscreen.GestureRecognitionFeedback
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TestGestureDialog(
  initialGesture: BlackScreenGestureType,
  initialSensitivity: Float,
  onDismiss: () -> Unit,
  onSaveSensitivity: (Float) -> Unit
) {
  var sensitivity by remember { mutableFloatStateOf(initialSensitivity) }
  var feedback by remember { mutableStateOf(GestureRecognitionFeedback()) }
  var successCount by remember { mutableIntStateOf(0) }

  val detector = remember(initialGesture, sensitivity) {
    BlackScreenGestureDetector(
      targetGesture = initialGesture,
      sensitivity = sensitivity,
      onGestureRecognized = {
        successCount++
      }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      shape = RoundedCornerShape(24.dp),
      color = DarkBackground,
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(20.dp)
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "GESTURE PRACTICE LAB",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "Test: ${initialGesture.title}",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(
            onClick = {
              onSaveSensitivity(sensitivity)
              onDismiss()
            },
            modifier = Modifier.testTag("close_gesture_test")
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = initialGesture.description,
          color = TextSecondary,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Sensitivity slider
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Sensitivity: ${(sensitivity * 100).toInt()}%",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp)
          )
          Slider(
            value = sensitivity,
            onValueChange = { sensitivity = it },
            valueRange = 0.5f..1.5f,
            steps = 9,
            colors = SliderDefaults.colors(
              thumbColor = CyanNeon,
              activeTrackColor = CyanNeon,
              inactiveTrackColor = DarkSurfaceBorder
            ),
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live status pill
        val statusBg = when {
          feedback.isSuccess -> StatusComplete.copy(alpha = 0.18f)
          feedback.pointerCount in 1..2 -> StatusMissed.copy(alpha = 0.18f)
          feedback.isThreeFingerActive -> CyanNeon.copy(alpha = 0.18f)
          else -> DarkSurfaceElevated
        }
        val statusBorder = when {
          feedback.isSuccess -> StatusComplete
          feedback.pointerCount in 1..2 -> StatusMissed.copy(alpha = 0.6f)
          feedback.isThreeFingerActive -> CyanNeon
          else -> DarkSurfaceBorder
        }
        val statusTextColor = when {
          feedback.isSuccess -> StatusComplete
          feedback.pointerCount in 1..2 -> StatusMissed
          feedback.isThreeFingerActive -> CyanNeon
          else -> TextSecondary
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(statusBg)
            .border(1.dp, statusBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            if (feedback.isSuccess) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusComplete, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
            } else {
              Icon(Icons.Default.TouchApp, contentDescription = null, tint = statusTextColor, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = feedback.message,
              color = statusTextColor,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive touch canvas
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .border(1.5.dp, if (feedback.isThreeFingerActive) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .pointerInteropFilter { motionEvent ->
              detector.onTouchEvent(motionEvent) { updatedFeedback ->
                feedback = updatedFeedback
              }
              true
            }
            .testTag("gesture_test_canvas"),
          contentAlignment = Alignment.Center
        ) {
          // Draw touch circles
          Canvas(modifier = Modifier.fillMaxSize()) {
            feedback.touchPoints.forEachIndexed { index, point ->
              val offset = Offset(point.first, point.second)
              // Outer ripple
              drawCircle(
                color = CyanNeon.copy(alpha = 0.25f),
                radius = 70f,
                center = offset
              )
              // Inner border
              drawCircle(
                color = CyanNeon,
                radius = 45f,
                center = offset,
                style = Stroke(width = 4f)
              )
              // Center dot
              drawCircle(
                color = Color.White,
                radius = 12f,
                center = offset
              )
            }
          }

          if (feedback.touchPoints.isEmpty()) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.padding(24.dp)
            ) {
              Text(
                text = "TOUCH ZONE",
                color = Color(0xFF333333),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Place 3 fingers here and perform gesture",
                color = Color(0xFF555555),
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "(1 or 2 finger touches will be deliberately rejected)",
                color = Color(0xFF444444),
                fontSize = 11.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom stats & action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Successful Exits: ",
              color = TextMuted,
              fontSize = 12.sp
            )
            Text(
              text = "$successCount",
              color = if (successCount > 0) StatusComplete else TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                feedback = GestureRecognitionFeedback()
                successCount = 0
              },
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear")
            }

            Button(
              onClick = {
                onSaveSensitivity(sensitivity)
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Save & Close", color = DarkBackground, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
