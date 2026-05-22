package app.andama.calmly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

@Composable
fun AchievementsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val achievementManager = remember { AchievementManager(context) }
    val scope = rememberCoroutineScope()
    
    var achievementData by remember { mutableStateOf<AchievementManager.AchievementData?>(null) }
    var encouragement by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        achievementManager.achievementData.collect { data ->
            achievementData = data
        }
        encouragement = achievementManager.getEncouragementMessage()
    }
    
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
                text = "Your Journey",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            if (achievementData != null) {
                // Stats cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Sessions",
                        value = achievementData!!.totalSessions.toString()
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Streak",
                        value = "${achievementData!!.currentStreak} days"
                    )
                }
                
                // Encouragement message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
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
                
                // Milestones
                Text(
                    text = "Milestones",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                
                val milestones = listOf(
                    Pair("10 Sessions", achievementData!!.milestones["10_sessions"] ?: false),
                    Pair("50 Sessions", achievementData!!.milestones["50_sessions"] ?: false),
                    Pair("100 Sessions", achievementData!!.milestones["100_sessions"] ?: false),
                    Pair("7 Day Streak", achievementData!!.milestones["7_days_streak"] ?: false),
                    Pair("30 Day Streak", achievementData!!.milestones["30_days_streak"] ?: false)
                )
                
                milestones.forEach { milestone ->
                    MilestoneCard(
                        name = milestone.first,
                        achieved = milestone.second
                    )
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
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier.height(100.dp),
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
                color = PrimaryBlue
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
            .height(60.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) CardBackground else SoftBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 18.sp,
                fontWeight = if (achieved) FontWeight.Medium else FontWeight.Normal,
                color = if (achieved) PrimaryBlue else TextSecondary
            )
            
            if (achieved) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 24.sp,
                    color = PrimaryBlue
                )
            }
        }
    }
}
