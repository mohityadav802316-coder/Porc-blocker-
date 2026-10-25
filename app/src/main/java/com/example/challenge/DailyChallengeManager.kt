package com.example.challenge

import android.content.Context
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.DailyChallengeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

object DailyChallengeManager {

  private val curatedChallenges = listOf(
    // READING
    Triple("Reading", "📖 15 पृष्ठ ज्ञानवर्धक पठन (Read 15 Pages)", "अपनी पसंदीदा गैर-काल्पनिक (non-fiction) पुस्तक के कम से कम 15 पृष्ठ एकाग्रता से पढ़ें।"),
    Triple("Reading", "📖 विचार-मंथन पठन (Core Idea Reading)", "किसी महत्वपूर्ण विषय पर एक गहरा लेख या अध्याय पढ़ें और 1 मुख्य विचार नोट करें।"),

    // MEDITATION
    Triple("Meditation", "🧘 10 मिनट गहन ध्यान (10 Min Deep Meditation)", "दिन के बीच में 10 मिनट के लिए श्वास पर ध्यान केंद्रित करें और मन शांत करें।"),
    Triple("Meditation", "🧘 कृतज्ञता साधना (5 Min Gratitude Silence)", "शांति से बैठकर अपने जीवन की 3 ऐसी बातों का आभार व्यक्त करें जिनके लिए आप कृतज्ञ हैं।"),

    // LEARNING
    Triple("Learning", "💡 1 नया कौशल या अवधारणा (Learn 1 New Concept)", "अपने कार्यक्षेत्र या अध्ययन में 1 नया सिद्धांत या तकनीक सीखें और समझें।"),
    Triple("Learning", "💡 दैनिक शब्दकोश & नोट्स (Expand Knowledge)", "किसी महत्वपूर्ण विषय का एक ज्ञानवर्धक सारांश या पॉडकास्ट सुनें।"),

    // ORGANIZATION
    Triple("Organization", "🧹 कार्यस्थल की स्वच्छता (Desk Decluttering)", "अपनी स्टडी टेबल और कार्यस्थल को पूरी तरह व्यवस्थित और साफ करें।"),
    Triple("Organization", "🧹 डिजिटल सफाई (Digital Inbox Zero)", "फोन की गैर-जरूरी फाइल्स या ईमेल साफ करें और ऐप्स व्यवस्थित करें।"),

    // EXERCISE
    Triple("Exercise", "🏃 25 मिनट सक्रिय गतिशीलता (25 Min Brisk Walk / Run)", "सवेरे या शाम 25 मिनट खुली हवा में तेज चलें या हल्का व्यायाम करें।"),
    Triple("Exercise", "🏃 30 पुशअप्स या सूर्य नमस्कार (Core Movement)", "शरीर को ऊर्जावान बनाने के लिए सूर्य नमस्कार या 30 पुशअप्स पूरे करें।"),

    // PRODUCTIVITY
    Triple("Productivity", "⚡ 45 मिनट अविभाजित फोकस (45m Deep Work)", "बिना किसी फोन या सोशल मीडिया व्यवधान के 45 मिनट का एक टास्क पूरा करें।"),
    Triple("Productivity", "⚡ सबसे कठिन काम पहले (Eat That Frog)", "आज के सबसे चुनौतीपूर्ण काम को सुबह के पहले पहर में ही समाप्त करें।"),

    // SELF-IMPROVEMENT
    Triple("Self-Improvement", "🌱 दिन की 3 सीख लिखें (Record 3 Learnings)", "आज आपने अपने अनुभव से क्या सीखा, सोने से पहले 3 पंक्तियों में दर्ज करें।"),
    Triple("Self-Improvement", "🌱 डिजिटल डिटॉक्स घंटा (1 Hour Screen-Free)", "सोने से 1 घंटा पहले सभी स्क्रीन बंद रखें और मन को शांत करें।")
  )

  suspend fun getOrCreateTodayChallenge(context: Context, date: String): DailyChallengeEntity = withContext(Dispatchers.IO) {
    val db = LifeTrackerDatabase.getDatabase(context)
    val dao = db.dailyChallengeDao()

    val existing = dao.getChallengeForDateSync(date)
    if (existing != null) {
      return@withContext existing
    }

    // Deterministically pick a challenge based on date hash
    val seed = date.replace("-", "").toLongOrNull() ?: 12345L
    val index = (seed % curatedChallenges.size).toInt()
    val chosen = curatedChallenges[index]

    val newChallenge = DailyChallengeEntity(
      date = date,
      category = chosen.first,
      title = chosen.second,
      description = chosen.third,
      targetMinutes = 15,
      isCompleted = false
    )

    dao.insertChallenge(newChallenge)
    newChallenge
  }

  suspend fun completeChallenge(context: Context, date: String) = withContext(Dispatchers.IO) {
    val db = LifeTrackerDatabase.getDatabase(context)
    db.dailyChallengeDao().markCompleted(date, true, System.currentTimeMillis())
  }

  suspend fun skipOrSwapChallenge(context: Context, date: String) = withContext(Dispatchers.IO) {
    val db = LifeTrackerDatabase.getDatabase(context)
    val dao = db.dailyChallengeDao()
    val existing = dao.getChallengeForDateSync(date)

    val currentIndex = curatedChallenges.indexOfFirst { it.second == existing?.title }
    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % curatedChallenges.size else 0
    val next = curatedChallenges[nextIndex]

    val swapped = DailyChallengeEntity(
      date = date,
      category = next.first,
      title = next.second,
      description = next.third,
      targetMinutes = 15,
      isCompleted = false,
      isSkipped = false
    )
    dao.insertChallenge(swapped)
  }
}
