package com.example.recovery

import android.content.Context
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RecoveryDayManager {

  /**
   * Applies an intelligent, lightened Recovery schedule for the target date.
   * Keeps essential tasks, lightens optional items, reschedules missed activities
   * starting from current time with gentle breathing buffers.
   * Never permanently alters routine_templates.
   */
  suspend fun activateRecoveryDay(context: Context, date: String, currentMinutes: Int) = withContext(Dispatchers.IO) {
    val db = LifeTrackerDatabase.getDatabase(context)
    val taskDao = db.taskDao()

    val existingTasks = taskDao.getTasksForDateSync(date)
    if (existingTasks.isEmpty()) return@withContext

    val essentialTasks = existingTasks.filter { task ->
      task.priority == TaskPriority.HIGH.code ||
      task.priority == TaskPriority.IMPORTANT.code ||
      task.category.contains("Health", ignoreCase = true) ||
      task.category.contains("Sleep", ignoreCase = true) ||
      task.category.contains("Essential", ignoreCase = true) ||
      task.name.contains("नींद", ignoreCase = true) ||
      task.name.contains("दवा", ignoreCase = true) ||
      task.name.contains("भोजन", ignoreCase = true) ||
      task.name.contains("Water", ignoreCase = true)
    }

    val optionalTasks = existingTasks.filterNot { it in essentialTasks }

    // Start scheduling from now or next 15-min boundary
    var cursorMinutes = maxOf(currentMinutes, 360) // At least 6:00 AM
    val roundedMinutes = ((cursorMinutes + 14) / 15) * 15

    var scheduledTime = roundedMinutes
    val updatedTasks = mutableListOf<DayTaskEntity>()

    // 1. First add a gentle "Recovery Awakening & Hydration" task if starting now
    updatedTasks.add(
      DayTaskEntity(
        date = date,
        name = "💧 शांत विश्राम, जलपान एवं गहरी श्वास (Recovery Reset)",
        timeMinutes = scheduledTime,
        category = "Recovery",
        status = TaskStatus.COMPLETE,
        isExtra = true,
        priority = TaskPriority.IMPORTANT.code,
        notes = "तनाव मुक्त हों। आज का लक्ष्य स्वयं को पुनः ऊर्जावान बनाना है।"
      )
    )
    scheduledTime += 30

    // 2. Schedule essential tasks in realistic order
    for (task in essentialTasks) {
      updatedTasks.add(
        task.copy(
          timeMinutes = scheduledTime,
          status = if (task.status == TaskStatus.COMPLETE) TaskStatus.COMPLETE else TaskStatus.MISSED,
          notes = "आवश्यक कार्य • ${task.notes}".trim()
        )
      )
      scheduledTime += 45 // 45 min duration + buffer
      if (scheduledTime >= 1320) break // Don't schedule past 10:00 PM
    }

    // 3. Keep 1 or 2 optional tasks as light suggestions
    for (task in optionalTasks.take(2)) {
      if (scheduledTime < 1260) {
        updatedTasks.add(
          task.copy(
            timeMinutes = scheduledTime,
            status = if (task.status == TaskStatus.COMPLETE) TaskStatus.COMPLETE else TaskStatus.MISSED,
            priority = TaskPriority.NORMAL.code,
            notes = "वैकल्पिक (Optional) • इच्छा हो तो ही करें"
          )
        )
        scheduledTime += 30
      }
    }

    // 4. Add night wind down
    updatedTasks.add(
      DayTaskEntity(
        date = date,
        name = "🌙 शांतिपूर्ण शयन एवं शरीर विश्राम (Early Rest)",
        timeMinutes = 1320, // 10:00 PM
        category = "Health",
        status = TaskStatus.MISSED,
        isExtra = true,
        priority = TaskPriority.HIGH.code,
        notes = "कल एक नई सुबह और सामान्य रूटीन का स्वागत करेंगे।"
      )
    )

    // Replace today's tasks with lightened recovery plan
    taskDao.deleteTasksForDate(date)
    taskDao.insertTasks(updatedTasks)
  }

  /**
   * Reverts today's schedule back to the original cycle template without data loss.
   */
  suspend fun restoreDefaultRoutine(context: Context, date: String, cycleDay: Int) = withContext(Dispatchers.IO) {
    val db = LifeTrackerDatabase.getDatabase(context)
    val repo = com.example.data.repository.LifeTrackerRepository(db)
    repo.restoreTodayTasksToDefault(date, cycleDay)
  }
}
