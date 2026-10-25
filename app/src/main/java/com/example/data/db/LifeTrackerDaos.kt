package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryAggregate
import com.example.data.model.CycleDayAverage
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.OverallStats
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY timeMinutes ASC, orderIndex ASC")
  fun getTasksForDate(date: String): Flow<List<DayTaskEntity>>

  @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY timeMinutes ASC, orderIndex ASC")
  suspend fun getTasksForDateSync(date: String): List<DayTaskEntity>

  @Query("SELECT * FROM day_tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timeMinutes ASC")
  fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<DayTaskEntity>>

  @Query("SELECT * FROM day_tasks ORDER BY date ASC, timeMinutes ASC")
  fun getAllTasks(): Flow<List<DayTaskEntity>>

  @Query("SELECT * FROM day_tasks ORDER BY date ASC, timeMinutes ASC")
  suspend fun getAllTasksSync(): List<DayTaskEntity>

  @Query("SELECT DISTINCT date FROM day_tasks ORDER BY date DESC")
  fun getDistinctDates(): Flow<List<String>>

  @Query("""
    SELECT 
      category,
      COUNT(*) AS totalCount,
      COALESCE(SUM(CASE WHEN status = 'COMPLETE' THEN 1 ELSE 0 END), 0) AS completedCount,
      COALESCE(SUM(CASE WHEN status = 'PARTIAL' THEN 1 ELSE 0 END), 0) AS partialCount
    FROM day_tasks
    WHERE category IS NOT NULL AND category != ''
    GROUP BY category
    ORDER BY totalCount DESC
  """)
  fun getCategoryAggregates(): Flow<List<CategoryAggregate>>

  @Query("SELECT * FROM day_tasks WHERE id = :id LIMIT 1")
  suspend fun getTaskById(id: Long): DayTaskEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: DayTaskEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTasks(tasks: List<DayTaskEntity>)

  @Update
  suspend fun updateTask(task: DayTaskEntity)

  @Delete
  suspend fun deleteTask(task: DayTaskEntity)

  @Query("DELETE FROM day_tasks WHERE id = :id")
  suspend fun deleteTaskById(id: Long)

  @Query("DELETE FROM day_tasks WHERE date = :date")
  suspend fun deleteTasksForDate(date: String)

  @Query("SELECT * FROM day_tasks WHERE templateId = :templateId")
  suspend fun getTasksForTemplateSync(templateId: Long): List<DayTaskEntity>

  @Query("UPDATE day_tasks SET templateId = :newTemplateId WHERE templateId = :oldTemplateId")
  suspend fun reassignTemplateId(oldTemplateId: Long, newTemplateId: Long)

  @Query("DELETE FROM day_tasks")
  suspend fun deleteAllTasks()
}

@Dao
interface RoutineDao {
  @Query("SELECT * FROM routine_templates ORDER BY timeMinutes ASC, orderIndex ASC")
  fun getAllRoutineTemplates(): Flow<List<RoutineTemplateEntity>>

  @Query("SELECT * FROM routine_templates ORDER BY timeMinutes ASC, orderIndex ASC")
  suspend fun getAllRoutineTemplatesSync(): List<RoutineTemplateEntity>

  @Query("SELECT * FROM routine_templates WHERE isActive = 1 ORDER BY timeMinutes ASC, orderIndex ASC")
  fun getActiveTemplates(): Flow<List<RoutineTemplateEntity>>

  @Query("SELECT * FROM routine_templates WHERE isActive = 1 ORDER BY timeMinutes ASC, orderIndex ASC")
  suspend fun getActiveTemplatesSync(): List<RoutineTemplateEntity>

  @Query("SELECT * FROM routine_templates WHERE id = :id LIMIT 1")
  suspend fun getRoutineTemplateById(id: Long): RoutineTemplateEntity?

  @Query("SELECT * FROM routine_templates WHERE activityKey = :key LIMIT 1")
  suspend fun getRoutineTemplateByActivityKey(key: String): RoutineTemplateEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoutineTemplate(template: RoutineTemplateEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoutineTemplates(templates: List<RoutineTemplateEntity>)

  @Update
  suspend fun updateRoutineTemplate(template: RoutineTemplateEntity)

  @Delete
  suspend fun deleteRoutineTemplate(template: RoutineTemplateEntity)

  @Query("DELETE FROM routine_templates WHERE id = :id")
  suspend fun deleteRoutineTemplateById(id: Long)

  @Query("SELECT COUNT(*) FROM routine_templates")
  suspend fun countRoutineTemplates(): Int

  @Query("DELETE FROM routine_templates")
  suspend fun deleteAllTemplates()
}

@Dao
interface ReflectionDao {
  @Query("SELECT * FROM reflections ORDER BY date DESC, timestamp DESC")
  fun getAllReflections(): Flow<List<ReflectionEntity>>

