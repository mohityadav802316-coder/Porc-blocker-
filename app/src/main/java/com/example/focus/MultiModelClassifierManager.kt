package com.example.focus

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Normalization types supported for TFLite vision classifiers.
 */
enum class NormalizationType(val id: String, val label: String) {
  ZERO_TO_ONE("zero_to_one", "0..1"),
  MINUS_ONE_TO_ONE("minus_one_to_one", "-1..1"),
  IMAGENET("imagenet", "ImageNet (mean/std)"),
  UINT8_NONE("uint8_none", "uint8 (कोई नहीं)"),
  BGR_MEAN_SUBTRACT("bgr_mean_subtract", "OpenNSFW2 (BGR)");

  companion object {
    fun fromId(id: String?): NormalizationType {
      return entries.find { it.id == id } ?: ZERO_TO_ONE
    }

    fun defaultForDataType(dataType: DataType): NormalizationType {
      return if (dataType == DataType.UINT8) UINT8_NONE else ZERO_TO_ONE
    }
  }
}

/**
 * Live health status badge for an imported model.
 */
enum class ModelHealthStatus {
  WORKING,
  WARNING,
  NOT_WORKING
}

/**
 * Metadata and dynamic execution configuration for an imported TFLite Classifier model.
 */
data class TFLiteModelConfig(
  val id: String,
  val displayName: String,
  val fileName: String,
  val nsfwOutputIndex: Int = 1, // Default index 1 for 2-element softmax [Safe, NSFW]
  val isEnabled: Boolean = true,
  val normalization: NormalizationType = NormalizationType.ZERO_TO_ONE,
  val sha256Hash: String = "",
  val inputLayout: String = "NHWC", // "NHWC" or "NCHW"
  val hasSanityWarning: Boolean = false,
  val sanityWarningMessage: String? = null
)

/**
 * Live Runtime statistics for an imported model.
 */
data class ModelRuntimeStatus(
  val config: TFLiteModelConfig,
  val isLoaded: Boolean,
  val healthStatus: ModelHealthStatus,
  val inputShapeStr: String,
  val inputDTypeStr: String,
  val inputLayout: String,
  val outputShapeStr: String,
  val outputDTypeStr: String,
  val avgLatencyMs: Long,
  val lastLatencyMs: Long,
  val lastScore: Float,
  val lastRunTimeAgoSec: Long,
  val totalInferences: Long,
  val totalFailures: Long,
  val isCircuitBreakerTripped: Boolean,
  val statusReasonHindi: String,
  val whatToDoHindi: String,
  val errorMessage: String? = null,
  val isSlowLatencyWarning: Boolean = false
)

/**
 * Validation report generated before finalizing model import.
 */
data class ModelValidationResult(
  val isSuccess: Boolean,
  val sha256Hash: String = "",
  val inputShape: IntArray = intArrayOf(),
  val inputDataType: DataType = DataType.FLOAT32,
  val inputLayout: String = "NHWC",
  val inputHeight: Int = 224,
  val inputWidth: Int = 224,
  val inputChannels: Int = 3,
  val outputShape: IntArray = intArrayOf(),
  val outputDataType: DataType = DataType.FLOAT32,
  val outputClasses: Int = 2,
  val recommendedNorm: NormalizationType = NormalizationType.ZERO_TO_ONE,
  val recommendedNsfwIndex: Int = 1,
  val dryRunLatencyMs: Long = 0L,
  val dryRunScore: Float = 0f,
  val hasSanityWarning: Boolean = false,
  val sanityWarningMessage: String? = null,
  val failureReasonHindi: String? = null,
  val nextStepHindi: String? = null
)

/**
 * Result returned by an individual model evaluation.
 */
data class ModelInferenceResult(
  val modelId: String,
  val modelName: String,
  val score: Float,
  val latencyMs: Long,
  val isSuccess: Boolean,
  val errorMessage: String? = null
)

/**
 * Report generated when user tests an image from gallery against all models.
 */
data class ModelTestReport(
  val modelId: String,
  val modelName: String,
  val score: Float,
  val scorePercent: Int,
  val latencyMs: Long,
  val normalizationLabel: String,
  val layout: String,
  val isSuccess: Boolean,
  val errorMessage: String? = null
)

/**
 * Combined ensemble consensus result across all active models.
 */
data class EnsembleDecision(
  val isNsfwTrigger: Boolean,
  val individualResults: List<ModelInferenceResult>,
  val reason: String,
  val consecutiveFrames: Int
)

/**
 * Multi-Model Manager for On-Device NSFW Classification in Life Tracker.
 *
 * Implements:
 * 1. Strict Import Validation (signature TFL3, size limits, SHA-256 duplicate check,
 *    interpreter compatibility & memory check, tensor layout NCHW/NHWC, dry-run NaN check,
 *    synthetic images sanity reaction check).
 * 2. Live Status Badges (🟢 Working, 🟡 Warning, 🔴 Not working) with clean Hindi explanations.
 * 3. Circuit Breaker: Auto-disables model after 5 consecutive failures, records to Protection Health Log,
 *    and allows manual reset.
 * 4. Layout-aware Preprocessing (NCHW & NHWC) and OpenNSFW2 BGR Mean-Subtract normalization.
 * 5. Standalone Test Image evaluation from gallery.
 */
object MultiModelClassifierManager {

  private const val TAG = "MultiModelClassifier"
  private const val PREFS_NAME = "multi_model_classifier_prefs"
  private const val KEY_CONFIGS_JSON = "models_configs_json"
  private const val KEY_SINGLE_MODEL_THRESHOLD = "single_model_threshold"
  private const val KEY_DUO_MODEL_THRESHOLD = "duo_model_threshold"

  const val MAX_MODELS = 3
  const val DEFAULT_SINGLE_THRESHOLD = 0.92f // 92%
  const val DEFAULT_DUO_THRESHOLD = 0.75f    // 75%
  const val DEFAULT_INSPECTION_INTERVAL_MS = 1500L
  const val MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024L // 100 MB

