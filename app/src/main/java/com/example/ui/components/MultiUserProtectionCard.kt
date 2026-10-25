package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.MultiUserProtectionManager
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment

/**
 * Multi-User / Guest / Second Space / Dual Apps Bypass Protection Card.
 *
 * Provides:
 * 1. Toggle for Multi-user protection.
 * 2. Visual indicator of 4 defensive layers:
 *    - Layer 1: Always-On Accessibility detection
 *    - Layer 2: Device Owner hard-restrictions (DISALLOW_ADD_USER, etc.)
 *    - Layer 3: Secure Settings (user_switcher_enabled=0)
 *    - Layer 4: Watchdog & Protection Health audit
 * 3. In-App Device Owner Setup Guide Dialog with copyable ADB commands.
 * 4. Configurable Keyword Editor for brand-specific user switcher labels.
 * 5. Recent interception counter & audit indicator.
 */
@Composable
fun MultiUserProtectionCard(
  isStrictLock: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var status by remember { mutableStateOf(MultiUserProtectionManager.getStatus(context)) }
  var showGuideDialog by remember { mutableStateOf(false) }
  var showKeywordEditorDialog by remember { mutableStateOf(false) }
  var showPinDialog by remember { mutableStateOf(false) }
  var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }

  fun refresh() {
    status = MultiUserProtectionManager.getStatus(context)
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, if (status.isHealthy) DarkSurfaceBorder else Color(0xFFEF4444).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
      .padding(16.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (status.isDeviceOwner) Color(0xFF064E3B) else ObsidianCard),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = if (status.isDeviceOwner) Color(0xFF34D399) else GoldBrass,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Multi-User / Guest Bypass सुरक्षा",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "नया प्रोफ़ाइल, गेस्ट या डुअल ऐप्स से बाईपास रोकें",
              color = TextMuted,
              fontSize = 11.sp
            )
          }
        }

        Switch(
          checked = status.isEnabled,
          enabled = !isStrictLock,
          onCheckedChange = { enabled ->
            if (enabled) {
              // ON karna bina PIN ke ho
              MultiUserProtectionManager.setEnabled(context, true)
              MultiUserProtectionManager.applyDeviceOwnerRestrictions(context)
              MultiUserProtectionManager.applySecureSettingsRestrictions(context)
              refresh()
            } else {
              // OFF karne par VERIFY_AND_ROTATE
              if (com.example.focus.AccountabilityPinManager.isPinSet(context) &&
                !com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)
              ) {
                pendingPinAction = {
                  MultiUserProtectionManager.setEnabled(context, false, pinVerified = true)
                  refresh()
                }
                showPinDialog = true
              } else {
                MultiUserProtectionManager.setEnabled(context, false)
                refresh()
              }
            }
          }
        )
      }

      // Status pill
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(if (status.isHealthy) DarkSurface else Color(0xFF3A1E1E))
          .border(
            0.6.dp,
            if (status.isHealthy) DarkSurfaceBorder else Color(0xFFEF4444).copy(alpha = 0.4f),
            RoundedCornerShape(10.dp)
          )
          .padding(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              if (status.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
              contentDescription = null,
              tint = if (status.isHealthy) Color(0xFF10B981) else Color(0xFFEF4444),
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = status.healthSummary,
              color = if (status.isHealthy) WarmOffWhite else Color(0xFFFFB4AB),
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }

          if (status.interceptionCount > 0) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(GoldBrass.copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "${status.interceptionCount} बार रोका गया",
                color = GoldBrass,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 4 Layers status breakdown
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurface)
          .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text("सुरक्षा परतें (Defensive Layers):", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        LayerRow(
          title = "Layer 1: Always-On Accessibility Detection",
          description = "Settings/SystemUI में Users, Guest, Second Space, Dual apps ब्लॉक",
          isActive = status.isAccessibilityActive
        )

        LayerRow(
          title = "Layer 2: Device Owner Hard-Restrictions",
          description = "DISALLOW_ADD_USER, DISALLOW_USER_SWITCH सिस्टम स्तर पर लागू",
          isActive = status.isDeviceOwner
        )

        LayerRow(
          title = "Layer 3: Secure Settings Hardening",
          description = "user_switcher_enabled = 0, guest_user_enabled = 0",
          isActive = status.hasWriteSecureSettings
        )

        LayerRow(
          title = "Layer 4: Watchdog & Protection Health",
          description = "15-मिनट समय पर री-एनफोर्समेंट और हेल्थ मॉनिटरिंग",
          isActive = true
        )
      }

      // Action buttons: Device Owner Setup Guide + Keyword Configuration
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = { showGuideDialog = true },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = if (status.isDeviceOwner) DarkSurface else GoldBrass),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Icon(
            Icons.Default.HelpOutline,
            contentDescription = null,
            tint = if (status.isDeviceOwner) WarmOffWhite else ObsidianCharcoal,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (status.isDeviceOwner) "Device Owner सक्रिय ✓" else "Device Owner गाइड",
            color = if (status.isDeviceOwner) WarmOffWhite else ObsidianCharcoal,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedButton(
          onClick = { showKeywordEditorDialog = true },
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("कीवर्ड्स (${status.activeKeywordsCount})", color = CyanNeon, fontSize = 11.sp)
        }
      }
    }
  }

  if (showGuideDialog) {
    DeviceOwnerSetupGuideDialog(
      isDeviceOwner = status.isDeviceOwner,
      hasWriteSecure = status.hasWriteSecureSettings,
      onDismiss = {
        showGuideDialog = false
        refresh()
      }
    )
  }

  if (showKeywordEditorDialog) {
    MultiUserKeywordEditorDialog(
      onDismiss = {
        showKeywordEditorDialog = false
        refresh()
      }
    )
  }

  if (showPinDialog) {
    AccountabilityPinDialog(
      onDismiss = {
        showPinDialog = false
        pendingPinAction = null
      },
      onActionApproved = {
        pendingPinAction?.invoke()
        showPinDialog = false
        pendingPinAction = null
      },
      mode = AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
      actionTitle = "Multi-User सुरक्षा बंद करना"
    )
  }
}

