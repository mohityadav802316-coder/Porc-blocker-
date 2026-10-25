package com.example.focus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.WakeUpAlarmReceiver
import com.example.data.db.LifeTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Manages Full-System Focus Mode (BlockSite-style blocking), Allowed Apps Whitelist,
 * AlarmManager Exact Schedule / Duration persistence, and reboot/restart restoration.
 */
object FocusModeManager {

  private const val TAG = "FocusModeManager"

  private const val PREFS_NAME = "life_tracker_focus_prefs"
  private const val KEY_FOCUS_ACTIVE = "focus_active"
  private const val KEY_FOCUS_END_TIMESTAMP = "focus_end_timestamp"
  private const val KEY_ALLOWED_PACKAGES = "focus_allowed_packages"
  private const val KEY_FOCUS_SCHEDULE_ENABLED = "focus_schedule_enabled"
  private const val KEY_FOCUS_START_MINUTES = "focus_start_minutes"
  private const val KEY_FOCUS_END_MINUTES = "focus_end_minutes"

  // Strict Lock (Adult Content Detection & Anti-Tamper) Constants
  private const val KEY_STRICT_LOCK_ACTIVE = "strict_lock_active"
  private const val KEY_STRICT_LOCK_START_TIMESTAMP = "strict_lock_start_timestamp"
  private const val KEY_STRICT_LOCK_END_TIMESTAMP = "strict_lock_end_timestamp"
  private const val KEY_STRICT_LOCK_START_ELAPSED = "strict_lock_start_elapsed"
  private const val KEY_STRICT_LOCK_ELAPSED_END = "strict_lock_elapsed_end"
  private const val KEY_STRICT_LOCK_DURATION_MS = "strict_lock_duration_ms"
  private const val KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS = "strict_lock_last_saved_remaining_ms"
  private const val KEY_STRICT_LOCK_REASON = "strict_lock_reason"
  private const val KEY_STRICT_LOCK_TRIGGER_WORD = "strict_lock_trigger_word"
  private const val KEY_STRICT_LOCK_EVENT_ID = "strict_lock_event_id"

  const val ACTION_FOCUS_MODE_ACTIVATE = "com.example.alarm.ACTION_FOCUS_MODE_ACTIVATE"
  const val ACTION_FOCUS_MODE_DEACTIVATE = "com.example.alarm.ACTION_FOCUS_MODE_DEACTIVATE"

  const val FOCUS_NOTIFICATION_CHANNEL_ID = "life_tracker_focus_channel"
  const val FOCUS_NOTIFICATION_ID = 9001

  private const val REQUEST_CODE_ACTIVATE = 8011
  private const val REQUEST_CODE_DEACTIVATE = 8012
  private const val REQUEST_CODE_EXACT_END = 8015

  // =========================================================================
  // STATE & DURATION PERSISTENCE (ANTI-TAMPERING TIMING ENGINE)
  // =========================================================================

  /**
   * Evaluates if Strict Lock is currently active.
   * While active, no user can cancel, change timer, or unlock Focus Mode early.
   */
  fun isStrictLockActive(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val active = prefs.getBoolean(KEY_STRICT_LOCK_ACTIVE, false)
    if (!active) return false

    return getStrictLockRemainingMillis(context) > 0L
  }

