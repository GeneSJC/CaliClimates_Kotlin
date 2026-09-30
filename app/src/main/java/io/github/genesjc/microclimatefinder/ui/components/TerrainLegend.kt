package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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

/**
 * Colour key for the map dots, built from the region's own stations and kept to one line until
 * opened: "Terrain legend" with a chevron; tapping it shows only the terrain types the region
 * actually has, each with its station count. A station is "ridge / highland" when its data says
 * ridge, foothill or mountain, or when it sits above 500 m.
 * [showRelief] adds the green "Mountain relief" entry, used only by the Bay Area map, whose
 * hand-drawn terrain has green relief bands.
 * [extra] is more content shown when opened, under the key (the Topo Map puts its dot-size slider
 * there); it also adds "& dot size" to the header so the control can be found.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerrainLegend(
    stations: List<MicroclimateStation>,
    modifier: Modifier = Modifier,
    showRelief: Boolean = false,
    extra: (@Composable () -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .clickable(
                    onClickLabel = if (expanded) "Hide terrain legend" else "Show terrain legend",
                    role = Role.Button
                ) { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (extra != null) "Terrain legend & dot size" else "Terrain legend",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (expanded) " ▴" else " ▾",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (expanded) {
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
