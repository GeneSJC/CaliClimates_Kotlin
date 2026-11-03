package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Small colored dot + label used in terrain-type legend rows
 * (Sacramento Topographical Map, Elevation Profile).
 */
@Composable
fun LegendItem(color: Color, label: String, dotScale: Float = 1f, dotSlot: Dp = 8.dp, ring: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // The dot follows the Dot size setting, so the legend shows what the slider does to the map's dots. It sits
        // centred in a fixed slot (as big as the biggest dot), so the label never moves as the dot grows
        Box(modifier = Modifier.size(dotSlot), contentAlignment = Alignment.Center) {
            Surface(
                shape = CircleShape,
                // A ring (no reading) is a white centre with the colour round it, like the map's hollow dots
                color = if (ring) Color.White else color,
                border = if (ring) androidx.compose.foundation.BorderStroke(2.dp, color) else null,
                modifier = Modifier.size(8.dp * dotScale)
            ) {}
        }
        Text(
            text = if (dotSlot > 8.dp) label else " $label",
            // Beside the fixed slot: a little room so even the biggest dot never touches the label
            modifier = if (dotSlot > 8.dp) Modifier.padding(start = 4.dp) else Modifier,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
