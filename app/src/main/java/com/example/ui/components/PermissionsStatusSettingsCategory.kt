package com.example.ui.components

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.shortcontent.AccessibilityHelper
import com.example.shortcontent.ShortContentAccessibilityService
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.SageGreen
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.CrashReporter

@Composable
fun PermissionsStatusSettingsCategory() {
  val context = LocalContext.current
  var refreshTrigger by remember { mutableIntStateOf(0) }

  // State trackers
  var hasExactAlarm by remember { mutableStateOf(false) }
  var hasNotification by remember { mutableStateOf(false) }
  var hasOverlay by remember { mutableStateOf(false) }
  var hasAccessibility by remember { mutableStateOf(false) }
  var hasBatteryExemption by remember { mutableStateOf(false) }
  var hasAudioStorage by remember { mutableStateOf(false) }

  var crashLogText by remember { mutableStateOf<String?>(null) }
  var showFullLog by remember { mutableStateOf(false) }

  // Check all statuses
  fun checkStatuses() {
    // 1. Exact Alarm
    hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      alarmManager?.canScheduleExactAlarms() ?: true
    } else {
      true
    }

    // 2. Notifications
    hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }

    // 3. Overlay
    hasOverlay = Settings.canDrawOverlays(context)

    // 4. Accessibility
    hasAccessibility = ShortContentAccessibilityService.isServiceRunning() ||
      AccessibilityHelper.isAccessibilityServiceEnabled(context)

    // 5. Battery Optimization
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    hasBatteryExemption = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false

    // 6. Audio / Storage
    hasAudioStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    // Crash Log
    crashLogText = CrashReporter.getLastCrash(context)
  }

  LaunchedEffect(refreshTrigger) {
    checkStatuses()
  }

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotification = isGranted
    refreshTrigger++
  }

  val audioLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasAudioStorage = isGranted
    refreshTrigger++
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Overview Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(ObsidianCard)
        .border(1.dp, GoldBrass.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(IceBlue.copy(alpha = 0.15f))
            .border(1.dp, IceBlue.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = IceBlue,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "SYSTEM PERMISSIONS & HEALTH",
            color = IceBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "अनुमतियां एवं सिस्टम स्वास्थ्य",
            color = TextPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "अलार्म, ओवरले, फोकस मोड और ब्लैक स्क्रीन की विश्वसनीयता के लिए अनुमतियां जांचें।",
            color = TextMuted,
            fontSize = 12.sp
          )
        }
        OutlinedButton(
          onClick = {
            checkStatuses()
            refreshTrigger++
            Toast.makeText(context, "स्थिति रीफ्रेश की गई", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.height(36.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp), tint = IceBlue)
        }
      }
    }

    // 1. EXACT ALARM PERMISSION
    PermissionItemCard(
      title = "सटीक अलार्म (Exact Alarms)",
      subtitle = "5:00 AM अलार्म और समय सारणी टाइमर को बिना मिस हुए समय पर बजाने के लिए",
      icon = Icons.Default.Alarm,
      accentColor = GoldHighlight,
      isGranted = hasExactAlarm,
      onGrantClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
              data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
          } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
          }
        } else {
          Toast.makeText(context, "इस Android संस्करण पर स्वचालित रूप से सक्षम है", Toast.LENGTH_SHORT).show()
        }
      }
    )

    // 2. NOTIFICATIONS PERMISSION
    PermissionItemCard(
      title = "सूचनाएं (Notifications)",
      subtitle = "स्मार्ट प्री-रिमाइंडर, दैनिक टास्क प्रोग्रेस, म्यूजिक और मेडिटेशन स्टेटस",
      icon = Icons.Default.Notifications,
      accentColor = CyanNeon,
      isGranted = hasNotification,
      onGrantClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
          Toast.makeText(context, "सूचनाएं अनुमत हैं", Toast.LENGTH_SHORT).show()
        }
      }
    )

    // 3. DISPLAY OVER OTHER APPS (SYSTEM OVERLAY)
    PermissionItemCard(
      title = "अन्य ऐप्स के ऊपर प्रदर्शन (Overlay)",
      subtitle = "OLED ब्लैक स्क्रीन फ्लोटिंग डॉट, फुल ब्लैक डिस्प्ले और फोकस मोड ब्लॉकिंग के लिए",
      icon = Icons.Default.Layers,
      accentColor = IceBlue,
      isGranted = hasOverlay,
      onGrantClick = {
        try {
          val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
          )
          context.startActivity(intent)
        } catch (_: Exception) {
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
          }
          context.startActivity(intent)
        }
      }
    )

    // 4. ACCESSIBILITY SERVICE
    PermissionItemCard(
      title = "एक्सेसिबिलिटी सेवा (Accessibility)",
      subtitle = "Reels/Shorts काउंटिंग, दैनिक वीडियो सीमा और फोकस मोड ऐप ब्लॉकिंग के लिए",
      icon = Icons.Default.Security,
      accentColor = DustyRose,
      isGranted = hasAccessibility,
      onGrantClick = {
        try {
          val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
          context.startActivity(intent)
        } catch (e: Exception) {
          Toast.makeText(context, "सेटिंग्स खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
        }
      }
    )

    // 5. BATTERY OPTIMIZATION EXEMPTION
    PermissionItemCard(
      title = "बैटरी ऑप्टिमाइज़ेशन छूट (No Battery Kill)",
      subtitle = "अलार्म, दैनिक स्वचालित बैकअप और ब्लैक स्क्रीन को बैकग्राउंड में बिना रुकावट चलाने के लिए",
      icon = Icons.Default.BatteryChargingFull,
      accentColor = SageGreen,
      isGranted = hasBatteryExemption,
      onGrantClick = {
        try {
          val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
          }
          context.startActivity(intent)
        } catch (_: Exception) {
          try {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
          } catch (e: Exception) {
            Toast.makeText(context, "बैटरी सेटिंग्स खोलें", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )

    // 6. AUDIO & STORAGE ACCESS
    PermissionItemCard(
      title = "म्यूजिक / ऑडियो स्टोरेज एक्सेस",
      subtitle = "डिवाइस के स्थानीय गाने स्कैन करने और कस्टम अलार्म टोन चलाने के लिए",
      icon = Icons.Default.MusicNote,
      accentColor = GoldBrass,
      isGranted = hasAudioStorage,
      onGrantClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          audioLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
          audioLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      }
    )

    // 7. AUTO-BACKUP FOLDER STORAGE
    val isBackupFolderAccessible = remember(refreshTrigger) { com.example.backup.BackupPreferences.isFolderAccessible(context) }
    val backupFolderDisplayName = remember(refreshTrigger) { com.example.backup.BackupPreferences.getFolderDisplayName(context) }
    PermissionItemCard(
      title = "स्वचालित बैकअप फ़ोल्डर (SAF Folder)",
      subtitle = if (isBackupFolderAccessible)
        "सक्रिय: ${backupFolderDisplayName ?: "Documents"} (अनइंस्टॉल के बाद भी सुरक्षित)"
      else
        "अपरिभाषित! अनइंस्टॉल के बाद डेटा खो सकता है। कृपया Documents में फ़ोल्डर चुनें",
      icon = Icons.Default.Folder,
      accentColor = if (isBackupFolderAccessible) SageGreen else DustyRose,
      isGranted = isBackupFolderAccessible,
      onGrantClick = {
        // Prompt to select folder in Settings > Data & Backup
        Toast.makeText(context, "कृपया सेटिंग्स > डेटा एवं बैकअप में जाकर फ़ोल्डर चुनें", Toast.LENGTH_LONG).show()
      }
    )

    Spacer(modifier = Modifier.height(10.dp))

    // =========================================================================
    // SAFETY NET: CRASH TELEMETRY & DIAGNOSTICS
    // =========================================================================
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .background(ObsidianCard)
        .border(1.dp, if (crashLogText != null) StatusMissed.copy(alpha = 0.5f) else ObsidianBorder, RoundedCornerShape(20.dp))
        .padding(18.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(if (crashLogText != null) StatusMissed.copy(alpha = 0.15f) else StatusComplete.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (crashLogText != null) Icons.Default.BugReport else Icons.Default.CheckCircle,
              contentDescription = null,
              tint = if (crashLogText != null) StatusMissed else StatusComplete,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "CRASH DIAGNOSTIC & SAFETY NET",
              color = if (crashLogText != null) StatusMissed else StatusComplete,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = if (crashLogText != null) "अंतिम क्रैश लॉग उपलब्ध है" else "सिस्टम सुरक्षित है (कोई क्रैश नहीं)",
              color = TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (crashLogText != null) {
          val log = crashLogText ?: ""
          Text(
            text = "ऐप में पिछली अनपेक्षित त्रुटि का विस्तृत स्टैक ट्रेस सुरक्षित रूप से रिकॉर्ड किया गया है। समस्या सुलझाने के लिए कॉपी करें।",
            color = TextSecondary,
            fontSize = 12.sp
          )

          // Crash preview box
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Text(
              text = if (showFullLog) log else log.take(350) + if (log.length > 350) "\n... [पूर्ण लॉग देखने के लिए टैप करें]" else "",
              color = TextMuted,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.clickable { showFullLog = !showFullLog }
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val ok = CrashReporter.copyCrashToClipboard(context)
                if (ok) {
                  Toast.makeText(context, "क्रैश रिपोर्ट क्लिपबोर्ड पर कॉपी हो गई!", Toast.LENGTH_LONG).show()
                } else {
                  Toast.makeText(context, "कॉपी करने में विफल", Toast.LENGTH_SHORT).show()
                }
              },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Copy Last Error", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = {
                CrashReporter.clearCrash(context)
                crashLogText = null
                Toast.makeText(context, "क्रैश लॉग साफ किया गया", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Clear Log", color = TextSecondary, fontSize = 12.sp)
            }
          }
        } else {
          Text(
            text = "ग्लोबल अनकॉट एक्सेप्शन हैंडलर सक्रिय है। यदि कभी भी कोई त्रुटि होती है, तो उसका स्टैक ट्रेस यहां तुरंत दिखाई देगा जिसे आप एक टैप में कॉपी कर सकते हैं।",
            color = TextMuted,
            fontSize = 12.sp
          )
        }
      }
    }
  }
}

@Composable
private fun PermissionItemCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  isGranted: Boolean,
  onGrantClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(ObsidianCard)
      .border(1.dp, if (isGranted) StatusComplete.copy(alpha = 0.3f) else ObsidianBorder, RoundedCornerShape(16.dp))
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(if (isGranted) StatusComplete.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isGranted) StatusComplete else accentColor,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          // Status Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isGranted) StatusComplete.copy(alpha = 0.15f) else StatusMissed.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = if (isGranted) "सक्रिय (Granted)" else "अनुमति आवश्यक",
              color = if (isGranted) StatusComplete else StatusMissed,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      if (!isGranted) {
        Button(
          onClick = onGrantClick,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor),
          shape = RoundedCornerShape(10.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.height(34.dp)
        ) {
          Text(
            text = "अनुमति दें",
            color = Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      } else {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Granted",
          tint = StatusComplete,
          modifier = Modifier.size(24.dp)
        )
      }
    }
  }
}
