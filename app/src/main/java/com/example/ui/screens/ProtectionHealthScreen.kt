package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.HealthLogEntry
import com.example.focus.HealthPillar
import com.example.focus.ProtectionHealthManager
import com.example.focus.ProtectionHealthReport
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

/**
 * Dedicated "Protection Health" Screen:
 * 1. Live status checklist of all security components with one-tap "ठीक करें" buttons.
 * 2. 7-Day Uptime Percentage & Heartbeat verification.
 * 3. Step-by-step phone setup guide for Motorola (Moto), Xiaomi, Samsung, etc.
 * 4. Local Health Audit Logs (Service started/stopped, Heartbeat missed, Boot, App update).
 * 5. Memory, CPU, and performance diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionHealthScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var report by remember { mutableStateOf(ProtectionHealthManager.getHealthReport(context)) }
  var healthLogs by remember { mutableStateOf(ProtectionHealthManager.getHealthLogs(context)) }
  var selectedTab by remember { mutableIntStateOf(0) } // 0: घटक (Pillars), 1: फोन गाइड (Phone Guide), 2: स्वास्थ्य लॉग (Logs), 3: डायग्नोस्टिक्स (Debug)

  BackHandler {
    onNavigateBack()
  }

  fun refreshAll() {
    ProtectionHealthManager.performWatchdogCheck(context)
    report = ProtectionHealthManager.getHealthReport(context)
    healthLogs = ProtectionHealthManager.getHealthLogs(context)
  }

  LaunchedEffect(Unit) {
    refreshAll()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    // 1. Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onNavigateBack,
          modifier = Modifier.size(38.dp)
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = WarmOffWhite
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "Protection Health",
            color = WarmOffWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "दीर्घकालिक सुरक्षा व स्थिरता केंद्र",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
      }

      IconButton(
        onClick = {
          refreshAll()
          Toast.makeText(context, "स्वास्थ्य स्थिति रीफ्रेश हुई", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.size(38.dp)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldBrass)
      }
    }

    // 2. Health Overview Hero Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .clip(RoundedCornerShape(18.dp))
        .background(if (report.isAllHealthy) Color(0xFF132B1D) else Color(0xFF331B1B))
        .border(
          1.dp,
          if (report.isAllHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
          RoundedCornerShape(18.dp)
        )
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              if (report.isAllHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (report.isAllHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (report.isAllHealthy) "🟢 पूर्ण सुरक्षा सक्रिय" else "🚨 सुरक्षा अधूरी है",
                color = WarmOffWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "${report.healthyCount} / ${report.totalCount} सुरक्षा घटक चालू",
                color = if (report.isAllHealthy) Color(0xFF34D399) else Color(0xFFFCA5A5),
                fontSize = 12.sp
              )
            }
          }

          // Percentage Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (report.isAllHealthy) Color(0xFF064E3B) else Color(0xFF7F1D1D))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "${report.healthPercentage}%",
              color = if (report.isAllHealthy) Color(0xFF34D399) else Color(0xFFF87171),
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        LinearProgressIndicator(
          progress = { report.healthPercentage / 100f },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
          color = if (report.isAllHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
          trackColor = DarkSurface
        )

        // Uptime & Heartbeat Metrics Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "🛡️ 7-Day Uptime: ${report.sevenDayUptimePercentage}%",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          val heartbeatText = if (report.lastHeartbeatSecAgo >= 0) "${report.lastHeartbeatSecAgo}s पहले" else "उपलब्ध नहीं"
          Text(
            text = "💓 Heartbeat: $heartbeatText",
            color = WarmParchment,
            fontSize = 11.sp
          )
        }
      }
    }

    // Active Pause Banner (Requirement 2)
    if (com.example.focus.ProtectionPauseManager.isPaused(context)) {
      val remainingPauseMs = com.example.focus.ProtectionPauseManager.getPauseRemainingMillis(context)
      val mins = (remainingPauseMs / 60000L).coerceAtLeast(1)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF332308))
          .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("⏸️ सुरक्षा अस्थायी रूप से पॉज़ है", color = Color(0xFFFFB74D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("बैंकिंग ऐप्स हेतु पॉज़ सक्रिय • शेष समय: $mins मिनट", color = TextMuted, fontSize = 11.sp)
          }
          Button(
            onClick = {
              com.example.focus.ProtectionPauseManager.cancelPause(context)
              refreshAll()
              Toast.makeText(context, "पॉज़ समाप्त। कृपया सुरक्षा घटक चालू करें।", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("चालू करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 3. Navigation Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = DarkBackground,
      contentColor = GoldBrass,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = GoldBrass
        )
      },
      divider = {}
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("घटक सूची", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("Moto/Phone गाइड", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text("स्वास्थ्य लॉग", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
      )
      Tab(
        selected = selectedTab == 3,
        onClick = { selectedTab = 3 },
        text = { Text("डायग्नोस्टिक्स", fontSize = 12.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) }
      )
    }

    // 4. Tab Content
    when (selectedTab) {
      0 -> PillarsListTab(pillars = report.pillars, onRefresh = { refreshAll() })
      1 -> PhoneSetupGuideTab()
      2 -> HealthLogsTab(logs = healthLogs, onClear = {
        ProtectionHealthManager.clearLogs(context)
        healthLogs = emptyList()
        Toast.makeText(context, "लॉग हटाए गए", Toast.LENGTH_SHORT).show()
      })
      3 -> DiagnosticsTab()
    }
  }
}

/**
 * Tab 0: Pillars Checklist with one-tap fix buttons.
 */
