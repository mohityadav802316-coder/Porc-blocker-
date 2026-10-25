package com.example.focus

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

/**
 * Manages 24/7 Always-On Protection (हमेशा-चालू सुरक्षा) for Life Tracker:
 *
 * 1. Scope Restriction (Zero Overreach):
 *    ONLY blocks tamper attempts directed at Life Tracker itself:
 *    - Life Tracker's Accessibility service detail/toggle page
 *    - Life Tracker's Device Admin deactivate page
 *    - Life Tracker's App Info page
 *    - Life Tracker's Uninstall confirmation dialog
 *    - Life Tracker's VPN forget/disconnect page
 *
 *    NEVER blocks:
 *    - Settings main page
 *    - Accessibility service list of other apps
 *    - Other apps' App Info / Uninstall / Install / Play Store pages
 *    Only acts when Life Tracker's identity ("Life Tracker", "लाइफ ट्रैकर", package name)
 *    is explicitly present on that sensitive screen.
 *
 * 2. 24-Hour Unlock Request:
 *    Allows user to request unlock with a mandatory 24-hour delay.
 *    Once the 24 hours expire, protection automatically deactivates (pause-like state)
 *    and repeating re-enable reminders trigger every 30 minutes in the Reminders channel.
 */
object AlwaysOnSecurityManager {

  private const val TAG = "AlwaysOnSecurityMgr"
  private const val PREFS_NAME = "life_tracker_always_on_security"

  private const val KEY_ALWAYS_ON_ENABLED = "always_on_enabled"
  private const val KEY_UNLOCK_REQUEST_TIMESTAMP = "unlock_request_timestamp"
  private const val KEY_UNLOCK_REQUEST_TARGET_TIME = "unlock_request_target_time"
  private const val KEY_UNLOCK_REQUEST_START_ELAPSED = "unlock_request_start_elapsed"
  private const val KEY_UNLOCK_REQUEST_ELAPSED_END = "unlock_request_elapsed_end"
  private const val KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS = "unlock_request_last_saved_remaining_ms"
  private const val KEY_UNLOCK_WINDOW_EXPIRY = "unlock_window_expiry"

  const val DELAY_24_HOURS_MS = 24 * 60 * 60 * 1000L // 24 hours
  const val UNLOCK_WINDOW_DURATION_MS = 60 * 60 * 1000L // 1 hour window once granted

  const val NOTIFICATION_ID_UNLOCK_REQ = 9022

