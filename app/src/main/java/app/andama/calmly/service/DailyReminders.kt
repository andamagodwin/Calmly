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
 * Local daily notifications, all scheduled on-device with AlarmManager:
 *
 *  - 09:00 morning: check-in nudge (skipped once checked in), milestone
 *    celebration on milestone days, and a comeback push on day zero.
 *  - 21:00 evening: "streak defense" — the day is nearly banked; don't fumble
 *    it at the buzzer. Evenings are when most streaks die.
 */
object DailyReminders {

    const val ACTION_MORNING = "app.andama.calmly.REMINDER_MORNING"
    const val ACTION_EVENING = "app.andama.calmly.REMINDER_EVENING"

    private const val MORNING_REQUEST_CODE = 300
    private const val EVENING_REQUEST_CODE = 310
    private const val CHECKIN_NOTIFICATION_ID = 3001
    private const val MILESTONE_NOTIFICATION_ID = 3002
    private const val DEFENSE_NOTIFICATION_ID = 3003
    private const val CHECKIN_CHANNEL_ID = "calmly_checkin_channel"
    private const val MILESTONE_CHANNEL_ID = "calmly_milestone_channel"
    private const val DEFENSE_CHANNEL_ID = "calmly_defense_channel"
    private const val MORNING_HOUR = 9
    private const val EVENING_HOUR = 21

    /** Arms both daily alarms once; safe to call on every app launch. */
    fun scheduleIfNeeded(context: Context) {
        val alreadyScheduled = PendingIntent.getBroadcast(
            context, MORNING_REQUEST_CODE,
            Intent(context, DailyReminderReceiver::class.java).setAction(ACTION_MORNING),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
        if (alreadyScheduled) return
        schedule(context)
    }

    /** Re-arms unconditionally — used after reboot, when alarms are wiped. */
    fun schedule(context: Context) {
        scheduleDaily(context, MORNING_REQUEST_CODE, ACTION_MORNING, MORNING_HOUR)
        scheduleDaily(context, EVENING_REQUEST_CODE, ACTION_EVENING, EVENING_HOUR)
    }

    private fun scheduleDaily(context: Context, requestCode: Int, action: String, hour: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, DailyReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
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
        manager.createNotificationChannel(
            NotificationChannel(
                DEFENSE_CHANNEL_ID,
                "Streak Defense",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Evening reminder to protect the day you've built" }
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

    private fun notify(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        requestCode: Int,
        highPriority: Boolean = true
    ) {
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openAppIntent(context, requestCode))
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, notification)
    }

    fun showMorning(context: Context, name: String?, streak: StreakInfo) {
        val who = name ?: "soldier"

        if (streak.days == 0) {
            // Day zero: the comeback push. This is the day the app matters most.
            notify(
                context, DEFENSE_CHANNEL_ID, DEFENSE_NOTIFICATION_ID,
                "Day zero, $who.",
                "Everyone falls. Not everyone gets back up the same morning. " +
                        "Check in, name what happened, and start the regrowth. Axolotls don't stay broken.",
                303
            )
            return
        }

        if (!streak.checkedInToday) {
            notify(
                context, CHECKIN_CHANNEL_ID, CHECKIN_NOTIFICATION_ID,
                "Morning, $who. Day ${streak.days}.",
                "Thirty seconds to log how you're feeling. Patterns win fights — and you're building one.",
                301,
                highPriority = false
            )
        }

        if (streak.days in StreakInfo.MILESTONES) {
            notify(
                context, MILESTONE_CHANNEL_ID, MILESTONE_NOTIFICATION_ID,
                "🏆 ${streak.days} days, $who.",
                "That's not luck — that's ${streak.days}x waking up and choosing. Cal is flexing for you. Keep the chain alive.",
                302
            )
        }
    }

    fun showEvening(context: Context, name: String?, streak: StreakInfo) {
        // Nothing to defend on day zero evenings; the morning push handles it.
        if (streak.days == 0) return
        val who = name ?: "soldier"
        notify(
            context, DEFENSE_CHANNEL_ID, DEFENSE_NOTIFICATION_ID,
            "Defend day ${streak.days}, $who.",
            "The day is almost banked. Evenings are where streaks go to die — " +
                    "not yours, not tonight. If it gets loud, hit the lock and let it scream at a wall.",
            304
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tracker = CalmlyTracker(context)
                val streak = tracker.getStreakInfo()
                val name = tracker.getUserName()
                when (action) {
                    DailyReminders.ACTION_MORNING -> DailyReminders.showMorning(context, name, streak)
                    DailyReminders.ACTION_EVENING -> DailyReminders.showEvening(context, name, streak)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
