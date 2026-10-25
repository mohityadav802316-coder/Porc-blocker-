package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeditationType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

enum class QuickAddType {
  ROUTINE,
  TODAY_TASK,
  DAILY_NOTE,
  MEDITATION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
  onDismiss: () -> Unit,
  onAddRoutine: (name: String, timeMinutes: Int, category: String, priority: String) -> Unit,
  onAddTodayTask: (name: String, timeMinutes: Int, category: String, priority: String) -> Unit,
  onSaveNote: (note: String, mood: String) -> Unit,
  onStartMeditation: (type: MeditationType, durationMinutes: Int) -> Unit,
  initialMood: String = "NORMAL",
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var selectedType by remember { mutableStateOf(QuickAddType.TODAY_TASK) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkBackground,
    scrimColor = Color.Black.copy(alpha = 0.7f),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 6.dp)
          .width(40.dp)
          .height(4.dp)
          .clip(CircleShape)
          .background(DarkSurfaceBorder)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "⚡ Quick Add",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "तुरंत एक्टिविटी, टास्क, नोट या ध्यान जोड़ें",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp
          )
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Tab selector
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        QuickAddTabChip(
          title = "Today Task",
          icon = Icons.Default.TaskAlt,
          isSelected = selectedType == QuickAddType.TODAY_TASK,
          onClick = { selectedType = QuickAddType.TODAY_TASK },
          modifier = Modifier.weight(1f)
        )
        QuickAddTabChip(
          title = "Routine",
          icon = Icons.Default.Schedule,
          isSelected = selectedType == QuickAddType.ROUTINE,
          onClick = { selectedType = QuickAddType.ROUTINE },
          modifier = Modifier.weight(1f)
        )
        QuickAddTabChip(
          title = "Note",
          icon = Icons.Default.EditNote,
          isSelected = selectedType == QuickAddType.DAILY_NOTE,
          onClick = { selectedType = QuickAddType.DAILY_NOTE },
          modifier = Modifier.weight(1f)
        )
        QuickAddTabChip(
          title = "Meditation",
          icon = Icons.Default.SelfImprovement,
          isSelected = selectedType == QuickAddType.MEDITATION,
          onClick = { selectedType = QuickAddType.MEDITATION },
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(18.dp))

      // Content based on tab
      when (selectedType) {
        QuickAddType.TODAY_TASK -> {
          QuickAddTaskForm(
            isRoutine = false,
            onConfirm = { name, time, cat, prio ->
              onAddTodayTask(name, time, cat, prio)
              onDismiss()
            }
          )
        }
        QuickAddType.ROUTINE -> {
          QuickAddTaskForm(
            isRoutine = true,
            onConfirm = { name, time, cat, prio ->
              onAddRoutine(name, time, cat, prio)
              onDismiss()
            }
          )
        }
        QuickAddType.DAILY_NOTE -> {
          QuickAddNoteForm(
            initialMood = initialMood,
            onConfirm = { note, mood ->
              onSaveNote(note, mood)
              onDismiss()
            }
          )
        }
        QuickAddType.MEDITATION -> {
          QuickAddMeditationForm(
            onConfirm = { type, duration ->
              onStartMeditation(type, duration)
              onDismiss()
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
private fun QuickAddTabChip(
  title: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bg = if (isSelected) CyanNeon.copy(alpha = 0.18f) else DarkSurface
  val border = if (isSelected) CyanNeon else DarkSurfaceBorder
  val tint = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.6f)

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bg)
      .border(1.dp, border, RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
      )
    }
  }
}

