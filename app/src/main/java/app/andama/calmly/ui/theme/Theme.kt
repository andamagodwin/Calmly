package app.andama.calmly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CalmlyColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SoftPurple,
    tertiary = AccentGradientEnd,
    background = DeepBackground,
    surface = SoftBackground,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CalmlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CalmlyColorScheme,
        typography = Typography,
        content = content
    )
}