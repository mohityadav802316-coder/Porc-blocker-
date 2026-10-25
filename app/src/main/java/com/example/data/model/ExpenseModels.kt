package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Core Expense Entity representing a single spending record in Expense Diary.
 */
@Entity(
  tableName = "expenses",
  indices = [
    Index(value = ["date"]),
    Index(value = ["category"]),
    Index(value = ["timestamp"])
  ]
)
data class ExpenseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val amount: Double,
  val note: String,
  val category: String, // e.g. "Food", "Travel", "Grocery", "Bills", "Shopping", "Health", "Entertainment", "Other"
  val categoryEmoji: String = "🍔",
  val categoryColorHex: String = "#FF9800",
  val date: String, // YYYY-MM-DD
  val time: String, // e.g. "04:15 PM"
  val timestamp: Long = System.currentTimeMillis(),
  val mood: String? = null // Optional mood: 😊 (खुश), 😌 (सुकून), 😐 (सामान्य), 😔 (तनाव), 😤 (पछतावा)
)

/**
 * Category definition with Hindi/English labels, icon, color and smart keywords.
 */
@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
  @PrimaryKey val id: String, // e.g. "food", "travel", "grocery"
  val nameHi: String,
  val nameEn: String,
  val emoji: String,
  val colorHex: String,
  val keywords: String, // Comma-separated keywords for smart classification
  val orderIndex: Int = 0,
  val monthlyBudget: Double = 0.0 // Optional category-specific monthly budget
)

/**
 * Recurring expenses (e.g. rent, mobile recharge, electricity bill).
 * Auto-added to expenses on their scheduled day of the month.
 */
@Entity(tableName = "expense_recurring")
data class RecurringExpenseEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val amount: Double,
  val category: String,
  val categoryEmoji: String = "🏠",
  val categoryColorHex: String = "#8B5CF6",
  val dayOfMonth: Int = 1, // 1 to 31
  val isEnabled: Boolean = true,
  val lastAddedMonth: String = "" // "YYYY-MM" to avoid duplicate additions in same month
)

/**
 * Quick Chip entity for 1-tap frequently used expenses.
 */
@Entity(tableName = "expense_quick_chips")
data class ExpenseQuickChipEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val label: String, // e.g. "चाय ₹20"
  val amount: Double,
  val note: String,
  val category: String,
  val emoji: String = "☕",
  val usageCount: Int = 1,
  val orderIndex: Int = 0
)

/**
 * Category aggregate summary for reports.
 */
data class CategoryExpenseSummary(
  val category: String,
  val totalAmount: Double,
  val count: Int,
  val emoji: String = "📦",
  val colorHex: String = "#00F0FF"
)

/**
 * Daily aggregate summary for weekly/monthly bar charts.
 */
data class DailyExpenseSummary(
  val date: String, // YYYY-MM-DD
  val dayLabel: String, // e.g. "सोम", "मंगल", "3 Oct"
  val totalAmount: Double,
  val count: Int
)

/**
 * Pre-defined standard categories with bilingual keywords.
 */
object ExpenseDefaults {

