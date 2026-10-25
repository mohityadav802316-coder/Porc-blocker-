package com.example.focus

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * DeviceAdminReceiver for Life Tracker Strict Lock.
 *
 * Prevents unauthorized or impulsive uninstallation of Life Tracker
 * when Strict Lock discipline is active.
 * If user attempts to deactivate device admin during active Strict Lock,
 * a non-bypassable warning is shown to remind them of the strict lock policy.
 */
class LifeTrackerAdminReceiver : DeviceAdminReceiver() {

  companion object {
    private const val TAG = "LifeTrackerAdmin"

    fun getComponentName(context: Context): ComponentName {
      return ComponentName(context, LifeTrackerAdminReceiver::class.java)
    }

    fun isDeviceAdminActive(context: Context): Boolean {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
      return dpm?.isAdminActive(getComponentName(context)) ?: false
    }

    fun isAdminActive(context: Context): Boolean = isDeviceAdminActive(context)

    fun requestAdminPermission(context: Context) {
      val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
        putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Peewee Block के सख्त अनुशासन के दौरान अनइंस्टॉल सुरक्षा चालू रखें।")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        Log.e(TAG, "Failed requesting admin permission", e)
      }
    }
  }

  override fun onEnabled(context: Context, intent: Intent) {
    super.onEnabled(context, intent)
    Log.i(TAG, "Peewee Block Device Admin enabled successfully.")
    Toast.makeText(context, "Peewee Block अनइंस्टॉल सुरक्षा सक्रिय", Toast.LENGTH_SHORT).show()
    // Re-apply Device Owner & Secure Settings user restrictions if available
    MultiUserProtectionManager.applyDeviceOwnerRestrictions(context)
    MultiUserProtectionManager.applySecureSettingsRestrictions(context)
  }

  override fun onDisabled(context: Context, intent: Intent) {
    super.onDisabled(context, intent)
    Log.w(TAG, "Peewee Block Device Admin disabled.")
  }

  override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
    if (FocusModeManager.isStrictLockActive(context)) {
      Log.w(TAG, "Device Admin deactivation requested while Strict Lock is ACTIVE!")
      val remaining = FocusModeManager.getStrictLockRemainingMillis(context)
      val mins = (remaining / 60000L).coerceAtLeast(1)
      return "⚠️ सख्त डिजिटल अनुशासन (Strict Lock) चालू है!\n" +
          "शेष समय: $mins मिनट।\n" +
          "सत्र समाप्त होने से पहले डिवाइस एडमिन हटाना या ऐप अनइंस्टॉल करना वर्जित है।"
    }
    return "Peewee Block अनइंस्टॉल सुरक्षा को निष्क्रिय करने जा रहे हैं।"
  }
}
