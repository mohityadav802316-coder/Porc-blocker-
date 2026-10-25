package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.focus.AccountabilityPinManager
import com.example.focus.ProtectionPauseManager
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

/**
 * Dialog for selecting Pause Duration and verifying/rotating Accountability PIN.
 *
 * Flow:
 * 1. Choose Duration (30m, 1h, 2h, 4h).
 * 2. Enter Old PIN -> Verified.
 * 3. MANDATORY ROTATION: "Naya PIN set karo (dost ke liye)" screen where partner enters new PIN twice.
 * 4. Only after new PIN is set successfully does the pause actually begin!
 */
@Composable
fun PauseProtectionDialog(
  onDismiss: () -> Unit,
  onPauseStarted: () -> Unit
) {
  val context = LocalContext.current
  val pausesUsedToday = remember { ProtectionPauseManager.getPausesUsedToday(context) }
  val canPause = remember { ProtectionPauseManager.canPauseToday(context) }
  val isPinSet = remember { AccountabilityPinManager.isPinSet(context) }

  // Steps:
  // Step 1: Select Duration & Enter Current PIN
  // Step 2: Friend enters New PIN twice (Mandatory Rotation)
  var currentStep by remember { mutableStateOf(1) }
  var selectedDurationMinutes by remember { mutableIntStateOf(60) } // Default 1 hour

  var currentPinInput by remember { mutableStateOf("") }
  var newPinInput by remember { mutableStateOf("") }
  var confirmPinInput by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val lockoutSec = remember(errorMessage) {
    AccountabilityPinManager.getLockoutRemainingSeconds(context)
  }

  val activity = context as? android.app.Activity
  androidx.compose.runtime.DisposableEffect(Unit) {
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

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      securePolicy = androidx.compose.ui.window.SecureFlagPolicy.SecureOff,
      usePlatformDefaultWidth = true
    ),
    containerColor = ObsidianCharcoal,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = if (currentStep == 2) Icons.Default.Autorenew else Icons.Default.PauseCircle,
          contentDescription = null,
          tint = if (currentStep == 2) GoldBrass else Color(0xFFFFB74D),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (currentStep == 2) "नया PIN सेट करें (दोस्त के लिए)" else "सुरक्षा पॉज़ करें (Pause)",
          color = WarmOffWhite,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Daily Quota indicator
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("दैनिक पॉज़ कोटा:", color = TextMuted, fontSize = 11.sp)
          Text(
            text = "$pausesUsedToday / ${ProtectionPauseManager.MAX_PAUSES_PER_DAY} उपयोग हुए",
            color = if (canPause) Color(0xFF10B981) else Color(0xFFEF4444),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        if (!canPause) {
          Text(
            text = "❌ आज के लिए अधिकतम 3 पॉज़ सीमा समाप्त हो चुकी है। कृपया कल तक प्रतीक्षा करें।",
            color = Color(0xFFEF4444),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        } else if (currentStep == 1) {
          // STEP 1: Duration selection & current PIN entry
          Text(
            text = "पॉज़ के दौरान Accessibility, VPN, Device Admin और AI डिटेक्शन बंद रहेंगे। अवधि चुनें:",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
              30 to "30 मिनट",
              60 to "1 घंटा",
              120 to "2 घंटे",
              240 to "4 घंटे"
            ).forEach { (minutes, label) ->
              val isSelected = selectedDurationMinutes == minutes
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) DarkSurfaceElevated else DarkSurface)
                  .border(1.dp, if (isSelected) GoldBrass else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                  .clickable { selectedDurationMinutes = minutes }
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(
                  selected = isSelected,
                  onClick = { selectedDurationMinutes = minutes },
                  colors = RadioButtonDefaults.colors(selectedColor = GoldBrass, unselectedColor = TextMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = label,
                  color = if (isSelected) WarmOffWhite else TextMuted,
                  fontSize = 13.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
              }
            }
          }

          if (isPinSet) {
            OutlinedTextField(
              value = currentPinInput,
              onValueChange = {
                if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                  currentPinInput = it
                  errorMessage = null
                }
              },
              label = { Text("वर्तमान Accountability PIN (4-6 अंक)", fontSize = 12.sp) },
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                autoCorrectEnabled = false
              ),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
              colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedTextColor = WarmOffWhite,
                unfocusedTextColor = WarmOffWhite,
                focusedIndicatorColor = GoldBrass,
                unfocusedIndicatorColor = DarkSurfaceBorder
              )
            )
          }

          Text(
            text = "⏳ पॉज़ समाप्त होने पर आपको 30 मिनट के अंतराल पर सुरक्षा पुनः चालू करने का रिमाइंडर मिलेगा।",
            color = Color(0xFFFFB74D),
            fontSize = 10.sp
          )
        } else {
          // STEP 2: Partner sets new PIN (Rotation)
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
                text = "“अब नया PIN खुद सेट करो और उसे अपने पास रखो, इसे दूसरे को मत बताओ।”",
                color = WarmOffWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp
              )
              Text(
                text = "पॉज़ चालू करने के लिए नया PIN दो बार दर्ज करें। नया PIN पुराने जैसा नहीं होना चाहिए।",
                color = TextMuted,
                fontSize = 10.sp
              )
            }
          }

          OutlinedTextField(
            value = newPinInput,
            onValueChange = {
              if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                newPinInput = it
                errorMessage = null
              }
            },
            label = { Text("नया PIN (4-6 अंक)", fontSize = 12.sp) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.NumberPassword,
              autoCorrectEnabled = false
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
              focusedContainerColor = DarkSurface,
              unfocusedContainerColor = DarkSurface,
              focusedTextColor = WarmOffWhite,
              unfocusedTextColor = WarmOffWhite,
              focusedIndicatorColor = GoldBrass,
              unfocusedIndicatorColor = DarkSurfaceBorder
            )
          )

          OutlinedTextField(
            value = confirmPinInput,
            onValueChange = {
              if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
                confirmPinInput = it
                errorMessage = null
              }
            },
            label = { Text("नया PIN दोबारा दर्ज करें (Confirm)", fontSize = 12.sp) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.NumberPassword,
              autoCorrectEnabled = false
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
              focusedContainerColor = DarkSurface,
              unfocusedContainerColor = DarkSurface,
              focusedTextColor = WarmOffWhite,
              unfocusedTextColor = WarmOffWhite,
              focusedIndicatorColor = GoldBrass,
              unfocusedIndicatorColor = DarkSurfaceBorder
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
      if (!canPause) {
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceBorder)
        ) {
          Text("ठीक है", color = WarmOffWhite, fontSize = 12.sp)
        }
      } else if (currentStep == 1) {
        Button(
          onClick = {
            if (isPinSet) {
              if (currentPinInput.length < 4) {
                errorMessage = "कृपया 4-6 अंकों का PIN दर्ज करें।"
                return@Button
              }
              if (!AccountabilityPinManager.verifyPin(context, currentPinInput)) {
                val rem = AccountabilityPinManager.getLockoutRemainingSeconds(context)
                errorMessage = if (rem > 0) {
                  "5 गलत प्रयास! $rem सेकंड का लॉकआउट सक्रिय।"
                } else {
                  "गलत PIN! (5 गलत प्रयासों पर 1 मिनट का लॉकआउट)"
                }
                return@Button
              }
              // Move to mandatory friend rotation step
              currentStep = 2
              errorMessage = null
            } else {
              // PIN not set yet, friend sets first PIN
              currentStep = 2
              errorMessage = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("सत्यापित करें व आगे बढ़ें", color = ObsidianCharcoal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      } else {
        // Step 2: Friend sets new PIN and starts pause
        Button(
          onClick = {
            if (newPinInput.length !in 4..6) {
              errorMessage = "नया PIN 4 से 6 अंकों का होना चाहिए।"
              return@Button
            }
            if (newPinInput != confirmPinInput) {
              errorMessage = "दोनों बार दर्ज किया गया PIN मेल नहीं खाता।"
              return@Button
            }

            val setRes = AccountabilityPinManager.setPin(
              context = context,
              newPin = newPinInput,
              reason = "ROTATION_ON_PAUSE_START ($selectedDurationMinutes min)"
            )

            when (setRes) {
              is AccountabilityPinManager.PinChangeResult.Error -> {
                errorMessage = setRes.message
                return@Button
              }
              is AccountabilityPinManager.PinChangeResult.Success -> {
                val success = ProtectionPauseManager.startPause(context, selectedDurationMinutes)
                if (success) {
                  Toast.makeText(context, "$selectedDurationMinutes मिनट के लिए सुरक्षा पॉज़ की गई (नया PIN सुरक्षित)", Toast.LENGTH_LONG).show()
                  onPauseStarted()
                  onDismiss()
                } else {
                  Toast.makeText(context, "पॉज़ शुरू नहीं हो सका", Toast.LENGTH_SHORT).show()
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("नया PIN सेट कर पॉज़ शुरू करें", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("रद्द करें", fontSize = 12.sp)
      }
    }
  )
}