  // In-memory model runtime wrapper
  private class LoadedModelInstance(
    val config: TFLiteModelConfig,
    val interpreter: Interpreter,
    val inputHeight: Int,
    val inputWidth: Int,
    val inputChannels: Int,
    val inputDataType: DataType,
    val inputLayout: String,
    val outputShape: IntArray,
    val outputDataType: DataType,
    val executor: java.util.concurrent.ExecutorService
  ) {
    val isBusy = AtomicBoolean(false)
    var skipNextCheck: Boolean = false
    var totalLatency: Long = 0L
    var inferenceCount: Long = 0L // successful runs
    var totalFailures: Long = 0L
    var consecutiveFailures: Int = 0
    var isCircuitBreakerTripped: Boolean = false
    var circuitBreakerReason: String? = null
    var lastRunTimestamp: Long = 0L
    var lastLatency: Long = 0L
    var lastScore: Float = 0f
    var lastError: String? = null
    var hasSanityWarning: Boolean = config.hasSanityWarning
    var sanityWarningMessage: String? = config.sanityWarningMessage

    val avgLatency: Long
      get() = if (inferenceCount > 0L) totalLatency / inferenceCount else 0L

    val isSlowLatencyWarning: Boolean
      get() = lastLatency > DEFAULT_INSPECTION_INTERVAL_MS

    fun resetCircuitBreaker() {
      isCircuitBreakerTripped = false
      consecutiveFailures = 0
      circuitBreakerReason = null
      lastError = null
    }

    fun close() {
      try {
        interpreter.close()
      } catch (_: Exception) {}
      try {
        executor.shutdownNow()
      } catch (_: Exception) {}
    }
  }

  private val loadedInstances = mutableMapOf<String, LoadedModelInstance>()
  private var consecutivePositiveFrames = 0
  private var lastEvaluationTime = 0L

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun getSingleModelThreshold(context: Context): Float {
    return getPrefs(context).getFloat(KEY_SINGLE_MODEL_THRESHOLD, DEFAULT_SINGLE_THRESHOLD)
  }

  fun setSingleModelThreshold(context: Context, threshold: Float) {
    getPrefs(context).edit().putFloat(KEY_SINGLE_MODEL_THRESHOLD, threshold.coerceIn(0.70f, 0.99f)).apply()
  }

  fun getDuoModelThreshold(context: Context): Float {
    return getPrefs(context).getFloat(KEY_DUO_MODEL_THRESHOLD, DEFAULT_DUO_THRESHOLD)
  }

  fun setDuoModelThreshold(context: Context, threshold: Float) {
    getPrefs(context).edit().putFloat(KEY_DUO_MODEL_THRESHOLD, threshold.coerceIn(0.50f, 0.95f)).apply()
  }

  fun getModelConfigs(context: Context): List<TFLiteModelConfig> {
    val jsonStr = getPrefs(context).getString(KEY_CONFIGS_JSON, null) ?: return emptyList()
    val list = mutableListOf<TFLiteModelConfig>()
    try {
      val arr = JSONArray(jsonStr)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        list.add(
          TFLiteModelConfig(
            id = obj.getString("id"),
            displayName = obj.getString("displayName"),
            fileName = obj.getString("fileName"),
            nsfwOutputIndex = obj.optInt("nsfwOutputIndex", 1),
            isEnabled = obj.optBoolean("isEnabled", true),
            normalization = NormalizationType.fromId(obj.optString("normalization", NormalizationType.ZERO_TO_ONE.id)),
            sha256Hash = obj.optString("sha256Hash", ""),
            inputLayout = obj.optString("inputLayout", "NHWC"),
            hasSanityWarning = obj.optBoolean("hasSanityWarning", false),
            sanityWarningMessage = obj.optString("sanityWarningMessage", "").ifEmpty { null }
          )
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error reading model configs", e)
    }
    return list
  }

  private fun saveModelConfigs(context: Context, configs: List<TFLiteModelConfig>) {
    try {
      val arr = JSONArray()
      for (cfg in configs) {
        val obj = JSONObject().apply {
          put("id", cfg.id)
          put("displayName", cfg.displayName)
          put("fileName", cfg.fileName)
          put("nsfwOutputIndex", cfg.nsfwOutputIndex)
          put("isEnabled", cfg.isEnabled)
          put("normalization", cfg.normalization.id)
          put("sha256Hash", cfg.sha256Hash)
          put("inputLayout", cfg.inputLayout)
          put("hasSanityWarning", cfg.hasSanityWarning)
          put("sanityWarningMessage", cfg.sanityWarningMessage ?: "")
        }
        arr.put(obj)
      }
      getPrefs(context).edit().putString(KEY_CONFIGS_JSON, arr.toString()).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Error saving model configs", e)
    }
  }

  /**
   * Computes SHA-256 hash string of a given file.
   */
  fun computeFileSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
      val buf = ByteArray(8192)
      var read: Int
      while (fis.read(buf).also { read = it } > 0) {
        digest.update(buf, 0, read)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  /**
   * Pre-inspects a Uri to determine detected DataType and default normalization without saving yet.
   */
  fun inspectUriDataType(context: Context, uri: Uri): Pair<DataType, NormalizationType>? {
    return try {
      val tempFile = File.createTempFile("inspect_", ".tflite", context.cacheDir)
      context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(tempFile).use { output ->
          input.copyTo(output)
        }
      }
      val fis = FileInputStream(tempFile)
      val channel = fis.channel
      val byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length())
      fis.close()
      val interp = Interpreter(byteBuffer, Interpreter.Options().apply { setNumThreads(1) })
      val inputTensor = interp.getInputTensor(0)
      val dType = inputTensor.dataType()
      interp.close()
      tempFile.delete()
      val defNorm = NormalizationType.defaultForDataType(dType)
      Pair(dType, defNorm)
    } catch (e: Exception) {
      Log.w(TAG, "inspectUriDataType error: ${e.message}")
      null
    }
  }

