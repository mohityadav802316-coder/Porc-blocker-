package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskStatus
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
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val selectedDate by viewModel.selectedDate.collectAsState()
  val allTasks by viewModel.allTasks.collectAsState()
  val dailyNote by viewModel.dailyNoteForSelectedDate.collectAsState()
  val selectedDateTasks by viewModel.tasksForSelectedDate.collectAsState()
  val summary by viewModel.summaryForSelectedDate.collectAsState()

  // Track the currently displayed year & month
  var calendarMonthOffset by remember { mutableStateOf(0) }

  val calendar = remember(calendarMonthOffset) {
    Calendar.getInstance().apply {
      add(Calendar.MONTH, calendarMonthOffset)
      set(Calendar.DAY_OF_MONTH, 1)
    }
  }

  val monthName = remember(calendar) {
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
  }

  // Precompute completion stats per date for fast calendar rendering
  val tasksByDate = remember(allTasks) {
    allTasks.groupBy { it.date }
  }

  val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
  // Calendar.DAY_OF_WEEK: Sunday is 1, Monday is 2
  val firstDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0, Sunday = 6
  val year = calendar.get(Calendar.YEAR)
  val month = calendar.get(Calendar.MONTH) + 1

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
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
            text = "CALENDAR • कैलेंडर",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "Cycle & Month Matrix",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    item {
      // Header Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(24.dp))
          .padding(18.dp)
          .testTag("calendar_header_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "CALENDAR VIEW / कैलेंडर",
                color = CyanNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
            }

            // Month navigation buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = { calendarMonthOffset-- },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Previous Month",
                  tint = TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
              IconButton(
                onClick = { calendarMonthOffset++ },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Next Month",
                  tint = TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = monthName,
              color = TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Black
            )

            if (calendarMonthOffset != 0) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(DarkSurfaceElevated)
                  .clickable { calendarMonthOffset = 0 }
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = "Current Month",
                  color = CyanNeon,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Days of week row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
          ) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
              Text(
                text = day,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Calendar Grid
          val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
          val numRows = totalCells / 7

          for (row in 0 until numRows) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
              horizontalArrangement = Arrangement.SpaceAround
            ) {
              for (col in 0..6) {
                val cellIndex = row * 7 + col
                val dayNum = cellIndex - firstDayOfWeek + 1

                if (dayNum in 1..daysInMonth) {
                  val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)
                  val isSelected = dateKey == selectedDate
                  val isToday = dateKey == viewModel.todayDate

                  // Calculate stats for day
                  val dayTasks = tasksByDate[dateKey].orEmpty()
                  val hasTasks = dayTasks.isNotEmpty()
                  val completedCount = dayTasks.count { it.status == TaskStatus.COMPLETE }
                  val totalCount = dayTasks.size
                  val completionRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                  val indicatorColor = when {
                    !hasTasks -> null
                    completionRatio >= 0.8f -> StatusComplete
                    completionRatio > 0.2f -> StatusPartial
                    else -> StatusMissed
                  }

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(44.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(
                        when {
                          isSelected -> CyanNeon.copy(alpha = 0.22f)
                          isToday -> VioletNeon.copy(alpha = 0.15f)
                          else -> Color.Transparent
                        }
                      )
                      .border(
                        1.dp,
                        when {
                          isSelected -> CyanNeon
                          isToday -> VioletNeon
                          else -> Color.Transparent
                        },
                        RoundedCornerShape(10.dp)
                      )
                      .clickable {
                        viewModel.selectDate(dateKey)
                      }
                      .testTag("cal_day_$dayNum"),
                    contentAlignment = Alignment.Center
                  ) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.Center
                    ) {
                      Text(
                        text = "$dayNum",
                        color = when {
                          isSelected -> CyanNeon
                          isToday -> VioletNeon
                          else -> TextPrimary
                        },
                        fontSize = 12.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Normal
                      )

                      if (indicatorColor != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                          modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(indicatorColor)
                        )
                      } else {
                        Spacer(modifier = Modifier.height(7.dp))
                      }
                    }
                  }
                } else {
                  // Empty spacer cell
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Legend
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
          ) {
            LegendItem(StatusComplete, "Completed (≥80%)")
            LegendItem(StatusPartial, "Partial")
            LegendItem(StatusMissed, "Missed")
          }
        }
      }
    }

    // Selected Date Details Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(16.dp)
          .testTag("selected_date_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = TimeUtils.formatDateDisplay(selectedDate),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${summary.completedCount}/${summary.totalTasks} Tasks Done (${summary.completionPercentage}%)",
                color = if (summary.completionPercentage >= 50) StatusComplete else TextSecondary,
                fontSize = 12.sp
              )
            }

            Button(
              onClick = {
                viewModel.setTab(MainTab.TODAY)
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = CyanNeon,
                contentColor = Color(0xFF00363D)
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .height(38.dp)
                .testTag("open_timeline_button")
            ) {
              Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Open Timeline", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          // Daily Note / Reflection snippet if available
          if (dailyNote != null && (!dailyNote?.note.isNullOrBlank() || !dailyNote?.mood.isNullOrBlank())) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .padding(10.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                val moodEmoji = when (dailyNote?.mood?.uppercase()) {
                  "GREAT" -> "😊"
                  "GOOD" -> "🙂"
                  "NORMAL" -> "😐"
                  "LOW" -> "😕"
                  "BAD" -> "😞"
                  else -> "📝"
                }
                Text(text = moodEmoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Daily Reflection (${dailyNote?.mood ?: "Note"})",
                    color = VioletNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                  if (!dailyNote?.note.isNullOrBlank()) {
                    Text(
                      text = dailyNote?.note ?: "",
                      color = TextPrimary,
                      fontSize = 12.sp
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Task list header for selected date
    item {
      Text(
        text = "ACTIVITIES ON THIS DATE (${selectedDateTasks.size})",
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp
      )
    }

    if (selectedDateTasks.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "इस तारीख के लिए कोई गतिविधि दर्ज नहीं है।",
            color = TextSecondary,
            fontSize = 13.sp
          )
        }
      }
    } else {
      items(selectedDateTasks, key = { it.id }) { task ->
        CalendarTaskRow(task = task)
      }
    }

    item {
      Spacer(modifier = Modifier.height(72.dp))
    }
  }
}

@Composable
private fun LegendItem(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      color = TextMuted,
      fontSize = 10.sp
    )
  }
}

@Composable
private fun CalendarTaskRow(task: DayTaskEntity) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(
            when (task.status) {
              TaskStatus.COMPLETE -> StatusComplete
              TaskStatus.PARTIAL -> StatusPartial
              TaskStatus.MISSED -> StatusMissed
            }
          )
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = task.name,
          color = TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "${TimeUtils.minutesTo12Hour(task.timeMinutes)} • ${task.category}",
          color = TextSecondary,
          fontSize = 11.sp
        )
      }
    }

    val statusText = when (task.status) {
      TaskStatus.COMPLETE -> "Done"
      TaskStatus.PARTIAL -> "Partial"
      TaskStatus.MISSED -> "Missed"
    }
    val statusColor = when (task.status) {
      TaskStatus.COMPLETE -> StatusComplete
      TaskStatus.PARTIAL -> StatusPartial
      TaskStatus.MISSED -> StatusMissed
    }

    Text(
      text = statusText,
      color = statusColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
  }
}
