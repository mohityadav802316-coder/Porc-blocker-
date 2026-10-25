package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.db.LifeTrackerDatabase
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class WakeUpAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    Log.d("WakeUpAlarmReceiver", "Received broadcast action: $action")

    when (action) {
      AlarmScheduler.ACTION_TRIGGER_ALARM -> {
        handleAlarmTrigger(context, intent)
      }
      AlarmScheduler.ACTION_TIMETABLE_REMINDER -> {
        handleTimetableReminder(context, intent)
      }
      AlarmScheduler.ACTION_SMART_PRE_REMINDER -> {
        handleSmartPreReminder(context, intent)
      }
      AlarmScheduler.ACTION_STOP_ALARM -> {
        AlarmScheduler.dismissRingingAlarm(context)
      }
      AlarmScheduler.ACTION_SNOOZE_ALARM -> {
        AlarmScheduler.dismissRingingAlarm(context)
        CoroutineScope(Dispatchers.IO).launch {
          val db = LifeTrackerDatabase.getDatabase(context)
          val settings = db.userSettingsDao().getSettingsSync()
          val snoozeMins = settings?.snoozeMinutes ?: 10
          AlarmScheduler.scheduleSnooze(context, snoozeMins)
        }
      }
      Intent.ACTION_BOOT_COMPLETED -> {
        com.example.focus.ProtectionHealthManager.onBootCompleted(context)
        handleBootCompleted(context)
      }
      Intent.ACTION_MY_PACKAGE_REPLACED -> {
        com.example.focus.ProtectionHealthManager.onPackageReplaced(context)
        handleBootCompleted(context)
      }
      "com.mohit.lifetracker.ACTION_WATCHDOG_HEARTBEAT_ALARM" -> {
        com.example.focus.ProtectionHealthManager.performWatchdogCheck(context)
      }
      com.example.focus.FocusModeManager.ACTION_FOCUS_MODE_ACTIVATE -> {
        handleFocusModeActivate(context)
      }
      com.example.focus.FocusModeManager.ACTION_FOCUS_MODE_DEACTIVATE -> {
        handleFocusModeDeactivate(context)
      }
    }
  }

  private fun handleFocusModeActivate(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync() ?: return@launch
        com.example.focus.FocusModeManager.activateFocusSchedule(
          context,
          settings.focusStartTimeMinutes,
          settings.focusEndTimeMinutes
        )
      } catch (e: Exception) {
        Log.e("WakeUpAlarmReceiver", "Failed to activate focus mode", e)
      }
    }
  }

  private fun handleFocusModeDeactivate(context: Context) {
    com.example.focus.FocusModeManager.deactivateFocusMode(context, force = true)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null && settings.isFocusScheduleEnabled) {
          Log.d("WakeUpAlarmReceiver", "Rescheduling recurring focus mode schedule for tomorrow")
          com.example.focus.FocusModeManager.scheduleFocusAlarms(
            context,
            settings.focusStartTimeMinutes,
            settings.focusEndTimeMinutes
          )
        }
      } catch (e: Exception) {
        Log.e("WakeUpAlarmReceiver", "Failed to reschedule recurring focus schedule", e)
      }
    }
  }

  private fun handleTimetableReminder(context: Context, intent: Intent) {
    AlarmScheduler.createNotificationChannels(context)

    val templateId = intent.getLongExtra(AlarmScheduler.EXTRA_TEMPLATE_ID, 0L)
    val name = intent.getStringExtra(AlarmScheduler.EXTRA_ACTIVITY_NAME) ?: "Daily Activity"
    val timeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_TIME_MINUTES, 0)
    val category = intent.getStringExtra(AlarmScheduler.EXTRA_CATEGORY) ?: "Routine"
    val notes = intent.getStringExtra(AlarmScheduler.EXTRA_NOTES) ?: ""

    val timeFormatted = TimeUtils.minutesTo12Hour(timeMinutes)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val openAppIntent = Intent(context, com.example.MainActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      (AlarmScheduler.NOTIFICATION_ID_TIMETABLE_BASE + (templateId % 50000)).toInt(),
      openAppIntent,
      flags
    )

    val priority = intent.getStringExtra(AlarmScheduler.EXTRA_PRIORITY) ?: "NORMAL"
    val priorityPrefix = when (priority.uppercase()) {
      "HIGH" -> "⚡ "
      "IMPORTANT" -> "⭐ "
      else -> ""
    }

    val notificationTitle = "$priorityPrefix$name • $timeFormatted"
    val notificationText = "$name — आपका निर्धारित समय हो गया है।"

    val builder = NotificationCompat.Builder(context, AlarmScheduler.TIMETABLE_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(notificationTitle)
      .setContentText(notificationText)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setContentIntent(contentPendingIntent)

    if (notes.isNotBlank()) {
      builder.setStyle(
        NotificationCompat.BigTextStyle().bigText("$notificationText\n$notes")
      )
    }

    val activityKey = intent.getStringExtra(AlarmScheduler.EXTRA_ACTIVITY_KEY) ?: ""
    val notificationId = if (activityKey.isNotBlank()) {
      AlarmScheduler.getStableRequestCodeForActivityKey(activityKey, templateId)
    } else {
      (AlarmScheduler.NOTIFICATION_ID_TIMETABLE_BASE + (templateId % 50000)).toInt()
    }
    notificationManager.notify(notificationId, builder.build())
    Log.d("WakeUpAlarmReceiver", "Dispatched timetable notification for $name [key=$activityKey] at $timeFormatted")

    // Daily recurring reschedule for next occurrence
    if (activityKey.isNotBlank() || templateId > 0L) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val db = LifeTrackerDatabase.getDatabase(context)
          val settings = db.userSettingsDao().getSettingsSync()
          val smartReminder = settings?.smartReminderMinutes ?: 0
          val template = if (activityKey.isNotBlank()) {
            db.routineDao().getRoutineTemplateByActivityKey(activityKey)
          } else {
            db.routineDao().getRoutineTemplateById(templateId)
          }
          if (template != null && template.isActive) {
            AlarmScheduler.scheduleTimetableAlarm(context, template, smartReminder)
          }
        } catch (e: Exception) {
          Log.e("WakeUpAlarmReceiver", "Failed to reschedule recurring timetable alarm", e)
        }
      }
    }
  }

  /**
   * Feature 2: Handles smart pre-reminder notification (e.g. 5, 10, 15, 30 mins before)
   */
  private fun handleSmartPreReminder(context: Context, intent: Intent) {
    AlarmScheduler.createNotificationChannels(context)

    val templateId = intent.getLongExtra(AlarmScheduler.EXTRA_TEMPLATE_ID, 0L)
    val name = intent.getStringExtra(AlarmScheduler.EXTRA_ACTIVITY_NAME) ?: "Daily Activity"
    val timeMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_TIME_MINUTES, 0)
    val preMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_PRE_MINUTES, 10)
    val priority = intent.getStringExtra(AlarmScheduler.EXTRA_PRIORITY) ?: "NORMAL"

    val timeFormatted = TimeUtils.minutesTo12Hour(timeMinutes)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val openAppIntent = Intent(context, com.example.MainActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val contentPendingIntent = PendingIntent.getActivity(
      context,
      (AlarmScheduler.NOTIFICATION_ID_PRE_REMINDER_BASE + (templateId % 50000)).toInt(),
      openAppIntent,
      flags
    )

    val priorityPrefix = when (priority.uppercase()) {
      "HIGH" -> "⚡ "
      "IMPORTANT" -> "⭐ "
      else -> "🔔 "
    }

    val notificationTitle = "$priorityPrefix$name in $preMinutes min • $timeFormatted"
    val notificationText = "$name — आपका निर्धारित समय $preMinutes मिनट में होने वाला है ($timeFormatted)।"

    val builder = NotificationCompat.Builder(context, AlarmScheduler.TIMETABLE_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(notificationTitle)
      .setContentText(notificationText)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setAutoCancel(true)
      .setDefaults(NotificationCompat.DEFAULT_ALL)
      .setContentIntent(contentPendingIntent)

    val activityKey = intent.getStringExtra(AlarmScheduler.EXTRA_ACTIVITY_KEY) ?: ""
    val notificationId = if (activityKey.isNotBlank()) {
      AlarmScheduler.getStablePreReminderRequestCodeForActivityKey(activityKey, templateId)
    } else {
      (AlarmScheduler.NOTIFICATION_ID_PRE_REMINDER_BASE + (templateId % 50000)).toInt()
    }
    notificationManager.notify(notificationId, builder.build())
    Log.d("WakeUpAlarmReceiver", "Dispatched smart pre-reminder notification for $name ($preMinutes min before) [key=$activityKey]")
  }

  private fun handleAlarmTrigger(context: Context, intent: Intent) {
    // 1. Start audio and vibration with user saved settings (custom/high-tone, volume, vibration)
    AlarmAudioPlayer.startWithSavedSettings(context)

    // 2. Format 12-hour AM/PM time
    val cal = Calendar.getInstance()
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val timeFormatted = TimeUtils.minutesTo12Hour(currentMinutes)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    // 3. Create High-Priority Alarm Notification Channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val existingChannel = notificationManager.getNotificationChannel(AlarmScheduler.ALARM_CHANNEL_ID)
      if (existingChannel == null) {
        val channel = NotificationChannel(
          AlarmScheduler.ALARM_CHANNEL_ID,
          "Wake-Up Alarm",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Recurring wake-up alarms for Life Tracker"
          enableVibration(true)
          vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
          val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
          val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          setSound(alarmSound, audioAttributes)
          setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
      }
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    // Full screen intent to AlarmActivity
    val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(context, 201, fullScreenIntent, flags)

    // Attempt direct activity launch when screen is turned on / device wakes up
    try {
      context.startActivity(fullScreenIntent)
    } catch (e: Exception) {
      Log.d("WakeUpAlarmReceiver", "startActivity fallback to fullScreenIntent: ${e.message}")
    }

    // Stop / Dismiss Action
    val stopIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      this.action = AlarmScheduler.ACTION_STOP_ALARM
    }
    val stopPendingIntent = PendingIntent.getBroadcast(context, 202, stopIntent, flags)

    // Snooze Action
    val snoozeIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      this.action = AlarmScheduler.ACTION_SNOOZE_ALARM
    }
    val snoozePendingIntent = PendingIntent.getBroadcast(context, 203, snoozeIntent, flags)

    val notification = NotificationCompat.Builder(context, AlarmScheduler.ALARM_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("⏰ Wake-Up Alarm • $timeFormatted")
      .setContentText("Rise and execute your scheduled daily routine!")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setAutoCancel(false)
      .setOngoing(true)
      .setContentIntent(fullScreenPendingIntent)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .addAction(
        NotificationCompat.Action.Builder(
          android.R.drawable.ic_menu_close_clear_cancel,
          "DISMISS / STOP",
          stopPendingIntent
        ).build()
      )
      .addAction(
        NotificationCompat.Action.Builder(
          android.R.drawable.ic_popup_sync,
          "SNOOZE",
          snoozePendingIntent
        ).build()
      )
      .setDeleteIntent(stopPendingIntent)
      .build()

    notificationManager.notify(AlarmScheduler.ALARM_NOTIFICATION_ID, notification)

    // 4. Automatically reschedule for the next cycle occurrence
    val wakeUpMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_WAKE_UP_MINUTES, currentMinutes)
    CoroutineScope(Dispatchers.IO).launch {
      val db = LifeTrackerDatabase.getDatabase(context)
      val settings = db.userSettingsDao().getSettingsSync()
      val repeatMode = settings?.alarmRepeatMode ?: "DAILY"
      val customDaysMask = settings?.alarmCustomDaysMask ?: 127
      AlarmScheduler.scheduleWakeUpAlarm(context, wakeUpMinutes, repeatMode, customDaysMask)
    }
  }

  private fun handleBootCompleted(context: Context) {
    AlarmScheduler.createNotificationChannels(context)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null && settings.isAlarmEnabled) {
          Log.d("WakeUpAlarmReceiver", "Rescheduling wake-up alarm after boot for ${settings.wakeUpMinutes} mins")
          AlarmScheduler.scheduleWakeUpAlarm(
            context,
            settings.wakeUpMinutes,
            settings.alarmRepeatMode,
            settings.alarmCustomDaysMask
          )
        }
        // Reschedule all active customized timetable alarms
        val smartReminder = settings?.smartReminderMinutes ?: 0
        val activeTemplates = db.routineDao().getActiveTemplatesSync()
        for (template in activeTemplates) {
          AlarmScheduler.scheduleTimetableAlarm(context, template, smartReminder)
        }
        Log.d("WakeUpAlarmReceiver", "Rescheduled ${activeTemplates.size} timetable alarms after device boot")

        // Restore Focus Mode state & schedule
        com.example.focus.FocusModeManager.onBootOrScheduleChange(context)

        // Run Watchdog enforcement and check AI screen detection prompt after reboot
        com.example.focus.StrictLockWatchdog.checkAndEnforce(context)
        com.example.focus.StrictLockWatchdog.checkAiDetectionAfterReboot(context)

        // Restore OLED Black Screen Mode & Floating Control if configured
        com.example.blackscreen.BlackScreenManager.onBootCompleted(context)

        // Reschedule automatic daily backup
        com.example.backup.DailyBackupWorker.rescheduleAfterBoot(context)
      } catch (e: Exception) {
        Log.e("WakeUpAlarmReceiver", "Failed to reschedule alarms after boot", e)
      }
    }
  }
}
