package com.example.blackscreen

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Production-ready foreground service for System-Wide OLED Black Screen Mode and Floating Control.
 */
class BlackScreenService : Service() {

  companion object {
    private const val TAG = "BlackScreenService"
  }

  private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
  private lateinit var windowManager: WindowManager

  private var floatingDotView: View? = null
  private var floatingDotParams: WindowManager.LayoutParams? = null

  private var blackOverlayView: FrameLayout? = null
  private var blackOverlayParams: WindowManager.LayoutParams? = null
  private var clockTextView: TextView? = null

  private var currentSettings: UserSettingsEntity? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  private var gestureDetector: BlackScreenGestureDetector? = null

  // Clock update runnable
  private val clockUpdateRunnable = object : Runnable {
    override fun run() {
      updateClockDisplay()
      mainHandler.postDelayed(this, 15000L) // Refresh every 15s for exact minute alignment
    }
  }

  // Broadcast receiver for Screen Lock / Unlock events
  private val screenEventsReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.action ?: return
      Log.d(TAG, "Screen event received: $action")
      when (action) {
        Intent.ACTION_USER_PRESENT -> {
          // Device was unlocked by user
          if (currentSettings?.isBlackScreenEnabled == true &&
            currentSettings?.isBlackScreenOverlayActive == true &&
            currentSettings?.blackScreenRestoreAfterUnlock == true
          ) {
            showBlackOverlay()
          }
        }
        Intent.ACTION_SCREEN_OFF -> {
          // Screen turned off by power button; leave state as is for restoration on unlock
          Log.d(TAG, "Screen turned off. Maintaining state.")
        }
      }
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "BlackScreenService onCreate")
    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    BlackScreenManager.createNotificationChannel(this)

    // Register screen lock/unlock broadcast receiver
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_USER_PRESENT)
      addAction(Intent.ACTION_SCREEN_OFF)
      addAction(Intent.ACTION_SCREEN_ON)
    }
    ContextCompat.registerReceiver(this, screenEventsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

    // Start listening to UserSettings updates from Room DB
    serviceScope.launch {
      val db = LifeTrackerDatabase.getDatabase(applicationContext)
      db.userSettingsDao().getSettings().collectLatest { settings ->
        if (settings != null) {
          currentSettings = settings
          applySettings(settings)
        }
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: BlackScreenManager.ACTION_SHOW_FLOATING_DOT
    Log.d(TAG, "onStartCommand action: $action")

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          BlackScreenManager.NOTIFICATION_ID,
          buildForegroundNotification(isOverlayShowing = blackOverlayView != null),
          android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
      } else {
        startForeground(
          BlackScreenManager.NOTIFICATION_ID,
          buildForegroundNotification(isOverlayShowing = blackOverlayView != null)
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "startForeground failed", e)
    }

    when (action) {
      BlackScreenManager.ACTION_ACTIVATE_BLACK_OVERLAY -> {
        showBlackOverlay()
      }
      BlackScreenManager.ACTION_DEACTIVATE_BLACK_OVERLAY -> {
        hideBlackOverlay()
      }
      BlackScreenManager.ACTION_SHOW_FLOATING_DOT -> {
        showFloatingDot()
      }
      BlackScreenManager.ACTION_HIDE_FLOATING_DOT -> {
        hideFloatingDot()
      }
      BlackScreenManager.ACTION_STOP_SERVICE -> {
        stopSelf()
      }
      BlackScreenManager.ACTION_UPDATE_CONFIG -> {
        currentSettings?.let { applySettings(it) }
      }
    }

    return START_STICKY
  }

  private fun applySettings(settings: UserSettingsEntity) {
    if (!BlackScreenManager.canDrawOverlays(this)) {
      Log.w(TAG, "Cannot draw overlays; overlay permission missing")
      hideFloatingDot()
      hideBlackOverlay()
      return
    }

    // Update gesture detector with user preferences
    val gestureType = BlackScreenGestureType.fromId(settings.blackScreenExitGesture)
    gestureDetector = BlackScreenGestureDetector(
      targetGesture = gestureType,
      sensitivity = settings.blackScreenGestureSensitivity,
      onGestureRecognized = {
        onExitGestureTriggered()
      }
    )

    if (settings.isBlackScreenEnabled) {
      if (settings.isBlackScreenOverlayActive) {
        showBlackOverlay()
      } else {
        hideBlackOverlay()
        if (settings.isFloatingDotEnabled) {
          showFloatingDot()
        } else {
          hideFloatingDot()
        }
      }
    } else {
      hideBlackOverlay()
      hideFloatingDot()
      stopSelf()
    }
  }

  // =========================================================================
  // FLOATING CONTROL DOT
  // =========================================================================

  @SuppressLint("ClickableViewAccessibility")
  private fun showFloatingDot() {
    if (BlackScreenManager.isTemporarilySuppressed.value) return
    if (!BlackScreenManager.canDrawOverlays(this)) return
    if (floatingDotView != null) {
      updateFloatingDotAppearance()
      return
    }

    val settings = currentSettings
    val dotSizeDp = settings?.blackScreenDotSize ?: 36
    val dotSizePx = dpToPx(dotSizeDp.toFloat())
    val opacity = settings?.blackScreenDotOpacity ?: 0.45f
    val initialX = settings?.blackScreenDotX ?: 80
    val initialY = settings?.blackScreenDotY ?: 260

    val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val params = WindowManager.LayoutParams(
      dotSizePx,
      dotSizePx,
      layoutFlag,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = initialX
      y = initialY
    }

    floatingDotParams = params

    // Create custom styled dot view
    val dot = View(this).apply {
      val drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(Color.argb((opacity * 255).toInt(), 0, 229, 255)) // Subtle Cyan Glow
        setStroke(dpToPx(1.5f), Color.argb((opacity * 255).toInt(), 255, 255, 255))
      }
      background = drawable
      elevation = dpToPx(6f).toFloat()
    }

    // Touch and drag listener with tap/double tap/long press detection
    var initialTouchX = 0f
    var initialTouchY = 0f
    var initialParamX = 0
    var initialParamY = 0
    var touchDownTime = 0L
    var lastTapTime = 0L
    var isDragging = false

    val longPressHandler = Handler(Looper.getMainLooper())
    var longPressTriggered = false

    val longPressRunnable = Runnable {
      if (!isDragging) {
        longPressTriggered = true
        performHapticFeedback()
        val activationMethod = currentSettings?.blackScreenActivationMethod ?: "DOUBLE_TAP"
        if (activationMethod == "LONG_PRESS") {
          showBlackOverlay()
        }
      }
    }

    dot.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          touchDownTime = System.currentTimeMillis()
          initialTouchX = event.rawX
          initialTouchY = event.rawY
          initialParamX = params.x
          initialParamY = params.y
          isDragging = false
          longPressTriggered = false
          longPressHandler.postDelayed(longPressRunnable, 600L)
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = event.rawX - initialTouchX
          val dy = event.rawY - initialTouchY
          if (hypot(dx, dy) > 18f) {
            isDragging = true
            longPressHandler.removeCallbacks(longPressRunnable)
            params.x = (initialParamX + dx).toInt()
            params.y = (initialParamY + dy).toInt()
            try {
              windowManager.updateViewLayout(dot, params)
            } catch (e: Exception) {
              Log.e(TAG, "Error updating floating dot layout", e)
            }
          }
          true
        }
        MotionEvent.ACTION_UP -> {
          longPressHandler.removeCallbacks(longPressRunnable)
          val duration = System.currentTimeMillis() - touchDownTime

          if (isDragging) {
            // Drag completed -> persist position
            BlackScreenManager.saveDotPosition(applicationContext, params.x, params.y)
          } else if (!longPressTriggered && duration < 350L) {
            // Detected click
            val currentTime = System.currentTimeMillis()
            val activationMethod = currentSettings?.blackScreenActivationMethod ?: "DOUBLE_TAP"

            when (activationMethod) {
              "TAP" -> {
                performHapticFeedback()
                showBlackOverlay()
              }
              "DOUBLE_TAP" -> {
                if (currentTime - lastTapTime < 380L) {
                  performHapticFeedback()
                  showBlackOverlay()
                  lastTapTime = 0L
                } else {
                  lastTapTime = currentTime
                }
              }
              else -> {
                // Long press mode -> single click gives gentle haptic indication
                performHapticFeedback()
              }
            }
          }
          true
        }
        MotionEvent.ACTION_CANCEL -> {
          longPressHandler.removeCallbacks(longPressRunnable)
          true
        }
        else -> false
      }
    }

    try {
      windowManager.addView(dot, params)
      floatingDotView = dot
      BlackScreenManager.setFloatingDotShowing(true)
      Log.d(TAG, "Floating dot added successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add floating dot", e)
    }
  }

  private fun updateFloatingDotAppearance() {
    val dot = floatingDotView ?: return
    val params = floatingDotParams ?: return
    val settings = currentSettings ?: return

    val dotSizeDp = settings.blackScreenDotSize
    val dotSizePx = dpToPx(dotSizeDp.toFloat())
    val opacity = settings.blackScreenDotOpacity

    params.width = dotSizePx
    params.height = dotSizePx

    val drawable = GradientDrawable().apply {
      shape = GradientDrawable.OVAL
      setColor(Color.argb((opacity * 255).toInt(), 0, 229, 255))
      setStroke(dpToPx(1.5f), Color.argb((opacity * 255).toInt(), 255, 255, 255))
    }
    dot.background = drawable

    try {
      windowManager.updateViewLayout(dot, params)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to update floating dot appearance", e)
    }
  }

  private fun hideFloatingDot() {
    floatingDotView?.let {
      try {
        windowManager.removeView(it)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to remove floating dot", e)
      }
      floatingDotView = null
      floatingDotParams = null
      BlackScreenManager.setFloatingDotShowing(false)
    }
  }

  // =========================================================================
  // FULL OLED BLACK SCREEN OVERLAY (TRULY EDGE-TO-EDGE WITH DISPLAY CUTOUT)
  // =========================================================================

  private fun getRealDisplayBounds(): Pair<Int, Int> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val metrics = windowManager.currentWindowMetrics
      val bounds = metrics.bounds
      Pair(bounds.width(), bounds.height())
    } else {
      val point = Point()
      @Suppress("DEPRECATION")
      windowManager.defaultDisplay.getRealSize(point)
      Pair(point.x, point.y)
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    val overlay = blackOverlayView
    val params = blackOverlayParams
    if (overlay != null && params != null) {
      val (newWidth, newHeight) = getRealDisplayBounds()
      params.width = newWidth
      params.height = newHeight
      try {
        windowManager.updateViewLayout(overlay, params)
        Log.d(TAG, "Updated black overlay layout after rotation: $newWidth x $newHeight")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to update black overlay on configuration change", e)
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun showBlackOverlay() {
    if (BlackScreenManager.isTemporarilySuppressed.value) return
    if (!BlackScreenManager.canDrawOverlays(this)) return
    if (blackOverlayView != null) return // Already showing

    // Temporarily hide floating dot while full black overlay is up
    hideFloatingDot()

    val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val (realWidth, realHeight) = getRealDisplayBounds()

    val flags = (
      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        WindowManager.LayoutParams.FLAG_FULLSCREEN or
        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
    )

    val params = WindowManager.LayoutParams(
      realWidth,
      realHeight,
      layoutFlag,
      flags,
      PixelFormat.OPAQUE // 100% solid black to let OLED display drivers turn off pixels completely
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = 0
      y = 0

      // Edge-to-edge display cutout handling: covers camera notch, cutout, and status bar
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
      }
    }
    blackOverlayParams = params

    val overlay = FrameLayout(this).apply {
      setBackgroundColor(Color.BLACK) // Pure OLED #000000
      isFocusable = true
      isFocusableInTouchMode = true

      // Immersive sticky fullscreen hiding status and navigation bar
      @Suppress("DEPRECATION")
      systemUiVisibility = (
        View.SYSTEM_UI_FLAG_FULLSCREEN or
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
          View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
          View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
          View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
      )

      // Fully consume system bar and display cutout insets to prevent internal padding
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        setOnApplyWindowInsetsListener { _, _ ->
          windowInsetsController?.let { controller ->
            controller.hide(
              WindowInsets.Type.statusBars() or
                WindowInsets.Type.navigationBars() or
                WindowInsets.Type.displayCutout()
            )
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
          }
          WindowInsets.CONSUMED
        }
      } else {
        @Suppress("DEPRECATION")
        setOnApplyWindowInsetsListener { _, insets ->
          insets.consumeSystemWindowInsets()
        }
      }
    }

    // Optional Minimal Dim Clock
    val clock = TextView(this).apply {
      setTextColor(Color.parseColor("#2C2C2C")) // Very faint subtle gray for OLED preservation
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      val layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        bottomMargin = dpToPx(48f)
      }
      this.layoutParams = layoutParams
      visibility = if (currentSettings?.blackScreenShowClock == true) View.VISIBLE else View.GONE
    }
    clockTextView = clock
    overlay.addView(clock)

    // Touch listener with 3-Finger Gesture recognition
    overlay.setOnTouchListener { _, event ->
      gestureDetector?.onTouchEvent(event) ?: false
    }

    try {
      windowManager.addView(overlay, params)
      blackOverlayView = overlay
      BlackScreenManager.setOverlayActive(true)
      updateClockDisplay()
      mainHandler.post(clockUpdateRunnable)
      updateNotification(isOverlayShowing = true)
      Log.d(TAG, "Edge-to-edge black overlay displayed successfully ($realWidth x $realHeight)")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add black overlay", e)
    }
  }

  private fun updateClockDisplay() {
    val clock = clockTextView ?: return
    val settings = currentSettings ?: return
    if (!settings.blackScreenShowClock) {
      clock.visibility = View.GONE
      return
    }

    clock.visibility = View.VISIBLE
    val pattern = if (settings.blackScreenClockFormat24) "HH:mm" else "h:mm a"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    clock.text = sdf.format(Date())

    // Burn-in protection: shift horizontally by a few pixels based on current minute
    val minute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
    val burnInShiftPx = (minute % 10 - 5) * dpToPx(1.5f)
    clock.translationX = burnInShiftPx.toFloat()
  }

  private fun hideBlackOverlay() {
    mainHandler.removeCallbacks(clockUpdateRunnable)
    blackOverlayView?.let {
      try {
        windowManager.removeView(it)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to remove black overlay", e)
      }
      blackOverlayView = null
      blackOverlayParams = null
      clockTextView = null
      BlackScreenManager.setOverlayActive(false)
      updateNotification(isOverlayShowing = false)
    }

    // Re-show floating dot if enabled
    if (currentSettings?.isFloatingDotEnabled == true) {
      showFloatingDot()
    }
  }

  private fun onExitGestureTriggered() {
    Log.d(TAG, "Exit gesture recognized! Dismissing black screen overlay.")
    performHapticFeedback()
    hideBlackOverlay()
    BlackScreenManager.deactivateBlackOverlay(applicationContext)
  }

  private fun performHapticFeedback() {
    try {
      val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(45L, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(45L)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Vibration failed", e)
    }
  }

  // =========================================================================
  // NOTIFICATION & SYSTEM INTEGRATION
  // =========================================================================

  private fun buildForegroundNotification(isOverlayShowing: Boolean): Notification {
    val openAppIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      this,
      1001,
      openAppIntent,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val exitIntent = Intent(this, BlackScreenService::class.java).apply {
      action = if (isOverlayShowing) {
        BlackScreenManager.ACTION_DEACTIVATE_BLACK_OVERLAY
      } else {
        BlackScreenManager.ACTION_STOP_SERVICE
      }
    }
    val exitPendingIntent = PendingIntent.getService(
      this,
      1002,
      exitIntent,
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val title = if (isOverlayShowing) {
      "OLED Black Screen Active"
    } else {
      "Life Tracker Floating Control Active"
    }

    val content = if (isOverlayShowing) {
      "Pure black screen active • Audio playing in background"
    } else {
      "Floating dot ready on screen • Tap to turn display black"
    }

    val actionTitle = if (isOverlayShowing) "Exit Black Screen" else "Turn Off Control Dot"

    return NotificationCompat.Builder(this, BlackScreenManager.CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(content)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setOngoing(true)
      .setContentIntent(openPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, actionTitle, exitPendingIntent)
      .build()
  }

  private fun updateNotification(isOverlayShowing: Boolean) {
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
    notificationManager?.notify(
      BlackScreenManager.NOTIFICATION_ID,
      buildForegroundNotification(isOverlayShowing)
    )
  }

  private fun dpToPx(dp: Float): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
  }

  override fun onDestroy() {
    super.onDestroy()
    Log.d(TAG, "BlackScreenService onDestroy")
    serviceScope.cancel()
    mainHandler.removeCallbacks(clockUpdateRunnable)
    try {
      unregisterReceiver(screenEventsReceiver)
    } catch (_: Exception) {}
    hideBlackOverlay()
    hideFloatingDot()
  }
}
