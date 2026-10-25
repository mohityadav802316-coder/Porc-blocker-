package com.example.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyBackupWorker(
  context: Context,
  params: WorkerParameters
) : CoroutineWorker(context, params) {

  override suspend fun doWork(): Result {
    if (!BackupPreferences.isAutoBackupEnabled(applicationContext)) {
      return Result.success()
    }

    if (!BackupPreferences.isFolderAccessible(applicationContext)) {
      BackupPreferences.recordBackupFailure(
        applicationContext,
        "बैकअप फ़ोल्डर उपलब्ध नहीं है। कृपया सेटिंग्स में जाकर फ़ोल्डर पुनः चुनें।"
      )
      return Result.failure()
    }

    val result = BackupManager.performBackup(applicationContext)
    return if (result.isSuccess) {
      Result.success()
    } else {
      if (runAttemptCount < 3) {
        Result.retry()
      } else {
        Result.failure()
      }
    }
  }

  companion object {
    const val WORK_NAME = "life_tracker_daily_backup_work"

    /**
     * Schedules or reschedules the daily periodic backup work at the user-configured time.
     */
    fun schedulePeriodic(context: Context) {
      val (hour, minute) = BackupPreferences.getBackupTime(context)
      val now = Calendar.getInstance()
      val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (before(now)) {
          add(Calendar.DAY_OF_YEAR, 1)
        }
      }

      val initialDelayMs = (target.timeInMillis - now.timeInMillis).coerceAtLeast(0)

      val constraints = Constraints.Builder()
        .setRequiresBatteryNotLow(true)
        .setRequiresStorageNotLow(true)
        .build()

      val workRequest = PeriodicWorkRequestBuilder<DailyBackupWorker>(1, TimeUnit.DAYS)
        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        workRequest
      )
    }

    fun rescheduleAfterBoot(context: Context) {
      if (BackupPreferences.isAutoBackupEnabled(context)) {
        schedulePeriodic(context)
      }
    }

    fun cancel(context: Context) {
      WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
  }
}
