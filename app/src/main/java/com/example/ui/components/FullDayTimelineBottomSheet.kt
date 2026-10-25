package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskStatus
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.util.TimeUtils

enum class DayPeriod(val title: String, val emoji: String, val startMin: Int, val endMin: Int) {
  MORNING("सुबह", "🌅", 240, 719),      // 4:00 AM - 11:59 AM
  AFTERNOON("दोपहर", "🌤️", 720, 1019),   // 12:00 PM - 4:59 PM
  EVENING("शाम", "🌆", 1020, 1259),     // 5:00 PM - 8:59 PM
  NIGHT("रात", "🌙", 1260, 239);        // 9:00 PM - 3:59 AM

  companion object {
    /**
     * Strictly derives day part label from each task's real time minutes.
     * Never uses static or outdated category names.
     */
    fun fromMinutes(minutes: Int): DayPeriod {
      val norm = ((minutes % 1440) + 1440) % 1440
      return when (norm) {
        in 240..719 -> MORNING
        in 720..1019 -> AFTERNOON
        in 1020..1259 -> EVENING
        else -> NIGHT
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullDayTimelineBottomSheet(
  tasks: List<DayTaskEntity>,
  nowMinutes: Int,
  isToday: Boolean,
  selectedDate: String,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
  onDismiss: () -> Unit,
  onStatusChange: (DayTaskEntity, TaskStatus) -> Unit,
  onEditTask: (DayTaskEntity) -> Unit,
  onDeleteTask: (DayTaskEntity) -> Unit,
  onAddTask: () -> Unit
) {
  val currentPeriod = remember(nowMinutes) { DayPeriod.fromMinutes(nowMinutes) }
  var expandedPeriods by remember { mutableStateOf(setOf(currentPeriod)) }

  // Group tasks by strictly calculated day periods based on real block time (with safety deduplication)
  val sortedTasks = remember(tasks) {
    tasks.groupBy { if (it.templateId != null) "t_${it.templateId}" else if (!it.isExtra) "r_${it.name.trim().lowercase()}_${it.timeMinutes}" else "e_${it.id}" }
      .values
      .map { group ->
        group.sortedWith(
          compareByDescending<DayTaskEntity> { it.status.score }
            .thenBy { it.id }
        ).first()
      }
      .sortedBy { it.timeMinutes }
  }
  val periodGroups = remember(sortedTasks) {
    val map = mutableMapOf<DayPeriod, MutableList<DayTaskEntity>>()
    DayPeriod.entries.forEach { map[it] = mutableListOf() }
    sortedTasks.forEach { task ->
      val p = DayPeriod.fromMinutes(task.timeMinutes)
      map[p]?.add(task)
    }
    map
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = ObsidianCharcoal,
    scrimColor = Color.Black.copy(alpha = 0.7f),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(bottom = 24.dp)
        .testTag("full_day_timeline_sheet")
    ) {
      // 1. Header with title, date, Add '+' button, and Close
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "पूरा दिन की दिनचर्या",
            color = WarmOffWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = TimeUtils.getHindiDateDisplay(selectedDate),
            color = WarmMuted,
            fontSize = 13.sp
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Add Task '+' button in header
          IconButton(
            onClick = onAddTask,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(GoldBrass.copy(alpha = 0.15f))
              .testTag("timeline_add_block_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add Block",
              tint = GoldBrass,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(38.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = WarmMuted,
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // 2. Timeline List grouped by part of day (सुबह, दोपहर, शाम, रात)
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        if (tasks.isEmpty()) {
          item(key = "empty_timeline_message") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  text = "अभी कोई routine नहीं है",
                  color = WarmOffWhite,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "ऊपर '+' बटन दबाकर आज का कार्य जोड़ें",
                  color = WarmMuted,
                  fontSize = 13.sp
                )
              }
            }
          }
        }

        DayPeriod.entries.forEach { period ->
          val periodTasks = periodGroups[period] ?: emptyList()
          if (periodTasks.isNotEmpty()) {
            val isExpanded = expandedPeriods.contains(period)
            val completedInPeriod = periodTasks.count { it.status == TaskStatus.COMPLETE }

            item(key = "period_header_${period.name}") {
              // Collapsible period section header
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    expandedPeriods = if (isExpanded) {
                      expandedPeriods - period
                    } else {
                      expandedPeriods + period
                    }
                  }
                  .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "${period.emoji} ${period.title}",
                    color = if (period == currentPeriod) GoldBrass else WarmParchment,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "$completedInPeriod / ${periodTasks.size}",
                    color = WarmMuted,
                    fontSize = 12.sp
                  )
                }

                Icon(
                  imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                  contentDescription = null,
                  tint = WarmMuted,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            if (isExpanded) {
              items(
                items = periodTasks,
                key = { it.id }
              ) { task ->
                val nextTask = sortedTasks.firstOrNull { it.timeMinutes > task.timeMinutes }
                val taskEndTime = nextTask?.timeMinutes ?: (task.timeMinutes + 60)
                val isCurrent = isToday && nowMinutes in task.timeMinutes until taskEndTime
                val isPastUnfinished = isToday && nowMinutes >= taskEndTime && task.status != TaskStatus.COMPLETE

                TimelineRowItem(
                  task = task,
                  isCurrent = isCurrent,
                  isPastUnfinished = isPastUnfinished,
                  onToggleDone = {
                    val newStatus = if (task.status == TaskStatus.COMPLETE) TaskStatus.MISSED else TaskStatus.COMPLETE
                    onStatusChange(task, newStatus)
                  },
                  onStatusSelect = { onStatusChange(task, it) },
                  onEdit = { onEditTask(task) },
                  onDelete = { onDeleteTask(task) }
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
private fun TimelineRowItem(
  task: DayTaskEntity,
  isCurrent: Boolean,
  isPastUnfinished: Boolean,
  onToggleDone: () -> Unit,
  onStatusSelect: (TaskStatus) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  val isCompleted = task.status == TaskStatus.COMPLETE
  val isPartial = task.status == TaskStatus.PARTIAL
  var showMenu by remember { mutableStateOf(false) }

  val rowAlpha = if (isCompleted) 0.55f else 1.0f

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .alpha(rowAlpha)
      .clip(RoundedCornerShape(12.dp))
      .background(
        if (isCurrent) GoldBrass.copy(alpha = 0.08f) else Color.Transparent
      )
      .border(
        width = if (isCurrent) 1.dp else 0.dp,
        color = if (isCurrent) GoldBrass.copy(alpha = 0.35f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
      )
      .clickable { onToggleDone() }
      .padding(horizontal = 8.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Time Column
    Text(
      text = TimeUtils.minutesTo12Hour(task.timeMinutes),
      color = if (isCurrent) GoldBrass else WarmMuted,
      fontSize = 13.sp,
      fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
      modifier = Modifier.width(68.dp)
    )

    // 2. Status Dot / Check Indicator
    Box(
      modifier = Modifier
        .size(22.dp)
        .clip(CircleShape)
        .background(
          when {
            isCompleted -> SageGreen
            isPartial -> GoldBrass.copy(alpha = 0.35f)
            isPastUnfinished -> DustyRose.copy(alpha = 0.25f)
            isCurrent -> GoldBrass.copy(alpha = 0.25f)
            else -> ObsidianElevated
          }
        )
        .border(
          width = 1.dp,
          color = when {
            isCompleted -> SageGreen
            isPartial -> GoldBrass
            isPastUnfinished -> DustyRose
            isCurrent -> GoldBrass
            else -> ObsidianBorder
          },
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      if (isCompleted) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = "Completed",
          tint = ObsidianCharcoal,
          modifier = Modifier.size(13.dp)
        )
      } else if (isPastUnfinished) {
        Box(
          modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(DustyRose)
        )
      } else if (isCurrent) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(GoldBrass)
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // 3. Title & Optional Notes/Tags
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = task.name,
        color = if (isCurrent) WarmOffWhite else if (isCompleted) WarmMuted else WarmOffWhite,
        fontSize = 15.sp,
        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      if (isPastUnfinished) {
        Text(
          text = "छूट गया • पूरा करने के लिए टैप करें",
          color = DustyRose,
          fontSize = 11.sp
        )
      } else if (isCurrent) {
        Text(
          text = "वर्तमान समय ब्लॉक",
          color = GoldBrass,
          fontSize = 11.sp
        )
      }
    }

    // 4. Subtle Context Options Menu
    Box {
      IconButton(
        onClick = { showMenu = true },
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "Options",
          tint = WarmMuted,
          modifier = Modifier.size(16.dp)
        )
      }

      DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        modifier = Modifier
          .background(ObsidianElevated)
          .border(1.dp, ObsidianBorder, RoundedCornerShape(8.dp))
      ) {
        DropdownMenuItem(
          text = { Text("✅ पूर्ण (Complete)", color = WarmOffWhite, fontSize = 13.sp) },
          onClick = {
            onStatusSelect(TaskStatus.COMPLETE)
            showMenu = false
          }
        )
        DropdownMenuItem(
          text = { Text("🌓 आधा हुआ (Half)", color = WarmOffWhite, fontSize = 13.sp) },
          onClick = {
            onStatusSelect(TaskStatus.PARTIAL)
            showMenu = false
          }
        )
        DropdownMenuItem(
          text = { Text("❌ छूटा (Missed)", color = WarmOffWhite, fontSize = 13.sp) },
          onClick = {
            onStatusSelect(TaskStatus.MISSED)
            showMenu = false
          }
        )
        DropdownMenuItem(
          text = { Text("✏️ संपादित करें (Edit)", color = WarmOffWhite, fontSize = 13.sp) },
          onClick = {
            onEdit()
            showMenu = false
          }
        )
        DropdownMenuItem(
          text = { Text("🗑️ हटाएं (Delete)", color = DustyRose, fontSize = 13.sp) },
          onClick = {
            onDelete()
            showMenu = false
          }
        )
      }
    }
  }
}
