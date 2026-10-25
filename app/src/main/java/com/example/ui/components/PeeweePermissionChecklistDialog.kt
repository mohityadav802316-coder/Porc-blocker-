package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.focus.LifeTrackerAdminReceiver
import com.example.focus.LocalDnsVpnService
import com.example.focus.PeeweeProtectionManager
import com.example.shortcontent.AccessibilityHelper
import com.example.util.BatteryOptimizationHelper
import kotlinx.coroutines.delay

private val CyanAccent = Color(0xFF00F0FF)
private val DarkBg = Color(0xFF0F1420)
private val CardBg = Color(0xFF171F33)
private val CardBorder = Color(0xFF263352)
private val TextWhite = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF9CA3AF)
private val GreenActive = Color(0xFF10B981)
private val AmberWarning = Color(0xFFF59E0B)

/**
 * Clear, user-friendly permission setup checklist for Peewee Block background protection.
 * Explains exactly why each permission is needed and guides the user step-by-step.
 */
@Composable
fun PeeweePermissionChecklistDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current

  // State checks
  var isVpnGranted by remember { mutableStateOf(LocalDnsVpnService.isPrepared(context)) }
  var isAccessibilityGranted by remember { mutableStateOf(AccessibilityHelper.isAccessibilityPermissionGrantedInSettings(context)) }
  var isNotificationGranted by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      } else true
    )
  }
  var isBatteryIgnored by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }
  var isDeviceAdminActive by remember { mutableStateOf(LifeTrackerAdminReceiver.isDeviceAdminActive(context)) }

  // Refresh periodically
  LaunchedEffect(Unit) {
    while (true) {
      isVpnGranted = LocalDnsVpnService.isPrepared(context)
      isAccessibilityGranted = AccessibilityHelper.isAccessibilityPermissionGrantedInSettings(context)
      isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
      } else true
      isBatteryIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
      isDeviceAdminActive = LifeTrackerAdminReceiver.isDeviceAdminActive(context)
      delay(1500)
    }
  }

  // VPN launcher
  val vpnLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      LocalDnsVpnService.start(context)
      PeeweeProtectionManager.evaluateProtectionState(context)
      Toast.makeText(context, "VPN Permission Granted", Toast.LENGTH_SHORT).show()
    }
  }

  // Notification permission launcher
  val notifLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    isNotificationGranted = granted
    PeeweeProtectionManager.evaluateProtectionState(context)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = {
          PeeweeProtectionManager.evaluateProtectionState(context)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("checklist_done_button")
      ) {
        Text("Done", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextMuted)
      }
    },
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Protection Checklist",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        item {
          Text(
            text = "Peewee Block runs completely on-device without external servers. Configure these Android permissions to ensure continuous, uninterrupted protection:",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }

        // 1. Local DNS Filter (VPN)
        item {
          ChecklistItem(
            title = "1. Local DNS Filter (VPN)",
            description = "Intercepts DNS requests locally to sinkhole adult domains (127.0.0.1). Never sends your data to any remote server.",
            isGranted = isVpnGranted,
            icon = Icons.Default.VpnKey,
            actionLabel = if (isVpnGranted) "Active" else "Grant VPN",
            onAction = {
              val intent = VpnService.prepare(context)
              if (intent != null) {
                vpnLauncher.launch(intent)
              } else {
                LocalDnsVpnService.start(context)
                PeeweeProtectionManager.evaluateProtectionState(context)
              }
            }
          )
        }

        // 2. Accessibility Service
        item {
          ChecklistItem(
            title = "2. Accessibility Service",
            description = "Monitors browser URL address bars and app titles to block incognito/private adult browsing attempts instantly.",
            isGranted = isAccessibilityGranted,
            icon = Icons.Default.Lock,
            actionLabel = if (isAccessibilityGranted) "Active" else "Enable in Settings",
            onAction = {
              AccessibilityHelper.openAccessibilitySettings(context)
            }
          )
        }

        // 3. Notifications
        item {
          ChecklistItem(
            title = "3. Status & Health Notifications",
            description = "Required to keep the background protection service alive and alert you immediately if protection is degraded or stopped.",
            isGranted = isNotificationGranted,
            icon = Icons.Default.Notifications,
            actionLabel = if (isNotificationGranted) "Allowed" else "Allow Alerts",
            onAction = {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              } else {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                  putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
              }
            }
          )
        }

        // 4. Battery Optimization
        item {
          ChecklistItem(
            title = "4. Unrestricted Battery / Background",
            description = "Prevents aggressive Android battery managers (Samsung Device Care, MIUI, ColorOS) from sleeping the filter during screen-off.",
            isGranted = isBatteryIgnored,
            icon = Icons.Default.BatteryChargingFull,
            actionLabel = if (isBatteryIgnored) "Unrestricted" else "Set Unrestricted",
            onAction = {
              try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                  val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                  }
                  context.startActivity(intent)
                }
              } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
              }
            }
          )
        }

        // 5. Anti-Tamper Device Admin (Optional)
        item {
          ChecklistItem(
            title = "5. Anti-Tamper Protection (Optional)",
            description = "Blocks hasty uninstallation during active Strict Lock. You retain full control to deactivate anytime via app settings.",
            isGranted = isDeviceAdminActive,
            icon = Icons.Default.Security,
            actionLabel = if (isDeviceAdminActive) "Active" else "Enable Admin",
            onAction = {
              if (!isDeviceAdminActive) {
                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                  putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    LifeTrackerAdminReceiver.getComponentName(context)
                  )
                  putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Protect Peewee Block from accidental removal during Strict Lock."
                  )
                }
                context.startActivity(intent)
              }
            }
          )
        }

        // OEM Guidance Box
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1E283D))
              .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null,
                  tint = CyanAccent,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Manufacturer Tips (Samsung, Xiaomi, Moto)",
                  color = TextWhite,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                text = "• Samsung: Settings → Device Care → Battery → Background usage limits → Add to 'Never sleeping apps'.\n• Xiaomi / MIUI: Enable 'Autostart' in App Info & set Battery Saver to 'No restrictions'.\n• Always-on VPN: In Android Settings → VPN → Peewee Block → Enable 'Always-on VPN' for 24/7 protection.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    },
    containerColor = DarkBg,
    shape = RoundedCornerShape(20.dp)
  )
}

@Composable
private fun ChecklistItem(
  title: String,
  description: String,
  isGranted: Boolean,
  icon: ImageVector,
  actionLabel: String,
  onAction: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(CardBg)
      .border(
        width = 1.dp,
        color = if (isGranted) GreenActive.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp)
      )
      .padding(12.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(if (isGranted) GreenActive.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isGranted) Icons.Default.Check else icon,
              contentDescription = null,
              tint = if (isGranted) GreenActive else AmberWarning,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = title,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Status Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isGranted) GreenActive.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = if (isGranted) "OK" else "NEEDED",
            color = if (isGranted) GreenActive else AmberWarning,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }

      Text(
        text = description,
        color = TextMuted,
        fontSize = 11.sp,
        lineHeight = 15.sp
      )

      if (!isGranted) {
        OutlinedButton(
          onClick = onAction,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
          border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.7f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
        ) {
          Text(text = actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
