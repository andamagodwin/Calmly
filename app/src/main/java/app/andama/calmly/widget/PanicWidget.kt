package app.andama.calmly.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.andama.calmly.R
import app.andama.calmly.service.OverlayService

class PanicWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updatePanicWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_PANIC) {
            OverlayService.startService(context, durationMs = 15 * 60 * 1000L, mode = "urge")
        }
    }

    companion object {
        const val ACTION_PANIC = "app.andama.calmly.ACTION_PANIC"
    }
}

internal fun updatePanicWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.panic_widget)

    val intent = Intent(context, PanicWidget::class.java).apply {
        action = PanicWidget.ACTION_PANIC
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        1,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    views.setOnClickPendingIntent(R.id.panic_widget_root, pendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
