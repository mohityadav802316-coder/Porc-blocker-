package com.example.focus

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Centralized Notification Channels Management for Life Tracker.
 *
 * Divides notifications into 4 distinct, user-controllable channels:
 * 1. Protection Running (Silent, Low importance): Foreground watchdog service notification.
 * 2. Strict Lock Timer (Silent, Low importance): 15-minute countdown, updated once per minute (never spamming or flashing torch).
 * 3. Security Alerts (High importance): Real security events (tamper intercepted, multi-user bypass, unexpected service shutdown).
 * 4. Reminders (Default importance): Re-enable reminders after pause/unlock expires (every 30 mins) & health suggestions.
 */
object AppNotificationChannels {

  const val CHANNEL_PROTECTION_RUNNING = "life_tracker_protection_running"
  const val CHANNEL_STRICT_LOCK_TIMER = "life_tracker_strict_lock_channel"
  const val CHANNEL_SECURITY_ALERTS = "life_tracker_security_alerts_channel"
  const val CHANNEL_REMINDERS = "life_tracker_reminders_channel"

  data class ChannelDetail(
    val id: String,
    val nameHindi: String,
    val nameEnglish: String,
    val importanceLabel: String,
    val triggerConditionHindi: String,
    val reasonNeededHindi: String
  )

  val ALL_CHANNELS = listOf(
    ChannelDetail(
      id = CHANNEL_PROTECTION_RUNNING,
      nameHindi = "प्रोटेक्शन रनिंग (Protection Running)",
      nameEnglish = "Protection Running Foreground Service",
      importanceLabel = "Silent / Low (शांत)",
      triggerConditionHindi = "जब Peewee Block की बैकग्राउंड सुरक्षा और वॉचडॉग सक्रिय हो।",
      reasonNeededHindi = "Android OS को ऐप की सुरक्षा सेवा को मेमोरी से बंद करने से रोकने हेतु आवश्यक निरंतर सूचना।"
    ),
    ChannelDetail(
      id = CHANNEL_STRICT_LOCK_TIMER,
      nameHindi = "स्ट्रिक्ट लॉक टाइमर (Strict Lock Timer)",
      nameEnglish = "Strict Lock Countdown Timer",
      importanceLabel = "Silent / Low (शांत, बिना फ्लैश/ध्वनि)",
      triggerConditionHindi = "जब 15 मिनट का सख्त अनुशासन लॉक सक्रिय होता है। प्रति 1 मिनट में केवल एक बार शांत अपडेट होता है।",
      reasonNeededHindi = "अनुशासन के दौरान शेष समय दिखाता है। बिना ध्वनि/वाइब्रेशन/फ्लैश के सुरक्षित रहता है।"
    ),
    ChannelDetail(
      id = CHANNEL_SECURITY_ALERTS,
      nameHindi = "सुरक्षा चेतावनियाँ (Security Alerts)",
      nameEnglish = "Security Alerts & Tamper Intercepts",
      importanceLabel = "High / ज़रूरी (ध्वनि/अलर्ट सहित)",
      triggerConditionHindi = "जब कोई Peewee Block को बंद/अनइंस्टॉल करने की कोशिश करे, मल्टी-यूज़र बायपास खुले, या सुरक्षा घटक बंद मिले।",
      reasonNeededHindi = "बायपास और छेड़छाड़ के प्रयास को तुरंत उपयोगकर्ता और जवाबदेह साथी को सूचित करने हेतु।"
    ),
    ChannelDetail(
      id = CHANNEL_REMINDERS,
      nameHindi = "रिमाइंडर (Reminders)",
      nameEnglish = "Re-enable & Health Reminders",
      importanceLabel = "Default (सामान्य)",
      triggerConditionHindi = "जब पॉज़ या 24 घंटे का अनलॉक समाप्त हो जाए और सुरक्षा घटक बंद हों (हर 30 मिनट में)।",
      reasonNeededHindi = "पॉज़ खत्म होने के बाद सुरक्षा वापस चालू करने की याद दिलाने हेतु। इसे आप चाहें तो बंद भी कर सकते हैं।"
    )
  )

  /**
   * Initializes all channels with proper importance, silent flags, and descriptions.
   */
  fun registerAllChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    // 1. Protection Running (Low importance, completely silent)
    val runningChannel = NotificationChannel(
      CHANNEL_PROTECTION_RUNNING,
      "Protection Running (सुरक्षा चालू)",
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = "Foreground watchdog service notification"
      setShowBadge(false)
      enableLights(false)
      enableVibration(false)
      setSound(null, null)
    }
    nm.createNotificationChannel(runningChannel)

    // 2. Strict Lock Timer (Low importance, completely silent, no vibration, no lights)
    val strictLockChannel = NotificationChannel(
      CHANNEL_STRICT_LOCK_TIMER,
      "Strict Lock Timer (सख्त लॉक टाइमर)",
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = "15-minute strict discipline countdown (silent, updated once per minute)"
      setShowBadge(false)
      enableLights(false)
      enableVibration(false)
      setSound(null, null)
    }
    nm.createNotificationChannel(strictLockChannel)

    // 3. Security Alerts (High importance)
    val alertsChannel = NotificationChannel(
      CHANNEL_SECURITY_ALERTS,
      "Security Alerts (सुरक्षा चेतावनियाँ)",
      NotificationManager.IMPORTANCE_HIGH
    ).apply {
      description = "Critical alerts for tamper attempts, multi-user bypass, or disabled protection"
      setShowBadge(true)
      enableLights(true)
      enableVibration(true)
    }
    nm.createNotificationChannel(alertsChannel)

    // 4. Reminders (Default importance)
    val remindersChannel = NotificationChannel(
      CHANNEL_REMINDERS,
      "Reminders (सुरक्षा रिमाइंडर)",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "30-minute re-enable reminders after pause/unlock and health recommendations"
      setShowBadge(true)
    }
    nm.createNotificationChannel(remindersChannel)
  }

  /**
   * Checks whether a specific channel is enabled in Android OS.
   */
  fun isChannelEnabled(context: Context, channelId: String): Boolean {
    val nm = NotificationManagerCompat.from(context)
    if (!nm.areNotificationsEnabled()) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = nm.getNotificationChannel(channelId) ?: return true // If not created yet, considered default
      return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
    return true
  }

  /**
   * Returns an Intent to open the specific channel settings page in Android Settings.
   */
  fun createOpenChannelSettingsIntent(context: Context, channelId: String): Intent {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    } else {
      Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    }
  }
}
