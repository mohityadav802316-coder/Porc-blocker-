package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.DaySummary
import com.example.data.model.DayTaskEntity
import com.example.data.model.EnergyMode
import com.example.data.model.StreakInfo
import com.example.data.model.TaskStatus
import com.example.music.MusicPlayerState
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

/**
 * Hero Card: Next Activity or Live Activity Tracker.
 */
@Composable
fun DashboardNextActivityCard(
  nextOrCurrentTask: DayTaskEntity?,
  isToday: Boolean,
  nowMinutes: Int,
  onStatusChange: (DayTaskEntity, TaskStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .background(GlassGradient)
      .border(
        1.dp,
        Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.6f), VioletNeon.copy(alpha = 0.4f), DarkSurfaceBorder)),
        RoundedCornerShape(22.dp)
      )
      .padding(18.dp)
      .testTag("next_activity_hero")
  ) {
    if (nextOrCurrentTask != null) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (nextOrCurrentTask.status == TaskStatus.COMPLETE) StatusComplete else CyanNeon)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isToday && nextOrCurrentTask.timeMinutes <= nowMinutes) "CURRENT ACTIVITY • अभी चल रहा है" else "NEXT ACTIVITY • अगला कार्य",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.8.sp
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(DarkSurface)
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = nextOrCurrentTask.category,
              color = TextSecondary,
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Text(
          text = nextOrCurrentTask.name,
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = TimeUtils.minutesTo12Hour(nextOrCurrentTask.timeMinutes),
              color = TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          if (nextOrCurrentTask.status != TaskStatus.COMPLETE) {
            Button(
              onClick = { onStatusChange(nextOrCurrentTask, TaskStatus.COMPLETE) },
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
              shape = RoundedCornerShape(10.dp),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Mark Done", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
            }
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusComplete, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Completed", color = StatusComplete, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    } else {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text("✨ All Routine Done for Today!", color = StatusComplete, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Take a moment to breathe and reflect.", color = TextMuted, fontSize = 12.sp)
      }
    }
  }
}

/**
 * Daily Progress Card showing completion percentage and progress bar.
 */
@Composable
fun DashboardDailyProgressCard(
  summary: DaySummary,
  streakInfo: StreakInfo,
  modifier: Modifier = Modifier
) {
  val animatedProgress by animateFloatAsState(
    targetValue = (summary.completionPercentage / 100f).coerceIn(0f, 1f),
    animationSpec = tween(700, easing = FastOutSlowInEasing),
    label = "home_progress"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
      .padding(14.dp)
      .testTag("dashboard_daily_progress_card")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Daily Progress • दैनिक प्रगति", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("🔥", fontSize = 12.sp)
          Spacer(modifier = Modifier.width(2.dp))
          Text("${streakInfo.currentStreak}d Streak", color = Color(0xFFFF6D00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "${summary.completionPercentage}%",
          color = if (summary.completionPercentage >= 75) StatusComplete else CyanNeon,
          fontSize = 26.sp,
          fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "${summary.completedCount}/${summary.totalTasks} Tasks Done",
          color = TextMuted,
          fontSize = 12.sp
        )
      }

      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth()
          .height(7.dp)
          .clip(CircleShape),
        color = if (summary.completionPercentage >= 75) StatusComplete else CyanNeon,
        trackColor = DarkSurface
      )
    }
  }
}

/**
 * Top Priority Task Card.
 */
@Composable
fun DashboardPriorityTaskCard(
  priorityTask: DayTaskEntity?,
  onStatusChange: (DayTaskEntity, TaskStatus) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
      .padding(14.dp)
      .testTag("dashboard_priority_task_card")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Priority Task • प्राथमिकता कार्य", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      if (priorityTask != null) {
        Text(
          text = priorityTask.name,
          color = TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = TimeUtils.minutesTo12Hour(priorityTask.timeMinutes),
            color = TextMuted,
            fontSize = 12.sp
          )
          if (priorityTask.status != TaskStatus.COMPLETE) {
            Button(
              onClick = { onStatusChange(priorityTask, TaskStatus.COMPLETE) },
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(Icons.Default.Check, contentDescription = "Done", tint = DarkBackground, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Done", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusComplete, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Completed", color = StatusComplete, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      } else {
        Text("No priority tasks pending", color = TextMuted, fontSize = 12.sp)
      }
    }
  }
}

/**
 * Streak & Consistency Score Card.
 */
