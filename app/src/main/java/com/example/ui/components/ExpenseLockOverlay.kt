package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite

@Composable
fun ExpenseLockOverlay(
  correctPin: String,
  onUnlocked: () -> Unit,
  onCancel: () -> Unit
) {
  var enteredPin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ObsidianCharcoal),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
      ) {
        IconButton(onClick = onCancel) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "वापस जाएं",
            tint = WarmOffWhite
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Box(
        modifier = Modifier
          .size(64.dp)
          .clip(CircleShape)
          .background(ObsidianElevated)
          .border(1.dp, CyanNeon, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = CyanNeon,
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "खर्च डायरी पिन लॉक",
        color = WarmOffWhite,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = if (isError) "गलत पिन! पुनः प्रयास करें" else "अपना 4-अंकों का पिन दर्ज करें",
        color = if (isError) Color(0xFFEF4444) else WarmMuted,
        fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      // 4 Dots
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        repeat(4) { idx ->
          val isFilled = idx < enteredPin.length
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(if (isFilled) CyanNeon else DarkSurface)
              .border(1.dp, if (isFilled) CyanNeon else DarkSurfaceBorder, CircleShape)
          )
        }
      }

      Spacer(modifier = Modifier.height(36.dp))

      // Numeric Keypad
      val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
      )

      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
          Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            row.forEach { digit ->
              if (digit.isEmpty()) {
                Spacer(modifier = Modifier.size(64.dp))
              } else if (digit == "DEL") {
                Box(
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .clickable {
                      if (enteredPin.isNotEmpty()) {
                        enteredPin = enteredPin.dropLast(1)
                        isError = false
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "डिलीट",
                    tint = WarmOffWhite,
                    modifier = Modifier.size(22.dp)
                  )
                }
              } else {
                Box(
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ObsidianElevated)
                    .border(1.dp, DarkSurfaceBorder, CircleShape)
                    .clickable {
                      if (enteredPin.length < 4) {
                        val next = enteredPin + digit
                        enteredPin = next
                        isError = false
                        if (next.length == 4) {
                          if (next == correctPin) {
                            onUnlocked()
                          } else {
                            isError = true
                            enteredPin = ""
                          }
                        }
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = digit,
                    color = WarmOffWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
