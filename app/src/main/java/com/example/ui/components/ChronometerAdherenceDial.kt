package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.MutedPlum
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luxury Chronograph Adherence Dial
 * Precision instrument inspired circular adherence ring showing today's completion percentage,
 * calculated daily score (e.g. "28.5 / 40 pts"), and the three compact stats: Done, Partial, Missed.
 */
@Composable
fun ChronometerAdherenceDial(
  completionPercentage: Int,
  completedCount: Int,
  partialCount: Int,
  missedCount: Int,
  totalTasks: Int,
  totalScore: Float,
  currentCycleDay: Int,
  modifier: Modifier = Modifier
) {
  val animatedProgress by animateFloatAsState(
    targetValue = (completionPercentage / 100f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
    label = "chrono_dial_progress"
  )

  // Calibrate max score to 40.0 pts or scale proportionally based on tasks
  val maxScore = if (totalTasks > 0) (totalTasks * 2.5f).coerceAtLeast(10f) else 40f
  val normalizedScore = if (totalTasks > 0) {
    // Each complete = 2.5 pts, partial = 1.25 pts
    (completedCount * 2.5f + partialCount * 1.25f)
  } else {
    totalScore
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(26.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            ObsidianElevated.copy(alpha = 0.95f),
            ObsidianCard.copy(alpha = 0.98f)
          )
        )
      )
      .border(
        width = 1.dp,
        brush = Brush.verticalGradient(
          colors = listOf(
            GoldBrass.copy(alpha = 0.45f),
            ObsidianBorder,
            GoldDark.copy(alpha = 0.25f)
          )
        ),
        shape = RoundedCornerShape(26.dp)
      )
      .padding(horizontal = 16.dp, vertical = 20.dp)
      .testTag("chronometer_adherence_card"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top subdial brand mark
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
            text = "CHRONOMETER PRECISION",
            color = GoldBrass,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
          )
        }

        Text(
          text = "DAY $currentCycleDay OF 7",
          color = WarmParchment,
          fontSize = 11.sp,
          fontFamily = ChronoSerifFamily,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.6.sp
        )
      }

      // Large Circular Adherence Dial with Horological Bezel Tick Marks
      Box(
        modifier = Modifier
          .size(210.dp)
          .testTag("chrono_progress_dial"),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.size(200.dp)) {
          val strokeWidth = 10.dp.toPx()
          val center = Offset(size.width / 2f, size.height / 2f)
          val radius = (size.minDimension / 2f) - (strokeWidth / 2f) - 6.dp.toPx()

          // 1. Horological 60-second/minute Bezel Tick Marks
          val outerBezelRadius = size.minDimension / 2f - 1.dp.toPx()
          for (i in 0 until 60) {
            val angleRad = (i * 6.0) * (PI / 180.0) - (PI / 2.0)
            val isMajorTick = (i % 5 == 0)
            val tickLength = if (isMajorTick) 7.dp.toPx() else 3.5.dp.toPx()
            val tickColor = if (isMajorTick) GoldBrass.copy(alpha = 0.75f) else ObsidianBorder.copy(alpha = 0.8f)
            val tickWidth = if (isMajorTick) 1.8.dp.toPx() else 1.0.dp.toPx()

            val startX = (center.x + (outerBezelRadius - tickLength) * cos(angleRad)).toFloat()
            val startY = (center.y + (outerBezelRadius - tickLength) * sin(angleRad)).toFloat()
            val endX = (center.x + outerBezelRadius * cos(angleRad)).toFloat()
            val endY = (center.y + outerBezelRadius * sin(angleRad)).toFloat()

            drawLine(
              color = tickColor,
              start = Offset(startX, startY),
              end = Offset(endX, endY),
              strokeWidth = tickWidth,
              cap = StrokeCap.Round
            )
          }

          // 2. Subtle Background Ring Track
          drawCircle(
            color = ObsidianBorder.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
          )

          // 3. Inner Decorative Hairline Concentric Ring
          drawCircle(
            color = GoldBrass.copy(alpha = 0.12f),
            radius = radius - strokeWidth - 6.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
          )

          // 4. Active Progress Arc in Horological Brass/Gold
          if (animatedProgress > 0f) {
            drawArc(
              brush = Brush.sweepGradient(
                0.0f to GoldDark,
                0.5f to GoldBrass,
                1.0f to GoldHighlight,
                center = center
              ),
              startAngle = -90f,
              sweepAngle = 360f * animatedProgress,
              useCenter = false,
              topLeft = Offset(center.x - radius, center.y - radius),
              size = Size(radius * 2, radius * 2),
              style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
              )
            )
          }
        }

        // Inner Dial Center Typography
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = "ADHERENCE",
            color = WarmMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "$completionPercentage%",
            color = WarmOffWhite,
            fontFamily = ChronoSerifFamily,
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          // Daily Score Display
          Text(
            text = String.format(Locale.US, "%.1f / %.0f pts", normalizedScore, maxScore),
            color = GoldBrass,
            fontFamily = ChronoSerifFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp
          )
        }
      }

      // 3. Three Compact Stats: Done, Partial, Missed
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(ObsidianElevated.copy(alpha = 0.8f))
          .border(0.8.dp, ObsidianBorder, RoundedCornerShape(16.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Done Stat
        CompactStatItem(
          label = "DONE",
          count = completedCount,
          indicatorColor = SageGreen,
          testTag = "chrono_stat_done"
        )

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(ObsidianBorder)
        )

        // Partial Stat
        CompactStatItem(
          label = "PARTIAL",
          count = partialCount,
          indicatorColor = MutedPlum,
          testTag = "chrono_stat_partial"
        )

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(ObsidianBorder)
        )

        // Missed Stat
        CompactStatItem(
          label = "MISSED",
          count = missedCount,
          indicatorColor = DustyRose,
          testTag = "chrono_stat_missed"
        )
      }
    }
  }
}

@Composable
private fun CompactStatItem(
  label: String,
  count: Int,
  indicatorColor: Color,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.testTag(testTag)
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(indicatorColor)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column {
      Text(
        text = label,
        color = WarmMuted,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
      )
      Text(
        text = "$count",
        color = WarmOffWhite,
        fontFamily = ChronoSerifFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}