  @Query("SELECT * FROM reflections ORDER BY date DESC, timestamp DESC")
  suspend fun getAllReflectionsSync(): List<ReflectionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReflection(reflection: ReflectionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReflections(reflections: List<ReflectionEntity>)

  @Update
  suspend fun updateReflection(reflection: ReflectionEntity)

  @Delete
  suspend fun deleteReflection(reflection: ReflectionEntity)

  @Query("DELETE FROM reflections WHERE id = :id")
  suspend fun deleteReflectionById(id: Long)

  @Query("DELETE FROM reflections")
  suspend fun deleteAllReflections()
}

@Dao
interface GoalDao {
  @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
  fun getAllGoals(): Flow<List<GoalEntity>>

  @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
  suspend fun getAllGoalsSync(): List<GoalEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoal(goal: GoalEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoals(goals: List<GoalEntity>)

  @Update
  suspend fun updateGoal(goal: GoalEntity)

  @Delete
  suspend fun deleteGoal(goal: GoalEntity)

  @Query("DELETE FROM goals WHERE id = :id")
  suspend fun deleteGoalById(id: Long)

  @Query("DELETE FROM goals")
  suspend fun deleteAllGoals()
}

@Dao
interface UserSettingsDao {
  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  fun getSettings(): Flow<UserSettingsEntity?>

  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  suspend fun getSettingsSync(): UserSettingsEntity?

  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  suspend fun getSettingsDirect(): UserSettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(settings: UserSettingsEntity)
}

@Dao
interface DailySnapshotDao {
  @Query("SELECT * FROM daily_snapshots WHERE date = :date LIMIT 1")
  fun getSnapshotForDate(date: String): Flow<DailySnapshotEntity?>

  @Query("SELECT * FROM daily_snapshots WHERE date = :date LIMIT 1")
  suspend fun getSnapshotForDateSync(date: String): DailySnapshotEntity?

