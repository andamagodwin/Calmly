package app.andama.calmly.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = AlarmScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                val alarmTime = scheduler.getAlarmTime()
                if (alarmTime != null) {
                    scheduler.scheduleSystemAlarm(alarmTime.first, alarmTime.second)
                }
            }
        }
    }
}
