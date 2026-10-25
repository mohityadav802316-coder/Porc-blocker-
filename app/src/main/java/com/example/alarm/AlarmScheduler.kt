package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.DayTaskEntity
import com.example.data.model.RoutineTemplateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object AlarmScheduler {
  const val ACTION_TRIGGER_ALARM = "com.mohit.lifetracker.ACTION_TRIGGER_ALARM"
  const val ACTION_STOP_ALARM = "com.mohit.lifetracker.ACTION_STOP_ALARM"
  const val ACTION_SNOOZE_ALARM = "com.mohit.lifetracker.ACTION_SNOOZE_ALARM"

  const val EXTRA_WAKE_UP_MINUTES = "extra_wake_up_minutes"
  const val ACTION_TIMETABLE_REMINDER = "com.mohit.lifetracker.ACTION_TIMETABLE_REMINDER"
  const val ACTION_SMART_PRE_REMINDER = "com.mohit.lifetracker.ACTION_SMART_PRE_REMINDER"
  const val EXTRA_TEMPLATE_ID = "extra_template_id"
  const val EXTRA_ACTIVITY_KEY = "extra_activity_key"
  const val EXTRA_ACTIVITY_NAME = "extra_activity_name"
  const val EXTRA_TIME_MINUTES = "extra_time_minutes"
  const val EXTRA_CATEGORY = "extra_category"
  const val EXTRA_NOTES = "extra_notes"
  const val EXTRA_PRE_MINUTES = "extra_pre_minutes"
  const val EXTRA_PRIORITY = "extra_priority"

  const val ALARM_CHANNEL_ID = "life_tracker_wake_up_channel"
  const val TIMETABLE_CHANNEL_ID = "life_tracker_timetable_channel"

  const val ALARM_NOTIFICATION_ID = 4001
  const val NOTIFICATION_ID_TIMETABLE_BASE = 10000
  const val NOTIFICATION_ID_PRE_REMINDER_BASE = 60000

  private const val REQUEST_CODE_ALARM = 101
  private const val REQUEST_CODE_SNOOZE = 102
  private const val REQUEST_CODE_TIMETABLE_BASE = 10000
  private const val REQUEST_CODE_PRE_REMINDER_BASE = 60000

  fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

      // 1. Wake-Up Alarm Channel (High Priority with Alarm Audio Attributes)
      if (notificationManager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
        val wakeUpChannel = NotificationChannel(
          ALARM_CHANNEL_ID,
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
          lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(wakeUpChannel)
      }

      // 2. Timetable & Activity Reminders Channel (High Priority, Lock Screen Visible)
      if (notificationManager.getNotificationChannel(TIMETABLE_CHANNEL_ID) == null) {
        val timetableChannel = NotificationChannel(
          TIMETABLE_CHANNEL_ID,
          "Timetable & Activity Reminders",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Scheduled daily notifications for timetable activities"
          enableVibration(true)
          vibrationPattern = longArrayOf(0, 500, 250, 500)
          val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
          val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
          setSound(notificationSound, audioAttributes)
          lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(timetableChannel)
      }
    }
  }

  fun calculateNextAlarmMillis(
    wakeUpMinutes: Int,
    repeatMode: String = "DAILY",
    customDaysMask: Int = 127
  ): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, wakeUpMinutes / 60)
      set(Calendar.MINUTE, wakeUpMinutes % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (target.timeInMillis <= now.timeInMillis) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Check repeat condition for up to 7 consecutive days
    for (i in 0..7) {
      val dayOfWeek = target.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
      val matches = when (repeatMode.uppercase()) {
        "WEEKDAYS" -> dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        "CUSTOM" -> {
          val bitIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
          }
          (customDaysMask and (1 shl bitIndex)) != 0
        }
        else -> true // DAILY
      }
      if (matches) break
      target.add(Calendar.DAY_OF_YEAR, 1)
    }

    return target.timeInMillis
  }

  fun scheduleWakeUpAlarm(
    context: Context,
    wakeUpMinutes: Int,
    repeatMode: String = "DAILY",
    customDaysMask: Int = 127
  ) {
    createNotificationChannels(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val triggerMillis = calculateNextAlarmMillis(wakeUpMinutes, repeatMode, customDaysMask)

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_ALARM
      putExtra(EXTRA_WAKE_UP_MINUTES, wakeUpMinutes)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_ALARM,
      intent,
      flags
    )

    val showIntent = Intent(context, MainActivity::class.java)
    val showPendingIntent = PendingIntent.getActivity(
      context,
      0,
      showIntent,
      flags
    )

    try {
      val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
      alarmManager.setAlarmClock(clockInfo, pendingIntent)
      Log.d("AlarmScheduler", "Alarm scheduled for epoch: $triggerMillis (${java.util.Date(triggerMillis)})")
    } catch (e: SecurityException) {
      Log.e("AlarmScheduler", "Exact alarm permission missing, falling back to setAndAllowWhileIdle", e)
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  fun scheduleSnooze(context: Context, snoozeMinutes: Int = 10) {
    createNotificationChannels(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_ALARM
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_SNOOZE,
      intent,
      flags
    )

    val showIntent = Intent(context, MainActivity::class.java)
    val showPendingIntent = PendingIntent.getActivity(
      context,
      0,
      showIntent,
      flags
    )

    try {
      val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
      alarmManager.setAlarmClock(clockInfo, pendingIntent)
    } catch (e: Exception) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_ALARM
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)
    alarmManager.cancel(pendingIntent)

    val snoozeIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_SNOOZE, intent, flags)
    alarmManager.cancel(snoozeIntent)

    AlarmAudioPlayer.stop()
  }

  /**
   * Idempotent dismissal for the currently active/ringing alarm session.
   * Stops audio playback, stops vibration, cancels active alarm notification,
   * cancels any scheduled snooze, and notifies the UI to finish.
   * IMPORTANT: Does NOT cancel tomorrow's recurring daily alarm (REQUEST_CODE_ALARM).
   */
  fun dismissRingingAlarm(context: Context) {
    try {
      // 1. Immediately stop audio playback & vibration (thread-safe, cancels pending start jobs)
      AlarmAudioPlayer.stop()

      // 2. Dismiss active alarm notification
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.cancel(ALARM_NOTIFICATION_ID)

      // 3. Cancel any pending snooze in AlarmManager
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      if (alarmManager != null) {
        val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
          action = ACTION_TRIGGER_ALARM
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
          PendingIntent.FLAG_UPDATE_CURRENT
        }
        val snoozeIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_SNOOZE, intent, flags)
        alarmManager.cancel(snoozeIntent)
      }

      // 4. Send internal broadcast so that any running AlarmActivity finishes immediately
      val dismissBroadcast = Intent(ACTION_STOP_ALARM).apply {
        setPackage(context.packageName)
      }
      context.sendBroadcast(dismissBroadcast)
      Log.d("AlarmScheduler", "dismissRingingAlarm completed successfully (recurring alarm preserved)")
    } catch (e: Exception) {
      Log.e("AlarmScheduler", "Error during alarm dismissal", e)
    }
  }

  /**
   * Calculates the next trigger timestamp for a timetable activity.
   * If the time today is in the future, returns today's epoch millis.
   * If the time today has passed, advances to tomorrow (or the next active day in daysMask).
   */
  fun calculateNextActivityMillis(timeMinutes: Int, daysMask: Int = 127): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, timeMinutes / 60)
      set(Calendar.MINUTE, timeMinutes % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    // If scheduled time has already passed today, advance to tomorrow
    if (target.timeInMillis <= now.timeInMillis) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Verify repeat condition for up to 7 consecutive days
    for (i in 0..7) {
      val dayOfWeek = target.get(Calendar.DAY_OF_WEEK)
      val bitIndex = when (dayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
      }
      if ((daysMask and (1 shl bitIndex)) != 0) {
        break
      }
      target.add(Calendar.DAY_OF_YEAR, 1)
    }

    return target.timeInMillis
  }

  /**
   * Schedules an exact system alarm for a timetable activity.
   * Uses stable unique RequestCode derived from template.id to prevent duplicate alarms.
   * Compatible with Android lock-screen and background Doze mode.
   */
  fun getStableRequestCodeForActivityKey(activityKey: String, templateId: Long = 0L): Int {
    val key = if (activityKey.isNotBlank()) activityKey else "template_$templateId"
    val hash = (key.hashCode() and 0x7fffffff) % 30000
    return REQUEST_CODE_TIMETABLE_BASE + hash
  }

  fun getStablePreReminderRequestCodeForActivityKey(activityKey: String, templateId: Long = 0L): Int {
    val key = if (activityKey.isNotBlank()) activityKey else "template_$templateId"
    val hash = (key.hashCode() and 0x7fffffff) % 30000
    return REQUEST_CODE_PRE_REMINDER_BASE + hash
  }

  private fun cancelExactAlarmByCode(context: Context, requestCode: Int, action: String) {
    try {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
        this.action = action
      }
      val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    } catch (_: Exception) {}
  }

  /**
   * Cancels old legacy template-id alarms (slots 1..200) to clean up stale duplicate alarms after migration.
   */
  fun cancelAllLegacyTemplateAlarms(context: Context) {
    try {
      for (id in 1L..200L) {
        val legacyCode = (REQUEST_CODE_TIMETABLE_BASE + (id % 50000)).toInt()
        val legacyPreCode = (REQUEST_CODE_PRE_REMINDER_BASE + (id % 50000)).toInt()
        cancelExactAlarmByCode(context, legacyCode, ACTION_TIMETABLE_REMINDER)
        cancelExactAlarmByCode(context, legacyPreCode, ACTION_SMART_PRE_REMINDER)
      }
      Log.d("AlarmScheduler", "Cancelled all legacy template-id alarms")
    } catch (e: Exception) {
      Log.e("AlarmScheduler", "Error cancelling legacy template alarms", e)
    }
  }

  /**
   * Schedules an exact system alarm for a timetable activity, plus an optional smart pre-reminder.
   * Uses stable unique RequestCode derived from template.activityKey to prevent duplicate alarms.
   * Old legacy template-id alarms are cleaned up automatically.
   * Compatible with Android lock-screen and background Doze mode.
   */
  fun scheduleTimetableAlarm(
    context: Context,
    template: RoutineTemplateEntity,
    smartReminderMinutes: Int = 0
  ) {
    if (!template.isActive) return
    createNotificationChannels(context)

    // 1. Cancel old legacy template-id alarm if requestCode differed
    val legacyCode = (REQUEST_CODE_TIMETABLE_BASE + (template.id % 50000)).toInt()
    val stableCode = getStableRequestCodeForActivityKey(template.activityKey, template.id)
    if (legacyCode != stableCode) {
      cancelExactAlarmByCode(context, legacyCode, ACTION_TIMETABLE_REMINDER)
    }

    val legacyPreCode = (REQUEST_CODE_PRE_REMINDER_BASE + (template.id % 50000)).toInt()
    val stablePreCode = getStablePreReminderRequestCodeForActivityKey(template.activityKey, template.id)
    if (legacyPreCode != stablePreCode) {
      cancelExactAlarmByCode(context, legacyPreCode, ACTION_SMART_PRE_REMINDER)
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerMillis = calculateNextActivityMillis(template.timeMinutes, template.daysMask)

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TIMETABLE_REMINDER
      putExtra(EXTRA_TEMPLATE_ID, template.id)
      putExtra(EXTRA_ACTIVITY_KEY, template.activityKey)
      putExtra(EXTRA_ACTIVITY_NAME, template.name)
      putExtra(EXTRA_TIME_MINUTES, template.timeMinutes)
      putExtra(EXTRA_CATEGORY, template.category)
      putExtra(EXTRA_NOTES, template.notes)
      putExtra(EXTRA_PRIORITY, template.priority)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      stableCode,
      intent,
      flags
    )

    val showIntent = Intent(context, MainActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val showPendingIntent = PendingIntent.getActivity(
      context,
      stableCode,
      showIntent,
      flags
    )

    try {
      // Exact-time alarm (Never disabled or weakened)
      val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
      alarmManager.setAlarmClock(clockInfo, pendingIntent)
      Log.d("AlarmScheduler", "Scheduled timetable alarm for '${template.name}' [key=${template.activityKey}, code=$stableCode] at $triggerMillis (${java.util.Date(triggerMillis)})")
    } catch (e: SecurityException) {
      Log.e("AlarmScheduler", "Exact alarm permission missing, falling back to setAndAllowWhileIdle", e)
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    } catch (e: Exception) {
      Log.e("AlarmScheduler", "Failed to schedule timetable alarm", e)
    }

    // Feature 2: Smart Pre-Reminder (e.g. 5, 10, 15, 30 mins before)
    if (smartReminderMinutes > 0) {
      val preTriggerMillis = triggerMillis - (smartReminderMinutes * 60 * 1000L)
      if (preTriggerMillis > System.currentTimeMillis()) {
        val preIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
          action = ACTION_SMART_PRE_REMINDER
          putExtra(EXTRA_TEMPLATE_ID, template.id)
          putExtra(EXTRA_ACTIVITY_KEY, template.activityKey)
          putExtra(EXTRA_ACTIVITY_NAME, template.name)
          putExtra(EXTRA_TIME_MINUTES, template.timeMinutes)
          putExtra(EXTRA_PRE_MINUTES, smartReminderMinutes)
          putExtra(EXTRA_CATEGORY, template.category)
          putExtra(EXTRA_NOTES, template.notes)
          putExtra(EXTRA_PRIORITY, template.priority)
        }
        val prePendingIntent = PendingIntent.getBroadcast(
          context,
          stablePreCode,
          preIntent,
          flags
        )
        try {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTriggerMillis, prePendingIntent)
          Log.d("AlarmScheduler", "Scheduled smart pre-reminder for '${template.name}' at $preTriggerMillis ($smartReminderMinutes min before)")
        } catch (_: Exception) {
          alarmManager.set(AlarmManager.RTC_WAKEUP, preTriggerMillis, prePendingIntent)
        }
      }
    }
  }

  /**
   * Cancels a previously scheduled alarm (and any pre-reminder) for a timetable activity.
   * Cancels BOTH legacy templateId slot and stable activityKey slot.
   */
  fun cancelTimetableAlarm(context: Context, templateId: Long, activityKey: String? = null) {
    // 1. Cancel legacy exact-time and pre-reminder alarms
    val legacyCode = (REQUEST_CODE_TIMETABLE_BASE + (templateId % 50000)).toInt()
    val legacyPreCode = (REQUEST_CODE_PRE_REMINDER_BASE + (templateId % 50000)).toInt()
    cancelExactAlarmByCode(context, legacyCode, ACTION_TIMETABLE_REMINDER)
    cancelExactAlarmByCode(context, legacyPreCode, ACTION_SMART_PRE_REMINDER)

    // 2. Cancel stable activityKey exact-time and pre-reminder alarms
    if (!activityKey.isNullOrBlank()) {
      val stableCode = getStableRequestCodeForActivityKey(activityKey, templateId)
      val stablePreCode = getStablePreReminderRequestCodeForActivityKey(activityKey, templateId)
      cancelExactAlarmByCode(context, stableCode, ACTION_TIMETABLE_REMINDER)
      cancelExactAlarmByCode(context, stablePreCode, ACTION_SMART_PRE_REMINDER)
    }

    Log.d("AlarmScheduler", "Cancelled timetable alarm & pre-reminder for templateId: $templateId, key: $activityKey")
  }

  fun cancelAllTimetableAlarms(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val activeTemplates = db.routineDao().getActiveTemplatesSync()
        for (template in activeTemplates) {
          cancelTimetableAlarm(context, template.id, template.activityKey)
        }
        cancelAllLegacyTemplateAlarms(context)
      } catch (e: Exception) {
        Log.e("AlarmScheduler", "Error cancelling timetable alarms", e)
      }
    }
  }

  /**
   * Feature 7: Schedules or reschedules an alarm specifically for today's customized task.
   */
  fun scheduleTodayTaskAlarm(context: Context, task: DayTaskEntity, smartReminderMinutes: Int = 0) {
    createNotificationChannels(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, task.timeMinutes / 60)
      set(Calendar.MINUTE, task.timeMinutes % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    // Only schedule if still in future today
    if (target.timeInMillis <= now.timeInMillis) return

    val triggerMillis = target.timeInMillis
    val requestCode = (REQUEST_CODE_TIMETABLE_BASE + (task.id % 50000)).toInt()

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TIMETABLE_REMINDER
      putExtra(EXTRA_TEMPLATE_ID, task.templateId ?: task.id)
      putExtra(EXTRA_ACTIVITY_NAME, task.name)
      putExtra(EXTRA_TIME_MINUTES, task.timeMinutes)
      putExtra(EXTRA_CATEGORY, task.category)
      putExtra(EXTRA_NOTES, task.notes)
      putExtra(EXTRA_PRIORITY, task.priority)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
    try {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      Log.d("AlarmScheduler", "Scheduled today customized alarm for '${task.name}' at $triggerMillis")
    } catch (_: Exception) {
      alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    // Smart pre-reminder for today task
    if (smartReminderMinutes > 0) {
      val preTriggerMillis = triggerMillis - (smartReminderMinutes * 60 * 1000L)
      if (preTriggerMillis > System.currentTimeMillis()) {
        val preRequestCode = (REQUEST_CODE_PRE_REMINDER_BASE + (task.id % 50000)).toInt()
        val preIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
          action = ACTION_SMART_PRE_REMINDER
          putExtra(EXTRA_TEMPLATE_ID, task.templateId ?: task.id)
          putExtra(EXTRA_ACTIVITY_NAME, task.name)
          putExtra(EXTRA_TIME_MINUTES, task.timeMinutes)
          putExtra(EXTRA_PRE_MINUTES, smartReminderMinutes)
          putExtra(EXTRA_CATEGORY, task.category)
          putExtra(EXTRA_NOTES, task.notes)
          putExtra(EXTRA_PRIORITY, task.priority)
        }
        val prePendingIntent = PendingIntent.getBroadcast(context, preRequestCode, preIntent, flags)
        try {
          alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTriggerMillis, prePendingIntent)
        } catch (_: Exception) {
          alarmManager.set(AlarmManager.RTC_WAKEUP, preTriggerMillis, prePendingIntent)
        }
      }
    }
  }

  /**
   * Reschedules all active timetable alarms from Room SQLite database.
   * Called on device boot (BOOT_COMPLETED) or app initialization.
   */
  fun rescheduleAllTimetableAlarms(context: Context) {
    createNotificationChannels(context)
    CoroutineScope(Dispatchers.IO).launch {
      try {
        cancelAllLegacyTemplateAlarms(context)
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        val smartReminder = settings?.smartReminderMinutes ?: 0
        val activeTemplates = db.routineDao().getActiveTemplatesSync()
        for (template in activeTemplates) {
          scheduleTimetableAlarm(context, template, smartReminder)
        }
        Log.d("AlarmScheduler", "Rescheduled ${activeTemplates.size} timetable alarms with smartReminder=$smartReminder min")
      } catch (e: Exception) {
        Log.e("AlarmScheduler", "Error rescheduling timetable alarms", e)
      }
    }
  }
}
