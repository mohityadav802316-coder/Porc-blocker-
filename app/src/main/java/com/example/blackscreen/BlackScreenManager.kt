package com.example.blackscreen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.db.LifeTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central controller for System-Wide OLED Black Screen Mode and Floating Control.
 */
object BlackScreenManager {

  private const val TAG = "BlackScreenManager"

  const val CHANNEL_ID = "life_tracker_black_screen_channel"
  const val NOTIFICATION_ID = 9527

  const val ACTION_ACTIVATE_BLACK_OVERLAY = "com.example.blackscreen.ACTION_ACTIVATE_BLACK_OVERLAY"
  const val ACTION_DEACTIVATE_BLACK_OVERLAY = "com.example.blackscreen.ACTION_DEACTIVATE_BLACK_OVERLAY"
  const val ACTION_SHOW_FLOATING_DOT = "com.example.blackscreen.ACTION_SHOW_FLOATING_DOT"
  const val ACTION_HIDE_FLOATING_DOT = "com.example.blackscreen.ACTION_HIDE_FLOATING_DOT"
  const val ACTION_UPDATE_CONFIG = "com.example.blackscreen.ACTION_UPDATE_CONFIG"
  const val ACTION_STOP_SERVICE = "com.example.blackscreen.ACTION_STOP_SERVICE"

  private val _isOverlayActive = MutableStateFlow(false)
  val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

  private val _isFloatingDotShowing = MutableStateFlow(false)
  val isFloatingDotShowing: StateFlow<Boolean> = _isFloatingDotShowing.asStateFlow()

  private val _isTemporarilySuppressed = MutableStateFlow(false)
  val isTemporarilySuppressed: StateFlow<Boolean> = _isTemporarilySuppressed.asStateFlow()

  fun setOverlayActive(active: Boolean) {
    _isOverlayActive.value = active
  }

  fun setFloatingDotShowing(showing: Boolean) {
    _isFloatingDotShowing.value = showing
  }

  fun setTemporarilySuppressed(suppressed: Boolean) {
    _isTemporarilySuppressed.value = suppressed
  }

  /**
   * Checks if Android OS allows drawing overlays over other apps.
   */
  fun canDrawOverlays(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Settings.canDrawOverlays(context)
    } else {
      true
    }
  }

  /**
   * Generates Intent to open the official Android "Display over other apps" settings screen.
   */
  fun getManageOverlayPermissionIntent(context: Context): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
      ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    } else {
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
  }

  /**
   * Ensures notification channel is created for Android 8.0+.
   */
  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Black Screen Mode & Floating Control",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Shows active OLED Black Screen Mode status and quick exit controls"
        setShowBadge(false)
        enableVibration(false)
        enableLights(false)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * Activates Black Screen Overlay immediately.
   */
  fun activateBlackOverlay(context: Context) {
    if (!canDrawOverlays(context)) {
      Log.w(TAG, "Overlay permission not granted; falling back to in-app overlay or prompt")
      _isOverlayActive.value = true
      return
    }

    createNotificationChannel(context)
    val intent = Intent(context, BlackScreenService::class.java).apply {
      action = ACTION_ACTIVATE_BLACK_OVERLAY
    }
    try {
      ContextCompat.startForegroundService(context, intent)
      _isOverlayActive.value = true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start BlackScreenService", e)
    }

    // Persist active state in Room DB
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val current = db.userSettingsDao().getSettingsSync()
        if (current != null) {
          db.userSettingsDao().insertOrUpdate(
            current.copy(isBlackScreenOverlayActive = true, isBlackScreenEnabled = true)
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist overlay active state", e)
      }
    }
  }

  /**
   * Deactivates Black Screen Overlay.
   */
  fun deactivateBlackOverlay(context: Context) {
    val intent = Intent(context, BlackScreenService::class.java).apply {
      action = ACTION_DEACTIVATE_BLACK_OVERLAY
    }
    try {
      context.startService(intent)
      _isOverlayActive.value = false
    } catch (e: Exception) {
      Log.e(TAG, "Failed to deactivate overlay service", e)
    }

    // Persist inactive state in Room DB
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val current = db.userSettingsDao().getSettingsSync()
        if (current != null) {
          db.userSettingsDao().insertOrUpdate(
            current.copy(isBlackScreenOverlayActive = false)
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist overlay inactive state", e)
      }
    }
  }

  /**
   * Starts or updates the Floating Control Dot service according to settings.
   */
  fun syncServiceState(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync() ?: return@launch

        if (!canDrawOverlays(context)) {
          Log.i(TAG, "Overlay permission not granted; cannot display floating dot or system overlay")
          return@launch
        }

        if (settings.isBlackScreenEnabled) {
          createNotificationChannel(context)
          val intent = Intent(context, BlackScreenService::class.java).apply {
            action = if (settings.isBlackScreenOverlayActive) {
              ACTION_ACTIVATE_BLACK_OVERLAY
            } else if (settings.isFloatingDotEnabled) {
              ACTION_SHOW_FLOATING_DOT
            } else {
              ACTION_UPDATE_CONFIG
            }
          }
          ContextCompat.startForegroundService(context, intent)
        } else {
          val intent = Intent(context, BlackScreenService::class.java).apply {
            action = ACTION_STOP_SERVICE
          }
          context.stopService(intent)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to sync BlackScreenService state", e)
      }
    }
  }

  /**
   * Handles device reboot recovery.
   */
  fun onBootCompleted(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync() ?: return@launch
        if (settings.isBlackScreenEnabled && settings.blackScreenRestoreAfterReboot && canDrawOverlays(context)) {
          syncServiceState(context)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to restore black screen after boot", e)
      }
    }
  }

  /**
   * Handles screen unlock / user present event.
   */
  fun onUserPresent(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync() ?: return@launch
        if (settings.isBlackScreenEnabled &&
          settings.isBlackScreenOverlayActive &&
          settings.blackScreenRestoreAfterUnlock &&
          canDrawOverlays(context)
        ) {
          activateBlackOverlay(context)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to restore black screen after user present", e)
      }
    }
  }

  /**
   * Persists dragged floating dot coordinates to Room DB.
   */
  fun saveDotPosition(context: Context, x: Int, y: Int) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync() ?: return@launch
        db.userSettingsDao().insertOrUpdate(
          settings.copy(blackScreenDotX = x, blackScreenDotY = y)
        )
      } catch (e: Exception) {
        Log.e(TAG, "Failed to save dot position", e)
      }
    }
  }
}
