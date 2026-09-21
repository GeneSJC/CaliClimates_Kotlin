package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
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
                                    text = "v0.21.0",
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
                // Explanatory Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "ℹ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Text(
                            text = "Select any of California's 6 core sections to inspect geographic bounding constraints [minLat, maxLat, minLon, maxLon] and elevation profiles for future transects.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
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

                        // Stylized Interactive Vector Map of California
                        CaliforniaInteractiveCanvas(
                            selectedRegionId = selectedRegionId,
                            onSelectRegion = { selectedRegionId = it }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Region Selector Chips Row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CaliforniaRegionalDefinitions.ALL_REGIONS) { region ->
                                val isSelected = region.id == selectedRegionId
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedRegionId = region.id }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    color = Color(android.graphics.Color.parseColor(region.colorHex)),
                                                    shape = CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = region.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (region.status == RegionStatus.ACTIVE_TRANSECT) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "★",
                                                fontSize = 11.sp,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
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
@Composable
private fun CaliforniaInteractiveCanvas(
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val yRatio = offset.y / size.height
                    val xRatio = offset.x / size.width

                    when {
                        // Northern CA (top 32%)
                        yRatio < 0.32f -> onSelectRegion(CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA.id)
                        // Bay Area / Sacramento (middle-west and center 32% to 54%)
                        yRatio < 0.54f && xRatio < 0.65f -> onSelectRegion(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
                        // Central Valley (interior swath 35% to 70%)
                        yRatio in 0.40f..0.72f && xRatio >= 0.50f -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_VALLEY.id)
                        // Central Coast (coastal swath 50% to 74%)
                        yRatio in 0.50f..0.74f && xRatio < 0.52f -> onSelectRegion(CaliforniaRegionalDefinitions.CENTRAL_COAST.id)
                        // LA (74% to 88%)
                        yRatio in 0.74f..0.88f -> onSelectRegion(CaliforniaRegionalDefinitions.LOS_ANGELES.id)
                        // San Diego (bottom 12%)
                        yRatio >= 0.88f -> onSelectRegion(CaliforniaRegionalDefinitions.SAN_DIEGO.id)
                        else -> onSelectRegion(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background grid lines (Atmospheric Lat/Lon Grid)
            for (i in 1..4) {
                val lineY = h * (i / 5f)
                drawLine(
                    color = Color(0xFF1E293B),
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 1f
                )
            }

            // Draw each region as a stylized polygonal block representing California's diagonal Northwest-to-Southeast orientation
            // 1. Far Northern CA (Top)
            val isNorthSelected = selectedRegionId == CaliforniaRegionalDefinitions.NORTHERN_CALIFORNIA.id
            val northColor = Color(0xFF8B5CF6)
            val northPath = Path().apply {
                moveTo(w * 0.18f, h * 0.05f) // NW corner (Del Norte / Oregon border)
                lineTo(w * 0.82f, h * 0.05f) // NE corner (Modoc)
                lineTo(w * 0.76f, h * 0.30f) // East Cascade/Sierra transition
                lineTo(w * 0.28f, h * 0.32f) // Mendocino Coast
                close()
            }
            drawPath(
                path = northPath,
                color = if (isNorthSelected) northColor.copy(alpha = 0.85f) else northColor.copy(alpha = 0.35f),
                style = Fill
            )
            drawPath(
                path = northPath,
                color = if (isNorthSelected) Color.White else northColor,
                style = Stroke(width = if (isNorthSelected) 3f else 1.5f)
            )

            // 2. Bay Area & Sacramento (Middle-West)
            val isBaySelected = selectedRegionId == CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id
            val bayColor = Color(0xFF0284C7)
            val bayPath = Path().apply {
                moveTo(w * 0.28f, h * 0.32f) // Sonoma/Marin
                lineTo(w * 0.65f, h * 0.30f) // Sacramento / Sierra Western slope
                lineTo(w * 0.58f, h * 0.50f) // San Joaquin Delta / Modesto line
                lineTo(w * 0.22f, h * 0.50f) // Monterey Bay / Santa Cruz
                close()
            }
            drawPath(
                path = bayPath,
                color = if (isBaySelected) bayColor.copy(alpha = 0.85f) else bayColor.copy(alpha = 0.35f),
                style = Fill
            )
            drawPath(
                path = bayPath,
                color = if (isBaySelected) Color.White else bayColor,
                style = Stroke(width = if (isBaySelected) 3f else 1.5f)
            )

            // 3. Central Valley (Interior agricultural trough)
            val isValleySelected = selectedRegionId == CaliforniaRegionalDefinitions.CENTRAL_VALLEY.id
            val valleyColor = Color(0xFFF97316)
            val valleyPath = Path().apply {
                moveTo(w * 0.58f, h * 0.32f)
                lineTo(w * 0.80f, h * 0.38f) // Sierra Nevada Crest
                lineTo(w * 0.72f, h * 0.72f) // Tehachapi Pass / Bakersfield
                lineTo(w * 0.50f, h * 0.70f) // Kettleman / Interior Coast Range
                lineTo(w * 0.58f, h * 0.50f)
                close()
            }
            drawPath(
                path = valleyPath,
                color = if (isValleySelected) valleyColor.copy(alpha = 0.85f) else valleyColor.copy(alpha = 0.35f),
                style = Fill
            )
            drawPath(
                path = valleyPath,
                color = if (isValleySelected) Color.White else valleyColor,
                style = Stroke(width = if (isValleySelected) 3f else 1.5f)
            )

            // 4. Central Coast (Big Sur, Monterey, Santa Barbara)
            val isCoastSelected = selectedRegionId == CaliforniaRegionalDefinitions.CENTRAL_COAST.id
            val coastColor = Color(0xFF10B981)
            val coastPath = Path().apply {
                moveTo(w * 0.22f, h * 0.50f) // Monterey Bay
                lineTo(w * 0.50f, h * 0.50f) // Salinas Valley
                lineTo(w * 0.50f, h * 0.70f) // San Luis Obispo
                lineTo(w * 0.44f, h * 0.75f) // Point Conception / Santa Barbara
                lineTo(w * 0.28f, h * 0.68f) // Santa Lucia Coastal scarp
                close()
            }
            drawPath(
                path = coastPath,
                color = if (isCoastSelected) coastColor.copy(alpha = 0.85f) else coastColor.copy(alpha = 0.35f),
                style = Fill
            )
            drawPath(
                path = coastPath,
                color = if (isCoastSelected) Color.White else coastColor,
                style = Stroke(width = if (isCoastSelected) 3f else 1.5f)
            )

            // 5. Los Angeles & Inland Empire
            val isLaSelected = selectedRegionId == CaliforniaRegionalDefinitions.LOS_ANGELES.id
            val laColor = Color(0xFFEC4899)
            val laPath = Path().apply {
                moveTo(w * 0.44f, h * 0.75f) // Ventura / Santa Monica
                lineTo(w * 0.72f, h * 0.72f) // San Gabriel / San Bernardino Mtns
                lineTo(w * 0.85f, h * 0.82f) // Coachella / Palm Springs boundary
                lineTo(w * 0.56f, h * 0.87f) // Orange County Coast
                close()
            }
            drawPath(
                path = laPath,
                color = if (isLaSelected) laColor.copy(alpha = 0.85f) else laColor.copy(alpha = 0.35f),
                style = Fill
            )
            drawPath(
                path = laPath,
                color = if (isLaSelected) Color.White else laColor,
                style = Stroke(width = if (isLaSelected) 3f else 1.5f)
            )

            // 6. San Diego & South Coast
            val isSdSelected = selectedRegionId == CaliforniaRegionalDefinitions.SAN_DIEGO.id
            val sdColor = Color(0xFFF59E0B)
            val sdPath = Path().apply {
                moveTo(w * 0.56f, h * 0.87f) // Oceanside / Carlsbad
                lineTo(w * 0.85f, h * 0.82f) // Anza-Borrego
                lineTo(w * 0.88f, h * 0.96f) // Imperial / Baja border
                lineTo(w * 0.62f, h * 0.96f) // Tijuana / Border Field
                close()
            }
            drawPath(
                path = sdPath,
                color = if (isSdSelected) sdColor.copy(alpha = 0.85f) else sdColor.copy(alpha = 0.35f),
                style = Fill
            )
            drawPath(
                path = sdPath,
                color = if (isSdSelected) Color.White else sdColor,
                style = Stroke(width = if (isSdSelected) 3f else 1.5f)
            )
        }
    }
}
