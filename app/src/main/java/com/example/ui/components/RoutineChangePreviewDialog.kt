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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RoutineTemplateEntity
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.util.TimeUtils

@Composable
fun RoutineChangePreviewDialog(
  originalTemplate: RoutineTemplateEntity?,
  updatedTemplate: RoutineTemplateEntity?,
  isDelete: Boolean = false,
  smartReminderMinutes: Int = 0,
  onConfirm: () -> Unit,
  onCancel: () -> Unit
) {
  val isNew = originalTemplate == null && updatedTemplate != null
  val isEdit = originalTemplate != null && updatedTemplate != null && !isDelete

  AlertDialog(
    onDismissRequest = onCancel,
    containerColor = DarkBackground,
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier
      .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
      .padding(4.dp)
      .testTag("routine_change_preview_dialog"),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
              when {
                isDelete -> Color(0xFFFF5252).copy(alpha = 0.2f)
                isNew -> CyanNeon.copy(alpha = 0.2f)
                else -> Color(0xFFFFB300).copy(alpha = 0.2f)
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = when {
              isDelete -> Icons.Default.Delete
              isNew -> Icons.Default.Schedule
              else -> Icons.Default.Edit
            },
            contentDescription = null,
            tint = when {
              isDelete -> Color(0xFFFF5252)
              isNew -> CyanNeon
              else -> Color(0xFFFFB300)
            },
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "रूटीन बदलाव पूर्वावलोकन",
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Routine Change Preview",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
          )
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        when {
          isNew && updatedTemplate != null -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CyanNeon.copy(alpha = 0.12f))
                .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "🟢 नई एक्टिविटी जोड़ी जाएगी:",
                  color = CyanNeon,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = updatedTemplate.name,
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text(
                    text = "समय: ${TimeUtils.formatTime12Hour(updatedTemplate.timeMinutes)}",
                    color = CyanNeon,
                    fontSize = 12.sp
                  )
                  Text(
                    text = "श्रेणी: ${updatedTemplate.category}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                  )
                }
              }
            }
          }

          isDelete && originalTemplate != null -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFF5252).copy(alpha = 0.12f))
                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = "🔴 एक्टिविटी हटाई जाएगी:",
                  color = Color(0xFFFF5252),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = originalTemplate.name,
                  color = Color.White,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "समय: ${TimeUtils.formatTime12Hour(originalTemplate.timeMinutes)} (${originalTemplate.category})",
                  color = Color.White.copy(alpha = 0.7f),
                  fontSize = 12.sp
                )
              }
            }
          }

          isEdit && originalTemplate != null && updatedTemplate != null -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "🟡 एक्टिविटी में बदलाव:",
                  color = Color(0xFFFFB300),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )

                if (originalTemplate.name != updatedTemplate.name) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("नाम: ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(originalTemplate.name, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(" ➔ ", color = CyanNeon, fontSize = 12.sp)
                    Text(updatedTemplate.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }

                if (originalTemplate.timeMinutes != updatedTemplate.timeMinutes) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("समय: ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(TimeUtils.formatTime12Hour(originalTemplate.timeMinutes), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(" ➔ ", color = CyanNeon, fontSize = 12.sp)
                    Text(TimeUtils.formatTime12Hour(updatedTemplate.timeMinutes), color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }

                if (originalTemplate.category != updatedTemplate.category) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("श्रेणी: ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(originalTemplate.category, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(" ➔ ", color = CyanNeon, fontSize = 12.sp)
                    Text(updatedTemplate.category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }

                if (originalTemplate.priority != updatedTemplate.priority) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("प्राथमिकता: ", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(originalTemplate.priority, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(" ➔ ", color = CyanNeon, fontSize = 12.sp)
                    Text(updatedTemplate.priority, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }

        // Alarm Impact Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
        ) {
          Row(verticalAlignment = Alignment.Top) {
            Icon(
              imageVector = Icons.Default.Alarm,
              contentDescription = null,
              tint = CyanNeon,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "अलार्म और रिमाइंडर्स पर प्रभाव:",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = when {
                  isDelete -> "हटाए गए एक्टिविटी का अलार्म स्वतः रद्द हो जाएगा।"
                  isNew && updatedTemplate != null -> "नया अलार्म ${TimeUtils.formatTime12Hour(updatedTemplate.timeMinutes)} पर शेड्यूल किया जाएगा।"
                  isEdit && updatedTemplate != null -> "अलार्म अपडेट होकर ${TimeUtils.formatTime12Hour(updatedTemplate.timeMinutes)} पर रीशेड्यूल होगा।"
                  else -> "अलार्म शेड्यूल स्वतः रीफ़्रेश किया जाएगा।"
                } + if (smartReminderMinutes > 0) " साथ ही ${smartReminderMinutes}m पूर्व सूचना भी अपडेट होगी।" else "",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isDelete) Color(0xFFFF5252) else CyanNeon,
          contentColor = if (isDelete) Color.White else DarkBackground
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("preview_confirm_btn")
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (isDelete) "हटाना कन्फर्म करें" else "बदलाव सेव करें",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onCancel,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
      ) {
        Text("Cancel", fontSize = 13.sp)
      }
    }
  )
}
