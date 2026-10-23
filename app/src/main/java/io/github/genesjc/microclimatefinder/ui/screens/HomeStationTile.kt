package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.microclimatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import io.github.genesjc.microclimatefinder.ui.components.StationSummaryTile
import io.github.genesjc.microclimatefinder.ui.components.HeroTile
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.ui.text.elevationNoteText
import io.github.genesjc.microclimatefinder.ui.text.elevationRangeText
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The selected station as a half-width tile that grows into the full compact station card. */
@Composable
internal fun StationHeroTile(
    station: MicroclimateStation,
    fullWidthPx: Int,
    regionStations: List<MicroclimateStation>,
    regionId: String,
    folded: Boolean,
    readingFor: (MicroclimateStation) -> StateFlow<ObservationResult?>
) {
    val reading by remember(station.id) { readingFor(station) }.collectAsStateWithLifecycle()
    HeroTile(
        fullWidthPx = fullWidthPx,
        // The same colour as the open card: its tint over the page colour, not over the backdrop
        tileColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            .compositeOver(MaterialTheme.colorScheme.background),
        tileBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        // The card draws its own tint and border; this backs it so nothing shows through
        panelColor = MaterialTheme.colorScheme.background,
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
                compact = true,
                elevationNote = elevationNoteText(station.elevationMeters, regionId),
                // Inside the card's border: the region's elevation range, and the way to fold it back
                // The region's elevation range, at the right of the terrain chip's line
                compactFooter = {
                    if (regionStations.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.region_elevation_range, elevationRangeText(regionStations.minOf { it.elevationMeters }, regionStations.maxOf { it.elevationMeters })),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                // Less at the right of the title row, where the other open cards have theirs
                compactHeaderAction = {
                    Text(
                        text = stringResource(R.string.action_less),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClickLabel = stringResource(R.string.station_card_hide), role = Role.Button) { close() }
                            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            )
        }
    )
}
