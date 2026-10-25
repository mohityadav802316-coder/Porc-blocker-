package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyNoteEntity
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseDefaults
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseQuickChipEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.expense.ExpenseBackupHelper
import com.example.expense.ExpenseSmartParser
import com.example.ui.components.AddEditRecurringDialog
import com.example.ui.components.AppLockSetupDialog
import com.example.ui.components.EditCategoryDialog
import com.example.ui.components.ExpenseLockOverlay
import com.example.ui.components.RestoreBackupDialog
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.TimeUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private enum class ExpenseTab(val title: String) {
  EXPENSES("खर्च"),
  REPORTS("रिपोर्ट"),
  DIARY("डायरी"),
  CUSTOMISE("कस्टम")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDiaryScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  // App Lock State
  val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
  val appLockPin by viewModel.appLockPin.collectAsState()
  var isUnlockedInSession by remember { mutableStateOf(!isAppLockEnabled) }

  // Core State
  val allExpenses by viewModel.allExpenses.collectAsState()
  val todayTotal by viewModel.todayTotalSpend.collectAsState()
  val categories by viewModel.expenseCategories.collectAsState()
  val quickChips by viewModel.expenseQuickChips.collectAsState()
  val recurringExpenses by viewModel.recurringExpenses.collectAsState()
  val monthlyBudget by viewModel.monthlyBudget.collectAsState()
  val currencySymbol by viewModel.currencySymbol.collectAsState()
  val firstDayOfWeek by viewModel.firstDayOfWeek.collectAsState()
  val reportColorTheme by viewModel.reportColorTheme.collectAsState()

  var selectedTab by remember { mutableStateOf(ExpenseTab.EXPENSES) }

  // Search & Filter state
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

  // Bottom Input States
  var inputAmountText by remember { mutableStateOf("") }
  var inputNoteText by remember { mutableStateOf("") }
  var selectedMood by remember { mutableStateOf<String?>(null) }
  var selectedCategory by remember {
    mutableStateOf(categories.firstOrNull { it.id == "food" } ?: ExpenseDefaults.DEFAULT_CATEGORIES.first())
  }
  var showCategoryDropdown by remember { mutableStateOf(false) }

  // Dialogs
  var expenseToEdit by remember { mutableStateOf<ExpenseEntity?>(null) }
  var categoryToEdit by remember { mutableStateOf<ExpenseCategoryEntity?>(null) }
  var recurringToEdit by remember { mutableStateOf<RecurringExpenseEntity?>(null) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var showAddQuickChipDialog by remember { mutableStateOf(false) }
  var showAddRecurringDialog by remember { mutableStateOf(false) }
  var showAppLockDialog by remember { mutableStateOf(false) }
  var showRestoreDialog by remember { mutableStateOf(false) }

  // Update selectedCategory if categories list changes and it was empty
  LaunchedEffect(categories) {
    if (categories.isNotEmpty() && selectedCategory.id == "food") {
      categories.firstOrNull { it.id == "food" }?.let { selectedCategory = it }
    }
  }

  // Trigger recurring expenses check on entry
  LaunchedEffect(Unit) {
    viewModel.checkRecurringExpenses()
  }

  // Smart Input Detection while typing note
  LaunchedEffect(inputNoteText) {
    if (inputNoteText.isNotBlank()) {
      val parsed = ExpenseSmartParser.parseInput(
        context = context,
        rawNoteText = inputNoteText,
        explicitAmount = inputAmountText.toDoubleOrNull(),
        availableCategories = categories.ifEmpty { ExpenseDefaults.DEFAULT_CATEGORIES }
      )
      if (parsed.isSmartDetected && parsed.amount != null) {
        inputAmountText = if (parsed.amount % 1.0 == 0.0) parsed.amount.toLong().toString() else parsed.amount.toString()
        selectedCategory = parsed.suggestedCategory
      } else if (inputAmountText.isNotBlank()) {
        selectedCategory = ExpenseSmartParser.detectCategory(
          context = context,
          note = inputNoteText,
          categories = categories.ifEmpty { ExpenseDefaults.DEFAULT_CATEGORIES }
        )
      }
    }
  }

  // Calculate current month's total spend
  val currentMonthPrefix = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
  val thisMonthExpenses = remember(allExpenses, currentMonthPrefix) {
    allExpenses.filter { it.date.startsWith(currentMonthPrefix) }
  }
  val thisMonthTotalSpend = remember(thisMonthExpenses) {
    thisMonthExpenses.sumOf { it.amount }
  }

  BackHandler {
    viewModel.navigateBack()
  }

  // If App Lock is enabled and not unlocked yet, show PIN lock overlay
  if (isAppLockEnabled && !isUnlockedInSession) {
    ExpenseLockOverlay(
      correctPin = appLockPin,
      onUnlocked = { isUnlockedInSession = true },
      onCancel = { viewModel.navigateBack() }
    )
    return
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground),
    containerColor = DarkBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(ObsidianElevated)
          .statusBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { viewModel.navigateBack() }) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "वापस जाएं",
              tint = WarmOffWhite
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "खर्च डायरी (Expense Diary)",
              color = WarmOffWhite,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "100% ऑफ़लाइन • पिन लॉक • आवर्ती खर्च",
              color = WarmMuted,
              fontSize = 12.sp
            )
          }

          // Quick Lock or CSV Export
          IconButton(onClick = {
            val csv = ExpenseBackupHelper.generateCsv(allExpenses)
            ExpenseBackupHelper.shareText(context, csv, "Expenses_${TimeUtils.getTodayDateString()}.csv")
          }) {
            Icon(
              imageVector = Icons.Default.TableChart,
              contentDescription = "CSV Export",
              tint = CyanNeon,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // Navigation Tabs
        ScrollableTabRow(
          selectedTabIndex = selectedTab.ordinal,
          containerColor = ObsidianElevated,
          contentColor = WarmOffWhite,
          edgePadding = 12.dp,
          indicator = { tabPositions ->
            if (selectedTab.ordinal < tabPositions.size) {
              TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = CyanNeon,
                height = 3.dp
              )
            }
          },
          divider = {}
        ) {
          ExpenseTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            Tab(
              selected = isSelected,
              onClick = { selectedTab = tab },
              text = {
                Text(
                  text = tab.title,
                  fontSize = 14.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) CyanNeon else WarmMuted
                )
              }
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (selectedTab) {
        ExpenseTab.EXPENSES -> {
          ExpenseListTabContent(
            allExpenses = allExpenses,
            todayTotal = todayTotal,
            monthlyBudget = monthlyBudget,
            thisMonthTotalSpend = thisMonthTotalSpend,
            currencySymbol = currencySymbol,
            quickChips = quickChips,
            categories = categories,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedCategoryFilter = selectedCategoryFilter,
            onCategoryFilterChange = { selectedCategoryFilter = it },
            inputAmountText = inputAmountText,
            onAmountChange = { inputAmountText = it },
            inputNoteText = inputNoteText,
            onNoteChange = { inputNoteText = it },
            selectedMood = selectedMood,
            onMoodSelect = { selectedMood = if (selectedMood == it) null else it },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it },
            showCategoryDropdown = showCategoryDropdown,
            onCategoryDropdownDismiss = { showCategoryDropdown = false },
            onCategoryDropdownOpen = { showCategoryDropdown = true },
            onQuickChipTap = { chip ->
              val matchedCat = categories.firstOrNull { it.nameEn.equals(chip.category, ignoreCase = true) || it.nameHi == chip.category }
                ?: ExpenseDefaults.DEFAULT_CATEGORIES.first()
              viewModel.addExpense(
                amount = chip.amount,
                note = chip.note,
                category = matchedCat.nameEn,
                categoryEmoji = chip.emoji.ifBlank { matchedCat.emoji },
                categoryColorHex = matchedCat.colorHex
              )
              Toast.makeText(context, "${chip.label} जोड़ा गया!", Toast.LENGTH_SHORT).show()
            },
            onSaveExpense = {
              val amt = inputAmountText.toDoubleOrNull() ?: 0.0
              val note = inputNoteText.trim()
              if (amt > 0 && note.isNotBlank()) {
                viewModel.addExpense(
                  amount = amt,
                  note = note,
                  category = selectedCategory.nameEn,
                  categoryEmoji = selectedCategory.emoji,
                  categoryColorHex = selectedCategory.colorHex,
                  mood = selectedMood
                )
                ExpenseSmartParser.rememberUserCorrection(context, note, selectedCategory.id)

                inputAmountText = ""
                inputNoteText = ""
                selectedMood = null
                Toast.makeText(context, "$currencySymbol${amt.toInt()} खर्च सहेजा गया", Toast.LENGTH_SHORT).show()
              }
            },
            onExpenseClick = { expenseToEdit = it },
            onDeleteExpense = { exp ->
              viewModel.deleteExpense(exp)
              scope.launch {
                val res = snackbarHostState.showSnackbar(
                  message = "$currencySymbol${exp.amount.toInt()} (${exp.note}) हटाया गया",
                  actionLabel = "पूर्ववत करें",
                  duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed) {
                  viewModel.undoDeleteExpense()
                }
              }
            }
          )
        }

        ExpenseTab.REPORTS -> {
          ExpenseReportsTabContent(
            allExpenses = allExpenses,
            categories = categories,
            currencySymbol = currencySymbol
          )
        }

        ExpenseTab.DIARY -> {
          ExpenseDiaryStoryTabContent(
            viewModel = viewModel,
            allExpenses = allExpenses,
            currencySymbol = currencySymbol
          )
        }

        ExpenseTab.CUSTOMISE -> {
          ExpenseCustomiseTabContent(
            monthlyBudget = monthlyBudget,
            onUpdateBudget = { viewModel.setMonthlyBudget(it) },
            categories = categories,
            onAddCategoryClick = { showAddCategoryDialog = true },
            onEditCategoryClick = { categoryToEdit = it },
            quickChips = quickChips,
            onAddQuickChipClick = { showAddQuickChipDialog = true },
            onDeleteQuickChip = { viewModel.deleteQuickChip(it) },
            onMoveQuickChip = { chip, up ->
              val curOrder = chip.orderIndex
              val newOrder = if (up) curOrder - 1 else curOrder + 1
              viewModel.updateQuickChip(chip.copy(orderIndex = newOrder))
            },
            recurringExpenses = recurringExpenses,
            onAddRecurringClick = { showAddRecurringDialog = true },
            onEditRecurringClick = { recurringToEdit = it },
            onToggleRecurring = { rec, isEnabled ->
              viewModel.updateRecurringExpense(rec.copy(isEnabled = isEnabled))
            },
            onDeleteRecurring = { rec -> viewModel.deleteRecurringExpense(rec) },
            currencySymbol = currencySymbol,
            onCurrencyChange = { viewModel.setCurrencySymbol(it) },
            firstDayOfWeek = firstDayOfWeek,
            onFirstDayOfWeekChange = { viewModel.setFirstDayOfWeek(it) },
            reportColorTheme = reportColorTheme,
            onReportColorThemeChange = { viewModel.setReportColorTheme(it) },
            isAppLockEnabled = isAppLockEnabled,
            onConfigureAppLock = { showAppLockDialog = true },
            onExportBackup = {
              val json = ExpenseBackupHelper.generateJsonBackup(monthlyBudget, allExpenses, categories, quickChips, recurringExpenses)
              ExpenseBackupHelper.shareText(context, json, "ExpenseDiary_Backup_${TimeUtils.getTodayDateString()}.json")
            },
            onImportBackup = { showRestoreDialog = true }
          )
        }
      }

      // Dialogs
      expenseToEdit?.let { exp ->
        EditExpenseDialog(
          expense = exp,
          categories = categories,
          currencySymbol = currencySymbol,
          onDismiss = { expenseToEdit = null },
          onSave = { updated ->
            viewModel.updateExpense(updated)
            expenseToEdit = null
            Toast.makeText(context, "खर्च अपडेट किया गया", Toast.LENGTH_SHORT).show()
          },
          onDelete = { toDel ->
            viewModel.deleteExpense(toDel)
            expenseToEdit = null
            scope.launch {
              val res = snackbarHostState.showSnackbar(
                message = "$currencySymbol${toDel.amount.toInt()} हटाया गया",
                actionLabel = "पूर्ववत करें",
                duration = SnackbarDuration.Short
              )
              if (res == SnackbarResult.ActionPerformed) {
                viewModel.undoDeleteExpense()
              }
            }
          }
        )
      }

      categoryToEdit?.let { cat ->
        EditCategoryDialog(
          category = cat,
          onDismiss = { categoryToEdit = null },
          onSave = { updated ->
            viewModel.updateExpenseCategory(updated)
            categoryToEdit = null
            Toast.makeText(context, "श्रेणी अपडेट की गई", Toast.LENGTH_SHORT).show()
          },
          onDelete = {
            viewModel.deleteExpenseCategory(cat)
            categoryToEdit = null
            Toast.makeText(context, "श्रेणी हटाई गई", Toast.LENGTH_SHORT).show()
          }
        )
      }

      if (showAddCategoryDialog) {
        AddCategoryDialog(
          onDismiss = { showAddCategoryDialog = false },
          onAddCategory = { hi, en, emoji, color, kw ->
            viewModel.addCustomCategory(hi, en, emoji, color, kw)
            showAddCategoryDialog = false
            Toast.makeText(context, "नई श्रेणी जोड़ी गई!", Toast.LENGTH_SHORT).show()
          }
        )
      }

      if (showAddQuickChipDialog) {
        AddQuickChipDialog(
          categories = categories,
          currencySymbol = currencySymbol,
          onDismiss = { showAddQuickChipDialog = false },
          onAddChip = { label, amt, note, cat, emoji ->
            viewModel.addQuickChip(label, amt, note, cat, emoji)
            showAddQuickChipDialog = false
            Toast.makeText(context, "क्विक बटन जोड़ा गया!", Toast.LENGTH_SHORT).show()
          }
        )
      }

      if (showAddRecurringDialog) {
        AddEditRecurringDialog(
          categories = categories,
          currencySymbol = currencySymbol,
          onDismiss = { showAddRecurringDialog = false },
          onSave = { title, amt, cat, emoji, color, day ->
            viewModel.addRecurringExpense(title, amt, cat, emoji, color, day)
            showAddRecurringDialog = false
            Toast.makeText(context, "आवर्ती खर्च जोड़ा गया!", Toast.LENGTH_SHORT).show()
          }
        )
      }

      recurringToEdit?.let { rec ->
        AddEditRecurringDialog(
          existing = rec,
          categories = categories,
          currencySymbol = currencySymbol,
          onDismiss = { recurringToEdit = null },
          onSave = { title, amt, cat, emoji, color, day ->
            viewModel.updateRecurringExpense(
              rec.copy(
                title = title,
                amount = amt,
                category = cat,
                categoryEmoji = emoji,
                categoryColorHex = color,
                dayOfMonth = day
              )
            )
            recurringToEdit = null
            Toast.makeText(context, "आवर्ती खर्च अपडेट किया गया!", Toast.LENGTH_SHORT).show()
          }
        )
      }

      if (showAppLockDialog) {
        AppLockSetupDialog(
          currentPin = appLockPin,
          onDismiss = { showAppLockDialog = false },
          onSave = { enabled, pin ->
            viewModel.setAppLock(enabled, pin)
            isUnlockedInSession = true
            showAppLockDialog = false
            Toast.makeText(context, if (enabled) "पिन लॉक चालू किया गया" else "पिन लॉक बंद किया गया", Toast.LENGTH_SHORT).show()
          }
        )
      }

      if (showRestoreDialog) {
        RestoreBackupDialog(
          onDismiss = { showRestoreDialog = false },
          onRestore = { json ->
            viewModel.restoreExpenseBackup(json) { success, msg ->
              Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
              if (success) showRestoreDialog = false
            }
          }
        )
      }
    }
  }
}

