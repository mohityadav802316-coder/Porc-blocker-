package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.MainTab

data class QuickAccessItem(
  val id: String,
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val iconColor: Color,
  val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAccessBottomSheet(
  onDismiss: () -> Unit,
  onNavigateTab: (MainTab) -> Unit,
  onOpenInsights: () -> Unit,
  onOpenAchievements: () -> Unit,
  onOpenHabits: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val items = listOf(
    QuickAccessItem(
      id = "journal",
      title = "Journal",
      subtitle = "Daily notes & reflection",
      icon = Icons.Default.RateReview,
      iconColor = VioletNeon,
      onClick = {
        onDismiss()
        onNavigateTab(MainTab.REFLECTION)
      }
    ),
    QuickAccessItem(
      id = "calendar",
      title = "Calendar",
      subtitle = "7-Day cycles & month",
      icon = Icons.Default.CalendarMonth,
      iconColor = CyanNeon,
      onClick = {
        onDismiss()
        onNavigateTab(MainTab.CALENDAR)
      }
    ),
    QuickAccessItem(
      id = "weekly_report",
      title = "Weekly Report",
      subtitle = "Cycle archive review",
      icon = Icons.Default.DateRange,
      iconColor = Color(0xFF64B5F6),
      onClick = {
        onDismiss()
        onNavigateTab(MainTab.WEEK)
      }
    ),
    QuickAccessItem(
      id = "monthly_report",
      title = "Monthly Report",
      subtitle = "Adherence & analytics",
      icon = Icons.Default.Insights,
      iconColor = Color(0xFF81C784),
      onClick = {
        onDismiss()
        onNavigateTab(MainTab.REPORT)
      }
    ),
    QuickAccessItem(
      id = "timeline",
      title = "Timeline",
      subtitle = "Chronological life log",
      icon = Icons.Default.History,
      iconColor = Color(0xFFFFB74D),
      onClick = {
        onDismiss()
        onNavigateTab(MainTab.TIMELINE)
      }
    ),
    QuickAccessItem(
      id = "insights",
      title = "Insights",
      subtitle = "Focus & habit trends",
      icon = Icons.Default.Lightbulb,
      iconColor = Color(0xFFFFD54F),
      onClick = {
        onDismiss()
        onOpenInsights()
      }
    ),
    QuickAccessItem(
      id = "achievements",
      title = "Achievements",
      subtitle = "Streak & milestone badges",
      icon = Icons.Default.EmojiEvents,
      iconColor = Color(0xFFFF8A65),
      onClick = {
        onDismiss()
        onOpenAchievements()
      }
    ),
    QuickAccessItem(
      id = "habits",
      title = "Habits & Goals",
      subtitle = "Templates & discipline",
      icon = Icons.Default.Flag,
      iconColor = Color(0xFFBA68C8),
      onClick = {
        onDismiss()
        onOpenHabits()
      }
    ),
    QuickAccessItem(
      id = "music",
      title = "Music & Playlists",
      subtitle = "Local library & mood player",
      icon = Icons.Default.MusicNote,
      iconColor = CyanNeon,
      onClick = {
        onDismiss()
        onNavigateTab(MainTab.MUSIC)
      }
    )
  )

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurfaceElevated,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 12.dp, bottom = 8.dp)
          .width(42.dp)
          .height(4.dp)
          .clip(CircleShape)
          .background(TextMuted.copy(alpha = 0.4f))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "QUICK ACCESS HUB",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "Tools & Reports",
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = TextSecondary
          )
        }
      }

      // Grid of 8 options
      LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(items) { item ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
              .clickable { item.onClick() }
              .padding(14.dp)
              .testTag("quick_access_${item.id}")
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(item.iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = item.icon,
                  contentDescription = item.title,
                  tint = item.iconColor,
                  modifier = Modifier.size(20.dp)
                )
              }

              Column {
                Text(
                  text = item.title,
                  color = TextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = item.subtitle,
                  color = TextMuted,
                  fontSize = 11.sp,
                  maxLines = 1
                )
              }
            }
          }
        }
      }
    }
  }
}
