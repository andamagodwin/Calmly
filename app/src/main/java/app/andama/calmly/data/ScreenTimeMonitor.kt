package app.andama.calmly.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Screen-time signals, read from [UsageStatsManager].
 *
 * Reads three things: when the screen was on, how often the phone was unlocked,
 * and which apps were in the foreground. The app-level data is what makes the
 * late-night intervention specific ("you've been in X for 32 minutes") rather
 * than vague ("you've been on your phone").
 *
 * Everything is computed on-device and held in memory or the local DataStore.
 * The app has no network code and no account — none of this can leave the phone.
 */
class ScreenTimeMonitor(private val context: Context) {

    /**
     * Screen-on minutes bucketed by hour-of-day, for one calendar day.
     * [minutesPerHour] is always length 24, index = hour.
     */
    data class DayUsage(
        val date: String,
        val minutesPerHour: List<Int>,
        val unlocks: Int
    ) {
        val totalMinutes: Int get() = minutesPerHour.sum()

        /**
         * Minutes inside a time band, handling windows that wrap past midnight
         * (23→5 is six hours, not a negative span).
         */
        fun minutesInBand(startHour: Int, endHour: Int): Int {
            if (startHour == endHour) return 0
            val hours = if (startHour < endHour) {
                (startHour until endHour)
            } else {
                (startHour until 24) + (0 until endHour)
            }
            return hours.sumOf { minutesPerHour[it] }
        }

        /** The band that matters for this app: 23:00 → 05:00. */
        val lateNightMinutes: Int get() = minutesInBand(LATE_NIGHT_START, LATE_NIGHT_END)
    }

    fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Usage access is a special permission — it can't be requested inline, only
     * granted by the user in system settings.
     */
    fun permissionSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** True when the OS is too old to report screen on/off events at all. */
    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    /**
     * Reads the last [days] calendar days of screen activity. Returns oldest
     * first; today is the last element. Empty if unsupported or not permitted.
     */
    fun readRecentDays(days: Int = 14): List<DayUsage> {
        if (!isSupported() || !hasPermission()) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val start = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -(days - 1))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = System.currentTimeMillis()

        // date -> minutes per hour, and date -> unlock count
        val perDayMinutes = mutableMapOf<String, IntArray>()
        val perDayUnlocks = mutableMapOf<String, Int>()

        fun bucketsFor(date: String) = perDayMinutes.getOrPut(date) { IntArray(24) }

        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()

