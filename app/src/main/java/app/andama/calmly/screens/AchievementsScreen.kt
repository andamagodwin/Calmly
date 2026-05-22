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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.achievements.AchievementManager
import app.andama.calmly.ui.theme.*

@Composable
fun AchievementsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val achievementManager = remember { AchievementManager(context) }

    var achievementData by remember { mutableStateOf<AchievementManager.AchievementData?>(null) }

    LaunchedEffect(Unit) {
        achievementManager.achievementData.collect { data ->
            achievementData = data
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your War Record",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            if (achievementData != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Sessions",
                        value = achievementData!!.totalSessions.toString(),
                        color = PrimaryBlue
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Streak",
                        value = "${achievementData!!.currentStreak}d",
                        color = PrimaryBlue
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Urges Beaten",
                        value = achievementData!!.urgesResisted.toString(),
                        color = SuccessGreen
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Best Streak",
                        value = "${achievementData!!.longestStreak}d",
                        color = WarningAmber
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Milestones",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val milestones = listOf(
                    "10 Sessions" to (achievementData!!.milestones["10_sessions"] ?: false),
                    "50 Sessions" to (achievementData!!.milestones["50_sessions"] ?: false),
                    "100 Sessions" to (achievementData!!.milestones["100_sessions"] ?: false),
                    "7 Day Streak" to (achievementData!!.milestones["7_days_streak"] ?: false),
                    "30 Day Streak" to (achievementData!!.milestones["30_days_streak"] ?: false),
                    "10 Urges Defeated" to (achievementData!!.milestones["10_urges_resisted"] ?: false),
                    "50 Urges Defeated" to (achievementData!!.milestones["50_urges_resisted"] ?: false),
                    "100 Urges Defeated" to (achievementData!!.milestones["100_urges_resisted"] ?: false)
                )

                milestones.forEach { (name, achieved) ->
                    MilestoneCard(name = name, achieved = achieved)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Back to Home",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = PrimaryBlue
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun MilestoneCard(
    name: String,
    achieved: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) CardBackground else SoftBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
                color = if (achieved) SuccessGreen else TextSecondary
            )

            if (achieved) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp,
                    color = CalmGrey
                )
            }
        }
    }
}
