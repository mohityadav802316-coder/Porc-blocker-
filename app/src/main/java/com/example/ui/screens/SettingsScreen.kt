package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import com.example.ui.components.ResetAppConfirmDialog
import com.example.ui.components.ResetAppWarningDialog
import com.example.ui.components.RestoreBackupsDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmScheduler
import com.example.data.model.EnergyMode
import com.example.data.model.GoalEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.ui.components.ExportFormatTab
import com.example.ui.components.ExportImportDialog
import com.example.ui.components.GoalBottomSheet
import com.example.ui.components.MathChallengeDialog
import com.example.ui.components.RoutineChangePreviewDialog
import com.example.ui.components.RoutineTemplateBottomSheet
import com.example.ui.components.TimePickerDialog12Hour
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MutedPlum
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }

  // Handle hardware back button when inside a category
  BackHandler(enabled = activeCategory != null) {
    activeCategory = null
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    val selectedCategory = activeCategory
    if (selectedCategory == null) {
      // 1. MAIN CATEGORIES MENU
      SettingsCategoryList(
        onSelectCategory = { activeCategory = it },
        onBack = { viewModel.navigateBack() }
      )
    } else {
      // 2. DEDICATED CATEGORY SCREEN
      SettingsCategoryDetail(
        category = selectedCategory,
        viewModel = viewModel,
        onBack = { activeCategory = null }
      )
    }
  }
}

