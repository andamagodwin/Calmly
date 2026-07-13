package app.andama.calmly

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import app.andama.calmly.alarm.AlarmActivity
import app.andama.calmly.alarm.AlarmService
import app.andama.calmly.navigation.CalmlyNavHost
import app.andama.calmly.service.DailyReminders
import app.andama.calmly.service.QuoteReceiver
import app.andama.calmly.ui.theme.CalmlyTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (redirectToAlarmIfRinging()) return

        requestNotificationPermission()
        scheduleQuoteNotifications()
        DailyReminders.scheduleIfNeeded(this)

        setContent {
            CalmlyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // The app runs edge-to-edge, so without this every screen draws
                    // under the status and navigation bars. Applying it once here
                    // consumes the insets for the whole graph.
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                        val navController = rememberNavController()
                        CalmlyNavHost(navController = navController)
                    }
                }
            }
        }
    }

    /**
     * Declared in the manifest but never requested, which meant every notification
     * the app posts (quotes, alarm foreground service) was silently dropped on
     * Android 13+.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * setRepeating() on every launch restarted the 4-hour window each time, so
     * anyone who opened the app regularly never reached the first quote.
     * FLAG_NO_CREATE tells us whether the alarm already exists; only schedule if not.
     */
    private fun scheduleQuoteNotifications() {
        val intent = Intent(this, QuoteReceiver::class.java)
        val alreadyScheduled = PendingIntent.getBroadcast(
            this, QUOTE_REQUEST_CODE, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null
        if (alreadyScheduled) return

        val pendingIntent = PendingIntent.getBroadcast(
            this, QUOTE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact: these are encouragement nudges, not alarms. Exact repeating
        // alarms are throttled by the system anyway and cost battery.
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + QUOTE_INTERVAL_MS,
            QUOTE_INTERVAL_MS,
            pendingIntent
        )
    }

    private fun redirectToAlarmIfRinging(): Boolean {
        if (AlarmService.isRinging) {
            val intent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
            return true
        }
        return false
    }

    companion object {
        private const val QUOTE_REQUEST_CODE = 100
        private const val QUOTE_INTERVAL_MS = 4 * 60 * 60 * 1000L
    }
}
