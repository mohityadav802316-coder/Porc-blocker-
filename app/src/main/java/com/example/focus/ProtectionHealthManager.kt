package com.example.focus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.shortcontent.AccessibilityHelper
import com.example.shortcontent.ShortContentAccessibilityService
import com.example.util.BatteryOptimizationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Health Item representing a specific protection pillar.
 */
data class HealthPillar(
  val id: String,
  val title: String,
  val subtitle: String,
  val isHealthy: Boolean,
  val actionLabel: String,
  val fixAction: (Context) -> Unit
)

/**
 * Comprehensive System Health Report.
 */
data class ProtectionHealthReport(
  val isAllHealthy: Boolean,
  val healthyCount: Int,
  val totalCount: Int,
  val healthPercentage: Int,
  val pillars: List<HealthPillar>,
  val failedSummary: String,
  val sevenDayUptimePercentage: Int,
  val lastHeartbeatSecAgo: Long
)

/**
 * Local persistent log event for service transitions and audits.
 */
data class HealthLogEntry(
  val timestamp: Long,
  val eventType: String,
  val message: String,
  val severity: String // "INFO", "WARN", "ERROR"
) {
  val formattedTime: String
    get() = SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Protection Health & Long-term Reliability Manager for Life Tracker.
 *
 * Responsibilities:
 * 1. Tracks live heartbeats from Accessibility Service every minute.
 * 2. Monitors all 10 security pillars (Accessibility, DNS VPN, Device Admin, Battery,
 *    Overlays, Notifications, Exact Alarms, Pause App Activity Exemption, FGS, AI Vision).
 * 3. Dual-watchdog verification (AlarmManager exact + WorkManager).
 * 4. High-priority alerting (re-alerts every 30 minutes if unresolved).
 * 5. Persistent Health Log and 7-Day Uptime Percentage.
 * 6. OEM / Moto phone step-by-step guidance.
 */
object ProtectionHealthManager {

  private const val TAG = "ProtectionHealthManager"
  private const val PREFS_NAME = "protection_health_prefs"

  private const val KEY_ACCESSIBILITY_HEARTBEAT = "key_accessibility_heartbeat"
  private const val KEY_LAST_ALERT_TIMESTAMP = "key_last_alert_timestamp"
  private const val KEY_HEALTH_LOGS_JSON = "key_health_logs_json"
  private const val KEY_UPTIME_CHECKS_JSON = "key_uptime_checks_json"

  private const val CHANNEL_ID = "life_tracker_protection_health"
  private const val NOTIF_ID_HEALTH_ALERT = 9120

  private const val ALERT_REMINDER_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes
  private const val MAX_LOG_ENTRIES = 120

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  /**
   * Called by ShortContentAccessibilityService every minute to record a live heartbeat.
   */
  fun recordAccessibilityHeartbeat(context: Context) {
    val now = System.currentTimeMillis()
    getPrefs(context).edit().putLong(KEY_ACCESSIBILITY_HEARTBEAT, now).apply()
  }

  fun getLastHeartbeatTimestamp(context: Context): Long {
    return getPrefs(context).getLong(KEY_ACCESSIBILITY_HEARTBEAT, 0L)
  }

  fun isAccessibilityHeartbeatAlive(context: Context): Boolean {
    val last = getLastHeartbeatTimestamp(context)
    if (last == 0L) return false
    val diff = System.currentTimeMillis() - last
    // Heartbeat is sent every minute; considered alive if updated within last 2.5 minutes
    return diff <= 150_000L
  }

  /**
   * Evaluates all security pillars and produces an actionable report.
   */
  fun getHealthReport(context: Context): ProtectionHealthReport {
    val pillars = mutableListOf<HealthPillar>()

    // 1. Accessibility Service & Live Heartbeat
    val hasA11ySetting = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val isHeartbeatAlive = isAccessibilityHeartbeatAlive(context)
    val isServiceConnected = ShortContentAccessibilityService.isServiceRunning()
    val a11yHealthy = hasA11ySetting && (isHeartbeatAlive || isServiceConnected)

    val lastHeartbeatMs = getLastHeartbeatTimestamp(context)
    val secAgo = if (lastHeartbeatMs > 0L) (System.currentTimeMillis() - lastHeartbeatMs) / 1000L else -1L

    val a11ySubtitle = when {
      !hasA11ySetting -> "सिस्टम सेटिंग्स में एक्सेसिबिलिटी बंद है"
      !a11yHealthy -> "सर्विस चालू है परंतु सिस्टम द्वारा रोकी गई है (Heartbeat $secAgo sec पहले)"
      else -> "सक्रिय और जुड़ी हुई है (Heartbeat: $secAgo sec पहले)"
    }

    pillars.add(
      HealthPillar(
        id = "accessibility",
        title = "Accessibility सर्विस",
        subtitle = a11ySubtitle,
        isHealthy = a11yHealthy,
        actionLabel = "सेटिंग्स खोलें",
        fixAction = { ctx ->
          try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
          } catch (e: Exception) {
            Log.e(TAG, "Error opening accessibility settings", e)
          }
        }
      )
    )

    // 2. Local DNS VPN
    val isVpnConfigured = LocalDnsVpnManager.isEnabled(context)
    val isVpnRunning = LocalDnsVpnService.isVpnRunning.value
    val vpnHealthy = isVpnConfigured && isVpnRunning

    pillars.add(
      HealthPillar(
        id = "dns_vpn",
        title = "Local DNS VPN फ़िल्टर",
        subtitle = if (vpnHealthy) "सक्रिय (स्थानीय डिवाइस पर सुरक्षित DNS)" else "बंद (नेटवर्क स्तर पर वयस्क डोमेन सुरक्षा निष्क्रिय)",
        isHealthy = vpnHealthy,
        actionLabel = if (!isVpnConfigured) "चालू करें" else "VPN सेटिंग्स",
        fixAction = { ctx ->
          if (!isVpnConfigured) {
            LocalDnsVpnManager.setEnabled(ctx, true)
            LocalDnsVpnManager.startVpn(ctx)
          } else {
            try {
              val intent = Intent(Settings.ACTION_VPN_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
              }
              ctx.startActivity(intent)
            } catch (_: Exception) {
              LocalDnsVpnManager.startVpn(ctx)
            }
          }
        }
      )
    )

    // 3. Device Admin (Anti-Uninstall)
    val isAdminActive = LifeTrackerAdminReceiver.isAdminActive(context)
    pillars.add(
      HealthPillar(
        id = "device_admin",
        title = "Device Admin (अनइंस्टॉल सुरक्षा)",
        subtitle = if (isAdminActive) "सक्रिय (कठोर अनुशासन के दौरान अनइंस्टॉल सुरक्षित)" else "निष्क्रिय (ऐप को सीधे हटाया जा सकता है)",
        isHealthy = isAdminActive,
        actionLabel = "Admin चालू करें",
        fixAction = { ctx ->
          LifeTrackerAdminReceiver.requestAdminPermission(ctx)
        }
      )
    )

    // 4. Battery "Unrestricted"
    val isBatteryUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    pillars.add(
      HealthPillar(
        id = "battery",
        title = "Battery: Unrestricted (अप्रतिबंधित)",
        subtitle = if (isBatteryUnrestricted) "अनुकूलित (OS बैकग्राउंड में ऐप को नहीं रोकेगा)" else "पाबंदी सक्रिय (फोन सोने पर सुरक्षा बंद हो सकती है)",
        isHealthy = isBatteryUnrestricted,
        actionLabel = "छूट दें",
        fixAction = { ctx ->
          BatteryOptimizationHelper.requestIgnoreBatteryOptimization(ctx)
        }
      )
    )

    // 5. Overlay Permission (Display over apps)
    val canDrawOverlays = Settings.canDrawOverlays(context)
    pillars.add(
      HealthPillar(
        id = "overlay",
        title = "Display Over Other Apps (Overlay)",
        subtitle = if (canDrawOverlays) "सक्रिय (ब्लॉकिंग स्क्रीन तुरंत प्रदर्शित होगी)" else "अनुमति नहीं (ब्लॉकिंग ओवरले नहीं दिखेगा)",
        isHealthy = canDrawOverlays,
        actionLabel = "अनुमति दें",
        fixAction = { ctx ->
          try {
            val intent = Intent(
              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              Uri.parse("package:${ctx.packageName}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            ctx.startActivity(intent)
          } catch (_: Exception) {}
        }
      )
    )

    // 6. Notification Permission
    val notificationsAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.areNotificationsEnabled() ?: true
    } else {
      true
    }
    pillars.add(
      HealthPillar(
        id = "notifications",
        title = "Notifications (सूचना अनुमति)",
        subtitle = if (notificationsAllowed) "सक्रिय (अलर्ट व सुरक्षा चेतावनियाँ समय पर मिलेंगी)" else "बंद (महत्वपूर्ण सुरक्षा चेतावनी नहीं दिखेगी)",
        isHealthy = notificationsAllowed,
        actionLabel = "चालू करें",
        fixAction = { ctx ->
          try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
              putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
          } catch (_: Exception) {}
        }
      )
    )

    // 7. Exact Alarm Permission
    val canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      am?.canScheduleExactAlarms() ?: true
    } else {
      true
    }
    pillars.add(
      HealthPillar(
        id = "exact_alarms",
        title = "Exact Alarm (सटीक समय अलार्म)",
        subtitle = if (canExactAlarm) "सक्रिय (15 मिनट Watchdog समय पर चलेगा)" else "अनुमति नहीं (Watchdog देरी से चल सकता है)",
        isHealthy = canExactAlarm,
        actionLabel = "अनुमति दें",
        fixAction = { ctx ->
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
              val intent = Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${ctx.packageName}")
              ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
              ctx.startActivity(intent)
            } catch (_: Exception) {}
          }
        }
      )
    )

    // 8. "Pause app activity if unused" बंद
    val isAutoRevokeWhitelisted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      try {
        context.packageManager.isAutoRevokeWhitelisted
      } catch (_: Exception) {
        true
      }
    } else {
      true
    }
    pillars.add(
      HealthPillar(
        id = "unused_app_pause",
        title = "Pause App Activity if Unused",
        subtitle = if (isAutoRevokeWhitelisted) "सुरक्षित (अस्थायी उपयोग न होने पर भी OS अनुमतियाँ नहीं छीनेगा)" else "सुझाव: App Info में 'Pause app activity' बंद करें",
        isHealthy = isAutoRevokeWhitelisted,
        actionLabel = "App Info",
        fixAction = { ctx ->
          try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.parse("package:${ctx.packageName}")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
          } catch (_: Exception) {}
        }
      )
    )

    // 9. Foreground Service
    val isFgsRunning = ProtectionWatchdogService.isServiceRunning() || isVpnRunning
    pillars.add(
      HealthPillar(
        id = "foreground_service",
        title = "Foreground Service (Keep-Alive)",
        subtitle = if (isFgsRunning) "सक्रिय (बैकग्राउंड प्रक्रिया सुरक्षित)" else "सक्रिय सुरक्षा सेवा चालू करें",
        isHealthy = isFgsRunning,
        actionLabel = "चालू करें",
        fixAction = { ctx ->
          ProtectionWatchdogService.start(ctx)
          checkAndEnforce(ctx)
        }
      )
    )

    // 10. AI Vision Models & Screenshot Capability
    val hasAiModels = MultiModelClassifierManager.hasAnyActiveModel(context)
    val isA11yRunning = ShortContentAccessibilityService.isServiceRunning()
    val canTakeScreenshot = ShortContentAccessibilityService.canTakeScreenshotGranted()

    val aiPillarHealthy: Boolean
    val aiPillarSubtitle: String
    val aiPillarAction: String
    val aiPillarFix: (Context) -> Unit

    if (!hasAiModels) {
      aiPillarHealthy = true
      aiPillarSubtitle = "कोई मॉडल नहीं (AI पहचान सीमित है)"
      aiPillarAction = "मॉडल जोड़ें"
      aiPillarFix = { ctx ->
        val intent = Intent(ctx, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra("NAVIGATE_TO_SETTINGS", true)
        }
        ctx.startActivity(intent)
      }
    } else if (!isA11yRunning) {
      aiPillarHealthy = false
      aiPillarSubtitle = "मॉडल सक्रिय हैं पर Accessibility बंद है (AI विज़न रुका हुआ है)"
      aiPillarAction = "चालू करें"
      aiPillarFix = { ctx ->
        try {
          val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          ctx.startActivity(intent)
        } catch (_: Exception) {}
      }
    } else if (!canTakeScreenshot) {
      aiPillarHealthy = false
      aiPillarSubtitle = "Accessibility को एक बार OFF/ON करें (स्क्रीनशॉट अनुमति OS द्वारा रोकी गई है)"
      aiPillarAction = "OFF/ON करें"
      aiPillarFix = { ctx ->
        try {
          val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          ctx.startActivity(intent)
        } catch (_: Exception) {}
      }
    } else {
      aiPillarHealthy = true
      aiPillarSubtitle = "सक्रिय (ऑन-डिवाइस मल्टी-मॉडल विज़न व स्क्रीनशॉट अनुमति उपलब्ध)"
      aiPillarAction = "मॉडल देखें"
      aiPillarFix = { ctx ->
        val intent = Intent(ctx, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          putExtra("NAVIGATE_TO_SETTINGS", true)
        }
        ctx.startActivity(intent)
      }
    }

    pillars.add(
      HealthPillar(
        id = "ai_models",
        title = "AI Screen Vision Models",
        subtitle = aiPillarSubtitle,
        isHealthy = aiPillarHealthy,
        actionLabel = aiPillarAction,
        fixAction = aiPillarFix
      )
    )

    // 11. Multi-User & Guest Profile Bypass Protection
    val multiUserStatus = MultiUserProtectionManager.getStatus(context)
    val multiUserHealthy = multiUserStatus.isEnabled && multiUserStatus.isAccessibilityActive
    val multiUserSubtitle = when {
      !multiUserStatus.isEnabled -> "सुरक्षा निष्क्रिय है"
      !multiUserStatus.isAccessibilityActive -> "Accessibility बंद है (Layer 1 सुरक्षा उपलब्ध नहीं)"
      multiUserStatus.isDeviceOwner -> "Device Owner सक्रिय: Add/Switch User hard-block लागू (${multiUserStatus.interceptionCount} बार रोका गया)"
      multiUserStatus.hasWriteSecureSettings -> "Secure Settings सक्रिय + Accessibility सक्रिय (${multiUserStatus.interceptionCount} बार रोका गया)"
      else -> "Accessibility Layer सक्रिय (Device Owner सेट नहीं है: हार्ड-ब्लॉक बंद; ${multiUserStatus.interceptionCount} बार रोका गया)"
    }
    pillars.add(
      HealthPillar(
        id = "multi_user",
        title = "Multi-User / Guest बाईपास सुरक्षा",
        subtitle = multiUserSubtitle,
        isHealthy = multiUserHealthy,
        actionLabel = if (!multiUserStatus.isDeviceOwner) "Guide / Setup" else "सेटिंग्स",
        fixAction = { ctx ->
          val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO_SETTINGS", true)
          }
          ctx.startActivity(intent)
        }
      )
    )

    // 12. Security Alerts Notification Channel (Requirement 4)
    val isAlertsChannelEnabled = AppNotificationChannels.isChannelEnabled(context, AppNotificationChannels.CHANNEL_SECURITY_ALERTS)
    pillars.add(
      HealthPillar(
        id = "security_alerts_channel",
        title = "सुरक्षा अलर्ट सूचना चैनल",
        subtitle = if (isAlertsChannelEnabled) {
          "सक्रिय (छेड़छाड़ व महत्वपूर्ण सुरक्षा चेतावनियाँ सक्षम)"
        } else {
          "⚠️ Security alerts बंद हैं (छेड़छाड़ सूचनाएँ नहीं मिलेंगी)"
        },
        isHealthy = isAlertsChannelEnabled,
        actionLabel = if (!isAlertsChannelEnabled) "सेटिंग्स खोलें" else "चैनल देखें",
        fixAction = { ctx ->
          try {
            ctx.startActivity(AppNotificationChannels.createOpenChannelSettingsIntent(ctx, AppNotificationChannels.CHANNEL_SECURITY_ALERTS))
          } catch (_: Exception) {}
        }
      )
    )

    val healthyCount = pillars.count { it.isHealthy }
    val totalCount = pillars.size
    val isAllHealthy = pillars.take(8).all { it.isHealthy } // Primary 8 are critical
    val healthPct = ((healthyCount.toFloat() / totalCount.toFloat()) * 100).toInt()

    val failedPillars = pillars.filter { !it.isHealthy }
    val failedSummary = if (failedPillars.isEmpty()) {
      "सभी सुरक्षा घटक पूर्णतः सक्रिय हैं"
    } else {
      "${failedPillars.size} समस्याएँ: " + failedPillars.joinToString(", ") { it.title }
    }

    val uptimePct = getSevenDayUptimePercentage(context)

    return ProtectionHealthReport(
      isAllHealthy = isAllHealthy,
      healthyCount = healthyCount,
      totalCount = totalCount,
      healthPercentage = healthPct,
      pillars = pillars,
      failedSummary = failedSummary,
      sevenDayUptimePercentage = uptimePct,
      lastHeartbeatSecAgo = secAgo
    )
  }

  /**
   * Main periodic Watchdog Execution called by WorkManager and AlarmManager every 15 minutes.
   */
  fun performWatchdogCheck(context: Context) {
    // If user temporarily paused protection, do not trigger alerts or restart components
    if (ProtectionPauseManager.isPaused(context)) {
      Log.i(TAG, "Protection is temporarily paused by PIN. Skipping health alert checks.")
      scheduleNextWatchdogExactAlarm(context)
      return
    }

    val report = getHealthReport(context)
    recordUptimeSnapshot(context, report.isAllHealthy)

    if (!report.isAllHealthy) {
      logHealthEvent(
        context = context,
        eventType = "HEALTH_CHECK_FAILED",
        message = "सुरक्षा जाँच विफल: ${report.failedSummary}",
        severity = "WARN"
      )

      val lastAlert = getPrefs(context).getLong(KEY_LAST_ALERT_TIMESTAMP, 0L)
      val now = System.currentTimeMillis()
      // Re-alert immediately or if 30 minutes elapsed since last reminder
      if (now - lastAlert >= ALERT_REMINDER_INTERVAL_MS) {
        postSecurityAlertNotification(context, report.failedSummary)
        getPrefs(context).edit().putLong(KEY_LAST_ALERT_TIMESTAMP, now).apply()
      }
    } else {
      // Clear alert notification if everything resolved
      clearSecurityAlertNotification(context)
    }

    // Check if Accessibility heartbeat missed
    if (!isAccessibilityHeartbeatAlive(context) && AccessibilityHelper.isAccessibilityServiceEnabled(context)) {
      logHealthEvent(
        context = context,
        eventType = "HEARTBEAT_MISSED",
        message = "Accessibility Service से 2.5 मिनट से कोई Heartbeat प्राप्त नहीं हुआ",
        severity = "WARN"
      )
    }

    // Enforce multi-user Device Owner & Secure Settings restrictions
    MultiUserProtectionManager.applyDeviceOwnerRestrictions(context)
    MultiUserProtectionManager.verifyAndEnforceSecureSettings(context)

    // Schedule next exact alarm in 15 minutes
    scheduleNextWatchdogExactAlarm(context)
  }

  fun checkAndEnforce(context: Context) {
    performWatchdogCheck(context)
  }

  /**
   * Posts High Priority notification when security components are down.
   */
  fun postSecurityAlertNotification(context: Context, issueSummary: String) {
    AppNotificationChannels.registerAllChannels(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("OPEN_PROTECTION_HEALTH", true)
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 9121, intent, pendingFlags)

    val notification = NotificationCompat.Builder(context, AppNotificationChannels.CHANNEL_SECURITY_ALERTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🚨 सुरक्षा बंद है! ठीक करने के लिए टैप करें")
      .setContentText(issueSummary)
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("Life Tracker की सुरक्षा अधूरी है: $issueSummary। पूर्ण डिजिटल सुरक्षा और लॉक बनाए रखने के लिए टैप करके ठीक करें।")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.notify(NOTIF_ID_HEALTH_ALERT, notification)
  }

  fun clearSecurityAlertNotification(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(NOTIF_ID_HEALTH_ALERT)
  }

  /**
   * Schedules an exact AlarmManager tick every 15 minutes for resilient double-heartbeat checking.
   */
  fun scheduleNextWatchdogExactAlarm(context: Context) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, com.example.alarm.WakeUpAlarmReceiver::class.java).apply {
      action = "com.mohit.lifetracker.ACTION_WATCHDOG_HEARTBEAT_ALARM"
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pi = PendingIntent.getBroadcast(context, 9125, intent, flags)

    val triggerAt = System.currentTimeMillis() + (15 * 60 * 1000L) // 15 mins

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
      } else {
        am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Exact alarm schedule fallback: ${e.message}")
      am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }
  }

  /**
   * Lifecycle Handler for BOOT_COMPLETED.
   */
  fun onBootCompleted(context: Context) {
    logHealthEvent(context, "BOOT_COMPLETED", "फ़ोन रीबूट पूरा हुआ। सभी सुरक्षा सेवाएँ पुनः आरंभ हो रही हैं।", "INFO")
    // Re-anchor anti-tamper timers across reboot (Strict Lock, 24-hr unlock, Pause)
    try {
      FocusModeManager.getStrictLockRemainingMillis(context)
      AlwaysOnSecurityManager.getRemainingUnlockDelayMillis(context)
      ProtectionPauseManager.getPauseRemainingMillis(context)
    } catch (e: Exception) {
      Log.w(TAG, "Error checking timers on boot: ${e.message}")
    }
    scheduleNextWatchdogExactAlarm(context)
    scheduleWorkManagerHeartbeat(context)
    performWatchdogCheck(context)
  }

  /**
   * Lifecycle Handler for MY_PACKAGE_REPLACED (App Updated).
   */
  fun onPackageReplaced(context: Context) {
    logHealthEvent(context, "APP_UPDATED", "Life Tracker का नया वर्ज़न इंस्टॉल हुआ (MY_PACKAGE_REPLACED)। सुरक्षा पुनः सत्यापित की जा रही है।", "INFO")
    scheduleNextWatchdogExactAlarm(context)
    scheduleWorkManagerHeartbeat(context)
    performWatchdogCheck(context)
  }

  /**
   * Lifecycle Handler for onTaskRemoved.
   */
  fun onTaskRemoved(context: Context) {
    logHealthEvent(context, "ON_TASK_REMOVED", "ऐप को रीसेंट ऐप्स से स्वाइप/बंद किया गया (onTaskRemoved)। Watchdog रीस्टार्ट निर्धारित हुआ।", "WARN")
    scheduleNextWatchdogExactAlarm(context)
  }

  /**
   * Schedules WorkManager periodic heartbeat check.
   */
  fun scheduleWorkManagerHeartbeat(context: Context) {
    try {
      val workRequest = androidx.work.PeriodicWorkRequestBuilder<ProtectionHeartbeatWorker>(
        15, java.util.concurrent.TimeUnit.MINUTES
      ).build()

      androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "LIFE_TRACKER_PROTECTION_HEARTBEAT_WORK",
        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
        workRequest
      )
    } catch (e: Exception) {
      Log.w(TAG, "Failed scheduling WorkManager heartbeat: ${e.message}")
    }
  }

  /**
   * Logs a persistent health event.
   */
  fun logHealthEvent(context: Context, eventType: String, message: String, severity: String) {
    recordLogEntry(context, eventType, message, severity)
  }

  fun recordLogEntry(context: Context, eventType: String, message: String, severity: String) {
    try {
      val logs = getHealthLogs(context).toMutableList()
      val newEntry = HealthLogEntry(
        timestamp = System.currentTimeMillis(),
        eventType = eventType,
        message = message,
        severity = severity
      )
      logs.add(0, newEntry) // Newest first
      if (logs.size > MAX_LOG_ENTRIES) {
        logs.subList(MAX_LOG_ENTRIES, logs.size).clear()
      }

      val arr = JSONArray()
      for (entry in logs) {
        arr.put(JSONObject().apply {
          put("timestamp", entry.timestamp)
          put("eventType", entry.eventType)
          put("message", entry.message)
          put("severity", entry.severity)
        })
      }
      getPrefs(context).edit().putString(KEY_HEALTH_LOGS_JSON, arr.toString()).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Error logging health event", e)
    }
  }

  fun getHealthLogs(context: Context): List<HealthLogEntry> {
    val jsonStr = getPrefs(context).getString(KEY_HEALTH_LOGS_JSON, null) ?: return emptyList()
    val list = mutableListOf<HealthLogEntry>()
    try {
      val arr = JSONArray(jsonStr)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          HealthLogEntry(
            timestamp = obj.getLong("timestamp"),
            eventType = obj.getString("eventType"),
            message = obj.getString("message"),
            severity = obj.optString("severity", "INFO")
          )
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error parsing health logs", e)
    }
    return list
  }

  fun clearLogs(context: Context) {
    getPrefs(context).edit().remove(KEY_HEALTH_LOGS_JSON).apply()
  }

  /**
   * Records a snapshot of whether all security was healthy at this check.
   * Keeps up to 7 days of 15-minute snapshots (~672 snapshots).
   */
  private fun recordUptimeSnapshot(context: Context, isHealthy: Boolean) {
    try {
      val currentSnapshots = getUptimeSnapshots(context).toMutableList()
      val now = System.currentTimeMillis()
      currentSnapshots.add(Pair(now, isHealthy))

      // Prune entries older than 7 days
      val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
      val pruned = currentSnapshots.filter { it.first >= sevenDaysAgo }

      val arr = JSONArray()
      for ((time, healthy) in pruned) {
        val obj = JSONObject().apply {
          put("t", time)
          put("h", healthy)
        }
        arr.put(obj)
      }
      getPrefs(context).edit().putString(KEY_UPTIME_CHECKS_JSON, arr.toString()).apply()
    } catch (_: Exception) {}
  }

  private fun getUptimeSnapshots(context: Context): List<Pair<Long, Boolean>> {
    val str = getPrefs(context).getString(KEY_UPTIME_CHECKS_JSON, null) ?: return emptyList()
    val list = mutableListOf<Pair<Long, Boolean>>()
    try {
      val arr = JSONArray(str)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(Pair(obj.getLong("t"), obj.getBoolean("h")))
      }
    } catch (_: Exception) {}
    return list
  }

  fun getSevenDayUptimePercentage(context: Context): Int {
    val snapshots = getUptimeSnapshots(context)
    if (snapshots.isEmpty()) return 100
    val healthyCount = snapshots.count { it.second }
    return ((healthyCount.toFloat() / snapshots.size.toFloat()) * 100).toInt().coerceIn(0, 100)
  }

  private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Protection Health & Watchdog",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Life Tracker की सुरक्षा व डिजिटल अनुशासन सक्रियता अलर्ट"
        enableVibration(true)
      }
      nm.createNotificationChannel(channel)
    }
  }
}