  val DEFAULT_CATEGORIES = listOf(
    ExpenseCategoryEntity(
      id = "food",
      nameHi = "खाना-पीना",
      nameEn = "Food",
      emoji = "🍔",
      colorHex = "#FF9800", // Vibrant Orange
      keywords = "चाय,chai,tea,coffee,खाना,khana,lunch,dinner,nashta,breakfast,roti,sabzi,pizza,burger,biryani,samosa,maggi,swiggy,zomato,cafe,restaurant,dhaba,hotel,snacks,biscuit,mithai,sweet,juice,lassi,cold drink",
      orderIndex = 0
    ),
    ExpenseCategoryEntity(
      id = "travel",
      nameHi = "यात्रा / पेट्रोल",
      nameEn = "Travel",
      emoji = "🚗",
      colorHex = "#00F0FF", // Cyan Neon
      keywords = "ऑटो,auto,rickshaw,riksha,cab,taxi,uber,ola,rapido,bus,metro,train,flight,ticket,fare,kiraya,petrol,diesel,fuel,cng,toll,parking,puncture,bike,car,scooty",
      orderIndex = 1
    ),
    ExpenseCategoryEntity(
      id = "grocery",
      nameHi = "किराना / राशन",
      nameEn = "Grocery",
      emoji = "🛒",
      colorHex = "#10B981", // Emerald Green
      keywords = "दूध,milk,doodh,curd,dahi,paneer,ration,kirana,dmart,bazaar,bazar,sabji,sabzi,fruit,fal,vegetable,aalu,pyaaz,tamatar,tel,oil,ghee,atta,rice,chawal,daal,masala,soap,surf",
      orderIndex = 2
    ),
    ExpenseCategoryEntity(
      id = "bills",
      nameHi = "बिल व रिचार्ज",
      nameEn = "Bills",
      emoji = "💡",
      colorHex = "#8B5CF6", // Violet
      keywords = "recharge,mobile,phone bill,wifi,net,broadband,electricity,bijli,current,rent,room rent,water,paani,cylinder,gas,lpg,maintenance,ott,subscription,netflix,prime,spotify",
      orderIndex = 3
    ),
    ExpenseCategoryEntity(
      id = "shopping",
      nameHi = "खरीदारी",
      nameEn = "Shopping",
      emoji = "🛍️",
      colorHex = "#EC4899", // Rose Pink
      keywords = "कपड़े,kapde,clothes,shoes,joota,chappal,shopping,amazon,flipkart,myntra,meesho,shirt,tshirt,jeans,pant,kurta,watch,bag,electronics,mobile cover,perfume",
      orderIndex = 4
    ),
    ExpenseCategoryEntity(
      id = "health",
      nameHi = "स्वास्थ्य व दवा",
      nameEn = "Health",
      emoji = "💊",
      colorHex = "#06B6D4", // Teal
      keywords = "दवा,dawa,medicine,tablet,syrup,doctor,fees,clinic,medical,chemist,pharmacy,test,blood test,lab,hospital,injection,bandage,eye drop,vitamins",
      orderIndex = 5
    ),
    ExpenseCategoryEntity(
      id = "entertainment",
      nameHi = "मनोरंजन",
      nameEn = "Entertainment",
      emoji = "🎬",
      colorHex = "#F59E0B", // Amber Gold
      keywords = "movie,cinema,pvr,inox,popcorn,film,game,gaming,match,cricket,party,club,outing,picnic,trip,fun,fair,mela",
      orderIndex = 6
    ),
    ExpenseCategoryEntity(
      id = "education",
      nameHi = "शिक्षा व पढ़ाई",
      nameEn = "Education",
      emoji = "📚",
      colorHex = "#3B82F6", // Blue
      keywords = "book,kitaab,pen,copy,register,notebook,stationary,pencil,fees,school,college,course,tuition,xerox,printout,form,exam",
      orderIndex = 7
    ),
    ExpenseCategoryEntity(
      id = "other",
      nameHi = "अन्य",
      nameEn = "Other",
      emoji = "📦",
      colorHex = "#64748B", // Slate Grey
      keywords = "other,extra,misc,khracha,dost,borrow,loan,gift,puja,donation,daan",
      orderIndex = 8
    )
  )

  val DEFAULT_QUICK_CHIPS = listOf(
    ExpenseQuickChipEntity(label = "चाय ₹10", amount = 10.0, note = "चाय", category = "Food", emoji = "☕", usageCount = 10, orderIndex = 0),
    ExpenseQuickChipEntity(label = "चाय ₹20", amount = 20.0, note = "चाय", category = "Food", emoji = "☕", usageCount = 9, orderIndex = 1),
    ExpenseQuickChipEntity(label = "ऑटो ₹30", amount = 30.0, note = "ऑटो", category = "Travel", emoji = "🛺", usageCount = 8, orderIndex = 2),
    ExpenseQuickChipEntity(label = "ऑटो ₹50", amount = 50.0, note = "ऑटो", category = "Travel", emoji = "🛺", usageCount = 7, orderIndex = 3),
    ExpenseQuickChipEntity(label = "नाश्ता ₹50", amount = 50.0, note = "नाश्ता", category = "Food", emoji = "🥪", usageCount = 6, orderIndex = 4),
    ExpenseQuickChipEntity(label = "लंच ₹120", amount = 120.0, note = "लंच", category = "Food", emoji = "🍱", usageCount = 5, orderIndex = 5),
    ExpenseQuickChipEntity(label = "दूध ₹35", amount = 35.0, note = "दूध", category = "Grocery", emoji = "🥛", usageCount = 4, orderIndex = 6),
    ExpenseQuickChipEntity(label = "पेट्रोल ₹100", amount = 100.0, note = "पेट्रोल", category = "Travel", emoji = "⛽", usageCount = 3, orderIndex = 7)
  )

  val MOOD_OPTIONS = listOf(
    "😊" to "खुश",
    "😌" to "सुकून",
    "😐" to "सामान्य",
    "😔" to "मजबूरी",
    "😤" to "पछतावा"
  )
}
