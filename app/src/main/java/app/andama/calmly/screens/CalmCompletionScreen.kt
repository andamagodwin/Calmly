package app.andama.calmly.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.achievements.AchievementManager
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
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Calming circle animation
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentGradientStart.copy(alpha = 0.4f),
                                AccentGradientEnd.copy(alpha = 0.2f),
                                SoftBackground
                            )
                        )
                    )
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = PrimaryBlue
                )
            }
            
            Text(
                text = "You stayed with yourself.",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "This feeling has passed.",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            if (encouragement.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
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
                            color = PrimaryBlue,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onBackToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Text(
                    text = "Back to Home",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Remember: You can always come back here.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp,
                color = CalmGrey,
                textAlign = TextAlign.Center
            )
        }
    }
}
