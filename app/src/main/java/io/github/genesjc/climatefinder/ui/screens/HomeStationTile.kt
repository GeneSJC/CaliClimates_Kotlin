package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import io.github.genesjc.climatefinder.ui.components.StationCard
import io.github.genesjc.climatefinder.ui.components.StationSummaryTile
import io.github.genesjc.climatefinder.ui.components.SkyLine
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.genesjc.climatefinder.ui.components.HeroTile
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import io.github.genesjc.climatefinder.ui.text.elevationNoteText
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/** The selected station as a half-width tile that grows into the full compact station card. */
@Composable
internal fun StationHeroTile(
    station: MicroclimateStation,
    fullWidthPx: Int,
    regionStations: List<MicroclimateStation>,
    regionId: String,
    regionName: String,
    folded: Boolean,
    readingFor: (MicroclimateStation) -> StateFlow<ObservationResult?>,
    /** Goes back to the station selected before this one; null when there is none (the card then shows no Back button). */
    onBack: (() -> Unit)? = null,
    /** Goes forward again to a station gone back from; null when there is none. */
    onForward: (() -> Unit)? = null
) {
    val reading by remember(station.id) { readingFor(station) }.collectAsStateWithLifecycle()
    val skyTemp = (reading as? ObservationResult.Reading)?.observation?.temperatureFahrenheit
    HeroTile(
        fullWidthPx = fullWidthPx,
        // The same colour as the open card: the plain surface, white in the light look, a tenth see-through (STATION_TILE_OPACITY)
        tileColor = MaterialTheme.colorScheme.surface.copy(alpha = STATION_TILE_OPACITY),
        tileBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        // The card draws its own border and its own background (at the same opacity), so nothing is stacked behind it: a second
        // layer under it would make the two add up to nearly solid
        panelColor = Color.Transparent,
        openLabel = stringResource(R.string.station_card_show),
        closeLabel = stringResource(R.string.station_card_hide),
        // Stays open when another station is picked from the map, and the map stays tappable behind it
        passThrough = true,
        // Half the rounding of the other cards
        tileCornerRadius = 8.dp,
        // The open card grows to the left, to within 12dp of the screen's edge, and not to the right: the map's stations stay in view beside it
        growLeftMarginDp = 12.dp,
        // The Region Info sheet sits above this card: opening it folds the card
        forceClosed = folded,
        tile = { StationSummaryTile(station, reading = reading) },
        panel = { close ->
            StationCard(
                station = station,
                isSelected = true,
                reading = reading,
                // Tapping the card folds it back, like Less
                onSelect = close,
                compact = true,
                elevationNote = elevationNoteText(station.elevationMeters, regionId),
                // The region's lowest and highest heights, for the elevation spectrum
                regionRangeMeters = if (regionStations.isNotEmpty()) {
                    regionStations.minOf { it.elevationMeters } to regionStations.maxOf { it.elevationMeters }
                } else null,
                // Less folds the card back, at the right of its top row
                onLess = close,
                // The region's name stays at the top when the card is open
                regionName = regionName,
                containerAlpha = STATION_TILE_OPACITY,
                onBack = onBack,
                onForward = onForward,
                // The first line of the open card: the sky's icon and wording, darkened to read on white; none without a reading
                skyLine = skyTemp?.let { t ->
                    val sky = skyThemeFor(t)
                    SkyLine(sky.icon, stringResource(sky.conditionRes), lerp(sky.accent, Color.Black, 0.4f))
                }
            )
        }
    )
}

/** How solid the Home station tile and its open card are: 0.95, five percent see-through (0.9 let the map's dots show too clearly through the card). */
private const val STATION_TILE_OPACITY = 0.95f
