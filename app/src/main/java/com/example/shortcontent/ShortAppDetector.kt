package com.example.shortcontent

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.model.ShortAppType
import java.security.MessageDigest

/**
 * Confidence levels for multi-signal short-form video detection.
 * HIGH: Strong multi-signal match (triggers tracking & blocking).
 * MEDIUM: Partial signals but incomplete/ambiguous (logged/monitored, NEVER false blocks).
 * LOW: Generic buttons, normal posts, or negative signals (ignored).
 */
enum class DetectionConfidence {
  HIGH,
  MEDIUM,
  LOW
}

/**
 * Detailed result of short-content inspection.
 */
data class DetectionResult(
  val confidence: DetectionConfidence,
  val appType: ShortAppType,
  val matchedSignals: List<String> = emptyList(),
  val score: Int = 0,
  val isShortVideo: Boolean = confidence == DetectionConfidence.HIGH
)

/**
 * Interface defining robust, multi-signal detection of short-form video screens
 * across major platforms (Instagram Reels, Facebook Reels, YouTube Shorts, Snapchat Spotlight).
 */
interface ShortAppDetector {
  val targetPackages: Set<String>
  val appType: ShortAppType

  /**
   * Evaluates the current accessibility hierarchy against multi-signal profiles.
   */
  fun detect(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): DetectionResult

  /**
   * Compatibility helper: Returns true if and only if HIGH confidence is reached.
   */
  fun isShortVideoScreen(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): Boolean = detect(rootNode, eventPackage, eventClassName).isShortVideo

  /**
   * Extract a deterministic, privacy-safe signature of the current short video.
   * NEVER reads or stores user personal messages or passwords.
   */
  fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String?
}

/**
 * Lightweight snapshot of an accessibility node used for multi-signal analysis.
 */
data class AccessibilityNodeSnapshot(
  val className: String?,
  val viewId: String?,
  val text: String?,
  val contentDescription: String?,
  val bounds: Rect,
  val isClickable: Boolean
) {
  val combinedLabel: String by lazy {
    buildString {
      if (!text.isNullOrBlank()) append(text.lowercase()).append(" ")
      if (!contentDescription.isNullOrBlank()) append(contentDescription.lowercase())
    }.trim()
  }
}

/**
 * Common action keywords that are part of UI chrome and must not be used as video signatures.
 */
private val GENERIC_UI_WORDS = setOf(
  "like", "liked", "comment", "comments", "share", "shares", "remix",
  "bookmark", "save", "saved", "send", "more", "audio", "original audio",
  "follow", "following", "subscribe", "subscribed", "dislike", "disliked",
  "back", "search", "camera", "menu", "options", "close", "mute", "unmute",
  "view profile", "pause", "play", "report", "sponsored", "reels", "shorts",
  "spotlight", "home", "subscriptions", "you", "profile", "explore", "inbox",
  "notifications", "watch again", "remix with this sound", "sound used in shorts",
  "original sound", "send in messenger", "favorite", "favourite"
)

