package io.github.genesjc.climatefinder.ui.screens

import io.github.genesjc.climatefinder.R
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color

/** The sky's colours, icon and wording for a live temperature in degrees Fahrenheit (null: no reading). */
internal fun skyThemeFor(tempF: Double?): SkyAtmosphereTheme = when {
    tempF == null -> SkyAtmosphereTheme(
        colors = listOf(
            Color(0xFF475569).copy(alpha = 0.40f),
            Color(0xFF94A3B8).copy(alpha = 0.20f),
            Color(0xFFE2E8F0).copy(alpha = 0.05f)
        ),
        conditionRes = R.string.reading_none,
        icon = null,
        accent = Color(0xFFCBD5E1),
        badgeBg = Color(0xFF1E293B)
    )
    tempF >= 84.0 -> SkyAtmosphereTheme(
        colors = listOf(
            Color(0xFFC2410C).copy(alpha = 0.45f),
            Color(0xFFF59E0B).copy(alpha = 0.22f),
            Color(0xFFFEF08A).copy(alpha = 0.05f)
        ),
        conditionRes = R.string.sky_valley_heat,
        icon = "☀️",
        accent = Color(0xFFF59E0B),
        badgeBg = Color(0xFF451A03)
    )
    tempF >= 74.0 -> SkyAtmosphereTheme(
        colors = listOf(
            Color(0xFF0284C7).copy(alpha = 0.45f),
            Color(0xFF38BDF8).copy(alpha = 0.22f),
            Color(0xFFFEF3C7).copy(alpha = 0.05f)
        ),
        conditionRes = R.string.sky_fair_foothills,
        icon = "🌤️",
        accent = Color(0xFF38BDF8),
        badgeBg = Color(0xFF082F49)
    )
    tempF >= 65.0 -> SkyAtmosphereTheme(
        colors = listOf(
            Color(0xFF0369A1).copy(alpha = 0.45f),
            Color(0xFF06B6D4).copy(alpha = 0.22f),
            Color(0xFF94A3B8).copy(alpha = 0.05f)
        ),
        conditionRes = R.string.sky_temperate_ridge,
        icon = "⛅",
        accent = Color(0xFF06B6D4),
        badgeBg = Color(0xFF164E63)
    )
    else -> SkyAtmosphereTheme(
        colors = listOf(
            Color(0xFF312E81).copy(alpha = 0.50f),
            Color(0xFF6366F1).copy(alpha = 0.25f),
            Color(0xFF38BDF8).copy(alpha = 0.06f)
        ),
        conditionRes = R.string.sky_crisp_alpine,
        icon = "❄️",
        accent = Color(0xFFA78BFA),
        badgeBg = Color(0xFF1E1B4B)
    )
}
