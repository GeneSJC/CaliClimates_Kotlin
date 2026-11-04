package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import io.github.genesjc.climatefinder.ui.text.elevationFeetText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.ms

/**
 * Whether the Profile view shows the sky badge at the left of the chart (Gene, 2026-11-04). Off: its snowflake and its condition
 * line ("Crisp Sub-Alpine") now sit in the collapsed station tile in the header, which already has the temperature and the name.
 * The badge also cycled to the next landmark station when tapped; turn this on to get both back.
 */
internal const val SHOW_SKY_BADGE = false

/** The sky badge at the left of the chart: the temperature, the station and the sky; a tap cycles to the next landmark station. */
@Composable
internal fun ElevationSkyBadge(
    skyTheme: SkyAtmosphereTheme,
    tempF: Double?,
    currentStation: MicroclimateStation?,
    cycleIds: List<String>,
    sorted: List<MicroclimateStation>,
    expandT: Float,
    embedded: Boolean,
    onSelectStation: (MicroclimateStation) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = skyTheme.badgeBg.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, skyTheme.accent.copy(alpha = 0.45f)),
        modifier = Modifier
            .offset(x = 52.dp + 5.dp * expandT, y = 50.dp + (if (embedded) HOME_TILE_BAND else 0.dp))
            .clickable {
                // Tap to cycle landmarks
                if (cycleIds.isNotEmpty()) {
                    val currentIndex = cycleIds.indexOfFirst { it == currentStation?.id }
                    val nextIndex = if (currentIndex >= 0) (currentIndex + 1) % cycleIds.size else 0
                    val nextId = cycleIds[nextIndex]
                    sorted.find { it.id == nextId }?.let { onSelectStation(it) }
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            skyTheme.icon?.let { Text(text = it, fontSize = ms(14f)) }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tempF?.let { stringResource(R.string.sky_temp_f, it.toInt()) } ?: stringResource(R.string.sky_temp_missing),
                        fontSize = ms(11f),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = currentStation?.displayName?.let { if (it.length > 26) it.take(25) + "\u2026" else it } ?: "",
                        fontSize = ms(9f),
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                }
                Text(
                    text = stringResource(R.string.sky_condition_line, stringResource(skyTheme.conditionRes), currentStation?.let { elevationFeetText(it.elevationMeters) }.orEmpty()),
                    fontSize = ms(8f),
                    color = skyTheme.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
