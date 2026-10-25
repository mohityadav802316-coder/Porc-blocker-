package com.example.focus

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.util.Log

/**
 * Multi-User, Guest Profile, Second Space, and Dual Apps Bypass Protection.
 *
 * Implements 4 defensive layers:
 * Layer 1: Always-on Accessibility detection of Multi-user, Guest, Second Space, Dual App screens,
 *          plus Quick Settings / lock-screen user switchers.
 * Layer 2: Device Owner restrictions (DISALLOW_ADD_USER, DISALLOW_USER_SWITCH, DISALLOW_REMOVE_USER,
 *          DISALLOW_ADD_MANAGED_PROFILE).
 * Layer 3: System settings hardening (user_switcher_enabled, guest_user_enabled) when WRITE_SECURE_SETTINGS is present.
 * Layer 4: Detection & Alerting in Protection Health (11th check), persistent logging, and audit history.
 */
object MultiUserProtectionManager {

  private const val TAG = "MultiUserProtection"
  private const val PREFS_NAME = "life_tracker_multi_user_protection"

  private const val KEY_ENABLED = "multi_user_protection_enabled"
  private const val KEY_CUSTOM_KEYWORDS = "custom_multi_user_keywords"
  private const val KEY_INTERCEPTIONS_COUNT = "multi_user_interceptions_count"
  private const val KEY_LAST_INTERCEPT_TIME = "multi_user_last_intercept_time"
  private const val KEY_LAST_INTERCEPT_DETAIL = "multi_user_last_intercept_detail"

  // Standard user restriction keys (defined in UserManager)
  private const val RESTRICTION_DISALLOW_ADD_USER = UserManager.DISALLOW_ADD_USER
  private const val RESTRICTION_DISALLOW_USER_SWITCH = UserManager.DISALLOW_USER_SWITCH
  private const val RESTRICTION_DISALLOW_REMOVE_USER = UserManager.DISALLOW_REMOVE_USER
  private const val RESTRICTION_DISALLOW_ADD_MANAGED_PROFILE = "no_add_managed_profile" // UserManager.DISALLOW_ADD_MANAGED_PROFILE

  // Moto & Android Multi-User screen trigger phrases.
  // Requirement 1 & 4: Detection matches when ANY 2 or more of these phrases appear together.
  // "Users" alone will NOT trigger blocking.
  val MOTO_MULTI_USER_SCREEN_INDICATORS = listOf(
    "allow user switch",
    "add user",
    "add guest",
    "delete guest activity",
    "allow guest to make phone calls",
    "users",
    // Hindi variants
    "उपयोगकर्ता स्विच करने दें",
    "उपयोगकर्ता जोड़ें",
    "अतिथि जोड़ें",
    "अतिथि गतिविधि हटाएं",
    "अतिथि को फ़ोन कॉल करने दें",
    "उपयोगकर्ता"
  )

  // Built-in keywords across Android brands (Stock, Moto, Samsung OneUI, MIUI/HyperOS, OnePlus/ColorOS) in English and Hindi
  val DEFAULT_KEYWORDS = listOf(
    // Specific compound phrases (standalone triggers)
    "multiple users",
    "multi-user",
    "multi user",
    "users & accounts",
    "add user",
    "add guest",
    "switch user",
    "allow user switch",
    "delete guest activity",
    "allow guest to make phone calls",
    "guest profile",
    "guest mode",
    "second space",
    "secondspace",
    "dual apps",
    "dual app",
    "parallel apps",
    "parallel app",
    "twin apps",
    "app cloner",
    "cloned apps",
    "clone app",
    "work profile",
    "managed profile",
    "user switcher",
    // Hindi keywords
    "एकाधिक उपयोगकर्ता",
    "उपयोगकर्ता जोड़ें",
    "उपयोगकर्ता स्विच करें",
    "उपयोगकर्ता स्विच करने दें",
    "अतिथि जोड़ें",
    "अतिथि गतिविधि हटाएं",
    "अतिथि प्रोफ़ाइल",
    "अतिथि मोड",
    "द्वितीय स्पेस",
    "समानांतर ऐप्स",
    "डुअल ऐप्स",
    "कार्य प्रोफ़ाइल"
  )

  // Target class names often associated with user/space creation or switching
  private val SENSITIVE_CLASS_FRAGMENTS = listOf(
    "usersettings",
    "usersdetailssettings",
    "userdetailssettings",
    "edituserphotoandnameactivity",
    "appcloneactivity",
    "secondspaceactivity",
    "secondspace",
    "dualappactivity",
    "parallelspaceactivity",
    "guestmodepreference",
    "userswitcheractivity",
    "userswitcherdialog",
    "userswitcherpopup",
    "userswitcherfragment"
  )

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isEnabled(context: Context): Boolean {
    return getPrefs(context).getBoolean(KEY_ENABLED, true)
  }

