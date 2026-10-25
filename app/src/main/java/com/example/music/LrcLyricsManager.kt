package com.example.music

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

data class LrcLine(
  val timestampMs: Long,
  val text: String
)

object LrcLyricsManager {

  private val TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})(?:\\.(\\d{2,3}))?]")

  /**
   * Searches for a matching .lrc file next to the song or in standard Music directories.
   */
  fun loadLyricsForSong(context: Context, songUri: String, songTitle: String): List<LrcLine>? {
    try {
      // 1. Try finding actual file path from MediaStore content URI
      val filePath = getFilePathFromUri(context, Uri.parse(songUri))
      if (!filePath.isNullOrBlank()) {
        val audioFile = File(filePath)
        val parentDir = audioFile.parentFile
        val baseName = audioFile.nameWithoutExtension

        if (parentDir != null && parentDir.exists()) {
          // Check <SongName>.lrc
          val directLrc = File(parentDir, "$baseName.lrc")
          if (directLrc.exists() && directLrc.isFile) {
            return parseLrcFile(directLrc)
          }

          // Case-insensitive search in same folder
          val matching = parentDir.listFiles { file ->
            file.extension.equals("lrc", ignoreCase = true) &&
              (file.nameWithoutExtension.equals(baseName, ignoreCase = true) ||
               file.nameWithoutExtension.contains(songTitle, ignoreCase = true))
          }
          if (!matching.isNullOrEmpty()) {
            return parseLrcFile(matching[0])
          }
        }
      }

      // 2. Check in standard Music / Download folders
      val commonFolders = mutableListOf(
        File("/storage/emulated/0/Music"),
        File("/storage/emulated/0/Download")
      )
      try {
        val musicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
        if (musicDir != null) commonFolders.add(musicDir)
      } catch (_: Exception) {}
      val cleanTitle = songTitle.trim().replace(Regex("[^a-zA-Z0-9\\s]"), "")
      for (folder in commonFolders) {
        if (folder.exists() && folder.isDirectory) {
          val candidate = File(folder, "$cleanTitle.lrc")
          if (candidate.exists()) return parseLrcFile(candidate)
        }
      }
    } catch (_: Exception) {}

    return null
  }

  fun parseLrcContent(content: String): List<LrcLine> {
    val lines = mutableListOf<LrcLine>()
    val reader = BufferedReader(java.io.StringReader(content))
    var line = reader.readLine()

    while (line != null) {
      val trimmed = line.trim()
      if (trimmed.isNotEmpty()) {
        val matcher = TIME_TAG_PATTERN.matcher(trimmed)
        var lastEnd = 0
        val timestamps = mutableListOf<Long>()

        while (matcher.find()) {
          val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
          val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
          val millisRaw = matcher.group(3)
          val millis = when {
            millisRaw == null -> 0L
            millisRaw.length == 2 -> (millisRaw.toLongOrNull() ?: 0L) * 10L
            millisRaw.length >= 3 -> (millisRaw.substring(0, 3).toLongOrNull() ?: 0L)
            else -> 0L
          }
          val totalMs = (minutes * 60 * 1000L) + (seconds * 1000L) + millis
          timestamps.add(totalMs)
          lastEnd = matcher.end()
        }

        if (timestamps.isNotEmpty()) {
          val lyricText = trimmed.substring(lastEnd).trim()
          for (timeMs in timestamps) {
            lines.add(LrcLine(timeMs, lyricText))
          }
        }
      }
      line = reader.readLine()
    }

    return lines.sortedBy { it.timestampMs }
  }

  fun parseLrcFile(file: File): List<LrcLine>? {
    return try {
      val content = file.readText(Charsets.UTF_8)
      val parsed = parseLrcContent(content)
      if (parsed.isNotEmpty()) parsed else null
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Finds index of the active lyric line for the current playback position.
   */
  fun findCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    if (positionMs < lines[0].timestampMs) return 0

    var low = 0
    var high = lines.size - 1
    var resultIndex = 0

    while (low <= high) {
      val mid = (low + high) ushr 1
      if (lines[mid].timestampMs <= positionMs) {
        resultIndex = mid
        low = mid + 1
      } else {
        high = mid - 1
      }
    }
    return resultIndex
  }

  private fun getFilePathFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "file") return uri.path
    if (uri.scheme == "content") {
      val projection = arrayOf(MediaStore.Audio.Media.DATA)
      try {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
          val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
          if (dataCol != -1 && cursor.moveToFirst()) {
            return cursor.getString(dataCol)
          }
        }
      } catch (_: Exception) {}
    }
    return null
  }
}
