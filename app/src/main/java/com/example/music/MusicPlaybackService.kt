package com.example.music

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity

class MusicPlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null

  @androidx.annotation.OptIn(UnstableApi::class)
  override fun onCreate() {
    super.onCreate()

    val manager = MusicPlayerManager.getInstance(applicationContext)
    val player = manager.exoPlayer

    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_OPEN_MUSIC, true)
    }

    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val sessionActivity = PendingIntent.getActivity(this, 1001, openIntent, pendingIntentFlags)

    val callback = object : MediaSession.Callback {
      override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
      ): MediaSession.ConnectionResult {
        val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
        val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon().build()
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
          .setAvailableSessionCommands(sessionCommands)
          .setAvailablePlayerCommands(playerCommands)
          .build()
      }

      override fun onMediaButtonEvent(
        session: MediaSession,
        controllerInfo: MediaSession.ControllerInfo,
        intent: Intent
      ): Boolean {
        // Handle headset and bluetooth media buttons
        return super.onMediaButtonEvent(session, controllerInfo, intent)
      }
    }

    mediaSession = MediaSession.Builder(this, player)
      .setSessionActivity(sessionActivity)
      .setCallback(callback)
      .build()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action
    val manager = MusicPlayerManager.getInstance(applicationContext)

    when (action) {
      ACTION_TOGGLE_PLAY -> manager.togglePlayPause()
      ACTION_NEXT -> manager.next()
      ACTION_PREV -> manager.previous()
      ACTION_STOP -> {
        manager.pause()
        stopSelf()
        return START_NOT_STICKY
      }
    }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    mediaSession?.run {
      // Do not release the shared player here so it survives transient service restarts,
      // only release the mediaSession
      release()
      mediaSession = null
    }
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.example.music.ACTION_START"
    const val ACTION_TOGGLE_PLAY = "com.example.music.ACTION_TOGGLE_PLAY"
    const val ACTION_NEXT = "com.example.music.ACTION_NEXT"
    const val ACTION_PREV = "com.example.music.ACTION_PREV"
    const val ACTION_STOP = "com.example.music.ACTION_STOP"
    const val EXTRA_OPEN_MUSIC = "extra_open_music"
  }
}
