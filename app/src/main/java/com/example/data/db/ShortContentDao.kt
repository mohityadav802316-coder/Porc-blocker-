package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ShortContentDailySummaryEntity
import com.example.data.model.ShortContentRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortContentDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecord(record: ShortContentRecordEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateSummary(summary: ShortContentDailySummaryEntity)

  @Query("SELECT * FROM short_content_daily_summaries WHERE date = :date LIMIT 1")
  fun getDailySummary(date: String): Flow<ShortContentDailySummaryEntity?>

  @Query("SELECT * FROM short_content_daily_summaries WHERE date = :date LIMIT 1")
  suspend fun getDailySummaryDirect(date: String): ShortContentDailySummaryEntity?

  @Query("SELECT * FROM short_content_daily_summaries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun getSummariesBetween(startDate: String, endDate: String): Flow<List<ShortContentDailySummaryEntity>>

  @Query("SELECT * FROM short_content_daily_summaries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  suspend fun getSummariesBetweenDirect(startDate: String, endDate: String): List<ShortContentDailySummaryEntity>

  @Query("SELECT * FROM short_content_daily_summaries ORDER BY date DESC LIMIT 30")
  fun getAllSummaries(): Flow<List<ShortContentDailySummaryEntity>>

  @Query("SELECT * FROM short_content_records WHERE date = :date ORDER BY timestamp DESC")
  fun getRecordsForDate(date: String): Flow<List<ShortContentRecordEntity>>

  @Query("SELECT COUNT(*) FROM short_content_records WHERE videoSignature = :signature AND timestamp >= :sinceTimestamp")
  suspend fun countRecentSignature(signature: String, sinceTimestamp: Long): Int

  @Query("SELECT videoSignature FROM short_content_records WHERE timestamp >= :sinceTimestamp")
  suspend fun getRecentSignatures(sinceTimestamp: Long): List<String>

  @Query("DELETE FROM short_content_records WHERE timestamp < :beforeTimestamp")
  suspend fun deleteOldRecords(beforeTimestamp: Long)
}
