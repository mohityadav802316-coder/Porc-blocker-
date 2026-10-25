package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategoryEntity
import com.example.data.model.ExpenseDefaults
import com.example.data.model.ExpenseEntity
import com.example.data.model.ExpenseQuickChipEntity
import com.example.data.model.RecurringExpenseEntity
import com.example.expense.ExpenseBackupHelper
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite

@Composable
fun EditCategoryDialog(
  category: ExpenseCategoryEntity,
  onDismiss: () -> Unit,
  onSave: (ExpenseCategoryEntity) -> Unit,
  onDelete: () -> Unit
) {
  var nameHi by remember { mutableStateOf(category.nameHi) }
  var nameEn by remember { mutableStateOf(category.nameEn) }
  var emoji by remember { mutableStateOf(category.emoji) }
  var colorHex by remember { mutableStateOf(category.colorHex) }
  var keywords by remember { mutableStateOf(category.keywords) }
  var budgetText by remember { mutableStateOf(if (category.monthlyBudget > 0) category.monthlyBudget.toInt().toString() else "") }

  val colorPalette = listOf("#FF9800", "#00F0FF", "#10B981", "#8B5CF6", "#EC4899", "#06B6D4", "#F59E0B", "#3B82F6", "#64748B")

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("श्रेणी संपादित करें", color = WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "हटाएं", tint = Color(0xFFEF4444))
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = emoji,
            onValueChange = { emoji = it },
            label = { Text("आइकन") },
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
          value = budgetText,
          onValueChange = { if (it.all { ch -> ch.isDigit() }) budgetText = it },
          label = { Text("मासिक बजट सीमा (वैकल्पिक ₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Text("रंग चुनें:", color = WarmMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          colorPalette.forEach { hex ->
            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
            val isSelected = colorHex.equals(hex, ignoreCase = true)
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (isSelected) 2.dp else 0.dp, WarmOffWhite, CircleShape)
                .clickable { colorHex = hex }
            )
          }
        }

        OutlinedTextField(
          value = keywords,
          onValueChange = { keywords = it },
          label = { Text("स्मार्ट कीवर्ड्स (कॉमा लगाकर)") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val budget = budgetText.toDoubleOrNull() ?: 0.0
          onSave(
            category.copy(
              nameHi = nameHi.trim(),
              nameEn = nameEn.trim(),
              emoji = emoji.trim().ifBlank { "🏷️" },
              colorHex = colorHex,
              keywords = keywords.trim(),
              monthlyBudget = budget
            )
          )
        },
        enabled = nameHi.isNotBlank() && nameEn.isNotBlank(),
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
fun AddEditRecurringDialog(
  existing: RecurringExpenseEntity? = null,
  categories: List<ExpenseCategoryEntity>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (title: String, amount: Double, category: String, emoji: String, colorHex: String, dayOfMonth: Int) -> Unit
) {
  var title by remember { mutableStateOf(existing?.title ?: "") }
  var amountText by remember { mutableStateOf(existing?.amount?.toInt()?.toString() ?: "") }
  var dayText by remember { mutableStateOf(existing?.dayOfMonth?.toString() ?: "1") }
  var selectedCategory by remember {
    mutableStateOf(
      categories.firstOrNull { it.nameEn.equals(existing?.category, ignoreCase = true) || it.nameHi == existing?.category }
        ?: categories.firstOrNull() ?: ExpenseDefaults.DEFAULT_CATEGORIES.first()
    )
  }
  var showMenu by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = {
      Text(
        text = if (existing != null) "आवर्ती खर्च संपादित करें" else "नया आवर्ती खर्च जोड़ें",
        color = WarmOffWhite,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("शीर्षक (जैसे रूम किराया, मोबाइल बिल)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = amountText,
          onValueChange = { if (it.all { ch -> ch.isDigit() }) amountText = it },
          label = { Text("रकम ($currencySymbol)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        OutlinedTextField(
          value = dayText,
          onValueChange = { if (it.all { ch -> ch.isDigit() }) dayText = it },
          label = { Text("महीने की तारीख (1 से 31)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )

        Box {
          OutlinedButton(
            onClick = { showMenu = true },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("${selectedCategory.emoji} ${selectedCategory.nameHi} (${selectedCategory.nameEn})", color = WarmOffWhite)
          }

          DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            categories.forEach { c ->
              DropdownMenuItem(
                text = { Text("${c.emoji} ${c.nameHi}", color = WarmOffWhite) },
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
          val day = (dayText.toIntOrNull() ?: 1).coerceIn(1, 31)
          if (title.isNotBlank() && amt > 0) {
            onSave(title, amt, selectedCategory.nameEn, selectedCategory.emoji, selectedCategory.colorHex, day)
          }
        },
        enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
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
fun AppLockSetupDialog(
  currentPin: String,
  onDismiss: () -> Unit,
  onSave: (enabled: Boolean, pin: String) -> Unit
) {
  var pin by remember { mutableStateOf(currentPin) }
  var confirmPin by remember { mutableStateOf(currentPin) }
  var isEnabled by remember { mutableStateOf(currentPin.isNotBlank()) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = { Text("खर्च डायरी पिन लॉक (App Lock)", color = WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("पिन लॉक सक्रिय करें", color = WarmOffWhite, fontSize = 14.sp)
          Switch(
            checked = isEnabled,
            onCheckedChange = { isEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = DarkSurface)
          )
        }

        if (isEnabled) {
          OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pin = it },
            label = { Text("4 अंकों का पिन") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmPin = it },
            label = { Text("पिन की पुष्टि करें") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          errorMsg?.let {
            Text(text = it, color = Color(0xFFEF4444), fontSize = 12.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (!isEnabled) {
            onSave(false, "")
          } else {
            if (pin.length != 4) {
              errorMsg = "पिन 4 अंकों का होना चाहिए!"
            } else if (pin != confirmPin) {
              errorMsg = "दोनों पिन मेल नहीं खाते!"
            } else {
              onSave(true, pin)
            }
          }
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
fun RestoreBackupDialog(
  onDismiss: () -> Unit,
  onRestore: (json: String) -> Unit
) {
  var jsonInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianElevated,
    title = { Text("बैकअप रीस्टोर करें", color = WarmOffWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
    text = {
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "पहले एक्सपोर्ट की गई बैकअप JSON फ़ाइल का टेक्स्ट यहाँ पेस्ट करें:",
          color = WarmMuted,
          fontSize = 12.sp
        )

        OutlinedTextField(
          value = jsonInput,
          onValueChange = { jsonInput = it },
          placeholder = { Text("{\"version\":1, \"expenses\":[...]}", color = WarmMuted, fontSize = 11.sp) },
          maxLines = 8,
          minLines = 4,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (jsonInput.isNotBlank()) onRestore(jsonInput) },
        enabled = jsonInput.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = ObsidianCharcoal)
      ) {
        Text("रीस्टोर करें", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = WarmMuted)
      }
    }
  )
}
