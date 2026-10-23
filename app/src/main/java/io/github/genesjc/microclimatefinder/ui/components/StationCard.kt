package io.github.genesjc.microclimatefinder.ui.components

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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.annotation.StringRes
import io.github.genesjc.microclimatefinder.AppFeatures
import io.github.genesjc.microclimatefinder.R
import io.github.genesjc.microclimatefinder.data.local.LocalNewsRepository
import io.github.genesjc.microclimatefinder.data.remote.ObservationResult
import io.github.genesjc.microclimatefinder.domain.model.LocalNewsStory
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.Units
import io.github.genesjc.microclimatefinder.ui.text.elevationDescribeText
import io.github.genesjc.microclimatefinder.ui.text.terrainDescriptionText

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
    elevationNote: String? = null,
    // Full card: a short mark next to the terrain badge while the card is the selected one (the Station List)
    selectedLabel: String? = null,
    // The station's live reading, from its ViewModel; null while it is being checked
    reading: ObservationResult? = null
) {
    val context = LocalContext.current
    var showNewsDialog by remember { mutableStateOf(false) }

    val display = if (useMockReadings) mockDisplay(station) else liveDisplay(reading)

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
                // A light tint laid over the card colour, so the card stays fully solid (never see-through)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    .compositeOver(MaterialTheme.colorScheme.surface)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        if (compact) CompactStationBody(station, terrainBadgeColor, display, compactFooter, compactHeaderAction, elevationNote) else Column(modifier = Modifier.padding(16.dp)) {
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
                        text = station.name,
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
                                text = stringResource(R.string.news_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.news_subtitle, cleanCity),
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
                                text = pluralStringResource(R.plurals.news_story_count, newsStories.size, newsStories.size),
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
                                                text = pluralStringResource(R.plurals.news_story_count, newsStories.size, newsStories.size),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.news_source, station.id),
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
                                                text = stringResource(R.string.news_tap_to_open),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = stringResource(R.string.news_read_story),
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
                text = display.tempC?.let { stringResource(R.string.reading_temp_f, Units.fahrenheit(it)) } ?: "\u2014",
                fontSize = 24.sp,
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
            // The reading's age, on the temperature's line at the right, under the card's control (Less)
            if (display.tempC != null && display.status != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = display.status.fullText(),
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
            CompactMetric(stringResource(R.string.metric_humidity), display.humidityText())
            CompactMetric(stringResource(R.string.metric_wind), display.windText())
            CompactMetric(stringResource(display.thirdLabelRes), display.thirdText())
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
                        text = stringResource(station.terrain.labelRes),
                        color = badgeColor,
                        fontSize = ms(10f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = elevationDescribeText(station.elevationMeters),
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
fun StationSummaryTile(station: MicroclimateStation, reading: ObservationResult? = null) {
    val display = liveDisplay(reading)
    // One line: the temperature in °F, a pipe divider, the name, and the chevron at the right. The name gives way
    // (with an ellipsis) only when it really cannot fit; the temperature always shows
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = display.tempC?.let { stringResource(R.string.reading_temp_f, Units.fahrenheit(it)) } ?: "\u2014",
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
