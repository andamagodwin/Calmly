package app.andama.calmly.data

import app.andama.calmly.data.ScreenTimeMonitor.DayUsage
import kotlin.math.roundToInt

/**
 * Derives actionable signals from raw screen-time buckets.
 *
 * Deliberately pure (no Context, no Android types) so the reasoning that drives
 * user-facing recommendations is unit-testable — these numbers get shown to
 * someone in a vulnerable moment, so they need to be right.
 */
object ScreenTimeInsights {

    /** Below this, a night's usage isn't meaningfully "late-night phone use". */
    private const val MIN_MEANINGFUL_NIGHT_MINUTES = 15

    /** An hour needs at least this much average use to count as part of a window. */
    private const val WINDOW_HOUR_THRESHOLD_MINUTES = 8

    /** Need this many relapses before a correlation is worth showing at all. */
    private const val MIN_RELAPSES_FOR_CORRELATION = 2

    /** Relapse nights must exceed clean nights by this ratio to be worth surfacing. */
    private const val CORRELATION_RATIO = 1.5f

    /** Hours scanned when suggesting a danger window, in clock order. */
    private val NIGHT_BAND = listOf(20, 21, 22, 23, 0, 1, 2, 3, 4, 5)

    data class RelapseCorrelation(
        val relapseNightAvgMinutes: Int,
        val cleanNightAvgMinutes: Int,
        val relapseNightsSampled: Int
    ) {
        /** How many times more late-night screen time precedes a relapse. */
        val multiplier: Float
            get() = if (cleanNightAvgMinutes <= 0) Float.MAX_VALUE
            else relapseNightAvgMinutes.toFloat() / cleanNightAvgMinutes
    }

    data class Insights(
        val daysAnalyzed: Int,
        val avgDailyMinutes: Int,
        val avgLateNightMinutes: Int,
        val lastNightLateMinutes: Int,
        val todayUnlocks: Int,
        val baselineUnlocks: Int,
        val suggestedWindow: Pair<Int, Int>?,
        val correlation: RelapseCorrelation?,
        /** Late-night minutes per day, oldest first — drives the little bar chart. */
        val lateNightTrend: List<Int>
    ) {
        /**
         * Today's unlock count as a percentage change from the user's own
         * baseline. Restless, repetitive phone-checking is a recognised
         * pre-relapse state, so a spike is worth flagging.
         */
        val restlessnessDelta: Int?
            get() {
                if (baselineUnlocks <= 0) return null
                return (((todayUnlocks - baselineUnlocks).toFloat() / baselineUnlocks) * 100).roundToInt()
            }

        val isRestlessToday: Boolean
            get() = (restlessnessDelta ?: 0) >= 30 && todayUnlocks > baselineUnlocks
    }

    fun analyze(days: List<DayUsage>, relapseDates: Set<String>): Insights? {
        if (days.isEmpty()) return null

        val today = days.last()
        val priorDays = days.dropLast(1)

        return Insights(
            daysAnalyzed = days.size,
            avgDailyMinutes = days.map { it.totalMinutes }.average().roundToInt(),
            avgLateNightMinutes = days.map { it.lateNightMinutes }.average().roundToInt(),
            // "Last night" is the night that just ended, i.e. yesterday's 23:00
            // through this morning's 05:00. The 00:00–05:00 half of that lands on
            // *today's* date, so it must be summed across both days.
            lastNightLateMinutes = lastNightMinutes(days),
            todayUnlocks = today.unlocks,
            baselineUnlocks = median(priorDays.map { it.unlocks }),
            suggestedWindow = suggestWindow(days),
            correlation = correlateWithRelapses(days, relapseDates),
            lateNightTrend = days.map { it.lateNightMinutes }
        )
    }

    /**
     * The night spanning yesterday evening into this morning: yesterday's 23:00+
     * plus today's pre-05:00. Treating "last night" as a single calendar day
     * would silently drop the 1am–3am hours, which are the ones that matter most.
     */
    private fun lastNightMinutes(days: List<DayUsage>): Int {
        val today = days.last()
        val yesterday = days.getOrNull(days.size - 2)
        val eveningPart = yesterday?.minutesInBand(ScreenTimeMonitor.LATE_NIGHT_START, 24) ?: 0
        val morningPart = today.minutesInBand(0, ScreenTimeMonitor.LATE_NIGHT_END)
        return eveningPart + morningPart
    }

