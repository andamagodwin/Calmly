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
import app.andama.calmly.data.Cal
import app.andama.calmly.data.CalMood
import app.andama.calmly.data.CalVoice
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.ScreenTimeInsights
import app.andama.calmly.data.ScreenTimeMonitor
import app.andama.calmly.data.StreakInfo
import app.andama.calmly.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Local daily notifications, all scheduled on-device with AlarmManager. Every one
 * of them arrives wearing Cal's face, and the face is picked from the same rules
 * the widget uses — so a bad night is something the user *sees* on the lockscreen
 * before they've read a word of it.
 *
 *  - 09:00 morning: check-in nudge (skipped once checked in), milestone
 *    celebration on milestone days, a comeback push on day zero, and — if last
 *    night ran long — a debrief naming the app that ate it.
 *  - 14:00 afternoon: Cal checks in. Either a restlessness warning (today's
 *    unlocks are spiking over the user's own baseline, which is the tell that
 *    tends to come *before* the urge) or an encouragement matched to how far in
 *    they actually are.
 *  - 21:00 evening: "streak defense" — the day is nearly banked; don't fumble
 *    it at the buzzer. Evenings are where most streaks die.
 */
object DailyReminders {

    const val ACTION_MORNING = "app.andama.calmly.REMINDER_MORNING"
    const val ACTION_AFTERNOON = "app.andama.calmly.REMINDER_AFTERNOON"
    const val ACTION_EVENING = "app.andama.calmly.REMINDER_EVENING"
    const val ACTION_LOCK_NOW = "app.andama.calmly.EVENING_LOCK_NOW"

    private const val MORNING_REQUEST_CODE = 300
    private const val EVENING_REQUEST_CODE = 310
    private const val AFTERNOON_REQUEST_CODE = 320
    private const val CHECKIN_NOTIFICATION_ID = 3001
    private const val MILESTONE_NOTIFICATION_ID = 3002
    private const val DEFENSE_NOTIFICATION_ID = 3003
    private const val CAL_NOTIFICATION_ID = 3004
    private const val DEBRIEF_NOTIFICATION_ID = 3005
    private const val CHECKIN_CHANNEL_ID = "calmly_checkin_channel"
    private const val MILESTONE_CHANNEL_ID = "calmly_milestone_channel"
    private const val DEFENSE_CHANNEL_ID = "calmly_defense_channel"
    private const val CAL_CHANNEL_ID = "calmly_cal_channel"
    private const val MORNING_HOUR = 9
    private const val AFTERNOON_HOUR = 14
    private const val EVENING_HOUR = 21

    /** A night worth talking about in the morning. Below this, silence is kinder. */
    private const val HEAVY_NIGHT_MINUTES = 45

