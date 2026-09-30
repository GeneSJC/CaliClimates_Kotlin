package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.launch
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType

/** How long the legend panel takes to grow out of its header, and to fold back. */
private const val LEGEND_OPEN_MS = 650
private const val LEGEND_CLOSE_MS = 500

/**
 * Colour key for the map dots, built from the region's own stations and kept to one line until
 * opened: "Terrain Legend" with a chevron. Tapping it opens a panel over the content below, which
 * does not move, showing only the terrain types the region actually has, each with its station
 * count. Tap the header, tap outside, or press Back to close. A station is "ridge / highland" when
 * its data says ridge, foothill or mountain, or when it sits above 500 m.
 * [showRelief] adds the green "Mountain relief" entry, used only by the Bay Area map, whose
 * hand-drawn terrain has green relief bands.
 * [extra] is more content shown in the panel, under the key (the Topo Map puts its dot-size
 * slider there). [headerFontSize] sets the header's size; [expandedWidthPx] lets the panel grow
 * wider than the header, for a header that sits in part of a row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerrainLegend(
    stations: List<MicroclimateStation>,
    modifier: Modifier = Modifier,
    showRelief: Boolean = false,
    extra: (@Composable () -> Unit)? = null,
    headerFontSize: TextUnit = 17.6.sp,
    /** When set, the panel grows to this width (Home gives it the full row); else to the header's width. */
    expandedWidthPx: Int = 0
) {
    var open by remember { mutableStateOf(false) }
    var widthPx by remember { mutableStateOf(0) }
    var headerHeightPx by remember { mutableStateOf(0) }
    val reveal = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val panelShape = RoundedCornerShape(14.dp)
    val padX = 12.dp
    // No padding above the header, so the panel starts where the header does and keeps the page's
    // gap to the card above it; a little below the content
    val padBottom = 6.dp
    val padXPx = with(density) { padX.roundToPx() }
    val padBottomPx = with(density) { padBottom.roundToPx() }

    fun openPanel() {
        open = true
        scope.launch { reveal.animateTo(1f, tween(LEGEND_OPEN_MS, easing = FastOutSlowInEasing)) }
    }
    fun closePanel() {
        scope.launch {
            reveal.animateTo(0f, tween(LEGEND_CLOSE_MS, easing = FastOutSlowInEasing))
            open = false
        }
    }

    // The header keeps its place in the layout; the panel floats over what is below it
    Box(modifier = modifier.fillMaxWidth().onSizeChanged { widthPx = it.width }) {
        LegendHeader(
            expanded = false,
            fontSize = headerFontSize,
            modifier = Modifier
                .onSizeChanged { headerHeightPx = it.height }
                .clickable(onClickLabel = "Show the Terrain Legend", role = Role.Button) { openPanel() }
        )
        if (open) {
            val p = reveal.value
            val startWidth = with(density) { widthPx.toDp() } + padX * 2
            val endWidth = if (expandedWidthPx > 0) with(density) { expandedWidthPx.toDp() } + padX * 2 else startWidth
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(-padXPx, 0),
                onDismissRequest = { closePanel() },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .width(lerp(startWidth, endWidth, p))
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val closedHeight = headerHeightPx + padBottomPx
                            val h = (closedHeight + (placeable.height - closedHeight) * p).toInt()
                            layout(placeable.width, h) { placeable.place(0, 0) }
                        }
                        .shadow(10.dp * p, panelShape)
                        .clip(panelShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, panelShape)
                ) {
                    // Laid out at the full width and revealed as the panel grows
                    Box(modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true)) {
                    Column(modifier = Modifier.width(endWidth).padding(start = padX, end = padX, bottom = padBottom)) {
                        LegendHeader(
                            expanded = true,
                            fontSize = headerFontSize,
                            modifier = Modifier.clickable(
                                onClickLabel = "Hide the Terrain Legend",
                                role = Role.Button
                            ) { closePanel() }
                        )
                        Column(modifier = Modifier.graphicsLayer { alpha = ((p - 0.25f) / 0.5f).coerceIn(0f, 1f) }) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(TerrainType.RIDGE, TerrainType.VALLEY_BASIN, TerrainType.COASTAL, TerrainType.URBAN_CANYON)
                                    .forEach { type ->
                                        val count = stations.count { it.terrain == type }
                                        if (count > 0) LegendItem(color = legendColor(type), label = "${legendName(type)} $count")
                                    }
                                if (showRelief) LegendItem(color = Color(0xFF166534), label = "Mountain relief")
                            }
                            Text(
                                text = "Ridge / highland: ridge, foothill or mountain terrain, or above 500 m",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (extra != null) extra()
                        }
                    }
                    }
                }
            }
        }
    }
}

/** "Terrain Legend" with a chevron: the one line the legend shows until opened. */
@Composable
private fun LegendHeader(expanded: Boolean, fontSize: TextUnit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Terrain Legend",
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (expanded) " ▴" else " ▾",
            fontSize = fontSize,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Same colours as the dots on the maps
private fun legendColor(type: TerrainType): Color = when (type) {
    TerrainType.RIDGE -> Color(0xFF7C3AED)
    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
    TerrainType.COASTAL -> Color(0xFF0284C7)
    TerrainType.URBAN_CANYON -> Color(0xFF475569)
}

private fun legendName(type: TerrainType): String = when (type) {
    TerrainType.RIDGE -> "Ridge / highland"
    TerrainType.VALLEY_BASIN -> "Valley basin"
    TerrainType.COASTAL -> "Coast / delta"
    TerrainType.URBAN_CANYON -> "Urban"
}
