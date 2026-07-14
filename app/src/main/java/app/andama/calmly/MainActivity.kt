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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import app.andama.calmly.alarm.AlarmActivity
import app.andama.calmly.alarm.AlarmService
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.navigation.CalmlyNavHost
import app.andama.calmly.navigation.Screen
import app.andama.calmly.service.DailyReminders
import app.andama.calmly.service.QuoteReceiver
import app.andama.calmly.ui.theme.CalmlyTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Plain Compose state on the Activity itself (not `remember`ed) so that both
    // onCreate (cold start) and onNewIntent (the app already running — this
    // activity is singleTask, so a notification tap re-enters through
    // onNewIntent rather than a fresh onCreate) can push a route into the same
    // place, and a composable can observe it and navigate.
    private var pendingDeepLinkRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (redirectToAlarmIfRinging()) return

        requestNotificationPermission()
        scheduleQuoteNotifications()
        DailyReminders.scheduleIfNeeded(this)
        pendingDeepLinkRoute = intent?.getStringExtra(EXTRA_DEEPLINK_ROUTE)

        // One tiny synchronous read at startup, while the splash screen is still
        // covering the window — the graph needs its start destination up front.
        val startDestination = runBlocking {
            if (CalmlyTracker(this@MainActivity).getUserName() == null) {
                Screen.Onboarding.route
            } else {
                Screen.Home.route
            }
        }

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

                        // Consume the deep link once the graph exists. Skipped on a
                        // fresh install: forcing a brand-new user off onboarding and
                        // into, say, Achievements would be a broken first run — not
                        // that a milestone or evening notification could fire before
                        // onboarding finishes anyway, but a stale/replayed intent
                        // shouldn't be able to do it either.
                        LaunchedEffect(pendingDeepLinkRoute) {
                            val route = pendingDeepLinkRoute
                            if (route != null && startDestination != Screen.Onboarding.route) {
                                navController.navigate(route) { launchSingleTop = true }
                            }
                            pendingDeepLinkRoute = null
                        }

                        CalmlyNavHost(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkRoute = intent.getStringExtra(EXTRA_DEEPLINK_ROUTE)
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

        /** Route string carried by notification PendingIntents; see [deepLinkIntent]. */
        const val EXTRA_DEEPLINK_ROUTE = "deeplink_route"
    }
}
