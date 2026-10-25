package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PersonalInsightsData
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.VioletNeon

@Composable
fun PersonalInsightsCard(
  insights: PersonalInsightsData,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(DarkSurfaceElevated)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          listOf(
            CyanNeon.copy(alpha = 0.4f),
            DarkSurfaceBorder,
            VioletNeon.copy(alpha = 0.35f)
          )
        ),
        shape = RoundedCornerShape(20.dp)
      )
      .padding(16.dp)
      .testTag("personal_insights_card")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CyanNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Psychology,
              contentDescription = "Insights",
              tint = CyanNeon,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Personal Insights (व्यक्तिगत विश्लेषण)",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "वास्तविक ऐतिहासिक डेटा पर आधारित तथ्य",
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 11.sp
            )
          }
        }

        // Factual badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Text("100% Factual", color = CyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Top 3 Metric Cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Routine Consistency
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground.copy(alpha = 0.7f))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
        ) {
          Column {
            Text("Routine", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(
              text = "${insights.routineConsistencyPercentage}%",
              color = CyanNeon,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${insights.qualifyingDaysCount}/${insights.totalRecordedDays} दिन",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 10.sp
            )
          }
        }

        // Meditation
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground.copy(alpha = 0.7f))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
        ) {
          Column {
            Text("Meditation", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(
              text = "${insights.totalMindfulMinutes} m",
              color = VioletNeon,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${insights.totalMeditationSessions} सत्र",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 10.sp
            )
          }
        }

        // Streak
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkBackground.copy(alpha = 0.7f))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
        ) {
          Column {
            Text("Streak", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(
              text = "${insights.currentStreak}d",
              color = Color(0xFFFF9100),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "रिकॉर्ड: ${insights.longestStreak}d",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 10.sp
            )
          }
        }
      }

      // Weekly Momentum / Trend
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (insights.weeklyScoreTrend >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
              contentDescription = null,
              tint = if (insights.weeklyScoreTrend >= 0) CyanNeon else Color(0xFFFF5252),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "साप्ताहिक गति (Weekly Momentum)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = if (insights.weeklyScoreTrend >= 0)
                  "पिछले सप्ताह की तुलना में +${insights.weeklyScoreTrend}% का सकारात्मक सुधार"
                else
                  "पिछले सप्ताह की तुलना में ${insights.weeklyScoreTrend}% का अंतर",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp
              )
            }
          }

          Text(
            text = "${if (insights.weeklyScoreTrend >= 0) "+" else ""}${insights.weeklyScoreTrend}%",
            color = if (insights.weeklyScoreTrend >= 0) CyanNeon else Color(0xFFFF5252),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Frequently Missed Activities (अक्सर छूटने वाली गतिविधियाँ)
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = null,
              tint = Color(0xFFFFB300),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "अक्सर छूटने वाले कार्य (Frequently Missed)",
              color = Color.White,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Text(
            text = "शीर्ष 5",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
          )
        }

        if (insights.mostMissedActivities.isNotEmpty()) {
          insights.mostMissedActivities.forEach { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground.copy(alpha = 0.5f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = item.activityName,
                  color = Color.White.copy(alpha = 0.9f),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
                Text(
                  text = item.category,
                  color = Color.White.copy(alpha = 0.5f),
                  fontSize = 10.sp
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "${item.missedCount} बार छूटा",
                  color = Color(0xFFFF5252),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        } else {
          Text(
            text = "अद्भुत! आपके पास अभी कोई बार-बार छूटने वाली गतिविधि नहीं है।",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }
      }

      // Strongest Category vs Needs Attention
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(10.dp)
        ) {
          Column {
            Text("मजबूत श्रेणी (Strongest)", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(insights.bestCategory, color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(10.dp)
        ) {
          Column {
            Text("ध्यान आवश्यक (Attention)", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
            Text(insights.needsAttentionCategory, color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
