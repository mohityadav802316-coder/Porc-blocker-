package com.example.focus

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.shortcontent.AccessibilityHelper
import com.example.shortcontent.ShortContentAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages Temporary Protection Pause (जवाबदेही पिन द्वारा अस्थायी पॉज़) for Life Tracker:
 *
 * 1. User enters Accountability PIN and selects duration (30 min / 1 hour / 2 hours / 4 hours).
 * 2. Temporarily disables all protection layers so banking / sensitive apps work without hindrance:
 *    - Accessibility Service (calls disableSelf())
 *    - Local DNS VPN Filter (stops VPN)
 *    - Device Admin (calls removeActiveAdmin())
 *    - Strict Lock (deactivates lock & overlay)
 *    - AI Screen Vision Scanning
 * 3. Daily quota: Maximum 3 pauses per day.
 * 4. During pause: Displays a persistent notification with live countdown and an "अभी वापस ON करें" button.
 * 5. When pause expires: Sends a re-enable reminder notification and repeats every 30 minutes (in the dedicated
 *    Reminders channel) until all protection components are restored by the user.
 * 6. Start & end recorded in Protection Health Log.
 */
object ProtectionPauseManager {

  private const val TAG = "ProtectionPauseMgr"
  private const val PREFS_NAME = "life_tracker_protection_pause_prefs"

  private const val KEY_IS_PAUSED = "is_paused"
  private const val KEY_PAUSE_START_TIME = "pause_start_time"
  private const val KEY_PAUSE_END_TIME = "pause_end_time"
  private const val KEY_PAUSE_START_ELAPSED = "pause_start_elapsed"
  private const val KEY_PAUSE_ELAPSED_END = "pause_elapsed_end"
  private const val KEY_PAUSE_LAST_SAVED_REMAINING_MS = "pause_last_saved_remaining_ms"
  private const val KEY_PAUSE_DURATION_MINS = "pause_duration_mins"
  private const val KEY_LAST_PAUSE_DATE = "last_pause_date"
  private const val KEY_PAUSES_COUNT_TODAY = "pauses_count_today"
  private const val KEY_LAST_REMINDER_TIMESTAMP = "last_reminder_timestamp"

  const val MAX_PAUSES_PER_DAY = 3
  const val NOTIFICATION_ID_PAUSE_COUNTDOWN = 9030
  const val NOTIFICATION_ID_REENABLE_REMINDER = 9031
  const val REMINDER_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

