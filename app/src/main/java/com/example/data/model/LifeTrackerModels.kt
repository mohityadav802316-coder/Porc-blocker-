package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class TaskStatus(val score: Float, val label: String) {
  COMPLETE(1.0f, "Complete"),
  PARTIAL(0.5f, "Partial"),
  MISSED(0.0f, "Missed");

  companion object {
    fun fromString(value: String): TaskStatus =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MISSED
  }
}

enum class ReflectionCategory(val label: String, val iconPrefix: String) {
  MISTAKE("गलती / कमी", "❌"),
  GOOD_DEED("अच्छाई", "✅"),
  OBSERVATION("Observation", "🔎");

  val fullDisplay: String
    get() = "$iconPrefix $label"

  companion object {
    fun fromString(value: String): ReflectionCategory =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OBSERVATION
  }
}

enum class TaskPriority(val code: String, val label: String, val badge: String) {
  NORMAL("NORMAL", "Normal", ""),
  IMPORTANT("IMPORTANT", "Important", "⭐"),
  HIGH("HIGH", "High Priority", "⚡");

  companion object {
    fun fromString(value: String): TaskPriority =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.code.equals(value, ignoreCase = true) } ?: NORMAL
  }
}

enum class DailyMood(val code: String, val emoji: String, val label: String) {
  GREAT("GREAT", "😊", "Great"),
  GOOD("GOOD", "🙂", "Good"),
  NORMAL("NORMAL", "😐", "Normal"),
  LOW("LOW", "😕", "Low"),
  BAD("BAD", "😞", "Bad");

  companion object {
    fun fromCode(code: String): DailyMood =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NORMAL
  }
}

@Entity(tableName = "daily_notes")
data class DailyNoteEntity(
  @PrimaryKey val date: String, // YYYY-MM-DD
  val note: String = "",
  val mood: String = "NORMAL", // "GREAT", "GOOD", "NORMAL", "LOW", "BAD"
  val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "day_tasks",
  indices = [Index(value = ["date"]), Index(value = ["templateId"])]
)
data class DayTaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String, // YYYY-MM-DD
  val templateId: Long? = null, // null if Extra Task
  val name: String,
  val timeMinutes: Int, // Minutes from midnight (e.g., 300 = 5:00 AM)
  val category: String = "Routine",
  val status: TaskStatus = TaskStatus.MISSED,
  val notes: String = "",
  val isExtra: Boolean = false, // True for Today Extra Tasks
  val orderIndex: Int = 0,
  val priority: String = "NORMAL" // "NORMAL", "IMPORTANT", "HIGH"
)

@Entity(
  tableName = "routine_templates",
  indices = [Index(name = "index_routine_templates_activityKey", value = ["activityKey"], unique = true)]
)
data class RoutineTemplateEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val activityKey: String = "",
  val name: String,
  val timeMinutes: Int, // Minutes from midnight
  val category: String = "Routine",
  val notes: String = "",
  val orderIndex: Int = 0,
  val daysMask: Int = 127, // Bitmask for 7 days (1..7 all active = 127)
  val isActive: Boolean = true,
  val priority: String = "NORMAL" // "NORMAL", "IMPORTANT", "HIGH"
)

@Entity(
  tableName = "reflections",
  indices = [Index(value = ["date"])]
)
data class ReflectionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val description: String = "",
  val date: String, // YYYY-MM-DD
  val timestamp: Long = System.currentTimeMillis(),
  val category: ReflectionCategory = ReflectionCategory.OBSERVATION,
  val tags: String = "" // Comma-separated tags
)