    /**
     * Finds the stretch of night hours the user is actually awake and on their
     * phone, so the app can propose a real danger window instead of asking them
     * to guess one.
     *
     * Takes the busiest night hour, then expands outward while neighbouring hours
     * are still meaningfully active. Returns null when there's no real late-night
     * pattern to act on — a false window would train the user to ignore the lock.
     */
    fun suggestWindow(days: List<DayUsage>): Pair<Int, Int>? {
        if (days.isEmpty()) return null

        val avgByHour = NIGHT_BAND.associateWith { hour ->
            days.map { it.minutesPerHour[hour] }.average()
        }

        val peakHour = avgByHour.maxByOrNull { it.value }?.key ?: return null
        val peakMinutes = avgByHour[peakHour] ?: 0.0
        if (peakMinutes < WINDOW_HOUR_THRESHOLD_MINUTES) return null

        val peakIndex = NIGHT_BAND.indexOf(peakHour)
        val threshold = maxOf(WINDOW_HOUR_THRESHOLD_MINUTES.toDouble(), peakMinutes * 0.35)

        var startIndex = peakIndex
        while (startIndex > 0 && (avgByHour[NIGHT_BAND[startIndex - 1]] ?: 0.0) >= threshold) {
            startIndex--
        }

        var endIndex = peakIndex
        while (endIndex < NIGHT_BAND.lastIndex &&
            (avgByHour[NIGHT_BAND[endIndex + 1]] ?: 0.0) >= threshold
        ) {
            endIndex++
        }

        val startHour = NIGHT_BAND[startIndex]
        // The window is exclusive of its end hour: a run ending at 02:00 means the
        // user is on their phone *during* 2am, so the window must close at 3am.
        val endHour = (NIGHT_BAND[endIndex] + 1) % 24
        return startHour to endHour
    }

    /**
     * Compares late-night screen time on the nights leading into a relapse
     * against clean nights.
     *
     * This is the insight that can actually change behaviour: not "you use your
     * phone a lot" but "this specific pattern is what precedes your relapses."
     * Returns null unless there's enough data AND a real difference — inventing a
     * correlation from two data points would be worse than saying nothing.
     */
    fun correlateWithRelapses(
        days: List<DayUsage>,
        relapseDates: Set<String>
    ): RelapseCorrelation? {
        if (relapseDates.isEmpty()) return null

        val relapseNights = days.filter { it.date in relapseDates }
        val cleanNights = days.filter { it.date !in relapseDates }

        if (relapseNights.size < MIN_RELAPSES_FOR_CORRELATION) return null
        if (cleanNights.isEmpty()) return null

        val relapseAvg = relapseNights.map { it.lateNightMinutes }.average().roundToInt()
        val cleanAvg = cleanNights.map { it.lateNightMinutes }.average().roundToInt()

        // Don't cry wolf over a trivial difference, or over nights where nobody
        // was really on their phone.
        if (relapseAvg < MIN_MEANINGFUL_NIGHT_MINUTES) return null
        if (relapseAvg < cleanAvg * CORRELATION_RATIO) return null

        return RelapseCorrelation(
            relapseNightAvgMinutes = relapseAvg,
            cleanNightAvgMinutes = cleanAvg,
            relapseNightsSampled = relapseNights.size
        )
    }

    private fun median(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            ((sorted[mid - 1] + sorted[mid]) / 2.0).roundToInt()
        } else {
            sorted[mid]
        }
    }

    /**
     * How hard Calmly should push, given how long the user has been on their
     * phone since their danger window opened.
     *
     * Escalates rather than nagging at a fixed volume: the first message is a
     * tap on the shoulder, the last one is a shove. Level 0 means say nothing —
     * a warning that fires the instant the window opens is a warning people
     * learn to swipe away without reading.
     */
    fun escalationLevel(minutesInWindow: Int): Int = when {
        minutesInWindow >= ESCALATE_SEVERE -> 3
        minutesInWindow >= ESCALATE_FIRM -> 2
        minutesInWindow >= ESCALATE_NUDGE -> 1
        else -> 0
    }

    const val ESCALATE_NUDGE = 20
    const val ESCALATE_FIRM = 45
    const val ESCALATE_SEVERE = 75

    /** "1h 47m" / "47m" */
    fun formatMinutes(minutes: Int): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }

    /** "23:00 → 03:00" */
    fun formatWindow(window: Pair<Int, Int>): String =
        "%02d:00 → %02d:00".format(window.first, window.second)
}
