package io.github.genesjc.microclimatefinder.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The backdrop behind every screen: a soft gradient with a faint glow and a light dot grid, so the
 * page is not one flat colour. Deep slate with an indigo glow in dark mode; in light mode a warm coral-yellow-coral wash: coral in the bottom left and top right corners, yellow across the middle.
 */
@Composable
fun AppBackground(darkTheme: Boolean, regionId: String? = null, content: @Composable () -> Unit) {
    // In light mode the two corner colours follow the selected region, easing over when the region changes
    val corner by animateColorAsState(
        targetValue = regionBackdropTint(regionId),
        animationSpec = tween(700),
        label = "backdropTint"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val base = if (darkTheme) {
                    Brush.verticalGradient(listOf(Color(0xFF0A101E), Color(0xFF0F172A), Color(0xFF111B33)))
                } else {
                    Brush.linearGradient(
                        // Warm, so it stands apart from the blue widgets: the same coral in the bottom left and the
                        // top right corners (a soft tint of the selected region's colour), fading to a pale cream-yellow across the middle
                        listOf(corner, androidx.compose.ui.graphics.lerp(corner, Color(0xFFFFF3CF), 0.8f), corner),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f)
                    )
                }
                val glowRadius = size.width * 1.0f
                val glowA = Brush.radialGradient(
                    listOf((if (darkTheme) Color(0xFF4F46E5) else corner).copy(alpha = if (darkTheme) 0.30f else 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.20f),
                    radius = glowRadius
                )
                val glowB = Brush.radialGradient(
                    listOf((if (darkTheme) Color(0xFF0EA5E9) else corner).copy(alpha = if (darkTheme) 0.12f else 0.25f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.85f),
                    radius = glowRadius
                )
                val dotColor = (if (darkTheme) Color.White else Color(0xFF475569)).copy(alpha = if (darkTheme) 0.06f else 0.10f)
                val step = 18.dp.toPx()
                val dot = 1.1.dp.toPx()
                onDrawBehind {
                    drawRect(base)
                    drawRect(glowA)
                    drawRect(glowB)
                    var y = step / 2f
                    while (y < size.height) {
                        var x = step / 2f
                        while (x < size.width) {
                            drawCircle(dotColor, radius = dot, center = Offset(x, y))
                            x += step
                        }
                        y += step
                    }
                }
            },
        content = { content() }
    )
}

private val DEFAULT_CORNER = Color(0xFFF3A583)

/**
 * A soft tint of the region's picker colour for the backdrop corners: the muted earth tone, half-way to white,
 * so every region has a gentle, similar lightness.
 */
private fun regionBackdropTint(regionId: String?): Color {
    val hex = CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == regionId }?.colorHex
        ?: return DEFAULT_CORNER
    return androidx.compose.ui.graphics.lerp(Color(android.graphics.Color.parseColor(hex)), Color.White, 0.52f)
}
