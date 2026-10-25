package com.example.focus

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Lightweight floating countdown pill for Strict Lock.
 * Displays "🔒 MM:SS" just below the top status bar / camera cutout on Home screen & allowed apps.
 * Uses FLAG_NOT_TOUCHABLE + FLAG_NOT_FOCUSABLE so touches pass directly through to Home screen,
 * Phone/Dialer, and other allowed apps without any interference.
 */
object StrictLockFloatingPillManager {

  private const val TAG = "StrictLockPill"

  private var pillView: TextView? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private var isPillAttached = false

  /**
   * Shows or attaches the floating pill if overlay permission is granted and strict lock is active.
   */
  @SuppressLint("SetTextI18n")
  fun show(context: Context) {
    mainHandler.post {
      if (!Settings.canDrawOverlays(context)) {
        Log.d(TAG, "Cannot show floating pill: overlay permission not granted")
        return@post
      }
      if (!FocusModeManager.isStrictLockActive(context)) {
        dismiss()
        return@post
      }
      if (isPillAttached && pillView != null) {
        pillView?.visibility = View.VISIBLE
        updateTime(context)
        return@post
      }

      val wm = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return@post

      dismissInternal(wm)

      val density = context.resources.displayMetrics.density
      fun dp(v: Float) = (v * density + 0.5f).toInt()

      val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
      } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
      }

      val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        layoutFlag,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = dp(28f) // Placed right below the top status bar / camera cutout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
      }

      val tv = TextView(context.applicationContext).apply {
        val bg = GradientDrawable().apply {
          setColor(Color.parseColor("#E60F172A")) // Dark Slate 900, 90% opacity
          cornerRadius = dp(14f).toFloat()
          setStroke(dp(1f), Color.parseColor("#334155")) // Muted Slate 700 border
        }
        background = bg
        setPadding(dp(10f), dp(3f), dp(10f), dp(3f))
        setTextColor(Color.parseColor("#F1F5F9"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        elevation = dp(4f).toFloat()
      }

      try {
        wm.addView(tv, params)
        pillView = tv
        isPillAttached = true
        updateTime(context)
        Log.d(TAG, "Floating countdown pill attached successfully")
      } catch (e: Exception) {
        Log.e(TAG, "Failed to attach floating countdown pill", e)
      }
    }
  }

  /**
   * Updates the remaining time displayed in the pill.
   */
  fun updateTime(context: Context, remainingMillis: Long? = null) {
    mainHandler.post {
      val tv = pillView ?: return@post
      val rem = remainingMillis ?: FocusModeManager.getStrictLockRemainingMillis(context)
      if (rem <= 0L) {
        dismiss()
        return@post
      }
      val totalSecs = (rem / 1000L).coerceAtLeast(0)
      val mins = totalSecs / 60
      val secs = totalSecs % 60
      val text = String.format("🔒 %02d:%02d", mins, secs)
      tv.text = text
    }
  }

  /**
   * Temporarily hides or restores pill visibility (e.g., while full-screen blocking overlay is showing).
   */
  fun setVisibility(visible: Boolean) {
    mainHandler.post {
      pillView?.visibility = if (visible) View.VISIBLE else View.GONE
    }
  }

  /**
   * Removes and dismisses the floating pill cleanly.
   */
  fun dismiss() {
    mainHandler.post {
      val view = pillView ?: return@post
      val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
      if (wm != null) {
        dismissInternal(wm)
      }
    }
  }

  private fun dismissInternal(wm: WindowManager) {
    pillView?.let {
      try {
        wm.removeView(it)
        Log.d(TAG, "Floating countdown pill removed")
      } catch (e: Exception) {
        Log.e(TAG, "Error removing floating pill", e)
      }
    }
    pillView = null
    isPillAttached = false
  }
}