@Composable
private fun QuickAddTaskForm(
  isRoutine: Boolean,
  onConfirm: (name: String, timeMinutes: Int, category: String, priority: String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var timeMinutes by remember { mutableIntStateOf(540) } // Default 9:00 AM
  var category by remember { mutableStateOf("Deep Work") }
  var priority by remember { mutableStateOf("NORMAL") }
  var showTimeDialog by remember { mutableStateOf(false) }

  val categories = listOf("Deep Work", "Routine", "Health", "Study", "Personal", "Evening")

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    OutlinedTextField(
      value = name,
      onValueChange = { name = it },
      label = { Text(if (isRoutine) "रूटीन एक्टिविटी नाम" else "टास्क का नाम") },
      placeholder = { Text("उदा. 20m Book Reading") },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("quick_add_name_field"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanNeon,
        unfocusedBorderColor = DarkSurfaceBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
      )
    )

    // Time Selection Button (Strict 12-Hour AM/PM)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
        .clickable { showTimeDialog = true }
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Schedule, contentDescription = "Time", tint = CyanNeon, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("निर्धारित समय (12-Hour AM/PM)", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
      }
      Text(
        text = TimeUtils.formatTime12Hour(timeMinutes),
        color = CyanNeon,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
      )
    }

    // Category Selector
    Text("श्रेणी (Category):", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      categories.take(3).forEach { cat ->
        QuickChip(
          text = cat,
          isSelected = category == cat,
          onClick = { category = cat },
          modifier = Modifier.weight(1f)
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      categories.drop(3).take(3).forEach { cat ->
        QuickChip(
          text = cat,
          isSelected = category == cat,
          onClick = { category = cat },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Priority Selector
    Text("प्राथमिकता (Priority):", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      QuickPriorityChip(
        label = "Normal",
        isSelected = priority == "NORMAL",
        color = Color.White.copy(alpha = 0.6f),
        onClick = { priority = "NORMAL" },
        modifier = Modifier.weight(1f)
      )
      QuickPriorityChip(
        label = "Important ⭐",
        isSelected = priority == "IMPORTANT",
        color = Color(0xFFFFB300),
        onClick = { priority = "IMPORTANT" },
        modifier = Modifier.weight(1f)
      )
      QuickPriorityChip(
        label = "High ⚡",
        isSelected = priority == "HIGH",
        color = Color(0xFFFF5252),
        onClick = { priority = "HIGH" },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Button(
      onClick = {
        if (name.isNotBlank()) {
          onConfirm(name.trim(), timeMinutes, category, priority)
        }
      },
      enabled = name.isNotBlank(),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("quick_add_submit_button"),
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanNeon,
        contentColor = DarkBackground
      ),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = if (isRoutine) "Add to Master Routine" else "Add Today Task",
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
      )
    }
  }

  if (showTimeDialog) {
    TimePickerDialog12Hour(
      initialMinutes = timeMinutes,
      onConfirm = { mins ->
        timeMinutes = mins
        showTimeDialog = false
      },
      onDismiss = { showTimeDialog = false }
    )
  }
}

@Composable
private fun QuickAddNoteForm(
  initialMood: String,
  onConfirm: (note: String, mood: String) -> Unit
) {
  var noteText by remember { mutableStateOf("") }
  var selectedMood by remember { mutableStateOf(initialMood) }

  val moods = listOf(
    Pair("GREAT", "🤩 Great"),
    Pair("GOOD", "🙂 Good"),
    Pair("NORMAL", "😐 Normal"),
    Pair("LOW", "😔 Low"),
    Pair("BAD", "😫 Bad")
  )

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("आज का मूड कैसा रहा?", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      moods.forEach { (moodKey, moodLabel) ->
        val isSelected = selectedMood == moodKey
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
            .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { selectedMood = moodKey }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = moodLabel,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }

    OutlinedTextField(
      value = noteText,
      onValueChange = { noteText = it },
      label = { Text("आज का अनुभव या विचार (Daily Note)") },
      placeholder = { Text("आज क्या सीखा, क्या अच्छा हुआ...") },
      minLines = 3,
      maxLines = 5,
      modifier = Modifier.fillMaxWidth(),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanNeon,
        unfocusedBorderColor = DarkSurfaceBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
      )
    )

    Button(
      onClick = { onConfirm(noteText, selectedMood) },
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = VioletNeon,
        contentColor = Color.White
      ),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Save Reflection Note", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
  }
}

@Composable
private fun QuickAddMeditationForm(
  onConfirm: (type: MeditationType, durationMinutes: Int) -> Unit
) {
  var selectedType by remember { mutableStateOf(MeditationType.BREATHING) }
  var selectedDuration by remember { mutableIntStateOf(10) }

  val durations = listOf(5, 10, 15, 20, 30)

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("मेडिटेशन प्रकार (Type):", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

    val quickTypes = listOf(
      MeditationType.BREATHING,
      MeditationType.MINDFULNESS,
      MeditationType.FOCUS,
      MeditationType.RELAXATION,
      MeditationType.SLEEP,
      MeditationType.GRATITUDE
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      quickTypes.take(3).forEach { type ->
        QuickChip(
          text = type.hindiTitle,
          isSelected = selectedType == type,
          onClick = { selectedType = type },
          modifier = Modifier.weight(1f)
        )
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      quickTypes.drop(3).take(3).forEach { type ->
        QuickChip(
          text = type.hindiTitle,
          isSelected = selectedType == type,
          onClick = { selectedType = type },
          modifier = Modifier.weight(1f)
        )
      }
    }

    Text("अवधि (Duration):", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      durations.forEach { dur ->
        val isSelected = selectedDuration == dur
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
            .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
            .clickable { selectedDuration = dur }
            .padding(vertical = 10.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$dur m",
            color = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Button(
      onClick = { onConfirm(selectedType, selectedDuration) },
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = CyanNeon,
        contentColor = DarkBackground
      ),
      shape = RoundedCornerShape(14.dp)
    ) {
      Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Start Mindful Session (${selectedDuration}m)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
  }
}

@Composable
private fun QuickChip(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
      .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = if (isSelected) CyanNeon else Color.White.copy(alpha = 0.7f),
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
    )
  }
}

@Composable
private fun QuickPriorityChip(
  label: String,
  isSelected: Boolean,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) color.copy(alpha = 0.2f) else DarkSurface)
      .border(1.dp, if (isSelected) color else DarkSurfaceBorder, RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (isSelected) color else Color.White.copy(alpha = 0.6f),
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
  }
}
