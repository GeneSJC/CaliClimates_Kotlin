package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import io.github.genesjc.climatefinder.ui.components.StationCard
import io.github.genesjc.climatefinder.ui.components.StationSummaryTile
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
    readingFor: (MicroclimateStation) -> StateFlow<ObservationResult?>
) {
    val reading by remember(station.id) { readingFor(station) }.collectAsStateWithLifecycle()
    HeroTile(
        fullWidthPx = fullWidthPx,
        // The same colour as the open card: the plain surface, white in the light look
        tileColor = MaterialTheme.colorScheme.surface,
        tileBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        // The card draws its own border; this backs it so nothing shows through
        panelColor = MaterialTheme.colorScheme.surface,
        openLabel = stringResource(R.string.station_card_show),
        closeLabel = stringResource(R.string.station_card_hide),
        // Stays open when another station is picked from the map, and the map stays tappable behind it
        passThrough = true,
        // Half the rounding of the other cards
        tileCornerRadius = 8.dp,
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
                elevationNote = elevationNoteText(station.elevationMeters, regionId),
                // The region's lowest and highest heights, for the elevation spectrum
                regionRangeMeters = if (regionStations.isNotEmpty()) {
                    regionStations.minOf { it.elevationMeters } to regionStations.maxOf { it.elevationMeters }
                } else null,
                // Less folds the card back, at the right of its top row
                onLess = close,
                // The region's name stays at the top when the card is open
                regionName = regionName
            )
        }
    )
}
