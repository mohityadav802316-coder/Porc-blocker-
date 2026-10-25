package com.example.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.shortcontent.ShortContentAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * Foreground Service running on-device MediaProjection screen capture and TFLite inference.
 *
 * Guaranteed Properties:
 * - 100% Offline: Zero network permissions/calls used for capture or inference.
 * - Zero Disk Storage: No images or frame buffers are ever saved to cache or external storage.
 * - Ultra-lightweight: Captures downsampled frames at ~3.5s interval to preserve battery.
 */
class AiScreenDetectionService : Service() {

  companion object {
    private const val TAG = "AiScreenDetectService"
    private const val NOTIFICATION_ID = 9102
    private const val CHANNEL_ID = "ai_screen_detection_channel"

    const val ACTION_START = "com.example.focus.ACTION_START_AI_DETECTION"
    const val ACTION_STOP = "com.example.focus.ACTION_STOP_AI_DETECTION"
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_DATA = "extra_data"

    // Downsampled resolution for fast on-device neural processing without battery drain
    private const val CAPTURE_WIDTH = 360
    private const val CAPTURE_HEIGHT = 640
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var scanJob: Job? = null

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null
  private var powerManager: PowerManager? = null

  override fun onCreate() {
    super.onCreate()
    powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
    createNotificationChannel()
    TfLiteNsfwClassifier.initialize(this)
    MultiModelClassifierManager.initialize(this)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      Log.i(TAG, "Stopping AI Screen Detection Service by request")
      stopCapture()
      stopForeground(STOP_FOREGROUND_REMOVE)
      stopSelf()
      return START_NOT_STICKY
    }

    val notification = buildNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }

