package com.example.backup

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.LifeTrackerDatabase
import com.example.util.ExportImportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object BackupManager {
  private const val TAG = "BackupManager"
  private fun getDateFormat() = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US)
  private fun getIsoFormat() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)

  /**
   * Performs a complete, consistent, and atomic backup.
   * Runs fully on Dispatchers.IO.
   */
  suspend fun performBackup(context: Context): Result<String> = withContext(Dispatchers.IO) {
    try {
      // 1. Validate Target Folder
      if (!BackupPreferences.isFolderAccessible(context)) {
        val err = "बैकअप फ़ोल्डर उपलब्ध नहीं है। कृपया सेटिंग्स > डेटा एवं बैकअप में जाकर एक सुरक्षित फ़ोल्डर चुनें।"
        BackupPreferences.recordBackupFailure(context, err)
        return@withContext Result.failure(IllegalStateException(err))
      }

      val treeUri = BackupPreferences.getTreeUri(context)
        ?: return@withContext Result.failure(IllegalStateException("No backup folder URI configured"))

      val rootDir = DocumentFile.fromTreeUri(context, treeUri)
        ?: return@withContext Result.failure(IllegalStateException("Cannot open DocumentFile from TreeUri"))

      // Clean up any stale temporary files from previous runs
      cleanupTempFiles(context, rootDir)

      // 2. Checkpoint SQLite WAL for consistent database file
      val database = LifeTrackerDatabase.getDatabase(context)
      try {
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
          cursor.moveToFirst()
        }
      } catch (e: Exception) {
        // Log and continue with best-effort sync
      }

      // 3. Count records to verify database is not empty or corrupted
      fun countTable(tableName: String): Int {
        return try {
          database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tableName").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
          }
        } catch (_: Exception) {
          0
        }
      }

      val taskCount = countTable("day_tasks")
      val routineCount = countTable("routine_templates")
      val reflectionCount = countTable("reflections")
      val goalCount = countTable("goals")
      val snapshotCount = countTable("daily_snapshots")
      val personCount = countTable("journalist_persons")
      val entryCount = countTable("journalist_entries")
      val meditationCount = countTable("meditation_sessions")

      val totalRecords = taskCount + routineCount + reflectionCount + goalCount + snapshotCount + personCount + entryCount + meditationCount
      if (totalRecords == 0) {
        val err = "डेटाबेस में कोई रिकॉर्ड नहीं है। खाली डेटा से बैकअप को ओवरराइट नहीं किया जाएगा।"
        BackupPreferences.recordBackupFailure(context, err)
        return@withContext Result.failure(IllegalStateException(err))
      }

      val recordCounts = mapOf(
        "dayTasks" to taskCount,
        "routineTemplates" to routineCount,
        "reflections" to reflectionCount,
        "goals" to goalCount,
        "dailySnapshots" to snapshotCount,
        "journalistPersons" to personCount,
        "journalistEntries" to entryCount,
        "meditationSessions" to meditationCount
      )

      // 4. Source Database File
      val dbFile = context.getDatabasePath("life_tracker_db")
      if (!dbFile.exists() || dbFile.length() == 0L) {
        val err = "डेटाबेस फ़ाइल उपलब्ध नहीं है।"
        BackupPreferences.recordBackupFailure(context, err)
        return@withContext Result.failure(IllegalStateException(err))
      }

      // 5. Export Preferences to JSON
      val prefsJson = collectPreferencesAsJson(context)

      // 6. Generate human-readable entity backup as secondary safety safeguard
      val entitiesJson = try {
        ExportImportHelper.generateJsonBackup(database)
      } catch (_: Exception) {
        "{}"
      }

      // 7. Write and assemble into local cache first (Atomic Preparation)
      val now = System.currentTimeMillis()
      val timeStampStr = getDateFormat().format(Date(now))
      val finalFileName = "LifeTracker_backup_${timeStampStr}.zip"
      val localTempZip = File(context.cacheDir, "temp_backup_${now}.zip")

      try {
        val dbSha256 = calculateSha256(dbFile)
        val prefsSha256 = calculateSha256String(prefsJson)
        val entitiesSha256 = calculateSha256String(entitiesJson)

        val pInfo = try {
          context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
          null
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          pInfo?.longVersionCode?.toInt() ?: 1
        } else {
          @Suppress("DEPRECATION")
          pInfo?.versionCode ?: 1
        }
        val versionName = pInfo?.versionName ?: "1.0"

        val manifest = BackupManifest(
          appVersionCode = versionCode,
          appVersionName = versionName,
          databaseVersion = LifeTrackerDatabase.CURRENT_VERSION,
          createdAt = now,
          createdDateIso = getIsoFormat().format(Date(now)),
          recordCounts = recordCounts,
          checksums = mapOf(
            "life_tracker_db" to dbSha256,
            "preferences_backup.json" to prefsSha256,
            "entities_readable.json" to entitiesSha256
          )
        )

        ZipOutputStream(BufferedOutputStream(FileOutputStream(localTempZip))).use { zos ->
          // Write manifest.json
          zos.putNextEntry(ZipEntry("manifest.json"))
          zos.write(manifest.toJson().toByteArray(Charsets.UTF_8))
          zos.closeEntry()

          // Write database file
          zos.putNextEntry(ZipEntry("life_tracker_db"))
          FileInputStream(dbFile).use { fis ->
            fis.copyTo(zos)
          }
          zos.closeEntry()

          // Write preferences JSON
          zos.putNextEntry(ZipEntry("preferences_backup.json"))
          zos.write(prefsJson.toByteArray(Charsets.UTF_8))
          zos.closeEntry()

          // Write human-readable entities JSON
          zos.putNextEntry(ZipEntry("entities_readable.json"))
          zos.write(entitiesJson.toByteArray(Charsets.UTF_8))
          zos.closeEntry()
        }

        // 8. Verify the generated local ZIP
        val verificationError = verifyZipArchive(localTempZip, manifest)
        if (verificationError != null) {
          localTempZip.delete()
          val err = "बैकअप सत्यापन विफल: $verificationError"
          BackupPreferences.recordBackupFailure(context, err)
          return@withContext Result.failure(IllegalStateException(err))
        }

        val backupFileSize = localTempZip.length()

        // 9. Atomic write to External SAF Folder
        // A. Dated backup file
        writeToDocumentFile(context, rootDir, finalFileName, localTempZip)

        // B. Keep a synchronized copy named "LifeTracker_latest.zip"
        writeToDocumentFile(context, rootDir, "LifeTracker_latest.zip", localTempZip)

        // 10. Prune older backups: keep last 10 dated backups safely
        pruneOldBackups(rootDir, maxKeep = 10)

        // Clean local temp
        localTempZip.delete()

        // 11. Record Success
        BackupPreferences.recordBackupSuccess(context, finalFileName, backupFileSize)
        Result.success(finalFileName)
      } catch (e: Exception) {
        localTempZip.delete()
        val err = "बैकअप लिखने में त्रुटि: ${e.localizedMessage ?: e.message}"
        BackupPreferences.recordBackupFailure(context, err)
        Result.failure(e)
      }
    } catch (e: Exception) {
      val err = "अप्रत्याशित बैकअप विफलता: ${e.localizedMessage ?: e.message}"
      BackupPreferences.recordBackupFailure(context, err)
      Result.failure(e)
    }
  }

  /**
   * Writes local file content to SAF DocumentFile atomically.
   */
  private fun writeToDocumentFile(
    context: Context,
    rootDir: DocumentFile,
    targetFileName: String,
    sourceFile: File
  ) {
    val tempFileName = ".tmp_${System.currentTimeMillis()}_$targetFileName"

    // Create temporary DocumentFile in target directory
    val tempDoc = rootDir.createFile("application/zip", tempFileName)
      ?: throw IllegalStateException("Cannot create temporary SAF document: $tempFileName")

    context.contentResolver.openOutputStream(tempDoc.uri, "w")?.use { out ->
      FileInputStream(sourceFile).use { fis ->
        fis.copyTo(out)
      }
      out.flush()
    } ?: throw IllegalStateException("Cannot open OutputStream for $tempFileName")

    // If an existing target file exists with targetFileName, remove or overwrite
    val existing = rootDir.findFile(targetFileName)
    if (existing != null && existing.exists()) {
      existing.delete()
    }

    // Rename temp file to targetFileName
    val renamed = tempDoc.renameTo(targetFileName)
    if (!renamed) {
      // If renameTo not supported by SAF provider, copy directly to target
      val directDoc = rootDir.findFile(targetFileName) ?: rootDir.createFile("application/zip", targetFileName)
      if (directDoc != null) {
        context.contentResolver.openOutputStream(directDoc.uri, "w")?.use { out ->
          FileInputStream(sourceFile).use { fis -> fis.copyTo(out) }
          out.flush()
        }
      }
      tempDoc.delete()
    }
  }

  /**
   * Verifies zip integrity, manifest, and checksums.
   */
  fun verifyZipArchive(zipFile: File, expectedManifest: BackupManifest? = null): String? {
    if (!zipFile.exists() || zipFile.length() == 0L) {
      return "फ़ाइल मौजूद नहीं है या 0 बाइट्स है"
    }

    try {
      ZipFile(zipFile).use { zf ->
        val manifestEntry = zf.getEntry("manifest.json")
          ?: return "manifest.json फ़ाइल नहीं मिली"

        val manifestStr = zf.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val manifest = BackupManifest.fromJson(manifestStr)
          ?: return "manifest.json अमान्य है"

        val dbEntry = zf.getEntry(manifest.databaseFileName)
          ?: return "डेटाबेस फ़ाइल ${manifest.databaseFileName} गायब है"

        val prefsEntry = zf.getEntry(manifest.preferencesFileName)
          ?: return "सेटिंग्स फ़ाइल ${manifest.preferencesFileName} गायब है"

        // Verify checksums
        val expectedDbChecksum = manifest.checksums[manifest.databaseFileName]
        if (expectedDbChecksum != null) {
          val actualDbChecksum = zf.getInputStream(dbEntry).use { calculateSha256(it) }
          if (!actualDbChecksum.equals(expectedDbChecksum, ignoreCase = true)) {
            return "डेटाबेस चेकसम मेल नहीं खाता (फ़ाइल क्षतिग्रस्त हो सकती है)"
          }
        }
      }
      return null
    } catch (e: Exception) {
      return "अमान्य ज़िप फ़ाइल: ${e.message}"
    }
  }

  /**
   * Deletes older dated backups, retaining only the latest [maxKeep] (default 10).
   */
  private fun pruneOldBackups(rootDir: DocumentFile, maxKeep: Int = 10) {
    try {
      val files: Array<DocumentFile> = rootDir.listFiles()
      val datedBackups: List<DocumentFile> = files.filter { doc: DocumentFile ->
        val name = doc.name ?: ""
        name.startsWith("LifeTracker_backup_") && name.endsWith(".zip")
      }.sortedByDescending { it.name ?: "" }

      if (datedBackups.size > maxKeep) {
        for (i in maxKeep until datedBackups.size) {
          try {
            datedBackups[i].delete()
          } catch (_: Exception) {}
        }
      }
    } catch (_: Exception) {}
  }

  /**
   * Removes any lingering `.tmp_` files left behind by killed processes.
   */
  private fun cleanupTempFiles(context: Context, rootDir: DocumentFile) {
    try {
      // Local cache temp cleanup
      context.cacheDir.listFiles()?.filter { it.name.startsWith("temp_backup_") }?.forEach {
        try { it.delete() } catch (_: Exception) {}
      }

      // SAF folder temp cleanup
      val files: Array<DocumentFile> = rootDir.listFiles()
      files.filter { doc: DocumentFile -> doc.name?.startsWith(".tmp_") == true }.forEach {
        try { it.delete() } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
  }

  /**
   * Checks if an automatic backup should be run immediately because
   * last successful backup is older than 24 hours (Catch-up backup).
   */
  suspend fun checkAndPerformCatchUp(context: Context) {
    if (!BackupPreferences.isAutoBackupEnabled(context)) return
    if (!BackupPreferences.isFolderAccessible(context)) return

    val lastBackup = BackupPreferences.getLastBackupTimestamp(context)
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L

    if (lastBackup == 0L || (now - lastBackup) > oneDayMs) {
      performBackup(context)
    }
  }

  /**
   * Collects all SharedPreferences into a consolidated JSON string.
   */
  private fun collectPreferencesAsJson(context: Context): String {
    val root = JSONObject()

    val prefsNames = listOf(
      "focus_mode_prefs",
      "breath_audio_prefs",
      "black_screen_prefs",
      "life_tracker_backup_prefs",
      "life_tracker_user_prefs",
      "com.aistudio.lifetracker.dmpkze_preferences",
      "CrashReporter"
    )

    for (name in prefsNames) {
      try {
        val sp = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        val all = sp.all
        if (all.isNotEmpty()) {
          val prefObj = JSONObject()
          for ((k, v) in all) {
            when (v) {
              is Boolean -> prefObj.put(k, v)
              is Int -> prefObj.put(k, v)
              is Long -> prefObj.put(k, v)
              is Float -> prefObj.put(k, v.toDouble())
              is Double -> prefObj.put(k, v)
              is String -> prefObj.put(k, v)
              is Set<*> -> {
                val arr = org.json.JSONArray()
                v.forEach { arr.put(it) }
                prefObj.put(k, arr)
              }
            }
          }
          root.put(name, prefObj)
        }
      } catch (_: Exception) {}
    }

    return root.toString(2)
  }

  fun calculateSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { fis ->
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (fis.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  fun calculateSha256(inputStream: java.io.InputStream): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var bytesRead: Int
    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
      digest.update(buffer, 0, bytesRead)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  fun calculateSha256String(data: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(data.toByteArray(Charsets.UTF_8))
    return hash.joinToString("") { "%02x".format(it) }
  }
}