// ----------------------------------------------------------------------------
// TAB 1: EXPENSES LIST & BOTTOM INPUT
// ----------------------------------------------------------------------------
@Composable
private fun ExpenseListTabContent(
  allExpenses: List<ExpenseEntity>,
  todayTotal: Double,
  monthlyBudget: Double,
  thisMonthTotalSpend: Double,
  currencySymbol: String,
  quickChips: List<ExpenseQuickChipEntity>,
  categories: List<ExpenseCategoryEntity>,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  selectedCategoryFilter: String?,
  onCategoryFilterChange: (String?) -> Unit,
  inputAmountText: String,
  onAmountChange: (String) -> Unit,
  inputNoteText: String,
  onNoteChange: (String) -> Unit,
  selectedMood: String?,
  onMoodSelect: (String) -> Unit,
  selectedCategory: ExpenseCategoryEntity,
  onCategoryChange: (ExpenseCategoryEntity) -> Unit,
  showCategoryDropdown: Boolean,
  onCategoryDropdownDismiss: () -> Unit,
  onCategoryDropdownOpen: () -> Unit,
  onQuickChipTap: (ExpenseQuickChipEntity) -> Unit,
  onSaveExpense: () -> Unit,
  onExpenseClick: (ExpenseEntity) -> Unit,
  onDeleteExpense: (ExpenseEntity) -> Unit
) {
  val budgetPercent = if (monthlyBudget > 0) ((thisMonthTotalSpend / monthlyBudget) * 100).toInt() else 0
  val remainingMonthlyBudget = (monthlyBudget - thisMonthTotalSpend).coerceAtLeast(0.0)

  // Calculate "आज का बचा बजट" (Daily Allowance Formula)
  val cal = remember { Calendar.getInstance() }
  val currentDay = cal.get(Calendar.DAY_OF_MONTH)
  val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)
  val dailyAllowance = if (monthlyBudget > 0) remainingMonthlyBudget / daysRemaining else 500.0
  val todayRemainingBudget = (dailyAllowance - todayTotal).coerceAtLeast(0.0)

  // Check Category-specific Budget warnings
  val categoryWarnings = remember(categories, allExpenses) {
    val curMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    val monthExps = allExpenses.filter { it.date.startsWith(curMonth) }
    categories.filter { cat ->
      cat.monthlyBudget > 0 && monthExps.filter { it.category.equals(cat.nameEn, ignoreCase = true) }.sumOf { it.amount } >= (cat.monthlyBudget * 0.8)
    }
  }

  // Filtered expenses
  val filteredExpenses = remember(allExpenses, searchQuery, selectedCategoryFilter) {
    allExpenses.filter { exp ->
      val matchesSearch = searchQuery.isBlank() ||
        exp.note.contains(searchQuery, ignoreCase = true) ||
        exp.category.contains(searchQuery, ignoreCase = true) ||
        exp.amount.toString().contains(searchQuery)

      val matchesCat = selectedCategoryFilter == null || exp.category.equals(selectedCategoryFilter, ignoreCase = true)
      matchesSearch && matchesCat
    }
  }

  // Group by date
  val groupedExpenses = remember(filteredExpenses) {
    filteredExpenses.groupBy { it.date }
  }

  val todayDate = remember { TimeUtils.getTodayDateString() }
  val yesterdayDate = remember {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, -1)
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
      // 1. Top Summary Card: Today's Total, Today's Remaining Budget & Monthly Status
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "आज का खर्च",
                  color = WarmMuted,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = "$currencySymbol${String.format(Locale.US, "%,.0f", todayTotal)}",
                  color = CyanNeon,
                  fontSize = 26.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "आज का बचा बजट",
                  color = GoldBrass,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "$currencySymbol${String.format(Locale.US, "%,.0f", todayRemainingBudget)}",
                  color = if (todayTotal > dailyAllowance) Color(0xFFEF4444) else Color(0xFF10B981),
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "दैनिक कोटा: $currencySymbol${dailyAllowance.toInt()}/दिन",
                  color = WarmMuted,
                  fontSize = 10.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Monthly Progress bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "माह का खर्च: $currencySymbol${String.format(Locale.US, "%,.0f", thisMonthTotalSpend)} / $currencySymbol${String.format(Locale.US, "%,.0f", monthlyBudget)}",
                color = WarmOffWhite,
                fontSize = 11.sp
              )
              Text(
                text = "$budgetPercent%",
                color = if (budgetPercent >= 100) Color(0xFFEF4444) else if (budgetPercent >= 80) Color(0xFFF59E0B) else CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
              progress = { (thisMonthTotalSpend.toFloat() / monthlyBudget.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = if (budgetPercent >= 100) Color(0xFFEF4444) else if (budgetPercent >= 80) Color(0xFFF59E0B) else CyanNeon,
              trackColor = DarkSurface
            )

            // Warning Banner at >= 80% or >= 100%
            if (budgetPercent >= 100) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0x33EF4444), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "अलर्ट: आपका मासिक बजट 100% पार हो चुका है!",
                  color = Color(0xFFFCA5A5),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            } else if (budgetPercent >= 80) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0x33F59E0B), RoundedCornerShape(8.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "चेतावनी: 80% मासिक बजट खर्च हो चुका है!",
                  color = Color(0xFFFCD34D),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }

            // Category budget warnings
            if (categoryWarnings.isNotEmpty()) {
              Spacer(modifier = Modifier.height(6.dp))
              categoryWarnings.forEach { cat ->
                Text(
                  text = "⚠️ श्रेणी चेतावनी: ${cat.emoji} ${cat.nameHi} का बजट 80%+ खर्च हुआ ($currencySymbol${cat.monthlyBudget.toInt()})",
                  color = Color(0xFFFCD34D),
                  fontSize = 10.sp
                )
              }
            }
          }
        }
      }

      // 2. Quick Chips: 1-Tap Entry for top frequent spends
      if (quickChips.isNotEmpty()) {
        item {
          Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
              text = "त्वरित खर्च (1-टैप):",
              color = WarmMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              quickChips.sortedBy { it.orderIndex }.take(10).forEach { chip ->
                Card(
                  modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onQuickChipTap(chip) },
                  colors = CardDefaults.cardColors(containerColor = DarkSurface),
                  border = BorderStroke(1.dp, DarkSurfaceBorder)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = chip.emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = chip.label,
                      color = WarmOffWhite,
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 3. Search and Category Filter Row
      item {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("खर्च खोजें (नोट, रकम, श्रेणी)...", color = WarmMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = WarmMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear", tint = WarmMuted, modifier = Modifier.size(16.dp))
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DarkSurface,
              unfocusedContainerColor = DarkSurface,
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedTextColor = WarmOffWhite,
              unfocusedTextColor = WarmOffWhite
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = selectedCategoryFilter == null,
              onClick = { onCategoryFilterChange(null) },
              label = { Text("सभी", fontSize = 11.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = CyanNeon,
                selectedLabelColor = ObsidianCharcoal,
                containerColor = DarkSurface,
                labelColor = WarmMuted
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedCategoryFilter == null,
                borderColor = DarkSurfaceBorder,
                selectedBorderColor = CyanNeon
              )
            )

            categories.forEach { cat ->
              val isSel = selectedCategoryFilter.equals(cat.nameEn, ignoreCase = true)
              FilterChip(
                selected = isSel,
                onClick = { onCategoryFilterChange(if (isSel) null else cat.nameEn) },
                label = { Text("${cat.emoji} ${cat.nameHi}", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CyanNeon,
                  selectedLabelColor = ObsidianCharcoal,
                  containerColor = DarkSurface,
                  labelColor = WarmOffWhite
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSel,
                  borderColor = DarkSurfaceBorder,
                  selectedBorderColor = CyanNeon
                )
              )
            }
          }
        }
      }

      // 4. Expenses List Grouped by Date
      if (filteredExpenses.isEmpty()) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(text = "🪙", fontSize = 42.sp)
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (searchQuery.isNotBlank() || selectedCategoryFilter != null) "कोई खर्च नहीं मिला" else "अभी कोई खर्च दर्ज नहीं है",
                color = WarmOffWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "नीचे दिए बॉक्स में रकम और नोट लिखकर Save दबाएं",
                color = WarmMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
              )
            }
          }
        }
      } else {
        groupedExpenses.forEach { (date, expensesForDate) ->
          val daySum = expensesForDate.sumOf { it.amount }
          val formattedDate = when (date) {
            todayDate -> "आज • ${formatDisplayDate(date)}"
            yesterdayDate -> "कल • ${formatDisplayDate(date)}"
            else -> formatDisplayDate(date)
          }

          item {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = formattedDate,
                color = GoldBrass,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "कुल: $currencySymbol${String.format(Locale.US, "%,.0f", daySum)}",
                color = WarmMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          items(expensesForDate, key = { it.id }) { expense ->
            ExpenseItemRow(
              expense = expense,
              currencySymbol = currencySymbol,
              onClick = { onExpenseClick(expense) },
              onDelete = { onDeleteExpense(expense) }
            )
            Spacer(modifier = Modifier.height(6.dp))
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(140.dp))
      }
    }

    // 5. Persistent Bottom Input Bar
    BottomExpenseInputBar(
      inputAmountText = inputAmountText,
      onAmountChange = onAmountChange,
      inputNoteText = inputNoteText,
      onNoteChange = onNoteChange,
      selectedMood = selectedMood,
      onMoodSelect = onMoodSelect,
      selectedCategory = selectedCategory,
      categories = categories,
      currencySymbol = currencySymbol,
      onCategorySelect = onCategoryChange,
      showCategoryDropdown = showCategoryDropdown,
      onCategoryDropdownDismiss = onCategoryDropdownDismiss,
      onCategoryDropdownOpen = onCategoryDropdownOpen,
      onSave = onSaveExpense
    )
  }
}

