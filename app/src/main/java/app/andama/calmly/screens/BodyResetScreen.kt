package app.andama.calmly.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.R
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun BodyResetScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var selectedExercise by remember { mutableStateOf<String?>(null) }
    var exerciseStarted by remember { mutableStateOf(false) }
    var exerciseTimer by remember { mutableIntStateOf(0) }
    var exerciseDone by remember { mutableStateOf(false) }

    val exercises = listOf(
        "30 push-ups" to 60,
        "50 jumping jacks" to 45,
        "Cold shower (2 min)" to 120,
        "Run in place (hard)" to 90,
        "Hold plank" to 60
    )

    if (exerciseStarted && !exerciseDone) {
        LaunchedEffect(exerciseStarted) {
            val duration = exercises.find { it.first == selectedExercise }?.second ?: 60
            exerciseTimer = duration
            while (exerciseTimer > 0) {
                delay(1000L)
                exerciseTimer--
            }
            exerciseDone = true
        }
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
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EnterBounce {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.mascot_flex),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Burn it off.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Your body is flooded with adrenaline.\nPick something and DO IT. No half-reps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!exerciseStarted) {
                exercises.forEach { (exercise, _) ->
                    val isSelected = selectedExercise == exercise
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) CardBackground else SoftBackground
                        ),
                        onClick = { selectedExercise = exercise }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = exercise,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) BrutalOrange else TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { exerciseStarted = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = selectedExercise != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrutalOrange,
                        disabledContainerColor = CalmGrey
                    )
                ) {
                    Text(
                        text = "START NOW",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            } else if (!exerciseDone) {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = selectedExercise ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrutalOrange,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = String.format("%02d:%02d", exerciseTimer / 60, exerciseTimer % 60),
                    style = MaterialTheme.typography.displayLarge,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = PrimaryBlue,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "DON'T STOP. KEEP GOING.",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Done.",
                    style = MaterialTheme.typography.displayMedium,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Feel that? That's your body reclaiming control.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Text(
                        text = "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
