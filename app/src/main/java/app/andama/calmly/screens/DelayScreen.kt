package app.andama.calmly.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.ui.components.CalmlyButton
import app.andama.calmly.ui.components.EnterBounce
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
        "The person you want to become sits on the other side of this timer."
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnterBounce {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.mascot_think),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Don't act. Wait.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "10 minutes of doing nothing\nwill save you from hours of regret.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            EnterBounce(delayMillis = 120) {
                Text(
                    text = timeDisplay,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp,
                    color = if (timeRemaining > 60) PrimaryBlue else SuccessGreen,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Crossfade instead of a hard snap when the message rotates.
            Crossfade(
                targetState = currentMessageIndex,
                animationSpec = tween(600),
                label = "delay_message"
            ) { index ->
                Text(
                    text = messages[index],
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isTimerComplete) {
                EnterBounce {
                    CalmlyButton(
                        text = "Good. Keep going.",
                        containerColor = SuccessGreen,
                        onClick = onNext
                    )
                }
            } else {
                Text(
                    text = "Stay here. Don't run from it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