  /**
   * Strictly validates a candidate .tflite model file on a temp file BEFORE finalizing in filesDir.
   *
   * Checks:
   * 1. File format: Size > 0 and < 100MB; first 8 bytes contains "TFL3" signature.
   * 2. Duplicate: SHA-256 hash does not match existing models.
   * 3. Interpreter load: Checks unsupported operators (with exact op name) and out of memory.
   * 4. Tensor check: Validates 3D/4D image input shape, dtype, detects layout (NCHW vs NHWC), output classes.
   * 5. Dry-run: Evaluates dummy input for NaN, Infinity, size mismatch or timeout.
   * 6. Sanity check: Evaluates 3 synthetic images (solid black, solid white, random noise);
   *    warns if output is identical across all three.
   */
  fun validateModelFile(context: Context, tempFile: File): ModelValidationResult {
    // 1. File format check
    val length = tempFile.length()
    if (length <= 0L || length > MAX_FILE_SIZE_BYTES) {
      val sizeMb = length / (1024 * 1024)
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "फ़ाइल का आकार अमान्य है (${if (length <= 0) "0 KB" else "$sizeMb MB"})। आकार 0 से 100 MB के बीच होना चाहिए।",
        nextStepHindi = "कृपया 100 MB से छोटी वैध .tflite फ़ाइल चुनें।"
      )
    }

