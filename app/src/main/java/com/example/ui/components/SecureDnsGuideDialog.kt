package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.LocalDnsVpnManager
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.WarmOffWhite

/**
 * Dialog displaying guidelines to prevent browsers from bypassing
 * Local DNS filters using Secure DNS (DoH).
 */
@Composable
fun SecureDnsGuideDialog(
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianCharcoal,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.FlashOn,
          contentDescription = null,
          tint = GoldBrass,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = LocalDnsVpnManager.SECURE_DNS_GUIDE_TITLE,
          color = WarmOffWhite,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = LocalDnsVpnManager.SECURE_DNS_GUIDE_TEXT,
          color = WarmOffWhite.copy(alpha = 0.9f),
          fontSize = 12.sp,
          lineHeight = 17.sp
        )
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(10.dp)
      ) {
        Text("समझ गया", color = ObsidianElevated, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  )
}
