package com.example.ui.screens

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.focus.AccountabilityPinManager
import com.example.focus.AlwaysOnSecurityManager
import com.example.focus.ProtectionHealthManager
import com.example.ui.components.AccountabilityPinDialogMode

/**
 * Full-Screen Robust Accountability PIN Screen.
 *
 * Direct full-screen Compose destination / view:
 * 1) Renders in the primary Activity Compose hierarchy (Zero Dialog nesting / Zero WindowManager secondary window failure).
 * 2) FLAG_SECURE is NEVER applied (eliminates Motorola & display compositor black/invisible window bugs).
 * 3) Suppresses floating dots and strict lock pills while this screen is active.
 * 4) Logs to Protection Health Log and shows user Toast if flow cannot start or encounters errors.
 */
@Composable
fun AccountabilityPinScreen(
  onDismiss: () -> Unit,
  mode: AccountabilityPinDialogMode? = null,
  actionTitle: String = "सुरक्षा बदलाव",
  onActionApproved: (() -> Unit)? = null
) {
  val context = LocalContext.current

  BackHandler {
    onDismiss()
  }

  // Manage Pin Flow Active state (suppresses floating dots / overlays while active)
  DisposableEffect(Unit) {
    AccountabilityPinManager.setPinFlowActive(context, true)
    onDispose {
      AccountabilityPinManager.setPinFlowActive(context, false)
    }
  }

  var renderError by remember { mutableStateOf<String?>(null) }

  if (renderError != null) {
    // Safe Fallback UI
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F172A))
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Warning,
          contentDescription = null,
          tint = Color(0xFFEF4444),
          modifier = Modifier.size(48.dp)
        )
        Text(
          text = "PIN स्क्रीन रेंडर करने में त्रुटि आई",
          color = Color(0xFFF1F5F9),
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = renderError ?: "अज्ञात त्रुटि। यह घटना हेल्थ लॉग में दर्ज कर ली गई है।",
          color = Color(0xFF94A3B8),
          fontSize = 13.sp,
          textAlign = TextAlign.Center
        )
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
        ) {
          Text("बंद करें", color = Color.White)
        }
      }
    }
  } else {
    AccountabilityPinScreenContent(
      onDismiss = onDismiss,
      mode = mode,
      actionTitle = actionTitle,
      onActionApproved = onActionApproved,
      onRenderFailed = { error ->
        renderError = error
        ProtectionHealthManager.logHealthEvent(
          context = context,
          eventType = "PIN_SCREEN_RENDER_ERROR",
          message = "PIN स्क्रीन रेंडरिंग विफल: $error",
          severity = "HIGH"
        )
        Toast.makeText(context, "PIN स्क्रीन त्रुटि: $error", Toast.LENGTH_LONG).show()
      }
    )
  }
}

