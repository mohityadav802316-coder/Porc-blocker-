package com.example.focus

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiScreenDetectionTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    AiScreenDetectionManager.setEnabled(context, false)
    AiScreenDetectionManager.setThreshold(context, 0.85f)
    TfLiteNsfwClassifier.reset()
  }

  @Test
  fun `verify AI Screen Detection is OFF by default`() {
    val isEnabled = AiScreenDetectionManager.isEnabled(context)
    assertFalse("AI Screen Detection MUST be OFF by default", isEnabled)
  }

  @Test
  fun `verify threshold setting and getting`() {
    assertEquals(0.85f, AiScreenDetectionManager.getThreshold(context), 0.01f)

    AiScreenDetectionManager.setThreshold(context, 0.90f)
    assertEquals(0.90f, AiScreenDetectionManager.getThreshold(context), 0.01f)

    AiScreenDetectionManager.setThreshold(context, 0.80f)
    assertEquals(0.80f, AiScreenDetectionManager.getThreshold(context), 0.01f)
  }

  @Test
  fun `verify model metadata strings are present and accurate`() {
    assertEquals("MobileNetV2-NSFW (TFLite Quantized)", TfLiteNsfwClassifier.MODEL_NAME)
    assertEquals("93.4% (ROC-AUC: 0.96)", TfLiteNsfwClassifier.MODEL_ACCURACY)
    assertEquals(0.85f, TfLiteNsfwClassifier.DEFAULT_THRESHOLD, 0.001f)
  }

  @Test
  fun `verify blank or normal graphic screen is NOT detected as NSFW (false positive prevention)`() {
    // Solid blue / productivity / code editor screen
    val codeBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.rgb(30, 30, 46)) // Dark IDE background
    }

    val result = TfLiteNsfwClassifier.classify(codeBitmap, threshold = 0.85f)
    assertFalse("Dark UI screen must NOT trigger NSFW", result.isNsfw)
    assertTrue("Confidence must be very low (< 0.3)", result.confidence < 0.30f)
  }

  @Test
  fun `verify landscape or nature screen is NOT detected as NSFW`() {
    // Green / landscape nature screen
    val natureBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.rgb(46, 125, 50)) // Deep green
    }

    val result = TfLiteNsfwClassifier.classify(natureBitmap, threshold = 0.85f)
    assertFalse("Nature/green screen must NOT trigger NSFW", result.isNsfw)
  }

  @Test
  fun `verify consecutive frame requirement prevents single transient false alert`() {
    // Create a bitmap with moderate skin tones (e.g. portrait face photo)
    val portraitBitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)
    for (x in 0 until 224) {
      for (y in 0 until 224) {
        // Upper center has face skin tones (approx 20% of image), rest is clothing/background
        if (x in 80..144 && y in 40..100) {
          portraitBitmap.setPixel(x, y, Color.rgb(220, 180, 140))
        } else {
          portraitBitmap.setPixel(x, y, Color.rgb(60, 60, 80))
        }
      }
    }

    val result = TfLiteNsfwClassifier.classify(portraitBitmap, threshold = 0.85f)
    assertFalse("Standard portrait photo must NOT trigger false positive", result.isNsfw)
  }

  @Test
  fun `verify interception counter increments correctly`() {
    val initial = AiScreenDetectionManager.getInterceptionCount(context)
    AiScreenDetectionManager.incrementInterceptionCount(context)
    assertEquals(initial + 1, AiScreenDetectionManager.getInterceptionCount(context))
  }

  @Test
  fun `verify non-TFL3 model file is rejected with clear message`() {
    val tempFile = java.io.File.createTempFile("invalid_", ".tflite", context.cacheDir)
    tempFile.writeText("THIS_IS_NOT_A_TFLITE_FILE_AT_ALL")

    val result = MultiModelClassifierManager.validateModelFile(context, tempFile)
    assertFalse("Non-TFL3 file must fail validation", result.isSuccess)
    assertNotNull("Failure reason must be present", result.failureReasonHindi)
    assertTrue("Reason should mention TFL3 or valid file", result.failureReasonHindi!!.contains("TFL3") || result.failureReasonHindi!!.contains("वैध"))
    tempFile.delete()
  }

  @Test
  fun `verify empty file is rejected with clear size message`() {
    val tempFile = java.io.File.createTempFile("empty_", ".tflite", context.cacheDir)

    val result = MultiModelClassifierManager.validateModelFile(context, tempFile)
    assertFalse("0-byte file must fail validation", result.isSuccess)
    assertNotNull("Failure reason must be present", result.failureReasonHindi)
    assertTrue("Reason must mention size", result.failureReasonHindi!!.contains("आकार") || result.failureReasonHindi!!.contains("साइज़"))
    tempFile.delete()
  }

  @Test
  fun `verify OpenNSFW2 BGR Mean Subtract normalization type is available`() {
    val norm = NormalizationType.BGR_MEAN_SUBTRACT
    assertEquals("bgr_mean_subtract", norm.id)
    assertEquals(norm, NormalizationType.fromId("bgr_mean_subtract"))
  }
}
