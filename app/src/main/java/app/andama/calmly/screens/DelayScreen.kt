package app.andama.calmly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun DelayScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var timeRemaining by remember { mutableIntStateOf(600) }
    var isTimerComplete by remember { mutableStateOf(false) }
    var currentMessageIndex by remember { mutableIntStateOf(0) }

    val messages = listOf(
        "This feeling will pass like a wave.",
        "You've survived every bad moment so far.",
        "10 minutes. That's all it takes to break the cycle.",
        "Your brain is lying. You don't need what it's offering.",
        "Discomfort is temporary. Regret lasts longer.",
        "The man you want to become sits on the other side of this timer."
    )

    LaunchedEffect(Unit) {
        while (timeRemaining > 0) {
            delay(1000L)
            timeRemaining--
        }
        isTimerComplete = true
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15000L)
            currentMessageIndex = (currentMessageIndex + 1) % messages.size
        }
    }

    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val timeDisplay = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Don't act. Wait.",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "10 minutes of doing nothing\nwill save you from hours of regret.",
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = if (timeRemaining > 60) PrimaryBlue else SuccessGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = messages[currentMessageIndex],
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isTimerComplete) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Text(
                        text = "Good. Keep going.",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            } else {
                Text(
                    text = "Stay here. Don't run from it.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
