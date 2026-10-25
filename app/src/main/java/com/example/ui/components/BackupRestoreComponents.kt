package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backup.BackupItemInfo
import com.example.backup.BackupManager
import com.example.backup.BackupPreferences
import com.example.backup.DailyBackupWorker
import com.example.backup.RestoreManager
import com.example.backup.RestoreResult
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianBorderSubtle
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import kotlinx.coroutines.launch

/**
 * First-launch welcome dialog shown on a fresh install when the database is empty.
 * Gives the user two prominent choices: Start Fresh or Restore from Backup.
 */
@Composable
fun FreshInstallWelcomeDialog(
  onStartFresh: () -> Unit,
  onOpenRestore: () -> Unit
) {
  AlertDialog(
    onDismissRequest = {}, // Disallow accidental outside tap dismissal
    containerColor = ObsidianCharcoal,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .border(1.dp, GoldBrass.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
    title = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(GoldBrass.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = GoldBrass,
            modifier = Modifier.size(28.dp)
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Life Tracker में आपका स्वागत है",
          color = WarmOffWhite,
          fontSize = 19.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "क्या आप नए सिरे से शुरुआत करना चाहते हैं या अपने पुराने बैकअप से सारा डेटा वापस लाना चाहते हैं?",
          color = WarmParchment,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          textAlign = TextAlign.Center
        )

        // Option 1: Restore from Backup
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianElevated)
            .border(1.dp, GoldBrass.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable { onOpenRestore() }
            .padding(16.dp)
            .testTag("welcome_restore_option_btn")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(GoldBrass.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = GoldBrass,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "बैकअप से वापस लाएँ",
                color = GoldBrass,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "पुराने रूटीन, कार्य, ध्यान, डायरी व सेटिंग्स पुनर्स्थापित करें",
                color = WarmMuted,
                fontSize = 11.sp
              )
            }
          }
        }

        // Option 2: Start Fresh
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianElevated)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .clickable { onStartFresh() }
            .padding(16.dp)
            .testTag("welcome_start_fresh_option_btn")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(SageGreen.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = SageGreen,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "नई शुरुआत (Start Fresh)",
                color = WarmOffWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "डिफ़ॉल्ट 5 AM - 10 PM समय सारणी के साथ शुरुआत करें",
                color = WarmMuted,
                fontSize = 11.sp
              )
            }
          }
        }
      }
    },
    confirmButton = {}
  )
}

/**
 * Dialog for scanning and selecting backups from a folder or single ZIP file.
 */
