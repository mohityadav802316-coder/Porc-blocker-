package com.example.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * PeeweeBootReceiver
 *
 * Listens for system boot completion and package updates to automatically restore
 * Peewee Block background protection without requiring manual user intervention.
 */
class PeeweeBootReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "PeeweeBootReceiver"
  }

  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    Log.i(TAG, "Received broadcast action: $action")

    when (action) {
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED,
      "android.intent.action.QUICKBOOT_POWERON",
      "com.htc.intent.action.QUICKBOOT_POWERON" -> {
        PeeweeProtectionManager.onBootOrAppUpdated(context)
      }
    }
  }
}