  fun getStrictLockEndTimestamp(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_STRICT_LOCK_END_TIMESTAMP, 0L)
  }

  /**
   * 🛡️ ANTI-TAMPERING REMAINING TIME ENGINE:
   * Uses both System.currentTimeMillis() (Epoch) and SystemClock.elapsedRealtime().
   * SystemClock.elapsedRealtime() is completely independent of the system clock and
   * CANNOT be manipulated by the user changing Date/Time settings in Android.
   */
  fun getStrictLockRemainingMillis(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val active = prefs.getBoolean(KEY_STRICT_LOCK_ACTIVE, false)
    if (!active) return 0L

    val epochEnd = prefs.getLong(KEY_STRICT_LOCK_END_TIMESTAMP, 0L)
    val startElapsed = prefs.getLong(KEY_STRICT_LOCK_START_ELAPSED, 0L)
    val elapsedEnd = prefs.getLong(KEY_STRICT_LOCK_ELAPSED_END, 0L)
    val lastSavedRemaining = prefs.getLong(KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS, 0L)

    val nowEpoch = System.currentTimeMillis()
    val nowElapsed = SystemClock.elapsedRealtime()

    // Detect if the device has rebooted since lock was initiated
    val hasRebooted = nowElapsed < startElapsed

    val remaining: Long = if (!hasRebooted && elapsedEnd > 0L) {
      // 🛡️ ANTI-TAMPER CHECK:
      // Phone was NOT rebooted. SystemClock.elapsedRealtime() cannot be manipulated by user!
      // Even if clock was moved hours forward or backward, elapsedRealtime ensures exact duration.
      val elapsedRemaining = (elapsedEnd - nowElapsed).coerceAtLeast(0L)
      elapsedRemaining
    } else {
      // Device was rebooted: verify against saved state and wall-clock.
      // Time aage badhane par timer kam na ho!
      val epochRemaining = epochEnd - nowEpoch
      val effectiveRemaining = if (lastSavedRemaining > 0L) {
        val calculated = (lastSavedRemaining - nowElapsed).coerceAtLeast(0L)
        if (epochRemaining in 0L..lastSavedRemaining && epochRemaining >= calculated) {
          epochRemaining
        } else {
          // If wall-clock was manipulated forward or rebooted, rely on saved state minus elapsed time since reboot
          calculated
        }
      } else {
        epochRemaining.coerceAtLeast(0L)
      }

      // Re-anchor to current boot session so subsequent calls use elapsedRealtime without drift or stuck state!
      if (effectiveRemaining > 0L) {
        prefs.edit()
          .putLong(KEY_STRICT_LOCK_START_ELAPSED, 0L)
          .putLong(KEY_STRICT_LOCK_ELAPSED_END, effectiveRemaining)
          .putLong(KEY_STRICT_LOCK_END_TIMESTAMP, nowEpoch + effectiveRemaining)
          .putLong(KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS, effectiveRemaining)
          .apply()
      }
      effectiveRemaining
    }

    if (remaining <= 0L) {
      prefs.edit()
        .putBoolean(KEY_STRICT_LOCK_ACTIVE, false)
        .putLong(KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS, 0L)
        .apply()
      return 0L
    } else {
      prefs.edit().putLong(KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS, remaining).apply()
      return remaining
    }
  }

  fun getStrictLockReason(context: Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_STRICT_LOCK_REASON, "वयस्क / अनुपयुक्त सामग्री पहचान") ?: "वयस्क / अनुपयुक्त सामग्री पहचान"
  }

  /**
   * Activates unbreakable Strict Lock for [durationMinutes] (default 15 minutes).
   * Triggered automatically upon detecting adult content.
   * Stores both Epoch End Time and SystemClock.elapsedRealtime End for tamper resistance.
   */
  fun activateStrictLock(
    context: Context,
    durationMinutes: Int = 15,
    reason: String = "वयस्क सामग्री पहचानी गई",
    triggerWord: String = "",
    packageName: String = ""
  ) {
    createNotificationChannel(context)
    val durationMs = durationMinutes * 60 * 1000L
    val nowEpoch = System.currentTimeMillis()
    val nowElapsed = SystemClock.elapsedRealtime()
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val currentStrictEnd = prefs.getLong(KEY_STRICT_LOCK_END_TIMESTAMP, 0L)
    val newEpochEnd = nowEpoch + durationMs
    val finalEpochEnd = if (currentStrictEnd > nowEpoch) maxOf(currentStrictEnd, newEpochEnd) else newEpochEnd

    val currentElapsedEnd = prefs.getLong(KEY_STRICT_LOCK_ELAPSED_END, 0L)
    val newElapsedEnd = nowElapsed + durationMs
    val finalElapsedEnd = if (currentElapsedEnd > nowElapsed) maxOf(currentElapsedEnd, newElapsedEnd) else newElapsedEnd

    prefs.edit()
      .putBoolean(KEY_STRICT_LOCK_ACTIVE, true)
      .putLong(KEY_STRICT_LOCK_START_TIMESTAMP, nowEpoch)
      .putLong(KEY_STRICT_LOCK_END_TIMESTAMP, finalEpochEnd)
      .putLong(KEY_STRICT_LOCK_START_ELAPSED, nowElapsed)
      .putLong(KEY_STRICT_LOCK_ELAPSED_END, finalElapsedEnd)
      .putLong(KEY_STRICT_LOCK_DURATION_MS, durationMs)
      .putLong(KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS, durationMs)
      .putString(KEY_STRICT_LOCK_REASON, reason)
      .putString(KEY_STRICT_LOCK_TRIGGER_WORD, triggerWord)
      .apply()

    saveFocusState(context, active = true, endTimestamp = finalEpochEnd)
    scheduleFocusExactEndAlarm(context, finalEpochEnd)
    showStrictLockNotification(context, durationMinutes, reason)

    // Launch persistent Foreground Service with ongoing live countdown notification
    StrictLockService.start(context)

    // Log to Room Database (StrictLockDao & UserSettingsDao)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val event = com.example.data.model.StrictLockEventEntity(
          timestamp = nowEpoch,
          date = com.example.util.TimeUtils.getTodayDateString(),
          time = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(nowEpoch)),
          reason = reason,
          triggerWord = triggerWord,
          packageName = packageName,
          durationMinutes = durationMinutes,
          epochEndTimestamp = finalEpochEnd,
          elapsedRealtimeEnd = finalElapsedEnd,
          startElapsedRealtime = nowElapsed,
          isCompleted = false
        )
        val eventId = db.strictLockDao().insertEvent(event)
        prefs.edit().putLong(KEY_STRICT_LOCK_EVENT_ID, eventId).apply()

        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = true,
              focusSessionEndTimestamp = finalEpochEnd
            )
          )
        }
        Log.i(TAG, "Strict Lock active for $durationMinutes mins ($reason) | Anti-tamper elapsedEnd: $finalElapsedEnd")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist strict lock to Room", e)
      }
    }
  }

  fun canDeactivateFocus(context: Context): Boolean {
    return !isStrictLockActive(context)
  }

  fun isFocusModeActive(context: Context): Boolean {
    if (isStrictLockActive(context)) return true

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 1. Self-healing check for scheduled focus mode
    val scheduleEnabled = prefs.getBoolean(KEY_FOCUS_SCHEDULE_ENABLED, false)
    if (scheduleEnabled) {
      val startMinutes = prefs.getInt(KEY_FOCUS_START_MINUTES, -1)
      val endMinutes = prefs.getInt(KEY_FOCUS_END_MINUTES, -1)
      if (startMinutes >= 0 && endMinutes >= 0) {
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        if (isCurrentTimeInFocusSchedule(nowMinutes, startMinutes, endMinutes)) {
          return true
        }
      }
    }

    // 2. Check manual session duration
    val active = prefs.getBoolean(KEY_FOCUS_ACTIVE, false)
    if (!active) return false

    val endTimestamp = prefs.getLong(KEY_FOCUS_END_TIMESTAMP, 0L)
    if (endTimestamp > 0L && System.currentTimeMillis() >= endTimestamp) {
      // Auto-expired
      deactivateFocusMode(context, force = true)
      return false
    }
    return true
  }

  fun getFocusSessionEndTimestamp(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_FOCUS_END_TIMESTAMP, 0L)
  }

  fun getRemainingSessionMillis(context: Context): Long {
    if (isStrictLockActive(context)) {
      return getStrictLockRemainingMillis(context)
    }
    val endTimestamp = getFocusSessionEndTimestamp(context)
    val now = System.currentTimeMillis()
    return if (endTimestamp > now) endTimestamp - now else 0L
  }

  fun getRemainingSessionMinutes(context: Context): Int {
    val millis = getRemainingSessionMillis(context)
    return ((millis + 59999L) / 60000L).toInt().coerceAtLeast(0)
  }

  private fun saveFocusState(context: Context, active: Boolean, endTimestamp: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_FOCUS_ACTIVE, active)
      .putLong(KEY_FOCUS_END_TIMESTAMP, endTimestamp)
      .apply()
  }

  // =========================================================================
  // ALLOWED APPS WHITELIST MANAGEMENT (GUARDED DURING STRICT LOCK)
  // =========================================================================

  @Volatile
  private var cachedAllowedPackages: Set<String>? = null

  @Volatile
  private var cachedCoreWhitelist: Set<String>? = null

  fun getAllowedPackages(context: Context): Set<String> {
    val cached = cachedAllowedPackages
    if (cached != null) return cached

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val set = prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
    cachedAllowedPackages = set
    return set
  }

  /**
   * Sets user allowed apps whitelist.
   * Strictly blocked if Strict Lock is currently active!
   */
  fun setAllowedPackages(context: Context, packages: Set<String>) {
    if (isStrictLockActive(context)) {
      Log.w(TAG, "Cannot modify allowed apps: Strict Lock is ACTIVE! Changes blocked.")
      return
    }
    cachedAllowedPackages = packages
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, packages).apply()
  }

  fun addAllowedPackage(context: Context, packageName: String) {
    if (isStrictLockActive(context)) {
      Log.w(TAG, "Cannot add allowed app: Strict Lock is ACTIVE!")
      return
    }
    val current = getAllowedPackages(context).toMutableSet()
    current.add(packageName)
    setAllowedPackages(context, current)
  }

  fun removeAllowedPackage(context: Context, packageName: String) {
    if (isStrictLockActive(context)) {
      Log.w(TAG, "Cannot remove allowed app: Strict Lock is ACTIVE!")
      return
    }
    val current = getAllowedPackages(context).toMutableSet()
    current.remove(packageName)
    setAllowedPackages(context, current)
  }

  /**
   * Returns immutable system packages that MUST NEVER be blocked:
   * 1. This Life Tracker app
   * 2. Phone / Default Dialer (Emergency calls)
   * 3. Home Launcher
   * 4. System UI, Android OS, and Permission Controllers
   */
  fun getCoreWhitelistedPackages(context: Context): Set<String> {
    val cached = cachedCoreWhitelist
    if (cached != null) return cached

    val core = mutableSetOf(
      context.packageName,
      "com.android.systemui",
      "android",
      "com.android.phone",
      "com.android.server.telecom",
      "com.google.android.permissioncontroller",
      "com.android.permissioncontroller",
      "com.google.android.apps.safetyhub",
      // Common OEM Phone/Dialer/In-Call apps for unconditional emergency calling
      "com.google.android.dialer",
      "com.samsung.android.dialer",
      "com.samsung.android.incallui",
      "com.android.incallui",
      "com.google.android.contacts",
      "com.android.contacts",
      "com.oneplus.dialer",
      "com.coloros.phoneno",
      "com.asus.contacts",
      "com.motorola.dialer",
      // Popular system launchers
      "com.google.android.apps.nexuslauncher",
      "com.sec.android.app.launcher",
      "com.miui.home",
      "com.oppo.launcher",
      "com.huawei.android.launcher"
    )

    // Default Phone / Dialer package
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        telecom?.defaultDialerPackage?.let { core.add(it) }
      }
    } catch (_: Exception) {}

    // Any dialer intent activities
    try {
      val pm = context.packageManager
      val dialIntent = Intent(Intent.ACTION_DIAL)
      val list = pm.queryIntentActivities(dialIntent, 0)
      for (info in list) {
        info.activityInfo?.packageName?.let { core.add(it) }
      }
    } catch (_: Exception) {}

    // Default Launcher / Home App
    try {
      val pm = context.packageManager
      val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
      }
      val resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName?.let { core.add(it) }
    } catch (_: Exception) {}

    cachedCoreWhitelist = core
    return core
  }

  /**
   * Packages and components that are ALWAYS HARD-BLOCKED during Strict Lock
   * to eliminate any tampering or uninstall evasion routes, even if present in Allowed Apps.
   */
  private val STRICT_LOCK_HARD_BLOCKED_PACKAGES = hashSetOf(
    "com.android.settings",
    "com.google.android.settings",
    "com.android.vending", // Google Play Store
    "com.google.android.packageinstaller",
    "com.android.packageinstaller",
    "com.samsung.android.packageinstaller",
    "com.miui.packageinstaller",
    "com.coloros.packageinstaller",
    "com.oppo.packageinstaller",
    "com.vivo.packageinstaller",
    "com.huawei.appmarket"
  )

  /**
   * Returns true if [packageName] is an evasion/uninstaller/settings component
   * that must be unconditionally blocked during Strict Lock.
   */
  fun isStrictLockHardBlocked(packageName: String): Boolean {
    val lower = packageName.lowercase()
    if (STRICT_LOCK_HARD_BLOCKED_PACKAGES.contains(lower)) {
      return true
    }
    if (lower.contains("packageinstaller") ||
        (lower.contains("settings") && !lower.contains("quicksettings")) ||
        lower.contains("deviceadmin") ||
        lower.contains("appmanager")) {
      return true
    }
    return false
  }

  /**
   * Deterministically evaluates if [packageName] is permitted to run in foreground.
   * Emergency calls, Phone/Dialer, and user-whitelisted apps are strictly permitted.
   */
  fun isPackageAllowed(context: Context, packageName: String): Boolean {
    if (packageName.isBlank()) return true

    // CRITICAL REQUIREMENT 2: In Strict Lock, Settings, Package Installers,
    // Play Store, Device Admin & VPN setting screens are ALWAYS HARD-BLOCKED,
    // even if accidentally or previously included in Allowed Apps!
    if (isStrictLockActive(context) && isStrictLockHardBlocked(packageName)) {
      Log.w(TAG, "Hard-blocking prohibited evasion package $packageName during active Strict Lock!")
      return false
    }

    // 1. Check core whitelist (Emergency, Dialer, Launcher, SystemUI, Life Tracker)
    if (getCoreWhitelistedPackages(context).contains(packageName)) {
      return true
    }

    // 1b. Check if package name is a dialer/telecom/emergency component (all Android OEMs)
    val lower = packageName.lowercase()
    if (lower.contains("dialer") ||
        lower.contains("incall") ||
        lower.contains("emergency") ||
        lower.contains("telecom") ||
        lower.contains("phone")) {
      return true
    }

    // 2. Check user-configured allowed apps
    val userAllowed = getAllowedPackages(context)
    return userAllowed.contains(packageName)
  }

  // =========================================================================
  // ACTIVATION MODES (DURATION & SCHEDULE)
  // =========================================================================

  /**
   * Activates Focus Mode for a specific duration in minutes (e.g. 15m, 25m Pomodoro, 60m).
   */
  fun activateFocusDuration(context: Context, durationMinutes: Int) {
    if (isStrictLockActive(context)) {
      Log.w(TAG, "Cannot modify focus duration: Strict Lock is ACTIVE! Changes blocked.")
      return
    }
    createNotificationChannel(context)
    val now = System.currentTimeMillis()
    val endTimestamp = now + (durationMinutes * 60 * 1000L)

    saveFocusState(context, active = true, endTimestamp = endTimestamp)
    scheduleFocusExactEndAlarm(context, endTimestamp)
    showFocusNotification(context, durationMinutes, isScheduled = false)

    // Sync to Room Database
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = true,
              focusSessionEndTimestamp = endTimestamp
            )
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist focus activation to Room", e)
      }
    }
  }

  /**
   * Activates / synchronizes scheduled Focus Mode (e.g. 21:00 to 06:00).
   */
  fun activateFocusSchedule(context: Context, startMinutes: Int, endMinutes: Int) {
    createNotificationChannel(context)
    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val shouldBeActive = isCurrentTimeInFocusSchedule(nowMinutes, startMinutes, endMinutes)

    val endTimestamp = if (shouldBeActive) {
      val remainingMins = calculateRemainingMinutes(nowMinutes, endMinutes)
      System.currentTimeMillis() + (remainingMins * 60 * 1000L)
    } else {
      0L
    }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_FOCUS_SCHEDULE_ENABLED, true)
      .putInt(KEY_FOCUS_START_MINUTES, startMinutes)
      .putInt(KEY_FOCUS_END_MINUTES, endMinutes)
      .apply()

    saveFocusState(context, active = shouldBeActive, endTimestamp = endTimestamp)
    scheduleFocusAlarms(context, startMinutes, endMinutes)

    if (shouldBeActive) {
      val remainingMins = calculateRemainingMinutes(nowMinutes, endMinutes)
      showFocusNotification(context, remainingMins, isScheduled = true)
    } else {
      cancelFocusNotification(context)
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = shouldBeActive,
              isFocusScheduleEnabled = true,
              focusStartTimeMinutes = startMinutes,
              focusEndTimeMinutes = endMinutes,
              focusSessionEndTimestamp = endTimestamp
            )
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist focus schedule to Room", e)
      }
    }
  }

  fun disableFocusSchedule(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_FOCUS_SCHEDULE_ENABLED, false)
      .apply()
    cancelFocusScheduleAlarms(context)
  }

  /**
   * Deactivates Focus Mode, cancels session alarms, removes blocking overlay, and notifies completion.
   * If [force] is false and Strict Lock is currently active, deactivation is strictly rejected.
   */
  fun deactivateFocusMode(context: Context, force: Boolean = false) {
    if (isStrictLockActive(context) && !force) {
      val remaining = getStrictLockRemainingMillis(context) / 1000L
      Log.w(TAG, "Cannot deactivate Focus Mode: Strict Lock is ACTIVE for another ${remaining}s. Deactivation blocked!")
      return
    }

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val eventId = prefs.getLong(KEY_STRICT_LOCK_EVENT_ID, -1L)
    prefs.edit()
      .putBoolean(KEY_STRICT_LOCK_ACTIVE, false)
      .putLong(KEY_STRICT_LOCK_LAST_SAVED_REMAINING_MS, 0L)
      .putLong(KEY_STRICT_LOCK_END_TIMESTAMP, 0L)
      .putLong(KEY_STRICT_LOCK_ELAPSED_END, 0L)
      .apply()

    saveFocusState(context, active = false, endTimestamp = 0L)
    cancelFocusExactEndAlarm(context)
    cancelFocusNotification(context)
    FocusBlockingOverlayManager.dismiss()
    StrictLockFloatingPillManager.dismiss()
    StrictLockService.stop(context)

    // Ensure recurring daily schedule remains scheduled for tomorrow if enabled
    val scheduleEnabled = prefs.getBoolean(KEY_FOCUS_SCHEDULE_ENABLED, false)
    if (scheduleEnabled) {
      val startMinutes = prefs.getInt(KEY_FOCUS_START_MINUTES, -1)
      val endMinutes = prefs.getInt(KEY_FOCUS_END_MINUTES, -1)
      if (startMinutes >= 0 && endMinutes >= 0) {
        scheduleFocusAlarms(context, startMinutes, endMinutes)
      }
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        if (eventId > 0L) {
          db.strictLockDao().markCompleted(eventId)
        } else {
          db.strictLockDao().markAllCompleted()
        }
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = false,
              focusSessionEndTimestamp = 0L
            )
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist deactivate focus to Room", e)
      }
    }
  }

  // =========================================================================
  // EXACT ALARM SCHEDULING (ALARM MANAGER)
  // =========================================================================

  fun scheduleFocusExactEndAlarm(context: Context, endTimestamp: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_EXACT_END, intent, flags)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimestamp, pendingIntent)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimestamp, pendingIntent)
      }
      Log.d(TAG, "Scheduled exact focus end alarm at $endTimestamp")
    } catch (e: SecurityException) {
      Log.w(TAG, "Exact alarm permission missing; using inexact fallback: ${e.message}")
      alarmManager.set(AlarmManager.RTC_WAKEUP, endTimestamp, pendingIntent)
    }
  }

  fun scheduleFocusAlarms(context: Context, startTimeMinutes: Int, endTimeMinutes: Int) {
    createNotificationChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val activateIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_ACTIVATE
    }
    val activatePending = PendingIntent.getBroadcast(context, REQUEST_CODE_ACTIVATE, activateIntent, flags)
    val activateMillis = calculateNextTriggerMillis(startTimeMinutes)

    val deactivateIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val deactivatePending = PendingIntent.getBroadcast(context, REQUEST_CODE_DEACTIVATE, deactivateIntent, flags)
    val deactivateMillis = calculateNextTriggerMillis(endTimeMinutes)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, activateMillis, activatePending)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deactivateMillis, deactivatePending)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, activateMillis, activatePending)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, deactivateMillis, deactivatePending)
      }
    } catch (e: SecurityException) {
      alarmManager.set(AlarmManager.RTC_WAKEUP, activateMillis, activatePending)
      alarmManager.set(AlarmManager.RTC_WAKEUP, deactivateMillis, deactivatePending)
    }
  }

  fun cancelFocusExactEndAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_NO_CREATE
    }

    val intentExact = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val pendingExact = PendingIntent.getBroadcast(context, REQUEST_CODE_EXACT_END, intentExact, flags)
    if (pendingExact != null) {
      alarmManager.cancel(pendingExact)
      pendingExact.cancel()
    }
  }

  fun cancelFocusScheduleAlarms(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_NO_CREATE
    }

    val intentActivate = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_ACTIVATE
    }
    val pendingActivate = PendingIntent.getBroadcast(context, REQUEST_CODE_ACTIVATE, intentActivate, flags)
    if (pendingActivate != null) {
      alarmManager.cancel(pendingActivate)
      pendingActivate.cancel()
    }

    val intentDeactivate = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val pendingDeactivate = PendingIntent.getBroadcast(context, REQUEST_CODE_DEACTIVATE, intentDeactivate, flags)
    if (pendingDeactivate != null) {
      alarmManager.cancel(pendingDeactivate)
      pendingDeactivate.cancel()
    }
  }

  fun cancelFocusAlarms(context: Context) {
    cancelFocusExactEndAlarm(context)
    cancelFocusScheduleAlarms(context)
  }

  // =========================================================================
  // BOOT & REBOOT RESTORATION
  // =========================================================================

  fun onBootOrScheduleChange(context: Context) {
    val now = System.currentTimeMillis()

    // 1. Strict Lock survives reboot unconditionally!
    val isStrict = isStrictLockActive(context)
    if (isStrict) {
      val remaining = getStrictLockRemainingMillis(context)
      if (remaining > 0L) {
        val newNowElapsed = SystemClock.elapsedRealtime()
        val newElapsedEnd = newNowElapsed + remaining
        val newEpochEnd = System.currentTimeMillis() + remaining

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
          .putLong(KEY_STRICT_LOCK_START_ELAPSED, newNowElapsed)
          .putLong(KEY_STRICT_LOCK_ELAPSED_END, newElapsedEnd)
          .putLong(KEY_STRICT_LOCK_END_TIMESTAMP, newEpochEnd)
          .apply()

        saveFocusState(context, active = true, endTimestamp = newEpochEnd)
        scheduleFocusExactEndAlarm(context, newEpochEnd)

        // Restart persistent Foreground Service after reboot
        StrictLockService.start(context)

        Log.i(TAG, "Restored active Strict Lock session after reboot: ${remaining / 1000L}s remaining")
        return
      } else {
        deactivateFocusMode(context, force = true)
      }
    }

    val isFocusActive = isFocusModeActive(context)
    val endTimestamp = getFocusSessionEndTimestamp(context)

    if (isFocusActive && endTimestamp > 0L) {
      if (now < endTimestamp) {
        // Active session survives reboot
        scheduleFocusExactEndAlarm(context, endTimestamp)
        val remainingMins = ((endTimestamp - now) / 60000L).toInt().coerceAtLeast(1)
        showFocusNotification(context, remainingMins, isScheduled = false)
        Log.d(TAG, "Restored active focus duration session after reboot: $remainingMins mins remaining")
      } else {
        // Expired while device was off
        deactivateFocusMode(context, force = true)
        Log.d(TAG, "Focus duration session expired during reboot/off; auto-deactivated")
      }
    } else {
      // Evaluate schedule
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val db = LifeTrackerDatabase.getDatabase(context)
          val settings = db.userSettingsDao().getSettingsSync() ?: return@launch
          if (settings.isFocusScheduleEnabled) {
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val shouldBeActive = isCurrentTimeInFocusSchedule(
              nowMinutes,
              settings.focusStartTimeMinutes,
              settings.focusEndTimeMinutes
            )

            if (shouldBeActive) {
              val remaining = calculateRemainingMinutes(nowMinutes, settings.focusEndTimeMinutes)
              saveFocusState(context, active = true, endTimestamp = now + (remaining * 60000L))
              showFocusNotification(context, remaining, isScheduled = true)
            } else {
              saveFocusState(context, active = false, endTimestamp = 0L)
              cancelFocusNotification(context)
            }
            scheduleFocusAlarms(context, settings.focusStartTimeMinutes, settings.focusEndTimeMinutes)
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to restore focus schedule after reboot", e)
        }
      }
    }
  }

  // =========================================================================
  // ONGOING NOTIFICATION
  // =========================================================================

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
      val channel = NotificationChannel(
        FOCUS_NOTIFICATION_CHANNEL_ID,
        "Focus Mode & Restriction",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Shows active Focus Mode status and remaining time"
        setShowBadge(true)
        enableLights(true)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showStrictLockNotification(context: Context, remainingMinutes: Int = 15, reason: String = "") {
    createNotificationChannel(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 8021, openAppIntent, flags)

    val builder = NotificationCompat.Builder(context, FOCUS_NOTIFICATION_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🚨 सख्त लॉक सक्रिय • $remainingMinutes मिनट शेष")
      .setContentText("वयस्क सामग्री पहचानी गई। 15 मिनट तक कोई अनलॉकिंग संभव नहीं है।")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setContentIntent(pendingIntent)
      .setAutoCancel(false)

    notificationManager.notify(FOCUS_NOTIFICATION_ID, builder.build())
  }

  fun showFocusNotification(context: Context, remainingMinutes: Int = -1, isScheduled: Boolean = false) {
    createNotificationChannel(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 8020, openAppIntent, flags)

    val remainingText = if (remainingMinutes > 0) {
      val hrs = remainingMinutes / 60
      val mins = remainingMinutes % 60
      if (hrs > 0) "$hrs घंटे $mins मिनट शेष" else "$mins मिनट शेष"
    } else {
      "डिजिटल अनुशासन सक्रिय"
    }

    val builder = NotificationCompat.Builder(context, FOCUS_NOTIFICATION_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🎯 Focus Mode सक्रिय • $remainingText")
      .setContentText("गहन अध्ययन व आत्म-नियंत्रण। अनधिकृत ऐप्स ब्लॉक हैं।")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setContentIntent(pendingIntent)
      .setAutoCancel(false)

    notificationManager.notify(FOCUS_NOTIFICATION_ID, builder.build())
  }

  fun cancelFocusNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return
    notificationManager.cancel(FOCUS_NOTIFICATION_ID)
  }

  // =========================================================================
  // UTILITY TIME CALCULATIONS
  // =========================================================================

  fun isCurrentTimeInFocusSchedule(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
    if (startMinutes == endMinutes) return false
    return if (startMinutes < endMinutes) {
      nowMinutes in startMinutes until endMinutes
    } else {
      nowMinutes >= startMinutes || nowMinutes < endMinutes
    }
  }

  fun calculateRemainingMinutes(nowMinutes: Int, endMinutes: Int): Int {
    return if (nowMinutes <= endMinutes) {
      endMinutes - nowMinutes
    } else {
      (1440 - nowMinutes) + endMinutes
    }
  }

  fun calculateNextTriggerMillis(targetMinutes: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, targetMinutes / 60)
      set(Calendar.MINUTE, targetMinutes % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (target.timeInMillis <= now.timeInMillis) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
  }
}
