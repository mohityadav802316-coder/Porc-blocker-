package com.example.focus

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LifeTrackerDatabase
import com.example.data.repository.LifeTrackerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusModeAndMathChallengeTest {

  @Test
  fun testMathChallengeGenerationAndVerification() {
    val questions = MathChallengeGenerator.generateThreeQuestions()
    assertEquals(3, questions.size)

    for (q in questions) {
      assertTrue(q.topic.isNotBlank())
      assertTrue(q.questionHi.isNotBlank())
      assertTrue(q.questionEn.isNotBlank())
      assertTrue(q.hint.isNotBlank())

      // Correct answer verification
      assertTrue(MathChallengeGenerator.isAnswerCorrect(q, q.correctAnswer.toString()))
      assertTrue(MathChallengeGenerator.isAnswerCorrect(q, "  ${q.correctAnswer}  "))

      // Incorrect answer verification
      assertFalse(MathChallengeGenerator.isAnswerCorrect(q, (q.correctAnswer + 13).toString()))
      assertFalse(MathChallengeGenerator.isAnswerCorrect(q, "wrong_text"))
      assertFalse(MathChallengeGenerator.isAnswerCorrect(q, ""))
    }
  }

  @Test
  fun testFocusModeScheduleCalculations() {
    // 1. Daytime schedule: 9:00 AM (540) to 5:00 PM (1020)
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(600, 540, 1020)) // 10:00 AM
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(540, 540, 1020)) // 9:00 AM start
    assertFalse(FocusModeManager.isCurrentTimeInFocusSchedule(1020, 540, 1020)) // 5:00 PM end
    assertFalse(FocusModeManager.isCurrentTimeInFocusSchedule(500, 540, 1020)) // 8:20 AM
    assertFalse(FocusModeManager.isCurrentTimeInFocusSchedule(1100, 540, 1020)) // 6:20 PM

    // 2. Overnight schedule: 9:00 PM (1260) to 6:00 AM (360)
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(1260, 1260, 360)) // 9:00 PM
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(1320, 1260, 360)) // 10:00 PM
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(1439, 1260, 360)) // 11:59 PM
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(0, 1260, 360)) // Midnight
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(180, 1260, 360)) // 3:00 AM
    assertTrue(FocusModeManager.isCurrentTimeInFocusSchedule(359, 1260, 360)) // 5:59 AM
    assertFalse(FocusModeManager.isCurrentTimeInFocusSchedule(360, 1260, 360)) // 6:00 AM
    assertFalse(FocusModeManager.isCurrentTimeInFocusSchedule(720, 1260, 360)) // 12:00 PM Noon
    assertFalse(FocusModeManager.isCurrentTimeInFocusSchedule(1259, 1260, 360)) // 8:59 PM

    // 3. Remaining minutes calculations
    // Daytime: from 10:00 AM (600) to 5:00 PM (1020) -> 420 mins (7 hrs)
    assertEquals(420, FocusModeManager.calculateRemainingMinutes(600, 1020))

    // Overnight: from 10:00 PM (1320) to 6:00 AM (360) -> 120 + 360 = 480 mins (8 hrs)
    assertEquals(480, FocusModeManager.calculateRemainingMinutes(1320, 360))
    // Overnight: from 3:00 AM (180) to 6:00 AM (360) -> 180 mins (3 hrs)
    assertEquals(180, FocusModeManager.calculateRemainingMinutes(180, 360))
  }

  @Test
  fun testFocusModeDatabasePersistenceAndUnlock() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java).build()
    val repo = LifeTrackerRepository(db)

    repo.initializeDefaultsIfNeeded("2026-09-25")

    val initialSettings = repo.userSettingsDao.getSettingsSync()
    assertFalse(initialSettings?.isFocusModeActive ?: true)
    assertFalse(initialSettings?.isFocusScheduleEnabled ?: true)

    // Activate Focus Mode manually
    repo.updateFocusModeActive(true)
    val activeSettings = repo.userSettingsDao.getSettingsSync()
    assertTrue(activeSettings?.isFocusModeActive ?: false)

    // Update Focus Schedule (9:00 PM = 1260, 6:00 AM = 360)
    repo.updateFocusSchedule(isEnabled = true, startTimeMinutes = 1260, endTimeMinutes = 360)
    val scheduledSettings = repo.userSettingsDao.getSettingsSync()
    assertTrue(scheduledSettings?.isFocusScheduleEnabled ?: false)
    assertEquals(1260, scheduledSettings?.focusStartTimeMinutes)
    assertEquals(360, scheduledSettings?.focusEndTimeMinutes)

    // Unlock Focus Mode (e.g. after solving 3-question math challenge)
    repo.updateFocusModeActive(false)
    val unlockedSettings = repo.userSettingsDao.getSettingsSync()
    assertFalse(unlockedSettings?.isFocusModeActive ?: true)

    db.close()
  }
}
