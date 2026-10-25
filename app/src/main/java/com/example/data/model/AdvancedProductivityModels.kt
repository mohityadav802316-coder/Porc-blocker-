package com.example.data.model

data class TimelineDayRecord(
  val date: String,
  val dayOfCycle: Int,
  val completionPercentage: Int,
  val totalTasks: Int,
  val completedTasks: Int,
  val missedTasks: Int,
  val mood: String?,
  val note: String?,
  val meditationMinutes: Int,
  val meditationCount: Int,
  val priorityTasksCompleted: List<String>,
  val priorityTasksMissed: List<String>,
  val achievements: List<String>
)

data class MissedActivityStat(
  val activityName: String,
  val category: String,
  val missedCount: Int,
  val totalScheduled: Int
)

data class PersonalInsightsData(
  val routineConsistencyPercentage: Int = 0,
  val totalRecordedDays: Int = 0,
  val qualifyingDaysCount: Int = 0,
  val totalMindfulMinutes: Int = 0,
  val totalMeditationSessions: Int = 0,
  val topMeditationMode: String = "Breathing",
  val currentStreak: Int = 0,
  val longestStreak: Int = 0,
  val mostMissedActivities: List<MissedActivityStat> = emptyList(),
  val weeklyScoreTrend: Int = 0, // e.g. +10% or -5%
  val bestCategory: String = "Routine",
  val needsAttentionCategory: String = "None"
)

data class RoutineChangePreviewData(
  val addedActivities: List<RoutineTemplateEntity> = emptyList(),
  val modifiedActivities: List<Pair<RoutineTemplateEntity, RoutineTemplateEntity>> = emptyList(), // old to new
  val removedActivities: List<RoutineTemplateEntity> = emptyList(),
  val affectedAlarmsCount: Int = 0
)