@Composable
private fun AccountabilityPinScreenContent(
  onDismiss: () -> Unit,
  mode: AccountabilityPinDialogMode?,
  actionTitle: String,
  onActionApproved: (() -> Unit)?,
  onRenderFailed: (String) -> Unit
) {
  val context = LocalContext.current
  val isPinAlreadySet = remember { AccountabilityPinManager.isPinSet(context) }
  val is24HrUnlocked = remember {
    AlwaysOnSecurityManager.isTemporarilyUnlocked(context) ||
      AlwaysOnSecurityManager.isUnlockRequestReadyToClaim(context)
  }

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

  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF090D16)) // Solid deep slate/obsidian background
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .testTag("accountability_pin_screen")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Top Navigation Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(44.dp)
            .background(Color(0xFF1E293B), CircleShape)
            .border(1.dp, Color(0xFF334155), CircleShape)
            .testTag("pin_cancel_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color(0xFFF1F5F9)
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color(0xFFD4AF37),
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "जवाबदेही सुरक्षा",
            color = Color(0xFFD4AF37),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Header Icon & Title
      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(Color(0xFF1E293B))
          .border(1.5.dp, Color(0xFFD4AF37), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = when {
            currentStep == 2 -> Icons.Default.Autorenew
            effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN -> Icons.Default.LockReset
            else -> Icons.Default.Lock
          },
          contentDescription = null,
          tint = Color(0xFFD4AF37),
          modifier = Modifier.size(32.dp)
        )
      }

      Text(
        text = when {
          currentStep == 2 && effectiveMode == AccountabilityPinDialogMode.VERIFY_AND_ROTATE ->
            "नया PIN सेट करें (मित्र के लिए)"
          currentStep == 2 && effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN ->
            "नया Accountability PIN सेट करें"
          currentStep == 2 ->
            "Accountability PIN सेट करें"
          effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN ->
            "पुराना PIN दर्ज करें"
          else ->
            "Accountability PIN दर्ज करें"
        },
        color = Color(0xFFF8FAFC),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Text(
        text = when {
          currentStep == 1 && effectiveMode == AccountabilityPinDialogMode.CHANGE_PIN ->
            "PIN बदलने के लिए पहले अपना वर्तमान PIN सत्यापित करें:"
          currentStep == 1 && effectiveMode == AccountabilityPinDialogMode.VERIFY_AND_ROTATE ->
            "'$actionTitle' के लिए PIN दर्ज करें। सत्यापन के तुरंत बाद दोस्त नया PIN सेट करेगा:"
          currentStep == 1 ->
            "इस सुरक्षा बदलाव के लिए अपना 4-6 अंकों का Accountability PIN दर्ज करें:"
          else ->
            "यह नया PIN आपका जवाबदेह मित्र खुद सेट करे ताकि आप खुद इसे कमजोर न कर सकें।"
        },
        color = Color(0xFF94A3B8),
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 8.dp)
      )

      // STEP 1 CONTENT: Current PIN Entry
      if (currentStep == 1) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131D2D))
            .border(1.dp, Color(0xFF233247), RoundedCornerShape(16.dp))
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Text(
            text = "वर्तमान Accountability PIN",
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )

          OutlinedTextField(
            value = currentPinInput,
            onValueChange = {
              if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                currentPinInput = it
                errorMessage = null
              }
            },
            placeholder = { Text("4-6 अंकों का PIN", color = Color(0xFF64748B), fontSize = 14.sp) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.NumberPassword,
              autoCorrectEnabled = false
            ),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pin_current_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A),
              focusedTextColor = Color(0xFFF8FAFC),
              unfocusedTextColor = Color(0xFFF8FAFC),
              focusedBorderColor = Color(0xFFD4AF37),
              unfocusedBorderColor = Color(0xFF334155),
              cursorColor = Color(0xFFD4AF37)
            )
          )

          // Masked Dots Indicator
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val length = currentPinInput.length
            for (i in 0 until 6) {
              Box(
                modifier = Modifier
                  .padding(horizontal = 4.dp)
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(
                    if (i < length) Color(0xFFD4AF37) else Color(0xFF334155)
                  )
              )
            }
          }
        }

        // Forgot PIN Informational Card (Strict 24-hr unlock rule)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131D2D))
            .border(1.dp, Color(0xFF233247), RoundedCornerShape(12.dp))
            .padding(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = Color(0xFFD4AF37),
              modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
              Text(
                text = "PIN भूल गए हैं?",
                color = Color(0xFFD4AF37),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "सुरक्षा नियमों के अनुसार कोई तात्कालिक रीसेट नहीं है। केवल Settings में 24 घंटे का Unlock Request पूरा होने पर ही नया PIN सेट किया जा सकता है।",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                lineHeight = 16.sp
              )
            }
          }
        }

      } else {
        // STEP 2 CONTENT: Friend sets new PIN with 2-time confirmation
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.2.dp, Color(0xFFD4AF37).copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .padding(14.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFD4AF37),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "जवाबदेह मित्र के लिए निर्देश:",
                color = Color(0xFFD4AF37),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = "“अब नया PIN खुद सेट करें और उसे अपने पास रखें, इसे दूसरे को मत बताओ।”",
              color = Color(0xFFF8FAFC),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              lineHeight = 18.sp
            )
            Text(
              text = "यह स्क्रीन केवल PIN सेट करने वाला मित्र देखे। नया PIN पुराने जैसा नहीं होना चाहिए।",
              color = Color(0xFF94A3B8),
              fontSize = 11.sp
            )
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131D2D))
            .border(1.dp, Color(0xFF233247), RoundedCornerShape(16.dp))
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // New PIN
          Text(
            text = "नया PIN दर्ज करें (4-6 अंक):",
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )

          OutlinedTextField(
            value = newPinInput,
            onValueChange = {
              if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                newPinInput = it
                errorMessage = null
              }
            },
            placeholder = { Text("4-6 अंक", color = Color(0xFF64748B), fontSize = 14.sp) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.NumberPassword,
              autoCorrectEnabled = false
            ),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pin_new_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A),
              focusedTextColor = Color(0xFFF8FAFC),
              unfocusedTextColor = Color(0xFFF8FAFC),
              focusedBorderColor = Color(0xFFD4AF37),
              unfocusedBorderColor = Color(0xFF334155),
              cursorColor = Color(0xFFD4AF37)
            )
          )

          // Confirm New PIN
          Text(
            text = "नया PIN दोबारा दर्ज करें (Confirm):",
            color = Color(0xFFE2E8F0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )

          OutlinedTextField(
            value = confirmPinInput,
            onValueChange = {
              if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                confirmPinInput = it
                errorMessage = null
              }
            },
            placeholder = { Text("वही PIN दोबारा", color = Color(0xFF64748B), fontSize = 14.sp) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.NumberPassword,
              autoCorrectEnabled = false
            ),
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pin_confirm_input"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF0F172A),
              unfocusedContainerColor = Color(0xFF0F172A),
              focusedTextColor = Color(0xFFF8FAFC),
              unfocusedTextColor = Color(0xFFF8FAFC),
              focusedBorderColor = Color(0xFFD4AF37),
              unfocusedBorderColor = Color(0xFF334155),
              cursorColor = Color(0xFFD4AF37)
            )
          )
        }
      }

      // Lockout or Error Banner
      AnimatedVisibility(visible = lockoutSec > 0 || errorMessage != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF451A1A))
            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = Color(0xFFEF4444),
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = if (lockoutSec > 0) {
                "⏳ 5 गलत प्रयासों के कारण लॉकआउट: $lockoutSec सेकंड प्रतीक्षा करें।"
              } else {
                errorMessage ?: ""
              },
              color = Color(0xFFFCA5A5),
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Action Buttons
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

            when (effectiveMode) {
              AccountabilityPinDialogMode.VERIFY_ONLY -> {
                onActionApproved?.invoke()
                onDismiss()
              }
              AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
              AccountabilityPinDialogMode.CHANGE_PIN -> {
                // Advance to Step 2: Friend rotation
                currentStep = 2
                errorMessage = null
              }
              else -> {
                onActionApproved?.invoke()
                onDismiss()
              }
            }
          } else {
            // STEP 2: Set New PIN
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
        enabled = lockoutSec <= 0,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("pin_submit_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFD4AF37),
          contentColor = Color(0xFF0F172A),
          disabledContainerColor = Color(0xFF334155),
          disabledContentColor = Color(0xFF64748B)
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(
          imageVector = if (currentStep == 1 && effectiveMode == AccountabilityPinDialogMode.VERIFY_ONLY) {
            Icons.Default.CheckCircle
          } else {
            Icons.Default.Autorenew
          },
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = when {
            currentStep == 1 && effectiveMode == AccountabilityPinDialogMode.VERIFY_ONLY ->
              "सत्यापित करें"
            currentStep == 1 ->
              "सत्यापित करें व आगे बढ़ें"
            else ->
              "नया PIN सुरक्षित रूप से सेट करें"
          },
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }

      OutlinedButton(
        onClick = onDismiss,
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = Color(0xFF94A3B8)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("रद्द करें (Cancel)", fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
