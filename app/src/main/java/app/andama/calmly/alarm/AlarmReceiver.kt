package app.andama.calmly.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Start the foreground service first - this works even when app is killed
        // The service's full-screen intent notification will launch AlarmActivity
        AlarmService.start(context)

        // Also try to launch the activity directly (works on older versions)
        try {
            val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(alarmIntent)
        } catch (_: Exception) {
            // If we can't start activity from background, the full-screen intent
            // from the foreground service notification will handle it
        }
    }
}
