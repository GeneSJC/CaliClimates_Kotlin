package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.animation.AnimatedVisibility
import io.github.genesjc.climatefinder.ui.components.ms
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One small white button with a blue glyph, in the style of the map's other controls. */
@Composable
private fun MapControlButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .height(32.dp)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
    ) {
        Box(modifier = Modifier.padding(horizontal = 9.dp), contentAlignment = Alignment.Center) { content() }
    }
}

/** A drawn "i" in a circle, in [color]. */
@Composable
internal fun InfoMark(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        drawCircle(color, radius = size.width / 2f - stroke / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        val cx = size.width / 2f
        drawCircle(color, radius = stroke * 0.6f, center = Offset(cx, size.height * 0.30f))
        drawLine(color, Offset(cx, size.height * 0.45f), Offset(cx, size.height * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/**
 * The Region Info button, in the style of the menu button above it: a drawn "i" in a circle, which becomes an X while the
 * panel is open. Topography only; the panel slides in from the right.
 */
@Composable
internal fun InfoControlButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val blue = Color(0xFF0284C7)
    MapControlButton(
        stringResource(if (open) R.string.region_info_close else R.string.region_info_open),
        onClick,
        modifier
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val stroke = 2.dp.toPx()
            if (open) {
                val inset = size.width * 0.18f
                drawLine(blue, Offset(inset, inset), Offset(size.width - inset, size.height - inset), strokeWidth = stroke * 1.2f, cap = StrokeCap.Round)
                drawLine(blue, Offset(size.width - inset, inset), Offset(inset, size.height - inset), strokeWidth = stroke * 1.2f, cap = StrokeCap.Round)
            } else {
                drawCircle(blue, radius = size.width / 2f - stroke / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                val cx = size.width / 2f
                drawCircle(blue, radius = stroke * 0.6f, center = Offset(cx, size.height * 0.30f))
                drawLine(blue, Offset(cx, size.height * 0.45f), Offset(cx, size.height * 0.74f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

/**
 * The row of small buttons at the map's top left. On Topography: the menu at the left, the north indicator in the
 * middle and the About (i) at the right. On Elevation: the menu, then a Stations drop-down (the north indicator and
 * the About button are not shown there). [stations] is only used by the drop-down.
 */
@Composable
internal fun MapControlRow(
    onElevation: Boolean,
    onMenu: () -> Unit,
    /** Topography only: shows the locate button (the selected station is out of view) and what it does when tapped. */
    showLocate: Boolean = false,
    onLocate: () -> Unit = {},
    stations: List<MicroclimateStation> = emptyList(),
    /** The region's best-known places as station ids, in order: the drop-down lists them first. */
    landmarkIds: List<String> = emptyList(),
    selectedStationId: String? = null,
    onPickStation: (String) -> Unit = {},
    /** Told where the menu button's top edge is on the screen (px), so the menu panel can open level with it. */
    onMenuTop: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val blue = Color(0xFF0284C7)
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        MapControlButton(stringResource(R.string.menu_open), onMenu, Modifier.onGloballyPositioned { onMenuTop(it.positionInRoot().y) }) {
            Text(text = "\u2630", fontSize = 18.sp, color = blue)
        }
        if (onElevation) {
            StationsDropdown(stations, landmarkIds, selectedStationId, onPickStation)
        } else {
            // The north indicator, as a button-sized tile (not a button: it does nothing)
            Box(modifier = Modifier.width(44.dp).height(32.dp), contentAlignment = Alignment.Center) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "\u25B2 N", fontSize = ms(8f), fontWeight = FontWeight.ExtraBold, color = blue)
                    }
                }
            }
            // The empty box (four corner brackets): brings the selected station back into view. It is only there while
            // the selected station is out of the visible map (panned away, or zoomed in past it)
            androidx.compose.animation.AnimatedVisibility(
                visible = showLocate,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                MapControlButton(stringResource(R.string.map_locate_selected), onLocate) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val stroke = 2.dp.toPx()
                        val arm = size.width * 0.38f
                        val w = size.width
                        val h = size.height
                        fun line(a: Offset, b: Offset) = drawLine(blue, a, b, strokeWidth = stroke, cap = StrokeCap.Round)
                        line(Offset(0f, 0f), Offset(arm, 0f)); line(Offset(0f, 0f), Offset(0f, arm))
                        line(Offset(w, 0f), Offset(w - arm, 0f)); line(Offset(w, 0f), Offset(w, arm))
                        line(Offset(0f, h), Offset(arm, h)); line(Offset(0f, h), Offset(0f, h - arm))
                        line(Offset(w, h), Offset(w - arm, h)); line(Offset(w, h), Offset(w, h - arm))
                    }
                }
            }
        }
    }
}

/**
 * The Elevation view's "Stations" drop-down: the region's best-known places first ([landmarkIds], in their order, under a
 * heading), then every other station by name; picking one makes it the selected station. A search filters both parts.
 */
@Composable
private fun StationsDropdown(stations: List<MicroclimateStation>, landmarkIds: List<String>, selectedStationId: String?, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val ink = Color(0xFF334155)
    val landmarks = remember(stations, landmarkIds) { landmarkIds.mapNotNull { id -> stations.firstOrNull { it.id == id } } }
    val rest = remember(stations, landmarks) { stations.filter { it !in landmarks }.sortedBy { it.name.lowercase() } }
    var query by remember { mutableStateOf("") }
    val q = query.trim()
    val landmarkMatches = remember(landmarks, query) { if (q.isEmpty()) landmarks else landmarks.filter { it.name.contains(q, ignoreCase = true) } }
    val restMatches = remember(rest, query) { if (q.isEmpty()) rest else rest.filter { it.name.contains(q, ignoreCase = true) } }
    val matches = landmarkMatches + restMatches
    Box {
        // The same type as the Topography / Elevation buttons
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color.White.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            modifier = Modifier
                .height(32.dp)
                .clickable(onClickLabel = stringResource(R.string.stations_choose), role = Role.Button) { open = true }
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.stations_title), fontSize = ms(9f), fontWeight = FontWeight.Bold, color = ink)
                Text(text = " \u25BE", fontSize = ms(9f), fontWeight = FontWeight.Bold, color = ink)
            }
        }
        val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = if (lightLook) Color(0xFFF1E2C4) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (lightLook) Color(0xFF3A2A1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            // Search box at the top: type to filter the list as you go (the full list shows until you do)
            androidx.compose.runtime.LaunchedEffect(open) { if (open) query = "" }
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .widthIn(min = 230.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.75f))
                    .border(1.dp, Color(0xFF3A2A1E).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // A magnifying glass, drawn
                Canvas(modifier = Modifier.size(16.dp)) {
                    val stroke = 2.dp.toPx()
                    val r = size.width * 0.32f
                    drawCircle(ink, radius = r, center = Offset(r + stroke / 2f, r + stroke / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
                    drawLine(ink, Offset(r * 1.75f + stroke, r * 1.75f + stroke), Offset(size.width - stroke / 2f, size.height - stroke / 2f), strokeWidth = stroke, cap = StrokeCap.Round)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.stations_search), fontSize = ms(9f), color = ink.copy(alpha = 0.55f))
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = ms(9f), color = Color(0xFF1E293B)),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(ink),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    Text(
                        text = "\u2715",
                        fontSize = ms(9f),
                        color = ink,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClickLabel = stringResource(R.string.stations_search_clear)) { query = "" }
                            .padding(start = 8.dp)
                    )
                }
            }
            // The list scrolls under the search box (which stays put)
            Column(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.6f).dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (matches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.stations_no_match),
                        fontSize = ms(9f),
                        color = ink.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                @Composable
                fun heading(text: String) = Text(
                    text = text,
                    fontSize = ms(8f),
                    fontWeight = FontWeight.Bold,
                    color = ink.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
                )
                if (landmarkMatches.isNotEmpty() && restMatches.isNotEmpty()) heading(stringResource(R.string.stations_main_places))
                matches.forEachIndexed { position, station ->
                    if (position == landmarkMatches.size && landmarkMatches.isNotEmpty() && restMatches.isNotEmpty()) {
                        androidx.compose.material3.HorizontalDivider(color = ink.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 4.dp))
                        heading(stringResource(R.string.stations_all))
                    }
                    val current = station.id == selectedStationId
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = station.name,
                                fontSize = ms(9f),
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            open = false
                            onPick(station.id)
                        }
                    )
                }
            }
        }
    }
}
