package app.andama.calmly.achievements

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.andama.calmly.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.achievementDataStore: DataStore<Preferences> by preferencesDataStore(name = "achievements")

class AchievementManager(private val context: Context) {
    
    private object Keys {
        val TOTAL_SESSIONS = intPreferencesKey("total_sessions")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val LAST_SESSION_DATE = longPreferencesKey("last_session_date")
        val URGES_RESISTED = intPreferencesKey("urges_resisted")
        val MILESTONE_10 = booleanPreferencesKey("milestone_10")
        val MILESTONE_50 = booleanPreferencesKey("milestone_50")
        val MILESTONE_100 = booleanPreferencesKey("milestone_100")
        val MILESTONE_7_DAYS = booleanPreferencesKey("milestone_7_days")
        val MILESTONE_30_DAYS = booleanPreferencesKey("milestone_30_days")
        val MILESTONE_URGES_10 = booleanPreferencesKey("milestone_urges_10")
        val MILESTONE_URGES_50 = booleanPreferencesKey("milestone_urges_50")
        val MILESTONE_URGES_100 = booleanPreferencesKey("milestone_urges_100")
    }
    
    data class AchievementData(
        val totalSessions: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val lastSessionDate: Long,
        val urgesResisted: Int,
        val milestones: Map<String, Boolean>
    )
    
    val achievementData: Flow<AchievementData> = context.achievementDataStore.data.map { preferences ->
        AchievementData(
            totalSessions = preferences[Keys.TOTAL_SESSIONS] ?: 0,
            currentStreak = preferences[Keys.CURRENT_STREAK] ?: 0,
            longestStreak = preferences[Keys.LONGEST_STREAK] ?: 0,
            lastSessionDate = preferences[Keys.LAST_SESSION_DATE] ?: 0,
            urgesResisted = preferences[Keys.URGES_RESISTED] ?: 0,
            milestones = mapOf(
                "10_sessions" to (preferences[Keys.MILESTONE_10] ?: false),
                "50_sessions" to (preferences[Keys.MILESTONE_50] ?: false),
                "100_sessions" to (preferences[Keys.MILESTONE_100] ?: false),
                "7_days_streak" to (preferences[Keys.MILESTONE_7_DAYS] ?: false),
                "30_days_streak" to (preferences[Keys.MILESTONE_30_DAYS] ?: false),
                "10_urges_resisted" to (preferences[Keys.MILESTONE_URGES_10] ?: false),
                "50_urges_resisted" to (preferences[Keys.MILESTONE_URGES_50] ?: false),
                "100_urges_resisted" to (preferences[Keys.MILESTONE_URGES_100] ?: false)
            )
        )
    }
    
    /** Local midnight for a timestamp, so streaks advance on calendar days. */
    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun recordSession() {
        val now = System.currentTimeMillis()
        val today = startOfDay(now)
        val oneDay = 24 * 60 * 60 * 1000L

        context.achievementDataStore.edit { preferences ->
            val totalSessions: Int = (preferences[Keys.TOTAL_SESSIONS] ?: 0) + 1
            val lastSessionDate: Long = preferences[Keys.LAST_SESSION_DATE] ?: 0L
            val currentStreak: Int = preferences[Keys.CURRENT_STREAK] ?: 0
            val longestStreak: Int = preferences[Keys.LONGEST_STREAK] ?: 0

            // Streaks count consecutive *days* with at least one session. A second
            // session on the same day must not advance it.
            val daysSinceLast: Long = if (lastSessionDate == 0L) {
                -1L
            } else {
                (today - startOfDay(lastSessionDate)) / oneDay
            }

            val newStreak: Int = when (daysSinceLast) {
                -1L -> 1
                0L -> maxOf(currentStreak, 1)
                1L -> currentStreak + 1
                else -> 1
            }

            val newLongestStreak: Int = maxOf(longestStreak, newStreak)

            preferences[Keys.TOTAL_SESSIONS] = totalSessions
            preferences[Keys.CURRENT_STREAK] = newStreak
            preferences[Keys.LONGEST_STREAK] = newLongestStreak
            preferences[Keys.LAST_SESSION_DATE] = now

            if (totalSessions >= 10) preferences[Keys.MILESTONE_10] = true
            if (totalSessions >= 50) preferences[Keys.MILESTONE_50] = true
            if (totalSessions >= 100) preferences[Keys.MILESTONE_100] = true
            if (newStreak >= 7) preferences[Keys.MILESTONE_7_DAYS] = true
            if (newStreak >= 30) preferences[Keys.MILESTONE_30_DAYS] = true
        }

        WidgetUpdater.updateWidget(context)
    }

    suspend fun recordUrgeResisted() {
        context.achievementDataStore.edit { preferences ->
            val urgesResisted: Int = (preferences[Keys.URGES_RESISTED] ?: 0) + 1
            preferences[Keys.URGES_RESISTED] = urgesResisted

            if (urgesResisted >= 10) preferences[Keys.MILESTONE_URGES_10] = true
            if (urgesResisted >= 50) preferences[Keys.MILESTONE_URGES_50] = true
            if (urgesResisted >= 100) preferences[Keys.MILESTONE_URGES_100] = true
        }
    }
    
    // A DataStore Flow never completes, so collecting it here would suspend
    // forever and the message would never be returned. Take the current value.
    suspend fun getEncouragementMessage(): String {
        val data = achievementData.first()
        return when {
            data.currentStreak >= 30 -> "30+ days of showing up for yourself. That's incredible."
            data.currentStreak >= 7 -> "A whole week of self-care. You're building something real."
            data.totalSessions >= 100 -> "100 sessions of choosing calm. You're a warrior."
            data.totalSessions >= 50 -> "50 times you've chosen yourself. Keep going."
            data.totalSessions >= 10 -> "10 sessions completed. You're building a habit."
            data.currentStreak >= 3 -> "3 days in a row. Momentum is building."
            data.totalSessions >= 1 -> "Every session counts. You're doing great."
            else -> "Your first step is the most important."
        }
    }
}
