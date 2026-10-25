package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimelineDayRecord
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils

@Composable
fun TimelineScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val timelineRecords by viewModel.timelineRecords.collectAsState()
  var searchQuery by remember { mutableStateOf("") }
  var filterOnlyAchievements by remember { mutableStateOf(false) }

  val filteredRecords = timelineRecords.filter { record ->
    val matchesSearch = searchQuery.isBlank() ||
      record.date.contains(searchQuery, ignoreCase = true) ||
      (record.note?.contains(searchQuery, ignoreCase = true) == true) ||
      record.priorityTasksCompleted.any { it.contains(searchQuery, ignoreCase = true) }

    val matchesAchievement = !filterOnlyAchievements || record.achievements.isNotEmpty()
    matchesSearch && matchesAchievement
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .testTag("life_timeline_screen")
  ) {
    // Header Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { viewModel.navigateBack() },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Today",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "⏳ Life Timeline",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${timelineRecords.size} कुल रिकॉर्डेड दिन",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
          )
        }
      }

      // Achievement filter toggle
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(if (filterOnlyAchievements) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
          .border(1.dp, if (filterOnlyAchievements) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(12.dp))
          .clickable { filterOnlyAchievements = !filterOnlyAchievements }
          .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = if (filterOnlyAchievements) CyanNeon else Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "उपलब्धियाँ",
            color = if (filterOnlyAchievements) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = if (filterOnlyAchievements) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("तारीख, नोट या मुख्य कार्य खोजें...", fontSize = 12.sp) },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = CyanNeon,
        unfocusedBorderColor = DarkSurfaceBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
      ),
      shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredRecords.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "कोई टाइमलाइन रिकॉर्ड नहीं मिला",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
      ) {
        items(filteredRecords, key = { it.date }) { record ->
          TimelineItemRow(
            record = record,
            onSelectDate = {
              viewModel.selectDate(record.date)
              viewModel.setTab(MainTab.TODAY)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun TimelineItemRow(
  record: TimelineDayRecord,
  onSelectDate: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(IntrinsicSize.Min)
  ) {
    // Spine column (Node + Connecting Line)
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.width(36.dp)
    ) {
      // Node Dot
      Box(
        modifier = Modifier
          .size(16.dp)
          .clip(CircleShape)
          .background(
            if (record.completionPercentage >= 80) CyanNeon
            else if (record.completionPercentage >= 50) Color(0xFFFFB300)
            else Color.White.copy(alpha = 0.3f)
          )
          .border(2.dp, DarkBackground, CircleShape)
      )
      // Connecting Line
      Box(
        modifier = Modifier
          .width(2.dp)
          .fillMaxHeight()
          .background(
            Brush.verticalGradient(
              listOf(
                CyanNeon.copy(alpha = 0.4f),
                DarkSurfaceBorder
              )
            )
          )
      )
    }

    // Content Card
    Card(
      modifier = Modifier
        .weight(1f)
        .padding(bottom = 16.dp)
        .clickable(onClick = onSelectDate),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
      border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Date & Score Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = record.date,
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            Text(
              text = "Cycle Day ${record.dayOfCycle}",
              color = Color.White.copy(alpha = 0.5f),
              fontSize = 10.sp
            )
          }

          // Score Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(
                when {
                  record.completionPercentage >= 80 -> CyanNeon.copy(alpha = 0.15f)
                  record.completionPercentage >= 50 -> Color(0xFFFFB300).copy(alpha = 0.15f)
                  else -> Color.White.copy(alpha = 0.08f)
                }
              )
              .border(
                1.dp,
                when {
                  record.completionPercentage >= 80 -> CyanNeon.copy(alpha = 0.4f)
                  record.completionPercentage >= 50 -> Color(0xFFFFB300).copy(alpha = 0.4f)
                  else -> DarkSurfaceBorder
                },
                RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = "${record.completionPercentage}% Score",
              color = when {
                record.completionPercentage >= 80 -> CyanNeon
                record.completionPercentage >= 50 -> Color(0xFFFFB300)
                else -> Color.White.copy(alpha = 0.7f)
              },
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Mood & Meditation Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (record.mood != null) {
            val moodEmoji = when (record.mood) {
              "GREAT" -> "🤩 Great"
              "GOOD" -> "🙂 Good"
              "NORMAL" -> "😐 Normal"
              "LOW" -> "😔 Low"
              "BAD" -> "😫 Bad"
              else -> record.mood
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(text = "Mood: $moodEmoji", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            }
          }

          if (record.meditationMinutes > 0) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(VioletNeon.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(text = "${record.meditationMinutes}m Mindful", color = VioletNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }

          Text(
            text = "${record.completedTasks}/${record.totalTasks} Tasks",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
          )
        }

        // Daily Reflection Note Quote
        if (!record.note.isNullOrBlank()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(DarkBackground.copy(alpha = 0.6f))
              .padding(8.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.FormatQuote,
              contentDescription = null,
              tint = CyanNeon.copy(alpha = 0.6f),
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = record.note,
              color = Color.White.copy(alpha = 0.85f),
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }

        // Important Tasks Completed
        if (record.priorityTasksCompleted.isNotEmpty()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = record.priorityTasksCompleted.joinToString(", "),
              color = Color.White.copy(alpha = 0.8f),
              fontSize = 11.sp
            )
          }
        }

        // Achievements Pills
        if (record.achievements.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            record.achievements.forEach { ach ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFFFB300).copy(alpha = 0.15f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = ach,
                  color = Color(0xFFFFB300),
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
}
