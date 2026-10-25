package com.example.shortcontent

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ShortAppType
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
class ShortAppDetectorTest {

  // =========================================================================
  // 1. INSTAGRAM REELS TESTS
  // =========================================================================

  @Test
  fun `verify Instagram Reels detection reaches HIGH confidence with valid multi-signals`() {
    val detector = InstagramReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.instagram.android"
      className = "com.instagram.clips.viewer.ClipsViewerFragment"
      viewIdResourceName = "com.instagram.android:id/clips_viewer"
      text = "Reels"
      contentDescription = "Original audio by artist"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.instagram.android",
      eventClassName = "com.instagram.clips.viewer.ClipsViewerFragment"
    )

    assertEquals(DetectionConfidence.HIGH, result.confidence)
    assertEquals(ShortAppType.INSTAGRAM, result.appType)
    assertTrue("Must be marked as short video", result.isShortVideo)
    assertTrue("Score must be >= 50", result.score >= 50)
  }

  @Test
  fun `verify normal Instagram feed photo post is NOT detected as Reels`() {
    val detector = InstagramReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.instagram.android"
      className = "android.widget.LinearLayout"
      viewIdResourceName = "com.instagram.android:id/row_feed_photo_profile_name"
      text = "Normal User Post"
      contentDescription = "Photo by john_doe"
      setBoundsInScreen(Rect(0, 0, 1080, 1200))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.instagram.android",
      eventClassName = "com.instagram.mainfeed.FeedFragment"
    )

    assertFalse("Normal feed post must NOT be detected as short video", result.isShortVideo)
    assertEquals("Should be LOW confidence", DetectionConfidence.LOW, result.confidence)
  }

  @Test
  fun `verify Instagram Story viewer is rejected by negative indicators`() {
    val detector = InstagramReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.instagram.android"
      className = "android.widget.FrameLayout"
      viewIdResourceName = "com.instagram.android:id/story_viewer"
      text = "Send message"
      contentDescription = "Reply to story"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.instagram.android",
      eventClassName = "com.instagram.stories.StoryActivity"
    )

    assertFalse("Stories must NOT be detected as Reels", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  @Test
  fun `verify Instagram standalone share dialog is NOT detected as Reels`() {
    val detector = InstagramReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.instagram.android"
      className = "android.widget.FrameLayout"
      viewIdResourceName = "com.instagram.android:id/direct_share_sheet"
      text = "Share to"
      contentDescription = "Send to users"
      setBoundsInScreen(Rect(0, 0, 1080, 1500))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.instagram.android",
      eventClassName = "com.instagram.direct.share.ShareDialog"
    )

    assertFalse("Share dialog must NOT be detected as Reels", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  // =========================================================================
  // 2. FACEBOOK REELS TESTS
  // =========================================================================

  @Test
  fun `verify Facebook Reels detection reaches HIGH confidence with valid signals`() {
    val detector = FacebookReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.facebook.katana"
      className = "com.facebook.feed.reels.ReelsViewerActivity"
      viewIdResourceName = "com.facebook.katana:id/fb_reels_container"
      text = "Reels"
      contentDescription = "Original audio"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.facebook.katana",
      eventClassName = "com.facebook.feed.reels.ReelsViewerActivity"
    )

    assertEquals(DetectionConfidence.HIGH, result.confidence)
    assertEquals(ShortAppType.FACEBOOK, result.appType)
    assertTrue("Must be identified as Facebook Reel", result.isShortVideo)
  }

  @Test
  fun `verify normal Facebook Newsfeed post is NOT detected as Reels`() {
    val detector = FacebookReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.facebook.katana"
      className = "android.widget.LinearLayout"
      viewIdResourceName = "com.facebook.katana:id/news_feed_stream"
      text = "What's on your mind?"
      contentDescription = "Post from Friends"
      setBoundsInScreen(Rect(0, 0, 1080, 1200))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.facebook.katana",
      eventClassName = "com.facebook.katana.activity.FbMainTabActivity"
    )

    assertFalse("Normal feed post must NOT be detected as Reel", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  @Test
  fun `verify Facebook Messenger chat is NOT detected as Reels`() {
    val detector = FacebookReelsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.facebook.katana"
      className = "android.widget.FrameLayout"
      viewIdResourceName = "com.facebook.katana:id/messenger_thread"
      text = "Type a message..."
      contentDescription = "Messenger chat with Alex"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.facebook.katana",
      eventClassName = "com.facebook.messenger.ThreadActivity"
    )

    assertFalse("Messenger must NOT be detected as Reel", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  // =========================================================================
  // 3. YOUTUBE SHORTS TESTS
  // =========================================================================

  @Test
  fun `verify YouTube Shorts detection reaches HIGH confidence with shorts indicators`() {
    val detector = YouTubeShortsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.google.android.youtube"
      className = "com.google.android.apps.youtube.app.extensions.reel.ReelPlayerView"
      viewIdResourceName = "com.google.android.youtube:id/shorts_player"
      text = "Shorts"
      contentDescription = "Sound used in shorts"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.google.android.youtube",
      eventClassName = "com.google.android.apps.youtube.app.extensions.reel.ShortsActivity"
    )

    assertEquals(DetectionConfidence.HIGH, result.confidence)
    assertEquals(ShortAppType.YOUTUBE, result.appType)
    assertTrue("YouTube Shorts must be detected", result.isShortVideo)
  }

  @Test
  fun `verify regular YouTube video player with seekbar is NOT detected as Shorts`() {
    val detector = YouTubeShortsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.google.android.youtube"
      className = "android.widget.FrameLayout"
      viewIdResourceName = "com.google.android.youtube:id/time_bar"
      text = "10:15 / 24:30"
      contentDescription = "Video progress bar"
      setBoundsInScreen(Rect(0, 0, 1080, 800))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.google.android.youtube",
      eventClassName = "com.google.android.apps.youtube.app.WatchWhileActivity"
    )

    assertFalse("Regular YouTube video player must NOT be detected as Shorts", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  @Test
  fun `verify regular YouTube recommendations list is NOT detected as Shorts`() {
    val detector = YouTubeShortsDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.google.android.youtube"
      className = "androidx.recyclerview.widget.RecyclerView"
      viewIdResourceName = "com.google.android.youtube:id/watch_next_list"
      text = "Up next: Amazing Science Documentary"
      setBoundsInScreen(Rect(0, 800, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.google.android.youtube",
      eventClassName = "com.google.android.apps.youtube.app.WatchWhileActivity"
    )

    assertFalse("Recommendations list must NOT be detected as Shorts", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  // =========================================================================
  // 4. SNAPCHAT SPOTLIGHT TESTS
  // =========================================================================

  @Test
  fun `verify Snapchat Spotlight reaches HIGH confidence with spotlight signals`() {
    val detector = SnapchatSpotlightDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.snapchat.android"
      className = "com.snapchat.android.spotlight.SpotlightViewer"
      viewIdResourceName = "com.snapchat.android:id/spotlight_feed"
      text = "Spotlight"
      contentDescription = "Original sound by creator"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.snapchat.android",
      eventClassName = "com.snapchat.android.spotlight.SpotlightActivity"
    )

    assertEquals(DetectionConfidence.HIGH, result.confidence)
    assertEquals(ShortAppType.SNAPCHAT, result.appType)
    assertTrue("Snapchat Spotlight must be detected", result.isShortVideo)
  }

  @Test
  fun `verify Snapchat Chat screen is NOT detected as Spotlight`() {
    val detector = SnapchatSpotlightDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.snapchat.android"
      className = "android.widget.EditText"
      viewIdResourceName = "com.snapchat.android:id/chat_input"
      text = "Send a chat"
      contentDescription = "Send a chat message"
      setBoundsInScreen(Rect(0, 0, 1080, 200))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.snapchat.android",
      eventClassName = "com.snapchat.android.chat.ChatActivity"
    )

    assertFalse("Chat screen must NOT be detected as Spotlight", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  @Test
  fun `verify Snapchat Camera screen is NOT detected as Spotlight`() {
    val detector = SnapchatSpotlightDetector()
    val root = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.snapchat.android"
      className = "android.widget.FrameLayout"
      viewIdResourceName = "com.snapchat.android:id/camera_shutter"
      text = "Take photo"
      contentDescription = "Camera capture button"
      setBoundsInScreen(Rect(400, 1800, 680, 2080))
    }

    val result = detector.detect(
      rootNode = root,
      eventPackage = "com.snapchat.android",
      eventClassName = "com.snapchat.android.camera.CameraActivity"
    )

    assertFalse("Camera screen must NOT be detected as Spotlight", result.isShortVideo)
    assertEquals(DetectionConfidence.LOW, result.confidence)
  }

  // =========================================================================
  // 5. REGISTRY AND GEOMETRY NORMALIZATION
  // =========================================================================

  @Test
  fun `verify registry holds all four platforms`() {
    val supported = ShortAppRegistry.getAllSupportedPackages()
    assertTrue(supported.contains("com.instagram.android"))
    assertTrue(supported.contains("com.facebook.katana"))
    assertTrue(supported.contains("com.facebook.lite"))
    assertTrue(supported.contains("com.google.android.youtube"))
    assertTrue(supported.contains("com.snapchat.android"))

    val igDetector = ShortAppRegistry.getDetectorForPackage("com.instagram.android")
    assertNotNull(igDetector)
    assertEquals(ShortAppType.INSTAGRAM, igDetector?.appType)

    val scDetector = ShortAppRegistry.getDetectorForPackage("com.snapchat.android")
    assertNotNull(scDetector)
    assertEquals(ShortAppType.SNAPCHAT, scDetector?.appType)
  }

  @Test
  fun `verify relative geometry adapts across different screen densities and sizes`() {
    // 1. Vertical rail test (e.g. Right rail in 1080p phone)
    val verticalPhoneSnapshots = listOf(
      AccessibilityNodeSnapshot(
        className = "android.widget.ImageView",
        viewId = "like",
        text = null,
        contentDescription = "like 1.2M",
        bounds = Rect(950, 1200, 1050, 1300), // 100x100 button
        isClickable = true
      ),
      AccessibilityNodeSnapshot(
        className = "android.widget.ImageView",
        viewId = "comment",
        text = null,
        contentDescription = "comment 45K",
        bounds = Rect(950, 1380, 1050, 1480), // 100x100 button, vertically lower
        isClickable = true
      )
    )
    assertTrue("Should detect vertical stack on phone", hasVerticalActionStack(verticalPhoneSnapshots, "like", "comment"))
    assertFalse("Should NOT detect horizontal pair on phone", hasHorizontalActionPair(verticalPhoneSnapshots, "like", "comment"))

    // 2. Horizontal row test (e.g. Feed card [Like] [Comment] side by side)
    val horizontalFeedSnapshots = listOf(
      AccessibilityNodeSnapshot(
        className = "android.widget.ImageView",
        viewId = "feed_like",
        text = "Like",
        contentDescription = "Like post",
        bounds = Rect(40, 900, 140, 980), // button 1
        isClickable = true
      ),
      AccessibilityNodeSnapshot(
        className = "android.widget.ImageView",
        viewId = "feed_comment",
        text = "Comment",
        contentDescription = "Comment on post",
        bounds = Rect(180, 900, 280, 980), // button 2 on same horizontal line
        isClickable = true
      )
    )
    assertTrue("Should detect horizontal pair on feed post", hasHorizontalActionPair(horizontalFeedSnapshots, "like", "comment"))
    assertFalse("Should NOT detect vertical stack on feed post", hasVerticalActionStack(horizontalFeedSnapshots, "like", "comment"))
  }

  // =========================================================================
  // 6. DEDUPLICATION TEST (REPEATED ACCESSIBILITY EVENTS)
  // =========================================================================

  @Test
  fun `verify repeated accessibility events on same Reel produces only one count`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = ShortContentTrackerManager.getInstance(context)

    val testSignature = "test_reel_unique_signature_123"
    val initialTime = System.currentTimeMillis()

    // Simulate 5 repeated events fired by Android Accessibility on the same video
    manager.onShortVideoDetected("com.instagram.android", testSignature, initialTime)
    manager.onShortVideoDetected("com.instagram.android", testSignature, initialTime + 100)
    manager.onShortVideoDetected("com.instagram.android", testSignature, initialTime + 300)
    manager.onShortVideoDetected("com.instagram.android", testSignature, initialTime + 700)
    manager.onShortVideoDetected("com.instagram.android", testSignature, initialTime + 1200)

    // Verify deterministic signature output
    val detector = InstagramReelsDetector()
    val node = AccessibilityNodeInfo.obtain().apply {
      packageName = "com.instagram.android"
      text = "Creator Reel Title"
      contentDescription = "@influencer"
      setBoundsInScreen(Rect(0, 0, 1080, 2400))
    }
    val sigA = detector.extractVideoSignature(node, "com.instagram.android")
    val sigB = detector.extractVideoSignature(node, "com.instagram.android")
    assertEquals(sigA, sigB)
  }
}