@Entity(tableName = "goals")
data class GoalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val description: String = "",
  val progress: Int = 0, // 0..100%
  val deadline: String? = null,
  val isCompleted: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
  @PrimaryKey val id: Int = 1,
  val anchorDate: String, // Date of Day 1 of Cycle
  val initializedDefaultRoutine: Boolean = true,
  val wakeUpMinutes: Int = 300, // Default 5:00 AM (e.g. 180 = 3:00 AM, 240 = 4:00 AM)
  val isAlarmEnabled: Boolean = false,
  val snoozeMinutes: Int = 10,
  val alarmSoundType: String = "DEFAULT", // "DEFAULT" or "CUSTOM"
  val customSoundUri: String? = null,
  val customSoundTitle: String = "High-Tone Digital Alarm",
  val alarmVolume: Float = 1.0f,
  val isVibrationEnabled: Boolean = true,
  val alarmRepeatMode: String = "DAILY", // "DAILY", "WEEKDAYS", "CUSTOM"
  val alarmCustomDaysMask: Int = 127,
  val isHapticFeedbackEnabled: Boolean = true,
  val smartReminderMinutes: Int = 0, // 0 = Off, 5, 10, 15, 30 mins before
  val userName: String = "मोहित", // User name for personalized meditation ending and greetings
  val meditationChimeEnabled: Boolean = true,
  val meditationVoiceLanguage: String = "HI", // "HI" for Hindi, "EN" for English
  val enableQuickAdd: Boolean = true,
  val enableMorningBrief: Boolean = true,
  val enableLifeTimeline: Boolean = true,
  val enablePersonalInsights: Boolean = true,
  val enableRecoveryMode: Boolean = true,
  val dailyEnergyMode: String = "NORMAL",
  val dailyEnergyModeDate: String = "",
  val dailyChallengeEnabled: Boolean = true,
  val musicAutoRoutineEnabled: Boolean = true,
  val shortDailyLimit: Int = 20,
  val shortWarning50Enabled: Boolean = true,
  val shortWarning80Enabled: Boolean = true,
  val shortWarning100Enabled: Boolean = true,
  val shortFocusLockIntegration: Boolean = true,
  val shortTrackingEnabled: Boolean = true,
  val dashboardPreset: String = "CUSTOM",
  val dashboardSectionsOrder: String = "NEXT_ACTIVITY,PRIORITY_TASK,TODAYS_ROUTINE,DAILY_PROGRESS,STREAK,ENERGY_MODE,SHORT_CONTENT_TRACKER,MEDITATION,MUSIC,DAILY_CHALLENGE,NEXT_ALARM,RECOVERY_DAY,WEEKLY_SUMMARY",
  val dashboardDisabledSections: String = "",
  val isFocusModeActive: Boolean = false,
  val isFocusScheduleEnabled: Boolean = false,
  val focusStartTimeMinutes: Int = 1260, // 9:00 PM (21 * 60)
  val focusEndTimeMinutes: Int = 360,    // 6:00 AM (6 * 60)
  val focusSessionEndTimestamp: Long = 0L,
  // System-Wide OLED Black Screen Mode
  val isBlackScreenEnabled: Boolean = false,
  val isBlackScreenOverlayActive: Boolean = false,
  val isFloatingDotEnabled: Boolean = true,
  val blackScreenDotX: Int = 80,
  val blackScreenDotY: Int = 260,
  val blackScreenDotSize: Int = 36,
  val blackScreenDotOpacity: Float = 0.45f,
  val blackScreenActivationMethod: String = "DOUBLE_TAP",
  val blackScreenExitGesture: String = "THREE_FINGER_TAP",
  val blackScreenGestureSensitivity: Float = 1.0f,
  val blackScreenShowClock: Boolean = true,
  val blackScreenClockFormat24: Boolean = false,
  val blackScreenRestoreAfterUnlock: Boolean = true,
  val blackScreenRestoreAfterReboot: Boolean = true
) {
  val dashboardOrder: String get() = dashboardSectionsOrder
  val dashboardDisabled: String get() = dashboardDisabledSections
}

@Entity(tableName = "daily_snapshots")
data class DailySnapshotEntity(
  @PrimaryKey val date: String, // YYYY-MM-DD
  val dayOfCycle: Int,
  val totalTasks: Int,
  val completedCount: Int,
  val partialCount: Int,
  val missedCount: Int,
  val totalScore: Float,
  val completionPercentage: Int,
  val lastUpdated: Long = System.currentTimeMillis()
)

data class OverallStats(
  val totalDays: Int = 0,
  val totalTasks: Int = 0,
  val completedTasks: Int = 0,
  val partialTasks: Int = 0,
  val missedTasks: Int = 0,
  val totalScore: Float = 0f,
  val averagePercentage: Float = 0f
)

data class CycleDayAverage(
  val dayOfCycle: Int,
  val dayCount: Int,
  val avgPercentage: Float
)

data class CategoryAggregate(
  val category: String,
  val totalCount: Int,
  val completedCount: Int,
  val partialCount: Int
)

data class DaySummary(
  val date: String,
  val dayOfCycle: Int,
  val totalTasks: Int,
  val completedCount: Int,
  val partialCount: Int,
  val missedCount: Int,
  val totalScore: Float,
  val completionPercentage: Int
)

data class StreakInfo(
  val currentStreak: Int = 0,
  val longestStreak: Int = 0,
  val todayStatus: String = "Pending", // "Maintained 🔥", "In Progress ⏳", "Pending ⚠️", "Broken"
  val isMaintainedToday: Boolean = false,
  val activeDaysCount: Int = 0
) {
  val bestStreak: Int get() = longestStreak
  val consistencyScore: Float get() = if (longestStreak > 0) ((currentStreak.toFloat() / longestStreak.toFloat()) * 100f).coerceIn(0f, 100f) else 100f
}

class LifeTrackerConverters {
  @TypeConverter
  fun fromTaskStatus(status: TaskStatus): String = status.name

  @TypeConverter
  fun toTaskStatus(value: String): TaskStatus = TaskStatus.fromString(value)

  @TypeConverter
  fun fromReflectionCategory(category: ReflectionCategory): String = category.name

  @TypeConverter
  fun toReflectionCategory(value: String): ReflectionCategory = ReflectionCategory.fromString(value)
}
