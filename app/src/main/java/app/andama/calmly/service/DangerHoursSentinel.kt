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
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.ScreenTimeInsights
import app.andama.calmly.data.ScreenTimeMonitor
import app.andama.calmly.navigation.Screen
import app.andama.calmly.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    const val ACTION_PATROL = "app.andama.calmly.DANGER_PATROL"

    private const val REQUEST_CODE = 200
    private const val PATROL_REQUEST_CODE = 210
    private const val NOTIFICATION_ID = 2001
    private const val PATROL_NOTIFICATION_ID = 2002
    private const val CHANNEL_ID = "calmly_danger_channel"
    private const val PATROL_CHANNEL_ID = "calmly_patrol_channel"
    private const val LOCK_DURATION_MS = 30 * 60 * 1000L

    /** How often the patrol wakes while the user is inside their danger window. */
    private const val PATROL_INTERVAL_MS = 15 * 60 * 1000L

    /** Reads the saved settings and arms (or disarms) the daily alarm. */
    suspend fun reschedule(context: Context) {
        val tracker = CalmlyTracker(context)
        val dangerHours = tracker.getDangerHours()
        if (dangerHours == null || !dangerHours.third) {
            cancel(context)
            cancelPatrol(context)
            return
        }
        scheduleNext(context, dangerHours.first)

        // If we're already inside the window — the user just switched the feature
        // on at 23:30, or the phone rebooted at 1am — start patrolling now rather
        // than leaving them unwatched until tomorrow night.
        if (tracker.isInDangerHours()) {
            schedulePatrol(context, delayMs = 60_000L)
        }
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
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
        manager.cancel(PATROL_NOTIFICATION_ID)
        OverlayService.startService(context, durationMs = LOCK_DURATION_MS, mode = "urge")
    }

    // ---------------------------------------------------------------------
    // Patrol: the live watchdog inside the danger window.
    //
    // Firing once when the window opens isn't much help — the danger isn't the
    // clock striking 11, it's still being on the phone at 12:30. The patrol wakes
    // every 15 minutes while the window is open, measures how long the screen has
    // actually been on since it opened, and escalates. It names the app that's
    // eating the time, because "you've been on your phone for 45 minutes" is easy
    // to shrug off and "you've been in Instagram for 38 of the last 45 minutes"
    // is not.
    // ---------------------------------------------------------------------

    fun schedulePatrol(context: Context, delayMs: Long = PATROL_INTERVAL_MS) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + delayMs
        val pending = patrolPendingIntent(context)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
        if (canExact) {
            // AllowWhileIdle: this needs to fire at 1am with the phone dozing,
            // which is exactly when Doze would otherwise defer it.
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at, 5 * 60 * 1000L, pending)
        }
    }

    fun cancelPatrol(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(patrolPendingIntent(context))
    }

    private fun patrolPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, PATROL_REQUEST_CODE,
            Intent(context, DangerHoursReceiver::class.java).setAction(ACTION_PATROL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /**
     * One patrol tick. Reschedules itself while the window stays open, and stops
     * cleanly the moment it closes or the feature is switched off.
     */
    suspend fun runPatrol(context: Context) {
        val tracker = CalmlyTracker(context)
        val dangerHours = tracker.getDangerHours()

        // Window closed, or the user turned the feature off since the last tick.
        if (dangerHours == null || !dangerHours.third || !tracker.isInDangerHours()) {
            cancelPatrol(context)
            return
        }

        val monitor = ScreenTimeMonitor(context)
        if (!monitor.isSupported() || !monitor.hasPermission()) {
            // Nothing to measure — the window-open notification is all we've got.
            cancelPatrol(context)
            return
        }

        // They're already locked down — they did the thing we'd be asking for.
        // Keep patrolling (the lock ends before the window does) but don't shout
        // at someone who is currently staring at our own overlay.
        if (OverlayService.isLocked) {
            schedulePatrol(context)
            return
        }

        val windowStart = monitor.windowStartMillis(dangerHours.first)
        val minutesOnPhone = monitor.screenOnMinutesSince(windowStart)
        val level = ScreenTimeInsights.escalationLevel(minutesOnPhone)

        // The night is keyed by when the window opened, not by today's date —
        // otherwise a window spanning midnight would reset its own escalation
        // ladder halfway through and start nagging from level 1 again.
        val nightKey = NIGHT_KEY_FORMAT.format(Date(windowStart))

        if (level > tracker.getPatrolLevel(nightKey)) {
            val topApp = monitor.appUsageSince(windowStart, limit = 1).firstOrNull()
            showPatrolNotification(
                context = context,
                name = tracker.getUserName(),
                level = level,
                minutesOnPhone = minutesOnPhone,
                topApp = topApp,
                // The stored level is still the old one, so feed in the level we
                // just measured: the face must match the words it arrives with.
                mood = Cal.face(tracker.getCalState().copy(escalation = level))
            )
            tracker.setPatrolLevel(nightKey, level)
            // Cal's face on the home screen sours in step with the escalation —
            // the widget is the one place they'll see him without opening anything.
            WidgetUpdater.updateWidget(context)
        }

        schedulePatrol(context)
    }

    private fun showPatrolNotification(
        context: Context,
        name: String?,
        level: Int,
        minutesOnPhone: Int,
        topApp: ScreenTimeMonitor.AppUsage?,
        mood: CalMood
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    PATROL_CHANNEL_ID,
                    "Danger Window Patrol",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Warns you when you're still on your phone deep in your danger window"
                }
            )
        }

        val who = name ?: "soldier"
        val onPhone = ScreenTimeInsights.formatMinutes(minutesOnPhone)

        val title = when (level) {
            1 -> "$onPhone into your danger window"
            2 -> "$who. $onPhone. Still scrolling."
            else -> "This is how it starts, $who."
        }

        val culprit = topApp
            ?.takeIf { it.minutes >= 5 }
            ?.let { " ${it.label} has taken ${ScreenTimeInsights.formatMinutes(it.minutes)} of it." }
            ?: ""

        val body = when (level) {
            1 -> "You're inside the hours that usually get you.$culprit " +
                    "Put it down now and tonight is a non-event."
            2 -> "You're $onPhone deep into the window with no sign of stopping.$culprit " +
                    "You know exactly where this road goes. Get off it."
            else -> "$onPhone in your danger window.$culprit This is the exact shape of every " +
                    "night you've regretted. Not tonight. Lock it down and go to bed."
        }

        val notification = NotificationCompat.Builder(context, PATROL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(CalIcon.face(context, mood))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(deepLinkIntent(context, Screen.Home.route, 211))
            .addAction(
                0,
                if (level >= 3) "LOCK IT DOWN. NOW." else "LOCK ME DOWN",
                PendingIntent.getBroadcast(
                    context, 212,
                    Intent(context, DangerHoursReceiver::class.java).setAction(ACTION_LOCK_NOW),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(PATROL_NOTIFICATION_ID, notification)
    }

    private val NIGHT_KEY_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun showWindowOpenNotification(
        context: Context,
        name: String? = null,
        mood: CalMood = CalMood.FIERCE
    ) {
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
            .setLargeIcon(CalIcon.face(context, mood))
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
                            DangerHoursSentinel.showWindowOpenNotification(
                                context,
                                tracker.getUserName(),
                                Cal.face(tracker.getCalState())
                            )
                            // Start watching. The opening bell is the least useful
                            // warning of the night; the ones that follow are the point.
                            DangerHoursSentinel.schedulePatrol(context)
                            // Cal's game face goes up on the home screen too.
                            WidgetUpdater.updateWidget(context)
                        }
                        DangerHoursSentinel.reschedule(context)
                    } finally {
                        pending.finish()
                    }
                }
            }

            DangerHoursSentinel.ACTION_PATROL -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        DangerHoursSentinel.runPatrol(context)
                    } finally {
                        pending.finish()
                    }
                }
            }

            DangerHoursSentinel.ACTION_LOCK_NOW -> DangerHoursSentinel.lockDownNow(context)
        }
    }
}
