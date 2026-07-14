package app.andama.calmly.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.data.CalMood
import app.andama.calmly.data.CalVoice
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.accentArgb
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.faceRes
import app.andama.calmly.ui.label
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun DailyCheckinScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()

    var selectedMood by remember { mutableStateOf<CalMood?>(null) }
    var note by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var alreadyCheckedIn by remember { mutableStateOf(false) }
    var cleanDays by remember { mutableIntStateOf(0) }
    var userName by remember { mutableStateOf<String?>(null) }
    var loggedMood by remember { mutableStateOf<CalMood?>(null) }

    LaunchedEffect(Unit) {
        alreadyCheckedIn = tracker.hasCheckedInToday()
        cleanDays = tracker.getCleanDays()
        userName = tracker.getUserName()
        if (alreadyCheckedIn) loggedMood = tracker.getLatestMood()
        tracker.startCleanStreak()
    }

    val who = userName ?: "soldier"
    val done = submitted || alreadyCheckedIn

    // The face at the top of the screen *is* the answer to "how are you?" — it
    // mirrors the option under the user's finger before they've committed to it.
    val headerMood = if (done) loggedMood ?: CalMood.NEUTRAL else selectedMood ?: CalMood.NEUTRAL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EnterBounce {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CalFaceCrossfade(mood = headerMood, size = 108.dp)

                Spacer(Modifier.height(12.dp))

                Text(
                    text = if (done) "Logged for today" else "How are you, $who?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "day $cleanDays clean",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        if (done) {
            val mood = loggedMood
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (mood != null) "You said: ${mood.label.lowercase()}" else "You've checked in today.",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = mood?.let { CalVoice.moodReply(it, who) }
                            ?: "That's today handled. Come back tomorrow.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }
        } else {
            MoodScale(
                selected = selectedMood,
                onSelect = { selectedMood = it }
            )

            Spacer(Modifier.height(16.dp))

            // Cal answers the instant you pick, before you've typed a word — the
            // point of a check-in is being met, not filling in a form.
            AnimatedVisibility(
                visible = selectedMood != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Text(
                        text = selectedMood?.let { CalVoice.moodReply(it, who) } ?: "",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 15.sp,
                        color = TextPrimary,
                        lineHeight = 21.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftBackground)
            ) {
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftBackground,
                        unfocusedContainerColor = SoftBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    placeholder = { Text("Anything on your mind? (optional)", color = CalmGrey) },
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(20.dp))

            val accent = selectedMood?.let { Color(it.accentArgb) } ?: CalmGrey
            Button(
                onClick = {
                    val mood = selectedMood ?: return@Button
                    scope.launch {
                        tracker.logMood(mood.level, note)
                        loggedMood = mood
                        submitted = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedMood != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    disabledContainerColor = CalmGrey.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = "Check In",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back to Home", color = TextSecondary)
        }
    }
}

/**
 * The 1-5 scale, as Cal's own faces. Numbered circles asked people to translate
 * a feeling into a digit; a face is the feeling.
 */
@Composable
private fun MoodScale(
    selected: CalMood?,
    onSelect: (CalMood) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        CalMood.entries.forEach { mood ->
            val isSelected = selected == mood
            val accent = Color(mood.accentArgb)

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.18f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "moodScale"
            )
            val fade by animateFloatAsState(
                targetValue = if (selected == null || isSelected) 1f else 0.4f,
                label = "moodFade"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(mood) }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .scale(scale)
                        .alpha(fade),
                    contentAlignment = Alignment.Center
                ) {
                    // A halo rather than a border — the selected face should look
                    // lit from behind, not fenced in.
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.22f))
                        )
                    }
                    Image(
                        painter = painterResource(mood.faceRes),
                        contentDescription = mood.label,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = mood.label,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accent else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Cal's face, springing whenever it changes. */
@Composable
private fun CalFaceCrossfade(mood: CalMood, size: androidx.compose.ui.unit.Dp) {
    val pop = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(mood) {
        pop.snapTo(0.86f)
        pop.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }
    Image(
        painter = painterResource(mood.faceRes),
        contentDescription = "Cal looking ${mood.label.lowercase()}",
        modifier = Modifier
            .size(size)
            .scale(pop.value)
    )
}
