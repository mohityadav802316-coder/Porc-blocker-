package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for tracking Strict Lock activations triggered by on-device adult content detection.
 * 100% offline, stored exclusively in local Room Database.
 */
@Entity(tableName = "strict_lock_events")
data class StrictLockEventEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val date: String, // YYYY-MM-DD
  val time: String, // hh:mm a
  val reason: String,
  val triggerWord: String = "",
  val packageName: String = "",
  val durationMinutes: Int = 15,
  val epochEndTimestamp: Long = 0L,
  val elapsedRealtimeEnd: Long = 0L,
  val startElapsedRealtime: Long = 0L,
  val isCompleted: Boolean = false
)
