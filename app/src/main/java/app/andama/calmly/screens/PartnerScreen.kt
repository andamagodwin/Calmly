package app.andama.calmly.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PartnerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var partnerName by remember { mutableStateOf("") }
    var partnerPhone by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val partner = tracker.getAccountabilityPartner()
        if (partner != null) {
            partnerName = partner.first
            partnerPhone = partner.second
            enabled = partner.third
            saved = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Accountability Partner",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "When you hit the urge button, this person\ngets an automatic text. No hiding.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-text enabled",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val smsPermission = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { }

                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            // The auto-text only sends silently with SEND_SMS granted;
                            // ask the moment the user opts in, not mid-crisis.
                            if (it && ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.SEND_SMS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                smsPermission.launch(Manifest.permission.SEND_SMS)
                            }
                            scope.launch {
                                tracker.setAccountabilityPartner(partnerName, partnerPhone, it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SuccessGreen,
                            checkedTrackColor = SuccessGreen.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBackground)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Partner's Name", fontSize = 14.sp, color = TextSecondary)
                    TextField(
                        value = partnerName,
                        onValueChange = { partnerName = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        placeholder = { Text("Their name", color = CalmGrey) },
                        singleLine = true
                    )

                    Text("Phone Number", fontSize = 14.sp, color = TextSecondary)
                    TextField(
                        value = partnerPhone,
                        onValueChange = { partnerPhone = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CardBackground,
                            unfocusedContainerColor = CardBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        placeholder = { Text("+1 (555) 123-4567", color = CalmGrey) },
                        singleLine = true
                    )
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        tracker.setAccountabilityPartner(partnerName, partnerPhone, enabled)
                        saved = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = partnerName.isNotEmpty() && partnerPhone.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = CalmGrey
                )
            ) {
                Text(
                    text = if (saved) "Saved" else "Save Partner",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (saved && enabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Message that will be sent:",
                            fontSize = 12.sp,
                            color = CalmGrey
                        )
                        Text(
                            text = "\"Hey $partnerName, I'm struggling right now and I need accountability. - Sent from Calmly\"",
                            fontSize = 14.sp,
                            color = WarningAmber,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Home", color = TextSecondary)
            }
        }
    }
}

fun sendAccountabilityText(context: android.content.Context, name: String, phone: String) {
    val message = "Hey $name, I'm struggling right now and I need accountability. - Sent from Calmly"

    // Send for real when we can. The old behaviour — opening the SMS composer —
    // happened *underneath* the urge lock's full-screen overlay, so the text was
    // never visible, never sent, and the partner never knew.
    val canSend = ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED
    if (canSend) {
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phone, null, message, null, null)
            return
        } catch (_: Exception) {
            // fall through to the composer
        }
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("sms:$phone")
        putExtra("sms_body", message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {}
}
