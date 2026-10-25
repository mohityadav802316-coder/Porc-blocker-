package com.example.focus

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android WorkManager Worker running every 15 minutes as dual-resilience backup to AlarmManager.
 * Ensures health status is evaluated even if the device was in deep sleep or Alarm was delayed.
 */
class ProtectionHeartbeatWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
    try {
      Log.i("ProtectionWorker", "Running 15-minute WorkManager Protection Health & Watchdog Check...")
      ProtectionHealthManager.performWatchdogCheck(applicationContext)
      Result.success()
    } catch (e: Exception) {
      Log.e("ProtectionWorker", "Error executing ProtectionHeartbeatWorker", e)
      Result.retry()
    }
  }
}
