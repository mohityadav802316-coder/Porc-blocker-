package com.example.backup

import org.json.JSONObject

data class BackupManifest(
  val appVersionCode: Int,
  val appVersionName: String,
  val databaseVersion: Int,
  val createdAt: Long,
  val createdDateIso: String,
  val recordCounts: Map<String, Int>,
  val checksums: Map<String, String>,
  val databaseFileName: String = "life_tracker_db",
  val preferencesFileName: String = "preferences_backup.json",
  val backupFormatVersion: Int = 1
) {
  fun toJson(): String {
    val json = JSONObject()
    json.put("appVersionCode", appVersionCode)
    json.put("appVersionName", appVersionName)
    json.put("databaseVersion", databaseVersion)
    json.put("createdAt", createdAt)
    json.put("createdDateIso", createdDateIso)
    json.put("databaseFileName", databaseFileName)
    json.put("preferencesFileName", preferencesFileName)
    json.put("backupFormatVersion", backupFormatVersion)

    val countsObj = JSONObject()
    recordCounts.forEach { (k, v) -> countsObj.put(k, v) }
    json.put("recordCounts", countsObj)

    val checksumsObj = JSONObject()
    checksums.forEach { (k, v) -> checksumsObj.put(k, v) }
    json.put("checksums", checksumsObj)

    return json.toString(2)
  }

  companion object {
    fun fromJson(jsonStr: String): BackupManifest? {
      return try {
        val obj = JSONObject(jsonStr)
        val recordCounts = mutableMapOf<String, Int>()
        if (obj.has("recordCounts")) {
          val cObj = obj.getJSONObject("recordCounts")
          val keys = cObj.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            recordCounts[key] = cObj.optInt(key, 0)
          }
        }

        val checksums = mutableMapOf<String, String>()
        if (obj.has("checksums")) {
          val sObj = obj.getJSONObject("checksums")
          val keys = sObj.keys()
          while (keys.hasNext()) {
            val key = keys.next()
            checksums[key] = sObj.optString(key, "")
          }
        }

        BackupManifest(
          appVersionCode = obj.optInt("appVersionCode", 1),
          appVersionName = obj.optString("appVersionName", "1.0"),
          databaseVersion = obj.optInt("databaseVersion", 15),
          createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
          createdDateIso = obj.optString("createdDateIso", ""),
          recordCounts = recordCounts,
          checksums = checksums,
          databaseFileName = obj.optString("databaseFileName", "life_tracker_db"),
          preferencesFileName = obj.optString("preferencesFileName", "preferences_backup.json"),
          backupFormatVersion = obj.optInt("backupFormatVersion", 1)
        )
      } catch (_: Exception) {
        null
      }
    }
  }
}
