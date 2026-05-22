package app.andama.calmly.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.alarm.AlarmScheduler
import app.andama.calmly.ui.theme.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.io.OutputStream

@Composable
fun AlarmSetupScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scheduler = remember { AlarmScheduler(context) }

    var selectedHour by remember { mutableIntStateOf(6) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var alarmSet by remember { mutableStateOf(false) }
    var qrSecret by remember { mutableStateOf<String?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var savedToGallery by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val existingTime = scheduler.getAlarmTime()
        if (existingTime != null) {
            selectedHour = existingTime.first
            selectedMinute = existingTime.second
            alarmSet = true
        }
        val secret = scheduler.getQrSecret()
        if (secret != null) {
            qrSecret = secret
            qrBitmap = generateQrBitmap(secret)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Wake-Up Alarm",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Only way to stop it: scan the QR code\nyou put in your bathroom.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Time display / picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                onClick = { showTimePicker = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Alarm Time",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%02d:%02d", selectedHour, selectedMinute),
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarmSet) SuccessGreen else PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (alarmSet) "Alarm is ON" else "Tap to set time",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = if (alarmSet) SuccessGreen else CalmGrey
                    )
                }
            }

            if (showTimePicker) {
                TimePickerCard(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onHourChange = { selectedHour = it },
                    onMinuteChange = { selectedMinute = it },
                    onConfirm = {
                        showTimePicker = false
                        scope.launch {
                            scheduler.setAlarm(selectedHour, selectedMinute)
                            alarmSet = true
                        }
                    }
                )
            }

            // Set / Cancel alarm buttons
            if (!alarmSet) {
                Button(
                    onClick = {
                        scope.launch {
                            scheduler.setAlarm(selectedHour, selectedMinute)
                            alarmSet = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(
                        text = "SET ALARM",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            scheduler.cancelAlarm()
                            alarmSet = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Cancel Alarm",
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code section
            Text(
                text = "Your QR Code",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Print this and stick it in your bathroom.\nIt's the ONLY thing that stops the alarm.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            if (qrBitmap != null) {
                Card(
                    modifier = Modifier.size(240.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Button(
                    onClick = {
                        qrBitmap?.let { bitmap ->
                            saveQrToGallery(context, bitmap)
                            savedToGallery = true
                            Toast.makeText(context, "QR saved to gallery! Print it.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (savedToGallery) SuccessGreen else PrimaryBlue
                    )
                ) {
                    Text(
                        text = if (savedToGallery) "Saved! Now print it." else "Save QR to Gallery",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            val secret = scheduler.generateQrSecret()
                            qrSecret = secret
                            qrBitmap = generateQrBitmap(secret)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrutalOrange)
                ) {
                    Text(
                        text = "Generate My QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "How it works:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Text(
                        text = "1. Generate your unique QR code\n2. Save it and PRINT it\n3. Stick it on your bathroom mirror\n4. When the alarm fires, you MUST physically get up and scan it\n5. No snooze. No dismiss. Only the QR code.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to Home",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TimePickerCard(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set Time",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { onHourChange((hour + 1) % 24) }) {
                        Text("▲", fontSize = 24.sp, color = PrimaryBlue)
                    }
                    Text(
                        text = String.format("%02d", hour),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    TextButton(onClick = { onHourChange(if (hour == 0) 23 else hour - 1) }) {
                        Text("▼", fontSize = 24.sp, color = PrimaryBlue)
                    }
                }

                Text(":", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                // Minute
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { onMinuteChange((minute + 5) % 60) }) {
                        Text("▲", fontSize = 24.sp, color = PrimaryBlue)
                    }
                    Text(
                        text = String.format("%02d", minute),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    TextButton(onClick = { onMinuteChange(if (minute < 5) 55 else minute - 5) }) {
                        Text("▼", fontSize = 24.sp, color = PrimaryBlue)
                    }
                }
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
            ) {
                Text(
                    text = "Confirm",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun generateQrBitmap(content: String): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }
    }
    return bitmap
}

private fun saveQrToGallery(context: Context, bitmap: Bitmap) {
    val filename = "calmly_alarm_qr_${System.currentTimeMillis()}.png"
    var outputStream: OutputStream? = null

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Calmly")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            outputStream = context.contentResolver.openOutputStream(it)
        }
    } else {
        @Suppress("DEPRECATION")
        val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val calmlyDir = java.io.File(imagesDir, "Calmly")
        calmlyDir.mkdirs()
        val file = java.io.File(calmlyDir, filename)
        outputStream = java.io.FileOutputStream(file)
    }

    outputStream?.use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
    }
}
