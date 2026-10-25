package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySummary
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.CyanGlow
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
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils
import java.util.Calendar

@Composable
fun SummaryHeaderCard(
  summary: DaySummary,
  selectedDate: String,
  isToday: Boolean,
  onPrevDay: () -> Unit,
  onNextDay: () -> Unit,
  onJumpToday: () -> Unit,
  modifier: Modifier = Modifier,
  currentTimeStr: String? = null,
  streakInfo: com.example.data.model.StreakInfo? = null
) {
  val animatedProgress by animateFloatAsState(
    targetValue = (summary.completionPercentage / 100f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "progress_arc"
  )

  val infiniteTransition = rememberInfiniteTransition(label = "beacon")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  // Contextual greeting
  val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..11 -> "Good Morning"
    in 12..16 -> "Good Afternoon"
    in 17..21 -> "Good Evening"
    else -> "Late Night Review"
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(26.dp))
      .background(GlassGradient)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            CyanNeon.copy(alpha = 0.55f),
            VioletNeon.copy(alpha = 0.35f),
            DarkSurfaceBorder.copy(alpha = 0.6f)
          )
        ),
        shape = RoundedCornerShape(26.dp)
      )
      .padding(20.dp)
      .testTag("summary_header_card")
  ) {
    Column {
      // 1. Greeting & Beacon Status Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Pulsing Beacon Dot
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(CyanNeon.copy(alpha = pulseAlpha))
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isToday && !currentTimeStr.isNullOrBlank()) "$greeting • $currentTimeStr" else "$greeting • TRACKER ACTIVE",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
          )
        }

        // Date navigation buttons
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          IconButton(
            onClick = onPrevDay,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, CircleShape)
              .testTag("prev_day_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Previous Day",
              tint = TextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }

          if (!isToday) {
            Box(
              modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(CyanNeon.copy(alpha = 0.15f))
                .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(17.dp))
                .clickable { onJumpToday() }
                .padding(horizontal = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Today,
                  contentDescription = "Today",
                  tint = CyanNeon,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "Today",
                  color = CyanNeon,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          val canGoNext = !isToday && selectedDate < TimeUtils.getTodayDateString()

          IconButton(
            onClick = onNextDay,
            enabled = canGoNext,
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(if (canGoNext) DarkSurface else DarkSurface.copy(alpha = 0.35f))
              .border(
                1.dp,
                if (canGoNext) DarkSurfaceBorder else DarkSurfaceBorder.copy(alpha = 0.25f),
                CircleShape
              )
              .testTag("next_day_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = if (canGoNext) "Next Day" else "Next Day (Locked)",
              tint = if (canGoNext) TextSecondary else TextMuted.copy(alpha = 0.3f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 2. Formatted Date & 7-Day Cycle Orbital Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = TimeUtils.formatDateDisplay(selectedDate),
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = (-0.3).sp
        )

        // Continuous 7-Day Cycle Indicator with 7 Micro-Pips
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = "CYCLE DAY ${summary.dayOfCycle}/7",
            color = VioletNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            (1..7).forEach { dayNum ->
              val isActive = dayNum == summary.dayOfCycle
              Box(
                modifier = Modifier
                  .size(if (isActive) 6.dp else 4.dp)
                  .clip(CircleShape)
                  .background(
                    if (isActive) CyanNeon else VioletNeon.copy(alpha = 0.3f)
                  )
              )
            }
          }
        }
      }

      // Feature 1: Streak Badge
      if (streakInfo != null) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated.copy(alpha = 0.8f))
            .border(
              1.dp,
              Brush.horizontalGradient(
                listOf(
                  Color(0xFFFF9800).copy(alpha = 0.5f),
                  VioletNeon.copy(alpha = 0.35f),
                  CyanNeon.copy(alpha = 0.25f)
                )
              ),
              RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${streakInfo.currentStreak} Day Streak",
              color = Color(0xFFFFB74D),
              fontSize = 12.sp,
              fontWeight = FontWeight.Black
            )
            Text(
              text = " • Best: ${streakInfo.longestStreak}d",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(
                if (streakInfo.isMaintainedToday) Color(0xFF4CAF50).copy(alpha = 0.2f)
                else DarkSurface
              )
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = streakInfo.todayStatus,
              color = if (streakInfo.isMaintainedToday) Color(0xFF81C784) else TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      // 3. Main Circular Glowing Progress Donut + Compact Score Summary
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Glowing Double-Ring Circular Progress
        Box(
          modifier = Modifier.size(104.dp),
          contentAlignment = Alignment.Center
        ) {
          Canvas(modifier = Modifier.size(94.dp)) {
            // Background ring track
            drawArc(
              color = DarkSurfaceBorder,
              startAngle = 135f,
              sweepAngle = 270f,
              useCenter = false,
              style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
            )

            if (animatedProgress > 0f) {
              // Glowing gradient arc
              drawArc(
                brush = Brush.sweepGradient(
                  colors = listOf(CyanNeon, BlueAccent, VioletGlow, CyanNeon)
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
              )
            }
          }

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${summary.completionPercentage}%",
              color = TextPrimary,
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = (-0.5).sp
            )
            Text(
              text = "ADHERENCE",
              color = TextMuted,
              fontSize = 8.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.sp
            )
          }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right side: Compact Score + 3 Status Lighting Indicators
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Score summary bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Total Score",
              color = TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = String.format(java.util.Locale.US, "%.1f / %d pts", summary.totalScore, summary.totalTasks),
              color = CyanNeon,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // 3-Way Status Micro-Badges
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StatusMicroBadge(
              label = "Done",
              count = summary.completedCount,
              accentColor = StatusComplete,
              dotEmoji = "🟢",
              modifier = Modifier.weight(1f)
            )
            StatusMicroBadge(
              label = "Partial",
              count = summary.partialCount,
              accentColor = StatusPartial,
              dotEmoji = "🟡",
              modifier = Modifier.weight(1f)
            )
            StatusMicroBadge(
              label = "Missed",
              count = summary.missedCount,
              accentColor = StatusMissed,
              dotEmoji = "🔴",
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatusMicroBadge(
  label: String,
  count: Int,
  accentColor: Color,
  dotEmoji: String,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(accentColor.copy(alpha = 0.10f))
      .border(1.dp, accentColor.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
      .padding(vertical = 6.dp, horizontal = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = count.toString(),
        color = accentColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Black
      )
      Text(
        text = label,
        color = accentColor.copy(alpha = 0.85f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