    /** Arms the daily alarms once; safe to call on every app launch. */
    fun scheduleIfNeeded(context: Context) {
        val alreadyScheduled = PendingIntent.getBroadcast(
            context, AFTERNOON_REQUEST_CODE,
            Intent(context, DailyReminderReceiver::class.java).setAction(ACTION_AFTERNOON),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
        // Checked against the *newest* alarm: users upgrading from a build that
        // only had morning/evening would otherwise never get the afternoon one.
        if (alreadyScheduled) return
        schedule(context)
    }

    /** Re-arms unconditionally — used after reboot, when alarms are wiped. */
    fun schedule(context: Context) {
        scheduleDaily(context, MORNING_REQUEST_CODE, ACTION_MORNING, MORNING_HOUR)
        scheduleDaily(context, AFTERNOON_REQUEST_CODE, ACTION_AFTERNOON, AFTERNOON_HOUR)
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
        manager.createNotificationChannel(
            NotificationChannel(
                CAL_CHANNEL_ID,
                "Cal's Check-Ins",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Encouragement, and what Cal notices in your screen time" }
        )
    }

    private fun notify(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        requestCode: Int,
        route: String,
        mood: CalMood,
        @androidx.annotation.DrawableRes faceRes: Int? = null,
        highPriority: Boolean = true,
        actionLabel: String? = null,
        actionIntent: PendingIntent? = null
    ) {
        ensureChannels(context)
        // A specific expression (excited on a milestone, sleepy on the night
        // debrief) beats the generic mood face when the moment has a clear feeling.
        val icon = if (faceRes != null) CalIcon.face(context, faceRes)
                   else CalIcon.face(context, mood)
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(icon)
            .setPriority(if (highPriority) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(deepLinkIntent(context, route, requestCode))
            .setAutoCancel(true)
        if (actionLabel != null && actionIntent != null) {
            builder.addAction(0, actionLabel, actionIntent)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(notificationId, builder.build())
    }

    fun showMorning(
        context: Context,
        name: String?,
        streak: StreakInfo,
        mood: CalMood,
        lastNight: NightDebrief? = null
    ) {
        val who = name ?: "soldier"

        // The night that just ended is the most useful thing we know at 9am, and
        // it lands hardest before the day has buried it.
        lastNight?.let { showNightDebrief(context, who, it) }

        if (streak.days == 0) {
            // Day zero: the comeback push. This is the day the app matters most.
            // Routes to check-in — "name what happened" is literally that screen.
            notify(
                context, DEFENSE_CHANNEL_ID, DEFENSE_NOTIFICATION_ID,
                "Day zero, $who.",
                "Everyone falls. Not everyone gets back up the same morning. " +
                        "Check in, name what happened, and start the regrowth. Axolotls don't stay broken.",
                303,
                route = Screen.DailyCheckin.route,
                mood = mood
            )
            return
        }

        if (!streak.checkedInToday) {
            notify(
                context, CHECKIN_CHANNEL_ID, CHECKIN_NOTIFICATION_ID,
                "Morning, $who. Day ${streak.days}.",
                "Thirty seconds to log how you're feeling. Patterns win fights — and you're building one.",
                301,
                route = Screen.DailyCheckin.route,
                mood = mood,
                faceRes = R.drawable.face_shy,
                highPriority = false
            )
        }

        if (streak.days in StreakInfo.MILESTONES) {
            notify(
                context, MILESTONE_CHANNEL_ID, MILESTONE_NOTIFICATION_ID,
                "🏆 ${streak.days} days, $who.",
                "That's not luck — that's ${streak.days}x waking up and choosing. Cal is buzzing for you. Keep the chain alive.",
                302,
                route = Screen.Achievements.route,
                mood = CalMood.HAPPY,
                faceRes = R.drawable.face_excited
            )
        }
    }

    /** Last night's late hours, as far as the phone can tell. */
    data class NightDebrief(val minutes: Int, val topAppLabel: String?, val topAppMinutes: Int)

    private fun showNightDebrief(context: Context, who: String, night: NightDebrief) {
        val total = ScreenTimeInsights.formatMinutes(night.minutes)
        val culprit = night.topAppLabel
            ?.takeIf { night.topAppMinutes >= 5 }
            ?.let { " ${ScreenTimeInsights.formatMinutes(night.topAppMinutes)} of it in $it." }
            ?: ""

        notify(
            context, CAL_CHANNEL_ID, DEBRIEF_NOTIFICATION_ID,
            "Cal saw your night, $who.",
            "$total on your phone after 11pm.$culprit That's the shape of the nights that " +
                    "cost you. Look at it in daylight, while it's still just a number.",
            306,
            route = Screen.Patterns.route,
            mood = CalMood.STRUGGLING,
            faceRes = R.drawable.face_sleepy,
            highPriority = false
        )
    }

    fun showAfternoon(
        context: Context,
        name: String?,
        streak: StreakInfo,
        mood: CalMood,
        insights: ScreenTimeInsights.Insights?
    ) {
        val who = name ?: "soldier"

        // Restlessness first: a spike in unlocks over their *own* baseline is the
        // tell that shows up before the urge does, so it's worth interrupting for.
        val delta = insights?.restlessnessDelta
        if (insights?.isRestlessToday == true && delta != null) {
            notify(
                context, CAL_CHANNEL_ID, CAL_NOTIFICATION_ID,
                "Something's off today, $who.",
                "You've picked your phone up ${insights.todayUnlocks} times — $delta% more than " +
                        "your normal day. That itch is the thing that goes looking for something to " +
                        "do. Give it something else. Two minutes of breathing beats two hours of regret.",
                307,
                route = Screen.Breathing.route,
                mood = CalMood.STRUGGLING,
                faceRes = R.drawable.face_anxious
            )
            return
        }

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        notify(
            context, CAL_CHANNEL_ID, CAL_NOTIFICATION_ID,
            if (streak.days == 0) "Cal's still here, $who." else "Day ${streak.days}, $who.",
            CalVoice.encouragement(streak.days, who, dayOfYear),
            308,
            route = Screen.Home.route,
            mood = mood,
            highPriority = false
        )
    }

    fun showEvening(context: Context, name: String?, streak: StreakInfo, mood: CalMood) {
        // Nothing to defend on day zero evenings; the morning push handles it.
        if (streak.days == 0) return
        val who = name ?: "soldier"

        // The copy tells them to "hit the lock" — give them an actual button that
        // does it, rather than a promise the notification can't keep.
        val lockAction = PendingIntent.getBroadcast(
            context, 305,
            Intent(context, DailyReminderReceiver::class.java).setAction(ACTION_LOCK_NOW),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notify(
            context, DEFENSE_CHANNEL_ID, DEFENSE_NOTIFICATION_ID,
            "Defend day ${streak.days}, $who.",
            "The day is almost banked. Evenings are where streaks go to die — " +
                    "not yours, not tonight. If it gets loud, hit the lock and let it scream at a wall.",
            304,
            route = Screen.Home.route,
            mood = mood,
            faceRes = R.drawable.face_smug,
            actionLabel = "LOCK IT DOWN",
            actionIntent = lockAction
        )
    }

    fun lockDownNow(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(DEFENSE_NOTIFICATION_ID)
        OverlayService.startService(context, durationMs = 15 * 60 * 1000L, mode = "urge")
    }

    /**
     * Reads last night's late hours. Bounded at 05:00 rather than "since 23:00",
     * or the morning's own scrolling would get blamed on the night before.
     */
    internal fun readLastNight(monitor: ScreenTimeMonitor, minutes: Int): NightDebrief? {
        if (minutes < HEAVY_NIGHT_MINUTES) return null

        // 23:00 -> 05:00 is six hours across midnight; compute the span rather
        // than hard-coding it, so moving the band moves the debrief with it.
        val bandHours =
            (ScreenTimeMonitor.LATE_NIGHT_END - ScreenTimeMonitor.LATE_NIGHT_START + 24) % 24
        val start = monitor.windowStartMillis(ScreenTimeMonitor.LATE_NIGHT_START)
        val end = start + bandHours * 60L * 60L * 1000L

        val top = monitor.appUsageBetween(start, end, limit = 1).firstOrNull()
        return NightDebrief(
            minutes = minutes,
            topAppLabel = top?.label,
            topAppMinutes = top?.minutes ?: 0
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Synchronous, no tracker read needed — handle before the async branch.
        if (action == DailyReminders.ACTION_LOCK_NOW) {
            DailyReminders.lockDownNow(context)
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tracker = CalmlyTracker(context)
                val streak = tracker.getStreakInfo()
                val name = tracker.getUserName()

                val monitor = ScreenTimeMonitor(context)
                val insights = if (monitor.isSupported() && monitor.hasPermission()) {
                    ScreenTimeInsights.analyze(monitor.readRecentDays(), tracker.getRelapseDates())
                } else {
                    null
                }

                val mood = Cal.face(
                    tracker.getCalState().copy(isRestless = insights?.isRestlessToday == true)
                )

                when (action) {
                    DailyReminders.ACTION_MORNING -> DailyReminders.showMorning(
                        context, name, streak, mood,
                        lastNight = insights?.let {
                            DailyReminders.readLastNight(monitor, it.lastNightLateMinutes)
                        }
                    )

                    DailyReminders.ACTION_AFTERNOON ->
                        DailyReminders.showAfternoon(context, name, streak, mood, insights)

                    DailyReminders.ACTION_EVENING ->
                        DailyReminders.showEvening(context, name, streak, mood)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
