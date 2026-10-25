package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyChallengeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyChallengeDao {

  @Query("SELECT * FROM daily_challenges WHERE date = :date LIMIT 1")
  fun getChallengeForDate(date: String): Flow<DailyChallengeEntity?>

  @Query("SELECT * FROM daily_challenges WHERE date = :date LIMIT 1")
  suspend fun getChallengeForDateSync(date: String): DailyChallengeEntity?

  @Query("SELECT * FROM daily_challenges WHERE isCompleted = 1 ORDER BY completedAt DESC")
  fun getAllCompletedChallenges(): Flow<List<DailyChallengeEntity>>

  @Query("SELECT COUNT(*) FROM daily_challenges WHERE isCompleted = 1")
  fun getCompletedCountFlow(): Flow<Int>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertChallenge(challenge: DailyChallengeEntity)

  @Update
  suspend fun updateChallenge(challenge: DailyChallengeEntity)

  @Query("UPDATE daily_challenges SET isCompleted = :completed, completedAt = :timestamp WHERE date = :date")
  suspend fun markCompleted(date: String, completed: Boolean, timestamp: Long)

  @Query("UPDATE daily_challenges SET isSkipped = 1 WHERE date = :date")
  suspend fun markSkipped(date: String)
}
