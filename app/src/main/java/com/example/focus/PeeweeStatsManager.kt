package com.example.focus

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayStats(
  val blockAttempts: Int,
  val strictLockActivations: Int,
  val blockedAttempts: Int
)

data class DayStatBreakdown(
  val date: String,      // YYYY-MM-DD
  val dayLabel: String,  // Mon, Tue, etc.
  val blockedCount: Int,
  val strictLockCount: Int
)

data class WeeklyStats(
  val totalBlockedAttempts: Int,
  val strictLockActivations: Int,
  val dailyBreakdown: List<DayStatBreakdown>
)

data class MonthlyStats(
  val totalBlockedAttempts: Int,
  val strictLockActivations: Int,
  val weeklyTrend: List<Int>, // 4-5 weekly totals in the month
  val monthName: String
)

/**
 * PeeweeStatsManager
 *
 * 100% on-device, private, persistent storage of blocking and Strict Lock statistics.
 * Never stores or uploads search queries, browsing history, or personal data.
 * Uses atomic SharedPreferences with reliable local calendar dates (YYYY-MM-DD).
 * Deduplicates events within 2-second windows to prevent inflated counts.
 */
object PeeweeStatsManager {
  private const val PREFS_NAME = "peewee_block_stats"
  private const val KEY_VERSION = "stats_version"

  // Deduplication state
  private var lastEventElapsedRealtime: Long = 0L
  private var lastEventTrigger: String = ""

  // Reactive flow for UI observation
  private val _statsUpdateTrigger = MutableStateFlow(System.currentTimeMillis())
  val statsUpdateTrigger: StateFlow<Long> = _statsUpdateTrigger.asStateFlow()

  private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
  private val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
  private val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

  fun getTodayDateString(): String = dateFormat.format(Date())
  fun getCurrentMonthString(): String = monthFormat.format(Date())

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  /**
   * Records a detected attempt and whether it was successfully blocked.
   */
  @Synchronized
  fun recordAttempt(context: Context, successfullyBlocked: Boolean, trigger: String = "") {
    val nowElapsed = SystemClock.elapsedRealtime()
    // Deduplicate identical triggers within 2 seconds
    if (nowElapsed - lastEventElapsedRealtime < 2000L && trigger.isNotEmpty() && trigger == lastEventTrigger) {
      return
    }
    lastEventElapsedRealtime = nowElapsed
    lastEventTrigger = trigger

    val today = getTodayDateString()
    val prefs = getPrefs(context)
    val editor = prefs.edit()

    val currentAttempts = prefs.getInt("attempts_$today", 0)
    editor.putInt("attempts_$today", currentAttempts + 1)

    if (successfullyBlocked) {
      val currentBlocked = prefs.getInt("blocked_$today", 0)
      editor.putInt("blocked_$today", currentBlocked + 1)
    }

    editor.apply()
    _statsUpdateTrigger.value = System.currentTimeMillis()
  }

  /**
   * Records a Strict Lock activation event.
   */
  @Synchronized
  fun recordStrictLockActivation(context: Context) {
    val today = getTodayDateString()
    val prefs = getPrefs(context)
    val editor = prefs.edit()

    val currentActivations = prefs.getInt("strict_lock_$today", 0)
    editor.putInt("strict_lock_$today", currentActivations + 1)

    editor.apply()
    _statsUpdateTrigger.value = System.currentTimeMillis()
  }

  /**
   * Returns today's statistics: Block Attempts, Strict Lock Activations, Blocked Attempts.
   */
  fun getTodayStats(context: Context): DayStats {
    val today = getTodayDateString()
    val prefs = getPrefs(context)
    return DayStats(
      blockAttempts = prefs.getInt("attempts_$today", 0),
      strictLockActivations = prefs.getInt("strict_lock_$today", 0),
      blockedAttempts = prefs.getInt("blocked_$today", 0)
    )
  }

  /**
   * Returns weekly statistics for the past 7 days (today and preceding 6 days).
   */
  fun getWeeklyStats(context: Context): WeeklyStats {
    val prefs = getPrefs(context)
    val calendar = Calendar.getInstance()
    var totalBlocked = 0
    var totalStrict = 0
    val breakdown = mutableListOf<DayStatBreakdown>()

    // Past 7 days in chronological order
    for (i in 6 downTo 0) {
      val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -i)
      }
      val dateStr = dateFormat.format(cal.time)
      val dayLabel = dayLabelFormat.format(cal.time)

      val blocked = prefs.getInt("blocked_$dateStr", 0)
      val strict = prefs.getInt("strict_lock_$dateStr", 0)

      totalBlocked += blocked
      totalStrict += strict

      breakdown.add(
        DayStatBreakdown(
          date = dateStr,
          dayLabel = dayLabel,
          blockedCount = blocked,
          strictLockCount = strict
        )
      )
    }

    return WeeklyStats(
      totalBlockedAttempts = totalBlocked,
      strictLockActivations = totalStrict,
      dailyBreakdown = breakdown
    )
  }

  /**
   * Returns monthly statistics for the current month.
   */
  fun getMonthlyStats(context: Context): MonthlyStats {
    val prefs = getPrefs(context)
    val calendar = Calendar.getInstance()
    val currentMonth = getCurrentMonthString()
    val monthName = monthNameFormat.format(calendar.time)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

    var totalBlocked = 0
    var totalStrict = 0

    // Compute weekly trend buckets (up to 5 weeks)
    val weekBuckets = IntArray(5)

    for (day in 1..daysInMonth) {
      val dayCal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, day)
      }
      val dateStr = dateFormat.format(dayCal.time)
      val blocked = prefs.getInt("blocked_$dateStr", 0)
      val strict = prefs.getInt("strict_lock_$dateStr", 0)

      totalBlocked += blocked
      totalStrict += strict

      val weekIndex = ((day - 1) / 7).coerceIn(0, 4)
      weekBuckets[weekIndex] += blocked
    }

    return MonthlyStats(
      totalBlockedAttempts = totalBlocked,
      strictLockActivations = totalStrict,
      weeklyTrend = weekBuckets.toList(),
      monthName = monthName
    )
  }

  /**
   * Safely resets all stored statistics.
   */
  @Synchronized
  fun resetStatistics(context: Context) {
    val prefs = getPrefs(context)
    prefs.edit().clear().apply()
    _statsUpdateTrigger.value = System.currentTimeMillis()
  }
}