        var screenOnAt = -1L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // NOTE: event.packageName is deliberately never touched. See class doc.
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenOnAt = event.timeStamp
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (screenOnAt > 0L) {
                        spreadSessionAcrossHours(screenOnAt, event.timeStamp, ::bucketsFor)
                        screenOnAt = -1L
                    }
                }

                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    val date = dateKey(event.timeStamp)
                    perDayUnlocks[date] = (perDayUnlocks[date] ?: 0) + 1
                }
            }
        }

        // The screen may still be on right now (it is — the user is looking at us).
        if (screenOnAt > 0L) {
            spreadSessionAcrossHours(screenOnAt, end, ::bucketsFor)
        }

        return (0 until days).map { offset ->
            val date = dateKey(
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -(days - 1 - offset))
                }.timeInMillis
            )
            DayUsage(
                date = date,
                minutesPerHour = (perDayMinutes[date] ?: IntArray(24)).toList(),
                unlocks = perDayUnlocks[date] ?: 0
            )
        }
    }

    /** Foreground time for a single app over some window. */
    data class AppUsage(
        val packageName: String,
        val label: String,
        val minutes: Int
    )

    /**
     * Minutes the screen was on between [startMs] and now.
     *
     * Computed from the raw event stream rather than the hourly buckets, because
     * the danger-window patrol needs a live figure mid-hour and mid-window — and
     * a window like 23:00→02:00 straddles midnight, where bucket arithmetic is
     * easy to get subtly wrong.
     */
    fun screenOnMinutesSince(startMs: Long): Int {
        if (!isSupported() || !hasPermission()) return 0
        val now = System.currentTimeMillis()
        if (startMs >= now) return 0

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startMs, now)
        val event = UsageEvents.Event()

        var total = 0L
        // The screen may already have been on when the window opened, so assume on
        // until told otherwise; the first SCREEN_NON_INTERACTIVE corrects us.
        var screenOnAt = startMs

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> screenOnAt = event.timeStamp
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    if (screenOnAt > 0L) {
                        total += (event.timeStamp - screenOnAt).coerceAtLeast(0L)
                        screenOnAt = -1L
                    }
                }
            }
        }
        if (screenOnAt > 0L) total += (now - screenOnAt).coerceAtLeast(0L)

        return (total / 60_000L).toInt()
    }

    /**
     * Foreground time per app between [startMs] and now, biggest first.
     * Calmly itself is excluded — the user opening their recovery app is not the
     * behaviour we want to warn them about.
     */
    fun appUsageSince(startMs: Long, limit: Int = 5): List<AppUsage> =
        appUsageBetween(startMs, System.currentTimeMillis(), limit)

    /**
     * Foreground time per app within a closed window. The morning debrief needs
     * this: "since 23:00" at 9am would sweep in the commute and the breakfast
     * scroll, and blame them on last night.
     */
    fun appUsageBetween(startMs: Long, endMs: Long, limit: Int = 5): List<AppUsage> {
        if (!isSupported() || !hasPermission()) return emptyList()
        val end = minOf(endMs, System.currentTimeMillis())
        if (startMs >= end) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startMs, end)
        val event = UsageEvents.Event()

        val totals = mutableMapOf<String, Long>()
        val openedAt = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> openedAt[pkg] = event.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = openedAt.remove(pkg) ?: continue
                    totals[pkg] = (totals[pkg] ?: 0L) + (event.timeStamp - start).coerceAtLeast(0L)
                }
            }
        }
        // Anything still open when the window closed was never paused, so it has
        // no closing event to pair with — credit it up to the window's end.
        openedAt.forEach { (pkg, start) ->
            totals[pkg] = (totals[pkg] ?: 0L) + (end - start).coerceAtLeast(0L)
        }

        val packageManager = context.packageManager
        return totals
            .filterKeys { it != context.packageName }
            .map { (pkg, millis) ->
                AppUsage(
                    packageName = pkg,
                    label = appLabel(packageManager, pkg),
                    minutes = (millis / 60_000L).toInt()
                )
            }
            .filter { it.minutes >= 1 }
            .sortedByDescending { it.minutes }
            .take(limit)
    }

    private fun appLabel(pm: android.content.pm.PackageManager, pkg: String): String = try {
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Exception) {
        // Uninstalled since the event was recorded, or a system package with no label.
        pkg.substringAfterLast('.')
    }

    /**
     * The timestamp at which the current (or most recent) danger window opened.
     * If it's 00:30 and the window starts at 23:00, that was *yesterday* — get
     * this wrong and the patrol measures the wrong stretch of the night entirely.
     */
    fun windowStartMillis(startHour: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis > System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal.timeInMillis
    }

    /**
     * A screen session can straddle hour boundaries and midnight (which is
     * exactly what a 1am doomscroll does), so credit each minute to the hour and
     * the day it actually happened in rather than to the hour the session began.
     */
    private fun spreadSessionAcrossHours(
        startMs: Long,
        endMs: Long,
        bucketsFor: (String) -> IntArray
    ) {
        if (endMs <= startMs) return
        // Guard against a pathological session (e.g. an OS that never emitted the
        // screen-off event) poisoning the averages.
        if (endMs - startMs > MAX_SESSION_MS) return

        val cursor = Calendar.getInstance().apply { timeInMillis = startMs }
        var remaining = endMs - startMs

        while (remaining > 0) {
            val hour = cursor.get(Calendar.HOUR_OF_DAY)
            val date = dateKey(cursor.timeInMillis)

            // Milliseconds left in the hour the cursor currently sits in.
            val msLeftInHour = HOUR_MS -
                (cursor.get(Calendar.MINUTE) * 60_000L + cursor.get(Calendar.SECOND) * 1000L)
            val chunk = minOf(remaining, msLeftInHour)

            bucketsFor(date)[hour] += (chunk / 60_000L).toInt()

            remaining -= chunk
            cursor.timeInMillis += chunk
        }
    }

    private fun dateKey(millis: Long): String = DATE_FORMAT.format(Date(millis))

    companion object {
        const val LATE_NIGHT_START = 23
        const val LATE_NIGHT_END = 5

        private const val HOUR_MS = 60 * 60 * 1000L
        private const val MAX_SESSION_MS = 8 * HOUR_MS

        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