  val DURATION_OPTIONS_MINUTES = listOf(30, 60, 120, 240)

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  private fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
  }

  /**
   * Returns how many pauses have been used today (max 3). Resets daily automatically.
   */
  fun getPausesUsedToday(context: Context): Int {
    val prefs = getPrefs(context)
    val savedDate = prefs.getString(KEY_LAST_PAUSE_DATE, "")
    val today = getTodayDateString()
    return if (savedDate == today) {
      prefs.getInt(KEY_PAUSES_COUNT_TODAY, 0)
    } else {
      0
    }
  }

  fun canPauseToday(context: Context): Boolean {
    return getPausesUsedToday(context) < MAX_PAUSES_PER_DAY
  }

  /**
   * Returns true if protection is currently temporarily paused.
   */
  fun isPaused(context: Context): Boolean {
    val prefs = getPrefs(context)
    val isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
    if (!isPaused) return false

    if (getPauseRemainingMillis(context) <= 0L) {
      // Pause has expired! Clean up pause state and initiate re-enable reminders
      onPauseExpired(context)
      return false
    }
    return true
  }

  fun getPauseRemainingMillis(context: Context): Long {
    val prefs = getPrefs(context)
    val isPaused = prefs.getBoolean(KEY_IS_PAUSED, false)
    if (!isPaused) return 0L

    val epochEnd = prefs.getLong(KEY_PAUSE_END_TIME, 0L)
    val startElapsed = prefs.getLong(KEY_PAUSE_START_ELAPSED, 0L)
    val elapsedEnd = prefs.getLong(KEY_PAUSE_ELAPSED_END, 0L)
    val lastSavedRemaining = prefs.getLong(KEY_PAUSE_LAST_SAVED_REMAINING_MS, 0L)

    val nowEpoch = System.currentTimeMillis()
    val nowElapsed = SystemClock.elapsedRealtime()

    val hasRebooted = nowElapsed < startElapsed
    val remaining: Long = if (!hasRebooted && elapsedEnd > 0L) {
      val elapsedRemaining = (elapsedEnd - nowElapsed).coerceAtLeast(0L)
      val epochRemaining = (epochEnd - nowEpoch).coerceAtLeast(0L)
      // Enforce the maximum remaining: prevents bypassing pause expiration by altering system clock
      maxOf(elapsedRemaining, epochRemaining)
    } else {
      // Device was rebooted: verify against saved state and wall-clock.
      // Time aage badhane par timer kam na ho!
      val epochRemaining = epochEnd - nowEpoch
      val calculated = if (lastSavedRemaining > 0L) (lastSavedRemaining - nowElapsed).coerceAtLeast(0L) else epochRemaining.coerceAtLeast(0L)
      val effectiveRemaining = if (lastSavedRemaining > 0L && epochRemaining in 0L..lastSavedRemaining && epochRemaining >= calculated) {
        epochRemaining
      } else {
        calculated
      }

      // Re-anchor to current boot session so subsequent calls use elapsedRealtime cleanly!
      if (effectiveRemaining > 0L) {
        prefs.edit()
          .putLong(KEY_PAUSE_START_ELAPSED, 0L)
          .putLong(KEY_PAUSE_ELAPSED_END, effectiveRemaining)
          .putLong(KEY_PAUSE_END_TIME, nowEpoch + effectiveRemaining)
          .putLong(KEY_PAUSE_LAST_SAVED_REMAINING_MS, effectiveRemaining)
          .apply()
      }
      effectiveRemaining
    }

    if (remaining > 0L) {
      prefs.edit().putLong(KEY_PAUSE_LAST_SAVED_REMAINING_MS, remaining).apply()
    }
    return remaining
  }

  fun getPauseDurationMinutes(context: Context): Int {
    return getPrefs(context).getInt(KEY_PAUSE_DURATION_MINS, 30)
  }

  /**
   * Starts a temporary pause for the given duration in minutes.
   */
  fun startPause(context: Context, durationMinutes: Int): Boolean {
    if (FocusModeManager.isStrictLockActive(context)) {
      Log.w(TAG, "Cannot pause: Strict Lock is ACTIVE! Pause blocked.")
      return false
    }
    val today = getTodayDateString()
    val usedToday = getPausesUsedToday(context)
    if (usedToday >= MAX_PAUSES_PER_DAY) {
      Log.w(TAG, "Cannot pause: Daily limit of $MAX_PAUSES_PER_DAY reached!")
      return false
    }

    val now = System.currentTimeMillis()
    val durationMs = durationMinutes * 60_000L
    val endTime = now + durationMs
    val nowElapsed = SystemClock.elapsedRealtime()
    val elapsedEnd = nowElapsed + durationMs

    getPrefs(context).edit()
      .putBoolean(KEY_IS_PAUSED, true)
      .putLong(KEY_PAUSE_START_TIME, now)
      .putLong(KEY_PAUSE_END_TIME, endTime)
      .putLong(KEY_PAUSE_START_ELAPSED, nowElapsed)
      .putLong(KEY_PAUSE_ELAPSED_END, elapsedEnd)
      .putLong(KEY_PAUSE_LAST_SAVED_REMAINING_MS, durationMs)
      .putInt(KEY_PAUSE_DURATION_MINS, durationMinutes)
      .putString(KEY_LAST_PAUSE_DATE, today)
      .putInt(KEY_PAUSES_COUNT_TODAY, usedToday + 1)
      .remove(KEY_LAST_REMINDER_TIMESTAMP)
      .apply()

    // 1. Log event in Health Log
    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "PROTECTION_PAUSE_STARTED",
      message = "सुरक्षा $durationMinutes मिनट के लिए पॉज़ की गई (आज: ${usedToday + 1}/$MAX_PAUSES_PER_DAY)",
      severity = "INFO"
    )

    // 2. Shut down Accessibility Service (via disableSelf)
    try {
      ShortContentAccessibilityService.disableService()
    } catch (e: Exception) {
      Log.w(TAG, "Error disabling accessibility service: ${e.message}")
    }

    // 3. Stop Local DNS VPN
    try {
      LocalDnsVpnManager.stopVpn(context)
    } catch (e: Exception) {
      Log.w(TAG, "Error stopping VPN: ${e.message}")
    }

    // 4. Remove Device Admin so banking apps do not flag or clash
    try {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
      val adminComponent = LifeTrackerAdminReceiver.getComponentName(context)
      if (dpm != null && dpm.isAdminActive(adminComponent)) {
        dpm.removeActiveAdmin(adminComponent)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error removing active device admin: ${e.message}")
    }

    // 5. Deactivate Strict Lock & Overlay
    try {
      FocusModeManager.deactivateFocusMode(context, force = true)
      FocusBlockingOverlayManager.dismiss()
    } catch (e: Exception) {
      Log.w(TAG, "Error clearing strict lock: ${e.message}")
    }

    // 6. Pause AI screen scanning
    try {
      AiScreenDetectionManager.setEnabled(context, false)
    } catch (e: Exception) {
      Log.w(TAG, "Error pausing AI detection: ${e.message}")
    }

    // 7. Post ongoing pause notification with countdown & restore button
    postPauseCountdownNotification(context, durationMinutes * 60_000L)

    Log.i(TAG, "Protection paused successfully for $durationMinutes minutes.")
    return true
  }

  /**
   * Posts or updates the ongoing countdown notification during pause.
   */
  fun postPauseCountdownNotification(context: Context, remainingMillis: Long) {
    AppNotificationChannels.registerAllChannels(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val totalSecs = (remainingMillis / 1000L).coerceAtLeast(0)
    val hours = totalSecs / 3600
    val mins = (totalSecs % 3600) / 60
    val timeFormatted = if (hours > 0) "${hours}घंटे ${mins}मिनट" else "${mins} मिनट"

    val restoreIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("NAVIGATE_TO_SETTINGS", true)
      putExtra("SHOW_RESTORE_PROTECTION", true)
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val restorePending = PendingIntent.getActivity(context, 9030, restoreIntent, pendingFlags)

    val notification = NotificationCompat.Builder(context, AppNotificationChannels.CHANNEL_PROTECTION_RUNNING)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("⏸️ सुरक्षा अस्थायी रूप से पॉज़ है")
      .setContentText("शेष समय: $timeFormatted • बैंकिंग/अन्य काम हेतु")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("Life Tracker सुरक्षा पॉज़ पर है।\nशेष समय: $timeFormatted\nपॉज़ समाप्त होने पर आपको रिमाइंडर मिलेगा। आप कभी भी नीचे बटन दबाकर इसे तुरंत दोबारा चालू कर सकते हैं।")
      )
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setContentIntent(restorePending)
      .addAction(
        R.mipmap.ic_launcher,
        "अभी वापस ON करें",
        restorePending
      )
      .build()

    nm.notify(NOTIFICATION_ID_PAUSE_COUNTDOWN, notification)
  }

  /**
   * Resumes protection immediately, ending any active pause.
   */
  fun resumeProtection(context: Context) {
    getPrefs(context).edit().putBoolean(KEY_IS_PAUSED, false).apply()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(NOTIFICATION_ID_PAUSE_COUNTDOWN)
    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "PROTECTION_PAUSE_CANCELLED",
      message = "Protection resumed immediately by user.",
      severity = "INFO"
    )
  }

  /**
   * Called when pause expires.
   */
  private fun onPauseExpired(context: Context) {
    getPrefs(context).edit().putBoolean(KEY_IS_PAUSED, false).apply()

    // Dismiss countdown notification
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(NOTIFICATION_ID_PAUSE_COUNTDOWN)

    // Log to Health Log
    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "PROTECTION_PAUSE_ENDED",
      message = "सुरक्षा पॉज़ समय समाप्त हुआ। कृपया सभी सुरक्षा घटक वापस चालू करें।",
      severity = "WARN"
    )

    // Send first re-enable reminder
    checkAndSendReenableReminder(context, force = true)
  }

  /**
   * Checks whether re-enable reminder should be posted (repeats every 30 minutes until restored).
   */
  fun checkAndSendReenableReminder(context: Context, force: Boolean = false) {
    // If protection is currently active or paused, no reminder needed
    if (isPaused(context)) return

    // If both Accessibility and VPN are already active, no reminder needed
    val a11yActive = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val vpnActive = LocalDnsVpnService.isVpnRunning.value
    if (a11yActive && vpnActive) {
      dismissReenableReminder(context)
      return
    }

    val prefs = getPrefs(context)
    val lastReminder = prefs.getLong(KEY_LAST_REMINDER_TIMESTAMP, 0L)
    val now = System.currentTimeMillis()

    if (force || (now - lastReminder >= REMINDER_INTERVAL_MS)) {
      postReenableReminderNotification(context)
      prefs.edit().putLong(KEY_LAST_REMINDER_TIMESTAMP, now).apply()
    }
  }

  fun postReenableReminderNotification(context: Context) {
    AppNotificationChannels.registerAllChannels(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("OPEN_PROTECTION_HEALTH", true)
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val openPending = PendingIntent.getActivity(context, 9031, openIntent, pendingFlags)

    val notification = NotificationCompat.Builder(context, AppNotificationChannels.CHANNEL_REMINDERS)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🔔 सुरक्षा वापस चालू करने का रिमाइंडर")
      .setContentText("पॉज़ समाप्त हो चुका है। Accessibility और VPN फ़िल्टर दोबारा चालू करें।")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("Life Tracker सुरक्षा पॉज़ समाप्त हो चुका है।\nअपने फ़ोन की अनुशासन सुरक्षा बनाए रखने हेतु Accessibility और VPN फ़िल्टर को दोबारा सक्रिय करें।\n(यह रिमाइंडर हर 30 मिनट में दोहराया जाएगा)")
      )
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(openPending)
      .addAction(
        R.mipmap.ic_launcher,
        "सुरक्षा चालू करें",
        openPending
      )
      .build()

    nm.notify(NOTIFICATION_ID_REENABLE_REMINDER, notification)
  }

  fun dismissReenableReminder(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(NOTIFICATION_ID_REENABLE_REMINDER)
  }

  /**
   * User manually restores protection or turns it back on.
   */
  fun cancelPause(context: Context) {
    getPrefs(context).edit()
      .putBoolean(KEY_IS_PAUSED, false)
      .remove(KEY_PAUSE_LAST_SAVED_REMAINING_MS)
      .apply()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.cancel(NOTIFICATION_ID_PAUSE_COUNTDOWN)
    dismissReenableReminder(context)

    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "PROTECTION_PAUSE_CANCELLED",
      message = "उपयोगकर्ता द्वारा सुरक्षा पॉज़ समाप्त करके सुरक्षा बहाल की गई।",
      severity = "INFO"
    )
  }
}
