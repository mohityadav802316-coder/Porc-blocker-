package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistRuleEntity
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

  // --- SONGS ---
  @Query("SELECT * FROM songs ORDER BY title ASC")
  fun getAllSongs(): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs ORDER BY title ASC")
  suspend fun getAllSongsDirect(): List<SongEntity>

  @Query("DELETE FROM songs WHERE id = :id")
  suspend fun deleteSongById(id: String)

  @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
  fun getFavoriteSongs(): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 30")
  fun getRecentlyPlayedSongs(): Flow<List<SongEntity>>

  @Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
  suspend fun getSongById(id: String): SongEntity?

  @Query("SELECT * FROM songs WHERE mood = :mood ORDER BY title ASC")
  suspend fun getSongsByMood(mood: String): List<SongEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSongs(songs: List<SongEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSong(song: SongEntity)

  @Update
  suspend fun updateSong(song: SongEntity)

  @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :songId")
  suspend fun updateFavoriteStatus(songId: String, isFav: Boolean)

  @Query("UPDATE songs SET lastPlayedAt = :timestamp, playCount = playCount + 1 WHERE id = :songId")
  suspend fun recordSongPlayed(songId: String, timestamp: Long)

  @Query("UPDATE songs SET mood = :mood WHERE id = :songId")
  suspend fun updateSongMood(songId: String, mood: String)

  // --- PLAYLISTS ---
  @Query("SELECT * FROM playlists ORDER BY name ASC")
  fun getAllPlaylists(): Flow<List<PlaylistEntity>>

  @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
  suspend fun getPlaylistById(id: Long): PlaylistEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Update
  suspend fun updatePlaylist(playlist: PlaylistEntity)

  @Query("DELETE FROM playlists WHERE id = :id")
  suspend fun deletePlaylist(id: Long)

  // --- PLAYLIST SONGS CROSS-REF ---
  @Query("""
    SELECT s.* FROM songs s
    INNER JOIN playlist_songs ps ON s.id = ps.songId
    WHERE ps.playlistId = :playlistId
    ORDER BY ps.orderIndex ASC, s.title ASC
  """)
  fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

  @Query("""
    SELECT s.* FROM songs s
    INNER JOIN playlist_songs ps ON s.id = ps.songId
    WHERE ps.playlistId = :playlistId
    ORDER BY ps.orderIndex ASC, s.title ASC
  """)
  suspend fun getSongsForPlaylistSync(playlistId: Long): List<SongEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

  @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
  suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

  @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
  suspend fun clearPlaylistSongs(playlistId: Long)

  @Query("SELECT playlistId FROM playlist_songs WHERE songId = :songId")
  suspend fun getPlaylistsForSong(songId: String): List<Long>

  // --- PLAYLIST RULES (Time + Mood + Routine) ---
  @Query("SELECT * FROM playlist_rules ORDER BY startMinutes ASC")
  fun getAllRules(): Flow<List<PlaylistRuleEntity>>

  @Query("SELECT * FROM playlist_rules WHERE playlistId = :playlistId ORDER BY startMinutes ASC")
  fun getRulesForPlaylist(playlistId: Long): Flow<List<PlaylistRuleEntity>>

  @Query("SELECT * FROM playlist_rules WHERE :currentMinutes >= startMinutes AND :currentMinutes < endMinutes")
  suspend fun getRulesMatchingTime(currentMinutes: Int): List<PlaylistRuleEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRule(rule: PlaylistRuleEntity): Long

  @Query("DELETE FROM playlist_rules WHERE id = :ruleId")
  suspend fun deleteRule(ruleId: Long)
}
