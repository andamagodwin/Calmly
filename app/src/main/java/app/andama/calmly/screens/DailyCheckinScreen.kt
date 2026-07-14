package app.andama.calmly.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DailyCheckinScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var selectedMood by remember { mutableIntStateOf(-1) }
    var note by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var alreadyCheckedIn by remember { mutableStateOf(false) }
    var cleanDays by remember { mutableIntStateOf(0) }

    val moods = listOf(
        "Terrible" to DangerRed,
        "Struggling" to BrutalOrange,
        "Okay" to WarningAmber,
        "Good" to PrimaryBlue,
        "Strong" to SuccessGreen
    )

    LaunchedEffect(Unit) {
        alreadyCheckedIn = tracker.hasCheckedInToday()
        cleanDays = tracker.getCleanDays()
        tracker.startCleanStreak()
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
            EnterBounce {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.mascot_writing),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Daily Check-In",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$cleanDays",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen
                    )
                    Text(
                        text = "days clean",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }

            if (submitted || alreadyCheckedIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You've checked in today. Keep going.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    text = "How are you feeling right now?",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    moods.forEachIndexed { index, (label, color) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedMood = index + 1 }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedMood == index + 1) color else color.copy(alpha = 0.2f))
                                    .then(
                                        if (selectedMood == index + 1) Modifier.border(2.dp, color, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if (selectedMood == index + 1) color else TextSecondary
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftBackground)
                ) {
                    TextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SoftBackground,
                            unfocusedContainerColor = SoftBackground,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = PrimaryBlue,
                            unfocusedIndicatorColor = CalmGrey
                        ),
                        placeholder = { Text("Anything on your mind? (optional)", color = CalmGrey) },
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            tracker.logMood(selectedMood, note)
                            submitted = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedMood > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        disabledContainerColor = CalmGrey
                    )
                ) {
                    Text(
                        text = "Check In",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to Home", color = TextSecondary)
            }
        }
    }
}
