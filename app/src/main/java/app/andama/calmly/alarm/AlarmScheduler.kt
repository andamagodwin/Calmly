package app.andama.calmly.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID

private val Context.alarmDataStore by preferencesDataStore(name = "alarms")

object AlarmKeys {
    val QR_SECRET = stringPreferencesKey("qr_secret")
    val ALARM_HOUR = longPreferencesKey("alarm_hour")
    val ALARM_MINUTE = longPreferencesKey("alarm_minute")
    val ALARM_ENABLED = stringPreferencesKey("alarm_enabled")
}

class AlarmScheduler(private val context: Context) {

    suspend fun generateQrSecret(): String {
        val secret = "CALMLY-${UUID.randomUUID().toString().take(8).uppercase()}"
        context.alarmDataStore.edit { prefs ->
            prefs[AlarmKeys.QR_SECRET] = secret
        }
        return secret
    }

    suspend fun getQrSecret(): String? {
        return context.alarmDataStore.data.map { prefs ->
            prefs[AlarmKeys.QR_SECRET]
        }.first()
    }

    suspend fun setAlarm(hour: Int, minute: Int) {
        context.alarmDataStore.edit { prefs ->
            prefs[AlarmKeys.ALARM_HOUR] = hour.toLong()
            prefs[AlarmKeys.ALARM_MINUTE] = minute.toLong()
            prefs[AlarmKeys.ALARM_ENABLED] = "true"
        }
        scheduleSystemAlarm(hour, minute)
    }

    suspend fun cancelAlarm() {
        context.alarmDataStore.edit { prefs ->
            prefs[AlarmKeys.ALARM_ENABLED] = "false"
        }
        cancelSystemAlarm()
    }

    suspend fun getAlarmTime(): Pair<Int, Int>? {
        val data = context.alarmDataStore.data.first()
        val enabled = data[AlarmKeys.ALARM_ENABLED] ?: "false"
        if (enabled != "true") return null
        val hour = data[AlarmKeys.ALARM_HOUR]?.toInt() ?: return null
        val minute = data[AlarmKeys.ALARM_MINUTE]?.toInt() ?: return null
        return Pair(hour, minute)
    }

    fun scheduleSystemAlarm(hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
        } else {
            // Never silently skip a wake-up alarm: if exact scheduling is revoked,
            // a windowed alarm still rings within a few minutes of the target.
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                5 * 60 * 1000L,
                pendingIntent
            )
        }
    }

    private fun cancelSystemAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
