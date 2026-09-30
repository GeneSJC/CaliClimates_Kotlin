package io.github.genesjc.microclimatefinder.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The backdrop behind every screen: a soft gradient with a faint glow and a light dot grid, so the
 * page is not one flat colour. Deep slate with an indigo glow in dark mode, lavender in light mode.
 */
@Composable
fun AppBackground(darkTheme: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                val base = if (darkTheme) {
                    Brush.verticalGradient(listOf(Color(0xFF0A101E), Color(0xFF0F172A), Color(0xFF111B33)))
                } else {
                    Brush.linearGradient(
                        listOf(Color(0xFFEEF1FF), Color(0xFFF9F6FE), Color(0xFFFCF6FF)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    )
                }
                val glowRadius = size.width * 1.0f
                val glowA = Brush.radialGradient(
                    listOf((if (darkTheme) Color(0xFF4F46E5) else Color(0xFF93A5F5)).copy(alpha = if (darkTheme) 0.30f else 0.22f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.20f),
                    radius = glowRadius
                )
                val glowB = Brush.radialGradient(
                    listOf((if (darkTheme) Color(0xFF0EA5E9) else Color(0xFFD8B4FE)).copy(alpha = if (darkTheme) 0.12f else 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.05f, size.height * 0.85f),
                    radius = glowRadius
                )
                val dotColor = (if (darkTheme) Color.White else Color(0xFF475569)).copy(alpha = if (darkTheme) 0.06f else 0.07f)
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
