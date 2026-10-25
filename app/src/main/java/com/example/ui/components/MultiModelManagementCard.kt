package com.example.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focus.AiScreenDetectionManager
import com.example.focus.ModelHealthStatus
import com.example.focus.ModelTestReport
import com.example.focus.ModelValidationResult
import com.example.focus.MultiModelClassifierManager
import com.example.focus.NormalizationType
import com.example.focus.TFLiteModelConfig
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Full Multi-Model Classifier Management Section:
 * 1. Pre-import Validation: TFL3 signature, size limit, SHA-256 duplicate, interpreter load & unsupported ops,
 *    tensor shape & layout (NCHW vs NHWC), dry-run NaN/Infinity check, 3-image synthetic sanity check.
 * 2. Live Status Badges on every model card:
 *    - 🟢 Working: (last score, last run sec ago, avg ms)
 *    - 🟡 Warning: specific issue (slow latency, sanity reaction warning, transient failures)
 *    - 🔴 Not working: why not working, clear Hindi reason, and 1-line "क्या करना है" + "दोबारा कोशिश करें" retry button.
 * 3. Circuit Breaker protection: Auto-disables model after 5 consecutive failures, logged in health log.
 * 4. Layout-aware (NCHW/NHWC) Preprocessing & Normalization options including OpenNSFW2 BGR Mean-Subtract.
 * 5. "Test Image" button: select image from gallery to verify models & preprocessing without screenshots.
 * 6. Master switch for AI Screen Vision scanning.
 */
