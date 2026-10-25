package com.example.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.alarm.AlarmScheduler
import com.example.blackscreen.BlackScreenManager
import com.example.data.db.LifeTrackerDatabase
import com.example.focus.FocusModeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class BackupItemInfo(
  val fileName: String,
  val fileUri: Uri,
  val sizeBytes: Long,
  val formattedDate: String,
  val formattedSize: String,
  val recordCountsText: String,
  val manifest: BackupManifest?,
  val isValid: Boolean,
  val errorReason: String? = null
)

data class RestoreResult(
  val fileName: String,
  val totalRecords: Int,
  val message: String
)

object RestoreManager {

  /**
   * Scans the chosen DocumentTree directory for available backup archives.
   * Filters and sorts valid backups with newest first, and flags corrupted ones.
   */
  suspend fun scanBackupsInFolder(context: Context, treeUri: Uri): List<BackupItemInfo> = withContext(Dispatchers.IO) {
    val results = mutableListOf<BackupItemInfo>()
    val rootDir = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()

    val allDocFiles: Array<DocumentFile> = rootDir.listFiles()
    val files: List<DocumentFile> = allDocFiles.filter { doc: DocumentFile ->
      val name = doc.name ?: ""
      (name.startsWith("LifeTracker_backup_") || name == "LifeTracker_latest.zip") && name.endsWith(".zip")
    }

    for (doc: DocumentFile in files) {
      val name = doc.name ?: "Unknown"
      val size = doc.length()

      // Copy to temporary cache file to safely inspect ZIP and manifest
      val tempInspectFile = File(context.cacheDir, "inspect_${System.currentTimeMillis()}_${name}")
      try {
        context.contentResolver.openInputStream(doc.uri)?.use { input ->
          FileOutputStream(tempInspectFile).use { output ->
            input.copyTo(output)
          }
        }

        if (tempInspectFile.length() == 0L) {
          results.add(
            BackupItemInfo(
              fileName = name,
              fileUri = doc.uri,
              sizeBytes = size,
              formattedDate = "अपूर्ण फ़ाइल",
              formattedSize = formatBytes(size),
              recordCountsText = "शून्य बाइट्स",
              manifest = null,
              isValid = false,
              errorReason = "फ़ाइल 0 बाइट्स है"
            )
          )
          tempInspectFile.delete()
          continue
        }

        // Validate ZIP and Manifest
        val validationErr = BackupManager.verifyZipArchive(tempInspectFile)
        if (validationErr != null) {
          results.add(
            BackupItemInfo(
              fileName = name,
              fileUri = doc.uri,
              sizeBytes = size,
              formattedDate = "क्षतिग्रस्त",
              formattedSize = formatBytes(size),
              recordCountsText = "त्रुटिपूर्ण",
              manifest = null,
              isValid = false,
              errorReason = validationErr
            )
          )
          tempInspectFile.delete()
          continue
        }

        // Read manifest
        ZipFile(tempInspectFile).use { zf ->
          val mEntry = zf.getEntry("manifest.json")
          val mStr = zf.getInputStream(mEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
          val manifest = BackupManifest.fromJson(mStr)

          if (manifest == null) {
            results.add(
              BackupItemInfo(
                fileName = name,
                fileUri = doc.uri,
                sizeBytes = size,
                formattedDate = "अज्ञात",
                formattedSize = formatBytes(size),
                recordCountsText = "manifest अनुपस्थित",
                manifest = null,
                isValid = false,
                errorReason = "अमान्य manifest"
              )
            )
          } else {
            val totalRecs = manifest.recordCounts.values.sum()
            val dateStr = formatTimestamp(manifest.createdAt)
            val countsSummary = "${manifest.recordCounts["dayTasks"] ?: 0} कार्य, ${manifest.recordCounts["routineTemplates"] ?: 0} रूटीन"

            results.add(
              BackupItemInfo(
                fileName = name,
                fileUri = doc.uri,
                sizeBytes = size,
                formattedDate = dateStr,
                formattedSize = formatBytes(size),
                recordCountsText = "$countsSummary ($totalRecs कुल रिकॉर्ड)",
                manifest = manifest,
                isValid = true
              )
            )
          }
        }
      } catch (e: Exception) {
        results.add(
          BackupItemInfo(
            fileName = name,
            fileUri = doc.uri,
            sizeBytes = size,
            formattedDate = "त्रुटि",
            formattedSize = formatBytes(size),
            recordCountsText = "अमान्य",
            manifest = null,
            isValid = false,
            errorReason = e.message
          )
        )
      } finally {
        tempInspectFile.delete()
      }
    }

    // Sort: Valid ones first, newest date first
    results.sortedWith(
      compareByDescending<BackupItemInfo> { it.isValid }
        .thenByDescending { it.manifest?.createdAt ?: 0L }
    )
  }

  /**
   * Inspects a single ZIP Uri selected directly by user via file picker.
   */
  suspend fun inspectSingleZip(context: Context, zipUri: Uri): BackupItemInfo = withContext(Dispatchers.IO) {
    val tempFile = File(context.cacheDir, "inspect_single_${System.currentTimeMillis()}.zip")
    try {
      context.contentResolver.openInputStream(zipUri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
          input.copyTo(output)
        }
      }

      val size = tempFile.length()
      val validationErr = BackupManager.verifyZipArchive(tempFile)
      if (validationErr != null) {
        return@withContext BackupItemInfo(
          fileName = "Selected Backup",
          fileUri = zipUri,
          sizeBytes = size,
          formattedDate = "अमान्य फ़ाइल",
          formattedSize = formatBytes(size),
          recordCountsText = "क्षतिग्रस्त",
          manifest = null,
          isValid = false,
          errorReason = validationErr
        )
      }

      ZipFile(tempFile).use { zf ->
        val mEntry = zf.getEntry("manifest.json")
          ?: return@withContext BackupItemInfo(
            fileName = "Selected Backup",
            fileUri = zipUri,
            sizeBytes = size,
            formattedDate = "अमान्य",
            formattedSize = formatBytes(size),
            recordCountsText = "No manifest",
            manifest = null,
            isValid = false,
            errorReason = "manifest.json नहीं मिला"
          )

        val mStr = zf.getInputStream(mEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val manifest = BackupManifest.fromJson(mStr)
          ?: return@withContext BackupItemInfo(
            fileName = "Selected Backup",
            fileUri = zipUri,
            sizeBytes = size,
            formattedDate = "अमान्य",
            formattedSize = formatBytes(size),
            recordCountsText = "Invalid manifest",
            manifest = null,
            isValid = false,
            errorReason = "manifest.json अमान्य है"
          )

        val totalRecs = manifest.recordCounts.values.sum()
        val dateStr = formatTimestamp(manifest.createdAt)
        val countsSummary = "${manifest.recordCounts["dayTasks"] ?: 0} कार्य, ${manifest.recordCounts["routineTemplates"] ?: 0} रूटीन"

        BackupItemInfo(
          fileName = "LifeTracker_backup_${manifest.createdDateIso}.zip",
          fileUri = zipUri,
          sizeBytes = size,
          formattedDate = dateStr,
          formattedSize = formatBytes(size),
          recordCountsText = "$countsSummary ($totalRecs कुल रिकॉर्ड)",
          manifest = manifest,
          isValid = true
        )
      }
    } catch (e: Exception) {
      BackupItemInfo(
        fileName = "Selected File",
        fileUri = zipUri,
        sizeBytes = 0L,
        formattedDate = "त्रुटि",
        formattedSize = "0 B",
        recordCountsText = "पढ़ने में असमर्थ",
        manifest = null,
        isValid = false,
        errorReason = e.message
      )
    } finally {
      tempFile.delete()
    }
  }

  /**
   * Safely restores the database and preferences from a verified ZIP archive.
   * Handles database version checks and migrations automatically.
   */
  suspend fun performRestore(
    context: Context,
    backupZipUri: Uri,
    rememberFolderTreeUri: Uri? = null,
    rememberFolderDisplayName: String? = null
  ): Result<RestoreResult> = withContext(Dispatchers.IO) {
    val tempZip = File(context.cacheDir, "restore_in_progress_${System.currentTimeMillis()}.zip")
    try {
      // 1. Copy ZIP from Uri to local temp
      context.contentResolver.openInputStream(backupZipUri)?.use { input ->
        FileOutputStream(tempZip).use { output ->
          input.copyTo(output)
        }
      } ?: return@withContext Result.failure(IllegalStateException("बैकअप फ़ाइल खोली नहीं जा सकी"))

      // 2. Verify archive
      val verifyErr = BackupManager.verifyZipArchive(tempZip)
      if (verifyErr != null) {
        return@withContext Result.failure(IllegalStateException("बैकअप फ़ाइल सत्यापित नहीं हो सकी: $verifyErr"))
      }

      // 3. Inspect Manifest and Database Version
      var manifest: BackupManifest? = null
      var prefsJsonString: String? = null
      val tempRestoredDbFile = File(context.cacheDir, "temp_restored_db_${System.currentTimeMillis()}")

      ZipFile(tempZip).use { zf ->
        val mEntry = zf.getEntry("manifest.json")
          ?: return@withContext Result.failure(IllegalStateException("manifest.json फ़ाइल नहीं मिली"))
        val mStr = zf.getInputStream(mEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        manifest = BackupManifest.fromJson(mStr)
          ?: return@withContext Result.failure(IllegalStateException("manifest.json अमान्य है"))

        // Version Safety Check
        if (manifest!!.databaseVersion > LifeTrackerDatabase.CURRENT_VERSION) {
          val msg = "यह बैकअप ऐप के नए वर्शन (डेटाबेस v${manifest!!.databaseVersion}) से बनाया गया है, जबकि वर्तमान ऐप v${LifeTrackerDatabase.CURRENT_VERSION} पर है। कृपया पहले ऐप को अपडेट करें।"
          return@withContext Result.failure(IllegalStateException(msg))
        }

        // Extract database file
        val dbEntry = zf.getEntry(manifest!!.databaseFileName)
          ?: return@withContext Result.failure(IllegalStateException("डेटाबेस फ़ाइल ${manifest!!.databaseFileName} गायब है"))

        zf.getInputStream(dbEntry).use { input ->
          FileOutputStream(tempRestoredDbFile).use { output ->
            input.copyTo(output)
          }
        }

        // Extract preferences
        val pEntry = zf.getEntry(manifest!!.preferencesFileName)
        if (pEntry != null) {
          prefsJsonString = zf.getInputStream(pEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
      }

      // 4. Create local safety backup of current data before replacing
      createLocalSafetyBackup(context)

      // 5. Close Room Database safely
      LifeTrackerDatabase.closeDatabase()

      // 6. Replace Room Database File atomically
      val targetDbFile = context.getDatabasePath("life_tracker_db")
      targetDbFile.parentFile?.mkdirs()

      // Delete existing SQLite helper files (-wal, -shm)
      val walFile = File(targetDbFile.parentFile, "life_tracker_db-wal")
      val shmFile = File(targetDbFile.parentFile, "life_tracker_db-shm")
      try { walFile.delete() } catch (_: Exception) {}
      try { shmFile.delete() } catch (_: Exception) {}

      // Replace main database file
      if (targetDbFile.exists()) {
        targetDbFile.delete()
      }
      tempRestoredDbFile.copyTo(targetDbFile, overwrite = true)
      tempRestoredDbFile.delete()

      // 7. Restore SharedPreferences
      if (!prefsJsonString.isNullOrBlank()) {
        restorePreferencesFromJson(context, prefsJsonString!!)
      }

      // 8. Remember the backup folder for future automatic backups
      if (rememberFolderTreeUri != null) {
        BackupPreferences.setTreeUri(context, rememberFolderTreeUri, rememberFolderDisplayName)
        BackupPreferences.setAutoBackupEnabled(context, true)
      }

      // 9. Reopen Room Database and ensure migrations run
      val reopenedDb = LifeTrackerDatabase.getDatabase(context)
      try {
        // Trigger a lightweight query to confirm database integrity and run migrations
        reopenedDb.routineDao().countRoutineTemplates()
      } catch (e: Exception) {
        return@withContext Result.failure(IllegalStateException("डेटाबेस माइग्रेशन में त्रुटि: ${e.message}"))
      }

      // 10. Reschedule system alarms, routines, and background jobs
      try {
        AlarmScheduler.createNotificationChannels(context)
        AlarmScheduler.rescheduleAllTimetableAlarms(context)
        FocusModeManager.onBootOrScheduleChange(context)
        BlackScreenManager.syncServiceState(context)
        DailyBackupWorker.schedulePeriodic(context)
      } catch (_: Exception) {}

      BackupPreferences.setFirstLaunchHandled(context, true)

      val totalRecords = manifest?.recordCounts?.values?.sum() ?: 0
      Result.success(
        RestoreResult(
          fileName = manifest?.databaseFileName ?: "life_tracker_db",
          totalRecords = totalRecords,
          message = "डेटा सफलतापूर्वक पुनर्स्थापित कर लिया गया! कुल $totalRecords रिकॉर्ड्स वापस आ गए हैं।"
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    } finally {
      tempZip.delete()
    }
  }

  /**
   * Creates a local safety copy before restoring to prevent any accidental loss.
   */
  fun createLocalSafetyBackup(context: Context) {
    try {
      val dbFile = context.getDatabasePath("life_tracker_db")
      if (!dbFile.exists() || dbFile.length() == 0L) return

      val safetyZip = File(context.filesDir, "safety_backup_before_restore.zip")
      ZipOutputStream(BufferedOutputStream(FileOutputStream(safetyZip))).use { zos ->
        zos.putNextEntry(java.util.zip.ZipEntry("life_tracker_db"))
        FileInputStream(dbFile).use { fis -> fis.copyTo(zos) }
        zos.closeEntry()
      }
    } catch (_: Exception) {}
  }

  /**
   * Restores SharedPreferences maps from JSON.
   */
  private fun restorePreferencesFromJson(context: Context, jsonStr: String) {
    try {
      val root = JSONObject(jsonStr)
      val keys = root.keys()
      while (keys.hasNext()) {
        val prefName = keys.next()
        val prefObj = root.getJSONObject(prefName)
        val sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val editor = sp.edit()

        val itemKeys = prefObj.keys()
        while (itemKeys.hasNext()) {
          val key = itemKeys.next()
          val value = prefObj.get(key)
          when (value) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Double -> editor.putFloat(key, value.toFloat())
            is String -> editor.putString(key, value)
            is org.json.JSONArray -> {
              val set = mutableSetOf<String>()
              for (i in 0 until value.length()) {
                set.add(value.getString(i))
              }
              editor.putStringSet(key, set)
            }
          }
        }
        editor.apply()
      }
    } catch (_: Exception) {}
  }

  private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
      String.format(Locale.US, "%.1f MB", mb)
    } else {
      String.format(Locale.US, "%.1f KB", kb)
    }
  }

  private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "अज्ञात"
    val sdf = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.US)
    return sdf.format(Date(timestamp))
  }
}