@Composable
private fun ExpenseItemRow(
  expense: ExpenseEntity,
  currencySymbol: String,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val offsetX = remember { Animatable(0f) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
  ) {
    // Swipe background layer (Green for Edit on right swipe, Red for Delete on left swipe)
    Row(
      modifier = Modifier
        .matchParentSize()
        .background(
          when {
            offsetX.value > 25f -> Color(0xFF059669)
            offsetX.value < -25f -> Color(0xFFDC2626)
            else -> DarkSurface
          }
        )
        .padding(horizontal = 16.dp),
      horizontalArrangement = if (offsetX.value > 0) Arrangement.Start else Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      if (offsetX.value > 25f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Edit, contentDescription = "एडिट", tint = Color.White, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("एडिट", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      } else if (offsetX.value < -25f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("हटाएं", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = Color.White, modifier = Modifier.size(18.dp))
        }
      }
    }

    // Foreground Card with drag gesture & click
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(expense.id) {
          detectHorizontalDragGestures(
            onDragEnd = {
              coroutineScope.launch {
                val curVal = offsetX.value
                offsetX.animateTo(0f)
                if (curVal > 100f) {
                  onClick() // Swiped right -> Edit
                } else if (curVal < -100f) {
                  onDelete() // Swiped left -> Delete
                }
              }
            },
            onDragCancel = {
              coroutineScope.launch { offsetX.animateTo(0f) }
            },
            onHorizontalDrag = { _, dragAmount ->
              coroutineScope.launch {
                val newOffset = (offsetX.value + dragAmount).coerceIn(-280f, 280f)
                offsetX.snapTo(newOffset)
              }
            }
          )
        }
        .clickable { onClick() },
      colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
      border = BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DarkSurface),
          contentAlignment = Alignment.Center
        ) {
          Text(text = expense.categoryEmoji, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = expense.note,
              color = WarmOffWhite,
              fontSize = 15.sp,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false)
            )

            expense.mood?.let { m ->
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = m, fontSize = 14.sp)
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = expense.category,
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
            Text(text = " • ", color = WarmMuted, fontSize = 11.sp)
            Text(
              text = expense.time,
              color = WarmMuted,
              fontSize = 11.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "$currencySymbol${String.format(Locale.US, "%,.0f", expense.amount)}",
            color = WarmOffWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.width(4.dp))

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "हटाएं",
              tint = WarmMuted,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BottomExpenseInputBar(
  inputAmountText: String,
  onAmountChange: (String) -> Unit,
  inputNoteText: String,
  onNoteChange: (String) -> Unit,
  selectedMood: String?,
  onMoodSelect: (String) -> Unit,
  selectedCategory: ExpenseCategoryEntity,
  categories: List<ExpenseCategoryEntity>,
  currencySymbol: String,
  onCategorySelect: (ExpenseCategoryEntity) -> Unit,
  showCategoryDropdown: Boolean,
  onCategoryDropdownDismiss: () -> Unit,
  onCategoryDropdownOpen: () -> Unit,
  onSave: () -> Unit
) {
  val canSave = (inputAmountText.toDoubleOrNull() ?: 0.0) > 0 && inputNoteText.isNotBlank()

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    colors = CardDefaults.cardColors(containerColor = ObsidianCharcoal),
    border = BorderStroke(1.dp, DarkSurfaceBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box {
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .clickable { onCategoryDropdownOpen() }
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = selectedCategory.emoji, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = selectedCategory.nameHi,
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          DropdownMenu(
            expanded = showCategoryDropdown,
            onDismissRequest = onCategoryDropdownDismiss,
            modifier = Modifier.background(ObsidianElevated)
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = cat.emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${cat.nameHi} (${cat.nameEn})", color = WarmOffWhite, fontSize = 13.sp)
                  }
                },
                onClick = {
                  onCategorySelect(cat)
                  onCategoryDropdownDismiss()
                }
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          ExpenseDefaults.MOOD_OPTIONS.forEach { (emoji, _) ->
            val isSelected = selectedMood == emoji
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) CyanNeon.copy(alpha = 0.25f) else Color.Transparent)
                .border(
                  width = if (isSelected) 1.5.dp else 0.dp,
                  color = if (isSelected) CyanNeon else Color.Transparent,
                  shape = CircleShape
                )
                .clickable { onMoodSelect(emoji) },
              contentAlignment = Alignment.Center
            ) {
              Text(text = emoji, fontSize = 16.sp)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = inputAmountText,
          onValueChange = { newVal ->
            if (newVal.all { it.isDigit() || it == '.' }) {
              onAmountChange(newVal)
            }
          },
          placeholder = { Text("$currencySymbol रकम", color = WarmMuted, fontSize = 13.sp) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier
            .width(105.dp)
            .height(50.dp)
            .testTag("expense_amount_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite
          )
        )

        OutlinedTextField(
          value = inputNoteText,
          onValueChange = onNoteChange,
          placeholder = { Text("क्या खर्च हुआ (जैसे चाय 20)", color = WarmMuted, fontSize = 13.sp) },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("expense_note_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite
          )
        )

        Button(
          onClick = onSave,
          enabled = canSave,
          modifier = Modifier
            .height(50.dp)
            .testTag("expense_save_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CyanNeon,
            contentColor = ObsidianCharcoal,
            disabledContainerColor = DarkSurface,
            disabledContentColor = WarmMuted
          )
        ) {
          Text(text = "Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

// ----------------------------------------------------------------------------
// TAB 2: REPORTS (WEEKLY & MONTHLY STATS & CANVAS BAR CHART)
// ----------------------------------------------------------------------------
private enum class ReportPeriod {
  WEEKLY, MONTHLY
}

@Composable
private fun ExpenseReportsTabContent(
  allExpenses: List<ExpenseEntity>,
  categories: List<ExpenseCategoryEntity>,
  currencySymbol: String
) {
  var reportPeriod by remember { mutableStateOf(ReportPeriod.WEEKLY) }
  var periodOffset by remember { mutableIntStateOf(0) }

  val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

  val (startDate, endDate, periodLabel) = remember(reportPeriod, periodOffset) {
    val cal = Calendar.getInstance()
    if (reportPeriod == ReportPeriod.WEEKLY) {
      cal.add(Calendar.WEEK_OF_YEAR, periodOffset)
      cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      val start = sdf.format(cal.time)
      cal.add(Calendar.DAY_OF_WEEK, 6)
      val end = sdf.format(cal.time)

      val labelFormat = SimpleDateFormat("dd MMM", Locale.US)
      val label = "${labelFormat.format(sdf.parse(start)!!)} - ${labelFormat.format(sdf.parse(end)!!)}"
      Triple(start, end, label)
    } else {
      cal.add(Calendar.MONTH, periodOffset)
      cal.set(Calendar.DAY_OF_MONTH, 1)
      val start = sdf.format(cal.time)
      val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      cal.set(Calendar.DAY_OF_MONTH, maxDay)
      val end = sdf.format(cal.time)

      val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
      Triple(start, end, monthLabel)
    }
  }

  val periodExpenses = remember(allExpenses, startDate, endDate) {
    allExpenses.filter { it.date in startDate..endDate }
  }

  val previousPeriodExpenses = remember(allExpenses, reportPeriod, periodOffset) {
    val cal = Calendar.getInstance()
    if (reportPeriod == ReportPeriod.WEEKLY) {
      cal.add(Calendar.WEEK_OF_YEAR, periodOffset - 1)
      cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      val prevStart = sdf.format(cal.time)
      cal.add(Calendar.DAY_OF_WEEK, 6)
      val prevEnd = sdf.format(cal.time)
      allExpenses.filter { it.date in prevStart..prevEnd }
    } else {
      cal.add(Calendar.MONTH, periodOffset - 1)
      cal.set(Calendar.DAY_OF_MONTH, 1)
      val prevStart = sdf.format(cal.time)
      val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      cal.set(Calendar.DAY_OF_MONTH, maxDay)
      val prevEnd = sdf.format(cal.time)
      allExpenses.filter { it.date in prevStart..prevEnd }
    }
  }

  val totalSpend = remember(periodExpenses) { periodExpenses.sumOf { it.amount } }
  val prevTotalSpend = remember(previousPeriodExpenses) { previousPeriodExpenses.sumOf { it.amount } }
  val daysCount = if (reportPeriod == ReportPeriod.WEEKLY) 7 else 30
  val dailyAverage = totalSpend / daysCount

  val highestExpense = remember(periodExpenses) {
    periodExpenses.maxByOrNull { it.amount }
  }

  val dailySummaries = remember(periodExpenses, startDate, endDate, reportPeriod) {
    val map = periodExpenses.groupBy { it.date }.mapValues { (_, list) -> list.sumOf { it.amount } }
    val cal = Calendar.getInstance()
    val list = mutableListOf<Pair<String, Double>>()

    if (reportPeriod == ReportPeriod.WEEKLY) {
      cal.time = sdf.parse(startDate)!!
      val dayNameFmt = SimpleDateFormat("E", Locale.US)
      for (i in 0 until 7) {
        val dStr = sdf.format(cal.time)
        val dName = dayNameFmt.format(cal.time)
        list.add(dName to (map[dStr] ?: 0.0))
        cal.add(Calendar.DAY_OF_YEAR, 1)
      }
    } else {
      cal.time = sdf.parse(startDate)!!
      val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
      for (day in 1..maxDays) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dStr = sdf.format(cal.time)
        list.add(day.toString() to (map[dStr] ?: 0.0))
      }
    }
    list
  }

  val peakDay = remember(dailySummaries) {
    dailySummaries.maxByOrNull { it.second }?.takeIf { it.second > 0 }
  }

  val categoryBreakdown = remember(periodExpenses, categories) {
    val total = if (totalSpend > 0) totalSpend else 1.0
    periodExpenses.groupBy { it.category }
      .map { (catName, list) ->
        val sum = list.sumOf { it.amount }
        val matched = categories.firstOrNull { it.nameEn.equals(catName, ignoreCase = true) || it.nameHi == catName }
        Triple(
          matched?.emoji ?: "📦",
          matched?.nameHi ?: catName,
          sum to (sum / total * 100).roundToInt()
        )
      }
      .sortedByDescending { it.third.first }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Column {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianElevated)
            .padding(4.dp),
          horizontalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (reportPeriod == ReportPeriod.WEEKLY) CyanNeon else Color.Transparent)
              .clickable {
                reportPeriod = ReportPeriod.WEEKLY
                periodOffset = 0
              }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "साप्ताहिक (Weekly)",
              color = if (reportPeriod == ReportPeriod.WEEKLY) ObsidianCharcoal else WarmMuted,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (reportPeriod == ReportPeriod.MONTHLY) CyanNeon else Color.Transparent)
              .clickable {
                reportPeriod = ReportPeriod.MONTHLY
                periodOffset = 0
              }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "मासिक (Monthly)",
              color = if (reportPeriod == ReportPeriod.MONTHLY) ObsidianCharcoal else WarmMuted,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = { periodOffset-- }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "पिछला", tint = CyanNeon)
          }

          Text(
            text = periodLabel,
            color = WarmOffWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )

          IconButton(
            onClick = { if (periodOffset < 0) periodOffset++ },
            enabled = periodOffset < 0
          ) {
            Icon(
              Icons.Default.ChevronRight,
              contentDescription = "अगला",
              tint = if (periodOffset < 0) CyanNeon else WarmMuted
            )
          }
        }
      }
    }

    // 2. Metrics
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          modifier = Modifier.weight(1f),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "कुल खर्च", color = WarmMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "$currencySymbol${String.format(Locale.US, "%,.0f", totalSpend)}",
              color = CyanNeon,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            val diff = totalSpend - prevTotalSpend
            val diffPct = if (prevTotalSpend > 0) ((diff / prevTotalSpend) * 100).roundToInt() else 0
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (diff <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                contentDescription = null,
                tint = if (diff <= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${if (diff > 0) "+" else ""}$diffPct% तुलना",
                color = if (diff <= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Card(
          modifier = Modifier.weight(1f),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "रोज़ का औसत", color = WarmMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "$currencySymbol${String.format(Locale.US, "%,.0f", dailyAverage)}",
              color = WarmOffWhite,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "प्रति दिन औसत", color = WarmMuted, fontSize = 10.sp)
          }
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Card(
          modifier = Modifier.weight(1f),
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "सबसे बड़ा खर्च", color = WarmMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (highestExpense != null) {
              Text(
                text = "$currencySymbol${highestExpense.amount.toInt()}",
                color = GoldBrass,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${highestExpense.categoryEmoji} ${highestExpense.note}",
                color = WarmOffWhite,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            } else {
              Text(text = "कोई नहीं", color = WarmMuted, fontSize = 13.sp)
            }
          }
        }

        Card(
          modifier = Modifier.weight(1f),
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(12.dp)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "पीक खर्च दिन", color = WarmMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if (peakDay != null) {
              Text(
                text = peakDay.first,
                color = CyanNeon,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "$currencySymbol${peakDay.second.toInt()}",
                color = WarmOffWhite,
                fontSize = 11.sp
              )
            } else {
              Text(text = "कोई नहीं", color = WarmMuted, fontSize = 13.sp)
            }
          }
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = if (reportPeriod == ReportPeriod.WEEKLY) "दैनिक खर्च चार्ट (हफ़्ता)" else "दैनिक खर्च चार्ट (माह)",
            color = WarmOffWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(14.dp))

          ExpenseBarChart(
            summaries = dailySummaries,
            modifier = Modifier
              .fillMaxWidth()
              .height(160.dp)
          )
        }
      }
    }

    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "श्रेणीवार खर्च वितरण (Category Breakdown)",
            color = WarmOffWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(12.dp))

          if (categoryBreakdown.isEmpty()) {
            Text(text = "इस अवधि में कोई खर्च नहीं है", color = WarmMuted, fontSize = 12.sp)
          } else {
            categoryBreakdown.forEach { (emoji, nameHi, stats) ->
              val (amt, pct) = stats
              Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = nameHi, color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  }
                  Text(
                    text = "$currencySymbol${amt.toInt()} ($pct%)",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                  progress = { (pct / 100f).coerceIn(0f, 1f) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                  color = CyanNeon,
                  trackColor = DarkSurface
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExpenseBarChart(
  summaries: List<Pair<String, Double>>,
  modifier: Modifier = Modifier
) {
  val maxVal = remember(summaries) {
    (summaries.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(10.0)
  }

  Canvas(modifier = modifier) {
    val barCount = summaries.size
    if (barCount == 0) return@Canvas

    val widthPerBar = size.width / barCount
    val maxBarHeight = size.height - 24.dp.toPx()

    summaries.forEachIndexed { i, (label, amount) ->
      val x = i * widthPerBar
      val barW = (widthPerBar * 0.65f).coerceAtMost(28.dp.toPx())
      val barX = x + (widthPerBar - barW) / 2f
      val barH = ((amount / maxVal) * maxBarHeight).toFloat().coerceAtLeast(2.dp.toPx())
      val barY = size.height - 20.dp.toPx() - barH

      val isHighest = amount == maxVal && amount > 0

      drawRoundRect(
        color = if (isHighest) GoldBrass else if (amount > 0) CyanNeon else DarkSurfaceBorder,
        topLeft = Offset(barX, barY),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
      )

      drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
          color = android.graphics.Color.parseColor("#94A3B8")
          textSize = 9.sp.toPx()
          textAlign = android.graphics.Paint.Align.CENTER
          isAntiAlias = true
        }
        if (barCount <= 7 || i % 5 == 0 || i == barCount - 1) {
          drawText(
            label,
            barX + barW / 2f,
            size.height - 2.dp.toPx(),
            paint
          )
        }
      }
    }
  }
}

// ----------------------------------------------------------------------------
// TAB 3: DIARY (SWIPEABLE/CALENDAR STORY, NO-SPEND STREAK, NOTES & MOOD)
// ----------------------------------------------------------------------------
@Composable
private fun ExpenseDiaryStoryTabContent(
  viewModel: LifeTrackerViewModel,
  allExpenses: List<ExpenseEntity>,
  currencySymbol: String
) {
  val context = LocalContext.current
  var selectedStoryDate by remember { mutableStateOf(TimeUtils.getTodayDateString()) }

  val dayExpenses = remember(allExpenses, selectedStoryDate) {
    allExpenses.filter { it.date == selectedStoryDate }
  }

  val dailyNote by viewModel.dailyNoteForSelectedDate.collectAsState()
  var personalNoteText by remember { mutableStateOf("") }

  LaunchedEffect(dailyNote) {
    personalNoteText = dailyNote?.note ?: ""
  }

  // Calculate No-Spend Streak & stats
  val (noSpendStreak, noSpendMonthCount) = remember(allExpenses) {
    calculateNoSpendStreak(allExpenses)
  }

  // On-device Mood Correlation
  val moodAnalysis = remember(allExpenses) {
    val expensesWithMood = allExpenses.filter { !it.mood.isNullOrBlank() }
    val groupedByMood = expensesWithMood.groupBy { it.mood!! }

    val moodAvgs = ExpenseDefaults.MOOD_OPTIONS.map { (emoji, label) ->
      val list = groupedByMood[emoji] ?: emptyList()
      val count = list.size
      val avg = if (count > 0) list.sumOf { it.amount } / count else 0.0
      Triple(emoji, label, avg to count)
    }

    val happyExpenses = expensesWithMood.filter { it.mood == "😊" || it.mood == "😌" }
    val stressExpenses = expensesWithMood.filter { it.mood == "😔" || it.mood == "😤" }

    val happyAvg = if (happyExpenses.isNotEmpty()) happyExpenses.sumOf { it.amount } / happyExpenses.size else 0.0
    val stressAvg = if (stressExpenses.isNotEmpty()) stressExpenses.sumOf { it.amount } / stressExpenses.size else 0.0

    val insightText = if (happyAvg > 0 && stressAvg > 0) {
      if (stressAvg > happyAvg) {
        val pctMore = (((stressAvg - happyAvg) / happyAvg) * 100).roundToInt()
        "अंतर्दृष्टि: तनाव (😔) या पछतावे (😤) वाले दिनों में आपका औसत खर्च खुश दिनों से $pctMore% अधिक रहा है। भावनात्मक खर्च (Emotional spending) पर नियंत्रण रखें।"
      } else {
        "अंतर्दृष्टि: आपका खर्च संतुलित है। तनाव में गैर-जरूरी फिजूलखर्ची नहीं देखी गई है।"
      }
    } else {
      "खर्च दर्ज करते समय मूड (😊/😔) चुनें ताकि आपका व्यक्तिगत खर्च-मूड विश्लेषण यहाँ दिख सके।"
    }

    moodAvgs to insightText
  }

  // Calendar DatePickerDialog launcher
  fun showCalendarPicker() {
    try {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val cur = sdf.parse(selectedStoryDate) ?: Date()
      val cal = Calendar.getInstance().apply { time = cur }
      DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
          val newDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
          if (newDate <= TimeUtils.getTodayDateString()) {
            selectedStoryDate = newDate
          }
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
      ).show()
    } catch (_: Exception) {}
  }

  fun stepDate(step: Int) {
    try {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val cal = Calendar.getInstance().apply { time = sdf.parse(selectedStoryDate)!! }
      cal.add(Calendar.DAY_OF_YEAR, step)
      val nextStr = sdf.format(cal.time)
      if (nextStr <= TimeUtils.getTodayDateString()) {
        selectedStoryDate = nextStr
      }
    } catch (_: Exception) {}
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .pointerInput(selectedStoryDate) {
        // Horizontal Swipe to flip pages like a real diary
        detectHorizontalDragGestures { _, dragAmount ->
          if (dragAmount > 60) {
            stepDate(-1) // Swipe right -> Previous day
          } else if (dragAmount < -60) {
            stepDate(1) // Swipe left -> Next day
          }
        }
      }
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Day Navigator with Calendar Picker & Swipe Hint
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = { stepDate(-1) }) {
              Icon(Icons.Default.ChevronLeft, contentDescription = "पिछला दिन", tint = CyanNeon)
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { showCalendarPicker() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = "कैलेंडर", tint = GoldBrass, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = formatDisplayDate(selectedStoryDate),
                  color = WarmOffWhite,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp
                )
                Text(
                  text = "स्वाइप करें या कैलेंडर से चुनें 📅",
                  color = WarmMuted,
                  fontSize = 10.sp
                )
              }
            }

            IconButton(
              onClick = { stepDate(1) },
              enabled = selectedStoryDate < TimeUtils.getTodayDateString()
            ) {
              Icon(
                Icons.Default.ChevronRight,
                contentDescription = "अगला दिन",
                tint = if (selectedStoryDate < TimeUtils.getTodayDateString()) CyanNeon else WarmMuted
              )
            }
          }
        }
      }

      // 2. No-Spend Streak Badge Card
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, if (noSpendStreak > 0) Color(0xFF10B981) else DarkSurfaceBorder)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(text = if (noSpendStreak >= 3) "🔥" else "🛡️", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "नो-स्पेंड स्ट्रीक (No-Spend Streak): $noSpendStreak दिन",
                color = WarmOffWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "इस माह $noSpendMonthCount दिन बिना किसी खर्च के बीते हैं!",
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }

      // 3. Day Spending Story
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.MenuBook, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "दिन की खर्च कहानी",
                color = WarmOffWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val storyText = remember(dayExpenses, selectedStoryDate, currencySymbol) {
              generateDayStory(dayExpenses, selectedStoryDate, currencySymbol)
            }

            Text(
              text = storyText,
              color = WarmOffWhite,
              fontSize = 13.sp,
              lineHeight = 20.sp
            )
          }
        }
      }

      // 4. Two-line Personal Reflection Note
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "आज का व्यक्तिगत नोट (दो लाइन)",
                  color = WarmOffWhite,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
              }

              Button(
                onClick = {
                  viewModel.saveDailyNote(personalNoteText, "EXPENSE_REFLECT")
                  Toast.makeText(context, "नोट सहेजा गया!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianCharcoal),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text(text = "Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = personalNoteText,
              onValueChange = { personalNoteText = it },
              placeholder = { Text("आज के खर्च पर अपना संक्षिप्त विचार लिखें (जैसे आज बाहर खाना ज्यादा हुआ, कल नियंत्रण रखेंगे)...", color = WarmMuted, fontSize = 12.sp) },
              maxLines = 3,
              minLines = 2,
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = GoldBrass,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = WarmOffWhite,
                unfocusedTextColor = WarmOffWhite
              )
            )
          }
        }
      }

      // 5. Mood Correlation
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, DarkSurfaceBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "मूड और खर्च का मेल (Mood Correlation)",
                color = WarmOffWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = moodAnalysis.second,
              color = WarmOffWhite,
              fontSize = 12.sp,
              lineHeight = 18.sp,
              modifier = Modifier
                .background(DarkSurface, RoundedCornerShape(10.dp))
                .padding(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              moodAnalysis.first.forEach { (emoji, label, stats) ->
                val (avg, count) = stats
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(4.dp)
                ) {
                  Text(text = emoji, fontSize = 22.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(text = label, color = WarmMuted, fontSize = 10.sp)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (count > 0) "$currencySymbol${avg.toInt()}" else "-",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(text = "$count बार", color = WarmMuted, fontSize = 9.sp)
                }
              }
            }
          }
        }
      }
    }
  }
}