  fun setEnabled(context: Context, enabled: Boolean, pinVerified: Boolean = false): Boolean {
    if (!enabled && !pinVerified && !AlwaysOnSecurityManager.isTemporarilyUnlocked(context)) {
      if (AccountabilityPinManager.isPinSet(context)) {
        Log.w(TAG, "Cannot disable Multi-User Protection: PIN verification or 24-hr unlock required")
        return false
      }
    }
    getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    return true
  }

  fun getInterceptionCount(context: Context): Int {
    return getPrefs(context).getInt(KEY_INTERCEPTIONS_COUNT, 0)
  }

  fun getLastInterceptTime(context: Context): Long {
    return getPrefs(context).getLong(KEY_LAST_INTERCEPT_TIME, 0L)
  }

  fun getLastInterceptDetail(context: Context): String {
    return getPrefs(context).getString(KEY_LAST_INTERCEPT_DETAIL, "") ?: ""
  }

  fun recordInterception(context: Context, detail: String, actionTaken: String = "GLOBAL_ACTION_BACK + HOME") {
    val count = getInterceptionCount(context) + 1
    val now = System.currentTimeMillis()
    getPrefs(context).edit()
      .putInt(KEY_INTERCEPTIONS_COUNT, count)
      .putLong(KEY_LAST_INTERCEPT_TIME, now)
      .putString(KEY_LAST_INTERCEPT_DETAIL, "$detail -> Action: $actionTaken")
      .apply()

    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "MULTI_USER_BYPASS_BLOCKED",
      message = "मल्टी-यूज़र / गेस्ट प्रोफ़ाइल बाईपास डिटेक्ट हुआ [$detail] | कार्रवाई: $actionTaken",
      severity = "WARN"
    )
  }

  /**
   * Returns list of active keywords (default + user-configured additions).
   */
  fun getActiveKeywords(context: Context): List<String> {
    val custom = getPrefs(context).getString(KEY_CUSTOM_KEYWORDS, "") ?: ""
    if (custom.isBlank()) {
      return DEFAULT_KEYWORDS
    }
    val additional = custom.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
    val combined = ArrayList<String>(DEFAULT_KEYWORDS.size + additional.size)
    combined.addAll(DEFAULT_KEYWORDS)
    for (k in additional) {
      if (!combined.contains(k)) {
        combined.add(k)
      }
    }
    return combined
  }

  fun setCustomKeywords(context: Context, keywordsCommaSeparated: String) {
    getPrefs(context).edit().putString(KEY_CUSTOM_KEYWORDS, keywordsCommaSeparated).apply()
  }

  /**
   * Result of checking a screen for multi-user bypass attempts.
   */
  data class DetectionResult(
    val isBlocked: Boolean,
    val matchedTexts: List<String> = emptyList(),
    val matchReason: String = ""
  )

  /**
   * Evaluates screen indicators and returns detailed DetectionResult.
   * Requirement 1: In com.android.settings (SubSettings on Moto, etc.), matches if ANY 2 or more of:
   * "Allow user switch", "Add user", "Add guest", "Delete guest activity", "Allow guest to make phone calls", "Users"
   * appear together in visible node text. "Users" alone will NOT block.
   */
  fun evaluateScreenDetection(
    context: Context,
    packageName: String,
    className: String,
    visibleText: String
  ): DetectionResult {
    if (!isEnabled(context)) return DetectionResult(false)

    val lowerPkg = packageName.lowercase()
    val lowerCls = className.lowercase()
    val lowerText = visibleText.lowercase()

    // 1. Target settings & system UI packages
    val isSettingsOrSystemUi = lowerPkg == "com.android.settings" ||
      lowerPkg == "com.google.android.settings" ||
      lowerPkg == "com.android.systemui" ||
      lowerPkg.contains("secondspace") ||
      lowerPkg.contains("appclone") ||
      lowerPkg.contains("dualapp")

    if (!isSettingsOrSystemUi) return DetectionResult(false)

    // 2. MOTO / STOCK MULTI-USER SCREEN CHECK (Settings > System > Multiple Users / SubSettings)
    // Counts how many distinct indicators appear in the visible text.
    // Must match at least 2 distinct indicators. "Users" / "उपयोगकर्ता" alone does not block.
    val matchedIndicators = mutableListOf<String>()
    for (indicator in MOTO_MULTI_USER_SCREEN_INDICATORS) {
      if (lowerText.contains(indicator)) {
        if (!matchedIndicators.contains(indicator)) {
          matchedIndicators.add(indicator)
        }
      }
    }

    if (matchedIndicators.size >= 2) {
      // Confirmed multi-user settings screen (Moto SubSettings / Settings)
      return DetectionResult(
        isBlocked = true,
        matchedTexts = matchedIndicators,
        matchReason = "Multi-User Screen (matched ${matchedIndicators.size} indicators: ${matchedIndicators.joinToString(", ")})"
      )
    }

    // 3. Class name heuristic check (when not SubSettings)
    for (fragment in SENSITIVE_CLASS_FRAGMENTS) {
      if (lowerCls.contains(fragment)) {
        return DetectionResult(
          isBlocked = true,
          matchedTexts = listOf(fragment),
          matchReason = "Sensitive Class: $fragment"
        )
      }
    }

    // 4. Configurable keywords check against visible text (e.g. "multiple users", "second space", etc.)
    val keywords = getActiveKeywords(context)
    val matchedKeywords = mutableListOf<String>()
    for (kw in keywords) {
      // Skip generic isolated single word "guest" or "users" to avoid false positives unless paired
      if (kw == "users" || kw == "उपयोगकर्ता") continue
      if (lowerText.contains(kw)) {
        matchedKeywords.add(kw)
      }
    }
    if (matchedKeywords.isNotEmpty()) {
      return DetectionResult(
        isBlocked = true,
        matchedTexts = matchedKeywords,
        matchReason = "Configurable Keywords: ${matchedKeywords.joinToString(", ")}"
      )
    }

    // 5. Quick Settings or Lock-screen user-switcher tiles in SystemUI
    if (lowerPkg == "com.android.systemui") {
      if (lowerCls.contains("user") || lowerCls.contains("switch") || lowerCls.contains("avatar")) {
        for (kw in keywords) {
          if (lowerText.contains(kw)) {
            return DetectionResult(
              isBlocked = true,
              matchedTexts = listOf(kw),
              matchReason = "SystemUI User Switcher Tile ($kw)"
            )
          }
        }
      }
    }

    return DetectionResult(false)
  }

  /**
   * Checks whether the current package, class, or visible text represents a multi-user,
   * guest profile, dual app, or user-switcher UI that allows bypassing the blocker.
   */
  fun isMultiUserOrGuestScreen(
    context: Context,
    packageName: String,
    className: String,
    visibleText: String
  ): Boolean {
    return evaluateScreenDetection(context, packageName, className, visibleText).isBlocked
  }

  /**
   * Checks whether Life Tracker is provisioned as Device Owner on this Android device.
   */
  fun isDeviceOwner(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
    return try {
      dpm.isDeviceOwnerApp(context.packageName)
    } catch (e: Exception) {
      Log.w(TAG, "isDeviceOwner check failed", e)
      false
    }
  }

  /**
   * Checks whether Life Tracker is active as Device Admin.
   */
  fun isDeviceAdminActive(context: Context): Boolean {
    return LifeTrackerAdminReceiver.isDeviceAdminActive(context)
  }

  /**
   * Applies all available multi-user restrictions if Life Tracker is Device Owner.
   * Called during app start, DeviceAdmin enabled, and periodic Watchdog runs.
   * Returns true if successfully enforced via Device Owner.
   */
  fun applyDeviceOwnerRestrictions(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager ?: return false
    val adminComponent = LifeTrackerAdminReceiver.getComponentName(context)

    if (!isDeviceOwner(context)) {
      Log.i(TAG, "App is not Device Owner; multi-user hard-restrictions via DPM skipped (Layer 1 active).")
      return false
    }

    return try {
      var allApplied = true

      // 1. Disallow adding new users
      try {
        dpm.addUserRestriction(adminComponent, RESTRICTION_DISALLOW_ADD_USER)
      } catch (e: Exception) {
        Log.w(TAG, "Failed applying DISALLOW_ADD_USER", e)
        allApplied = false
      }

      // 2. Disallow switching users
      try {
        dpm.addUserRestriction(adminComponent, RESTRICTION_DISALLOW_USER_SWITCH)
      } catch (e: Exception) {
        Log.w(TAG, "Failed applying DISALLOW_USER_SWITCH", e)
        allApplied = false
      }

      // 3. Disallow removing users (prevents tampering with managed user accounts)
      try {
        dpm.addUserRestriction(adminComponent, RESTRICTION_DISALLOW_REMOVE_USER)
      } catch (e: Exception) {
        Log.w(TAG, "Failed applying DISALLOW_REMOVE_USER", e)
        allApplied = false
      }

      // 4. Disallow adding managed profiles (work profiles / dual apps)
      try {
        dpm.addUserRestriction(adminComponent, RESTRICTION_DISALLOW_ADD_MANAGED_PROFILE)
      } catch (e: Exception) {
        Log.w(TAG, "Failed applying DISALLOW_ADD_MANAGED_PROFILE", e)
        allApplied = false
      }

      Log.i(TAG, "Device Owner user restrictions successfully enforced: allApplied=$allApplied")
      true
    } catch (e: Exception) {
      Log.e(TAG, "Error applying device owner restrictions", e)
      false
    }
  }

  /**
   * Checks whether the app has WRITE_SECURE_SETTINGS permission granted via ADB.
   */
  fun hasWriteSecureSettings(context: Context): Boolean {
    return try {
      val result = context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
      result == android.content.pm.PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
      false
    }
  }

  /**
   * Layer 3: Hardens Settings.Global user switcher & guest mode if WRITE_SECURE_SETTINGS is present.
   * Returns true if settings were verified/written.
   */
  fun applySecureSettingsRestrictions(context: Context): Boolean {
    if (!hasWriteSecureSettings(context)) return false

    return try {
      val cr = context.contentResolver
      // user_switcher_enabled = 0
      Settings.Global.putInt(cr, "user_switcher_enabled", 0)
      // guest_user_enabled = 0
      Settings.Global.putInt(cr, "guest_user_enabled", 0)
      Log.i(TAG, "Secure settings hardened: user_switcher_enabled=0, guest_user_enabled=0")
      true
    } catch (e: Exception) {
      Log.w(TAG, "Failed writing secure settings", e)
      false
    }
  }

  /**
   * Verifies that secure settings have not been re-enabled. If re-enabled, forces them back to 0.
   */
  fun verifyAndEnforceSecureSettings(context: Context): Boolean {
    if (!hasWriteSecureSettings(context)) return false
    return try {
      val cr = context.contentResolver
      val switcher = Settings.Global.getInt(cr, "user_switcher_enabled", 0)
      val guest = Settings.Global.getInt(cr, "guest_user_enabled", 0)

      if (switcher != 0 || guest != 0) {
        Log.w(TAG, "Secure settings were tampered! Restoring user_switcher_enabled=0, guest_user_enabled=0")
        Settings.Global.putInt(cr, "user_switcher_enabled", 0)
        Settings.Global.putInt(cr, "guest_user_enabled", 0)
        ProtectionHealthManager.logHealthEvent(
          context = context,
          eventType = "SECURE_SETTINGS_TAMPER_RESTORED",
          message = "यूज़र स्विचर सेटिंग्स को सुरक्षित स्थिति (0) पर वापस बहाल किया गया।",
          severity = "WARN"
        )
      }
      true
    } catch (e: Exception) {
      Log.w(TAG, "Error checking secure settings", e)
      false
    }
  }

  /**
   * Comprehensive status structure for UI and Protection Health reports.
   */
  data class MultiUserStatus(
    val isEnabled: Boolean,
    val isAccessibilityActive: Boolean,
    val isDeviceAdminActive: Boolean,
    val isDeviceOwner: Boolean,
    val hasWriteSecureSettings: Boolean,
    val interceptionCount: Int,
    val lastInterceptTime: Long,
    val lastInterceptDetail: String,
    val activeKeywordsCount: Int,
    val healthSummary: String,
    val isHealthy: Boolean
  )

  fun getStatus(context: Context): MultiUserStatus {
    val enabled = isEnabled(context)
    val a11yActive = com.example.shortcontent.AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val adminActive = isDeviceAdminActive(context)
    val owner = isDeviceOwner(context)
    val secureSettings = hasWriteSecureSettings(context)
    val count = getInterceptionCount(context)
    val lastTime = getLastInterceptTime(context)
    val lastDetail = getLastInterceptDetail(context)
    val keywordsCount = getActiveKeywords(context).size

    val isHealthy = enabled && a11yActive

    val summary = when {
      !enabled -> "सुरक्षा निष्क्रिय है"
      !a11yActive -> "Accessibility बंद है (Layer 1 सुरक्षा उपलब्ध नहीं)"
      owner -> "पूर्णतः सुरक्षित (Device Owner + Accessibility दोनों सक्रिय)"
      secureSettings -> "उच्च सुरक्षा (Secure Settings + Accessibility सक्रिय, Device Owner नहीं)"
      else -> "Accessibility Layer सक्रिय (Device Owner सेट नहीं है: हार्ड-ब्लॉक बंद)"
    }

    return MultiUserStatus(
      isEnabled = enabled,
      isAccessibilityActive = a11yActive,
      isDeviceAdminActive = adminActive,
      isDeviceOwner = owner,
      hasWriteSecureSettings = secureSettings,
      interceptionCount = count,
      lastInterceptTime = lastTime,
      lastInterceptDetail = lastDetail,
      activeKeywordsCount = keywordsCount,
      healthSummary = summary,
      isHealthy = isHealthy
    )
  }
}
