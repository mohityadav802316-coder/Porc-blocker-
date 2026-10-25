package com.example.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object BackupPreferences {
  private const val PREFS_NAME = "life_tracker_backup_prefs"
  private const val KEY_TREE_URI = "backup_tree_uri"
  private const val KEY_FOLDER_NAME = "backup_folder_display_name"
  private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
  private const val KEY_BACKUP_HOUR = "backup_hour"
  private const val KEY_BACKUP_MINUTE = "backup_minute"
  private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
  private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
  private const val KEY_LAST_BACKUP_FILENAME = "last_backup_filename"
  private const val KEY_LAST_BACKUP_STATUS = "last_backup_status"
  private const val KEY_LAST_BACKUP_ERROR = "last_backup_error"
  private const val KEY_FIRST_LAUNCH_HANDLED = "first_launch_handled_v2"

  const val STATUS_OK = "OK"
  const val STATUS_ERROR = "ERROR"
  const val STATUS_NEVER = "NEVER"
  const val STATUS_NO_FOLDER = "NO_FOLDER"

  fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun getTreeUri(context: Context): Uri? {
    val str = getPrefs(context).getString(KEY_TREE_URI, null) ?: return null
    return try {
      Uri.parse(str)
    } catch (_: Exception) {
      null
    }
  }

  fun setTreeUri(context: Context, uri: Uri?, displayName: String?) {
    getPrefs(context).edit().apply {
      if (uri != null) {
        putString(KEY_TREE_URI, uri.toString())
        putString(KEY_FOLDER_NAME, displayName ?: uri.lastPathSegment ?: "Backup Folder")
      } else {
        remove(KEY_TREE_URI)
        remove(KEY_FOLDER_NAME)
      }
      apply()
    }
  }

  fun getFolderDisplayName(context: Context): String? {
    return getPrefs(context).getString(KEY_FOLDER_NAME, null)
  }

  /**
   * Verifies if the persistable URI permission is still valid and the directory is accessible.
   * If permission was revoked or folder deleted, returns false.
   */
  fun isFolderAccessible(context: Context): Boolean {
    val uri = getTreeUri(context) ?: return false
    return try {
      // Check if permission still held
      val hasPermission = context.contentResolver.persistedUriPermissions.any {
        it.uri == uri && (it.isWritePermission || it.isReadPermission)
      }
      if (!hasPermission) return false

      val docFile = DocumentFile.fromTreeUri(context, uri)
      docFile != null && docFile.exists() && docFile.isDirectory && docFile.canWrite()
    } catch (_: Exception) {
      false
    }
  }

  fun isAutoBackupEnabled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
  }

  fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
    getPrefs(context).edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
  }

  fun getBackupTime(context: Context): Pair<Int, Int> {
    val p = getPrefs(context)
    val hour = p.getInt(KEY_BACKUP_HOUR, 5)
    val minute = p.getInt(KEY_BACKUP_MINUTE, 30)
    return Pair(hour, minute)
  }

  fun setBackupTime(context: Context, hour: Int, minute: Int) {
    getPrefs(context).edit()
      .putInt(KEY_BACKUP_HOUR, hour.coerceIn(0, 23))
      .putInt(KEY_BACKUP_MINUTE, minute.coerceIn(0, 59))
      .apply()
  }

  fun getLastBackupTimestamp(context: Context): Long {
    return getPrefs(context).getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
  }

  fun getLastBackupSize(context: Context): Long {
    return getPrefs(context).getLong(KEY_LAST_BACKUP_SIZE, 0L)
  }

  fun getLastBackupFileName(context: Context): String? {
    return getPrefs(context).getString(KEY_LAST_BACKUP_FILENAME, null)
  }

  fun getLastBackupStatus(context: Context): String {
    if (!isFolderAccessible(context)) {
      return if (getTreeUri(context) == null) STATUS_NO_FOLDER else STATUS_ERROR
    }
    return getPrefs(context).getString(KEY_LAST_BACKUP_STATUS, STATUS_NEVER) ?: STATUS_NEVER
  }

  fun getLastBackupError(context: Context): String? {
    if (getTreeUri(context) != null && !isFolderAccessible(context)) {
      return "चयनित बैकअप फ़ोल्डर तक पहुंच नहीं है (अनुमति समाप्त हो गई या फ़ोल्डर हटा दिया गया)। कृपया फ़ोल्डर पुनः चुनें।"
    }
    return getPrefs(context).getString(KEY_LAST_BACKUP_ERROR, null)
  }

  fun recordBackupSuccess(context: Context, fileName: String, sizeBytes: Long) {
    getPrefs(context).edit()
      .putLong(KEY_LAST_BACKUP_TIMESTAMP, System.currentTimeMillis())
      .putLong(KEY_LAST_BACKUP_SIZE, sizeBytes)
      .putString(KEY_LAST_BACKUP_FILENAME, fileName)
      .putString(KEY_LAST_BACKUP_STATUS, STATUS_OK)
      .remove(KEY_LAST_BACKUP_ERROR)
      .apply()
  }

  fun recordBackupFailure(context: Context, errorMessage: String) {
    getPrefs(context).edit()
      .putString(KEY_LAST_BACKUP_STATUS, STATUS_ERROR)
      .putString(KEY_LAST_BACKUP_ERROR, errorMessage)
      .apply()
  }

  fun isFirstLaunchHandled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH_HANDLED, false)
  }

  fun setFirstLaunchHandled(context: Context, handled: Boolean = true) {
    getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH_HANDLED, handled).apply()
  }
}
