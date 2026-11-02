package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

/** The weather pictures in the upper-left of the sky for a live temperature (nothing without a reading), [padding] in from the chart's edges. */
internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWeatherVisuals(tempF: Double?, padding: Float) {
    if (tempF == null) {
        // No live reading: plain sky, no weather visuals
    } else if (tempF >= 84.0) {
        // Valley solar heat corona
        drawCircle(
            color = Color(0xFFFDE047).copy(alpha = 0.25f),
            radius = 34f,
            center = Offset(padding + 48f, padding + 48f)
        )
        drawCircle(
            color = Color(0xFFFEF08A).copy(alpha = 0.85f),
            radius = 16f,
            center = Offset(padding + 48f, padding + 48f)
        )
    } else if (tempF >= 74.0) {
        // Foothill cumulus cloud puffs
        drawCircle(color = Color.White.copy(alpha = 0.55f), radius = 18f, center = Offset(padding + 45f, padding + 48f))
        drawCircle(color = Color.White.copy(alpha = 0.65f), radius = 24f, center = Offset(padding + 65f, padding + 44f))
        drawCircle(color = Color.White.copy(alpha = 0.50f), radius = 16f, center = Offset(padding + 84f, padding + 48f))
    } else if (tempF >= 65.0) {
        // Stratified mountain ridge streaks
        drawLine(
            color = Color(0xFFBAE6FD).copy(alpha = 0.5f),
            start = Offset(padding + 28f, padding + 42f),
            end = Offset(padding + 110f, padding + 42f),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE0F2FE).copy(alpha = 0.4f),
            start = Offset(padding + 48f, padding + 54f),
            end = Offset(padding + 130f, padding + 54f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    } else {
        // Alpine cirrus streaks
        drawLine(
            color = Color(0xFFC7D2FE).copy(alpha = 0.6f),
            start = Offset(padding + 25f, padding + 36f),
            end = Offset(padding + 95f, padding + 36f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFA5B4FC).copy(alpha = 0.5f),
            start = Offset(padding + 45f, padding + 48f),
            end = Offset(padding + 115f, padding + 48f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}
