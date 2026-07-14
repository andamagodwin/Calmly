package app.andama.calmly.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.data.ScreenTimeInsights
import app.andama.calmly.data.ScreenTimeMonitor
import app.andama.calmly.ui.components.*
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PatternsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val monitor = remember { ScreenTimeMonitor(context) }
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(monitor.hasPermission()) }
    var insights by remember { mutableStateOf<ScreenTimeInsights.Insights?>(null) }
    var lateApps by remember { mutableStateOf<List<ScreenTimeMonitor.AppUsage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var windowApplied by remember { mutableStateOf(false) }

    // Usage access is granted in system settings, so the user leaves the app and
    // comes back. Re-check on resume, otherwise they'd return to a screen still
    // insisting the permission is missing.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = monitor.hasPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        withContext(Dispatchers.IO) {
            val days = monitor.readRecentDays()
            insights = ScreenTimeInsights.analyze(days, tracker.getRelapseDates())
            // What actually held their attention last night, from 11pm onwards.
            lateApps = monitor.appUsageSince(
                monitor.windowStartMillis(ScreenTimeMonitor.LATE_NIGHT_START),
                limit = 4
            )
        }
        loading = false
    }

    CalmlyScreen(title = "Your Patterns", onBack = onBack) {
        when {
            !monitor.isSupported() -> UnsupportedState()
            !hasPermission -> PermissionGate(
                onGrant = { context.startActivity(monitor.permissionSettingsIntent()) }
            )
            loading -> LoadingState()
            insights == null -> EmptyState()
            else -> InsightsContent(
                insights = insights!!,
                lateApps = lateApps,
                windowApplied = windowApplied,
                onApplyWindow = { window ->
                    scope.launch {
                        tracker.setDangerHours(window.first, window.second, enabled = true)
                        windowApplied = true
                    }
                }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ColumnScope.UnsupportedState() {
    Spacer(Modifier.height(40.dp))
    CalmlyCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Not available on this Android version",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Screen-time patterns need Android 9 or newer.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ColumnScope.LoadingState() {
    Spacer(Modifier.height(80.dp))
    Text(
        text = "Reading the last two weeks…",
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )
}

@Composable
private fun ColumnScope.EmptyState() {
    Spacer(Modifier.height(40.dp))
    CalmlyCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = "Not enough data yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Give it a few days. Cal needs to watch a little longer before saying anything useful.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

/**
 * The permission ask. This is a porn-recovery app requesting access to phone
 * usage — if that ask isn't completely straight about what it does and doesn't
 * read, the user is right to refuse it.
 */
@Composable
private fun ColumnScope.PermissionGate(onGrant: () -> Unit) {
    Spacer(Modifier.height(20.dp))

    EnterBounce {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.mascot_writing),
                contentDescription = null,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Let Cal spot your pattern",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Late-night phone use is the clearest warning sign there is. " +
                        "If Calmly can see when your screen is on, it can tell you when " +
                        "you're heading for trouble — before you get there.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    EnterBounce(delayMillis = 120) {
        CalmlyCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = SoftBackground
        ) {
            Column(Modifier.padding(18.dp)) {
                SectionLabel("What Calmly reads")
                Spacer(Modifier.height(12.dp))
                PrivacyLine("✓", "When your screen turns on and off", PrimaryBlue)
                PrivacyLine("✓", "How many times you unlock your phone", PrimaryBlue)
                PrivacyLine("✓", "Which apps held your attention, and for how long", PrimaryBlue)
                Spacer(Modifier.height(12.dp))
                SectionLabel("What it never reads")
                Spacer(Modifier.height(12.dp))
                PrivacyLine("✗", "Anything you type, watch, or browse", DangerRed)
                PrivacyLine("✗", "Message or media content of any kind", DangerRed)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "All of it stays on this phone. Calmly has no servers and no account — " +
                            "there is nowhere for it to be sent, even by accident.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    CalmlyButton(
        text = "Turn on usage access",
        onClick = onGrant
    )

    Spacer(Modifier.height(10.dp))

    Text(
        text = "Android will open its settings — find Calmly in the list and flip it on.",
        style = MaterialTheme.typography.bodySmall,
        color = TextTertiary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PrivacyLine(glyph: String, text: String, accent: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = glyph,
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun ColumnScope.InsightsContent(
    insights: ScreenTimeInsights.Insights,
    lateApps: List<ScreenTimeMonitor.AppUsage>,
    windowApplied: Boolean,
    onApplyWindow: (Pair<Int, Int>) -> Unit
) {
    Spacer(Modifier.height(8.dp))

    // Hero: last night, the number that matters most.
    EnterBounce {
        CalmlyCard(
            modifier = Modifier.fillMaxWidth(),
            containerColor = CardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                SectionLabel("Last night, 11pm — 5am")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ScreenTimeInsights.formatMinutes(insights.lastNightLateMinutes),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    color = lateNightColor(insights.lastNightLateMinutes)
                )
                Text(
                    text = lateNightVerdict(
                        insights.lastNightLateMinutes,
                        insights.avgLateNightMinutes
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }

    // The answer to the obvious next question: kept me up doing *what*?
    if (lateApps.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        SectionLabel("What kept you up")
        Spacer(Modifier.height(12.dp))

        EnterBounce(delayMillis = 40) {
            CalmlyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SoftBackground
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    val worst = lateApps.first().minutes.coerceAtLeast(1)
                    lateApps.forEach { app ->
                        AppUsageRow(app = app, worstMinutes = worst)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    EnterBounce(delayMillis = 70) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatTile(
                modifier = Modifier.weight(1f),
                value = ScreenTimeInsights.formatMinutes(insights.avgDailyMinutes),
                label = "daily screen time",
                accent = SoftPurple
            )
            StatTile(
                modifier = Modifier.weight(1f),
                value = "${insights.todayUnlocks}",
                label = "unlocks today",
                accent = if (insights.isRestlessToday) WarningAmber else PrimaryBlue
            )
        }
    }

    // Restlessness — a spike in phone-checking is a recognised pre-relapse state.
    if (insights.isRestlessToday) {
        Spacer(Modifier.height(12.dp))
        EnterBounce(delayMillis = 110) {
            CalmlyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WarningAmber.copy(alpha = 0.14f)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "You're restless today",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "You've picked up your phone ${insights.restlessnessDelta}% more than usual. " +
                                "That itch is worth naming before it names you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    SectionLabel("Late nights, last ${insights.daysAnalyzed} days")
    Spacer(Modifier.height(12.dp))

    EnterBounce(delayMillis = 150) {
        LateNightChart(insights.lateNightTrend)
    }

    // The payload: the pattern behind their own relapses.
    insights.correlation?.let { correlation ->
        Spacer(Modifier.height(24.dp))
        EnterBounce(delayMillis = 190) {
            CalmlyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = DangerRed.copy(alpha = 0.14f)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        text = "Cal noticed something",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DangerRed
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your last ${correlation.relapseNightsSampled} resets each followed a night " +
                                "with about ${ScreenTimeInsights.formatMinutes(correlation.relapseNightAvgMinutes)} " +
                                "of late-night screen time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Your clean nights average ${ScreenTimeInsights.formatMinutes(correlation.cleanNightAvgMinutes)}. " +
                                "The phone isn't the enemy — but for you, this is where it starts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }

    // Turn the observation into one tap of action.
    insights.suggestedWindow?.let { window ->
        Spacer(Modifier.height(24.dp))
        EnterBounce(delayMillis = 230) {
            CalmlyCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = SoftBackground
            ) {
                Column(Modifier.padding(18.dp)) {
                    SectionLabel("Suggested danger window")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = ScreenTimeInsights.formatWindow(window),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrutalOrange
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "This is when you're actually awake and on your phone — " +
                                "not a guess, your real data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    if (windowApplied) {
                        Text(
                            text = "✓ Set as your danger window",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen
                        )
                    } else {
                        CalmlyButton(
                            text = "Use this as my danger window",
                            containerColor = BrutalOrange,
                            onClick = { onApplyWindow(window) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One app's share of the night: real launcher icon, name, minutes, and a bar
 * scaled against the worst offender. Seeing the actual app that ate 40 minutes
 * of your 1am is harder to rationalise away than an abstract screen-time number.
 */
@Composable
private fun AppUsageRow(
    app: ScreenTimeMonitor.AppUsage,
    worstMinutes: Int
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }

    val fraction by animateFloatAsState(
        targetValue = (app.minutes.toFloat() / worstMinutes).coerceIn(0.04f, 1f),
        animationSpec = spring(dampingRatio = 0.75f),
        label = "app-${app.packageName}"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            androidx.compose.foundation.Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(ElevatedBackground)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = ScreenTimeInsights.formatMinutes(app.minutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ElevatedBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(listOf(SoftPurple, BrutalOrange))
                        )
                )
            }
        }
    }
}

/** Simple bar chart of late-night minutes; today is the rightmost, brightest bar. */
@Composable
private fun LateNightChart(trend: List<Int>) {
    val peak = (trend.maxOrNull() ?: 0).coerceAtLeast(1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        trend.forEachIndexed { index, minutes ->
            val isToday = index == trend.lastIndex
            val fraction by animateFloatAsState(
                targetValue = (minutes.toFloat() / peak).coerceIn(0.02f, 1f),
                animationSpec = spring(dampingRatio = 0.7f),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isToday) {
                                listOf(BrutalOrange, DangerRed)
                            } else {
                                listOf(SoftPurple.copy(alpha = 0.85f), AccentGradientEnd.copy(alpha = 0.5f))
                            }
                        )
                    )
            )
        }
    }
}

private fun lateNightColor(minutes: Int) = when {
    minutes >= 90 -> DangerRed
    minutes >= 30 -> WarningAmber
    else -> SuccessGreen
}

private fun lateNightVerdict(lastNight: Int, average: Int): String = when {
    lastNight >= 90 -> "That's a long night. Those are the ones that cost you."
    lastNight >= 30 && lastNight > average -> "Heavier than your usual night. Worth watching."
    lastNight >= 30 -> "About normal for you — but normal isn't the goal."
    lastNight > 0 -> "Barely touched it. That's how the streak survives."
    else -> "Phone stayed down. That's a clean night."
}