    val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
    val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent?.getParcelableExtra(EXTRA_DATA)
    }

    if (resultCode != 0 && data != null) {
      setupMediaProjection(resultCode, data)
    } else if (mediaProjection == null) {
      Log.w(TAG, "No MediaProjection data available, stopping service")
      stopSelf()
      return START_NOT_STICKY
    }

    return START_STICKY
  }

  private fun setupMediaProjection(resultCode: Int, data: Intent) {
    try {
      val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager ?: return
      mediaProjection?.stop()
      mediaProjection = mpManager.getMediaProjection(resultCode, data)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
          override fun onStop() {
            Log.i(TAG, "MediaProjection stopped by system")
            stopCapture()
          }
        }, null)
      }

      val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      wm.defaultDisplay.getMetrics(metrics)
      val densityDpi = metrics.densityDpi

      imageReader?.close()
      imageReader = ImageReader.newInstance(
        CAPTURE_WIDTH,
        CAPTURE_HEIGHT,
        PixelFormat.RGBA_8888,
        2
      )

      virtualDisplay?.release()
      virtualDisplay = mediaProjection?.createVirtualDisplay(
        "LifeTrackerAiCapture",
        CAPTURE_WIDTH,
        CAPTURE_HEIGHT,
        densityDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        imageReader?.surface,
        null,
        null
      )

      startAnalysisLoop()
      Log.i(TAG, "MediaProjection & VirtualDisplay initialized successfully for on-device NSFW scanning")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to initialize MediaProjection capture", e)
      stopSelf()
    }
  }

  private fun startAnalysisLoop() {
    scanJob?.cancel()
    scanJob = serviceScope.launch {
      while (isActive) {
        delay(3500L) // Analyze a frame every 3.5 seconds

        // 1. Skip if screen is off to preserve battery
        if (powerManager?.isInteractive == false) {
          continue
        }

        // 2. Skip if Strict Lock is already active (already locked down)
        if (FocusModeManager.isStrictLockActive(this@AiScreenDetectionService)) {
          continue
        }

        // 3. Acquire frame from VirtualDisplay ImageReader
        val reader = imageReader ?: continue
        var image: Image? = null
        var bitmap: Bitmap? = null

        try {
          image = reader.acquireLatestImage()
          if (image == null) continue

          bitmap = imageToBitmap(image)
        } catch (e: Exception) {
          Log.w(TAG, "Failed reading capture buffer: ${e.message}")
        } finally {
          image?.close()
        }

        if (bitmap == null) continue

        try {
          if (MultiModelClassifierManager.hasAnyActiveModel(this@AiScreenDetectionService)) {
            val decision = MultiModelClassifierManager.evaluateBitmap(
              context = this@AiScreenDetectionService,
              bitmap = bitmap,
              checkIntervalMs = 3500L
            )

            if (decision.isNsfwTrigger) {
              Log.w(TAG, "🚨 NSFW SCREEN CONTENT CONFIRMED BY MULTI-MODEL ENSEMBLE! ${decision.reason}. Activating Strict Lock.")
              val handled = ShortContentAccessibilityService.performGlobalHome()
              if (!handled) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                  addCategory(Intent.CATEGORY_HOME)
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
              }

              FocusModeManager.activateStrictLock(
                context = this@AiScreenDetectionService,
                durationMinutes = 15,
                reason = "AI स्क्रीन डिटेक्शन (मल्टी-मॉडल): ${decision.reason}",
                triggerWord = "Multi-Model Ensemble",
                packageName = "Screen (AI On-Device)"
              )

              FocusBlockingOverlayManager.showStrictLock(
                context = this@AiScreenDetectionService,
                blockedPackage = "Screen (On-Device Vision)",
                matchedTerm = "AI Ensemble Match"
              )

              AiScreenDetectionManager.incrementInterceptionCount(this@AiScreenDetectionService)
              MultiModelClassifierManager.reset()
            }
          } else if (TfLiteNsfwClassifier.isModelAvailable()) {
            val threshold = AiScreenDetectionManager.getThreshold(this@AiScreenDetectionService)
            val result = TfLiteNsfwClassifier.classify(bitmap, threshold)

            if (result.isNsfw) {
              val scorePercent = (result.confidence * 100f).toInt()
              Log.w(TAG, "🚨 NSFW SCREEN CONTENT CONFIRMED BY TFLITE! Score: $scorePercent%. Activating Strict Lock.")

              // Immediately send unallowed screen to Home
              val handled = ShortContentAccessibilityService.performGlobalHome()
              if (!handled) {
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                  addCategory(Intent.CATEGORY_HOME)
                  flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
              }

              // Trigger Phase 1 Strict Lock (15 minutes unbreakable)
              FocusModeManager.activateStrictLock(
                context = this@AiScreenDetectionService,
                durationMinutes = 15,
                reason = "AI स्क्रीन डिटेक्शन: वयस्क सामग्री पहचानी गई ($scorePercent%)",
                triggerWord = "AI Vision ($scorePercent%)",
                packageName = "Screen (AI On-Device)"
              )

              // Display full-screen blocking overlay
              FocusBlockingOverlayManager.showStrictLock(
                context = this@AiScreenDetectionService,
                blockedPackage = "Screen (On-Device Vision)",
                matchedTerm = "AI NSFW ($scorePercent%)"
              )

              AiScreenDetectionManager.incrementInterceptionCount(this@AiScreenDetectionService)
              TfLiteNsfwClassifier.reset()
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error in on-device AI frame classification", e)
        } finally {
          bitmap.recycle() // 🛡️ Zero memory leaks, zero disk storage
        }
      }
    }
  }

  /**
   * Converts ImageReader RGBA_8888 Image plane into an in-memory Bitmap.
   */
  private fun imageToBitmap(image: Image): Bitmap? {
    val planes = image.planes
    if (planes.isEmpty()) return null

    val buffer: ByteBuffer = planes[0].buffer
    val pixelStride = planes[0].pixelStride
    val rowStride = planes[0].rowStride
    val rowPadding = rowStride - pixelStride * image.width

    val bitmap = Bitmap.createBitmap(
      image.width + rowPadding / pixelStride,
      image.height,
      Bitmap.Config.ARGB_8888
    )
    bitmap.copyPixelsFromBuffer(buffer)

    // Crop padding if rowPadding exists
    return if (rowPadding == 0) {
      bitmap
    } else {
      val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
      bitmap.recycle()
      cropped
    }
  }

  private fun stopCapture() {
    scanJob?.cancel()
    scanJob = null
    try {
      virtualDisplay?.release()
      virtualDisplay = null
      imageReader?.close()
      imageReader = null
      mediaProjection?.stop()
      mediaProjection = null
    } catch (e: Exception) {
      Log.e(TAG, "Error releasing capture resources", e)
    }
    AiScreenDetectionManager.setEnabled(this, false)
  }

  override fun onDestroy() {
    super.onDestroy()
    stopCapture()
    serviceScope.cancel()
    Log.i(TAG, "AiScreenDetectionService destroyed cleanly")
  }

  private fun buildNotification(): Notification {
    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val contentPending = PendingIntent.getActivity(this, 9102, openIntent, flags)

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🛡️ AI Screen Detection सक्रिय")
      .setContentText("ऑन-डिवाइस TFLite मॉडल स्क्रीन सुरक्षा निगरानी कर रहा है (100% ऑफ़लाइन)")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("ऑन-डिवाइस MobileNetV2 TFLite विज़न मॉडल स्क्रीन की निगरानी कर रहा है।\nकोई तस्वीर सेव या फ़ोन से बाहर नहीं जाती। वयस्क दृश्य दिखते ही 15 मिनट का सख्त लॉक सक्रिय होगा।")
      )
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setContentIntent(contentPending)
      .build()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (nm.getNotificationChannel(CHANNEL_ID) == null) {
        val channel = NotificationChannel(
          CHANNEL_ID,
          "AI Screen Protection Service",
          NotificationManager.IMPORTANCE_LOW
        ).apply {
          description = "Maintains persistent on-device TFLite vision screen monitoring"
          setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
      }
    }
  }
}