@Composable
private fun SettingsCategoryList(
  onSelectCategory: (SettingsCategory) -> Unit,
  onBack: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
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
          onClick = onBack,
          modifier = Modifier.size(40.dp)
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
            text = "CONTROL CENTER • सेटिंग्स",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "App Settings & Control",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }

    // 8 Categories
    items(SettingsCategory.entries) { category ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
          .clickable { onSelectCategory(category) }
          .padding(16.dp)
          .testTag("settings_cat_${category.name.lowercase()}")
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
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(category.iconColor.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = category.iconColor,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = category.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = category.description,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1
              )
            }
          }

          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsCategoryDetail(
  category: SettingsCategory,
  viewModel: LifeTrackerViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Bar
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Settings Menu",
            tint = TextPrimary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = category.hindiTitle.uppercase(),
            color = category.iconColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = category.title,
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }

    when (category) {
      SettingsCategory.FOCUS_RESTRICTION -> {
        item {
          FocusDisciplineCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.PERMISSIONS_STATUS -> {
        item {
          com.example.ui.components.PermissionsStatusSettingsCategory()
        }
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 1: ALARMS & NOTIFICATIONS
// -------------------------------------------------------------
@Composable
private fun AlarmsAndNotificationsCategory(
  viewModel: LifeTrackerViewModel,
  onOpenAlarmCenter: () -> Unit
) {
  val context = LocalContext.current
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val wakeUpMinutes by viewModel.wakeUpMinutes.collectAsState()
  val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
  val smartReminderMinutes by viewModel.smartReminderMinutes.collectAsState()
  val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Quick link to Dedicated Alarm Center
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(CyanNeon.copy(alpha = 0.12f))
        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        .clickable { onOpenAlarmCenter() }
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Open Full Alarm Center →",
            color = CyanNeon,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Upcoming alarms, tone tests, repeat days & health",
            color = TextPrimary,
            fontSize = 12.sp
          )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanNeon)
      }
    }

    // Master Switch
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Master Alarm System", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(if (isAlarmEnabled) "Alarms active" else "Alarms muted", color = if (isAlarmEnabled) StatusComplete else TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = isAlarmEnabled,
          onCheckedChange = { checked ->
            viewModel.toggleAlarm(context, checked)
            if (checked) AlarmScheduler.rescheduleAllTimetableAlarms(context)
            else {
              AlarmScheduler.cancelAlarm(context)
              AlarmScheduler.cancelAllTimetableAlarms(context)
            }
          }
        )
      }
    }

    // Smart Reminder
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Smart Pre-Reminder", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Pre-routine notification: ${if (smartReminderMinutes > 0) "$smartReminderMinutes min before" else "Off"}", color = TextMuted, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(0 to "Off", 5 to "5m", 10 to "10m", 15 to "15m").forEach { (mins, label) ->
            val isSelected = smartReminderMinutes == mins
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable {
                  viewModel.updateSmartReminderMinutes(mins)
                  AlarmScheduler.rescheduleAllTimetableAlarms(context)
                }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(label, color = if (isSelected) CyanNeon else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Vibration Alert
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Vibration Alert", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Pulsing vibration for alarms", color = TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = isVibrationEnabled,
          onCheckedChange = { viewModel.updateVibrationEnabled(it) }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 2: ROUTINE & SCHEDULE
// -------------------------------------------------------------
@Composable
private fun RoutineAndScheduleCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val routineTemplates by viewModel.routineTemplates.collectAsState()
  val anchorDate by viewModel.anchorDate.collectAsState()
  var editingTemplate by remember { mutableStateOf<RoutineTemplateEntity?>(null) }
  var isAddingTemplate by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 7-Day Anchor Date
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("7-Day Cycle Anchor Date", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Current Anchor: $anchorDate", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("The starting date that defines continuous 7-day cycles", color = TextMuted, fontSize = 11.sp)
      }
    }

    // Routine Templates Management
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Master Routine Activities (${routineTemplates.size})", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
      Button(
        onClick = { isAddingTemplate = true },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    routineTemplates.forEach { template ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(template.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${TimeUtils.minutesTo12Hour(template.timeMinutes)} • ${template.category}", color = TextMuted, fontSize = 11.sp)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { editingTemplate = template }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanNeon, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { viewModel.deleteRoutineTemplate(context, template.id) }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusMissed, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }

  if (isAddingTemplate || editingTemplate != null) {
    RoutineTemplateBottomSheet(
      initialTemplate = editingTemplate,
      onSave = { id, name, mins, cat, notes, days, active ->
        viewModel.saveRoutineTemplate(
          id = id,
          name = name,
          timeMinutes = mins,
          category = cat,
          notes = notes,
          daysMask = days,
          isActive = active
        )
        editingTemplate = null
        isAddingTemplate = false
      },
      onDismiss = {
        editingTemplate = null
        isAddingTemplate = false
      }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 3: MEDITATION
// -------------------------------------------------------------
@Composable
private fun MeditationSettingsCategory(viewModel: LifeTrackerViewModel) {
  val chimeEnabled by viewModel.meditationChimeEnabled.collectAsState()
  val voiceLang by viewModel.meditationVoiceLanguage.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Guided Voice Language (वाणी भाषा)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          listOf("HI" to "हिंदी (Hindi)", "EN" to "English").forEach { (code, label) ->
            val isSelected = voiceLang == code
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) VioletNeon.copy(alpha = 0.18f) else DarkSurface)
                .border(1.dp, if (isSelected) VioletNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable { viewModel.updateMeditationSettings(chimeEnabled, code) }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(label, color = if (isSelected) VioletNeon else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Tibetan Singing Bowl & Bells", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Harmonic start chime & ending bells", color = TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = chimeEnabled,
          onCheckedChange = { viewModel.updateMeditationSettings(it, voiceLang) }
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Offline Audio Support", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("On-device synthesized Tibetan singing bowl and local Android TTS. 100% offline without internet connection.", color = TextMuted, fontSize = 12.sp)
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 4: FOCUS & DISCIPLINE
// -------------------------------------------------------------
@Composable
private fun FocusDisciplineCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()
  val isFocusScheduleEnabled by viewModel.isFocusScheduleEnabled.collectAsState()
  val focusStartTimeMinutes by viewModel.focusStartTimeMinutes.collectAsState()
  val focusEndTimeMinutes by viewModel.focusEndTimeMinutes.collectAsState()
  val enableRecoveryMode by viewModel.enableRecoveryMode.collectAsState()
  val strictLockEvents by viewModel.strictLockEvents.collectAsState()

  val isStrictLock = com.example.focus.FocusModeManager.isStrictLockActive(context)

  var showStartTimePicker by remember { mutableStateOf(false) }
  var showEndTimePicker by remember { mutableStateOf(false) }
  var showFocusExitConfirmationDialog by remember { mutableStateOf(false) }

  var showAdultBlocklistDialog by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var pinDialogMode by remember { mutableStateOf<com.example.ui.components.AccountabilityPinDialogMode?>(null) }
  var pendingPinActionTitle by remember { mutableStateOf("सुरक्षा बदलाव") }
  var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showSecureDnsGuideDialog by remember { mutableStateOf(false) }

  fun executeWithPin(actionTitle: String, action: () -> Unit) {
    if (com.example.focus.AccountabilityPinManager.isPinSet(context) &&
      !com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)
    ) {
      pendingPinActionTitle = actionTitle
      pendingPinAction = action
      pinDialogMode = com.example.ui.components.AccountabilityPinDialogMode.VERIFY_AND_ROTATE
      showPinDialog = true
    } else {
      action()
    }
  }

  var isAiScreenDetectionEnabled by remember {
    mutableStateOf(com.example.focus.AiScreenDetectionManager.isEnabled(context))
  }
  var aiThreshold by remember {
    mutableStateOf(com.example.focus.AiScreenDetectionManager.getThreshold(context))
  }

  val mediaProjectionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
      com.example.focus.AiScreenDetectionManager.startService(context, result.resultCode, result.data!!)
      isAiScreenDetectionEnabled = true
      Toast.makeText(context, "AI Screen Detection सक्रिय: 100% ऑन-डिवाइस सुरक्षा चालू", Toast.LENGTH_SHORT).show()
    } else {
      isAiScreenDetectionEnabled = false
      com.example.focus.AiScreenDetectionManager.setEnabled(context, false)
      Toast.makeText(context, "स्क्रीन कैप्चर अनुमति रद्द कर दी गई", Toast.LENGTH_SHORT).show()
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. LIVE FOCUS STATUS & MANUAL TOGGLE CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(
          1.dp,
          if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.8f)
          else if (isFocusModeActive) GoldBrass.copy(alpha = 0.6f)
          else DarkSurfaceBorder,
          RoundedCornerShape(18.dp)
        )
        .padding(16.dp)
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
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isStrictLock) Color(0xFFEF4444) else if (isFocusModeActive) GoldBrass else WarmMuted)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isStrictLock) "STRICT LOCK ACTIVE • सख्त लॉक सक्रिय" else if (isFocusModeActive) "FOCUS MODE ACTIVE • सक्रिय" else "FOCUS MODE INACTIVE • निष्क्रिय",
              color = if (isStrictLock) Color(0xFFFF6B6B) else if (isFocusModeActive) GoldBrass else WarmMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          }

          if (isFocusModeActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.2f) else GoldBrass.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (isStrictLock) "Strict 15m" else if (isFocusScheduleEnabled) "Scheduled" else "Manual",
                color = if (isStrictLock) Color(0xFFFF6B6B) else GoldHighlight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Manual Focus Mode", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = if (isStrictLock) {
                "🚫 सख्त लॉक सक्रिय है: वयस्क सामग्री पहचान के कारण 15 मिनट का लॉक बीच में बंद नहीं हो सकता।"
              } else if (isFocusModeActive) {
                "सक्रिय Focus सत्र चल रहा है। बंद करने के लिए पुष्टि करें।"
              } else {
                "तत्काल डिजिटल अनुशासन सत्र चालू करें।"
              },
              color = if (isStrictLock) Color(0xFFFFB4AB) else TextMuted,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }
          Switch(
            checked = isFocusModeActive,
            enabled = !isStrictLock,
            onCheckedChange = { shouldEnable ->
              if (shouldEnable) {
                viewModel.setFocusModeActive(true)
              } else {
                // Confirm turn off
                showFocusExitConfirmationDialog = true
              }
            }
          )
        }
      }
    }

    // 1B. STRICT LOCK: ADULT CONTENT PROTECTION CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFF3B1515)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "सख्त सुरक्षा लॉक (Strict Lock)",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "100% ऑफ़लाइन व ऑन-डिवाइस सक्रिय",
              color = Color(0xFF10B981),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Text(
          text = "ब्राउज़र या किसी ऐप में ख़राब (adult / explicit) कंटेंट पहचानते ही Focus Mode अपने आप 15 मिनट के लिए सक्रिय हो जाता है और बीच में बंद नहीं किया जा सकता। कोई डेटा फ़ोन से बाहर नहीं जाता।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "कुल अवरोधित घटनाएं (Total Interceptions)",
            color = WarmOffWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "${strictLockEvents.size}",
            color = Color(0xFFEF4444),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        if (strictLockEvents.isNotEmpty()) {
          Text(
            text = "हालिया अवरोधित गतिविधियां:",
            color = WarmMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
          strictLockEvents.take(3).forEach { ev ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${ev.date} ${ev.time} • ${ev.reason}",
                color = WarmParchment,
                fontSize = 10.sp,
                maxLines = 1
              )
              Text(
                text = "${ev.durationMinutes}m",
                color = Color(0xFFFF6B6B),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        var isPinVerifiedForAction by remember { mutableStateOf(false) }

        // Protection Health & Diagnostics Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable { viewModel.setTab(com.example.ui.viewmodel.MainTab.PROTECTION_HEALTH) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Protection Health (सुरक्षा स्वास्थ्य व स्थिरता)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              Text("Accessibility, DNS VPN, बैटरी छूट व लॉग जांचें", color = TextMuted, fontSize = 10.sp)
            }
          }
          Text("खोलें →", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Blocklist Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable { showAdultBlocklistDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("ब्लॉक-लिस्ट व सुरक्षित शब्द (जोड़ें/घटाएं)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
          Text("प्रबंधित करें →", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Accountability PIN Row
        val isPinConfigured = remember(showPinDialog) { com.example.focus.AccountabilityPinManager.isPinSet(context) }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable {
              if (isStrictLock) {
                Toast.makeText(context, "🚫 15-मिनट सख्त लॉक सक्रिय है। समाप्त होने तक PIN नहीं बदला जा सकता।", Toast.LENGTH_SHORT).show()
                com.example.focus.ProtectionHealthManager.logHealthEvent(
                  context = context,
                  eventType = "PIN_CHANGE_BLOCKED_STRICT_LOCK",
                  message = "सख्त लॉक के दौरान PIN बदलाव का प्रयास अवरोधित",
                  severity = "MEDIUM"
                )
              } else {
                try {
                  pinDialogMode = if (isPinConfigured) {
                    com.example.ui.components.AccountabilityPinDialogMode.CHANGE_PIN
                  } else {
                    com.example.ui.components.AccountabilityPinDialogMode.INITIAL_SETUP
                  }
                  pendingPinActionTitle = if (isPinConfigured) "Accountability PIN बदलना" else "Accountability PIN सेट करना"
                  showPinDialog = true
                } catch (e: Exception) {
                  com.example.focus.ProtectionHealthManager.logHealthEvent(
                    context = context,
                    eventType = "PIN_FLOW_INIT_FAILED",
                    message = "PIN स्क्रीन खोलने में त्रुटि: ${e.message}",
                    severity = "HIGH"
                  )
                  Toast.makeText(context, "PIN स्क्रीन खोलने में असमर्थ: ${e.message}", Toast.LENGTH_SHORT).show()
                }
              }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = if (isPinConfigured) GoldBrass else Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Accountability PIN (जवाबदेही पिन)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (isPinConfigured) "पिन सुरक्षित है • बदलाव हेतु पिन आवश्यक" else "सेट नहीं है • किसी भरोसेमंद व्यक्ति से सेट कराएं",
                color = if (isPinConfigured) Color(0xFF10B981) else Color(0xFFFFB4AB),
                fontSize = 10.sp
              )
            }
          }
          Text(if (isPinConfigured) "बदलें →" else "सेट करें →", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Hide PIN screen from Recents / Screenshot Toggle
        var hidePinFromRecents by remember {
          mutableStateOf(com.example.focus.AccountabilityPinManager.isHidePinScreenFromRecents(context))
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = if (hidePinFromRecents) GoldBrass else TextMuted,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "PIN स्क्रीन को Recents / Screenshot से छुपाएं",
                color = WarmOffWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Recents और स्क्रीनशॉट में PIN को छिपाएं (Moto / कुछ फोनों पर काली स्क्रीन आने पर इसे बंद रखें)",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp
              )
            }
          }
          Switch(
            checked = hidePinFromRecents,
            onCheckedChange = { checked ->
              hidePinFromRecents = checked
              com.example.focus.AccountabilityPinManager.setHidePinScreenFromRecents(context, checked)
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = GoldBrass,
              checkedTrackColor = GoldBrass.copy(alpha = 0.5f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceBorder
            )
          )
        }

        // Device Admin (Uninstall Protection) Row
        val isDeviceAdminActive = remember { com.example.focus.LifeTrackerAdminReceiver.isDeviceAdminActive(context) }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable(enabled = !isStrictLock) {
              if (!isDeviceAdminActive) {
                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                  putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    com.example.focus.LifeTrackerAdminReceiver.getComponentName(context)
                  )
                  putExtra(
                    android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Strict Lock के दौरान ऐप को अनइंस्टॉल होने या रोके जाने से सुरक्षित रखने के लिए Device Admin सक्रिय करें।"
                  )
                }
                context.startActivity(intent)
              } else {
                Toast.makeText(context, "Device Admin सुरक्षा पहले से सक्रिय है", Toast.LENGTH_SHORT).show()
              }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = if (isDeviceAdminActive) Color(0xFF10B981) else GoldBrass, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("Device Admin (अनइंस्टॉल सुरक्षा)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (isDeviceAdminActive) "सुरक्षा सक्रिय • लॉक में अनइंस्टॉल असंभव" else "निष्क्रिय • अनइंस्टॉल रोकने हेतु चालू करें",
                color = if (isDeviceAdminActive) Color(0xFF10B981) else Color(0xFFFFB4AB),
                fontSize = 10.sp
              )
            }
          }
          Text(if (isDeviceAdminActive) "सक्रिय ✓" else "चालू करें →", color = if (isDeviceAdminActive) Color(0xFF10B981) else GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Secure DNS (DoH) Bypass Prevention Guide Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .clickable { showSecureDnsGuideDialog = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FlashOn, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("ब्राउज़र Secure DNS (DoH) बायपास गाइड", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              Text("Chrome / Brave में DoH बंद करने का तरीका", color = TextMuted, fontSize = 10.sp)
            }
          }
          Text("देखें →", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Incognito & Private Browsing Block Toggle (Gated)
        var isIncognitoBlocked by remember {
          mutableStateOf(com.example.focus.BrowserUrlExtractor.isIncognitoBlockingEnabled(context))
        }
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = if (isIncognitoBlocked) GoldBrass else TextMuted,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "गुप्त / इनकॉग्निटो ब्राउज़िंग ब्लॉक (Incognito Block)",
                color = WarmOffWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Chrome, Brave, Firefox, Edge में गुप्त टैब खोलते ही तुरंत बंद करेगा",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 13.sp
              )
            }
          }
          Switch(
            checked = isIncognitoBlocked,
            enabled = !isStrictLock,
            onCheckedChange = { checked ->
              if (checked) {
                // ON karna bina PIN ke ho
                isIncognitoBlocked = true
                com.example.focus.BrowserUrlExtractor.setIncognitoBlockingEnabled(context, true)
              } else {
                // OFF karne par VERIFY_AND_ROTATE
                executeWithPin("इनकॉग्निटो ब्लॉक बंद करना") {
                  isIncognitoBlocked = false
                  com.example.focus.BrowserUrlExtractor.setIncognitoBlockingEnabled(context, false, pinVerified = true)
                }
              }
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = GoldBrass,
              checkedTrackColor = GoldBrass.copy(alpha = 0.5f),
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurfaceBorder
            )
          )
        }

        // 24/7 Always-On Security & 24-Hour Unlock Request Card
        val isAlwaysOnActive = remember { com.example.focus.AlwaysOnSecurityManager.isAlwaysOnEnabled(context) }
        var hasPendingUnlockReq by remember { mutableStateOf(com.example.focus.AlwaysOnSecurityManager.hasPendingUnlockRequest(context)) }
        var isUnlockReady by remember { mutableStateOf(com.example.focus.AlwaysOnSecurityManager.isUnlockRequestReadyToClaim(context)) }
        var isTempUnlocked by remember { mutableStateOf(com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) }
        val remainingUnlockMs = remember(hasPendingUnlockReq) { com.example.focus.AlwaysOnSecurityManager.getRemainingUnlockDelayMillis(context) }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(0.8.dp, if (hasPendingUnlockReq) Color(0xFFE57A00) else DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = if (isTempUnlocked) Color(0xFF10B981) else Color(0xFFFFB4AB), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("हमेशा-चालू सुरक्षा (24/7 Always-On)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = when {
                      isTempUnlocked -> "अस्थायी रूप से अनलॉक (1 घंटा खुला)"
                      hasPendingUnlockReq -> "अनलॉक अनुरोध जारी • 24 घंटे की देरी"
                      else -> "सक्रिय • सेटिंग्स खोलने हेतु PIN या अनुरोध"
                    },
                    color = when {
                      isTempUnlocked -> Color(0xFF10B981)
                      hasPendingUnlockReq -> Color(0xFFFFB74D)
                      else -> Color(0xFF10B981)
                    },
                    fontSize = 10.sp
                  )
                }
              }

              if (hasPendingUnlockReq) {
                Button(
                  onClick = {
                    com.example.focus.AlwaysOnSecurityManager.cancelUnlockRequest(context)
                    hasPendingUnlockReq = false
                    Toast.makeText(context, "अनलॉक अनुरोध रद्द कर दिया गया", Toast.LENGTH_SHORT).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("अनुरोध रद्द करें", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              } else if (isUnlockReady) {
                Button(
                  onClick = {
                    com.example.focus.AlwaysOnSecurityManager.claimUnlockWindow(context)
                    isTempUnlocked = true
                    isUnlockReady = false
                    Toast.makeText(context, "1 घंटे के लिए सुरक्षा अनलॉक की गई", Toast.LENGTH_SHORT).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("अनलॉक शुरू करें (1h)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              } else if (isTempUnlocked) {
                // When 24-hr unlock window is claimed, allow resetting/updating forgotten PIN
                Button(
                  onClick = {
                    pinDialogMode = com.example.ui.components.AccountabilityPinDialogMode.INITIAL_SETUP
                    showPinDialog = true
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("नया PIN सेट करें", fontSize = 10.sp, color = DarkSurface, fontWeight = FontWeight.Bold)
                }
              } else {
                Button(
                  onClick = {
                    com.example.focus.AlwaysOnSecurityManager.startUnlockRequest(context)
                    hasPendingUnlockReq = true
                    Toast.makeText(context, "24 घंटे का अनलॉक अनुरोध शुरू हुआ", Toast.LENGTH_SHORT).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("अनलॉक अनुरोध (24h)", fontSize = 10.sp, color = GoldBrass, fontWeight = FontWeight.Bold)
                }
              }
            }

            if (hasPendingUnlockReq) {
              val hours = remainingUnlockMs / (3600 * 1000L)
              val mins = (remainingUnlockMs % (3600 * 1000L)) / (60 * 1000L)
              Text(
                text = "⏳ शेष इंतज़ार: $hours घंटे $mins मिनट। 24 घंटे पूरे होने पर 1 घंटे की अनलॉक विंडो खुलेगी।",
                color = Color(0xFFFFB74D),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            } else {
              Text(
                text = "Accessibility, Device Admin या Uninstall स्क्रीन खोलने के लिए या तो Accountability PIN चाहिए या 24 घंटे का कूलडाउन अनुरोध।",
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 14.sp
              )
            }
          }
        }

        // Temporary Protection Pause Card (Requirement 2)
        var isPausedState by remember { mutableStateOf(com.example.focus.ProtectionPauseManager.isPaused(context)) }
        val remainingPauseMs = remember(isPausedState) { com.example.focus.ProtectionPauseManager.getPauseRemainingMillis(context) }
        val pausesToday = remember(isPausedState) { com.example.focus.ProtectionPauseManager.getPausesUsedToday(context) }
        var showPauseProtectionDialog by remember { mutableStateOf(false) }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(0.8.dp, if (isPausedState) Color(0xFFFFB74D) else DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.PauseCircle,
                  contentDescription = null,
                  tint = if (isPausedState) Color(0xFFFFB74D) else GoldBrass,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("सुरक्षा अस्थायी पॉज़ (Pause Protection)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = if (isPausedState) {
                      val m = (remainingPauseMs / 60000L).coerceAtLeast(1)
                      "⏸️ पॉज़ सक्रिय • शेष: $m मिनट"
                    } else {
                      "बैंकिंग ऐप्स हेतु • आज उपयोग: $pausesToday/3"
                    },
                    color = if (isPausedState) Color(0xFFFFB74D) else TextMuted,
                    fontSize = 10.sp
                  )
                }
              }

              if (isPausedState) {
                Button(
                  onClick = {
                    com.example.focus.ProtectionPauseManager.cancelPause(context)
                    isPausedState = false
                    Toast.makeText(context, "पॉज़ समाप्त। कृपया Accessibility व VPN वापस चालू करें।", Toast.LENGTH_LONG).show()
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("अभी वापस ON करें", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              } else {
                Button(
                  onClick = { showPauseProtectionDialog = true },
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("पॉज़ करें (PIN)", fontSize = 10.sp, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                }
              }
            }

            Text(
              text = "पॉज़ करने पर Accessibility, VPN, Admin व Strict Lock बंद हो जाते हैं ताकि बैंकिंग ऐप्स चल सकें। समाप्त होने पर हर 30 मिनट में रिमाइंडर मिलेगा।",
              color = TextMuted,
              fontSize = 10.sp,
              lineHeight = 14.sp
            )
          }
        }

        if (showPauseProtectionDialog) {
          com.example.ui.components.PauseProtectionDialog(
            onDismiss = { showPauseProtectionDialog = false },
            onPauseStarted = {
              isPausedState = true
            }
          )
        }
      }
    }

    // 1C. MULTI-MODEL AI SCREEN DETECTION (.TFLITE IMPORT & ENSEMBLE ENGINE)
    com.example.ui.components.MultiModelManagementCard(
      isStrictLock = isStrictLock
    )

    // 1D. MULTI-USER / GUEST / SECOND SPACE / DUAL APPS BYPASS PROTECTION
    com.example.ui.components.MultiUserProtectionCard(
      isStrictLock = isStrictLock
    )

    // 1E. NOTIFICATION CHANNELS TRANSPARENT MANAGEMENT (REQUIREMENT 4)
    com.example.ui.components.NotificationChannelsManagementCard()

    // 2. AUTOMATIC SCHEDULE CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Automatic Schedule", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "निर्धारित समय पर फोकस मोड स्वतः चालू व बंद हो। रीबूट के बाद भी प्रभावी।",
              color = TextMuted,
              fontSize = 12.sp
            )
          }
          Switch(
            checked = isFocusScheduleEnabled,
            enabled = !isStrictLock,
            onCheckedChange = { enabled ->
              viewModel.updateFocusSchedule(enabled, focusStartTimeMinutes, focusEndTimeMinutes)
            }
          )
        }

        AnimatedVisibility(visible = isFocusScheduleEnabled) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.HorizontalDivider(color = DarkSurfaceBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Start Time Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCard)
                .clickable(enabled = !isStrictLock) { showStartTimePicker = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Start Time (आरंभ समय)", color = WarmMuted, fontSize = 11.sp)
                Text(
                  text = TimeUtils.minutesTo12Hour(focusStartTimeMinutes),
                  color = WarmOffWhite,
                  fontFamily = ChronoSerifFamily,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                text = if (isStrictLock) "🔒 लॉक" else "बदलें →",
                color = if (isStrictLock) WarmMuted else GoldBrass,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // End Time Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCard)
                .clickable(enabled = !isStrictLock) { showEndTimePicker = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("End Time (समाप्ति समय)", color = WarmMuted, fontSize = 11.sp)
                Text(
                  text = TimeUtils.minutesTo12Hour(focusEndTimeMinutes),
                  color = WarmOffWhite,
                  fontFamily = ChronoSerifFamily,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Text(
                text = if (isStrictLock) "🔒 लॉक" else "बदलें →",
                color = if (isStrictLock) WarmMuted else GoldBrass,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }

            // Overnight helper note
            val startFormatted = TimeUtils.minutesTo12Hour(focusStartTimeMinutes)
            val endFormatted = TimeUtils.minutesTo12Hour(focusEndTimeMinutes)
            val isOvernight = focusStartTimeMinutes > focusEndTimeMinutes
            Text(
              text = if (isOvernight) "🌙 रात्रि-कालीन शेड्यूल: $startFormatted से अगली सुबह $endFormatted तक।" else "☀️ दिन-कालीन शेड्यूल: $startFormatted से $endFormatted तक।",
              color = GoldHighlight,
              fontSize = 11.sp
            )
          }
        }
      }
    }

    // 2B. ALLOWED APPS WHITELIST CARD
    var showAllowedAppsSettingsDialog by remember { mutableStateOf(false) }
    val allowedAppsCount = remember(showAllowedAppsSettingsDialog) { com.example.focus.FocusModeManager.getAllowedPackages(context).size }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.4f) else DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Allowed Apps (अनुमत ऐप्स)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = if (isStrictLock) {
                "🔒 सख्त लॉक सक्रिय है: 15 मिनट पूर्ण होने से पहले अनुमत ऐप्स की सूची नहीं बदली जा सकती।"
              } else {
                "फोकस मोड / सख्त लॉक में सिर्फ Phone/Dialer (आपातकालीन कॉल हमेशा चालू) और ये चुनी हुई ऐप्स ही चल सकेंगी। बाकी सभी ऐप्स ब्लॉक रहेंगे।"
              },
              color = if (isStrictLock) Color(0xFFFFB4AB) else TextMuted,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard)
            .clickable(enabled = !isStrictLock) {
              executeWithPin("अनुमत ऐप्स (Allowed Apps) सूची में बदलाव") {
                showAllowedAppsSettingsDialog = true
              }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Shield,
              contentDescription = null,
              tint = if (isStrictLock) Color(0xFFEF4444) else GoldBrass,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "अनुमत ऐप्स: $allowedAppsCount चुनी गईं",
              color = WarmOffWhite,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = if (isStrictLock) "🔒 लॉक" else "बदलें →",
            color = if (isStrictLock) WarmMuted else GoldBrass,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    if (showAllowedAppsSettingsDialog) {
      com.example.ui.components.AllowedAppsDialog(
        onDismiss = { showAllowedAppsSettingsDialog = false },
        onSave = { showAllowedAppsSettingsDialog = false }
      )
    }
  }

  // DIALOGS:
  // Start Time Picker
  if (showStartTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = focusStartTimeMinutes,
      onDismiss = { showStartTimePicker = false },
      onConfirm = { newMins ->
        showStartTimePicker = false
        viewModel.updateFocusSchedule(isFocusScheduleEnabled, newMins, focusEndTimeMinutes)
      }
    )
  }

  // End Time Picker
  if (showEndTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = focusEndTimeMinutes,
      onDismiss = { showEndTimePicker = false },
      onConfirm = { newMins ->
        showEndTimePicker = false
        viewModel.updateFocusSchedule(isFocusScheduleEnabled, focusStartTimeMinutes, newMins)
      }
    )
  }

  // Focus Mode exit confirmation dialog (Simple & Clear: "Focus Mode बंद करना है?", "Cancel", "Turn Off")
  if (showFocusExitConfirmationDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showFocusExitConfirmationDialog = false },
      containerColor = DarkSurfaceElevated,
      title = {
        Text(
          text = "Focus Mode बंद करना है?",
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = "क्या आप अभी Focus Mode सत्र समाप्त करना चाहते हैं? सभी ऐप्स पुनः सामान्य रूप से उपलब्ध हो जाएंगे।",
          color = TextSecondary,
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showFocusExitConfirmationDialog = false
            viewModel.unlockFocusMode()
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = DarkBackground),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("confirm_turn_off_focus_button")
        ) {
          Text("Turn Off", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showFocusExitConfirmationDialog = false },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
          modifier = Modifier.testTag("cancel_turn_off_focus_button")
        ) {
          Text("Cancel")
        }
      }
    )

    if (showAdultBlocklistDialog) {
      com.example.ui.components.AdultBlocklistDialog(
        onDismiss = { showAdultBlocklistDialog = false }
      )
    }

    if (showPinDialog) {
      com.example.ui.screens.AccountabilityPinScreen(
        onDismiss = {
          showPinDialog = false
          pendingPinAction = null
        },
        mode = pinDialogMode,
        actionTitle = pendingPinActionTitle,
        onActionApproved = {
          pendingPinAction?.invoke()
          showPinDialog = false
          pendingPinAction = null
        }
      )
    }

    if (showSecureDnsGuideDialog) {
      com.example.ui.components.SecureDnsGuideDialog(
        onDismiss = { showSecureDnsGuideDialog = false }
      )
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 3: APPEARANCE & THEME (Luxury Chronograph Instrument)
// -------------------------------------------------------------
@Composable
private fun AppearanceCategory(viewModel: LifeTrackerViewModel) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldBrass))
          Spacer(modifier = Modifier.width(6.dp))
          Text("CHRONOMETER PRECISION INSTRUMENT", color = GoldBrass, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        }
        Text("Luxury Watch Aesthetic", color = WarmOffWhite, fontFamily = ChronoSerifFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Inspired by luxury mechanical chronographs. Crafted with deep warm obsidian charcoal, satin horological brass accents, warm off-white typography, and precision 60-bezel index marks.",
          color = WarmParchment,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        // Palette Swatches
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            Pair("Obsidian", Color(0xFF14120E)),
            Pair("Brass", GoldBrass),
            Pair("Champagne", GoldHighlight),
            Pair("Sage", SageGreen),
            Pair("Plum", MutedPlum),
            Pair("Ice Blue", IceBlue),
            Pair("Off-White", WarmOffWhite)
          ).forEach { (label, col) ->
            Box(
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(col)
                .border(0.6.dp, ObsidianBorder, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(label.take(1), color = if (col == WarmOffWhite || col == GoldHighlight) Color(0xFF14120E) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Typography & Dial Scaling", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Serif numerals for major metrics and adherence dials; geometric sans-serif for labels, time ranges, and timetable blocks.", color = WarmMuted, fontSize = 12.sp)
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Time Display Format", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("12-Hour AM/PM with Indian Time Notations", color = GoldBrass, fontFamily = ChronoSerifFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 6: GOALS & SCORING
// -------------------------------------------------------------
@Composable
private fun GoalsAndScoringCategory(viewModel: LifeTrackerViewModel) {
  val streakInfo by viewModel.streakInfo.collectAsState()
  val goals by viewModel.allGoals.collectAsState()
  var showNewGoalSheet by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. Scoring System Overview Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldBrass))
          Spacer(modifier = Modifier.width(6.dp))
          Text("DAILY ADHERENCE BENCHMARK", color = GoldBrass, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        }
        Text("Daily Score Calibration: 40.0 pts", color = WarmOffWhite, fontFamily = ChronoSerifFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "• Completed Task: +2.5 pts (Full Adherence ✓)\n• Half / Partial Task: +1.25 pts (~ Half)\n• Missed Task: 0.0 pts (✕ Pending)\nTarget benchmark: 32.0 / 40.0 pts (80% Adherence).",
          color = WarmParchment,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )
      }
    }

    // 2. Streak Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Consistency Streak", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Current: ${streakInfo.currentStreak} Days • Best: ${streakInfo.longestStreak} Days", color = WarmMuted, fontSize = 12.sp)
        }
        Text(
          text = "${streakInfo.currentStreak}d 🔥",
          color = GoldBrass,
          fontFamily = ChronoSerifFamily,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // 3. Active Goals List
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "ACTIVE GOALS (${goals.size})",
        color = GoldBrass,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
      )
      Button(
        onClick = { showNewGoalSheet = true },
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianElevated, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("+ New Goal", color = ObsidianElevated, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    if (goals.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(ObsidianCard)
          .border(0.8.dp, ObsidianBorder, RoundedCornerShape(14.dp))
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No active goals yet. Tap '+ New Goal' to begin tracking.", color = WarmMuted, fontSize = 12.sp)
      }
    } else {
      goals.forEach { goal ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(0.8.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(goal.title, color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text("${goal.progress}%", color = GoldBrass, fontFamily = ChronoSerifFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (goal.description.isNotBlank()) {
              Text(goal.description, color = WarmMuted, fontSize = 11.sp)
            }
            LinearProgressIndicator(
              progress = { (goal.progress / 100f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
              color = if (goal.isCompleted) SageGreen else GoldBrass,
              trackColor = ObsidianElevated
            )
          }
        }
      }
    }
  }

  if (showNewGoalSheet) {
    GoalBottomSheet(
      initialGoal = null,
      onSave = { id, title, desc, prog, deadline, isCompleted ->
        viewModel.saveGoal(id, title, desc, prog, deadline, isCompleted)
        showNewGoalSheet = false
      },
      onDismiss = { showNewGoalSheet = false }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 8: GENERAL SETTINGS
// -------------------------------------------------------------
@Composable
private fun GeneralCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()
  val userName by viewModel.userName.collectAsState()
  var nameInput by remember(userName) { mutableStateOf(userName) }
  var isEditingName by remember { mutableStateOf(false) }
  val dailyEnergyMode by viewModel.dailyEnergyMode.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. Profile Name Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("User Profile & Greeting", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Displayed in morning status and voice audio", color = WarmMuted, fontSize = 11.sp)
          }
          IconButton(onClick = { isEditingName = !isEditingName }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = GoldBrass, modifier = Modifier.size(16.dp))
          }
        }

        if (isEditingName) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = nameInput,
              onValueChange = { nameInput = it },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldBrass,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = WarmOffWhite,
                unfocusedTextColor = WarmOffWhite,
                focusedContainerColor = ObsidianElevated,
                unfocusedContainerColor = ObsidianElevated
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            )
            Button(
              onClick = {
                if (nameInput.isNotBlank()) {
                  viewModel.updateUserName(nameInput.trim())
                  isEditingName = false
                  Toast.makeText(context, "नाम अपडेट किया गया: $nameInput", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Save", color = ObsidianElevated, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Text(
            text = userName,
            color = GoldHighlight,
            fontFamily = ChronoSerifFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 2. 7-Day Cycle Anchor Date
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Cycle Anchor Date (Day 1)", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Determines 7-day cyclical routine repetition", color = WarmMuted, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(userSettings?.anchorDate ?: "2026-09-24", color = GoldBrass, fontFamily = ChronoSerifFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // 3. Energy Mode Quick Selector
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily Energy Mode", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          EnergyMode.entries.forEach { mode ->
            val isSelected = dailyEnergyMode == mode
            val color = when (mode) {
              EnergyMode.LOW -> IceBlue
              EnergyMode.NORMAL -> GoldBrass
              EnergyMode.HIGH -> GoldHighlight
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) color.copy(alpha = 0.2f) else ObsidianElevated)
                .border(1.dp, if (isSelected) color else ObsidianBorder, RoundedCornerShape(12.dp))
                .clickable { viewModel.setDailyEnergyMode(mode) }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("${mode.emoji} ${mode.label}", color = if (isSelected) color else WarmParchment, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Persistence & Architecture
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("System Persistence & Alarms", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Precision alarms automatically reschedule upon device reboot. All data is persisted 100% offline in local Room SQLite database.", color = WarmMuted, fontSize = 12.sp, lineHeight = 17.sp)
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 6: REPORTS & TRACKING
// -------------------------------------------------------------
@Composable
private fun ReportsTrackingCategory(viewModel: LifeTrackerViewModel) {
  val enableMorningBrief by viewModel.enableMorningBrief.collectAsState()
  val enableLifeTimeline by viewModel.enableLifeTimeline.collectAsState()
  val enablePersonalInsights by viewModel.enablePersonalInsights.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Smart Morning Brief", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Daily morning motivation card and priority heads-up", color = TextMuted, fontSize = 12.sp)
        }
        Switch(checked = enableMorningBrief, onCheckedChange = { viewModel.updateFeatureToggle("morningBrief", it) })
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Life Timeline Tracking", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Record chronological daily snapshots & milestones", color = TextMuted, fontSize = 12.sp)
        }
        Switch(checked = enableLifeTimeline, onCheckedChange = { viewModel.updateFeatureToggle("lifeTimeline", it) })
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Personal Insights Analytics", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Calculate peak focus hours and habit completion trends", color = TextMuted, fontSize = 12.sp)
        }
        Switch(checked = enablePersonalInsights, onCheckedChange = { viewModel.updateFeatureToggle("personalInsights", it) })
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 7: BACKUP & DATA
// -------------------------------------------------------------
@Composable
private fun DataBackupCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var refreshTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }

  val folderUri = remember(refreshTrigger) { com.example.backup.BackupPreferences.getTreeUri(context) }
  val isFolderAccessible = remember(refreshTrigger) { com.example.backup.BackupPreferences.isFolderAccessible(context) }
  val folderName = remember(refreshTrigger) { com.example.backup.BackupPreferences.getFolderDisplayName(context) }
  var isAutoBackup by remember(refreshTrigger) { mutableStateOf(com.example.backup.BackupPreferences.isAutoBackupEnabled(context)) }
  val backupTime = remember(refreshTrigger) { com.example.backup.BackupPreferences.getBackupTime(context) }
  val lastBackupTimestamp = remember(refreshTrigger) { com.example.backup.BackupPreferences.getLastBackupTimestamp(context) }
  val lastBackupSize = remember(refreshTrigger) { com.example.backup.BackupPreferences.getLastBackupSize(context) }
  val lastBackupFileName = remember(refreshTrigger) { com.example.backup.BackupPreferences.getLastBackupFileName(context) }
  val lastBackupError = remember(refreshTrigger) { com.example.backup.BackupPreferences.getLastBackupError(context) }

  var isBackingUpNow by remember { mutableStateOf(false) }
  var showRestoreDialog by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var exportDialogInitialTab by remember { mutableStateOf(ExportFormatTab.JSON) }

  // Reset App Safety Dialogs State
  var showResetWarningDialog by remember { mutableStateOf(false) }
  var showResetConfirmDialog by remember { mutableStateOf(false) }
  var isResettingApp by remember { mutableStateOf(false) }

  // Folder Picker Launcher (OpenDocumentTree with persistable permission)
  val folderLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      try {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
        val name = doc?.name ?: uri.lastPathSegment ?: "LifeTracker Backup"
        com.example.backup.BackupPreferences.setTreeUri(context, uri, name)
        com.example.backup.DailyBackupWorker.schedulePeriodic(context)
        Toast.makeText(context, "बैकअप फ़ोल्डर सफलतापूर्वक चुना गया!", Toast.LENGTH_SHORT).show()
        refreshTrigger++
      } catch (e: Exception) {
        Toast.makeText(context, "फ़ोल्डर अनुमति में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

    // =========================================================
    // SECTION 1: BACKUP DATA (डेटा बैकअप)
    // =========================================================
    Text(
      text = "1. बैकअप डेटा (Backup Data)",
      color = CyanNeon,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )

    // Folder Selection Card (Survival after Uninstall)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(if (isFolderAccessible) DarkSurfaceElevated else DustyRose.copy(alpha = 0.12f))
        .border(
          width = 1.dp,
          color = if (isFolderAccessible) SageGreen.copy(alpha = 0.5f) else DustyRose.copy(alpha = 0.6f),
          shape = RoundedCornerShape(18.dp)
        )
        .padding(16.dp)
        .testTag("backup_folder_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isFolderAccessible) Icons.Default.FolderOpen else Icons.Default.WarningAmber,
              contentDescription = null,
              tint = if (isFolderAccessible) SageGreen else DustyRose,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isFolderAccessible) "सुरक्षित बैकअप फ़ोल्डर" else "⚠️ बैकअप फ़ोल्डर आवश्यक है",
              color = if (isFolderAccessible) WarmOffWhite else DustyRose,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }

          if (isFolderAccessible) {
            Text(
              text = "सक्रिय",
              color = SageGreen,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        if (isFolderAccessible) {
          Text(
            text = "📂 चयनित फ़ोल्डर: ${folderName ?: "Documents"}",
            color = WarmOffWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "यह फ़ोल्डर ऐप अनइंस्टॉल करने के बाद भी सुरक्षित रहेगा। भविष्य में ऐप दोबारा इंस्टॉल करने पर इस फ़ोल्डर से पूरा डेटा वापस लाया जा सकेगा।",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp
          )
        } else {
          Text(
            text = "चेतावनी: बैकअप तब तक सुरक्षित नहीं है जब तक आप एक बाहरी फ़ोल्डर नहीं चुनते! ऐप अनइंस्टॉल होने पर आंतरिक स्टोरेज मिट जाता है। कृपया Documents में 'LifeTracker Backup' फ़ोल्डर बनाएं।",
            color = DustyRose,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }

        Button(
          onClick = { folderLauncher.launch(null) },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isFolderAccessible) ObsidianCard else DustyRose,
            contentColor = if (isFolderAccessible) GoldBrass else DarkBackground
          ),
          border = if (isFolderAccessible) androidx.compose.foundation.BorderStroke(1.dp, GoldBrass.copy(alpha = 0.5f)) else null,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("choose_backup_folder_btn")
        ) {
          Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isFolderAccessible) "फ़ोल्डर बदलें (Change Folder)" else "फ़ोल्डर चुनें (Choose Folder)",
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // Automatic Schedule Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
        .testTag("auto_backup_schedule_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "स्वचालित दैनिक बैकअप",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "प्रतिदिन बैकअप लेकर सुरक्षित ज़िप फ़ाइल बनाता है",
              color = TextMuted,
              fontSize = 11.sp
            )
          }

          Switch(
            checked = isAutoBackup,
            onCheckedChange = { enabled ->
              isAutoBackup = enabled
              com.example.backup.BackupPreferences.setAutoBackupEnabled(context, enabled)
              if (enabled) {
                com.example.backup.DailyBackupWorker.schedulePeriodic(context)
              } else {
                com.example.backup.DailyBackupWorker.cancel(context)
              }
              refreshTrigger++
            },
            colors = SwitchDefaults.colors(
              checkedThumbColor = DarkBackground,
              checkedTrackColor = CyanNeon,
              uncheckedThumbColor = TextMuted,
              uncheckedTrackColor = DarkSurface
            ),
            modifier = Modifier.testTag("auto_backup_switch")
          )
        }

        if (isAutoBackup) {
          val (hour, min) = backupTime
          val timeStr = TimeUtils.minutesTo12Hour(hour * 60 + min)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .clickable { showTimePicker = true }
              .padding(12.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text("बैकअप समय", color = TextSecondary, fontSize = 11.sp)
                  Text(timeStr, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
              }

              Text("बदलें →", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
          }
        }
      }
    }

    // Backup Status & Details Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
        .testTag("backup_status_details_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("बैकअप स्थिति एवं विवरण", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

          val status = com.example.backup.BackupPreferences.getLastBackupStatus(context)
          val (badgeText, badgeColor) = when (status) {
            com.example.backup.BackupPreferences.STATUS_OK -> "सक्रिय (OK)" to SageGreen
            com.example.backup.BackupPreferences.STATUS_ERROR -> "त्रुटि (Error)" to DustyRose
            com.example.backup.BackupPreferences.STATUS_NO_FOLDER -> "फ़ोल्डर नहीं है" to DustyRose
            else -> "कोई बैकअप नहीं" to WarmMuted
          }

          Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }

        if (lastBackupTimestamp > 0L) {
          val sdf = java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.US)
          val formattedDate = sdf.format(java.util.Date(lastBackupTimestamp))
          val sizeKb = (lastBackupSize / 1024.0)
          val sizeStr = if (sizeKb >= 1024) String.format(java.util.Locale.US, "%.1f MB", sizeKb / 1024.0)
          else String.format(java.util.Locale.US, "%.1f KB", sizeKb)

          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("• पिछला सफल बैकअप: $formattedDate", color = WarmOffWhite, fontSize = 12.sp)
            Text("• बैकअप साइज़: $sizeStr", color = TextMuted, fontSize = 12.sp)
            if (!lastBackupFileName.isNullOrBlank()) {
              Text("• फ़ाइल: $lastBackupFileName", color = TextMuted, fontSize = 11.sp)
            }
          }
        } else {
          Text("• अभी तक कोई बैकअप नहीं लिया गया है।", color = TextMuted, fontSize = 12.sp)
        }

        if (isAutoBackup && isFolderAccessible) {
          val (h, m) = backupTime
          Text("• अगला निर्धारित बैकअप: प्रतिदिन ${TimeUtils.minutesTo12Hour(h * 60 + m)}", color = SageGreen, fontSize = 12.sp)
        }

        if (!lastBackupError.isNullOrBlank()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(DustyRose.copy(alpha = 0.15f))
              .border(1.dp, DustyRose.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Text("अंतिम त्रुटि: $lastBackupError", color = DustyRose, fontSize = 11.sp)
          }
        }

        Button(
          onClick = {
            if (!isBackingUpNow) {
              isBackingUpNow = true
              coroutineScope.launch {
                val res = com.example.backup.BackupManager.performBackup(context)
                isBackingUpNow = false
                refreshTrigger++
                if (res.isSuccess) {
                  Toast.makeText(context, "✅ बैकअप सफलतापूर्वक पूर्ण हुआ!", Toast.LENGTH_SHORT).show()
                } else {
                  val err = res.exceptionOrNull()?.localizedMessage ?: "बैकअप विफल"
                  Toast.makeText(context, "❌ $err", Toast.LENGTH_LONG).show()
                }
              }
            }
          },
          enabled = !isBackingUpNow,
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = DarkBackground),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("backup_now_btn")
        ) {
          if (isBackingUpNow) {
            CircularProgressIndicator(color = DarkBackground, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("बैकअप चालू...", fontSize = 13.sp)
          } else {
            Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("अभी बैकअप लें (Backup Now)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }

    // =========================================================
    // SECTION 2: RESTORE BACKUP (पुराना बैकअप वापस लाएँ)
    // =========================================================
    Text(
      text = "2. बैकअप से पुनर्स्थापित करें (Restore Backup)",
      color = GoldBrass,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GoldBrass.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        .padding(16.dp)
        .testTag("restore_backup_section_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.History, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Existing User / Restore Backup",
            color = WarmOffWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Text(
          text = "यदि आपके पास पूर्व बैकअप है (सुरक्षित फ़ोल्डर या .zip फ़ाइल में), तो आप उसे यहाँ से पुनर्स्थापित कर सकते हैं। पुनर्स्थापना से पहले फ़ाइल की वैधता एवं स्कीमा जाँची जाती है।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )

        OutlinedButton(
          onClick = { showRestoreDialog = true },
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, GoldBrass.copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth().testTag("restore_from_backup_btn")
        ) {
          Icon(Icons.Default.History, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("फ़ोल्डर या ZIP फ़ाइल से रिस्टोर करें", color = GoldBrass, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }

    // =========================================================
    // SECTION 3: NEW USER / RESET APP (नया यूज़र / ऐप रीसेट करें)
    // =========================================================
    Text(
      text = "3. नया यूज़र / ऐप रीसेट करें (New User / Reset App)",
      color = DustyRose,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DustyRose.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
        .padding(16.dp)
        .testTag("reset_app_section_card")
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.WarningAmber, contentDescription = null, tint = DustyRose, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "New User / Reset App",
            color = WarmOffWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Text(
          text = "ऐप को एक नए उपयोगकर्ता की तरह शुरुआत करने के लिए रीसेट करें। आपके सभी रूटीन, दैनिक कार्य, व्यक्तिगत नोट्स और सेटिंग्स हट जाएंगे और ऐप पूरी तरह नए सिरे से खाली रूटीन के साथ शुरू होगा।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )

        Button(
          onClick = { showResetWarningDialog = true },
          colors = ButtonDefaults.buttonColors(
            containerColor = DustyRose.copy(alpha = 0.15f),
            contentColor = DustyRose
          ),
          border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("open_reset_warning_btn")
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = null, tint = DustyRose, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("नया यूज़र / ऐप रीसेट करें (Reset App)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }

    // =========================================================
    // SECTION 4: EXPORT DATA (डेटा एक्सपोर्ट)
    // =========================================================
    Text(
      text = "4. डेटा एक्सपोर्ट (Export Data)",
      color = CyanNeon,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
        .clickable {
          exportDialogInitialTab = ExportFormatTab.JSON
          showExportDialog = true
        }
        .padding(14.dp)
        .testTag("open_export_data_btn")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("डेटा एक्सपोर्ट करें (Export Data)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          Text("JSON, CSV, Markdown, Plain Text या AI कोचिंग विश्लेषण में कॉपी / शेयर करें", color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
      }
    }

    // =========================================================
    // SECTION 5: IMPORT DATA (डेटा इम्पोर्ट / रीस्टोर)
    // =========================================================
    Text(
      text = "5. डेटा इम्पोर्ट (Import Data)",
      color = VioletNeon,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold
    )

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
        .clickable {
          exportDialogInitialTab = ExportFormatTab.IMPORT
          showExportDialog = true
        }
        .padding(14.dp)
        .testTag("open_import_data_btn")
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("डेटा इम्पोर्ट / रिस्टोर करें (Import Data)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          Text("JSON टेक्स्ट पेस्ट करके सीधे डेटाबेस में डेटा पुनर्स्थापित करें", color = TextMuted, fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
      }
    }
  }

  // Restore Dialog
  if (showRestoreDialog) {
    RestoreBackupsDialog(
      initialFolderUri = folderUri,
      onDismiss = { showRestoreDialog = false },
      onRestoreSuccess = {
        refreshTrigger++
        Toast.makeText(context, "डेटा पुनर्स्थापित हो गया!", Toast.LENGTH_SHORT).show()
        (context as? android.app.Activity)?.recreate()
      },
      onOpenPermissionsStatus = {
        showRestoreDialog = false
      }
    )
  }

  // Step 1: Reset App Warning Dialog
  if (showResetWarningDialog) {
    ResetAppWarningDialog(
      onDismiss = { showResetWarningDialog = false },
      onBackupFirst = {
        showResetWarningDialog = false
        if (isFolderAccessible) {
          coroutineScope.launch {
            val res = com.example.backup.BackupManager.performBackup(context)
            if (res.isSuccess) {
              Toast.makeText(context, "✅ बैकअप सुरक्षित सहेजा गया! अब आप चाहें तो रीसेट जारी रख सकते हैं।", Toast.LENGTH_LONG).show()
              showResetConfirmDialog = true
            } else {
              Toast.makeText(context, "बैकअप विफल: ${res.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
          }
        } else {
          Toast.makeText(context, "कृपया पहले बैकअप फ़ोल्डर चुनें", Toast.LENGTH_SHORT).show()
          folderLauncher.launch(null)
        }
      },
      onContinue = {
        showResetWarningDialog = false
        showResetConfirmDialog = true
      }
    )
  }

  // Step 2: Final Confirmation Dialog
  if (showResetConfirmDialog) {
    ResetAppConfirmDialog(
      isResetting = isResettingApp,
      onDismiss = { showResetConfirmDialog = false },
      onConfirmReset = {
        isResettingApp = true
        viewModel.resetAppToFreshState { success, msg ->
          isResettingApp = false
          showResetConfirmDialog = false
          refreshTrigger++
          Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
          if (success) {
            (context as? android.app.Activity)?.recreate()
          }
        }
      }
    )
  }

  // Time Picker Dialog
  if (showTimePicker) {
    val (curH, curM) = backupTime
    TimePickerDialog12Hour(
      initialMinutes = curH * 60 + curM,
      onDismiss = { showTimePicker = false },
      onConfirm = { mins ->
        val newH = (mins / 60) % 24
        val newM = mins % 60
        com.example.backup.BackupPreferences.setBackupTime(context, newH, newM)
        com.example.backup.DailyBackupWorker.schedulePeriodic(context)
        showTimePicker = false
        refreshTrigger++
      }
    )
  }

  // Advanced Export / Import Dialog
  if (showExportDialog) {
    ExportImportDialog(
      initialTab = exportDialogInitialTab,
      onDismiss = { showExportDialog = false },
      onGenerateContent = { tab ->
        when (tab) {
          ExportFormatTab.JSON -> viewModel.exportJson()
          ExportFormatTab.CSV -> viewModel.exportCsv()
          ExportFormatTab.MARKDOWN -> viewModel.exportMarkdown()
          ExportFormatTab.TXT -> viewModel.exportPlainText()
          ExportFormatTab.AI_PROMPT -> viewModel.exportAiAnalysis()
          else -> ""
        }
      },
      onImportJson = { json, cb -> viewModel.importJson(json, cb) }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 8: USER PROFILE
// -------------------------------------------------------------
@Composable
private fun ProfileCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val currentName by viewModel.userName.collectAsState()
  var nameInput by remember(currentName) { mutableStateOf(currentName) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your Name (तुम्हारा नाम)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("This name is used in personalized voice announcements during meditation completion (\"मोहित, तुम्हारा मेडिटेशन...\") and morning greeting.", color = TextMuted, fontSize = 12.sp)

        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Profile Name") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = {
            viewModel.updateUserName(nameInput)
            Toast.makeText(context, "नाम सफलतापूर्वक सहेजा गया!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save Name", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 9: ENERGY MODE & CHALLENGE
// -------------------------------------------------------------
@Composable
private fun EnergyAndChallengeCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val dailyEnergyMode by viewModel.dailyEnergyMode.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()
  val isRecoveryActive by viewModel.isRecoveryDayActive.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Energy Mode Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("⚡ Daily Energy Mode • ऊर्जा मोड", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "दिन के लिए अपना Energy Mode चुनें। आवश्यक रूटीन हमेशा बना रहेगा, पर Low Energy में वैकल्पिक कार्य हल्के कर दिए जाएंगे। यह अगले दिन स्वतः सामान्य हो जाता है।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          EnergyMode.entries.forEach { mode ->
            val isSelected = dailyEnergyMode == mode
            val modeColor = when (mode) {
              EnergyMode.LOW -> Color(0xFF4DD0E1)
              EnergyMode.NORMAL -> CyanNeon
              EnergyMode.HIGH -> Color(0xFFFFB300)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) modeColor.copy(alpha = 0.2f) else DarkSurface)
                .border(1.dp, if (isSelected) modeColor else DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .clickable {
                  viewModel.setDailyEnergyMode(mode)
                  Toast.makeText(context, "${mode.label} ऊर्जा मोड सेट किया गया", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mode.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(mode.label, color = if (isSelected) modeColor else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 2. Recovery Day Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("🌿 Recovery Day • रिकवरी डे", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          if (isRecoveryActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF4DD0E1).copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text("Active", color = Color(0xFF4DD0E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        Text(
          text = "जब दिन का रूटीन किसी कारण छूट जाए, तो तनाव लेने के बजाय Recovery Day शुरू करें। यह आवश्यक कार्यों को प्राथमिकता देकर हल्का शेड्यूल बनाता है और मूल समय सारणी को स्थाई रूप से नहीं बदलता।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              viewModel.activateRecoveryDay()
              Toast.makeText(context, "रिकवरी डे सक्रिय किया गया!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Start Recovery Day", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          if (isRecoveryActive) {
            Button(
              onClick = {
                viewModel.restoreDefaultRoutine()
                Toast.makeText(context, "मूल समय सारणी पुनः लागू की गई", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Restore Default", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // 3. Daily Challenge Toggle Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("🌱 Daily Challenge System", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "दैनिक सकारात्मक चुनौती (पठन, ध्यान, सीखना, व्यायाम आदि)",
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = userSettings?.dailyChallengeEnabled ?: true,
            onCheckedChange = { enabled ->
              coroutineScope.launch {
                viewModel.repository.updateDailyChallengeEnabled(enabled)
              }
            }
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 10: MUSIC SYSTEM
// -------------------------------------------------------------
@Composable
private fun MusicSystemCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val allSongs by viewModel.allSongs.collectAsState()
  val allPlaylists by viewModel.allPlaylists.collectAsState()
  val playlistRules by viewModel.playlistRules.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Library Status Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎵 Music Library & Playlists", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Life Tracker में पूरा Music System उपलब्ध है। अपने फोन के गानों को लाइब्रेरी में स्कैन करें, प्लेलिस्ट बनाएं, और समय व मूड के हिसाब से ऑटोमैटिक रूटीन से जोड़ें।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("${allSongs.size}", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("कुल ऑडियो", color = TextMuted, fontSize = 11.sp)
          }
          Column {
            Text("${allPlaylists.size}", color = VioletNeon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("प्लेलिस्ट्स", color = TextMuted, fontSize = 11.sp)
          }
          Column {
            Text("${playlistRules.size}", color = Color(0xFFFFB300), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("मूड/टाइम रूल्स", color = TextMuted, fontSize = 11.sp)
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              viewModel.scanDeviceMusic()
              Toast.makeText(context, "ऑडियो स्कैन किया जा रहा है...", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Scan Audio Files", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          Button(
            onClick = { viewModel.navigateTo(MainTab.MUSIC) },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Open Music Player", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // 2. Routine Auto-Sync Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Connect Routine with Music", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "दिनचर्या के समय अनुसार (सुबह, वर्कआउट, रात) परिभाषित मूड के गाने चुनें",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
        Switch(
          checked = userSettings?.musicAutoRoutineEnabled ?: true,
          onCheckedChange = { enabled ->
            coroutineScope.launch {
              viewModel.repository.updateMusicAutoRoutineEnabled(enabled)
            }
          }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 11: HOME SCREEN WIDGET
// -------------------------------------------------------------
@Composable
private fun HomeWidgetCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("📱 Android Home Screen Widgets", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Life Tracker में 3 प्रकार के नेटिव Android होम स्क्रीन विजेट उपलब्ध हैं। अपने फोन की होम स्क्रीन पर खाली जगह को लॉन्ग प्रेस करके 'Widgets' चुनें और Life Tracker जोड़ें:",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        // Small Widget Info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("1. Small Widget (छोटा विजेट)", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("• अगला कार्य (Next Activity)\n• आरंभ समय एवं समय शेष (Remaining Time)", color = TextSecondary, fontSize = 11.sp)
          }
        }

        // Medium Widget Info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("2. Medium Widget (मध्यम विजेट)", color = VioletNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("• आज की प्रगति % एवं टास्क काउंटर\n• अगला कार्य एवं 1-टैप 'Complete' बटन", color = TextSecondary, fontSize = 11.sp)
          }
        }

        // Large Widget Info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("3. Large Widget (विस्तृत विजेट)", color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("• आज की समय सारणी की लिस्ट\n• क्विक एक्शन: टास्क पूर्ण करें, ध्यान साधना शुरू करें, ऐप खोलें", color = TextSecondary, fontSize = 11.sp)
          }
        }

        Button(
          onClick = {
            com.example.widget.LifeTrackerWidgetProvider.updateAllWidgets(context)
            Toast.makeText(context, "विजेट्स को सफलतापूर्वक रीफ्रेश किया गया!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Refresh All Widgets Now", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

enum class SettingsCategory(
  val title: String,
  val hindiTitle: String,
  val description: String,
  val icon: ImageVector,
  val iconColor: Color
) {
  FOCUS_RESTRICTION(
    title = "Porn Blocker & Security Controls",
    hindiTitle = "वयस्क सामग्री अवरोधक व सुरक्षा नियंत्रण",
    description = "Strict Lock, Adult Blocklist, Accountability PIN, DNS VPN, AI Vision & Always-On Security",
    icon = Icons.Default.Shield,
    iconColor = IceBlue
  ),
  PERMISSIONS_STATUS(
    title = "Permissions & System Status",
    hindiTitle = "अनुमतियां एवं सिस्टम स्थिति",
    description = "Accessibility, Device Admin, Battery optimization & Diagnostics",
    icon = Icons.Default.Healing,
    iconColor = GoldBrass
  )
}
