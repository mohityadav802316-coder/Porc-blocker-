package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StrictLockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrictLockDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: StrictLockEventEntity): Long

  @Query("SELECT * FROM strict_lock_events ORDER BY timestamp DESC")
  fun getAllEvents(): Flow<List<StrictLockEventEntity>>

  @Query("SELECT * FROM strict_lock_events ORDER BY timestamp DESC LIMIT 20")
  fun getRecentEvents(): Flow<List<StrictLockEventEntity>>

  @Query("SELECT COUNT(*) FROM strict_lock_events")
  suspend fun countEvents(): Int

  @Query("SELECT * FROM strict_lock_events WHERE date = :date ORDER BY timestamp DESC")
  fun getEventsForDate(date: String): Flow<List<StrictLockEventEntity>>

  @Query("SELECT * FROM strict_lock_events WHERE isCompleted = 0 ORDER BY timestamp DESC LIMIT 1")
  suspend fun getActiveStrictLockEvent(): StrictLockEventEntity?

  @Query("UPDATE strict_lock_events SET isCompleted = 1 WHERE id = :id")
  suspend fun markCompleted(id: Long)

  @Query("UPDATE strict_lock_events SET isCompleted = 1")
  suspend fun markAllCompleted()
}