    try {
      val header = ByteArray(8)
      FileInputStream(tempFile).use { it.read(header) }
      val str4to7 = if (header.size >= 8) String(header, 4, 4, Charsets.US_ASCII) else ""
      val str0to3 = if (header.size >= 4) String(header, 0, 4, Charsets.US_ASCII) else ""
      if (str4to7 != "TFL3" && str0to3 != "TFL3") {
        return ModelValidationResult(
          isSuccess = false,
          failureReasonHindi = "यह .tflite फ़ाइल वैध नहीं है या खराब है (TFL3 हस्ताक्षर नहीं मिला)।",
          nextStepHindi = "कृपया कोई अन्य वैध TensorFlow Lite (.tflite) मॉडल चुनें।"
        )
      }
    } catch (e: Exception) {
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "फ़ाइल पढ़ने में त्रुटि: ${e.message}",
        nextStepHindi = "कृपया फ़ाइल दोबारा डाउनलोड या चुनें।"
      )
    }

    // 2. Duplicate check via SHA-256
    val sha256 = try {
      computeFileSha256(tempFile)
    } catch (e: Exception) {
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "फ़ाइल हैश गणना में त्रुटि: ${e.message}",
        nextStepHindi = "कृपया पुनः प्रयास करें।"
      )
    }

    val currentConfigs = getModelConfigs(context)
    val isDuplicate = currentConfigs.any { cfg ->
      if (cfg.sha256Hash.isNotEmpty() && cfg.sha256Hash.equals(sha256, ignoreCase = true)) {
        true
      } else {
        val existingFile = File(context.filesDir, cfg.fileName)
        if (existingFile.exists()) {
          try {
            computeFileSha256(existingFile).equals(sha256, ignoreCase = true)
          } catch (_: Exception) {
            false
          }
        } else false
      }
    }

    if (isDuplicate) {
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "यह मॉडल पहले से जोड़ा गया है (डुप्लिकेट फ़ाइल पहचान)।",
        nextStepHindi = "यह मॉडल सूची में पहले से मौजूद है। कोई अन्य नया मॉडल चुनें।"
      )
    }

    // 3. Interpreter load attempt with exact error diagnosis
    val interp: Interpreter
    try {
      val fis = FileInputStream(tempFile)
      val channel = fis.channel
      val byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length())
      fis.close()
      interp = Interpreter(byteBuffer, Interpreter.Options().apply { setNumThreads(1) })
    } catch (e: OutOfMemoryError) {
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "मॉडल बहुत बड़ा है, मेमोरी कम पड़ी।",
        nextStepHindi = "कृपया छोटा या क्वांटाइज़्ड (INT8/FLOAT16) मॉडल चुनें।"
      )
    } catch (e: Exception) {
      val msg = e.message ?: ""
      val isMemory = msg.contains("memory", ignoreCase = true) || msg.contains("allocate", ignoreCase = true)
      val isOp = msg.contains("op", ignoreCase = true) || msg.contains("operator", ignoreCase = true) ||
        msg.contains("Didn't find op", ignoreCase = true)

      val reason = when {
        isMemory -> "मॉडल बहुत बड़ा है, मेमोरी कम पड़ी।"
        isOp -> {
          val opMatch = Regex("(?:op|operator|Didn't find op) '([^']+)'", RegexOption.IGNORE_CASE).find(msg)
          val opName = opMatch?.groupValues?.getOrNull(1) ?: msg.take(40)
          "इस मॉडल में ऐसे ऑपरेटर्स हैं जो यह फ़ोन सपोर्ट नहीं करता ($opName)।"
        }
        else -> "यह .tflite फ़ाइल वैध नहीं है या खराब है (${e.localizedMessage ?: "अज्ञात त्रुटि"})।"
      }
      val nextStep = if (isOp) "मानक TFLite बिल्ट-इन ऑप्स वाला मॉडल चुनें।" else "कृपया कोई दूसरी .tflite फ़ाइल चुनें।"
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = reason,
        nextStepHindi = nextStep
      )
    }

    // 4. Tensor shape & Layout check
    val inShape: IntArray
    val inDataType: DataType
    val inHeight: Int
    val inWidth: Int
    val inChannels: Int
    val layout: String

    val outShape: IntArray
    val outDataType: DataType
    val outClasses: Int
    val recNsfwIndex: Int
    val recNorm: NormalizationType

    try {
      val inputTensor = interp.getInputTensor(0)
      inShape = inputTensor.shape()
      inDataType = inputTensor.dataType()

      if (inShape.size == 4) {
        if (inShape[1] == 1 || inShape[1] == 3) {
          layout = "NCHW"
          inChannels = inShape[1]
          inHeight = inShape[2]
          inWidth = inShape[3]
        } else if (inShape[3] == 1 || inShape[3] == 3) {
          layout = "NHWC"
          inHeight = inShape[1]
          inWidth = inShape[2]
          inChannels = inShape[3]
        } else {
          interp.close()
          return ModelValidationResult(
            isSuccess = false,
            failureReasonHindi = "इनपुट इमेज-जैसा नहीं है (shape: ${inShape.contentToString()})।",
            nextStepHindi = "इमेज क्लासिफायर मॉडल चुनें जिसका इनपुट [1, H, W, 3] या [1, 3, H, W] हो।"
          )
        }
      } else if (inShape.size == 3) {
        layout = "NHWC"
        inHeight = inShape[0]
        inWidth = inShape[1]
        inChannels = inShape[2]
      } else {
        interp.close()
        return ModelValidationResult(
          isSuccess = false,
          failureReasonHindi = "इनपुट इमेज-जैसा नहीं है (shape: ${inShape.contentToString()})।",
          nextStepHindi = "इमेज क्लासिफायर मॉडल चुनें जिसका इनपुट 3 या 4 डायमेंशन का हो।"
        )
      }

      val outputTensor = interp.getOutputTensor(0)
      outShape = outputTensor.shape()
      outDataType = outputTensor.dataType()
      outClasses = if (outShape.isNotEmpty()) outShape.last() else 2
      recNsfwIndex = if (outClasses >= 2) 1 else 0
      recNorm = NormalizationType.defaultForDataType(inDataType)
    } catch (e: Exception) {
      interp.close()
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "टेंसर जानकारी पढ़ने में त्रुटि: ${e.message}",
        nextStepHindi = "कृपया कोई अन्य .tflite मॉडल चुनें।"
      )
    }

    // 5. Dry-run on dummy image
    val dryRunStart = System.currentTimeMillis()
    var dryRunScore = 0f
    try {
      val dummyBitmap = Bitmap.createBitmap(inWidth.coerceAtLeast(1), inHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
      val dummyBuffer = prepareInputBuffer(
        bitmap = dummyBitmap,
        inputWidth = inWidth,
        inputHeight = inHeight,
        inputChannels = inChannels,
        inputDataType = inDataType,
        layout = layout,
        norm = recNorm
      )
      dummyBitmap.recycle()

      val dryRunOut = Array(1) { FloatArray(outClasses) }
      interp.run(dummyBuffer, dryRunOut)
      dryRunScore = dryRunOut[0][recNsfwIndex.coerceIn(0, outClasses - 1)]

      if (dryRunScore.isNaN() || dryRunScore.isInfinite()) {
        interp.close()
        return ModelValidationResult(
          isSuccess = false,
          failureReasonHindi = "ड्राई-रन विफल: मॉडल आउटपुट में NaN या Infinity आया।",
          nextStepHindi = "मॉडल आउटपुट अमान्य है, कृपया कोई अन्य मॉडल चुनें।"
        )
      }
    } catch (e: Exception) {
      interp.close()
      return ModelValidationResult(
        isSuccess = false,
        failureReasonHindi = "ड्राई-रन अनुमान विफल: ${e.message}",
        nextStepHindi = "कृपया कोई अन्य मॉडल चुनें।"
      )
    }

    val dryRunLatency = System.currentTimeMillis() - dryRunStart

    // 6. Sanity check: 3 synthetic images (Solid black, Solid white, Random noise)
    var hasSanityWarning = false
    var sanityWarningMsg: String? = null
    try {
      // Synthetic 1: Black
      val blackBitmap = Bitmap.createBitmap(inWidth, inHeight, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
      val blackBuf = prepareInputBuffer(blackBitmap, inWidth, inHeight, inChannels, inDataType, layout, recNorm)
      blackBitmap.recycle()
      val blackOut = Array(1) { FloatArray(outClasses) }
      interp.run(blackBuf, blackOut)
      val scoreBlack = blackOut[0][recNsfwIndex.coerceIn(0, outClasses - 1)]

      // Synthetic 2: White
      val whiteBitmap = Bitmap.createBitmap(inWidth, inHeight, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
      val whiteBuf = prepareInputBuffer(whiteBitmap, inWidth, inHeight, inChannels, inDataType, layout, recNorm)
      whiteBitmap.recycle()
      val whiteOut = Array(1) { FloatArray(outClasses) }
      interp.run(whiteBuf, whiteOut)
      val scoreWhite = whiteOut[0][recNsfwIndex.coerceIn(0, outClasses - 1)]

      // Synthetic 3: Random Noise
      val noiseBitmap = Bitmap.createBitmap(inWidth, inHeight, Bitmap.Config.ARGB_8888)
      val rng = Random(42)
      val noisePixels = IntArray(inWidth * inHeight) {
        Color.rgb(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256))
      }
      noiseBitmap.setPixels(noisePixels, 0, inWidth, 0, 0, inWidth, inHeight)
      val noiseBuf = prepareInputBuffer(noiseBitmap, inWidth, inHeight, inChannels, inDataType, layout, recNorm)
      noiseBitmap.recycle()
      val noiseOut = Array(1) { FloatArray(outClasses) }
      interp.run(noiseBuf, noiseOut)
      val scoreNoise = noiseOut[0][recNsfwIndex.coerceIn(0, outClasses - 1)]

      if (Math.abs(scoreBlack - scoreWhite) < 1e-4f && Math.abs(scoreWhite - scoreNoise) < 1e-4f) {
        hasSanityWarning = true
        sanityWarningMsg = "यह मॉडल इनपुट पर प्रतिक्रिया (react) नहीं कर रहा (काली, सफेद और रैंडम इमेज पर समान स्कोर: ${(scoreBlack * 100).toInt()}%), शायद गलत या खराब फ़ाइल है। (नोट: यह जाँच पक्का नहीं बताती कि यह NSFW मॉडल ही है)"
      }
    } catch (_: Exception) {}

    interp.close()

    return ModelValidationResult(
      isSuccess = true,
      sha256Hash = sha256,
      inputShape = inShape,
      inputDataType = inDataType,
      inputLayout = layout,
      inputHeight = inHeight,
      inputWidth = inWidth,
      inputChannels = inChannels,
      outputShape = outShape,
      outputDataType = outDataType,
      outputClasses = outClasses,
      recommendedNorm = recNorm,
      recommendedNsfwIndex = recNsfwIndex,
      dryRunLatencyMs = dryRunLatency,
      dryRunScore = dryRunScore,
      hasSanityWarning = hasSanityWarning,
      sanityWarningMessage = sanityWarningMsg
    )
  }

  /**
   * Finalizes import of a validated model into internal filesDir and loads instance.
   */
  fun importValidatedModel(
    context: Context,
    tempFile: File,
    suggestedName: String,
    validationResult: ModelValidationResult,
    nsfwIndex: Int = 1,
    customNormalization: NormalizationType? = null
  ): Result<TFLiteModelConfig> {
    val current = getModelConfigs(context)
    if (current.size >= MAX_MODELS) {
      tempFile.delete()
      return Result.failure(IllegalStateException("अधिकतम $MAX_MODELS मॉडल ही जोड़े जा सकते हैं। कृपया पहले एक मॉडल हटाएं।"))
    }

    val modelId = "model_" + System.currentTimeMillis()
    val fileName = "$modelId.tflite"
    val destFile = File(context.filesDir, fileName)

    try {
      tempFile.copyTo(destFile, overwrite = true)
      tempFile.delete()
    } catch (e: Exception) {
      destFile.delete()
      tempFile.delete()
      return Result.failure(e)
    }

    val finalNorm = customNormalization ?: validationResult.recommendedNorm
    val finalName = suggestedName.ifBlank { "TFLite Model ${current.size + 1}" }

    val newConfig = TFLiteModelConfig(
      id = modelId,
      displayName = finalName,
      fileName = fileName,
      nsfwOutputIndex = nsfwIndex,
      isEnabled = true,
      normalization = finalNorm,
      sha256Hash = validationResult.sha256Hash,
      inputLayout = validationResult.inputLayout,
      hasSanityWarning = validationResult.hasSanityWarning,
      sanityWarningMessage = validationResult.sanityWarningMessage
    )

    val updated = current + newConfig
    saveModelConfigs(context, updated)

    // Load into runtime
    loadModelInstance(context, newConfig)

    return Result.success(newConfig)
  }

  /**
   * Backward-compatible direct import from Uri, running full validation cycle on temp file.
   */
  fun importModelFromUri(
    context: Context,
    uri: Uri,
    suggestedName: String,
    nsfwIndex: Int = 1,
    customNormalization: NormalizationType? = null
  ): Result<TFLiteModelConfig> {
    val tempFile = try {
      File.createTempFile("val_", ".tflite", context.cacheDir).apply {
        context.contentResolver.openInputStream(uri)?.use { input ->
          FileOutputStream(this).use { output ->
            input.copyTo(output)
          }
        } ?: throw IllegalArgumentException("फ़ाइल खोली नहीं जा सकी")
      }
    } catch (e: Exception) {
      return Result.failure(e)
    }

    val validation = validateModelFile(context, tempFile)
    if (!validation.isSuccess) {
      tempFile.delete()
      val msg = validation.failureReasonHindi ?: "सत्यापन विफल"
      return Result.failure(IllegalArgumentException(msg))
    }

    return importValidatedModel(
      context = context,
      tempFile = tempFile,
      suggestedName = suggestedName,
      validationResult = validation,
      nsfwIndex = nsfwIndex,
      customNormalization = customNormalization
    )
  }

  /**
   * Removes an imported model and its storage file.
   */
  fun deleteModel(context: Context, modelId: String, pinVerified: Boolean = false): Boolean {
    if (!pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot delete AI model: PIN verification or 24-hr unlock required")
        return false
      }
    }
    synchronized(loadedInstances) {
      loadedInstances.remove(modelId)?.close()
    }
    val current = getModelConfigs(context)
    val target = current.find { it.id == modelId }
    if (target != null) {
      File(context.filesDir, target.fileName).delete()
    }
    saveModelConfigs(context, current.filterNot { it.id == modelId })
    return true
  }

  /**
   * Resets circuit breaker for a model, clearing failure count and re-enabling.
   */
  fun resetCircuitBreaker(context: Context, modelId: String) {
    synchronized(loadedInstances) {
      val inst = loadedInstances[modelId]
      if (inst != null) {
        inst.resetCircuitBreaker()
      } else {
        val cfg = getModelConfigs(context).find { it.id == modelId }
        if (cfg != null && cfg.isEnabled) {
          loadModelInstance(context, cfg)
        }
      }
    }
  }

  /**
   * Updates model display name, enabled state, NSFW output index, or normalization.
   */
  fun updateModelConfig(context: Context, updatedConfig: TFLiteModelConfig, pinVerified: Boolean = false): Boolean {
    if (!updatedConfig.isEnabled && !pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot disable AI model: PIN verification or 24-hr unlock required")
        return false
      }
    }
    val current = getModelConfigs(context).toMutableList()
    val idx = current.indexOfFirst { it.id == updatedConfig.id }
    if (idx != -1) {
      current[idx] = updatedConfig
      saveModelConfigs(context, current)
      synchronized(loadedInstances) {
        loadedInstances.remove(updatedConfig.id)?.close()
      }
      if (updatedConfig.isEnabled) {
        loadModelInstance(context, updatedConfig)
      }
    }
    return true
  }

  /**
   * Initializes all enabled models into memory.
   */
  fun initialize(context: Context) {
    val configs = getModelConfigs(context)
    for (cfg in configs) {
      if (cfg.isEnabled && !loadedInstances.containsKey(cfg.id)) {
        loadModelInstance(context, cfg)
      }
    }
  }

  private fun loadModelInstance(context: Context, config: TFLiteModelConfig) {
    val file = File(context.filesDir, config.fileName)
    if (!file.exists()) return

    try {
      val fis = FileInputStream(file)
      val channel = fis.channel
      val byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length())
      fis.close()

      val options = Interpreter.Options().apply {
        setNumThreads(2)
      }
      val interpreter = Interpreter(byteBuffer, options)

      // Inspect Input Tensor
      val inputTensor = interpreter.getInputTensor(0)
      val inShape = inputTensor.shape()
      val inputDataType = inputTensor.dataType()

      val inHeight: Int
      val inWidth: Int
      val inChannels: Int
      val detectedLayout: String

      if (inShape.size == 4) {
        if (inShape[1] == 1 || inShape[1] == 3) {
          detectedLayout = "NCHW"
          inChannels = inShape[1]
          inHeight = inShape[2]
          inWidth = inShape[3]
        } else {
          detectedLayout = "NHWC"
          inHeight = inShape[1]
          inWidth = inShape[2]
          inChannels = if (inShape.size >= 4) inShape[3] else 3
        }
      } else if (inShape.size == 3) {
        detectedLayout = "NHWC"
        inHeight = inShape[0]
        inWidth = inShape[1]
        inChannels = inShape[2]
      } else {
        detectedLayout = config.inputLayout.ifEmpty { "NHWC" }
        inHeight = 224
        inWidth = 224
        inChannels = 3
      }

      // Inspect Output Tensor
      val outputTensor = interpreter.getOutputTensor(0)
      val outShape = outputTensor.shape()
      val outputDataType = outputTensor.dataType()

      val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "TFLite-${config.displayName}").apply { isDaemon = true }
      }

      val instance = LoadedModelInstance(
        config = config,
        interpreter = interpreter,
        inputHeight = inHeight,
        inputWidth = inWidth,
        inputChannels = inChannels,
        inputDataType = inputDataType,
        inputLayout = detectedLayout,
        outputShape = outShape,
        outputDataType = outputDataType,
        executor = executor
      )

      synchronized(loadedInstances) {
        loadedInstances[config.id] = instance
      }
      Log.i(TAG, "Successfully loaded model: ${config.displayName} (Input: ${inHeight}x${inWidth}x$inChannels $inputDataType [$detectedLayout], Norm: ${config.normalization.label}, Output: ${outShape.contentToString()} $outputDataType)")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to load model instance ${config.displayName}", e)
    }
  }

  fun hasAnyActiveModel(context: Context): Boolean {
    val configs = getModelConfigs(context)
    return configs.any { it.isEnabled }
  }

  fun getRuntimeStatuses(context: Context): List<ModelRuntimeStatus> {
    val configs = getModelConfigs(context)
    val now = System.currentTimeMillis()

    return configs.map { cfg ->
      val inst = synchronized(loadedInstances) { loadedInstances[cfg.id] }
      if (inst != null) {
        val timeAgoSec = if (inst.lastRunTimestamp > 0L) (now - inst.lastRunTimestamp) / 1000L else -1L

        val healthStatus = when {
          !cfg.isEnabled -> ModelHealthStatus.NOT_WORKING
          inst.isCircuitBreakerTripped -> ModelHealthStatus.NOT_WORKING
          inst.lastError != null -> ModelHealthStatus.NOT_WORKING
          inst.hasSanityWarning || inst.isSlowLatencyWarning || inst.consecutiveFailures > 0 -> ModelHealthStatus.WARNING
          else -> ModelHealthStatus.WORKING
        }

        val reasonHindi = when {
          !cfg.isEnabled -> "मॉडल अक्षम (Disabled) है"
          inst.isCircuitBreakerTripped -> inst.circuitBreakerReason ?: "लगातार 5 विफलताएं: सर्किट ब्रेकर द्वारा बंद"
          inst.lastError != null -> "अंतिम त्रुटि: ${inst.lastError}"
          inst.isSlowLatencyWarning -> "धीमा अनुमान (${inst.lastLatency}ms > 1500ms): कतार से बचाव सक्रिय"
          inst.hasSanityWarning -> inst.sanityWarningMessage ?: "इनपुट पर कम प्रतिक्रिया दर्ज"
          inst.consecutiveFailures > 0 -> "अस्थायी विफलताएं (${inst.consecutiveFailures}/5)"
          else -> "सक्रिय और तैयार"
        }

        val whatToDo = when {
          !cfg.isEnabled -> "मॉडल सेटिंग्स में 'सक्रिय रखें' चालू करें।"
          inst.isCircuitBreakerTripped -> "'दोबारा कोशिश करें' बटन दबाएं।"
          inst.lastError != null -> "मॉडल नॉर्मलाइज़ेशन और इनपुट सेटिंग्स जाँचें।"
          inst.isSlowLatencyWarning -> "सिस्टम लोड कम करें या हल्का मॉडल चुनें।"
          inst.hasSanityWarning -> "जाँचें कि क्या यह सही विज़न क्लासिफायर मॉडल है।"
          else -> "सब कुछ सामान्य रूप से काम कर रहा है।"
        }

        ModelRuntimeStatus(
          config = cfg,
          isLoaded = true,
          healthStatus = healthStatus,
          inputShapeStr = "[1, ${inst.inputHeight}, ${inst.inputWidth}, ${inst.inputChannels}]",
          inputDTypeStr = inst.inputDataType.name,
          inputLayout = inst.inputLayout,
          outputShapeStr = inst.outputShape.contentToString(),
          outputDTypeStr = inst.outputDataType.name,
          avgLatencyMs = inst.avgLatency,
          lastLatencyMs = inst.lastLatency,
          lastScore = inst.lastScore,
          lastRunTimeAgoSec = timeAgoSec,
          totalInferences = inst.inferenceCount,
          totalFailures = inst.totalFailures,
          isCircuitBreakerTripped = inst.isCircuitBreakerTripped,
          statusReasonHindi = reasonHindi,
          whatToDoHindi = whatToDo,
          errorMessage = inst.lastError,
          isSlowLatencyWarning = inst.isSlowLatencyWarning
        )
      } else {
        ModelRuntimeStatus(
          config = cfg,
          isLoaded = false,
          healthStatus = ModelHealthStatus.NOT_WORKING,
          inputShapeStr = "—",
          inputDTypeStr = "—",
          inputLayout = cfg.inputLayout.ifEmpty { "NHWC" },
          outputShapeStr = "—",
          outputDTypeStr = "—",
          avgLatencyMs = 0L,
          lastLatencyMs = 0L,
          lastScore = 0f,
          lastRunTimeAgoSec = -1L,
          totalInferences = 0L,
          totalFailures = 0L,
          isCircuitBreakerTripped = false,
          statusReasonHindi = if (!cfg.isEnabled) "अक्षम (Disabled)" else "फ़ाइल लोड नहीं हुई",
          whatToDoHindi = if (!cfg.isEnabled) "सक्रिय रखें चालू करें।" else "मॉडल को हटाएं और दोबारा जोड़ें।",
          errorMessage = if (!cfg.isEnabled) "अक्षम (Disabled)" else "लोड नहीं हुआ",
          isSlowLatencyWarning = false
        )
      }
    }
  }

  /**
   * Evaluates an image bitmap across all active models synchronously for the "Test Image" button.
   */
  fun testImageOnModels(context: Context, bitmap: Bitmap): List<ModelTestReport> {
    val activeInstances = synchronized(loadedInstances) {
      loadedInstances.values.toList()
    }
    if (activeInstances.isEmpty()) return emptyList()

    return activeInstances.map { inst ->
      val res = runSingleModelInference(inst, bitmap)
      ModelTestReport(
        modelId = inst.config.id,
        modelName = inst.config.displayName,
        score = res.score,
        scorePercent = (res.score * 100).toInt(),
        latencyMs = res.latencyMs,
        normalizationLabel = inst.config.normalization.label,
        layout = inst.inputLayout,
        isSuccess = res.isSuccess,
        errorMessage = res.errorMessage
      )
    }
  }

  /**
   * Prepares ByteBuffer with layout-awareness (NCHW or NHWC) and selected Normalization.
   */
  private fun prepareInputBuffer(
    bitmap: Bitmap,
    inputWidth: Int,
    inputHeight: Int,
    inputChannels: Int,
    inputDataType: DataType,
    layout: String,
    norm: NormalizationType
  ): ByteBuffer {
    val scaled = if (bitmap.width == inputWidth && bitmap.height == inputHeight) {
      bitmap
    } else {
      Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)
    }

    val totalPixels = inputWidth * inputHeight
    val intValues = IntArray(totalPixels)
    scaled.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight)

    if (scaled != bitmap) {
      scaled.recycle()
    }

    val bytesPerChannel = if (inputDataType == DataType.FLOAT32) 4 else 1
    val bufferSize = 1 * inputHeight * inputWidth * inputChannels * bytesPerChannel
    val buffer = ByteBuffer.allocateDirect(bufferSize).apply {
      order(ByteOrder.nativeOrder())
    }

    val isFloat = inputDataType == DataType.FLOAT32
    val isNCHW = layout.equals("NCHW", ignoreCase = true)
    val isBGR = norm == NormalizationType.BGR_MEAN_SUBTRACT

    fun putChannelValue(value: Int, channelIdx: Int) {
      if (isFloat) {
        when (norm) {
          NormalizationType.ZERO_TO_ONE -> {
            buffer.putFloat(value / 255.0f)
          }
          NormalizationType.MINUS_ONE_TO_ONE -> {
            buffer.putFloat((value / 127.5f) - 1.0f)
          }
          NormalizationType.IMAGENET -> {
            val mean = when (channelIdx) {
              0 -> 0.485f
              1 -> 0.456f
              else -> 0.406f
            }
            val std = when (channelIdx) {
              0 -> 0.229f
              1 -> 0.224f
              else -> 0.225f
            }
            buffer.putFloat(((value / 255.0f) - mean) / std)
          }
          NormalizationType.BGR_MEAN_SUBTRACT -> {
            // OpenNSFW2 style BGR Mean subtraction: B: 104.007, G: 116.669, R: 122.679
            val mean = when (channelIdx) {
              0 -> 104.00698793f // B
              1 -> 116.66876762f // G
              else -> 122.67891434f // R
            }
            buffer.putFloat(value.toFloat() - mean)
          }
          NormalizationType.UINT8_NONE -> {
            buffer.putFloat(value.toFloat())
          }
        }
      } else {
        buffer.put(value.toByte())
      }
    }

    if (isNCHW) {
      // Channel 0 Plane (R or B)
      for (pixel in intValues) {
        val c0 = if (isBGR) (pixel and 0xFF) else ((pixel shr 16) and 0xFF)
        putChannelValue(c0, 0)
      }
      // Channel 1 Plane (G)
      for (pixel in intValues) {
        val c1 = (pixel shr 8) and 0xFF
        putChannelValue(c1, 1)
      }
      // Channel 2 Plane (B or R)
      for (pixel in intValues) {
        val c2 = if (isBGR) ((pixel shr 16) and 0xFF) else (pixel and 0xFF)
        putChannelValue(c2, 2)
      }
    } else {
      // NHWC: Interleaved pixels
      for (pixel in intValues) {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        val c0 = if (isBGR) b else r
        val c1 = g
        val c2 = if (isBGR) r else b
        putChannelValue(c0, 0)
        putChannelValue(c1, 1)
        putChannelValue(c2, 2)
      }
    }

    buffer.rewind()
    return buffer
  }

  /**
   * Evaluates a screen bitmap across all loaded models concurrently.
   */
  fun evaluateBitmap(
    context: Context,
    bitmap: Bitmap,
    checkIntervalMs: Long = DEFAULT_INSPECTION_INTERVAL_MS
  ): EnsembleDecision {
    val now = System.currentTimeMillis()
    if (now - lastEvaluationTime > 7000L) {
      consecutivePositiveFrames = 0
    }
    lastEvaluationTime = now

    val activeInstances = synchronized(loadedInstances) {
      loadedInstances.values.toList()
    }

    if (activeInstances.isEmpty()) {
      return EnsembleDecision(
        isNsfwTrigger = false,
        individualResults = emptyList(),
        reason = "कोई सक्रिय मॉडल नहीं (AI पहचान सीमित)",
        consecutiveFrames = 0
      )
    }

    val tasks = mutableListOf<Triple<LoadedModelInstance, Future<ModelInferenceResult>?, ModelInferenceResult?>>()

    for (inst in activeInstances) {
      // Circuit Breaker check: If model has tripped, skip and don't spam
      if (inst.isCircuitBreakerTripped) {
        val trippedResult = ModelInferenceResult(
          modelId = inst.config.id,
          modelName = inst.config.displayName,
          score = 0f,
          latencyMs = 0L,
          isSuccess = false,
          errorMessage = inst.circuitBreakerReason ?: "सर्किट ब्रेकर द्वारा बंद"
        )
        tasks.add(Triple(inst, null, trippedResult))
        continue
      }

      // 1. Skip next check if previous inference was longer than inspection interval
      if (inst.skipNextCheck) {
        inst.skipNextCheck = false
        Log.i(TAG, "Skipping inference for model ${inst.config.displayName} (last latency: ${inst.lastLatency}ms > interval: ${checkIntervalMs}ms). Queue prevented.")
        val skippedResult = ModelInferenceResult(
          modelId = inst.config.id,
          modelName = inst.config.displayName,
          score = inst.lastScore,
          latencyMs = inst.lastLatency,
          isSuccess = true,
          errorMessage = "कतार से बचाव: अनुमान समय (${inst.lastLatency}ms > ${checkIntervalMs}ms) लंबा होने से यह जाँच छोड़ी गई"
        )
        tasks.add(Triple(inst, null, skippedResult))
        continue
      }

      // 2. Prevent overlapping executions if previous task is still running on background thread
      if (!inst.isBusy.compareAndSet(false, true)) {
        Log.i(TAG, "Model ${inst.config.displayName} is busy executing. Skipping to avoid queue.")
        val busyResult = ModelInferenceResult(
          modelId = inst.config.id,
          modelName = inst.config.displayName,
          score = inst.lastScore,
          latencyMs = inst.lastLatency,
          isSuccess = true,
          errorMessage = "कतार से बचाव: मॉडल पहले से व्यस्त है"
        )
        tasks.add(Triple(inst, null, busyResult))
        continue
      }

      val future = inst.executor.submit<ModelInferenceResult> {
        try {
          runSingleModelInference(inst, bitmap)
        } finally {
          inst.isBusy.set(false)
        }
      }
      tasks.add(Triple(inst, future, null))
    }

    val results = mutableListOf<ModelInferenceResult>()
    for ((inst, future, precomputed) in tasks) {
      if (precomputed != null) {
        results.add(precomputed)
        continue
      }
      if (future != null) {
        try {
          val timeoutMs = (checkIntervalMs + 500L).coerceAtLeast(1500L)
          val res = future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
          results.add(res)

          if (res.isSuccess) {
            inst.consecutiveFailures = 0
            inst.inferenceCount++
            inst.lastScore = res.score
            inst.lastLatency = res.latencyMs
            inst.totalLatency += res.latencyMs
            inst.lastRunTimestamp = System.currentTimeMillis()
            inst.lastError = null

            if (res.latencyMs > checkIntervalMs) {
              inst.skipNextCheck = true
              Log.w(TAG, "Model ${inst.config.displayName} took ${res.latencyMs}ms (> ${checkIntervalMs}ms). Skipping next check.")
            }
          } else {
            handleModelFailure(context, inst, res.errorMessage ?: "अज्ञात त्रुटि")
          }
        } catch (e: Exception) {
          inst.isBusy.set(false)
          val err = "समय सीमा समाप्त या निष्पादन त्रुटि: ${e.message}"
          handleModelFailure(context, inst, err)
          results.add(
            ModelInferenceResult(
              modelId = inst.config.id,
              modelName = inst.config.displayName,
              score = 0f,
              latencyMs = 0L,
              isSuccess = false,
              errorMessage = err
            )
          )
        }
      }
    }

    val singleThreshold = getSingleModelThreshold(context)
    val duoThreshold = getDuoModelThreshold(context)

    val successfulScores = results.filter { it.isSuccess }.map { it.score }

    val anySingleHigh = successfulScores.any { it >= singleThreshold }
    val duoModerate = successfulScores.count { it >= duoThreshold } >= 2

    val isFramePositive = anySingleHigh || duoModerate

    if (isFramePositive) {
      consecutivePositiveFrames++
    } else {
      consecutivePositiveFrames = maxOf(0, consecutivePositiveFrames - 1)
    }

    val isTrigger = consecutivePositiveFrames >= 2

    val reason = when {
      anySingleHigh -> "एकल मॉडल उच्च स्कोर (>= ${(singleThreshold * 100).toInt()}%)"
      duoModerate -> "दोहरा मॉडल मध्यम स्कोर (कम से कम 2 मॉडल >= ${(duoThreshold * 100).toInt()}%)"
      else -> "सुरक्षित (सीमा से कम)"
    }

    return EnsembleDecision(
      isNsfwTrigger = isTrigger,
      individualResults = results,
      reason = "$reason (फ़्रेम पुष्टि: $consecutivePositiveFrames/2)",
      consecutiveFrames = consecutivePositiveFrames
    )
  }

  private fun handleModelFailure(context: Context, inst: LoadedModelInstance, errorMsg: String) {
    inst.consecutiveFailures++
    inst.totalFailures++
    inst.lastError = errorMsg

    if (inst.consecutiveFailures >= 5 && !inst.isCircuitBreakerTripped) {
      inst.isCircuitBreakerTripped = true
      inst.circuitBreakerReason = "लगातार 5 बार विफल होने पर सर्किट ब्रेकर द्वारा ऑटो-डिसएबल किया गया"
      Log.e(TAG, "Circuit breaker tripped for ${inst.config.displayName} after 5 consecutive failures")

      try {
        ProtectionHealthManager.recordLogEntry(
          context = context,
          eventType = "AI_MODEL_CIRCUIT_BREAKER",
          message = "मॉडल '${inst.config.displayName}' लगातार 5 बार विफल होने पर ऑटो-डिसएबल किया गया।",
          severity = "ERROR"
        )
      } catch (_: Exception) {}
    }
  }

  private fun runSingleModelInference(inst: LoadedModelInstance, originalBitmap: Bitmap): ModelInferenceResult {
    val startTime = System.currentTimeMillis()
    try {
      val inputBuffer = prepareInputBuffer(
        bitmap = originalBitmap,
        inputWidth = inst.inputWidth,
        inputHeight = inst.inputHeight,
        inputChannels = inst.inputChannels,
        inputDataType = inst.inputDataType,
        layout = inst.inputLayout,
        norm = inst.config.normalization
      )

      val outSize = if (inst.outputShape.isNotEmpty()) inst.outputShape.last() else 2
      val outputArray = Array(1) { FloatArray(outSize) }

      inst.interpreter.run(inputBuffer, outputArray)

      val targetIdx = inst.config.nsfwOutputIndex.coerceIn(0, outSize - 1)
      val rawScore = outputArray[0][targetIdx]

      // Output sanity check: NaN, Infinity
      if (rawScore.isNaN() || rawScore.isInfinite()) {
        throw IllegalStateException("मॉडल आउटपुट में NaN या Infinity आया")
      }

      val nsfwScore = rawScore.coerceIn(0.0f, 1.0f)
      val latency = System.currentTimeMillis() - startTime

      return ModelInferenceResult(
        modelId = inst.config.id,
        modelName = inst.config.displayName,
        score = nsfwScore,
        latencyMs = latency,
        isSuccess = true
      )
    } catch (e: Exception) {
      val latency = System.currentTimeMillis() - startTime
      return ModelInferenceResult(
        modelId = inst.config.id,
        modelName = inst.config.displayName,
        score = 0f,
        latencyMs = latency,
        isSuccess = false,
        errorMessage = e.message ?: "अज्ञात त्रुटि"
      )
    }
  }

  fun reset() {
    consecutivePositiveFrames = 0
    lastEvaluationTime = 0L
  }
}
