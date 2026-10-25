package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Supported short-content apps.
 * Architecture is open to adding additional video platforms.
 */
enum class ShortAppType(
  val packageName: String,
  val appName: String,
  val hindiName: String,
  val iconTag: String,
  val colorHex: String
) {
  INSTAGRAM(
    packageName = "com.instagram.android",
    appName = "Instagram Reels",
    hindiName = "इंस्टाग्राम रील्स",
    iconTag = "instagram",
    colorHex = "#E1306C"
  ),
  YOUTUBE(
    packageName = "com.google.android.youtube",
    appName = "YouTube Shorts",
    hindiName = "यूट्यूब शॉर्ट्स",
    iconTag = "youtube",
    colorHex = "#FF0000"
  ),
  FACEBOOK(
    packageName = "com.facebook.katana",
    appName = "Facebook Reels",
    hindiName = "फेसबुक रील्स",
    iconTag = "facebook",
    colorHex = "#1877F2"
  ),
  SNAPCHAT(
    packageName = "com.snapchat.android",
    appName = "Snapchat Spotlight",
    hindiName = "स्नैपचैट स्पॉटलाइट",
    iconTag = "snapchat",
    colorHex = "#FFFC00"
  ),
  OTHER(
    packageName = "other",
    appName = "Other Shorts",
    hindiName = "अन्य शॉर्ट्स",
    iconTag = "other",
    colorHex = "#00F0FF"
  );

  companion object {
    fun fromPackage(pkg: String?): ShortAppType {
      if (pkg == null) return OTHER
      return when {
        pkg.startsWith("com.instagram.android") -> INSTAGRAM
        pkg.startsWith("com.google.android.youtube") -> YOUTUBE
        pkg.startsWith("com.facebook.katana") || pkg.startsWith("com.facebook.lite") -> FACEBOOK
        pkg.startsWith("com.snapchat.android") -> SNAPCHAT
        else -> OTHER
      }
    }
  }
}

/**
 * Individual short content view event (deduplicated & validated).
 */
@Entity(
  tableName = "short_content_records",
  indices = [Index(value = ["date"]), Index(value = ["videoSignature"])]
)
data class ShortContentRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String, // YYYY-MM-DD
  val appPackage: String,
  val appName: String,
  val videoSignature: String,
  val timestamp: Long = System.currentTimeMillis(),
  val durationSeconds: Int = 0
)

/**
 * Daily summary of short content consumption.
 */
@Entity(tableName = "short_content_daily_summaries")
data class ShortContentDailySummaryEntity(
  @PrimaryKey val date: String, // YYYY-MM-DD
  val instagramCount: Int = 0,
  val youtubeCount: Int = 0,
  val facebookCount: Int = 0,
  val otherCount: Int = 0,
  val totalCount: Int = 0,
  val totalTimeSeconds: Long = 0L,
  val dailyLimit: Int = 20,
  val lastUpdated: Long = System.currentTimeMillis()
) {
  val remainingShorts: Int
    get() = (dailyLimit - totalCount).coerceAtLeast(0)

  val isLimitReached: Boolean
    get() = totalCount >= dailyLimit

  val progressFraction: Float
    get() = if (dailyLimit > 0) (totalCount.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f

  val formattedTime: String
    get() {
      val totalMinutes = (totalTimeSeconds / 60).toInt()
      return if (totalMinutes < 60) {
        "$totalMinutes मिनट"
      } else {
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        "${hrs}घंटे ${mins}मि"
      }
    }
}

/**
 * Aggregated stats for weekly or monthly view.
 */
data class ShortContentPeriodStats(
  val totalCount: Int = 0,
  val totalTimeMinutes: Int = 0,
  val instagramCount: Int = 0,
  val youtubeCount: Int = 0,
  val facebookCount: Int = 0,
  val otherCount: Int = 0,
  val daysActive: Int = 0,
  val dailyAverageCount: Float = 0f,
  val limitExceededDays: Int = 0
)
