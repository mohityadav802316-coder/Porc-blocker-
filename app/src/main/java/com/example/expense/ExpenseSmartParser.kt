package com.example.expense

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseDefaults
import java.util.regex.Pattern

data class ParsedExpenseInput(
  val amount: Double?,
  val note: String,
  val suggestedCategory: ExpenseCategoryEntity,
  val isSmartDetected: Boolean
)

object ExpenseSmartParser {

  private const val PREFS_NAME = "expense_smart_parser_prefs"
  private const val KEY_LEARNED_MAPPINGS = "learned_keyword_mappings"

  // Regex to detect amounts like "20", "₹20", "20.50", "Rs 150", "150/-"
  private val AMOUNT_PATTERN = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:/-)?", Pattern.CASE_INSENSITIVE)

  /**
   * Parses free-form text like "चाय 20" or "auto 80 office" or "lunch 150"
   * to separate amount, clean note, and suggest the best category.
   */
  fun parseInput(
    context: Context,
    rawNoteText: String,
    explicitAmount: Double? = null,
    availableCategories: List<ExpenseCategoryEntity> = ExpenseDefaults.DEFAULT_CATEGORIES
  ): ParsedExpenseInput {
    val trimmed = rawNoteText.trim()
    var detectedAmount = explicitAmount
    var cleanNote = trimmed
    var isSmartDetected = false

    if (explicitAmount == null || explicitAmount <= 0.0) {
      val matcher = AMOUNT_PATTERN.matcher(trimmed)
      var bestMatch: String? = null
      var foundAmount: Double? = null

      while (matcher.find()) {
        val numStr = matcher.group(1)
        val num = numStr?.toDoubleOrNull()
        if (num != null && num > 0) {
          foundAmount = num
          bestMatch = matcher.group(0)
          break
        }
      }

      if (foundAmount != null && bestMatch != null) {
        detectedAmount = foundAmount
        // Remove amount from text to obtain clean note
        cleanNote = trimmed.replaceFirst(bestMatch, " ")
          .replace(Regex("\\s+"), " ")
          .trim()
        isSmartDetected = true
      }
    }

    val category = detectCategory(context, cleanNote.ifBlank { trimmed }, availableCategories)

    return ParsedExpenseInput(
      amount = detectedAmount,
      note = cleanNote,
      suggestedCategory = category,
      isSmartDetected = isSmartDetected && detectedAmount != null
    )
  }

  /**
   * Detects appropriate category based on user learned corrections first,
   * then bilingual keyword dictionary.
   */
  fun detectCategory(
    context: Context,
    note: String,
    categories: List<ExpenseCategoryEntity> = ExpenseDefaults.DEFAULT_CATEGORIES
  ): ExpenseCategoryEntity {
    val lowerNote = note.lowercase().trim()
    if (lowerNote.isBlank()) {
      return categories.firstOrNull { it.id == "other" } ?: categories.first()
    }

    // 1. Check user-learned personal corrections
    val learnedMap = getLearnedMappings(context)
    val tokens = lowerNote.split(Regex("[\\s,._/-]+")).filter { it.isNotBlank() }

    for (token in tokens) {
      val learnedCatId = learnedMap[token]
      if (learnedCatId != null) {
        val matched = categories.firstOrNull { it.id == learnedCatId }
        if (matched != null) return matched
      }
    }

    // 2. Check predefined categories keywords
    for (cat in categories) {
      val kwList = cat.keywords.split(",").map { it.trim().lowercase() }
      for (kw in kwList) {
        if (kw.isNotBlank() && (lowerNote.contains(kw) || tokens.contains(kw))) {
          return cat
        }
      }
    }

    // Default fallback
    return categories.firstOrNull { it.id == "other" } ?: categories.first()
  }

  /**
   * Remembers user's manual category correction so the app learns user's style.
   */
  fun rememberUserCorrection(context: Context, note: String, categoryId: String) {
    val tokens = note.lowercase().split(Regex("[\\s,._/-]+")).filter { it.length >= 2 }
    if (tokens.isEmpty()) return

    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val current = getLearnedMappings(context).toMutableMap()

    for (token in tokens) {
      current[token] = categoryId
    }

    // Persist as comma-separated key:value
    val serialized = current.entries.joinToString(";") { "${it.key}:${it.value}" }
    prefs.edit().putString(KEY_LEARNED_MAPPINGS, serialized).apply()
  }

  private fun getLearnedMappings(context: Context): Map<String, String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_LEARNED_MAPPINGS, null) ?: return emptyMap()
    val result = mutableMapOf<String, String>()
    raw.split(";").forEach { pair ->
      val parts = pair.split(":")
      if (parts.size == 2) {
        result[parts[0].trim()] = parts[1].trim()
      }
    }
    return result
  }
}
