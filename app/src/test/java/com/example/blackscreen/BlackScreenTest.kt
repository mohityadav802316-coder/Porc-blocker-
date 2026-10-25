package com.example.blackscreen

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.UserSettingsEntity
import com.example.data.repository.LifeTrackerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BlackScreenTest {

  private fun createMultiTouchEvent(
    action: Int,
    pointerCoords: List<Pair<Float, Float>>,
    downTime: Long = SystemClock.uptimeMillis(),
    eventTime: Long = SystemClock.uptimeMillis()
  ): MotionEvent {
    val pointerCount = pointerCoords.size
    val pointerProperties = Array(pointerCount) { i ->
      MotionEvent.PointerProperties().apply {
        id = i
        toolType = MotionEvent.TOOL_TYPE_FINGER
      }
    }
    val pointerCoordsArray = Array(pointerCount) { i ->
      MotionEvent.PointerCoords().apply {
        x = pointerCoords[i].first
        y = pointerCoords[i].second
        pressure = 1.0f
        size = 1.0f
      }
    }

    return MotionEvent.obtain(
      downTime,
      eventTime,
      action,
      pointerCount,
      pointerProperties,
      pointerCoordsArray,
      0,
      0,
      1.0f,
      1.0f,
      0,
      0,
      0,
      0
    )
  }

  @Test
  fun testGestureTypeParsing() {
    assertEquals(BlackScreenGestureType.THREE_FINGER_TAP, BlackScreenGestureType.fromId("THREE_FINGER_TAP"))
    assertEquals(BlackScreenGestureType.THREE_FINGER_LONG_PRESS, BlackScreenGestureType.fromId("THREE_FINGER_LONG_PRESS"))
    assertEquals(BlackScreenGestureType.THREE_FINGER_SWIPE_UP, BlackScreenGestureType.fromId("THREE_FINGER_SWIPE_UP"))
    assertEquals(BlackScreenGestureType.THREE_FINGER_SWIPE_DOWN, BlackScreenGestureType.fromId("THREE_FINGER_SWIPE_DOWN"))
    assertEquals(BlackScreenGestureType.THREE_FINGER_SWIPE_LEFT, BlackScreenGestureType.fromId("THREE_FINGER_SWIPE_LEFT"))
    assertEquals(BlackScreenGestureType.THREE_FINGER_SWIPE_RIGHT, BlackScreenGestureType.fromId("THREE_FINGER_SWIPE_RIGHT"))
    // Fallback default
    assertEquals(BlackScreenGestureType.THREE_FINGER_TAP, BlackScreenGestureType.fromId("UNKNOWN_ID"))
  }

  @Test
  fun testRejectionOfOneFingerTouch() {
    var detected = false
    val detector = BlackScreenGestureDetector(
      targetGesture = BlackScreenGestureType.THREE_FINGER_TAP,
      sensitivity = 1.0f,
      onGestureRecognized = { detected = true }
    )

    val downTime = SystemClock.uptimeMillis()

    // 1-finger down
    val down = createMultiTouchEvent(MotionEvent.ACTION_DOWN, listOf(100f to 200f), downTime, downTime)
    detector.onTouchEvent(down)

    // 1-finger up
    val up = createMultiTouchEvent(MotionEvent.ACTION_UP, listOf(100f to 200f), downTime, downTime + 150)
    detector.onTouchEvent(up)

    // Must NOT recognize with only 1 finger
    assertFalse(detected)
  }

  @Test
  fun testRejectionOfTwoFingerTouch() {
    var detected = false
    val detector = BlackScreenGestureDetector(
      targetGesture = BlackScreenGestureType.THREE_FINGER_TAP,
      sensitivity = 1.0f,
      onGestureRecognized = { detected = true }
    )

    val downTime = SystemClock.uptimeMillis()

    // 2-finger down
    val down = createMultiTouchEvent(
      MotionEvent.ACTION_DOWN,
      listOf(100f to 200f),
      downTime,
      downTime
    )
    detector.onTouchEvent(down)

    val pointer2Down = createMultiTouchEvent(
      MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
      listOf(100f to 200f, 150f to 200f),
      downTime,
      downTime + 20
    )
    detector.onTouchEvent(pointer2Down)

    val up = createMultiTouchEvent(
      MotionEvent.ACTION_UP,
      listOf(100f to 200f, 150f to 200f),
      downTime,
      downTime + 150
    )
    detector.onTouchEvent(up)

    // Must NOT recognize with only 2 fingers
    assertFalse(detected)
  }

  @Test
  fun testThreeFingerTapDetection() {
    var detected = false
    val detector = BlackScreenGestureDetector(
      targetGesture = BlackScreenGestureType.THREE_FINGER_TAP,
      sensitivity = 1.0f,
      onGestureRecognized = { detected = true }
    )

    val downTime = SystemClock.uptimeMillis()
    val fingers = listOf(100f to 200f, 200f to 200f, 300f to 200f)

    // 1st finger down
    val ev1 = createMultiTouchEvent(MotionEvent.ACTION_DOWN, listOf(fingers[0]), downTime, downTime)
    detector.onTouchEvent(ev1)

    // 2nd finger down
    val ev2 = createMultiTouchEvent(
      MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
      listOf(fingers[0], fingers[1]),
      downTime,
      downTime + 10
    )
    detector.onTouchEvent(ev2)

    // 3rd finger down
    val ev3 = createMultiTouchEvent(
      MotionEvent.ACTION_POINTER_DOWN or (2 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
      fingers,
      downTime,
      downTime + 20
    )
    detector.onTouchEvent(ev3)

    // 3 fingers up within tap duration (180ms)
    val evUp = createMultiTouchEvent(MotionEvent.ACTION_UP, fingers, downTime, downTime + 180)
    detector.onTouchEvent(evUp)

    assertTrue("Three-finger tap should be successfully recognized", detected)
  }

  @Test
  fun testThreeFingerSwipeUpDetection() {
    var detected = false
    val detector = BlackScreenGestureDetector(
      targetGesture = BlackScreenGestureType.THREE_FINGER_SWIPE_UP,
      sensitivity = 1.0f,
      onGestureRecognized = { detected = true }
    )

    val downTime = SystemClock.uptimeMillis()
    val startFingers = listOf(100f to 500f, 200f to 500f, 300f to 500f)
    val movedFingers = listOf(100f to 250f, 200f to 250f, 300f to 250f) // Moved upward by 250px

    detector.onTouchEvent(createMultiTouchEvent(MotionEvent.ACTION_DOWN, listOf(startFingers[0]), downTime, downTime))
    detector.onTouchEvent(createMultiTouchEvent(MotionEvent.ACTION_POINTER_DOWN or (1 shl 8), listOf(startFingers[0], startFingers[1]), downTime, downTime + 10))
    detector.onTouchEvent(createMultiTouchEvent(MotionEvent.ACTION_POINTER_DOWN or (2 shl 8), startFingers, downTime, downTime + 20))

    // Move upwards
    detector.onTouchEvent(createMultiTouchEvent(MotionEvent.ACTION_MOVE, movedFingers, downTime, downTime + 150))

    // Lift fingers
    detector.onTouchEvent(createMultiTouchEvent(MotionEvent.ACTION_UP, movedFingers, downTime, downTime + 200))

    assertTrue("Three-finger swipe up should be detected", detected)
  }

  @Test
  fun testRoomDatabasePersistenceAndSettings() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, LifeTrackerDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repository = LifeTrackerRepository(db)

    // Initial default settings
    val initial = UserSettingsEntity(anchorDate = "2026-09-27")
    db.userSettingsDao().insertOrUpdate(initial)

    // Update black screen settings
    repository.updateBlackScreenEnabled(true)
    repository.updateBlackScreenFloatingDotEnabled(true)
    repository.updateBlackScreenDotPosition(120, 340)
    repository.updateBlackScreenDotSize(48)
    repository.updateBlackScreenDotOpacity(0.8f)
    repository.updateBlackScreenActivationMethod("LONG_PRESS")
    repository.updateBlackScreenExitGesture("THREE_FINGER_SWIPE_UP")
    repository.updateBlackScreenGestureSensitivity(1.2f)
    repository.updateBlackScreenShowClock(true)
    repository.updateBlackScreenClockFormat24(true)
    repository.updateBlackScreenRestoreAfterUnlock(true)
    repository.updateBlackScreenRestoreAfterReboot(true)

    val stored = db.userSettingsDao().getSettingsDirect()
    assertNotNull(stored)
    assertEquals(true, stored?.isBlackScreenEnabled)
    assertEquals(true, stored?.isFloatingDotEnabled)
    assertEquals(120, stored?.blackScreenDotX)
    assertEquals(340, stored?.blackScreenDotY)
    assertEquals(48, stored?.blackScreenDotSize)
    assertEquals(0.8f, stored?.blackScreenDotOpacity ?: 0f, 0.01f)
    assertEquals("LONG_PRESS", stored?.blackScreenActivationMethod)
    assertEquals("THREE_FINGER_SWIPE_UP", stored?.blackScreenExitGesture)
    assertEquals(1.2f, stored?.blackScreenGestureSensitivity ?: 0f, 0.01f)
    assertEquals(true, stored?.blackScreenShowClock)
    assertEquals(true, stored?.blackScreenClockFormat24)
    assertEquals(true, stored?.blackScreenRestoreAfterUnlock)
    assertEquals(true, stored?.blackScreenRestoreAfterReboot)

    db.close()
  }
}
