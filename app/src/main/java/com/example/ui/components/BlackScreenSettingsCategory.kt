package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blackscreen.BlackScreenGestureType
import com.example.blackscreen.BlackScreenManager
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.BatteryOptimizationHelper

@Composable
fun BlackScreenSettingsCategory(
  viewModel: LifeTrackerViewModel
) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()

  val isEnabled = userSettings?.isBlackScreenEnabled ?: false
  val isDotEnabled = userSettings?.isFloatingDotEnabled ?: true
  val dotX = userSettings?.blackScreenDotX ?: 80
  val dotY = userSettings?.blackScreenDotY ?: 260
  val dotSize = userSettings?.blackScreenDotSize ?: 36
  val dotOpacity = userSettings?.blackScreenDotOpacity ?: 0.45f
  val activationMethod = userSettings?.blackScreenActivationMethod ?: "DOUBLE_TAP"
  val exitGestureId = userSettings?.blackScreenExitGesture ?: "THREE_FINGER_TAP"
  val currentGesture = remember(exitGestureId) { BlackScreenGestureType.fromId(exitGestureId) }
  val sensitivity = userSettings?.blackScreenGestureSensitivity ?: 1.0f
  val showClock = userSettings?.blackScreenShowClock ?: true
  val clockFormat24 = userSettings?.blackScreenClockFormat24 ?: false
  val restoreAfterUnlock = userSettings?.blackScreenRestoreAfterUnlock ?: true
  val restoreAfterReboot = userSettings?.blackScreenRestoreAfterReboot ?: true

  val hasOverlayPermission = BlackScreenManager.canDrawOverlays(context)
  var showTestGestureDialog by remember { mutableStateOf(false) }

  if (showTestGestureDialog) {
    TestGestureDialog(
      initialGesture = currentGesture,
      initialSensitivity = sensitivity,
      onDismiss = { showTestGestureDialog = false },
      onSaveSensitivity = { newSens ->
        viewModel.setBlackScreenGestureSensitivity(newSens)
      }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {

    // 1. MASTER SWITCH & HERO CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, if (isEnabled) CyanNeon.copy(alpha = 0.5f) else DarkSurfaceBorder, RoundedCornerShape(20.dp))
        .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isEnabled) CyanNeon.copy(alpha = 0.18f) else DarkSurface),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = null,
                tint = if (isEnabled) CyanNeon else TextMuted,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "Black Screen Mode",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (isEnabled) "Active & Ready" else "Disabled",
                color = if (isEnabled) StatusComplete else TextMuted,
                fontSize = 12.sp
              )
            }
          }

          Switch(
            checked = isEnabled,
            onCheckedChange = { checked ->
              if (checked && !hasOverlayPermission) {
                Toast.makeText(context, "कृपया पहले 'Display over other apps' अनुमति प्रदान करें", Toast.LENGTH_LONG).show()
                context.startActivity(BlackScreenManager.getManageOverlayPermissionIntent(context))
              }
              viewModel.setBlackScreenEnabled(checked)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = CyanNeon,
              checkedTrackColor = CyanNeon.copy(alpha = 0.35f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurface
            ),
            modifier = Modifier.testTag("black_screen_master_switch")
          )
        }

        Text(
          text = "Keep display pure black (#000000) for OLED energy savings while music/audio plays in background. Exit requires deliberate 3-finger gesture.",
          color = TextSecondary,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )

        // Instant Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              if (!hasOverlayPermission) {
                Toast.makeText(context, "Overlay permission needed for system-wide black screen", Toast.LENGTH_SHORT).show()
                context.startActivity(BlackScreenManager.getManageOverlayPermissionIntent(context))
              }
              viewModel.activateBlackScreenOverlay()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("activate_black_screen_now_btn")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Activate Now", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }

          OutlinedButton(
            onClick = { showTestGestureDialog = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).testTag("test_gesture_dialog_btn")
          ) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Test Gesture", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }

    // 2. SYSTEM-WIDE FLOATING CONTROL DOT
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("System-Wide Floating Dot", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = if (isDotEnabled) "Visible over home screen & other apps" else "Hidden",
              color = TextMuted,
              fontSize = 12.sp
            )
          }
          Switch(
            checked = isDotEnabled,
            onCheckedChange = { viewModel.setBlackScreenFloatingDotEnabled(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.35f)),
            modifier = Modifier.testTag("floating_dot_switch")
          )
        }

        AnimatedVisibility(visible = isDotEnabled) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Activation Method
            Text("Activation Gesture on Dot:", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val methods = listOf("TAP" to "Single Tap", "DOUBLE_TAP" to "Double Tap", "LONG_PRESS" to "Long Press")
              methods.forEach { (key, label) ->
                val isSelected = activationMethod == key
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
                    .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { viewModel.setBlackScreenActivationMethod(key) }
                    .padding(vertical = 10.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = if (isSelected) CyanNeon else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }

            // Dot Size Selector
            Text("Dot Size: ${dotSize}dp", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val sizes = listOf(28 to "Small (28)", 36 to "Medium (36)", 48 to "Large (48)")
              sizes.forEach { (sz, label) ->
                val isSelected = dotSize == sz
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
                    .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable { viewModel.setBlackScreenDotSize(sz) }
                    .padding(vertical = 8.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = if (isSelected) CyanNeon else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }

            // Dot Opacity Slider
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("Dot Opacity", color = TextSecondary, fontSize = 12.sp)
                Text("${(dotOpacity * 100).toInt()}%", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
              Slider(
                value = dotOpacity,
                onValueChange = { viewModel.setBlackScreenDotOpacity(it) },
                valueRange = 0.15f..1.0f,
                colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
              )
            }

            // Dot Position Reset
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Position: X=$dotX, Y=$dotY (drag to move)", color = TextMuted, fontSize = 11.sp)
              TextButton(
                onClick = { viewModel.setBlackScreenDotPosition(80, 260) }
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyanNeon)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset Position", color = CyanNeon, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }

    // 3. DIFFICULT CUSTOM EXIT GESTURE SELECTION
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Custom 3-Finger Exit Gesture", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Requires 3 simultaneous fingers to prevent accidental touches", color = TextMuted, fontSize = 12.sp)
          }
        }

        // List of all 6 gestures
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          BlackScreenGestureType.entries.forEach { gesture ->
            val isSelected = currentGesture == gesture
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .clickable { viewModel.setBlackScreenExitGesture(gesture.id) }
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = gesture.title,
                    color = if (isSelected) CyanNeon else TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = gesture.description,
                    color = TextMuted,
                    fontSize = 11.sp
                  )
                }
                if (isSelected) {
                  Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = CyanNeon, modifier = Modifier.size(20.dp))
                }
              }
            }
          }
        }

        // Sensitivity slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Gesture Sensitivity", color = TextSecondary, fontSize = 12.sp)
            Text(
              text = when {
                sensitivity < 0.8f -> "Strict / High Precision"
                sensitivity > 1.2f -> "Easy / Responsive"
                else -> "Balanced (Default)"
              },
              color = CyanNeon,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Slider(
            value = sensitivity,
            onValueChange = { viewModel.setBlackScreenGestureSensitivity(it) },
            valueRange = 0.5f..1.5f,
            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
          )
        }

        // Test Gesture Button
        OutlinedButton(
          onClick = { showTestGestureDialog = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("open_test_gesture_dialog_btn")
        ) {
          Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Open Practice Test Bed", color = CyanNeon, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 4. OLED DISPLAY & CLOCK
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Display & Minimal Clock", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Show Subtle Clock", color = TextPrimary, fontSize = 14.sp)
            Text("Dim gray time display with burn-in pixel shift", color = TextMuted, fontSize = 11.sp)
          }
          Switch(
            checked = showClock,
            onCheckedChange = { viewModel.setBlackScreenShowClock(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.35f))
          )
        }

        AnimatedVisibility(visible = showClock) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("24-Hour Format", color = TextPrimary, fontSize = 14.sp)
              Text("12-hour (10:30 PM) vs 24-hour (22:30)", color = TextMuted, fontSize = 11.sp)
            }
            Switch(
              checked = clockFormat24,
              onCheckedChange = { viewModel.setBlackScreenClockFormat24(it) },
              colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.35f))
            )
          }
        }
      }
    }

    // 5. LOCK SCREEN & REBOOT RESTORATION
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Lock & Reboot Persistence", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Restore Black Mode After Unlock", color = TextPrimary, fontSize = 14.sp)
            Text("Maintains black screen when phone is unlocked after power-button sleep", color = TextMuted, fontSize = 11.sp)
          }
          Switch(
            checked = restoreAfterUnlock,
            onCheckedChange = { viewModel.setBlackScreenRestoreAfterUnlock(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.35f))
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Restore After Device Reboot", color = TextPrimary, fontSize = 14.sp)
            Text("Automatically restarts floating control dot after device restart", color = TextMuted, fontSize = 11.sp)
          }
          Switch(
            checked = restoreAfterReboot,
            onCheckedChange = { viewModel.setBlackScreenRestoreAfterReboot(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = CyanNeon.copy(alpha = 0.35f))
          )
        }
      }
    }

    // 6. PERMISSIONS & SYSTEM INTEGRATION
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("System Permissions & Status", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        // Overlay Permission
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, if (hasOverlayPermission) StatusComplete.copy(alpha = 0.4f) else StatusMissed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Display over other apps", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(
                text = if (hasOverlayPermission) "Granted • Floating dot and overlay enabled" else "Required for system-wide floating control",
                color = if (hasOverlayPermission) StatusComplete else StatusMissed,
                fontSize = 11.sp
              )
            }
            if (!hasOverlayPermission) {
              Button(
                onClick = { context.startActivity(BlackScreenManager.getManageOverlayPermissionIntent(context)) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Grant", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            } else {
              Icon(Icons.Default.CheckCircle, contentDescription = "Granted", tint = StatusComplete, modifier = Modifier.size(20.dp))
            }
          }
        }

        // Battery Optimization
        val isIgnoringBattery = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, if (isIgnoringBattery) StatusComplete.copy(alpha = 0.4f) else DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Background Battery Optimization", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(
                text = if (isIgnoringBattery) "Unrestricted • Stable background audio playback" else "May be paused by Android power management",
                color = if (isIgnoringBattery) StatusComplete else TextMuted,
                fontSize = 11.sp
              )
            }
            if (!isIgnoringBattery) {
              OutlinedButton(
                onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context) },
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Configure", color = CyanNeon, fontSize = 11.sp)
              }
            } else {
              Icon(Icons.Default.CheckCircle, contentDescription = "Optimized", tint = StatusComplete, modifier = Modifier.size(20.dp))
            }
          }
        }
      }
    }

    // 7. PRIVACY & SECURITY STATEMENT
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GoldBrass.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Security, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Privacy & Security Guarantee", color = GoldBrass, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(
          text = "• Pure display cover: Does not capture, read, or record your screen.\n" +
            "• Zero network transmission: Operates 100% offline.\n" +
            "• No camera or microphone access required for this feature.\n" +
            "• Transparent safety: An exit notification is always available in your system status bar.",
          color = TextSecondary,
          fontSize = 11.sp,
          lineHeight = 16.sp
        )
      }
    }
  }
}
