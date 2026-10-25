package com.example.data.repository

import android.content.Context
import com.example.data.db.DailyNoteDao
import com.example.data.db.DailySnapshotDao
import com.example.data.db.GoalDao
import com.example.data.db.JournalistEntryDao
import com.example.data.db.JournalistPersonDao
import com.example.data.db.LifeTrackerDatabase
import com.example.data.db.MeditationDao
import com.example.data.db.ReflectionDao
import com.example.data.db.RoutineDao
import com.example.data.db.TaskDao
import com.example.data.db.UserSettingsDao
import com.example.data.model.CategoryAggregate
import com.example.data.model.CycleDayAverage
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DaySummary
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.OverallStats
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.UserSettingsEntity
import com.example.util.RoutineUtils
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class LifeTrackerRepository(private val database: LifeTrackerDatabase) {
  val taskDao: TaskDao = database.taskDao()
  val routineDao: RoutineDao = database.routineDao()
  val reflectionDao: ReflectionDao = database.reflectionDao()
  val goalDao: GoalDao = database.goalDao()
  val userSettingsDao: UserSettingsDao = database.userSettingsDao()
  val dailySnapshotDao: DailySnapshotDao = database.dailySnapshotDao()
  val journalistPersonDao: JournalistPersonDao = database.journalistPersonDao()
  val journalistEntryDao: JournalistEntryDao = database.journalistEntryDao()
  val dailyNoteDao: DailyNoteDao = database.dailyNoteDao()
  val meditationDao: MeditationDao = database.meditationDao()
  val musicDao: com.example.data.db.MusicDao = database.musicDao()
  val dailyChallengeDao: com.example.data.db.DailyChallengeDao = database.dailyChallengeDao()
  val shortContentDao: com.example.data.db.ShortContentDao = database.shortContentDao()
  val expenseDao: com.example.data.db.ExpenseDao = database.expenseDao()
  val strictLockDao: com.example.data.db.StrictLockDao = database.strictLockDao()

  /**
   * Safe SQLite write with automatic retry to guarantee persistence
   * across rapid UI actions or transient lock states.
   */
  private suspend fun <T> safeDbWrite(maxAttempts: Int = 3, action: suspend () -> T): Result<T> {
    var lastException: Throwable? = null
    for (attempt in 1..maxAttempts) {
      try {
        val result = action()
        return Result.success(result)
      } catch (e: Exception) {
        lastException = e
        if (attempt < maxAttempts) {
          delay(attempt * 40L)
        }
      }
    }
    return Result.failure(lastException ?: Exception("Database write failed"))
  }

  suspend fun initializeDefaultsIfNeeded(todayDate: String) = withContext(Dispatchers.IO) {
    var settings = userSettingsDao.getSettingsSync()
    if (settings == null) {
      settings = UserSettingsEntity(
        id = 1,
        anchorDate = todayDate,
        initializedDefaultRoutine = true
      )
      safeDbWrite { userSettingsDao.insertOrUpdate(settings) }
    }

    // User Requirement: On new install / empty database, do NOT automatically create/seed/insert default routines.
    // The routine section starts completely blank. User will add routines manually from Settings.
    // If existing routines exist (from an existing user), preserve and deduplicate them.
    val routineCount = routineDao.countRoutineTemplates()
    if (routineCount > 0) {
      // Ensure existing routine rows are deduplicated and day_tasks re-linked
      deduplicateRoutineTemplatesAndRelinkDayTasks()
    }
    // Clean any accidental duplicate tasks across days while safely preserving completed history
    deduplicateDayTasks()

    // Ensure fixed default person "मैं (स्वयं)" exists
    val selfPerson = journalistPersonDao.getPersonById("self")
    if (selfPerson == null) {
      safeDbWrite {
        journalistPersonDao.insertPerson(
          JournalistPersonEntity(
            id = "self",
            name = "मैं (स्वयं)",
            emoji = "👤",
            createdAt = System.currentTimeMillis()
          )
        )
      }
    }

    // Ensure today's day tasks exist without altering any historical day
    val cycleDay = TimeUtils.calculateCycleDay(settings.anchorDate, todayDate)
    ensureDayInitialized(todayDate, cycleDay)

    // Ensure historical snapshots exist for all stored dates
    syncAllHistoricalSnapshots()
  }

  private val dayInitMutex = Mutex()

  suspend fun ensureDayInitialized(date: String, cycleDay: Int) = withContext(Dispatchers.IO) {
    dayInitMutex.withLock {
      // 1. First run safety deduplication on this date to clean any previous accidental doubles
      deduplicateDayTasksForDate(date)

      val existing = taskDao.getTasksForDateSync(date)
      val templates = routineDao.getActiveTemplatesSync()
      val maskBit = 1 shl (cycleDay - 1)
      val applicableTemplates = templates.filter { (it.daysMask and maskBit) != 0 }

      // CRITICAL DEDUPLICATION: Ensure no duplicate templates exist for same logical activity
      val distinctTemplates = applicableTemplates.distinctBy {
        if (it.activityKey.isNotBlank()) it.activityKey else it.name.trim().lowercase()
      }

      val existingTemplateIds = existing.mapNotNull { it.templateId }.toSet()
      val newTasksToInsert = mutableListOf<DayTaskEntity>()
      val startOrder = existing.size

      for (template in distinctTemplates) {
        // Check if task already exists on this date by templateId
        if (existingTemplateIds.contains(template.id)) {
          continue
        }
        // Check if task already exists by matching name and timeMinutes
        val existingMatch = existing.firstOrNull {
          !it.isExtra &&
            it.name.trim().equals(template.name.trim(), ignoreCase = true) &&
            it.timeMinutes == template.timeMinutes
        }

        if (existingMatch != null) {
          // If task already exists but had null templateId, link templateId instead of creating duplicate
          if (existingMatch.templateId == null) {
            safeDbWrite {
              taskDao.updateTask(existingMatch.copy(templateId = template.id))
            }
          }
          continue
        }

        // Genuinely missing task: create single row
        newTasksToInsert.add(
          DayTaskEntity(
            date = date,
            templateId = template.id,
            name = template.name,
            timeMinutes = template.timeMinutes,
            category = template.category,
            status = TaskStatus.MISSED,
            notes = template.notes,
            isExtra = false,
            orderIndex = startOrder + newTasksToInsert.size,
            priority = template.priority
          )
        )
      }

      if (newTasksToInsert.isNotEmpty()) {
        safeDbWrite { taskDao.insertTasks(newTasksToInsert) }
        deduplicateDayTasksForDate(date)
      }
      // Automatically persist the daily snapshot for this day
      syncDailySnapshot(date)
    }
  }

  /**
   * Safely deduplicates accidental double/duplicate tasks in day_tasks table for a specific date.
   * Preserves user's actual completion status (COMPLETE > PARTIAL > MISSED) and never deletes user-added tasks.
   */
  suspend fun deduplicateDayTasksForDate(date: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val tasks = taskDao.getTasksForDateSync(date)
      if (tasks.size <= 1) return@safeDbWrite

      // Group by logical activity identity (date + routine/extra + normalized name + timeMinutes)
      val groups = tasks.groupBy { task ->
        val typeTag = if (task.isExtra) "extra" else "routine"
        "${task.date}_${typeTag}_${task.name.trim().lowercase()}_${task.timeMinutes}"
      }

      for ((_, taskList) in groups) {
        if (taskList.size > 1) {
          // Retain survivor with highest status score (COMPLETE > PARTIAL > MISSED), then lowest id
          val survivor = taskList.sortedWith(
            compareByDescending<DayTaskEntity> { it.status.score }
              .thenBy { it.id }
          ).first()

          // If survivor has null templateId and another duplicate had templateId, inherit it
          val anyTemplateId = survivor.templateId ?: taskList.mapNotNull { it.templateId }.firstOrNull()
          val anyNotes = if (survivor.notes.isNotBlank()) survivor.notes else taskList.map { it.notes }.firstOrNull { it.isNotBlank() } ?: ""

          if (anyTemplateId != survivor.templateId || anyNotes != survivor.notes) {
            taskDao.updateTask(survivor.copy(templateId = anyTemplateId, notes = anyNotes))
          }

          for (dup in taskList) {
            if (dup.id != survivor.id) {
              taskDao.deleteTaskById(dup.id)
            }
          }
        }
      }

      // Second safety pass: group by templateId (if non-null)
      val linkedTasks = taskDao.getTasksForDateSync(date).filter { it.templateId != null }
      val templateGroups = linkedTasks.groupBy { "${it.date}_tmpl_${it.templateId}" }
      for ((_, taskList) in templateGroups) {
        if (taskList.size > 1) {
          val survivor = taskList.sortedWith(
            compareByDescending<DayTaskEntity> { it.status.score }
              .thenBy { it.id }
          ).first()
          for (dup in taskList) {
            if (dup.id != survivor.id) {
              taskDao.deleteTaskById(dup.id)
            }
          }
        }
      }
    }
  }

  /**
   * Safely deduplicates accidental double/duplicate tasks in day_tasks table across all stored dates.
   * Preserves user's actual completion status (COMPLETE > PARTIAL > MISSED) and never deletes user-added tasks.
   */
  suspend fun deduplicateDayTasks() = withContext(Dispatchers.IO) {
    safeDbWrite {
      val allTasks = taskDao.getAllTasksSync()
      val distinctDates = allTasks.map { it.date }.distinct()
      for (d in distinctDates) {
        deduplicateDayTasksForDate(d)
      }
    }
  }

  /**
   * Persists / updates the DailySnapshot for a given date in SQLite.
   */
  suspend fun syncDailySnapshot(date: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val tasks = taskDao.getTasksForDateSync(date)
      val settings = userSettingsDao.getSettingsSync()
      val anchor = settings?.anchorDate ?: date
      val cycleDay = TimeUtils.calculateCycleDay(anchor, date)

      val total = tasks.size
      val completed = tasks.count { it.status == TaskStatus.COMPLETE }
      val partial = tasks.count { it.status == TaskStatus.PARTIAL }
      val missed = tasks.count { it.status == TaskStatus.MISSED }
      val totalScore = (completed * 1.0f) + (partial * 0.5f)
      val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

      val snapshot = DailySnapshotEntity(
        date = date,
        dayOfCycle = cycleDay,
        totalTasks = total,
        completedCount = completed,
        partialCount = partial,
        missedCount = missed,
        totalScore = totalScore,
        completionPercentage = percentage,
        lastUpdated = System.currentTimeMillis()
      )
      dailySnapshotDao.insertOrUpdate(snapshot)
    }
  }

  /**
   * Scans all stored dates and guarantees that each day has a persistent snapshot in SQLite.
   */
  suspend fun syncAllHistoricalSnapshots() = withContext(Dispatchers.IO) {
    val allTasks = taskDao.getAllTasksSync()
    val distinctDates = allTasks.map { it.date }.distinct()
    for (d in distinctDates) {
      syncDailySnapshot(d)
    }
  }

  fun getTasksForDate(date: String): Flow<List<DayTaskEntity>> = taskDao.getTasksForDate(date)

  fun getDailySnapshot(date: String): Flow<DailySnapshotEntity?> = dailySnapshotDao.getSnapshotForDate(date)

  fun getAllDailySnapshots(): Flow<List<DailySnapshotEntity>> = dailySnapshotDao.getAllSnapshots()

  fun getOverallStats(): Flow<OverallStats> = dailySnapshotDao.getOverallStats()

  fun getCycleDayAverages(): Flow<List<CycleDayAverage>> = dailySnapshotDao.getCycleDayAverages()

  fun getCategoryAggregates(): Flow<List<CategoryAggregate>> = taskDao.getCategoryAggregates()

  fun getDaySummary(date: String, cycleDay: Int): Flow<DaySummary> {
    return taskDao.getTasksForDate(date).map { tasks ->
      val total = tasks.size
      val completed = tasks.count { it.status == TaskStatus.COMPLETE }
      val partial = tasks.count { it.status == TaskStatus.PARTIAL }
      val missed = tasks.count { it.status == TaskStatus.MISSED }
      val totalScore = (completed * 1.0f) + (partial * 0.5f) + (missed * 0.0f)
      val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

      DaySummary(
        date = date,
        dayOfCycle = cycleDay,
        totalTasks = total,
        completedCount = completed,
        partialCount = partial,
        missedCount = missed,
        totalScore = totalScore,
        completionPercentage = percentage
      )
    }
  }

  suspend fun updateTaskStatus(task: DayTaskEntity, newStatus: TaskStatus) = withContext(Dispatchers.IO) {
    safeDbWrite {
      taskDao.updateTask(task.copy(status = newStatus))
    }
    syncDailySnapshot(task.date)
  }

  suspend fun updateTask(task: DayTaskEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      taskDao.updateTask(task)
    }
    syncDailySnapshot(task.date)
  }

  suspend fun insertTask(task: DayTaskEntity): Long = withContext(Dispatchers.IO) {
    val id = safeDbWrite {
      taskDao.insertTask(task)
    }.getOrDefault(0L)
    syncDailySnapshot(task.date)
    id
  }

  suspend fun deleteTask(taskId: Long, date: String? = null) = withContext(Dispatchers.IO) {
    safeDbWrite {
      taskDao.deleteTaskById(taskId)
    }
    date?.let { syncDailySnapshot(it) }
  }

  // Routine templates
  fun getAllRoutineTemplates(): Flow<List<RoutineTemplateEntity>> = routineDao.getAllRoutineTemplates()

  suspend fun getRoutineTemplateById(id: Long): RoutineTemplateEntity? = withContext(Dispatchers.IO) {
    routineDao.getRoutineTemplateById(id)
  }

  suspend fun getRoutineTemplateByActivityKey(key: String): RoutineTemplateEntity? = withContext(Dispatchers.IO) {
    routineDao.getRoutineTemplateByActivityKey(key)
  }

  /**
   * Startup cleanup to ensure no duplicate default+custom routine rows exist.
   * If duplicates exist for the same logical activity key, preserves the user's
   * custom/active row and completed task history, and re-links day_tasks.
   */
  suspend fun deduplicateRoutineTemplatesAndRelinkDayTasks() = withContext(Dispatchers.IO) {
    safeDbWrite {
      val allTemplates = routineDao.getAllRoutineTemplatesSync()
      if (allTemplates.isEmpty()) return@safeDbWrite

      val groups = mutableMapOf<String, MutableList<RoutineTemplateEntity>>()
      for (template in allTemplates) {
        val key = if (template.activityKey.isNotBlank()) {
          template.activityKey
        } else {
          RoutineUtils.resolveActivityKey(template.name)
        }
        groups.getOrPut(key) { mutableListOf() }.add(template)
      }

      val usedKeys = mutableSetOf<String>()
      for ((key, templates) in groups) {
        if (templates.size > 1) {
          // Multiple rows for same logical activity!
          // Pick survivor: prefer active row, custom notes/time, or highest id (user-customized row)
          val survivor = templates.maxByOrNull { t ->
            var score = 0
            if (t.isActive) score += 20
            if (t.notes.isNotBlank()) score += 10
            if (t.id > 14) score += 5
            score
          } ?: templates.last()

          val finalKey = if (usedKeys.contains(key)) "${key}_${survivor.id}" else key
          usedKeys.add(finalKey)

          if (survivor.activityKey != finalKey) {
            routineDao.updateRoutineTemplate(survivor.copy(activityKey = finalKey))
          }

          // Re-link all day_tasks from duplicates to the surviving template row, preserving history
          for (duplicate in templates) {
            if (duplicate.id != survivor.id) {
              taskDao.reassignTemplateId(duplicate.id, survivor.id)
              routineDao.deleteRoutineTemplateById(duplicate.id)
            }
          }
        } else {
          val single = templates.first()
          val finalKey = if (usedKeys.contains(key)) "${key}_${single.id}" else key
          usedKeys.add(finalKey)
          if (single.activityKey != finalKey) {
            routineDao.updateRoutineTemplate(single.copy(activityKey = finalKey))
          }
        }
      }
    }
  }

  suspend fun insertRoutineTemplate(item: RoutineTemplateEntity): Long = withContext(Dispatchers.IO) {
    safeDbWrite {
      routineDao.insertRoutineTemplate(item)
    }.getOrDefault(0L)
  }

  suspend fun updateRoutineTemplate(item: RoutineTemplateEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      routineDao.updateRoutineTemplate(item)
    }
  }

  suspend fun deleteRoutineTemplate(id: Long) = withContext(Dispatchers.IO) {
    safeDbWrite {
      routineDao.deleteRoutineTemplateById(id)
    }
  }

  /**
   * Synchronizes today's day_tasks with customized routine templates in real time.
   * If an existing task's template time or details were modified, updates today's task.
   * If a new template was added, inserts it into today's task list.
   * If a template was deleted or deactivated, removes it from today if uncompleted.
   * Preserves user's historical records and completed statuses.
   */
  suspend fun syncTodayTasksWithTemplates(todayDate: String) = withContext(Dispatchers.IO) {
    val activeTemplates = routineDao.getActiveTemplatesSync()
    val existingTasks = taskDao.getTasksForDateSync(todayDate)

    val settings = userSettingsDao.getSettingsSync()
    val anchor = settings?.anchorDate ?: todayDate
    val cycleDay = TimeUtils.calculateCycleDay(anchor, todayDate)
    val maskBit = 1 shl (cycleDay - 1)
    val applicableTemplates = activeTemplates.filter { (it.daysMask and maskBit) != 0 }

    val activeTemplateMap = applicableTemplates.associateBy { it.id }
    val existingLinkedTasks = existingTasks.filter { !it.isExtra && it.templateId != null }

    safeDbWrite {
      // 1. Update existing tasks linked to templates
      for (task in existingLinkedTasks) {
        val template = activeTemplateMap[task.templateId]
        if (template != null) {
          val updatedTask = task.copy(
            name = template.name,
            timeMinutes = template.timeMinutes,
            category = template.category,
            notes = template.notes,
            orderIndex = template.orderIndex,
            priority = template.priority
          )
          if (updatedTask != task) {
            taskDao.updateTask(updatedTask)
          }
        } else {
          // Template was removed or disabled. If not marked done, clean up from today
          if (task.status == TaskStatus.MISSED) {
            taskDao.deleteTask(task)
          }
        }
      }

      // 2. Insert any newly added active templates for today (with deduplication)
      val existingTemplateIds = existingTasks.mapNotNull { it.templateId }.toSet()
      val distinctApplicable = applicableTemplates.distinctBy {
        if (it.activityKey.isNotBlank()) it.activityKey else it.name.trim().lowercase()
      }

      val newTasksToInsert = mutableListOf<DayTaskEntity>()
      for (template in distinctApplicable) {
        if (existingTemplateIds.contains(template.id)) continue

        val match = existingTasks.firstOrNull {
          !it.isExtra &&
            it.name.trim().equals(template.name.trim(), ignoreCase = true) &&
            it.timeMinutes == template.timeMinutes
        }

        if (match != null) {
          if (match.templateId == null) {
            taskDao.updateTask(match.copy(templateId = template.id))
          }
          continue
        }

        newTasksToInsert.add(
          DayTaskEntity(
            date = todayDate,
            templateId = template.id,
            name = template.name,
            timeMinutes = template.timeMinutes,
            category = template.category,
            status = TaskStatus.MISSED,
            notes = template.notes,
            isExtra = false,
            orderIndex = template.orderIndex,
            priority = template.priority
          )
        )
      }

      if (newTasksToInsert.isNotEmpty()) {
        taskDao.insertTasks(newTasksToInsert)
      }
      deduplicateDayTasksForDate(todayDate)
    }

    syncDailySnapshot(todayDate)
  }

  /**
   * Feature 7: Restores today's routine tasks from the permanent routine templates
   * without affecting historical data or extra tasks.
   */
  suspend fun restoreTodayTasksToDefault(date: String, cycleDay: Int) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val existingTasks = taskDao.getTasksForDateSync(date)
      // Delete existing routine tasks for this date
      for (task in existingTasks) {
        if (!task.isExtra) {
          taskDao.deleteTask(task)
        }
      }
      val templates = routineDao.getActiveTemplatesSync()
      val maskBit = 1 shl (cycleDay - 1)
      val applicableTemplates = templates.filter { (it.daysMask and maskBit) != 0 }
      val newTasks = applicableTemplates.mapIndexed { index, template ->
        DayTaskEntity(
          date = date,
          templateId = template.id,
          name = template.name,
          timeMinutes = template.timeMinutes,
          category = template.category,
          status = TaskStatus.MISSED,
          notes = template.notes,
          isExtra = false,
          orderIndex = index,
          priority = template.priority
        )
      }
      if (newTasks.isNotEmpty()) {
        taskDao.insertTasks(newTasks)
      }
    }
    syncDailySnapshot(date)
  }

  // Feature 3: Daily Notes & Mood
  fun getDailyNote(date: String): Flow<DailyNoteEntity?> = dailyNoteDao.getNoteForDate(date)

  suspend fun getDailyNoteSync(date: String): DailyNoteEntity? = dailyNoteDao.getNoteForDateSync(date)

  fun getAllDailyNotes(): Flow<List<DailyNoteEntity>> = dailyNoteDao.getAllNotes()

  fun getDailyNotesBetweenDates(startDate: String, endDate: String): Flow<List<DailyNoteEntity>> =
    dailyNoteDao.getNotesBetweenDates(startDate, endDate)

  suspend fun saveDailyNote(date: String, note: String, mood: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      dailyNoteDao.insertOrUpdate(
        DailyNoteEntity(
          date = date,
          note = note,
          mood = mood,
          updatedAt = System.currentTimeMillis()
        )
      )
    }
  }

  suspend fun updateSmartReminderMinutes(minutes: Int) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val current = userSettingsDao.getSettingsSync() ?: return@safeDbWrite
      userSettingsDao.insertOrUpdate(current.copy(smartReminderMinutes = minutes))
    }
  }

  // Reflections
  fun getAllReflections(): Flow<List<ReflectionEntity>> = reflectionDao.getAllReflections()

  suspend fun insertReflection(reflection: ReflectionEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      reflectionDao.insertReflection(reflection)
    }
  }

  suspend fun updateReflection(reflection: ReflectionEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      reflectionDao.updateReflection(reflection)
    }
  }

  suspend fun deleteReflection(id: Long) = withContext(Dispatchers.IO) {
    safeDbWrite {
      reflectionDao.deleteReflectionById(id)
    }
  }

  // Goals
  fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

  suspend fun insertGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      goalDao.insertGoal(goal)
    }
  }

  suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      goalDao.updateGoal(goal)
    }
  }

  suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
    safeDbWrite {
      goalDao.deleteGoalById(id)
    }
  }

  // User Settings
  fun getUserSettings(): Flow<UserSettingsEntity?> = userSettingsDao.getSettings()

  suspend fun updateAnchorDate(newAnchorDate: String) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = newAnchorDate)
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(anchorDate = newAnchorDate))
    }
    syncAllHistoricalSnapshots()
  }

  suspend fun updateWakeUpSchedule(
    newWakeUpMinutes: Int,
    isAlarmEnabled: Boolean,
    snoozeMinutes: Int = 10,
    updateFutureMasterRoutine: Boolean = true
  ) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    val updated = current.copy(
      wakeUpMinutes = newWakeUpMinutes,
      isAlarmEnabled = isAlarmEnabled,
      snoozeMinutes = snoozeMinutes
    )
    safeDbWrite {
      userSettingsDao.insertOrUpdate(updated)
    }

    if (updateFutureMasterRoutine) {
      val templates = routineDao.getAllRoutineTemplatesSync()
      val wakeUpTemplate = templates.firstOrNull { it.category.equals("Morning", ignoreCase = true) || it.orderIndex == 0 }
      if (wakeUpTemplate != null) {
        safeDbWrite {
          routineDao.updateRoutineTemplate(wakeUpTemplate.copy(timeMinutes = newWakeUpMinutes))
        }
      }
    }
  }

  suspend fun setAlarmEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(isAlarmEnabled = isEnabled))
    }
  }

  suspend fun updateAlarmSound(
    soundType: String,
    customUri: String?,
    soundTitle: String
  ) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(
        current.copy(
          alarmSoundType = soundType,
          customSoundUri = customUri,
          customSoundTitle = soundTitle
        )
      )
    }
  }

  suspend fun updateAlarmVolume(volume: Float) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(alarmVolume = volume.coerceIn(0.05f, 1.0f)))
    }
  }

  suspend fun updateVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(isVibrationEnabled = enabled))
    }
  }

  suspend fun updateHapticFeedbackEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(isHapticFeedbackEnabled = enabled))
    }
  }

  suspend fun updateAlarmSnoozeMinutes(snoozeMinutes: Int) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(snoozeMinutes = snoozeMinutes))
    }
  }

  suspend fun updateAlarmRepeat(repeatMode: String, customDaysMask: Int = 127) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(
        current.copy(
          alarmRepeatMode = repeatMode,
          alarmCustomDaysMask = customDaysMask
        )
      )
    }
  }

  fun getAllTasks(): Flow<List<DayTaskEntity>> = taskDao.getAllTasks()

  // Journalist Persons
  fun getAllJournalistPersons(): Flow<List<JournalistPersonEntity>> = journalistPersonDao.getAllPersons()

  suspend fun insertJournalistPerson(person: JournalistPersonEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistPersonDao.insertPerson(person)
    }
  }

  suspend fun updateJournalistPerson(person: JournalistPersonEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistPersonDao.updatePerson(person)
    }
  }

  suspend fun deleteJournalistPerson(id: String) = withContext(Dispatchers.IO) {
    if (id == "self") return@withContext // Self can never be deleted
    safeDbWrite {
      journalistPersonDao.deletePersonById(id)
      journalistEntryDao.deleteEntriesForPerson(id)
    }
  }

  // Journalist Entries
  fun getAllJournalistEntries(): Flow<List<JournalistEntryEntity>> = journalistEntryDao.getAllEntries()

  fun getJournalistEntriesForPerson(personId: String): Flow<List<JournalistEntryEntity>> =
    journalistEntryDao.getEntriesForPerson(personId)

  suspend fun insertJournalistEntry(entry: JournalistEntryEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistEntryDao.insertEntry(entry)
    }
  }

  suspend fun updateJournalistEntry(entry: JournalistEntryEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistEntryDao.updateEntry(entry)
    }
  }

  suspend fun deleteJournalistEntry(id: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistEntryDao.deleteEntryById(id)
    }
  }

  // Meditation Persistence & Settings
  fun getAllMeditationSessions(): Flow<List<MeditationSessionEntity>> =
    meditationDao.getAllSessions()

  fun getMeditationSessionsForDate(date: String): Flow<List<MeditationSessionEntity>> =
    meditationDao.getSessionsForDate(date)

  fun getMeditationSessionsBetweenDates(startDate: String, endDate: String): Flow<List<MeditationSessionEntity>> =
    meditationDao.getSessionsBetweenDates(startDate, endDate)

  suspend fun insertMeditationSession(session: MeditationSessionEntity): Long = withContext(Dispatchers.IO) {
    var insertedId = 0L
    safeDbWrite {
      insertedId = meditationDao.insertSession(session)
    }
    insertedId
  }

  suspend fun updateUserName(name: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val current = userSettingsDao.getSettingsSync() ?: return@safeDbWrite
      userSettingsDao.insertOrUpdate(current.copy(userName = name.trim()))
    }
  }

  suspend fun updateMeditationSettings(chimeEnabled: Boolean, language: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val current = userSettingsDao.getSettingsSync() ?: return@safeDbWrite
      userSettingsDao.insertOrUpdate(current.copy(meditationChimeEnabled = chimeEnabled, meditationVoiceLanguage = language))
    }
  }

  suspend fun updateFeatureToggle(feature: String, enabled: Boolean) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val current = userSettingsDao.getSettingsSync() ?: return@safeDbWrite
      val updated = when (feature) {
        "QUICK_ADD" -> current.copy(enableQuickAdd = enabled)
        "MORNING_BRIEF" -> current.copy(enableMorningBrief = enabled)
        "LIFE_TIMELINE" -> current.copy(enableLifeTimeline = enabled)
        "PERSONAL_INSIGHTS" -> current.copy(enablePersonalInsights = enabled)
        "RECOVERY_MODE" -> current.copy(enableRecoveryMode = enabled)
        else -> current
      }
      userSettingsDao.insertOrUpdate(updated)
    }
  }

  suspend fun applyEmergencyRecoveryTasks(targetDate: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val recoveryTasks = listOf(
        DayTaskEntity(
          date = targetDate,
          templateId = null,
          name = "🌅 Gentle Wake Up & Hydration",
          timeMinutes = 330, // 5:30 AM
          category = "Health",
          status = TaskStatus.MISSED,
          notes = "Recovery Mode: Drink warm water, stretch gently without rush",
          isExtra = true,
          orderIndex = 1,
          priority = "IMPORTANT"
        ),
        DayTaskEntity(
          date = targetDate,
          templateId = null,
          name = "🧘 10 Min Breathing Meditation (श्वास ध्यान)",
          timeMinutes = 360, // 6:00 AM
          category = "Mindfulness",
          status = TaskStatus.MISSED,
          notes = "Recovery Mode: Calming box breathing to reset nervous system",
          isExtra = true,
          orderIndex = 2,
          priority = "HIGH"
        ),
        DayTaskEntity(
          date = targetDate,
          templateId = null,
          name = "🎯 One Essential Priority Focus",
          timeMinutes = 570, // 9:30 AM
          category = "Deep Work",
          status = TaskStatus.MISSED,
          notes = "Recovery Mode: Focus on just 1 critical goal today to regain momentum",
          isExtra = true,
          orderIndex = 3,
          priority = "HIGH"
        ),
        DayTaskEntity(
          date = targetDate,
          templateId = null,
          name = "🚶 Restorative Walk & Fresh Air",
          timeMinutes = 1050, // 5:30 PM
          category = "Health",
          status = TaskStatus.MISSED,
          notes = "Recovery Mode: 20 min mindful outdoor walking",
          isExtra = true,
          orderIndex = 4,
          priority = "NORMAL"
        ),
        DayTaskEntity(
          date = targetDate,
          templateId = null,
          name = "🌙 Early Restorative Sleep",
          timeMinutes = 1320, // 10:00 PM
          category = "Sleep",
          status = TaskStatus.MISSED,
          notes = "Recovery Mode: Sleep early to rebuild physical and mental energy",
          isExtra = true,
          orderIndex = 5,
          priority = "IMPORTANT"
        )
      )
      taskDao.insertTasks(recoveryTasks)
    }
  }

  // Energy Mode Persistence
  suspend fun updateDailyEnergyMode(mode: String, date: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val s = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
      userSettingsDao.insertOrUpdate(
        s.copy(
          dailyEnergyMode = mode,
          dailyEnergyModeDate = date
        )
      )
    }
  }

  suspend fun updateDailyChallengeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val s = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
      userSettingsDao.insertOrUpdate(s.copy(dailyChallengeEnabled = enabled))
    }
  }

  suspend fun updateMusicAutoRoutineEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val s = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
      userSettingsDao.insertOrUpdate(s.copy(musicAutoRoutineEnabled = enabled))
    }
  }

  // Daily Challenge Methods
  fun getDailyChallengeFlow(date: String) = dailyChallengeDao.getChallengeForDate(date)

  suspend fun getOrCreateTodayChallenge(context: android.content.Context, date: String) =
    com.example.challenge.DailyChallengeManager.getOrCreateTodayChallenge(context, date)

  suspend fun markDailyChallengeComplete(context: android.content.Context, date: String) =
    com.example.challenge.DailyChallengeManager.completeChallenge(context, date)

  suspend fun skipDailyChallenge(context: android.content.Context, date: String) =
    com.example.challenge.DailyChallengeManager.skipOrSwapChallenge(context, date)

  // Music System Methods
  fun getAllSongsFlow() = musicDao.getAllSongs()
  fun getFavoriteSongsFlow() = musicDao.getFavoriteSongs()
  fun getRecentlyPlayedSongsFlow() = musicDao.getRecentlyPlayedSongs()
  fun getAllPlaylistsFlow() = musicDao.getAllPlaylists()
  fun getSongsForPlaylistFlow(playlistId: Long) = musicDao.getSongsForPlaylist(playlistId)
  fun getAllPlaylistRulesFlow() = musicDao.getAllRules()

  suspend fun createPlaylist(name: String, description: String, colorHex: String): Long {
    return musicDao.insertPlaylist(
      com.example.data.model.PlaylistEntity(
        name = name.trim(),
        description = description.trim(),
        colorHex = colorHex
      )
    )
  }

  suspend fun deletePlaylist(playlistId: Long) {
    musicDao.clearPlaylistSongs(playlistId)
    musicDao.deletePlaylist(playlistId)
  }

  suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
    musicDao.addSongToPlaylist(
      com.example.data.model.PlaylistSongCrossRef(
        playlistId = playlistId,
        songId = songId
      )
    )
  }

  suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
    musicDao.removeSongFromPlaylist(playlistId, songId)
  }

  suspend fun addPlaylistRule(rule: com.example.data.model.PlaylistRuleEntity): Long {
    return musicDao.insertRule(rule)
  }

  suspend fun deletePlaylistRule(ruleId: Long) {
    musicDao.deleteRule(ruleId)
  }

  suspend fun toggleSongFavorite(songId: String, isFav: Boolean) {
    musicDao.updateFavoriteStatus(songId, isFav)
  }

  // --- SHORT CONTENT TRACKER METHODS ---

  fun getTodayShortContentSummary(date: String): Flow<com.example.data.model.ShortContentDailySummaryEntity?> {
    return shortContentDao.getDailySummary(date)
  }

  suspend fun getTodayShortContentSummaryDirect(date: String): com.example.data.model.ShortContentDailySummaryEntity? {
    return shortContentDao.getDailySummaryDirect(date)
  }

  fun getShortContentSummariesBetween(startDate: String, endDate: String): Flow<List<com.example.data.model.ShortContentDailySummaryEntity>> {
    return shortContentDao.getSummariesBetween(startDate, endDate)
  }

  fun getAllShortContentSummaries(): Flow<List<com.example.data.model.ShortContentDailySummaryEntity>> {
    return shortContentDao.getAllSummaries()
  }

  suspend fun updateShortContentSettings(
    limit: Int,
    w50: Boolean,
    w80: Boolean,
    w100: Boolean,
    focusLock: Boolean,
    enabled: Boolean
  ) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    val updated = current.copy(
      shortDailyLimit = limit,
      shortWarning50Enabled = w50,
      shortWarning80Enabled = w80,
      shortWarning100Enabled = w100,
      shortFocusLockIntegration = focusLock,
      shortTrackingEnabled = enabled
    )
    userSettingsDao.insertOrUpdate(updated)

    // Also update today's summary limit if it exists
    val today = TimeUtils.getTodayDateString()
    val todaySummary = shortContentDao.getDailySummaryDirect(today)
    if (todaySummary != null) {
      shortContentDao.insertOrUpdateSummary(todaySummary.copy(dailyLimit = limit))
    }
  }

  suspend fun resetShortContentToday(date: String) = safeDbWrite {
    val current = shortContentDao.getDailySummaryDirect(date)
    val settings = userSettingsDao.getSettingsDirect()
    val limit = settings?.shortDailyLimit ?: 20
    val reset = com.example.data.model.ShortContentDailySummaryEntity(
      date = date,
      instagramCount = 0,
      youtubeCount = 0,
      facebookCount = 0,
      otherCount = 0,
      totalCount = 0,
      totalTimeSeconds = 0L,
      dailyLimit = limit,
      lastUpdated = System.currentTimeMillis()
    )
    shortContentDao.insertOrUpdateSummary(reset)
  }

  fun calculateShortContentPeriodStats(summaries: List<com.example.data.model.ShortContentDailySummaryEntity>): com.example.data.model.ShortContentPeriodStats {
    if (summaries.isEmpty()) return com.example.data.model.ShortContentPeriodStats()
    val totalCount = summaries.sumOf { it.totalCount }
    val totalSeconds = summaries.sumOf { it.totalTimeSeconds }
    val ig = summaries.sumOf { it.instagramCount }
    val yt = summaries.sumOf { it.youtubeCount }
    val fb = summaries.sumOf { it.facebookCount }
    val other = summaries.sumOf { it.otherCount }
    val activeDays = summaries.count { it.totalCount > 0 }
    val limitExceeded = summaries.count { it.isLimitReached }
    val avg = if (summaries.isNotEmpty()) totalCount.toFloat() / summaries.size.toFloat() else 0f

    return com.example.data.model.ShortContentPeriodStats(
      totalCount = totalCount,
      totalTimeMinutes = (totalSeconds / 60).toInt(),
      instagramCount = ig,
      youtubeCount = yt,
      facebookCount = fb,
      otherCount = other,
      daysActive = activeDays,
      dailyAverageCount = avg,
      limitExceededDays = limitExceeded
    )
  }

  // --- PERSONAL DASHBOARD BUILDER METHODS ---

  suspend fun updateDashboardConfiguration(
    preset: String,
    orderedSections: String,
    disabledSections: String
  ) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    val updated = current.copy(
      dashboardPreset = preset,
      dashboardSectionsOrder = orderedSections,
      dashboardDisabledSections = disabledSections
    )
    userSettingsDao.insertOrUpdate(updated)
  }

  // --- FOCUS / PHONE RESTRICTION MODE METHODS ---

  suspend fun updateFocusModeActive(isActive: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(isFocusModeActive = isActive))
  }

  suspend fun updateFocusSchedule(
    isEnabled: Boolean,
    startTimeMinutes: Int,
    endTimeMinutes: Int
  ) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    val updated = current.copy(
      isFocusScheduleEnabled = isEnabled,
      focusStartTimeMinutes = startTimeMinutes,
      focusEndTimeMinutes = endTimeMinutes
    )
    userSettingsDao.insertOrUpdate(updated)
  }

  fun getAllStrictLockEvents(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.StrictLockEventEntity>> =
    strictLockDao.getAllEvents()

  fun getRecentStrictLockEvents(): kotlinx.coroutines.flow.Flow<List<com.example.data.model.StrictLockEventEntity>> =
    strictLockDao.getRecentEvents()

  suspend fun countStrictLockEvents(): Int = withContext(Dispatchers.IO) {
    try {
      strictLockDao.countEvents()
    } catch (e: Exception) {
      0
    }
  }

  suspend fun recordStrictLockEvent(event: com.example.data.model.StrictLockEventEntity): Long = safeDbWrite {
    strictLockDao.insertEvent(event)
  }.getOrDefault(-1L)

  // --- SYSTEM-WIDE OLED BLACK SCREEN MODE METHODS ---

  suspend fun updateBlackScreenEnabled(isEnabled: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(isBlackScreenEnabled = isEnabled))
  }

  suspend fun updateBlackScreenOverlayActive(isActive: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(isBlackScreenOverlayActive = isActive))
  }

  suspend fun updateBlackScreenFloatingDotEnabled(isEnabled: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(isFloatingDotEnabled = isEnabled))
  }

  suspend fun updateBlackScreenDotPosition(x: Int, y: Int) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenDotX = x, blackScreenDotY = y))
  }

  suspend fun updateBlackScreenDotSize(size: Int) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenDotSize = size))
  }

  suspend fun updateBlackScreenDotOpacity(opacity: Float) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenDotOpacity = opacity))
  }

  suspend fun updateBlackScreenActivationMethod(method: String) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenActivationMethod = method))
  }

  suspend fun updateBlackScreenExitGesture(gesture: String) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenExitGesture = gesture))
  }

  suspend fun updateBlackScreenGestureSensitivity(sensitivity: Float) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenGestureSensitivity = sensitivity))
  }

  suspend fun updateBlackScreenShowClock(show: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenShowClock = show))
  }

  suspend fun updateBlackScreenClockFormat24(is24Hour: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenClockFormat24 = is24Hour))
  }

  suspend fun updateBlackScreenRestoreAfterUnlock(restore: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenRestoreAfterUnlock = restore))
  }

  suspend fun updateBlackScreenRestoreAfterReboot(restore: Boolean) = safeDbWrite {
    val current = userSettingsDao.getSettingsDirect() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    userSettingsDao.insertOrUpdate(current.copy(blackScreenRestoreAfterReboot = restore))
  }

  // --- DATA RESET & EXISTING USER DATA DETECTION ---

  /**
   * Determines whether the app has meaningful existing user data (tasks, notes, reflections, snapshots, etc.)
   */
  suspend fun hasExistingUserData(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
      val dbFile = context.getDatabasePath("life_tracker_db")
      if (!dbFile.exists() || dbFile.length() == 0L) return@withContext false

      val tasks = taskDao.getAllTasksSync().size
      val routines = routineDao.countRoutineTemplates()
      val snapshots = dailySnapshotDao.getAllSnapshotsSync().size
      val notes = dailyNoteDao.getAllNotesSync().size
      val reflections = reflectionDao.getAllReflectionsSync().size
      tasks > 0 || routines > 0 || snapshots > 0 || notes > 0 || reflections > 0
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Production-safe app reset to start as a fresh user.
   * Takes a local safety backup first, clears all Room tables, and re-initializes pristine default routines.
   */
  suspend fun resetAppToFreshState(context: Context, todayDate: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      // 1. Create a local safety backup snapshot before resetting
      com.example.backup.RestoreManager.createLocalSafetyBackup(context)

      // 2. Clear all Room entity tables
      database.clearAllTables()

      // 3. Re-initialize defaults (default 11 routines, default settings)
      initializeDefaultsIfNeeded(todayDate)

      // 4. Update system alarms and sync services
      try {
        com.example.alarm.AlarmScheduler.createNotificationChannels(context)
        com.example.alarm.AlarmScheduler.rescheduleAllTimetableAlarms(context)
        com.example.focus.FocusModeManager.onBootOrScheduleChange(context)
        com.example.blackscreen.BlackScreenManager.syncServiceState(context)
      } catch (_: Exception) {}

      com.example.backup.BackupPreferences.setFirstLaunchHandled(context, true)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // =========================================================
  // EXPENSE DIARY REPOSITORY METHODS
  // =========================================================

  suspend fun initializeExpenseDefaultsIfNeeded() = withContext(Dispatchers.IO) {
    try {
      val existingCategories = expenseDao.getAllCategoriesSync()
      if (existingCategories.isEmpty()) {
        expenseDao.insertCategories(com.example.data.model.ExpenseDefaults.DEFAULT_CATEGORIES)
      }
      val existingChips = expenseDao.getAllQuickChipsSync()
      if (existingChips.isEmpty()) {
        expenseDao.insertQuickChips(com.example.data.model.ExpenseDefaults.DEFAULT_QUICK_CHIPS)
      }
    } catch (_: Exception) {}
  }

  fun getAllExpenses(): Flow<List<com.example.data.model.ExpenseEntity>> =
    expenseDao.getAllExpenses()

  fun getTodayExpenses(date: String): Flow<List<com.example.data.model.ExpenseEntity>> =
    expenseDao.getExpensesForDate(date)

  fun getTodayTotalSpend(date: String): Flow<Double> =
    expenseDao.getTodayTotalFlow(date)

  fun getExpensesBetweenDates(startDate: String, endDate: String): Flow<List<com.example.data.model.ExpenseEntity>> =
    expenseDao.getExpensesBetweenDates(startDate, endDate)

  suspend fun getExpensesBetweenDatesSync(startDate: String, endDate: String): List<com.example.data.model.ExpenseEntity> =
    expenseDao.getExpensesBetweenDatesSync(startDate, endDate)

  suspend fun addExpense(expense: com.example.data.model.ExpenseEntity): Result<Long> = safeDbWrite {
    val id = expenseDao.insertExpense(expense)
    try {
      expenseDao.bumpQuickChipUsage(expense.note, expense.amount)
    } catch (_: Exception) {}
    id
  }

  suspend fun updateExpense(expense: com.example.data.model.ExpenseEntity): Result<Unit> = safeDbWrite {
    expenseDao.updateExpense(expense)
  }

  suspend fun deleteExpense(expense: com.example.data.model.ExpenseEntity): Result<Unit> = safeDbWrite {
    expenseDao.deleteExpense(expense)
  }

  suspend fun deleteExpenseById(id: Long): Result<Unit> = safeDbWrite {
    expenseDao.deleteExpenseById(id)
  }

  fun getAllQuickChips(): Flow<List<com.example.data.model.ExpenseQuickChipEntity>> =
    expenseDao.getAllQuickChips()

  fun getAllExpenseCategories(): Flow<List<com.example.data.model.ExpenseCategoryEntity>> =
    expenseDao.getAllCategories()

  suspend fun getAllExpenseCategoriesSync(): List<com.example.data.model.ExpenseCategoryEntity> =
    expenseDao.getAllCategoriesSync()

  suspend fun updateExpenseCategory(category: com.example.data.model.ExpenseCategoryEntity): Result<Unit> = safeDbWrite {
    expenseDao.updateCategory(category)
  }

  suspend fun deleteExpenseCategory(category: com.example.data.model.ExpenseCategoryEntity): Result<Unit> = safeDbWrite {
    expenseDao.deleteCategory(category)
  }

  suspend fun updateQuickChip(chip: com.example.data.model.ExpenseQuickChipEntity): Result<Unit> = safeDbWrite {
    expenseDao.updateQuickChip(chip)
  }

  // --- Recurring Expenses ---
  fun getAllRecurringExpenses(): Flow<List<com.example.data.model.RecurringExpenseEntity>> =
    expenseDao.getAllRecurringExpenses()

  suspend fun getAllRecurringExpensesSync(): List<com.example.data.model.RecurringExpenseEntity> =
    expenseDao.getAllRecurringExpensesSync()

  suspend fun addRecurringExpense(recurring: com.example.data.model.RecurringExpenseEntity): Result<Long> = safeDbWrite {
    expenseDao.insertRecurringExpense(recurring)
  }

  suspend fun updateRecurringExpense(recurring: com.example.data.model.RecurringExpenseEntity): Result<Unit> = safeDbWrite {
    expenseDao.updateRecurringExpense(recurring)
  }

  suspend fun deleteRecurringExpense(recurring: com.example.data.model.RecurringExpenseEntity): Result<Unit> = safeDbWrite {
    expenseDao.deleteRecurringExpense(recurring)
  }

  /**
   * Checks all recurring expenses against today's date.
   * If today's day of month >= recurring.dayOfMonth and not added for current month,
   * automatically generates the expense entry!
   */
  suspend fun checkAndTriggerRecurringExpenses(todayDateStr: String): List<com.example.data.model.ExpenseEntity> = withContext(Dispatchers.IO) {
    val addedExpenses = mutableListOf<com.example.data.model.ExpenseEntity>()
    try {
      val recurringList = expenseDao.getAllRecurringExpensesSync().filter { it.isEnabled }
      val currentMonth = todayDateStr.substring(0, 7.coerceAtMost(todayDateStr.length)) // "YYYY-MM"
      val todayDay = todayDateStr.split("-").getOrNull(2)?.toIntOrNull() ?: 1

      for (rec in recurringList) {
        if (rec.lastAddedMonth != currentMonth && todayDay >= rec.dayOfMonth) {
          val expense = com.example.data.model.ExpenseEntity(
            amount = rec.amount,
            note = "${rec.title} (आवर्ती)",
            category = rec.category,
            categoryEmoji = rec.categoryEmoji,
            categoryColorHex = rec.categoryColorHex,
            date = todayDateStr,
            time = "09:00 AM",
            timestamp = System.currentTimeMillis(),
            mood = "😌"
          )
          expenseDao.insertExpense(expense)
          expenseDao.updateRecurringExpense(rec.copy(lastAddedMonth = currentMonth))
          addedExpenses.add(expense)
        }
      }
    } catch (_: Exception) {}
    addedExpenses
  }
}
