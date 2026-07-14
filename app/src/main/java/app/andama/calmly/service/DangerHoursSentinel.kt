package app.andama.calmly.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Watches the user's self-declared danger window from the background.
 *
 * Before this existed, "danger hours" only did anything if the user happened to
 * open the app during the window — the one scenario where they least need a
 * reminder. The sentinel fires an exact alarm at the window's start, posts a
 * high-priority warning, and puts a one-tap LOCK ME DOWN action right in the
 * notification. It reschedules itself for the next day after each firing and is
 * re-armed by [app.andama.calmly.alarm.BootReceiver] after reboots.
 */
object DangerHoursSentinel {

    const val ACTION_WINDOW_OPEN = "app.andama.calmly.DANGER_WINDOW_OPEN"
    const val ACTION_LOCK_NOW = "app.andama.calmly.DANGER_LOCK_NOW"

    private const val REQUEST_CODE = 200
    private const val NOTIFICATION_ID = 2001
    private const val CHANNEL_ID = "calmly_danger_channel"
    private const val LOCK_DURATION_MS = 30 * 60 * 1000L

    /** Reads the saved settings and arms (or disarms) the daily alarm. */
    suspend fun reschedule(context: Context) {
        val dangerHours = CalmlyTracker(context).getDangerHours()
        if (dangerHours == null || !dangerHours.third) {
            cancel(context)
            return
        }
        scheduleNext(context, dangerHours.first)
    }

    private fun scheduleNext(context: Context, startHour: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, next.timeInMillis, pendingIntent(context)
            )
        } else {
            // Late is still better than never.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP, next.timeInMillis, 10 * 60 * 1000L, pendingIntent(context)
            )
        }
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DangerHoursReceiver::class.java).apply {
            action = ACTION_WINDOW_OPEN
        }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun lockDownNow(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
        OverlayService.startService(context, durationMs = LOCK_DURATION_MS, mode = "urge")
    }

    fun showWindowOpenNotification(context: Context, name: String? = null) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Danger Window Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Fires when your high-risk hours begin"
                }
            )
        }

        // The danger banner lives on Home, so that's exactly where this should land.
        val openApp = deepLinkIntent(context, Screen.Home.route, 201)

        val lockNow = PendingIntent.getBroadcast(
            context, 202,
            Intent(context, DangerHoursReceiver::class.java).apply {
                action = ACTION_LOCK_NOW
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val who = name?.let { "$it, your" } ?: "Your"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("$who danger window just opened")
            .setContentText("This is the hour that usually wins. Not tonight. Lock down before it starts.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "This is the hour that usually wins. Not tonight. " +
                            "Lock down before the urge shows up, or open Calmly and get ahead of it."
                )
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .addAction(0, "LOCK ME DOWN", lockNow)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

class DangerHoursReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DangerHoursSentinel.ACTION_WINDOW_OPEN -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Settings may have been disabled since the alarm was armed.
                        val tracker = CalmlyTracker(context)
                        val dangerHours = tracker.getDangerHours()
                        if (dangerHours != null && dangerHours.third) {
                            DangerHoursSentinel.showWindowOpenNotification(context, tracker.getUserName())
                        }
                        DangerHoursSentinel.reschedule(context)
                    } finally {
                        pending.finish()
                    }
                }
            }

            DangerHoursSentinel.ACTION_LOCK_NOW -> DangerHoursSentinel.lockDownNow(context)
        }
    }
}
