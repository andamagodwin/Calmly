package app.andama.calmly.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Start the foreground service for persistent alarm sound
        AlarmService.start(this)

        val alarmScheduler = AlarmScheduler(this)

        setContent {
            var showScanner by remember { mutableStateOf(false) }
            var scanError by remember { mutableStateOf("") }

            if (showScanner) {
                QrScannerScreen(
                    onQrScanned = { scannedValue ->
                        CoroutineScope(Dispatchers.IO).launch {
                            val secret = alarmScheduler.getQrSecret()
                            if (scannedValue == secret) {
                                runOnUiThread {
                                    dismissAlarm()
                                }
                            } else {
                                runOnUiThread {
                                    scanError = "Wrong QR code. Get the one from your bathroom."
                                    showScanner = false
                                }
                            }
                        }
                    },
                    onBack = { showScanner = false }
                )
            } else {
                AlarmScreen(
                    scanError = scanError,
                    onScanClick = {
                        scanError = ""
                        showScanner = true
                    }
                )
            }
        }
    }

    private fun dismissAlarm() {
        AlarmService.stop(this)
        val scheduler = AlarmScheduler(this)
        CoroutineScope(Dispatchers.IO).launch {
            val alarmTime = scheduler.getAlarmTime()
            if (alarmTime != null) {
                scheduler.scheduleSystemAlarm(alarmTime.first, alarmTime.second)
            }
        }
        finish()
    }

    @Deprecated("Use onBackInvokedCallback")
    override fun onBackPressed() {
        // Do nothing - can't dismiss alarm with back button
    }
}

@Composable
fun AlarmScreen(
    scanError: String,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "WAKE UP",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = DangerRed,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Get out of bed.\nGo scan your QR code.",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "There is NO other way to stop this alarm.\nSwitching apps won't help.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = WarningAmber,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (scanError.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DangerRed.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = scanError,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 16.sp,
                        color = DangerRed,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onScanClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed
                )
            ) {
                Text(
                    text = "SCAN QR CODE TO DISMISS",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "No snooze. No shortcuts.\nGet up and move.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = CalmGrey,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
