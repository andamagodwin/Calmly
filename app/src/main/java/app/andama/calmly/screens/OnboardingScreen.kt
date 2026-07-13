package app.andama.calmly.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.andama.calmly.R
import app.andama.calmly.data.CalmlyTracker
import app.andama.calmly.ui.components.CalmlyButton
import app.andama.calmly.ui.components.CalmlyScreen
import app.andama.calmly.ui.components.EnterBounce
import app.andama.calmly.ui.theme.*
import kotlinx.coroutines.launch

/**
 * First-launch screen: Cal introduces itself and asks for a name. The name
 * personalises greetings, the lock overlay, and every notification.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember { CalmlyTracker(context) }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    val validName = name.trim().length >= 2

    CalmlyScreen(scrollable = true) {
        Spacer(Modifier.weight(1f))

        EnterBounce {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(R.drawable.mascot_idle),
                    contentDescription = "Cal the axolotl waving hello",
                    modifier = Modifier.size(180.dp)
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Hey. I'm Cal.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Axolotls regrow what they lose.\nSo will you. I'm here for every round of it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        EnterBounce(delayMillis = 150) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "What should I call you?",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )

                Spacer(Modifier.height(12.dp))

                TextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Your name", color = TextTertiary) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CardBackground,
                        unfocusedContainerColor = CardBackground,
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = CardBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )

                Spacer(Modifier.height(20.dp))

                CalmlyButton(
                    text = if (validName) "Let's go, ${name.trim()}" else "Let's go",
                    enabled = validName,
                    onClick = {
                        scope.launch {
                            tracker.setUserName(name)
                            onDone()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.weight(1.4f))
    }
}
