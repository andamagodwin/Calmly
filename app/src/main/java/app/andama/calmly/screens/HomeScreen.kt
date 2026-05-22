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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onOverwhelmClick: () -> Unit,
    onUrgeClick: () -> Unit,
    onNightResetClick: () -> Unit,
    onAlarmClick: () -> Unit,
    onDailyCheckinClick: () -> Unit,
    onTriggerTrackerClick: () -> Unit,
    onRelapseClick: () -> Unit,
    onDangerHoursClick: () -> Unit,
    onPartnerClick: () -> Unit,
    onAchievementsClick: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    var cleanDays by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        tracker.startCleanStreak()
        cleanDays = tracker.getCleanDays()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Calmly",
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = 2.sp
            )

            // Clean streak counter
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
                        color = if (cleanDays >= 7) SuccessGreen else if (cleanDays >= 3) PrimaryBlue else WarningAmber
                    )
                    Text(
                        text = "days clean",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Emergency buttons
            Button(
                onClick = onOverwhelmClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = Brush.horizontalGradient(listOf(AccentGradientStart, AccentGradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I feel overwhelmed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }

            Button(
                onClick = onUrgeClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = Brush.horizontalGradient(listOf(DangerRed, BrutalOrange))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "I'm having an urge",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Lock me down now",
                            fontSize = 12.sp,
                            color = TextPrimary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tools grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Check In",
                    subtitle = "Daily mood",
                    color = PrimaryBlue,
                    onClick = onDailyCheckinClick
                )
                ToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Triggers",
                    subtitle = "Know patterns",
                    color = WarningAmber,
                    onClick = onTriggerTrackerClick
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Danger Hours",
                    subtitle = "Auto-lock",
                    color = BrutalOrange,
                    onClick = onDangerHoursClick
                )
                ToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Partner",
                    subtitle = "Accountability",
                    color = SuccessGreen,
                    onClick = onPartnerClick
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Alarm",
                    subtitle = "QR wake-up",
                    color = WarningAmber,
                    onClick = onAlarmClick
                )
                ToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Night Reset",
                    subtitle = "Sleep routine",
                    color = PrimaryBlue,
                    onClick = onNightResetClick
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Bottom links
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onRelapseClick) {
                    Text("Relapse Log", color = DangerRed, fontSize = 14.sp)
                }
                TextButton(onClick = onAchievementsClick) {
                    Text("Achievements", color = PrimaryBlue, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ToolCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SoftBackground),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}
