package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.AppNotificationChannels
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmOffWhite

/**
 * Dedicated Notification Channels Management Section for Life Tracker (Requirement 4).
 *
 * Provides complete A-to-Z transparency:
 * - Channel Name (English & Hindi)
 * - Importance Level
 * - When it triggers (ये कब आता है)
 * - Why it's necessary (क्यों ज़रूरी है)
 * - Live ON/OFF status
 * - One-tap "सिस्टम सेटिंग्स में खोलें" button to manage or silence individual channels in Android OS.
 */
@Composable
fun NotificationChannelsManagementCard(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var showPinDialog by remember { mutableStateOf(false) }
  var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var pendingActionTitle by remember { mutableStateOf("") }

  fun executeWithPin(actionTitle: String, action: () -> Unit) {
    if (com.example.focus.AccountabilityPinManager.isPinSet(context) &&
      !com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)
    ) {
      pendingPinAction = action
      pendingActionTitle = actionTitle
      showPinDialog = true
    } else {
      action()
    }
  }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
      .padding(16.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CyanNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("सूचना चैनल प्रबंधन (Notifications)", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("चैनल वार विवरण व नियंत्रण (4 अलग श्रेणियां)", color = TextMuted, fontSize = 11.sp)
          }
        }
      }

      // Root-cause fix explanation banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(DarkSurface)
          .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
          .padding(10.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("💡 फ़्लैशलाइट व सूचना स्पैम का समाधान", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "Android Accessibility में 'Flash notifications' चालू होने पर हर हाई-अलर्ट पर फ़ोन की फ्लैशलाइट जलती थी। अब सभी काउंटडाउन सूचनाओं को 'Silent', 'Alert-Once' और प्रति 1 मिनट अपडेट पर सेट किया गया है, ताकि कोई स्पैम या फ्लैश न हो।",
            color = TextMuted,
            fontSize = 10.sp,
            lineHeight = 14.sp
          )
        }
      }

      // Channel details list
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppNotificationChannels.ALL_CHANNELS.forEach { channel ->
          val isEnabled = AppNotificationChannels.isChannelEnabled(context, channel.id)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurface)
              .border(0.8.dp, if (!isEnabled && channel.id == AppNotificationChannels.CHANNEL_SECURITY_ALERTS) Color(0xFFEF4444) else DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(channel.nameHindi, color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text("महत्व: ${channel.importanceLabel}", color = GoldBrass, fontSize = 10.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(if (isEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = if (isEnabled) "🟢 चालू" else "🔴 बंद",
                      color = if (isEnabled) Color(0xFF10B981) else Color(0xFFEF4444),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }

              // Details
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "• कब आता है: ${channel.triggerConditionHindi}",
                  color = TextMuted,
                  fontSize = 10.sp,
                  lineHeight = 14.sp
                )
                Text(
                  text = "• क्यों ज़रूरी है: ${channel.reasonNeededHindi}",
                  color = TextMuted,
                  fontSize = 10.sp,
                  lineHeight = 14.sp
                )
              }

              // Action button to open channel settings in OS
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                OutlinedButton(
                  onClick = {
                    try {
                      val intent = AppNotificationChannels.createOpenChannelSettingsIntent(context, channel.id)
                      context.startActivity(intent)
                    } catch (e: Exception) {
                      Toast.makeText(context, "सेटिंग्स नहीं खोली जा सकी: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                  },
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("सिस्टम सेटिंग्स में खोलें", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }
    }
  }
}
