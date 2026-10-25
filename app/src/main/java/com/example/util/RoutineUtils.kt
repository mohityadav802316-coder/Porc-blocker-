package com.example.util

object RoutineUtils {
  /**
   * Resolves a stable, canonical activity key for a routine activity.
   * Enables 1:1 mapping between logical activities (Bathing, Food, Study/Project...)
   * and routine rows, preventing duplicate active rows.
   */
  fun resolveActivityKey(name: String, existingKey: String? = null): String {
    if (!existingKey.isNullOrBlank() && existingKey != "custom_0") {
      return existingKey
    }

    val lower = name.trim().lowercase()
    return when {
      // 1. Wake up & morning hydration
      lower.contains("wake up") || lower.contains("hydration") || lower.contains("उठना") || lower.contains("जगाना") -> "activity_wake_up"

      // 2. Evening Fitness & Cardio
      lower.contains("evening fitness") || lower.contains("outdoor cardio") || lower.contains("evening workout") -> "activity_evening_fitness"

      // 3. Morning Workout & Fitness / Gym / कसरत
      lower.contains("workout") || lower.contains("fitness") || lower.contains("कसरत") || lower.contains("व्यायाम") || lower.contains("gym") || lower.contains("exercise") -> "activity_workout"

      // 4. Bathing & Morning Nutrition / Shower / नाश्ता / स्नान
      lower.contains("shower") || lower.contains("bath") || lower.contains("bathing") || lower.contains("नहाना") || lower.contains("स्नान") || lower.contains("breakfast") || lower.contains("नाश्ता") -> "activity_breakfast"

      // 5. Day Planning & Top 3 Priorities / योजना
      lower.contains("day planning") || lower.contains("top 3 priorities") || lower.contains("planning") || lower.contains("योजना") -> "activity_planning"

      // 6. Focus Block 1 / Study / Project / पढ़ाई / Work
      lower.contains("focus block 1") || lower.contains("study") || lower.contains("project") || lower.contains("पढ़ाई") || lower.contains("deep work") || lower.contains("coding") || lower.contains("काम") -> "activity_focus_1"

      // 7. Review & Communication Sync
      lower.contains("review & communication") || lower.contains("communication sync") || lower.contains("sync") || lower.contains("meeting") -> "activity_review_sync"

      // 8. Lunch & Outdoor Walk / Food / दोपहर का भोजन / खाना
      lower.contains("lunch") || lower.contains("food") || lower.contains("भोजन") || lower.contains("खाना") || lower.contains("meal") || lower.contains("दोपहर") -> "activity_lunch"

      // 9. Focus Block 2 / Afternoon Focus
      lower.contains("focus block 2") || lower.contains("deep focus 2") -> "activity_focus_2"

      // 10. Skill Practice & Reading / Learning / सीखना / पढ़ना
      lower.contains("skill") || lower.contains("reading") || lower.contains("learning") || lower.contains("किताब") || lower.contains("सीखना") -> "activity_learning"

      // 11. Dinner & Mindful Decompression / रात का खाना
      lower.contains("dinner") || lower.contains("रात का खाना") -> "activity_dinner"

      // 12. Reflection & Daily Review / आत्मनिरीक्षण / समीक्षा
      lower.contains("reflection") || lower.contains("daily review") || lower.contains("आत्मनिरीक्षण") || lower.contains("समीक्षा") -> "activity_reflection"

      // 13. Night Wind-Down & Screens Off
      lower.contains("wind-down") || lower.contains("wind down") || lower.contains("screens off") -> "activity_wind_down"

      // 14. Sleep Prep & Lights Out / सोना / नींद
      lower.contains("sleep") || lower.contains("lights out") || lower.contains("सोना") || lower.contains("नींद") -> "activity_sleep"

      // Fallback: derive slug from custom name
      else -> {
        val slug = lower.replace(Regex("[^a-z0-9]+"), "_").trim('_')
        if (slug.isNotBlank()) "activity_$slug" else "custom_${System.currentTimeMillis()}"
      }
    }
  }
}