@Composable
fun DashboardStreakCard(
  streakInfo: StreakInfo,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, Color(0xFFFF6D00).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
      .padding(14.dp)
      .testTag("dashboard_streak_card")
  ) {
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
            .background(Color(0xFFFF6D00).copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF6D00), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Daily Streak • निरंतरता",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Best: ${streakInfo.bestStreak}d • Consistency Score: ${streakInfo.consistencyScore.toInt()}%",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
      }
      Text(
        text = "${streakInfo.currentStreak} Days",
        color = Color(0xFFFF6D00),
        fontSize = 16.sp,
        fontWeight = FontWeight.Black
      )
    }
  }
}

/**
 * Energy Mode Card with quick switcher and descriptions.
 */
@Composable
fun DashboardEnergyModeCard(
  dailyEnergyMode: EnergyMode,
  onModeChange: (EnergyMode) -> Unit,
  modifier: Modifier = Modifier
) {
  val energyColor = when (dailyEnergyMode) {
    EnergyMode.LOW -> Color(0xFF4DD0E1)
    EnergyMode.NORMAL -> CyanNeon
    EnergyMode.HIGH -> Color(0xFFFFB300)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, energyColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
      .padding(14.dp)
      .testTag("dashboard_energy_mode_card")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Text(dailyEnergyMode.emoji, fontSize = 24.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Energy Mode: ${dailyEnergyMode.titleHi} (${dailyEnergyMode.titleEn})",
            color = energyColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = dailyEnergyMode.description,
            color = TextMuted,
            fontSize = 10.sp
          )
        }
      }
      Button(
        onClick = {
          val nextMode = when (dailyEnergyMode) {
            EnergyMode.NORMAL -> EnergyMode.HIGH
            EnergyMode.HIGH -> EnergyMode.LOW
            EnergyMode.LOW -> EnergyMode.NORMAL
          }
          onModeChange(nextMode)
        },
        colors = ButtonDefaults.buttonColors(containerColor = energyColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(28.dp)
      ) {
        Text("बदलें", color = energyColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Meditation Sanctuary Shortcut Card.
 */
@Composable
fun DashboardMeditationCard(
  sessionsCount: Int,
  onStartMeditation: () -> Unit,
  onOpenMeditation: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, VioletNeon.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
      .clickable { onOpenMeditation() }
      .padding(14.dp)
      .testTag("home_meditation_shortcut")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(VioletNeon.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.SelfImprovement,
            contentDescription = null,
            tint = VioletNeon,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Meditation Sanctuary • ध्यान साधना",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "$sessionsCount sessions today • Tap to start session",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
      }

      Button(
        onClick = onStartMeditation,
        colors = ButtonDefaults.buttonColors(containerColor = VioletNeon.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("10 Min", color = VioletNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Music / Now Playing Mini Bar Card.
 */
@Composable
fun DashboardMusicCard(
  musicPlayerState: MusicPlayerState,
  onTogglePlayPause: () -> Unit,
  onNextSong: () -> Unit,
  onOpenMusic: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
      .clickable { onOpenMusic() }
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("home_music_mini_bar")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(CyanNeon.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(16.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = musicPlayerState.currentSong?.title ?: "Music System • संगीत",
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (musicPlayerState.currentSong != null) {
              "${musicPlayerState.currentSong.artist} • ${musicPlayerState.currentMood.title}"
            } else {
              "स्थानीय लाइब्रेरी और मूड प्लेयर खोलें"
            },
            color = TextMuted,
            fontSize = 10.sp,
            maxLines = 1
          )
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onTogglePlayPause,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = if (musicPlayerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (musicPlayerState.isPlaying) "Pause" else "Play",
            tint = CyanNeon,
            modifier = Modifier.size(18.dp)
          )
        }
        IconButton(
          onClick = onNextSong,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

/**
 * Daily Challenge Card.
 */
@Composable
fun DashboardDailyChallengeCard(
  challenge: DailyChallengeEntity,
  onComplete: () -> Unit,
  onSkip: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(
        1.dp,
        if (challenge.isCompleted) StatusComplete.copy(alpha = 0.45f) else DarkSurfaceBorder,
        RoundedCornerShape(18.dp)
      )
      .padding(14.dp)
      .testTag("home_daily_challenge_card")
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("🌱", fontSize = 13.sp)
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "DAILY CHALLENGE • आज की चुनौती",
            color = if (challenge.isCompleted) StatusComplete else Color(0xFF81C784),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
        }
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF81C784).copy(alpha = 0.15f))
            .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
          Text(
            text = challenge.category,
            color = Color(0xFF81C784),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Text(
        text = challenge.title,
        color = TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )

      if (challenge.description.isNotBlank()) {
        Text(
          text = challenge.description,
          color = TextMuted,
          fontSize = 11.sp,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (challenge.isCompleted) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = StatusComplete,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "चुनौती पूर्ण हुई (Completed)",
              color = StatusComplete,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        } else {
          TextButton(
            onClick = onSkip,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text("बदलें (Skip)", color = TextMuted, fontSize = 11.sp)
          }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("पूर्ण करें (Done)", color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Next Alarm Status Card.
 */
@Composable
fun DashboardNextAlarmCard(
  isAlarmEnabled: Boolean,
  wakeUpMinutes: Int,
  onOpenAlarm: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, if (isAlarmEnabled) CyanNeon.copy(alpha = 0.35f) else DarkSurfaceBorder, RoundedCornerShape(18.dp))
      .clickable { onOpenAlarm() }
      .padding(14.dp)
      .testTag("dashboard_alarm_card")
  ) {
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
            .background(if (isAlarmEnabled) CyanNeon.copy(alpha = 0.16f) else DarkSurface),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = if (isAlarmEnabled) CyanNeon else TextMuted,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Wake-Up Alarm • सुबह का अलार्म",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (isAlarmEnabled) "सक्रिय • ${TimeUtils.minutesTo12Hour(wakeUpMinutes)}" else "अलार्म बंद है • सेट करने के लिए टैप करें",
            color = if (isAlarmEnabled) CyanNeon else TextMuted,
            fontSize = 11.sp
          )
        }
      }
      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
    }
  }
}

/**
 * Recovery Day Active Banner Card.
 */
@Composable
fun DashboardRecoveryDayCard(
  isRecoveryDayActive: Boolean,
  onRestoreRoutine: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!isRecoveryDayActive) return

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xFF004D40).copy(alpha = 0.35f))
      .border(1.dp, Color(0xFF4DD0E1).copy(alpha = 0.45f), RoundedCornerShape(16.dp))
      .padding(12.dp)
      .testTag("recovery_day_banner")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF4DD0E1).copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Healing,
            contentDescription = null,
            tint = Color(0xFF4DD0E1),
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "🌿 रिकवरी डे सक्रिय (Recovery Day Active)",
            color = Color(0xFFE0F7FA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "हल्का एवं संतुलित शेड्यूल • मुख्य कार्य प्राथमिकता पर",
            color = Color(0xFF80DEEA),
            fontSize = 10.sp
          )
        }
      }
      Button(
        onClick = onRestoreRoutine,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1).copy(alpha = 0.22f)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(28.dp)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF4DD0E1), modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text("सामान्य रूटीन", color = Color(0xFF4DD0E1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Weekly Cycle Summary Card.
 */
@Composable
fun DashboardWeeklySummaryCard(
  currentCycleDay: Int,
  onOpenWeek: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
      .clickable { onOpenWeek() }
      .padding(14.dp)
      .testTag("dashboard_weekly_summary_card")
  ) {
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
            .background(CyanNeon.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Timeline, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Weekly Cycle • 7-दिवसीय चक्र",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Day $currentCycleDay of 7 • साप्ताहिक प्रदर्शन देखने के लिए टैप करें",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
      }
      Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
    }
  }
}