  @Query("SELECT * FROM daily_snapshots WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun getSnapshotsBetweenDates(startDate: String, endDate: String): Flow<List<DailySnapshotEntity>>

  @Query("SELECT * FROM daily_snapshots ORDER BY date DESC")
  fun getAllSnapshots(): Flow<List<DailySnapshotEntity>>

  @Query("SELECT * FROM daily_snapshots ORDER BY date DESC")
  suspend fun getAllSnapshotsSync(): List<DailySnapshotEntity>

  @Query("""
    SELECT 
      COUNT(*) AS totalDays,
      COALESCE(SUM(totalTasks), 0) AS totalTasks,
      COALESCE(SUM(completedCount), 0) AS completedTasks,
      COALESCE(SUM(partialCount), 0) AS partialTasks,
      COALESCE(SUM(missedCount), 0) AS missedTasks,
      COALESCE(SUM(totalScore), 0.0) AS totalScore,
      COALESCE(AVG(completionPercentage), 0.0) AS averagePercentage
    FROM daily_snapshots
  """)
  fun getOverallStats(): Flow<OverallStats>

  @Query("""
    SELECT 
      dayOfCycle,
      COUNT(*) AS dayCount,
      COALESCE(AVG(completionPercentage), 0.0) AS avgPercentage
    FROM daily_snapshots
    GROUP BY dayOfCycle
    ORDER BY dayOfCycle ASC
  """)
  fun getCycleDayAverages(): Flow<List<CycleDayAverage>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(snapshot: DailySnapshotEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateAll(snapshots: List<DailySnapshotEntity>)

  @Query("DELETE FROM daily_snapshots WHERE date = :date")
  suspend fun deleteSnapshotForDate(date: String)

  @Query("DELETE FROM daily_snapshots")
  suspend fun deleteAllSnapshots()
}

@Dao
interface JournalistPersonDao {
  @Query("SELECT * FROM journalist_persons ORDER BY CASE WHEN id = 'self' THEN 0 ELSE 1 END, createdAt ASC")
  fun getAllPersons(): Flow<List<JournalistPersonEntity>>

  @Query("SELECT * FROM journalist_persons ORDER BY CASE WHEN id = 'self' THEN 0 ELSE 1 END, createdAt ASC")
  suspend fun getAllPersonsSync(): List<JournalistPersonEntity>

  @Query("SELECT * FROM journalist_persons WHERE id = :id LIMIT 1")
  suspend fun getPersonById(id: String): JournalistPersonEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPerson(person: JournalistPersonEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPersons(persons: List<JournalistPersonEntity>)

  @Update
  suspend fun updatePerson(person: JournalistPersonEntity)

  @Delete
  suspend fun deletePerson(person: JournalistPersonEntity)

  @Query("DELETE FROM journalist_persons WHERE id = :id AND id != 'self'")
  suspend fun deletePersonById(id: String)
}

@Dao
interface JournalistEntryDao {
  @Query("SELECT * FROM journalist_entries ORDER BY date DESC, time DESC, createdAt DESC")
  fun getAllEntries(): Flow<List<JournalistEntryEntity>>

  @Query("SELECT * FROM journalist_entries ORDER BY date DESC, time DESC, createdAt DESC")
  suspend fun getAllEntriesSync(): List<JournalistEntryEntity>

  @Query("SELECT * FROM journalist_entries WHERE personId = :personId ORDER BY date DESC, time DESC, createdAt DESC")
  fun getEntriesForPerson(personId: String): Flow<List<JournalistEntryEntity>>

  @Query("SELECT * FROM journalist_entries WHERE personId = :personId ORDER BY date DESC, time DESC, createdAt DESC")
  suspend fun getEntriesForPersonSync(personId: String): List<JournalistEntryEntity>

  @Query("SELECT * FROM journalist_entries WHERE id = :id LIMIT 1")
  suspend fun getEntryById(id: String): JournalistEntryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntry(entry: JournalistEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntries(entries: List<JournalistEntryEntity>)

  @Update
  suspend fun updateEntry(entry: JournalistEntryEntity)

  @Delete
  suspend fun deleteEntry(entry: JournalistEntryEntity)

  @Query("DELETE FROM journalist_entries WHERE id = :id")
  suspend fun deleteEntryById(id: String)

  @Query("DELETE FROM journalist_entries WHERE personId = :personId")
  suspend fun deleteEntriesForPerson(personId: String)
}

@Dao
interface DailyNoteDao {
  @Query("SELECT * FROM daily_notes WHERE date = :date LIMIT 1")
  fun getNoteForDate(date: String): Flow<DailyNoteEntity?>

  @Query("SELECT * FROM daily_notes WHERE date = :date LIMIT 1")
  suspend fun getNoteForDateSync(date: String): DailyNoteEntity?

  @Query("SELECT * FROM daily_notes ORDER BY date DESC")
  fun getAllNotes(): Flow<List<DailyNoteEntity>>

  @Query("SELECT * FROM daily_notes ORDER BY date DESC")
  suspend fun getAllNotesSync(): List<DailyNoteEntity>

  @Query("SELECT * FROM daily_notes WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun getNotesBetweenDates(startDate: String, endDate: String): Flow<List<DailyNoteEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(note: DailyNoteEntity)

  @Query("DELETE FROM daily_notes WHERE date = :date")
  suspend fun deleteNoteForDate(date: String)

  @Query("DELETE FROM daily_notes")
  suspend fun deleteAllNotes()
}

@Dao
interface MeditationDao {
  @Query("SELECT * FROM meditation_sessions ORDER BY timestamp DESC")
  fun getAllSessions(): Flow<List<MeditationSessionEntity>>

  @Query("SELECT * FROM meditation_sessions ORDER BY timestamp DESC")
  suspend fun getAllSessionsSync(): List<MeditationSessionEntity>

  @Query("SELECT * FROM meditation_sessions WHERE date = :date ORDER BY timestamp DESC")
  fun getSessionsForDate(date: String): Flow<List<MeditationSessionEntity>>

  @Query("SELECT * FROM meditation_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY timestamp ASC")
  fun getSessionsBetweenDates(startDate: String, endDate: String): Flow<List<MeditationSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: MeditationSessionEntity): Long

  @Query("DELETE FROM meditation_sessions WHERE id = :id")
  suspend fun deleteSessionById(id: Long)

  @Query("SELECT COALESCE(SUM(completedSeconds), 0) / 60 FROM meditation_sessions WHERE isCompleted = 1")
  fun getTotalMeditationMinutes(): Flow<Int>

  @Query("SELECT COUNT(*) FROM meditation_sessions WHERE isCompleted = 1")
  fun getCompletedSessionsCount(): Flow<Int>
}

