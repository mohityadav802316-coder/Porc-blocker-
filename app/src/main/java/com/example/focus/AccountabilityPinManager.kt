package com.example.focus

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Manages Accountability PIN for Life Tracker Strict Lock.
 *
 * Rules:
 * 1. PIN is NEVER stored in plain text. Uses PBKDF2/Salted SHA-256 with per-PIN unique random salt.
 * 2. PIN must be 4-6 numeric digits.
 * 3. Never exported or backed up.
 * 4. Only 2 ways to change PIN:
 *    a) Enter old PIN -> Set new PIN (new PIN cannot equal old PIN).
 *    b) If forgotten -> 24-hour Unlock Request (no forgot PIN shortcut).
 * 5. PIN Rotation: Whenever PIN is used to weaken protection (Pause, Allowed Apps, toggles),
 *    the partner/friend sets a new PIN on the spot before the action completes.
 * 6. Health logs record every PIN change/rotation without revealing the PIN itself.
 */
object AccountabilityPinManager {

  private const val TAG = "AccountabilityPinMgr"
  private const val PREFS_NAME = "accountability_pin_prefs"
  private const val KEY_PIN_HASH = "accountability_pin_hash"
  private const val KEY_PIN_SALT = "accountability_pin_salt"
  private const val KEY_PIN_ENABLED = "accountability_pin_enabled"
  private const val KEY_FAILED_ATTEMPTS = "accountability_failed_attempts"
  private const val KEY_LOCKOUT_UNTIL = "accountability_lockout_until"
  private const val KEY_HIDE_PIN_SCREEN_FROM_RECENTS = "key_hide_pin_screen_from_recents"

  private const val STATIC_PEPPER = "LifeTrackerStrictLockPepper2026#SecurePin"

  private val _isPinFlowActive = kotlinx.coroutines.flow.MutableStateFlow(false)
  val isPinFlowActive: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPinFlowActive

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun isHidePinScreenFromRecents(context: Context): Boolean {
    // Default OFF to prevent Motorola and display compositor black-screen glitches
    return getPrefs(context).getBoolean(KEY_HIDE_PIN_SCREEN_FROM_RECENTS, false)
  }

  fun setHidePinScreenFromRecents(context: Context, enabled: Boolean) {
    getPrefs(context).edit().putBoolean(KEY_HIDE_PIN_SCREEN_FROM_RECENTS, enabled).apply()
  }

  fun setPinFlowActive(context: Context, active: Boolean) {
    _isPinFlowActive.value = active
    com.example.blackscreen.BlackScreenManager.setTemporarilySuppressed(active)
    if (active) {
      StrictLockFloatingPillManager.setVisibility(false)
    } else {
      StrictLockFloatingPillManager.show(context)
    }
  }

