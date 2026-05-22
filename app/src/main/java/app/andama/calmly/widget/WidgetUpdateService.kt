package app.andama.calmly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.andama.calmly.MainActivity
import app.andama.calmly.R
import app.andama.calmly.achievements.AchievementManager

object WidgetUpdater {
    
    fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, CalmlyWidget::class.java)
        )
        
        if (appWidgetIds.isEmpty()) return
        
        val achievementManager = AchievementManager(context)
        
        // Get achievement data synchronously
        var totalSessions = 0
        var currentStreak = 0
        
        kotlinx.coroutines.runBlocking {
            achievementManager.achievementData.collect { data ->
                totalSessions = data.totalSessions
                currentStreak = data.currentStreak
                return@collect
            }
        }
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, totalSessions, currentStreak)
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        totalSessions: Int,
        currentStreak: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.calmly_widget)
        
        // Update streak text
        views.setTextViewText(
            R.id.widget_streak,
            "$currentStreak day${if (currentStreak != 1) "s" else ""} streak"
        )
        
        // Update sessions text
        views.setTextViewText(
            R.id.widget_sessions,
            "$totalSessions session${if (totalSessions != 1) "s" else ""}"
        )
        
        // Create intent to launch MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
