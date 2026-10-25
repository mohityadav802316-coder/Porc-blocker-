package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShortAppType
import com.example.shortcontent.AccessibilityHelper
import com.example.shortcontent.AccessibilityHealthStatus
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
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import kotlin.math.roundToInt

@Composable
fun ShortContentTrackerScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val todaySummary by viewModel.todayShortContentSummary.collectAsState()
  val allSummaries by viewModel.allShortContentSummaries.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()

  var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityHelper.isAccessibilityServiceEnabled(context)) }
  var selectedPeriodTab by remember { mutableIntStateOf(0) } // 0: Today, 1: Weekly, 2: Monthly

  // Period stats
  val periodStats = remember(allSummaries, selectedPeriodTab) {
    when (selectedPeriodTab) {
      1 -> viewModel.getShortContentPeriodStats(allSummaries.take(7))
      2 -> viewModel.getShortContentPeriodStats(allSummaries.take(30))
      else -> viewModel.getShortContentPeriodStats(listOfNotNull(todaySummary))
    }
  }

  var healthStatus by remember {
    mutableStateOf(AccessibilityHelper.getServiceHealthStatus(context))
  }
  val isServiceConnected by viewModel.shortTrackerManager.isServiceConnected.collectAsState()

  var showPinDialog by remember { mutableStateOf(false) }
  var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var pendingActionTitle by remember { mutableStateOf("") }

  fun executeWithPin(actionTitle: String, action: () -> Unit) {
    if (com.example.focus.AccountabilityPinManager.isPinSet(context) &&
      !com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)
    ) {
      pendingPinAction = action
      pendingActionTitle = actionTitle
      showPinDialog = true
    } else {
      action()
    }
  }

  // Refresh accessibility status on resume & service state change
  LaunchedEffect(Unit, isServiceConnected) {
    healthStatus = AccessibilityHelper.getServiceHealthStatus(context)
    isAccessibilityEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    viewModel.shortTrackerManager.loadTodaySummary()
  }

  // Lifecycle observer to re-check exact runtime health whenever user returns to screen
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
  androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        healthStatus = AccessibilityHelper.getServiceHealthStatus(context)
        isAccessibilityEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(context)
        viewModel.shortTrackerManager.loadTodaySummary()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  BackHandler {
    viewModel.navigateTo(MainTab.TODAY)
  }

  val dailyLimit = userSettings?.shortDailyLimit ?: 20
  val totalCount = todaySummary?.totalCount ?: 0
  val remaining = (dailyLimit - totalCount).coerceAtLeast(0)
  val totalMinutes = ((todaySummary?.totalTimeSeconds ?: 0L) / 60).toInt()

  val progress = if (dailyLimit > 0) (totalCount.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f
  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = tween(700, easing = FastOutSlowInEasing),
    label = "short_content_screen_progress"
  )

  val statusColor = when {
    totalCount >= dailyLimit -> Color(0xFFFF5252)
    totalCount >= (dailyLimit * 0.8f).toInt() -> Color(0xFFFF9800)
    totalCount >= (dailyLimit * 0.5f).toInt() -> Color(0xFFFFD54F)
    else -> CyanNeon
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("short_content_tracker_screen"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Top Bar Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = { viewModel.navigateTo(MainTab.TODAY) },
              modifier = Modifier.size(36.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(
                text = "Short Content Tracker",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
              )
              Text(
                text = "सख्त मोड • रील्स व शॉर्ट्स सीमा",
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          IconButton(
            onClick = {
              isAccessibilityEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(context)
              viewModel.shortTrackerManager.loadTodaySummary()
              Toast.makeText(context, "डेटा रिफ्रेश हुआ", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(36.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
          }
        }
      }

      // 2. Accessibility Permission & Health Banner
      item {
        when {
          healthStatus == AccessibilityHealthStatus.ACTIVE && (userSettings?.shortTrackingEnabled ?: true) -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StatusComplete.copy(alpha = 0.12f))
                .border(1.dp, StatusComplete.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("accessibility_active_banner")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusComplete, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "🟢 सख्त ट्रैकिंग सक्रिय (Strict Tracking Active) • 100% सुरक्षित व ऑफलाइन",
                  color = StatusComplete,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
          healthStatus == AccessibilityHealthStatus.NEEDS_RESTART -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFF8F00).copy(alpha = 0.15f))
                .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .padding(14.dp)
                .testTag("accessibility_recovering_banner")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "🟡 सर्विस पुनः प्रारंभ आवश्यक (Service Interrupted by OS)",
                    color = Color(0xFFFFE082),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Text(
                  text = "Android सिस्टम ने बैकग्राउंड मेमोरी बचाने के लिए एक्सेसिबिलिटी सेवा को रोक दिया है। सेटिंग्स में जाकर Life Tracker सेवा को एक बार बंद करके पुनः चालू करें।",
                  color = TextMuted,
                  fontSize = 11.sp,
                  lineHeight = 16.sp
                )
                Button(
                  onClick = { AccessibilityHelper.openAccessibilitySettings(context) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300)),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("पुनः चालू करें (Open Settings to Rebind)", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
          else -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF5D4037).copy(alpha = 0.35f))
                .border(1.dp, Color(0xFFFFB74D).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(14.dp)
                .testTag("accessibility_warning_banner")
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "🔴 एक्सेसिबिलिटी परमिशन आवश्यक है",
                    color = Color(0xFFFFE0B2),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Text(
                  text = "Instagram Reels और YouTube Shorts की सटीक गिनती के लिए लाइफ ट्रैकर की एक्सेसिबिलिटी सेवा चालू करें। कोई भी निजी संदेश या पासवर्ड नहीं पढ़ा जाता।",
                  color = TextMuted,
                  fontSize = 11.sp,
                  lineHeight = 16.sp
                )

                Button(
                  onClick = { AccessibilityHelper.openAccessibilitySettings(context) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB74D)),
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                  Icon(Icons.Default.Settings, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("सेटिंग्स खोलें (Open Settings)", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // 3. Hero Card: Today Total Count & Progress
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DarkSurfaceElevated)
            .border(
              1.dp,
              if (totalCount >= dailyLimit) Color(0xFFFF5252).copy(alpha = 0.6f) else DarkSurfaceBorder,
              RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
            .testTag("short_content_hero_card")
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    .background(statusColor.copy(alpha = 0.18f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.SmartDisplay,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = "TODAY'S USAGE • आज का उपयोग",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  )
                  Text(
                    text = if (totalCount >= dailyLimit) "दैनिक सीमा समाप्त (Limit Reached)" else "$remaining शॉर्ट्स शेष हैं",
                    color = if (totalCount >= dailyLimit) Color(0xFFFF5252) else CyanNeon,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }

              // Time Spent Badge
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "$totalMinutes मिनट",
                  color = TextPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Big Numbers
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$totalCount",
                color = statusColor,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "/ $dailyLimit Shorts Limit",
                color = TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Progress Bar with checkpoints (50%, 80%, 100%)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(8.dp)
                  .clip(CircleShape),
                color = statusColor,
                trackColor = DarkSurface
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("0%", color = TextMuted, fontSize = 9.sp)
                Text("50% चेतावनी", color = TextMuted, fontSize = 9.sp)
                Text("80% अलर्ट", color = TextMuted, fontSize = 9.sp)
                Text("100% लॉक", color = if (totalCount >= dailyLimit) Color(0xFFFF5252) else TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 4. App Breakdown Cards (Instagram, YouTube, Facebook)
      item {
        Text(
          text = "PLATFORM BREAKDOWN • ऐप अनुसार विवरण",
          color = TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Instagram Reels
          val igCount = todaySummary?.instagramCount ?: 0
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, Color(0xFFE1306C).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Instagram", color = Color(0xFFE1306C), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("रील्स", color = TextMuted, fontSize = 9.sp)
              Text("$igCount", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
          }

          // YouTube Shorts
          val ytCount = todaySummary?.youtubeCount ?: 0
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, Color(0xFFFF0000).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("YouTube", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("शॉर्ट्स", color = TextMuted, fontSize = 9.sp)
              Text("$ytCount", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
          }

          // Facebook Reels
          val fbCount = todaySummary?.facebookCount ?: 0
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, Color(0xFF1877F2).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Facebook", color = Color(0xFF42A5F5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("रील्स", color = TextMuted, fontSize = 9.sp)
              Text("$fbCount", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }

      // 5. Time Period Stats (Today, Weekly, Monthly)
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("PERIOD ANALYTICS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

              // Period Switcher
              Row(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(DarkSurface)
                  .padding(2.dp)
              ) {
                listOf("आज", "7 दिन", "30 दिन").forEachIndexed { index, title ->
                  val isSelected = selectedPeriodTab == index
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color.Transparent)
                      .clickable { selectedPeriodTab = index }
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = title,
                      color = if (isSelected) CyanNeon else TextSecondary,
                      fontSize = 10.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  }
                }
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("कुल शॉर्ट्स (Total)", color = TextMuted, fontSize = 10.sp)
                Text("${periodStats.totalCount}", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              }
              Column {
                Text("कुल समय (Time)", color = TextMuted, fontSize = 10.sp)
                Text("${periodStats.totalTimeMinutes}m", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              }
              Column {
                Text("दैनिक औसत (Avg)", color = TextMuted, fontSize = 10.sp)
                Text("%.1f / दिन".format(periodStats.dailyAverageCount), color = CyanNeon, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              }
              Column {
                Text("सीमा उल्लंघन", color = TextMuted, fontSize = 10.sp)
                Text("${periodStats.limitExceededDays} दिन", color = if (periodStats.limitExceededDays > 0) Color(0xFFFF5252) else TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      // 6. Limits & Discipline Settings
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("LIMITS & DISCIPLINE SETTINGS", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            // Daily Limit Slider
            var tempLimit by remember(dailyLimit) { mutableStateOf(dailyLimit.toFloat()) }
            Column {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("दैनिक शॉर्ट्स सीमा (Daily Limit)", color = TextSecondary, fontSize = 12.sp)
                Text("${tempLimit.roundToInt()} शॉर्ट्स", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
              }
              Slider(
                value = tempLimit,
                onValueChange = { tempLimit = it },
                onValueChangeFinished = {
                  val newLim = tempLimit.roundToInt()
                  if (newLim > dailyLimit) {
                    executeWithPin("दैनिक शॉर्ट्स सीमा बढ़ाना ($newLim)") {
                      viewModel.updateShortContentSettings(
                        limit = newLim,
                        w50 = userSettings?.shortWarning50Enabled ?: true,
                        w80 = userSettings?.shortWarning80Enabled ?: true,
                        w100 = userSettings?.shortWarning100Enabled ?: true,
                        focusLock = userSettings?.shortFocusLockIntegration ?: true,
                        enabled = userSettings?.shortTrackingEnabled ?: true,
                        pinVerified = true
                      )
                      Toast.makeText(context, "दैनिक सीमा सेट की गई: $newLim", Toast.LENGTH_SHORT).show()
                    }
                  } else {
                    viewModel.updateShortContentSettings(
                      limit = newLim,
                      w50 = userSettings?.shortWarning50Enabled ?: true,
                      w80 = userSettings?.shortWarning80Enabled ?: true,
                      w100 = userSettings?.shortWarning100Enabled ?: true,
                      focusLock = userSettings?.shortFocusLockIntegration ?: true,
                      enabled = userSettings?.shortTrackingEnabled ?: true
                    )
                    Toast.makeText(context, "दैनिक सीमा सेट की गई: $newLim", Toast.LENGTH_SHORT).show()
                  }
                },
                valueRange = 5f..100f,
                steps = 18,
                colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon, inactiveTrackColor = DarkSurface)
              )
            }

            // Warning toggles
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("50% चेतावनी (Halfway Notice)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("आधा कोटा पूरा होने पर सूचना", color = TextMuted, fontSize = 10.sp)
              }
              Switch(
                checked = userSettings?.shortWarning50Enabled ?: true,
                onCheckedChange = { checked ->
                  if (checked) {
                    viewModel.updateShortContentSettings(
                      limit = dailyLimit,
                      w50 = true,
                      w80 = userSettings?.shortWarning80Enabled ?: true,
                      w100 = userSettings?.shortWarning100Enabled ?: true,
                      focusLock = userSettings?.shortFocusLockIntegration ?: true,
                      enabled = userSettings?.shortTrackingEnabled ?: true
                    )
                  } else {
                    executeWithPin("50% चेतावनी बंद करना") {
                      viewModel.updateShortContentSettings(
                        limit = dailyLimit,
                        w50 = false,
                        w80 = userSettings?.shortWarning80Enabled ?: true,
                        w100 = userSettings?.shortWarning100Enabled ?: true,
                        focusLock = userSettings?.shortFocusLockIntegration ?: true,
                        enabled = userSettings?.shortTrackingEnabled ?: true,
                        pinVerified = true
                      )
                    }
                  }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon, checkedThumbColor = DarkBackground)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("80% गंभीर चेतावनी (Urgent Alert)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("कोटा समाप्त होने के निकट अलर्ट", color = TextMuted, fontSize = 10.sp)
              }
              Switch(
                checked = userSettings?.shortWarning80Enabled ?: true,
                onCheckedChange = { checked ->
                  if (checked) {
                    viewModel.updateShortContentSettings(
                      limit = dailyLimit,
                      w50 = userSettings?.shortWarning50Enabled ?: true,
                      w80 = true,
                      w100 = userSettings?.shortWarning100Enabled ?: true,
                      focusLock = userSettings?.shortFocusLockIntegration ?: true,
                      enabled = userSettings?.shortTrackingEnabled ?: true
                    )
                  } else {
                    executeWithPin("80% गंभीर चेतावनी बंद करना") {
                      viewModel.updateShortContentSettings(
                        limit = dailyLimit,
                        w50 = userSettings?.shortWarning50Enabled ?: true,
                        w80 = false,
                        w100 = userSettings?.shortWarning100Enabled ?: true,
                        focusLock = userSettings?.shortFocusLockIntegration ?: true,
                        enabled = userSettings?.shortTrackingEnabled ?: true,
                        pinVerified = true
                      )
                    }
                  }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon, checkedThumbColor = DarkBackground)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("100% सीमा समाप्त अलर्ट (Limit Reached)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("सीमा पार होने पर कड़ा संदेश", color = TextMuted, fontSize = 10.sp)
              }
              Switch(
                checked = userSettings?.shortWarning100Enabled ?: true,
                onCheckedChange = { checked ->
                  if (checked) {
                    viewModel.updateShortContentSettings(
                      limit = dailyLimit,
                      w50 = userSettings?.shortWarning50Enabled ?: true,
                      w80 = userSettings?.shortWarning80Enabled ?: true,
                      w100 = true,
                      focusLock = userSettings?.shortFocusLockIntegration ?: true,
                      enabled = userSettings?.shortTrackingEnabled ?: true
                    )
                  } else {
                    executeWithPin("100% सीमा समाप्त अलर्ट बंद करना") {
                      viewModel.updateShortContentSettings(
                        limit = dailyLimit,
                        w50 = userSettings?.shortWarning50Enabled ?: true,
                        w80 = userSettings?.shortWarning80Enabled ?: true,
                        w100 = false,
                        focusLock = userSettings?.shortFocusLockIntegration ?: true,
                        enabled = userSettings?.shortTrackingEnabled ?: true,
                        pinVerified = true
                      )
                    }
                  }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon, checkedThumbColor = DarkBackground)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Focus Lock / डिजिटल अनुशासन से जोड़ें", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("सीमा समाप्त होने पर लाइफ ट्रैकर पर लौटने का आग्रह", color = TextMuted, fontSize = 10.sp)
              }
              Switch(
                checked = userSettings?.shortFocusLockIntegration ?: true,
                onCheckedChange = { checked ->
                  if (checked) {
                    viewModel.updateShortContentSettings(
                      limit = dailyLimit,
                      w50 = userSettings?.shortWarning50Enabled ?: true,
                      w80 = userSettings?.shortWarning80Enabled ?: true,
                      w100 = userSettings?.shortWarning100Enabled ?: true,
                      focusLock = true,
                      enabled = userSettings?.shortTrackingEnabled ?: true
                    )
                  } else {
                    executeWithPin("Focus Lock अनुशासन हटाना") {
                      viewModel.updateShortContentSettings(
                        limit = dailyLimit,
                        w50 = userSettings?.shortWarning50Enabled ?: true,
                        w80 = userSettings?.shortWarning80Enabled ?: true,
                        w100 = userSettings?.shortWarning100Enabled ?: true,
                        focusLock = false,
                        enabled = userSettings?.shortTrackingEnabled ?: true,
                        pinVerified = true
                      )
                    }
                  }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = CyanNeon, checkedThumbColor = DarkBackground)
              )
            }

            // Reset Today
            OutlinedButton(
              onClick = {
                executeWithPin("आज का शॉर्ट काउंट रीसेट करना") {
                  viewModel.resetShortContentToday(pinVerified = true)
                  Toast.makeText(context, "आज का शॉर्ट काउंट रीसेट हुआ", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("आज का काउंट रीसेट करें (Reset Today)", fontSize = 12.sp)
            }
          }
        }
      }

      // 7. Privacy & Security Assurance Note
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface.copy(alpha = 0.5f))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Security, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("गोपनीयता सुरक्षा गारंटी (100% Offline & Private)", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(
                "कोई भी व्यक्तिगत संदेश, चैट, पासवर्ड या इनपुट टेक्स्ट कभी भी नहीं पढ़ा जाता। एक्सेसिबिलिटी सेवा केवल रील्स स्क्रीन की उपस्थिति और समय की गणना स्थानीय रूप से करती है।",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    }
  }

  if (showPinDialog) {
    com.example.ui.components.AccountabilityPinDialog(
      onDismiss = {
        showPinDialog = false
        pendingPinAction = null
      },
      onActionApproved = {
        pendingPinAction?.invoke()
        showPinDialog = false
        pendingPinAction = null
      },
      mode = com.example.ui.components.AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
      actionTitle = pendingActionTitle
    )
  }
}
