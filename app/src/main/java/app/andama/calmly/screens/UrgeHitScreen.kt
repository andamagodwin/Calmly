package app.andama.calmly.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.ui.components.CalmlyCard
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.components.GradientActionButton
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun UrgeHitScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var timeRemaining by remember { mutableIntStateOf(900) }
    var isTimerComplete by remember { mutableStateOf(false) }
    var currentMessageIndex by remember { mutableIntStateOf(0) }

    val brutalMessages = listOf(
        "You know exactly where this leads.",
        "15 minutes of shame for 10 seconds of nothing.",
        "Think about the person you said you'd become.",
        "This is your brain lying to you. It's not a need.",
        "The urge is loudest right before it breaks.",
        "You've never once felt better after. Never.",
        "The version of you that resists this is the real you.",
        "Your future self is watching. Make them proud.",
        "This urge will peak and die. You just have to outlast it.",
        "You're stronger than a chemical impulse."
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
            delay(12000L)
            currentMessageIndex = (currentMessageIndex + 1) % brutalMessages.size
        }
    }

    // A slow heartbeat on the timer — urgency you can see without reading.
    val heartbeat = rememberInfiniteTransition(label = "heartbeat")
    val timerScale by heartbeat.animateFloat(
        initialValue = 1f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "timer_pulse"
    )

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
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnterBounce {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.mascot_guard),
                        contentDescription = "Cal standing guard",
                        modifier = Modifier.size(104.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "STOP.",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = DangerRed,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "You don't need this. You never did.",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            EnterBounce(delayMillis = 120) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeDisplay,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2).sp,
                        color = DangerRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.scale(if (isTimerComplete) 1f else timerScale)
                    )

                    Text(
                        text = "Survive this timer. That's all.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            EnterBounce(delayMillis = 200) {
                CalmlyCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(
                            targetState = currentMessageIndex,
                            animationSpec = tween(600),
                            label = "urge_message"
                        ) { index ->
                            Text(
                                text = brutalMessages[index],
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = WarningAmber,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isTimerComplete) {
                EnterBounce {
                    GradientActionButton(
                        title = "I survived it. Next step.",
                        gradient = listOf(SuccessGreen, PrimaryBlue),
                        height = 64.dp,
                        onClick = onNext
                    )
                }
            } else {
                Text(
                    text = "No shortcuts. Sit with the discomfort.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
