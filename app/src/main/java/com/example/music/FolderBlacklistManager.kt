package com.example.music

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages blacklisted folders to exclude non-music audio (e.g. WhatsApp audio,
 * voice notes, call recordings, ringtones) from the local music library.
 */
class FolderBlacklistManager private constructor(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("music_folder_blacklist_prefs", Context.MODE_PRIVATE)

  // Default folder keywords that should be excluded by default
  val defaultBlacklistKeywords = setOf(
    "whatsapp audio",
    "whatsapp voice notes",
    "whatsapp animated gifs",
    "callrecordings",
    "call recordings",
    "call_recordings",
    "voice recorder",
    "voicerecorder",
    "sound_recorder",
    "telegram audio",
    "telegram files",
    "ringtones",
    "notifications",
    "alarms",
    ".nomedia"
  )

  /**
   * Returns whether a given file path is in a blacklisted folder or matches excluded patterns.
   */
  fun isPathBlacklisted(filePath: String): Boolean {
    val lower = filePath.lowercase()
    val userBlacklist = getCustomBlacklist()

    // Check user custom folders
    for (folder in userBlacklist) {
      if (lower.contains(folder.lowercase())) return true
    }

    // Check default keywords
    for (kw in defaultBlacklistKeywords) {
      if (lower.contains(kw)) return true
    }

    return false
  }

  fun getCustomBlacklist(): Set<String> {
    return prefs.getStringSet(KEY_CUSTOM_BLACKLIST, emptySet()) ?: emptySet()
  }

  fun addCustomBlacklist(folderNameOrPath: String) {
    val current = getCustomBlacklist().toMutableSet()
    current.add(folderNameOrPath.trim())
    prefs.edit().putStringSet(KEY_CUSTOM_BLACKLIST, current).apply()
  }

  fun removeCustomBlacklist(folderNameOrPath: String) {
    val current = getCustomBlacklist().toMutableSet()
    current.remove(folderNameOrPath.trim())
    prefs.edit().putStringSet(KEY_CUSTOM_BLACKLIST, current).apply()
  }

  fun resetBlacklist() {
    prefs.edit().remove(KEY_CUSTOM_BLACKLIST).apply()
  }

  companion object {
    private const val KEY_CUSTOM_BLACKLIST = "key_custom_blacklist"

    @Volatile
    private var instance: FolderBlacklistManager? = null

    fun getInstance(context: Context): FolderBlacklistManager {
      return instance ?: synchronized(this) {
        instance ?: FolderBlacklistManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
