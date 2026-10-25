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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MutedPlum
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.util.TimeUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChronoTimeBlockDialog(
  initialTask: DayTaskEntity? = null,
  initialTimeMinutes: Int = 360,
  onDismiss: () -> Unit,
  onSave: (name: String, timeMinutes: Int, category: String, notes: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var name by remember { mutableStateOf(initialTask?.name ?: "") }
  var timeMinutes by remember { mutableIntStateOf(initialTask?.timeMinutes ?: initialTimeMinutes) }
  var category by remember { mutableStateOf(initialTask?.category ?: "Deep Work") }
  var notes by remember { mutableStateOf(initialTask?.notes ?: "") }
  var showTimePicker by remember { mutableStateOf(false) }

  val categories = listOf("Deep Work", "Fitness", "Health", "Learning", "Morning", "Rest")

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = modifier.testTag("chrono_time_block_dialog"),
    containerColor = ObsidianElevated,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "PRECISION TIME BLOCK",
            color = GoldBrass,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.0.sp
          )
          Text(
            text = if (initialTask == null) "Schedule New Block" else "Edit Time Block",
            color = WarmOffWhite,
            fontFamily = ChronoSerifFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
        }
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        // Time Picker Button
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .clickable { showTimePicker = true }
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Scheduled Time", color = WarmParchment, fontSize = 13.sp)
          }
          Text(
            text = TimeUtils.minutesTo12Hour(timeMinutes),
            color = GoldHighlight,
            fontFamily = ChronoSerifFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Activity Name
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Task / Activity Name", color = WarmMuted, fontSize = 12.sp) },
          placeholder = { Text("e.g. Deep Focus Work Block 1", color = WarmMuted.copy(alpha = 0.6f), fontSize = 12.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldBrass,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite,
            focusedContainerColor = ObsidianCard,
            unfocusedContainerColor = ObsidianCard
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Notes
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes / Objective (Optional)", color = WarmMuted, fontSize = 12.sp) },
          placeholder = { Text("e.g. Complete module architecture", color = WarmMuted.copy(alpha = 0.6f), fontSize = 12.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GoldBrass,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite,
            focusedContainerColor = ObsidianCard,
            unfocusedContainerColor = ObsidianCard
          ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        )

        // Categories Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Category", color = WarmParchment, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            categories.forEach { cat ->
              val isSelected = category.equals(cat, ignoreCase = true)
              val color = when (cat) {
                "Deep Work" -> GoldBrass
                "Fitness" -> SageGreen
                "Health" -> IceBlue
                "Learning" -> MutedPlum
                else -> WarmParchment
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isSelected) color.copy(alpha = 0.2f) else ObsidianCard)
                  .border(
                    width = if (isSelected) 1.dp else 0.6.dp,
                    color = if (isSelected) color else ObsidianBorder,
                    shape = RoundedCornerShape(10.dp)
                  )
                  .clickable { category = cat }
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Text(
                  text = cat,
                  color = if (isSelected) color else WarmMuted,
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onSave(name.trim(), timeMinutes, category, notes.trim())
          }
        },
        enabled = name.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
          containerColor = GoldBrass,
          disabledContainerColor = ObsidianBorder
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Save Block", color = Color(0xFF14120E), fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = WarmMuted, fontSize = 12.sp)
      }
    }
  )

  if (showTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = timeMinutes,
      onDismiss = { showTimePicker = false },
      onConfirm = { selectedMinutes ->
        timeMinutes = selectedMinutes
        showTimePicker = false
      }
    )
  }
}
