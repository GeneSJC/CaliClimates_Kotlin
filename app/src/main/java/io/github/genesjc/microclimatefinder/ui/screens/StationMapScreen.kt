package io.github.genesjc.microclimatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.microclimatefinder.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.microclimatefinder.ui.map.StationMapViewModel
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.ui.components.MapExpandToggle
import io.github.genesjc.microclimatefinder.ui.components.TerrainLegend
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerButton
import io.github.genesjc.microclimatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.microclimatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.microclimatefinder.ui.components.StationCard
import io.github.genesjc.microclimatefinder.ui.text.datasetHeaderText

@Composable
fun StationMapScreen(
    dataset: StationDatasetInfo,
    selectedRegionId: String,
    onSelectRegion: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** True when shown inside another screen (the Home tabs): no app bar and no region picker of its own. */
    embedded: Boolean = false,
    /** A station selected elsewhere (the other Home tab), and a way to report selections back. */
    sharedSelectedStationId: String? = null,
    onStationSelected: ((String) -> Unit)? = null,
    /** The dot size, when Home owns it (its Terrain Legend panel holds the slider). */
    sharedPinScale: Float? = null,
    /** The zoom to start at, and a report of each change (Home keeps the zoom when it switches views). */
    initialZoom: Float = 1f,
    onZoomChange: ((Float) -> Unit)? = null,
    /** Whether the slider shows, when Home owns it (one setting for both map views), and a report of changes. */
    sliderShownShared: Boolean? = null,
    onSliderShownChange: ((Boolean) -> Unit)? = null,
    /** Home: where the slider's arrow button starts (below the view buttons, which are measured there). */
    sliderToggleTop: Dp = 52.dp,
    /** Home: 0 normally, 1 when the map has grown out into the page margins (the Elevation view); eased between. */
    expandT: Float = 0f,
    /** Home: told whether the selected station is out of the visible map (panned away or zoomed past it). */
    onSelectedOffscreenChange: ((Boolean) -> Unit)? = null,
    /** Home: bumped to ask the map to bring the selected station back to the middle of the view. */
    recenterTick: Int = 0,
    viewModel: StationMapViewModel = viewModel(),
) {
    val stations by remember(dataset) { viewModel.stations(dataset) }.collectAsStateWithLifecycle()

    // The selected station can be shared with another screen (Home keeps it across its two tabs)
    // The region's terrain picture, loaded off the main thread; the map shows a plain base until it arrives
    val terrain by remember(dataset.regionId) { viewModel.terrain(dataset.regionId) }.collectAsStateWithLifecycle()
    // Keyed to the data, so a region change starts from that region's first station and not the old region's id
    var localSelectedId by remember(stations) { mutableStateOf(stations.firstOrNull()?.id) }
    val selectedStationId = stations.firstOrNull { it.id == sharedSelectedStationId }?.id ?: localSelectedId
    var localPinScale by remember { mutableStateOf(DEFAULT_PIN_SCALE) }
    val pinScale = sharedPinScale ?: localPinScale

    val selectedStation = stations.find { it.id == selectedStationId } ?: stations.firstOrNull()
    val pickerState = rememberRegionPickerState()
    // The map starts at its regular height; the toggle on the map stretches it, slowly, to fill the screen
    var localExpanded by remember { mutableStateOf(false) }
    // On Home the map always fills the space; the standalone screen keeps the Expand / Shrink toggle
    val mapExpanded = embedded || localExpanded
    val pageScroll = rememberScrollState()
    val density = LocalDensity.current
    var aboveMapPx by remember { mutableStateOf(0) }

    // Top and bottom padding of the page: the embedded map has no app bar, so less above it
    // Embedded: just the 5 dp left under the map, which goes to 0 as the map grows out to the screen bottom
    val mapVerticalPadding = if (embedded) 5.dp * (1f - expandT) else 32.dp
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    // Expanded height: the screen less the padding and everything above the map
    val expandedMapHeight = with(density) {
        ((maxHeight - mapVerticalPadding).roundToPx() - aboveMapPx).toDp()
    }.coerceAtLeast(MAP_REGULAR_HEIGHT)
    val mapHeight by animateDpAsState(
        targetValue = if (mapExpanded) expandedMapHeight else MAP_REGULAR_HEIGHT,
        animationSpec = tween(MAP_RESIZE_MS, easing = FastOutSlowInEasing),
        label = "mapHeight"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (embedded) PaddingValues(top = 0.dp, bottom = 5.dp * (1f - expandT)) else PaddingValues(16.dp))
            .verticalScroll(pageScroll)
    ) {
      Column(modifier = Modifier.onSizeChanged { aboveMapPx = it.height }) {
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
                    text = stringResource(R.string.tab_topography),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = datasetHeaderText(dataset),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            RegionPickerButton(state = pickerState, selectedRegionId = selectedRegionId)
        }

        Spacer(modifier = Modifier.height(14.dp))
        }

        // The selected station card; embedded, Home shows it above the tabs
        if (!embedded) {
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
                        text = stringResource(R.string.map_hint_topography),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        }

        // 2. Terrain legend: one line until opened, then this region's terrain types with counts.
        // Embedded, Home shows it beside the Region Info link.
        if (!embedded) {
        TerrainLegend(
            stations = stations,
            // Dot size lives here, out of the way, instead of in a row under the map
            extra = { PinSizeSlider(value = pinScale, onValueChange = { localPinScale = it }) }
        )

        Spacer(modifier = Modifier.height(6.dp))
        }
      }

        // 3. The map with its dot-size slider: regular height, or the rest of the screen when expanded
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(mapHeight)
        ) {
            Surface(
                // The bottom corners square off as the map grows out to the screen edges
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp * (1f - expandT), bottomEnd = 16.dp * (1f - expandT)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                // A 1dp outline, for a crisper edge
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxSize()
            ) {
                SpatialMapCanvas(
                    stations = stations,
                    terrain = terrain,
                    cityName = viewModel::cityName,
                    embedded = embedded,
                    selectedStationId = selectedStationId,
                    pinScale = pinScale,
                    initialZoom = initialZoom,
                    onZoomChange = onZoomChange,
                    sliderToggleTop = sliderToggleTop,
                    expandT = expandT,
                    onSelectedOffscreenChange = onSelectedOffscreenChange,
                    recenterTick = recenterTick,
                    sliderShownShared = sliderShownShared,
                    onSliderShownChange = onSliderShownChange,
                    onSelectStation = {
                    localSelectedId = it.id
                    onStationSelected?.invoke(it.id)
                }
                )
            }
            // Expand / shrink toggle, just left of the top of the zoom slider (not on Home, where the map always fills the space)
            if (!embedded) {
            MapExpandToggle(
                expanded = mapExpanded,
                onToggle = {
                    localExpanded = !mapExpanded
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // Just left of the top of the zoom slider
                    .padding(end = 50.dp, top = 56.dp)
            )
            }
        }

    }
    if (!embedded) {
        RegionPickerOverlay(
            state = pickerState,
            selectedRegionId = selectedRegionId,
            onSelectRegion = onSelectRegion
        )
    }
    }
}

/** How much room the map leaves around the stations, as a fraction of their spread. */
internal const val FRAME_MARGIN = 0.08

/** The map's regular height, before it is expanded to fill the screen. */
private val MAP_REGULAR_HEIGHT = 300.dp

/** How long the map takes to grow to full height and back. */
private const val MAP_RESIZE_MS = 700

/** How far the map can be dragged past its edge, as a fraction of the map's size, at any zoom. */
internal const val MAP_OVERSCROLL = 0.12f

/** Dots start at the largest size the Dot size slider allows, so they are easy to see and tap. */
internal const val DEFAULT_PIN_SCALE = 2.5f
