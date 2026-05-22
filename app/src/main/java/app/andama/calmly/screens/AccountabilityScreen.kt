package app.andama.calmly.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.andama.calmly.ui.theme.*

@Composable
fun AccountabilityScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var whyText by remember { mutableStateOf("") }
    var hasAnswered by remember { mutableStateOf(false) }

    val questions = listOf(
        "Who would be disappointed if you relapsed right now?",
        "What were you doing right before the urge hit?",
        "What will you feel in 5 minutes if you give in?"
    )
    var currentQuestion by remember { mutableIntStateOf(0) }

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
                text = "Be honest with yourself.",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = questions[currentQuestion],
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = WarningAmber,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                )
            ) {
                TextField(
                    value = whyText,
                    onValueChange = {
                        whyText = it
                        hasAnswered = it.length >= 10
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedIndicatorColor = DangerRed,
                        unfocusedIndicatorColor = CalmGrey,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    placeholder = {
                        Text(
                            text = "Write your answer. Be brutally honest.",
                            color = TextSecondary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            if (!hasAnswered) {
                Text(
                    text = "Write at least a sentence. No half-assing this.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentQuestion < questions.size - 1) {
                Button(
                    onClick = {
                        currentQuestion++
                        whyText = ""
                        hasAnswered = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = hasAnswered,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        disabledContainerColor = CalmGrey
                    )
                ) {
                    Text(
                        text = "Next Question",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }
            } else {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = hasAnswered,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen,
                        disabledContainerColor = CalmGrey
                    )
                ) {
                    Text(
                        text = "I'm back in control.",
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