@Composable
fun MultiModelManagementCard(
  isStrictLock: Boolean,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var isAiScanningEnabled by remember { mutableStateOf(AiScreenDetectionManager.isEnabled(context)) }
  var modelStatuses by remember { mutableStateOf(MultiModelClassifierManager.getRuntimeStatuses(context)) }
  var singleThreshold by remember { mutableFloatStateOf(MultiModelClassifierManager.getSingleModelThreshold(context)) }
  var duoThreshold by remember { mutableFloatStateOf(MultiModelClassifierManager.getDuoModelThreshold(context)) }

  var editingConfig by remember { mutableStateOf<TFLiteModelConfig?>(null) }

  // Validation & Import Dialog States
  var isValidating by remember { mutableStateOf(false) }
  var pendingTempFile by remember { mutableStateOf<File?>(null) }
  var validationReport by remember { mutableStateOf<ModelValidationResult?>(null) }
  var validationErrorDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // (Reason, NextStep)
  var showConfirmImportDialog by remember { mutableStateOf(false) }

  // Test Image Dialog States
  var isTestingImage by remember { mutableStateOf(false) }
  var testImageReports by remember { mutableStateOf<List<ModelTestReport>?>(null) }

  var showPinDialog by remember { mutableStateOf(false) }
  var pendingPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
  var pendingActionTitle by remember { mutableStateOf("") }

  fun executeWithPin(actionTitle: String, action: () -> Unit) {
    if (com.example.focus.AccountabilityPinManager.isPinSet(context) &&
      !com.example.focus.AlwaysOnSecurityManager.isTemporarilyUnlocked(context)
    ) {
      pendingPinAction = action
      pendingActionTitle = actionTitle
      showPinDialog = true
    } else {
      action()
    }
  }

  fun refresh() {
    modelStatuses = MultiModelClassifierManager.getRuntimeStatuses(context)
    isAiScanningEnabled = AiScreenDetectionManager.isEnabled(context)
  }

  // File Picker for importing .tflite files
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
    onResult = { uri ->
      if (uri != null) {
        isValidating = true
        coroutineScope.launch(Dispatchers.IO) {
          try {
            val tempFile = File.createTempFile("val_", ".tflite", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
              FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
              }
            } ?: throw IllegalArgumentException("फ़ाइल खोली नहीं जा सकी")

            val report = MultiModelClassifierManager.validateModelFile(context, tempFile)
            withContext(Dispatchers.Main) {
              isValidating = false
              if (report.isSuccess) {
                pendingTempFile = tempFile
                validationReport = report
                showConfirmImportDialog = true
              } else {
                tempFile.delete()
                validationErrorDialog = Pair(
                  report.failureReasonHindi ?: "सत्यापन विफल हुआ।",
                  report.nextStepHindi ?: "कृपया कोई अन्य .tflite फ़ाइल चुनें।"
                )
              }
            }
          } catch (e: Exception) {
            withContext(Dispatchers.Main) {
              isValidating = false
              validationErrorDialog = Pair(
                "फ़ाइल पढ़ने में त्रुटि: ${e.message}",
                "कृपया फ़ाइल को दोबारा चुनें।"
              )
            }
          }
        }
      }
    }
  )

  // Image Picker for "Test Image" button
  val testImagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent(),
    onResult = { uri ->
      if (uri != null) {
        isTestingImage = true
        coroutineScope.launch(Dispatchers.IO) {
          try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
              BitmapFactory.decodeStream(it)
            }
            if (bitmap != null) {
              val reports = MultiModelClassifierManager.testImageOnModels(context, bitmap)
              bitmap.recycle()
              withContext(Dispatchers.Main) {
                isTestingImage = false
                testImageReports = reports
              }
            } else {
              withContext(Dispatchers.Main) {
                isTestingImage = false
                Toast.makeText(context, "इमेज लोड करने में विफल", Toast.LENGTH_SHORT).show()
              }
            }
          } catch (e: Exception) {
            withContext(Dispatchers.Main) {
              isTestingImage = false
              Toast.makeText(context, "परीक्षण त्रुटि: ${e.message}", Toast.LENGTH_LONG).show()
            }
          }
        }
      }
    }
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, if (modelStatuses.any { it.isLoaded }) Color(0xFF8B5CF6).copy(alpha = 0.5f) else DarkSurfaceBorder, RoundedCornerShape(18.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {

    // 1. Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (modelStatuses.any { it.isLoaded }) Color(0xFF2E1065) else ObsidianElevated),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            Icons.Default.Psychology,
            contentDescription = null,
            tint = if (modelStatuses.any { it.isLoaded }) Color(0xFFA78BFA) else WarmParchment,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Multi-Model AI Screen Vision", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "${modelStatuses.count { it.isLoaded }} / ${MultiModelClassifierManager.MAX_MODELS} मॉडल लोड | ${modelStatuses.count { it.healthStatus == ModelHealthStatus.WORKING }} सक्रिय",
            color = if (modelStatuses.any { it.healthStatus == ModelHealthStatus.WORKING }) Color(0xFF10B981) else Color(0xFFFFB4AB),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Button(
        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
        enabled = !isStrictLock && modelStatuses.size < MultiModelClassifierManager.MAX_MODELS && !isValidating,
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
      ) {
        if (isValidating) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ObsidianElevated, strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianElevated, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("मॉडल जोड़ें (.tflite)", color = ObsidianElevated, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // 2. Master Scanning Switch & Test Image Action
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurface)
        .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("AI स्क्रीन विज़न स्कैनिंग", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
          Text(
            text = if (isAiScanningEnabled || modelStatuses.any { it.isLoaded }) "सक्रिय: ऑन-डिवाइस स्क्रीनशॉट मूल्यांकन चालू है" else "बंद: कोई स्क्रीन स्कैन नहीं होगा",
            color = if (isAiScanningEnabled || modelStatuses.any { it.isLoaded }) Color(0xFF10B981) else TextMuted,
            fontSize = 10.sp
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Test Image Button
          OutlinedButton(
            onClick = { testImagePickerLauncher.launch("image/*") },
            enabled = modelStatuses.any { it.isLoaded } && !isTestingImage,
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            if (isTestingImage) {
              CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GoldBrass, strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Image, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Test Image", color = GoldBrass, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }

          Switch(
            checked = isAiScanningEnabled || modelStatuses.any { it.isLoaded },
            onCheckedChange = { checked ->
              if (!isStrictLock) {
                if (checked) {
                  // ON karna bina PIN ke ho
                  isAiScanningEnabled = true
                  AiScreenDetectionManager.setEnabled(context, true)
                  refresh()
                } else {
                  // OFF karne par VERIFY_AND_ROTATE
                  executeWithPin("AI स्क्रीन विज़न स्कैनिंग बंद करना") {
                    isAiScanningEnabled = false
                    AiScreenDetectionManager.setEnabled(context, false, pinVerified = true)
                    refresh()
                  }
                }
              }
            }
          )
        }
      }
    }

    Text(
      text = "आंतरिक filesDir में 3 मॉडल तक सुरक्षित रहते हैं। कोई नेटवर्क अनुमति नहीं। हर मॉडल के लिए इनपुट लेआउट (NCHW/NHWC) स्वतः पहचाना जाता है।",
      color = TextMuted,
      fontSize = 11.sp,
      lineHeight = 15.sp
    )

    // 3. Warning if no models imported
    if (modelStatuses.none { it.isLoaded }) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF332015))
          .border(1.dp, Color(0xFFE57A00), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text("⚠️ AI पहचान सीमित है", color = Color(0xFFFFB74D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "कोई सक्रिय TFLite मॉडल नहीं मिला। विज़न AI पहचान सीमित है। ऊपर दिए गए बटन से OpenNSFW2 या कोई अन्य .tflite मॉडल जोड़ें।",
              color = WarmOffWhite.copy(alpha = 0.9f),
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }
    }

    // 4. Models List with Live Badges and Circuit Breaker
    modelStatuses.forEach { status ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurface)
          .border(
            0.8.dp,
            when (status.healthStatus) {
              ModelHealthStatus.WORKING -> Color(0xFF10B981).copy(alpha = 0.6f)
              ModelHealthStatus.WARNING -> Color(0xFFF59E0B).copy(alpha = 0.7f)
              ModelHealthStatus.NOT_WORKING -> Color(0xFFEF4444).copy(alpha = 0.6f)
            },
            RoundedCornerShape(12.dp)
          )
          .padding(12.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          // Top title, Status Badge, and actions
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                // Live Status Badge
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                      when (status.healthStatus) {
                        ModelHealthStatus.WORKING -> Color(0xFF064E3B)
                        ModelHealthStatus.WARNING -> Color(0xFF78350F)
                        ModelHealthStatus.NOT_WORKING -> Color(0xFF7F1D1D)
                      }
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = when (status.healthStatus) {
                      ModelHealthStatus.WORKING -> "🟢 Working"
                      ModelHealthStatus.WARNING -> "🟡 Warning"
                      ModelHealthStatus.NOT_WORKING -> "🔴 Not working"
                    },
                    color = when (status.healthStatus) {
                      ModelHealthStatus.WORKING -> Color(0xFF34D399)
                      ModelHealthStatus.WARNING -> Color(0xFFFBBF24)
                      ModelHealthStatus.NOT_WORKING -> Color(0xFFF87171)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(status.config.displayName, color = WarmOffWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              if (status.healthStatus == ModelHealthStatus.NOT_WORKING && status.isCircuitBreakerTripped) {
                IconButton(
                  onClick = {
                    MultiModelClassifierManager.resetCircuitBreaker(context, status.config.id)
                    refresh()
                    Toast.makeText(context, "${status.config.displayName} पुनः सक्रिय किया गया", Toast.LENGTH_SHORT).show()
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = GoldBrass, modifier = Modifier.size(16.dp))
                }
              }
              IconButton(
                onClick = { editingConfig = status.config },
                enabled = !isStrictLock,
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GoldBrass, modifier = Modifier.size(16.dp))
              }
              IconButton(
                onClick = {
                  executeWithPin("AI मॉडल '${status.config.displayName}' हटाना") {
                    MultiModelClassifierManager.deleteModel(context, status.config.id, pinVerified = true)
                    refresh()
                    Toast.makeText(context, "${status.config.displayName} हटाया गया", Toast.LENGTH_SHORT).show()
                  }
                },
                enabled = !isStrictLock,
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
              }
            }
          }

          // Live Metrics Line (Score, Latency, Last Run)
          if (status.healthStatus == ModelHealthStatus.WORKING) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "आख़िरी स्कोर: ${(status.lastScore * 100).toInt()}%",
                color = GoldBrass,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "रन: ${if (status.lastRunTimeAgoSec >= 0) "${status.lastRunTimeAgoSec}s पहले" else "प्रतीक्षारत"} | Avg: ${status.avgLatencyMs} ms",
                color = WarmParchment,
                fontSize = 10.sp
              )
            }
          }

          // Status & What To Do Guidance for WARNING & NOT_WORKING
          if (status.healthStatus != ModelHealthStatus.WORKING) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (status.healthStatus == ModelHealthStatus.WARNING) Color(0xFF332015) else Color(0xFF3A1717))
                .border(
                  0.8.dp,
                  if (status.healthStatus == ModelHealthStatus.WARNING) Color(0xFFE57A00) else Color(0xFFEF4444),
                  RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                  text = "वजह: ${status.statusReasonHindi}",
                  color = if (status.healthStatus == ModelHealthStatus.WARNING) Color(0xFFFFB74D) else Color(0xFFFFB4AB),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "क्या करना है: ${status.whatToDoHindi}",
                  color = WarmOffWhite.copy(alpha = 0.9f),
                  fontSize = 10.sp
                )
                if (status.isCircuitBreakerTripped) {
                  Button(
                    onClick = {
                      MultiModelClassifierManager.resetCircuitBreaker(context, status.config.id)
                      refresh()
                      Toast.makeText(context, "${status.config.displayName} पुनः सक्रिय", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.padding(top = 4.dp)
                  ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = ObsidianElevated, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("दोबारा कोशिश करें (Retry)", color = ObsidianElevated, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }

          // Tensor Shapes, Layout, DType & Inferences Stats
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Input: ${status.inputShapeStr} (${status.inputDTypeStr}) [${status.inputLayout}]", color = TextMuted, fontSize = 10.sp)
              Text("Output: ${status.outputShapeStr} (${status.outputDTypeStr})", color = TextMuted, fontSize = 10.sp)
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("NSFW Index: [${status.config.nsfwOutputIndex}]", color = CyanNeon, fontSize = 10.sp)
              Text("कुल जाँच: ${status.totalInferences} | विफलता: ${status.totalFailures}", color = TextMuted, fontSize = 10.sp)
            }
          }

          // Normalization Choice Selector (including OpenNSFW2 BGR)
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Normalization चुनाव:", color = WarmOffWhite, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = "चुना गया: ${status.config.normalization.label}",
                color = GoldBrass,
                fontSize = 9.sp
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              NormalizationType.entries.forEach { norm ->
                val isSelected = status.config.normalization == norm
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) GoldBrass else ObsidianElevated)
                    .border(
                      0.8.dp,
                      if (isSelected) GoldBrass else DarkSurfaceBorder,
                      RoundedCornerShape(6.dp)
                    )
                    .clickable(enabled = !isStrictLock) {
                      val updated = status.config.copy(normalization = norm)
                      MultiModelClassifierManager.updateModelConfig(context, updated)
                      refresh()
                    }
                    .padding(vertical = 5.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = when (norm) {
                      NormalizationType.ZERO_TO_ONE -> "0..1"
                      NormalizationType.MINUS_ONE_TO_ONE -> "-1..1"
                      NormalizationType.IMAGENET -> "ImageNet"
                      NormalizationType.UINT8_NONE -> "uint8"
                      NormalizationType.BGR_MEAN_SUBTRACT -> "OpenNSFW2"
                    },
                    color = if (isSelected) ObsidianElevated else WarmOffWhite,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }
          }
        }
      }
    }

    // 5. Ensemble Lock Rules & Threshold Sliders
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurface)
        .border(0.8.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
        .padding(12.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Tune, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("मल्टी-मॉडल लॉक नियम (Ensemble Rules)", color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Text(
          text = "नियम: कोई 1 मॉडल ≥ ${(singleThreshold * 100).toInt()}% हो, या कम से कम 2 मॉडल ≥ ${(duoThreshold * 100).toInt()}% हों, और लगातार 2 फ़्रेम पुष्टि पर 15 मिनट का Strict Lock लागू होगा।",
          color = WarmOffWhite.copy(alpha = 0.85f),
          fontSize = 11.sp,
          lineHeight = 15.sp
        )

        // Single Model Threshold Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("एकल मॉडल सीमा (Single Model 92%+):", color = TextMuted, fontSize = 11.sp)
            Text("${(singleThreshold * 100).toInt()}%", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Slider(
            value = singleThreshold,
            onValueChange = {
              if (!isStrictLock) {
                singleThreshold = it
                MultiModelClassifierManager.setSingleModelThreshold(context, it)
              }
            },
            valueRange = 0.70f..0.99f,
            colors = SliderDefaults.colors(thumbColor = GoldBrass, activeTrackColor = GoldBrass)
          )
        }

        // Duo Models Threshold Slider
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("दोहरा मॉडल सीमा (2 Models 75%+):", color = TextMuted, fontSize = 11.sp)
            Text("${(duoThreshold * 100).toInt()}%", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
          Slider(
            value = duoThreshold,
            onValueChange = {
              if (!isStrictLock) {
                duoThreshold = it
                MultiModelClassifierManager.setDuoModelThreshold(context, it)
              }
            },
            valueRange = 0.50f..0.90f,
            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
          )
        }
      }
    }
  }

  // A) Error Dialog on Import Validation Failure
  if (validationErrorDialog != null) {
    val (reason, nextStep) = validationErrorDialog!!
    AlertDialog(
      onDismissRequest = { validationErrorDialog = null },
      containerColor = ObsidianCharcoal,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("मॉडल सत्यापन विफल", color = Color(0xFFEF4444), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF3A1717))
              .border(0.8.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
              .padding(10.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("वजह:", color = Color(0xFFFFB4AB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(reason, color = WarmOffWhite, fontSize = 12.sp, lineHeight = 16.sp)
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(DarkSurface)
              .padding(10.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("अगला कदम:", color = GoldBrass, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text(nextStep, color = WarmOffWhite.copy(alpha = 0.9f), fontSize = 12.sp)
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { validationErrorDialog = null },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass)
        ) {
          Text("समझ गया", color = ObsidianElevated, fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  // B) Validated Import Confirmation Dialog
  if (showConfirmImportDialog && pendingTempFile != null && validationReport != null) {
    val report = validationReport!!
    val tempFile = pendingTempFile!!

    var nameInput by remember { mutableStateOf("Classifier Model ${modelStatuses.size + 1}") }
    var nsfwIndexInput by remember { mutableIntStateOf(report.recommendedNsfwIndex) }
    var normInput by remember { mutableStateOf(report.recommendedNorm) }

    AlertDialog(
      onDismissRequest = {
        tempFile.delete()
        pendingTempFile = null
        validationReport = null
        showConfirmImportDialog = false
      },
      containerColor = ObsidianCharcoal,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("मॉडल सफलतापूर्वक सत्यापित!", color = WarmOffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Verification badges
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF064E3B))
              .border(0.8.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text("✅ TFL3 हस्ताक्षर व साइज़ मान्य", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("Input: ${report.inputHeight}x${report.inputWidth}x${report.inputChannels} (${report.inputDataType}) [${report.inputLayout}]", color = WarmOffWhite, fontSize = 10.sp)
              Text("Output: ${report.outputShape.contentToString()} (${report.outputClasses} classes) | ड्राई-रन: ${report.dryRunLatencyMs}ms", color = WarmOffWhite, fontSize = 10.sp)
            }
          }

          if (report.hasSanityWarning) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF332015))
                .border(0.8.dp, Color(0xFFE57A00), RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              Text(report.sanityWarningMessage ?: "", color = Color(0xFFFFB74D), fontSize = 10.sp)
            }
          }

          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("मॉडल नाम (जैसे OpenNSFW2)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
          )

          // Normalization Selector in Import Dialog
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Normalization (डिफ़ॉल्ट अनुशंसित):", color = WarmOffWhite, fontSize = 11.sp)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              NormalizationType.entries.forEach { norm ->
                val isSelected = normInput == norm
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) GoldBrass else DarkSurface)
                    .clickable { normInput = norm }
                    .padding(vertical = 5.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = when (norm) {
                      NormalizationType.ZERO_TO_ONE -> "0..1"
                      NormalizationType.MINUS_ONE_TO_ONE -> "-1..1"
                      NormalizationType.IMAGENET -> "ImageNet"
                      NormalizationType.UINT8_NONE -> "uint8"
                      NormalizationType.BGR_MEAN_SUBTRACT -> "OpenNSFW2"
                    },
                    color = if (isSelected) ObsidianElevated else WarmOffWhite,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }
          }

          // Output Index Selector
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("NSFW स्कोर आउटपुट इंडेक्स:", color = WarmOffWhite, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              (0 until minOf(report.outputClasses, 4)).forEach { idx ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (nsfwIndexInput == idx) GoldBrass else DarkSurface)
                    .clickable { nsfwIndexInput = idx }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text("Index $idx", color = if (nsfwIndexInput == idx) ObsidianElevated else WarmOffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val res = MultiModelClassifierManager.importValidatedModel(
              context = context,
              tempFile = tempFile,
              suggestedName = nameInput,
              validationResult = report,
              nsfwIndex = nsfwIndexInput,
              customNormalization = normInput
            )
            if (res.isSuccess) {
              Toast.makeText(context, "मॉडल सफलतापूर्वक सुरक्षित और लोड हुआ!", Toast.LENGTH_SHORT).show()
              refresh()
            } else {
              Toast.makeText(context, "त्रुटि: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
            pendingTempFile = null
            validationReport = null
            showConfirmImportDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass)
        ) {
          Text("सहेजें व चालू करें", color = ObsidianElevated, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = {
            tempFile.delete()
            pendingTempFile = null
            validationReport = null
            showConfirmImportDialog = false
          }
        ) {
          Text("रद्द करें", color = WarmParchment)
        }
      }
    )
  }

  // C) Edit Model Config Dialog
  if (editingConfig != null) {
    val cfg = editingConfig!!
    var editName by remember { mutableStateOf(cfg.displayName) }
    var editIndex by remember { mutableIntStateOf(cfg.nsfwOutputIndex) }
    var editNorm by remember { mutableStateOf(cfg.normalization) }
    var editEnabled by remember { mutableStateOf(cfg.isEnabled) }

    AlertDialog(
      onDismissRequest = { editingConfig = null },
      containerColor = ObsidianCharcoal,
      title = { Text("मॉडल सेटिंग्स बदलें", color = WarmOffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = editName,
            onValueChange = { editName = it },
            label = { Text("मॉडल नाम") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface)
          )

          // Normalization in Edit Dialog
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Normalization:", color = WarmOffWhite, fontSize = 12.sp)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              NormalizationType.entries.forEach { norm ->
                val isSelected = editNorm == norm
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) GoldBrass else DarkSurface)
                    .clickable { editNorm = norm }
                    .padding(vertical = 5.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = when (norm) {
                      NormalizationType.ZERO_TO_ONE -> "0..1"
                      NormalizationType.MINUS_ONE_TO_ONE -> "-1..1"
                      NormalizationType.IMAGENET -> "ImageNet"
                      NormalizationType.UINT8_NONE -> "uint8"
                      NormalizationType.BGR_MEAN_SUBTRACT -> "OpenNSFW2"
                    },
                    color = if (isSelected) ObsidianElevated else WarmOffWhite,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                  )
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("NSFW आउटपुट इंडेक्स:", color = WarmOffWhite, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf(0 to "Index 0", 1 to "Index 1").forEach { (idx, label) ->
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (editIndex == idx) GoldBrass else DarkSurface)
                    .clickable { editIndex = idx }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(label, color = if (editIndex == idx) ObsidianElevated else WarmOffWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("सक्रिय रखें (Enabled):", color = WarmOffWhite, fontSize = 12.sp)
            Switch(
              checked = editEnabled,
              onCheckedChange = { editEnabled = it }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val updated = cfg.copy(
              displayName = editName,
              nsfwOutputIndex = editIndex,
              normalization = editNorm,
              isEnabled = editEnabled
            )
            if (!editEnabled && cfg.isEnabled) {
              executeWithPin("AI मॉडल '${cfg.displayName}' निष्क्रिय करना") {
                MultiModelClassifierManager.updateModelConfig(context, updated, pinVerified = true)
                editingConfig = null
                refresh()
                Toast.makeText(context, "सेटिंग्स अपडेट हुईं", Toast.LENGTH_SHORT).show()
              }
            } else {
              MultiModelClassifierManager.updateModelConfig(context, updated)
              editingConfig = null
              refresh()
              Toast.makeText(context, "सेटिंग्स अपडेट हुईं", Toast.LENGTH_SHORT).show()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass)
        ) {
          Text("सहेजें", color = ObsidianElevated, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { editingConfig = null }) {
          Text("रद्द करें", color = WarmParchment)
        }
      }
    )
  }

  // D) Test Image Evaluation Results Dialog
  if (testImageReports != null) {
    val reports = testImageReports!!
    AlertDialog(
      onDismissRequest = { testImageReports = null },
      containerColor = ObsidianCharcoal,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Image, contentDescription = null, tint = GoldBrass, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("गैलरी इमेज परीक्षण परिणाम", color = WarmOffWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "चयनित इमेज पर सभी सक्रिय मॉडल्स का निष्पादन व स्कोर:",
            color = TextMuted,
            fontSize = 11.sp
          )

          reports.forEach { rep ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(
                  0.8.dp,
                  if (rep.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444),
                  RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(rep.modelName, color = WarmOffWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  Text(
                    text = if (rep.isSuccess) "${rep.scorePercent}% NSFW" else "विफल",
                    color = if (rep.isSuccess) (if (rep.score >= 0.75f) Color(0xFFEF4444) else GoldBrass) else Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }

                if (rep.isSuccess) {
                  LinearProgressIndicator(
                    progress = { (rep.score).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = if (rep.score >= 0.75f) Color(0xFFEF4444) else GoldBrass,
                    trackColor = ObsidianElevated
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text("Norm: ${rep.normalizationLabel} [${rep.layout}]", color = TextMuted, fontSize = 9.sp)
                    Text("समय: ${rep.latencyMs} ms", color = WarmParchment, fontSize = 9.sp)
                  }
                } else {
                  Text("त्रुटि: ${rep.errorMessage}", color = Color(0xFFFFB4AB), fontSize = 10.sp)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { testImageReports = null },
          colors = ButtonDefaults.buttonColors(containerColor = GoldBrass)
        ) {
          Text("बंद करें", color = ObsidianElevated, fontWeight = FontWeight.Bold)
        }
      }
    )
  }

  if (showPinDialog) {
    AccountabilityPinDialog(
      onDismiss = {
        showPinDialog = false
        pendingPinAction = null
      },
      onActionApproved = {
        pendingPinAction?.invoke()
        showPinDialog = false
        pendingPinAction = null
      },
      mode = AccountabilityPinDialogMode.VERIFY_AND_ROTATE,
      actionTitle = pendingActionTitle
    )
  }
}