@Composable
private fun PillarsListTab(
  pillars: List<HealthPillar>,
  onRefresh: () -> Unit
) {
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    items(pillars, key = { it.id }) { pillar ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(
            0.8.dp,
            if (pillar.isHealthy) DarkSurfaceBorder else Color(0xFFEF4444).copy(alpha = 0.5f),
            RoundedCornerShape(14.dp)
          )
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              if (pillar.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (pillar.isHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = pillar.title,
                color = WarmOffWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = pillar.subtitle,
                color = if (pillar.isHealthy) TextMuted else Color(0xFFFFB4AB),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          if (!pillar.isHealthy) {
            Button(
              onClick = {
                pillar.fixAction(context)
                onRefresh()
              },
              colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(pillar.actionLabel, color = ObsidianCharcoal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          } else {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text("सक्रिय", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

/**
 * Tab 1: Motorola & OEM specific reliability setup guide.
 */
@Composable
private fun PhoneSetupGuideTab() {
  val context = LocalContext.current

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "फ़ोन-विशिष्ट बैकग्राउंड स्थिरता गाइड",
        color = WarmOffWhite,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Motorola, Xiaomi, Samsung आदि फ़ोनों में सिस्टम आक्रामक रूप से बैकग्राउंड ऐप्स को बंद कर देता है। निर्बाध सुरक्षा हेतु नीचे दिए गए कदम पूरे करें:",
        color = TextMuted,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )
    }

    // Motorola (Moto) Card
    item {
      GuideCard(
        title = "📱 Motorola (Moto फ़ोनों के लिए)",
        steps = listOf(
          "Settings -> Apps -> Life Tracker खोलें।",
          "Battery (बैटरी) विकल्प चुनें और 'Unrestricted' (अप्रतिबंधित) पर सेट करें।",
          "'Pause app activity if unused' विकल्प को बंद (Turn OFF) करें।",
          "Settings -> Battery -> Adaptive Battery में जाएं और Life Tracker को पाबंदी से मुक्त रखें।"
        ),
        onOpenSettings = {
          com.example.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
        }
      )
    }

    // Xiaomi / Redmi / Poco
    item {
      GuideCard(
        title = "📱 Xiaomi / Redmi / Poco (MIUI / HyperOS)",
        steps = listOf(
          "Settings -> Apps -> Manage apps -> Life Tracker में Autostart (ऑटोस्टार्ट) चालू करें।",
          "Battery saver विकल्प में जाकर 'No restrictions' चुनें।",
          "Recent Apps स्क्रीन में Life Tracker कार्ड पर लंबा दबाकर Lock (ताला) लगाएं।"
        ),
        onOpenSettings = {
          com.example.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
        }
      )
    }

    // Samsung
    item {
      GuideCard(
        title = "📱 Samsung (One UI)",
        steps = listOf(
          "Settings -> Battery and device care -> Battery खोलें।",
          "Background usage limits -> 'Never sleeping apps' में Life Tracker जोड़ें।",
          "Settings -> Apps -> Life Tracker -> Battery -> 'Unrestricted' चुनें।"
        ),
        onOpenSettings = {
          com.example.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
        }
      )
    }

    // OnePlus / Oppo / Vivo / Realme
    item {
      GuideCard(
        title = "📱 OnePlus / Oppo / Vivo / Realme",
        steps = listOf(
          "App info -> Battery usage में Allow background activity चालू करें।",
          "Auto-launch (स्वतः आरंभ) की अनुमति दें।",
          "Recent apps में ऐप को लॉक करें।"
        ),
        onOpenSettings = {
          com.example.util.BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
        }
      )
    }

    // Device Owner & Multi-User Hard-Block Guide
    item {
      val pkg = context.packageName
      GuideCard(
        title = "🛡️ Multi-User & Device Owner सेटअप गाइड",
        steps = listOf(
          "Layer 1 (Accessibility): बिना ADB के भी Settings & SystemUI में Multi-user / Guest स्क्रीन को तुरंत ब्लॉक करता है।",
          "Layer 2 (Device Owner Hard-Block): PC से 'adb shell dpm set-device-owner $pkg/com.example.focus.LifeTrackerAdminReceiver' चलाकर सिस्टम-स्तर पर DISALLOW_ADD_USER लगाएं।",
          "Layer 3 (Secure Settings): 'adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS' से यूज़र-स्विचर को 0 पर लॉक करें।"
        ),
        onOpenSettings = {
          com.example.focus.LifeTrackerAdminReceiver.requestAdminPermission(context)
        }
      )
    }
  }
}

@Composable
private fun GuideCard(
  title: String,
  steps: List<String>,
  onOpenSettings: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
      .padding(14.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(title, color = GoldBrass, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Button(
          onClick = onOpenSettings,
          colors = ButtonDefaults.buttonColors(containerColor = ObsidianCard),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text("सेटिंग्स", color = WarmOffWhite, fontSize = 10.sp)
        }
      }

      steps.forEachIndexed { idx, step ->
        Row(modifier = Modifier.fillMaxWidth()) {
          Text("${idx + 1}. ", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text(step, color = WarmOffWhite.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 15.sp)
        }
      }
    }
  }
}

/**
 * Tab 2: Health Audit & Transition Logs.
 */
@Composable
private fun HealthLogsTab(
  logs: List<HealthLogEntry>,
  onClear: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "स्थानीय सुरक्षा व निष्पादन लॉग (${logs.size})",
        color = WarmOffWhite,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      if (logs.isNotEmpty()) {
        OutlinedButton(
          onClick = onClear,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("लॉग साफ़ करें", color = Color(0xFFEF4444), fontSize = 11.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (logs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("कोई लॉग दर्ज नहीं है।", color = TextMuted, fontSize = 12.sp)
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(logs) { entry ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(0.6.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .padding(10.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val badgeColor = when (entry.severity) {
                  "ERROR" -> Color(0xFFEF4444)
                  "WARN" -> Color(0xFFFFB74D)
                  else -> Color(0xFF10B981)
                }
                Text(
                  text = "[${entry.eventType}]",
                  color = badgeColor,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = entry.formattedTime,
                  color = TextMuted,
                  fontSize = 10.sp
                )
              }
              Text(
                text = entry.message,
                color = WarmOffWhite,
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Tab 3: Memory, CPU, and System Diagnostics.
 */
@Composable
private fun DiagnosticsTab() {
  val runtime = Runtime.getRuntime()
  val usedMemMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
  val totalMemMb = runtime.totalMemory() / (1024 * 1024)
  val maxMemMb = runtime.maxMemory() / (1024 * 1024)

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text(
        text = "सिस्टम व मेमोरी डायग्नोस्टिक्स (Memory & Smoothness)",
        color = WarmOffWhite,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
      )
    }

    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("JVM Heap Memory (रैम उपयोग)", color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }

          Text("उपयोग: $usedMemMb MB / कुल आवंटित: $totalMemMb MB (अधिकतम: $maxMemMb MB)", color = TextMuted, fontSize = 11.sp)
          LinearProgressIndicator(
            progress = { if (maxMemMb > 0) usedMemMb.toFloat() / maxMemMb.toFloat() else 0f },
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

    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("मेमोरी व सीपीयू सुरक्षा विशेषताएं", color = GoldBrass, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("• स्क्रीन बंद होने पर AI स्क्रीनशॉट लूप पूरी तरह रुक जाता है (0% बैटरी/सीपीयू खर्च)।", color = WarmOffWhite, fontSize = 11.sp)
          Text("• हर स्क्रीनशॉट Bitmap का हार्डवेयर बफर तुरंत रिलीज़ और recycle किया जाता है (Zero Bitmap Leaks)।", color = WarmOffWhite, fontSize = 11.sp)
          Text("• Accessibility सर्विस में कीबोर्ड व सिस्टम यूआई इवेंट फ़िल्टर किए जाते हैं (Zero Typing Lag)।", color = WarmOffWhite, fontSize = 11.sp)
          Text("• नोड पदानुक्रम गहराई 5 स्तरों और 25 नोड्स तक सीमित है (मुख्य थ्रेड हैन्ग से बचाव)।", color = WarmOffWhite, fontSize = 11.sp)
        }
      }
    }
  }
}
