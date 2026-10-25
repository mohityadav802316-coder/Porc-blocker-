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
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service for unbreakable Strict Lock persistence.
 *
 * Runs with an ongoing, non-dismissible notification showing live countdown.
 * Resilient against task dismissal (onTaskRemoved), OS memory pressure (START_STICKY),
 * and restarts automatically if terminated while lock duration has not elapsed.
 */
class StrictLockService : Service() {

  companion object {
    private const val TAG = "StrictLockService"
    const val CHANNEL_ID = "life_tracker_strict_lock_channel"
    const val NOTIFICATION_ID = 9005
    const val ACTION_START = "com.example.focus.ACTION_START_STRICT_LOCK"
    const val ACTION_STOP = "com.example.focus.ACTION_STOP_STRICT_LOCK"

    fun start(context: Context) {
      val intent = Intent(context, StrictLockService::class.java).apply {
        action = ACTION_START
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(context, intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to start StrictLockService", e)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, StrictLockService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.stopService(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to stop StrictLockService", e)
      }
    }
  }

  private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
  private var tickerJob: Job? = null

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      if (FocusModeManager.isStrictLockActive(this)) {
        Log.w(TAG, "Stop requested but Strict Lock is still active! Ignoring stop command.")
        return START_STICKY
      }
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = buildNotification(FocusModeManager.getStrictLockRemainingMillis(this))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    StrictLockFloatingPillManager.show(this)
    startTicker()

    return START_STICKY
  }

  private fun startTicker() {
    tickerJob?.cancel()
    tickerJob = serviceScope.launch {
      var lastNotifiedMinute = -1L
      while (isActive) {
        val remainingMillis = FocusModeManager.getStrictLockRemainingMillis(this@StrictLockService)

        if (remainingMillis <= 0L) {
          Log.i(TAG, "Strict Lock 15-minute timer expired! Deactivating cleanly and unlocking.")
          FocusModeManager.deactivateFocusMode(this@StrictLockService, force = true)
          FocusBlockingOverlayManager.dismiss()
          StrictLockFloatingPillManager.dismiss()
          stopForeground(STOP_FOREGROUND_REMOVE)
          stopSelf()
          break
        }

        // Update floating pill countdown every 1 second
        StrictLockFloatingPillManager.updateTime(this@StrictLockService, remainingMillis)

        // Requirement 4: Update notification countdown once per minute (NOT every 2 seconds)
        val currentMinute = (remainingMillis + 999L) / 60_000L
        if (currentMinute != lastNotifiedMinute) {
          lastNotifiedMinute = currentMinute
          val notification = buildNotification(remainingMillis)
          val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
          nm?.notify(NOTIFICATION_ID, notification)
        }

        delay(1000L)
      }
    }
  }

  private fun buildNotification(remainingMillis: Long): Notification {
    val totalSecs = (remainingMillis / 1000L).coerceAtLeast(0)
    val mins = totalSecs / 60
    val timeFormatted = "$mins मिनट"

    val openIntent = Intent(this, MainActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val contentPending = PendingIntent.getActivity(this, 9005, openIntent, pendingFlags)

    return NotificationCompat.Builder(this, AppNotificationChannels.CHANNEL_STRICT_LOCK_TIMER)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🔒 सख्त अनुशासन लॉक सक्रिय")
      .setContentText("15 मिनट का अनिवार्य लॉक • शेष समय: $timeFormatted")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("अनुपयुक्त सामग्री पहचान के कारण फोन 15 मिनट के लिए सख्त लॉक पर है।\nकेवल Phone/Dialer व अनुमत ऐप्स उपलब्ध हैं।\nशेष समय: $timeFormatted")
      )
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setAutoCancel(false)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setContentIntent(contentPending)
      .build()
  }

  private fun createNotificationChannel() {
    AppNotificationChannels.registerAllChannels(this)
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    super.onTaskRemoved(rootIntent)
    // If user swipes app from recents, restart immediately if strict lock is active!
    if (FocusModeManager.isStrictLockActive(this)) {
      Log.w(TAG, "Task removed while Strict Lock active. Restarting service immediately!")
      val restartIntent = Intent(applicationContext, StrictLockService::class.java).apply {
        action = ACTION_START
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(applicationContext, restartIntent)
        } else {
          startService(restartIntent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to restart StrictLockService onTaskRemoved", e)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
    if (FocusModeManager.isStrictLockActive(this)) {
      Log.w(TAG, "StrictLockService destroyed while lock active! Relaunching...")
      val restartIntent = Intent(applicationContext, StrictLockService::class.java).apply {
        action = ACTION_START
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          ContextCompat.startForegroundService(applicationContext, restartIntent)
        } else {
          startService(restartIntent)
        }
      } catch (_: Exception) {}
    }
  }
}
