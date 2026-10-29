package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.compositeOver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.climatefinder.ui.map.ElevationViewModel
import androidx.compose.ui.platform.LocalDensity
import io.github.genesjc.climatefinder.ui.components.MapExpandToggle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.data.local.StationDatasetInfo
import io.github.genesjc.climatefinder.ui.components.TerrainLegend
import io.github.genesjc.climatefinder.ui.components.StationCard
import io.github.genesjc.climatefinder.ui.text.datasetHeaderText

internal const val LANDMARK_COUNT = 6

// Chart margin in pixels; the Canvas and the label overlay must use the same value
internal const val CHART_PADDING_PX = 32f

// The profile sits between a little headroom and sea level; the bottom of the chart is ocean, so sea level
// is in view and the cities spread over more of the height
internal const val PROFILE_HEAD = 0.15f
// The slider's height on Home, as a share of the room below its arrow button (Topography uses 0.8)
internal const val ELEVATION_SLIDER_FRACTION = 0.64f

internal const val SEA_FRACTION = 0.126f

/** Where (0 at the chart's top, 1 at its bottom) a height, as a share of the highest station, is drawn. */
internal fun profileFrac(elevRatio: Float): Float = PROFILE_HEAD + (1f - elevRatio) * (1f - PROFILE_HEAD - SEA_FRACTION)



@Composable
fun ElevationProfileScreen(
    dataset: StationDatasetInfo,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when shown inside another screen (the Home tabs): no app bar of its own. */
    embedded: Boolean = false,
    /** A station selected elsewhere (the other Home tab), and a way to report selections back. */
    sharedSelectedStationId: String? = null,
    onStationSelected: ((String) -> Unit)? = null,
    /** The zoom to start at, and a report of each change (Home keeps the zoom when it switches views). */
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    /** Whether the slider shows, when Home owns it (one setting for both map views), and a report of changes. */
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    /** Home: 0 normally, 1 when the chart has grown out into the page margins (this view); eased between. */
    expandT: Float = 0f,
    /** A short note for the person (Home shows it as a snackbar), such as which view the layers button switched to. */
    onNotice: ((String) -> Unit)? = null,
    viewModel: ElevationViewModel = viewModel(),
) {
    val stations by remember(dataset) { viewModel.stations(dataset) }.collectAsStateWithLifecycle()

    // The region's terrain picture (the one the Topography map draws), used as a faint texture in the ground
    val terrain by remember(dataset.regionId) { viewModel.terrain(dataset.regionId) }.collectAsStateWithLifecycle()

    // The region's real skylines, for the distant ranges
    val skyline by remember(dataset.regionId) { viewModel.skyline(dataset.regionId) }.collectAsStateWithLifecycle()

    val layersOn by viewModel.layers.collectAsStateWithLifecycle()
    val layersOnNotice = stringResource(R.string.elevation_layers_on_notice)
    val layersOffNotice = stringResource(R.string.elevation_layers_off_notice)

    // The selected station can be shared with another screen (Home keeps it across its two tabs)
    var localSelectedId by remember(stations) { mutableStateOf(stations.firstOrNull()?.id) }
    val selectedStationId = stations.firstOrNull { it.id == sharedSelectedStationId }?.id ?: localSelectedId

    val selectedStation = stations.find { it.id == selectedStationId } ?: stations.firstOrNull()

    // The chart starts at its regular height; the toggle on it stretches it, slowly, to fill the screen
    var localExpanded by remember { mutableStateOf(false) }
    // On Home the chart always fills the space; the standalone screen keeps the Expand / Shrink toggle
    val chartExpanded = embedded || localExpanded
    var aboveChartPx by remember { mutableStateOf(0) }
    var belowChartPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    // Embedded: just the 5 dp left under the chart, which goes to 0 as it grows out to the screen bottom
    val verticalPadding = if (embedded) 5.dp * (1f - expandT) else 32.dp
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // Expanded height: the screen less the padding and everything above and below the chart
    val expandedHeight = with(density) {
        ((maxHeight - verticalPadding).roundToPx() - aboveChartPx - belowChartPx).toDp()
    }.coerceAtLeast(CHART_REGULAR_HEIGHT)
    val chartHeight by animateDpAsState(
        targetValue = if (chartExpanded) expandedHeight else CHART_REGULAR_HEIGHT,
        animationSpec = tween(CHART_RESIZE_MS, easing = FastOutSlowInEasing),
        label = "chartHeight"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (embedded) PaddingValues(top = 0.dp, bottom = 5.dp * (1f - expandT)) else PaddingValues(16.dp))
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.onSizeChanged { aboveChartPx = it.height }) {
        if (!embedded) {
        // App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onNavigateBack)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = stringResource(R.string.tab_elevation),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = datasetHeaderText(dataset),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = stringResource(R.string.elevation_plotted, stations.size),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        }
        }

        // Elevation chart, with its Expand / Shrink toggle
        Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            Surface(
                // The bottom corners square off as the chart grows out to the screen edges
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp * (1f - expandT), bottomEnd = 16.dp * (1f - expandT)),
                // An opaque base (the old tint, laid over the page colour) so the backdrop's relief does not show through the chart
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.background),
                // A 1dp outline, for a crisper edge
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxSize()
            ) {
                ElevationProfileCanvas(
                    stations = stations,
                    regionId = dataset.regionId,
                    terrain = terrain,
                    skyline = skyline,
                    cityName = viewModel::cityName,
                    knownReading = viewModel::knownReading,
                    fetchReading = viewModel::latestReading,
                    startVisit = viewModel::startVisit,
                    onLineTapped = viewModel::lineTapped,
                    embedded = embedded,
                    expandT = expandT,
                    layersOn = layersOn,
                    onToggleLayers = {
                        viewModel.toggleLayers()
                        onNotice?.invoke(if (layersOn) layersOffNotice else layersOnNotice)
                    },
                    selectedStationId = selectedStationId,
                    initialZoom = initialZoom,
                    onZoomChange = onZoomChange,
                    sliderShownShared = sliderShownShared,
                    onSliderShownChange = onSliderShownChange,
                    onSelectStation = {
                        localSelectedId = it.id
                        onStationSelected?.invoke(it.id)
                    }
                )
            }
            if (!embedded) {
            MapExpandToggle(
                expanded = chartExpanded,
                onToggle = {
                    localExpanded = !chartExpanded
                },
                // Just left of the top of the zoom slider, as on the Topography map
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 50.dp, top = 56.dp)
            )
            }
        }

        Column(modifier = Modifier.onSizeChanged { belowChartPx = it.height }) {
        if (!embedded) Spacer(modifier = Modifier.height(12.dp))

        // Terrain legend: one line until opened, then this region's terrain types with counts.
        // Embedded, Home shows it beside the Region Info link.
        if (!embedded) TerrainLegend(stations = stations)

        // The selected station card; embedded, Home shows it above the tabs
        if (!embedded) {
        Spacer(modifier = Modifier.height(16.dp))

        // Selected station card (the Station Inspector)

        if (selectedStation != null) {
            val reading by remember(selectedStation.id) { viewModel.reading(selectedStation) }.collectAsStateWithLifecycle()
            StationCard(
                station = selectedStation,
                isSelected = true,
                onSelect = { /* Already selected */ },
                compact = true,
                reading = reading
            )
        } else {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.map_hint_elevation),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        }
        }
    }
    }
}

/** The chart's regular height, before it is expanded to fill the screen. */
internal val CHART_REGULAR_HEIGHT = 300.dp

/** How long the chart takes to grow to full height and back. */
private const val CHART_RESIZE_MS = 700
