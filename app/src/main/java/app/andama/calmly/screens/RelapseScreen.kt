package app.andama.calmly.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.RelapseEntry
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun RelapseScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var showLogForm by remember { mutableStateOf(false) }
    var triggerText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var logged by remember { mutableStateOf(false) }
    var relapseLog by remember { mutableStateOf<List<RelapseEntry>>(emptyList()) }
    var totalRelapses by remember { mutableIntStateOf(0) }
    var cleanDays by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        relapseLog = tracker.getRelapseLog()
        totalRelapses = tracker.getTotalRelapses()
        cleanDays = tracker.getCleanDays()
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(R.drawable.mascot_comfort),
                contentDescription = "Cal offering a hug — a reset is not the end",
                modifier = Modifier.size(110.dp)
            )

            Text(
                text = "Relapse Log",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Honesty is the only path forward.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$cleanDays",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Text("days clean", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$totalRelapses",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Text("total relapses", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!showLogForm && !logged) {
                Button(
                    onClick = { showLogForm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text(
                        text = "I Relapsed",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Only press this if you actually did.\nBe honest with yourself.",
                    fontSize = 14.sp,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }

            if (showLogForm && !logged) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "What triggered this?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )

                        TextField(
                            value = triggerText,
                            onValueChange = { triggerText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            placeholder = { Text("Boredom, loneliness, etc.", color = CalmGrey) },
                            singleLine = true
                        )

                        Text(
                            text = "What will you do differently next time?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )

                        TextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CardBackground,
                                unfocusedContainerColor = CardBackground,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            placeholder = { Text("Be specific...", color = CalmGrey) }
                        )

                        Button(
                            onClick = {
                                scope.launch {
                                    tracker.logRelapse(triggerText, notesText)
                                    logged = true
                                    relapseLog = tracker.getRelapseLog()
                                    totalRelapses = tracker.getTotalRelapses()
                                    cleanDays = 0
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = triggerText.isNotEmpty() && notesText.length >= 10,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                disabledContainerColor = CalmGrey
                            )
                        ) {
                            Text("Log It. Start Over.", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }
                }
            }

            if (logged) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Streak reset to 0.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "But you didn't hide from it.\nThat takes strength. Now rebuild.",
                            fontSize = 16.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            if (relapseLog.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                relapseLog.take(10).forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.date, fontSize = 12.sp, color = CalmGrey)
                                Text(
                                    "${entry.streakLost}d lost",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            }
                            Text(
                                "Trigger: ${entry.trigger}",
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Home", color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
