package com.example.focus

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Manages preferences and lifecycle for the On-Device AI Screen Detection engine.
 */
object AiScreenDetectionManager {

  private const val TAG = "AiScreenDetectionManager"
  private const val PREFS_NAME = "ai_screen_detection_prefs"

  private const val KEY_ENABLED = "ai_detection_enabled"
  private const val KEY_THRESHOLD = "ai_detection_threshold"
  private const val KEY_TOTAL_AI_INTERCEPTIONS = "total_ai_interceptions"

  fun isEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ENABLED, false)
  }

  fun setEnabled(context: Context, enabled: Boolean, pinVerified: Boolean = false): Boolean {
    if (!enabled && !pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context) && !ProtectionPauseManager.isPaused(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot disable AI Screen Detection: PIN verification or 24-hr unlock required")
        return false
      }
    }
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    if (!enabled) {
      stopService(context)
    }
    return true
  }

  fun getThreshold(context: Context): Float {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getFloat(KEY_THRESHOLD, TfLiteNsfwClassifier.DEFAULT_THRESHOLD)
  }

  fun setThreshold(context: Context, threshold: Float) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putFloat(KEY_THRESHOLD, threshold.coerceIn(0.60f, 0.95f)).apply()
  }

  fun getInterceptionCount(context: Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_TOTAL_AI_INTERCEPTIONS, 0)
  }

  fun incrementInterceptionCount(context: Context) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = prefs.getInt(KEY_TOTAL_AI_INTERCEPTIONS, 0)
    prefs.edit().putInt(KEY_TOTAL_AI_INTERCEPTIONS, current + 1).apply()
  }

  fun startService(context: Context, resultCode: Int, data: Intent) {
    if (resultCode != Activity.RESULT_OK) {
      Log.w(TAG, "Cannot start AI Screen Detection: MediaProjection result not OK")
      return
    }

    val intent = Intent(context, AiScreenDetectionService::class.java).apply {
      action = AiScreenDetectionService.ACTION_START
      putExtra(AiScreenDetectionService.EXTRA_RESULT_CODE, resultCode)
      putExtra(AiScreenDetectionService.EXTRA_DATA, data)
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
      setEnabled(context, true)
      Log.i(TAG, "Started AiScreenDetectionService successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start AiScreenDetectionService", e)
    }
  }

  fun stopService(context: Context) {
    val intent = Intent(context, AiScreenDetectionService::class.java).apply {
      action = AiScreenDetectionService.ACTION_STOP
    }
    try {
      context.startService(intent)
    } catch (_: Exception) {}
  }
}
