package com.example.shortcontent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.ShortAppType
import com.example.data.model.ShortContentDailySummaryEntity
import com.example.data.model.ShortContentRecordEntity
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central Strict-Mode Short Content Tracker Manager.
 *
 * Implements:
 * 1. Strict counting with minimum viewing threshold (2.5s) to avoid swipe-through counts.
 * 2. Anti-duplicate signature LRU cache + DB lookup to prevent overcounting on refresh/redraw/rewind.
 * 3. Exact per-app metrics (Instagram Reels, YouTube Shorts, Facebook Reels).
 * 4. Foreground session time accumulation.
 * 5. Configurable warnings (50%, 80%, 100%) and Focus Lock / Digital Discipline integration.
 * 6. Completely offline, safe local database storage.
 */
class ShortContentTrackerManager private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val database = LifeTrackerDatabase.getDatabase(appContext)
  private val shortContentDao = database.shortContentDao()
  private val userSettingsDao = database.userSettingsDao()
  private val mutex = Mutex()

  private val _todaySummary = MutableStateFlow<ShortContentDailySummaryEntity?>(null)
  val todaySummary: StateFlow<ShortContentDailySummaryEntity?> = _todaySummary.asStateFlow()

  private val _isServiceConnected = MutableStateFlow(false)
  val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

  fun notifyServiceConnected(connected: Boolean) {
    _isServiceConnected.value = connected
  }

  // In-flight video state
  private var activeAppPackage: String? = null
  private var activeSignature: String? = null
  private var viewStartTimestamp: Long = 0L
  private var isCurrentVideoCommitted: Boolean = false

  // Session time tracking
  private var shortSessionStartMs: Long = 0L

  // Anti-duplicate memory cache: holds recent signatures and timestamp
  private val recentSignatures = LinkedHashMap<String, Long>(50, 0.75f, true)
  private val DUPLICATE_CACHE_WINDOW_MS = 15 * 60 * 1000L // 15 minutes window
  private val MIN_WATCH_THRESHOLD_MS = 1200L // 1.2 seconds natural debounce before counting
  private var watchCommitJob: kotlinx.coroutines.Job? = null

  // Warning thresholds triggered today
  private var warning50TriggeredDate: String? = null
  private var warning80TriggeredDate: String? = null
  private var warning100TriggeredDate: String? = null

  init {
    createDisciplineNotificationChannel()
    loadTodaySummary()
  }

  fun loadTodaySummary() {
    scope.launch {
      val today = TimeUtils.getTodayDateString()
      val summary = shortContentDao.getDailySummaryDirect(today) ?: ShortContentDailySummaryEntity(date = today)
      _todaySummary.value = summary
    }
  }

  /**
   * Check if user has exceeded their configured daily short content limit.
   */
  fun isDailyLimitExceeded(): Boolean {
    val today = TimeUtils.getTodayDateString()
    val summary = _todaySummary.value
    if (summary == null || summary.date != today) {
      loadTodaySummary()
      return false
    }
    val limit = summary.dailyLimit
    return limit > 0 && summary.totalCount >= limit
  }

  /**
   * Called by ShortContentAccessibilityService when a short-video container is detected.
   */
  fun onShortVideoDetected(
    appPackage: String,
    signature: String?,
    nowMs: Long = System.currentTimeMillis()
  ) {
    if (signature.isNullOrBlank()) return

    scope.launch {
      mutex.withLock {
        // Automatically rollover to today's summary if day changed
        val today = TimeUtils.getTodayDateString()
        if (_todaySummary.value?.date != today) {
          val current = shortContentDao.getDailySummaryDirect(today) ?: ShortContentDailySummaryEntity(date = today)
          _todaySummary.value = current
        }

        // Check if settings allow short tracking
        val settings = userSettingsDao.getSettingsDirect()
        if (settings?.shortTrackingEnabled == false) return@withLock

        // Start session timer if entering short video screen
        if (shortSessionStartMs == 0L) {
          shortSessionStartMs = nowMs
        }

        // If the signature is the exact same as currently active video, ignore (same video playing/redraw)
        if (activeSignature == signature) {
          // If already playing for >= MIN_WATCH_THRESHOLD_MS and not yet committed, commit now!
          if (!isCurrentVideoCommitted && (nowMs - viewStartTimestamp) >= MIN_WATCH_THRESHOLD_MS) {
            commitShortVideo(appPackage, signature, nowMs)
          }
          return@withLock
        }

        // Different video detected!
        // First: if previous video reached threshold before switching, commit it!
        val prevSig = activeSignature
        if (prevSig != null && !isCurrentVideoCommitted && (nowMs - viewStartTimestamp) >= MIN_WATCH_THRESHOLD_MS) {
          commitShortVideo(activeAppPackage ?: appPackage, prevSig, nowMs)
        }

        // Cancel any pending commit job from previous video
        watchCommitJob?.cancel()

        // Now setup the new video
        activeAppPackage = appPackage
        activeSignature = signature
        viewStartTimestamp = nowMs
        isCurrentVideoCommitted = false

        // Check duplicate cache (only for real token signatures, never blacklist dynamic sequences)
        cleanDuplicateCache(nowMs)
        val isDynamicSequence = signature.contains("_seq_")
        if (!isDynamicSequence) {
          if (recentSignatures.containsKey(signature)) {
            // Video was already counted recently in this session! Mark as already committed so we don't duplicate.
            isCurrentVideoCommitted = true
            return@withLock
          }

          // Also check DB for duplicates within recent window
          val recentCountInDb = shortContentDao.countRecentSignature(signature, nowMs - DUPLICATE_CACHE_WINDOW_MS)
          if (recentCountInDb > 0) {
            recentSignatures[signature] = nowMs
            isCurrentVideoCommitted = true
            return@withLock
          }
        }

        // Automatically commit this video once watched for MIN_WATCH_THRESHOLD_MS (1.2s)
        watchCommitJob = scope.launch {
          kotlinx.coroutines.delay(MIN_WATCH_THRESHOLD_MS)
          mutex.withLock {
            if (activeSignature == signature && !isCurrentVideoCommitted) {
              commitShortVideo(appPackage, signature, System.currentTimeMillis())
            }
          }
        }
      }
    }
  }

  /**
   * Called periodically (or on scroll/state changed) to finalize in-flight watch duration.
   */
  fun checkInFlightVideoCommit(nowMs: Long = System.currentTimeMillis()) {
    scope.launch {
      mutex.withLock {
        val sig = activeSignature
        if (sig != null && !isCurrentVideoCommitted && (nowMs - viewStartTimestamp) >= MIN_WATCH_THRESHOLD_MS) {
          commitShortVideo(activeAppPackage ?: "other", sig, nowMs)
        }
      }
    }
  }

  /**
   * Called when user leaves the short-video screen or switches apps.
   */
  fun onExitShortVideoScreen(nowMs: Long = System.currentTimeMillis()) {
    watchCommitJob?.cancel()
    watchCommitJob = null
    scope.launch {
      mutex.withLock {
        // If in-flight video met threshold, commit it
        val inFlightSig = activeSignature
        if (inFlightSig != null && !isCurrentVideoCommitted && (nowMs - viewStartTimestamp) >= MIN_WATCH_THRESHOLD_MS) {
          commitShortVideo(activeAppPackage ?: "other", inFlightSig, nowMs)
        }

        // Flush session time
        if (shortSessionStartMs > 0L) {
          val elapsedSeconds = ((nowMs - shortSessionStartMs) / 1000).coerceAtLeast(0)
          if (elapsedSeconds > 0) {
            flushSessionDuration(elapsedSeconds)
          }
          shortSessionStartMs = 0L
        }

        activeSignature = null
        activeAppPackage = null
        isCurrentVideoCommitted = false
      }
    }
  }

  /**
   * Commits a validated, non-duplicate short video to the database.
   */
  private suspend fun commitShortVideo(appPackage: String, signature: String, nowMs: Long) {
    isCurrentVideoCommitted = true
    recentSignatures[signature] = nowMs

    val today = TimeUtils.getTodayDateString()
    val appType = ShortAppType.fromPackage(appPackage)
    val appName = appType.appName

    val durationSec = ((nowMs - viewStartTimestamp) / 1000).toInt().coerceAtLeast(2)

    // Insert record
    val record = ShortContentRecordEntity(
      date = today,
      appPackage = appPackage,
      appName = appName,
      videoSignature = signature,
      timestamp = nowMs,
      durationSeconds = durationSec
    )
    shortContentDao.insertRecord(record)

    // Update Daily Summary
    val settings = userSettingsDao.getSettingsDirect()
    val dailyLimit = settings?.shortDailyLimit ?: 20

    val currentSummary = shortContentDao.getDailySummaryDirect(today) ?: ShortContentDailySummaryEntity(date = today, dailyLimit = dailyLimit)

    val updatedSummary = currentSummary.copy(
      instagramCount = if (appType == ShortAppType.INSTAGRAM) currentSummary.instagramCount + 1 else currentSummary.instagramCount,
      youtubeCount = if (appType == ShortAppType.YOUTUBE) currentSummary.youtubeCount + 1 else currentSummary.youtubeCount,
      facebookCount = if (appType == ShortAppType.FACEBOOK) currentSummary.facebookCount + 1 else currentSummary.facebookCount,
      otherCount = if (appType == ShortAppType.OTHER || appType == ShortAppType.SNAPCHAT) currentSummary.otherCount + 1 else currentSummary.otherCount,
      totalCount = currentSummary.totalCount + 1,
      totalTimeSeconds = currentSummary.totalTimeSeconds + durationSec,
      dailyLimit = dailyLimit,
      lastUpdated = nowMs
    )

    shortContentDao.insertOrUpdateSummary(updatedSummary)
    _todaySummary.value = updatedSummary

    // Check limit warnings
    checkLimitWarnings(updatedSummary, settings?.shortWarning50Enabled ?: true, settings?.shortWarning80Enabled ?: true, settings?.shortWarning100Enabled ?: true, settings?.shortFocusLockIntegration ?: true)
  }

  private suspend fun flushSessionDuration(elapsedSeconds: Long) {
    val today = TimeUtils.getTodayDateString()
    val currentSummary = shortContentDao.getDailySummaryDirect(today) ?: return
    val updatedSummary = currentSummary.copy(
      totalTimeSeconds = currentSummary.totalTimeSeconds + elapsedSeconds,
      lastUpdated = System.currentTimeMillis()
    )
    shortContentDao.insertOrUpdateSummary(updatedSummary)
    _todaySummary.value = updatedSummary
  }

  private fun cleanDuplicateCache(nowMs: Long) {
    val iterator = recentSignatures.entries.iterator()
    while (iterator.hasNext()) {
      val entry = iterator.next()
      if (nowMs - entry.value > DUPLICATE_CACHE_WINDOW_MS) {
        iterator.remove()
      }
    }
  }

  /**
   * Check configurable warning thresholds (50%, 80%, 100%) and trigger discipline notifications.
   */
  private fun checkLimitWarnings(
    summary: ShortContentDailySummaryEntity,
    w50: Boolean,
    w80: Boolean,
    w100: Boolean,
    focusLockIntegration: Boolean
  ) {
    val limit = summary.dailyLimit
    if (limit <= 0) return
    val count = summary.totalCount
    val today = summary.date

    val ratio = count.toFloat() / limit.toFloat()

    // 100% Limit reached
    if (ratio >= 1.0f && w100 && warning100TriggeredDate != today) {
      warning100TriggeredDate = today
      showDisciplineNotification(
        notificationId = 1003,
        title = "🚨 दैनिक रील्स सीमा समाप्त! (Daily Limit Reached)",
        message = "आज आपने $count/$limit रील्स/शॉर्ट्स देख लिए हैं। डिजिटल अनुशासन (Focus Lock) सक्रिय करें।",
        isUrgent = true,
        openFocusLock = focusLockIntegration
      )
      return
    }

    // 80% Warning
    if (ratio >= 0.8f && ratio < 1.0f && w80 && warning80TriggeredDate != today) {
      warning80TriggeredDate = today
      val remaining = (limit - count).coerceAtLeast(0)
      showDisciplineNotification(
        notificationId = 1002,
        title = "⚠️ 80% रील्स सीमा समाप्त (Limit Warning)",
        message = "आपने $count/$limit शॉर्ट्स देखे हैं। केवल $remaining शॉर्ट्स शेष हैं। ध्यान केंद्रित रखें!",
        isUrgent = false,
        openFocusLock = false
      )
      return
    }

    // 50% Warning
    if (ratio >= 0.5f && ratio < 0.8f && w50 && warning50TriggeredDate != today) {
      warning50TriggeredDate = today
      showDisciplineNotification(
        notificationId = 1001,
        title = "⏳ 50% शॉर्ट्स सीमा (Halfway Notice)",
        message = "आधा कोटा पूरा: $count/$limit रील्स देखे गए। दिनचर्या के कार्यों पर लौटें।",
        isUrgent = false,
        openFocusLock = false
      )
    }
  }

  private fun showDisciplineNotification(
    notificationId: Int,
    title: String,
    message: String,
    isUrgent: Boolean,
    openFocusLock: Boolean
  ) {
    try {
      val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

      val launchIntent = Intent(appContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (openFocusLock) {
          putExtra("EXTRA_NAVIGATE_TAB", "SHORT_CONTENT_TRACKER")
          putExtra("EXTRA_FOCUS_LOCK_ALERT", true)
        }
      }

      val pendingIntent = PendingIntent.getActivity(
        appContext,
        notificationId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val builder = NotificationCompat.Builder(appContext, CHANNEL_DISCIPLINE_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)

      notificationManager.notify(notificationId, builder.build())
    } catch (_: Exception) {
      // Safe no-op
    }
  }

  private fun createDisciplineNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      val channel = NotificationChannel(
        CHANNEL_DISCIPLINE_ID,
        "Digital Discipline & Limit Warnings",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Alerts and discipline warnings when Short/Reels limit thresholds are reached"
        enableVibration(true)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  companion object {
    const val CHANNEL_DISCIPLINE_ID = "life_tracker_discipline_channel"

    @Volatile
    private var INSTANCE: ShortContentTrackerManager? = null

    fun getInstance(context: Context): ShortContentTrackerManager {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: ShortContentTrackerManager(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
