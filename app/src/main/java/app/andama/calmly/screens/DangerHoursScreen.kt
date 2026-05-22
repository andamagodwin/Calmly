package app.andama.calmly.screens

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
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DangerHoursScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var startHour by remember { mutableIntStateOf(23) }
    var endHour by remember { mutableIntStateOf(5) }
    var enabled by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val dangerHours = tracker.getDangerHours()
        if (dangerHours != null) {
            startHour = dangerHours.first
            endHour = dangerHours.second
            enabled = dangerHours.third
        }
        loaded = true
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Danger Hours",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Set your high-risk hours.\nOverlay auto-activates during this window.",
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
                        text = "Enabled",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            scope.launch {
                                tracker.setDangerHours(startHour, endHour, it)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DangerRed,
                            checkedTrackColor = DangerRed.copy(alpha = 0.4f)
                        )
                    )
                }
            }

            // Start hour
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lock starts at", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(onClick = {
                            startHour = if (startHour == 0) 23 else startHour - 1
                            scope.launch { tracker.setDangerHours(startHour, endHour, enabled) }
                        }) {
                            Text("◀", fontSize = 24.sp, color = PrimaryBlue)
                        }
                        Text(
                            text = String.format("%02d:00", startHour),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        TextButton(onClick = {
                            startHour = (startHour + 1) % 24
                            scope.launch { tracker.setDangerHours(startHour, endHour, enabled) }
                        }) {
                            Text("▶", fontSize = 24.sp, color = PrimaryBlue)
                        }
                    }
                }
            }

            // End hour
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lock ends at", fontSize = 14.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(onClick = {
                            endHour = if (endHour == 0) 23 else endHour - 1
                            scope.launch { tracker.setDangerHours(startHour, endHour, enabled) }
                        }) {
                            Text("◀", fontSize = 24.sp, color = PrimaryBlue)
                        }
                        Text(
                            text = String.format("%02d:00", endHour),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        TextButton(onClick = {
                            endHour = (endHour + 1) % 24
                            scope.launch { tracker.setDangerHours(startHour, endHour, enabled) }
                        }) {
                            Text("▶", fontSize = 24.sp, color = PrimaryBlue)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Text(
                    text = if (enabled)
                        "The overlay will auto-activate between ${String.format("%02d:00", startHour)} and ${String.format("%02d:00", endHour)} every day."
                    else
                        "Enable to auto-lock your phone during dangerous hours.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = if (enabled) WarningAmber else CalmGrey,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Home", color = TextSecondary)
            }
        }
    }
}