// ----------------------------------------------------------------------------
// TAB 4: CUSTOMISE (CATEGORIES, QUICK CHIPS, RECURRING, BUDGET & DATA/SECURITY)
// ----------------------------------------------------------------------------
@Composable
private fun ExpenseCustomiseTabContent(
  monthlyBudget: Double,
  onUpdateBudget: (Double) -> Unit,
  categories: List<ExpenseCategoryEntity>,
  onAddCategoryClick: () -> Unit,
  onEditCategoryClick: (ExpenseCategoryEntity) -> Unit,
  quickChips: List<ExpenseQuickChipEntity>,
  onAddQuickChipClick: () -> Unit,
  onDeleteQuickChip: (Long) -> Unit,
  onMoveQuickChip: (ExpenseQuickChipEntity, Boolean) -> Unit,
  recurringExpenses: List<RecurringExpenseEntity>,
  onAddRecurringClick: () -> Unit,
  onEditRecurringClick: (RecurringExpenseEntity) -> Unit,
  onToggleRecurring: (RecurringExpenseEntity, Boolean) -> Unit,
  onDeleteRecurring: (RecurringExpenseEntity) -> Unit,
  currencySymbol: String,
  onCurrencyChange: (String) -> Unit,
  firstDayOfWeek: String,
  onFirstDayOfWeekChange: (String) -> Unit,
  reportColorTheme: String,
  onReportColorThemeChange: (String) -> Unit,
  isAppLockEnabled: Boolean,
  onConfigureAppLock: () -> Unit,
  onExportBackup: () -> Unit,
  onImportBackup: () -> Unit
) {
  val context = LocalContext.current
  var budgetInput by remember { mutableStateOf(monthlyBudget.toInt().toString()) }

  val currencies = listOf("₹", "$", "€", "£", "AED")
  val daysOfWeek = listOf("MONDAY" to "सोमवार", "SUNDAY" to "रविवार")

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Monthly Budget Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "मासिक बजट सेट करें",
            color = WarmOffWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "80% खर्च होने पर चेतावनी और 100% खर्च होने पर अलर्ट मिलेगा।",
            color = WarmMuted,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = budgetInput,
              onValueChange = { if (it.all { ch -> ch.isDigit() }) budgetInput = it },
              prefix = { Text("$currencySymbol ", color = CyanNeon, fontWeight = FontWeight.Bold) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = DarkSurfaceBorder,
                focusedTextColor = WarmOffWhite,
                unfocusedTextColor = WarmOffWhite
              )
            )

            Button(
              onClick = {
                val amt = budgetInput.toDoubleOrNull() ?: 15000.0
                onUpdateBudget(amt)
                Toast.makeText(context, "मासिक बजट $currencySymbol${amt.toInt()} सेट किया गया", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(50.dp)
            ) {
              Text(text = "अपडेट करें", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 2. Manage Recurring Expenses (किराया, रिचार्ज, बिल)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Repeat, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "आवर्ती खर्च (Recurring)",
                color = WarmOffWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }

            OutlinedButton(
              onClick = onAddRecurringClick,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
              border = BorderStroke(1.dp, CyanNeon)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("नया आवर्ती", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "किराया, रिचार्ज, बिल जो हर माह तय तारीख पर अपने आप दर्ज हो जाते हैं।",
            color = WarmMuted,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.height(10.dp))

          if (recurringExpenses.isEmpty()) {
            Text(text = "अभी कोई आवर्ती खर्च नहीं है", color = WarmMuted, fontSize = 12.sp)
          } else {
            recurringExpenses.forEach { rec ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .background(DarkSurface, RoundedCornerShape(10.dp))
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = rec.categoryEmoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(text = rec.title, color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  Text(
                    text = "$currencySymbol${rec.amount.toInt()} • हर माह ${rec.dayOfMonth} तारीख",
                    color = GoldBrass,
                    fontSize = 11.sp
                  )
                }

                Switch(
                  checked = rec.isEnabled,
                  onCheckedChange = { onToggleRecurring(rec, it) },
                  colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = DarkSurfaceBorder)
                )

                IconButton(onClick = { onEditRecurringClick(rec) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Edit, contentDescription = "संपादित", tint = WarmMuted, modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = { onDeleteRecurring(rec) }, modifier = Modifier.size(28.dp)) {
                  Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
              }
            }
          }
        }
      }
    }

    // 3. Manage Categories (Add, Edit, Delete, Category Budget)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "खर्च श्रेणियां व बजट",
              color = WarmOffWhite,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )

            OutlinedButton(
              onClick = onAddCategoryClick,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
              border = BorderStroke(1.dp, CyanNeon)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("नई श्रेणी", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          categories.forEach { cat ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(DarkSurface, RoundedCornerShape(10.dp))
                .clickable { onEditCategoryClick(cat) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(text = cat.emoji, fontSize = 20.sp)
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${cat.nameHi} (${cat.nameEn})",
                  color = WarmOffWhite,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                )
                if (cat.monthlyBudget > 0) {
                  Text(
                    text = "बजट सीमा: $currencySymbol${cat.monthlyBudget.toInt()}/माह",
                    color = GoldBrass,
                    fontSize = 11.sp
                  )
                }
              }

              IconButton(onClick = { onEditCategoryClick(cat) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "संपादित", tint = WarmMuted, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    // 4. Quick Chips Management (Add, Reorder up/down, Delete)
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "त्वरित बटन (Quick Chips)",
              color = WarmOffWhite,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )

            OutlinedButton(
              onClick = onAddQuickChipClick,
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
              border = BorderStroke(1.dp, CyanNeon)
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("नया बटन", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          quickChips.sortedBy { it.orderIndex }.forEachIndexed { idx, chip ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .background(DarkSurface, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(text = chip.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = chip.label, color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMoveQuickChip(chip, true) }, enabled = idx > 0, modifier = Modifier.size(26.dp)) {
                  Icon(Icons.Default.ArrowUpward, contentDescription = "ऊपर", tint = if (idx > 0) WarmOffWhite else WarmMuted, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { onMoveQuickChip(chip, false) }, enabled = idx < quickChips.size - 1, modifier = Modifier.size(26.dp)) {
                  Icon(Icons.Default.ArrowDownward, contentDescription = "नीचे", tint = if (idx < quickChips.size - 1) WarmOffWhite else WarmMuted, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = { onDeleteQuickChip(chip.id) }, modifier = Modifier.size(26.dp)) {
                  Icon(Icons.Default.Close, contentDescription = "हटाएं", tint = WarmMuted, modifier = Modifier.size(14.dp))
                }
              }
            }
          }
        }
      }
    }

    // 5. Preferences: Currency, First Day of Week, Report Theme
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(text = "पसंदीदा सेटिंग्स (Preferences)", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)

          // Currency symbol
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("करेंसी चिह्न (Currency):", color = WarmMuted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              currencies.forEach { sym ->
                val isSel = currencySymbol == sym
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) CyanNeon else DarkSurface)
                    .clickable { onCurrencyChange(sym) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(text = sym, color = if (isSel) ObsidianCharcoal else WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
              }
            }
          }

          // First day of week
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("हफ़्ते का पहला दिन:", color = WarmMuted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              daysOfWeek.forEach { (code, label) ->
                val isSel = firstDayOfWeek == code
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSel) CyanNeon else DarkSurface)
                    .clickable { onFirstDayOfWeekChange(code) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(text = label, color = if (isSel) ObsidianCharcoal else WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    }

    // 6. Security & Data Backup/Restore
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianElevated),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, DarkSurfaceBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(text = "सुरक्षा व डेटा बैकअप (Security & Data)", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)

          // App Lock Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(DarkSurface, RoundedCornerShape(10.dp))
              .clickable { onConfigureAppLock() }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (isAppLockEnabled) CyanNeon else WarmMuted
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("पिन लॉक (App Lock)", color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = if (isAppLockEnabled) "सक्रिय है (PIN सेट है)" else "निष्क्रिय (पिन सेट करें)", color = WarmMuted, fontSize = 11.sp)
              }
            }
            Text(text = "सेट करें >", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          // Backup & Restore Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = onExportBackup,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
              border = BorderStroke(1.dp, CyanNeon)
            ) {
              Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("बैकअप लें (JSON)", fontSize = 11.sp)
            }

            OutlinedButton(
              onClick = onImportBackup,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldBrass),
              border = BorderStroke(1.dp, GoldBrass)
            ) {
              Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("रीस्टोर (Import)", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

// ----------------------------------------------------------------------------
// DIALOGS: EDIT EXPENSE, ADD CATEGORY, ADD QUICK CHIP
// ----------------------------------------------------------------------------
@Composable
private fun EditExpenseDialog(
  expense: ExpenseEntity,
  categories: List<ExpenseCategoryEntity>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (ExpenseEntity) -> Unit,
  onDelete: (ExpenseEntity) -> Unit
) {
  var amountText by remember { mutableStateOf(if (expense.amount % 1.0 == 0.0) expense.amount.toInt().toString() else expense.amount.toString()) }
  var noteText by remember { mutableStateOf(expense.note) }
  var selectedCategory by remember {
    mutableStateOf(categories.firstOrNull { it.nameEn.equals(expense.category, ignoreCase = true) || it.nameHi == expense.category }
      ?: categories.firstOrNull() ?: ExpenseDefaults.DEFAULT_CATEGORIES.first())
  }
  var selectedMood by remember { mutableStateOf(expense.mood) }
  var showCatMenu by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("खर्च संपादित करें", color = WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        IconButton(onClick = { onDelete(expense) }) {
          Icon(Icons.Default.Delete, contentDescription = "डिलीट करें", tint = Color(0xFFEF4444))
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = amountText,
          onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) amountText = it },
          label = { Text("रकम ($currencySymbol)", color = WarmMuted) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        OutlinedTextField(
          value = noteText,
          onValueChange = { noteText = it },
          label = { Text("नोट", color = WarmMuted) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Box {
          OutlinedButton(
            onClick = { showCatMenu = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(text = "${selectedCategory.emoji} ${selectedCategory.nameHi} (${selectedCategory.nameEn})", color = WarmOffWhite)
          }

          DropdownMenu(
            expanded = showCatMenu,
            onDismissRequest = { showCatMenu = false },
            modifier = Modifier.background(ObsidianElevated)
          ) {
            categories.forEach { cat ->
              DropdownMenuItem(
                text = { Text("${cat.emoji} ${cat.nameHi}", color = WarmOffWhite) },
                onClick = {
                  selectedCategory = cat
                  showCatMenu = false
                }
              )
            }
          }
        }

        Text("मूड (वैकल्पिक):", color = WarmMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          ExpenseDefaults.MOOD_OPTIONS.forEach { (emoji, _) ->
            val isSel = selectedMood == emoji
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSel) CyanNeon.copy(alpha = 0.3f) else DarkSurface)
                .clickable { selectedMood = if (isSel) null else emoji },
              contentAlignment = Alignment.Center
            ) {
              Text(text = emoji, fontSize = 18.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: expense.amount
          val n = noteText.trim().ifBlank { expense.note }
          onSave(
            expense.copy(
              amount = amt,
              note = n,
              category = selectedCategory.nameEn,
              categoryEmoji = selectedCategory.emoji,
              categoryColorHex = selectedCategory.colorHex,
              mood = selectedMood
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)
      ) {
        Text("सहेजें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = WarmMuted)
      }
    }
  )
}

@Composable
private fun AddCategoryDialog(
  onDismiss: () -> Unit,
  onAddCategory: (nameHi: String, nameEn: String, emoji: String, colorHex: String, keywords: String) -> Unit
) {
  var nameHi by remember { mutableStateOf("") }
  var nameEn by remember { mutableStateOf("") }
  var emoji by remember { mutableStateOf("🏷️") }
  var keywords by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = { Text("नई श्रेणी जोड़ें", color = WarmOffWhite, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it },
            label = { Text("इमोजी") },
            modifier = Modifier.width(80.dp),
            singleLine = true
          )
          OutlinedTextField(
            value = nameHi,
            onValueChange = { nameHi = it },
            label = { Text("हिंदी नाम") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        OutlinedTextField(
          value = nameEn,
          onValueChange = { nameEn = it },
          label = { Text("अंग्रेजी नाम") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = keywords,
          onValueChange = { keywords = it },
          label = { Text("स्मार्ट कीवर्ड्स (कॉमा लगाकर)") },
          placeholder = { Text("जैसे: जिम,gym,fitness,protein") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (nameHi.isNotBlank() && nameEn.isNotBlank()) {
            onAddCategory(nameHi, nameEn, emoji.ifBlank { "📦" }, "#00F0FF", keywords)
          }
        },
        enabled = nameHi.isNotBlank() && nameEn.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)
      ) {
        Text("जोड़ें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = WarmMuted)
      }
    }
  )
}

@Composable
private fun AddQuickChipDialog(
  categories: List<ExpenseCategoryEntity>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onAddChip: (label: String, amount: Double, note: String, category: String, emoji: String) -> Unit
) {
  var label by remember { mutableStateOf("") }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var emoji by remember { mutableStateOf("⚡") }
  var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: ExpenseDefaults.DEFAULT_CATEGORIES.first()) }
  var showMenu by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = { Text("त्वरित बटन जोड़ें (Quick Chip)", color = WarmOffWhite, fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it },
            label = { Text("इमोजी") },
            modifier = Modifier.width(80.dp),
            singleLine = true
          )
          OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text("बटन का नाम (जैसे चाय ₹20)") },
            modifier = Modifier.weight(1f),
            singleLine = true
          )
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { if (it.all { ch -> ch.isDigit() || ch == '.' }) amountText = it },
          label = { Text("रकम ($currencySymbol)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("खर्च का विवरण (नोट)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Box {
          OutlinedButton(
            onClick = { showMenu = true },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("${selectedCategory.emoji} ${selectedCategory.nameHi}")
          }
          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            categories.forEach { c ->
              DropdownMenuItem(
                text = { Text("${c.emoji} ${c.nameHi}") },
                onClick = {
                  selectedCategory = c
                  showMenu = false
                }
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: 0.0
          if (label.isNotBlank() && amt > 0) {
            onAddChip(label, amt, note.ifBlank { label }, selectedCategory.nameEn, emoji)
          }
        },
        enabled = label.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)
      ) {
        Text("जोड़ें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = WarmMuted)
      }
    }
  )
}

// ----------------------------------------------------------------------------
// HELPER FUNCTIONS
// ----------------------------------------------------------------------------
private fun formatDisplayDate(dateStr: String): String {
  return try {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val outFmt = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("hi"))
    outFmt.format(sdf.parse(dateStr)!!)
  } catch (_: Exception) {
    dateStr
  }
}

private fun generateDayStory(expenses: List<ExpenseEntity>, dateStr: String, currencySymbol: String): String {
  if (expenses.isEmpty()) {
    return "$dateStr को कोई खर्च दर्ज नहीं हुआ। दिन पूरी तरह बचत भरा रहा! 🎉"
  }

  val total = expenses.sumOf { it.amount }
  val count = expenses.size
  val sorted = expenses.sortedBy { it.timestamp }
  val highest = expenses.maxByOrNull { it.amount }!!

  val sb = StringBuilder()
  sb.append("इस दिन आपने कुल $currencySymbol${total.toInt()} खर्च किए ($count बार में)। ")

  val first = sorted.first()
  sb.append("दिन की शुरुआत ${first.time} पर ${first.categoryEmoji} \"${first.note}\" ($currencySymbol${first.amount.toInt()}) से हुई। ")

  if (count > 1) {
    sb.append("दिन का सबसे बड़ा खर्च \"${highest.note}\" रहा, जिसमें $currencySymbol${highest.amount.toInt()} लगे। ")
  }

  val moodCounts = expenses.filter { !it.mood.isNullOrBlank() }.groupBy { it.mood!! }
  if (moodCounts.isNotEmpty()) {
    val topMood = moodCounts.maxByOrNull { it.value.size }?.key
    val moodLabel = ExpenseDefaults.MOOD_OPTIONS.firstOrNull { it.first == topMood }?.second ?: ""
    sb.append("दिन का मुख्य मूड $topMood $moodLabel रहा। ")
  }

  return sb.toString()
}

private fun calculateNoSpendStreak(allExpenses: List<ExpenseEntity>): Pair<Int, Int> {
  val datesWithExpenses = allExpenses.map { it.date }.toSet()
  val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  val cal = Calendar.getInstance()

  val todayDay = cal.get(Calendar.DAY_OF_MONTH)
  var noSpendMonthCount = 0
  val tempCal = Calendar.getInstance()
  for (day in 1..todayDay) {
    tempCal.set(Calendar.DAY_OF_MONTH, day)
    val dStr = sdf.format(tempCal.time)
    if (dStr !in datesWithExpenses) {
      noSpendMonthCount++
    }
  }

  var streak = 0
  val todayStr = sdf.format(cal.time)
  val checkCal = Calendar.getInstance()
  if (todayStr in datesWithExpenses) {
    checkCal.add(Calendar.DAY_OF_YEAR, -1)
  }

  for (i in 0 until 365) {
    val dStr = sdf.format(checkCal.time)
    if (dStr !in datesWithExpenses) {
      streak++
      checkCal.add(Calendar.DAY_OF_YEAR, -1)
    } else {
      break
    }
  }

  return Pair(streak, noSpendMonthCount)
}
