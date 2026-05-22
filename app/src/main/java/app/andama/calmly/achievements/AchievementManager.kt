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
import kotlinx.coroutines.flow.map
import java.util.Calendar

private val Context.achievementDataStore: DataStore<Preferences> by preferencesDataStore(name = "achievements")

class AchievementManager(private val context: Context) {
    
    private object Keys {
        val TOTAL_SESSIONS = intPreferencesKey("total_sessions")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val LAST_SESSION_DATE = longPreferencesKey("last_session_date")
        val MILESTONE_10 = booleanPreferencesKey("milestone_10")
        val MILESTONE_50 = booleanPreferencesKey("milestone_50")
        val MILESTONE_100 = booleanPreferencesKey("milestone_100")
        val MILESTONE_7_DAYS = booleanPreferencesKey("milestone_7_days")
        val MILESTONE_30_DAYS = booleanPreferencesKey("milestone_30_days")
    }
    
    data class AchievementData(
        val totalSessions: Int,
        val currentStreak: Int,
        val longestStreak: Int,
        val lastSessionDate: Long,
        val milestones: Map<String, Boolean>
    )
    
    val achievementData: Flow<AchievementData> = context.achievementDataStore.data.map { preferences ->
        AchievementData(
            totalSessions = preferences[Keys.TOTAL_SESSIONS] ?: 0,
            currentStreak = preferences[Keys.CURRENT_STREAK] ?: 0,
            longestStreak = preferences[Keys.LONGEST_STREAK] ?: 0,
            lastSessionDate = preferences[Keys.LAST_SESSION_DATE] ?: 0,
            milestones = mapOf(
                "10_sessions" to (preferences[Keys.MILESTONE_10] ?: false),
                "50_sessions" to (preferences[Keys.MILESTONE_50] ?: false),
                "100_sessions" to (preferences[Keys.MILESTONE_100] ?: false),
                "7_days_streak" to (preferences[Keys.MILESTONE_7_DAYS] ?: false),
                "30_days_streak" to (preferences[Keys.MILESTONE_30_DAYS] ?: false)
            )
        )
    }
    
    suspend fun recordSession() {
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        val oneDay = 24 * 60 * 60 * 1000L
        
        context.achievementDataStore.edit { preferences ->
            val totalSessions: Int = (preferences[Keys.TOTAL_SESSIONS] ?: 0) + 1
            val lastSessionDate: Long = preferences[Keys.LAST_SESSION_DATE] ?: 0L
            val currentStreak: Int = preferences[Keys.CURRENT_STREAK] ?: 0
            val longestStreak: Int = preferences[Keys.LONGEST_STREAK] ?: 0
            
            // Calculate streak
            val newStreak: Int = if (lastSessionDate == 0L) {
                1
            } else if (today - lastSessionDate <= oneDay * 2) {
                // Within 2 days (allow for slightly different times)
                currentStreak + 1
            } else {
                1
            }
            
            val newLongestStreak: Int = maxOf(longestStreak, newStreak)
            
            preferences[Keys.TOTAL_SESSIONS] = totalSessions
            preferences[Keys.CURRENT_STREAK] = newStreak
            preferences[Keys.LONGEST_STREAK] = newLongestStreak
            preferences[Keys.LAST_SESSION_DATE] = today
            
            // Check milestones
            if (totalSessions >= 10) preferences[Keys.MILESTONE_10] = true
            if (totalSessions >= 50) preferences[Keys.MILESTONE_50] = true
            if (totalSessions >= 100) preferences[Keys.MILESTONE_100] = true
            if (newStreak >= 7) preferences[Keys.MILESTONE_7_DAYS] = true
            if (newStreak >= 30) preferences[Keys.MILESTONE_30_DAYS] = true
        }
        
        // Update widget
        WidgetUpdater.updateWidget(context)
    }
    
    suspend fun getEncouragementMessage(): String {
        var result = ""
        achievementData.collect { data ->
            result = when {
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
        return result
    }
}
