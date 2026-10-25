package com.example.util

import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistCategory
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.ReflectionCategory
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.TaskStatus
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object ExportImportHelper {

  suspend fun generateJsonBackup(database: LifeTrackerDatabase): String = withContext(Dispatchers.IO) {
    val tasks = database.taskDao().getAllTasksSync()
    val routines = database.routineDao().getAllRoutineTemplatesSync()
    val reflections = database.reflectionDao().getAllReflectionsSync()
    val goals = database.goalDao().getAllGoalsSync()
    val settings = database.userSettingsDao().getSettingsSync()

    val root = JSONObject()
    root.put("version", 1)
    root.put("exportDate", TimeUtils.getTodayDateString())

    // Settings
    val settingsObj = JSONObject()
    settingsObj.put("anchorDate", settings?.anchorDate ?: TimeUtils.getTodayDateString())
    settingsObj.put("wakeUpMinutes", settings?.wakeUpMinutes ?: 300)
    settingsObj.put("isAlarmEnabled", settings?.isAlarmEnabled ?: false)
    settingsObj.put("snoozeMinutes", settings?.snoozeMinutes ?: 10)
    settingsObj.put("alarmSoundType", settings?.alarmSoundType ?: "DEFAULT")
    settingsObj.put("customSoundTitle", settings?.customSoundTitle ?: "High-Tone Digital Alarm")
    settingsObj.put("alarmVolume", (settings?.alarmVolume ?: 1.0f).toDouble())
    settingsObj.put("isVibrationEnabled", settings?.isVibrationEnabled ?: true)
    settingsObj.put("alarmRepeatMode", settings?.alarmRepeatMode ?: "DAILY")
    settingsObj.put("alarmCustomDaysMask", settings?.alarmCustomDaysMask ?: 127)
    root.put("settings", settingsObj)

    // Routine Templates
    val routinesArray = JSONArray()
    for (r in routines) {
      val item = JSONObject()
      item.put("name", r.name)
      item.put("timeMinutes", r.timeMinutes)
      item.put("category", r.category)
      item.put("notes", r.notes)
      item.put("daysMask", r.daysMask)
      item.put("isActive", r.isActive)
      item.put("orderIndex", r.orderIndex)
      routinesArray.put(item)
    }
    root.put("routineTemplates", routinesArray)

    // Day Tasks
    val tasksArray = JSONArray()
    for (t in tasks) {
      val item = JSONObject()
      item.put("date", t.date)
      item.put("name", t.name)
      item.put("timeMinutes", t.timeMinutes)
      item.put("category", t.category)
      item.put("status", t.status.name)
      item.put("notes", t.notes)
      item.put("isExtra", t.isExtra)
      item.put("orderIndex", t.orderIndex)
      tasksArray.put(item)
    }
    root.put("dayTasks", tasksArray)

    // Reflections
    val reflectionsArray = JSONArray()
    for (rf in reflections) {
      val item = JSONObject()
      item.put("title", rf.title)
      item.put("description", rf.description)
      item.put("date", rf.date)
      item.put("timestamp", rf.timestamp)
      item.put("category", rf.category.name)
      item.put("tags", rf.tags)
      reflectionsArray.put(item)
    }
    root.put("reflections", reflectionsArray)

    // Goals
    val goalsArray = JSONArray()
    for (g in goals) {
      val item = JSONObject()
      item.put("title", g.title)
      item.put("description", g.description)
      item.put("progress", g.progress)
      item.put("deadline", g.deadline ?: "")
      item.put("isCompleted", g.isCompleted)
      item.put("createdAt", g.createdAt)
      goalsArray.put(item)
    }
    root.put("goals", goalsArray)

    // Daily Snapshots
    val snapshots = database.dailySnapshotDao().getAllSnapshotsSync()
    val snapshotsArray = JSONArray()
    for (sn in snapshots) {
      val item = JSONObject()
      item.put("date", sn.date)
      item.put("dayOfCycle", sn.dayOfCycle)
      item.put("totalTasks", sn.totalTasks)
      item.put("completedCount", sn.completedCount)
      item.put("partialCount", sn.partialCount)
      item.put("missedCount", sn.missedCount)
      item.put("totalScore", sn.totalScore)
      item.put("completionPercentage", sn.completionPercentage)
      item.put("lastUpdated", sn.lastUpdated)
      snapshotsArray.put(item)
    }
    root.put("dailySnapshots", snapshotsArray)

    // Journalist Persons
    val persons = database.journalistPersonDao().getAllPersonsSync()
    val personsArray = JSONArray()
    for (p in persons) {
      val item = JSONObject()
      item.put("id", p.id)
      item.put("name", p.name)
      item.put("emoji", p.emoji)
      item.put("createdAt", p.createdAt)
      personsArray.put(item)
    }
    root.put("journalistPersons", personsArray)

    // Journalist Entries
    val jEntries = database.journalistEntryDao().getAllEntriesSync()
    val jEntriesArray = JSONArray()
    for (e in jEntries) {
      val item = JSONObject()
      item.put("id", e.id)
      item.put("personId", e.personId)
      item.put("personName", e.personName)
      item.put("category", e.category.name)
      item.put("text", e.text)
      item.put("timestamp", e.timestamp)
      item.put("date", e.date)
      item.put("time", e.time)
      item.put("context", e.context)
      item.put("intensity", e.intensity)
      item.put("createdAt", e.createdAt)
      item.put("updatedAt", e.updatedAt)
      jEntriesArray.put(item)
    }
    root.put("journalistEntries", jEntriesArray)

    root.toString(2)
  }

  suspend fun restoreFromJson(database: LifeTrackerDatabase, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val root = JSONObject(jsonString)

      // Restore Settings
      if (root.has("settings")) {
        val s = root.getJSONObject("settings")
        val current = database.userSettingsDao().getSettingsSync()
        val anchorDate = s.optString("anchorDate", current?.anchorDate ?: TimeUtils.getTodayDateString())
        val wakeUpMinutes = s.optInt("wakeUpMinutes", current?.wakeUpMinutes ?: 300)
        val isAlarmEnabled = s.optBoolean("isAlarmEnabled", current?.isAlarmEnabled ?: false)
        val snoozeMinutes = s.optInt("snoozeMinutes", current?.snoozeMinutes ?: 10)
        val alarmSoundType = s.optString("alarmSoundType", current?.alarmSoundType ?: "DEFAULT")
        val customSoundTitle = s.optString("customSoundTitle", current?.customSoundTitle ?: "High-Tone Digital Alarm")
        val alarmVolume = s.optDouble("alarmVolume", (current?.alarmVolume ?: 1.0f).toDouble()).toFloat()
        val isVibrationEnabled = s.optBoolean("isVibrationEnabled", current?.isVibrationEnabled ?: true)
        val alarmRepeatMode = s.optString("alarmRepeatMode", current?.alarmRepeatMode ?: "DAILY")
        val alarmCustomDaysMask = s.optInt("alarmCustomDaysMask", current?.alarmCustomDaysMask ?: 127)

        database.userSettingsDao().insertOrUpdate(
          UserSettingsEntity(
            id = 1,
            anchorDate = anchorDate,
            wakeUpMinutes = wakeUpMinutes,
            isAlarmEnabled = isAlarmEnabled,
            snoozeMinutes = snoozeMinutes,
            alarmSoundType = alarmSoundType,
            customSoundUri = current?.customSoundUri,
            customSoundTitle = customSoundTitle,
            alarmVolume = alarmVolume,
            isVibrationEnabled = isVibrationEnabled,
            alarmRepeatMode = alarmRepeatMode,
            alarmCustomDaysMask = alarmCustomDaysMask
          )
        )
      }

      var importedRoutinesCount = 0
      if (root.has("routineTemplates")) {
        val rArray = root.getJSONArray("routineTemplates")
        val newRoutines = mutableListOf<RoutineTemplateEntity>()
        for (i in 0 until rArray.length()) {
          val obj = rArray.getJSONObject(i)
          newRoutines.add(
            RoutineTemplateEntity(
              name = obj.getString("name"),
              timeMinutes = obj.getInt("timeMinutes"),
              category = obj.optString("category", "Routine"),
              notes = obj.optString("notes", ""),
              daysMask = obj.optInt("daysMask", 127),
              isActive = obj.optBoolean("isActive", true),
              orderIndex = obj.optInt("orderIndex", i)
            )
          )
        }
        if (newRoutines.isNotEmpty()) {
          database.routineDao().insertRoutineTemplates(newRoutines)
          importedRoutinesCount = newRoutines.size
        }
      }

      var importedTasksCount = 0
      if (root.has("dayTasks")) {
        val tArray = root.getJSONArray("dayTasks")
        val newTasks = mutableListOf<DayTaskEntity>()
        for (i in 0 until tArray.length()) {
          val obj = tArray.getJSONObject(i)
          newTasks.add(
            DayTaskEntity(
              date = obj.getString("date"),
              name = obj.getString("name"),
              timeMinutes = obj.getInt("timeMinutes"),
              category = obj.optString("category", "Routine"),
              status = TaskStatus.fromString(obj.optString("status", "MISSED")),
              notes = obj.optString("notes", ""),
              isExtra = obj.optBoolean("isExtra", false),
              orderIndex = obj.optInt("orderIndex", i)
            )
          )
        }
        if (newTasks.isNotEmpty()) {
          database.taskDao().insertTasks(newTasks)
          importedTasksCount = newTasks.size
        }
      }

      var importedReflectionsCount = 0
      if (root.has("reflections")) {
        val rfArray = root.getJSONArray("reflections")
        val newReflections = mutableListOf<ReflectionEntity>()
        for (i in 0 until rfArray.length()) {
          val obj = rfArray.getJSONObject(i)
          newReflections.add(
            ReflectionEntity(
              title = obj.getString("title"),
              description = obj.optString("description", ""),
              date = obj.getString("date"),
              timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
              category = ReflectionCategory.fromString(obj.optString("category", "OBSERVATION")),
              tags = obj.optString("tags", "")
            )
          )
        }
        if (newReflections.isNotEmpty()) {
          database.reflectionDao().insertReflections(newReflections)
          importedReflectionsCount = newReflections.size
        }
      }

      var importedGoalsCount = 0
      if (root.has("goals")) {
        val gArray = root.getJSONArray("goals")
        val newGoals = mutableListOf<GoalEntity>()
        for (i in 0 until gArray.length()) {
          val obj = gArray.getJSONObject(i)
          newGoals.add(
            GoalEntity(
              title = obj.getString("title"),
              description = obj.optString("description", ""),
              progress = obj.optInt("progress", 0),
              deadline = obj.optString("deadline").ifEmpty { null },
              isCompleted = obj.optBoolean("isCompleted", false),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
          )
        }
        if (newGoals.isNotEmpty()) {
          database.goalDao().insertGoals(newGoals)
          importedGoalsCount = newGoals.size
        }
      }

      var importedSnapshotsCount = 0
      if (root.has("dailySnapshots")) {
        val sArray = root.getJSONArray("dailySnapshots")
        val newSnapshots = mutableListOf<DailySnapshotEntity>()
        for (i in 0 until sArray.length()) {
          val obj = sArray.getJSONObject(i)
          newSnapshots.add(
            DailySnapshotEntity(
              date = obj.getString("date"),
              dayOfCycle = obj.optInt("dayOfCycle", 1),
              totalTasks = obj.optInt("totalTasks", 0),
              completedCount = obj.optInt("completedCount", 0),
              partialCount = obj.optInt("partialCount", 0),
              missedCount = obj.optInt("missedCount", 0),
              totalScore = obj.optDouble("totalScore", 0.0).toFloat(),
              completionPercentage = obj.optInt("completionPercentage", 0),
              lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
            )
          )
        }
        if (newSnapshots.isNotEmpty()) {
          database.dailySnapshotDao().insertOrUpdateAll(newSnapshots)
          importedSnapshotsCount = newSnapshots.size
        }
      }

      var importedPersonsCount = 0
      if (root.has("journalistPersons")) {
        val pArray = root.getJSONArray("journalistPersons")
        val newPersons = mutableListOf<JournalistPersonEntity>()
        for (i in 0 until pArray.length()) {
          val obj = pArray.getJSONObject(i)
          newPersons.add(
            JournalistPersonEntity(
              id = obj.getString("id"),
              name = obj.getString("name"),
              emoji = obj.optString("emoji", "👤"),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
          )
        }
        if (newPersons.isNotEmpty()) {
          database.journalistPersonDao().insertPersons(newPersons)
          importedPersonsCount = newPersons.size
        }
      }

      var importedEntriesCount = 0
      if (root.has("journalistEntries")) {
        val eArray = root.getJSONArray("journalistEntries")
        val newEntries = mutableListOf<JournalistEntryEntity>()
        for (i in 0 until eArray.length()) {
          val obj = eArray.getJSONObject(i)
          val catStr = obj.optString("category", "OBSERVATION")
          newEntries.add(
            JournalistEntryEntity(
              id = obj.getString("id"),
              personId = obj.getString("personId"),
              personName = obj.getString("personName"),
              category = JournalistCategory.fromString(catStr),
              text = obj.getString("text"),
              timestamp = obj.optString("timestamp", ""),
              date = obj.getString("date"),
              time = obj.optString("time", "12:00"),
              context = obj.optString("context", ""),
              intensity = obj.optString("intensity", ""),
              createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
              updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
          )
        }
        if (newEntries.isNotEmpty()) {
          database.journalistEntryDao().insertEntries(newEntries)
          importedEntriesCount = newEntries.size
        }
      }

      Result.success("Successfully imported $importedTasksCount tasks, $importedRoutinesCount routines, $importedReflectionsCount reflections, $importedGoalsCount goals, $importedPersonsCount persons, and $importedEntriesCount entries.")
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun generateCsv(database: LifeTrackerDatabase): String = withContext(Dispatchers.IO) {
    val tasks = database.taskDao().getAllTasksSync()
    val sb = StringBuilder()
    sb.append("Date,12-Hour Time,Task Name,Category,Type,Status,Score,Notes\n")
    for (t in tasks) {
      val typeStr = if (t.isExtra) "Extra Task" else "Routine"
      val scoreStr = t.status.score.toString()
      val timeStr = TimeUtils.minutesTo12Hour(t.timeMinutes)
      sb.append("\"${t.date}\",\"$timeStr\",\"${t.name.replace("\"", "\"\"")}\",\"${t.category}\",\"$typeStr\",\"${t.status.label}\",\"$scoreStr\",\"${t.notes.replace("\"", "\"\"")}\"\n")
    }
    sb.toString()
  }

  suspend fun generateMarkdown(database: LifeTrackerDatabase): String = withContext(Dispatchers.IO) {
    val tasks = database.taskDao().getAllTasksSync()
    val reflections = database.reflectionDao().getAllReflectionsSync()
    val goals = database.goalDao().getAllGoalsSync()

    val sb = StringBuilder()
    sb.append("# Life Tracker Export\n\n")
    sb.append("**Export Date:** ${TimeUtils.getTodayDateString()}\n\n")

    sb.append("## Daily Task Performance\n\n")
    val groupedByDate = tasks.groupBy { it.date }
    for ((date, dayTasks) in groupedByDate.toSortedMap(compareByDescending { it })) {
      val total = dayTasks.size
      val completed = dayTasks.count { it.status == TaskStatus.COMPLETE }
      val partial = dayTasks.count { it.status == TaskStatus.PARTIAL }
      val missed = dayTasks.count { it.status == TaskStatus.MISSED }
      val score = (completed * 1.0f) + (partial * 0.5f)
      val pct = if (total > 0) ((score / total) * 100).toInt() else 0

      sb.append("### ${TimeUtils.formatDateDisplay(date)} (Completion: $pct% | Score: $score / $total)\n\n")
      sb.append("| Time | Task | Category | Type | Status |\n")
      sb.append("| :--- | :--- | :--- | :--- | :--- |\n")
      for (t in dayTasks) {
        val typeBadge = if (t.isExtra) "Extra" else "Routine"
        val statusEmoji = when (t.status) {
          TaskStatus.COMPLETE -> "🟢 COMPLETE"
          TaskStatus.PARTIAL -> "🟡 PARTIAL"
          TaskStatus.MISSED -> "🔴 MISSED"
        }
        sb.append("| ${TimeUtils.minutesTo12Hour(t.timeMinutes)} | ${t.name} | ${t.category} | $typeBadge | $statusEmoji |\n")
      }
      sb.append("\n")
    }

    if (reflections.isNotEmpty()) {
      sb.append("## Reflections\n\n")
      for (rf in reflections) {
        sb.append("- **${rf.category.fullDisplay}** (${rf.date}): **${rf.title}**\n")
        if (rf.description.isNotEmpty()) {
          sb.append("  > ${rf.description}\n")
        }
        if (rf.tags.isNotEmpty()) {
          sb.append("  *Tags:* `${rf.tags}`\n")
        }
      }
      sb.append("\n")
    }

    if (goals.isNotEmpty()) {
      sb.append("## Goals\n\n")
      for (g in goals) {
        val status = if (g.isCompleted) "✅ Completed" else "${g.progress}% In Progress"
        sb.append("- **${g.title}** ($status)\n")
        if (g.description.isNotEmpty()) {
          sb.append("  ${g.description}\n")
        }
        if (g.deadline != null) {
          sb.append("  Deadline: ${g.deadline}\n")
        }
      }
    }

    sb.toString()
  }

  suspend fun generatePlainText(database: LifeTrackerDatabase): String = withContext(Dispatchers.IO) {
    val tasks = database.taskDao().getAllTasksSync()
    val reflections = database.reflectionDao().getAllReflectionsSync()
    val sb = StringBuilder()
    sb.append("LIFE TRACKER REPORT\n")
    sb.append("Generated: ${TimeUtils.getTodayDateString()}\n")
    sb.append("========================================\n\n")

    val grouped = tasks.groupBy { it.date }
    for ((date, dayTasks) in grouped.toSortedMap(compareByDescending { it })) {
      val total = dayTasks.size
      val completed = dayTasks.count { it.status == TaskStatus.COMPLETE }
      val partial = dayTasks.count { it.status == TaskStatus.PARTIAL }
      val missed = dayTasks.count { it.status == TaskStatus.MISSED }
      val score = completed + (partial * 0.5f)
      val pct = if (total > 0) ((score / total) * 100).toInt() else 0

      sb.append("[$date] $pct% ($score/$total) - Done: $completed, Partial: $partial, Missed: $missed\n")
      for (t in dayTasks) {
        val tag = if (t.isExtra) "[EXTRA]" else "[ROUTINE]"
        sb.append("  ${TimeUtils.minutesTo12Hour(t.timeMinutes)} - ${t.name} $tag -> ${t.status.label}\n")
      }
      sb.append("\n")
    }

    if (reflections.isNotEmpty()) {
      sb.append("REFLECTIONS & OBSERVATIONS:\n")
      sb.append("----------------------------------------\n")
      for (rf in reflections) {
        sb.append("[${rf.date}] [${rf.category.label}] ${rf.title}\n")
        if (rf.description.isNotEmpty()) sb.append("  ${rf.description}\n")
      }
    }
    sb.toString()
  }

  suspend fun generateAiAnalysisPrompt(database: LifeTrackerDatabase): String = withContext(Dispatchers.IO) {
    val tasks = database.taskDao().getAllTasksSync()
    val reflections = database.reflectionDao().getAllReflectionsSync()
    val goals = database.goalDao().getAllGoalsSync()
    val routines = database.routineDao().getAllRoutineTemplatesSync()
    val settings = database.userSettingsDao().getSettingsSync()

    val totalTasks = tasks.size
    val totalComplete = tasks.count { it.status == TaskStatus.COMPLETE }
    val totalPartial = tasks.count { it.status == TaskStatus.PARTIAL }
    val totalMissed = tasks.count { it.status == TaskStatus.MISSED }
    val totalScore = totalComplete + (totalPartial * 0.5f)
    val avgAdherence = if (totalTasks > 0) ((totalScore / totalTasks) * 100).toInt() else 0
    val daysCount = tasks.map { it.date }.distinct().size

    val categoryBreakdown = tasks.groupBy { it.category }.mapValues { (_, list) ->
      val c = list.count { it.status == TaskStatus.COMPLETE }
      val p = list.count { it.status == TaskStatus.PARTIAL }
      val score = c + (p * 0.5f)
      val rate = if (list.isNotEmpty()) ((score / list.size) * 100).toInt() else 0
      "$rate% (${list.size} tasks)"
    }

    val mistakes = reflections.filter { it.category == ReflectionCategory.MISTAKE }
    val goodDeeds = reflections.filter { it.category == ReflectionCategory.GOOD_DEED }
    val observations = reflections.filter { it.category == ReflectionCategory.OBSERVATION }

    val sb = StringBuilder()
    sb.append("# AI Executive Coaching Prompt: Life Tracker Analysis\n\n")
    sb.append("> **Instructions for AI**: You are a world-class executive performance and behavioral coach. Analyze this comprehensive tracked data and provide an incisive diagnostic review, identifying psychological friction points, routine bottlenecks, win-streaks, and actionable high-leverage micro-adjustments for the next 7-day cycle.\n\n")

    sb.append("## 1. Quantitative Performance Overview\n")
    sb.append("- **Total Days Logged**: $daysCount days\n")
    sb.append("- **Total Tasks Recorded**: $totalTasks\n")
    sb.append("- **Overall Adherence Score**: $avgAdherence%\n")
    sb.append("- **Complete (1.0)**: $totalComplete\n")
    sb.append("- **Partial (0.5)**: $totalPartial\n")
    sb.append("- **Missed (0.0)**: $totalMissed\n")
    sb.append("- **7-Day Continuous Cycle Anchor**: ${settings?.anchorDate ?: "Initial"}\n\n")

    sb.append("## 2. Category Performance Breakdown\n")
    for ((cat, metric) in categoryBreakdown) {
      sb.append("- **$cat**: $metric\n")
    }
    sb.append("\n")

    sb.append("## 3. Fixed Routine Blueprint (5:00 AM - 10:00 PM)\n")
    for (r in routines) {
      sb.append("- ${TimeUtils.minutesTo12Hour(r.timeMinutes)}: ${r.name} [${r.category}]\n")
    }
    sb.append("\n")

    sb.append("## 4. Qualitative Behavioral Log (Recent Reflections)\n")
    if (mistakes.isNotEmpty()) {
      sb.append("### ❌ Mistakes / Weaknesses (गलती / कमी):\n")
      for (m in mistakes.take(15)) {
        sb.append("- [${m.date}] **${m.title}**: ${m.description} ${if (m.tags.isNotEmpty()) "(${m.tags})" else ""}\n")
      }
    }

    if (goodDeeds.isNotEmpty()) {
      sb.append("\n### ✅ Good Actions & Wins (अच्छाई):\n")
      for (g in goodDeeds.take(15)) {
        sb.append("- [${g.date}] **${g.title}**: ${g.description}\n")
      }
    }

    if (observations.isNotEmpty()) {
      sb.append("\n### 🔎 Neutral Observations:\n")
      for (o in observations.take(15)) {
        sb.append("- [${o.date}] **${o.title}**: ${o.description}\n")
      }
    }

    sb.append("\n## 5. Strategic Goals Status\n")
    if (goals.isNotEmpty()) {
      for (g in goals) {
        val status = if (g.isCompleted) "Completed" else "${g.progress}%"
        sb.append("- **${g.title}**: $status | Deadline: ${g.deadline ?: "Ongoing"}\n")
      }
    } else {
      sb.append("No active long-term goals registered.\n")
    }

    sb.append("\n---\n")
    sb.append("### Please provide your response structured as follows:\n")
    sb.append("1. **Diagnosis**: Where is friction or energy drop occurring in the daily schedule?\n")
    sb.append("2. **Mistake Pattern Analysis**: What underlying root causes connect the logged ❌ mistakes?\n")
    sb.append("3. **Strength Leverage**: How can the user replicate the logged ✅ good actions?\n")
    sb.append("4. **Recommended Protocol for the Next 7-Day Cycle**: 3 concrete, non-negotiable rules.\n")

    sb.toString()
  }

  fun exportPersonAsTxt(person: JournalistPersonEntity, entries: List<JournalistEntryEntity>): String {
    val sb = StringBuilder()
    sb.append("═══════════════════════════════════════════════════\n")
    sb.append("JOURNALIST REPORT: ${person.name} ${person.emoji}\n")
    sb.append("═══════════════════════════════════════════════════\n\n")

    val goodCount = entries.count { it.category == JournalistCategory.GOOD }
    val badCount = entries.count { it.category == JournalistCategory.BAD }
    val obsCount = entries.count { it.category == JournalistCategory.OBSERVATION }

    sb.append("SUMMARY:\n")
    sb.append("• कुल Entries: ${entries.size}\n")
    sb.append("• अच्छाई (Good): $goodCount\n")
    sb.append("• बुराई (Bad): $badCount\n")
    sb.append("• Observation: $obsCount\n\n")
    sb.append("───────────────────────────────────────────────────\n")
    sb.append("TIMELINE OF OBSERVATIONS:\n")
    sb.append("───────────────────────────────────────────────────\n\n")

    if (entries.isEmpty()) {
      sb.append("कोई entry नहीं मिली।\n")
    } else {
      for ((idx, entry) in entries.withIndex()) {
        val catHeader = when (entry.category) {
          JournalistCategory.GOOD -> "🟢 अच्छाई"
          JournalistCategory.BAD -> "🔴 बुराई"
          JournalistCategory.OBSERVATION -> "⚪ Observation"
        }
        val timeDisplay = TimeUtils.timeStringToHindi(entry.time)
        val dateDisplay = TimeUtils.formatDateDisplay(entry.date)

        sb.append("${idx + 1}. [$catHeader] $dateDisplay • $timeDisplay\n")
        sb.append("   विवरण: ${entry.text}\n")
        if (entry.context.isNotBlank()) {
          sb.append("   संदर्भ (Context): ${entry.context}\n")
        }
        if (entry.intensity.isNotBlank()) {
          sb.append("   तीव्रता (Intensity): ${entry.intensity}\n")
        }
        sb.append("\n")
      }
    }
    sb.append("═══════════════════════════════════════════════════\n")
    return sb.toString()
  }

  fun exportPersonAsJson(person: JournalistPersonEntity, entries: List<JournalistEntryEntity>): String {
    val root = JSONObject()
    val personObj = JSONObject()
    personObj.put("id", person.id)
    personObj.put("name", person.name)
    personObj.put("emoji", person.emoji)
    personObj.put("createdAt", person.createdAt)
    root.put("person", personObj)

    val entriesArray = JSONArray()
    for (e in entries) {
      val item = JSONObject()
      item.put("id", e.id)
      item.put("category", e.category.name)
      item.put("categoryLabel", e.category.label)
      item.put("text", e.text)
      item.put("date", e.date)
      item.put("time", e.time)
      item.put("timeHindi", TimeUtils.timeStringToHindi(e.time))
      item.put("context", e.context)
      item.put("intensity", e.intensity)
      item.put("timestamp", e.timestamp)
      item.put("createdAt", e.createdAt)
      entriesArray.put(item)
    }
    root.put("entries", entriesArray)
    return root.toString(2)
  }
}
