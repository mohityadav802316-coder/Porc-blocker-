package com.example.music

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
  val isActive: Boolean = false,
  val remainingSeconds: Int = 0,
  val stopAtEndOfTrack: Boolean = false,
  val selectedMinutes: Int = 0
) {
  val formattedRemaining: String
    get() {
      if (!isActive) return ""
      if (stopAtEndOfTrack) return "गाने के अंत में"
      val mins = remainingSeconds / 60
      val secs = remainingSeconds % 60
      return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

class SleepTimerManager private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var timerJob: Job? = null

  private val _state = MutableStateFlow(SleepTimerState())
  val state: StateFlow<SleepTimerState> = _state.asStateFlow()

  var onFadeVolume: ((Float) -> Unit)? = null
  var onTimerExpired: (() -> Unit)? = null

  fun startTimer(minutes: Int) {
    if (minutes <= 0) {
      cancel()
      return
    }

    timerJob?.cancel()
    val totalSeconds = minutes * 60

    _state.update {
      SleepTimerState(
        isActive = true,
        remainingSeconds = totalSeconds,
        stopAtEndOfTrack = false,
        selectedMinutes = minutes
      )
    }

    // Reset volume to full in case it was fading
    onFadeVolume?.invoke(1.0f)

    timerJob = scope.launch {
      var remaining = totalSeconds
      while (remaining > 0 && isActive) {
        delay(1000L)
        remaining--
        _state.update { it.copy(remainingSeconds = remaining) }

        // In the final 15 seconds, fade out volume smoothly
        if (remaining <= 15) {
          val volume = (remaining / 15f).coerceIn(0f, 1f)
          onFadeVolume?.invoke(volume)
        }
      }

      // Time expired: pause playback
      onFadeVolume?.invoke(0f)
      onTimerExpired?.invoke()

      // Reset timer & restore volume
      delay(200L)
      onFadeVolume?.invoke(1.0f)
      _state.update { SleepTimerState() }
    }
  }

  fun startStopAtEndOfTrack() {
    timerJob?.cancel()
    _state.update {
      SleepTimerState(
        isActive = true,
        remainingSeconds = 0,
        stopAtEndOfTrack = true,
        selectedMinutes = 0
      )
    }
  }

  fun onTrackCompleted() {
    if (_state.value.isActive && _state.value.stopAtEndOfTrack) {
      onTimerExpired?.invoke()
      _state.update { SleepTimerState() }
    }
  }

  fun cancel() {
    timerJob?.cancel()
    timerJob = null
    onFadeVolume?.invoke(1.0f)
    _state.update { SleepTimerState() }
  }

  companion object {
    @Volatile
    private var instance: SleepTimerManager? = null

    fun getInstance(context: Context): SleepTimerManager {
      return instance ?: synchronized(this) {
        instance ?: SleepTimerManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