  fun applyActivitySecureFlag(activity: android.app.Activity?, secure: Boolean) {
    if (activity == null) return
    try {
      if (secure) {
        activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
      } else {
        activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to update activity FLAG_SECURE: ${e.message}")
    }
  }

  fun isPinSet(context: Context): Boolean {
    val prefs = getPrefs(context)
    val hash = prefs.getString(KEY_PIN_HASH, null)
    return !hash.isNullOrBlank() && prefs.getBoolean(KEY_PIN_ENABLED, true)
  }

  /**
   * Hashes a 4-6 digit numeric PIN using a unique salt + static pepper with SHA-256 (1000 rounds).
   */
  private fun hashPinWithSalt(pin: String, salt: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    var current = (STATIC_PEPPER + pin).toByteArray(Charsets.UTF_8) + salt
    // Multiple digest rounds to resist brute-force
    for (i in 0 until 1000) {
      current = md.digest(current)
    }
    return current.joinToString("") { "%02x".format(it) }
  }

  private fun generateRandomSalt(): ByteArray {
    val salt = ByteArray(16)
    SecureRandom().nextBytes(salt)
    return salt
  }

  /**
   * Sets or initialises the Accountability PIN.
   *
   * @param context Android context
   * @param newPin 4-6 digit numeric PIN
   * @param reason Description of why PIN is set/changed (for Health Log, never includes the PIN)
   * @return Result indicating success or failure reason
   */
  fun setPin(context: Context, newPin: String, reason: String = "INITIAL_SETUP"): PinChangeResult {
    if (newPin.length !in 4..6 || !newPin.all { it.isDigit() }) {
      return PinChangeResult.Error("PIN 4 से 6 अंकों का होना चाहिए।")
    }

    val prefs = getPrefs(context)
    val existingHash = prefs.getString(KEY_PIN_HASH, null)
    val existingSaltBase64 = prefs.getString(KEY_PIN_SALT, null)

    if (existingHash != null && existingSaltBase64 != null) {
      try {
        val existingSalt = Base64.decode(existingSaltBase64, Base64.NO_WRAP)
        val computedOldHash = hashPinWithSalt(newPin, existingSalt)
        if (computedOldHash == existingHash) {
          return PinChangeResult.Error("नया PIN पुराने PIN जैसा नहीं हो सकता। नया PIN चुनें।")
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error checking old pin match", e)
      }
    }

    val newSalt = generateRandomSalt()
    val newSaltBase64 = Base64.encodeToString(newSalt, Base64.NO_WRAP)
    val hashed = hashPinWithSalt(newPin, newSalt)

    prefs.edit()
      .putString(KEY_PIN_HASH, hashed)
      .putString(KEY_PIN_SALT, newSaltBase64)
      .putBoolean(KEY_PIN_ENABLED, true)
      .putInt(KEY_FAILED_ATTEMPTS, 0)
      .putLong(KEY_LOCKOUT_UNTIL, 0L)
      .apply()

    Log.i(TAG, "Accountability PIN updated successfully (Reason: $reason).")

    // Record in Health Log (WITHOUT recording the PIN)
    ProtectionHealthManager.logHealthEvent(
      context = context,
      eventType = "PIN_CHANGED",
      message = "Accountability PIN बदला/रोटेट हुआ (कारण: $reason)",
      severity = "INFO"
    )

    return PinChangeResult.Success
  }

  /**
   * Changes PIN with old PIN verification requirement.
   *
   * User MUST enter old PIN, and new PIN must not equal old PIN.
   */
  fun changePinWithOld(context: Context, oldPin: String, newPin: String): PinChangeResult {
    if (!verifyPin(context, oldPin)) {
      return PinChangeResult.Error("पुराना PIN गलत है। यदि PIN भूल गए हैं, तो 24 घंटे का Unlock Request शुरू करें।")
    }
    if (oldPin == newPin) {
      return PinChangeResult.Error("नया PIN पुराने PIN के समान नहीं हो सकता। कृपया अलग PIN चुनें।")
    }
    return setPin(context, newPin, reason = "MANUAL_PIN_CHANGE")
  }

  /**
   * Resets PIN after 24-hour Unlock Window claim.
   */
  fun resetPinVia24HourUnlock(context: Context, newPin: String): PinChangeResult {
    if (!AlwaysOnSecurityManager.isTemporarilyUnlocked(context) &&
        !AlwaysOnSecurityManager.isUnlockRequestReadyToClaim(context)) {
      return PinChangeResult.Error("24 घंटे का Unlock Request पूरा होने के बाद ही PIN बदला जा सकता है।")
    }
    return setPin(context, newPin, reason = "24_HOUR_UNLOCK_CLAIM")
  }

  /**
   * Validates user-entered PIN against saved salted hash.
   */
  fun verifyPin(context: Context, enteredPin: String): Boolean {
    val prefs = getPrefs(context)
    val now = System.currentTimeMillis()
    val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    if (now < lockoutUntil) {
      val remainingSec = ((lockoutUntil - now) / 1000L).coerceAtLeast(1L)
      Log.w(TAG, "PIN verification blocked: lockout active ($remainingSec sec remaining).")
      return false
    }

    val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return true
    val saltBase64 = prefs.getString(KEY_PIN_SALT, null)

    val enteredHash = if (saltBase64 != null) {
      try {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        hashPinWithSalt(enteredPin, salt)
      } catch (e: Exception) {
        // Fallback backward compatibility for legacy salt if any
        legacyHashPin(enteredPin)
      }
    } else {
      legacyHashPin(enteredPin)
    }

    val matches = (savedHash == enteredHash)

    if (matches) {
      prefs.edit()
        .putInt(KEY_FAILED_ATTEMPTS, 0)
        .putLong(KEY_LOCKOUT_UNTIL, 0L)
        .apply()
      return true
    } else {
      val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
      val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed)
      if (failed >= 5) {
        editor.putLong(KEY_LOCKOUT_UNTIL, now + 60_000L) // 1 minute lockout
      }
      editor.apply()

      ProtectionHealthManager.logHealthEvent(
        context = context,
        eventType = "PIN_VERIFY_FAILED",
        message = "गलत PIN प्रयास ($failed/5)। 5 पर 1 मिनट लॉकआउट।",
        severity = if (failed >= 5) "WARN" else "INFO"
      )

      return false
    }
  }

  fun getLockoutRemainingSeconds(context: Context): Long {
    val prefs = getPrefs(context)
    val now = System.currentTimeMillis()
    val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
    return if (now < lockoutUntil) {
      ((lockoutUntil - now) / 1000L).coerceAtLeast(1L)
    } else {
      0L
    }
  }

  /**
   * Backward-compatibility hasher for existing installs prior to per-PIN random salt.
   */
  private fun legacyHashPin(pin: String): String {
    val salt = "LifeTrackerStrictLockSalt2026#"
    val bytes = (salt + pin).toByteArray(Charsets.UTF_8)
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
  }

  sealed class PinChangeResult {
    data object Success : PinChangeResult()
    data class Error(val message: String) : PinChangeResult()
  }
}
