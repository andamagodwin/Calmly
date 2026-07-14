package app.andama.calmly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.andama.calmly.MainActivity
import app.andama.calmly.R
import app.andama.calmly.data.Cal
import app.andama.calmly.data.CalState
import app.andama.calmly.data.CalVoice
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.StreakInfo
import app.andama.calmly.ui.accentArgb
import app.andama.calmly.ui.faceRes

object WidgetUpdater {

    /**
     * Renders the streak widget from live tracker data. Suspend — a DataStore
     * flow never completes, so this must never block on collect (the original
     * implementation did exactly that and froze the caller). It also read the
     * *session* streak from AchievementManager, which is a different number
     * than the clean streak shown everywhere else in the app.
     */
    suspend fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, CalmlyWidget::class.java)
        )

        if (appWidgetIds.isEmpty()) return

        val tracker = CalmlyTracker(context)
        val streak = tracker.getStreakInfo()
        val state = tracker.getCalState()

        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, streak, state))
        }
    }

    private fun buildViews(context: Context, streak: StreakInfo, state: CalState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.calmly_widget)
        val mood = Cal.face(state)

        views.setTextViewText(R.id.widget_days, "${streak.days}")
        views.setTextViewText(
            R.id.widget_days_label,
            if (streak.days == 1) "DAY CLEAN" else "DAYS CLEAN"
        )

        // Cal is the whole point of the widget: a number on a home screen is easy
        // to stop seeing, a face that changes is not. The count takes his colour
        // so the state reads at a glance, from across the room, without squinting
        // at the caption.
        views.setImageViewResource(R.id.widget_face, mood.faceRes)
        views.setTextViewText(R.id.widget_cal_line, CalVoice.widgetLine(mood, state))
        views.setTextColor(R.id.widget_days, mood.accentArgb)
        views.setTextColor(R.id.widget_cal_line, mood.accentArgb)

        val next = streak.nextMilestone
        views.setTextViewText(
            R.id.widget_milestone,
            when {
                next == null -> "every milestone cleared 🏆"
                next - streak.days == 1 -> "${next}-day milestone tomorrow"
                else -> "${next}-day milestone in ${next - streak.days} days"
            }
        )
        views.setProgressBar(
            R.id.widget_progress, 100,
            (streak.milestoneProgress * 100).toInt(), false
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        return views
    }
}
