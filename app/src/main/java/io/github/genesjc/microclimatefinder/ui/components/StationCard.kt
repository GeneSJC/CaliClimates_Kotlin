package io.github.genesjc.microclimatefinder.ui.components

import io.github.genesjc.microclimatefinder.domain.Elevation
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.genesjc.microclimatefinder.AppFeatures
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.LocalNewsStory
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.Units
import java.util.Locale

@Composable
fun StationCard(
    station: MicroclimateStation,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    modifier: Modifier = Modifier,
    onOpenStory: ((LocalNewsStory) -> Unit)? = null,
    // Demos pass true to keep the built-in sample numbers; the real app shows live NWS readings.
    useMockReadings: Boolean = false,
    // A tighter layout for the Station Inspector under the maps: no description, one-line metrics
    compact: Boolean = false,
    // Compact only: a line at the bottom of the card, inside its border
    compactFooter: (@Composable () -> Unit)? = null,
    // Compact only: a control at the right end of the title row (the open card's Less link)
    compactHeaderAction: (@Composable () -> Unit)? = null,
    // Compact only: a sentence about the height, shown under the terrain row
    elevationNote: String? = null
) {
    val context = LocalContext.current
    var showNewsDialog by remember { mutableStateOf(false) }

    val display = if (useMockReadings) mockDisplay(station) else liveDisplay(station)

    val cleanCity = remember(station.id, station.name) {
        LocalNewsRepository.getCleanCityName(station.id, station.name)
    }

    val newsStories = remember(cleanCity) {
        if (AppFeatures.LOCAL_NEWS) LocalNewsRepository.getLocalStories(cleanCity) else emptyList()
    }

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
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        if (compact) CompactStationBody(station, terrainBadgeColor, display, compactFooter, compactHeaderAction, elevationNote) else Column(modifier = Modifier.padding(16.dp)) {
            // Header row: Terrain Badge & Elevation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = terrainBadgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = station.terrain.label,
                        color = terrainBadgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "${Elevation.describe(station.elevationMeters)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
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
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = station.microclimateDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.padding(horizontal = 4.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = display.tempC?.let { String.format(Locale.US, "%.1f°F", Units.fahrenheit(it)) } ?: "—",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = display.tempC?.let { String.format(Locale.US, "%.1f°C", it) }
                            ?: display.status.orEmpty(),
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
                    MetricPill(label = "Humidity", value = display.humidity)
                    MetricPill(label = "Wind", value = display.wind)
                    MetricPill(label = display.thirdLabel, value = display.thirdValue)
                    // The age of the reading, as a fourth column (its value wraps if it must)
                    if (display.tempC != null && display.status != null) {
                        MetricPill(
                            label = "Updated",
                            value = display.status.removePrefix("Updated").removePrefix(":").trim(),
                            modifier = Modifier.widthIn(max = 76.dp)
                        )
                    }
                }
            }

            if (AppFeatures.LOCAL_NEWS) {
            Spacer(modifier = Modifier.height(12.dp))

            // =========================================================================
            // "Local News" Section Button with Badge & Open Arrow (Opens Hero Dialog)
            // =========================================================================
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNewsDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📰",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local News",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$cleanCity • What's happening in this location",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${newsStories.size} stories",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Hero animation open indicator
                        Text(
                            text = "↗",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            }
        }
    }

    // =========================================================================
    // 90% Full Screen Hero Dialog for Local News Stories
    // Translucent backdrop with solid opaque inner article post containers for 100% crisp readability
    // =========================================================================
    if (AppFeatures.LOCAL_NEWS && showNewsDialog) {
        Dialog(
            onDismissRequest = { showNewsDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showNewsDialog = false },
                contentAlignment = Alignment.Center
            ) {
                // 90% Full-Screen Hero Dialog Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.90f)
                        .clickable(enabled = false) {}, // Prevent clicks inside dialog from closing it
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Dialog Header: Station/City Title, Count & Close 'X' Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("📰", fontSize = 18.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cleanCity,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${newsStories.size} stories",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Google News RSS • ${station.id} Area",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Close 'X' Button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { showNewsDialog = false }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "✕",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Scrollable Article Feed with 100% Opaque White/Dark Cards for Pristine Readability
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            newsStories.forEach { story ->
                                // Opaque Background Card with Rounded Corners and Crisp Spacing
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface, // 100% opaque background: no translucent interference!
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showNewsDialog = false
                                            if (onOpenStory != null) {
                                                onOpenStory(story)
                                            } else {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(story.url))
                                                context.startActivity(intent)
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = story.source,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = story.published,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Article Post Headline (High-contrast, bold, clear)
                                        Text(
                                            text = story.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 19.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (story.snippet.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = story.snippet,
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Tap to open in app viewer",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Read story",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "→",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The tighter card body, in three rows: the name with the temperature; Humidity, Wind and Pressure
 * with the °C value; then the terrain chip and elevation with the reading's age. About half the
 * height of the full card.
 */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun CompactStationBody(
    station: MicroclimateStation,
    badgeColor: Color,
    display: CardDisplay,
    footer: (@Composable () -> Unit)? = null,
    headerAction: (@Composable () -> Unit)? = null,
    elevationNote: String? = null
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        // Row 1: name, and the card's control (Less) at the right, like the other open cards
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                fontSize = ms(16f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (headerAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                headerAction()
            }
        }
        // Row 2: the temperature in °F, with °C (or why there is no reading) beside it
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = display.tempC?.let { String.format(Locale.US, "%.1f°F", Units.fahrenheit(it)) } ?: "\u2014",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = display.tempC?.let { String.format(Locale.US, "%.1f°C", it) }
                    ?: display.status.orEmpty(),
                fontSize = ms(12f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            // The reading's age, on the temperature's line at the right, under the card's control (Less)
            if (display.tempC != null && display.status != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = display.status,
                    fontSize = ms(10f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
        // Row 3: Humidity, Wind and Pressure
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CompactMetric("Humidity", display.humidity)
            CompactMetric("Wind", display.wind)
            CompactMetric(display.thirdLabel, display.thirdValue)
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Row 4: terrain chip and elevation on the left; the footer (the region's elevation range) on the right
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = station.terrain.label,
                        color = badgeColor,
                        fontSize = ms(10f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Elevation.describe(station.elevationMeters),
                    fontSize = ms(11f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (footer != null) {
                Spacer(modifier = Modifier.width(8.dp))
                footer()
            }
        }
        if (elevationNote != null) {
            Text(
                text = elevationNote,
                fontSize = ms(11f),
                lineHeight = ms(14f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun CompactMetric(label: String, value: String) {
    Row {
        Text(text = label, fontSize = ms(11f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            fontSize = ms(11f),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The summary of a station for the Home row: the temperature in °F, a pipe divider, then the name,
 * and a chevron on the right. The age of the reading is in the full card that the chevron opens. When
 * there is no reading, the reason shows where the °C would be.
 */
@Composable
fun StationSummaryTile(station: MicroclimateStation) {
    val display = liveDisplay(station)
    // One line: the temperature in °F, a pipe divider, the name, and the chevron at the right. The name gives way
    // (with an ellipsis) only when it really cannot fit; the temperature always shows
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = display.tempC?.let { String.format(Locale.US, "%.1f°F", Units.fahrenheit(it)) } ?: "\u2014",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "|",
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = station.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
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

/** What the card shows for the current conditions. Null temperature means no reading to show. */
private class CardDisplay(
    val tempC: Double?,
    val humidity: String,
    val wind: String,
    val thirdLabel: String,
    val thirdValue: String,
    val status: String?
)

private fun mockDisplay(station: MicroclimateStation): CardDisplay {
    val r = station.currentReading
    return CardDisplay(
        tempC = r.temperatureCelsius,
        humidity = "${r.humidityPercent}%",
        wind = String.format(Locale.US, "%.1f mph", Units.mph(r.windSpeedKmh)),
        thirdLabel = "Solar",
        thirdValue = String.format(Locale.US, "%.0f W/m²", r.solarIrradianceWm2),
        status = null
    )
}

/** Real NWS reading for the station: from the prefetch cache when present, else fetched now. */
@Composable
private fun liveDisplay(station: MicroclimateStation): CardDisplay {
    var result by remember(station.id) { mutableStateOf(LiveObservationCache.peek(station.id)) }
    LaunchedEffect(station.id) {
        result = if (station.canHaveLiveReading) LiveObservationCache.get(station.id) else ObservationResult.NoReading
    }
    return when (val r = result) {
        is ObservationResult.Reading -> {
            val o = r.observation
            val age = o.ageMinutes()
            CardDisplay(
                tempC = o.temperatureCelsius,
                humidity = o.humidityPercent?.let { String.format(Locale.US, "%.0f%%", it) } ?: "—",
                wind = o.windSpeedKmh?.let { String.format(Locale.US, "%.1f mph", Units.mph(it)) } ?: "—",
                thirdLabel = "Pressure",
                thirdValue = o.pressureHpa?.let { String.format(Locale.US, "%.2f inHg", Units.inHg(it)) } ?: "—",
                status = when {
                    age == null -> "Updated: time unknown"
                    age < 90 -> "Updated $age min ago"
                    else -> "Updated ${age / 60} h ago"
                }
            )
        }
        else -> CardDisplay(
            tempC = null,
            humidity = "—",
            wind = "—",
            thirdLabel = "Pressure",
            thirdValue = "—",
            status = when (r) {
                null -> "Checking…"
                ObservationResult.Failed -> "Couldn't load reading"
                else -> "No current reading"
            }
        )
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
