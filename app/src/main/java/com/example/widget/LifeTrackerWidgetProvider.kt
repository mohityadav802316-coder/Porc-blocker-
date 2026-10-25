package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.TaskStatus
import com.example.meditation.MeditationManager
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LifeTrackerWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    for (appWidgetId in appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId)
    }
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle?
  ) {
    updateAppWidget(context, appWidgetManager, appWidgetId)
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    when (intent.action) {
      ACTION_WIDGET_COMPLETE_TASK -> {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        CoroutineScope(Dispatchers.IO).launch {
          val db = LifeTrackerDatabase.getDatabase(context)
          if (taskId > 0) {
            val task = db.taskDao().getTaskById(taskId)
            if (task != null) {
              db.taskDao().updateTask(task.copy(status = TaskStatus.COMPLETE))
            }
          } else {
            // Find first incomplete task for today
            val todayDate = TimeUtils.getTodayIsoDate()
            val tasks = db.taskDao().getTasksForDateSync(todayDate)
            val nextIncomplete = tasks.firstOrNull { it.status != TaskStatus.COMPLETE }
            if (nextIncomplete != null) {
              db.taskDao().updateTask(nextIncomplete.copy(status = TaskStatus.COMPLETE))
            }
          }
          updateAllWidgets(context)
        }
      }
      ACTION_WIDGET_START_MEDITATION -> {
        // Open app directly on Meditation tab
        val openIntent = Intent(context, MainActivity::class.java).apply {
          setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          putExtra(EXTRA_OPEN_MEDITATION, true)
        }
        context.startActivity(openIntent)
      }
    }
  }

  companion object {
    const val ACTION_WIDGET_COMPLETE_TASK = "com.mohit.lifetracker.ACTION_WIDGET_COMPLETE_TASK"
    const val ACTION_WIDGET_START_MEDITATION = "com.mohit.lifetracker.ACTION_WIDGET_START_MEDITATION"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_OPEN_MEDITATION = "extra_open_meditation"

    fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val componentName = ComponentName(context, LifeTrackerWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
      for (id in appWidgetIds) {
        updateAppWidget(context, appWidgetManager, id)
      }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      CoroutineScope(Dispatchers.IO).launch {
        val db = LifeTrackerDatabase.getDatabase(context)
        val todayDate = TimeUtils.getTodayIsoDate()
        val allTasks = db.taskDao().getTasksForDateSync(todayDate)

        val totalTasks = allTasks.size
        val completedCount = allTasks.count { it.status == TaskStatus.COMPLETE }
        val completionPct = if (totalTasks > 0) ((completedCount * 100) / totalTasks) else 0

        val nowMinutes = TimeUtils.getCurrentMinutesFromMidnight()
        val nextTask = allTasks.firstOrNull { it.status != TaskStatus.COMPLETE && it.timeMinutes >= nowMinutes }
          ?: allTasks.firstOrNull { it.status != TaskStatus.COMPLETE }
          ?: allTasks.lastOrNull()

        val nextTaskTitle = nextTask?.name ?: "All Tasks Done! ✨"
        val nextTaskTime = nextTask?.let { TimeUtils.minutesTo12Hour(it.timeMinutes) } ?: "Great Job"
        val nextTaskId = nextTask?.id ?: -1L

        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
          PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Base App Intent
        val appIntent = Intent(context, MainActivity::class.java).apply {
          setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, flags)

        // Complete Task Action Intent
        val completeIntent = Intent(context, LifeTrackerWidgetProvider::class.java).apply {
          action = ACTION_WIDGET_COMPLETE_TASK
          putExtra(EXTRA_TASK_ID, nextTaskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(context, 1, completeIntent, flags)

        // Meditate Action Intent
        val medIntent = Intent(context, LifeTrackerWidgetProvider::class.java).apply {
          action = ACTION_WIDGET_START_MEDITATION
        }
        val medPendingIntent = PendingIntent.getBroadcast(context, 2, medIntent, flags)

        val views: RemoteViews = if (minHeight >= 180 || minWidth >= 260) {
          // LARGE WIDGET
          RemoteViews(context.packageName, R.layout.widget_large).apply {
            setTextViewText(R.id.widget_large_progress_badge, "$completionPct% Done")
            setProgressBar(R.id.widget_large_progress_bar, 100, completionPct, false)
            setTextViewText(R.id.widget_large_current_title, nextTaskTitle)
            setTextViewText(R.id.widget_large_current_time, "$nextTaskTime • ${nextTask?.category ?: "Routine"}")

            val previewTasks = allTasks.take(4)
            val textViews = listOf(R.id.widget_large_task1, R.id.widget_large_task2, R.id.widget_large_task3, R.id.widget_large_task4)
            for (i in textViews.indices) {
              if (i < previewTasks.size) {
                val t = previewTasks[i]
                val mark = if (t.status == TaskStatus.COMPLETE) "✓" else if (t.priority == "HIGH") "⚡" else "⏳"
                setTextViewText(textViews[i], "$mark ${TimeUtils.minutesTo12Hour(t.timeMinutes)} — ${t.name}")
              } else {
                setTextViewText(textViews[i], "")
              }
            }

            setOnClickPendingIntent(R.id.widget_large_btn_complete, completePendingIntent)
            setOnClickPendingIntent(R.id.widget_large_btn_meditation, medPendingIntent)
            setOnClickPendingIntent(R.id.widget_large_btn_open, appPendingIntent)
            setOnClickPendingIntent(R.id.widget_large_root, appPendingIntent)
          }
        } else if (minWidth >= 190) {
          // MEDIUM WIDGET
          RemoteViews(context.packageName, R.layout.widget_medium).apply {
            setTextViewText(R.id.widget_medium_progress_pct, "$completionPct%")
            setTextViewText(R.id.widget_medium_progress_text, "$completedCount/$totalTasks Done")
            setProgressBar(R.id.widget_medium_progress_bar, 100, completionPct, false)
            setTextViewText(R.id.widget_medium_next_title, nextTaskTitle)
            setTextViewText(R.id.widget_medium_next_time, nextTaskTime)

            setOnClickPendingIntent(R.id.widget_medium_btn_complete, completePendingIntent)
            setOnClickPendingIntent(R.id.widget_medium_root, appPendingIntent)
          }
        } else {
          // SMALL WIDGET
          RemoteViews(context.packageName, R.layout.widget_small).apply {
            setTextViewText(R.id.widget_small_time, nextTaskTime)
            setTextViewText(R.id.widget_small_title, nextTaskTitle)
            val diff = (nextTask?.timeMinutes ?: nowMinutes) - nowMinutes
            val countdown = if (diff > 0) "In $diff minutes" else "Active Now"
            setTextViewText(R.id.widget_small_countdown, countdown)

            setOnClickPendingIntent(R.id.widget_small_root, appPendingIntent)
          }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
      }
    }
  }
}
