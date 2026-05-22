package app.andama.calmly.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TriggerTrackerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var triggerStats by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var selectedTrigger by remember { mutableStateOf<String?>(null) }
    var customTrigger by remember { mutableStateOf("") }
    var logged by remember { mutableStateOf(false) }

    val commonTriggers = listOf(
        "Boredom",
        "Loneliness",
        "Late at night",
        "Stress/Anxiety",
        "Social media",
        "Alcohol",
        "After argument",
        "Woke up early"
    )

    LaunchedEffect(Unit) {
        triggerStats = tracker.getTriggerStats()
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

            Text(
                text = "Trigger Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Know your patterns. Defeat them.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!logged) {
                Text(
                    text = "What triggered you?",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )

                commonTriggers.forEach { trigger ->
                    val isSelected = selectedTrigger == trigger
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CardBackground else SoftBackground
                        ),
                        onClick = { selectedTrigger = trigger }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = trigger,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrutalOrange else TextPrimary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftBackground)
                ) {
                    TextField(
                        value = customTrigger,
                        onValueChange = {
                            customTrigger = it
                            if (it.isNotEmpty()) selectedTrigger = it
                        },
                        modifier = Modifier.fillMaxSize(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SoftBackground,
                            unfocusedContainerColor = SoftBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = SoftBackground
                        ),
                        placeholder = { Text("Other...", color = CalmGrey) },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            selectedTrigger?.let {
                                tracker.logTrigger(it)
                                triggerStats = tracker.getTriggerStats()
                                logged = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = selectedTrigger != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = CalmGrey
                    )
                ) {
                    Text("Log Trigger", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Logged. Now you know what to watch for.",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (triggerStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your Pattern",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                triggerStats.entries.take(8).forEach { (trigger, count) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = trigger,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${count}x",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (count >= 5) DangerRed else if (count >= 3) WarningAmber else TextSecondary
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
