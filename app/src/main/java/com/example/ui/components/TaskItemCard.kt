package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
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
import com.example.ui.theme.ActiveCardBorderGradient
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExtraTaskAccent
import com.example.ui.theme.ExtraTaskBorderGradient
import com.example.ui.theme.ExtraTaskSurface
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

@Composable
fun TaskItemCard(
  task: DayTaskEntity,
  onStatusChange: (TaskStatus) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  isCurrentTask: Boolean = false,
  isNextTask: Boolean = false,
  isFutureTask: Boolean = false
) {
  var menuExpanded by remember { mutableStateOf(false) }

  val cardBorderBrush = when {
    isFutureTask -> Brush.linearGradient(listOf(DarkSurfaceBorder.copy(alpha = 0.35f), DarkSurfaceBorder.copy(alpha = 0.15f)))
    task.isExtra -> ExtraTaskBorderGradient
    isCurrentTask -> ActiveCardBorderGradient
    isNextTask -> Brush.linearGradient(listOf(VioletNeon.copy(alpha = 0.6f), DarkSurfaceBorder))
    else -> CardBorderGradient
  }

  val cardBackground = when {
    isFutureTask -> DarkSurface.copy(alpha = 0.45f)
    task.isExtra -> ExtraTaskSurface
    isCurrentTask -> DarkSurfaceElevated.copy(alpha = 0.95f)
    else -> DarkSurfaceElevated.copy(alpha = 0.85f)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(cardBackground)
      .border(
        width = if (isCurrentTask || task.isExtra) 1.2.dp else 1.dp,
        brush = cardBorderBrush,
        shape = RoundedCornerShape(18.dp)
      )
      .padding(horizontal = 14.dp, vertical = 12.dp)
      .testTag(if (isFutureTask) "task_item_locked_${task.id}" else "task_item_${task.id}")
  ) {
    Column {
      // 1. Top Header Row: Time Badge, Current/Next Indicators, Category, and Action Menu
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Time badge (Always 12-Hour AM/PM)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isCurrentTask) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
              .border(
                1.dp,
                if (isCurrentTask) CyanNeon.copy(alpha = 0.5f) else DarkSurfaceBorder,
                RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Icon(
              imageVector = Icons.Default.AccessTime,
              contentDescription = "Time",
              tint = if (isCurrentTask) CyanNeon else TextSecondary,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = TimeUtils.minutesToHindiTime(task.timeMinutes),
              color = if (isCurrentTask) CyanNeon else TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // "NOW" active indicator
          if (isCurrentTask) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CyanNeon)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00363D), modifier = Modifier.size(10.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "NOW",
                color = Color(0xFF00363D),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
              )
            }
          } else if (isNextTask) {
            // "UP NEXT" indicator
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VioletNeon.copy(alpha = 0.2f))
                .border(1.dp, VioletNeon.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(10.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text(
                text = "NEXT",
                color = VioletNeon,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }

          // Category Badge
          if (task.category.isNotBlank()) {
            Text(
              text = task.category,
              color = TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          // Priority Badge (Feature 6)
          if (task.priority.equals("HIGH", ignoreCase = true)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE53935).copy(alpha = 0.2f))
                .border(1.dp, Color(0xFFE53935).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "⚡ HIGH",
                color = Color(0xFFFF5252),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
              )
            }
          } else if (task.priority.equals("IMPORTANT", ignoreCase = true)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFFB300).copy(alpha = 0.2f))
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "⭐ IMPORTANT",
                color = Color(0xFFFFD54F),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }

          // Distinguishing Today Extra Task Badge
          if (task.isExtra) {
            Text(
              text = "⚡ EXTRA TASK",
              color = ExtraTaskAccent,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ExtraTaskAccent.copy(alpha = 0.15f))
                .border(1.dp, ExtraTaskAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (isFutureTask) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceBorder.copy(alpha = 0.5f))
                .border(1.dp, DarkSurfaceBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Locked",
                  tint = TextMuted,
                  modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = "समय आने पर खुलेगा",
                  color = TextMuted,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        // Action Menu
        Box {
          IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier.size(26.dp)
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options",
              tint = TextMuted,
              modifier = Modifier.size(16.dp)
            )
          }

          DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modifier = Modifier.background(DarkSurface)
          ) {
            DropdownMenuItem(
              text = { Text("Edit Task", color = TextPrimary, fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
              },
              onClick = {
                menuExpanded = false
                onEdit()
              }
            )
            DropdownMenuItem(
              text = { Text("Delete Task", color = StatusMissed, fontSize = 13.sp) },
              leadingIcon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = StatusMissed, modifier = Modifier.size(16.dp))
              },
              onClick = {
                menuExpanded = false
                onDelete()
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Task Title & Notes
      Text(
        text = task.name,
        color = if (isFutureTask) TextMuted else TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      if (task.notes.isNotBlank()) {
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = task.notes,
          color = TextSecondary,
          fontSize = 11.sp,
          lineHeight = 15.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 3. Subtle Status Lighting Chips: Complete, Partial, Missed
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        RefinedStatusChip(
          label = "Complete",
          score = "1.0",
          isSelected = task.status == TaskStatus.COMPLETE,
          accentColor = StatusComplete,
          dotEmoji = "🟢",
          onClick = { onStatusChange(TaskStatus.COMPLETE) },
          enabled = !isFutureTask,
          modifier = Modifier.weight(1f)
        )

        RefinedStatusChip(
          label = "Partial",
          score = "0.5",
          isSelected = task.status == TaskStatus.PARTIAL,
          accentColor = StatusPartial,
          dotEmoji = "🟡",
          onClick = { onStatusChange(TaskStatus.PARTIAL) },
          enabled = !isFutureTask,
          modifier = Modifier.weight(1f)
        )

        RefinedStatusChip(
          label = "Missed",
          score = "0",
          isSelected = task.status == TaskStatus.MISSED,
          accentColor = StatusMissed,
          dotEmoji = "🔴",
          onClick = { onStatusChange(TaskStatus.MISSED) },
          enabled = !isFutureTask,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun RefinedStatusChip(
  label: String,
  score: String,
  isSelected: Boolean,
  accentColor: Color,
  dotEmoji: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
  modifier: Modifier = Modifier
) {
  val animatedBg by animateColorAsState(
    targetValue = when {
      !enabled -> DarkSurface.copy(alpha = 0.3f)
      isSelected -> accentColor.copy(alpha = 0.18f)
      else -> DarkSurface
    },
    animationSpec = tween(250),
    label = "chip_bg"
  )
  val animatedBorder by animateColorAsState(
    targetValue = when {
      !enabled -> DarkSurfaceBorder.copy(alpha = 0.3f)
      isSelected -> accentColor
      else -> DarkSurfaceBorder
    },
    animationSpec = tween(250),
    label = "chip_border"
  )

  Box(
    modifier = modifier
      .height(34.dp)
      .clip(RoundedCornerShape(9.dp))
      .background(animatedBg)
      .border(1.dp, animatedBorder, RoundedCornerShape(9.dp))
      .clickable(enabled = enabled) { onClick() }
      .padding(horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (enabled) {
        Text(
          text = dotEmoji,
          fontSize = 10.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = if (enabled) label else "🔒 Locked",
        color = if (!enabled) TextMuted.copy(alpha = 0.5f) else if (isSelected) accentColor else TextSecondary,
        fontSize = 10.sp,
        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
      )
    }
  }
}
