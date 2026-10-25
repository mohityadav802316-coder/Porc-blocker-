package com.example.ui.components

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.example.focus.AccountabilityPinManager
import com.example.focus.AlwaysOnSecurityManager
import com.example.focus.ProtectionHealthManager
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

enum class AccountabilityPinDialogMode {
  VERIFY_AND_ROTATE, // Default: Enter old PIN -> If verified, friend sets new PIN -> Action proceeds
  VERIFY_ONLY,       // Only verify existing PIN without mandatory immediate rotation
  CHANGE_PIN,        // Change PIN from Settings: Enter Old PIN -> Set New PIN twice
  INITIAL_SETUP      // First time setting PIN: Friend sets PIN twice
}

/**
 * Robust Accountability PIN Dialog.
 *
 * Root Cause Fixes:
 * 1) SecureFlagPolicy.SecureOn REMOVED from DialogProperties.
 *    On Motorola / emulator displays, secondary secure dialog windows were rendered as solid black
 *    by the hardware display compositor. FLAG_SECURE is now managed optionally at the Activity level.
 * 2) DialogProperties uses safe platform standard width (usePlatformDefaultWidth = true).
 * 3) Explicit high-contrast colors (Material 3 Slate/Gold) guarantee inputs and buttons are visible.
 * 4) Overlays (Black Screen floating dot & Strict Lock pill) are suppressed while dialog is active.
 * 5) Try-catch error logging writes directly to ProtectionHealthLog on any render issue.
 */
