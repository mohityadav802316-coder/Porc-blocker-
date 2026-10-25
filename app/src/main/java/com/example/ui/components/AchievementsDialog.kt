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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Whatshot
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
import com.example.data.model.StreakInfo
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon

data class BadgeInfo(
  val title: String,
  val desc: String,
  val iconEmoji: String,
  val isUnlocked: Boolean
)

@Composable
fun AchievementsDialog(
  streakInfo: StreakInfo,
  totalTasksCompleted: Int,
  totalMeditationMinutes: Int,
  onDismiss: () -> Unit
) {
  val badges = listOf(
    BadgeInfo("Consistency Initiate", "Complete a 3-day active streak", "🔥", streakInfo.currentStreak >= 3 || streakInfo.longestStreak >= 3),
    BadgeInfo("Momentum Builder", "Reach a 7-day continuous streak", "⚡", streakInfo.currentStreak >= 7 || streakInfo.longestStreak >= 7),
    BadgeInfo("Mindful Monk", "Meditate for 30+ total minutes", "🧘", totalMeditationMinutes >= 30),
    BadgeInfo("Century Achiever", "Complete 100+ lifetime tasks", "🏆", totalTasksCompleted >= 100)
  )

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        .padding(20.dp)
        .testTag("achievements_dialog")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFB300),
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Achievements & Streaks",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Discipline & Consistency Milestones",
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

        // Stats Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${streakInfo.currentStreak} Days",
                color = Color(0xFFFF6D00),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "Current Streak",
                color = TextMuted,
                fontSize = 10.sp
              )
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${streakInfo.longestStreak} Days",
                color = Color(0xFFFFB300),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "Best Streak",
                color = TextMuted,
                fontSize = 10.sp
              )
            }
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$totalTasksCompleted",
                color = CyanNeon,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "Done Tasks",
                color = TextMuted,
                fontSize = 10.sp
              )
            }
          }
        }

        // Badges List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Milestone Badges",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )

          badges.forEach { badge ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (badge.isUnlocked) DarkSurface else DarkSurface.copy(alpha = 0.5f))
                .border(
                  1.dp,
                  if (badge.isUnlocked) StatusComplete.copy(alpha = 0.4f) else DarkSurfaceBorder,
                  RoundedCornerShape(14.dp)
                )
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = badge.iconEmoji, fontSize = 22.sp)
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = badge.title,
                      color = if (badge.isUnlocked) TextPrimary else TextMuted,
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = badge.desc,
                      color = TextMuted,
                      fontSize = 11.sp
                    )
                  }
                }

                if (badge.isUnlocked) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(StatusComplete.copy(alpha = 0.15f))
                      .padding(horizontal = 8.dp, vertical = 3.dp)
                  ) {
                    Text("UNLOCKED", color = StatusComplete, fontSize = 9.sp, fontWeight = FontWeight.Black)
                  }
                } else {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = TextMuted.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.15f)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Done", color = CyanNeon, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
