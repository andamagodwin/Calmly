package app.andama.calmly.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.andama.calmly.service.DangerHoursSentinel
import app.andama.calmly.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Context.trackerDataStore by preferencesDataStore(name = "calmly_tracker")

object TrackerKeys {
    val CLEAN_STREAK_START = longPreferencesKey("clean_streak_start")
    val LONGEST_CLEAN_STREAK = intPreferencesKey("longest_clean_streak")
    val TOTAL_RELAPSES = intPreferencesKey("total_relapses")
    val MOOD_LOG = stringPreferencesKey("mood_log")
    val TRIGGER_LOG = stringPreferencesKey("trigger_log")
    val RELAPSE_LOG = stringPreferencesKey("relapse_log")
    val DANGER_HOURS_START = intPreferencesKey("danger_hours_start")
    val DANGER_HOURS_END = intPreferencesKey("danger_hours_end")
    val DANGER_HOURS_ENABLED = stringPreferencesKey("danger_hours_enabled")
    val PARTNER_NAME = stringPreferencesKey("partner_name")
    val PARTNER_PHONE = stringPreferencesKey("partner_phone")
    val PARTNER_ENABLED = stringPreferencesKey("partner_enabled")
    val LAST_CHECKIN_DATE = stringPreferencesKey("last_checkin_date")
    val USER_NAME = stringPreferencesKey("user_name")
    val PATROL_LEVEL = intPreferencesKey("patrol_level")
    val PATROL_NIGHT = stringPreferencesKey("patrol_night")
    val WINDOWS_DEFENDED = intPreferencesKey("windows_defended")
    val LAST_DEFENDED_AT = longPreferencesKey("last_defended_at")
    val LAST_DEFENDED_COMEBACK = stringPreferencesKey("last_defended_comeback")
    val BLOCKED_PACKAGES = stringPreferencesKey("blocked_packages")
}

data class MoodEntry(
    val date: String,
    val level: Int,
    val note: String = ""
)

data class TriggerEntry(
    val date: String,
    val trigger: String,
    val notes: String = ""
)

data class RelapseEntry(
    val date: String,
    val trigger: String,
    val notes: String,
    val streakLost: Int
)

/** Everything the home screen renders, resolved in a single read. */
data class StreakInfo(
    val days: Int,
    val hours: Int,
    val longest: Int,
    val totalRelapses: Int,
    val checkedInToday: Boolean
) {
    val nextMilestone: Int? = MILESTONES.firstOrNull { it > days }

    /**
     * Fraction of the way from the previous milestone to the next one. Hours are
     * folded in so the ring creeps forward during day zero instead of sitting
     * empty for 24 hours — that first day is when the feedback matters most.
     */
    val milestoneProgress: Float
        get() {
            val next = nextMilestone ?: return 1f
            val previous = MILESTONES.lastOrNull { it <= days } ?: 0
            val span = next - previous
            if (span <= 0) return 1f
            val elapsed = (days - previous) + (hours / 24f)
            return (elapsed / span).coerceIn(0f, 1f)
        }

    companion object {
        val MILESTONES = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
    }
}

