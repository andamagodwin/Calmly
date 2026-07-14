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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.ui.theme.*

@Composable
fun BreathingScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var breathText by remember { mutableStateOf("Inhale") }

    // One driver for both the circle and the label, so they can never drift:
    // the old version scaled 4s-up/4s-down while the text promised a 6s exhale.
    val breathScale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            breathText = "Inhale"
            breathScale.animateTo(1.5f, tween(durationMillis = 4000, easing = FastOutSlowInEasing))
            breathText = "Exhale"
            breathScale.animateTo(1f, tween(durationMillis = 6000, easing = FastOutSlowInEasing))
        }
    }
    val scale = breathScale.value
    
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
                text = "Follow the circle",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Animated breathing circle
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentGradientStart.copy(alpha = 0.6f),
                                AccentGradientEnd.copy(alpha = 0.3f),
                                SoftBackground
                            )
                        )
                    )
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.mascot_meditate),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        text = breathText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Light,
                        color = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = "4 seconds in, 6 seconds out",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                )
            ) {
                Text(
                    text = "I'm ready to continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Go back",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
