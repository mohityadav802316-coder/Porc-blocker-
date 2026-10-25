package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShortContentDailySummaryEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ShortContentSummaryCard(
  summary: ShortContentDailySummaryEntity?,
  isAccessibilityEnabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val totalCount = summary?.totalCount ?: 0
  val dailyLimit = summary?.dailyLimit ?: 20
  val remaining = (dailyLimit - totalCount).coerceAtLeast(0)
  val totalMinutes = ((summary?.totalTimeSeconds ?: 0L) / 60).toInt()
  val progress = if (dailyLimit > 0) (totalCount.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f

  val statusColor = when {
    totalCount >= dailyLimit -> Color(0xFFFF5252) // Red
    totalCount >= (dailyLimit * 0.8f).toInt() -> Color(0xFFFF9800) // Amber/Orange
    totalCount >= (dailyLimit * 0.5f).toInt() -> Color(0xFFFFD54F) // Yellow
    else -> CyanNeon
  }

  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(600, easing = FastOutSlowInEasing),
    label = "short_content_progress"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(
        1.dp,
        if (totalCount >= dailyLimit) Color(0xFFFF5252).copy(alpha = 0.5f) else DarkSurfaceBorder,
        RoundedCornerShape(18.dp)
      )
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("dashboard_short_content_card")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
      // Top row: Title + Accessibility indicator + Arrow
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(statusColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.SmartDisplay,
              contentDescription = null,
              tint = statusColor,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "SHORT CONTENT TRACKER",
              color = TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "शॉर्ट्स व रील्स सीमा ट्रैकर",
              color = TextMuted,
              fontSize = 10.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (!isAccessibilityEnabled) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFFF9800).copy(alpha = 0.18f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Perm Off", color = Color(0xFFFF9800), fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Details",
            tint = TextSecondary,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      // Middle row: Stats count + time + remaining
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "$totalCount",
              color = statusColor,
              fontSize = 22.sp,
              fontWeight = FontWeight.Black
            )
            Text(
              text = "/$dailyLimit shorts",
              color = TextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = if (remaining > 0) "$remaining शेष (Remaining)" else "🚨 सीमा समाप्त (Limit Exceeded)",
            color = if (remaining > 0) TextSecondary else Color(0xFFFF5252),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "$totalMinutes min",
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
          // App breakdown summary
          val ig = summary?.instagramCount ?: 0
          val yt = summary?.youtubeCount ?: 0
          Text(
            text = "IG: $ig • YT: $yt",
            color = TextMuted,
            fontSize = 10.sp
          )
        }
      }

      // Progress bar
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(CircleShape),
        color = statusColor,
        trackColor = DarkSurface
      )
    }
  }
}
