package app.andama.calmly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        "Think about the man you said you'd become.",
        "This is your brain lying to you. It's not a need.",
        "Every time you give in, you get weaker.",
        "You've never once felt better after. Never.",
        "The version of you that resists this is the real you.",
        "Your future self is watching. Make him proud.",
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
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "STOP.",
                style = MaterialTheme.typography.displayLarge,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = DangerRed,
                textAlign = TextAlign.Center
            )

            Text(
                text = "You don't need this. You never did.",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = timeDisplay,
                style = MaterialTheme.typography.displayLarge,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = DangerRed,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Survive this timer. That's all.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = brutalMessages[currentMessageIndex],
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarningAmber,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (isTimerComplete) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SuccessGreen, PrimaryBlue)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "I survived it. Next step.",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            } else {
                Text(
                    text = "No shortcuts. Sit with the discomfort.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
