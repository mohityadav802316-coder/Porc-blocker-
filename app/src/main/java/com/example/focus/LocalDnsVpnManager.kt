package com.example.focus

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages configuration, persistent state, and guidance for Local DNS Filter VPN.
 * Also provides instructions and warnings on how to prevent browsers from
 * bypassing local DNS filters via Secure DNS (DoH - DNS over HTTPS).
 */
object LocalDnsVpnManager {

  private const val PREFS_NAME = "local_dns_vpn_prefs"
  private const val KEY_ENABLED = "dns_vpn_enabled"

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isEnabled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_ENABLED, false)
  }

  fun setEnabled(context: Context, enabled: Boolean, pinVerified: Boolean = false): Boolean {
    if (!enabled && !pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context) && !ProtectionPauseManager.isPaused(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        return false
      }
    }
    getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    if (enabled) {
      startVpn(context)
    } else {
      stopVpn(context, pinVerified)
    }
    return true
  }

  fun startVpn(context: Context) {
    LocalDnsVpnService.start(context)
  }

  fun stopVpn(context: Context, pinVerified: Boolean = false): Boolean {
    return LocalDnsVpnService.stop(context, pinVerified)
  }

  /**
   * Guidance for closing the "Secure DNS" (DoH) bypass loophole in Chrome, Brave, Firefox, etc.
   */
  const val SECURE_DNS_GUIDE_TITLE = "ब्राउज़र का Secure DNS (DoH) बायपास कैसे रोकें"
  const val SECURE_DNS_GUIDE_TEXT =
    "कुछ आधुनिक ब्राउज़र (जैसे Chrome और Brave) में 'Use Secure DNS' डिफ़ॉल्ट रूप से चालू होता है, " +
    "जो स्थानीय DNS को दरकिनार (bypass) कर सीधे एन्क्रिप्टेड HTTPS (Port 443) पर रिज़ॉल्व करता है।\n\n" +
    "पूर्ण सुरक्षा के लिए:\n" +
    "1. Chrome / Brave खोलें -> Settings -> Privacy and security -> 'Use secure DNS' को OFF करें, " +
    "या System Private DNS का उपयोग करें।\n" +
    "2. इसके बाद Life Tracker का Local DNS फ़िल्टर और Accessibility दोनों मिलकर 100% अभेद्य सुरक्षा प्रदान करते हैं।"
}
