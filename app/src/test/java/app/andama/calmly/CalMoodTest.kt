package app.andama.calmly

import app.andama.calmly.data.Cal
import app.andama.calmly.data.CalMood
import app.andama.calmly.data.CalState
import app.andama.calmly.data.CalVoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalMoodTest {

    // --- the check-in scale ---

    @Test
    fun `the five faces are the 1 to 5 mood scale`() {
        assertEquals(1, CalMood.SAD.level)
        assertEquals(5, CalMood.FIERCE.level)
        CalMood.entries.forEach { mood ->
            assertEquals(mood, CalMood.fromLevel(mood.level))
        }
    }

    @Test
    fun `a mood level from an old build never crashes the scale`() {
        // Levels are persisted; a bad or out-of-range one must degrade, not throw.
        assertEquals(CalMood.NEUTRAL, CalMood.fromLevel(0))
        assertEquals(CalMood.NEUTRAL, CalMood.fromLevel(9))
    }

    // --- grief outranks everything ---

    @Test
    fun `the hours right after a fall show grief, not a lecture`() {
        val face = Cal.face(CalState(cleanDays = 0, hoursIntoDay = 2))
        assertEquals(CalMood.SAD, face)
    }

    @Test
    fun `later on day zero he is shaken but standing`() {
        val face = Cal.face(CalState(cleanDays = 0, hoursIntoDay = 18))
        assertEquals(CalMood.STRUGGLING, face)
    }

    @Test
    fun `a fresh fall inside the danger window is still grief`() {
        // Nobody needs a game face an hour after they fell. Grief wins the tie.
        val face = Cal.face(
            CalState(cleanDays = 0, hoursIntoDay = 1, inDangerWindow = true, escalation = 3)
        )
        assertEquals(CalMood.SAD, face)
    }

    // --- the danger-window arc ---

    @Test
    fun `cal deteriorates as the danger window drags on`() {
        fun at(escalation: Int) = Cal.face(
            CalState(cleanDays = 10, inDangerWindow = true, escalation = escalation)
        )
        // Guard up, nothing has gone wrong yet.
        assertEquals(CalMood.FIERCE, at(0))
        // The arc is the mechanic: a mascot you're letting down is harder to
        // swipe away than a warning you can argue with.
        assertEquals(CalMood.STRUGGLING, at(1))
        assertEquals(CalMood.SAD, at(2))
        assertEquals(CalMood.SAD, at(3))
    }

    @Test
    fun `escalation outside the window is ignored`() {
        // A stale level left over from last night must not sour today's face.
        val face = Cal.face(CalState(cleanDays = 10, inDangerWindow = false, escalation = 3))
        assertEquals(CalMood.HAPPY, face)
    }

    // --- the ordinary days ---

    @Test
    fun `restlessness shows on his face even when the streak is intact`() {
        val face = Cal.face(CalState(cleanDays = 12, isRestless = true))
        assertEquals(CalMood.STRUGGLING, face)
    }

    @Test
    fun `milestones are celebrated`() {
        assertEquals(CalMood.HAPPY, Cal.face(CalState(cleanDays = 7)))
    }

    @Test
    fun `he hardens once the streak is long`() {
        assertEquals(CalMood.NEUTRAL, Cal.face(CalState(cleanDays = 2)))
        assertEquals(CalMood.HAPPY, Cal.face(CalState(cleanDays = 12)))
        assertEquals(CalMood.FIERCE, Cal.face(CalState(cleanDays = 45)))
    }

    // --- his voice ---

    @Test
    fun `the widget nags for a check-in before it praises`() {
        val state = CalState(cleanDays = 12, checkedInToday = false)
        assertEquals("check in?", CalVoice.widgetLine(Cal.face(state), state))
    }

    @Test
    fun `the widget line always fits on one line`() {
        // The caption sits under a 62dp face. Anything longer gets ellipsised by
        // the launcher, and half a sentence of Cal is worse than none.
        val states = listOf(
            CalState(cleanDays = 0, hoursIntoDay = 1),
            CalState(cleanDays = 0, hoursIntoDay = 20),
            CalState(cleanDays = 1, checkedInToday = true),
            CalState(cleanDays = 40, checkedInToday = true),
            CalState(cleanDays = 12, checkedInToday = false),
            CalState(cleanDays = 5, checkedInToday = true, inDangerWindow = true),
            CalState(cleanDays = 5, checkedInToday = true, inDangerWindow = true, escalation = 2),
            CalState(cleanDays = 5, checkedInToday = true, isRestless = true)
        )
        states.forEach { state ->
            val line = CalVoice.widgetLine(Cal.face(state), state)
            assertTrue(
                "'$line' (${line.length}) will be ellipsised on the widget",
                line.length <= CalVoice.MAX_WIDGET_LINE
            )
        }
    }

    @Test
    fun `encouragement is personal, and matched to how far in they are`() {
        val dayZero = CalVoice.encouragement(cleanDays = 0, name = "Andama", seed = 1)
        val veteran = CalVoice.encouragement(cleanDays = 90, name = "Andama", seed = 1)

        assertTrue(dayZero.contains("Andama"))
        assertNotEquals(dayZero, veteran)
        // A leftover placeholder in a shipped notification is a visible bug.
        assertTrue(CalMood.entries.isNotEmpty())
        listOf(0, 3, 15, 200).forEach { days ->
            (0..40).forEach { seed ->
                val line = CalVoice.encouragement(days, "Andama", seed)
                assertTrue(line.isNotBlank())
                assertTrue("placeholder left in: $line", !line.contains("{name}"))
            }
        }
    }

    @Test
    fun `encouragement is stable for a given day`() {
        // The line is rebuilt every time the notification is constructed; it must
        // not reshuffle mid-day.
        val first = CalVoice.encouragement(10, "Andama", seed = 197)
        val again = CalVoice.encouragement(10, "Andama", seed = 197)
        assertEquals(first, again)
    }

    @Test
    fun `a negative seed still lands inside the pool`() {
        // Calendar fields are sane today, but a rem that goes negative would throw
        // an IndexOutOfBounds inside a broadcast receiver at 2pm.
        val line = CalVoice.encouragement(10, "Andama", seed = -7)
        assertTrue(line.isNotBlank())
    }

    @Test
    fun `every mood has something to say on the check-in`() {
        CalMood.entries.forEach { mood ->
            val reply = CalVoice.moodReply(mood, "Andama")
            assertTrue(reply.contains("Andama"))
            assertTrue(reply.isNotBlank())
        }
    }
}