// ===================================================================================
// 1. INSTAGRAM REELS DETECTOR (com.instagram.android)
// ===================================================================================
class InstagramReelsDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.instagram.android")
  override val appType: ShortAppType = ShortAppType.INSTAGRAM

  private val reelIndicatorIds = setOf(
    "clips",
    "reel",
    "video_container",
    "clips_viewer",
    "reel_viewer",
    "clips_item",
    "clips_overlay_container",
    "reel_viewer_title",
    "clips_action_bar",
    "clips_author_name",
    "clips_audio_title",
    "reel_swipe_area",
    "media_view",
    "like_button",
    "comment_button",
    "share_button"
  )

  private val reelKeywords = setOf(
    "reels",
    "reel by",
    "original audio",
    "use audio",
    "remix",
    "share reel",
    "watch reels",
    "watch again",
    "trending audio"
  )

  private val negativeIndicatorIds = setOf(
    "story_progress_bar",
    "story_viewer",
    "row_feed_photo_profile_name",
    "direct_thread",
    "direct_message",
    "message_composer",
    "edit_profile",
    "profile_tab",
    "user_profile",
    "row_feed_button_like"
  )

  override fun detect(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): DetectionResult {
    val pkg = eventPackage?.toString() ?: ""
    if (rootNode == null || pkg != "com.instagram.android") {
      return DetectionResult(DetectionConfidence.LOW, appType, emptyList(), 0)
    }

    val signals = mutableListOf<String>()
    var score = 0
    val snapshots = collectNodeSnapshots(rootNode)

    // 1. Check Negative Indicators (Normal Feed post, Story viewer, Chat)
    for (node in snapshots) {
      val vid = node.viewId ?: ""
      if (negativeIndicatorIds.any { vid.contains(it, ignoreCase = true) }) {
        signals.add("negative_id:$vid")
        score -= 40
        break
      }
      if (node.combinedLabel.contains("send message") || node.combinedLabel.contains("reply to story")) {
        signals.add("negative_story_reply")
        score -= 40
        break
      }
    }

    // Check if Like & Comment are arranged horizontally (Normal Feed post hallmark)
    if (hasHorizontalActionPair(snapshots, "like", "comment")) {
      signals.add("negative_horizontal_feed_buttons")
      score -= 35
    }

    // 2. Class / Activity Name Signals
    val classNameStr = eventClassName?.toString() ?: ""
    if (classNameStr.contains("Clips", ignoreCase = true) ||
      classNameStr.contains("Reel", ignoreCase = true) ||
      classNameStr.contains("ClipsViewer", ignoreCase = true)
    ) {
      signals.add("class:$classNameStr")
      score += 35
    }

    // 3. View Resource IDs
    for (node in snapshots) {
      val vid = node.viewId ?: continue
      for (indicator in reelIndicatorIds) {
        if (vid.contains(indicator, ignoreCase = true)) {
          signals.add("id:$vid")
          score += 30
          break
        }
      }
      if (signals.any { it.startsWith("id:") }) break
    }

    // 4. Keyword & Header Signals
    for (node in snapshots) {
      val label = node.combinedLabel
      for (kw in reelKeywords) {
        if (label == kw || label.startsWith("$kw ") || label.contains(" $kw ")) {
          signals.add("keyword:$kw")
          score += 25
          break
        }
      }
      if (signals.any { it.startsWith("keyword:") }) break
    }

    // 5. Vertical Action Rail Cluster (Like + Comment + Share + Follow/Audio in right rail)
    val hasLike = snapshots.any { it.combinedLabel.contains("like") }
    val hasComment = snapshots.any { it.combinedLabel.contains("comment") }
    val hasShare = snapshots.any { it.combinedLabel.contains("share") || it.combinedLabel.contains("send") }
    val hasFollowOrAudio = snapshots.any {
      it.combinedLabel.contains("follow") ||
        it.combinedLabel.contains("audio") ||
        it.combinedLabel.contains("remix")
    }

    if (hasLike && hasComment) {
      score += 15
      signals.add("cluster:like_comment")
      if (hasShare) {
        score += 10
        signals.add("cluster:share")
      }
      if (hasFollowOrAudio) {
        score += 15
        signals.add("cluster:follow_or_audio")
      }

      // Check vertical right-rail alignment
      if (hasVerticalActionStack(snapshots, "like", "comment")) {
        score += 20
        signals.add("vertical_rail_aligned")
      }
    }

    val confidence = when {
      score >= 50 -> DetectionConfidence.HIGH
      score in 25..49 -> DetectionConfidence.MEDIUM
      else -> DetectionConfidence.LOW
    }

    return DetectionResult(confidence, appType, signals, score)
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("ig_" + tokens.joinToString("::"))
      } else {
        val boundsToken = extractRobustContainerSignature(rootNode, "ig")
        val seq = System.currentTimeMillis() / 2500L
        if (boundsToken != null) {
          "ig_seq_${seq}_${boundsToken.hashCode()}"
        } else {
          "ig_seq_$seq"
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

// ===================================================================================
// 2. FACEBOOK REELS DETECTOR (com.facebook.katana, com.facebook.lite)
// ===================================================================================
class FacebookReelsDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.facebook.katana", "com.facebook.lite")
  override val appType: ShortAppType = ShortAppType.FACEBOOK

  private val fbReelsIndicatorIds = setOf(
    "fb_reels",
    "watch_reel",
    "reels_viewer_root",
    "reels_video_view",
    "reel_container",
    "fb_reels_container",
    "reels_action_bar",
    "reels_pill"
  )

  private val fbReelsKeywords = setOf(
    "reels",
    "reel",
    "watch more reels",
    "original audio",
    "remix",
    "send in messenger",
    "watch reel"
  )

  private val negativeIndicatorIds = setOf(
    "feed_story_message",
    "feed_story_attachment",
    "news_feed_stream",
    "messenger_thread",
    "marketplace_tab",
    "group_feed"
  )

  override fun detect(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): DetectionResult {
    val pkg = eventPackage?.toString() ?: ""
    if (rootNode == null || (!pkg.startsWith("com.facebook.katana") && !pkg.startsWith("com.facebook.lite"))) {
      return DetectionResult(DetectionConfidence.LOW, appType, emptyList(), 0)
    }

    val signals = mutableListOf<String>()
    var score = 0
    val snapshots = collectNodeSnapshots(rootNode)

    // 1. Negative Signals (Newsfeed, Messenger, Marketplace)
    for (node in snapshots) {
      val vid = node.viewId ?: ""
      if (negativeIndicatorIds.any { vid.contains(it, ignoreCase = true) }) {
        signals.add("negative_id:$vid")
        score -= 40
        break
      }
    }

    if (hasHorizontalActionPair(snapshots, "like", "comment")) {
      signals.add("negative_horizontal_feed_buttons")
      score -= 35
    }

    // 2. Class / Activity
    val classNameStr = eventClassName?.toString() ?: ""
    if (classNameStr.contains("Reel", ignoreCase = true) ||
      classNameStr.contains("FbReels", ignoreCase = true) ||
      classNameStr.contains("ReelsViewer", ignoreCase = true)
    ) {
      signals.add("class:$classNameStr")
      score += 35
    }

    // 3. View Resource IDs
    for (node in snapshots) {
      val vid = node.viewId ?: continue
      for (indicator in fbReelsIndicatorIds) {
        if (vid.contains(indicator, ignoreCase = true)) {
          signals.add("id:$vid")
          score += 30
          break
        }
      }
      if (signals.any { it.startsWith("id:") }) break
    }

    // 4. Keywords
    for (node in snapshots) {
      val label = node.combinedLabel
      for (kw in fbReelsKeywords) {
        if (label == kw || label.startsWith("$kw ") || label.contains(" $kw ")) {
          signals.add("keyword:$kw")
          score += 25
          break
        }
      }
      if (signals.any { it.startsWith("keyword:") }) break
    }

    // 5. Interaction Cluster
    val hasLike = snapshots.any { it.combinedLabel.contains("like") }
    val hasComment = snapshots.any { it.combinedLabel.contains("comment") }
    val hasShare = snapshots.any { it.combinedLabel.contains("share") || it.combinedLabel.contains("send") }
    val hasFollowOrAudio = snapshots.any {
      it.combinedLabel.contains("follow") ||
        it.combinedLabel.contains("original audio") ||
        it.combinedLabel.contains("remix")
    }

    if (hasLike && hasComment) {
      score += 15
      signals.add("cluster:like_comment")
      if (hasShare) {
        score += 10
        signals.add("cluster:share")
      }
      if (hasFollowOrAudio) {
        score += 15
        signals.add("cluster:follow_audio")
      }
      if (hasVerticalActionStack(snapshots, "like", "comment")) {
        score += 20
        signals.add("vertical_rail_aligned")
      }
    }

    val confidence = when {
      score >= 50 -> DetectionConfidence.HIGH
      score in 25..49 -> DetectionConfidence.MEDIUM
      else -> DetectionConfidence.LOW
    }

    return DetectionResult(confidence, appType, signals, score)
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("fb_" + tokens.joinToString("::"))
      } else {
        val boundsToken = extractRobustContainerSignature(rootNode, "fb")
        val seq = System.currentTimeMillis() / 2500L
        if (boundsToken != null) {
          "fb_seq_${seq}_${boundsToken.hashCode()}"
        } else {
          "fb_seq_$seq"
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

// ===================================================================================
// 3. YOUTUBE SHORTS DETECTOR (com.google.android.youtube)
// ===================================================================================
class YouTubeShortsDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.google.android.youtube")
  override val appType: ShortAppType = ShortAppType.YOUTUBE

  private val shortsIndicatorIds = setOf(
    "shorts",
    "reel",
    "reel_recycler_view",
    "shorts_player",
    "reel_player_page_container",
    "shorts_container",
    "shorts_video_view",
    "reel_player_view",
    "reel_channel_name",
    "reel_player_title_text",
    "reel_right_action_bar"
  )

  private val shortsKeywords = setOf(
    "shorts",
    "dislike this video",
    "sound used in shorts",
    "use this sound",
    "remix with this sound",
    "remix"
  )

  private val regularPlayerNegativeIds = setOf(
    "time_bar",
    "player_control",
    "progress_bar",
    "time_current",
    "time_duration",
    "watch_next_list",
    "related_videos",
    "channel_tabs",
    "search_results"
  )

  override fun detect(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): DetectionResult {
    val pkg = eventPackage?.toString() ?: ""
    if (rootNode == null || pkg != "com.google.android.youtube") {
      return DetectionResult(DetectionConfidence.LOW, appType, emptyList(), 0)
    }

    val signals = mutableListOf<String>()
    var score = 0
    val snapshots = collectNodeSnapshots(rootNode)

    // 1. Negative Signals: Standard YouTube Video Player (Seek bar, time scrubber, watch next)
    for (node in snapshots) {
      val vid = node.viewId ?: ""
      if (regularPlayerNegativeIds.any { vid.contains(it, ignoreCase = true) }) {
        signals.add("negative_id:$vid")
        score -= 40
        break
      }
    }

    // 2. Class / Activity Name
    val classNameStr = eventClassName?.toString() ?: ""
    if (classNameStr.contains("Shorts", ignoreCase = true) ||
      classNameStr.contains("ReelPlayer", ignoreCase = true) ||
      classNameStr.contains("ShortsActivity", ignoreCase = true)
    ) {
      signals.add("class:$classNameStr")
      score += 35
    }

    // 3. View Resource IDs
    for (node in snapshots) {
      val vid = node.viewId ?: continue
      for (indicator in shortsIndicatorIds) {
        if (vid.contains(indicator, ignoreCase = true)) {
          signals.add("id:$vid")
          score += 30
          break
        }
      }
      if (signals.any { it.startsWith("id:") }) break
    }

    // 4. Shorts-Specific Keywords
    for (node in snapshots) {
      val label = node.combinedLabel
      for (kw in shortsKeywords) {
        if (label == kw || label.startsWith("$kw ") || label.contains(" $kw ")) {
          signals.add("keyword:$kw")
          score += 25
          break
        }
      }
      if (signals.any { it.startsWith("keyword:") }) break
    }

    // 5. Shorts Distinctive Action Cluster:
    // In Shorts, Dislike is vertically below Like on the right side!
    val hasLike = snapshots.any { it.combinedLabel.contains("like") }
    val hasDislike = snapshots.any { it.combinedLabel.contains("dislike") }
    val hasComment = snapshots.any { it.combinedLabel.contains("comment") }
    val hasRemixOrSound = snapshots.any {
      it.combinedLabel.contains("remix") ||
        it.combinedLabel.contains("sound") ||
        it.combinedLabel.contains("subscribe")
    }

    if (hasLike && hasDislike) {
      score += 25
      signals.add("cluster:like_dislike")
      if (hasComment) {
        score += 15
        signals.add("cluster:comments")
      }
      if (hasRemixOrSound) {
        score += 15
        signals.add("cluster:remix_sound")
      }
      // Check vertical stacking of Like -> Dislike on the right side
      if (hasVerticalActionStack(snapshots, "like", "dislike")) {
        score += 20
        signals.add("vertical_like_dislike_stack")
      }
    } else if (hasLike && hasComment) {
      score += 10
      signals.add("cluster:like_comment_partial")
    }

    val confidence = when {
      score >= 50 -> DetectionConfidence.HIGH
      score in 25..49 -> DetectionConfidence.MEDIUM
      else -> DetectionConfidence.LOW
    }

    return DetectionResult(confidence, appType, signals, score)
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("yt_" + tokens.joinToString("::"))
      } else {
        val boundsToken = extractRobustContainerSignature(rootNode, "yt")
        val seq = System.currentTimeMillis() / 2500L
        if (boundsToken != null) {
          "yt_seq_${seq}_${boundsToken.hashCode()}"
        } else {
          "yt_seq_$seq"
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

// ===================================================================================
// 4. SNAPCHAT SPOTLIGHT DETECTOR (com.snapchat.android)
// ===================================================================================
class SnapchatSpotlightDetector : ShortAppDetector {
  override val targetPackages: Set<String> = setOf("com.snapchat.android")
  override val appType: ShortAppType = ShortAppType.SNAPCHAT

  private val spotlightIndicatorIds = setOf(
    "spotlight",
    "spotlight_carousel",
    "spotlight_viewer",
    "spotlight_feed",
    "spotlight_action_menu",
    "spotlight_video_player",
    "subscribe_button",
    "favorite_button",
    "share_button"
  )

  private val spotlightKeywords = setOf(
    "spotlight",
    "original sound",
    "sound",
    "subscribe",
    "remix",
    "favorite"
  )

  private val negativeIndicatorIds = setOf(
    "chat_input",
    "send_chat",
    "chat_message_list",
    "camera_shutter",
    "lens_carousel",
    "camera_flip",
    "capture_button",
    "story_grid",
    "discover_feed",
    "bitmoji_avatar",
    "settings_gear"
  )

  override fun detect(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?,
    eventClassName: CharSequence?
  ): DetectionResult {
    val pkg = eventPackage?.toString() ?: ""
    if (rootNode == null || pkg != "com.snapchat.android") {
      return DetectionResult(DetectionConfidence.LOW, appType, emptyList(), 0)
    }

    val signals = mutableListOf<String>()
    var score = 0
    val snapshots = collectNodeSnapshots(rootNode)

    // 1. Negative Signals (Chat, Camera, Stories grid)
    for (node in snapshots) {
      val vid = node.viewId ?: ""
      if (negativeIndicatorIds.any { vid.contains(it, ignoreCase = true) }) {
        signals.add("negative_id:$vid")
        score -= 40
        break
      }
      if (node.combinedLabel.contains("send a chat") || node.combinedLabel.contains("tap to chat")) {
        signals.add("negative_chat_composer")
        score -= 40
        break
      }
    }

    // 2. Class / Activity
    val classNameStr = eventClassName?.toString() ?: ""
    if (classNameStr.contains("Spotlight", ignoreCase = true) ||
      classNameStr.contains("SpotlightFeed", ignoreCase = true) ||
      classNameStr.contains("SpotlightViewer", ignoreCase = true)
    ) {
      signals.add("class:$classNameStr")
      score += 35
    }

    // 3. View Resource IDs
    for (node in snapshots) {
      val vid = node.viewId ?: continue
      for (indicator in spotlightIndicatorIds) {
        if (vid.contains(indicator, ignoreCase = true)) {
          signals.add("id:$vid")
          score += 30
          break
        }
      }
      if (signals.any { it.startsWith("id:") }) break
    }

    // 4. Keywords
    for (node in snapshots) {
      val label = node.combinedLabel
      for (kw in spotlightKeywords) {
        if (label == kw || label.startsWith("$kw ") || label.contains(" $kw ")) {
          signals.add("keyword:$kw")
          score += 25
          break
        }
      }
      if (signals.any { it.startsWith("keyword:") }) break
    }

    // 5. Interaction Cluster: Heart/Favorite + Comment + Share + (Subscribe OR Sound)
    val hasLike = snapshots.any { it.combinedLabel.contains("like") || it.combinedLabel.contains("favorite") }
    val hasComment = snapshots.any { it.combinedLabel.contains("comment") || it.combinedLabel.contains("remix") }
    val hasShare = snapshots.any { it.combinedLabel.contains("share") || it.combinedLabel.contains("send") }
    val hasSubscribeOrSound = snapshots.any {
      it.combinedLabel.contains("subscribe") ||
        it.combinedLabel.contains("original sound") ||
        it.combinedLabel.contains("spotlight")
    }

    if (hasLike && (hasComment || hasShare)) {
      score += 20
      signals.add("cluster:spotlight_actions")
      if (hasSubscribeOrSound) {
        score += 20
        signals.add("cluster:subscribe_or_sound")
      }
      if (hasVerticalActionStack(snapshots, "like", "share") || hasVerticalActionStack(snapshots, "favorite", "share")) {
        score += 20
        signals.add("vertical_rail_aligned")
      }
    }

    val confidence = when {
      score >= 50 -> DetectionConfidence.HIGH
      score in 25..49 -> DetectionConfidence.MEDIUM
      else -> DetectionConfidence.LOW
    }

    return DetectionResult(confidence, appType, signals, score)
  }

  override fun extractVideoSignature(
    rootNode: AccessibilityNodeInfo?,
    eventPackage: CharSequence?
  ): String? {
    if (rootNode == null) return null
    return try {
      val tokens = mutableListOf<String>()
      collectDistinctPublicTokens(
        node = rootNode,
        outTokens = tokens,
        maxTokens = 4,
        depth = 0,
        maxDepth = 15
      )

      if (tokens.isNotEmpty()) {
        hashSignature("sc_" + tokens.joinToString("::"))
      } else {
        val boundsToken = extractRobustContainerSignature(rootNode, "sc")
        val seq = System.currentTimeMillis() / 2500L
        if (boundsToken != null) {
          "sc_seq_${seq}_${boundsToken.hashCode()}"
        } else {
          "sc_seq_$seq"
        }
      }
    } catch (_: Exception) {
      null
    }
  }
}

// ===================================================================================
// REGISTRY HOLDING ALL 4 ACTIVE DETECTORS
// ===================================================================================
object ShortAppRegistry {
  private val detectors = mutableListOf<ShortAppDetector>(
    InstagramReelsDetector(),
    YouTubeShortsDetector(),
    FacebookReelsDetector(),
    SnapchatSpotlightDetector()
  )

  fun registerDetector(detector: ShortAppDetector) {
    if (!detectors.contains(detector)) {
      detectors.add(detector)
    }
  }

  fun getDetectorForPackage(packageName: String?): ShortAppDetector? {
    if (packageName == null) return null
    return detectors.firstOrNull { it.targetPackages.contains(packageName) }
  }

  fun getAllSupportedPackages(): Set<String> {
    return detectors.flatMap { it.targetPackages }.toSet()
  }
}

// ===================================================================================
// STRUCTURAL & GEOMETRY HELPERS
// ===================================================================================

/**
 * Traverses accessibility hierarchy up to maxDepth and maxNodes, collecting lightweight snapshots.
 */
internal fun collectNodeSnapshots(
  root: AccessibilityNodeInfo?,
  maxDepth: Int = 15,
  maxNodes: Int = 120
): List<AccessibilityNodeSnapshot> {
  if (root == null) return emptyList()
  val list = mutableListOf<AccessibilityNodeSnapshot>()

  fun visit(node: AccessibilityNodeInfo?, depth: Int) {
    if (node == null || depth > maxDepth || list.size >= maxNodes) return
    try {
      // Privacy check: Skip password and input fields
      if (node.isPassword || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
        return
      }

      val rect = Rect()
      node.getBoundsInScreen(rect)

      val snapshot = AccessibilityNodeSnapshot(
        className = node.className?.toString(),
        viewId = node.viewIdResourceName,
        text = node.text?.toString(),
        contentDescription = node.contentDescription?.toString(),
        bounds = rect,
        isClickable = node.isClickable
      )
      list.add(snapshot)

      for (i in 0 until node.childCount) {
        if (list.size >= maxNodes) break
        visit(node.getChild(i), depth + 1)
      }
    } catch (_: Exception) {}
  }

  visit(root, 0)
  return list
}

/**
 * Checks if two action buttons are stacked vertically (characteristic of Reels/Shorts right rail),
 * rather than horizontally. Uses normalized relative geometry tolerant of screen sizes, DPI, and scaling.
 */
internal fun hasVerticalActionStack(
  nodes: List<AccessibilityNodeSnapshot>,
  labelA: String,
  labelB: String
): Boolean {
  val nodeA = nodes.firstOrNull { it.combinedLabel.contains(labelA) } ?: return false
  val nodeB = nodes.firstOrNull { it.combinedLabel.contains(labelB) } ?: return false

  val rectA = nodeA.bounds
  val rectB = nodeB.bounds

  // If bounds are unmeasured or dummy, fall back safely
  if (rectA.isEmpty || rectB.isEmpty) return true

  val refHeight = Math.max(rectA.height(), rectB.height()).coerceAtLeast(30)
  val refWidth = Math.max(rectA.width(), rectB.width()).coerceAtLeast(30)

  val verticalSeparation = Math.abs(rectA.centerY() - rectB.centerY())
  val horizontalSeparation = Math.abs(rectA.centerX() - rectB.centerX())

  // In vertical stack: vertically separated by at least 40% of button height,
  // and horizontally aligned within 2.5x button width.
  return verticalSeparation >= (refHeight * 0.4f) && horizontalSeparation <= (refWidth * 2.5f)
}

/**
 * Checks if two action buttons are aligned horizontally side-by-side (characteristic of Normal Feed Posts).
 * Uses normalized relative geometry.
 */
internal fun hasHorizontalActionPair(
  nodes: List<AccessibilityNodeSnapshot>,
  labelA: String,
  labelB: String
): Boolean {
  val nodeA = nodes.firstOrNull { it.combinedLabel.contains(labelA) } ?: return false
  val nodeB = nodes.firstOrNull { it.combinedLabel.contains(labelB) } ?: return false

  val rectA = nodeA.bounds
  val rectB = nodeB.bounds

  if (rectA.isEmpty || rectB.isEmpty) return false

  val refHeight = Math.max(rectA.height(), rectB.height()).coerceAtLeast(30)
  val refWidth = Math.max(rectA.width(), rectB.width()).coerceAtLeast(30)

  val verticalDiff = Math.abs(rectA.centerY() - rectB.centerY())
  val horizontalDiff = Math.abs(rectA.centerX() - rectB.centerX())

  // In normal feed cards, Like and Comment buttons sit on the same horizontal row
  return verticalDiff <= (refHeight * 0.6f) && horizontalDiff >= (refWidth * 0.5f)
}

/**
 * Helper to safely find any matching view resource ID in the accessibility node hierarchy.
 */
internal fun findAnyMatchingId(
  node: AccessibilityNodeInfo?,
  targetIds: Set<String>,
  depth: Int = 0,
  maxDepth: Int = 15
): Boolean {
  if (node == null || depth > maxDepth) return false
  try {
    val resName = node.viewIdResourceName
    if (resName != null) {
      for (target in targetIds) {
        if (resName.contains(target, ignoreCase = true)) {
          return true
        }
      }
    }
    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      if (findAnyMatchingId(child, targetIds, depth + 1, maxDepth)) {
        return true
      }
    }
  } catch (_: Exception) {
    return false
  }
  return false
}

/**
 * Helper to check text or contentDescription keywords in the node hierarchy.
 */
internal fun findAnyMatchingContentOrText(
  node: AccessibilityNodeInfo?,
  keywords: Set<String>,
  depth: Int = 0,
  maxDepth: Int = 15
): Boolean {
  if (node == null || depth > maxDepth) return false
  try {
    val text = node.text?.toString()?.lowercase()
    val desc = node.contentDescription?.toString()?.lowercase()

    for (kw in keywords) {
      if ((text != null && text.contains(kw)) || (desc != null && desc.contains(kw))) {
        return true
      }
    }

    for (i in 0 until node.childCount) {
      val child = node.getChild(i) ?: continue
      if (findAnyMatchingContentOrText(child, keywords, depth + 1, maxDepth)) {
        return true
      }
    }
  } catch (_: Exception) {
    return false
  }
  return false
}

/**
 * Checks for co-presence of distinct action buttons that signify a full-screen vertical player.
 */
internal fun hasActionCluster(
  node: AccessibilityNodeInfo?,
  actionGroupA: Set<String>,
  actionGroupB: Set<String>,
  depth: Int = 0,
  maxDepth: Int = 15
): Boolean {
  var foundA = false
  var foundB = false

  fun traverse(curr: AccessibilityNodeInfo?, d: Int) {
    if (curr == null || d > maxDepth || (foundA && foundB)) return
    try {
      val text = curr.text?.toString()?.lowercase()
      val desc = curr.contentDescription?.toString()?.lowercase()

      if (!foundA) {
        for (a in actionGroupA) {
          if ((text != null && text.contains(a)) || (desc != null && desc.contains(a))) {
            foundA = true
            break
          }
        }
      }

      if (!foundB) {
        for (b in actionGroupB) {
          if ((text != null && text.contains(b)) || (desc != null && desc.contains(b))) {
            foundB = true
            break
          }
        }
      }

      for (i in 0 until curr.childCount) {
        traverse(curr.getChild(i), d + 1)
      }
    } catch (_: Exception) {}
  }

  traverse(node, depth)
  return foundA && foundB
}

/**
 * Recursively collects public, non-sensitive text strings (e.g. channel name, video title, song)
 * strictly ignoring any passwords, input fields, or generic UI buttons.
 */
internal fun collectDistinctPublicTokens(
  node: AccessibilityNodeInfo?,
  outTokens: MutableList<String>,
  maxTokens: Int = 4,
  depth: Int = 0,
  maxDepth: Int = 15
) {
  if (node == null || depth > maxDepth || outTokens.size >= maxTokens) return

  try {
    // SECURITY & PRIVACY: Absolutely skip password or editable text nodes
    if (node.isPassword || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
      return
    }

    val text = node.text?.toString()?.trim()
    val desc = node.contentDescription?.toString()?.trim()

    val candidate = when {
      !text.isNullOrBlank() && text.length in 2..120 -> text
      !desc.isNullOrBlank() && desc.length in 2..120 -> desc
      else -> null
    }

    if (candidate != null) {
      val lower = candidate.lowercase()
      val isGenericAction = GENERIC_UI_WORDS.any { lower == it || lower.startsWith("$it ") }
      if (!isGenericAction && !outTokens.contains(candidate)) {
        outTokens.add(candidate)
      }
    }

    for (i in 0 until node.childCount) {
      if (outTokens.size >= maxTokens) break
      val child = node.getChild(i) ?: continue
      collectDistinctPublicTokens(child, outTokens, maxTokens, depth + 1, maxDepth)
    }
  } catch (_: Exception) {
    // Safe graceful handling on rapid UI refresh
  }
}

/**
 * Extracts robust container signature based on geometry, child count, and layout structure.
 */
internal fun extractRobustContainerSignature(
  node: AccessibilityNodeInfo?,
  prefix: String
): String? {
  if (node == null) return null
  return try {
    val rect = Rect()
    node.getBoundsInScreen(rect)

    val childCount = node.childCount
    val hash = node.className?.toString().hashCode() xor (rect.width() * 31 + rect.height())
    "${prefix}_${rect.left}_${rect.top}_${rect.width()}x${rect.height()}_${childCount}_$hash"
  } catch (_: Exception) {
    null
  }
}

/**
 * Generate a short SHA-256 hash string for video signatures to avoid storing raw element data.
 */
internal fun hashSignature(input: String): String {
  return try {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray())
    digest.take(8).joinToString("") { "%02x".format(it) }
  } catch (_: Exception) {
    input.hashCode().toString()
  }
}
