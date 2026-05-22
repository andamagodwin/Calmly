package app.andama.calmly.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

class CalmlyTracker(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun getCleanDays(): Int {
        val startTime = context.trackerDataStore.data.map { it[TrackerKeys.CLEAN_STREAK_START] }.first()
        if (startTime == null || startTime == 0L) return 0
        val diff = System.currentTimeMillis() - startTime
        return (diff / (24 * 60 * 60 * 1000L)).toInt()
    }

    suspend fun getLongestCleanStreak(): Int {
        return context.trackerDataStore.data.map { it[TrackerKeys.LONGEST_CLEAN_STREAK] ?: 0 }.first()
    }

    suspend fun getTotalRelapses(): Int {
        return context.trackerDataStore.data.map { it[TrackerKeys.TOTAL_RELAPSES] ?: 0 }.first()
    }

    suspend fun startCleanStreak() {
        context.trackerDataStore.edit { prefs ->
            if (prefs[TrackerKeys.CLEAN_STREAK_START] == null || prefs[TrackerKeys.CLEAN_STREAK_START] == 0L) {
                prefs[TrackerKeys.CLEAN_STREAK_START] = System.currentTimeMillis()
            }
        }
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
    }

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
}
