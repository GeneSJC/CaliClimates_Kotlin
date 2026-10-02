package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The small button that tucks the vertical zoom slider away and brings it back. Its chevron points up while the
 * slider is [shown] (tap to hide it) and down when it is hidden (tap to show it).
 */
@Composable
fun SliderToggleButton(shown: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .size(32.dp)
            .clickable(
                onClickLabel = if (shown) "Hide the zoom slider" else "Show the zoom slider",
                role = Role.Button,
                onClick = onToggle
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 11.dp)) {
            val stroke = 2.2.dp.toPx()
            val w = size.width
            val h = size.height
            val ink = Color(0xFF0284C7)
            val tipY = if (shown) 0f else h
            val baseY = if (shown) h else 0f
            drawLine(ink, Offset(0f, baseY), Offset(w / 2f, tipY), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(ink, Offset(w, baseY), Offset(w / 2f, tipY), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}