@Composable
fun RestoreBackupsDialog(
  initialFolderUri: Uri?,
  onDismiss: () -> Unit,
  onRestoreSuccess: (RestoreResult) -> Unit,
  onOpenPermissionsStatus: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var currentFolderUri by remember { mutableStateOf(initialFolderUri) }
  var backupsList by remember { mutableStateOf<List<BackupItemInfo>>(emptyList()) }
  var selectedBackup by remember { mutableStateOf<BackupItemInfo?>(null) }
  var isScanning by remember { mutableStateOf(false) }
  var isRestoring by remember { mutableStateOf(false) }
  var restoreErrorMessage by remember { mutableStateOf<String?>(null) }
  var completedResult by remember { mutableStateOf<RestoreResult?>(null) }
  var showOverwriteConfirmDialog by remember { mutableStateOf(false) }

  // Folder Picker Launcher (OpenDocumentTree)
  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri ->
    if (uri != null) {
      try {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
          android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        val docFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
        val displayName = docFile?.name ?: uri.lastPathSegment ?: "Backup Folder"
        BackupPreferences.setTreeUri(context, uri, displayName)
        currentFolderUri = uri
      } catch (e: Exception) {
        Toast.makeText(context, "फ़ोल्डर अनुमति सहेजने में विफल: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Single ZIP File Picker Launcher
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      coroutineScope.launch {
        isScanning = true
        val singleItem = RestoreManager.inspectSingleZip(context, uri)
        backupsList = listOf(singleItem)
        selectedBackup = if (singleItem.isValid) singleItem else null
        isScanning = false
      }
    }
  }

  // Auto-scan whenever currentFolderUri changes
  LaunchedEffect(currentFolderUri) {
    val uri = currentFolderUri
    if (uri != null) {
      isScanning = true
      restoreErrorMessage = null
      val items = RestoreManager.scanBackupsInFolder(context, uri)
      backupsList = items
      // Pre-select newest valid backup
      selectedBackup = items.firstOrNull { it.isValid }
      isScanning = false
    }
  }

  AlertDialog(
    onDismissRequest = {
      if (!isRestoring) onDismiss()
    },
    containerColor = ObsidianCharcoal,
    shape = RoundedCornerShape(24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(8.dp)
      .border(1.dp, ObsidianBorder, RoundedCornerShape(24.dp)),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = if (completedResult != null) "पुनर्स्थापना संपन्न! 🎉" else "बैकअप पुनर्स्थापित करें",
          color = WarmOffWhite,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        if (!isRestoring) {
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmMuted)
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        if (completedResult != null) {
          // Success State: show summary and prompt to re-grant permissions
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(SageGreen.copy(alpha = 0.12f))
              .border(1.dp, SageGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
              .padding(14.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = completedResult!!.message,
                color = WarmOffWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = "महत्वपूर्ण सूचना: एंड्रॉइड अनइंस्टॉल के बाद सभी अनुमतियां हटा देता है। अलार्म, ब्लैक स्क्रीन और शॉर्ट-कंटेंट ट्रैकर ठीक से चलने के लिए कृपया आवश्यक अनुमतियां पुनः प्रदान करें।",
                color = WarmParchment,
                fontSize = 12.sp,
                lineHeight = 17.sp
              )
            }
          }

          Button(
            onClick = {
              onDismiss()
              onOpenPermissionsStatus()
            },
            colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianCharcoal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("अनुमतियां एवं सिस्टम स्थिति जांचें", fontWeight = FontWeight.Bold)
          }
        } else {
          // Normal Restore Flow: Pick Folder or File, List Backups, and Confirm
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { folderPickerLauncher.launch(null) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.FolderOpen, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("फ़ोल्डर चुनें", color = GoldBrass, fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = { filePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.History, contentDescription = null, tint = WarmOffWhite, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("ZIP फ़ाइल चुनें", color = WarmOffWhite, fontSize = 12.sp)
            }
          }

          if (isScanning) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(color = GoldBrass, modifier = Modifier.size(24.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Text("बैकअप फ़ाइलें स्कैन की जा रही हैं...", color = WarmMuted, fontSize = 13.sp)
            }
          } else if (backupsList.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianElevated)
                .padding(16.dp)
            ) {
              Text(
                text = if (currentFolderUri == null)
                  "कृपया वह फ़ोल्डर चुनें जहां आपके Life Tracker बैकअप रखे हैं (जैसे Documents > LifeTracker Backup)।"
                else
                  "इस फ़ोल्डर में कोई मान्य Life Tracker बैकअप फ़ाइल नहीं मिली।",
                color = WarmMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
              )
            }
          } else {
            Text(
              text = "उपलब्ध बैकअप फ़ाइलें (${backupsList.size}):",
              color = WarmParchment,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )

            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(backupsList) { item ->
                val isSelected = selectedBackup?.fileUri == item.fileUri
                val isCorrupted = !item.isValid

                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                      if (isSelected) GoldBrass.copy(alpha = 0.12f)
                      else if (isCorrupted) DustyRose.copy(alpha = 0.08f)
                      else ObsidianElevated
                    )
                    .border(
                      width = 1.dp,
                      color = if (isSelected) GoldBrass
                      else if (isCorrupted) DustyRose.copy(alpha = 0.3f)
                      else ObsidianBorderSubtle,
                      shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = item.isValid && !isRestoring) {
                      selectedBackup = item
                    }
                    .padding(12.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    if (item.isValid) {
                      RadioButton(
                        selected = isSelected,
                        onClick = { selectedBackup = item },
                        colors = RadioButtonDefaults.colors(selectedColor = GoldBrass, unselectedColor = WarmMuted)
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Corrupted",
                        tint = DustyRose,
                        modifier = Modifier
                          .size(24.dp)
                          .padding(start = 6.dp, end = 6.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                      ) {
                        Text(
                          text = item.formattedDate,
                          color = if (item.isValid) WarmOffWhite else DustyRose,
                          fontSize = 14.sp,
                          fontWeight = FontWeight.SemiBold
                        )
                        Text(
                          text = item.formattedSize,
                          color = WarmMuted,
                          fontSize = 12.sp
                        )
                      }
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text = if (item.isValid) item.recordCountsText else "त्रुटि: ${item.errorReason ?: "क्षतिग्रस्त फ़ाइल"}",
                        color = if (item.isValid) WarmMuted else DustyRose,
                        fontSize = 11.sp
                      )
                    }
                  }
                }
              }
            }
          }

          if (restoreErrorMessage != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DustyRose.copy(alpha = 0.15f))
                .border(1.dp, DustyRose.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(10.dp)
            ) {
              Text(
                text = restoreErrorMessage!!,
                color = DustyRose,
                fontSize = 12.sp
              )
            }
          }

          if (isRestoring) {
            Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              LinearProgressIndicator(color = GoldBrass, modifier = Modifier.fillMaxWidth())
              Text(
                text = "डेटाबेस पुनर्स्थापित किया जा रहा है...",
                color = GoldBrass,
                fontSize = 12.sp
              )
            }
          }
        }
      }
    },
    confirmButton = {
      if (completedResult == null && selectedBackup != null && !isRestoring) {
        Button(
          onClick = {
            // Show explicit overwrite warning before executing restore
            showOverwriteConfirmDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianCharcoal),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("execute_restore_trigger_btn")
        ) {
          Text("पुनर्स्थापित करें (Restore Now)", fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      if (completedResult == null && !isRestoring) {
        TextButton(onClick = onDismiss) {
          Text("रद्द करें", color = WarmMuted)
        }
      }
    }
  )

  // Explicit Overwrite Warning Dialog before replacing database
  if (showOverwriteConfirmDialog && selectedBackup != null) {
    AlertDialog(
      onDismissRequest = { showOverwriteConfirmDialog = false },
      containerColor = ObsidianCharcoal,
      shape = RoundedCornerShape(20.dp),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .border(1.dp, GoldBrass.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
      icon = {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(32.dp))
      },
      title = {
        Text(
          text = "डेटा अधिलेखन चेतावनी (Data Overwrite)",
          color = WarmOffWhite,
          fontSize = 17.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "यह बैकअप पुनर्स्थापित करने पर वर्तमान का डेटा (टास्क, नोट्स, रूटीन) इस बैकअप से बदल दिया जाएगा।",
            color = WarmParchment,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
          Text(
            text = "सुरक्षा आश्वासन: पुनर्स्थापना शुरू करने से पहले वर्तमान डेटा की एक स्थानीय सुरक्षा प्रतिलिपि (Safety Backup) स्वतः बना ली जाएगी।",
            color = SageGreen,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showOverwriteConfirmDialog = false
            isRestoring = true
            restoreErrorMessage = null
            coroutineScope.launch {
              val targetBackup = selectedBackup!!
              val res = RestoreManager.performRestore(
                context = context,
                backupZipUri = targetBackup.fileUri,
                rememberFolderTreeUri = currentFolderUri,
                rememberFolderDisplayName = BackupPreferences.getFolderDisplayName(context)
              )
              isRestoring = false
              if (res.isSuccess) {
                val r = res.getOrThrow()
                completedResult = r
                onRestoreSuccess(r)
              } else {
                restoreErrorMessage = res.exceptionOrNull()?.localizedMessage
                  ?: "पुनर्स्थापना में अज्ञात त्रुटि आई"
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianCharcoal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("confirm_overwrite_restore_btn")
        ) {
          Text("हाँ, पुनर्स्थापित करें", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showOverwriteConfirmDialog = false }) {
          Text("रद्द करें", color = WarmMuted)
        }
      }
    )
  }
}

/**
 * Step 1: Warning Dialog shown when the user chooses "New User / Reset App".
 * Explicitly explains what will happen and provides three clear choices:
 * [Cancel]
 * [Backup First]
 * [Continue]
 */
@Composable
fun ResetAppWarningDialog(
  onDismiss: () -> Unit,
  onBackupFirst: () -> Unit,
  onContinue: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = ObsidianCharcoal,
    shape = RoundedCornerShape(22.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .border(1.dp, DustyRose.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(DustyRose.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = DustyRose, modifier = Modifier.size(28.dp))
      }
    },
    title = {
      Text(
        text = "ऐप रीसेट चेतावनी (New User / Reset App)",
        color = WarmOffWhite,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "यह आपके मौजूदा Life Tracker data को हटाकर ऐप को नए user की तरह शुरू करेगा।",
          color = DustyRose,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          lineHeight = 20.sp
        )
        Text(
          text = "इसके अंतर्गत आपके सभी रूटीन, दैनिक कार्य, व्यक्तिगत नोट्स, ध्यान सत्र, दैनिक चुनौतियाँ और प्रोग्रेस हटा दिए जाएंगे और ऐप खाली रूटीन के साथ नए सिरे से शुरू होगा।",
          color = WarmParchment,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GoldBrass.copy(alpha = 0.12f))
            .border(1.dp, GoldBrass.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(10.dp)
        ) {
          Text(
            text = "💡 अनुशंसित कदम: रीसेट करने से पहले कृपया अपने डेटा का बैकअप ले लें ताकि भविष्य में कभी भी ज़रूरत पड़ने पर आप इसे वापस ला सकें।",
            color = GoldBrass,
            fontSize = 11.sp,
            lineHeight = 16.sp
          )
        }
      }
    },
    confirmButton = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onBackupFirst,
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass, contentColor = ObsidianCharcoal),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("reset_backup_first_btn")
        ) {
          Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("पहले बैकअप लें (Backup First)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("reset_cancel_btn")
          ) {
            Text("रद्द करें", color = WarmMuted, fontSize = 12.sp)
          }

          Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = DustyRose, contentColor = WarmOffWhite),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("reset_continue_btn")
          ) {
            Text("आगे बढ़ें", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }
  )
}

