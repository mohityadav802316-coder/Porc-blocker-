package com.example

import android.app.Application
import android.util.Log
import com.example.alarm.AlarmScheduler
import com.example.blackscreen.BlackScreenManager
import com.example.focus.FocusModeManager
import com.example.util.CrashReporter

/**
 * Custom Application class for Life Tracker.
 * Initializes crash diagnostic hooks, ensures notifications channels are prepared,
 * and maintains crash-safe process lifecycle without blocking app startup.
 */
class LifeTrackerApp : Application() {

  override fun onCreate() {
    super.onCreate()
    Log.i("LifeTrackerApp", "LifeTrackerApp starting...")

    // 1. Initialize Global Uncaught Exception Handler
    CrashReporter.init(this)

    // 2. Initialize essential notification channels early (lightweight)
    try {
      AlarmScheduler.createNotificationChannels(this)
      FocusModeManager.createNotificationChannel(this)
      BlackScreenManager.createNotificationChannel(this)
    } catch (e: Exception) {
      Log.e("LifeTrackerApp", "Error creating notification channels", e)
    }
  }
}
