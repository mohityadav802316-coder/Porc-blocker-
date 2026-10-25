package com.example.meditation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Dedicated Breath Audio Engine for Meditation / Pranayama sessions.
 *
 * Guarantees:
 * 1. Inhale phase: Plays soft, calming breath-in sound matched to phase duration.
 * 2. Exhale phase: Plays gentle, relaxing breath-out sound matched to phase duration.
 * 3. Hold & Rest phases: Complete absolute silence (no sound, zero overlap).
 * 4. Procedural acoustic synthesis of air flow if no raw file is provided.
 * 5. Supports optional raw files: res/raw/inhale and res/raw/exhale.
 * 6. Supports independent custom audio files via Storage Access Framework with persistable URIs.
 * 7. Graceful fallback to default sound if custom file is missing or unreadable.
 * 8. Immediate release of MediaPlayer / AudioTrack on pause, stop, or exit.
 */
class BreathAudioPlayer(private val context: Context) {

  companion object {
    private const val TAG = "BreathAudioPlayer"

    private const val PREFS_NAME = "meditation_breath_prefs"
    private const val KEY_BREATH_SOUNDS_ENABLED = "breath_sounds_enabled"
    private const val KEY_INHALE_TYPE = "inhale_type"
    private const val KEY_INHALE_URI = "inhale_uri"
    private const val KEY_INHALE_NAME = "inhale_name"
    private const val KEY_EXHALE_TYPE = "exhale_type"
    private const val KEY_EXHALE_URI = "exhale_uri"
    private const val KEY_EXHALE_NAME = "exhale_name"

    const val TYPE_DEFAULT = "DEFAULT"
    const val TYPE_CUSTOM = "CUSTOM"

    fun isBreathSoundsEnabled(context: Context): Boolean {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getBoolean(KEY_BREATH_SOUNDS_ENABLED, true)
    }

    fun setBreathSoundsEnabled(context: Context, enabled: Boolean) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().putBoolean(KEY_BREATH_SOUNDS_ENABLED, enabled).apply()
    }

