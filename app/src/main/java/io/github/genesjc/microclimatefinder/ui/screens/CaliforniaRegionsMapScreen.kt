package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegion
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import io.github.genesjc.microclimatefinder.domain.model.RegionStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaliforniaRegionsMapScreen(
    onNavigateBack: () -> Unit,
    onNavigateToActiveTransect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedRegionId by remember { mutableStateOf(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id) }
    var currentMapStyle by remember { mutableStateOf(CaliforniaMapStyle.CLASSIC) }
    val selectedRegion = remember(selectedRegionId) {
        CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == selectedRegionId }
            ?: CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "California Regional Sections",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "v0.23.0",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Macro Climatic Constraints & Regional Boundaries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "←",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Interactive California Spatial Map Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Interactive California Section Map",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap region to inspect",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Segmented Map Style Selector Tabs: "Classic CA" & "Picasso Style"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                CaliforniaMapStyle.values().forEach { style ->
                                    val isSelected = style == currentMapStyle
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { currentMapStyle = style }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = style.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Map Viewport (Strict Light Mode)
                        when (currentMapStyle) {
                            CaliforniaMapStyle.CLASSIC -> CaliforniaClassicMapCanvas(
                                selectedRegionId = selectedRegionId,
                                onSelectRegion = { selectedRegionId = it }
                            )
                            CaliforniaMapStyle.PICASSO -> CaliforniaPicassoMapCanvas(
                                selectedRegionId = selectedRegionId,
                                onSelectRegion = { selectedRegionId = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Region Dropdown Selector (Pill Tag Outline Style)
                        var dropdownExpanded by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            // Pill Tag Outline Trigger
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                ),
                                shadowElevation = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { dropdownExpanded = !dropdownExpanded }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    color = Color(android.graphics.Color.parseColor(selectedRegion.colorHex)),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedRegion.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "★",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Select",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (dropdownExpanded) "▴" else "▾",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Material3 DropdownMenu
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        RoundedCornerShape(14.dp)
                                    )
                            ) {
                                CaliforniaRegionalDefinitions.ALL_REGIONS.forEach { region ->
                                    val isSelected = region.id == selectedRegionId
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(
                                                                color = Color(android.graphics.Color.parseColor(region.colorHex)),
                                                                shape = CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = region.name,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (region.status == RegionStatus.ACTIVE_TRANSECT) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "★",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "✓",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedRegionId = region.id
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selected Region Detail & Constraints Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Region Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedRegion.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = selectedRegion.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) "ACTIVE (36 STATIONS)" else "PLANNED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Geographic Coordinate Constraints Table
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "GEOGRAPHICAL BOUNDING CONSTRAINTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(text = "Latitude Bounds", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${selectedRegion.minLat}° N to ${selectedRegion.maxLat}° N",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text(text = "Longitude Bounds", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${selectedRegion.minLon}° W to ${selectedRegion.maxLon}° W",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(text = "Center Coordinate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${selectedRegion.centerLat}° N, ${selectedRegion.centerLon}° W",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Column {
                                        Text(text = "Elevation Span", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        val maxFt = (selectedRegion.maxElevationMeters * 3.28084).toInt()
                                        Text(
                                            text = "${selectedRegion.minElevationMeters}m to ${selectedRegion.maxElevationMeters}m (${maxFt} ft)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Description
                        Text(
                            text = selectedRegion.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )

                        // Microclimate Features
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "KEY MICROCLIMATIC DYNAMICS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            selectedRegion.microclimateTraits.forEach { trait ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = trait,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Key Reference Cities Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ANCHOR CITIES & STATIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedRegion.keyCities.forEach { city ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = city,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Constraint Code Definition Blueprint
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "// Regional Draggable Canvas Constraint Blueprint",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "const val MIN_LAT = ${selectedRegion.minLat}\nconst val MAX_LAT = ${selectedRegion.maxLat}\nconst val MIN_LON = ${selectedRegion.minLon}\nconst val MAX_LON = ${selectedRegion.maxLon}\nconst val CENTER_LAT = ${selectedRegion.centerLat}\nconst val CENTER_LON = ${selectedRegion.centerLon}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Action Button if Active
                        if (selectedRegion.status == RegionStatus.ACTIVE_TRANSECT) {
                            Button(
                                onClick = onNavigateToActiveTransect,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = "Open Active Transect (36 Stations)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Custom Canvas rendering a stylized geographical representation of California's 6 climatic regions.
 */
enum class CaliforniaMapStyle(val label: String) {
    CLASSIC("Classic CA"),
    PICASSO("Picasso Style")
}

/**
 * Classic CA: Realistic Geographic Silhouette of California
 * With accurate state bounds (42°N Oregon border, Lake Tahoe corner, diagonal Nevada border,
 * Point Conception coastal hook, down to 32.5°N Mexico border) and 6 interlocking macro sections.
 * Rendered in strict Light Mode.
 */

/**
 * Classic CA: Realistic Geographic Silhouette of California
 * With continuous interlocking regional topology:
 * - North Coast, Cascade & Sierra Range
 * - Bay Area & Sacramento River Transect
 * - Central Valley agricultural basin
 * - Central Coast (Big Sur to Santa Barbara)
 * - Los Angeles & Basin
 * - San Diego & South Coast
 * All 6 regional polygons share identical boundary vertices, guaranteeing ZERO interior gaps.
 * Rendered in strict Light Mode with subtle atmospheric grid lines.
 */
@Composable
private fun CaliforniaClassicMapCanvas(
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFBEB), Color(0xFFF3E1B8), Color(0xFFE6CD98)),
                    radius = 900f
                ),
                RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, Color(0xFFC9A66B), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val padX = w * 0.05f
                    val padY = h * 0.05f
                    val availW = w * 0.90f
                    val availH = h * 0.90f
                    val lonMin = -124.5f
                    val lonMax = -114.1f
                    val latMin = 32.4f
                    val latMax = 42.1f

                    fun px(lon: Float): Float = padX + ((lon - lonMin) / (lonMax - lonMin)) * availW
                    fun py(lat: Float): Float = padY + (1.0f - (lat - latMin) / (latMax - latMin)) * availH

                    fun inPoly(pt: Offset, poly: List<Offset>): Boolean {
                        var inside = false
                        var j = poly.size - 1
                        for (i in poly.indices) {
                            val xi = poly[i].x
                            val yi = poly[i].y
                            val xj = poly[j].x
                            val yj = poly[j].y
                            val intersect = ((yi > pt.y) != (yj > pt.y)) &&
                                    (pt.x < (xj - xi) * (pt.y - yi) / (yj - yi) + xi)
                            if (intersect) inside = !inside
                            j = i
                        }
                        return inside
                    }

                    // Shared boundary vertices (lon, lat)
                    val pTahoe = Offset(px(-120.00f), py(39.00f))
                    val pNorthValleyMid = Offset(px(-120.80f), py(39.15f))
                    val tNorthBayValley = Offset(px(-121.60f), py(39.20f))
                    val pNorthBayMid = Offset(px(-122.80f), py(38.95f))
                    val pCoastMendoSonoma = Offset(px(-123.65f), py(38.80f))

                    val pBayValley1 = Offset(px(-121.20f), py(38.50f))
                    val pBayValley2 = Offset(px(-121.15f), py(37.95f))
                    val pBayValley3 = Offset(px(-121.30f), py(37.50f))
                    val tBayValleyCoast = Offset(px(-121.50f), py(37.00f))
                    val pBayCoastMid = Offset(px(-121.80f), py(36.95f))
                    val pCoastMontereyBay = Offset(px(-121.85f), py(36.80f))

                    val pCoastValley1 = Offset(px(-120.70f), py(36.20f))
                    val pCoastValley2 = Offset(px(-120.10f), py(35.50f))
                    val pCoastValley3 = Offset(px(-119.50f), py(35.00f))
                    val tCoastValleyLa = Offset(px(-119.00f), py(34.80f))
                    val pCoastLaMid = Offset(px(-119.30f), py(34.45f))
                    val pCoastVentura = Offset(px(-119.10f), py(34.10f))

                    val pValleyLa1 = Offset(px(-118.45f), py(35.05f))
                    val pValleyLa2 = Offset(px(-117.80f), py(35.35f))
                    val pValleyLa3 = Offset(px(-117.20f), py(35.80f))
                    val pNevadaValleyLa = Offset(px(-115.80f), py(36.00f))

                    val pCoastSanOnofre = Offset(px(-117.60f), py(33.38f))
                    val pLaSd1 = Offset(px(-117.10f), py(33.45f))
                    val pLaSd2 = Offset(px(-116.30f), py(33.35f))
                    val pLaSd3 = Offset(px(-115.50f), py(33.20f))
                    val pColoradoRiverMid = Offset(px(-114.60f), py(33.30f))

                    val northPoly = listOf(
                        pCoastMendoSonoma,
                        Offset(px(-124.41f), py(40.44f)),
                        Offset(px(-124.21f), py(42.00f)),
                        Offset(px(-120.00f), py(42.00f)),
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pNorthBayMid
                    )

                    val bayPoly = listOf(
                        pCoastMendoSonoma,
                        pNorthBayMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pBayCoastMid,
                        pCoastMontereyBay,
                        Offset(px(-122.20f), py(37.00f)),
                        Offset(px(-122.51f), py(37.77f)),
                        Offset(px(-123.00f), py(38.00f))
                    )

                    val valleyPoly = listOf(
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-117.20f), py(37.00f)),
                        Offset(px(-118.60f), py(38.00f))
                    )

                    val coastPoly = listOf(
                        pCoastMontereyBay,
                        pBayCoastMid,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pCoastLaMid,
                        pCoastVentura,
                        Offset(px(-119.70f), py(34.40f)),
                        Offset(px(-120.47f), py(34.45f)),
                        Offset(px(-120.85f), py(35.30f)),
                        Offset(px(-121.90f), py(36.30f))
                    )

                    val laPoly = listOf(
                        pCoastVentura,
                        pCoastLaMid,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-114.63f), py(35.00f)),
                        pColoradoRiverMid,
                        pLaSd3,
                        pLaSd2,
                        pLaSd1,
                        pCoastSanOnofre,
                        Offset(px(-118.40f), py(33.74f)),
                        Offset(px(-118.85f), py(34.02f))
                    )

                    val sdPoly = listOf(
                        pCoastSanOnofre,
                        pLaSd1,
                        pLaSd2,
                        pLaSd3,
                        pColoradoRiverMid,
                        Offset(px(-114.72f), py(32.72f)),
                        Offset(px(-117.12f), py(32.53f)),
                        Offset(px(-117.25f), py(32.72f))
                    )

                    when {
                        inPoly(offset, northPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA.id)
                        inPoly(offset, bayPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
                        inPoly(offset, valleyPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_VALLEY.id)
                        inPoly(offset, coastPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_COAST.id)
                        inPoly(offset, laPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.LOS_ANGELES.id)
                        inPoly(offset, sdPoly) -> onSelectRegion(CaliforniaRegionalDefinitions.SAN_DIEGO.id)
                        else -> {
                            val tapLon = lonMin + ((offset.x - padX) / availW) * (lonMax - lonMin)
                            val tapLat = latMin + (1f - (offset.y - padY) / availH) * (latMax - latMin)
                            val closest = CaliforniaRegionalDefinitions.ALL_REGIONS.minByOrNull { r ->
                                val dLat = tapLat - r.centerLat
                                val dLon = tapLon - r.centerLon
                                dLat * dLat + dLon * dLon
                            }
                            closest?.let { onSelectRegion(it.id) }
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.05f
            val padY = h * 0.05f
            val availW = w * 0.90f
            val availH = h * 0.90f
            val lonMin = -124.5f
            val lonMax = -114.1f
            val latMin = 32.4f
            val latMax = 42.1f

            fun px(lon: Float): Float = padX + ((lon - lonMin) / (lonMax - lonMin)) * availW
            fun py(lat: Float): Float = padY + (1.0f - (lat - latMin) / (latMax - latMin)) * availH

            // 1. Warm-toned dashed graticule lines (34°N, 36°N, 38°N, 40°N) — antique cartography feel
            val graticuleEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            val gridLats = floatArrayOf(34f, 36f, 38f, 40f)
            for (glat in gridLats) {
                val y = py(glat)
                drawLine(
                    color = Color(0xFFB89968),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f,
                    pathEffect = graticuleEffect,
                    alpha = 0.55f
                )
            }

            // Decorative compass rose watermark (top-right), for character and a sense of place
            drawCompassRose(center = Offset(w - 34f, 34f), radius = 22f, color = Color(0xFF8B6F3E))

            // Shared boundary vertices (Continuous Interlocking Topology)
            val pTahoe = Offset(px(-120.00f), py(39.00f))
            val pNorthValleyMid = Offset(px(-120.80f), py(39.15f))
            val tNorthBayValley = Offset(px(-121.60f), py(39.20f))
            val pNorthBayMid = Offset(px(-122.80f), py(38.95f))
            val pCoastMendoSonoma = Offset(px(-123.65f), py(38.80f))

            val pBayValley1 = Offset(px(-121.20f), py(38.50f))
            val pBayValley2 = Offset(px(-121.15f), py(37.95f))
            val pBayValley3 = Offset(px(-121.30f), py(37.50f))
            val tBayValleyCoast = Offset(px(-121.50f), py(37.00f))
            val pBayCoastMid = Offset(px(-121.80f), py(36.95f))
            val pCoastMontereyBay = Offset(px(-121.85f), py(36.80f))

            val pCoastValley1 = Offset(px(-120.70f), py(36.20f))
            val pCoastValley2 = Offset(px(-120.10f), py(35.50f))
            val pCoastValley3 = Offset(px(-119.50f), py(35.00f))
            val tCoastValleyLa = Offset(px(-119.00f), py(34.80f))
            val pCoastLaMid = Offset(px(-119.30f), py(34.45f))
            val pCoastVentura = Offset(px(-119.10f), py(34.10f))

            val pValleyLa1 = Offset(px(-118.45f), py(35.05f))
            val pValleyLa2 = Offset(px(-117.80f), py(35.35f))
            val pValleyLa3 = Offset(px(-117.20f), py(35.80f))
            val pNevadaValleyLa = Offset(px(-115.80f), py(36.00f))

            val pCoastSanOnofre = Offset(px(-117.60f), py(33.38f))
            val pLaSd1 = Offset(px(-117.10f), py(33.45f))
            val pLaSd2 = Offset(px(-116.30f), py(33.35f))
            val pLaSd3 = Offset(px(-115.50f), py(33.20f))
            val pColoradoRiverMid = Offset(px(-114.60f), py(33.30f))

            fun makePath(pts: List<Offset>): Path {
                val path = Path()
                if (pts.isNotEmpty()) {
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        path.lineTo(pts[i].x, pts[i].y)
                    }
                    path.close()
                }
                return path
            }

            // Pacific Ocean coastal wash
            val oceanPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(px(-124.21f), py(42.00f))
                lineTo(px(-124.41f), py(40.44f))
                lineTo(pCoastMendoSonoma.x, pCoastMendoSonoma.y)
                lineTo(px(-123.00f), py(38.00f))
                lineTo(px(-122.51f), py(37.77f))
                lineTo(px(-122.20f), py(37.00f))
                lineTo(pCoastMontereyBay.x, pCoastMontereyBay.y)
                lineTo(px(-121.90f), py(36.30f))
                lineTo(px(-120.85f), py(35.30f))
                lineTo(px(-120.47f), py(34.45f))
                lineTo(px(-119.70f), py(34.40f))
                lineTo(pCoastVentura.x, pCoastVentura.y)
                lineTo(px(-118.85f), py(34.02f))
                lineTo(px(-118.40f), py(33.74f))
                lineTo(pCoastSanOnofre.x, pCoastSanOnofre.y)
                lineTo(px(-117.25f), py(32.72f))
                lineTo(px(-117.12f), py(32.53f))
                lineTo(0f, py(32.53f))
                close()
            }
            drawPath(
                oceanPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFCDEEF2), Color(0xFFA8DEE6)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // 2. The 6 seamlessly interlocking regions (ZERO interior gaps)
            val regionPolys = listOf(
                Pair(
                    CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA,
                    makePath(listOf(
                        pCoastMendoSonoma,
                        Offset(px(-124.41f), py(40.44f)),
                        Offset(px(-124.21f), py(42.00f)),
                        Offset(px(-120.00f), py(42.00f)),
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pNorthBayMid
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO,
                    makePath(listOf(
                        pCoastMendoSonoma,
                        pNorthBayMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pBayCoastMid,
                        pCoastMontereyBay,
                        Offset(px(-122.20f), py(37.00f)),
                        Offset(px(-122.51f), py(37.77f)),
                        Offset(px(-123.00f), py(38.00f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.CENTRAL_VALLEY,
                    makePath(listOf(
                        pTahoe,
                        pNorthValleyMid,
                        tNorthBayValley,
                        pBayValley1,
                        pBayValley2,
                        pBayValley3,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-117.20f), py(37.00f)),
                        Offset(px(-118.60f), py(38.00f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.CENTRAL_COAST,
                    makePath(listOf(
                        pCoastMontereyBay,
                        pBayCoastMid,
                        tBayValleyCoast,
                        pCoastValley1,
                        pCoastValley2,
                        pCoastValley3,
                        tCoastValleyLa,
                        pCoastLaMid,
                        pCoastVentura,
                        Offset(px(-119.70f), py(34.40f)),
                        Offset(px(-120.47f), py(34.45f)),
                        Offset(px(-120.85f), py(35.30f)),
                        Offset(px(-121.90f), py(36.30f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.LOS_ANGELES,
                    makePath(listOf(
                        pCoastVentura,
                        pCoastLaMid,
                        tCoastValleyLa,
                        pValleyLa1,
                        pValleyLa2,
                        pValleyLa3,
                        pNevadaValleyLa,
                        Offset(px(-114.63f), py(35.00f)),
                        pColoradoRiverMid,
                        pLaSd3,
                        pLaSd2,
                        pLaSd1,
                        pCoastSanOnofre,
                        Offset(px(-118.40f), py(33.74f)),
                        Offset(px(-118.85f), py(34.02f))
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.SAN_DIEGO,
                    makePath(listOf(
                        pCoastSanOnofre,
                        pLaSd1,
                        pLaSd2,
                        pLaSd3,
                        pColoradoRiverMid,
                        Offset(px(-114.72f), py(32.72f)),
                        Offset(px(-117.12f), py(32.53f)),
                        Offset(px(-117.25f), py(32.72f))
                    ))
                )
            )

            // Soft glow halo behind the selected region, for a "pop on tap" feel
            for ((region, path) in regionPolys) {
                if (selectedRegionId == region.id) {
                    val color = Color(android.graphics.Color.parseColor(region.colorHex))
                    drawPath(path = path, color = color.copy(alpha = 0.30f), style = Stroke(width = 14f))
                }
            }

            // Fills lock together seamlessly — richer gradient fills instead of flat washed-out tints
            for ((region, path) in regionPolys) {
                val isSelected = selectedRegionId == region.id
                val color = Color(android.graphics.Color.parseColor(region.colorHex))
                val bounds = path.getBounds()
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(color.copy(alpha = if (isSelected) 1f else 0.72f).lighten(0.18f), color.copy(alpha = if (isSelected) 1f else 0.72f)),
                        start = Offset(bounds.left, bounds.top),
                        end = Offset(bounds.right, bounds.bottom)
                    )
                )
            }

            // Clean boundary strokes — warm ink-brown for an inked, hand-drawn map feel
            for ((_, path) in regionPolys) {
                drawPath(
                    path = path,
                    color = Color(0xFF4A3728).copy(alpha = 0.55f),
                    style = Stroke(width = 1.3f)
                )
            }

            // Selected region highlighted stroke
            for ((region, path) in regionPolys) {
                if (selectedRegionId == region.id) {
                    drawPath(
                        path = path,
                        color = Color(0xFF2A1B12),
                        style = Stroke(width = 3f)
                    )
                    val bounds = path.getBounds()
                    drawPinMarker(
                        center = Offset(bounds.center.x, bounds.center.y),
                        color = Color(android.graphics.Color.parseColor(region.colorHex))
                    )
                }
            }

            // Key anchor cities — warm ink-brown ring with bright core, matching the antique-map treatment
            val cityMarkers = listOf(
                Offset(px(-122.42f), py(37.77f)) to 4.5f, // SF Bay
                Offset(px(-121.49f), py(38.58f)) to 3.6f, // Sacramento
                Offset(px(-118.24f), py(34.05f)) to 4f,   // LA
                Offset(px(-117.16f), py(32.72f)) to 4f    // San Diego
            )
            for ((center, radius) in cityMarkers) {
                drawCircle(color = Color(0xFF2A1B12).copy(alpha = 0.55f), radius = radius + 1.6f, center = center)
                drawCircle(color = Color(0xFFFFFBEB), radius = radius, center = center)
                drawCircle(color = Color(0xFFB8860B), radius = radius * 0.45f, center = center)
            }
        }
    }
}

private fun Color.lighten(fraction: Float): Color = Color(
    red = red + (1f - red) * fraction,
    green = green + (1f - green) * fraction,
    blue = blue + (1f - blue) * fraction,
    alpha = alpha
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPinMarker(center: Offset, color: Color) {
    drawCircle(color = Color(0xFF2A1B12).copy(alpha = 0.35f), radius = 11f, center = center)
    drawCircle(color = Color.White, radius = 8.5f, center = center)
    drawCircle(color = color, radius = 6f, center = center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompassRose(center: Offset, radius: Float, color: Color) {
    val points = mutableListOf<Offset>()
    val spikes = 8
    for (i in 0 until spikes * 2) {
        val r = if (i % 2 == 0) radius else radius * 0.38f
        val angle = (Math.PI * i / spikes) - Math.PI / 2
        points.add(Offset(center.x + (r * kotlin.math.cos(angle)).toFloat(), center.y + (r * kotlin.math.sin(angle)).toFloat()))
    }
    val star = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        close()
    }
    drawPath(star, color = color.copy(alpha = 0.35f))
    drawCircle(color = color.copy(alpha = 0.5f), radius = radius * 0.14f, center = center)
}

/**
 * Picasso Style: Cubist Polygonal Representation of California
 * Seamless geometric abstraction with shared vertices between all adjacent blocks,
 * completely filling interior gaps.
 */
@Composable
private fun CaliforniaPicassoMapCanvas(
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(colors = listOf(Color(0xFF1F2937), Color(0xFF111827))),
                RoundedCornerShape(16.dp)
            )
            .border(1.5.dp, Color(0xFF374151), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val yRatio = offset.y / size.height
                    val xRatio = offset.x / size.width
                    when {
                        yRatio < 0.31f -> onSelectRegion(CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA.id)
                        yRatio < 0.52f && xRatio < 0.60f -> onSelectRegion(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
                        yRatio in 0.31f..0.72f && xRatio >= 0.50f -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_VALLEY.id)
                        yRatio in 0.52f..0.76f && xRatio < 0.50f -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_COAST.id)
                        yRatio in 0.72f..0.88f -> onSelectRegion(CaliforniaRegionalDefinitions.LOS_ANGELES.id)
                        yRatio >= 0.88f -> onSelectRegion(CaliforniaRegionalDefinitions.SAN_DIEGO.id)
                        else -> onSelectRegion(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Faint structural grid lines against the dark gallery backdrop
            for (i in 1..4) {
                val lineY = h * (i / 5f)
                drawLine(
                    color = Color.White.copy(alpha = 0.06f),
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 1f
                )
            }

            // Shared Picasso Vertices (normalized to completely eliminate gaps)
            fun pt(xRatio: Float, yRatio: Float) = Offset(w * xRatio, h * yRatio)

            val pNorthWest = pt(0.18f, 0.05f)
            val pNorthEast = pt(0.85f, 0.05f)
            val pTahoe = pt(0.78f, 0.29f)
            val pNorthCoast = pt(0.27f, 0.31f)
            val tNorthBayValley = pt(0.55f, 0.30f)

            val pBayCoast = pt(0.22f, 0.49f)
            val tBayValleyCoast = pt(0.57f, 0.49f)

            val pCoastLaCoast = pt(0.36f, 0.73f)
            val tCoastValleyLa = pt(0.48f, 0.69f)
            val pValleyNevada = pt(0.74f, 0.69f)

            val pLaSdCoast = pt(0.58f, 0.84f)
            val pLaSdEast = pt(0.87f, 0.79f)

            val pSdMexEast = pt(0.91f, 0.95f)
            val pSdMexCoast = pt(0.64f, 0.95f)

            fun makePath(pts: List<Offset>): Path {
                val path = Path()
                if (pts.isNotEmpty()) {
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        path.lineTo(pts[i].x, pts[i].y)
                    }
                    path.close()
                }
                return path
            }

            val picassoPolys = listOf(
                Pair(
                    CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA,
                    makePath(listOf(pNorthWest, pNorthEast, pTahoe, tNorthBayValley, pNorthCoast))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO,
                    makePath(listOf(pNorthCoast, tNorthBayValley, tBayValleyCoast, pBayCoast))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.CENTRAL_VALLEY,
                    makePath(listOf(
                        pTahoe,
                        tNorthBayValley,
                        tBayValleyCoast,
                        tCoastValleyLa,
                        pValleyNevada,
                        pt(0.82f, 0.35f)
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.CENTRAL_COAST,
                    makePath(listOf(
                        pBayCoast,
                        tBayValleyCoast,
                        tCoastValleyLa,
                        pCoastLaCoast,
                        pt(0.28f, 0.66f)
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.LOS_ANGELES,
                    makePath(listOf(
                        pCoastLaCoast,
                        tCoastValleyLa,
                        pValleyNevada,
                        pLaSdEast,
                        pLaSdCoast
                    ))
                ),
                Pair(
                    CaliforniaRegionalDefinitions.SAN_DIEGO,
                    makePath(listOf(
                        pLaSdCoast,
                        pLaSdEast,
                        pSdMexEast,
                        pSdMexCoast
                    ))
                )
            )

            // Soft glow behind the selected block
            for ((region, path) in picassoPolys) {
                if (selectedRegionId == region.id) {
                    val color = Color(android.graphics.Color.parseColor(region.colorHex))
                    drawPath(path = path, color = color.copy(alpha = 0.45f), style = Stroke(width = 16f))
                }
            }

            // Vivid gradient block fills — bold "museum canvas" cubist panels
            for ((region, path) in picassoPolys) {
                val isSelected = selectedRegionId == region.id
                val color = Color(android.graphics.Color.parseColor(region.colorHex))
                val bounds = path.getBounds()
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(color.lighten(0.22f), color).map { if (isSelected) it else it.copy(alpha = 0.82f) },
                        start = Offset(bounds.left, bounds.top),
                        end = Offset(bounds.right, bounds.bottom)
                    )
                )
            }

            // Clean white canvas-mount outlines between every block
            for ((_, path) in picassoPolys) {
                drawPath(path = path, color = Color.White.copy(alpha = 0.5f), style = Stroke(width = 1.6f))
            }

            // Selected block gets a bold outline + pin marker
            for ((region, path) in picassoPolys) {
                if (selectedRegionId == region.id) {
                    drawPath(path = path, color = Color.White, style = Stroke(width = 3.2f))
                    val bounds = path.getBounds()
                    drawPinMarker(
                        center = Offset(bounds.center.x, bounds.center.y),
                        color = Color(android.graphics.Color.parseColor(region.colorHex))
                    )
                }
            }
        }
    }
}
