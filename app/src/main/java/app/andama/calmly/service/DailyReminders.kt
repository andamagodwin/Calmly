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
import app.andama.calmly.MainActivity
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.StreakInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Local daily notifications: a morning check-in nudge (skipped when the user has
 * already checked in) and a celebration on the morning a streak milestone lands.
 * Everything is scheduled on-device with AlarmManager — no push service involved.
 */
object DailyReminders {

    private const val REQUEST_CODE = 300
    private const val CHECKIN_NOTIFICATION_ID = 3001
    private const val MILESTONE_NOTIFICATION_ID = 3002
    private const val CHECKIN_CHANNEL_ID = "calmly_checkin_channel"
    private const val MILESTONE_CHANNEL_ID = "calmly_milestone_channel"
    private const val REMINDER_HOUR = 9

    /** Arms the daily alarm once; safe to call on every app launch. */
    fun scheduleIfNeeded(context: Context) {
        val intent = Intent(context, DailyReminderReceiver::class.java)
        val alreadyScheduled = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
        if (alreadyScheduled) return
        schedule(context)
    }

    /** Re-arms unconditionally — used after reboot, when alarms are wiped. */
    fun schedule(context: Context) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE,
            Intent(context, DailyReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMINDER_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact is fine — a nudge at 9:07 works as well as one at 9:00.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHECKIN_CHANNEL_ID,
                "Daily Check-In Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "A morning nudge to log your mood" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                MILESTONE_CHANNEL_ID,
                "Streak Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Celebrates the day you hit a streak milestone" }
        )
    }

    private fun openAppIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun showCheckInReminder(context: Context, streakDays: Int) {
        ensureChannels(context)
        val text = if (streakDays > 0) {
            "Day $streakDays. Thirty seconds to log how you're feeling — patterns win fights."
        } else {
            "Thirty seconds to log how you're feeling — patterns win fights."
        }
        val notification = NotificationCompat.Builder(context, CHECKIN_CHANNEL_ID)
            .setContentTitle("Morning check-in")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context, 301))
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(CHECKIN_NOTIFICATION_ID, notification)
    }

    fun showMilestoneNotification(context: Context, days: Int) {
        ensureChannels(context)
        val text = "$days days clean. That's not luck — that's ${days}x waking up and choosing. Keep the chain alive."
        val notification = NotificationCompat.Builder(context, MILESTONE_CHANNEL_ID)
            .setContentTitle("🏆 $days-day milestone")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setContentIntent(openAppIntent(context, 302))
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(MILESTONE_NOTIFICATION_ID, notification)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val streak = CalmlyTracker(context).getStreakInfo()
                if (!streak.checkedInToday) {
                    DailyReminders.showCheckInReminder(context, streak.days)
                }
                if (streak.days in StreakInfo.MILESTONES) {
                    DailyReminders.showMilestoneNotification(context, streak.days)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
