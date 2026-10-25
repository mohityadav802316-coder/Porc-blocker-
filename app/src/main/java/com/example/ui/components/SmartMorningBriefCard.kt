package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.MeditationType
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.VioletNeon
import com.example.util.TimeUtils

@Composable
fun SmartMorningBriefCard(
  userName: String,
  currentStreak: Int,
  tasks: List<DayTaskEntity>,
  smartReminderMinutes: Int,
  onStartMeditation: (MeditationType, Int) -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(true) }

  val priorityTasks = tasks.filter { it.priority == "HIGH" || it.priority == "IMPORTANT" }
  val highPriorityCount = tasks.count { it.priority == "HIGH" }
  val importantCount = tasks.count { it.priority == "IMPORTANT" }
  val totalTasks = tasks.size

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(
            DarkSurfaceElevated,
            DarkSurface.copy(alpha = 0.95f)
          )
        )
      )
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          colors = listOf(
            Color(0xFFFFB300).copy(alpha = 0.5f),
            DarkSurfaceBorder,
            CyanNeon.copy(alpha = 0.35f)
          )
        ),
        shape = RoundedCornerShape(20.dp)
      )
      .padding(16.dp)
      .testTag("smart_morning_brief_card")
  ) {
    Column {
      // Header: Greeting + Streak + Expand/Collapse
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFFFFB300).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.WbSunny,
              contentDescription = "Morning Brief",
              tint = Color(0xFFFFB300),
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = "सुप्रभात, $userName! 🌅",
              color = Color.White,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Smart Morning Brief",
              color = Color.White.copy(alpha = 0.6f),
              fontSize = 11.sp
            )
          }
        }

        // Streak badge pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF9100).copy(alpha = 0.15f))
            .border(1.dp, Color(0xFFFF9100).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "🔥 ${currentStreak}d Streak",
            color = Color(0xFFFF9100),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
          onClick = { isExpanded = !isExpanded },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = "Toggle expand",
            tint = Color.White.copy(alpha = 0.7f)
          )
        }
      }

      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          // Priority tasks highlight
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkBackground.copy(alpha = 0.6f))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "आज के प्राथमिकता कार्य (Priority Tasks)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                }
                Text(
                  text = "$totalTasks कुल एक्टिविटीज",
                  color = Color.White.copy(alpha = 0.5f),
                  fontSize = 11.sp
                )
              }

              if (priorityTasks.isNotEmpty()) {
                priorityTasks.take(3).forEach { task ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    val pColor = if (task.priority == "HIGH") Color(0xFFFF5252) else Color(0xFFFFB300)
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(pColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = task.name,
                      color = Color.White.copy(alpha = 0.9f),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.Medium,
                      modifier = Modifier.weight(1f)
                    )
                    Text(
                      text = TimeUtils.formatTime12Hour(task.timeMinutes),
                      color = CyanNeon,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
                if (priorityTasks.size > 3) {
                  Text(
                    text = "+ ${priorityTasks.size - 3} अन्य महत्वपूर्ण कार्य",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                  )
                }
              } else {
                Text(
                  text = "आज कोई विशेष हाई-प्रायोरिटी टास्क नहीं है। पूरे दिन का रूटीन संतुलित है।",
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 11.sp
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Meditation suggestion card
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(VioletNeon.copy(alpha = 0.12f))
              .border(1.dp, VioletNeon.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
              .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = "Meditation",
                tint = VioletNeon,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "दैनिक ध्यान अनुशंसा (10 Min)",
                  color = Color.White,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "श्वास ध्यान (Breathing) से दिन की शुरुआत करें",
                  color = Color.White.copy(alpha = 0.6f),
                  fontSize = 10.sp
                )
              }
            }

            Button(
              onClick = { onStartMeditation(MeditationType.BREATHING, 10) },
              colors = ButtonDefaults.buttonColors(
                containerColor = VioletNeon,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Text("शुरू करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Reminders info row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Alarms",
                tint = CyanNeon,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (smartReminderMinutes > 0)
                  "स्मार्ट रिमाइंडर एक्टिव (${smartReminderMinutes}m पूर्व सूचना)"
                else
                  "शेड्यूल अलार्म्स ऑन-टाइम चालू हैं",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
              )
            }

            Text(
              text = "आज का लक्ष्य: 100%",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
