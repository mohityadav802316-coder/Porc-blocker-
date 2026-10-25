package com.example.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.shortcontent.AccessibilityHelper
import com.example.util.BatteryOptimizationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Honest 5-Tier Protection States according to strict user specification:
 * - PROTECTION_ACTIVE: All required engines (VPN & Accessibility) are actively running in memory.
 * - PROTECTION_LIMITED: Master protection is enabled, but coverage is reduced (e.g. VPN active but Accessibility killed/disabled).
 * - PERMISSION_REQUIRED: Master protection is enabled, but required Android permissions are missing or revoked.
 * - PROTECTION_PAUSED: Protection is temporarily paused for banking or authorized use.
 * - PROTECTION_DISABLED: Master protection is intentionally turned off by user.
 */
enum class ProtectionStatus(
  val label: String,
  val badgeColorHex: Long,
  val description: String
) {
  PROTECTION_ACTIVE(
    label = "Protection Active",
    badgeColorHex = 0xFF10B981, // Emerald Green
    description = "All blocking engines are running and guarding in real-time."
  ),
  PROTECTION_LIMITED(
    label = "Protection Limited",
    badgeColorHex = 0xFFF59E0B, // Amber
    description = "Protection is running with reduced coverage. Action required to restore full security."
  ),
  PERMISSION_REQUIRED(
    label = "Permission Required",
    badgeColorHex = 0xFFFB923C, // Orange
    description = "Required Android permissions need your authorization in system settings."
  ),
  PROTECTION_PAUSED(
    label = "Protection Paused",
    badgeColorHex = 0xFF60A5FA, // Blue / Amber
    description = "Protection is temporarily suspended. Will automatically re-arm."
  ),
  PROTECTION_DISABLED(
    label = "Protection Disabled",
    badgeColorHex = 0xFF9CA3AF, // Gray / Muted
    description = "Protection is currently turned off."
  )
}

/**
 * Immutable status report summarizing exact system health.
 */
data class ProtectionStateReport(
  val status: ProtectionStatus,
  val isMasterEnabled: Boolean,
  val isVpnRunning: Boolean,
  val isAccessibilityRunning: Boolean,
  val isVpnPermissionGranted: Boolean,
  val isAccessibilityPermissionGranted: Boolean,
  val isNotificationPermissionGranted: Boolean,
  val isBatteryOptimizationIgnored: Boolean,
  val isDeviceAdminActive: Boolean,
  val isStrictLockActive: Boolean,
  val strictLockRemainingMinutes: Int,
  val isPaused: Boolean,
  val pauseRemainingMinutes: Int,
  val lastCheckTimestamp: Long,
  val formattedLastCheck: String,
  val actionRequired: String?
)

/**
 * PeeweeProtectionManager
 *
 * Core coordinator for persistent, resilient, and honest background protection.
 *
 * Principles:
 * 1. Persistent User Preference: Once enabled, protection stays enabled across reboots and app kills.
 * 2. Honest Status: Never claims "Protected" unless verified live in memory.
 * 3. Graceful Recovery: Coordinates boot restoration, process recovery, and watchdog self-healing.
 */
object PeeweeProtectionManager {
  private const val TAG = "PeeweeProtectionMgr"
  private const val PREFS_NAME = "peewee_protection_prefs"
  private const val KEY_MASTER_PROTECTION_ENABLED = "key_master_protection_enabled"
  private const val KEY_LAST_STATUS_CHECK_MS = "key_last_status_check_ms"

  private const val ALERT_CHANNEL_ID = "peewee_block_alerts"
  private const val ALERT_NOTIF_ID = 9021

  private val _protectionStateFlow = MutableStateFlow<ProtectionStateReport?>(null)
  val protectionStateFlow: StateFlow<ProtectionStateReport?> = _protectionStateFlow.asStateFlow()

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isMasterProtectionEnabled(context: Context): Boolean {
    // Default to true on fresh install so protection is ready, but respects explicit user preference
    return getPrefs(context).getBoolean(KEY_MASTER_PROTECTION_ENABLED, true)
  }

  fun setMasterProtectionEnabled(context: Context, enabled: Boolean) {
    getPrefs(context).edit().putBoolean(KEY_MASTER_PROTECTION_ENABLED, enabled).apply()
    evaluateProtectionState(context)
  }

