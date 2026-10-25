package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskStatus
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

data class TimeBlockData(
  val startMinutes: Int,
  val endMinutes: Int,
  val label: String,
  val tasks: List<DayTaskEntity>
)

@Composable
fun CollapsibleTimeBlockCard(
  block: TimeBlockData,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onAddSmallTask: () -> Unit,
  onStatusChange: (DayTaskEntity, TaskStatus) -> Unit,
  onEditTask: (DayTaskEntity) -> Unit,
  onDeleteTask: (DayTaskEntity) -> Unit,
  currentTaskId: Long?,
  isCurrentBlock: Boolean,
  isFutureBlock: Boolean = false,
  modifier: Modifier = Modifier
) {
  val totalTasks = block.tasks.size
  val completedTasks = block.tasks.count { it.status == TaskStatus.COMPLETE }
  val partialTasks = block.tasks.count { it.status == TaskStatus.PARTIAL }
  val allDone = totalTasks > 0 && completedTasks == totalTasks

  val arrowRotation by animateFloatAsState(
    targetValue = if (isExpanded && !isFutureBlock) 180f else 0f,
    animationSpec = tween(durationMillis = 250),
    label = "arrowRotation"
  )

  val borderBrush = when {
    isFutureBlock -> androidx.compose.ui.graphics.Brush.linearGradient(
      listOf(DarkSurfaceBorder.copy(alpha = 0.35f), DarkSurfaceBorder.copy(alpha = 0.15f))
    )
    isCurrentBlock -> androidx.compose.ui.graphics.Brush.linearGradient(
      listOf(CyanNeon.copy(alpha = 0.7f), DarkSurfaceBorder)
    )
    allDone -> androidx.compose.ui.graphics.Brush.linearGradient(
      listOf(StatusComplete.copy(alpha = 0.5f), DarkSurfaceBorder)
    )
    else -> CardBorderGradient
  }

  val cardBackgroundBrush = if (isFutureBlock) {
    androidx.compose.ui.graphics.Brush.linearGradient(
      listOf(DarkSurface.copy(alpha = 0.45f), DarkSurface.copy(alpha = 0.45f))
    )
  } else {
    GlassGradient
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(cardBackgroundBrush)
      .border(1.dp, borderBrush, RoundedCornerShape(18.dp))
      .testTag(if (isFutureBlock) "time_block_locked_${block.startMinutes}" else "time_block_${block.startMinutes}")
  ) {
    Column {
      // 1. Collapsible Block Header Row (Not clickable if future timetable)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(enabled = !isFutureBlock, onClick = onToggleExpand)
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Time Range Label with Natural Hindi Display
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Status indicator dot
          Box(
            modifier = Modifier
              .size(9.dp)
              .clip(CircleShape)
              .background(
                when {
                  isFutureBlock -> DarkSurfaceBorder.copy(alpha = 0.5f)
                  isCurrentBlock -> CyanNeon
                  allDone -> StatusComplete
                  completedTasks > 0 || partialTasks > 0 -> StatusPartial
                  else -> DarkSurfaceBorder
                }
              )
          )

          Spacer(modifier = Modifier.width(10.dp))

          Text(
            text = block.label,
            color = when {
              isFutureBlock -> TextMuted
              isCurrentBlock -> CyanNeon
              else -> TextPrimary
            },
            fontSize = 14.sp,
            fontWeight = if (isCurrentBlock) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isFutureBlock) {
          // Locked Pill for future time table
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurfaceElevated.copy(alpha = 0.8f))
              .border(1.dp, DarkSurfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = TextMuted,
                modifier = Modifier.size(11.dp)
              )
              Text(
                text = "समय आने पर खुलेगा",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        } else {
          // Right: Task Count Badge & Animated Chevron (▼ / ▲)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Progress / Count Pill
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  when {
                    allDone -> StatusComplete.copy(alpha = 0.15f)
                    isCurrentBlock -> CyanNeon.copy(alpha = 0.15f)
                    else -> DarkSurfaceElevated
                  }
                )
                .border(
                  1.dp,
                  when {
                    allDone -> StatusComplete.copy(alpha = 0.4f)
                    isCurrentBlock -> CyanNeon.copy(alpha = 0.4f)
                    else -> DarkSurfaceBorder
                  },
                  RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = if (allDone) "$completedTasks/$totalTasks पूर्ण ✓" else "$completedTasks/$totalTasks",
                color = if (allDone) StatusComplete else if (isCurrentBlock) CyanNeon else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Expand / Collapse Chevron Icon
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = if (isExpanded) "Collapse" else "Expand",
              tint = if (isExpanded) CyanNeon else TextMuted,
              modifier = Modifier
                .size(20.dp)
                .rotate(arrowRotation)
            )
          }
        }
      }

      // 2. Expanded Content: List of Small Tasks + Add Small Task Button
      AnimatedVisibility(
        visible = isExpanded && !isFutureBlock,
        enter = expandVertically(animationSpec = tween(220)) + fadeIn(),
        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 12.dp)
        ) {
          HorizontalDivider(
            color = DarkSurfaceBorder.copy(alpha = 0.6f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          if (block.tasks.isEmpty()) {
            Text(
              text = "इस टाइम ब्लॉक में अभी कोई टास्क नहीं है।",
              color = TextMuted,
              fontSize = 12.sp,
              modifier = Modifier.padding(vertical = 8.dp)
            )
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              block.tasks.forEach { task ->
                val isCurrentTask = task.id == currentTaskId
                SmallTaskRow(
                  task = task,
                  isCurrent = isCurrentTask,
                  isFuture = isFutureBlock,
                  onStatusChange = { newStatus -> onStatusChange(task, newStatus) },
                  onEdit = { onEditTask(task) },
                  onDelete = { onDeleteTask(task) }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Add Small Task in this Block Button
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurfaceElevated.copy(alpha = 0.7f))
              .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
              .clickable(onClick = onAddSmallTask)
              .padding(vertical = 8.dp, horizontal = 12.dp)
              .testTag("add_small_task_${block.startMinutes}"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "+ छोटा टास्क जोड़ें",
              color = CyanNeon,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

/**
 * Compact, scannable small task checklist row inside a time block.
 * Maintains full Complete / Partial / Missed support and edit/delete actions.
 */
@Composable
fun SmallTaskRow(
  task: DayTaskEntity,
  isCurrent: Boolean,
  isFuture: Boolean = false,
  onStatusChange: (TaskStatus) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showStatusMenu by remember { mutableStateOf(false) }

  val isComplete = task.status == TaskStatus.COMPLETE
  val isPartial = task.status == TaskStatus.PARTIAL
  val isMissed = task.status == TaskStatus.MISSED

  val animatedBg by animateColorAsState(
    targetValue = when {
      isFuture -> DarkSurface.copy(alpha = 0.35f)
      isCurrent -> DarkSurfaceElevated.copy(alpha = 0.95f)
      isComplete -> DarkSurface.copy(alpha = 0.6f)
      else -> DarkSurface.copy(alpha = 0.45f)
    },
    label = "taskRowBg"
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(animatedBg)
      .border(
        1.dp,
        if (isCurrent) CyanNeon.copy(alpha = 0.4f) else DarkSurfaceBorder.copy(alpha = if (isFuture) 0.3f else 0.6f),
        RoundedCornerShape(10.dp)
      )
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left: Status Checkbox & Task Details
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .weight(1f)
        .clickable(enabled = !isFuture) {
          // Fast toggle: Missed -> Complete -> Missed
          val nextStatus = if (isComplete) TaskStatus.MISSED else TaskStatus.COMPLETE
          onStatusChange(nextStatus)
        }
    ) {
      // Checkbox Box
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(
            when {
              isFuture -> DarkSurfaceElevated.copy(alpha = 0.6f)
              isComplete -> StatusComplete
              isPartial -> StatusPartial
              isMissed -> StatusMissed
              else -> DarkSurfaceElevated
            }
          )
          .border(
            1.dp,
            when {
              isFuture -> DarkSurfaceBorder.copy(alpha = 0.5f)
              isComplete -> StatusComplete
              isPartial -> StatusPartial
              isMissed -> StatusMissed
              else -> DarkSurfaceBorder
            },
            RoundedCornerShape(6.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        if (isFuture) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Task Locked",
            tint = TextMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
          )
        } else {
          when {
            isComplete -> {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete",
                tint = Color(0xFF003314),
                modifier = Modifier.size(16.dp)
              )
            }
            isPartial -> {
              Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Partial",
                tint = Color(0xFF332000),
                modifier = Modifier.size(16.dp)
              )
            }
            isMissed -> {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Missed",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // Task Name + Short Hindi Time + Category
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = task.name,
            color = when {
              isFuture -> TextMuted
              isComplete -> TextMuted
              isCurrent -> CyanNeon
              else -> TextPrimary
            },
            fontSize = 13.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
            textDecoration = if (isComplete) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Short Hindi Time
          Text(
            text = TimeUtils.minutesToShortHindiTime(task.timeMinutes),
            color = if (isFuture) TextMuted.copy(alpha = 0.6f) else TextSecondary,
            fontSize = 10.sp
          )

          if (task.category.isNotBlank() && !task.category.equals("Routine", ignoreCase = true)) {
            Text(
              text = "•",
              color = TextMuted,
              fontSize = 9.sp
            )
            Text(
              text = task.category,
              color = TextMuted,
              fontSize = 10.sp
            )
          }

          if (task.notes.isNotBlank()) {
            Text(
              text = "• 📝",
              color = TextMuted,
              fontSize = 9.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Right: Status Switcher & Edit / Delete
    if (isFuture) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(DarkSurfaceElevated.copy(alpha = 0.6f))
          .border(1.dp, DarkSurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
          .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Text(
          text = "🔒 Locked",
          color = TextMuted,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // Fast 3-State Picker Button (Complete ✓ / Partial ½ / Missed ✗)
        Box {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(6.dp))
              .clickable { showStatusMenu = true }
              .padding(horizontal = 6.dp, vertical = 3.dp)
          ) {
            Text(
              text = when (task.status) {
                TaskStatus.COMPLETE -> "✓ पूर्ण"
                TaskStatus.PARTIAL -> "½ आधा"
                TaskStatus.MISSED -> "✗ छूटा"
              },
              color = when (task.status) {
                TaskStatus.COMPLETE -> StatusComplete
                TaskStatus.PARTIAL -> StatusPartial
                TaskStatus.MISSED -> StatusMissed
              },
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }

          DropdownMenu(
            expanded = showStatusMenu,
            onDismissRequest = { showStatusMenu = false },
            modifier = Modifier.background(DarkSurfaceElevated)
          ) {
            DropdownMenuItem(
              text = { Text("✓ Complete (पूर्ण)", color = StatusComplete, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
              onClick = {
                onStatusChange(TaskStatus.COMPLETE)
                showStatusMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("½ Partial (आधा)", color = StatusPartial, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
              onClick = {
                onStatusChange(TaskStatus.PARTIAL)
                showStatusMenu = false
              }
            )
            DropdownMenuItem(
              text = { Text("✗ Missed (छूट गया)", color = StatusMissed, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
              onClick = {
                onStatusChange(TaskStatus.MISSED)
                showStatusMenu = false
              }
            )
          }
        }

        // Edit Button
        IconButton(
          onClick = onEdit,
          modifier = Modifier.size(26.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Task",
            tint = TextSecondary,
            modifier = Modifier.size(13.dp)
          )
        }

        // Delete Button
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(26.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete Task",
            tint = StatusMissed.copy(alpha = 0.8f),
            modifier = Modifier.size(13.dp)
          )
        }
      }
    }
  }
}
