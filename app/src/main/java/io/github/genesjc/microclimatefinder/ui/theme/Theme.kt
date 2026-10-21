package io.github.genesjc.microclimatefinder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFFB8C0DC),
    secondaryContainer = Color(0xFF2B3553),
    onSecondaryContainer = Color(0xFFDDE3F7),
    tertiary = Color(0xFFF0B8C8),
    tertiaryContainer = Color(0xFF4A2A3A),
    onTertiaryContainer = Color(0xFFFFD9E3),
    background = DarkBackground,
    onBackground = Color(0xFFE2E8F0),
    surface = DarkSurface,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = Color(0xFF5B5F7A),
    secondaryContainer = Color(0xFFDFE1F7),
    tertiary = Color(0xFF7D5260),
    tertiaryContainer = Color(0xFFFFD9E3),
    onTertiaryContainer = Color(0xFF31111D),
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0)
)

/**
 * The app's look. The colours are the app's own (no wallpaper-based colours), so it looks the same on
 * every phone. [darkTheme] is the user's choice in the menu, not the system setting.
 */
@Composable
fun CaliforniaClimateFinderTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Light or dark status-bar icons to match
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