/**
 * Step 2: Final explicit confirmation dialog.
 * Requires user to confirm destruction of data before executing.
 */
@Composable
fun ResetAppConfirmDialog(
  isResetting: Boolean,
  onDismiss: () -> Unit,
  onConfirmReset: () -> Unit
) {
  var confirmationText by remember { mutableStateOf("") }
  val isConfirmationMatched = confirmationText.trim().uppercase() == "RESET"

  AlertDialog(
    onDismissRequest = { if (!isResetting) onDismiss() },
    containerColor = ObsidianCharcoal,
    shape = RoundedCornerShape(22.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .border(1.dp, DustyRose, RoundedCornerShape(22.dp)),
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(DustyRose.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DustyRose, modifier = Modifier.size(28.dp))
      }
    },
    title = {
      Text(
        text = "अंतिम पुष्टि (Final Confirmation)",
        color = WarmOffWhite,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "क्या आप वाकई सारा डेटा मिटाना चाहते हैं? यह क्रिया वापस नहीं ली जा सकती।",
          color = WarmOffWhite,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium,
          lineHeight = 18.sp
        )
        Text(
          text = "पुष्टि करने के लिए नीचे 'RESET' लिखें:",
          color = WarmMuted,
          fontSize = 12.sp
        )

        OutlinedTextField(
          value = confirmationText,
          onValueChange = { confirmationText = it },
          placeholder = { Text("RESET लिखें", color = WarmMuted, fontSize = 13.sp) },
          singleLine = true,
          enabled = !isResetting,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DustyRose,
            unfocusedBorderColor = ObsidianBorder,
            focusedTextColor = WarmOffWhite,
            unfocusedTextColor = WarmOffWhite
          ),
          modifier = Modifier.fillMaxWidth().testTag("reset_confirm_input")
        )

        if (isResetting) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(color = DustyRose, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("डेटा रीसेट किया जा रहा है...", color = DustyRose, fontSize = 12.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onConfirmReset,
        enabled = isConfirmationMatched && !isResetting,
        colors = ButtonDefaults.buttonColors(
          containerColor = DustyRose,
          contentColor = WarmOffWhite,
          disabledContainerColor = DustyRose.copy(alpha = 0.3f),
          disabledContentColor = WarmMuted
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("confirm_reset_final_btn")
      ) {
        Text("हाँ, डेटा मिटाएं (Reset Now)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    },
    dismissButton = {
      if (!isResetting) {
        TextButton(onClick = onDismiss) {
          Text("रद्द करें", color = WarmMuted)
        }
      }
    }
  )
}
