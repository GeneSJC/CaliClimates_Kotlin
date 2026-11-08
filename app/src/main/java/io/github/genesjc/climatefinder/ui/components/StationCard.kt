package io.github.genesjc.climatefinder.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.TerrainType
import io.github.genesjc.climatefinder.domain.Elevation
import io.github.genesjc.climatefinder.domain.Units
import io.github.genesjc.climatefinder.ui.text.elevationDescribeText
import io.github.genesjc.climatefinder.ui.text.labelRes
import io.github.genesjc.climatefinder.ui.text.terrainDescriptionText

@Composable
fun StationCard(
    station: MicroclimateStation,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    modifier: Modifier = Modifier,
    // Demos pass true to keep the built-in sample numbers; the real app shows live NWS readings.
    useMockReadings: Boolean = false,
    // A tighter layout for the Station Inspector under the maps: no description, one-line metrics
    compact: Boolean = false,
    // Compact only: the lowest and highest station heights of the region, in metres, for the elevation spectrum
    regionRangeMeters: Pair<Int, Int>? = null,
    // Compact only: what the card's Less control does; no control is shown when it is null
    onLess: (() -> Unit)? = null,
    // Compact only: the region's name, as a small line under the station's name
    regionName: String? = null,
    // Compact only: a sentence about the height, shown under the terrain row
    elevationNote: String? = null,
    // Full card: a short mark next to the terrain badge while the card is the selected one (the Station List)
    selectedLabel: String? = null,
    // The station's live reading, from its ViewModel; null while it is being checked
    reading: ObservationResult? = null,
    // Compact only: the sky's icon and wording ("Crisp Sub-Alpine"), as the card's first line; none when null
    skyLine: SkyLine? = null,
    // How solid the card's background is (1 is opaque); Home's open card is a tenth see-through
    containerAlpha: Float = 1f,
    // Compact only: goes back to the station selected before; the Back button shows under the fold arrow only when this is given
    onBack: (() -> Unit)? = null,
    // Compact only: goes forward again to a station gone back from; the Forward button shows beside Back only when this is given
    onForward: (() -> Unit)? = null
) {

    val display = if (useMockReadings) mockDisplay(station) else liveDisplay(reading)

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val terrainBadgeColor = when (station.terrain) {
        TerrainType.COASTAL -> Color(0xFF0284C7)
        TerrainType.RIDGE -> Color(0xFF7C3AED)
        TerrainType.VALLEY_BASIN -> Color(0xFFEA580C)
        TerrainType.URBAN_CANYON -> Color(0xFF475569)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        // The card is the plain surface colour, selected or not (white in the light look); the selection shows in the border
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = containerAlpha))
    ) {
        if (compact) CompactStationBody(station, terrainBadgeColor, display, regionRangeMeters, onLess, elevationNote, regionName, skyLine, onBack, onForward) else Column(modifier = Modifier.padding(16.dp)) {
            // Header row: the badge (and the selected mark) at the left take the room they need; the elevation text takes
            // what is left and aligns to the right, wrapping inside its own space, so the two never overlap
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = terrainBadgeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = stringResource(station.terrain.labelRes),
                            color = terrainBadgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (isSelected && selectedLabel != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "\u2713 $selectedLabel",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = elevationDescribeText(station.elevationMeters),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Station Title & Dual Temperature Readings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = station.microclimateNote ?: terrainDescriptionText(station.terrain, station.elevationMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = display.tempC?.let { stringResource(R.string.reading_temp_f, Units.fahrenheit(it)) } ?: "—",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = display.tempC?.let { stringResource(R.string.reading_temp_c, it) }
                            ?: display.status?.fullText().orEmpty(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Microclimate Metric Pills (Humidity, Wind, Solar in demos / Pressure for live readings)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricPill(label = stringResource(R.string.metric_humidity), value = display.humidityText())
                    MetricPill(label = stringResource(R.string.metric_wind), value = display.windText())
                    MetricPill(label = stringResource(display.thirdLabelRes), value = display.thirdText())
                    // The age of the reading, as a fourth column (its value wraps if it must)
                    if (display.tempC != null && display.status is CardStatus.Updated) {
                        MetricPill(
                            label = stringResource(R.string.metric_updated),
                            value = display.status.ageText(),
                            modifier = Modifier.widthIn(max = 76.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The refined card body, top to bottom: the terrain badge and the height band with the Less control; the
 * station's name on its own line; the temperature with the reading's age; then three sections that fold: the
 * readings as tiles, where the station stands in the region's heights, and a note about the height.
 */
@Composable
private fun CompactStationBody(
    station: MicroclimateStation,
    badgeColor: Color,
    display: CardDisplay,
    regionRangeMeters: Pair<Int, Int>? = null,
    onLess: (() -> Unit)? = null,
    elevationNote: String? = null,
    regionName: String? = null,
    skyLine: SkyLine? = null,
    onBack: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null
) {
    // Which sections are open: the readings are, the height and the note start folded, so more of the map shows
    var readingsOpen by rememberSaveable { mutableStateOf(true) }
    var spectrumOpen by rememberSaveable { mutableStateOf(false) }
    var noteOpen by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 2.dp, bottom = 12.dp)) {
        // Row 1: the station's name, whole, and the card's fold arrow at the right (no word, so the name has the room)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = station.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontSize = ms(17f),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            )
            if (onLess != null) {
                CardToggle(
                    label = "\u25B2",
                    onClickLabel = stringResource(R.string.station_card_hide),
                    onClick = onLess
                )
            } else {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        // The region the station is in, as a small line under its name, with the Back button at the right (under the fold arrow)
        if (regionName != null || onBack != null || onForward != null) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = regionName.orEmpty(),
                    fontSize = ms(12f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp)
                )
                // Both slots always take their room, so Back stays put whether or not Forward is showing
                CardNavButton(R.string.station_back_button, R.string.station_card_back, onBack)
                CardNavButton(R.string.station_forward_button, R.string.station_card_forward, onForward)
            }
        }
        // Row 2: the terrain badge and the height band
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TerrainBadge(text = stringResource(station.terrain.labelRes), color = badgeColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Elevation.band(station.elevationMeters).labelRes),
                fontSize = ms(11f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        // Row 3: the temperature in °F, with °C beside it, and the reading's age at the right
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = display.tempC?.let { stringResource(R.string.reading_temp_f, Units.fahrenheit(it)) } ?: "\u2014",
                fontSize = ms(24f),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = display.tempC?.let { stringResource(R.string.reading_temp_c, it) }
                    ?: display.status?.fullText().orEmpty(),
                fontSize = ms(12f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (display.tempC != null && display.status != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = display.status.fullText(),
                    fontSize = ms(10f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp, start = 8.dp)
                )
            }
        }
        // Under the temperature: the sky's icon and wording (what the Profile view's badge used to say), small, in the sky's colour
        if (skyLine != null) {
            Row(modifier = Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                skyLine.icon?.let {
                    Text(text = it, fontSize = ms(12f), maxLines = 1)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(text = skyLine.text, fontSize = ms(11f), fontWeight = FontWeight.SemiBold, color = skyLine.color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // Row 4: Humidity, Wind and Pressure, three tiles of one height; folds to one line of values
        Box(modifier = Modifier.padding(top = 4.dp, end = 6.dp)) {
            ReadingsSection(
                summary = listOf(display.humidityText(), display.windText(), display.thirdText()).joinToString(" \u00B7 "),
                open = readingsOpen,
                onToggle = { readingsOpen = !readingsOpen }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTile(stringResource(R.string.metric_humidity), display.humidityText(), Color(0xFF3B82F6), Modifier.weight(1f).fillMaxHeight())
                    MetricTile(stringResource(R.string.metric_wind), display.windText(), Color(0xFF14B8A6), Modifier.weight(1f).fillMaxHeight())
                    MetricTile(stringResource(display.thirdLabelRes), display.thirdText(), Color(0xFFF59E0B), Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        // Row 5: where this station stands among the region's heights; the height row folds the track
        if (regionRangeMeters != null) {
            Box(modifier = Modifier.padding(top = 2.dp, end = 6.dp)) {
                ElevationSpectrum(
                    stationMeters = station.elevationMeters,
                    minMeters = regionRangeMeters.first,
                    maxMeters = regionRangeMeters.second,
                    markerColor = badgeColor,
                    open = spectrumOpen,
                    onToggle = { spectrumOpen = !spectrumOpen }
                )
            }
        }
        // Row 6: the terrain's name and a sentence about the height; the name folds the sentence
        if (elevationNote != null) {
            Box(modifier = Modifier.padding(top = 6.dp, end = 6.dp)) {
                MicroclimateCallout(
                    terrainName = stringResource(station.terrain.labelRes),
                    color = badgeColor,
                    note = elevationNote,
                    open = noteOpen,
                    onToggle = { noteOpen = !noteOpen }
                )
            }
        }
    }
}

/** The summary tile's temperature colour: a dark forest green, so it stands apart from the station's name. */
private val TEMPERATURE_COLOR = Color(0xFF166534)

/**
 * How far the tile's temperature sits above the name's baseline: about half the height of the name's capital letters
 * (17sp type, capitals about 12sp tall), so the temperature's bottom is level with the middle of the first capital.
 */
private val TEMPERATURE_RAISE = 6.dp

/**
 * The summary of a station for the Home row, on one line: the temperature in °F (smaller, in its own colour, on the
 * raised beside the name), the station's name and a chevron at the right. The age of the reading is in the
 * full card that the chevron opens. When there is no reading, a dash shows where the temperature would be. The name
 * gives way (with an ellipsis) only when it really cannot fit; the temperature always shows.
 */
@Composable
fun StationSummaryTile(station: MicroclimateStation, reading: ObservationResult? = null) {
    val display = liveDisplay(reading)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // The temperature, smaller and in its own colour, sits close beside the name, raised so it reads as a top-left corner mark
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = display.tempC?.let { stringResource(R.string.reading_temp_f, Units.fahrenheit(it)) } ?: "\u2014",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TEMPERATURE_COLOR,
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline().offset(y = -TEMPERATURE_RAISE)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = station.displayName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline()
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "\u25BC",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

/** How current the card's reading is, or why there is none. The words are string resources, chosen where it is shown. */
private sealed interface CardStatus {
    /** A reading; [minutes] is its age, or null when the station gave no time. */
    class Updated(val minutes: Long?) : CardStatus
    object Checking : CardStatus
    object Failed : CardStatus
    object NoReading : CardStatus
}

/** What the card shows for the current conditions, as numbers. Null temperature means no reading to show. */
private class CardDisplay(
    val tempC: Double?,
    val humidityPercent: Double?,
    val windMph: Double?,
    @StringRes val thirdLabelRes: Int,
    /** W/m\u00B2 for the sample solar value, inHg for pressure. */
    val thirdValue: Double?,
    val thirdIsSolar: Boolean,
    val status: CardStatus?
) {
    @Composable
    fun humidityText(): String = humidityPercent?.let { stringResource(R.string.reading_humidity_value, it) } ?: "\u2014"

    @Composable
    fun windText(): String = windMph?.let { stringResource(R.string.reading_wind_value, it) } ?: "\u2014"

    @Composable
    fun thirdText(): String = thirdValue?.let {
        if (thirdIsSolar) stringResource(R.string.reading_solar_value, it) else stringResource(R.string.reading_pressure_value, it)
    } ?: "\u2014"
}

private fun mockDisplay(station: MicroclimateStation): CardDisplay {
    val r = station.currentReading
    return CardDisplay(
        tempC = r.temperatureCelsius,
        humidityPercent = r.humidityPercent.toDouble(),
        windMph = Units.mph(r.windSpeedKmh),
        thirdLabelRes = R.string.metric_solar,
        thirdValue = r.solarIrradianceWm2,
        thirdIsSolar = true,
        status = null
    )
}

/** What the card shows for the station's real NWS reading (null while it is being checked). */
private fun liveDisplay(result: ObservationResult?): CardDisplay {
    return when (val r = result) {
        is ObservationResult.Reading -> {
            val o = r.observation
            CardDisplay(
                tempC = o.temperatureCelsius,
                humidityPercent = o.humidityPercent,
                windMph = o.windSpeedKmh?.let { Units.mph(it) },
                thirdLabelRes = R.string.metric_pressure,
                thirdValue = o.pressureHpa?.let { Units.inHg(it) },
                thirdIsSolar = false,
                status = CardStatus.Updated(o.ageMinutes())
            )
        }
        else -> CardDisplay(
            tempC = null,
            humidityPercent = null,
            windMph = null,
            thirdLabelRes = R.string.metric_pressure,
            thirdValue = null,
            thirdIsSolar = false,
            status = when (r) {
                null -> CardStatus.Checking
                ObservationResult.Failed -> CardStatus.Failed
                else -> CardStatus.NoReading
            }
        )
    }
}

/** The status as a full line: "Updated 5 min ago", or why there is no reading. */
@Composable
private fun CardStatus.fullText(): String = when (this) {
    is CardStatus.Updated -> {
        val m = minutes
        when {
            m == null -> stringResource(R.string.reading_updated_unknown)
            m < 90 -> pluralStringResource(R.plurals.reading_updated_minutes, m.toInt(), m.toInt())
            else -> pluralStringResource(R.plurals.reading_updated_hours, (m / 60).toInt(), (m / 60).toInt())
        }
    }
    CardStatus.Checking -> stringResource(R.string.reading_checking)
    CardStatus.Failed -> stringResource(R.string.reading_failed)
    CardStatus.NoReading -> stringResource(R.string.reading_none)
}

/** Just the age, for the column under the "Updated" label: "5 min ago". */
@Composable
private fun CardStatus.Updated.ageText(): String {
    val m = minutes
    return when {
        m == null -> stringResource(R.string.reading_age_unknown)
        m < 90 -> pluralStringResource(R.plurals.reading_age_minutes, m.toInt(), m.toInt())
        else -> pluralStringResource(R.plurals.reading_age_hours, (m / 60).toInt(), (m / 60).toInt())
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
