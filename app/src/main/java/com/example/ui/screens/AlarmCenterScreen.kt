package com.example.ui.screens

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmAudioPlayer
import com.example.alarm.AlarmScheduler
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
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.BatteryOptimizationHelper
import com.example.util.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlarmCenterScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val wakeUpMinutes by viewModel.wakeUpMinutes.collectAsState()
  val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
  val alarmSoundType by viewModel.alarmSoundType.collectAsState()
  val customSoundUri by viewModel.customSoundUri.collectAsState()
  val customSoundTitle by viewModel.customSoundTitle.collectAsState()
  val alarmVolume by viewModel.alarmVolume.collectAsState()
  val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
  val alarmRepeatMode by viewModel.alarmRepeatMode.collectAsState()
  val alarmCustomDaysMask by viewModel.alarmCustomDaysMask.collectAsState()
  val smartReminderMinutes by viewModel.smartReminderMinutes.collectAsState()
  val todayTasks by viewModel.tasksForSelectedDate.collectAsState()
  val routineTasks = remember(todayTasks) { todayTasks.filter { !it.isExtra } }

  var showTimePicker by remember { mutableStateOf(false) }
  var isTestingSound by remember { mutableStateOf(false) }
  var testVolumeSlider by remember(alarmVolume) { mutableFloatStateOf(alarmVolume) }
  var isBatteryOptimized by remember {
    mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
  }

  // System ringtone picker
  val ringtonePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    if (uri != null) {
      val ringtone = RingtoneManager.getRingtone(context, uri)
      val title = ringtone?.getTitle(context) ?: "Custom System Tone"
      viewModel.updateAlarmSound("SYSTEM", uri.toString(), title)
      Toast.makeText(context, "अलार्म टोन सेट: $title", Toast.LENGTH_SHORT).show()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      if (AlarmAudioPlayer.isPlaying()) {
        AlarmAudioPlayer.stop()
      }
    }
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
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Top Bar
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = { viewModel.navigateBack() },
            modifier = Modifier.size(42.dp)
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
              text = "ALARM CENTER • अलार्म केंद्र",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "Routine Alarms & Reminders",
              color = TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold
            )
          }
        }
      }

      // 2. Master Alarm System Switch & Health Banner
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(GlassGradient)
            .border(
              1.dp,
              if (isAlarmEnabled) CyanNeon.copy(alpha = 0.5f) else DarkSurfaceBorder,
              RoundedCornerShape(22.dp)
            )
            .padding(18.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isAlarmEnabled) CyanNeon.copy(alpha = 0.16f) else DarkSurfaceBorder),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = if (isAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (isAlarmEnabled) CyanNeon else TextMuted,
                    modifier = Modifier.size(24.dp)
                  )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                  Text(
                    text = "Master Alarm System",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = if (isAlarmEnabled) "सक्रिय (Alarms & reminders will ring)" else "निष्क्रिय (All alarms turned off)",
                    color = if (isAlarmEnabled) StatusComplete else TextMuted,
                    fontSize = 12.sp
                  )
                }
              }

              Switch(
                checked = isAlarmEnabled,
                onCheckedChange = { checked ->
                  viewModel.toggleAlarm(context, checked)
                  if (checked) {
                    AlarmScheduler.rescheduleAllTimetableAlarms(context)
                  } else {
                    AlarmScheduler.cancelAlarm(context)
                    AlarmScheduler.cancelAllTimetableAlarms(context)
                  }
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = DarkBackground,
                  checkedTrackColor = CyanNeon,
                  uncheckedThumbColor = TextMuted,
                  uncheckedTrackColor = DarkSurface
                ),
                modifier = Modifier.testTag("alarm_master_switch")
              )
            }

            // Battery Optimization Warning if needed
            if (!isBatteryOptimized) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                  .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(
                      imageVector = Icons.Default.BatteryAlert,
                      contentDescription = null,
                      tint = Color(0xFFFFB300),
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Fix background restriction for 100% on-time alarm ringing.",
                      color = TextPrimary,
                      fontSize = 11.sp
                    )
                  }
                  OutlinedButton(
                    onClick = {
                      BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                      isBatteryOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Text("Whitelist", fontSize = 11.sp, color = Color(0xFFFFB300))
                  }
                }
              }
            }
          }
        }
      }

      // 3. Wake-Up Morning Alarm Card
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Morning Wake-Up Alarm",
                  color = TextPrimary,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "रोज सुबह जागने का मुख्य अलार्म",
                  color = TextMuted,
                  fontSize = 11.sp
                )
              }

              // Time Button (12-hour format)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(CyanNeon.copy(alpha = 0.12f))
                  .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                  .clickable { showTimePicker = true }
                  .padding(horizontal = 14.dp, vertical = 8.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = TimeUtils.minutesTo12Hour(wakeUpMinutes),
                    color = CyanNeon,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                  )
                }
              }
            }

            // Repeat Mode Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Repeat Schedule (दोहराव):",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                listOf("DAILY" to "Everyday", "WEEKDAYS" to "Mon–Fri", "CUSTOM" to "Custom").forEach { (mode, label) ->
                  val isSelected = alarmRepeatMode == mode
                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .background(if (isSelected) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                      .border(
                        1.dp,
                        if (isSelected) CyanNeon else DarkSurfaceBorder,
                        RoundedCornerShape(10.dp)
                      )
                      .clickable {
                        viewModel.updateAlarmRepeat(mode, alarmCustomDaysMask, context)
                      }
                      .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = label,
                      color = if (isSelected) CyanNeon else TextSecondary,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  }
                }
              }

              // Custom Days Selector if CUSTOM is selected
              AnimatedVisibility(visible = alarmRepeatMode == "CUSTOM") {
                val dayLetters = listOf("S" to 1, "M" to 2, "T" to 4, "W" to 8, "T" to 16, "F" to 32, "S" to 64)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  dayLetters.forEach { (name, maskVal) ->
                    val isDaySelected = (alarmCustomDaysMask and maskVal) != 0
                    Box(
                      modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDaySelected) VioletNeon.copy(alpha = 0.25f) else DarkSurface)
                        .border(1.dp, if (isDaySelected) VioletNeon else DarkSurfaceBorder, CircleShape)
                        .clickable {
                          val newMask = if (isDaySelected) alarmCustomDaysMask and maskVal.inv() else alarmCustomDaysMask or maskVal
                          viewModel.updateAlarmRepeat("CUSTOM", newMask, context)
                        },
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = name,
                        color = if (isDaySelected) VioletNeon else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }

            // Snooze Duration
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Snooze Duration (स्नूज़):",
                color = TextSecondary,
                fontSize = 12.sp
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 10, 15).forEach { mins ->
                  val isSelected = snoozeMinutes == mins
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (isSelected) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                      .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                      .clickable { viewModel.updateAlarmSnoozeMinutes(mins) }
                      .padding(horizontal = 10.dp, vertical = 5.dp)
                  ) {
                    Text(
                      text = "$mins m",
                      color = if (isSelected) CyanNeon else TextSecondary,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 4. Smart Pre-Reminder (Feature 2)
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "Smart Pre-Reminder",
                  color = TextPrimary,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "गतिविधि शुरू होने से पहले सूचना",
                  color = TextMuted,
                  fontSize = 11.sp
                )
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (smartReminderMinutes > 0) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (smartReminderMinutes > 0) "$smartReminderMinutes min before" else "Off",
                  color = if (smartReminderMinutes > 0) CyanNeon else TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              listOf(0 to "Off", 5 to "5 min", 10 to "10 min", 15 to "15 min", 30 to "30 min").forEach { (mins, label) ->
                val isSelected = smartReminderMinutes == mins
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                    .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable {
                      viewModel.updateSmartReminderMinutes(mins)
                      AlarmScheduler.rescheduleAllTimetableAlarms(context)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                  Text(
                    text = label,
                    color = if (isSelected) CyanNeon else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }
          }
        }
      }

      // 5. Sound, Tone & Vibration Settings
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
              text = "Sound & Vibration Tone",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )

            // Sound Type Selector
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                "DEFAULT" to "High-Tone Digital",
                "SYSTEM" to "System Ringtone",
                "CUSTOM" to "Custom File"
              ).forEach { (type, label) ->
                val isSelected = alarmSoundType == type
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) VioletNeon.copy(alpha = 0.15f) else DarkSurface)
                    .border(1.dp, if (isSelected) VioletNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                    .clickable {
                      if (type == "SYSTEM") {
                        val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                          putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                          putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                        }
                        ringtonePickerLauncher.launch(intent)
                      } else {
                        viewModel.updateAlarmSound(type, customSoundUri, if (type == "DEFAULT") "High-Tone Digital Alarm" else customSoundTitle)
                      }
                    }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    color = if (isSelected) VioletNeon else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                  )
                }
              }
            }

            // Volume Slider & Test Button
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Alarm Volume: ${(testVolumeSlider * 100).roundToInt()}%",
                    color = TextSecondary,
                    fontSize = 12.sp
                  )
                }

                // Test Button
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isTestingSound) StatusMissed.copy(alpha = 0.2f) else CyanNeon.copy(alpha = 0.12f))
                    .clickable {
                      if (isTestingSound) {
                        AlarmAudioPlayer.stop()
                        isTestingSound = false
                      } else {
                        isTestingSound = true
                        AlarmAudioPlayer.start(
                          context = context,
                          soundType = alarmSoundType,
                          customUriString = customSoundUri,
                          volume = testVolumeSlider,
                          enableVibration = isVibrationEnabled
                        )
                        scope.launch {
                          delay(3500L)
                          AlarmAudioPlayer.stop()
                          isTestingSound = false
                        }
                      }
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = if (isTestingSound) Icons.Default.Stop else Icons.Default.PlayArrow,
                      contentDescription = null,
                      tint = if (isTestingSound) StatusMissed else CyanNeon,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = if (isTestingSound) "Stop" else "Test Tone",
                      color = if (isTestingSound) StatusMissed else CyanNeon,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              Slider(
                value = testVolumeSlider,
                onValueChange = { testVolumeSlider = it },
                onValueChangeFinished = { viewModel.updateAlarmVolume(testVolumeSlider) },
                colors = SliderDefaults.colors(
                  thumbColor = CyanNeon,
                  activeTrackColor = CyanNeon,
                  inactiveTrackColor = DarkSurface
                )
              )
            }

            // Vibration Switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Vibration,
                  contentDescription = null,
                  tint = TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "Vibration Alert (कंपन)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                  )
                  Text(
                    text = "Heavy pulsing haptic vibration",
                    color = TextMuted,
                    fontSize = 10.sp
                  )
                }
              }

              Switch(
                checked = isVibrationEnabled,
                onCheckedChange = { viewModel.updateVibrationEnabled(it) },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = DarkBackground,
                  checkedTrackColor = CyanNeon,
                  uncheckedThumbColor = TextMuted,
                  uncheckedTrackColor = DarkSurface
                )
              )
            }
          }
        }
      }

      // 6. Today's Scheduled Routine Alarms (Timetable Alarms)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Today's Timetable Alarms (${routineTasks.size})",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = if (isAlarmEnabled) "Active" else "Muted",
              color = if (isAlarmEnabled) StatusComplete else TextMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          if (routineTasks.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurfaceElevated)
                .padding(20.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "आज कोई टाइमटेबल गतिविधि शेड्यूल नहीं है।",
                color = TextMuted,
                fontSize = 13.sp
              )
            }
          } else {
            routineTasks.forEach { task ->
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
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CyanNeon.copy(alpha = 0.1f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = task.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                      Text(
                        text = "${TimeUtils.minutesTo12Hour(task.timeMinutes)} • ${task.category}",
                        color = TextMuted,
                        fontSize = 11.sp
                      )
                    }
                  }

                  Text(
                    text = if (isAlarmEnabled) "Scheduled" else "Off",
                    color = if (isAlarmEnabled) StatusComplete else TextMuted,
                    fontSize = 11.sp,
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

  // 12-Hour Time Picker Dialog
  if (showTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = wakeUpMinutes,
      onConfirm = { minutes ->
        viewModel.updateWakeUpSchedule(context, minutes, isAlarmEnabled)
        showTimePicker = false
      },
      onDismiss = { showTimePicker = false }
    )
  }
}
