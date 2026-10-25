package com.example.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.shortcontent.AccessibilityHelper

/**
 * Watchdog & System Protection Monitor for Life Tracker.
 *
 * Monitors critical protection services:
 * 1. Accessibility Service connection status
 * 2. DNS VPN Filter running state
 * 3. AI Screen Detection permission status after device reboots
 *
 * If any of these essential security pillars are disabled or stopped
 * while Strict Lock is active or expected, immediately triggers a high-priority
 * alert notification and resumes enforcement upon opening Life Tracker.
 */
object StrictLockWatchdog {

  private const val TAG = "StrictLockWatchdog"
  private const val CHANNEL_ID = "life_tracker_security_watchdog"
  private const val NOTIF_ID_WATCHDOG_ALERT = 9010
  private const val NOTIF_ID_AI_REBOOT_ALERT = 9011

  fun checkAndEnforce(context: Context) {
    // If protection is currently paused by Accountability PIN, do not trigger alerts or restart components
    if (ProtectionPauseManager.isPaused(context)) {
      Log.i(TAG, "Protection is temporarily paused. Skipping watchdog alerts and enforcement.")
      return
    }

    // If 24-hour unlock request has completed its delay, automatically claim and deactivate protection cleanly
    if (AlwaysOnSecurityManager.isUnlockRequestReadyToClaim(context)) {
      Log.i(TAG, "24-hour unlock delay has elapsed. Auto-claiming unlock window.")
      AlwaysOnSecurityManager.claimUnlockWindow(context)
      return
    }

    val isStrict = FocusModeManager.isStrictLockActive(context)
    val hasAccessibility = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val isVpnActive = LocalDnsVpnService.isVpnRunning.value

    // CRITICAL REQUIREMENT: Accessibility या VPN बंद पाए जाने पर watchdog चेतावनी भेजे (जब पॉज़ न हो)
    if (!hasAccessibility) {
      Log.w(TAG, "WATCHDOG TRIGGERED: Accessibility Service is disabled! Sending alert.")
      postAlertNotification(
        context = context,
        title = "⚠️ सुरक्षा चेतावनी: एक्सेसिबिलिटी सेवा बंद है!",
        message = if (isStrict) {
          "सख्त डिजिटल अनुशासन (Strict Lock) चालू है। फ़ौरन एक्सेसिबिलिटी दोबारा चालू करें।"
        } else {
          "Peewee Block की सुरक्षा व डिजिटल अनुशासन सक्रिय रखने हेतु एक्सेसिबिलिटी चालू करना आवश्यक है।"
        },
        intentAction = android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
      )
    }

    if (LocalDnsVpnManager.isEnabled(context) && !isVpnActive) {
      Log.w(TAG, "WATCHDOG TRIGGERED: DNS VPN filter is stopped! Sending alert.")
      if (isStrict) {
        LocalDnsVpnManager.startVpn(context)
      } else {
        postAlertNotification(
          context = context,
          title = "⚠️ सुरक्षा चेतावनी: लोकल DNS फ़िल्टर बंद है!",
          message = "वयस्क वेबसाइटों को नेटवर्क स्तर पर रोकने के लिए VPN फ़िल्टर चालू करें।",
          intentAction = android.provider.Settings.ACTION_VPN_SETTINGS
        )
      }
    }

    if (isStrict) {
      // Ensure StrictLockService foreground watchdog is alive
      StrictLockService.start(context)
    }

    // Update pending Unlock Request notification if active
    if (AlwaysOnSecurityManager.hasPendingUnlockRequest(context)) {
      val remaining = AlwaysOnSecurityManager.getRemainingUnlockDelayMillis(context)
      AlwaysOnSecurityManager.postUnlockRequestNotification(context, remaining)
    }

    // Multi-User restrictions enforcement (Device Owner & Secure Settings)
    MultiUserProtectionManager.applyDeviceOwnerRestrictions(context)
    MultiUserProtectionManager.verifyAndEnforceSecureSettings(context)
  }

  /**
   * Notifies user after device reboot if AI Screen Detection was previously enabled,
   * reminding them that Android OS requires a one-tap MediaProjection permission re-grant.
   */
  fun checkAiDetectionAfterReboot(context: Context) {
    if (AiScreenDetectionManager.isEnabled(context)) {
      Log.i(TAG, "Device rebooted with AI Screen Detection enabled. Posting permission re-prompt notification.")
      createNotificationChannel(context)

      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("NAVIGATE_TO_SETTINGS", true)
      }
      val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      val pendingIntent = PendingIntent.getActivity(context, 9012, intent, pendingFlags)

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("🧠 AI Screen Detection: अनुमति आवश्यक")
        .setContentText("फोन रीबूट के बाद ऑन-डिवाइस AI स्क्रीन सुरक्षा फिर शुरू करने के लिए टैप करें।")
        .setStyle(
          NotificationCompat.BigTextStyle()
            .bigText("Android सुरक्षा नीति के अनुसार स्क्रीन सुरक्षा चालू रखने के लिए एक बार अनुमति देना आवश्यक है। कोई भी तस्वीर फ़ोन से बाहर नहीं जाती।")
        )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.notify(NOTIF_ID_AI_REBOOT_ALERT, notification)
    }
  }

  fun postAlertNotification(
    context: Context,
    title: String,
    message: String,
    intentAction: String
  ) {
    AppNotificationChannels.registerAllChannels(context)

    val targetIntent = Intent(intentAction).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, NOTIF_ID_WATCHDOG_ALERT, targetIntent, pendingFlags)

    val notification = NotificationCompat.Builder(context, AppNotificationChannels.CHANNEL_SECURITY_ALERTS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setOngoing(false)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.notify(NOTIF_ID_WATCHDOG_ALERT, notification)
  }

  private fun createNotificationChannel(context: Context) {
    AppNotificationChannels.registerAllChannels(context)
  }
}