  /**
   * Performs an honest, comprehensive evaluation of all protection layers.
   */
  fun evaluateProtectionState(context: Context): ProtectionStateReport {
    val isMaster = isMasterProtectionEnabled(context)
    val now = System.currentTimeMillis()

    // 1. Evaluate individual engines
    val isVpnRunning = LocalDnsVpnService.isVpnRunning.value
    val isVpnPermGranted = LocalDnsVpnService.isPrepared(context)
    val isAccessibilityRunning = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val isAccessibilityPermGranted = AccessibilityHelper.isAccessibilityPermissionGrantedInSettings(context)
    val isNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true
    val isBatteryIgnored = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    val isDeviceAdmin = LifeTrackerAdminReceiver.isDeviceAdminActive(context)

    // 2. Strict Lock and Pause states
    val isStrictLock = FocusModeManager.isStrictLockActive(context)
    val strictLockMins = ((FocusModeManager.getStrictLockRemainingMillis(context) / 60000L).coerceAtLeast(1)).toInt()
    val isPaused = ProtectionPauseManager.isPaused(context)
    val pauseMins = ((ProtectionPauseManager.getPauseRemainingMillis(context) / 60000L).coerceAtLeast(1)).toInt()

    // 3. Determine honest status
    val status: ProtectionStatus
    val actionRequired: String?

    when {
      isPaused -> {
        status = ProtectionStatus.PROTECTION_PAUSED
        actionRequired = "Protection paused ($pauseMins mins remaining). Tap to resume immediately."
      }
      !isMaster -> {
        status = ProtectionStatus.PROTECTION_DISABLED
        actionRequired = "Protection is currently turned off. Tap 'Turn Protection ON' to protect this device."
      }
      !isAccessibilityPermGranted || !isVpnPermGranted -> {
        status = ProtectionStatus.PERMISSION_REQUIRED
        actionRequired = when {
          !isAccessibilityPermGranted && !isVpnPermGranted -> "Grant Accessibility and VPN permissions in Android Settings."
          !isAccessibilityPermGranted -> "Grant Accessibility Service permission in Android Settings."
          else -> "Start and allow Local DNS VPN connection."
        }
      }
      isVpnRunning && isAccessibilityRunning -> {
        status = ProtectionStatus.PROTECTION_ACTIVE
        actionRequired = null
      }
      isVpnRunning || isAccessibilityRunning -> {
        status = ProtectionStatus.PROTECTION_LIMITED
        actionRequired = when {
          !isAccessibilityRunning -> "Accessibility Service stopped by Android. Tap to restart in Settings."
          else -> "DNS Filter VPN is not connected. Tap to reconnect."
        }
      }
      else -> {
        // Permissions granted but neither service is running in memory
        status = ProtectionStatus.PROTECTION_LIMITED
        actionRequired = "Protection services stopped in background. Tap to start."
      }
    }

    // Save timestamp
    getPrefs(context).edit().putLong(KEY_LAST_STATUS_CHECK_MS, now).apply()

    val report = ProtectionStateReport(
      status = status,
      isMasterEnabled = isMaster,
      isVpnRunning = isVpnRunning,
      isAccessibilityRunning = isAccessibilityRunning,
      isVpnPermissionGranted = isVpnPermGranted,
      isAccessibilityPermissionGranted = isAccessibilityPermGranted,
      isNotificationPermissionGranted = isNotificationGranted,
      isBatteryOptimizationIgnored = isBatteryIgnored,
      isDeviceAdminActive = isDeviceAdmin,
      isStrictLockActive = isStrictLock,
      strictLockRemainingMinutes = strictLockMins,
      isPaused = isPaused,
      pauseRemainingMinutes = pauseMins,
      lastCheckTimestamp = now,
      formattedLastCheck = formatTimestamp(now),
      actionRequired = actionRequired
    )

    _protectionStateFlow.value = report
    return report
  }

  /**
   * Boot and App Update Handler: Restores protection permitted by Android.
   */
  fun onBootOrAppUpdated(context: Context) {
    Log.i(TAG, "Device rebooted or package replaced. Restoring Peewee Block protection...")
    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "BOOT_OR_UPDATE_RECOVERY",
      message = "Reboot/Update recovery executed. Checking master protection preferences...",
      severity = "INFO"
    )

    val isMaster = isMasterProtectionEnabled(context)
    if (!isMaster) {
      Log.i(TAG, "Master protection is disabled by user. Skipping auto-start.")
      return
    }

    // 1. Resume Strict Lock if still active
    FocusModeManager.onBootOrScheduleChange(context)
    StrictLockWatchdog.checkAndEnforce(context)

    // 2. Restart DNS VPN if prepared
    if (VpnService.prepare(context) == null) {
      try {
        LocalDnsVpnService.start(context)
        Log.i(TAG, "LocalDnsVpnService restarted on boot.")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to restart LocalDnsVpnService on boot", e)
      }
    } else {
      Log.w(TAG, "VPN permission not prepared on boot. User prompt required.")
    }

    // 3. Start background health watchdog
    try {
      ProtectionWatchdogService.start(context)
      ProtectionHealthManager.scheduleWorkManagerHeartbeat(context)
      ProtectionHealthManager.scheduleNextWatchdogExactAlarm(context)
    } catch (e: Exception) {
      Log.e(TAG, "Error starting watchdog on boot", e)
    }

    // 4. Evaluate state and notify if degraded
    val report = evaluateProtectionState(context)
    if (report.status == ProtectionStatus.PROTECTION_LIMITED || report.status == ProtectionStatus.PERMISSION_REQUIRED) {
      notifyProtectionDegraded(
        context = context,
        title = "Peewee Block: Attention Required",
        message = report.actionRequired ?: "Protection is limited after device reboot."
      )
    }
  }

  /**
   * Posts an explicit, honest system notification when protection is degraded or permissions are lost.
   */
  fun notifyProtectionDegraded(context: Context, title: String, message: String) {
    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
          ALERT_CHANNEL_ID,
          "Peewee Block Protection Alerts",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Alerts when website blocking protection is disabled or degraded"
        }
        notificationManager.createNotificationChannel(channel)
      }

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
      val pendingIntent = PendingIntent.getActivity(
        context,
        9021,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      notificationManager.notify(ALERT_NOTIF_ID, notification)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to post degraded notification", e)
    }
  }

  private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffSec = (now - timestamp) / 1000L
    return when {
      diffSec < 10 -> "Just now"
      diffSec < 60 -> "$diffSec sec ago"
      diffSec < 3600 -> "${diffSec / 60} mins ago"
      else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    }
  }
}
