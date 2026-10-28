package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import kotlinx.coroutines.delay

/**
 * The About dialog: a coloured header with the app's name, then one short section for each thing the app
 * does (live readings, terrain, elevation, regions), which fade in one after another.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutDialog(appName: String, buildLabel: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.88f).dp)
        ) {
            Column {
                // Header: a wash of the app's colours, the state icon and the name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StateUsaOutlineIcon(
                            fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            stroke = MaterialTheme.colorScheme.onPrimaryContainer,
                            regionColors = regionPickerColors(),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.padding(end = 28.dp)) {
                            Text(
                                text = appName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(LocalStateUsa.current.taglineRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.build_label, buildLabel),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Close: an X at the top right, where the eye already is (a tap outside the dialog closes it too)
                    val closeColor = MaterialTheme.colorScheme.onPrimaryContainer
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClickLabel = stringResource(R.string.action_close), role = Role.Button, onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val stroke = 2.4.dp.toPx()
                            val inset = stroke / 2f
                            drawLine(closeColor, Offset(inset, inset), Offset(size.width - inset, size.height - inset), stroke, StrokeCap.Round)
                            drawLine(closeColor, Offset(size.width - inset, inset), Offset(inset, size.height - inset), stroke, StrokeCap.Round)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Reveal(0) {
                        AboutSection(
                            icon = "📡",
                            title = stringResource(R.string.about_live_title),
                            accent = Color(0xFF16A34A),
                            badge = { LiveBadge() }
                        ) {
                            Text(
                                text = stringResource(R.string.about_live_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Reveal(1) {
                        AboutSection(icon = "🗺️", title = stringResource(R.string.about_terrain_title), accent = Color(0xFFB8860B)) {
                            Text(
                                text = stringResource(R.string.about_terrain_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Reveal(2) {
                        AboutSection(icon = "⛰️", title = stringResource(R.string.about_elevation_title), accent = Color(0xFF7C3AED)) {
                            Text(
                                text = stringResource(R.string.about_elevation_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BandBar()
                        }
                    }
                    Reveal(3) {
                        AboutSection(icon = "🧭", title = stringResource(R.string.about_regions_title), accent = Color(0xFF2563EB)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LocalStateUsa.current.regions.forEach { region ->
                                    val color = Color(android.graphics.Color.parseColor(region.colorHex))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = color.copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, color.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = stringResource(region.nameRes),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Reveal(4) {
                        Text(
                            text = stringResource(R.string.about_disclaimer),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

/** Fades its content in and lifts it a little, one after another by [index]. */
@Composable
private fun Reveal(index: Int, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150L + index * 130L)
        shown = true
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "aboutReveal"
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 28f
        }
    ) { content() }
}

/** One section: a tinted icon tile, a title, an optional badge at the right, and the body under them. */
@Composable
private fun AboutSection(
    icon: String,
    title: String,
    accent: Color,
    badge: (@Composable () -> Unit)? = null,
    body: @Composable () -> Unit
) {
    Row {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Text(text = icon, fontSize = 20.sp) }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                badge?.invoke()
            }
            Spacer(modifier = Modifier.height(3.dp))
            body()
        }
    }
}

/** A small "LIVE" tag with a gently pulsing dot. */
@Composable
private fun LiveBadge() {
    val pulse = rememberInfiniteTransition(label = "live")
    val glow by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveGlow"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).alpha(glow).clip(CircleShape).background(Color(0xFF16A34A)))
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.about_live_badge),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF16A34A)
        )
    }
}

/** The six height bands as one coloured bar, from near sea level to high country. */
@Composable
private fun BandBar() {
    val colors = listOf(
        Color(0xFF7FD1C7), Color(0xFF9BCB7A), Color(0xFFD7C56B),
        Color(0xFFC9A266), Color(0xFFA88B75), Color(0xFFE6E2EE)
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
            colors.forEach { Box(Modifier.weight(1f).height(10.dp).background(it)) }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.elevation_band_near_sea_level), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.elevation_band_foothills), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.elevation_band_high_country), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
