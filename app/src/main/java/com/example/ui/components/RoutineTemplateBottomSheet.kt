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
import androidx.compose.material3.Text
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
import com.example.data.model.RoutineTemplateEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoutineTemplateBottomSheet(
  initialTemplate: RoutineTemplateEntity? = null,
  onDismiss: () -> Unit,
  onSave: (id: Long, name: String, timeMinutes: Int, category: String, notes: String, daysMask: Int, isActive: Boolean) -> Unit,
  onDelete: ((Long) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
  var timeMinutes by remember { mutableIntStateOf(initialTemplate?.timeMinutes ?: 300) } // Default 5:00 AM
  var category by remember { mutableStateOf(initialTemplate?.category ?: "Routine") }
  var notes by remember { mutableStateOf(initialTemplate?.notes ?: "") }
  var showTimePicker by remember { mutableStateOf(false) }

  val defaultCategories = listOf("Morning", "Deep Work", "Fitness", "Health", "Learning", "Mindset", "Rest", "Routine")

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurface,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 36.dp)
        .testTag("routine_template_bottom_sheet")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (initialTemplate != null) "Edit Routine Step" else "Add Routine Step",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Generates daily task automatically for upcoming days",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }

        if (initialTemplate != null && onDelete != null) {
          OutlinedButton(
            onClick = {
              onDelete(initialTemplate.id)
              onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusMissed),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 12.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Step Name") },
        placeholder = { Text("e.g., Morning Meditation, Exercise, Review") },
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
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 12-Hour Time Selector Button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
          .clickable { showTimePicker = true }
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Scheduled Time (Hindi Time Format)", color = TextMuted, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = TimeUtils.minutesToHindiTime(timeMinutes),
            color = CyanNeon,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Icon(Icons.Default.AccessTime, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Category Chips
      Text("Category", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      Spacer(modifier = Modifier.height(6.dp))
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

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Details / Intentions (Optional)") },
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
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(
              initialTemplate?.id ?: 0L,
              name.trim(),
              timeMinutes,
              category.trim().ifEmpty { "Routine" },
              notes.trim(),
              127,
              true
            )
            onDismiss()
          }
        },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00363D)),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text(
          text = if (initialTemplate != null) "Update Routine Step" else "Save Routine Step",
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
