package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Production crash diagnostic & safety-net reporter.
 * Catches unhandled crashes globally, logs full diagnostic telemetry to persistent storage,
 * and allows easy one-tap copy/export from Settings > Permissions & Status.
 */
object CrashReporter {

  private const val TAG = "CrashReporter"
  private const val CRASH_FILE_NAME = "last_crash_report.txt"
  private const val PREFS_NAME = "life_tracker_diagnostics"
  private const val KEY_LAST_CRASH_TIMESTAMP = "last_crash_timestamp"
  private const val KEY_LAST_CRASH_MESSAGE = "last_crash_message"

  private var isInitialized = false

  fun init(context: Context) {
    if (isInitialized) return
    isInitialized = true

    val appContext = context.applicationContext
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        saveCrashReport(appContext, thread, throwable)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist crash report", e)
      } finally {
        defaultHandler?.uncaughtException(thread, throwable)
      }
    }
    Log.i(TAG, "Global UncaughtExceptionHandler initialized successfully")
  }

  fun saveCrashReport(context: Context, thread: Thread, throwable: Throwable) {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    throwable.printStackTrace(pw)
    val stackTrace = sw.toString()

    val report = buildString {
      appendLine("=== LIFE TRACKER CRASH DIAGNOSTIC ===")
      appendLine("Timestamp: $timestamp")
      appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
      appendLine("Android SDK: ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})")
      appendLine("Thread: ${thread.name} (id: ${thread.id})")
      appendLine("Exception: ${throwable.javaClass.name}")
      appendLine("Message: ${throwable.message ?: "No message provided"}")
      appendLine("--- STACK TRACE ---")
      appendLine(stackTrace)
      appendLine("=====================================")
    }

    try {
      val file = File(context.filesDir, CRASH_FILE_NAME)
      file.writeText(report)

      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit()
        .putLong(KEY_LAST_CRASH_TIMESTAMP, System.currentTimeMillis())
        .putString(KEY_LAST_CRASH_MESSAGE, "${throwable.javaClass.simpleName}: ${throwable.message ?: ""}")
        .apply()

      Log.e(TAG, "Crash report recorded:\n$report")
    } catch (e: Exception) {
      Log.e(TAG, "Error writing crash report to file", e)
    }
  }

  fun recordHandledException(context: Context, tag: String, throwable: Throwable) {
    Log.w(tag, "Handled exception recorded: ${throwable.message}", throwable)
  }

  fun hasCrash(context: Context): Boolean {
    val file = File(context.filesDir, CRASH_FILE_NAME)
    return file.exists() && file.length() > 0
  }

  fun getLastCrash(context: Context): String? {
    val file = File(context.filesDir, CRASH_FILE_NAME)
    return if (file.exists() && file.length() > 0) {
      try {
        file.readText()
      } catch (e: Exception) {
        Log.e(TAG, "Failed to read crash report", e)
        null
      }
    } else {
      null
    }
  }

  fun clearCrash(context: Context): Boolean {
    return try {
      val file = File(context.filesDir, CRASH_FILE_NAME)
      if (file.exists()) file.delete()
      val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().clear().apply()
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear crash report", e)
      false
    }
  }

  fun copyCrashToClipboard(context: Context): Boolean {
    val crashText = getLastCrash(context) ?: "No crash report found."
    return try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      val clip = ClipData.newPlainText("Life Tracker Crash Log", crashText)
      clipboard?.setPrimaryClip(clip)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed to copy crash report to clipboard", e)
      false
    }
  }
}
