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
import app.andama.calmly.data.Cal
import app.andama.calmly.data.CalState
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.ScreenTimeInsights
import app.andama.calmly.data.ScreenTimeMonitor
import app.andama.calmly.data.StreakInfo
import app.andama.calmly.ui.faceRes
import app.andama.calmly.ui.label
import app.andama.calmly.ui.components.*
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    onAchievementsClick: () -> Unit,
    onPatternsClick: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val screenTime = remember { ScreenTimeMonitor(context) }
    var streak by remember { mutableStateOf<StreakInfo?>(null) }
    var inDangerHours by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf<String?>(null) }
    var insights by remember { mutableStateOf<ScreenTimeInsights.Insights?>(null) }
    var screenTimeEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        tracker.startCleanStreak()
        streak = tracker.getStreakInfo()
        inDangerHours = tracker.isInDangerHours()
        userName = tracker.getUserName()

        screenTimeEnabled = screenTime.isSupported() && screenTime.hasPermission()
        if (screenTimeEnabled) {
            insights = withContext(Dispatchers.IO) {
                ScreenTimeInsights.analyze(
                    screenTime.readRecentDays(),
                    tracker.getRelapseDates()
                )
            }
        }
    }

    val days = streak?.days ?: 0
    val ringColor = streakColor(days)

    // Cal's face on the home header is a mirror, not decoration: day zero, an open
    // danger window or a restless day all show up here before the user reads a word.
    val calState = CalState(
        cleanDays = days,
        hoursIntoDay = streak?.hours ?: 0,
        checkedInToday = streak?.checkedInToday == true,
        inDangerWindow = inDangerHours,
        escalation = 0,
        isRestless = insights?.isRestlessToday == true
    )
    val calMood = Cal.face(calState)

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
                    painter = painterResource(calMood.faceRes),
                    contentDescription = "Cal the axolotl, looking ${calMood.label.lowercase()}",
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

        // Screen-time signal. Only ever shown when it has something to say —
        // a warning if last night ran long or the user is restless today, and
        // otherwise a quiet nudge to turn the feature on. No noise in between.
        ScreenTimeCard(
            insights = insights,
            enabled = screenTimeEnabled,
            supported = screenTime.isSupported(),
            onClick = onPatternsClick
        )

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

/**
 * The screen-time signal on Home. Deliberately quiet: it only earns its space
 * when it has an actual warning, otherwise it's a one-line offer or nothing.
 * A card that says "everything is fine" every day teaches people to ignore it.
 */
@Composable
private fun ScreenTimeCard(
    insights: ScreenTimeInsights.Insights?,
    enabled: Boolean,
    supported: Boolean,
    onClick: () -> Unit
) {
    if (!supported) return

    if (!enabled) {
        CalmlyCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = SoftBackground,
            onClick = onClick
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Let Cal spot your pattern",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Late nights on your phone are the warning sign. Turn on usage access and Calmly will call them out.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        return
    }

    val data = insights ?: return

    val heavyNight = data.lastNightLateMinutes >= 60
    val restless = data.isRestlessToday
    if (!heavyNight && !restless) return

    val accent = if (heavyNight) DangerRed else WarningAmber
    val title = if (heavyNight) {
        "Long night on the phone"
    } else {
        "You're restless today"
    }
    val body = if (heavyNight) {
        "${ScreenTimeInsights.formatMinutes(data.lastNightLateMinutes)} after 11pm last night. " +
            "That's the pattern that usually costs you — stay close to the tools today."
    } else {
        "You've unlocked your phone ${data.restlessnessDelta}% more than your usual. " +
            "Name the itch before it names you."
    }

    CalmlyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = accent.copy(alpha = 0.14f),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
    Spacer(Modifier.height(12.dp))
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
