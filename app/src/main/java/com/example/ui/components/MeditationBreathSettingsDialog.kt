package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.meditation.BreathAudioPlayer
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeditationBreathSettingsDialog(
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val breathPlayer = remember { BreathAudioPlayer(context) }

  var breathSoundsEnabled by remember {
    mutableStateOf(BreathAudioPlayer.isBreathSoundsEnabled(context))
  }

  var inhaleType by remember {
    mutableStateOf(BreathAudioPlayer.getInhaleSoundType(context))
  }
  var inhaleName by remember {
    mutableStateOf(BreathAudioPlayer.getInhaleCustomName(context) ?: "कोई फ़ाइल चयनित नहीं")
  }
  var isPreviewingInhale by remember { mutableStateOf(false) }

  var exhaleType by remember {
    mutableStateOf(BreathAudioPlayer.getExhaleSoundType(context))
  }
  var exhaleName by remember {
    mutableStateOf(BreathAudioPlayer.getExhaleCustomName(context) ?: "कोई फ़ाइल चयनित नहीं")
  }
  var isPreviewingExhale by remember { mutableStateOf(false) }

  // Cleanup on dialog dismiss
  DisposableEffect(Unit) {
    onDispose {
      breathPlayer.stop()
    }
  }

  // OpenDocument launcher for Inhale
  val inhaleFilePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)

        val fileName = queryFileName(context, uri) ?: "inhale_audio.mp3"
        BreathAudioPlayer.setInhaleCustomSound(context, uri.toString(), fileName)
        inhaleType = BreathAudioPlayer.TYPE_CUSTOM
        inhaleName = fileName
        Toast.makeText(context, "Inhale sound: $fileName सेट किया गया", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "फ़ाइल लोड करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // OpenDocument launcher for Exhale
  val exhaleFilePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)

        val fileName = queryFileName(context, uri) ?: "exhale_audio.mp3"
        BreathAudioPlayer.setExhaleCustomSound(context, uri.toString(), fileName)
        exhaleType = BreathAudioPlayer.TYPE_CUSTOM
        exhaleName = fileName
        Toast.makeText(context, "Exhale sound: $fileName सेट किया गया", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "फ़ाइल लोड करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .clip(RoundedCornerShape(24.dp))
        .border(1.2.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
      color = DarkBackground
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(CyanNeon.copy(alpha = 0.18f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Air, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "श्वास ध्वनियाँ (Breath Sounds)",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Inhale & Exhale Audio Settings",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master Switch: Breath Sounds ON / OFF
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, if (breathSoundsEnabled) CyanNeon.copy(alpha = 0.4f) else DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("श्वास ध्वनि मार्गदर्शन (Breath Sounds)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              Text(
                text = if (breathSoundsEnabled) "सक्रिय: श्वास अंदर/बाहर ध्वनि बजेगी, होल्ड पर मौन रहेगा" else "निष्क्रिय: संपूर्ण चक्र शांत रहेगा",
                color = TextMuted,
                fontSize = 11.sp
              )
            }
            Switch(
              checked = breathSoundsEnabled,
              onCheckedChange = {
                breathSoundsEnabled = it
                BreathAudioPlayer.setBreathSoundsEnabled(context, it)
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = CyanNeon,
                checkedTrackColor = CyanNeon.copy(alpha = 0.35f)
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 1: INHALE SOUND (श्वास अंदर लेने की ध्वनि)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("१. श्वास अंदर (Inhale Sound)", color = CyanNeon, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              // Preview button
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isPreviewingInhale) CyanNeon else DarkSurface)
                  .clickable {
                    if (isPreviewingInhale) {
                      breathPlayer.stop()
                      isPreviewingInhale = false
                    } else {
                      breathPlayer.stop()
                      breathPlayer.previewInhale()
                      isPreviewingInhale = true
                      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isPreviewingInhale = false
                      }, 4000L)
                    }
                  }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    if (isPreviewingInhale) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPreviewingInhale) Color(0xFF003038) else CyanNeon,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    if (isPreviewingInhale) "रुकें" else "सुनें",
                    color = if (isPreviewingInhale) Color(0xFF003038) else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            // Radio 1: Default Procedural Sound
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (inhaleType == BreathAudioPlayer.TYPE_DEFAULT) CyanNeon.copy(alpha = 0.12f) else DarkSurface)
                .clickable {
                  inhaleType = BreathAudioPlayer.TYPE_DEFAULT
                  BreathAudioPlayer.resetInhaleToDefault(context)
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .border(2.dp, if (inhaleType == BreathAudioPlayer.TYPE_DEFAULT) CyanNeon else TextMuted, CircleShape)
                  .background(if (inhaleType == BreathAudioPlayer.TYPE_DEFAULT) CyanNeon else Color.Transparent)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("डिफ़ॉल्ट श्वास ध्वनि (Organic Soft Breath)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("सॉफ्ट नेचुरल एयर फ्लो, समय के साथ मेल खाती ध्वनि", color = TextMuted, fontSize = 10.sp)
              }
            }

            // Radio 2: Custom Sound from Storage
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (inhaleType == BreathAudioPlayer.TYPE_CUSTOM) CyanNeon.copy(alpha = 0.12f) else DarkSurface)
                .clickable {
                  inhaleFilePicker.launch(arrayOf("audio/*"))
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (inhaleType == BreathAudioPlayer.TYPE_CUSTOM) CyanNeon else TextMuted, CircleShape)
                    .background(if (inhaleType == BreathAudioPlayer.TYPE_CUSTOM) CyanNeon else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text("कस्टम ऑडियो फ़ाइल (फ़ोन से चुनें)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                  Text(
                    text = if (inhaleType == BreathAudioPlayer.TYPE_CUSTOM) inhaleName else "फ़ोन मेमोरी से कोई भी ऑडियो चुनें",
                    color = if (inhaleType == BreathAudioPlayer.TYPE_CUSTOM) CyanNeon else TextMuted,
                    fontSize = 10.sp
                  )
                }
              }

              Icon(Icons.Default.FolderOpen, contentDescription = "Pick file", tint = CyanNeon, modifier = Modifier.size(18.dp))
            }

            // Reset Inhale Button (if custom)
            if (inhaleType == BreathAudioPlayer.TYPE_CUSTOM) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                Text(
                  text = "डिफ़ॉल्ट पर रीसेट करें",
                  color = TextMuted,
                  fontSize = 11.sp,
                  modifier = Modifier
                    .clickable {
                      BreathAudioPlayer.resetInhaleToDefault(context)
                      inhaleType = BreathAudioPlayer.TYPE_DEFAULT
                      inhaleName = "कोई फ़ाइल चयनित नहीं"
                    }
                    .padding(4.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SECTION 2: EXHALE SOUND (श्वास बाहर छोड़ने की ध्वनि)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("२. श्वास बाहर (Exhale Sound)", color = VioletNeon, fontSize = 14.sp, fontWeight = FontWeight.Bold)
              // Preview button
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isPreviewingExhale) VioletNeon else DarkSurface)
                  .clickable {
                    if (isPreviewingExhale) {
                      breathPlayer.stop()
                      isPreviewingExhale = false
                    } else {
                      breathPlayer.stop()
                      breathPlayer.previewExhale()
                      isPreviewingExhale = true
                      android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isPreviewingExhale = false
                      }, 4000L)
                    }
                  }
                  .padding(horizontal = 10.dp, vertical = 5.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    if (isPreviewingExhale) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPreviewingExhale) Color.White else VioletNeon,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    if (isPreviewingExhale) "रुकें" else "सुनें",
                    color = if (isPreviewingExhale) Color.White else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            // Radio 1: Default Procedural Sound
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (exhaleType == BreathAudioPlayer.TYPE_DEFAULT) VioletNeon.copy(alpha = 0.12f) else DarkSurface)
                .clickable {
                  exhaleType = BreathAudioPlayer.TYPE_DEFAULT
                  BreathAudioPlayer.resetExhaleToDefault(context)
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .border(2.dp, if (exhaleType == BreathAudioPlayer.TYPE_DEFAULT) VioletNeon else TextMuted, CircleShape)
                  .background(if (exhaleType == BreathAudioPlayer.TYPE_DEFAULT) VioletNeon else Color.Transparent)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text("डिफ़ॉल्ट श्वास ध्वनि (Gentle Exhale Release)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("तनावमुक्त करने वाली प्राकृतिक धीमी श्वास ध्वनि", color = TextMuted, fontSize = 10.sp)
              }
            }

            // Radio 2: Custom Sound from Storage
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (exhaleType == BreathAudioPlayer.TYPE_CUSTOM) VioletNeon.copy(alpha = 0.12f) else DarkSurface)
                .clickable {
                  exhaleFilePicker.launch(arrayOf("audio/*"))
                }
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (exhaleType == BreathAudioPlayer.TYPE_CUSTOM) VioletNeon else TextMuted, CircleShape)
                    .background(if (exhaleType == BreathAudioPlayer.TYPE_CUSTOM) VioletNeon else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text("कस्टम ऑडियो फ़ाइल (फ़ोन से चुनें)", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                  Text(
                    text = if (exhaleType == BreathAudioPlayer.TYPE_CUSTOM) exhaleName else "फ़ोन मेमोरी से कोई भी ऑडियो चुनें",
                    color = if (exhaleType == BreathAudioPlayer.TYPE_CUSTOM) VioletNeon else TextMuted,
                    fontSize = 10.sp
                  )
                }
              }

              Icon(Icons.Default.FolderOpen, contentDescription = "Pick file", tint = VioletNeon, modifier = Modifier.size(18.dp))
            }

            // Reset Exhale Button (if custom)
            if (exhaleType == BreathAudioPlayer.TYPE_CUSTOM) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                Text(
                  text = "डिफ़ॉल्ट पर रीसेट करें",
                  color = TextMuted,
                  fontSize = 11.sp,
                  modifier = Modifier
                    .clickable {
                      BreathAudioPlayer.resetExhaleToDefault(context)
                      exhaleType = BreathAudioPlayer.TYPE_DEFAULT
                      exhaleName = "कोई फ़ाइल चयनित नहीं"
                    }
                    .padding(4.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Explanatory note
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Text(
            text = "✨ होल्ड (Hold) और विराम (Rest) चरणों में पूर्ण मौन रहेगा। कस्टम ऑडियो यदि लंबा होगा तो चरण समाप्त होते ही स्वतः स्मूद फ़ेड-आउट हो जाएगा।",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Done button
        Button(
          onClick = {
            breathPlayer.stop()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("save_breath_settings_button")
        ) {
          Text("पूर्ण (Done)", color = Color(0xFF003038), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

private fun queryFileName(context: Context, uri: Uri): String? {
  return try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIndex != -1 && cursor.moveToFirst()) {
        cursor.getString(nameIndex)
      } else null
    }
  } catch (_: Exception) {
    uri.lastPathSegment
  }
}
