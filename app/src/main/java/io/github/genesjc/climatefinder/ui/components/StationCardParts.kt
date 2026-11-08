package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.text.elevationFeetText
import androidx.compose.ui.res.stringResource

/*
 * The shared pieces of the refined station card: the same shapes, spacing and weights wherever a station is
 * shown. Sizes pass through ms() so they follow the map's one text scale.
 */

/** The small rounded badge: the terrain name, in the terrain's colour. */
@Composable
internal fun TerrainBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.14f)) {
        Text(
            text = text,
            color = color,
            fontSize = ms(10f),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/** The card's fold control, with a tap area of at least 48dp however small the label is. */
@Composable
internal fun CardToggle(label: String, onClickLabel: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = ms(11f),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

/** The card's Back button: a small pill under the fold arrow, with a tap area of at least 36dp tall, that goes to the station picked before. */
@Composable
internal fun CardBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = stringResource(R.string.station_card_back), role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.station_back_button),
            fontSize = ms(11f),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

/**
 * A row that folds the section under it: the [content] at the left, a small chevron at the right, and a tap area
 * of at least 40dp. The row stays when the section is folded, so what it says is what a folded section shows.
 */
@Composable
internal fun SectionToggleRow(open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .clickable(onClickLabel = stringResource(R.string.card_section_toggle), role = Role.Button, onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) { content() }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (open) "\u25B4" else "\u25BE",
            fontSize = ms(11f),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
    }
}

/** The readings as a section: a "Readings" row (with the values in one line while folded) over the three tiles. */
@Composable
internal fun ReadingsSection(summary: String, open: Boolean, onToggle: () -> Unit, tiles: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionToggleRow(open = open, onToggle = onToggle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.card_readings),
                    fontSize = ms(11f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (!open) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary,
                        fontSize = ms(11f),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        AnimatedVisibility(visible = open) { tiles() }
    }
}

/**
 * One reading as a small rounded tile: a coloured dot and the label, the number large, the unit under it. The
 * [value] is the formatted reading ("3.4 mph"); its last word, if it has one, is shown as the unit.
 */
@Composable
internal fun MetricTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val number = value.substringBeforeLast(' ')
    val unit = value.substringAfterLast(' ', "")
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = ms(10f),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = number,
            fontSize = ms(15f),
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                fontSize = ms(9f),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** The colours of the spectrum track, from the lowest ground to the highest. */
private val SPECTRUM_COLORS = listOf(Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFF6366F1))

/**
 * Where the station stands in its region's heights: a gradient track from the lowest station to the highest, a
 * marker at this station's place, and the station's own height written above it where nothing can clip it. The
 * height row folds the track; the height stays in view when it is folded.
 */
@Composable
internal fun ElevationSpectrum(stationMeters: Int, minMeters: Int, maxMeters: Int, markerColor: Color, open: Boolean, onToggle: () -> Unit) {
    val range = maxMeters - minMeters
    val fraction = if (range > 0) ((stationMeters - minMeters).toFloat() / range).coerceIn(0f, 1f) else 0f
    val markerSize = 16.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionToggleRow(open = open, onToggle = onToggle) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.card_station_elevation),
                    fontSize = ms(11f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = elevationFeetText(stationMeters),
                    fontSize = ms(13f),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
        AnimatedVisibility(visible = open) {
            Column(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(markerSize)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(SPECTRUM_COLORS))
                    )
                    Box(
                        modifier = Modifier
                            .offset(x = (maxWidth - markerSize) * fraction)
                            .size(markerSize)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = elevationFeetText(minMeters),
                        fontSize = ms(9f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = elevationFeetText(maxMeters),
                        fontSize = ms(9f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** A tinted note under the readings: the terrain's name as its title (which folds the note), and a sentence about the height. */
@Composable
internal fun MicroclimateCallout(terrainName: String, color: Color, note: String, open: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.25f)), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        SectionToggleRow(open = open, onToggle = onToggle, modifier = Modifier.heightIn(min = 32.dp)) {
            Text(
                text = stringResource(R.string.card_microclimate, terrainName),
                fontSize = ms(11f),
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(visible = open) {
            Text(
                text = note,
                fontSize = ms(11f),
                lineHeight = ms(14f),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}
