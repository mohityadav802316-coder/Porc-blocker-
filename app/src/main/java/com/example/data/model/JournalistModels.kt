package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class JournalistCategory(val label: String, val iconPrefix: String, val exportCode: String) {
  GOOD("अच्छाई", "🟢", "GOOD"),
  BAD("बुराई", "🔴", "BAD"),
  OBSERVATION("Observation", "⚪", "OBSERVATION");

  val fullDisplay: String
    get() = "$iconPrefix $label"

  companion object {
    fun fromString(value: String): JournalistCategory =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) || it.exportCode.equals(value, ignoreCase = true) } ?: OBSERVATION
  }
}

@Entity(tableName = "journalist_persons")
data class JournalistPersonEntity(
  @PrimaryKey val id: String, // unique id, "self" for default person
  val name: String,
  val emoji: String = "👤",
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
  tableName = "journalist_entries",
  indices = [Index(value = ["date"]), Index(value = ["personId"])]
)
data class JournalistEntryEntity(
  @PrimaryKey val id: String, // unique id UUID
  val personId: String,
  val personName: String,
  val category: JournalistCategory = JournalistCategory.OBSERVATION,
  val text: String,
  val timestamp: String, // ISO timestamp e.g. 2026-09-21T10:30:00
  val date: String, // YYYY-MM-DD
  val time: String, // HH:mm
  val context: String = "", // घर, ऑफिस, दोस्त, अकेला, अन्य
  val intensity: String = "", // हल्का, मध्यम, गहरा
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

class JournalistConverters {
  @TypeConverter
  fun fromJournalistCategory(category: JournalistCategory): String = category.name

  @TypeConverter
  fun toJournalistCategory(value: String): JournalistCategory = JournalistCategory.fromString(value)
}
