package com.example.shortcontent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.focus.FocusBlockingOverlayManager
import com.example.focus.FocusModeManager
import com.example.focus.StrictLockService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Strict-Mode Android Accessibility Service for Short Content Tracking and System-Wide Focus Blocking.
 *
 * Capabilities:
 * 1. System-Wide Focus App Interception: Detects non-allowed foreground applications and triggers
 *    BlockSite-style full overlay protection or redirects to Home.
 * 2. Privacy & Security: Only reads package identity on window state changes; never logs keystrokes,
 *    passwords, or personal chat messages.
 * 3. Short-form Video Analytics for Instagram, YouTube, and Facebook.
 */
class ShortContentAccessibilityService : AccessibilityService() {

  companion object {
    private var instanceRef: WeakReference<ShortContentAccessibilityService>? = null

    fun performGlobalHome(): Boolean {
      val service = instanceRef?.get() ?: return false
      return service.performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun isServiceRunning(): Boolean = instanceRef?.get() != null

    fun disableService(): Boolean {
      val service = instanceRef?.get() ?: return false
      service.disableSelf()
      return true
    }

    fun canTakeScreenshotGranted(): Boolean {
      val service = instanceRef?.get() ?: return false
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
      val caps = service.serviceInfo?.capabilities ?: 0
      return (caps and AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT) != 0
    }
  }

  private lateinit var trackerManager: ShortContentTrackerManager
  private var lastCheckedPackage: String? = null
  private var lastContentChangedTimestamp: Long = 0L
  private var lastCapabilityWarnTimestamp: Long = 0L
  private var lastScreenshotSuccessTimestamp: Long = 0L
  private var powerManager: android.os.PowerManager? = null

  // Background coroutine scope for periodic on-device AI screen screenshot inspection & heartbeat
  private val serviceScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
  private var aiScreenshotJob: kotlinx.coroutines.Job? = null
  private var heartbeatJob: kotlinx.coroutines.Job? = null
  private var isScreenshotInFlight = false

  // Multi-user & Guest profile bypass detection throttling
  private var lastMultiUserCheckTimestamp: Long = 0L
  private var lastMultiUserBlockedTimestamp: Long = 0L
  private var lastSettingsContentChangeCheckTimestamp: Long = 0L
  private var lastTamperBlockedTimestamp: Long = 0L

  /**
   * Fast, bounded recursive text extraction from an accessibility node hierarchy.
   * Traverses up to maxDepth levels and maxNodes count to prevent any UI lag.
   */
  private fun extractAllNodeTexts(root: AccessibilityNodeInfo?, maxDepth: Int = 5, maxNodes: Int = 40): String {
    if (root == null) return ""
    val sb = StringBuilder()
    var visitedCount = 0

    fun traverse(node: AccessibilityNodeInfo?, depth: Int) {
      if (node == null || depth > maxDepth || visitedCount >= maxNodes) return
      visitedCount++

      val txt = node.text
      if (!txt.isNullOrBlank()) {
        sb.append(txt).append(' ')
      }
      val desc = node.contentDescription
      if (!desc.isNullOrBlank()) {
        sb.append(desc).append(' ')
      }

      val childCount = node.childCount
      for (i in 0 until childCount) {
        if (visitedCount >= maxNodes) break
        val child = try { node.getChild(i) } catch (_: Exception) { null }
        if (child != null) {
          traverse(child, depth + 1)
          try { child.recycle() } catch (_: Exception) {}
        }
      }
    }

    traverse(root, 0)
    return sb.toString()
  }

  /**
   * Executes multi-user bypass block:
   * 1. GLOBAL_ACTION_BACK immediately.
   * 2. Shows toast message: "यह विकल्प सुरक्षा के लिए बंद है".
   * 3. Schedules a verification check 1 second later: if screen is still settings or target, executes GLOBAL_ACTION_HOME.
   * 4. Logs to Health Log with detected texts and action taken.
   */
  private fun blockMultiUserBypass(
    packageName: String,
    className: String,
    detectionResult: com.example.focus.MultiUserProtectionManager.DetectionResult
  ) {
    val now = System.currentTimeMillis()
    if (now - lastMultiUserBlockedTimestamp < 1000L) return
    lastMultiUserBlockedTimestamp = now

    val matchedSummary = detectionResult.matchedTexts.joinToString(", ")
    Log.w("ShortContentService", "MULTI-USER BYPASS DETECTED in $packageName ($className)! Matched: [$matchedSummary]. Executing BACK.")

    // Step 1: Immediate GLOBAL_ACTION_BACK
    performGlobalAction(GLOBAL_ACTION_BACK)

    // Step 2: Inform user with message
    android.os.Handler(android.os.Looper.getMainLooper()).post {
      android.widget.Toast.makeText(applicationContext, "यह विकल्प सुरक्षा के लिए बंद है", android.widget.Toast.LENGTH_SHORT).show()
    }

    // Step 3: Record in Health Log (detected texts, action)
    com.example.focus.MultiUserProtectionManager.recordInterception(
      context = this,
      detail = "$packageName ($className) | Matched: [$matchedSummary]",
      actionTaken = "GLOBAL_ACTION_BACK (1s fallback HOME scheduled)"
    )

    if (Settings.canDrawOverlays(this)) {
      FocusBlockingOverlayManager.show(
        context = this,
        blockedPackage = packageName,
        customTitle = "🛡️ मल्टी-यूज़र सुरक्षा सक्रिय",
        customNotice = "उपयोगकर्ता बदलने, नया प्रोफ़ाइल या गेस्ट बनाने का विकल्प सुरक्षा के लिए बंद है।"
      )
    }

    // Step 4: After 1 second, verify if screen is still settings / not exited -> execute GLOBAL_ACTION_HOME
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
      try {
        val currentRoot = rootInActiveWindow
        val currentPkg = currentRoot?.packageName?.toString() ?: ""
        val currentText = extractAllNodeTexts(currentRoot, maxDepth = 4, maxNodes = 25)
        val stillMatching = com.example.focus.MultiUserProtectionManager.evaluateScreenDetection(
          context = applicationContext,
          packageName = currentPkg,
          className = currentRoot?.className?.toString() ?: "",
          visibleText = currentText
        ).isBlocked

        if (stillMatching || currentPkg == "com.android.settings" || currentPkg == "com.google.android.settings") {
          Log.w("ShortContentService", "Screen still active after 1s! Executing fallback GLOBAL_ACTION_HOME.")
          performGlobalAction(GLOBAL_ACTION_HOME)
        }
      } catch (e: Exception) {
        Log.w("ShortContentService", "Fallback home check exception", e)
      }
    }, 1000L)
  }

  override fun onCreate() {
    super.onCreate()
    instanceRef = WeakReference(this)
    powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    trackerManager = ShortContentTrackerManager.getInstance(applicationContext)
    com.example.focus.TfLiteNsfwClassifier.initialize(applicationContext)
    com.example.focus.MultiModelClassifierManager.initialize(applicationContext)
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    instanceRef = WeakReference(this)
    Log.i("ShortContentService", "ShortContentAccessibilityService connected successfully")

    com.example.focus.ProtectionHealthManager.recordAccessibilityHeartbeat(applicationContext)
    com.example.focus.ProtectionHealthManager.logHealthEvent(
      context = applicationContext,
      eventType = "SERVICE_STARTED",
      message = "Accessibility Service कनेक्ट हुई और सक्रिय है।",
      severity = "INFO"
    )

    // Dynamically configure serviceInfo
    try {
      val info = serviceInfo ?: AccessibilityServiceInfo()
      info.packageNames = null
      info.eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
      info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
      info.notificationTimeout = 100
      info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
        AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
      serviceInfo = info
    } catch (e: Exception) {
      Log.w("ShortContentService", "Failed to update dynamic serviceInfo", e)
    }

    try {
      trackerManager.notifyServiceConnected(true)
    } catch (_: Exception) {}

    startHeartbeatLoop()
    startAiScreenshotInspectionLoop()
  }

  private fun startHeartbeatLoop() {
    heartbeatJob?.cancel()
    heartbeatJob = serviceScope.launch {
      while (isActive) {
        delay(60_000L) // 1 minute pulse
        com.example.focus.ProtectionHealthManager.recordAccessibilityHeartbeat(applicationContext)
      }
    }
  }

  /**
   * Periodic On-Device AI Screen Inspection via AccessibilityService.takeScreenshot()
   * Runs every 1.5 seconds without requiring MediaProjection dialog.
   * Completely pauses when device screen is turned off.
   */
  private fun startAiScreenshotInspectionLoop() {
    aiScreenshotJob?.cancel()
    aiScreenshotJob = serviceScope.launch {
      while (isActive) {
        delay(1500L)

        // 1. Completely pause inspection if screen is turned off
        if (powerManager?.isInteractive == false) {
          continue
        }

        // 2. Only scan when AI Screen Detection is enabled by user OR active models exist
        val isAiExplicitlyEnabled = com.example.focus.AiScreenDetectionManager.isEnabled(applicationContext)
        val hasActiveModels = com.example.focus.MultiModelClassifierManager.hasAnyActiveModel(applicationContext)
        if (!isAiExplicitlyEnabled && !hasActiveModels) {
          continue
        }

        // Only Android 11+ (API 30+) supports AccessibilityService.takeScreenshot()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          // Check if system granted canTakeScreenshot capability to this running service
          val caps = serviceInfo?.capabilities ?: 0
          val canTakeScreenshot = (caps and AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT) != 0
          if (!canTakeScreenshot) {
            val now = System.currentTimeMillis()
            if (now - lastCapabilityWarnTimestamp > 180_000L) {
              lastCapabilityWarnTimestamp = now
              Log.w("ShortContentService", "canTakeScreenshot capability is NOT granted by OS! Accessibility must be toggled OFF and ON.")
              com.example.focus.ProtectionHealthManager.recordLogEntry(
                context = applicationContext,
                eventType = "AI_CAPABILITY_MISSING",
                message = "Accessibility स्क्रीनशॉट अनुमति (canTakeScreenshot) OS द्वारा रोकी गई है। कृपया Android सेटिंग्स में Life Tracker Accessibility को एक बार OFF करके दोबारा ON करें।",
                severity = "ERROR"
              )
            }
            continue
          }

          if (isScreenshotInFlight) continue

          isScreenshotInFlight = true
          try {
            takeScreenshot(
              android.view.Display.DEFAULT_DISPLAY,
              applicationContext.mainExecutor,
              object : TakeScreenshotCallback {
                override fun onSuccess(screenshotResult: ScreenshotResult) {
                  try {
                    val now = System.currentTimeMillis()
                    if (now - lastScreenshotSuccessTimestamp > 600_000L) {
                      lastScreenshotSuccessTimestamp = now
                      com.example.focus.ProtectionHealthManager.recordLogEntry(
                        context = applicationContext,
                        eventType = "AI_SCREENSHOT_ACTIVE",
                        message = "AI स्क्रीनशॉट सक्रिय: स्क्रीन स्कैन और मॉडल मूल्यांकन जारी है",
                        severity = "INFO"
                      )
                    }

                    val hardwareBuffer = screenshotResult.hardwareBuffer
                    val colorSpace = screenshotResult.colorSpace
                    val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)?.copy(Bitmap.Config.ARGB_8888, false)
                    hardwareBuffer.close()

                    if (bitmap != null) {
                      serviceScope.launch(Dispatchers.Default) {
                        try {
                          android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                          evaluateScreenshotBitmap(bitmap)
                        } finally {
                          bitmap.recycle() // 🛡️ Zero memory leaks
                        }
                      }
                    }
                  } catch (e: Exception) {
                    Log.w("ShortContentService", "Screenshot processing error: ${e.message}")
                  } finally {
                    isScreenshotInFlight = false
                  }
                }

                override fun onFailure(errorCode: Int) {
                  isScreenshotInFlight = false
                  val errorMsg = when (errorCode) {
                    1 -> "ERROR_TAKE_SCREENSHOT_INTERNAL ($errorCode: सिस्टम आंतरिक त्रुटि)"
                    2 -> "ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS ($errorCode: एक्सेसिबिलिटी अनुमति नहीं - Accessibility OFF/ON करें)"
                    3 -> "ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT ($errorCode: जाँच अंतराल बहुत कम)"
                    4 -> "ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY ($errorCode: अमान्य डिस्प्ले)"
                    5 -> "ERROR_TAKE_SCREENSHOT_INVALID_WINDOW ($errorCode: अमान्य विंडो)"
                    else -> "अज्ञात स्क्रीनशॉट त्रुटि ($errorCode)"
                  }
                  Log.w("ShortContentService", "takeScreenshot onFailure: $errorMsg")
                  val now = System.currentTimeMillis()
                  if (now - lastCapabilityWarnTimestamp > 60_000L) {
                    lastCapabilityWarnTimestamp = now
                    com.example.focus.ProtectionHealthManager.recordLogEntry(
                      context = applicationContext,
                      eventType = "AI_SCREENSHOT_FAIL",
                      message = "स्क्रीनशॉट विफलता: $errorMsg",
                      severity = "WARN"
                    )
                  }
                }
              }
            )
          } catch (e: Exception) {
            isScreenshotInFlight = false
            Log.e("ShortContentService", "Exception calling takeScreenshot", e)
          }
        }
      }
    }
  }

  private fun evaluateScreenshotBitmap(bitmap: Bitmap) {
    // 1. Detect FLAG_SECURE blank/black screen
    if (com.example.focus.TfLiteNsfwClassifier.isBlankOrFlagSecure(bitmap)) {
      Log.d("ShortContentService", "Screen capture is blank/solid black (FLAG_SECURE window). Marking as uninspectable and skipping.")
      return
    }

    // 2. Check imported multi-models first if any active
    if (com.example.focus.MultiModelClassifierManager.hasAnyActiveModel(applicationContext)) {
      val decision = com.example.focus.MultiModelClassifierManager.evaluateBitmap(applicationContext, bitmap)
      if (decision.isNsfwTrigger) {
        Log.w("ShortContentService", "MULTI-MODEL ENSEMBLE TRIGGER: Adult visual content confirmed! ${decision.reason}")
        com.example.focus.AiScreenDetectionManager.incrementInterceptionCount(applicationContext)

        // Immediately return to Home screen
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Engage 15-minute Strict Lock
        FocusModeManager.activateStrictLock(
          context = applicationContext,
          durationMinutes = 15,
          reason = "AI स्क्रीन पहचान (मल्टी-मॉडल): ${decision.reason}",
          triggerWord = "Multi-Model Ensemble",
          packageName = "On-Device Screen"
        )

        if (Settings.canDrawOverlays(applicationContext)) {
          FocusBlockingOverlayManager.showStrictLock(applicationContext, "AI Screen Vision", "वयस्क दृश्य पहचाना गया (मल्टी-मॉडल)")
        }
      }
      return
    }

    // 3. Fallback to default classifier if model available
    if (com.example.focus.TfLiteNsfwClassifier.isModelAvailable()) {
      val threshold = com.example.focus.AiScreenDetectionManager.getThreshold(applicationContext)
      val result = com.example.focus.TfLiteNsfwClassifier.classify(bitmap, threshold)

      if (result.isUninspectable) {
        return
      }

      if (result.isNsfw) {
        Log.w("ShortContentService", "AI SCREEN DETECTION TRIGGER: Adult visual content confirmed! ${result.details}")
        com.example.focus.AiScreenDetectionManager.incrementInterceptionCount(applicationContext)

        performGlobalAction(GLOBAL_ACTION_HOME)

        FocusModeManager.activateStrictLock(
          context = applicationContext,
          durationMinutes = 15,
          reason = "AI स्क्रीन पहचान: वयस्क दृश्य (${result.details})",
          triggerWord = "AI Vision Match",
          packageName = "On-Device Screen"
        )

        if (Settings.canDrawOverlays(applicationContext)) {
          FocusBlockingOverlayManager.showStrictLock(applicationContext, "AI Screen Vision", "वयस्क दृश्य पहचाना गया")
        }
      }
    }
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return
    if (powerManager?.isInteractive == false) return // Zero work when screen off

    val packageName = event.packageName?.toString() ?: return

    // 0. NEVER process our own package events (prevents WindowManager / Toast event feedback loops)
    if (packageName == this.packageName) {
      return
    }

    // 0a. PIN FLOW ACTIVE: Never process events while user or friend is entering Accountability PIN
    if (com.example.focus.AccountabilityPinManager.isPinFlowActive.value) {
      return
    }

    // 0b. SINGLE-SHOT STATE MACHINE: Ignore ALL events during SHOWING and 1.5s COOLDOWN
    if (FocusBlockingOverlayManager.isInBlockingOrCooldown()) {
      return
    }

    // Skip non-actionable input methods & system keyboard to eliminate typing lag
    if (packageName.startsWith("com.google.android.inputmethod") ||
        packageName.contains("keyboard", ignoreCase = true)) {
      return
    }

    // MULTI-USER & GUEST PROFILE BYPASS CHECK FOR SYSTEMUI (Quick Settings tile / Lock-screen switcher)
    if (packageName == "com.android.systemui") {
      val now = System.currentTimeMillis()
      // Throttle SystemUI checks to 300ms to eliminate UI stutter
      if (now - lastMultiUserCheckTimestamp >= 300L) {
        lastMultiUserCheckTimestamp = now
        val eventClassName = event.className?.toString() ?: ""
        val eventTextJoined = event.text?.joinToString(" ") ?: ""
        val contentDesc = event.contentDescription?.toString() ?: ""
        val combinedText = "$eventTextJoined $contentDesc"

        val detection = com.example.focus.MultiUserProtectionManager.evaluateScreenDetection(
          context = this,
          packageName = packageName,
          className = eventClassName,
          visibleText = combinedText
        )
        if (detection.isBlocked) {
          blockMultiUserBypass(packageName, eventClassName, detection)
        }
      }
      return
    }

    val isOwnApp = packageName == this.packageName
    if (isOwnApp) {
      FocusBlockingOverlayManager.dismiss()
      return
    }

    // Dismiss overlay if user returns to Home Launcher
    if (packageName.contains("launcher", ignoreCase = true)) {
      FocusBlockingOverlayManager.dismiss()
    }

    // 00. ALWAYS-ON SECURITY & UNINSTALL PROTECTION (हमेशा-चालू सुरक्षा - App Info, Uninstall Dialog, Accessibility Toggle, Device Admin)
    // Instantly executes BACK and HOME (< 0.1s) whenever Peewee Block is targeted for uninstall or disabling.
    val eventClassName = event.className?.toString() ?: ""
    val isSettingsPkg = packageName == "com.android.settings" || packageName == "com.google.android.settings"

    // Check 1: Multi-user / Guest / Second Space / Dual apps bypass screens
    var shouldCheckMultiUser = true
    if (isSettingsPkg && event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      val now = System.currentTimeMillis()
      if (now - lastSettingsContentChangeCheckTimestamp < 300L) {
        shouldCheckMultiUser = false
      } else {
        lastSettingsContentChangeCheckTimestamp = now
      }
    }

    if (shouldCheckMultiUser && isSettingsPkg) {
      val eventTextJoined = event.text?.joinToString(" ") ?: ""
      val contentDesc = event.contentDescription?.toString() ?: ""
      val hierarchyText = extractAllNodeTexts(rootInActiveWindow ?: event.source, maxDepth = 5, maxNodes = 40)
      val combinedText = "$eventTextJoined $contentDesc $hierarchyText"

      val detection = com.example.focus.MultiUserProtectionManager.evaluateScreenDetection(
        context = this,
        packageName = packageName,
        className = eventClassName,
        visibleText = combinedText
      )

      if (detection.isBlocked) {
        blockMultiUserBypass(packageName, eventClassName, detection)
        return
      }
    }

    // Check 2: Peewee Block Tamper & Uninstall Protection (Accessibility toggle, Device Admin deactivate, App Info, Uninstall)
    // Only triggers when Peewee Block's identity is on screen. Settings main page and other apps are NEVER blocked!
    val eventTextJoined = event.text?.joinToString(" ") ?: ""
    val contentDescJoined = event.contentDescription?.toString() ?: ""
    val isInstallerOrSettings = isSettingsPkg ||
      packageName.contains("packageinstaller", ignoreCase = true) ||
      packageName.contains("installer", ignoreCase = true) ||
      packageName == "android" ||
      packageName.contains("launcher", ignoreCase = true)
    val hierarchyForTamper = if (isInstallerOrSettings) {
      extractAllNodeTexts(rootInActiveWindow ?: event.source, maxDepth = 6, maxNodes = 50)
    } else ""
    val fullTamperText = "$eventTextJoined $contentDescJoined $hierarchyForTamper"

    val tamperResult = com.example.focus.AlwaysOnSecurityManager.evaluateTamperScreen(
      context = this,
      packageName = packageName,
      className = eventClassName,
      visibleText = fullTamperText
    )

    if (tamperResult.isTamper) {
      Log.w("ShortContentService", "PEEWEE BLOCK TAMPER INTERCEPTED (< 0.1s): ${tamperResult.reason} ($packageName / $eventClassName)!")

      // 1. Immediately dismiss popup dialog and drop to Home screen in < 0.1s
      performGlobalAction(GLOBAL_ACTION_BACK)
      performGlobalAction(GLOBAL_ACTION_HOME)

      // 2. Launch Peewee Block home screen to show protected status
      try {
        val homeIntent = android.content.Intent(applicationContext, com.example.MainActivity::class.java).apply {
          flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
            android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
          putExtra("INTERCEPTED_TAMPER_ATTEMPT", true)
          putExtra("TAMPER_REASON", tamperResult.reason)
        }
        startActivity(homeIntent)
      } catch (_: Exception) {}

      // 3. Debounce notification and toast only (so actions ALWAYS execute, but notifications don't flood)
      val now = System.currentTimeMillis()
      if (now - lastTamperBlockedTimestamp > 1200L) {
        lastTamperBlockedTimestamp = now

        android.os.Handler(android.os.Looper.getMainLooper()).post {
          android.widget.Toast.makeText(applicationContext, "Peewee Block: Protection is active. Settings modification is locked.", android.widget.Toast.LENGTH_SHORT).show()
        }

        com.example.focus.ProtectionHealthManager.logHealthEvent(
          context = this,
          eventType = "TAMPER_ATTEMPT_BLOCKED",
          message = "${tamperResult.reason} ($packageName / $eventClassName)",
          severity = "WARN"
        )

        com.example.focus.StrictLockWatchdog.postAlertNotification(
          context = this,
          title = "🛡️ Peewee Block Security Alert",
          message = "${tamperResult.reason} was blocked.",
          intentAction = android.provider.Settings.ACTION_SETTINGS
        )
      }
      return
    }

    // 0A. CRITICAL: STRICT LOCK & INCOGNITO EVASION DETECTION (Incognito / Private Tabs / Tab Switchers in all browsers)
    val isStrict = FocusModeManager.isStrictLockActive(this)
    val isFocus = FocusModeManager.isFocusModeActive(this)
    val isIncognitoBlockingActive = com.example.focus.BrowserUrlExtractor.isIncognitoBlockingEnabled(this)

    if (isStrict || isIncognitoBlockingActive) {
      val eventTextStr = event.text?.joinToString(" ") ?: ""
      val contentDescStr = event.contentDescription?.toString() ?: ""
      if (com.example.focus.BrowserUrlExtractor.isIncognitoOrTabAction(
          packageName = packageName,
          eventText = eventTextStr,
          contentDescription = contentDescStr,
          node = rootInActiveWindow ?: event.source
        )) {
        Log.w("ShortContentService", "INCOGNITO / TAB SWITCHER BLOCKED in browser $packageName!")
        com.example.focus.PeeweeStatsManager.recordAttempt(this, successfullyBlocked = true, trigger = "Incognito Tab")
        // Immediately kick user back out using BACK and HOME
        performGlobalAction(GLOBAL_ACTION_BACK)
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (Settings.canDrawOverlays(this)) {
          FocusBlockingOverlayManager.showStrictLock(this, packageName, "Incognito / Private Tab Blocked")
        }
        return
      }
    }

    // 0. STRICT LOCK: INSTANT ON-DEVICE ADULT CONTENT DETECTION (Never inspect our own app or system UI)
    val isSystemOrOwnApp = isOwnApp || packageName == "com.android.systemui" || packageName == "android" || packageName.contains("launcher", ignoreCase = true)
    if (!isSystemOrOwnApp) {
      val adultResult = com.example.focus.AdultContentDetector.inspectEvent(this, event, rootInActiveWindow)
      if (adultResult != null && adultResult.isAdult) {
        Log.w("ShortContentService", "ADULT CONTENT DETECTED: matched '${adultResult.matchedTerm}' in package $packageName! Activating 15-minute Strict Lock.")

        // Record statistics in PeeweeStatsManager
        com.example.focus.PeeweeStatsManager.recordAttempt(this, successfullyBlocked = true, trigger = adultResult.matchedTerm)
        com.example.focus.PeeweeStatsManager.recordStrictLockActivation(this)

        // Immediately kick user out of adult content to Home screen
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Activate unbreakable 15-minute Strict Lock Focus Mode
        FocusModeManager.activateStrictLock(
          context = this,
          durationMinutes = 15,
          reason = "वयस्क सामग्री अवरोधित: ${adultResult.matchedTerm}",
          triggerWord = adultResult.matchedTerm,
          packageName = packageName
        )

        // Immediately display unbreakable strict blocking overlay if permitted
        if (Settings.canDrawOverlays(this)) {
          FocusBlockingOverlayManager.showStrictLock(this, packageName, adultResult.matchedTerm)
        }
        return
      }
    }

    // 1. In Peewee Block, Strict Lock secures protection settings and website blocking.
    // It intentionally allows normal apps (Chrome, productivity apps, social apps) to run freely,
    // intercepting ONLY explicit adult content and tamper attempts directed at Peewee Block.
    FocusBlockingOverlayManager.dismiss()

    // 2. SHORT CONTENT VIDEO TRACKING & BLOCKING (Instagram, YouTube, Facebook)
    val supportedPackages = ShortAppRegistry.getAllSupportedPackages()
    val now = System.currentTimeMillis()

    // If user navigated away from supported short-video apps
    if (!supportedPackages.contains(packageName)) {
      if (lastCheckedPackage != null && supportedPackages.contains(lastCheckedPackage)) {
        trackerManager.onExitShortVideoScreen(now)
      }
      lastCheckedPackage = packageName
      return
    }

    lastCheckedPackage = packageName

    // Debounce high-frequency content changed events (within 80ms)
    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
      if (now - lastContentChangedTimestamp < 80) {
        return
      }
      lastContentChangedTimestamp = now
    }

    val detector = ShortAppRegistry.getDetectorForPackage(packageName) ?: return

    try {
      // Find the best root node: rootInActiveWindow, climbed event.source, or event.source itself
      var rootNode: AccessibilityNodeInfo? = rootInActiveWindow
      if (rootNode == null || rootNode.packageName?.toString() != packageName) {
        var src = event.source
        var climbDepth = 0
        while (src != null && src.parent != null && climbDepth < 10) {
          src = src.parent
          climbDepth++
        }
        if (src != null && src.packageName?.toString() == packageName) {
          rootNode = src
        } else if (event.source?.packageName?.toString() == packageName) {
          rootNode = event.source
        }
      }

      val detectionResult = detector.detect(
        rootNode = rootNode ?: event.source,
        eventPackage = event.packageName,
        eventClassName = event.className
      )

      if (detectionResult.confidence == DetectionConfidence.HIGH) {
        // If not blocked, dismiss any old overlay and track current video
        FocusBlockingOverlayManager.dismiss()

        val signature = detector.extractVideoSignature(rootNode, event.packageName)
        if (signature != null) {
          trackerManager.onShortVideoDetected(packageName, signature, now)
        } else {
          trackerManager.checkInFlightVideoCommit(now)
        }
      } else if (detectionResult.confidence == DetectionConfidence.MEDIUM) {
        Log.d("ShortContentService", "Medium confidence short content in $packageName (monitored, not blocked): ${detectionResult.matchedSignals}")
      } else {
        trackerManager.onExitShortVideoScreen(now)
      }
    } catch (e: Exception) {
      Log.w("ShortContentService", "Safe fallback on UI structure event: ${e.message}")
    }
  }

  override fun onInterrupt() {
    // Safe no-op
  }

  override fun onDestroy() {
    super.onDestroy()
    heartbeatJob?.cancel()
    aiScreenshotJob?.cancel()
    if (instanceRef?.get() == this) {
      instanceRef = null
    }
    FocusBlockingOverlayManager.dismiss()
    com.example.focus.ProtectionHealthManager.logHealthEvent(
      context = applicationContext,
      eventType = "SERVICE_STOPPED",
      message = "Accessibility Service रोकी गई (onDestroy)।",
      severity = "WARN"
    )
    try {
      trackerManager.notifyServiceConnected(false)
      trackerManager.onExitShortVideoScreen()
    } catch (_: Exception) {}
  }
}
