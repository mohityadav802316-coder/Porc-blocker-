package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Lightweight Persistent Foreground Service for Life Tracker Protection & Watchdog.
 * Runs with START_STICKY and handles onTaskRemoved to guarantee long-term reliability.
 */
class ProtectionWatchdogService : Service() {

  companion object {
    private const val TAG = "ProtectionWatchdogSvc"
    private const val NOTIFICATION_ID = 9122
    private const val CHANNEL_ID = "life_tracker_watchdog_channel"

    @Volatile
    private var isRunning = false

    fun isServiceRunning(): Boolean = isRunning

    fun start(context: Context) {
      val intent = Intent(context, ProtectionWatchdogService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, ProtectionWatchdogService::class.java)
      context.stopService(intent)
    }
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var tickerJob: Job? = null

  override fun onCreate() {
    super.onCreate()
    isRunning = true
    createNotificationChannel()
    ProtectionHealthManager.logHealthEvent(
      context = this,
      eventType = "SERVICE_STARTED",
      message = "Protection Watchdog Service चालू हुई (START_STICKY)।",
      severity = "INFO"
    )
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = buildNotification()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      try {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
      } catch (_: Exception) {
        startForeground(NOTIFICATION_ID, notification)
      }
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    startPeriodicTicker()

    // START_STICKY ensures Android OS recreates this service if killed by memory pressure
    return START_STICKY
  }

  private fun startPeriodicTicker() {
    tickerJob?.cancel()
    tickerJob = serviceScope.launch {
      while (isActive) {
        delay(60_000L) // Check every 60 seconds (once per minute)
        try {
          val report = PeeweeProtectionManager.evaluateProtectionState(this@ProtectionWatchdogService)
          val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
          nm?.notify(NOTIFICATION_ID, buildNotification())

          if (ProtectionPauseManager.isPaused(this@ProtectionWatchdogService)) {
            // During pause, update pause countdown notification
            val remaining = ProtectionPauseManager.getPauseRemainingMillis(this@ProtectionWatchdogService)
            ProtectionPauseManager.postPauseCountdownNotification(this@ProtectionWatchdogService, remaining)
          } else {
            // Check if re-enable reminders are due after pause expiration
            ProtectionPauseManager.checkAndSendReenableReminder(this@ProtectionWatchdogService)
            ProtectionHealthManager.performWatchdogCheck(this@ProtectionWatchdogService)
          }

          // If master is enabled but protection is degraded, post alert
          if (report.isMasterEnabled && (report.status == ProtectionStatus.PROTECTION_LIMITED || report.status == ProtectionStatus.PERMISSION_REQUIRED)) {
            PeeweeProtectionManager.notifyProtectionDegraded(
              context = this@ProtectionWatchdogService,
              title = "Peewee Block: Protection Degraded",
              message = report.actionRequired ?: "One or more protection engines have stopped."
            )
          }
        } catch (e: Exception) {
          Log.w(TAG, "Ticker error: ${e.message}")
        }
      }
    }
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    Log.w(TAG, "App swiped away from Recents (onTaskRemoved). Scheduling watchdog recovery.")
    ProtectionHealthManager.onTaskRemoved(this)
    // Schedule alarm to bring back watchdog immediately
    ProtectionHealthManager.scheduleNextWatchdogExactAlarm(this)
  }

  override fun onDestroy() {
    super.onDestroy()
    isRunning = false
    serviceScope.cancel()
    ProtectionHealthManager.logHealthEvent(
      context = this,
      eventType = "SERVICE_STOPPED",
      message = "Protection Watchdog Service stopped.",
      severity = "WARN"
    )
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun buildNotification(): Notification {
    val report = PeeweeProtectionManager.evaluateProtectionState(this)
    val intent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      putExtra("OPEN_PROTECTION_HEALTH", true)
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(this, 9123, intent, pendingFlags)

    val (title, content) = when (report.status) {
      ProtectionStatus.PROTECTION_ACTIVE -> "🛡️ Peewee Block: Active" to "On-device website & content protection running."
      ProtectionStatus.PROTECTION_LIMITED -> "⚠️ Peewee Block: Limited" to (report.actionRequired ?: "Some protection engines need attention.")
      ProtectionStatus.PERMISSION_REQUIRED -> "🔒 Peewee Block: Action Needed" to (report.actionRequired ?: "Required permissions need authorization.")
      ProtectionStatus.PROTECTION_PAUSED -> "⏸️ Peewee Block: Paused" to "Protection paused (${report.pauseRemainingMinutes}m left)."
      ProtectionStatus.PROTECTION_DISABLED -> "Peewee Block: Disabled" to "Master protection is turned off."
    }

    return NotificationCompat.Builder(this, AppNotificationChannels.CHANNEL_PROTECTION_RUNNING)
      .setSmallIcon(R.drawable.ic_launcher_foreground)
      .setContentTitle(title)
      .setContentText(content)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setContentIntent(pendingIntent)
      .build()
  }

  private fun createNotificationChannel() {
    AppNotificationChannels.registerAllChannels(this)
  }
}
