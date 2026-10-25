package com.example.music

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

data class EqualizerBand(
  val bandIndex: Short,
  val centerFreqHz: Int,
  val formattedFreq: String,
  val levelMb: Short,
  val minLevelMb: Short = -1500,
  val maxLevelMb: Short = 1500
)

data class EqualizerState(
  val isEnabled: Boolean = false,
  val bands: List<EqualizerBand> = emptyList(),
  val bassBoost: Int = 0, // 0 - 1000
  val virtualizer: Int = 0, // 0 - 1000
  val loudnessGain: Int = 0, // 0 - 1000 mB (0 to 10 dB)
  val currentPreset: String = "Flat",
  val availablePresets: List<String> = listOf("Flat", "Bass Heavy", "Vocal", "Rock", "Pop", "Classical"),
  val customPresets: Map<String, List<Int>> = emptyMap(),
  val volumeNormalization: Boolean = false
)

class EqualizerManager private constructor(private val appContext: Context) {

  private val prefs: SharedPreferences =
    appContext.getSharedPreferences("music_equalizer_settings", Context.MODE_PRIVATE)

  private val _state = MutableStateFlow(EqualizerState())
  val state: StateFlow<EqualizerState> = _state.asStateFlow()

  private var equalizer: Equalizer? = null
  private var bassBoost: BassBoost? = null
  private var virtualizer: Virtualizer? = null
  private var loudnessEnhancer: LoudnessEnhancer? = null

  private var currentSessionId: Int = 0

  init {
    loadSavedSettings()
  }

  /**
   * Attaches audio effects to ExoPlayer's audio session ID so all equalizer
   * curves, bass boost, and virtualizer directly process the music output.
   */
  @Synchronized
  fun attachAudioSession(audioSessionId: Int) {
    if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
    currentSessionId = audioSessionId

    releaseEffects()

    try {
      // 1. Equalizer
      equalizer = Equalizer(0, audioSessionId).apply {
        enabled = _state.value.isEnabled
      }

      // Query bands from hardware
      val numBands = equalizer?.numberOfBands?.toInt() ?: 0
      val range = equalizer?.bandLevelRange ?: shortArrayOf(-1500, 1500)
      val minMb = range[0]
      val maxMb = range[1]

      val savedBands = loadSavedBandLevels(numBands)
      val bandList = mutableListOf<EqualizerBand>()

      for (i in 0 until numBands) {
        val bandShort = i.toShort()
        val centerHz = (equalizer?.getCenterFreq(bandShort) ?: 1000000) / 1000
        val formatted = formatFrequency(centerHz)
        val level = savedBands.getOrElse(i) { 0 }.toShort().coerceIn(minMb, maxMb)

        try {
          equalizer?.setBandLevel(bandShort, level)
        } catch (_: Exception) {}

        bandList.add(
          EqualizerBand(
            bandIndex = bandShort,
            centerFreqHz = centerHz,
            formattedFreq = formatted,
            levelMb = level,
            minLevelMb = minMb,
            maxLevelMb = maxMb
          )
        )
      }

      // 2. Bass Boost
      bassBoost = BassBoost(0, audioSessionId).apply {
        enabled = _state.value.isEnabled
        try {
          if (strengthSupported) {
            setStrength(_state.value.bassBoost.toShort())
          }
        } catch (_: Exception) {}
      }

      // 3. Virtualizer
      virtualizer = Virtualizer(0, audioSessionId).apply {
        enabled = _state.value.isEnabled
        try {
          if (strengthSupported) {
            setStrength(_state.value.virtualizer.toShort())
          }
        } catch (_: Exception) {}
      }

      // 4. Loudness Enhancer
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        try {
          loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
            enabled = _state.value.isEnabled
            setTargetGain(if (_state.value.volumeNormalization) 400 else _state.value.loudnessGain)
          }
        } catch (_: Exception) {}
      }