  data class TamperCheckResult(
    val isTamper: Boolean,
    val screenType: String = "",
    val reason: String = ""
  )

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isAlwaysOnEnabled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_ALWAYS_ON_ENABLED, true)
  }

  /**
   * Sets Always-On Security state. Gated: cannot be turned OFF without PIN verification
   * or active 24-hour temporary unlock. Turning ON is always permitted without PIN.
   */
  fun setAlwaysOnEnabled(context: Context, enabled: Boolean, pinVerified: Boolean = false): Boolean {
    if (!enabled && !pinVerified && !isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot disable Always-On Security: PIN verification or 24-hr unlock required")
        return false
      }
    }
    getPrefs(context).edit().putBoolean(KEY_ALWAYS_ON_ENABLED, enabled).apply()
    return true
  }

  /**
   * Returns true if the temporary 1-hour unlock window is currently open (after 24-hr delay).
   */
  fun isTemporarilyUnlocked(context: Context): Boolean {
    val expiry = getPrefs(context).getLong(KEY_UNLOCK_WINDOW_EXPIRY, 0L)
    return System.currentTimeMillis() < expiry
  }

  fun getUnlockRequestTargetTime(context: Context): Long {
    return getPrefs(context).getLong(KEY_UNLOCK_REQUEST_TARGET_TIME, 0L)
  }

  fun getRemainingUnlockDelayMillis(context: Context): Long {
    val prefs = getPrefs(context)
    val target = getUnlockRequestTargetTime(context)
    if (target == 0L) return 0L

    val startElapsed = prefs.getLong(KEY_UNLOCK_REQUEST_START_ELAPSED, 0L)
    val elapsedEnd = prefs.getLong(KEY_UNLOCK_REQUEST_ELAPSED_END, 0L)
    val lastSavedRemaining = prefs.getLong(KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS, DELAY_24_HOURS_MS)
    val nowEpoch = System.currentTimeMillis()
    val nowElapsed = SystemClock.elapsedRealtime()

    val hasRebooted = nowElapsed < startElapsed
    val remaining: Long = if (!hasRebooted && elapsedEnd > 0L) {
      // Hardware clock: user cannot change via Settings -> Date & Time
      val elapsedRemaining = (elapsedEnd - nowElapsed).coerceAtLeast(0L)
      val epochRemaining = (target - nowEpoch).coerceAtLeast(0L)
      // Enforce maximum remaining: prevents fast-forwarding system date!
      maxOf(elapsedRemaining, epochRemaining)
    } else {
      // Device was rebooted: verify against saved state and wall-clock.
      // Time aage badhane par timer kam na ho!
      val epochRemaining = target - nowEpoch
      val calculated = (lastSavedRemaining - nowElapsed).coerceAtLeast(0L)
      val effectiveRemaining = if (epochRemaining in 0L..lastSavedRemaining && epochRemaining >= calculated) {
        epochRemaining
      } else {
        calculated
      }

      // Re-anchor to current boot session so subsequent calls use elapsedRealtime without drift or stuck state!
      if (effectiveRemaining > 0L) {
        prefs.edit()
          .putLong(KEY_UNLOCK_REQUEST_START_ELAPSED, 0L)
          .putLong(KEY_UNLOCK_REQUEST_ELAPSED_END, effectiveRemaining)
          .putLong(KEY_UNLOCK_REQUEST_TARGET_TIME, nowEpoch + effectiveRemaining)
          .putLong(KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS, effectiveRemaining)
          .apply()
      }
      effectiveRemaining
    }

    if (remaining > 0L) {
      prefs.edit().putLong(KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS, remaining).apply()
    }
    return remaining
  }

  fun hasPendingUnlockRequest(context: Context): Boolean {
    val target = getUnlockRequestTargetTime(context)
    if (target == 0L) return false
    return getRemainingUnlockDelayMillis(context) > 0L
  }

  fun isUnlockRequestReadyToClaim(context: Context): Boolean {
    val target = getUnlockRequestTargetTime(context)
    if (target == 0L) return false
    return getRemainingUnlockDelayMillis(context) == 0L && !isTemporarilyUnlocked(context)
  }

  /**
   * Starts a 24-hour Unlock Request.
   */
  fun startUnlockRequest(context: Context): Long {
    val now = System.currentTimeMillis()
    val targetTime = now + DELAY_24_HOURS_MS
    val nowElapsed = SystemClock.elapsedRealtime()
    val elapsedEnd = nowElapsed + DELAY_24_HOURS_MS

    getPrefs(context).edit()
      .putLong(KEY_UNLOCK_REQUEST_TIMESTAMP, now)
      .putLong(KEY_UNLOCK_REQUEST_TARGET_TIME, targetTime)
      .putLong(KEY_UNLOCK_REQUEST_START_ELAPSED, nowElapsed)
      .putLong(KEY_UNLOCK_REQUEST_ELAPSED_END, elapsedEnd)
      .putLong(KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS, DELAY_24_HOURS_MS)
      .remove(KEY_UNLOCK_WINDOW_EXPIRY)
      .apply()

    Log.i(TAG, "Started 24-hour Unlock Request. Target time: $targetTime")
    postUnlockRequestNotification(context, DELAY_24_HOURS_MS)
    return targetTime
  }

  /**
   * Cancels the active Unlock Request.
   */
  fun cancelUnlockRequest(context: Context) {
    getPrefs(context).edit()
      .remove(KEY_UNLOCK_REQUEST_TIMESTAMP)
      .remove(KEY_UNLOCK_REQUEST_TARGET_TIME)
      .remove(KEY_UNLOCK_REQUEST_START_ELAPSED)
      .remove(KEY_UNLOCK_REQUEST_ELAPSED_END)
      .remove(KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS)
      .remove(KEY_UNLOCK_WINDOW_EXPIRY)
      .apply()

    Log.i(TAG, "Cancelled 24-hour Unlock Request.")
    dismissNotification(context)
  }

  /**
   * Automatically deactivates protection when 24 hours expire (Requirement 3),
   * putting app into a clean pause-like state with repeating 30-min reminders.
   */
  fun claimUnlockWindow(context: Context): Boolean {
    if (!isUnlockRequestReadyToClaim(context)) return false
    val now = System.currentTimeMillis()
    val expiry = now + UNLOCK_WINDOW_DURATION_MS
    getPrefs(context).edit()
      .putLong(KEY_UNLOCK_WINDOW_EXPIRY, expiry)
      .remove(KEY_UNLOCK_REQUEST_TIMESTAMP)
      .remove(KEY_UNLOCK_REQUEST_TARGET_TIME)
      .remove(KEY_UNLOCK_REQUEST_START_ELAPSED)
      .remove(KEY_UNLOCK_REQUEST_ELAPSED_END)
      .remove(KEY_UNLOCK_REQUEST_LAST_SAVED_REMAINING_MS)
      .apply()

    Log.i(TAG, "24-hr unlock matured: Pausing protection automatically.")
    dismissNotification(context)

    // Deactivate protection automatically (just like pause)
    ProtectionPauseManager.startPause(context, durationMinutes = 60)
    return true
  }

  /**
   * Evaluates whether the current screen is an attempt to tamper with Life Tracker.
   *
   * Crucial Policy:
   * 1. If screen does NOT contain Life Tracker identity (English, Hindi, or package name),
   *    returns isTamper = false immediately.
   * 2. Settings main page, Accessibility list, other apps' App Info / Uninstall are NEVER blocked.
   * 3. Blocks ONLY Life Tracker's own:
   *    - Accessibility toggle/detail page
   *    - Device Admin deactivate page
   *    - App Info page
   *    - Uninstall confirmation dialog
   *    - VPN forget/disconnect page
   */
  fun evaluateTamperScreen(
    context: Context,
    packageName: String,
    className: String,
    visibleText: String
  ): TamperCheckResult {
    // If user has temporarily paused protection or 24-hr unlock window is open, allow access
    if (ProtectionPauseManager.isPaused(context) || isTemporarilyUnlocked(context) || !isAlwaysOnEnabled(context)) {
      return TamperCheckResult(isTamper = false)
    }

    val lowerPkg = packageName.lowercase()
    val lowerCls = className.lowercase()
    val lowerText = visibleText.lowercase()
    val ourPkg = context.packageName.lowercase()

    // Match Life Tracker identity on screen (Requirement 1):
    // 1. App name in English ("life tracker", "lifetracker")
    // 2. App name in Hindi ("लाइफ ट्रैकर")
    // 3. Package name or component ("com.example")
    val hasLifeTrackerIdentity = lowerText.contains("life tracker") ||
      lowerText.contains("lifetracker") ||
      lowerText.contains("लाइफ ट्रैकर") ||
      lowerText.contains(ourPkg)

    // If Life Tracker identity is NOT on screen, NEVER block! (Allows other apps & general settings)
    if (!hasLifeTrackerIdentity) {
      return TamperCheckResult(isTamper = false)
    }

    // Now evaluate if this specific screen is one of the 5 tamper targets:

    // 1. Life Tracker's Accessibility service detail/toggle page
    val isSettingsPkg = lowerPkg == "com.android.settings" || lowerPkg == "com.google.android.settings"
    if (isSettingsPkg) {
      val isA11yScreen = lowerCls.contains("accessibility") || lowerCls.contains("togglescreen") || lowerCls.contains("subsettings")
      if (isA11yScreen) {
        // Must be the DETAIL / TOGGLE page of Life Tracker, not merely a mention in a generic list
        val hasToggleIndicators = lowerText.contains("use life tracker") ||
          lowerText.contains("life tracker चालू करें") ||
          lowerText.contains("लाइफ ट्रैकर का इस्तेमाल करें") ||
          lowerText.contains("allow life tracker") ||
          lowerText.contains("shortcut") ||
          lowerText.contains("शॉर्टकट") ||
          lowerText.contains("stop") ||
          lowerText.contains("रोकें") ||
          lowerText.contains("shortcontentaccessibilityservice") ||
          (lowerText.contains("service") && (lowerText.contains("on") || lowerText.contains("off") || lowerText.contains("चालू") || lowerText.contains("बंद")))

        if (hasToggleIndicators) {
          return TamperCheckResult(
            isTamper = true,
            screenType = "Accessibility Service Toggle",
            reason = "Life Tracker एक्सेसिबिलिटी सेवा बंद करने का प्रयास"
          )
        }
      }

      // 2. Life Tracker's Device Admin deactivate page
      val isDeviceAdminScreen = lowerCls.contains("deviceadmin") || lowerCls.contains("device_admin")
      if (isDeviceAdminScreen) {
        val hasDeactivateWords = lowerText.contains("deactivate") ||
          lowerText.contains("निष्क्रिय") ||
          lowerText.contains("remove") ||
          lowerText.contains("हटाएं") ||
          lowerText.contains("admin") ||
          lowerText.contains("प्रशासक")
        if (hasDeactivateWords) {
          return TamperCheckResult(
            isTamper = true,
            screenType = "Device Admin Deactivation",
            reason = "Life Tracker डिवाइस एडमिनिस्ट्रेटर हटाने का प्रयास"
          )
        }
      }

      // 3. Life Tracker's App Info page
      val isAppInfoScreen = lowerCls.contains("appinfo") ||
        lowerCls.contains("installedapp") ||
        lowerCls.contains("applications") ||
        lowerCls.contains("manageapplications") ||
        lowerCls.contains("appdetails")
      if (isAppInfoScreen) {
        val hasAppInfoActions = lowerText.contains("uninstall") ||
          lowerText.contains("अनइंस्टॉल") ||
          lowerText.contains("force stop") ||
          lowerText.contains("जबरन रोकें") ||
          lowerText.contains("storage") ||
          lowerText.contains("स्टोरेज") ||
          lowerText.contains("permissions") ||
          lowerText.contains("अनुमतियां")
        if (hasAppInfoActions) {
          return TamperCheckResult(
            isTamper = true,
            screenType = "App Info Page",
            reason = "Life Tracker ऐप इन्फो या सेटिंग्स से छेड़छाड़ का प्रयास"
          )
        }
      }

      // 4. Life Tracker's VPN forget/disconnect page
      val isVpnScreen = lowerCls.contains("vpn")
      if (isVpnScreen) {
        val hasVpnTamperWords = lowerText.contains("forget") ||
          lowerText.contains("disconnect") ||
          lowerText.contains("हटाएं") ||
          lowerText.contains("डिस्कनेक्ट") ||
          lowerText.contains("dns")
        if (hasVpnTamperWords) {
          return TamperCheckResult(
            isTamper = true,
            screenType = "VPN Disconnect/Forget",
            reason = "Life Tracker DNS VPN फ़िल्टर हटाने का प्रयास"
          )
        }
      }
    }

    // 5. Life Tracker's Uninstall confirmation dialog (PackageInstaller)
    if (lowerPkg.contains("packageinstaller") || lowerPkg.contains("settings")) {
      val hasUninstallQuestion = lowerText.contains("uninstall") ||
        lowerText.contains("अनइंस्टॉल") ||
        lowerText.contains("do you want to uninstall") ||
        lowerText.contains("क्या आप इस ऐप को अनइंस्टॉल")
      if (hasUninstallQuestion) {
        return TamperCheckResult(
          isTamper = true,
          screenType = "Uninstall Confirmation Dialog",
          reason = "Life Tracker अनइंस्टॉल करने का प्रयास"
        )
      }
    }

    return TamperCheckResult(isTamper = false)
  }

  fun postUnlockRequestNotification(context: Context, remainingMillis: Long) {
    AppNotificationChannels.registerAllChannels(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val hours = remainingMillis / (3600 * 1000L)
    val mins = (remainingMillis % (3600 * 1000L)) / (60 * 1000L)
    val timeFormatted = String.format("%02d घंटे %02d मिनट", hours, mins)

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("NAVIGATE_TO_SETTINGS", true)
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val contentPending = PendingIntent.getActivity(context, NOTIFICATION_ID_UNLOCK_REQ, openIntent, pendingFlags)

    val notification = NotificationCompat.Builder(context, AppNotificationChannels.CHANNEL_REMINDERS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("⏳ सुरक्षा अनलॉक अनुरोध सक्रिय")
      .setContentText("24 घंटे का अनिवार्य इंतज़ार • शेष: $timeFormatted")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("Life Tracker सुरक्षा अनलॉक का अनुरोध जारी है।\nशेष समय: $timeFormatted\nआवेग में लिए गए निर्णय से बचने के लिए 24 घंटे की देरी लागू है। 24 घंटे बाद सुरक्षा स्वतः पॉज़ हो जाएगी। आप इसे सेटिंग्स से कभी भी रद्द कर सकते हैं।")
      )
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setAutoCancel(false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setContentIntent(contentPending)
      .build()

    nm.notify(NOTIFICATION_ID_UNLOCK_REQ, notification)
  }

  fun dismissNotification(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(NOTIFICATION_ID_UNLOCK_REQ)
  }
}
