package app.andama.calmly.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.andama.calmly.MainActivity

/**
 * Builds a PendingIntent that opens the app and jumps straight to [route] once
 * the nav graph is up — instead of every notification dumping the user back on
 * whatever screen the app happened to be showing (or the home screen) with no
 * connection to what the notification was actually about.
 */
fun deepLinkIntent(context: Context, route: String, requestCode: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_DEEPLINK_ROUTE, route)
    }
    return PendingIntent.getActivity(
        context, requestCode, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
