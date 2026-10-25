package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyRecoveryBottomSheet(
  onDismiss: () -> Unit,
  onApplyForTomorrow: () -> Unit,
  onApplyForToday: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkBackground,
    scrimColor = Color.Black.copy(alpha = 0.75f),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 6.dp)
          .width(40.dp)
          .height(4.dp)
          .clip(CircleShape)
          .background(DarkSurfaceBorder)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(Color(0xFF26A69A).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Healing,
              contentDescription = "Recovery",
              tint = Color(0xFF26A69A),
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "Emergency Recovery Mode",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "शांत, तनाव-मुक्त पुनर्प्राप्ति रूटीन",
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 12.sp
            )
          }
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.7f))
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Encouragement Box
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(Color(0xFF26A69A).copy(alpha = 0.1f))
          .border(1.dp, Color(0xFF26A69A).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
          .padding(14.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "दिन योजना के अनुसार नहीं चला? कोई बात नहीं।",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          )
          Text(
            text = "खुद को दोष देने के बजाय, कल के लिए एक सरल और व्यावहारिक रिकवरी रूटीन अपनाएँ ताकि आप स्वाभाविक रूप से अपनी ऊर्जा और अनुशासन पुनः प्राप्त कर सकें।",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 12.sp,
            lineHeight = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "सुझाई गई 5-चरणीय रिकवरी योजना:",
        color = CyanNeon,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(10.dp))

      val recoverySteps = listOf(
        Pair("🌅 5:30 AM", "Gentle Wake Up & Hydration — हल्का खिंचाव और गुनगुना पानी"),
        Pair("🧘 6:00 AM", "10 Min Breathing Meditation — तंत्रिका तंत्र को शांत करने के लिए"),
        Pair("🎯 9:30 AM", "One Essential Priority Focus — केवल 1 मुख्य लक्ष्य पूरा करें"),
        Pair("🚶 5:30 PM", "Restorative Walk & Fresh Air — 20 मिनट बिना फ़ोन के खुली हवा"),
        Pair("🌙 10:00 PM", "Early Restorative Sleep — जल्दी सोएं ताकि शरीर और मन फ्रेश हो")
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recoverySteps.forEach { (time, desc) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = time,
              color = Color(0xFF26A69A),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              modifier = Modifier.width(90.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = desc,
              color = Color.White.copy(alpha = 0.9f),
              fontSize = 11.sp,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Safety Notice: Timetable Protection
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
          .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = CyanNeon,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "यह रूटीन केवल एक दिन के लिए अतिरिक्त टास्क के रूप में जोड़ी जाएगी। आपका मुख्य टाइमटेबल और ऐतिहासिक डेटा सुरक्षित रहेगा।",
          color = Color.White.copy(alpha = 0.7f),
          fontSize = 11.sp
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Button(
        onClick = {
          onApplyForTomorrow()
          onDismiss()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("apply_recovery_tomorrow_btn"),
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF26A69A),
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("कल के लिए रिकवरी रूटीन लागू करें", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedButton(
        onClick = {
          onApplyForToday()
          onDismiss()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp)
          .testTag("apply_recovery_today_btn"),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = CyanNeon
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text("आज के बचे समय के लिए लागू करें", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
      }

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
