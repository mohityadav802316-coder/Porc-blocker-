package com.example.focus

import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LifeTrackerDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StrictLockAndAdultContentTest {

  // =========================================================================
  // 1. ADULT CONTENT DETECTOR: ON-DEVICE & OFFLINE DETECTION
  // =========================================================================

  @Test
  fun `verify adult domain detection across browsers`() {
    val result1 = AdultContentDetector.checkText("https://www.pornhub.com/view_video", "com.android.chrome")
    assertNotNull(result1)
    assertTrue(result1!!.isAdult)
    assertEquals("pornhub", result1.matchedTerm)

    val result2 = AdultContentDetector.checkText("xvideos.com/video123", "org.mozilla.firefox")
    assertNotNull(result2)
    assertTrue(result2!!.isAdult)
    assertEquals("xvideos", result2.matchedTerm)

    val result3 = AdultContentDetector.checkText("m.xnxx.com/search/video", "com.brave.browser")
    assertNotNull(result3)
    assertTrue(result3!!.isAdult)
    assertEquals("xnxx", result3.matchedTerm)
  }

  @Test
  fun `verify explicit adult keywords in English, Hindi, and Hinglish`() {
    // English
    val enResult = AdultContentDetector.checkText("Watch trending porn clips online", "com.android.chrome")
    assertNotNull(enResult)
    assertTrue(enResult!!.isAdult)
    assertEquals("porn", enResult.matchedTerm)

    // Hindi (Devanagari)
    val hiResult1 = AdultContentDetector.checkText("नई हिंदी पोर्न क्लिप", "com.android.chrome")
    assertNotNull(hiResult1)
    assertTrue(hiResult1!!.isAdult)
    assertEquals("पोर्न", hiResult1.matchedTerm)

    val hiResult2 = AdultContentDetector.checkText("देसी ब्लूफिल्म वीडियो", "com.android.chrome")
    assertNotNull(hiResult2)
    assertTrue(hiResult2!!.isAdult)
    assertEquals("ब्लूफिल्म", hiResult2.matchedTerm)

    // Hinglish phrases
    val hinglishResult = AdultContentDetector.checkText("viral chudai video full hd", "com.android.chrome")
    assertNotNull(hinglishResult)
    assertTrue(hinglishResult!!.isAdult)
    assertEquals("chudai", hinglishResult.matchedTerm)
  }

  @Test
  fun `verify leetspeak obfuscation normalization`() {
    val leetResult = AdultContentDetector.checkText("search for p0rn videos", "com.android.chrome")
    assertNotNull(leetResult)
    assertTrue(leetResult!!.isAdult)
    assertEquals("porn", leetResult.matchedTerm)
  }

  @Test
  fun `verify false-positive protection for educational and geographical terms`() {
    // Educational / Medical / Whitelist
    assertNull("Sex education must NOT be detected as adult content",
      AdultContentDetector.checkText("High school sex education curriculum", "com.android.chrome")
    )

    assertNull("County of Essex must NOT be detected as adult content",
      AdultContentDetector.checkText("Visiting university of Essex in UK", "com.android.chrome")
    )

    assertNull("Gender and sex ratio must NOT be detected as adult content",
      AdultContentDetector.checkText("Census study on national sex ratio and gender", "com.android.chrome")
    )

    assertNull("Biology chromosome must NOT be detected as adult content",
      AdultContentDetector.checkText("Biology lecture on human sex chromosome genetics", "com.android.chrome")
    )
  }

  // =========================================================================
  // 2. STRICT LOCK ACTIVATION & ENFORCEMENT
  // =========================================================================

  @Test
  fun `verify Strict Lock activates for 15 minutes and cannot be cancelled or deactivated early`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Clear any previous state
    FocusModeManager.deactivateFocusMode(context, force = true)
    assertFalse(FocusModeManager.isStrictLockActive(context))

    // 1. Activate Strict Lock for 15 minutes upon adult detection
    FocusModeManager.activateStrictLock(
      context = context,
      durationMinutes = 15,
      reason = "वयस्क सामग्री अवरोधित: xvideos",
      triggerWord = "xvideos",
      packageName = "com.android.chrome"
    )

    // 2. Verify Strict Lock is ACTIVE
    assertTrue("Strict Lock must be active", FocusModeManager.isStrictLockActive(context))
    assertTrue("Focus Mode must be active during Strict Lock", FocusModeManager.isFocusModeActive(context))

    val remainingMinutes = FocusModeManager.getRemainingSessionMinutes(context)
    assertTrue("Remaining minutes must be around 15 mins", remainingMinutes in 14..15)

    // 3. Verify user CANNOT deactivate Focus Mode early
    assertFalse("canDeactivateFocus must return false while Strict Lock is active", FocusModeManager.canDeactivateFocus(context))

    // Attempt non-force deactivation (e.g. user pressing turn off button)
    FocusModeManager.deactivateFocusMode(context, force = false)

    // Must STILL be active!
    assertTrue("Strict Lock must STILL be active after attempted cancellation", FocusModeManager.isStrictLockActive(context))
    assertTrue("Focus Mode must STILL be active", FocusModeManager.isFocusModeActive(context))

    // 4. Verify modifying allowed apps is blocked
    val initialAllowed = FocusModeManager.getAllowedPackages(context)
    FocusModeManager.setAllowedPackages(context, setOf("com.bypass.app"))
    assertEquals("Allowed packages must NOT change during Strict Lock", initialAllowed, FocusModeManager.getAllowedPackages(context))

    // 5. Clean teardown with force = true
    FocusModeManager.deactivateFocusMode(context, force = true)
    assertFalse("Strict Lock should be inactive after force deactivation", FocusModeManager.isStrictLockActive(context))
  }

  @Test
  fun `verify Strict Lock persists to Room database upon activation`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = LifeTrackerDatabase.getDatabase(context)

    FocusModeManager.activateStrictLock(
      context = context,
      durationMinutes = 15,
      reason = "वयस्क सामग्री अवरोधित: pornhub",
      triggerWord = "pornhub",
      packageName = "com.sec.android.app.sbrowser"
    )

    // Allow async DB insertion to complete
    kotlinx.coroutines.delay(200L)

    val count = db.strictLockDao().countEvents()
    assertTrue("Strict Lock event must be logged in database", count > 0)

    val activeEvent = db.strictLockDao().getActiveStrictLockEvent()
    assertNotNull("Logged active event must exist", activeEvent)
    assertEquals("pornhub", activeEvent?.triggerWord)
    assertEquals(15, activeEvent?.durationMinutes)
    assertEquals("com.sec.android.app.sbrowser", activeEvent?.packageName)

    FocusModeManager.deactivateFocusMode(context, force = true)
  }

  @Test
  fun `verify dialer and emergency calls are always allowed during Strict Lock`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Core emergency dialer packages
    assertTrue("com.google.android.dialer must always be allowed",
      FocusModeManager.isPackageAllowed(context, "com.google.android.dialer")
    )
    assertTrue("com.samsung.android.dialer must always be allowed",
      FocusModeManager.isPackageAllowed(context, "com.samsung.android.dialer")
    )
    assertTrue("com.android.phone must always be allowed",
      FocusModeManager.isPackageAllowed(context, "com.android.phone")
    )
    assertTrue("In-call UI must always be allowed",
      FocusModeManager.isPackageAllowed(context, "com.android.incallui")
    )

    // Life Tracker itself and System UI must always be allowed
    assertTrue("Life Tracker must be allowed",
      FocusModeManager.isPackageAllowed(context, context.packageName)
    )
    assertTrue("System UI must be allowed",
      FocusModeManager.isPackageAllowed(context, "com.android.systemui")
    )

    // Unallowed apps must NOT be allowed
    assertFalse("Instagram must NOT be allowed by default",
      FocusModeManager.isPackageAllowed(context, "com.instagram.android")
    )
    assertFalse("YouTube must NOT be allowed by default",
      FocusModeManager.isPackageAllowed(context, "com.google.android.youtube")
    )
  }

  @Test
  fun `verify boot restoration preserves active Strict Lock session`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    FocusModeManager.deactivateFocusMode(context, force = true)

    FocusModeManager.activateStrictLock(
      context = context,
      durationMinutes = 15,
      reason = "वयस्क सामग्री टेस्ट",
      triggerWord = "pornhub",
      packageName = "com.android.chrome"
    )

    assertTrue(FocusModeManager.isStrictLockActive(context))

    // Simulate reboot restoration
    FocusModeManager.onBootOrScheduleChange(context)

    assertTrue("Strict Lock must survive reboot restoration", FocusModeManager.isStrictLockActive(context))
    assertTrue("Remaining time must be positive after reboot", FocusModeManager.getStrictLockRemainingMillis(context) > 0L)

    FocusModeManager.deactivateFocusMode(context, force = true)
    assertFalse(FocusModeManager.isStrictLockActive(context))
  }

  // =========================================================================
  // 3. BROWSER URL EXTRACTOR & CUSTOM BLOCKLIST MANAGEMENT TESTS
  // =========================================================================

  @Test
  fun `verify browser recognition across all requested browsers`() {
    assertTrue("Chrome must be recognized as browser", BrowserUrlExtractor.isBrowser("com.android.chrome"))
    assertTrue("Brave must be recognized as browser", BrowserUrlExtractor.isBrowser("com.brave.browser"))
    assertTrue("Firefox must be recognized as browser", BrowserUrlExtractor.isBrowser("org.mozilla.firefox"))
    assertTrue("Samsung must be recognized as browser", BrowserUrlExtractor.isBrowser("com.sec.android.app.sbrowser"))
    assertTrue("Opera must be recognized as browser", BrowserUrlExtractor.isBrowser("com.opera.browser"))
    assertTrue("Opera Mini must be recognized as browser", BrowserUrlExtractor.isBrowser("com.opera.mini.native"))
    assertTrue("Edge must be recognized as browser", BrowserUrlExtractor.isBrowser("com.microsoft.emmx"))
    assertTrue("DuckDuckGo must be recognized as browser", BrowserUrlExtractor.isBrowser("com.duckduckgo.mobile.android"))
    assertTrue("Vivaldi must be recognized as browser", BrowserUrlExtractor.isBrowser("com.vivaldi.browser"))

    assertFalse("Calculator must NOT be a browser", BrowserUrlExtractor.isBrowser("com.google.android.calculator"))
    assertFalse("Phone dialer must NOT be a browser", BrowserUrlExtractor.isBrowser("com.google.android.dialer"))
  }

  @Test
  fun `verify user can add and remove custom domains, keywords, and safe-words`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Custom Domain addition & removal
    val customDomain = "badexampletestsite.xyz"
    assertFalse(AdultBlocklistManager.isDomainBlocked(context, customDomain))

    AdultBlocklistManager.addCustomDomain(context, customDomain)
    assertTrue("Domain must be blocked after addition", AdultBlocklistManager.isDomainBlocked(context, customDomain))
    assertTrue("Custom domains list must contain added domain", AdultBlocklistManager.getCustomBlockedDomains(context).contains(customDomain))

    AdultBlocklistManager.removeCustomDomain(context, customDomain)
    assertFalse("Domain must NOT be blocked after removal", AdultBlocklistManager.isDomainBlocked(context, customDomain))

    // 2. Custom Keyword addition & removal
    val customKw = "badcustomkeyword"
    assertNull(AdultBlocklistManager.checkContent(context, "some text with badcustomkeyword", "com.android.chrome"))

    AdultBlocklistManager.addCustomKeyword(context, customKw)
    val matchResult = AdultBlocklistManager.checkContent(context, "some text with badcustomkeyword", "com.android.chrome")
    assertNotNull("Custom keyword must be detected", matchResult)
    assertEquals(customKw, matchResult?.matchedTerm)

    AdultBlocklistManager.removeCustomKeyword(context, customKw)
    assertNull("Custom keyword must NOT be detected after removal",
      AdultBlocklistManager.checkContent(context, "some text with badcustomkeyword", "com.android.chrome")
    )

    // 3. Custom Safe-Word addition & false-positive prevention
    val customSafeWord = "medical test analysis"
    AdultBlocklistManager.addCustomSafeWord(context, customSafeWord)
    assertTrue(AdultBlocklistManager.getCustomSafeWords(context).contains(customSafeWord))

    // With safe word in text, false positive is prevented even if keyword overlaps
    val safeText = "patient underwent medical test analysis for condition"
    assertNull("Custom safe word must whitelist text",
      AdultBlocklistManager.checkContent(context, safeText, "com.android.chrome")
    )

    AdultBlocklistManager.removeCustomSafeWord(context, customSafeWord)
  }

  @Test
  fun `verify DNS domain check on built-in adult domains`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    assertTrue("pornhub.com must be blocked on DNS", AdultBlocklistManager.isDomainBlocked(context, "pornhub.com"))
    assertTrue("m.xvideos.com must be blocked on DNS", AdultBlocklistManager.isDomainBlocked(context, "m.xvideos.com"))
    assertTrue("something.adult must be blocked by adult TLD", AdultBlocklistManager.isDomainBlocked(context, "something.adult"))

    assertFalse("google.com must NOT be blocked", AdultBlocklistManager.isDomainBlocked(context, "google.com"))
    assertFalse("wikipedia.org must NOT be blocked", AdultBlocklistManager.isDomainBlocked(context, "wikipedia.org"))
  }
}
