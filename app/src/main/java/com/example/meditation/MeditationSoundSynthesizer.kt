package com.example.meditation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure acoustic synthesis of Tibetan singing bowls, crystal chimes, and meditative bells
 * using native Android AudioTrack. Zero external asset dependencies, 100% reliable offline sound.
 */
class MeditationSoundSynthesizer {

  private val scope = CoroutineScope(Dispatchers.Default)
  private val sampleRate = 44100

  @Volatile
  private var currentTrack: AudioTrack? = null
  private var activeJob: kotlinx.coroutines.Job? = null

  fun stop() {
    activeJob?.cancel()
    activeJob = null
    currentTrack?.let {
      try {
        it.pause()
        it.flush()
        it.stop()
        it.release()
      } catch (_: Exception) {}
    }
    currentTrack = null
  }

  fun playTibetanBowl(durationSec: Float = 3.5f) {
    activeJob?.cancel()
    activeJob = scope.launch {
      val numSamples = (durationSec * sampleRate).toInt()
      val samples = ShortArray(numSamples)
      val freqFundamental = 432.0 // Natural healing resonance
      val freqHarmonic1 = 864.0
      val freqHarmonic2 = 1296.0

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        // Exponential decay envelope
        val envelope = exp(-t * 1.3)
        // Gentle 4.5Hz vibrato/tremolo simulating singing bowl rim vibration
        val tremolo = 1.0 + 0.15 * sin(2 * PI * 4.5 * t)

        val s = (
          0.60 * sin(2 * PI * freqFundamental * t) +
          0.25 * sin(2 * PI * freqHarmonic1 * t) +
          0.15 * sin(2 * PI * freqHarmonic2 * t)
        ) * envelope * tremolo

        samples[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.75).toInt().toShort()
      }
      playPcmBuffer(samples)
    }
  }

  fun playInhaleChime() {
    scope.launch {
      // 528 Hz Solfeggio frequency with gentle crystal bell attack and decay
      val durationSec = 1.6f
      val numSamples = (durationSec * sampleRate).toInt()
      val samples = ShortArray(numSamples)
      val freq = 528.0

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val attack = (t / 0.05).coerceAtMost(1.0)
        val decay = exp(-t * 2.2)
        val s = (sin(2 * PI * freq * t) + 0.3 * sin(2 * PI * freq * 2 * t)) * attack * decay
        samples[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.65).toInt().toShort()
      }
      playPcmBuffer(samples)
    }
  }

  fun playHoldChime() {
    scope.launch {
      // 396 Hz Solfeggio liberating tone
      val durationSec = 1.4f
      val numSamples = (durationSec * sampleRate).toInt()
      val samples = ShortArray(numSamples)
      val freq = 396.0

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val attack = (t / 0.04).coerceAtMost(1.0)
        val decay = exp(-t * 2.5)
        val s = (sin(2 * PI * freq * t) + 0.25 * sin(2 * PI * freq * 2 * t)) * attack * decay
        samples[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.55).toInt().toShort()
      }
      playPcmBuffer(samples)
    }
  }

  fun playExhaleChime() {
    scope.launch {
      // 285 Hz grounding root tone
      val durationSec = 1.8f
      val numSamples = (durationSec * sampleRate).toInt()
      val samples = ShortArray(numSamples)
      val freq = 285.0

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val attack = (t / 0.06).coerceAtMost(1.0)
        val decay = exp(-t * 2.0)
        val s = (sin(2 * PI * freq * t) + 0.2 * sin(2 * PI * freq * 2 * t)) * attack * decay
        samples[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.60).toInt().toShort()
      }
      playPcmBuffer(samples)
    }
  }

  fun playRelaxChime() {
    scope.launch {
      // Soothing dual harmonic (432 Hz & 216 Hz)
      val durationSec = 1.5f
      val numSamples = (durationSec * sampleRate).toInt()
      val samples = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val attack = (t / 0.08).coerceAtMost(1.0)
        val decay = exp(-t * 1.8)
        val s = (0.7 * sin(2 * PI * 432.0 * t) + 0.3 * sin(2 * PI * 216.0 * t)) * attack * decay
        samples[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.50).toInt().toShort()
      }
      playPcmBuffer(samples)
    }
  }

  fun playEndingBells() {
    scope.launch {
      playTibetanBowl(4.0f)
    }
  }

  private fun playPcmBuffer(samples: ShortArray) {
    try {
      val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )
      val bufferSize = maxOf(samples.size * 2, minBufferSize)

      val audioTrack = AudioTrack.Builder()
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

      currentTrack = audioTrack
      audioTrack.write(samples, 0, samples.size)
      audioTrack.play()

      // Track will be automatically released when audio completes
      val durationMs = ((samples.size.toDouble() / sampleRate) * 1000).toLong() + 300L
      Thread.sleep(durationMs)
      try {
        audioTrack.stop()
        audioTrack.release()
      } catch (_: Exception) {}
      if (currentTrack == audioTrack) currentTrack = null
    } catch (_: Exception) {}
  }
}
