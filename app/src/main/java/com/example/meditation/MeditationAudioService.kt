package com.example.meditation

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
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.TimeUtils

class MeditationAudioService : Service() {

  private var wakeLock: PowerManager.WakeLock? = null
  private val channelId = "meditation_service_channel"
  private val notificationId = 9988

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    acquireWakeLock()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: ACTION_START
    val manager = MeditationManager.getInstance(applicationContext)

    when (action) {
      ACTION_START -> {
        try {
          val notification = buildNotification(manager.state.value)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
              notificationId,
              notification,
              ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
          } else {
            startForeground(notificationId, notification)
          }
        } catch (e: Exception) {
          android.util.Log.e("MeditationAudioService", "startForeground failed", e)
        }
      }
      ACTION_PAUSE -> {
        manager.pauseSession()
        updateNotification(manager.state.value)
      }
      ACTION_RESUME -> {
        manager.resumeSession()
        updateNotification(manager.state.value)
      }
      ACTION_NOTIFICATION_END -> {
        manager.stopSession(savePartial = true)
        stopForeground(true)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_STOP -> {
        stopForeground(true)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_UPDATE -> {
        updateNotification(manager.state.value)
      }
    }

    return START_STICKY
  }

  private fun updateNotification(state: MeditationState) {
    if (!state.isActive) {
      stopForeground(true)
      stopSelf()
      return
    }
    val notification = buildNotification(state)
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.notify(notificationId, notification)
  }

  private fun buildNotification(state: MeditationState): Notification {
    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    // Intent to open MainActivity on Meditation Screen
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_OPEN_MEDITATION, true)
    }
    val contentPendingIntent = PendingIntent.getActivity(this, 101, openIntent, pendingIntentFlags)

    // Action: Pause or Resume
    val toggleActionIntent = Intent(this, MeditationAudioService::class.java).apply {
      action = if (state.isPlaying) ACTION_PAUSE else ACTION_RESUME
    }
    val togglePendingIntent = PendingIntent.getService(this, 102, toggleActionIntent, pendingIntentFlags)
    val toggleActionTitle = if (state.isPlaying) "Pause" else "Resume"
    val toggleActionIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

    // Action: End Session
    val stopActionIntent = Intent(this, MeditationAudioService::class.java).apply {
      action = ACTION_NOTIFICATION_END
    }
    val stopPendingIntent = PendingIntent.getService(this, 103, stopActionIntent, pendingIntentFlags)

    val remainingFormatted = TimeUtils.formatSecondsToMmSs(state.remainingSeconds)
    val phaseText = "${state.currentPhase.enName} (${state.phaseRemainingSeconds}s)"
    val title = "🧘 ${state.type.englishTitle} Meditation • $remainingFormatted"
    val content = if (state.isCompleted) {
      "Meditation Complete ✨"
    } else {
      "$phaseText • ${state.stageInstruction}"
    }

    return NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.drawable.ic_meditation_notif)
      .setContentTitle(title)
      .setContentText(content)
      .setContentIntent(contentPendingIntent)
      .setOngoing(state.isActive && !state.isCompleted)
      .setOnlyAlertOnce(true)
      .setProgress(state.targetDurationSeconds, state.elapsedSeconds, false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(toggleActionIcon, toggleActionTitle, togglePendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", stopPendingIntent)
      .build()
  }

  private fun acquireWakeLock() {
    try {
      if (wakeLock == null) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "LifeTracker:MeditationWakeLock"
        )?.apply {
          setReferenceCounted(false)
          acquire(60 * 60 * 1000L) // Max 1 hour safety timeout
        }
      }
    } catch (_: Exception) {}
  }

  private fun releaseWakeLock() {
    try {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
      wakeLock = null
    } catch (_: Exception) {}
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Meditation Audio Guidance",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Displays ongoing meditation timer and background audio playback status"
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    releaseWakeLock()
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.example.meditation.ACTION_START"
    const val ACTION_PAUSE = "com.example.meditation.ACTION_PAUSE"
    const val ACTION_RESUME = "com.example.meditation.ACTION_RESUME"
    const val ACTION_STOP = "com.example.meditation.ACTION_STOP"
    const val ACTION_NOTIFICATION_END = "com.example.meditation.ACTION_NOTIFICATION_END"
    const val ACTION_UPDATE = "com.example.meditation.ACTION_UPDATE"
    const val EXTRA_OPEN_MEDITATION = "extra_open_meditation"
  }
}
