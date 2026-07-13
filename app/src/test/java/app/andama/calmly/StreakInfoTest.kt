package app.andama.calmly

import app.andama.calmly.data.StreakInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreakInfoTest {

    private fun streak(days: Int) = StreakInfo(
        days = days,
        hours = 0,
        longest = days,
        totalRelapses = 0,
        checkedInToday = false
    )

    @Test
    fun `next milestone is the first one strictly ahead`() {
        assertEquals(1, streak(0).nextMilestone)
        assertEquals(3, streak(1).nextMilestone)
        assertEquals(7, streak(3).nextMilestone)
        assertEquals(30, streak(14).nextMilestone)
    }

    @Test
    fun `progress is measured between the surrounding milestones`() {
        // Day 0 of the 0 -> 1 span.
        assertEquals(0f, streak(0).milestoneProgress, 0.001f)
        // Day 2 sits halfway through the 1 -> 3 span.
        assertEquals(0.5f, streak(2).milestoneProgress, 0.001f)
        // Day 5 sits halfway through the 3 -> 7 span.
        assertEquals(0.5f, streak(5).milestoneProgress, 0.001f)
        // Landing exactly on a milestone restarts the next span.
        assertEquals(0f, streak(7).milestoneProgress, 0.001f)
    }

    @Test
    fun `hours advance the ring during day zero`() {
        val midDayOne = StreakInfo(
            days = 0, hours = 12, longest = 0, totalRelapses = 0, checkedInToday = false
        )
        // Halfway through the first day, halfway to the 1-day milestone.
        assertEquals(0.5f, midDayOne.milestoneProgress, 0.001f)
    }

    @Test
    fun `past the final milestone the ring is full and there is no next goal`() {
        assertNull(streak(365).nextMilestone)
        assertEquals(1f, streak(365).milestoneProgress, 0.001f)
        assertEquals(1f, streak(500).milestoneProgress, 0.001f)
    }
}
