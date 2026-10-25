package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import kotlinx.coroutines.launch

enum class ExportFormatTab(val title: String) {
  AI_PROMPT("AI Analysis"),
  JSON("JSON Backup"),
  CSV("CSV Export"),
  MARKDOWN("Markdown"),
  TXT("Plain TXT"),
  IMPORT("Import Restore")
}

@Composable
fun ExportImportDialog(
  initialTab: ExportFormatTab = ExportFormatTab.AI_PROMPT,
  onDismiss: () -> Unit,
  onGenerateContent: suspend (ExportFormatTab) -> String,
  onImportJson: (String, (Boolean, String) -> Unit) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedTab by remember { mutableStateOf(initialTab) }
  var contentText by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(true) }
  var importInputText by remember { mutableStateOf("") }
  var importStatusMessage by remember { mutableStateOf<String?>(null) }
  var isImportError by remember { mutableStateOf(false) }

  LaunchedEffect(selectedTab) {
    if (selectedTab != ExportFormatTab.IMPORT) {
      isLoading = true
      contentText = onGenerateContent(selectedTab)
      isLoading = false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = DarkSurface,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(24.dp))
      .testTag("export_import_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Data Management & Export",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Local-first backups & AI dossiers",
            color = TextSecondary,
            fontSize = 11.sp
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Format Selector Tabs (Horizontal Scroll)
        val tabScroll = rememberScrollState()
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(tabScroll),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ExportFormatTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
                .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable { selectedTab = tab }
                .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
              Text(
                text = tab.title,
                color = if (isSelected) Color(0xFF00363D) else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == ExportFormatTab.IMPORT) {
          // Import Tab
          Text(
            text = "Paste JSON Backup Below:",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedTextField(
            value = importInputText,
            onValueChange = { importInputText = it },
            placeholder = { Text("{\n  \"version\": 1,\n  \"dayTasks\": [...]\n}") },
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = CyanNeon,
              unfocusedBorderColor = DarkSurfaceBorder,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
          )

          if (importStatusMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = importStatusMessage ?: "",
              color = if (isImportError) Color(0xFFEF4444) else StatusComplete,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              if (importInputText.isNotBlank()) {
                onImportJson(importInputText) { success, message ->
                  isImportError = !success
                  importStatusMessage = message
                  if (success) {
                    Toast.makeText(context, "Data imported successfully!", Toast.LENGTH_SHORT).show()
                  }
                }
              }
            },
            enabled = importInputText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00363D)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Restore From Backup", fontWeight = FontWeight.Bold)
          }
        } else {
          // Export Preview
          if (isLoading) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(color = CyanNeon, modifier = Modifier.size(32.dp))
            }
          } else {
            val contentScroll = rememberScrollState()
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkBackground)
                .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
                .verticalScroll(contentScroll)
                .padding(14.dp)
            ) {
              Text(
                text = contentText,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Copy & Share
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Life Tracker Export", contentText)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "Copied ${selectedTab.title} to clipboard!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Color(0xFF00363D)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              Button(
                onClick = {
                  val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, contentText)
                    type = "text/plain"
                  }
                  val shareIntent = Intent.createChooser(sendIntent, "Export Life Tracker Data")
                  context.startActivity(shareIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = Color.White),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done", color = CyanNeon, fontWeight = FontWeight.Bold)
      }
    }
  )
}
