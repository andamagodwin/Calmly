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
import app.andama.calmly.ui.components.CalmlyButton
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.theme.*

@Composable
fun ThoughtDumpScreen(
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var thoughtText by remember { mutableStateOf("") }
    var hasWrittenEnough by remember { mutableStateOf(false) }

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
                        painter = painterResource(R.drawable.mascot_writing),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Get it out of your head.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Write what triggered you. What you're feeling.\nThis never gets stored or sent anywhere.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                )
            ) {
                TextField(
                    value = thoughtText,
                    onValueChange = {
                        thoughtText = it
                        hasWrittenEnough = it.length >= 20
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = CalmGrey,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    placeholder = {
                        Text(
                            text = "What's going on in your head right now?",
                            color = TextSecondary
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge
                )
            }

            if (!hasWrittenEnough) {
                Text(
                    text = "Write something real. Don't skip this.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                    color = CalmGrey,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            CalmlyButton(
                text = "Done. Move on.",
                enabled = hasWrittenEnough,
                onClick = onNext
            )
        }
    }
}
