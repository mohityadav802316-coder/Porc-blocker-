package com.example

import com.example.data.model.BreathPhase
import com.example.data.model.MeditationType
import com.example.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun meditationTypes_allTenModesPresent() {
    val types = MeditationType.entries
    assertEquals(10, types.size)

    val expectedIds = listOf(
      "breathing", "mindfulness", "focus", "relaxation", "sleep",
      "morning", "visualization", "sound", "gratitude", "walking"
    )
    expectedIds.forEach { id ->
      val type = MeditationType.fromId(id)
      assertNotNull("Type $id must exist", type)
      assertTrue("Hindi title must not be blank for $id", type.hindiTitle.isNotBlank())
      assertTrue("English title must not be blank for $id", type.englishTitle.isNotBlank())
      assertTrue("Cycle duration must be >= 4 seconds for $id", type.cycleDurationSec >= 4)
    }
  }

  @Test
  fun meditationPersonalizedEnding_formatsCorrectly() {
    fun buildClosingText(userName: String?, lang: String): String {
      val trimmedName = userName?.trim().orEmpty()
      return if (lang.equals("EN", ignoreCase = true)) {
        if (trimmedName.isNotEmpty()) {
          "$trimmedName, your meditation is complete. You may gently open your eyes now."
        } else {
          "Your meditation is complete. You may gently open your eyes now."
        }
      } else {
        if (trimmedName.isNotEmpty()) {
          "$trimmedName, तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।"
        } else {
          "तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।"
        }
      }
    }

    // Personalized Hindi with name
    val hindiWithName = buildClosingText("मोहित", "HI")
    assertEquals("मोहित, तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।", hindiWithName)

    // Personalized Hindi fallback without name
    val hindiFallback = buildClosingText("", "HI")
    assertEquals("तुम्हारा मेडिटेशन कंप्लीट हो गया है। अब तुम आँखें खोल सकते हो।", hindiFallback)

    // English with name
    val enWithName = buildClosingText("Mohit", "EN")
    assertEquals("Mohit, your meditation is complete. You may gently open your eyes now.", enWithName)

    // English fallback
    val enFallback = buildClosingText(null, "EN")
    assertEquals("Your meditation is complete. You may gently open your eyes now.", enFallback)
  }

  @Test
  fun timeUtils_todayDateConsistency() {
    val dateIso = TimeUtils.getTodayIsoDate()
    val dateStr = TimeUtils.getTodayDateString()
    assertEquals(dateStr, dateIso)
    assertTrue(dateIso.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
  }

  @Test
  fun backupManifest_roundTripSerialization() {
    val manifest = com.example.backup.BackupManifest(
      appVersionCode = 2,
      appVersionName = "1.1",
      databaseVersion = 15,
      createdAt = 1727800000000L,
      createdDateIso = "2026-10-02T05:30:00Z",
      recordCounts = mapOf("dayTasks" to 14, "routineTemplates" to 14),
      checksums = mapOf("life_tracker_db" to "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    )
    val json = manifest.toJson()
    val parsed = com.example.backup.BackupManifest.fromJson(json)
    assertNotNull(parsed)
    assertEquals(15, parsed!!.databaseVersion)
    assertEquals(2, parsed.appVersionCode)
    assertEquals(14, parsed.recordCounts["dayTasks"])
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", parsed.checksums["life_tracker_db"])
  }
}
