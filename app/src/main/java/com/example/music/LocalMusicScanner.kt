package com.example.music

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object LocalMusicScanner {

  fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_MEDIA_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  /**
   * Scans strictly local audio files from device storage, excluding blacklisted folders
   * (e.g. WhatsApp audio, voice notes, call recordings, ringtones).
   * Absolutely NO fake built-in songs or mock playlists are seeded.
   */
  suspend fun scanDeviceAudio(context: Context): List<SongEntity> = withContext(Dispatchers.IO) {
    val songs = mutableListOf<SongEntity>()
    val blacklistManager = FolderBlacklistManager.getInstance(context)

    if (hasStoragePermission(context)) {
      try {
        val projection = arrayOf(
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.TITLE,
          MediaStore.Audio.Media.ARTIST,
          MediaStore.Audio.Media.ALBUM,
          MediaStore.Audio.Media.DURATION,
          MediaStore.Audio.Media.ALBUM_ID,
          MediaStore.Audio.Media.DATA
        )

        // Only music tracks with minimum duration of 15 seconds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          projection,
          selection,
          null,
          sortOrder
        )?.use { cursor ->
          val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
          val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
          val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
          val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
          val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
          val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
          val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

          while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "Unknown Song"
            val artist = cursor.getString(artistCol) ?: "Unknown Artist"
            val album = cursor.getString(albumCol) ?: "Unknown Album"
            val duration = cursor.getLong(durationCol)
            val albumId = cursor.getLong(albumIdCol)
            val filePath = if (dataCol != -1) cursor.getString(dataCol) else ""

            // Check if filePath is in blacklisted folder (e.g. WhatsApp, Call recordings)
            if (!filePath.isNullOrBlank() && blacklistManager.isPathBlacklisted(filePath)) {
              continue
            }

            val contentUri = ContentUris.withAppendedId(
              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
              id
            ).toString()

            val albumArtUri = "content://media/external/audio/albumart/$albumId"

            songs.add(
              SongEntity(
                id = "device_$id",
                title = title.trim(),
                artist = if (artist.isBlank() || artist == "<unknown>") "Local Artist" else artist.trim(),
                album = if (album.isBlank() || album == "<unknown>") "Local Music" else album.trim(),
                durationMs = duration,
                contentUri = contentUri,
                albumArtUri = albumArtUri,
                isFavorite = false,
                isBuiltIn = false,
                mood = "CUSTOM"
              )
            )
          }
        }
      } catch (_: Exception) {}
    }

    val db = LifeTrackerDatabase.getDatabase(context)
    val dao = db.musicDao()

    // Clean up legacy built-in placeholder songs if any existed previously
    try {
      val existingSongs = dao.getAllSongsDirect()
      val builtInIds = existingSongs.filter { it.isBuiltIn || it.id.startsWith("builtin_") }.map { it.id }
      for (bId in builtInIds) {
        dao.deleteSongById(bId)
      }
    } catch (_: Exception) {}

    // Insert or update scanned device songs
    if (songs.isNotEmpty()) {
      dao.insertSongs(songs)
    }

    songs
  }

  /**
   * Returns list of unique music folders on device with song count.
   */
  suspend fun getFolders(context: Context): List<Pair<String, Int>> = withContext(Dispatchers.IO) {
    val folderCounts = mutableMapOf<String, Int>()
    if (hasStoragePermission(context)) {
      try {
        val projection = arrayOf(MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        context.contentResolver.query(
          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          projection,
          selection,
          null,
          null
        )?.use { cursor ->
          val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
          if (dataCol != -1) {
            val blacklist = FolderBlacklistManager.getInstance(context)
            while (cursor.moveToNext()) {
              val path = cursor.getString(dataCol) ?: continue
              if (blacklist.isPathBlacklisted(path)) continue
              val parent = File(path).parentFile?.name ?: "Music"
              folderCounts[parent] = (folderCounts[parent] ?: 0) + 1
            }
          }
        }
      } catch (_: Exception) {}
    }
    folderCounts.toList().sortedBy { it.first.lowercase() }
  }
}
