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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PersonalInsightsData
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

@Composable
fun PersonalInsightsDialog(
  insights: PersonalInsightsData,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, VioletNeon.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        .padding(20.dp)
        .testTag("personal_insights_dialog")
    ) {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(VioletNeon.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = VioletNeon,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Personal Insights",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Data-driven behavioral analytics",
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        // Consistency Score & Mindfulness
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurface)
              .padding(12.dp)
          ) {
            Column {
              Text(
                text = "Consistency",
                color = TextMuted,
                fontSize = 11.sp
              )
              Text(
                text = "${insights.routineConsistencyPercentage}%",
                color = if (insights.routineConsistencyPercentage >= 70) StatusComplete else CyanNeon,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "Trend: ${if (insights.weeklyScoreTrend >= 0) "+${insights.weeklyScoreTrend}%" else "${insights.weeklyScoreTrend}%"}",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurface)
              .padding(12.dp)
          ) {
            Column {
              Text(
                text = "Top Mode",
                color = TextMuted,
                fontSize = 11.sp
              )
              Text(
                text = insights.topMeditationMode,
                color = Color(0xFFFFB300),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${insights.totalMindfulMinutes} total mins",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }
          }
        }

        // Top Category Performance
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Strongest Domain", color = TextMuted, fontSize = 11.sp)
              Text(insights.bestCategory.ifBlank { "Routine Work" }, color = StatusComplete, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Needs Attention", color = TextMuted, fontSize = 11.sp)
              Text(insights.needsAttentionCategory.ifBlank { "Physical Fitness" }, color = StatusMissed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Most Missed Activities
        if (insights.mostMissedActivities.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Areas to Guard (Frequent Misses)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            insights.mostMissedActivities.take(3).forEach { stat ->
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(DarkSurface)
                  .padding(horizontal = 12.dp, vertical = 8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(stat.activityName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                  Text("Missed ${stat.missedCount} times", color = StatusMissed, fontSize = 11.sp)
                }
              }
            }
          }
        }

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = VioletNeon.copy(alpha = 0.2f)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Done", color = VioletNeon, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
