package com.example.expense

import android.content.Context
import android.content.Intent
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseQuickChipEntity
import com.example.data.model.RecurringExpenseEntity
import org.json.JSONArray
import org.json.JSONObject

data class ExpenseBackupData(
  val monthlyBudget: Double,
  val expenses: List<ExpenseEntity>,
  val categories: List<ExpenseCategoryEntity>,
  val quickChips: List<ExpenseQuickChipEntity>,
  val recurring: List<RecurringExpenseEntity>
)

object ExpenseBackupHelper {

  /**
   * Generates a clean tabular CSV for spreadsheets (Excel / Google Sheets).
   */
  fun generateCsv(expenses: List<ExpenseEntity>): String {
    val sb = StringBuilder()
    sb.append("ID,Date,Time,Amount,Note,Category,Mood\n")
    for (exp in expenses.sortedByDescending { it.timestamp }) {
      val cleanNote = exp.note.replace("\"", "\"\"")
      val cleanCat = exp.category.replace("\"", "\"\"")
      val mood = exp.mood ?: ""
      sb.append("${exp.id},\"${exp.date}\",\"${exp.time}\",${exp.amount},\"$cleanNote\",\"$cleanCat\",\"$mood\"\n")
    }
    return sb.toString()
  }

  /**
   * Generates a comprehensive JSON backup string.
   */
  fun generateJsonBackup(
    monthlyBudget: Double,
    expenses: List<ExpenseEntity>,
    categories: List<ExpenseCategoryEntity>,
    quickChips: List<ExpenseQuickChipEntity>,
    recurring: List<RecurringExpenseEntity>
  ): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("appName", "Life Tracker - Expense Diary")
    root.put("exportedAt", System.currentTimeMillis())
    root.put("monthlyBudget", monthlyBudget)

    // Expenses array
    val expArray = JSONArray()
    for (e in expenses) {
      val obj = JSONObject()
      obj.put("id", e.id)
      obj.put("amount", e.amount)
      obj.put("note", e.note)
      obj.put("category", e.category)
      obj.put("categoryEmoji", e.categoryEmoji)
      obj.put("categoryColorHex", e.categoryColorHex)
      obj.put("date", e.date)
      obj.put("time", e.time)
      obj.put("timestamp", e.timestamp)
      obj.put("mood", e.mood ?: "")
      expArray.put(obj)
    }
    root.put("expenses", expArray)

    // Categories array
    val catArray = JSONArray()
    for (c in categories) {
      val obj = JSONObject()
      obj.put("id", c.id)
      obj.put("nameHi", c.nameHi)
      obj.put("nameEn", c.nameEn)
      obj.put("emoji", c.emoji)
      obj.put("colorHex", c.colorHex)
      obj.put("keywords", c.keywords)
      obj.put("orderIndex", c.orderIndex)
      obj.put("monthlyBudget", c.monthlyBudget)
      catArray.put(obj)
    }
    root.put("categories", catArray)

    // Quick chips array
    val chipArray = JSONArray()
    for (q in quickChips) {
      val obj = JSONObject()
      obj.put("id", q.id)
      obj.put("label", q.label)
      obj.put("amount", q.amount)
      obj.put("note", q.note)
      obj.put("category", q.category)
      obj.put("emoji", q.emoji)
      obj.put("usageCount", q.usageCount)
      obj.put("orderIndex", q.orderIndex)
      chipArray.put(obj)
    }
    root.put("quickChips", chipArray)

    // Recurring expenses array
    val recArray = JSONArray()
    for (r in recurring) {
      val obj = JSONObject()
      obj.put("id", r.id)
      obj.put("title", r.title)
      obj.put("amount", r.amount)
      obj.put("category", r.category)
      obj.put("categoryEmoji", r.categoryEmoji)
      obj.put("categoryColorHex", r.categoryColorHex)
      obj.put("dayOfMonth", r.dayOfMonth)
      obj.put("isEnabled", r.isEnabled)
      obj.put("lastAddedMonth", r.lastAddedMonth)
      recArray.put(obj)
    }
    root.put("recurring", recArray)

    return root.toString(2)
  }

  /**
   * Parses a JSON backup string into entities.
   */
  fun parseJsonBackup(jsonStr: String): ExpenseBackupData {
    val root = JSONObject(jsonStr)
    val budget = root.optDouble("monthlyBudget", 15000.0)

    val expenses = mutableListOf<ExpenseEntity>()
    val expArray = root.optJSONArray("expenses") ?: JSONArray()
    for (i in 0 until expArray.length()) {
      val o = expArray.getJSONObject(i)
      expenses.add(
        ExpenseEntity(
          id = o.optLong("id", 0L),
          amount = o.getDouble("amount"),
          note = o.getString("note"),
          category = o.optString("category", "Other"),
          categoryEmoji = o.optString("categoryEmoji", "📦"),
          categoryColorHex = o.optString("categoryColorHex", "#64748B"),
          date = o.getString("date"),
          time = o.optString("time", "12:00 PM"),
          timestamp = o.optLong("timestamp", System.currentTimeMillis()),
          mood = o.optString("mood").ifBlank { null }
        )
      )
    }

    val categories = mutableListOf<ExpenseCategoryEntity>()
    val catArray = root.optJSONArray("categories") ?: JSONArray()
    for (i in 0 until catArray.length()) {
      val o = catArray.getJSONObject(i)
      categories.add(
        ExpenseCategoryEntity(
          id = o.getString("id"),
          nameHi = o.getString("nameHi"),
          nameEn = o.getString("nameEn"),
          emoji = o.optString("emoji", "🏷️"),
          colorHex = o.optString("colorHex", "#00F0FF"),
          keywords = o.optString("keywords", ""),
          orderIndex = o.optInt("orderIndex", i),
          monthlyBudget = o.optDouble("monthlyBudget", 0.0)
        )
      )
    }

    val chips = mutableListOf<ExpenseQuickChipEntity>()
    val chipArray = root.optJSONArray("quickChips") ?: JSONArray()
    for (i in 0 until chipArray.length()) {
      val o = chipArray.getJSONObject(i)
      chips.add(
        ExpenseQuickChipEntity(
          id = o.optLong("id", 0L),
          label = o.getString("label"),
          amount = o.getDouble("amount"),
          note = o.getString("note"),
          category = o.optString("category", "Other"),
          emoji = o.optString("emoji", "⚡"),
          usageCount = o.optInt("usageCount", 1),
          orderIndex = o.optInt("orderIndex", i)
        )
      )
    }

    val recurring = mutableListOf<RecurringExpenseEntity>()
    val recArray = root.optJSONArray("recurring") ?: JSONArray()
    for (i in 0 until recArray.length()) {
      val o = recArray.getJSONObject(i)
      recurring.add(
        RecurringExpenseEntity(
          id = o.optLong("id", 0L),
          title = o.getString("title"),
          amount = o.getDouble("amount"),
          category = o.optString("category", "Bills"),
          categoryEmoji = o.optString("categoryEmoji", "🏠"),
          categoryColorHex = o.optString("categoryColorHex", "#8B5CF6"),
          dayOfMonth = o.optInt("dayOfMonth", 1),
          isEnabled = o.optBoolean("isEnabled", true),
          lastAddedMonth = o.optString("lastAddedMonth", "")
        )
      )
    }

    return ExpenseBackupData(
      monthlyBudget = budget,
      expenses = expenses,
      categories = categories,
      quickChips = chips,
      recurring = recurring
    )
  }

  fun shareText(context: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, title)
      putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, title))
  }
}
