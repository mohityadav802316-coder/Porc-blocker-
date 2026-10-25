package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskStatus
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.DustyRoseSurface
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MutedPlum
import com.example.ui.theme.MutedPlumSurface
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurfaceHighlight
import com.example.ui.theme.SageGreen
import com.example.ui.theme.SageGreenSurface
import com.example.ui.theme.WarmDisabled
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.util.TimeUtils

/**
 * Vertical Timeline of Daily Schedule
 * Shows actual timetable blocks with time range, phase label, task details,
 * category badges, and quick-toggle status controls.
 */
@Composable
fun ChronometerScheduleTimeline(
  tasks: List<DayTaskEntity>,
  nowMinutes: Int,
  isToday: Boolean,
  onStatusChange: (DayTaskEntity, TaskStatus) -> Unit,
  onEditTask: (DayTaskEntity) -> Unit,
  onDeleteTask: (DayTaskEntity) -> Unit,
  onAddTask: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Schedule Section Header with Add Block action
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(GoldBrass)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "DAILY SCHEDULE & TIMETABLE",
          color = GoldBrass,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.0.sp
        )
      }

      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(ObsidianElevated)
          .border(0.8.dp, ObsidianBorder, RoundedCornerShape(12.dp))
          .clickable { onAddTask() }
          .padding(horizontal = 10.dp, vertical = 5.dp)
          .testTag("chrono_add_schedule_block"),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Add Time Block",
          tint = GoldBrass,
          modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Add Block",
          color = WarmOffWhite,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    if (tasks.isEmpty()) {
      // Empty Schedule State
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(ObsidianElevated)
          .border(0.8.dp, ObsidianBorder, RoundedCornerShape(18.dp))
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = WarmMuted,
            modifier = Modifier.size(32.dp)
          )
          Text(
            text = "No Schedule Blocks Configured",
            color = WarmOffWhite,
            fontFamily = ChronoSerifFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Add custom time blocks or load routine templates from Settings.",
            color = WarmMuted,
            fontSize = 12.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = onAddTask,
            colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Create First Time Block", color = Color(0xFF14120E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    } else {
      // Sequence of Schedule Blocks connected by a vertical timeline
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        tasks.forEachIndexed { index, task ->
          val nextTask = tasks.getOrNull(index + 1)
          val endTimeMinutes = nextTask?.timeMinutes ?: (task.timeMinutes + 45)
          val isCurrent = isToday && nowMinutes >= task.timeMinutes && nowMinutes < endTimeMinutes

          ChronoTimeBlockItem(
            task = task,
            startTimeMinutes = task.timeMinutes,
            endTimeMinutes = endTimeMinutes,
            isCurrent = isCurrent,
            onStatusChange = { newStatus -> onStatusChange(task, newStatus) },
            onEdit = { onEditTask(task) },
            onDelete = { onDeleteTask(task) }
          )
        }
      }
    }
  }
}