@Composable
private fun LayerRow(
  title: String,
  description: String,
  isActive: Boolean
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top
  ) {
    Icon(
      if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning,
      contentDescription = null,
      tint = if (isActive) Color(0xFF10B981) else Color(0xFF9CA3AF),
      modifier = Modifier
        .size(14.dp)
        .padding(top = 2.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column {
      Text(
        text = title,
        color = if (isActive) WarmOffWhite else TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = description,
        color = TextMuted,
        fontSize = 10.sp,
        lineHeight = 13.sp
      )
    }
  }
}

/**
 * In-App Step-by-Step Guide for Device Owner & WRITE_SECURE_SETTINGS ADB setup.
 */
@Composable
fun DeviceOwnerSetupGuideDialog(
  isDeviceOwner: Boolean,
  hasWriteSecure: Boolean,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

  val pkg = context.packageName
  val dpmCommand = "adb shell dpm set-device-owner $pkg/com.example.focus.LifeTrackerAdminReceiver"
  val secureSettingsCommand = "adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS"

  fun copyToClipboard(label: String, text: String) {
    clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "कमांड कॉपी हो गई!", Toast.LENGTH_SHORT).show()
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("समझ गया", color = ObsidianCharcoal, fontWeight = FontWeight.Bold)
      }
    },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Security, contentDescription = null, tint = GoldBrass)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Device Owner & ADB गाइड", color = WarmOffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "यदि आपका फ़ोन बिना फ़ॉर्मेट किए उपयोग में है, तो Layer 1 (Accessibility) हमेशा-चालू रहकर मल्टी-यूज़र स्क्रीन को ब्लॉक करता है।",
          color = WarmOffWhite,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        Text(
          text = "सिस्टम-स्तर पर 'User Switch' और 'Guest Mode' को स्थायी रूप से अक्षम करने हेतु 2 विकल्प हैं:",
          color = TextMuted,
          fontSize = 11.sp
        )

        // Option A: Secure Settings (Easy)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(0.6.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("विकल्प A: WRITE_SECURE_SETTINGS (सरल)", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              IconButton(
                onClick = { copyToClipboard("ADB Secure Settings", secureSettingsCommand) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanNeon, modifier = Modifier.size(14.dp))
              }
            }
            Text("किसी भी समय PC से एक बार चलाएं (नो-फ़ैक्टरी रीसेट):", color = TextMuted, fontSize = 10.sp)
            Text(
              text = secureSettingsCommand,
              color = WarmParchment,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              lineHeight = 12.sp
            )
            Text(
              text = if (hasWriteSecure) "✓ स्थिति: अनुमति प्राप्त है (Hardened)" else "स्थिति: अनुमति नहीं",
              color = if (hasWriteSecure) Color(0xFF10B981) else Color(0xFFFFB4AB),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Option B: Full Device Owner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(0.6.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("विकल्प B: Full Device Owner (सर्वोच्च स्तर)", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              IconButton(
                onClick = { copyToClipboard("ADB Device Owner", dpmCommand) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = GoldBrass, modifier = Modifier.size(14.dp))
              }
            }
            Text("नए फ़ोन या फ़ैक्टरी रीसेट के बाद पहली बार सेटअप के समय चलाएं:", color = TextMuted, fontSize = 10.sp)
            Text(
              text = dpmCommand,
              color = WarmParchment,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              lineHeight = 12.sp
            )
            Text(
              text = if (isDeviceOwner) "✓ स्थिति: Device Owner सक्रिय है" else "स्थिति: Device Owner सेट नहीं है (Layer 1 सक्रिय)",
              color = if (isDeviceOwner) Color(0xFF10B981) else Color(0xFFFFB4AB),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    },
    containerColor = DarkSurface
  )
}

/**
 * Dialog to view and customize multi-user detection keywords (for Motorola or other OEM custom labels).
 */
@Composable
fun MultiUserKeywordEditorDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val currentKeywords = remember { MultiUserProtectionManager.getActiveKeywords(context) }
  var customInput by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(
        onClick = {
          if (customInput.isNotBlank()) {
            MultiUserProtectionManager.setCustomKeywords(context, customInput)
            Toast.makeText(context, "कीवर्ड्स सहेजे गए!", Toast.LENGTH_SHORT).show()
          }
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("सहेजें", color = ObsidianCharcoal, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("रद्द करें", color = TextMuted)
      }
    },
    title = {
      Text("Multi-User डिटेक्शन कीवर्ड्स", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "यदि आपके Motorola या अन्य फ़ोन में यूज़र-स्विचर का कोई नया नाम दिखता है, तो यहाँ अल्पविराम (comma) लगाकर जोड़ें:",
          color = TextMuted,
          fontSize = 11.sp,
          lineHeight = 15.sp
        )

        OutlinedTextField(
          value = customInput,
          onValueChange = { customInput = it },
          placeholder = { Text("उदा. moto user, family space, dual space", color = TextMuted, fontSize = 11.sp) },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite
          )
        )

        Text(
          text = "डिफ़ॉल्ट सक्रिय कीवर्ड्स (${currentKeywords.size}):\n" + currentKeywords.take(12).joinToString(", ") + "...",
          color = TextMuted,
          fontSize = 10.sp,
          lineHeight = 14.sp
        )
      }
    },
    containerColor = DarkSurface
  )
}
