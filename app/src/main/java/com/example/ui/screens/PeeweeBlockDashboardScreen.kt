package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.AccountabilityPinManager
import com.example.focus.DayStatBreakdown
import com.example.focus.DayStats
import com.example.focus.FocusModeManager
import com.example.focus.LifeTrackerAdminReceiver
import com.example.focus.LocalDnsVpnService
import com.example.focus.MonthlyStats
import com.example.focus.PeeweeProtectionManager
import com.example.focus.PeeweeStatsManager
import com.example.focus.ProtectionPauseManager
import com.example.focus.ProtectionStatus
import com.example.focus.ProtectionWatchdogService
import com.example.focus.WeeklyStats
import com.example.shortcontent.AccessibilityHelper
import com.example.ui.components.AccountabilityPinDialog
import com.example.ui.components.AccountabilityPinDialogMode
import com.example.ui.components.AdultBlocklistDialog
import com.example.ui.components.PeeweePermissionChecklistDialog
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VpnKey
import kotlinx.coroutines.delay

private val CyanAccent = Color(0xFF00F0FF)
private val PurpleAccent = Color(0xFFA855F7)
private val DarkBg = Color(0xFF090D16)
private val CardBg = Color(0xFF131826)
private val CardBorder = Color(0xFF222C42)
private val TextWhite = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF9CA3AF)
private val RedAlert = Color(0xFFEF4444)
private val GreenActive = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeeweeBlockDashboardScreen(
  onOpenSettings: () -> Unit = {},
  onOpenHealthLog: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Live service & protection manager states
  val protectionReport by PeeweeProtectionManager.protectionStateFlow.collectAsState()
  val activeReport = protectionReport ?: remember { PeeweeProtectionManager.evaluateProtectionState(context) }
  val isVpnRunning by LocalDnsVpnService.isVpnRunning.collectAsState()
  var isAccessibilityActive by remember { mutableStateOf(AccessibilityHelper.isAccessibilityServiceEnabled(context)) }
  var isDeviceAdminActive by remember { mutableStateOf(LifeTrackerAdminReceiver.isDeviceAdminActive(context)) }
  var isStrictLockActive by remember { mutableStateOf(FocusModeManager.isStrictLockActive(context)) }
  var strictLockRemainingMins by remember {
    mutableStateOf(((FocusModeManager.getStrictLockRemainingMillis(context) / 60000L).coerceAtLeast(1)).toInt())
  }
  var isPinSet by remember { mutableStateOf(AccountabilityPinManager.isPinSet(context)) }

  // Statistics from PeeweeStatsManager
  val statsTrigger by PeeweeStatsManager.statsUpdateTrigger.collectAsState()
  var todayStats by remember(statsTrigger) { mutableStateOf(PeeweeStatsManager.getTodayStats(context)) }
  var weeklyStats by remember(statsTrigger) { mutableStateOf(PeeweeStatsManager.getWeeklyStats(context)) }
  var monthlyStats by remember(statsTrigger) { mutableStateOf(PeeweeStatsManager.getMonthlyStats(context)) }

  // Periodic polling for status refresh
  LaunchedEffect(Unit) {
    while (true) {
      PeeweeProtectionManager.evaluateProtectionState(context)
      isAccessibilityActive = AccessibilityHelper.isAccessibilityServiceEnabled(context)
      isDeviceAdminActive = LifeTrackerAdminReceiver.isDeviceAdminActive(context)
      isStrictLockActive = FocusModeManager.isStrictLockActive(context)
      strictLockRemainingMins = ((FocusModeManager.getStrictLockRemainingMillis(context) / 60000L).coerceAtLeast(1)).toInt()
      isPinSet = AccountabilityPinManager.isPinSet(context)
      todayStats = PeeweeStatsManager.getTodayStats(context)
      weeklyStats = PeeweeStatsManager.getWeeklyStats(context)
      monthlyStats = PeeweeStatsManager.getMonthlyStats(context)
      delay(2500)
    }
  }

  // Dialog & Sheet state
  var showBlocklistDialog by remember { mutableStateOf(false) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showChecklistDialog by remember { mutableStateOf(false) }
  var showStrictLockDialog by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var pinDialogMode by remember { mutableStateOf(AccountabilityPinDialogMode.INITIAL_SETUP) }
  var showResetConfirmDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  // VPN launcher
  val vpnPrepareLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      LocalDnsVpnService.start(context)
      PeeweeProtectionManager.evaluateProtectionState(context)
      Toast.makeText(context, "Peewee Block DNS Filter active", Toast.LENGTH_SHORT).show()
    } else {
      PeeweeProtectionManager.evaluateProtectionState(context)
      Toast.makeText(context, "VPN permission required for local DNS filtering.", Toast.LENGTH_LONG).show()
    }
  }

  val isMasterProtectionOn = isVpnRunning || isAccessibilityActive

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .statusBarsPadding()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. TOP HEADER & BRANDING
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF151A2E))
              .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "Peewee Block Icon",
              tint = CyanAccent,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Peewee Block",
              color = TextWhite,
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Website & Adult Content Blocker",
              color = TextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }

        IconButton(
          onClick = { showSettingsSheet = true },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CardBg)
            .border(1.dp, CardBorder, CircleShape)
            .testTag("dashboard_settings_button")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = CyanAccent,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // 2. STRICT LOCK ALERT BANNER (If Active)
    if (isStrictLockActive) {
      item {
        val hours = strictLockRemainingMins / 60
        val mins = strictLockRemainingMins % 60
        val remainingText = if (hours > 0) "${hours}h ${mins}m" else "$strictLockRemainingMins minutes"

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2C1014))
            .border(1.5.dp, RedAlert, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("strict_lock_alert_banner")
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(RedAlert.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = RedAlert,
                  modifier = Modifier.size(22.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Strict Lock Active",
                  color = RedAlert,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "$remainingText remaining • Protection locked",
                  color = Color(0xFFFFB4AB),
                  fontSize = 12.sp
                )
              }
            }

            if (isPinSet) {
              OutlinedButton(
                onClick = {
                  pinDialogMode = AccountabilityPinDialogMode.VERIFY_ONLY
                  showPinDialog = true
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                  .height(34.dp)
                  .testTag("strict_lock_unlock_button")
              ) {
                Text("Unlock (PIN)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 3. MAIN PROTECTION CONTROL CARD (HONEST 5-STATE DASHBOARD)
    item {
      val statusColor = Color(activeReport.status.badgeColorHex)

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(CardBg)
          .border(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.45f),
            shape = RoundedCornerShape(18.dp)
          )
          .padding(18.dp)
          .testTag("main_protection_card")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          // Status Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "HONEST PROTECTION STATUS",
                  color = TextMuted,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp
                )
              }
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = activeReport.status.label,
                color = statusColor,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
              )
            }

            // Primary Toggle Switch
            Switch(
              checked = activeReport.isMasterEnabled && activeReport.status != ProtectionStatus.PROTECTION_DISABLED,
              enabled = true,
              onCheckedChange = { enable ->
                if (enable) {
                  PeeweeProtectionManager.setMasterProtectionEnabled(context, true)
                  val vpnIntent = VpnService.prepare(context)
                  if (vpnIntent != null) {
                    vpnPrepareLauncher.launch(vpnIntent)
                  } else {
                    LocalDnsVpnService.start(context)
                  }
                  if (!isAccessibilityActive) {
                    AccessibilityHelper.openAccessibilitySettings(context)
                  }
                  ProtectionWatchdogService.start(context)
                  PeeweeProtectionManager.evaluateProtectionState(context)
                } else {
                  if (isStrictLockActive) {
                    if (isPinSet) {
                      pinDialogMode = AccountabilityPinDialogMode.VERIFY_ONLY
                      showPinDialog = true
                    } else {
                      Toast.makeText(context, "Strict Lock is active. Ordinary controls cannot disable it without a configured PIN.", Toast.LENGTH_LONG).show()
                    }
                  } else if (isPinSet) {
                    pinDialogMode = AccountabilityPinDialogMode.VERIFY_ONLY
                    showPinDialog = true
                  } else {
                    PeeweeProtectionManager.setMasterProtectionEnabled(context, false)
                    LocalDnsVpnService.stop(context, pinVerified = true)
                    PeeweeProtectionManager.evaluateProtectionState(context)
                  }
                }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = statusColor,
                checkedTrackColor = statusColor.copy(alpha = 0.35f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color(0xFF1F293D)
              ),
              modifier = Modifier.testTag("protection_master_switch")
            )
          }

          // Status Description
          Text(
            text = activeReport.status.description,
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )

          // Last Check Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF182033))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Last verified: ${activeReport.formattedLastCheck}",
                color = TextWhite.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }

            IconButton(
              onClick = {
                PeeweeProtectionManager.evaluateProtectionState(context)
                Toast.makeText(context, "Protection verified", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Verify Now",
                tint = CyanAccent,
                modifier = Modifier.size(15.dp)
              )
            }
          }

          // Action Required Banner (if any action is needed)
          if (activeReport.actionRequired != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Action Required",
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                Text(
                  text = activeReport.actionRequired!!,
                  color = TextWhite.copy(alpha = 0.9f),
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                )

                if (activeReport.status == ProtectionStatus.PROTECTION_PAUSED) {
                  Button(
                    onClick = {
                      ProtectionPauseManager.resumeProtection(context)
                      PeeweeProtectionManager.evaluateProtectionState(context)
                      Toast.makeText(context, "Protection resumed immediately.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(34.dp)
                  ) {
                    Text("Resume Protection Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                } else {
                  Button(
                    onClick = { showChecklistDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = statusColor, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(34.dp)
                  ) {
                    Text("Resolve in Checklist →", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          // Service Status Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StatusPill(
              label = "DNS Filter",
              isActive = isVpnRunning,
              onClick = {
                if (!isVpnRunning) {
                  val vpnIntent = VpnService.prepare(context)
                  if (vpnIntent != null) vpnPrepareLauncher.launch(vpnIntent) else LocalDnsVpnService.start(context)
                }
              },
              modifier = Modifier.weight(1f)
            )

            StatusPill(
              label = "Accessibility",
              isActive = isAccessibilityActive,
              onClick = {
                if (!isAccessibilityActive) AccessibilityHelper.openAccessibilitySettings(context)
              },
              modifier = Modifier.weight(1.1f)
            )

            StatusPill(
              label = "Anti-Tamper",
              isActive = isDeviceAdminActive,
              onClick = {
                if (!isDeviceAdminActive) {
                  val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(
                      android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                      LifeTrackerAdminReceiver.getComponentName(context)
                    )
                    putExtra(
                      android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                      "Protect Peewee Block from accidental removal during Strict Lock."
                    )
                  }
                  context.startActivity(intent)
                }
              },
              modifier = Modifier.weight(1.1f)
            )
          }

          // Primary Actions Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { showChecklistDialog = true },
              modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("protection_checklist_button"),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
              border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Checklist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                if (activeReport.status == ProtectionStatus.PROTECTION_DISABLED) {
                  PeeweeProtectionManager.setMasterProtectionEnabled(context, true)
                  val vpnIntent = VpnService.prepare(context)
                  if (vpnIntent != null) vpnPrepareLauncher.launch(vpnIntent) else LocalDnsVpnService.start(context)
                  if (!isAccessibilityActive) AccessibilityHelper.openAccessibilitySettings(context)
                  ProtectionWatchdogService.start(context)
                  PeeweeProtectionManager.evaluateProtectionState(context)
                } else if (activeReport.status == ProtectionStatus.PROTECTION_PAUSED) {
                  ProtectionPauseManager.resumeProtection(context)
                  PeeweeProtectionManager.evaluateProtectionState(context)
                } else if (activeReport.status == ProtectionStatus.PROTECTION_ACTIVE) {
                  showChecklistDialog = true
                } else {
                  showChecklistDialog = true
                }
              },
              modifier = Modifier
                .weight(1.6f)
                .height(44.dp)
                .testTag("protection_action_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (activeReport.status == ProtectionStatus.PROTECTION_ACTIVE) Color(0xFF1E283D) else statusColor
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(
                imageVector = if (activeReport.status == ProtectionStatus.PROTECTION_ACTIVE) Icons.Default.Shield else Icons.Default.Security,
                contentDescription = null,
                tint = if (activeReport.status == ProtectionStatus.PROTECTION_ACTIVE) CyanAccent else Color(0xFF090D16),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when (activeReport.status) {
                  ProtectionStatus.PROTECTION_ACTIVE -> "Active (View Checklist)"
                  ProtectionStatus.PROTECTION_PAUSED -> "Resume Protection"
                  ProtectionStatus.PROTECTION_DISABLED -> "Turn Protection ON"
                  else -> "Fix Protection"
                },
                color = if (activeReport.status == ProtectionStatus.PROTECTION_ACTIVE) TextWhite else Color(0xFF090D16),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 4. TODAY'S STATISTICS (3 COMPACT CARDS)
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "TODAY'S STATISTICS",
          color = TextMuted,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          StatCard(
            title = "Block Attempts",
            value = "${todayStats.blockAttempts}",
            subtitle = "Detected today",
            accentColor = CyanAccent,
            modifier = Modifier.weight(1f)
          )

          StatCard(
            title = "Strict Lock",
            value = "${todayStats.strictLockActivations}",
            subtitle = "Activations today",
            accentColor = PurpleAccent,
            modifier = Modifier.weight(1f)
          )

          StatCard(
            title = "Blocked",
            value = "${todayStats.blockedAttempts}",
            subtitle = "Stopped safely",
            accentColor = GreenActive,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // 5. WEEKLY STATISTICS SECTION
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(CardBg)
          .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
          .padding(16.dp)
          .testTag("weekly_stats_card")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "WEEKLY SUMMARY",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
              Text(
                text = "${weeklyStats.totalBlockedAttempts} Blocked This Week",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PurpleAccent.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "${weeklyStats.strictLockActivations} Strict Locks",
                color = PurpleAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // 7-Day breakdown bars
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
          ) {
            val maxDayBlocked = weeklyStats.dailyBreakdown.maxOfOrNull { it.blockedCount }?.coerceAtLeast(1) ?: 1

            weeklyStats.dailyBreakdown.forEach { day ->
              DailyBarItem(
                day = day,
                maxCount = maxDayBlocked,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }
    }

    // 6. MONTHLY STATISTICS SECTION
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(CardBg)
          .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
          .padding(16.dp)
          .testTag("monthly_stats_card")
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "MONTHLY OVERVIEW (${monthlyStats.monthName})",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              )
              Text(
                text = "${monthlyStats.totalBlockedAttempts} Total Blocked",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CyanAccent.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "${monthlyStats.strictLockActivations} Strict Locks",
                color = CyanAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // Weekly trend rows
          val maxWeekCount = monthlyStats.weeklyTrend.maxOrNull()?.coerceAtLeast(1) ?: 1
          monthlyStats.weeklyTrend.forEachIndexed { index, count ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Week ${index + 1}",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.width(55.dp)
              )
              Box(
                modifier = Modifier
                  .weight(1f)
                  .height(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(Color(0xFF1E283D))
              ) {
                val fraction = if (maxWeekCount > 0) (count.toFloat() / maxWeekCount).coerceIn(0f, 1f) else 0f
                Box(
                  modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyanAccent)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "$count",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.End
              )
            }
          }
        }
      }
    }

    // 7. DIRECT SHORTCUT ACTIONS
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { showBlocklistDialog = true },
          modifier = Modifier
            .weight(1f)
            .height(44.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CardBg),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
          Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Blocklist", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Button(
          onClick = { showSettingsSheet = true },
          modifier = Modifier
            .weight(1f)
            .height(44.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CardBg),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = PurpleAccent, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Settings", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }

  // DIALOGS & BOTTOM SHEET:

  if (showBlocklistDialog) {
    AdultBlocklistDialog(onDismiss = { showBlocklistDialog = false })
  }

  if (showChecklistDialog) {
    PeeweePermissionChecklistDialog(onDismiss = { showChecklistDialog = false })
  }

  if (showPinDialog) {
    AccountabilityPinDialog(
      onDismiss = { showPinDialog = false },
      mode = pinDialogMode,
      actionTitle = if (pinDialogMode == AccountabilityPinDialogMode.INITIAL_SETUP) "Setup Accountability PIN" else "Verify PIN",
      onActionApproved = {
        showPinDialog = false
        isPinSet = AccountabilityPinManager.isPinSet(context)
        if (pinDialogMode == AccountabilityPinDialogMode.VERIFY_ONLY) {
          if (isStrictLockActive) {
            FocusModeManager.deactivateFocusMode(context, force = true)
            isStrictLockActive = false
            PeeweeProtectionManager.evaluateProtectionState(context)
            Toast.makeText(context, "Strict Lock unlocked with PIN.", Toast.LENGTH_SHORT).show()
          } else {
            PeeweeProtectionManager.setMasterProtectionEnabled(context, false)
            LocalDnsVpnService.stop(context, pinVerified = true)
            PeeweeProtectionManager.evaluateProtectionState(context)
            Toast.makeText(context, "Protection disabled with PIN.", Toast.LENGTH_SHORT).show()
          }
        }
      }
    )
  }

  // STRICT LOCK DURATION PICKER DIALOG (15m, 1h, 24h)
  if (showStrictLockDialog) {
    AlertDialog(
      onDismissRequest = { showStrictLockDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = RedAlert, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Strict Lock Protection", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Strict Lock hardens adult content blocking and uninstall protection. While active, protection cannot be disabled without entering your configured PIN, and access to uninstalling Peewee Block is locked.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text("Choose Protection Duration:", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)

          val durations = listOf(
            15 to "15 Minutes (Emergency Cooldown)",
            60 to "1 Hour (Intensive Focus)",
            1440 to "24 Hours (Full-Day Anti-Relapse Lock)"
          )

          durations.forEach { (mins, label) ->
            OutlinedButton(
              onClick = {
                showStrictLockDialog = false
                FocusModeManager.activateStrictLock(
                  context = context,
                  durationMinutes = mins,
                  reason = "Manual Strict Lock ($label)",
                  packageName = context.packageName
                )
                PeeweeStatsManager.recordStrictLockActivation(context)
                isStrictLockActive = true
                strictLockRemainingMins = mins
                PeeweeProtectionManager.evaluateProtectionState(context)
                val durationDisplay = if (mins >= 60) "${mins / 60} hours" else "$mins minutes"
                Toast.makeText(context, "Strict Lock active for $durationDisplay.", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
              border = androidx.compose.foundation.BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showStrictLockDialog = false }) {
          Text("Cancel", color = TextMuted)
        }
      },
      containerColor = CardBg,
      shape = RoundedCornerShape(16.dp)
    )
  }

  // MINIMAL SETTINGS BOTTOM SHEET
  if (showSettingsSheet) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      sheetState = sheetState,
      containerColor = CardBg,
      contentColor = TextWhite
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Sheet Title
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Peewee Block Settings",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          IconButton(onClick = { showSettingsSheet = false }) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        // Setting 0: Protection Checklist & Background Persistence
        SettingsRowItem(
          icon = Icons.Default.VpnKey,
          iconTint = GreenActive,
          title = "Protection Checklist & Permissions",
          subtitle = "VPN, Accessibility, Notifications & Battery setup",
          actionText = "Setup →",
          onClick = {
            showSettingsSheet = false
            showChecklistDialog = true
          }
        )

        // Setting 1: Strict Lock Mode (15m / 1h / 24h)
        SettingsRowItem(
          icon = Icons.Default.Lock,
          iconTint = if (isStrictLockActive) RedAlert else CyanAccent,
          title = "Strict Lock Mode",
          subtitle = if (isStrictLockActive) {
            val hours = strictLockRemainingMins / 60
            val mins = strictLockRemainingMins % 60
            val remText = if (hours > 0) "${hours}h ${mins}m" else "$strictLockRemainingMins mins"
            "Active ($remText remaining • Tap to unlock with PIN)"
          } else {
            "Lock protection for 15 minutes, 1 hour, or 24 hours"
          },
          actionText = if (isStrictLockActive) "Unlock →" else "Configure →",
          onClick = {
            showSettingsSheet = false
            if (isStrictLockActive) {
              if (isPinSet) {
                pinDialogMode = AccountabilityPinDialogMode.VERIFY_ONLY
                showPinDialog = true
              } else {
                Toast.makeText(context, "Strict Lock active. Set an Accountability PIN to unlock early, or wait for timer.", Toast.LENGTH_SHORT).show()
              }
            } else {
              showStrictLockDialog = true
            }
          }
        )

        // Setting 2: Blocklist Shortcut
        SettingsRowItem(
          icon = Icons.Default.Security,
          iconTint = CyanAccent,
          title = "Blocked Websites & Keywords",
          subtitle = "150,000+ domains, custom blocklist, and safe words",
          actionText = "Manage →",
          onClick = {
            showSettingsSheet = false
            showBlocklistDialog = true
          }
        )

        // Setting 3: Accountability PIN
        SettingsRowItem(
          icon = Icons.Default.Lock,
          iconTint = if (isPinSet) GreenActive else PurpleAccent,
          title = "Accountability PIN",
          subtitle = if (isPinSet) "PIN is active (Required to change settings)" else "No PIN set (Set a PIN to lock settings)",
          actionText = if (isPinSet) "Change →" else "Set PIN →",
          onClick = {
            pinDialogMode = if (isPinSet) AccountabilityPinDialogMode.CHANGE_PIN else AccountabilityPinDialogMode.INITIAL_SETUP
            showSettingsSheet = false
            showPinDialog = true
          }
        )

        // Setting 3: Privacy & Data Information
        SettingsRowItem(
          icon = Icons.Default.Info,
          iconTint = CyanAccent,
          title = "Privacy & Local Data",
          subtitle = "Zero tracking, 100% on-device, no history uploaded",
          actionText = "Details →",
          onClick = { showPrivacyDialog = true }
        )

        // Setting 4: Reset Statistics
        SettingsRowItem(
          icon = Icons.Default.DeleteOutline,
          iconTint = RedAlert,
          title = "Reset Statistics",
          subtitle = "Clear today, weekly, and monthly block counts",
          actionText = "Reset",
          onClick = { showResetConfirmDialog = true }
        )

        // Version & Diagnostics Footer
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Peewee Block v1.0.1 • 100% On-Device",
            color = TextMuted,
            fontSize = 11.sp
          )

          TextButton(
            onClick = {
              showSettingsSheet = false
              onOpenHealthLog()
            }
          ) {
            Text("Health Logs →", color = CyanAccent, fontSize = 12.sp)
          }
        }
      }
    }
  }

  // RESET CONFIRMATION DIALOG
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = { Text("Reset Statistics?") },
      text = { Text("This will clear all daily, weekly, and monthly block attempt counts. This action cannot be undone.") },
      confirmButton = {
        Button(
          onClick = {
            PeeweeStatsManager.resetStatistics(context)
            showResetConfirmDialog = false
            Toast.makeText(context, "Statistics reset successfully.", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
        ) {
          Text("Confirm Reset")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text("Cancel", color = TextMuted)
        }
      },
      containerColor = CardBg,
      titleContentColor = TextWhite,
      textContentColor = TextMuted
    )
  }

  // PRIVACY INFO DIALOG
  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy & On-Device Blocking") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("• 100% Local DNS: Domain filtering runs on device loopback 127.0.0.1. Zero external filtering servers are used.")
          Text("• Zero Data Collection: Your browsing history, visited URLs, and search queries are never logged, stored, or transmitted anywhere.")
          Text("• Offline Safety: Statistics are stored strictly on your local device storage.")
          Text("• Android Limitations: Websites with custom DNS-over-HTTPS (DoH) in browser settings may require disabling DoH in Chrome/Brave for network-level blocking. Accessibility inspection provides backup coverage.")
        }
      },
      confirmButton = {
        Button(
          onClick = { showPrivacyDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
        ) {
          Text("Got It", color = Color(0xFF090D16), fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardBg,
      titleContentColor = TextWhite,
      textContentColor = TextMuted
    )
  }
}

@Composable
private fun StatCard(
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(CardBg)
      .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
      .padding(12.dp)
  ) {
    Column {
      Text(
        text = title,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        color = accentColor,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        color = TextMuted.copy(alpha = 0.7f),
        fontSize = 9.sp,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun StatusPill(
  label: String,
  isActive: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFF181F30))
      .border(
        width = 1.dp,
        color = if (isActive) GreenActive.copy(alpha = 0.4f) else Color(0xFF2E384D),
        shape = RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 8.dp, vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(if (isActive) GreenActive else Color(0xFFFFB4AB))
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        color = if (isActive) TextWhite else TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun DailyBarItem(
  day: DayStatBreakdown,
  maxCount: Int,
  modifier: Modifier = Modifier
) {
  val heightFraction = if (maxCount > 0) (day.blockedCount.toFloat() / maxCount).coerceIn(0.1f, 1f) else 0.1f

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Bottom
  ) {
    Text(
      text = "${day.blockedCount}",
      color = if (day.blockedCount > 0) CyanAccent else TextMuted,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .width(14.dp)
        .height((36 * heightFraction).dp)
        .clip(RoundedCornerShape(3.dp))
        .background(if (day.blockedCount > 0) CyanAccent else Color(0xFF1E283D))
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = day.dayLabel.take(3),
      color = TextMuted,
      fontSize = 10.sp
    )
  }
}

@Composable
private fun SettingsRowItem(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  actionText: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF181F30))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(34.dp)
          .clip(CircleShape)
          .background(iconTint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(text = title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(text = subtitle, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
    }
    Text(text = actionText, color = iconTint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
  }
}
