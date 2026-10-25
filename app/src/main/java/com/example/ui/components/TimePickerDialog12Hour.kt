package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.TimeUtils
import java.util.Locale

@Composable
fun TimePickerDialog12Hour(
  initialMinutes: Int,
  onDismiss: () -> Unit,
  onConfirm: (Int) -> Unit
) {
  val initialNormalized = ((initialMinutes % 1440) + 1440) % 1440
  val initialHour24 = initialNormalized / 60
  val initialIsPm = initialHour24 >= 12
  val initialHour12 = when {
    initialHour24 == 0 -> 12
    initialHour24 > 12 -> initialHour24 - 12
    else -> initialHour24
  }
  val initialMinute = initialNormalized % 60

  var selectedHour by remember { mutableIntStateOf(initialHour12) }
  var selectedMinute by remember { mutableIntStateOf(initialMinute) }
  var isPm by remember { mutableStateOf(initialIsPm) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(24.dp))
      .testTag("time_picker_dialog"),
    title = {
      Text(
        text = "Select Time (12-Hour AM/PM)",
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Preview Banner
        val previewMinutes = run {
          val h24 = when {
            selectedHour == 12 && !isPm -> 0
            selectedHour == 12 && isPm -> 12
            isPm -> selectedHour + 12
            else -> selectedHour
          }
          h24 * 60 + selectedMinute
        }
        val previewHindiTime = TimeUtils.minutesToHindiTime(previewMinutes)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = previewHindiTime,
            color = CyanNeon,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hour Selection (1..12)
        Text(
          text = "Hour",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          (1..6).forEach { h ->
            TimePillButton(
              text = "$h",
              isSelected = selectedHour == h,
              onClick = { selectedHour = h }
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          (7..12).forEach { h ->
            TimePillButton(
              text = "$h",
              isSelected = selectedHour == h,
              onClick = { selectedHour = h }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Minute Selection
        Text(
          text = "Minute",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        val minuteOptions = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          minuteOptions.take(6).forEach { m ->
            TimePillButton(
              text = String.format(Locale.US, "%02d", m),
              isSelected = selectedMinute == m,
              onClick = { selectedMinute = m }
            )
          }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          minuteOptions.drop(6).forEach { m ->
            TimePillButton(
              text = String.format(Locale.US, "%02d", m),
              isSelected = selectedMinute == m,
              onClick = { selectedMinute = m }
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AM / PM Segmented Selector
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AmPmToggleTab(
            text = "AM",
            isSelected = !isPm,
            onClick = { isPm = false },
            modifier = Modifier.weight(1f)
          )
          AmPmToggleTab(
            text = "PM",
            isSelected = isPm,
            onClick = { isPm = true },
            modifier = Modifier.weight(1f)
          )
        }
      }
    },
    confirmButton = {
      ElevatedButton(
        onClick = {
          val finalMinutes = TimeUtils.parse12HourToMinutes(selectedHour, selectedMinute, isPm)
          onConfirm(finalMinutes)
        },
        colors = ButtonDefaults.elevatedButtonColors(
          containerColor = CyanNeon,
          contentColor = Color(0xFF00363D)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("time_confirm_button")
      ) {
        Text("Apply Time", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondary)
      }
    }
  )
}

@Composable
private fun TimePillButton(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .size(width = 44.dp, height = 36.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
      .border(
        width = 1.dp,
        color = if (isSelected) CyanNeon else DarkSurfaceBorder,
        shape = RoundedCornerShape(8.dp)
      )
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isSelected) Color(0xFF00363D) else TextPrimary,
      fontSize = 13.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
private fun AmPmToggleTab(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .height(44.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) CyanNeon else Color.Transparent)
      .clickable { onClick() },
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isSelected) Color(0xFF00363D) else TextSecondary,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
