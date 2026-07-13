package app.andama.calmly.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.achievements.AchievementManager
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CalmCompletionScreen(
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val achievementManager = remember { AchievementManager(context) }
    val scope = rememberCoroutineScope()
    var encouragement by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            achievementManager.recordSession()
            encouragement = achievementManager.getEncouragementMessage()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnterBounce {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SuccessGreen.copy(alpha = 0.4f),
                                    AccentGradientEnd.copy(alpha = 0.2f),
                                    SoftBackground
                                )
                            )
                        )
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.mascot_celebrate),
                        contentDescription = "Cal celebrating — you stayed calm",
                        modifier = Modifier.size(104.dp)
                    )
                }
            }

            Text(
                text = "You stayed with yourself.",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "The storm passed. You didn't break.",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (encouragement.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = encouragement,
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen
                )
            ) {
                Text(
                    text = "Back to Home",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}
