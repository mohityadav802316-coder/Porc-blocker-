package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.MeditationType
import com.example.data.model.TaskStatus
import com.example.ui.components.PersonalInsightsCard
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
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
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils
import kotlin.math.roundToInt

@Composable
fun ReportScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val overallStats by viewModel.overallStats.collectAsState()
  val cycleDayAveragesSql by viewModel.cycleDayAverages.collectAsState()
  val categoryAggregatesSql by viewModel.categoryAggregates.collectAsState()
  val dailySnapshots by viewModel.dailySnapshots.collectAsState()
  val streakInfo by viewModel.streakInfo.collectAsState()
  val allTasks by viewModel.allTasks.collectAsState()
  val allDailyNotes by viewModel.allDailyNotes.collectAsState()
  val smartReminderMinutes by viewModel.smartReminderMinutes.collectAsState()
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val routineTemplates by viewModel.routineTemplates.collectAsState()
  val allMeditationSessions by viewModel.allMeditationSessions.collectAsState()
  val enablePersonalInsights by viewModel.enablePersonalInsights.collectAsState()
  val personalInsights by viewModel.personalInsightsData.collectAsState()

  // Feature 5 & 6: Priority breakdown
  val priorityMetrics = remember(allTasks) {
    listOf(
      Triple("HIGH", "⚡ High Priority", Color(0xFFFF5252)),
      Triple("IMPORTANT", "⭐ Important", Color(0xFFFFD54F)),
      Triple("NORMAL", "Normal Routine", CyanNeon)
    ).map { (prio, label, color) ->
      val matching = allTasks.filter { it.priority.equals(prio, ignoreCase = true) }
      val total = matching.size
      val done = matching.count { it.status == TaskStatus.COMPLETE }
      val pct = if (total > 0) ((done.toFloat() / total) * 100).roundToInt() else 0
      PriorityMetricData(prio, label, total, done, pct, color)
    }
  }

  // Feature 5: Best & Most Missed Routine Activities
  val activityStats = remember(allTasks) {
    allTasks.filter { !it.isExtra }
      .groupBy { it.name }
      .map { (name, tasks) ->
        val total = tasks.size
        val completed = tasks.count { it.status == TaskStatus.COMPLETE }
        val missed = tasks.count { it.status == TaskStatus.MISSED }
        val completionRate = if (total > 0) ((completed.toFloat() / total) * 100).roundToInt() else 0
        ActivityPerfData(name, total, completed, missed, completionRate)
      }
  }

  val bestActivities = remember(activityStats) {
    activityStats.filter { it.total >= 1 }.sortedByDescending { it.completionRate }.take(3)
  }

  val mostMissedActivities = remember(activityStats) {
    activityStats.filter { it.total >= 1 && it.missed > 0 }.sortedByDescending { it.missed }.take(3)
  }

  // Feature 5: Mood distribution & reflections
  val moodDistribution = remember(allDailyNotes) {
    listOf(
      "GREAT" to ("😊" to "Great"),
      "GOOD" to ("🙂" to "Good"),
      "NORMAL" to ("😐" to "Normal"),
      "LOW" to ("😕" to "Low"),
      "BAD" to ("😞" to "Bad")
    ).map { (code, pair) ->
      val count = allDailyNotes.count { it.mood.equals(code, ignoreCase = true) }
      MoodStatData(code, pair.first, pair.second, count)
    }
  }

  val recentReflections = remember(allDailyNotes) {
    allDailyNotes.filter { it.note.isNotBlank() }.sortedByDescending { it.date }.take(4)
  }

  // Daily performance breakdown directly from SQLite DailySnapshots table
  val dailySummaries = remember(dailySnapshots) {
    dailySnapshots.map { snap ->
      DailyPerf(
        date = snap.date,
        cycleDay = snap.dayOfCycle,
        totalTasks = snap.totalTasks,
        completed = snap.completedCount,
        partial = snap.partialCount,
        missed = snap.missedCount,
        score = snap.totalScore,
        percentage = snap.completionPercentage
      )
    }.sortedBy { it.date }
  }

  val totalDays = overallStats.totalDays
  val totalTasks = overallStats.totalTasks
  val totalComplete = overallStats.completedTasks
  val totalPartial = overallStats.partialTasks
  val totalMissed = overallStats.missedTasks
  val totalScore = overallStats.totalScore
  val overallAdherence = if (totalTasks > 0) ((totalScore / totalTasks.toFloat()) * 100f).roundToInt() else 0

  // Category adherence breakdown directly from SQL query
  val categoryMetrics = remember(categoryAggregatesSql) {
    categoryAggregatesSql.map { agg ->
      val score = agg.completedCount + (agg.partialCount * 0.5f)
      val rate = if (agg.totalCount > 0) ((score / agg.totalCount.toFloat()) * 100f).roundToInt() else 0
      CategoryMetric(agg.category, agg.totalCount, rate)
    }.sortedByDescending { it.adherenceRate }
  }

  // 7-day Cycle Performance Breakdown directly from SQL GROUP BY query
  val cycleDayAverages = remember(cycleDayAveragesSql) {
    (1..7).map { cycleDay ->
      val match = cycleDayAveragesSql.firstOrNull { it.dayOfCycle == cycleDay }
      val avgPct = match?.avgPercentage?.roundToInt() ?: 0
      val count = match?.dayCount ?: 0
      CycleDayMetric(cycleDay, avgPct, count)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.navigateBack() },
          modifier = Modifier.size(38.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "REPORTS & ANALYTICS • रिपोर्ट",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
          Text(
            text = "Adherence & Long-Term Trends",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
          )
        }
      }
    }

    // Feature 4: Personal Insights (Data-driven, Factual)
    if (enablePersonalInsights) {
      item {
        PersonalInsightsCard(insights = personalInsights)
      }
    }

    // 1. KPI Cards Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        LuxuryKpiCard(
          title = "Overall Adherence",
          value = "$overallAdherence%",
          subtitle = "All historical days",
          accentColor = CyanNeon,
          icon = Icons.Default.Insights,
          modifier = Modifier.weight(1f)
        )
        LuxuryKpiCard(
          title = "Active History",
          value = "$totalDays days",
          subtitle = "Recorded logs",
          accentColor = VioletNeon,
          icon = Icons.Default.DateRange,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        LuxuryKpiCard(
          title = "Completed Tasks",
          value = totalComplete.toString(),
          subtitle = "Full completion (1.0)",
          accentColor = StatusComplete,
          icon = Icons.Default.CheckCircle,
          modifier = Modifier.weight(1f)
        )
        LuxuryKpiCard(
          title = "Total Scored",
          value = String.format(java.util.Locale.US, "%.1f", totalScore),
          subtitle = "Points accumulated",
          accentColor = VioletGlow,
          icon = Icons.Default.AutoGraph,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // 2. Status Ratio Multi-Segment Breakdown
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Status Distribution", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("$totalTasks total tasks", color = TextSecondary, fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Multi-color segmented progress bar
          if (totalTasks > 0) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
            ) {
              val completeWeight = (totalComplete.toFloat() / totalTasks).coerceAtLeast(0.01f)
              val partialWeight = (totalPartial.toFloat() / totalTasks).coerceAtLeast(0.01f)
              val missedWeight = (totalMissed.toFloat() / totalTasks).coerceAtLeast(0.01f)

              Box(modifier = Modifier.weight(completeWeight).fillMaxSize().background(StatusComplete))
              Box(modifier = Modifier.weight(partialWeight).fillMaxSize().background(StatusPartial))
              Box(modifier = Modifier.weight(missedWeight).fillMaxSize().background(StatusMissed))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("🟢 Complete: $totalComplete (${if (totalTasks > 0) (totalComplete * 100 / totalTasks) else 0}%)", color = StatusComplete, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text("🟡 Partial: $totalPartial", color = StatusPartial, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text("🔴 Missed: $totalMissed", color = StatusMissed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    // 3. Daily Adherence Trend Canvas Chart (Recent Days)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("daily_trend_chart")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Daily Performance Trend",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Recent history adherence",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }

            // Target Badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(StatusComplete.copy(alpha = 0.15f))
                .border(1.dp, StatusComplete.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "70% TARGET",
                color = StatusComplete,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          val recentDays = dailySummaries.takeLast(10)
          if (recentDays.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No historical records logged yet.", color = TextMuted, fontSize = 12.sp)
            }
          } else {
            Canvas(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
            ) {
              val width = size.width
              val height = size.height
              val barCount = recentDays.size
              val barSpacing = 10.dp.toPx()
              val availableWidth = width - (barSpacing * (barCount - 1))
              val barWidth = (availableWidth / barCount).coerceAtMost(36.dp.toPx())

              // 70% Target benchmark line
              val benchmarkY = height * (1f - 0.70f)
              drawLine(
                color = StatusComplete.copy(alpha = 0.35f),
                start = Offset(0f, benchmarkY),
                end = Offset(width, benchmarkY),
                strokeWidth = 1.dp.toPx()
              )

              recentDays.forEachIndexed { index, day ->
                val barHeight = (height * (day.percentage / 100f)).coerceAtLeast(4.dp.toPx())
                val x = index * (barWidth + barSpacing)
                val y = height - barHeight

                val barColor = when {
                  day.percentage >= 70 -> Brush.verticalGradient(listOf(CyanNeon, VioletNeon))
                  day.percentage > 0 -> Brush.verticalGradient(listOf(StatusPartial, StatusPartial.copy(alpha = 0.6f)))
                  else -> Brush.verticalGradient(listOf(DarkSurfaceBorder, DarkSurfaceBorder))
                }

                drawRoundRect(
                  brush = barColor,
                  topLeft = Offset(x, y),
                  size = Size(barWidth, barHeight),
                  cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              recentDays.forEach { d ->
                Text(
                  text = TimeUtils.formatDateShort(d.date),
                  color = TextMuted,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
      }
    }

    // 4. Continuous 7-Day Cycle Performance (Day 1..Day 7 averages)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Column {
          Text(
            text = "7-Day Cycle Performance Curve",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Average adherence by cycle position (Day 1 to 7)",
            color = TextSecondary,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          cycleDayAverages.forEach { metric ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Day ${metric.day}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(54.dp)
              )
              LinearProgressIndicator(
                progress = { (metric.averagePercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .weight(1f)
                  .height(7.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = if (metric.averagePercentage >= 70) CyanNeon else VioletNeon,
                trackColor = DarkSurface
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "${metric.averagePercentage}%",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
              )
            }
          }
        }
      }
    }

    // 5. Category Adherence
    if (categoryMetrics.isNotEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassGradient)
            .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Column {
            Text(
              text = "Category Adherence Breakdown",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            categoryMetrics.forEach { cat ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(cat.category, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${cat.adherenceRate}% (${cat.totalTasks})", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  LinearProgressIndicator(
                    progress = { (cat.adherenceRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                    color = CyanNeon,
                    trackColor = DarkSurface
                  )
                }
              }
            }
          }
        }
      }
    }

    // 6. Feature 1 & 5: Streak Consistency Summary Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("report_streak_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Streak & Consistency Summary", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(streakInfo.todayStatus, color = if (streakInfo.isMaintainedToday) Color(0xFF81C784) else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column {
                Text("CURRENT STREAK", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("🔥 ${streakInfo.currentStreak} Days", color = Color(0xFFFFB74D), fontSize = 18.sp, fontWeight = FontWeight.Black)
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column {
                Text("LONGEST STREAK", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("🏆 ${streakInfo.longestStreak} Days", color = VioletNeon, fontSize = 18.sp, fontWeight = FontWeight.Black)
              }
            }
          }
        }
      }
    }

    // 7. Feature 5 & 6: Priority Tasks Adherence Breakdown
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("report_priority_card")
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Priority Tasks Breakdown", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(12.dp))

          priorityMetrics.forEach { metric ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(metric.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  Text("${metric.done}/${metric.total} (${metric.pct}%)", color = metric.color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                  progress = { (metric.pct / 100f).coerceIn(0f, 1f) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                  color = metric.color,
                  trackColor = DarkSurface
                )
              }
            }
          }
        }
      }
    }

    // 8. Feature 5: Best Performing vs Most Missed Activities
    if (bestActivities.isNotEmpty() || mostMissedActivities.isNotEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassGradient)
            .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Column {
            Text("Routine Activities Performance", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (bestActivities.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ThumbUp, contentDescription = null, tint = StatusComplete, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("MOST CONSISTENT ACTIVITIES", color = StatusComplete, fontSize = 11.sp, fontWeight = FontWeight.Black)
              }
              Spacer(modifier = Modifier.height(6.dp))
              bestActivities.forEach { act ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(act.name, color = TextPrimary, fontSize = 12.sp)
                  Text("${act.completionRate}% (${act.completed}/${act.total})", color = StatusComplete, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
              Spacer(modifier = Modifier.height(10.dp))
            }

            if (mostMissedActivities.isNotEmpty()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ThumbDown, contentDescription = null, tint = StatusMissed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("MOST MISSED ACTIVITIES", color = StatusMissed, fontSize = 11.sp, fontWeight = FontWeight.Black)
              }
              Spacer(modifier = Modifier.height(6.dp))
              mostMissedActivities.forEach { act ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(act.name, color = TextPrimary, fontSize = 12.sp)
                  Text("${act.missed} times missed", color = StatusMissed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    // 9. Feature 2 & 5: Smart Reminder & Alarms Usage
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Alarm & Smart Reminder Usage", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("Exact Alarm Engine", color = TextSecondary, fontSize = 11.sp)
              Text(if (isAlarmEnabled) "Active • Survives Reboot" else "Disabled in Settings", color = if (isAlarmEnabled) CyanNeon else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Pre-Reminder", color = TextSecondary, fontSize = 11.sp)
              Text(
                if (smartReminderMinutes > 0) "$smartReminderMinutes min early alert" else "Off",
                color = if (smartReminderMinutes > 0) CyanNeon else TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 10. Feature 3 & 5: Mood Summary & Recent Reflections
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("report_mood_card")
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mood, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Weekly Mood & Reflections", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Mood pips row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            moodDistribution.forEach { stat ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(DarkSurfaceElevated)
                  .padding(horizontal = 8.dp, vertical = 6.dp)
              ) {
                Text(stat.emoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(stat.label, color = TextSecondary, fontSize = 10.sp)
                Text("${stat.count}d", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          if (recentReflections.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.EditNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("RECENT DAILY REFLECTIONS", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            recentReflections.forEach { note ->
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(DarkSurface)
                  .padding(8.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(TimeUtils.formatDateDisplay(note.date), color = VioletNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  if (!note.mood.isNullOrBlank()) {
                    Text(note.mood, color = TextSecondary, fontSize = 10.sp)
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(note.note, color = TextPrimary, fontSize = 11.sp)
              }
              Spacer(modifier = Modifier.height(6.dp))
            }
          }
        }
      }
    }

    // 10. Feature: Weekly Meditation & Mindfulness Analytics
    item {
      WeeklyMeditationReportCard(
        sessions = allMeditationSessions,
        onOpenSanctuary = { viewModel.setTab(MainTab.MEDITATION) }
      )
    }
  }
}

@Composable
private fun WeeklyMeditationReportCard(
  sessions: List<MeditationSessionEntity>,
  onOpenSanctuary: () -> Unit
) {
  val totalSessions = sessions.size
  val completedSessions = sessions.count { it.isCompleted }
  val totalMinutes = sessions.sumOf { it.completedSeconds } / 60
  val completionRate = if (totalSessions > 0) ((completedSessions.toFloat() / totalSessions) * 100).roundToInt() else 0

  val mostPracticedType = sessions.groupBy { it.type }
    .maxByOrNull { it.value.size }?.key?.let { MeditationType.fromId(it).englishTitle } ?: "Breathing"

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(GlassGradient)
      .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
      .padding(18.dp)
      .testTag("report_meditation_analytics_card")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("Meditation & Mindfulness Report", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("ध्यान एवं मनःशांति विश्लेषण", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Medium)
          }
        }
        Text(
          text = "Sanctuary ↗",
          color = CyanNeon,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.clickable { onOpenSanctuary() }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("TOTAL TIME", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
          Spacer(modifier = Modifier.height(3.dp))
          Text("${totalMinutes}m", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Black)
          Text("mindful minutes", color = TextSecondary, fontSize = 9.sp)
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("SESSIONS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
          Spacer(modifier = Modifier.height(3.dp))
          Text("$totalSessions", color = VioletNeon, fontSize = 16.sp, fontWeight = FontWeight.Black)
          Text("$completedSessions completed", color = StatusComplete, fontSize = 9.sp)
        }

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(10.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("TOP MODE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Black)
          Spacer(modifier = Modifier.height(3.dp))
          Text(mostPracticedType, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("$completionRate% completion", color = TextSecondary, fontSize = 9.sp)
        }
      }

      if (sessions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Spa, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("RECENT SESSIONS LOG", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(8.dp))

        sessions.take(5).forEach { session ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val medType = MeditationType.fromId(session.type)
            Column {
              Text(
                text = "${medType.hindiTitle} (${medType.englishTitle})",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "${TimeUtils.formatDateDisplay(session.date)} • ${session.completedSeconds / 60}m ${session.completedSeconds % 60}s",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (session.isCompleted) StatusComplete.copy(alpha = 0.15f) else StatusPartial.copy(alpha = 0.15f)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = if (session.isCompleted) "Completed" else "Partial",
                color = if (session.isCompleted) StatusComplete else StatusPartial,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LuxuryKpiCard(
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(GlassGradient)
      .border(1.dp, CardBorderGradient, RoundedCornerShape(18.dp))
      .padding(14.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = value, color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
    }
  }
}

private data class DailyPerf(
  val date: String,
  val cycleDay: Int,
  val totalTasks: Int,
  val completed: Int,
  val partial: Int,
  val missed: Int,
  val score: Float,
  val percentage: Int
)

private data class CategoryMetric(
  val category: String,
  val totalTasks: Int,
  val adherenceRate: Int
)

private data class CycleDayMetric(
  val day: Int,
  val averagePercentage: Int,
  val samplesCount: Int
)

private data class PriorityMetricData(
  val priority: String,
  val label: String,
  val total: Int,
  val done: Int,
  val pct: Int,
  val color: Color
)

private data class ActivityPerfData(
  val name: String,
  val total: Int,
  val completed: Int,
  val missed: Int,
  val completionRate: Int
)

private data class MoodStatData(
  val code: String,
  val emoji: String,
  val label: String,
  val count: Int
)

