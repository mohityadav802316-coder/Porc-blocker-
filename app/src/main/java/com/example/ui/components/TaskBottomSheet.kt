package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExtraTaskAccent
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskBottomSheet(
  initialTask: DayTaskEntity? = null,
  isExtraTaskDefault: Boolean = false,
  initialTimeMinutesDefault: Int? = null,
  onDismiss: () -> Unit,
  onSave: (id: Long, name: String, timeMinutes: Int, category: String, notes: String, isExtra: Boolean) -> Unit = { _, _, _, _, _, _ -> },
  onSaveWithPriority: ((id: Long, name: String, timeMinutes: Int, category: String, notes: String, isExtra: Boolean, priority: String) -> Unit)? = null,
  onDelete: ((Long) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var name by remember { mutableStateOf(initialTask?.name ?: "") }
  var timeMinutes by remember { mutableIntStateOf(initialTask?.timeMinutes ?: initialTimeMinutesDefault ?: 480) } // Default 8:00 AM (480 min)
  var category by remember { mutableStateOf(initialTask?.category ?: "Routine") }
  var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
  var isExtra by remember { mutableStateOf(initialTask?.isExtra ?: isExtraTaskDefault) }
  var priority by remember { mutableStateOf(initialTask?.priority ?: "NORMAL") }
  var showTimePicker by remember { mutableStateOf(false) }

  val defaultCategories = listOf("Morning", "Deep Work", "Fitness", "Health", "Learning", "Mindset", "Rest", "Routine")

  var showDeleteConfirmation by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurface,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    if (showDeleteConfirmation && initialTask != null && onDelete != null) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showDeleteConfirmation = false },
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        title = { Text("Delete Task?", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete \"${initialTask.name}\"? This action cannot be undone.", color = TextSecondary, fontSize = 13.sp) },
        confirmButton = {
          Button(
            onClick = {
              showDeleteConfirmation = false
              onDelete(initialTask.id)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = StatusMissed, contentColor = Color.White)
          ) {
            Text("Delete")
          }
        },
        dismissButton = {
          androidx.compose.material3.TextButton(onClick = { showDeleteConfirmation = false }) {
            Text("Cancel", color = TextSecondary)
          }
        }
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
        .testTag("task_bottom_sheet")
    ) {
      // Sheet Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = when {
              initialTask != null -> "Edit Task"
              isExtra -> "Add Today Extra Task"
              else -> "Add Daily Task"
            },
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (isExtra) "One-time task for today only" else "Timeline routine item",
            color = if (isExtra) ExtraTaskAccent else TextSecondary,
            fontSize = 12.sp
          )
        }

        if (initialTask != null && onDelete != null) {
          OutlinedButton(
            onClick = {
              showDeleteConfirmation = true
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusMissed),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(StatusMissed.copy(alpha = 0.5f))),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Task Name Input
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Task Name") },
        placeholder = { Text("e.g., Cold Plunge, Deep Work Block") },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("task_name_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 12-Hour Time Selector Button (Mandatory 12h format)
      Text(
        text = "Time (12-Hour Format)",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .clickable { showTimePicker = true }
          .padding(horizontal = 16.dp, vertical = 14.dp)
          .testTag("task_time_button"),
        contentAlignment = Alignment.CenterStart
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AccessTime,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = TimeUtils.minutesToHindiTime(timeMinutes),
              color = CyanNeon,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = "Change",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Category Chips
      Text(
        text = "Category",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        defaultCategories.forEach { cat ->
          val isSelected = category.equals(cat, ignoreCase = true)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
              .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
              .clickable { category = cat }
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = cat,
              color = if (isSelected) Color(0xFF00363D) else TextSecondary,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Priority Selection (Feature 6)
      Text(
        text = "Priority Level / प्राथमिकता",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          "NORMAL" to "Normal",
          "IMPORTANT" to "⭐ Important",
          "HIGH" to "⚡ High"
        ).forEach { (code, label) ->
          val isSelected = priority.equals(code, ignoreCase = true)
          val activeColor = when (code) {
            "HIGH" -> Color(0xFFFF5252)
            "IMPORTANT" -> Color(0xFFFFD54F)
            else -> CyanNeon
          }
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) activeColor.copy(alpha = 0.2f) else DarkSurfaceElevated)
              .border(1.dp, if (isSelected) activeColor else DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable { priority = code }
              .padding(vertical = 9.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              color = if (isSelected) activeColor else TextSecondary,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Extra Task Switch
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Today Extra Task",
            color = if (isExtra) ExtraTaskAccent else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Belongs only to this date, does not affect routine",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
        Switch(
          checked = isExtra,
          onCheckedChange = { isExtra = it },
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = ExtraTaskAccent,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = DarkBackground
          )
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Notes Input
      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes / Details (Optional)") },
        placeholder = { Text("Add specific instructions, targets...") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated
        ),
        shape = RoundedCornerShape(14.dp),
        maxLines = 3
      )

      Spacer(modifier = Modifier.height(24.dp))

      // Save Button
      Button(
        onClick = {
          if (name.isNotBlank()) {
            if (onSaveWithPriority != null) {
              onSaveWithPriority(
                initialTask?.id ?: 0L,
                name.trim(),
                timeMinutes,
                category.trim().ifEmpty { "Routine" },
                notes.trim(),
                isExtra,
                priority
              )
            } else {
              onSave(
                initialTask?.id ?: 0L,
                name.trim(),
                timeMinutes,
                category.trim().ifEmpty { "Routine" },
                notes.trim(),
                isExtra
              )
            }
            onDismiss()
          }
        },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isExtra) ExtraTaskAccent else CyanNeon,
          contentColor = if (isExtra) Color.White else Color(0xFF00363D)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_task_button"),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = if (initialTask != null) "Update Task" else "Save Task",
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }

  if (showTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = timeMinutes,
      onDismiss = { showTimePicker = false },
      onConfirm = { chosenMinutes ->
        timeMinutes = chosenMinutes
        showTimePicker = false
      }
    )
  }
}
