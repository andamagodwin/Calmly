package app.andama.calmly.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CalmlyColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = DeepBackground,
    primaryContainer = ElevatedBackground,
    onPrimaryContainer = PrimaryBlue,
    secondary = SoftPurple,
    onSecondary = DeepBackground,
    tertiary = AccentGradientEnd,
    onTertiary = DeepBackground,
    background = DeepBackground,
    onBackground = TextPrimary,
    surface = SoftBackground,
    onSurface = TextPrimary,
    surfaceVariant = CardBackground,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = ElevatedBackground,
    error = DangerRed,
    onError = TextPrimary,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle
)

val CalmlyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CalmlyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CalmlyColorScheme,
        typography = Typography,
        shapes = CalmlyShapes,
        content = content
    )
}
