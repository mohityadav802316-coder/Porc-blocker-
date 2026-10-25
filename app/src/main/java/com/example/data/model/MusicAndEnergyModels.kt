package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Energy Mode for the day: Low, Normal, High.
 * Essential routine activities remain intact.
 * Automatically resets to Normal the next day.
 */
enum class EnergyMode(
  val titleEn: String,
  val titleHi: String,
  val icon: String,
  val description: String
) {
  LOW("Low Energy", "कम ऊर्जा", "🔋", "हल्का दिन • सिर्फ आवश्यक कार्य और विश्राम"),
  NORMAL("Normal Energy", "सामान्य ऊर्जा", "⚡", "संतुलित दिनचर्या • सामान्य रूटीन"),
  HIGH("High Energy", "उच्च ऊर्जा", "🚀", "पीक फोकस • उच्च उत्पादकता और लक्ष्य");

  val emoji: String get() = icon
  val label: String get() = titleHi

  companion object {
    fun fromString(value: String?): EnergyMode =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
  }
}

/**
 * Mood definitions for Music System.
 */
enum class MusicMood(
  val titleEn: String,
  val titleHi: String,
  val emoji: String,
  val colorHex: String
) {
  CALM("Calm", "शांत", "🌿", "#10B981"),
  ENERGETIC("Energetic", "ऊर्जावान", "⚡", "#F59E0B"),
  HAPPY("Happy", "प्रसन्न", "😊", "#EC4899"),
  FOCUS("Focus", "एकाग्रता", "🎯", "#00F0FF"),
  RELAX("Relax", "विश्राम", "☕", "#8B5CF6"),
  SLEEP("Sleep", "नींद", "🌙", "#6366F1"),
  SAD("Reflective", "गंभीर", "🌧️", "#64748B"),
  DEVOTIONAL("Devotional", "भक्ति", "🪔", "#F97316"),
  CUSTOM("Custom", "कस्टम", "🎵", "#06B6D4");

  val title: String get() = titleEn

  companion object {
    fun fromString(value: String?): MusicMood =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CALM
  }
}

/**
 * Song entity representing local audio file on device or built-in ambient track.
 */
@Entity(tableName = "songs")
data class SongEntity(
  @PrimaryKey val id: String, // MediaStore ID or "builtin_xxx"
  val title: String,
  val artist: String = "Unknown Artist",
  val album: String = "Unknown Album",
  val durationMs: Long = 0L,
  val contentUri: String, // content:// or asset / synthesizer uri
  val albumArtUri: String? = null,
  val isFavorite: Boolean = false,
  val lastPlayedAt: Long = 0L,
  val playCount: Int = 0,
  val isBuiltIn: Boolean = false,
  val mood: String = "CALM" // MusicMood name
)

/**
 * Custom playlist created by user (e.g. Morning, Workout, Study, Relax, Travel).
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val description: String = "",
  val colorHex: String = "#00F0FF",
  val icon: String = "playlist_play",
  val createdAt: Long = System.currentTimeMillis()
)

/**
 * Cross-reference table linking songs to playlists.
 * A song can be added to multiple playlists.
 */
@Entity(
  tableName = "playlist_songs",
  primaryKeys = ["playlistId", "songId"],
  indices = [Index(value = ["playlistId"]), Index(value = ["songId"])]
)
data class PlaylistSongCrossRef(
  val playlistId: Long,
  val songId: String,
  val orderIndex: Int = 0,
  val addedAt: Long = System.currentTimeMillis()
)

/**
 * Playlist + Time + Mood Rule.
 * E.g.: Morning Playlist: 5:00-6:00 -> Calm, 6:00-7:00 -> Energetic.
 * Connects with Life Tracker Daily Routine.
 */
@Entity(
  tableName = "playlist_rules",
  indices = [Index(value = ["playlistId"])]
)
data class PlaylistRuleEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val playlistId: Long,
  val startMinutes: Int, // e.g. 300 = 5:00 AM
  val endMinutes: Int,   // e.g. 360 = 6:00 AM
  val mood: String = "CALM", // MusicMood
  val situation: String = "Routine", // e.g. "Morning Routine", "Workout", "Study"
  val linkedCategory: String? = null, // Matches DayTaskEntity.category
  val autoPlay: Boolean = false
)

/**
 * Daily Challenge entity.
 */
@Entity(tableName = "daily_challenges")
data class DailyChallengeEntity(
  @PrimaryKey val date: String, // YYYY-MM-DD
  val category: String, // Reading, Meditation, Learning, Organization, Exercise, Productivity, Self-Improvement
  val title: String,
  val description: String,
  val targetMinutes: Int = 15,
  val isCompleted: Boolean = false,
  val completedAt: Long = 0L,
  val isSkipped: Boolean = false
)
