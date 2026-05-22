package app.andama.calmly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

@Composable
fun HomeScreen(
    onOverwhelmClick: () -> Unit,
    onUrgeClick: () -> Unit,
    onNightResetClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onAchievementsClick: () -> Unit
) {
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
            Text(
                text = "Calmly",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onOverwhelmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(24.dp)),
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
                                colors = listOf(AccentGradientStart, AccentGradientEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I feel overwhelmed",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = onUrgeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(24.dp)),
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
                                colors = listOf(DangerRed, BrutalOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "I'm having an urge",
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Lock me down now",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = TextPrimary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onNightResetClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PrimaryBlue
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(PrimaryBlue, SoftPurple)
                    )
                )
            ) {
                Text(
                    text = "Night Reset",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = PrimaryBlue,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedButton(
                onClick = onAlarmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(24.dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = WarningAmber
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(WarningAmber, BrutalOrange)
                    )
                )
            ) {
                Text(
                    text = "Wake-Up Alarm (QR)",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarningAmber,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You have the power to choose differently.",
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onAchievementsClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Achievements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryBlue
                )
            }
        }
    }
}
