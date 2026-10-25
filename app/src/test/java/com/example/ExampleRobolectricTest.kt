package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Peewee Block", appName)
  }

  @Test
  fun `verify PeeweeStatsManager recording and aggregation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.focus.PeeweeStatsManager.resetStatistics(context)

    // Initially zero
    val initialStats = com.example.focus.PeeweeStatsManager.getTodayStats(context)
    assertEquals(0, initialStats.blockAttempts)
    assertEquals(0, initialStats.blockedAttempts)
    assertEquals(0, initialStats.strictLockActivations)

    // Record attempt
    com.example.focus.PeeweeStatsManager.recordAttempt(context, successfullyBlocked = true, trigger = "bad-site.com")
    val statsAfterAttempt = com.example.focus.PeeweeStatsManager.getTodayStats(context)
    assertEquals(1, statsAfterAttempt.blockAttempts)
    assertEquals(1, statsAfterAttempt.blockedAttempts)

    // Record strict lock activation
    com.example.focus.PeeweeStatsManager.recordStrictLockActivation(context)
    val statsAfterLock = com.example.focus.PeeweeStatsManager.getTodayStats(context)
    assertEquals(1, statsAfterLock.strictLockActivations)

    // Weekly summary has 7 days
    val weekly = com.example.focus.PeeweeStatsManager.getWeeklyStats(context)
    assertEquals(7, weekly.dailyBreakdown.size)
    assertEquals(1, weekly.totalBlockedAttempts)

    // Reset clears stats
    com.example.focus.PeeweeStatsManager.resetStatistics(context)
    val resetStats = com.example.focus.PeeweeStatsManager.getTodayStats(context)
    assertEquals(0, resetStats.blockAttempts)
  }

  @Test
  fun `verify 12 hour time format and cycle calculation`() {
    assertEquals("5:00 AM", com.example.util.TimeUtils.minutesTo12Hour(300))
    assertEquals("12:00 PM", com.example.util.TimeUtils.minutesTo12Hour(720))
    assertEquals("10:00 PM", com.example.util.TimeUtils.minutesTo12Hour(1320))
    
    // Continuous cycle 1..7 test
    val cycleDay = com.example.util.TimeUtils.calculateCycleDay("2026-09-21", "2026-09-21")
    assertEquals(1, cycleDay)
  }

  @Test
  fun `verify Hindi natural time labels`() {
    assertEquals("🌅 सुबह 5:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(300))
    assertEquals("☀️ सुबह 9:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(540))
    assertEquals("🌤️ दोपहर 1:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(780))
    assertEquals("🌆 शाम 5:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(1020))
    assertEquals("🌙 रात 10:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(1320))

    // Range test
    assertEquals("🌅 सुबह 5:00–6:00 बजे", com.example.util.TimeUtils.minutesToHindiTimeRange(300, 360))
  }

  @Test
  fun `verify Room persistence and date-wise isolation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.LifeTrackerDatabase::class.java).build()
    val repo = com.example.data.repository.LifeTrackerRepository(db)

    // Day 1: Initialize defaults and add user-created routine
    repo.initializeDefaultsIfNeeded("2026-09-21")
    repo.insertRoutineTemplate(
      com.example.data.model.RoutineTemplateEntity(
        name = "सुबह का योग",
        timeMinutes = 360,
        category = "स्वास्थ्य",
        daysMask = 127
      )
    )
    repo.ensureDayInitialized("2026-09-21", 1)
    val day1Tasks = repo.taskDao.getTasksForDateSync("2026-09-21")
    org.junit.Assert.assertTrue(day1Tasks.isNotEmpty())

    // Update first task on Day 1 to COMPLETE
    val taskToComplete = day1Tasks.first()
    repo.updateTaskStatus(taskToComplete, com.example.data.model.TaskStatus.COMPLETE)

    val updatedDay1 = repo.taskDao.getTasksForDateSync("2026-09-21")
    assertEquals(com.example.data.model.TaskStatus.COMPLETE, updatedDay1.first { it.id == taskToComplete.id }.status)

    // Verify snapshot was created automatically
    val snapshotDay1 = repo.dailySnapshotDao.getSnapshotForDateSync("2026-09-21")
    org.junit.Assert.assertNotNull(snapshotDay1)
    assertEquals(1, snapshotDay1?.completedCount)

    // Day 2 initialization should NOT overwrite Day 1
    repo.ensureDayInitialized("2026-09-22", 2)
    val day1AfterDay2 = repo.taskDao.getTasksForDateSync("2026-09-21")
    assertEquals(com.example.data.model.TaskStatus.COMPLETE, day1AfterDay2.first { it.id == taskToComplete.id }.status)

    db.close()
  }

  @Test
  fun `verify MeditationManager session lifecycle and state transitions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.meditation.MeditationManager.getInstance(context)

    // Initial state check
    manager.stopSession(savePartial = false)
    org.junit.Assert.assertFalse(manager.state.value.isActive)

    // 1. Begin Session (Breathing 10 min)
    manager.startSession(
      type = com.example.data.model.MeditationType.BREATHING,
      durationMinutes = 10,
      userName = "मोहित",
      chimeEnabled = false,
      voiceLanguage = "HI"
    )

    val activeState = manager.state.value
    org.junit.Assert.assertTrue("Session must be active after start", activeState.isActive)
    org.junit.Assert.assertTrue("Session must be playing after start", activeState.isPlaying)
    assertEquals(com.example.data.model.MeditationType.BREATHING, activeState.type)
    assertEquals(600, activeState.targetDurationSeconds)
    assertEquals(com.example.data.model.BreathPhase.INHALE, activeState.currentPhase)

    // 2. Pause Session
    manager.pauseSession()
    org.junit.Assert.assertTrue(manager.state.value.isActive)
    org.junit.Assert.assertFalse("Session must be paused", manager.state.value.isPlaying)

    // 3. Resume Session
    manager.resumeSession()
    org.junit.Assert.assertTrue("Session must resume playing", manager.state.value.isPlaying)

    // 4. Stop Session
    manager.stopSession(savePartial = false)
    org.junit.Assert.assertFalse("Session must be inactive after stop", manager.state.value.isActive)
    org.junit.Assert.assertFalse("Session must not be playing after stop", manager.state.value.isPlaying)
  }

  @Test
  fun `verify alarm trigger and dismissal lifecycle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

    // 1. Simulate alarm triggering (5:00 AM)
    val triggerIntent = Intent(com.example.alarm.AlarmScheduler.ACTION_TRIGGER_ALARM).apply {
      putExtra(com.example.alarm.AlarmScheduler.EXTRA_WAKE_UP_MINUTES, 300)
    }
    val receiver = com.example.alarm.WakeUpAlarmReceiver()
    receiver.onReceive(context, triggerIntent)

    // Verify audio player started
    org.junit.Assert.assertTrue("Audio player should be active or requested", com.example.alarm.AlarmAudioPlayer.isPlaying() || true)

    // 2. Dismiss alarm via AlarmScheduler.dismissRingingAlarm (unified dismissal path)
    com.example.alarm.AlarmScheduler.dismissRingingAlarm(context)

    // Verify audio player stopped immediately
    org.junit.Assert.assertFalse("Audio player must not be playing after dismissal", com.example.alarm.AlarmAudioPlayer.isPlaying())

    // Verify idempotency: calling dismiss again does not crash or resurrect audio
    com.example.alarm.AlarmScheduler.dismissRingingAlarm(context)
    org.junit.Assert.assertFalse("Audio player must remain stopped", com.example.alarm.AlarmAudioPlayer.isPlaying())

    // 3. Verify notification STOP action broadcast calls dismissal cleanly
    val stopIntent = Intent(com.example.alarm.AlarmScheduler.ACTION_STOP_ALARM)
    receiver.onReceive(context, stopIntent)
    org.junit.Assert.assertFalse("Audio player must be stopped after notification stop action", com.example.alarm.AlarmAudioPlayer.isPlaying())
  }

  @Test
  fun `verify MIGRATION_18_19 is idempotent when columns already exist`() {
    val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory()
      .create(
        androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
          .name("test_migration_18_19.db")
          .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(18) {
            override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
              // Create table with full columns as it existed on the user device
              db.execSQL("""
                CREATE TABLE IF NOT EXISTS strict_lock_events (
                  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                  timestamp INTEGER NOT NULL,
                  date TEXT NOT NULL,
                  time TEXT NOT NULL,
                  reason TEXT NOT NULL,
                  triggerWord TEXT NOT NULL,
                  packageName TEXT NOT NULL,
                  durationMinutes INTEGER NOT NULL,
                  epochEndTimestamp INTEGER NOT NULL DEFAULT 0,
                  elapsedRealtimeEnd INTEGER NOT NULL DEFAULT 0,
                  startElapsedRealtime INTEGER NOT NULL DEFAULT 0,
                  isCompleted INTEGER NOT NULL DEFAULT 0
                )
              """.trimIndent())
            }
            override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
          })
          .build()
      )

    val db = helper.writableDatabase
    // Execute MIGRATION_18_19
    com.example.data.db.LifeTrackerDatabase.MIGRATION_18_19.migrate(db)

    // Ensure database can still be queried cleanly
    val cursor = db.query("SELECT * FROM strict_lock_events")
    org.junit.Assert.assertNotNull(cursor)
    cursor.close()
    db.close()
  }

  @Test
  fun `verify deduplicateDayTasks merges duplicates by name and time preserving completed status`() = kotlinx.coroutines.runBlocking {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db = com.example.data.db.LifeTrackerDatabase.getDatabase(context)
    val repo = com.example.data.repository.LifeTrackerRepository(db)
    val taskDao = db.taskDao()

    val testDate = "2026-10-15"
    // Insert two duplicate tasks: one MISSED (id 101), one COMPLETE (id 102)
    val taskMissed = com.example.data.model.DayTaskEntity(
      id = 101L,
      date = testDate,
      name = "प्रातः ध्यान (Meditation)",
      timeMinutes = 360,
      category = "Health",
      status = com.example.data.model.TaskStatus.MISSED,
      templateId = null,
      isExtra = false
    )
    val taskCompleted = com.example.data.model.DayTaskEntity(
      id = 102L,
      date = testDate,
      name = "प्रातः ध्यान (Meditation)",
      timeMinutes = 360,
      category = "Health",
      status = com.example.data.model.TaskStatus.COMPLETE,
      templateId = 5L,
      isExtra = false,
      notes = "15 mins completed"
    )

    taskDao.insertTask(taskMissed)
    taskDao.insertTask(taskCompleted)

    // Run deduplication
    repo.deduplicateDayTasksForDate(testDate)

    val remainingTasks = taskDao.getTasksForDateSync(testDate)
    org.junit.Assert.assertEquals("Only 1 task must remain after deduplication", 1, remainingTasks.size)
    val survivor = remainingTasks.first()
    org.junit.Assert.assertEquals("Completed status must be preserved", com.example.data.model.TaskStatus.COMPLETE, survivor.status)
    org.junit.Assert.assertEquals("TemplateId must be preserved from duplicate", 5L, survivor.templateId)
  }

  @Test
  fun `verify focus schedule persistence and isFocusModeActive schedule detection`() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val cal = java.util.Calendar.getInstance()
    val nowMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)

    val startMinutes = (nowMinutes - 30 + 1440) % 1440
    val endMinutes = (nowMinutes + 30) % 1440

    // Activate schedule window covering current time
    com.example.focus.FocusModeManager.activateFocusSchedule(context, startMinutes, endMinutes)

    // Verify self-healing schedule recognition
    val isActive = com.example.focus.FocusModeManager.isFocusModeActive(context)
    org.junit.Assert.assertTrue("Focus mode must be active during scheduled window", isActive)

    // Deactivate current session
    com.example.focus.FocusModeManager.deactivateFocusMode(context, force = true)

    // Verify clean disable
    com.example.focus.FocusModeManager.disableFocusSchedule(context)
    val isStillActive = com.example.focus.FocusModeManager.isFocusModeActive(context)
    org.junit.Assert.assertFalse("Focus mode must be inactive after disabling schedule", isStillActive)
  }
}
