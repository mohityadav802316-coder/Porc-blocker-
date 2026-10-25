package com.example.focus

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.blackscreen.BlackScreenManager
import com.example.shortcontent.ShortContentAccessibilityService

/**
 * Production-ready WindowManager overlay manager.
 * Features:
 * 1) Single-Shot State Machine (IDLE -> SHOWING -> COOLDOWN) for Strict Lock blocking.
 *    Eliminates all double-blink, flicker, and accessibility re-triggering loops.
 * 2) Reusable view hierarchy to prevent GC churn and WindowManager add/remove race conditions.
 * 3) Tranquil, premium dark design (slate/obsidian #121216) with 150ms fade-in, 900ms hold,
 *    GLOBAL_ACTION_HOME, and 150ms fade-out.
 * 4) Preserves standard focus mode and daily limit overlay support.
 */
object FocusBlockingOverlayManager {

  private const val TAG = "FocusBlockingOverlay"

  enum class BlockerState {
    IDLE,
    SHOWING,
    COOLDOWN
  }

  @Volatile
  private var blockerState: BlockerState = BlockerState.IDLE

  private var overlayView: FrameLayout? = null
  private var currentBlockedPackage: String? = null
  private val mainHandler = Handler(Looper.getMainLooper())

  // Reusable Strict Lock View Elements
  private var strictCardView: LinearLayout? = null
  private var strictIconView: ImageView? = null
  private var strictTitleView: TextView? = null
  private var strictTimerView: TextView? = null
  private var strictNoticeView: TextView? = null

  private var countdownTextView: TextView? = null

  private val timerTicker = object : Runnable {
    override fun run() {
      updateRemainingTime()
      if (overlayView != null) {
        mainHandler.postDelayed(this, 1000L)
      }
    }
  }

  /**
   * Returns true if Strict Lock single-shot blocker is either currently SHOWING
   * or in the 1.5-second COOLDOWN window.
   * During this time, ALL incoming accessibility events must be ignored.
   */
  fun isInBlockingOrCooldown(): Boolean {
    return blockerState != BlockerState.IDLE
  }

  /**
   * Single-Shot Strict Lock Blocker:
   * 1. Displays a clean, calm obsidian card with 150ms fade-in.
   * 2. Holds on screen for ~900ms without flicker or double-flash.
   * 3. Dispatches GLOBAL_ACTION_HOME via [onSendHome] at 900ms.
   * 4. Fades out in 150ms and transitions to COOLDOWN (ignoring all events for 1.5s total).
   */
  @SuppressLint("SetTextI18n")
  fun showStrictLockSingleShot(
    context: Context,
    blockedPackage: String,
    onSendHome: () -> Unit
  ) {
    mainHandler.post {
      // If already showing or in cooldown, discard immediately
      if (blockerState != BlockerState.IDLE) {
        Log.d(TAG, "Strict Lock blocker in state $blockerState; skipping duplicate trigger for $blockedPackage")
        return@post
      }

      // If OLED Black Screen is active, keep screen dark and do not clash overlays
      if (BlackScreenManager.isOverlayActive.value) {
        onSendHome()
        return@post
      }

      val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: run {
        onSendHome()
        return@post
      }

      // Transition to SHOWING
      blockerState = BlockerState.SHOWING
      currentBlockedPackage = blockedPackage

      // Temporarily hide the small floating pill while full blocking screen is shown
      StrictLockFloatingPillManager.setVisibility(false)

      val density = context.resources.displayMetrics.density
      fun dp(value: Float): Int = (value * density + 0.5f).toInt()

      val pm = context.packageManager
      val appLabel = try {
        pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, 0)).toString()
      } catch (_: Exception) {
        blockedPackage
      }

      val remainingMillis = FocusModeManager.getStrictLockRemainingMillis(context)
      val totalSecs = (remainingMillis / 1000L).coerceAtLeast(0)
      val mins = totalSecs / 60
      val secs = totalSecs % 60
      val timeFormatted = String.format("%02d:%02d", mins, secs)

      // Ensure reusable view hierarchy is built
      ensureStrictLockView(context, density, dp(24f))

      // Update text and icon
      strictTitleView?.text = "सख्त लॉक सक्रिय"
      strictTimerView?.text = "⏳ $timeFormatted शेष"
      strictNoticeView?.text = "15 मिनट का डिजिटल लॉक सक्रिय है। “$appLabel” प्रतिबंधित है। मुख्य स्क्रीन पर भेजा जा रहा है..."