    fun getInhaleSoundType(context: Context): String {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_INHALE_TYPE, TYPE_DEFAULT) ?: TYPE_DEFAULT
    }

    fun getInhaleCustomUri(context: Context): String? {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_INHALE_URI, null)
    }

    fun getInhaleCustomName(context: Context): String? {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_INHALE_NAME, null)
    }

    fun setInhaleCustomSound(context: Context, uri: String, name: String) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putString(KEY_INHALE_TYPE, TYPE_CUSTOM)
        .putString(KEY_INHALE_URI, uri)
        .putString(KEY_INHALE_NAME, name)
        .apply()
    }

    fun resetInhaleToDefault(context: Context) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putString(KEY_INHALE_TYPE, TYPE_DEFAULT)
        .remove(KEY_INHALE_URI)
        .remove(KEY_INHALE_NAME)
        .apply()
    }

    fun getExhaleSoundType(context: Context): String {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_EXHALE_TYPE, TYPE_DEFAULT) ?: TYPE_DEFAULT
    }

    fun getExhaleCustomUri(context: Context): String? {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_EXHALE_URI, null)
    }

    fun getExhaleCustomName(context: Context): String? {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      return prefs.getString(KEY_EXHALE_NAME, null)
    }

    fun setExhaleCustomSound(context: Context, uri: String, name: String) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putString(KEY_EXHALE_TYPE, TYPE_CUSTOM)
        .putString(KEY_EXHALE_URI, uri)
        .putString(KEY_EXHALE_NAME, name)
        .apply()
    }

    fun resetExhaleToDefault(context: Context) {
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putString(KEY_EXHALE_TYPE, TYPE_DEFAULT)
        .remove(KEY_EXHALE_URI)
        .remove(KEY_EXHALE_NAME)
        .apply()
    }
  }

  private val coroutineScope = CoroutineScope(Dispatchers.Default)
  private val mainHandler = Handler(Looper.getMainLooper())

  private var activeMediaPlayer: MediaPlayer? = null
  private var activeAudioTrack: AudioTrack? = null
  private var synthesisJob: Job? = null

  var onFallbackMessageListener: ((String) -> Unit)? = null

  /**
   * Plays soft inhale sound matched to the duration in seconds.
   */
  fun playInhale(durationSec: Int) {
    if (!isBreathSoundsEnabled(context)) {
      stop()
      return
    }

    val type = getInhaleSoundType(context)
    val customUri = getInhaleCustomUri(context)

    if (type == TYPE_CUSTOM && !customUri.isNullOrBlank()) {
      playCustomUri(customUri, durationSec, isBreathInhale = true)
    } else {
      playDefaultSound(isInhale = true, durationSec = durationSec)
    }
  }

  /**
   * Plays soft exhale sound matched to the duration in seconds.
   */
  fun playExhale(durationSec: Int) {
    if (!isBreathSoundsEnabled(context)) {
      stop()
      return
    }

    val type = getExhaleSoundType(context)
    val customUri = getExhaleCustomUri(context)

    if (type == TYPE_CUSTOM && !customUri.isNullOrBlank()) {
      playCustomUri(customUri, durationSec, isBreathInhale = false)
    } else {
      playDefaultSound(isInhale = false, durationSec = durationSec)
    }
  }

  /**
   * Plays default sound: checks res/raw/inhale or res/raw/exhale first,
   * falling back to procedural acoustic synthesis.
   */
  private fun playDefaultSound(isInhale: Boolean, durationSec: Int) {
    stop()

    // 1. Check if user provided audio files in res/raw/
    val rawResName = if (isInhale) "inhale" else "exhale"
    val rawId = context.resources.getIdentifier(rawResName, "raw", context.packageName)

    if (rawId != 0) {
      playRawResource(rawId, durationSec)
      return
    }

    // 2. Procedural soft breath synthesis
    synthesizeBreathSound(isInhale, durationSec)
  }

  private fun playRawResource(rawResId: Int, durationSec: Int) {
    try {
      val mp = MediaPlayer.create(context, rawResId).apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
      }
      activeMediaPlayer = mp
      mp.start()

      // Fade out before phase ends so it NEVER bleeds into hold phase
      val fadeOutStartMs = ((durationSec * 1000L) - 350L).coerceAtLeast(100L)
      mainHandler.postDelayed({
        fadeAndStopMediaPlayer(mp)
      }, fadeOutStartMs)

      mp.setOnCompletionListener {
        it.release()
        if (activeMediaPlayer == it) activeMediaPlayer = null
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error playing raw breath sound", e)
      synthesizeBreathSound(isInhale = true, durationSec = durationSec)
    }
  }

  private fun playCustomUri(uriString: String, durationSec: Int, isBreathInhale: Boolean) {
    stop()
    try {
      val uri = Uri.parse(uriString)
      val mp = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        setDataSource(context, uri)
        prepare()
      }
      activeMediaPlayer = mp
      mp.start()

      // Handle custom file longer than phase duration: fade out smoothly
      val fadeOutStartMs = ((durationSec * 1000L) - 400L).coerceAtLeast(100L)
      mainHandler.postDelayed({
        fadeAndStopMediaPlayer(mp)
      }, fadeOutStartMs)

      mp.setOnCompletionListener {
        it.release()
        if (activeMediaPlayer == it) activeMediaPlayer = null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Custom breath audio unavailable or unreadable: ${e.message}")
      mainHandler.post {
        onFallbackMessageListener?.invoke("कस्टम श्वास ऑडियो अनुपलब्ध है; डिफ़ॉल्ट श्वास ध्वनि का उपयोग किया जा रहा है")
      }
      // Fallback to default procedural sound without crashing
      playDefaultSound(isInhale = isBreathInhale, durationSec = durationSec)
    }
  }

  private fun fadeAndStopMediaPlayer(mp: MediaPlayer) {
    var volume = 1.0f
    val step = 0.2f
    val fadeRunnable = object : Runnable {
      override fun run() {
        if (activeMediaPlayer != mp) return
        volume -= step
        if (volume > 0.05f) {
          try {
            mp.setVolume(volume, volume)
            mainHandler.postDelayed(this, 50L)
          } catch (_: Exception) {}
        } else {
          try {
            mp.stop()
            mp.release()
          } catch (_: Exception) {}
          if (activeMediaPlayer == mp) activeMediaPlayer = null
        }
      }
    }
    mainHandler.post(fadeRunnable)
  }

  /**
   * Procedural filtered pink/white noise synthesis simulating natural, soft nasal/chest breathing.
   * Inhale: Soft ascending crescendo envelope (700Hz–1100Hz gentle low-pass air rush).
   * Exhale: Relaxing descending decrescendo envelope (500Hz–800Hz smooth warm breath release).
   */
  private fun synthesizeBreathSound(isInhale: Boolean, durationSec: Int) {
    synthesisJob?.cancel()
    synthesisJob = coroutineScope.launch {
      val sampleRate = 22050 // Optimized sample rate for soft airflow
      val safeDurationSec = durationSec.coerceIn(2, 12).toFloat()
      val numSamples = (safeDurationSec * sampleRate).toInt()
      val pcmBuffer = ShortArray(numSamples)

      var lastFilterOutput = 0.0
      val filterAlpha = if (isInhale) 0.12 else 0.08 // Gentle low pass filter

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val progress = (t / safeDurationSec).coerceIn(0.0, 1.0)

        // Raw white noise
        val rawNoise = (Math.random() * 2.0 - 1.0)

        // Single-pole low-pass filtering for organic, soft whisper-like air sound
        lastFilterOutput += filterAlpha * (rawNoise - lastFilterOutput)

        // Smooth volume envelope
        val envelope = if (isInhale) {
          // Gentle rising crescendo with soft attack and smooth round peak
          val attack = (progress / 0.15).coerceAtMost(1.0)
          val fadeOutAtEnd = if (progress > 0.85) (1.0 - progress) / 0.15 else 1.0
          (progress.pow(1.3)) * attack * fadeOutAtEnd
        } else {
          // Exhale: starts warm and full, gradually decrescendos to absolute silence
          val attack = (progress / 0.10).coerceAtMost(1.0)
          val decay = (1.0 - progress).pow(1.2)
          decay * attack
        }

        // Sub-bass warmth modulation (38Hz subtle chest resonance)
        val chestResonance = 1.0 + 0.15 * sin(2 * PI * 38.0 * t)
        val finalSample = lastFilterOutput * envelope * chestResonance * 0.40

        pcmBuffer[i] = (finalSample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
      }

      try {
        val minBufferSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(pcmBuffer.size * 2, minBufferSize)

        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_MEDIA)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        activeAudioTrack = track
        track.write(pcmBuffer, 0, pcmBuffer.size)
        track.play()

        val playTimeMs = (safeDurationSec * 1000L).toLong()
        Thread.sleep(playTimeMs)

        try {
          track.stop()
          track.release()
        } catch (_: Exception) {}
        if (activeAudioTrack == track) activeAudioTrack = null
      } catch (e: Exception) {
        Log.e(TAG, "AudioTrack synthesis error", e)
      }
    }
  }

  /**
   * Previews the inhale sound for 3.5 seconds.
   */
  fun previewInhale() {
    playInhale(4)
  }

  /**
   * Previews the exhale sound for 4 seconds.
   */
  fun previewExhale() {
    playExhale(4)
  }

  /**
   * Immediately silences and releases all breath audio resources.
   * Call on hold phase, pause, stop, or leaving screen.
   */
  fun stop() {
    synthesisJob?.cancel()
    synthesisJob = null

    activeMediaPlayer?.let {
      try {
        if (it.isPlaying) {
          it.stop()
        }
        it.release()
      } catch (_: Exception) {}
    }
    activeMediaPlayer = null

    activeAudioTrack?.let {
      try {
        it.stop()
        it.release()
      } catch (_: Exception) {}
    }
    activeAudioTrack = null
  }
}
