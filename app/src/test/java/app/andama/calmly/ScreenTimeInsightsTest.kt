package app.andama.calmly

import app.andama.calmly.data.ScreenTimeInsights
import app.andama.calmly.data.ScreenTimeMonitor.DayUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeInsightsTest {

    /** Builds a day with the given (hour -> minutes) usage. */
    private fun day(
        date: String,
        vararg hourMinutes: Pair<Int, Int>,
        unlocks: Int = 0
    ): DayUsage {
        val buckets = MutableList(24) { 0 }
        hourMinutes.forEach { (hour, minutes) -> buckets[hour] = minutes }
        return DayUsage(date = date, minutesPerHour = buckets, unlocks = unlocks)
    }

    // --- band maths ---

    @Test
    fun `late night band wraps across midnight`() {
        val d = day("2026-01-01", 23 to 30, 0 to 40, 4 to 20, 12 to 99)
        // 23:00 + 00:00 + 04:00 count; midday does not.
        assertEquals(90, d.lateNightMinutes)
    }

    @Test
    fun `band excludes its end hour`() {
        val d = day("2026-01-01", 5 to 60)
        // The 23->5 band must not swallow the 5am hour itself.
        assertEquals(0, d.lateNightMinutes)
    }

    // --- window suggestion ---

    @Test
    fun `suggests the window covering the users actual late night usage`() {
        // Consistently on the phone 23:00-01:59 across a week.
        val days = (1..7).map {
            day("2026-01-0$it", 23 to 45, 0 to 50, 1 to 40)
        }
        val window = ScreenTimeInsights.suggestWindow(days)
        assertNotNull(window)
        // Runs 23,0,1 -> closes at 02:00, since 1am usage means the window is
        // still live during that hour.
        assertEquals(23 to 2, window)
    }

    @Test
    fun `no window suggested when there is no real late night pattern`() {
        // Heavy daytime use, essentially nothing at night.
        val days = (1..7).map { day("2026-01-0$it", 13 to 120, 19 to 90, 23 to 2) }
        assertNull(ScreenTimeInsights.suggestWindow(days))
    }

    @Test
    fun `no window suggested from no data`() {
        assertNull(ScreenTimeInsights.suggestWindow(emptyList()))
    }

    // --- relapse correlation ---

    @Test
    fun `surfaces correlation when relapse nights are much heavier`() {
        val days = listOf(
            day("2026-01-01", 0 to 90),   // relapse
            day("2026-01-02", 0 to 5),
            day("2026-01-03", 0 to 8),
            day("2026-01-04", 0 to 100),  // relapse
            day("2026-01-05", 0 to 4)
        )
        val correlation = ScreenTimeInsights.correlateWithRelapses(
            days, setOf("2026-01-01", "2026-01-04")
        )
        assertNotNull(correlation!!)
        assertEquals(95, correlation.relapseNightAvgMinutes)
        assertEquals(2, correlation.relapseNightsSampled)
        assertTrue(correlation.multiplier > 10f)
    }

    @Test
    fun `no correlation claimed from a single relapse`() {
        val days = listOf(
            day("2026-01-01", 0 to 90),
            day("2026-01-02", 0 to 5)
        )
        // One data point is an anecdote, not a pattern.
        assertNull(ScreenTimeInsights.correlateWithRelapses(days, setOf("2026-01-01")))
    }

    @Test
    fun `no correlation claimed when relapse nights look like clean nights`() {
        val days = listOf(
            day("2026-01-01", 0 to 40),  // relapse
            day("2026-01-02", 0 to 38),
            day("2026-01-03", 0 to 42),  // relapse
            day("2026-01-04", 0 to 39)
        )
        // Nearly identical usage — asserting a link here would be a fabrication.
        assertNull(
            ScreenTimeInsights.correlateWithRelapses(
                days, setOf("2026-01-01", "2026-01-03")
            )
        )
    }

    // --- last night, spanning the date boundary ---

    @Test
    fun `last night sums yesterday evening and this morning`() {
        val days = listOf(
            day("2026-01-01", 23 to 40),           // last night's evening half
            day("2026-01-02", 1 to 35, 20 to 60)   // its morning half, plus tonight
        )
        val insights = ScreenTimeInsights.analyze(days, emptySet())!!
        // 40 (23:00 yesterday) + 35 (01:00 today). The 20:00 use is tonight, not last night.
        assertEquals(75, insights.lastNightLateMinutes)
    }

    // --- restlessness ---

    @Test
    fun `restlessness flags an unlock spike over the users own baseline`() {
        val days = (1..6).map { day("2026-01-0$it", unlocks = 50) } +
            day("2026-01-07", unlocks = 90)
        val insights = ScreenTimeInsights.analyze(days, emptySet())!!
        assertEquals(50, insights.baselineUnlocks)
        assertEquals(80, insights.restlessnessDelta)
        assertTrue(insights.isRestlessToday)
    }

    @Test
    fun `a normal day is not flagged as restless`() {
        val days = (1..6).map { day("2026-01-0$it", unlocks = 50) } +
            day("2026-01-07", unlocks = 52)
        val insights = ScreenTimeInsights.analyze(days, emptySet())!!
        assertTrue(!insights.isRestlessToday)
    }

    // --- danger-window escalation ---

    @Test
    fun `escalation stays silent early in the window`() {
        // Firing the instant the window opens teaches people to swipe it away.
        assertEquals(0, ScreenTimeInsights.escalationLevel(0))
        assertEquals(0, ScreenTimeInsights.escalationLevel(19))
    }

    @Test
    fun `escalation climbs with time spent on the phone`() {
        assertEquals(1, ScreenTimeInsights.escalationLevel(20))
        assertEquals(1, ScreenTimeInsights.escalationLevel(44))
        assertEquals(2, ScreenTimeInsights.escalationLevel(45))
        assertEquals(2, ScreenTimeInsights.escalationLevel(74))
        assertEquals(3, ScreenTimeInsights.escalationLevel(75))
        assertEquals(3, ScreenTimeInsights.escalationLevel(300))
    }

    // --- formatting ---

    @Test
    fun `formats durations and windows for display`() {
        assertEquals("45m", ScreenTimeInsights.formatMinutes(45))
        assertEquals("2h", ScreenTimeInsights.formatMinutes(120))
        assertEquals("1h 47m", ScreenTimeInsights.formatMinutes(107))
        assertEquals("23:00 → 02:00", ScreenTimeInsights.formatWindow(23 to 2))
    }
}