      try {
        val appInfo = pm.getApplicationInfo(blockedPackage, 0)
        strictIconView?.setImageDrawable(pm.getApplicationIcon(appInfo))
      } catch (_: Exception) {
        strictIconView?.setImageResource(android.R.drawable.ic_lock_lock)
      }

      val root = overlayView ?: return@post

      val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

      val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        layoutFlag,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
          WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.CENTER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }

      try {
        if (root.parent == null) {
          root.alpha = 0f
          wm.addView(root, params)
        }
        root.visibility = View.VISIBLE
        // Smooth 150ms fade-in
        root.animate()
          .alpha(1f)
          .setDuration(150L)
          .start()
        Log.d(TAG, "Single-shot strict lock overlay showing for $blockedPackage")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to attach strict lock overlay", e)
      }

      // Step 2: Hold for 900ms, then dispatch HOME and fade out
      mainHandler.postDelayed({
        // Dispatch HOME action
        try {
          onSendHome()
        } catch (e: Exception) {
          Log.w(TAG, "Failed to trigger home action: ${e.message}")
        }

        // Fade out in 150ms
        root.animate()
          .alpha(0f)
          .setDuration(150L)
          .withEndAction {
            dismissInternal(wm)
            StrictLockFloatingPillManager.setVisibility(true)
          }
          .start()

        // Transition to COOLDOWN state
        blockerState = BlockerState.COOLDOWN
        Log.d(TAG, "Strict Lock blocker transitioned to COOLDOWN")
      }, 900L)

      // Step 3: Complete 1500ms cooldown and return to IDLE
      mainHandler.postDelayed({
        blockerState = BlockerState.IDLE
        Log.d(TAG, "Strict Lock blocker returned to IDLE")
      }, 1500L)
    }
  }

  /**
   * Builds or caches the clean, tranquil dark UI for Strict Lock single-shot blocking.
   */
  private fun ensureStrictLockView(context: Context, density: Float, cornerRadius: Int) {
    if (overlayView != null) return

    fun dp(v: Float) = (v * density + 0.5f).toInt()

    val root = FrameLayout(context.applicationContext).apply {
      setBackgroundColor(Color.parseColor("#F5121216")) // 96% obsidian dark
      isFocusable = true
      isFocusableInTouchMode = true
      isClickable = true
    }

    // Center Card (Muted charcoal #1C1C24 with calm slate #334155 border)
    val card = LinearLayout(context.applicationContext).apply {
      orientation = LinearLayout.VERTICAL
      gravity = Gravity.CENTER_HORIZONTAL
      val cardBg = GradientDrawable().apply {
        setColor(Color.parseColor("#1C1C24"))
        this.cornerRadius = dp(24f).toFloat()
        setStroke(dp(1.2f), Color.parseColor("#334155"))
      }
      background = cardBg
      setPadding(dp(24f), dp(28f), dp(24f), dp(24f))
    }

    val cardParams = FrameLayout.LayoutParams(
      dp(330f).coerceAtMost((context.resources.displayMetrics.widthPixels * 0.90f).toInt()),
      FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
      gravity = Gravity.CENTER
    }

    // App Icon
    val iconView = ImageView(context.applicationContext).apply {
      setImageResource(android.R.drawable.ic_lock_lock)
      val iconSize = dp(52f)
      layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
        bottomMargin = dp(14f)
      }
    }
    strictIconView = iconView
    card.addView(iconView)

    // Title (Calm slate-white, bold, no screaming red)
    val titleView = TextView(context.applicationContext).apply {
      text = "सख्त लॉक सक्रिय"
      setTextColor(Color.parseColor("#F8FAFC"))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
      typeface = android.graphics.Typeface.DEFAULT_BOLD
      gravity = Gravity.CENTER
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        bottomMargin = dp(8f)
      }
    }
    strictTitleView = titleView
    card.addView(titleView)

    // Countdown Pill (Soft Cyan in dark pill)
    val timerPill = LinearLayout(context.applicationContext).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER
      val pillBg = GradientDrawable().apply {
        setColor(Color.parseColor("#0F172A"))
        this.cornerRadius = dp(12f).toFloat()
        setStroke(dp(1f), Color.parseColor("#1E293B"))
      }
      background = pillBg
      setPadding(dp(16f), dp(6f), dp(16f), dp(6f))
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        bottomMargin = dp(16f)
      }
    }

    val timerText = TextView(context.applicationContext).apply {
      text = "⏳ 15:00 शेष"
      setTextColor(Color.parseColor("#38BDF8")) // Sky blue
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
      typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    strictTimerView = timerText
    timerPill.addView(timerText)
    card.addView(timerPill)

    // Notice message
    val noticeView = TextView(context.applicationContext).apply {
      text = "15 मिनट का डिजिटल लॉक सक्रिय है। मुख्य स्क्रीन पर भेजा जा रहा है..."
      setTextColor(Color.parseColor("#94A3B8")) // Slate 400
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
      gravity = Gravity.CENTER
      setLineSpacing(dp(2f).toFloat(), 1.0f)
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )
    }
    strictNoticeView = noticeView
    card.addView(noticeView)

    root.addView(card, cardParams)

    overlayView = root
    strictCardView = card
  }

  /**
   * Displays the standard blocking overlay (used for Focus Mode sessions and Short Video limits).
   */
  @SuppressLint("SetTextI18n")
  fun show(
    context: Context,
    blockedPackage: String,
    customTitle: String? = null,
    customNotice: String? = null,
    isShortContentLimit: Boolean = false,
    isStrictLock: Boolean = false
  ) {
    if (isStrictLock || FocusModeManager.isStrictLockActive(context)) {
      showStrictLockSingleShot(context, blockedPackage) {
        ShortContentAccessibilityService.performGlobalHome()
      }
      return
    }

    mainHandler.post {
      if (BlackScreenManager.isOverlayActive.value) {
        return@post
      }

      if (overlayView != null && currentBlockedPackage == blockedPackage) {
        if (!isShortContentLimit) {
          updateRemainingTime()
        }
        return@post
      }

      val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

      dismissInternal(wm)

      currentBlockedPackage = blockedPackage

      val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

      val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        layoutFlag,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
          WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.CENTER
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }

      val density = context.resources.displayMetrics.density
      fun dp(value: Float): Int = (value * density + 0.5f).toInt()

      val root = FrameLayout(context.applicationContext).apply {
        setBackgroundColor(Color.parseColor("#F5121214"))
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
      }

      val card = LinearLayout(context.applicationContext).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        val cardBg = GradientDrawable().apply {
          setColor(Color.parseColor("#1C1C22"))
          cornerRadius = dp(24f).toFloat()
          val strokeColor = Color.parseColor("#B39855")
          setStroke(dp(1.4f), strokeColor)
        }
        background = cardBg
        setPadding(dp(24f), dp(28f), dp(24f), dp(24f))
      }

      val cardParams = FrameLayout.LayoutParams(
        dp(340f).coerceAtMost((context.resources.displayMetrics.widthPixels * 0.90f).toInt()),
        FrameLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        gravity = Gravity.CENTER
      }

      val iconView = ImageView(context.applicationContext).apply {
        val pm = context.packageManager
        try {
          val appInfo = pm.getApplicationInfo(blockedPackage, 0)
          setImageDrawable(pm.getApplicationIcon(appInfo))
        } catch (_: Exception) {
          setImageResource(android.R.drawable.ic_lock_lock)
        }
        val iconSize = dp(56f)
        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
          bottomMargin = dp(16f)
        }
      }
      card.addView(iconView)

      val titleView = TextView(context.applicationContext).apply {
        text = customTitle ?: "🎯 Focus Mode सक्रिय"
        val titleColor = if (isShortContentLimit) "#FF5252" else "#F2C94C"
        setTextColor(Color.parseColor(titleColor))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = dp(6f)
        }
      }
      card.addView(titleView)

      val pm = context.packageManager
      val appLabel = try {
        pm.getApplicationLabel(pm.getApplicationInfo(blockedPackage, 0)).toString()
      } catch (_: Exception) {
        blockedPackage
      }

      val noticeView = TextView(context.applicationContext).apply {
        text = customNotice ?: "“$appLabel” वर्तमान सत्र में प्रतिबंधित है। अपने मुख्य लक्ष्य पर ध्यान केंद्रित रखें।"
        setTextColor(Color.parseColor("#E0E0E0"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        gravity = Gravity.CENTER
        setLineSpacing(dp(3f).toFloat(), 1.0f)
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = dp(18f)
        }
      }
      card.addView(noticeView)

      val timerPill = LinearLayout(context.applicationContext).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        val pillBg = GradientDrawable().apply {
          setColor(Color.parseColor("#282832"))
          cornerRadius = dp(12f).toFloat()
        }
        background = pillBg
        setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          bottomMargin = dp(24f)
        }
        if (isShortContentLimit) {
          visibility = View.GONE
        }
      }

      val timerText = TextView(context.applicationContext).apply {
        text = "शेष समय: गणना हो रही है..."
        setTextColor(Color.parseColor("#00E5FF"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
      }
      countdownTextView = timerText
      timerPill.addView(timerText)
      card.addView(timerPill)

      val homeButton = Button(context.applicationContext).apply {
        text = "मुख्य स्क्रीन (Go Home)"
        setTextColor(Color.parseColor("#121214"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        val btnBg = GradientDrawable().apply {
          setColor(Color.parseColor("#D4AF37"))
          cornerRadius = dp(14f).toFloat()
        }
        background = btnBg
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          dp(48f)
        ).apply {
          bottomMargin = dp(10f)
        }
        setOnClickListener {
          dismiss()
          val handled = ShortContentAccessibilityService.performGlobalHome()
          if (!handled) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
              addCategory(Intent.CATEGORY_HOME)
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
          }
        }
      }
      card.addView(homeButton)

      val appButton = Button(context.applicationContext).apply {
        text = "Peewee Block खोलें (Open Peewee Block)"
        setTextColor(Color.parseColor("#E0E0E0"))
        isEnabled = true
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        val btnBg = GradientDrawable().apply {
          setColor(Color.TRANSPARENT)
          cornerRadius = dp(14f).toFloat()
          setStroke(dp(1f), Color.parseColor("#444455"))
        }
        background = btnBg
        layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          dp(44f)
        )
        setOnClickListener {
          dismiss()
          val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          }
          context.startActivity(openIntent)
        }
      }
      card.addView(appButton)

      root.addView(card, cardParams)

      try {
        wm.addView(root, params)
        overlayView = root
        mainHandler.post(timerTicker)
        Log.d(TAG, "Displayed focus blocking overlay for: $blockedPackage")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to display focus blocking overlay", e)
      }
    }
  }

  fun showStrictLock(
    context: Context,
    blockedPackage: String,
    matchedTerm: String? = null
  ) {
    showStrictLockSingleShot(context, blockedPackage) {
      ShortContentAccessibilityService.performGlobalHome()
    }
  }

  private fun updateRemainingTime() {
    val timerView = countdownTextView ?: return
    val context = timerView.context ?: return
    val remainingMillis = FocusModeManager.getRemainingSessionMillis(context)

    if (remainingMillis <= 0L) {
      timerView.text = "सत्र समाप्त हो रहा है... ताला खुल रहा है"
      FocusModeManager.deactivateFocusMode(context, force = true)
      dismiss()
      return
    }

    val totalSecs = remainingMillis / 1000L
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val hrs = mins / 60
    val remMins = mins % 60

    timerView.text = if (hrs > 0) {
      String.format("शेष समय: %d घंटा %02d मिनट %02d सेकंड", hrs, remMins, secs)
    } else {
      String.format("शेष समय: %02d मिनट %02d सेकंड", mins, secs)
    }
  }

  /**
   * Dismisses and removes the blocking overlay view cleanly.
   */
  fun dismiss() {
    mainHandler.post {
      val view = overlayView ?: return@post
      val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      if (wm != null) {
        dismissInternal(wm)
      }
    }
  }

  private fun dismissInternal(wm: WindowManager) {
    mainHandler.removeCallbacks(timerTicker)
    overlayView?.let {
      try {
        if (it.parent != null) {
          wm.removeView(it)
        }
        Log.d(TAG, "Dismissed focus blocking overlay")
      } catch (e: Exception) {
        Log.e(TAG, "Error removing focus blocking overlay", e)
      }
    }
    overlayView = null
    currentBlockedPackage = null
    countdownTextView = null
    strictCardView = null
    strictIconView = null
    strictTitleView = null
    strictTimerView = null
    strictNoticeView = null
  }
}
