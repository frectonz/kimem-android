package et.frectonz.kimem.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import et.frectonz.kimem.core.model.ThemeMode

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

private fun monoScheme(ink: Color, paper: Color) = lightColorScheme(
    primary = ink,
    onPrimary = paper,
    primaryContainer = ink,
    onPrimaryContainer = paper,
    inversePrimary = paper,
    secondary = ink,
    onSecondary = paper,
    secondaryContainer = paper,
    onSecondaryContainer = ink,
    tertiary = ink,
    onTertiary = paper,
    tertiaryContainer = paper,
    onTertiaryContainer = ink,
    background = paper,
    onBackground = ink,
    surface = paper,
    onSurface = ink,
    surfaceVariant = paper,
    onSurfaceVariant = ink,
    surfaceTint = paper,
    inverseSurface = ink,
    inverseOnSurface = paper,
    error = ink,
    onError = paper,
    errorContainer = paper,
    onErrorContainer = ink,
    outline = ink,
    outlineVariant = ink,
    scrim = ink,
    surfaceBright = paper,
    surfaceDim = paper,
    surfaceContainer = paper,
    surfaceContainerHigh = paper,
    surfaceContainerHighest = paper,
    surfaceContainerLow = paper,
    surfaceContainerLowest = paper,
)

private val LightScheme = monoScheme(ink = Black, paper = White)
private val DarkScheme = monoScheme(ink = White, paper = Black)

private val MonoShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun KimemTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val selection = TextSelectionColors(
        handleColor = scheme.onBackground,
        backgroundColor = scheme.onBackground.copy(alpha = 0.25f),
    )

    MaterialTheme(colorScheme = scheme, typography = KimemTypography, shapes = MonoShapes) {
        CompositionLocalProvider(LocalTextSelectionColors provides selection, content = content)
    }
}
