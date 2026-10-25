package com.example.focus

import android.content.Context
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Dedicated Browser URL Bar & Visible Text Hierarchy Extractor.
 *
 * Fully covers major Android browsers:
 * - Google Chrome & Chromium (`com.android.chrome`)
 * - Brave Browser (`com.brave.browser`)
 * - Mozilla Firefox & Focus (`org.mozilla.firefox`, `org.mozilla.focus`)
 * - Samsung Internet (`com.sec.android.app.sbrowser`, `com.sec.android.app.sbrowser.beta`)
 * - Opera, Opera Mini, Opera Touch (`com.opera.browser`, `com.opera.mini.native`, `com.opera.touch`)
 * - Microsoft Edge (`com.microsoft.emmx`)
 * - DuckDuckGo (`com.duckduckgo.mobile.android`)
 * - Vivaldi (`com.vivaldi.browser`)
 * - UC Browser & Mi Browser (`com.UCMobile.intl`, `com.mi.globalbrowser`)
 * - JioPages (`com.jio.web`)
 *
 * Extracts both the address bar input / search query and the visible page content text.
 */
object BrowserUrlExtractor {

  // Known browser package names
  val BROWSER_PACKAGES = hashSetOf(
    "com.android.chrome",
    "com.brave.browser",
    "org.mozilla.firefox",
    "org.mozilla.focus",
    "com.sec.android.app.sbrowser",
    "com.sec.android.app.sbrowser.beta",
    "com.opera.browser",
    "com.opera.mini.native",
    "com.opera.touch",
    "com.opera.gx",
    "com.microsoft.emmx",
    "com.duckduckgo.mobile.android",
    "com.vivaldi.browser",
    "com.UCMobile.intl",
    "com.mi.globalbrowser",
    "com.jio.web",
    "com.kiwibrowser.browser",
    "com.ecosia.android",
    "com.yandex.browser"
  )

  // Known address bar view IDs across browsers
  private val URL_BAR_VIEW_IDS = listOf(
    "url_bar",
    "search_box_text",
    "location_bar",
    "location_bar_edit_text",
    "mozac_browser_toolbar_url_view",
    "toolbar_url",
    "url_bar_title",
    "url_field",
    "address_bar",
    "omnibarTextInput",
    "search_hint",
    "url_text",
    "input_url",
    "toolbar_edit_text"
  )

  fun isBrowser(packageName: String): Boolean {
    if (packageName.isBlank()) return false
    return BROWSER_PACKAGES.contains(packageName) ||
      packageName.contains("browser", ignoreCase = true) ||
      packageName.contains("chrome", ignoreCase = true)
  }

  /**
   * Attempts to locate the browser address bar / search input specifically.
   */
  fun extractUrlBarText(root: AccessibilityNodeInfo?): String? {
    if (root == null) return null

    // 1. Check known resource view IDs
    for (idSubstring in URL_BAR_VIEW_IDS) {
      val foundNodes = try {
        root.findAccessibilityNodeInfosByViewId(idSubstring)
      } catch (_: Exception) {
        null
      }
      if (!foundNodes.isNullOrEmpty()) {
        for (node in foundNodes) {
          val text = node.text?.toString()
          if (!text.isNullOrBlank()) {
            return text
          }
        }
      }
    }

    // 2. Fallback: Search for editable text node or text containing common URL markers
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(root)
    var visited = 0

    while (queue.isNotEmpty() && visited < 45) {
      val current = queue.removeFirst()
      visited++

      val viewId = current.viewIdResourceName?.lowercase() ?: ""
      val isUrlField = URL_BAR_VIEW_IDS.any { viewId.contains(it) }

      val text = current.text?.toString()
      if (!text.isNullOrBlank()) {
        if (isUrlField) {
          return text
        }
        if (current.isEditable && (text.contains(".") || text.contains("/") || text.contains("http"))) {
          return text
        }
      }

      val childCount = current.childCount
      for (i in 0 until childCount) {
        val child = current.getChild(i)
        if (child != null) {
          queue.add(child)
        }
      }
    }

    return null
  }

