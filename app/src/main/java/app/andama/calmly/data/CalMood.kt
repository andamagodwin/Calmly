package app.andama.calmly.data

/**
 * Cal's five faces, ordered worst to best so that the ordinal doubles as the
 * 1-5 mood scale on the daily check-in.
 *
 * Deliberately free of Android imports: the rules below decide the emotional
 * state of the entire app — the widget, the home header, the notifications —
 * and that logic is worth being able to test without a device. The drawable for
 * each face lives in `ui/CalFaces.kt`.
 */
enum class CalMood {
    SAD,
    STRUGGLING,
    NEUTRAL,
    HAPPY,
    FIERCE;

    /** 1..5, matching the check-in scale stored by [CalmlyTracker.logMood]. */
    val level: Int get() = ordinal + 1

    companion object {
        fun fromLevel(level: Int): CalMood =
            entries.getOrElse(level - 1) { NEUTRAL }
    }
}

/** Everything Cal's face reacts to, gathered in one read. */
data class CalState(
    val cleanDays: Int,
    /** Hours into the current day of the streak — on day zero, hours since the fall. */
    val hoursIntoDay: Int = 0,
    val checkedInToday: Boolean = false,
    val inDangerWindow: Boolean = false,
    /** 0-3, from [ScreenTimeInsights.escalationLevel]. Only meaningful in the window. */
    val escalation: Int = 0,
    val isRestless: Boolean = false,
    /** A danger window was ridden out clean within the last day. */
    val defendedWindowRecently: Boolean = false,
    /** That defended window followed a recent fall — the redemption case. */
    val comeback: Boolean = false
) {
    val isMilestoneDay: Boolean get() = cleanDays in StreakInfo.MILESTONES
}

object Cal {

    /** Past this many clean days, Cal stops being pleased and starts being hard. */
    const val HARDENED_DAYS = 30

    /** The rawest part of day zero, when the face should be grief and not a lecture. */
    const val FRESH_FALL_HOURS = 12

    /**
     * The face Cal is wearing right now.
     *
     * The order is the whole design. Grief outranks everything — nobody needs a
     * grinning mascot an hour after they fell. Then the danger window, where the
     * arc is the point: Cal starts fierce (guard up, nothing has gone wrong yet)
     * and visibly deteriorates as the minutes pile up, because a mascot you are
     * letting down is a harder thing to swipe away than a warning you can argue
     * with.
     */
    fun face(state: CalState): CalMood = when {
        state.cleanDays == 0 && state.hoursIntoDay < FRESH_FALL_HOURS -> CalMood.SAD
        state.cleanDays == 0 -> CalMood.STRUGGLING

        state.inDangerWindow && state.escalation >= 2 -> CalMood.SAD
        state.inDangerWindow && state.escalation >= 1 -> CalMood.STRUGGLING
        state.inDangerWindow -> CalMood.FIERCE

        // The morning after riding a window out clean — proudest he gets, and
        // proudest of all when it was a comeback from a recent fall.
        state.defendedWindowRecently -> CalMood.FIERCE

        state.isRestless -> CalMood.STRUGGLING
        state.isMilestoneDay -> CalMood.HAPPY
        state.cleanDays >= HARDENED_DAYS -> CalMood.FIERCE
        state.cleanDays >= 3 -> CalMood.HAPPY
        else -> CalMood.NEUTRAL
    }
}

/**
 * Everything Cal says. Kept in one place so his voice stays consistent across
 * the widget, the notifications and the check-in — and so the copy can be read
 * end to end without opening six files.
 */
object CalVoice {

    /**
     * The line under the face on the streak widget. It sits in a column barely
     * wider than the 62dp face, so it has roughly twelve characters before the
     * launcher ellipsises it into nonsense — [MAX_WIDGET_LINE] is enforced by test.
     */
    fun widgetLine(mood: CalMood, state: CalState): String = when {
        state.inDangerWindow && state.escalation >= 2 -> "put it down"
        state.inDangerWindow -> "on watch"
        state.comeback -> "you're back"
        state.defendedWindowRecently -> "you beat it"
        state.cleanDays == 0 && state.hoursIntoDay < Cal.FRESH_FALL_HOURS -> "still here"
        state.cleanDays == 0 -> "get back up"
        !state.checkedInToday -> "check in?"
        state.isRestless -> "you okay?"
        mood == CalMood.FIERCE -> "unbreakable"
        state.isMilestoneDay -> "milestone!"
        else -> "keep going"
    }

    const val MAX_WIDGET_LINE = 12

    /** Cal's reply the moment you pick a mood on the check-in. */
    fun moodReply(mood: CalMood, name: String): String = when (mood) {
        CalMood.SAD ->
            "Then today is survival, $name. That counts. Don't decide anything big tonight — just don't feed it."
        CalMood.STRUGGLING ->
            "Struggling isn't losing, $name. It's what winning feels like from the inside. Stay in the fight."
        CalMood.NEUTRAL ->
            "Okay is underrated, $name. Most days are won quietly, and this is what quiet looks like."
        CalMood.HAPPY ->
            "Good. Bank it, $name. Notice exactly what today felt like — that's the feeling you're protecting."
        CalMood.FIERCE ->
            "That's the face I like, $name. Strong days are for building, not coasting. Go do something hard."
    }

    /**
     * The afternoon encouragement, matched to how far in they actually are.
     * [seed] is the day of the year, so the line changes daily but doesn't
     * shuffle every time the notification is rebuilt.
     */
    fun encouragement(cleanDays: Int, name: String, seed: Int): String {
        val pool = when {
            cleanDays == 0 -> DAY_ZERO
            cleanDays < 7 -> EARLY
            cleanDays < Cal.HARDENED_DAYS -> BUILDING
            else -> HARDENED
        }
        return pool[Math.floorMod(seed, pool.size)].replace("{name}", name)
    }

    private val DAY_ZERO = listOf(
        "Yesterday is spent, {name}. It's not coming back and it's not the point. The only streak that matters starts the next time you say no.",
        "You are not the relapse, {name}. You're the thing that keeps standing back up in front of it.",
        "Zero is a number, not a verdict. Get through today and it isn't zero anymore."
    )

    private val EARLY = listOf(
        "The first week is the loudest, {name}. It gets quieter. Not easier — quieter. Hold the line.",
        "Your brain is negotiating with you right now, {name}. It always does at this stage. You don't have to take the meeting.",
        "Every hour you don't give in, you're rewiring something real. This is the part that actually costs you. Pay it.",
        "Cravings peak and pass in about twenty minutes, {name}. You can outlast twenty minutes. You've done harder."
    )

    private val BUILDING = listOf(
        "You've got real ground behind you now, {name}. That's the ground it'll try to convince you doesn't matter. It matters.",
        "The dangerous thought this week is 'I'm fine now'. You're not cured, {name} — you're consistent. Keep being consistent.",
        "Nobody is coming to hand you this, {name}. You're already taking it. Keep taking it.",
        "Boredom is the trigger nobody warns you about, {name}. Fill the day before it fills itself."
    )

    private val HARDENED = listOf(
        "You've rebuilt something most people only talk about, {name}. Don't hand it back for ten minutes of nothing.",
        "At this point it's not willpower, {name}. It's identity. This is just who you are now.",
        "The streak isn't the prize. The person it made you is. Protect him.",
        "You're past the hard part and into the long part, {name}. The long part is won by not getting bored of winning."
    )
}
