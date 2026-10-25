package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.JournalistCategory
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalistEntryBottomSheet(
  initialEntry: JournalistEntryEntity? = null,
  persons: List<JournalistPersonEntity>,
  defaultPersonId: String = "self",
  onDismiss: () -> Unit,
  onSave: (
    id: String?,
    personId: String,
    personName: String,
    category: JournalistCategory,
    text: String,
    date: String,
    time: String,
    context: String,
    intensity: String
  ) -> Unit,
  onDelete: ((String) -> Unit)? = null
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var selectedPersonId by remember(initialEntry) {
    mutableStateOf(initialEntry?.personId ?: defaultPersonId)
  }
  var category by remember(initialEntry) {
    mutableStateOf(initialEntry?.category ?: JournalistCategory.OBSERVATION)
  }
  var text by remember(initialEntry) { mutableStateOf(initialEntry?.text ?: "") }
  var dateStr by remember(initialEntry) {
    mutableStateOf(initialEntry?.date ?: TimeUtils.getTodayDateString())
  }
  var timeStr by remember(initialEntry) {
    mutableStateOf(initialEntry?.time ?: TimeUtils.getCurrentTimeString())
  }
  var contextStr by remember(initialEntry) { mutableStateOf(initialEntry?.context ?: "") }
  var intensityStr by remember(initialEntry) { mutableStateOf(initialEntry?.intensity ?: "") }
  var isError by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }

  val contextOptions = listOf("घर", "ऑफिस", "दोस्त", "अकेला", "यात्रा", "सोशल मीडिया", "अन्य")
  val intensityOptions = listOf("हल्का", "मध्यम", "गहरा")

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurface,
    tonalElevation = 8.dp,
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 10.dp)
          .size(width = 44.dp, height = 4.dp)
          .background(DarkSurfaceBorder, RoundedCornerShape(2.dp))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header Title
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (initialEntry == null) "NEW OBSERVATION" else "EDIT OBSERVATION",
          color = CyanNeon,
          fontSize = 12.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.2.sp
        )

        if (initialEntry != null && onDelete != null) {
          OutlinedButton(
            onClick = {
              onDelete(initialEntry.id)
              onDismiss()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusMissed),
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusMissed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.testTag("delete_journalist_entry_btn")
          ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("हटाएं", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 1. Person Selector (Horizontally scrollable person chips)
      Text(
        text = "व्यक्ति (OBSERVED PERSON)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        persons.forEach { person ->
          val isSelected = selectedPersonId == person.id
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
              .border(
                1.dp,
                if (isSelected) CyanNeon else DarkSurfaceBorder,
                RoundedCornerShape(10.dp)
              )
              .clickable { selectedPersonId = person.id }
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(person.emoji, fontSize = 14.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = person.name,
                color = if (isSelected) CyanNeon else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 2. Category Selector (Good, Bad, Observation)
      Text(
        text = "CATEGORY (प्रकार)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        JournalistCategory.entries.forEach { cat ->
          val isSelected = category == cat
          val accentColor = when (cat) {
            JournalistCategory.GOOD -> StatusComplete
            JournalistCategory.BAD -> StatusMissed
            JournalistCategory.OBSERVATION -> CyanNeon
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) accentColor.copy(alpha = 0.2f) else DarkSurfaceElevated)
              .border(
                1.dp,
                if (isSelected) accentColor else DarkSurfaceBorder,
                RoundedCornerShape(10.dp)
              )
              .clickable { category = cat }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(cat.iconPrefix, fontSize = 13.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = cat.label,
                color = if (isSelected) accentColor else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3. Observation Text Input (Primary)
      Text(
        text = "विवरण (OBSERVATION TEXT) *",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = text,
        onValueChange = {
          text = it
          if (it.isNotBlank()) isError = false
        },
        placeholder = {
          Text(
            "निष्पक्ष पत्रकार की नज़र से लिखें कि क्या हुआ या क्या देखा...",
            color = TextMuted,
            fontSize = 13.sp
          )
        },
        isError = isError,
        supportingText = if (isError) {
          { Text("विवरण खाली नहीं हो सकता", color = StatusMissed, fontSize = 11.sp) }
        } else null,
        minLines = 3,
        maxLines = 6,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurfaceElevated,
          unfocusedContainerColor = DarkSurfaceElevated,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_journalist_text")
      )

      Spacer(modifier = Modifier.height(14.dp))

      // 4. Date & Time Row
      Text(
        text = "तारीख और समय (DATE & TIME)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = dateStr,
          onValueChange = { dateStr = it },
          label = { Text("Date (YYYY-MM-DD)", fontSize = 11.sp) },
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("input_journalist_date")
        )

        OutlinedTextField(
          value = timeStr,
          onValueChange = { timeStr = it },
          label = { Text("Time (HH:mm)", fontSize = 11.sp) },
          trailingIcon = {
            IconButton(
              onClick = { showTimePicker = true },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Pick Time",
                tint = CyanNeon,
                modifier = Modifier.size(18.dp)
              )
            }
          },
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("input_journalist_time")
        )
      }

      // Natural Hindi Time Preview & Quick "Now" button
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val timeHindiPreview = TimeUtils.timeStringToHindi(timeStr)
        Text(
          text = "समय प्रारूप: $timeHindiPreview",
          color = CyanNeon.copy(alpha = 0.85f),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyanNeon.copy(alpha = 0.12f))
            .border(0.5.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable {
              timeStr = TimeUtils.getCurrentTimeString()
              dateStr = TimeUtils.getTodayDateString()
            }
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("btn_journalist_set_now")
        ) {
          Text(
            text = "🕒 अभी का समय (Now)",
            color = CyanNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 5. Context Tags (Quick Selection Chips)
      Text(
        text = "संदर्भ (CONTEXT / SITUATION)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        contextOptions.forEach { opt ->
          val isSelected = contextStr == opt
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) VioletNeon.copy(alpha = 0.25f) else DarkSurfaceElevated)
              .border(1.dp, if (isSelected) VioletNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
              .clickable { contextStr = if (isSelected) "" else opt }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = opt,
              color = if (isSelected) VioletNeon else TextSecondary,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 6. Intensity Selection (हल्का, मध्यम, गहरा)
      Text(
        text = "तीव्रता (INTENSITY)",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        intensityOptions.forEach { opt ->
          val isSelected = intensityStr == opt
          val color = when (opt) {
            "हल्का" -> CyanNeon
            "मध्यम" -> VioletNeon
            else -> StatusMissed
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) color.copy(alpha = 0.2f) else DarkSurfaceElevated)
              .border(1.dp, if (isSelected) color else DarkSurfaceBorder, RoundedCornerShape(8.dp))
              .clickable { intensityStr = if (isSelected) "" else opt }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = opt,
              color = if (isSelected) color else TextSecondary,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(22.dp))

      // Action Buttons (Cancel / Save)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.weight(1f)
        ) {
          Text("रद्द करें")
        }

        Button(
          onClick = {
            if (text.isBlank()) {
              isError = true
              return@Button
            }
            val resolvedPerson = persons.firstOrNull { it.id == selectedPersonId }
            val resolvedName = resolvedPerson?.name ?: "मैं (स्वयं)"
            onSave(
              initialEntry?.id,
              selectedPersonId,
              resolvedName,
              category,
              text,
              dateStr.ifBlank { TimeUtils.getTodayDateString() },
              timeStr.ifBlank { TimeUtils.getCurrentTimeString() },
              contextStr,
              intensityStr
            )
            onDismiss()
          },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color.Black),
          modifier = Modifier
            .weight(1f)
            .testTag("btn_save_journalist_entry")
        ) {
          Text("सेव करें", fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  if (showTimePicker) {
    val parts = timeStr.trim().split(":")
    val parsedMinutes = if (parts.size >= 2) {
      val h = parts[0].toIntOrNull() ?: 12
      val m = parts[1].toIntOrNull() ?: 0
      (h * 60 + m) % 1440
    } else {
      TimeUtils.getCurrentMinutes()
    }
    TimePickerDialog12Hour(
      initialMinutes = parsedMinutes,
      onDismiss = { showTimePicker = false },
      onConfirm = { chosenMinutes ->
        val h = chosenMinutes / 60
        val m = chosenMinutes % 60
        timeStr = String.format(java.util.Locale.US, "%02d:%02d", h, m)
        showTimePicker = false
      }
    )
  }
}