  /**
   * Collects all visible text chunks from the browser window (page titles, headers, search results).
   */
  fun collectVisibleTextChunks(root: AccessibilityNodeInfo?, maxNodes: Int = 60): List<String> {
    if (root == null) return emptyList()

    val chunks = mutableListOf<String>()
    val queue = ArrayDeque<AccessibilityNodeInfo>()
    queue.add(root)
    var visited = 0

    while (queue.isNotEmpty() && visited < maxNodes) {
      val current = queue.removeFirst()
      visited++

      val text = current.text?.toString()
      if (!text.isNullOrBlank() && text.length >= 3) {
        chunks.add(text)
      }

      val desc = current.contentDescription?.toString()
      if (!desc.isNullOrBlank() && desc.length >= 3) {
        chunks.add(desc)
      }

      val childCount = current.childCount
      for (i in 0 until childCount) {
        val child = current.getChild(i)
        if (child != null) {
          queue.add(child)
        }
      }
    }

    return chunks
  }

  /**
   * Keywords and view identifiers that indicate Incognito / Private Tab modes or tab-switching
   * across Google Chrome, Brave, Firefox, Samsung Internet, Opera, Edge, etc.
   * Hindi, English, and Hinglish variants included.
   */
  private val INCOGNITO_KEYWORDS = listOf(
    // English
    "new incognito tab", "incognito tab", "new private tab", "private tab",
    "inprivate tab", "secret mode", "turn on secret mode",
    // Hindi & Hinglish
    "नया गुप्त टैब", "गुप्त टैब", "प्राइवेट टैब", "गुप्त मोड", "सीक्रेट टैब"
  )

  /**
   * Checks whether the current accessibility event or node hierarchy represents
   * an attempt to open Incognito/Private tabs in browsers.
   * Standard tab switching, new standard tabs, and web browsing are NEVER blocked!
   */
  fun isIncognitoOrTabAction(
    packageName: String,
    eventText: String,
    contentDescription: String,
    node: AccessibilityNodeInfo?
  ): Boolean {
    if (!isBrowser(packageName)) return false

    val textToCheck = "$eventText $contentDescription".lowercase()
    for (kw in INCOGNITO_KEYWORDS) {
      if (textToCheck.contains(kw)) {
        return true
      }
    }

    // Inspect child hierarchy for explicit Incognito buttons or menus
    if (node != null) {
      val queue = ArrayDeque<AccessibilityNodeInfo>()
      queue.add(node)
      var visited = 0
      while (queue.isNotEmpty() && visited < 30) {
        val cur = queue.removeFirst()
        visited++

        val viewId = cur.viewIdResourceName?.lowercase() ?: ""
        if (viewId.contains("incognito") || viewId.contains("inprivate") || viewId.contains("secret_mode")) {
          return true
        }

        val nodeText = cur.text?.toString()?.lowercase() ?: ""
        val nodeDesc = cur.contentDescription?.toString()?.lowercase() ?: ""
        for (kw in INCOGNITO_KEYWORDS) {
          if (nodeText.contains(kw) || nodeDesc.contains(kw)) {
            return true
          }
        }

        for (i in 0 until cur.childCount) {
          cur.getChild(i)?.let { queue.add(it) }
        }
      }
    }

    return false
  }

  private const val PREFS_NAME = "browser_incognito_block_prefs"
  private const val KEY_INCOGNITO_BLOCK_ENABLED = "incognito_block_enabled"

  fun isIncognitoBlockingEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_INCOGNITO_BLOCK_ENABLED, true)
  }

  fun setIncognitoBlockingEnabled(context: Context, enabled: Boolean, pinVerified: Boolean = false): Boolean {
    if (!enabled && !pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        return false
      }
    }
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_INCOGNITO_BLOCK_ENABLED, enabled).apply()
    return true
  }
}

