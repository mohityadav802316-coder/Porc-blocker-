package com.example.blackscreen

import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Supported deliberate three-finger gestures for exiting Black Screen Mode.
 */
enum class BlackScreenGestureType(val id: String, val title: String, val description: String) {
  THREE_FINGER_TAP(
    id = "THREE_FINGER_TAP",
    title = "Three-Finger Tap",
    description = "Place 3 fingers down together and release within a deliberate window"
  ),
  THREE_FINGER_LONG_PRESS(
    id = "THREE_FINGER_LONG_PRESS",
    title = "Three-Finger Long Press",
    description = "Place 3 fingers down and hold firmly in place for over 1.2 seconds"
  ),
  THREE_FINGER_SWIPE_UP(
    id = "THREE_FINGER_SWIPE_UP",
    title = "Three-Finger Swipe Up",
    description = "Slide 3 fingers firmly upwards across the screen"
  ),
  THREE_FINGER_SWIPE_DOWN(
    id = "THREE_FINGER_SWIPE_DOWN",
    title = "Three-Finger Swipe Down",
    description = "Slide 3 fingers firmly downwards across the screen"
  ),
  THREE_FINGER_SWIPE_LEFT(
    id = "THREE_FINGER_SWIPE_LEFT",
    title = "Three-Finger Swipe Left",
    description = "Slide 3 fingers firmly to the left across the screen"
  ),
  THREE_FINGER_SWIPE_RIGHT(
    id = "THREE_FINGER_SWIPE_RIGHT",
    title = "Three-Finger Swipe Right",
    description = "Slide 3 fingers firmly to the right across the screen"
  );

  companion object {
    fun fromId(id: String): BlackScreenGestureType {
      return entries.find { it.id.equals(id, ignoreCase = true) } ?: THREE_FINGER_TAP
    }
  }
}

/**
 * State snapshot for interactive gesture testing & visualization.
 */
data class GestureRecognitionFeedback(
  val pointerCount: Int = 0,
  val touchPoints: List<Pair<Float, Float>> = emptyList(),
  val message: String = "Place 3 fingers on screen",
  val isThreeFingerActive: Boolean = false,
  val recognizedGesture: BlackScreenGestureType? = null,
  val isSuccess: Boolean = false
)

/**
 * Production-ready multi-touch gesture detector for OLED Black Screen Mode.
 * Strictly verifies pointerCount >= 3 to prevent accidental one/two-finger exit.
 */
