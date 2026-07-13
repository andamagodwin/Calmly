package app.andama.calmly.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.StreakInfo
import app.andama.calmly.ui.components.*
import app.andama.calmly.ui.theme.*
import java.util.Calendar

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
    var streak by remember { mutableStateOf<StreakInfo?>(null) }
    var inDangerHours by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        tracker.startCleanStreak()
        streak = tracker.getStreakInfo()
        inDangerHours = tracker.isInDangerHours()
        userName = tracker.getUserName()
    }

    val days = streak?.days ?: 0
    val ringColor = streakColor(days)

    CalmlyScreen(scrollable = true) {
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = userName ?: "Calmly",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (streak?.checkedInToday == true) {
                    Text(
                        text = "✓ checked in",
                        style = MaterialTheme.typography.labelMedium,
                        color = SuccessGreen
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Image(
                    painter = painterResource(R.drawable.mascot_idle),
                    contentDescription = "Cal the axolotl waving",
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        EnterBounce(delayMillis = 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            StreakRing(
                days = days,
                hours = streak?.hours ?: 0,
                progress = streak?.milestoneProgress ?: 0f,
                nextMilestone = streak?.nextMilestone,
                ringColor = ringColor
            )
        }

        Spacer(Modifier.height(24.dp))

        EnterBounce(delayMillis = 70) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    modifier = Modifier.weight(1f),
                    value = "${streak?.longest ?: 0}",
                    label = "best streak",
                    accent = SoftPurple
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    value = "${streak?.totalRelapses ?: 0}",
                    label = "resets",
                    accent = TextSecondary
                )
                StatTile(
                    modifier = Modifier.weight(1f),
                    value = if (streak?.checkedInToday == true) "Yes" else "No",
                    label = "checked in",
                    accent = if (streak?.checkedInToday == true) SuccessGreen else WarningAmber
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel("Right now")
        Spacer(Modifier.height(12.dp))

        // The user told us this window is risky for them. Surface it and offer the
        // lock — rather than forcing an opaque overlay over the app they just opened
        // for help, which is what the old auto-lock did.
        if (inDangerHours) {
            CalmlyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = BrutalOrange.copy(alpha = 0.14f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "You're in your danger window",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrutalOrange
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "This is the hour that usually wins. Strike first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    CalmlyButton(
                        text = "Lock me down now",
                        containerColor = BrutalOrange,
                        onClick = onUrgeClick
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        EnterBounce(delayMillis = 140) {
            Column {
                GradientActionButton(
                    title = "I feel overwhelmed",
                    subtitle = "Slow it down for 10 minutes",
                    gradient = listOf(AccentGradientStart, AccentGradientEnd),
                    onClick = onOverwhelmClick
                )

                Spacer(Modifier.height(12.dp))

                GradientActionButton(
                    title = "I'm having an urge",
                    subtitle = "Lock me down now",
                    gradient = listOf(DangerRed, BrutalOrange),
                    onClick = onUrgeClick
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        SectionLabel("Your tools")
        Spacer(Modifier.height(12.dp))

        EnterBounce(delayMillis = 210) {
            Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_checkin,
                title = "Check In",
                subtitle = "Daily mood",
                onClick = onDailyCheckinClick
            )
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_triggers,
                title = "Triggers",
                subtitle = "Know patterns",
                onClick = onTriggerTrackerClick
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_dangerhours,
                title = "Danger Hours",
                subtitle = "High-risk window",
                onClick = onDangerHoursClick
            )
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_partner,
                title = "Partner",
                subtitle = "Accountability",
                onClick = onPartnerClick
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_alarm,
                title = "Alarm",
                subtitle = "QR wake-up",
                onClick = onAlarmClick
            )
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_nightreset,
                title = "Night Reset",
                subtitle = "Sleep routine",
                onClick = onNightResetClick
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_achievements,
                title = "Achievements",
                subtitle = "How far you've come",
                onClick = onAchievementsClick
            )
            ToolTile(
                modifier = Modifier.weight(1f),
                iconRes = R.drawable.ic_resetlog,
                title = "Reset Log",
                subtitle = "Learn, don't shame",
                onClick = onRelapseClick
            )
        }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

/** Ring colour tiers by milestone so progress is legible at a glance. */
private fun streakColor(days: Int): Color = when {
    days >= 30 -> StreakForged
    days >= 7 -> StreakRooted
    days >= 3 -> StreakSprout
    else -> StreakSeed
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "You're up late"
}
