package com.example.focus

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlwaysOnTamperProtectionTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ProtectionPauseManager.cancelPause(context)
    AlwaysOnSecurityManager.cancelUnlockRequest(context)
    AlwaysOnSecurityManager.setAlwaysOnEnabled(context, true)
  }

  // =========================================================================
  // 1. TAMPER SCREEN EVALUATION (REQUIREMENT 1: SCOPE RESTRICTION)
  // =========================================================================

  @Test
  fun `settings main page and other apps are NEVER blocked`() {
    // 1. Settings main page
    val mainSettings = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.Settings",
      visibleText = "Settings Network & Internet Connected devices Apps Notifications Battery Storage"
    )
    assertFalse(mainSettings.isTamper)

    // 2. Other app's App Info (e.g. WhatsApp)
    val whatsappAppInfo = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "WhatsApp Version 2.24.1 Uninstall Force stop Storage & cache"
    )
    assertFalse(whatsappAppInfo.isTamper)

    // 3. Other app's uninstall confirmation dialog
    val otherUninstall = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.google.android.packageinstaller",
      className = "com.android.packageinstaller.UninstallerActivity",
      visibleText = "Do you want to uninstall Chrome? Cancel OK"
    )
    assertFalse(otherUninstall.isTamper)

    // 4. Accessibility main list (with other apps)
    val a11yList = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.accessibility.AccessibilitySettings",
      visibleText = "Accessibility Downloaded apps TalkBack Select to Speak Switch Access"
    )
    assertFalse(a11yList.isTamper)
  }

  @Test
  fun `blocks Life Tracker own accessibility service detail and toggle page`() {
    // English toggle screen
    val englishToggle = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.SubSettings",
      visibleText = "Life Tracker Use Life Tracker Shortcut ShortContentAccessibilityService"
    )
    assertTrue(englishToggle.isTamper)
    assertEquals("Accessibility Service Toggle", englishToggle.screenType)

    // Hindi toggle screen
    val hindiToggle = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.SubSettings",
      visibleText = "लाइफ ट्रैकर लाइफ ट्रैकर का इस्तेमाल करें शॉर्टकट चालू"
    )
    assertTrue(hindiToggle.isTamper)
  }

  @Test
  fun `blocks Life Tracker own device admin deactivation screen`() {
    val adminScreen = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.DeviceAdminAdd",
      visibleText = "Life Tracker LifeTrackerAdminReceiver Deactivate this device admin app"
    )
    assertTrue(adminScreen.isTamper)
    assertEquals("Device Admin Deactivation", adminScreen.screenType)

    // Hindi admin screen
    val hindiAdmin = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.DeviceAdminAdd",
      visibleText = "लाइफ ट्रैकर यह डिवाइस व्यवस्थापक ऐप निष्क्रिय करें"
    )
    assertTrue(hindiAdmin.isTamper)
  }

  @Test
  fun `blocks Life Tracker own app info screen`() {
    val appInfo = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "Life Tracker Uninstall Force stop Storage & cache Permissions"
    )
    assertTrue(appInfo.isTamper)
    assertEquals("App Info Page", appInfo.screenType)
  }

  @Test
  fun `blocks Life Tracker uninstall confirmation dialog`() {
    val uninstallDialog = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.google.android.packageinstaller",
      className = "com.android.packageinstaller.UninstallerActivity",
      visibleText = "Do you want to uninstall this app? Life Tracker Cancel OK"
    )
    assertTrue(uninstallDialog.isTamper)
    assertEquals("Uninstall Confirmation Dialog", uninstallDialog.screenType)
  }

  @Test
  fun `blocks Life Tracker VPN disconnect or forget page`() {
    val vpnDisconnect = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.vpn2.VpnSettings",
      visibleText = "VPN Life Tracker Connected Disconnect Forget VPN"
    )
    assertTrue(vpnDisconnect.isTamper)
    assertEquals("VPN Disconnect/Forget", vpnDisconnect.screenType)
  }

  // =========================================================================
  // 2. TEMPORARY PAUSE (REQUIREMENT 2)
  // =========================================================================

  @Test
  fun `temporary pause allows tamper screens without blocking`() {
    // Start 60 min pause
    val started = ProtectionPauseManager.startPause(context, 60)
    assertTrue(started)
    assertTrue(ProtectionPauseManager.isPaused(context))

    // Same Life Tracker App Info screen must NOT be blocked during pause!
    val duringPause = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "Life Tracker Uninstall Force stop Storage & cache"
    )
    assertFalse(duringPause.isTamper)

    // Cancel pause restores protection
    ProtectionPauseManager.cancelPause(context)
    assertFalse(ProtectionPauseManager.isPaused(context))

    val afterPause = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "Life Tracker Uninstall Force stop Storage & cache"
    )
    assertTrue(afterPause.isTamper)
  }

  // =========================================================================
  // 3. NOTIFICATION CHANNELS (REQUIREMENT 4)
  // =========================================================================

  @Test
  fun `all 4 notification channels are defined and accessible`() {
    AppNotificationChannels.registerAllChannels(context)

    assertEquals(4, AppNotificationChannels.ALL_CHANNELS.size)
    assertTrue(AppNotificationChannels.ALL_CHANNELS.any { it.id == AppNotificationChannels.CHANNEL_PROTECTION_RUNNING })
    assertTrue(AppNotificationChannels.ALL_CHANNELS.any { it.id == AppNotificationChannels.CHANNEL_STRICT_LOCK_TIMER })
    assertTrue(AppNotificationChannels.ALL_CHANNELS.any { it.id == AppNotificationChannels.CHANNEL_SECURITY_ALERTS })
    assertTrue(AppNotificationChannels.ALL_CHANNELS.any { it.id == AppNotificationChannels.CHANNEL_REMINDERS })
  }

  // =========================================================================
  // 4. PEEWEE BLOCK UNINSTALL & STRICT LOCK TESTS
  // =========================================================================

  @Test
  fun `blocks Peewee Block own uninstall confirmation dialog`() {
    val uninstallDialog = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.google.android.packageinstaller",
      className = "com.android.packageinstaller.UninstallerActivity",
      visibleText = "Do you want to uninstall this app? Peewee Block Cancel OK"
    )
    assertTrue("Must detect Peewee Block uninstall dialog", uninstallDialog.isTamper)
    assertEquals("Uninstall Confirmation Dialog", uninstallDialog.screenType)
  }

  @Test
  fun `blocks Peewee Block own app info screen`() {
    val appInfo = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "Peewee Block Uninstall Force stop Storage & cache Permissions"
    )
    assertTrue("Must detect Peewee Block App Info page", appInfo.isTamper)
    assertEquals("App Info Page", appInfo.screenType)
  }

  @Test
  fun `blocks Peewee Block own accessibility toggle screen`() {
    val a11yToggle = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.SubSettings",
      visibleText = "Peewee Block Use Peewee Block Allow Shortcut ShortContentAccessibilityService"
    )
    assertTrue("Must detect Peewee Block accessibility toggle", a11yToggle.isTamper)
    assertEquals("Accessibility Service Toggle", a11yToggle.screenType)
  }

  @Test
  fun `blocks Peewee Block own device admin deactivation screen`() {
    val adminScreen = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.DeviceAdminAdd",
      visibleText = "Peewee Block LifeTrackerAdminReceiver Deactivate this device admin app"
    )
    assertTrue("Must detect Peewee Block device admin deactivation", adminScreen.isTamper)
    assertEquals("Device Admin Deactivation", adminScreen.screenType)
  }

  @Test
  fun `24-hour Strict Lock countdown persists and survives simulated reboot`() {
    FocusModeManager.deactivateFocusMode(context, force = true)
    assertFalse(FocusModeManager.isStrictLockActive(context))

    // Activate 24-hour Strict Lock (1440 minutes = 86400 seconds)
    FocusModeManager.activateStrictLock(
      context = context,
      durationMinutes = 1440,
      reason = "24-hour Anti-Relapse Lock",
      packageName = context.packageName
    )

    assertTrue("Strict Lock must be active", FocusModeManager.isStrictLockActive(context))
    val remainingMillis = FocusModeManager.getStrictLockRemainingMillis(context)
    assertTrue("Remaining time should be approximately 24 hours", remainingMillis > (23 * 3600 * 1000L))

    // Simulate reboot recovery
    FocusModeManager.onBootOrScheduleChange(context)
    assertTrue("Strict Lock must remain active after simulated reboot", FocusModeManager.isStrictLockActive(context))
    assertTrue("Remaining time must still be positive after reboot", FocusModeManager.getStrictLockRemainingMillis(context) > 0L)

    // Ordinary deactivation without force is blocked
    FocusModeManager.deactivateFocusMode(context, force = false)
    assertTrue("Strict Lock must STILL be active after unforced deactivation attempt", FocusModeManager.isStrictLockActive(context))

    // Force deactivation (used when PIN is verified)
    FocusModeManager.deactivateFocusMode(context, force = true)
    assertFalse("Strict Lock should be inactive after force deactivation with PIN", FocusModeManager.isStrictLockActive(context))
  }

  @Test
  fun `blocks Peewee Block uninstall dialog when triggered from custom launcher or android system package`() {
    // 1. Android system package dialog
    val systemDialog = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "android",
      className = "com.android.server.pm.PackageInstallerActivity",
      visibleText = "Do you want to uninstall Peewee Block? Cancel OK"
    )
    assertTrue("System android package uninstall dialog must be blocked", systemDialog.isTamper)

    // 2. Launcher uninstall dialog (e.g. Samsung One UI Home or Pixel Launcher)
    val launcherDialog = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.sec.android.app.launcher",
      className = "com.sec.android.app.launcher.UninstallConfirmActivity",
      visibleText = "Uninstall Peewee Block? Cancel OK"
    )
    assertTrue("Launcher uninstall dialog must be blocked", launcherDialog.isTamper)

    // 3. Hindi launcher uninstall dialog
    val hindiLauncherDialog = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.google.android.apps.nexuslauncher",
      className = "com.google.android.apps.nexuslauncher.NexusLauncherActivity",
      visibleText = "क्या आप Peewee Block को अनइंस्टॉल करना चाहते हैं? रद्द करें ठीक है"
    )
    assertTrue("Hindi uninstall dialog must be blocked", hindiLauncherDialog.isTamper)
  }

  @Test
  fun `guarantees zero interference when accessing Chrome and other apps settings or browsing`() {
    // 1. Chrome App Info in Settings must NEVER be blocked
    val chromeSettings = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "Chrome Google LLC Version 122.0.6261.64 Disable Force stop Storage Notifications"
    )
    assertFalse("Chrome App Info must NEVER be blocked", chromeSettings.isTamper)

    // 2. WhatsApp App Info in Settings must NEVER be blocked
    val whatsAppSettings = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.applications.InstalledAppDetails",
      visibleText = "WhatsApp Meta Version 2.24.4.76 Uninstall Force stop"
    )
    assertFalse("WhatsApp settings must NEVER be blocked", whatsAppSettings.isTamper)

    // 3. System General Settings (Wi-Fi, Bluetooth, Display, Sound) must NEVER be blocked
    val wifiSettings = AlwaysOnSecurityManager.evaluateTamperScreen(
      context = context,
      packageName = "com.android.settings",
      className = "com.android.settings.Settings\$NetworkProviderSettingsActivity",
      visibleText = "Internet Wi-Fi Mobile data Airplane mode Hotspot & tethering"
    )
    assertFalse("Wi-Fi settings must NEVER be blocked", wifiSettings.isTamper)

    // 4. Google AI Studio domain in AdultBlocklistManager is strictly whitelisted
    assertFalse("aistudio.google.com must never be blocked", AdultBlocklistManager.isDomainBlocked(context, "aistudio.google.com"))
    assertFalse("google.com must never be blocked", AdultBlocklistManager.isDomainBlocked(context, "google.com"))
    assertFalse("github.com must never be blocked", AdultBlocklistManager.isDomainBlocked(context, "github.com"))
  }
}
