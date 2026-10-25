package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import com.example.data.db.LifeTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

object AlarmAudioPlayer {
  private var mediaPlayer: MediaPlayer? = null
  private var vibrator: Vibrator? = null
  @Volatile private var isPlayingState = false
  @Volatile private var shouldBeRinging = false
  private var startJob: Job? = null

  fun isPlaying(): Boolean = isPlayingState

  /**
   * Verifies if an audio file or URI can be parsed and prepared by the device media codec.
   * Returns failure if corrupted, missing, or unsupported codec.
   */
  fun testPlayback(context: Context, uriString: String): Result<Unit> {
    return try {
      val testPlayer = MediaPlayer()
      val customFile = File(uriString)
      if (customFile.exists()) {
        testPlayer.setDataSource(customFile.absolutePath)
      } else {
        val uri = Uri.parse(uriString)
        testPlayer.setDataSource(context.applicationContext, uri)
      }
      testPlayer.prepare()
      testPlayer.release()
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Codec validation failed for $uriString", e)
      Result.failure(e)
    }
  }

  @Synchronized
  fun startWithSavedSettings(context: Context) {
    shouldBeRinging = true
    startJob?.cancel()
    startJob = CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (!shouldBeRinging) {
          Log.d("AlarmAudioPlayer", "Alarm dismissed before settings loaded; aborting start")
          return@launch
        }
        val soundType = settings?.alarmSoundType ?: "DEFAULT"
        val customUri = settings?.customSoundUri
        val volume = settings?.alarmVolume ?: 1.0f
        val vibration = settings?.isVibrationEnabled ?: true
        start(context, soundType, customUri, volume, vibration)
      } catch (e: Exception) {
        Log.e("AlarmAudioPlayer", "Error loading alarm settings for playback", e)
        if (shouldBeRinging) {
          start(context, "DEFAULT", null, 1.0f, true)
        }
      }
    }
  }

  @Synchronized
  fun start(
    context: Context,
    soundType: String = "DEFAULT",
    customUriString: String? = null,
    volume: Float = 1.0f,
    enableVibration: Boolean = true
  ) {
    if (!shouldBeRinging) {
      Log.d("AlarmAudioPlayer", "start() called but shouldBeRinging is false; aborting playback")
      stopInternal()
      return
    }

    // 1. Cleanly stop any existing playback before starting
    stopInternal()
    isPlayingState = true

    // 2. Configure and start audio player with resilient multi-stage fallback
    try {
      var startedSuccessfully = false
      val clampedVol = volume.coerceIn(0.05f, 1.0f)
      val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

      // Stage 1: Try custom sound if specified
      if (soundType.equals("CUSTOM", ignoreCase = true) && !customUriString.isNullOrBlank()) {
        var customPlayer: MediaPlayer? = null
        try {
          customPlayer = MediaPlayer()
          val customFile = File(customUriString)
          if (customFile.exists()) {
            customPlayer.setDataSource(customFile.absolutePath)
          } else {
            val uri = Uri.parse(customUriString)
            customPlayer.setDataSource(context.applicationContext, uri)
          }
          customPlayer.setAudioAttributes(audioAttrs)
          customPlayer.setVolume(clampedVol, clampedVol)
          customPlayer.isLooping = true
          customPlayer.prepare()
          customPlayer.start()
          mediaPlayer = customPlayer
          startedSuccessfully = true
        } catch (e: Exception) {
          Log.w("AlarmAudioPlayer", "Custom sound prepare/playback failed ($customUriString). Falling back to built-in high tone.", e)
          try {
            customPlayer?.release()
          } catch (_: Exception) {}
          mediaPlayer = null
          startedSuccessfully = false
        }
      }

      // Stage 2: Built-in High-Tone Sharp Alarm Sound
      if (!startedSuccessfully) {
        var rawPlayer: MediaPlayer? = null
        try {
          rawPlayer = MediaPlayer()
          val afd = context.resources.openRawResourceFd(R.raw.high_tone_alarm)
          if (afd != null) {
            rawPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            rawPlayer.setAudioAttributes(audioAttrs)
            rawPlayer.setVolume(clampedVol, clampedVol)
            rawPlayer.isLooping = true
            rawPlayer.prepare()
            afd.close()
            rawPlayer.start()
            mediaPlayer = rawPlayer
            startedSuccessfully = true
          }
        } catch (e: Exception) {
          Log.w("AlarmAudioPlayer", "Failed playing high_tone_alarm raw resource, falling back to system ringtone", e)
          try {
            rawPlayer?.release()
          } catch (_: Exception) {}
          mediaPlayer = null
          startedSuccessfully = false
        }
      }

      // Stage 3: System fallback ringtone / alarm
      if (!startedSuccessfully) {
        var sysPlayer: MediaPlayer? = null
        try {
          sysPlayer = MediaPlayer()
          val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
          if (alertUri != null) {
            sysPlayer.setDataSource(context.applicationContext, alertUri)
            sysPlayer.setAudioAttributes(audioAttrs)
            sysPlayer.setVolume(clampedVol, clampedVol)
            sysPlayer.isLooping = true
            sysPlayer.prepare()
            sysPlayer.start()
            mediaPlayer = sysPlayer
            startedSuccessfully = true
          }
        } catch (e: Exception) {
          Log.e("AlarmAudioPlayer", "Fatal fallback error playing system alert tone", e)
          try {
            sysPlayer?.release()
          } catch (_: Exception) {}
          mediaPlayer = null
        }
      }
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error initializing alarm audio player", e)
    }

    // 3. Configure vibration if enabled
    if (enableVibration) {
      try {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vibratorManager?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(pattern, 0)
        }
      } catch (e: Exception) {
        Log.e("AlarmAudioPlayer", "Error starting vibration", e)
      }
    }
  }

  @Synchronized
  fun setVolume(volume: Float) {
    try {
      val clamped = volume.coerceIn(0.05f, 1.0f)
      mediaPlayer?.setVolume(clamped, clamped)
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error updating volume", e)
    }
  }

  @Synchronized
  fun stop() {
    shouldBeRinging = false
    startJob?.cancel()
    startJob = null
    isPlayingState = false
    stopInternal()
  }

  private fun stopInternal() {
    try {
      mediaPlayer?.let {
        try {
          if (it.isPlaying) {
            it.stop()
          }
        } catch (_: Exception) {}
        try {
          it.release()
        } catch (_: Exception) {}
      }
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error releasing MediaPlayer", e)
    } finally {
      mediaPlayer = null
    }

    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error cancelling vibration", e)
    } finally {
      vibrator = null
    }
  }
}
