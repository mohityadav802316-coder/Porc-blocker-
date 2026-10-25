package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow

object BatteryOptimizationHelper {

  /**
   * Checks whether the app is whitelisted from battery optimization.
   */
  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
      powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    } else {
      true
    }
  }

  /**
   * Attempts to open the battery optimization exemption prompt directly for this package,
   * falling back to the system battery optimization list if needed.
   */
  @SuppressLint("BatteryLife")
  fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
          data = Uri.parse("package:${context.packageName}")
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
      } catch (e: Exception) {
        // Fallback to general battery settings
        try {
          val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(fallback)
        } catch (_: Exception) {
          // Last resort: open app detail settings
          try {
            val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
              data = Uri.parse("package:${context.packageName}")
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appDetails)
          } catch (_: Exception) {}
        }
      }
    }
  }

  /**
   * Returns true if device manufacturer is known for aggressive background killers
   * (Xiaomi / Redmi / Poco, Oppo, Vivo, Realme, OnePlus, Samsung).
   */
  fun isAggressiveManufacturer(): Boolean {
    val mfg = Build.MANUFACTURER.lowercase()
    return mfg.contains("xiaomi") || mfg.contains("redmi") || mfg.contains("poco") ||
      mfg.contains("oppo") || mfg.contains("vivo") || mfg.contains("realme") ||
      mfg.contains("oneplus") || mfg.contains("samsung") || mfg.contains("huawei")
  }
}

@Composable
fun BatteryOptimizationDialog(
  onDismiss: () -> Unit,
  onOpenSettings: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .border(1.dp, Brush.horizontalGradient(listOf(CyanNeon.copy(alpha = 0.5f), VioletGlow)), RoundedCornerShape(20.dp)),
      color = DarkSurface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(StatusPartial.copy(alpha = 0.15f))
              .border(1.dp, StatusPartial.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.BatteryAlert,
              contentDescription = "Battery Alert",
              tint = StatusPartial,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Battery Optimization Notice",
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "बैकग्राउंड अलार्म विश्वसनीयता",
              fontSize = 13.sp,
              color = TextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Xiaomi, Vivo, Oppo, OnePlus व Samsung फोन बैकग्राउंड ऐप्स को बंद कर देते हैं, जिससे अलार्म तय समय पर नहीं बज पाता।",
          fontSize = 13.sp,
          color = TextSecondary,
          lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "कृपया दो सेटिंग्स ऑन करें:",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = CyanNeon
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "1. App Info > Battery > 'Unrestricted' (प्रतिबंध रहित)\n2. Autostart / Background Run की अनुमति दें",
              fontSize = 12.sp,
              color = TextPrimary,
              lineHeight = 18.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("battery_dialog_dismiss"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder)
          ) {
            Text("बाद में (Later)", fontSize = 13.sp)
          }

          Button(
            onClick = {
              onOpenSettings()
              onDismiss()
            },
            modifier = Modifier
              .weight(1.3f)
              .height(48.dp)
              .testTag("battery_dialog_open_settings"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = CyanNeon,
              contentColor = DarkBackground
            )
          ) {
            Icon(
              imageVector = Icons.Default.OpenInNew,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
