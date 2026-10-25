package com.example.meditation

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.BreathPhase
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.MeditationType
import com.example.util.TimeUtils
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

data class MeditationState(
  val isActive: Boolean = false,
  val isPlaying: Boolean = false,
  val type: MeditationType = MeditationType.BREATHING,
  val targetDurationSeconds: Int = 300,
  val elapsedSeconds: Int = 0,
  val currentPhase: BreathPhase = BreathPhase.INHALE,
  val phaseRemainingSeconds: Int = 4,
  val phaseProgressFraction: Float = 0f,
  val isConcluding: Boolean = false,
  val isCompleted: Boolean = false,
  val stageInstruction: String = "Inhale deeply through your nose",
  val chimeEnabled: Boolean = true,
  val voiceLanguage: String = "HI",
  val userName: String = "मोहित"
) {
  val remainingSeconds: Int
    get() = (targetDurationSeconds - elapsedSeconds).coerceAtLeast(0)

  val totalProgressFraction: Float
    get() = if (targetDurationSeconds > 0) (elapsedSeconds.toFloat() / targetDurationSeconds).coerceIn(0f, 1f) else 0f
}

class MeditationManager private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var tickerJob: Job? = null

  private val soundSynthesizer = MeditationSoundSynthesizer()
  val breathAudioPlayer = BreathAudioPlayer(appContext)

  private val _state = MutableStateFlow(MeditationState())
  val state: StateFlow<MeditationState> = _state.asStateFlow()

  fun updateSettings(userName: String, chimeEnabled: Boolean, language: String) {
    _state.update {
      it.copy(
        userName = userName,
        chimeEnabled = chimeEnabled,
        voiceLanguage = language
      )
    }
  }

  fun startSession(
    type: MeditationType,
    durationMinutes: Int,
    userName: String = "मोहित",
    chimeEnabled: Boolean = true,
    voiceLanguage: String = "HI"
  ) {
    tickerJob?.cancel()
    tickerJob = null

    val targetSeconds = durationMinutes * 60
    _state.value = MeditationState(
      isActive = true,
      isPlaying = true,
      type = type,
      targetDurationSeconds = targetSeconds,
      elapsedSeconds = 0,
      currentPhase = BreathPhase.INHALE,
      phaseRemainingSeconds = type.inhaleSec,
      phaseProgressFraction = 0f,
      isConcluding = false,
      isCompleted = false,
      stageInstruction = getInstructionForPhase(type, BreathPhase.INHALE, voiceLanguage),
      chimeEnabled = chimeEnabled,
      voiceLanguage = voiceLanguage,
      userName = userName
    )

    // Start Foreground Service so audio continues with screen locked/off
    startForegroundService()

    // Play gentle starting Tibetan bowl if enabled, and trigger initial inhale breath sound
    if (chimeEnabled) {
      soundSynthesizer.playTibetanBowl(3.0f)
    }
    breathAudioPlayer.playInhale(type.inhaleSec)

    startTicker()
  }

  fun pauseSession() {
    _state.update { it.copy(isPlaying = false) }
    tickerJob?.cancel()
    breathAudioPlayer.stop()
    soundSynthesizer.stop()
    updateForegroundNotification()
  }

  fun resumeSession() {
    if (!_state.value.isActive || _state.value.isCompleted) return
    _state.update { it.copy(isPlaying = true) }
    // Resume breath sound for current phase
    when (_state.value.currentPhase) {
      BreathPhase.INHALE -> breathAudioPlayer.playInhale(_state.value.phaseRemainingSeconds)
      BreathPhase.EXHALE -> breathAudioPlayer.playExhale(_state.value.phaseRemainingSeconds)
      else -> {
        breathAudioPlayer.stop()
        soundSynthesizer.stop()
      }
    }
    startTicker()
    updateForegroundNotification()
  }

  fun stopSession(savePartial: Boolean = true) {
    val currentState = _state.value
    tickerJob?.cancel()
    tickerJob = null
    breathAudioPlayer.stop()
    soundSynthesizer.stop()

    if (savePartial && currentState.isActive && !currentState.isCompleted && currentState.elapsedSeconds >= 30) {
      // Save partial session
      scope.launch(Dispatchers.IO) {
        try {
          val db = LifeTrackerDatabase.getDatabase(appContext)
          val session = MeditationSessionEntity(
            date = TimeUtils.getTodayIsoDate(),
            type = currentState.type.name,
            durationMinutes = currentState.targetDurationSeconds / 60,
            completedSeconds = currentState.elapsedSeconds,
            isCompleted = false
          )
          db.meditationDao().insertSession(session)
        } catch (_: Exception) {}
      }
    }

    _state.update {
      it.copy(
        isActive = false,
        isPlaying = false,
        isConcluding = false,
        isCompleted = false
      )
    }

    stopForegroundService()
  }

  fun dismissCompletion() {
    breathAudioPlayer.stop()
    soundSynthesizer.stop()
    _state.update {
      it.copy(
        isActive = false,
        isPlaying = false,
        isConcluding = false,
        isCompleted = false
      )
    }
    stopForegroundService()
  }

  private fun startTicker() {
    tickerJob?.cancel()
    tickerJob = scope.launch {
      var currentPhase = _state.value.currentPhase
      val phaseTotal = getPhaseDuration(_state.value.type, currentPhase)
      var phaseElapsed = (phaseTotal - _state.value.phaseRemainingSeconds).coerceIn(0, phaseTotal)

      while (isActive) {
        delay(1000L)
        if (!_state.value.isPlaying) continue

        val currentState = _state.value
        val newElapsed = currentState.elapsedSeconds + 1
        val remaining = (currentState.targetDurationSeconds - newElapsed).coerceAtLeast(0)

        // Check if reaching personalized ending (last 6 seconds)
        if (remaining <= 6 && !currentState.isConcluding) {
          _state.update { it.copy(isConcluding = true) }
          if (currentState.chimeEnabled) {
            soundSynthesizer.playEndingBells()
          }
        }

        if (newElapsed >= currentState.targetDurationSeconds) {
          // Time completed -> Trigger personalized voice ending
          handleSessionCompletion()
          break
        }

        // Advance breathing cycle
        phaseElapsed++
        val type = currentState.type
        val phaseDuration = getPhaseDuration(type, currentPhase)

        if (phaseElapsed >= phaseDuration) {
          // Transition to next phase
          currentPhase = getNextPhase(type, currentPhase)
          phaseElapsed = 0

          // Breath sounds only:
          // Inhale -> play soft breath-in sound matched to phase duration
          // Hold -> COMPLETE SILENCE (no sound, zero overlap)
          // Exhale -> play soft breath-out sound matched to phase duration
          // Rest -> COMPLETE SILENCE
          when (currentPhase) {
            BreathPhase.INHALE -> breathAudioPlayer.playInhale(type.inhaleSec)
            BreathPhase.HOLD -> breathAudioPlayer.stop()
            BreathPhase.EXHALE -> breathAudioPlayer.playExhale(type.exhaleSec)
            BreathPhase.REST -> breathAudioPlayer.stop()
          }
        }

        val remainingInPhase = (getPhaseDuration(type, currentPhase) - phaseElapsed).coerceAtLeast(1)
        val phaseProgress = (phaseElapsed.toFloat() / getPhaseDuration(type, currentPhase)).coerceIn(0f, 1f)

        _state.update {
          it.copy(
            elapsedSeconds = newElapsed,
            currentPhase = currentPhase,
            phaseRemainingSeconds = remainingInPhase,
            phaseProgressFraction = phaseProgress,
            stageInstruction = getInstructionForPhase(type, currentPhase, currentState.voiceLanguage)
          )
        }

        updateForegroundNotification()
      }
    }
  }

  private fun handleSessionCompletion() {
    val currentState = _state.value
    tickerJob?.cancel()
    tickerJob = null
    breathAudioPlayer.stop()

    // Smooth silent completion without spoken voice prompts
    _state.update {
      it.copy(
        isPlaying = false,
        isConcluding = false,
        isCompleted = true,
        elapsedSeconds = currentState.targetDurationSeconds,
        stageInstruction = if (currentState.voiceLanguage == "HI") "मेडिटेशन पूर्ण हुआ" else "Meditation Complete"
      )
    }

    scope.launch(Dispatchers.IO) {
      try {
        val db = LifeTrackerDatabase.getDatabase(appContext)
        val session = MeditationSessionEntity(
          date = TimeUtils.getTodayIsoDate(),
          type = currentState.type.name,
          durationMinutes = currentState.targetDurationSeconds / 60,
          completedSeconds = currentState.targetDurationSeconds,
          isCompleted = true
        )
        db.meditationDao().insertSession(session)
      } catch (_: Exception) {}
    }

    updateForegroundNotification()
  }

  private fun getPhaseDuration(type: MeditationType, phase: BreathPhase): Int {
    return when (phase) {
      BreathPhase.INHALE -> type.inhaleSec.coerceAtLeast(1)
      BreathPhase.HOLD -> type.holdSec.coerceAtLeast(1)
      BreathPhase.EXHALE -> type.exhaleSec.coerceAtLeast(1)
      BreathPhase.REST -> type.restSec.coerceAtLeast(1)
    }
  }

  private fun getNextPhase(type: MeditationType, current: BreathPhase): BreathPhase {
    return when (current) {
      BreathPhase.INHALE -> if (type.holdSec > 0) BreathPhase.HOLD else BreathPhase.EXHALE
      BreathPhase.HOLD -> BreathPhase.EXHALE
      BreathPhase.EXHALE -> if (type.restSec > 0) BreathPhase.REST else BreathPhase.INHALE
      BreathPhase.REST -> BreathPhase.INHALE
    }
  }

  private fun getInstructionForPhase(type: MeditationType, phase: BreathPhase, language: String): String {
    val isHi = language == "HI"
    return when (phase) {
      BreathPhase.INHALE -> if (isHi) "श्वास अंदर लें — ऊर्जा और शांति को महसूस करें" else "Inhale deeply — fill your lungs with calm energy"
      BreathPhase.HOLD -> if (isHi) "श्वास रोकें — अपने अंतर्मन में शांति बनाए रखें" else "Hold gently — anchor your focus within"
      BreathPhase.EXHALE -> if (isHi) "श्वास छोड़ें — सारा तनाव और चिंता मुक्त करें" else "Exhale smoothly — release all tension"
      BreathPhase.REST -> if (isHi) "विश्राम करें — अपने शरीर को पूरी तरह शांत रखें" else "Rest and relax — feel the stillness"
    }
  }

  private fun startForegroundService() {
    try {
      val intent = Intent(appContext, MeditationAudioService::class.java).apply {
        action = MeditationAudioService.ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      } else {
        appContext.startService(intent)
      }
    } catch (_: Exception) {}
  }

  private fun updateForegroundNotification() {
    try {
      val intent = Intent(appContext, MeditationAudioService::class.java).apply {
        action = MeditationAudioService.ACTION_UPDATE
      }
      appContext.startService(intent)
    } catch (_: Exception) {}
  }

  private fun stopForegroundService() {
    try {
      val intent = Intent(appContext, MeditationAudioService::class.java).apply {
        action = MeditationAudioService.ACTION_STOP
      }
      appContext.startService(intent)
    } catch (_: Exception) {}
  }

  companion object {
    @Volatile
    private var instance: MeditationManager? = null

    fun getInstance(context: Context): MeditationManager {
      return instance ?: synchronized(this) {
        instance ?: MeditationManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