@Composable
private fun ChronoTimeBlockItem(
  task: DayTaskEntity,
  startTimeMinutes: Int,
  endTimeMinutes: Int,
  isCurrent: Boolean,
  onStatusChange: (TaskStatus) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  // Phase label derived from start time
  val phaseLabel = remember(startTimeMinutes) {
    when (startTimeMinutes) {
      in 240..420 -> "DAWN & WAKE UP"
      in 421..660 -> "MORNING FOCUS"
      in 661..840 -> "MIDDAY BLOCK"
      in 841..1080 -> "AFTERNOON FOCUS"
      in 1081..1260 -> "EVENING FITNESS"
      else -> "NIGHT RESTORE"
    }
  }

  // Category Color Accent
  val (categoryColor, categoryLabel) = remember(task.category) {
    when (task.category.lowercase()) {
      "fitness", "gym", "workout" -> Pair(SageGreen, "Fitness")
      "health", "hydration", "nutrition" -> Pair(IceBlue, "Health")
      "deep work", "focus", "work" -> Pair(GoldBrass, "Deep Work")
      "learning", "study", "reading" -> Pair(MutedPlum, "Learning")
      "morning", "rest", "sleep" -> Pair(WarmParchment, task.category)
      else -> Pair(WarmParchment, task.category.ifBlank { "Routine" })
    }
  }

  val statusColor by animateColorAsState(
    targetValue = when (task.status) {
      TaskStatus.COMPLETE -> SageGreen
      TaskStatus.PARTIAL -> MutedPlum
      TaskStatus.MISSED -> DustyRose
    },
    label = "task_status_color"
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(if (isCurrent) ObsidianSurfaceHighlight else ObsidianCard)
      .border(
        width = if (isCurrent) 1.2.dp else 0.8.dp,
        brush = if (isCurrent) {
          Brush.horizontalGradient(listOf(GoldHighlight, GoldBrass, ObsidianBorder))
        } else {
          Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
        },
        shape = RoundedCornerShape(18.dp)
      )
      .padding(14.dp)
      .testTag("chrono_time_block_${task.id}")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      // Top line: Time range + Phase Label + Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Status indicator pip
          Box(
            modifier = Modifier
              .size(9.dp)
              .clip(CircleShape)
              .background(statusColor)
              .border(
                width = 1.dp,
                color = if (isCurrent) GoldHighlight else Color.Transparent,
                shape = CircleShape
              )
          )
          Spacer(modifier = Modifier.width(8.dp))
          // Time range formatted
          Text(
            text = "${TimeUtils.minutesTo12Hour(startTimeMinutes)} — ${TimeUtils.minutesTo12Hour(endTimeMinutes)}",
            color = if (isCurrent) GoldBrass else WarmOffWhite,
            fontFamily = ChronoSerifFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          if (isCurrent) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(GoldBrass.copy(alpha = 0.18f))
                .border(0.6.dp, GoldBrass, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "ACTIVE NOW",
                color = GoldHighlight,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Small Category Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(categoryColor.copy(alpha = 0.12f))
              .border(0.6.dp, categoryColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
              .padding(horizontal = 7.dp, vertical = 2.dp)
          ) {
            Text(
              text = categoryLabel,
              color = categoryColor,
              fontSize = 9.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(modifier = Modifier.width(4.dp))
          Box {
            IconButton(
              onClick = { showMenu = true },
              modifier = Modifier.size(24.dp)
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
              modifier = Modifier.background(ObsidianElevated)
            ) {
              DropdownMenuItem(
                text = { Text("Edit Block", color = WarmOffWhite, fontSize = 12.sp) },
                onClick = {
                  showMenu = false
                  onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(16.dp)) }
              )
              DropdownMenuItem(
                text = { Text("Delete Block", color = DustyRose, fontSize = 12.sp) },
                onClick = {
                  showMenu = false
                  onDelete()
                },
                leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = DustyRose, modifier = Modifier.size(16.dp)) }
              )
            }
          }
        }
      }

      // Task Name & Phase
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = task.name,
            color = WarmOffWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
          if (task.notes.isNotBlank()) {
            Text(
              text = task.notes,
              color = WarmMuted,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          } else {
            Text(
              text = phaseLabel,
              color = WarmMuted,
              fontSize = 10.sp,
              letterSpacing = 0.5.sp
            )
          }
        }

        // Tap-able Status Control: Done | Partial | Missed
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Done Button
          val isDone = task.status == TaskStatus.COMPLETE
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isDone) SageGreen else SageGreenSurface)
              .border(
                width = 0.8.dp,
                color = if (isDone) SageGreen else SageGreen.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
              )
              .clickable {
                onStatusChange(if (isDone) TaskStatus.MISSED else TaskStatus.COMPLETE)
              }
              .padding(horizontal = 9.dp, vertical = 6.dp)
              .testTag("chrono_status_done_${task.id}"),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Done",
                tint = if (isDone) Color(0xFF14120E) else SageGreen,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "✓",
                color = if (isDone) Color(0xFF14120E) else SageGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Partial Button
          val isPartial = task.status == TaskStatus.PARTIAL
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isPartial) MutedPlum else MutedPlumSurface)
              .border(
                width = 0.8.dp,
                color = if (isPartial) MutedPlum else MutedPlum.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
              )
              .clickable {
                onStatusChange(if (isPartial) TaskStatus.MISSED else TaskStatus.PARTIAL)
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("chrono_status_partial_${task.id}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "~ Half",
              color = if (isPartial) Color(0xFF14120E) else MutedPlum,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Missed Button
          val isMissed = task.status == TaskStatus.MISSED
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isMissed) DustyRoseSurface else Color.Transparent)
              .border(
                width = 0.8.dp,
                color = if (isMissed) DustyRose.copy(alpha = 0.5f) else ObsidianBorder,
                shape = RoundedCornerShape(10.dp)
              )
              .clickable {
                onStatusChange(TaskStatus.MISSED)
              }
              .padding(horizontal = 6.dp, vertical = 6.dp)
              .testTag("chrono_status_missed_${task.id}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "✕",
              color = if (isMissed) DustyRose else WarmDisabled,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
