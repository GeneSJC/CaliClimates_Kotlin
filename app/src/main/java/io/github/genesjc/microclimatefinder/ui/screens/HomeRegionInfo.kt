package io.github.genesjc.microclimatefinder.ui.screens

import io.github.genesjc.microclimatefinder.ui.components.regionPickerColors
import io.github.genesjc.microclimatefinder.ui.components.MapText
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import io.github.genesjc.microclimatefinder.ui.components.LegendItem
import io.github.genesjc.microclimatefinder.ui.components.HeroTile
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegendCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegion
import io.github.genesjc.microclimatefinder.ui.components.CaliforniaOutlineIcon

/** The mix of terrain types in the region, as a bar with counts. */
@Composable
private fun TerrainMixCard(stations: List<MicroclimateStation>) {
    val mix = TerrainType.values()
        .map { type -> type to stations.count { it.terrain == type } }
        .filter { it.second > 0 }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = sheetCardColor(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Terrain mix",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            ) {
                mix.forEach { (type, count) ->
                    Box(
                        modifier = Modifier
                            .weight(count.toFloat())
                            .fillMaxHeight()
                            .background(terrainColor(type))
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                mix.forEach { (type, count) ->
                    LegendItem(color = terrainColor(type), label = "${terrainShortName(type)} $count")
                }
            }
        }
    }
}

// Same colors as the map legends
private fun terrainColor(type: TerrainType): Color = when (type) {
    TerrainType.RIDGE -> Color(0xFF7C3AED)
    TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
    TerrainType.COASTAL -> Color(0xFF0284C7)
    TerrainType.URBAN_CANYON -> Color(0xFF475569)
}

private fun terrainShortName(type: TerrainType): String = when (type) {
    TerrainType.RIDGE -> "Ridge"
    TerrainType.VALLEY_BASIN -> "Valley"
    TerrainType.COASTAL -> "Coastal"
    TerrainType.URBAN_CANYON -> "Urban"
}

/**
 * About the current region, as a half-width tile. "More" grows it, in width and height, into a
 * full-width panel floating over the page, so nothing on the page moves.
 */
@Composable
private fun RegionDetailsCard(region: CaliforniaRegion, fullWidthPx: Int, folded: Boolean) {
    val cardColor = sheetCardColor()
    HeroTile(
        fullWidthPx = fullWidthPx,
        tileColor = cardColor,
        panelColor = cardColor.compositeOver(MaterialTheme.colorScheme.background),
        openLabel = "Show more about ${region.name}",
        closeLabel = "Show less about ${region.name}",
        resetKey = region.id,
        forceClosed = folded,
        tile = { RegionDetailsTile(region) },
        panel = { RegionDetailsBody(region, expanded = true) }
    )
}


/** The half-width tile: title, a short summary, and the More link. */
@Composable
private fun RegionDetailsTile(region: CaliforniaRegion) {
    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = region.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = region.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "More \u25BC",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** The expanded region panel: title, description, traits and key places. */
@Composable
private fun RegionDetailsBody(region: CaliforniaRegion, expanded: Boolean = true) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = region.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = region.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (expanded) "Less ▲" else "More ▼",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = region.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "What shapes the weather here",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            region.microclimateTraits.forEach { trait ->
                Text(
                    text = "\u2022 $trait",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Key places",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = region.keyCities.joinToString(" \u2022 "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}


/** What the info sheet holds: the region tile and stats, then the terrain mix. */
@Composable
internal fun RegionInfoContent(
    region: CaliforniaRegion,
    hasStationData: Boolean,
    stations: List<MicroclimateStation>,
    onOpenStationList: () -> Unit,
    menuOpen: Boolean,
    dotScale: Float = 1f,
    legendExtra: (@Composable () -> Unit)? = null
) {
    var topRowWidthPx by remember { mutableStateOf(0) }
    Column {
        // The title band: a solid, deeper colour than the lavender header widgets above it and the pale body
        // below, so it reads as its own bar. A California icon with this region lit, the title, and the
        // Station List link as a pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CaliforniaOutlineIcon(
                    fill = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    stroke = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    regionColors = regionPickerColors(),
                    selectedRegionId = region.id,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Region Info",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        // Off-white, not pure white, so it is easier on the eyes against the dark band
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
                    )
                }
                if (hasStationData) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClickLabel = "Open the Station List", role = Role.Button) { onOpenStationList() }
                    ) {
                        Text(
                            text = "Station List \u203A",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
        // The body's text follows the shared text scale (MapText), like the map labels and the station card
        val baseDensity = LocalDensity.current
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * MapText.scale)
        ) {
        Column(modifier = Modifier.padding(16.dp)) {
        // The region tile, the full width of the sheet
        Box(modifier = Modifier.fillMaxWidth().onSizeChanged { topRowWidthPx = it.width }) {
            RegionDetailsCard(region, fullWidthPx = topRowWidthPx, folded = menuOpen)
        }
        if (stations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            // The Terrain mix card keeps its original text size for now (it does not follow the text scale)
            androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides baseDensity) {
                TerrainMixCard(stations)
            }
            // The Terrain Legend, at the bottom, folded until opened
            Spacer(modifier = Modifier.height(12.dp))
            TerrainLegendCard(stations = stations, containerColor = sheetCardColor(), dotScale = dotScale, extra = legendExtra)
        }
        }
        }
    }
}
