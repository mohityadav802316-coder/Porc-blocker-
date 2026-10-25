package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.FocusModeManager
import com.example.shortcontent.AccessibilityHelper
import com.example.ui.components.AllowedAppsDialog
import com.example.ui.components.EmergencyRecoveryBottomSheet
import com.example.ui.components.MathChallengeDialog
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.TimeUtils
import kotlinx.coroutines.delay

/**
 * Fullscreen System-Wide Focus Restriction Screen (BlockSite-style).
 * Displays live countdown, allowed apps manager, permission alerts,
 * and deliberate 10th-standard Math Challenge unlock to enforce digital discipline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusRestrictionScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  // Enforce discipline: back button cannot dismiss Focus Mode
  BackHandler(enabled = true) {}

  val liveTime by viewModel.currentLiveTime.collectAsState()
  val currentTimeMinutes by viewModel.currentTimeMinutes.collectAsState()
  val isScheduleEnabled by viewModel.isFocusScheduleEnabled.collectAsState()
  val focusStartTimeMinutes by viewModel.focusStartTimeMinutes.collectAsState()
  val focusEndTimeMinutes by viewModel.focusEndTimeMinutes.collectAsState()

  var showExitConfirmationDialog by remember { mutableStateOf(false) }
  var showEmergencySheet by remember { mutableStateOf(false) }
  var showAllowedAppsDialog by remember { mutableStateOf(false) }
  var showPinVerificationForAllowedApps by remember { mutableStateOf(false) }

  var remainingMillis by remember { mutableLongStateOf(FocusModeManager.getRemainingSessionMillis(context)) }
  var isStrictLock by remember { mutableStateOf(FocusModeManager.isStrictLockActive(context)) }
  var strictLockReason by remember { mutableStateOf(FocusModeManager.getStrictLockReason(context)) }

  // Ticker for real-time countdown (every second)
  LaunchedEffect(Unit) {
    while (true) {
      remainingMillis = FocusModeManager.getRemainingSessionMillis(context)
      isStrictLock = FocusModeManager.isStrictLockActive(context)
      if (isStrictLock) {
        strictLockReason = FocusModeManager.getStrictLockReason(context)
      }
      if (remainingMillis <= 0L) {
        FocusModeManager.deactivateFocusMode(context, force = true)
        break
      }
      delay(1000L)
    }
  }

  val hasOverlayPermission = remember { Settings.canDrawOverlays(context) }
  val hasAccessibility = remember { AccessibilityHelper.isAccessibilityServiceEnabled(context) }
  val allowedAppsCount = remember(showAllowedAppsDialog) { FocusModeManager.getAllowedPackages(context).size }

  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianCharcoal)
      .statusBarsPadding()
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag("focus_restriction_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 520.dp)
        .verticalScroll(scrollState),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

      // 1. TOP STATUS BADGE & TIME
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.2f) else GoldBrass.copy(alpha = 0.15f))
            .border(1.dp, if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.6f) else GoldBrass.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isStrictLock) Color(0xFFEF4444) else GoldBrass)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isStrictLock) "🚨 STRICT LOCK ACTIVE • सख्त डिजिटल अनुशासन" else "FOCUS MODE ACTIVE • डिजिटल अनुशासन",
              color = if (isStrictLock) Color(0xFFFF6B6B) else GoldBrass,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.0.sp
            )
          }
        }

        Text(
          text = liveTime.ifBlank { TimeUtils.getNowTimeShort() },
          color = WarmOffWhite,
          fontFamily = ChronoSerifFamily,
          fontSize = 38.sp,
          fontWeight = FontWeight.Bold
        )

        Text(
          text = TimeUtils.formatDateDisplay(TimeUtils.getTodayDateString()),
          color = WarmParchment,
          fontSize = 12.sp
        )
      }

      // 2. PERMISSION HEALTH BANNER (If Accessibility or Overlay missing)
      if (!hasOverlayPermission || !hasAccessibility) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF332010))
            .border(1.dp, Color(0xFFE57A00), RoundedCornerShape(14.dp))
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("सिस्टम अनुमतियाँ आवश्यक", color = Color(0xFFFFB74D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text(
              text = "अन्य ऐप्स पर पूर्ण ब्लॉकिंग स्क्रीन प्रदर्शित करने के लिए एक्सेसिबिलिटी और ओवरले अनुमति सक्रिय करें।",
              color = WarmOffWhite,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (!hasAccessibility) {
                Button(
                  onClick = { AccessibilityHelper.openAccessibilitySettings(context) },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57A00)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("Accessibility चालू करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
              if (!hasOverlayPermission) {
                Button(
                  onClick = {
                    val intent = Intent(
                      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:${context.packageName}")
                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(intent)
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57A00)),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("Overlay अनुमति दें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }

      // 3. MAIN COUNTDOWN & FOCUS CARD
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(ObsidianElevated)
          .border(
            width = 1.4.dp,
            brush = if (isStrictLock) {
              Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFF7F1D1D), Color(0xFFEF4444)))
            } else {
              Brush.linearGradient(listOf(GoldBrass.copy(alpha = 0.5f), ObsidianBorder, GoldDark.copy(alpha = 0.3f)))
            },
            shape = RoundedCornerShape(24.dp)
          )
          .padding(20.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Circular Icon Frame
          Box(
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(
                  if (isStrictLock) listOf(Color(0xFFEF4444).copy(alpha = 0.3f), ObsidianElevated)
                  else listOf(GoldBrass.copy(alpha = 0.25f), ObsidianElevated)
                )
              )
              .border(
                1.dp,
                if (isStrictLock) Color(0xFFEF4444) else GoldBrass.copy(alpha = 0.6f),
                CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isStrictLock) Icons.Default.Shield else Icons.Default.CenterFocusStrong,
              contentDescription = "Focus Icon",
              tint = if (isStrictLock) Color(0xFFFF6B6B) else GoldHighlight,
              modifier = Modifier.size(30.dp)
            )
          }

          // Live Countdown Timer
          if (remainingMillis > 0L) {
            val totalSecs = remainingMillis / 1000L
            val mins = totalSecs / 60
            val secs = totalSecs % 60
            val hrs = mins / 60
            val remMins = mins % 60

            val countdownText = if (hrs > 0) {
              String.format("%d:%02d:%02d", hrs, remMins, secs)
            } else {
              String.format("%02d:%02d", mins, secs)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = countdownText,
                color = WarmOffWhite,
                fontFamily = ChronoSerifFamily,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = if (isStrictLock) "सख्त लॉक समाप्ति तक शेष समय" else "सत्र समाप्ति तक शेष समय",
                color = if (isStrictLock) Color(0xFFFF6B6B) else GoldBrass,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          } else {
            Text(
              text = if (isStrictLock) "सख्त लॉक सक्रिय" else "सक्रिय फोकस सत्र",
              color = WarmOffWhite,
              fontFamily = ChronoSerifFamily,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Strict Reason Callout
          if (isStrictLock) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF3B1515))
                .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = "🚨 $strictLockReason",
                  color = Color(0xFFFFB4AB),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center
                )
                Text(
                  text = "वयस्क / अनुपयुक्त सामग्री पहचान के कारण 15 मिनट का अनिवार्य लॉक",
                  color = WarmOffWhite.copy(alpha = 0.8f),
                  fontSize = 10.sp,
                  textAlign = TextAlign.Center
                )
              }
            }
          }

          // Allowed Apps Pill / Selector Button
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(ObsidianCard)
              .border(0.8.dp, if (isStrictLock) Color(0xFFEF4444).copy(alpha = 0.4f) else GoldBrass.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
              .clickable(enabled = !isStrictLock) {
                if (com.example.focus.AccountabilityPinManager.isPinSet(context)) {
                  showPinVerificationForAllowedApps = true
                } else {
                  showAllowedAppsDialog = true
                }
              }
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Default.Apps,
                contentDescription = null,
                tint = if (isStrictLock) Color(0xFFEF4444) else GoldBrass,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isStrictLock) "अनुमत ऐप्स: $allowedAppsCount (सख्त लॉक में बदलाव बंद 🔒)" else "अनुमत ऐप्स: $allowedAppsCount चुने गए (बदलें)",
                color = if (isStrictLock) WarmParchment else WarmOffWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          // Schedule info if scheduled
          if (isScheduleEnabled) {
            val startFormatted = TimeUtils.minutesTo12Hour(focusStartTimeMinutes)
            val endFormatted = TimeUtils.minutesTo12Hour(focusEndTimeMinutes)
            Text(
              text = "निर्धारित शेड्यूल: $startFormatted – $endFormatted",
              color = WarmParchment,
              fontSize = 11.sp
            )
          }

          Text(
            text = "“गहन अध्ययन व एकाग्रता का समय। अनधिकृत ऐप्स ब्लॉक हैं ताकि आपका ध्यान न भटके।”",
            color = WarmParchment.copy(alpha = 0.9f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
          )
        }
      }

      // 4. EXTEND / SESSION DURATION SHORTCUTS (Only when not in strict lock)
      if (!isStrictLock) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text("सत्र की अवधि निर्धारित / विस्तृत करें:", color = WarmParchment, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(15 to "15m", 25 to "25m (पोमोडोरो)", 45 to "45m", 60 to "1h").forEach { (duration, label) ->
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(ObsidianElevated)
                  .border(0.8.dp, ObsidianBorder, RoundedCornerShape(10.dp))
                  .clickable {
                    FocusModeManager.activateFocusDuration(context, duration)
                    remainingMillis = duration * 60000L
                  }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(label, color = WarmOffWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
              }
            }
          }
        }
      }

      // 5. UNLOCK & EMERGENCY ACTIONS (Strict Lock blocks all early unlocks)
      if (isStrictLock) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF281313))
            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("सख्त लॉक: 15 मिनट अनिवार्य", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(
              text = "वयस्क सामग्री पहचान के कारण यह सत्र बीच में बंद नहीं किया जा सकता। 15 मिनट पूर्ण होने पर फोन स्वतः सामान्य स्थिति में आएगा।",
              color = WarmOffWhite.copy(alpha = 0.85f),
              fontSize = 11.sp,
              textAlign = TextAlign.Center,
              lineHeight = 16.sp
            )
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { showExitConfirmationDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("unlock_focus_button")
          ) {
            Icon(Icons.Default.LockOpen, contentDescription = null, tint = ObsidianElevated, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "फोकस मोड समाप्त करें (Turn Off)",
              color = ObsidianElevated,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }

          OutlinedButton(
            onClick = { showEmergencySheet = true },
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("emergency_recovery_focus_button")
          ) {
            Icon(Icons.Default.Healing, contentDescription = null, tint = SageGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("आपातकालीन रिकवरी मोड", color = WarmParchment, fontSize = 12.sp)
          }
        }
      }
    }
  }

  // DIALOGS
  if (showAllowedAppsDialog) {
    AllowedAppsDialog(
      onDismiss = { showAllowedAppsDialog = false },
      onSave = { showAllowedAppsDialog = false }
    )
  }

  // Clean, clear Focus Mode exit confirmation dialog (no annoying math questions)
  if (showExitConfirmationDialog) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { showExitConfirmationDialog = false },
      containerColor = ObsidianElevated,
      title = {
        Text(
          text = "Focus Mode बंद करना है?",
          color = WarmOffWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Text(
          text = "क्या आप अभी Focus Mode सत्र समाप्त करना चाहते हैं? सभी ऐप्स पुनः सामान्य रूप से उपलब्ध हो जाएंगे।",
          color = WarmParchment,
          fontSize = 14.sp,
          lineHeight = 20.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showExitConfirmationDialog = false
            FocusModeManager.deactivateFocusMode(context)
            viewModel.unlockFocusMode()
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianElevated),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("confirm_turn_off_focus_button")
        ) {
          Text("Turn Off (बंद करें)", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showExitConfirmationDialog = false },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmMuted),
          border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
          modifier = Modifier.testTag("cancel_turn_off_focus_button")
        ) {
          Text("Cancel (रद्द करें)")
        }
      }
    )
  }

  if (showEmergencySheet) {
    EmergencyRecoveryBottomSheet(
      onDismiss = { showEmergencySheet = false },
      onApplyForTomorrow = {
        viewModel.applyEmergencyRecoveryMode(forTomorrow = true)
        showEmergencySheet = false
      },
      onApplyForToday = {
        viewModel.applyEmergencyRecoveryMode(forTomorrow = false)
        showEmergencySheet = false
      }
    )
  }

  if (showPinVerificationForAllowedApps) {
    com.example.ui.screens.AccountabilityPinScreen(
      onDismiss = { showPinVerificationForAllowedApps = false },
      mode = com.example.ui.components.AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
      actionTitle = "Allowed Apps सूची में बदलाव",
      onActionApproved = {
        showPinVerificationForAllowedApps = false
        showAllowedAppsDialog = true
      }
    )
  }
}
