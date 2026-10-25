package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseQuickChipEntity
import com.example.data.model.RecurringExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

  // --- EXPENSES ---
  @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
  fun getAllExpenses(): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
  suspend fun getAllExpensesSync(): List<ExpenseEntity>

  @Query("SELECT * FROM expenses WHERE date = :date ORDER BY timestamp DESC")
  fun getExpensesForDate(date: String): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE date = :date ORDER BY timestamp DESC")
  suspend fun getExpensesForDateSync(date: String): List<ExpenseEntity>

  @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, timestamp DESC")
  fun getExpensesBetweenDates(startDate: String, endDate: String): Flow<List<ExpenseEntity>>

  @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC, timestamp DESC")
  suspend fun getExpensesBetweenDatesSync(startDate: String, endDate: String): List<ExpenseEntity>

  @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date = :date")
  fun getTodayTotalFlow(date: String): Flow<Double>

  @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date = :date")
  suspend fun getTodayTotalSync(date: String): Double

  @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE date >= :startDate AND date <= :endDate")
  suspend fun getTotalSpendBetweenDates(startDate: String, endDate: String): Double

  @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
  suspend fun getExpenseById(id: Long): ExpenseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExpense(expense: ExpenseEntity): Long

  @Update
  suspend fun updateExpense(expense: ExpenseEntity)

  @Delete
  suspend fun deleteExpense(expense: ExpenseEntity)

  @Query("DELETE FROM expenses WHERE id = :id")
  suspend fun deleteExpenseById(id: Long)

  // --- QUICK CHIPS ---
  @Query("SELECT * FROM expense_quick_chips ORDER BY usageCount DESC, orderIndex ASC LIMIT 12")
  fun getAllQuickChips(): Flow<List<ExpenseQuickChipEntity>>

  @Query("SELECT * FROM expense_quick_chips ORDER BY usageCount DESC, orderIndex ASC LIMIT 12")
  suspend fun getAllQuickChipsSync(): List<ExpenseQuickChipEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuickChips(chips: List<ExpenseQuickChipEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertQuickChip(chip: ExpenseQuickChipEntity): Long

  @Query("DELETE FROM expense_quick_chips WHERE id = :id")
  suspend fun deleteQuickChipById(id: Long)

  @Query("UPDATE expense_quick_chips SET usageCount = usageCount + 1 WHERE note = :note AND amount = :amount")
  suspend fun bumpQuickChipUsage(note: String, amount: Double)

  // --- CATEGORIES ---
  @Query("SELECT * FROM expense_categories ORDER BY orderIndex ASC")
  fun getAllCategories(): Flow<List<ExpenseCategoryEntity>>

  @Query("SELECT * FROM expense_categories ORDER BY orderIndex ASC")
  suspend fun getAllCategoriesSync(): List<ExpenseCategoryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategories(categories: List<ExpenseCategoryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCategory(category: ExpenseCategoryEntity)

  @Update
  suspend fun updateCategory(category: ExpenseCategoryEntity)

  @Delete
  suspend fun deleteCategory(category: ExpenseCategoryEntity)

  @Query("DELETE FROM expense_categories WHERE id = :id")
  suspend fun deleteCategoryById(id: String)

  @Update
  suspend fun updateQuickChip(chip: ExpenseQuickChipEntity)

  // --- RECURRING EXPENSES ---
  @Query("SELECT * FROM expense_recurring ORDER BY dayOfMonth ASC")
  fun getAllRecurringExpenses(): Flow<List<RecurringExpenseEntity>>

  @Query("SELECT * FROM expense_recurring ORDER BY dayOfMonth ASC")
  suspend fun getAllRecurringExpensesSync(): List<RecurringExpenseEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecurringExpense(recurring: RecurringExpenseEntity): Long

  @Update
  suspend fun updateRecurringExpense(recurring: RecurringExpenseEntity)

  @Delete
  suspend fun deleteRecurringExpense(recurring: RecurringExpenseEntity)

  @Query("DELETE FROM expense_recurring WHERE id = :id")
  suspend fun deleteRecurringExpenseById(id: Long)
}
