package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BreathPhase(val enName: String, val hiName: String) {
  INHALE("Inhale", "श्वास अंदर लें"),
  HOLD("Hold", "रोकें"),
  EXHALE("Exhale", "श्वास छोड़ें"),
  REST("Relax", "शांत रहें")
}

enum class MeditationType(
  val id: String,
  val englishTitle: String,
  val hindiTitle: String,
  val description: String,
  val inhaleSec: Int,
  val holdSec: Int,
  val exhaleSec: Int,
  val restSec: Int,
  val iconTag: String
) {
  BREATHING(
    id = "breathing",
    englishTitle = "Breathing",
    hindiTitle = "श्वास ध्यान",
    description = "Box & rhythmic breathing: Inhale, Hold, Exhale with clear voice & soft bell cues",
    inhaleSec = 4,
    holdSec = 4,
    exhaleSec = 4,
    restSec = 2,
    iconTag = "air"
  ),
  MINDFULNESS(
    id = "mindfulness",
    englishTitle = "Mindfulness",
    hindiTitle = "सचेतन ध्यान",
    description = "Present-moment awareness, body grounding, observing thoughts without judgment",
    inhaleSec = 5,
    holdSec = 0,
    exhaleSec = 5,
    restSec = 0,
    iconTag = "mind"
  ),
  FOCUS(
    id = "focus",
    englishTitle = "Focus",
    hindiTitle = "एकाग्रता ध्यान",
    description = "Laser concentration, single-pointed awareness and breath counting",
    inhaleSec = 4,
    holdSec = 2,
    exhaleSec = 4,
    restSec = 2,
    iconTag = "focus"
  ),
  RELAXATION(
    id = "relaxation",
    englishTitle = "Relaxation",
    hindiTitle = "गहरा विश्राम",
    description = "Progressive muscle relaxation, releasing physical tension from crown to toes",
    inhaleSec = 6,
    holdSec = 2,
    exhaleSec = 6,
    restSec = 2,
    iconTag = "relax"
  ),
  SLEEP(
    id = "sleep",
    englishTitle = "Sleep",
    hindiTitle = "गहरी नींद",
    description = "4-7-8 calming delta frequency flow for deep, restorative sleep induction",
    inhaleSec = 4,
    holdSec = 7,
    exhaleSec = 8,
    restSec = 2,
    iconTag = "sleep"
  ),
  MORNING(
    id = "morning",
    englishTitle = "Morning",
    hindiTitle = "सवेरे की ऊर्जा",
    description = "Energizing morning breath, oxygenation and positive intention setting",
    inhaleSec = 4,
    holdSec = 2,
    exhaleSec = 4,
    restSec = 1,
    iconTag = "morning"
  ),
  VISUALIZATION(
    id = "visualization",
    englishTitle = "Visualization",
    hindiTitle = "सकारात्मक कल्पना",
    description = "Mental sanctuary of golden healing light, calm strength and empowerment",
    inhaleSec = 5,
    holdSec = 2,
    exhaleSec = 5,
    restSec = 2,
    iconTag = "vision"
  ),
  SOUND(
    id = "sound",
    englishTitle = "Sound",
    hindiTitle = "नाद ध्यान",
    description = "Tibetan singing bowl harmonics, resonant frequency and profound inner silence",
    inhaleSec = 6,
    holdSec = 2,
    exhaleSec = 6,
    restSec = 2,
    iconTag = "sound"
  ),
  GRATITUDE(
    id = "gratitude",
    englishTitle = "Gratitude",
    hindiTitle = "कृतज्ञता भाव",
    description = "Heart-centered awareness, blessing reflection and deep contentment",
    inhaleSec = 5,
    holdSec = 2,
    exhaleSec = 5,
    restSec = 2,
    iconTag = "heart"
  ),
  WALKING(
    id = "walking",
    englishTitle = "Walking",
    hindiTitle = "सजग गति",
    description = "Mindful pacing, grounded presence and synchronous rhythmic movement",
    inhaleSec = 4,
    holdSec = 0,
    exhaleSec = 4,
    restSec = 0,
    iconTag = "walk"
  );

  val cycleDurationSec: Int
    get() = (inhaleSec + holdSec + exhaleSec + restSec).coerceAtLeast(4)

  companion object {
    fun fromId(id: String): MeditationType =
      entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
        ?: BREATHING
  }
}

@Entity(
  tableName = "meditation_sessions",
  indices = [Index(value = ["date"])]
)
data class MeditationSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String, // YYYY-MM-DD
  val type: String, // from MeditationType.name or id
  val durationMinutes: Int,
  val completedSeconds: Int,
  val isCompleted: Boolean,
  val timestamp: Long = System.currentTimeMillis()
)

data class MeditationStats(
  val totalSessions: Int = 0,
  val totalMinutes: Int = 0,
  val completedSessions: Int = 0,
  val mostPracticedType: String = "Breathing"
)
