package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.focus.ProtectionHealthManager
import com.example.ui.theme.WarmOffWhite

/**
 * Top Banner on Home / TodayScreen:
 * - Green if 100% healthy: "🟢 100% सुरक्षा सक्रिय (सभी घटक चालू)"
 * - Red if any security component is down: "🚨 सुरक्षा अधूरी है: X समस्याएँ • ठीक करें"
 */
@Composable
fun ProtectionHealthBanner(
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var report by remember { mutableStateOf(ProtectionHealthManager.getHealthReport(context)) }

  LaunchedEffect(Unit) {
    report = ProtectionHealthManager.getHealthReport(context)
  }

  val isHealthy = report.isAllHealthy

  val bgColor = if (isHealthy) Color(0xFF132B1D) else Color(0xFF3B1818)
  val borderColor = if (isHealthy) Color(0xFF10B981) else Color(0xFFEF4444)
  val iconColor = if (isHealthy) Color(0xFF34D399) else Color(0xFFF87171)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .border(1.dp, borderColor, RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Icon(
          if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = if (isHealthy) "🟢 100% सुरक्षा सक्रिय" else "🚨 सुरक्षा अधूरी है (${report.totalCount - report.healthyCount} समस्या)",
            color = WarmOffWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (isHealthy) "सभी 8 सुरक्षा घटक सुचारू रूप से कार्य कर रहे हैं" else report.failedSummary,
            color = if (isHealthy) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
            fontSize = 11.sp,
            maxLines = 1
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = if (isHealthy) "विवरण" else "ठीक करें",
          color = iconColor,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        Icon(
          Icons.Default.ChevronRight,
          contentDescription = null,
          tint = iconColor,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}
