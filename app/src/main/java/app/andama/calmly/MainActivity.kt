package app.andama.calmly

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.navigation.CalmlyNavHost
import app.andama.calmly.service.OverlayService
import app.andama.calmly.service.QuoteReceiver
import app.andama.calmly.ui.theme.CalmlyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        scheduleQuoteNotifications()
        checkDangerHours()

        setContent {
            CalmlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    CalmlyNavHost(navController = navController)
                }
            }
        }
    }

    private fun scheduleQuoteNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, QuoteReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 4 * 60 * 60 * 1000L,
            4 * 60 * 60 * 1000L,
            pendingIntent
        )
    }

    private fun checkDangerHours() {
        CoroutineScope(Dispatchers.IO).launch {
            val tracker = CalmlyTracker(this@MainActivity)
            if (tracker.isInDangerHours()) {
                val hasPermission = android.provider.Settings.canDrawOverlays(this@MainActivity)
                if (hasPermission) {
                    OverlayService.startService(
                        this@MainActivity,
                        durationMs = 60 * 60 * 1000L,
                        mode = "urge"
                    )
                }
            }
        }
    }
}
