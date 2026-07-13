package app.andama.calmly.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.andama.calmly.service.DailyReminders
import app.andama.calmly.service.DangerHoursSentinel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = AlarmScheduler(context)
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarmTime = scheduler.getAlarmTime()
                    if (alarmTime != null) {
                        scheduler.scheduleSystemAlarm(alarmTime.first, alarmTime.second)
                    }
                    // Alarms don't survive reboot; re-arm the danger-window sentinel
                    // and the daily reminder too.
                    DangerHoursSentinel.reschedule(context)
                    DailyReminders.schedule(context)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