class BlackScreenGestureDetector(
  var targetGesture: BlackScreenGestureType = BlackScreenGestureType.THREE_FINGER_TAP,
  var sensitivity: Float = 1.0f, // 0.5f (more sensitive/easier) to 1.5f (stricter)
  private val onGestureRecognized: (BlackScreenGestureType) -> Unit = {}
) {

  private var maxPointerCountSeen = 0
  private var downTimestamp: Long = 0L
  private val startCoords = mutableMapOf<Int, Pair<Float, Float>>()
  private val lastCoords = mutableMapOf<Int, Pair<Float, Float>>()
  private var longPressTriggered = false

  // Base threshold values in pixels (adjusted by sensitivity)
  val swipeMinDistance: Float
    get() = 120f * (1.0f / sensitivity.coerceIn(0.5f, 2.0f))

  val tapMaxMovement: Float
    get() = 90f * (1.0f / sensitivity.coerceIn(0.5f, 2.0f))

  val longPressMinDurationMs: Long = 1000L

  /**
   * Processes a MotionEvent.
   * Returns true if event was consumed or contributed to the 3-finger gesture.
   */
  fun onTouchEvent(
    event: MotionEvent,
    onFeedbackUpdate: ((GestureRecognitionFeedback) -> Unit)? = null
  ): Boolean {
    val actionMasked = event.actionMasked
    val pointerCount = event.pointerCount

    if (pointerCount > maxPointerCountSeen) {
      maxPointerCountSeen = pointerCount
    }

    val currentPoints = (0 until pointerCount).map { index ->
      event.getX(index) to event.getY(index)
    }

    when (actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downTimestamp = System.currentTimeMillis()
        maxPointerCountSeen = 1
        startCoords.clear()
        lastCoords.clear()
        longPressTriggered = false

        val id = event.getPointerId(0)
        val pt = event.getX(0) to event.getY(0)
        startCoords[id] = pt
        lastCoords[id] = pt

        onFeedbackUpdate?.invoke(
          GestureRecognitionFeedback(
            pointerCount = 1,
            touchPoints = currentPoints,
            message = "1 finger detected (requires 3 fingers)",
            isThreeFingerActive = false
          )
        )
      }

      MotionEvent.ACTION_POINTER_DOWN -> {
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        val pt = event.getX(actionIndex) to event.getY(actionIndex)
        startCoords[pointerId] = pt
        lastCoords[pointerId] = pt

        val isThreeOrMore = pointerCount >= 3
        val msg = if (isThreeOrMore) {
          "3 fingers placed! Performing ${targetGesture.title}..."
        } else {
          "$pointerCount fingers detected (requires 3 fingers)"
        }

        onFeedbackUpdate?.invoke(
          GestureRecognitionFeedback(
            pointerCount = pointerCount,
            touchPoints = currentPoints,
            message = msg,
            isThreeFingerActive = isThreeOrMore
          )
        )
      }

      MotionEvent.ACTION_MOVE -> {
        for (i in 0 until pointerCount) {
          val id = event.getPointerId(i)
          val coord = event.getX(i) to event.getY(i)
          lastCoords[id] = coord
          if (!startCoords.containsKey(id)) {
            startCoords[id] = coord
          }
        }

        val duration = System.currentTimeMillis() - downTimestamp

        // Check for long press while fingers are still down
        if (targetGesture == BlackScreenGestureType.THREE_FINGER_LONG_PRESS &&
          !longPressTriggered &&
          maxPointerCountSeen >= 3 &&
          pointerCount >= 3 &&
          duration >= longPressMinDurationMs
        ) {
          // Verify movement remained within tap bounds
          var maxMove = 0f
          for ((id, start) in startCoords) {
            val curr = lastCoords[id] ?: continue
            val dist = hypot(curr.first - start.first, curr.second - start.second)
            if (dist > maxMove) maxMove = dist
          }

          if (maxMove <= tapMaxMovement) {
            longPressTriggered = true
            onGestureRecognized(BlackScreenGestureType.THREE_FINGER_LONG_PRESS)
            onFeedbackUpdate?.invoke(
              GestureRecognitionFeedback(
                pointerCount = pointerCount,
                touchPoints = currentPoints,
                message = "Three-Finger Long Press Recognized! ✅",
                isThreeFingerActive = true,
                recognizedGesture = BlackScreenGestureType.THREE_FINGER_LONG_PRESS,
                isSuccess = true
              )
            )
            return true
          }
        }

        val isThreeOrMore = pointerCount >= 3
        val progressMsg = if (isThreeOrMore) {
          when (targetGesture) {
            BlackScreenGestureType.THREE_FINGER_LONG_PRESS -> "Holding... (${duration}ms / ${longPressMinDurationMs}ms)"
            BlackScreenGestureType.THREE_FINGER_TAP -> "3 fingers active. Release to complete tap."
            else -> "3 fingers moving for ${targetGesture.title}..."
          }
        } else {
          "$pointerCount fingers active (need 3)"
        }

        onFeedbackUpdate?.invoke(
          GestureRecognitionFeedback(
            pointerCount = pointerCount,
            touchPoints = currentPoints,
            message = progressMsg,
            isThreeFingerActive = isThreeOrMore
          )
        )
      }

      MotionEvent.ACTION_POINTER_UP -> {
        // Record last coordinate before lifting
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        lastCoords[pointerId] = event.getX(actionIndex) to event.getY(actionIndex)
      }

      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        val duration = if (event.eventTime >= event.downTime && event.downTime > 0) {
          event.eventTime - event.downTime
        } else {
          System.currentTimeMillis() - downTimestamp
        }

        // Strictly check that at least 3 fingers were on the screen during this touch interaction
        if (maxPointerCountSeen < 3) {
          onFeedbackUpdate?.invoke(
            GestureRecognitionFeedback(
              pointerCount = 0,
              touchPoints = emptyList(),
              message = "Ignored: Only $maxPointerCountSeen finger(s) detected. Must use 3 fingers.",
              isThreeFingerActive = false
            )
          )
          return false
        }

        if (longPressTriggered) {
          // Already handled
          maxPointerCountSeen = 0
          return true
        }

        // Calculate average delta X and Y across all tracked fingers
        var totalDeltaX = 0f
        var totalDeltaY = 0f
        var countedPointers = 0
        var maxDistanceMoved = 0f

        for ((id, start) in startCoords) {
          val end = lastCoords[id] ?: continue
          val dx = end.first - start.first
          val dy = end.second - start.second
          totalDeltaX += dx
          totalDeltaY += dy
          val dist = hypot(dx, dy)
          if (dist > maxDistanceMoved) maxDistanceMoved = dist
          countedPointers++
        }

        val avgDeltaX = if (countedPointers > 0) totalDeltaX / countedPointers else 0f
        val avgDeltaY = if (countedPointers > 0) totalDeltaY / countedPointers else 0f

        val detectedGesture = evaluateGesture(
          durationMs = duration,
          avgDeltaX = avgDeltaX,
          avgDeltaY = avgDeltaY,
          maxDistanceMoved = maxDistanceMoved
        )

        maxPointerCountSeen = 0

        if (detectedGesture != null && detectedGesture == targetGesture) {
          onGestureRecognized(detectedGesture)
          onFeedbackUpdate?.invoke(
            GestureRecognitionFeedback(
              pointerCount = 0,
              touchPoints = emptyList(),
              message = "${detectedGesture.title} Recognized! ✅",
              isThreeFingerActive = false,
              recognizedGesture = detectedGesture,
              isSuccess = true
            )
          )
          return true
        } else if (detectedGesture != null) {
          onFeedbackUpdate?.invoke(
            GestureRecognitionFeedback(
              pointerCount = 0,
              touchPoints = emptyList(),
              message = "Recognized ${detectedGesture.title}, but selected gesture is ${targetGesture.title}",
              isThreeFingerActive = false,
              recognizedGesture = detectedGesture,
              isSuccess = false
            )
          )
        } else {
          onFeedbackUpdate?.invoke(
            GestureRecognitionFeedback(
              pointerCount = 0,
              touchPoints = emptyList(),
              message = "Gesture not recognized. Try a deliberate 3-finger motion.",
              isThreeFingerActive = false
            )
          )
        }
      }
    }

    return true
  }

  private fun evaluateGesture(
    durationMs: Long,
    avgDeltaX: Float,
    avgDeltaY: Float,
    maxDistanceMoved: Float
  ): BlackScreenGestureType? {
    val absX = abs(avgDeltaX)
    val absY = abs(avgDeltaY)

    // Check Tap: duration <= 700ms and minimal movement
    if (durationMs in 0..700 && maxDistanceMoved <= tapMaxMovement) {
      return BlackScreenGestureType.THREE_FINGER_TAP
    }

    // Check Long Press on release if not already triggered
    if (durationMs >= longPressMinDurationMs && maxDistanceMoved <= tapMaxMovement) {
      return BlackScreenGestureType.THREE_FINGER_LONG_PRESS
    }

    // Check Swipes
    if (absY > swipeMinDistance && absY > absX * 1.3f) {
      return if (avgDeltaY < 0) {
        BlackScreenGestureType.THREE_FINGER_SWIPE_UP
      } else {
        BlackScreenGestureType.THREE_FINGER_SWIPE_DOWN
      }
    }

    if (absX > swipeMinDistance && absX > absY * 1.3f) {
      return if (avgDeltaX < 0) {
        BlackScreenGestureType.THREE_FINGER_SWIPE_LEFT
      } else {
        BlackScreenGestureType.THREE_FINGER_SWIPE_RIGHT
      }
    }

    return null
  }
}