/**
 * Routine Header with date navigation, filters, and add task button.
 */
@Composable
fun DashboardRoutineHeader(
  routineTasksCount: Int,
  isToday: Boolean,
  showFilters: Boolean,
  selectedCategoryFilter: String?,
  categories: List<String>,
  onPrevDay: () -> Unit,
  onNextDay: () -> Unit,
  onSetToday: () -> Unit,
  onToggleFilters: () -> Unit,
  onSelectCategory: (String?) -> Unit,
  onAddTask: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "TODAY'S ROUTINE",
          color = TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyanNeon.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = "$routineTasksCount",
            color = CyanNeon,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = onPrevDay, modifier = Modifier.size(30.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Day", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
        if (!isToday) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(CyanNeon.copy(alpha = 0.12f))
              .clickable { onSetToday() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("Today", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
        IconButton(onClick = onNextDay, modifier = Modifier.size(30.dp)) {
          Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Day", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }

        IconButton(onClick = onToggleFilters, modifier = Modifier.size(30.dp)) {
          Icon(
            Icons.Default.FilterList,
            contentDescription = "Filter",
            tint = if (selectedCategoryFilter != null) CyanNeon else TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }

        Button(
          onClick = onAddTask,
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          modifier = Modifier.height(30.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text("Add", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    AnimatedVisibility(visible = showFilters) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selectedCategoryFilter == null) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
            .clickable { onSelectCategory(null) }
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text("All", color = if (selectedCategoryFilter == null) CyanNeon else TextMuted, fontSize = 11.sp)
        }
        categories.forEach { cat ->
          val isSelected = selectedCategoryFilter == cat
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
              .clickable { onSelectCategory(if (isSelected) null else cat) }
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(cat, color = if (isSelected) CyanNeon else TextMuted, fontSize = 11.sp)
          }
        }
      }
    }
  }
}