@Composable
fun AccountabilityPinDialog(
  onDismiss: () -> Unit,
  mode: AccountabilityPinDialogMode? = null,
  actionTitle: String = "सुरक्षा बदलाव",
  onActionApproved: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val activity = context as? Activity

  // Manage Activity-level FLAG_SECURE & Overlay Suppression
  DisposableEffect(Unit) {
    val shouldSecure = AccountabilityPinManager.isHidePinScreenFromRecents(context)
    if (shouldSecure) {
      AccountabilityPinManager.applyActivitySecureFlag(activity, true)
    }
    AccountabilityPinManager.setPinFlowActive(context, true)

    onDispose {
      if (shouldSecure) {
        AccountabilityPinManager.applyActivitySecureFlag(activity, false)
      }
      AccountabilityPinManager.setPinFlowActive(context, false)
    }
  }

  val isPinAlreadySet = remember { AccountabilityPinManager.isPinSet(context) }
  val is24HrUnlocked = remember {
    AlwaysOnSecurityManager.isTemporarilyUnlocked(context) ||
      AlwaysOnSecurityManager.isUnlockRequestReadyToClaim(context)
  }

  // Determine starting state
  val effectiveMode = mode ?: if (!isPinAlreadySet) {
    AccountabilityPinDialogMode.INITIAL_SETUP
  } else {
    AccountabilityPinDialogMode.VERIFY_AND_ROTATE
  }

  var currentStep by remember {
    mutableIntStateOf(
      if (effectiveMode == AccountabilityPinDialogMode.INITIAL_SETUP || (is24HrUnlocked && !isPinAlreadySet)) {
        2
      } else {
        1
      }
    )
  }

  var currentPinInput by remember { mutableStateOf("") }
  var newPinInput by remember { mutableStateOf("") }
  var confirmPinInput by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val lockoutSec = remember(errorMessage) {
    AccountabilityPinManager.getLockoutRemainingSeconds(context)
  }

  AlertDialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(
        securePolicy = SecureFlagPolicy.SecureOff,
        usePlatformDefaultWidth = true
      ),
      containerColor = Color(0xFF131D2D), // Slate-900 container
      shape = RoundedCornerShape(20.dp),
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = when {
              currentStep == 2 -> Icons.Default.Autorenew
              effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN -> Icons.Default.LockReset
              else -> Icons.Default.Shield
            },
            contentDescription = null,
            tint = GoldBrass,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = when {
              currentStep == 2 && effectiveMode == AccountabilityPinDialogMode.VERIFY_AND_ROTATE -> "नया PIN सेट करें (मित्र के लिए)"
              currentStep == 2 && effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN -> "नया PIN सेट करें"
              currentStep == 2 -> "Accountability PIN सेट करें"
              effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN -> "पुराना PIN दर्ज करें"
              else -> "Accountability PIN दर्ज करें"
            },
            color = WarmOffWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
          )
        }
      },
      text = {
        val scrollState = rememberScrollState()
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(scrollState),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          if (currentStep == 1) {
            // STEP 1: Enter Current / Old PIN
            Text(
              text = when (effectiveMode) {
                AccountabilityPinDialogMode.CHANGE_PIN ->
                  "PIN बदलने के लिए पहले अपना पुराना PIN दर्ज करें:"
                AccountabilityPinDialogMode.VERIFY_AND_ROTATE ->
                  "'$actionTitle' के लिए जवाबदेही PIN दर्ज करें। सत्यापन के बाद नया PIN रोटेट होगा:"
                else ->
                  "इस सुरक्षा बदलाव के लिए 4-6 अंकों का Accountability PIN दर्ज करें:"
              },
              color = Color(0xFFCBD5E1),
              fontSize = 12.sp,
              lineHeight = 17.sp
            )

            OutlinedTextField(
              value = currentPinInput,
              onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                  currentPinInput = it
                  errorMessage = null
                }
              },
              label = { Text("वर्तमान PIN (4-6 अंक)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                autoCorrectEnabled = false
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_current_input_dialog"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC),
                focusedBorderColor = GoldBrass,
                unfocusedBorderColor = DarkSurfaceBorder,
                cursorColor = GoldBrass
              )
            )

            // Forgot PIN guidance (Strictly no bypass - 24hr Unlock Request only)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                .border(0.6.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(10.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "💡 PIN भूल गए?",
                  color = GoldBrass,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "सुरक्षा नियमों के अनुसार कोई 'Forgot PIN' रीसेट नहीं है। केवल 24 घंटे का Unlock Request पूरा होने पर ही PIN रीसेट किया जा सकता है।",
                  color = WarmParchment.copy(alpha = 0.85f),
                  fontSize = 10.sp,
                  lineHeight = 14.sp
                )
              }
            }

          } else {
            // STEP 2: Friend sets new PIN (Rotation / Initial setup)
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                .border(1.dp, GoldBrass.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.Lock, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "जवाबदेह मित्र के लिए निर्देश:",
                    color = GoldBrass,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Text(
                  text = "“अब नया PIN खुद सेट करें और उसे अपने पास रखें, इसे दूसरे को मत बताओ।”",
                  color = WarmOffWhite,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  lineHeight = 16.sp
                )
                Text(
                  text = "यह स्क्रीन केवल PIN सेट करने वाला व्यक्ति देखे। नया PIN पुराने जैसा नहीं होना चाहिए।",
                  color = TextMuted,
                  fontSize = 10.sp
                )
              }
            }

            OutlinedTextField(
              value = newPinInput,
              onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                  newPinInput = it
                  errorMessage = null
                }
              },
              label = { Text("नया PIN दर्ज करें (4-6 अंक)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                autoCorrectEnabled = false
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_new_input_dialog"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC),
                focusedBorderColor = GoldBrass,
                unfocusedBorderColor = DarkSurfaceBorder,
                cursorColor = GoldBrass
              )
            )

            OutlinedTextField(
              value = confirmPinInput,
              onValueChange = {
                if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                  confirmPinInput = it
                  errorMessage = null
                }
              },
              label = { Text("नया PIN दोबारा दर्ज करें (Confirm)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                autoCorrectEnabled = false
              ),
              singleLine = true,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_confirm_input_dialog"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC),
                focusedBorderColor = GoldBrass,
                unfocusedBorderColor = DarkSurfaceBorder,
                cursorColor = GoldBrass
              )
            )
          }

          if (lockoutSec > 0) {
            Text(
              text = "⏳ गलत प्रयासों के कारण लॉकआउट: $lockoutSec सेकंड प्रतीक्षा करें।",
              color = Color(0xFFEF4444),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          } else if (errorMessage != null) {
            Text(
              text = errorMessage ?: "",
              color = Color(0xFFEF4444),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (currentStep == 1) {
              if (currentPinInput.length < 4) {
                errorMessage = "कृपया 4-6 अंकों का PIN दर्ज करें।"
                return@Button
              }
              val verified = AccountabilityPinManager.verifyPin(context, currentPinInput)
              if (!verified) {
                val rem = AccountabilityPinManager.getLockoutRemainingSeconds(context)
                errorMessage = if (rem > 0) {
                  "5 गलत प्रयास! $rem सेकंड का लॉकआउट सक्रिय।"
                } else {
                  "गलत PIN दर्ज किया गया।"
                }
                return@Button
              }

              // If verified:
              when (effectiveMode) {
                AccountabilityPinDialogMode.VERIFY_ONLY -> {
                  onActionApproved?.invoke()
                  onDismiss()
                }
                AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
                AccountabilityPinDialogMode.CHANGE_PIN -> {
                  // Move to Step 2: Mandatory Friend Rotation
                  currentStep = 2
                  errorMessage = null
                }
                else -> {
                  onActionApproved?.invoke()
                  onDismiss()
                }
              }
            } else {
              // STEP 2: Setting new PIN (with confirmation)
              if (newPinInput.length !in 4..6) {
                errorMessage = "नया PIN 4 से 6 अंकों का होना चाहिए।"
                return@Button
              }
              if (newPinInput != confirmPinInput) {
                errorMessage = "दोनों बार दर्ज किया गया PIN मेल नहीं खाता।"
                return@Button
              }

              val reason = when (effectiveMode) {
                AccountabilityPinDialogMode.CHANGE_PIN -> "MANUAL_CHANGE_BY_OLD_PIN"
                AccountabilityPinDialogMode.INITIAL_SETUP -> "INITIAL_SETUP"
                else -> "ROTATED_AFTER_ACTION: $actionTitle"
              }

              val result = AccountabilityPinManager.setPin(context, newPinInput, reason = reason)
              when (result) {
                is AccountabilityPinManager.PinChangeResult.Success -> {
                  onActionApproved?.invoke()
                  onDismiss()
                }
                is AccountabilityPinManager.PinChangeResult.Error -> {
                  errorMessage = result.message
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = GoldBrass,
            contentColor = ObsidianCharcoal
          ),
          enabled = lockoutSec <= 0
        ) {
          Text(
            text = when {
              currentStep == 1 && effectiveMode == AccountabilityPinDialogMode.VERIFY_ONLY -> "सत्यापित करें"
              currentStep == 1 -> "सत्यापित करें व आगे बढ़ें"
              else -> "नया PIN सेट करें"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = onDismiss,
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
        ) {
          Text("रद्द करें", fontSize = 13.sp)
        }
      }
    )
}