class CalmlyTracker(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    private fun elapsedMillis(startTime: Long?): Long {
        if (startTime == null || startTime == 0L) return 0L
        return (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
    }

    suspend fun getCleanDays(): Int {
        val startTime = context.trackerDataStore.data.map { it[TrackerKeys.CLEAN_STREAK_START] }.first()
        return (elapsedMillis(startTime) / DAY_MS).toInt()
    }

    /** Hours elapsed within the current day of the streak (0..23). */
    suspend fun getCleanHours(): Int {
        val startTime = context.trackerDataStore.data.map { it[TrackerKeys.CLEAN_STREAK_START] }.first()
        return ((elapsedMillis(startTime) % DAY_MS) / HOUR_MS).toInt()
    }

    /**
     * The stored value only updates on relapse, so a streak that is currently
     * the longest one ever wouldn't be reflected until the user broke it.
     */
    suspend fun getLongestCleanStreak(): Int {
        val stored = context.trackerDataStore.data.map { it[TrackerKeys.LONGEST_CLEAN_STREAK] ?: 0 }.first()
        return maxOf(stored, getCleanDays())
    }

    suspend fun getTotalRelapses(): Int {
        return context.trackerDataStore.data.map { it[TrackerKeys.TOTAL_RELAPSES] ?: 0 }.first()
    }

    suspend fun getStreakInfo(): StreakInfo {
        val prefs = context.trackerDataStore.data.first()
        val elapsed = elapsedMillis(prefs[TrackerKeys.CLEAN_STREAK_START])
        val days = (elapsed / DAY_MS).toInt()
        return StreakInfo(
            days = days,
            hours = ((elapsed % DAY_MS) / HOUR_MS).toInt(),
            longest = maxOf(prefs[TrackerKeys.LONGEST_CLEAN_STREAK] ?: 0, days),
            totalRelapses = prefs[TrackerKeys.TOTAL_RELAPSES] ?: 0,
            checkedInToday = prefs[TrackerKeys.LAST_CHECKIN_DATE] == dateFormat.format(Date())
        )
    }

    suspend fun startCleanStreak() {
        context.trackerDataStore.edit { prefs ->
            if (prefs[TrackerKeys.CLEAN_STREAK_START] == null || prefs[TrackerKeys.CLEAN_STREAK_START] == 0L) {
                prefs[TrackerKeys.CLEAN_STREAK_START] = System.currentTimeMillis()
            }
        }
        WidgetUpdater.updateWidget(context)
    }

    suspend fun logRelapse(trigger: String, notes: String) {
        val cleanDays = getCleanDays()
        context.trackerDataStore.edit { prefs ->
            val longestStreak = prefs[TrackerKeys.LONGEST_CLEAN_STREAK] ?: 0
            if (cleanDays > longestStreak) {
                prefs[TrackerKeys.LONGEST_CLEAN_STREAK] = cleanDays
            }
            prefs[TrackerKeys.CLEAN_STREAK_START] = System.currentTimeMillis()
            prefs[TrackerKeys.TOTAL_RELAPSES] = (prefs[TrackerKeys.TOTAL_RELAPSES] ?: 0) + 1

            val logJson = prefs[TrackerKeys.RELAPSE_LOG] ?: "[]"
            val array = JSONArray(logJson)
            val entry = JSONObject().apply {
                put("date", dateTimeFormat.format(Date()))
                put("trigger", trigger)
                put("notes", notes)
                put("streak_lost", cleanDays)
            }
            array.put(entry)
            prefs[TrackerKeys.RELAPSE_LOG] = array.toString()
        }
        // The home-screen widget shows this number; don't let it lie.
        WidgetUpdater.updateWidget(context)
    }

    suspend fun getRelapseLog(): List<RelapseEntry> {
        val logJson = context.trackerDataStore.data.map { it[TrackerKeys.RELAPSE_LOG] ?: "[]" }.first()
        val array = JSONArray(logJson)
        val entries = mutableListOf<RelapseEntry>()
        for (i in array.length() - 1 downTo 0) {
            val obj = array.getJSONObject(i)
            entries.add(RelapseEntry(
                date = obj.getString("date"),
                trigger = obj.getString("trigger"),
                notes = obj.optString("notes", ""),
                streakLost = obj.optInt("streak_lost", 0)
            ))
        }
        return entries
    }

    /**
     * Relapse days as bare `yyyy-MM-dd` keys, for correlating against screen-time
     * buckets. The log stores a full timestamp; only the date part is comparable.
     */
    suspend fun getRelapseDates(): Set<String> =
        getRelapseLog().map { it.date.take(10) }.toSet()

    suspend fun logMood(level: Int, note: String = "") {
        val today = dateFormat.format(Date())
        context.trackerDataStore.edit { prefs ->
            val logJson = prefs[TrackerKeys.MOOD_LOG] ?: "[]"
            val array = JSONArray(logJson)
            val entry = JSONObject().apply {
                put("date", today)
                put("level", level)
                put("note", note)
            }
            array.put(entry)
            prefs[TrackerKeys.MOOD_LOG] = array.toString()
            prefs[TrackerKeys.LAST_CHECKIN_DATE] = today
        }
        // The widget nags "check in?" until this happens — stop nagging.
        WidgetUpdater.updateWidget(context)
    }

    /** The most recent mood the user logged, if they have ever logged one. */
    suspend fun getLatestMood(): CalMood? =
        getMoodLog().firstOrNull()?.let { CalMood.fromLevel(it.level) }

    suspend fun getMoodLog(): List<MoodEntry> {
        val logJson = context.trackerDataStore.data.map { it[TrackerKeys.MOOD_LOG] ?: "[]" }.first()
        val array = JSONArray(logJson)
        val entries = mutableListOf<MoodEntry>()
        for (i in array.length() - 1 downTo maxOf(0, array.length() - 30)) {
            val obj = array.getJSONObject(i)
            entries.add(MoodEntry(
                date = obj.getString("date"),
                level = obj.getInt("level"),
                note = obj.optString("note", "")
            ))
        }
        return entries
    }

    suspend fun hasCheckedInToday(): Boolean {
        val lastDate = context.trackerDataStore.data.map { it[TrackerKeys.LAST_CHECKIN_DATE] }.first()
        val today = dateFormat.format(Date())
        return lastDate == today
    }

    suspend fun logTrigger(trigger: String, notes: String = "") {
        context.trackerDataStore.edit { prefs ->
            val logJson = prefs[TrackerKeys.TRIGGER_LOG] ?: "[]"
            val array = JSONArray(logJson)
            val entry = JSONObject().apply {
                put("date", dateTimeFormat.format(Date()))
                put("trigger", trigger)
                put("notes", notes)
            }
            array.put(entry)
            prefs[TrackerKeys.TRIGGER_LOG] = array.toString()
        }
    }

    suspend fun getTriggerLog(): List<TriggerEntry> {
        val logJson = context.trackerDataStore.data.map { it[TrackerKeys.TRIGGER_LOG] ?: "[]" }.first()
        val array = JSONArray(logJson)
        val entries = mutableListOf<TriggerEntry>()
        for (i in array.length() - 1 downTo maxOf(0, array.length() - 50)) {
            val obj = array.getJSONObject(i)
            entries.add(TriggerEntry(
                date = obj.getString("date"),
                trigger = obj.getString("trigger"),
                notes = obj.optString("notes", "")
            ))
        }
        return entries
    }

    suspend fun getTriggerStats(): Map<String, Int> {
        val logJson = context.trackerDataStore.data.map { it[TrackerKeys.TRIGGER_LOG] ?: "[]" }.first()
        val array = JSONArray(logJson)
        val counts = mutableMapOf<String, Int>()
        for (i in 0 until array.length()) {
            val trigger = array.getJSONObject(i).getString("trigger")
            counts[trigger] = (counts[trigger] ?: 0) + 1
        }
        return counts.toList().sortedByDescending { it.second }.toMap()
    }

    suspend fun setDangerHours(startHour: Int, endHour: Int, enabled: Boolean) {
        context.trackerDataStore.edit { prefs ->
            prefs[TrackerKeys.DANGER_HOURS_START] = startHour
            prefs[TrackerKeys.DANGER_HOURS_END] = endHour
            prefs[TrackerKeys.DANGER_HOURS_ENABLED] = if (enabled) "true" else "false"
        }
        // Arm (or disarm) the background alarm that watches this window; without
        // it the setting only ever mattered while the app happened to be open.
        DangerHoursSentinel.reschedule(context)
    }

    suspend fun getDangerHours(): Triple<Int, Int, Boolean>? {
        val data = context.trackerDataStore.data.first()
        val enabled = data[TrackerKeys.DANGER_HOURS_ENABLED] == "true"
        val start = data[TrackerKeys.DANGER_HOURS_START] ?: return null
        val end = data[TrackerKeys.DANGER_HOURS_END] ?: return null
        return Triple(start, end, enabled)
    }

    suspend fun isInDangerHours(): Boolean {
        val dangerHours = getDangerHours() ?: return false
        if (!dangerHours.third) return false
        val now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = dangerHours.first
        val end = dangerHours.second
        return if (start <= end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }

    suspend fun setAccountabilityPartner(name: String, phone: String, enabled: Boolean) {
        context.trackerDataStore.edit { prefs ->
            prefs[TrackerKeys.PARTNER_NAME] = name
            prefs[TrackerKeys.PARTNER_PHONE] = phone
            prefs[TrackerKeys.PARTNER_ENABLED] = if (enabled) "true" else "false"
        }
    }

    suspend fun getAccountabilityPartner(): Triple<String, String, Boolean>? {
        val data = context.trackerDataStore.data.first()
        val name = data[TrackerKeys.PARTNER_NAME] ?: return null
        val phone = data[TrackerKeys.PARTNER_PHONE] ?: return null
        val enabled = data[TrackerKeys.PARTNER_ENABLED] == "true"
        return Triple(name, phone, enabled)
    }

    suspend fun setUserName(name: String) {
        context.trackerDataStore.edit { prefs ->
            prefs[TrackerKeys.USER_NAME] = name.trim()
        }
    }

    /** Null until onboarding has run. */
    suspend fun getUserName(): String? {
        return context.trackerDataStore.data
            .map { it[TrackerKeys.USER_NAME]?.takeIf { name -> name.isNotBlank() } }
            .first()
    }

    /**
     * Highest danger-window warning already delivered for the night identified by
     * [nightKey]. Escalations should only ever fire once each — the patrol wakes
     * every 15 minutes, and re-sending the same warning every time would be noise
     * the user learns to ignore.
     */
    suspend fun getPatrolLevel(nightKey: String): Int {
        val prefs = context.trackerDataStore.data.first()
        if (prefs[TrackerKeys.PATROL_NIGHT] != nightKey) return 0
        return prefs[TrackerKeys.PATROL_LEVEL] ?: 0
    }

    suspend fun setPatrolLevel(nightKey: String, level: Int) {
        context.trackerDataStore.edit { prefs ->
            prefs[TrackerKeys.PATROL_NIGHT] = nightKey
            prefs[TrackerKeys.PATROL_LEVEL] = level
        }
    }

    /**
     * How far tonight's escalation has already climbed, or 0 when we're outside
     * the window. A window that spans midnight is keyed to the day it *opened*,
     * so at 01:00 the live night is yesterday's key — hence both are accepted.
     */
    private suspend fun getCurrentPatrolLevel(): Int {
        val prefs = context.trackerDataStore.data.first()
        val night = prefs[TrackerKeys.PATROL_NIGHT] ?: return 0
        val now = System.currentTimeMillis()
        val today = dateFormat.format(Date(now))
        val yesterday = dateFormat.format(Date(now - DAY_MS))
        if (night != today && night != yesterday) return 0
        return prefs[TrackerKeys.PATROL_LEVEL] ?: 0
    }

    /**
     * Everything Cal's face reacts to, in one call. Restlessness is left at its
     * default — it costs a 14-day screen-time read, so only callers that already
     * have insights on hand should fold it in with `copy(isRestless = ...)`.
     */
    suspend fun getCalState(): CalState {
        val prefs = context.trackerDataStore.data.first()
        val elapsed = elapsedMillis(prefs[TrackerKeys.CLEAN_STREAK_START])
        val streak = StreakInfo(
            days = (elapsed / DAY_MS).toInt(),
            hours = ((elapsed % DAY_MS) / HOUR_MS).toInt(),
            longest = 0,
            totalRelapses = 0,
            checkedInToday = prefs[TrackerKeys.LAST_CHECKIN_DATE] == dateFormat.format(Date())
        )
        val inWindow = isInDangerHours()

        // A defended window is only celebration-worthy for the daylight after it —
        // and never while a fresh window is open, when the point is still to be
        // watchful, not proud.
        val defendedAt = prefs[TrackerKeys.LAST_DEFENDED_AT] ?: 0L
        val defendedRecently = !inWindow &&
            defendedAt > 0L &&
            System.currentTimeMillis() - defendedAt < DEFENDED_SHOW_MS

        return CalState(
            cleanDays = streak.days,
            hoursIntoDay = streak.hours,
            checkedInToday = streak.checkedInToday,
            inDangerWindow = inWindow,
            escalation = if (inWindow) getCurrentPatrolLevel() else 0,
            defendedWindowRecently = defendedRecently,
            comeback = defendedRecently && prefs[TrackerKeys.LAST_DEFENDED_COMEBACK] == "true"
        )
    }

    /** Number of danger windows the user has ridden out clean. */
    suspend fun getWindowsDefended(): Int =
        context.trackerDataStore.data.map { it[TrackerKeys.WINDOWS_DEFENDED] ?: 0 }.first()

    /**
     * Relapse instants as epoch millis, for asking "did a relapse happen inside
     * this window?" The log stores "yyyy-MM-dd HH:mm"; anything unparseable is
     * dropped rather than crashing the window-close check.
     */
    suspend fun getRelapseTimestamps(): List<Long> {
        val logJson = context.trackerDataStore.data.map { it[TrackerKeys.RELAPSE_LOG] ?: "[]" }.first()
        val array = JSONArray(logJson)
        val out = mutableListOf<Long>()
        for (i in 0 until array.length()) {
            val raw = array.getJSONObject(i).optString("date")
            runCatching { dateTimeFormat.parse(raw)?.time }.getOrNull()?.let { out.add(it) }
        }
        return out
    }

    /**
     * Records that a danger window just closed with no relapse inside it.
     * [comeback] means they'd fallen recently and still held this one — the
     * redemption case worth shouting about on the widget.
     */
    suspend fun recordWindowDefended(comeback: Boolean) {
        context.trackerDataStore.edit { prefs ->
            prefs[TrackerKeys.WINDOWS_DEFENDED] = (prefs[TrackerKeys.WINDOWS_DEFENDED] ?: 0) + 1
            prefs[TrackerKeys.LAST_DEFENDED_AT] = System.currentTimeMillis()
            prefs[TrackerKeys.LAST_DEFENDED_COMEBACK] = if (comeback) "true" else "false"
        }
        WidgetUpdater.updateWidget(context)
    }

    // --- App lock -------------------------------------------------------------

    /** Package names the accessibility service should slam shut. */
    suspend fun getBlockedPackages(): Set<String> = parsePackages(
        context.trackerDataStore.data.map { it[TrackerKeys.BLOCKED_PACKAGES] ?: "[]" }.first()
    )

    /** Live view for the accessibility service, which stays running and must see edits at once. */
    val blockedPackagesFlow: kotlinx.coroutines.flow.Flow<Set<String>>
        get() = context.trackerDataStore.data.map { parsePackages(it[TrackerKeys.BLOCKED_PACKAGES] ?: "[]") }

    suspend fun setBlockedPackages(packages: Set<String>) {
        val array = JSONArray()
        packages.forEach { array.put(it) }
        context.trackerDataStore.edit { prefs ->
            prefs[TrackerKeys.BLOCKED_PACKAGES] = array.toString()
        }
    }

    private fun parsePackages(json: String): Set<String> {
        val array = JSONArray(json)
        return buildSet { for (i in 0 until array.length()) add(array.getString(i)) }
    }

    companion object {
        private const val HOUR_MS = 60 * 60 * 1000L
        private const val DAY_MS = 24 * HOUR_MS

        /** How long a defended window stays celebrated on the widget — one day. */
        private const val DEFENDED_SHOW_MS = 18 * HOUR_MS
    }
}