      _state.update { it.copy(bands = bandList) }
    } catch (_: Exception) {}
  }

  fun setEnabled(enabled: Boolean) {
    _state.update { it.copy(isEnabled = enabled) }
    try {
      equalizer?.enabled = enabled
      bassBoost?.enabled = enabled
      virtualizer?.enabled = enabled
      loudnessEnhancer?.enabled = enabled
    } catch (_: Exception) {}

    prefs.edit().putBoolean(KEY_EQ_ENABLED, enabled).apply()
  }

  fun setBandLevel(bandIndex: Short, levelMb: Short) {
    try {
      equalizer?.setBandLevel(bandIndex, levelMb)
    } catch (_: Exception) {}

    _state.update { s ->
      val updatedBands = s.bands.map { band ->
        if (band.bandIndex == bandIndex) band.copy(levelMb = levelMb) else band
      }
      s.copy(bands = updatedBands, currentPreset = "Custom")
    }

    saveCurrentBandLevels()
  }

  fun setBassBoost(strength: Int) {
    val clamped = strength.coerceIn(0, 1000)
    _state.update { it.copy(bassBoost = clamped) }
    try {
      bassBoost?.setStrength(clamped.toShort())
    } catch (_: Exception) {}
    prefs.edit().putInt(KEY_BASS_BOOST, clamped).apply()
  }

  fun setVirtualizer(strength: Int) {
    val clamped = strength.coerceIn(0, 1000)
    _state.update { it.copy(virtualizer = clamped) }
    try {
      virtualizer?.setStrength(clamped.toShort())
    } catch (_: Exception) {}
    prefs.edit().putInt(KEY_VIRTUALIZER, clamped).apply()
  }

  fun setLoudnessGain(gainmB: Int) {
    val clamped = gainmB.coerceIn(0, 1000)
    _state.update { it.copy(loudnessGain = clamped) }
    try {
      loudnessEnhancer?.setTargetGain(clamped)
    } catch (_: Exception) {}
    prefs.edit().putInt(KEY_LOUDNESS_GAIN, clamped).apply()
  }

  fun setVolumeNormalization(enabled: Boolean) {
    _state.update { it.copy(volumeNormalization = enabled) }
    try {
      if (enabled) {
        loudnessEnhancer?.enabled = true
        loudnessEnhancer?.setTargetGain(350)
      } else {
        loudnessEnhancer?.enabled = _state.value.isEnabled
        loudnessEnhancer?.setTargetGain(_state.value.loudnessGain)
      }
    } catch (_: Exception) {}
    prefs.edit().putBoolean(KEY_VOL_NORM, enabled).apply()
  }

  fun applyPreset(presetName: String) {
    val bands = _state.value.bands
    if (bands.isEmpty()) return

    val numBands = bands.size
    val targetLevels = when (presetName) {
      "Flat" -> List(numBands) { 0 }
      "Bass Heavy" -> calculatePresetCurve(numBands, listOf(800, 650, 400, 150, 0, -100, 0, 150, 300, 400))
      "Vocal" -> calculatePresetCurve(numBands, listOf(-200, -100, 200, 500, 600, 500, 300, 100, 0, -100))
      "Rock" -> calculatePresetCurve(numBands, listOf(500, 400, 200, 0, -200, -150, 100, 300, 500, 600))
      "Pop" -> calculatePresetCurve(numBands, listOf(200, 400, 500, 400, 200, 0, 100, 300, 400, 300))
      "Classical" -> calculatePresetCurve(numBands, listOf(400, 300, 200, 100, -100, -100, 0, 200, 300, 400))
      else -> {
        _state.value.customPresets[presetName] ?: List(numBands) { 0 }
      }
    }

    val updatedBands = bands.mapIndexed { index, band ->
      val newLevel = targetLevels.getOrElse(index) { 0 }.toShort().coerceIn(band.minLevelMb, band.maxLevelMb)
      try {
        equalizer?.setBandLevel(band.bandIndex, newLevel)
      } catch (_: Exception) {}
      band.copy(levelMb = newLevel)
    }

    _state.update {
      it.copy(
        bands = updatedBands,
        currentPreset = presetName
      )
    }

    saveCurrentBandLevels()
    prefs.edit().putString(KEY_PRESET_NAME, presetName).apply()
  }

  fun saveCustomPreset(name: String) {
    val currentLevels = _state.value.bands.map { it.levelMb.toInt() }
    val updated = _state.value.customPresets.toMutableMap()
    updated[name] = currentLevels

    _state.update {
      it.copy(
        customPresets = updated,
        currentPreset = name,
        availablePresets = it.availablePresets.filter { p -> p != name } + name
      )
    }

    // Persist custom presets
    val json = JSONObject()
    updated.forEach { (k, v) -> json.put(k, org.json.JSONArray(v)) }
    prefs.edit()
      .putString(KEY_CUSTOM_PRESETS, json.toString())
      .putString(KEY_PRESET_NAME, name)
      .apply()
    saveCurrentBandLevels()
  }

  fun deleteCustomPreset(name: String) {
    val updated = _state.value.customPresets.toMutableMap()
    updated.remove(name)

    _state.update {
      it.copy(
        customPresets = updated,
        currentPreset = if (it.currentPreset == name) "Flat" else it.currentPreset,
        availablePresets = it.availablePresets.filter { p -> p != name }
      )
    }

    val json = JSONObject()
    updated.forEach { (k, v) -> json.put(k, org.json.JSONArray(v)) }
    prefs.edit().putString(KEY_CUSTOM_PRESETS, json.toString()).apply()

    if (_state.value.currentPreset == "Flat") {
      applyPreset("Flat")
    }
  }

  private fun calculatePresetCurve(actualBands: Int, ideal10Bands: List<Int>): List<Int> {
    if (actualBands == ideal10Bands.size) return ideal10Bands
    if (actualBands <= 0) return emptyList()

    // Resample/interpolate 10-band curve to actual number of device bands (e.g. 5 bands)
    return List(actualBands) { i ->
      val factor = (i.toFloat() / (actualBands - 1).coerceAtLeast(1)) * (ideal10Bands.size - 1)
      val lower = factor.toInt().coerceIn(0, ideal10Bands.size - 1)
      val upper = (lower + 1).coerceIn(0, ideal10Bands.size - 1)
      val fraction = factor - lower
      ((ideal10Bands[lower] * (1f - fraction)) + (ideal10Bands[upper] * fraction)).toInt()
    }
  }

  private fun formatFrequency(hz: Int): String {
    return when {
      hz >= 1000 -> {
        val kHz = hz / 1000.0
        if (kHz == kHz.toInt().toDouble()) "${kHz.toInt()} kHz" else String.format(java.util.Locale.US, "%.1f kHz", kHz)
      }
      else -> "$hz Hz"
    }
  }

  private fun saveCurrentBandLevels() {
    val levels = _state.value.bands.joinToString(",") { it.levelMb.toString() }
    prefs.edit().putString(KEY_BAND_LEVELS, levels).apply()
  }

  private fun loadSavedBandLevels(bandCount: Int): List<Int> {
    val raw = prefs.getString(KEY_BAND_LEVELS, null) ?: return emptyList()
    return raw.split(",").mapNotNull { it.toIntOrNull() }.take(bandCount)
  }

  private fun loadSavedSettings() {
    val enabled = prefs.getBoolean(KEY_EQ_ENABLED, false)
    val bass = prefs.getInt(KEY_BASS_BOOST, 0)
    val virt = prefs.getInt(KEY_VIRTUALIZER, 0)
    val loud = prefs.getInt(KEY_LOUDNESS_GAIN, 0)
    val norm = prefs.getBoolean(KEY_VOL_NORM, false)
    val preset = prefs.getString(KEY_PRESET_NAME, "Flat") ?: "Flat"

    // Load custom presets
    val customMap = mutableMapOf<String, List<Int>>()
    val customJson = prefs.getString(KEY_CUSTOM_PRESETS, null)
    if (!customJson.isNullOrBlank()) {
      try {
        val jsonObj = JSONObject(customJson)
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
          val k = keys.next()
          val arr = jsonObj.getJSONArray(k)
          val list = mutableListOf<Int>()
          for (j in 0 until arr.length()) list.add(arr.getInt(j))
          customMap[k] = list
        }
      } catch (_: Exception) {}
    }

    val available = listOf("Flat", "Bass Heavy", "Vocal", "Rock", "Pop", "Classical") + customMap.keys

    _state.value = EqualizerState(
      isEnabled = enabled,
      bassBoost = bass,
      virtualizer = virt,
      loudnessGain = loud,
      volumeNormalization = norm,
      currentPreset = preset,
      availablePresets = available.distinct(),
      customPresets = customMap
    )
  }

  private fun releaseEffects() {
    try { equalizer?.release() } catch (_: Exception) {}
    try { bassBoost?.release() } catch (_: Exception) {}
    try { virtualizer?.release() } catch (_: Exception) {}
    try { loudnessEnhancer?.release() } catch (_: Exception) {}
    equalizer = null
    bassBoost = null
    virtualizer = null
    loudnessEnhancer = null
  }

  companion object {
    private const val KEY_EQ_ENABLED = "eq_enabled"
    private const val KEY_BASS_BOOST = "eq_bass_boost"
    private const val KEY_VIRTUALIZER = "eq_virtualizer"
    private const val KEY_LOUDNESS_GAIN = "eq_loudness_gain"
    private const val KEY_VOL_NORM = "eq_volume_norm"
    private const val KEY_PRESET_NAME = "eq_preset_name"
    private const val KEY_BAND_LEVELS = "eq_band_levels"
    private const val KEY_CUSTOM_PRESETS = "eq_custom_presets"

    @Volatile
    private var instance: EqualizerManager? = null

    fun getInstance(context: Context): EqualizerManager {
      return instance ?: synchronized(this) {
        instance ?: EqualizerManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
