package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import io.github.genesjc.climatefinder.domain.DepthCue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.ui.components.ms

/**
 * One station's tag on the Elevation chart: its name and height in a small rounded box at the spot the label placement
 * chose, with a stripe down the left edge in its band's colour (see the key at the bottom right). A far station's text
 * is fainter ([depth]) but its tag stays solid. [fade] is how visible it is (labels fade in and out as room changes).
 */
@Composable
internal fun ElevationStationTag(
    label: PlacedLabel,
    isSelected: Boolean,
    fade: Float,
    depth: Float?,
    stripe: Color,
    onSelect: () -> Unit
) {
    // A far station's text is fainter, but its tag stays solid
    val depthFade = depth?.let { DepthCue.alpha(it) } ?: 1f

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            // Nearly solid, so the ridges and ground behind never show through the tag
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f * depthFade)
        ),
        modifier = Modifier
            .offset(x = label.xDp.dp, y = label.yDp.dp)
            .widthIn(max = LABEL_MAX_WIDTH_DP.dp)
            .alpha(fade)
            .clickable(onClick = onSelect)
    ) {
        Text(
            text = label.text,
            maxLines = 2,
            fontSize = ms(9f * (depth?.let { DepthCue.textScale(it) } ?: 1f)),
            // A tight line height (the theme's default is far taller than this small text), so a wrapped tag stays compact
            lineHeight = ms(9f * (depth?.let { DepthCue.textScale(it) } ?: 1f) * 1.15f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = depthFade),
            // A stripe down the tag's left edge in its band's colour (see the key at the bottom right)
            modifier = Modifier
                .drawBehind { drawRect(stripe, size = Size(6.dp.toPx(), size.height)) }
                .padding(start = 11.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        )
    }
}
