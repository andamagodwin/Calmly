package app.andama.calmly.alarm

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

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

        startAlarmSound()
        startVibration()

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

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {}
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 800, 200, 800, 200, 800, 500)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 800, 200, 800, 200, 800, 500), 0)
        }
    }

    private fun dismissAlarm() {
        stopAlarmSound()
        stopVibration()
        val scheduler = AlarmScheduler(this)
        CoroutineScope(Dispatchers.IO).launch {
            val alarmTime = scheduler.getAlarmTime()
            if (alarmTime != null) {
                scheduler.scheduleSystemAlarm(alarmTime.first, alarmTime.second)
            }
        }
        finish()
    }

    private fun stopAlarmSound() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
        stopVibration()
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
                text = "There is NO other way to stop this alarm.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = WarningAmber,
                textAlign = TextAlign.Center
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
