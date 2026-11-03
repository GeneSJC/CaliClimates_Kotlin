package io.github.genesjc.climatefinder.ui.screens

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import io.github.genesjc.climatefinder.ui.components.drawFolderTab
import io.github.genesjc.climatefinder.ui.components.PICKER_BUTTON_HEIGHT
import io.github.genesjc.climatefinder.ui.components.RegionPickerChip
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import io.github.genesjc.climatefinder.ui.components.MapViewButton
import io.github.genesjc.climatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.climatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import io.github.genesjc.climatefinder.ui.components.TOP_NOTICE_MS
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import io.github.genesjc.climatefinder.domain.model.ObservationResult
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.climatefinder.ui.components.AboutDialog
import io.github.genesjc.climatefinder.ui.navigation.AppScreen
import kotlinx.coroutines.launch

/** One row in the quick region picker. */
/** The height of the three header widgets on Home. */
/**
 * The region name bar (the name with its drop-down, and the region picker) is parked while the station tile takes the top of
 * the screen; true brings it back above the tile. The picker button itself stays in the station row either way.
 */
private const val SHOW_REGION_BAR = false

/** The height of the region name bar, when it shows. */
internal val HEADER_HEIGHT = 42.dp

data class RegionOption(val id: String, @StringRes val nameRes: Int, val stationCount: Int?)


@Composable
fun LandingScreen(
    selectedRegionId: String,
    dataset: StationDatasetInfo?,
    fullDataRegions: Int,
    totalRegions: Int,
    /** The build label for the drawer and the About dialog; the screen does not read the build constants itself. */
    buildLabel: String,
    regionOptions: List<RegionOption>,
    onSelectRegion: (String) -> Unit,
    onNavigateTo: (AppScreen) -> Unit,
    infoOpen: Boolean,
    onInfoOpenChange: (Boolean) -> Unit,
    homeTab: Int,
    onHomeTabChange: (Int) -> Unit,
    selectedStationId: String?,
    onStationSelected: (String) -> Unit,
    pinScale: Float,
    onPinScaleChange: (Float) -> Unit,
    sliderShown: Boolean,
    onSliderShownChange: (Boolean) -> Unit,
    /** The map zoom shared by both map views: read when a map is shown, reported as it changes. */
    mapZoom: () -> Float,
    onMapZoomChange: (Float) -> Unit,
    /** The stations of a region, for the drop-down, Terrain Legend and station card. */
    stationsForRegion: (String) -> List<MicroclimateStation>,
    /** The live reading of a station, for its tile and card; null while it is being checked. */
    readingFor: (MicroclimateStation) -> StateFlow<ObservationResult?>,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val stateUsa = LocalStateUsa.current
    val region = remember(stateUsa, selectedRegionId) {
        stateUsa.region(selectedRegionId) ?: stateUsa.defaultRegion()
    }
    val hasStationData = dataset != null
    val snapshotStations = remember(dataset) {
        if (dataset != null) stationsForRegion(selectedRegionId) else emptyList()
    }
    val appName = stringResource(R.string.app_name)
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLimitedDataDialog by remember { mutableStateOf(false) }

    val pickerState = rememberRegionPickerState()

    // Region info sheet: slides in from the right over the start screen
    // The open state lives with the caller, so the sheet is still open when Back returns from the Station List
    val infoReveal = remember { Animatable(if (infoOpen) 1f else 0f) }
    // Where the sheet starts: the top of the map (or, with no map, just under the link)
    var mapTopPx by remember { mutableStateOf(0f) }
    // The Topography / Elevation buttons' height (it follows the text size), measured
    val mapDensity = LocalDensity.current
    // Both views fill the screen: the map grows out into the page margins (west, east and south) and has no outline or rounded
    // corners. The views use the same value for the geometry the expanded Elevation view always had; it is no longer eased
    // when switching views because there is nothing to ease
    val expandT = 1f
    // Reported by the Topography map: is the selected station out of view; and a counter the locate button bumps
    var selectedOffscreen by remember { mutableStateOf(false) }
    var recenterTick by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    // A note near the top of the screen (a counter and the words, so the same words twice still restart the timer);
    // it clears itself after a moment
    var notice by remember { mutableStateOf<Pair<Int, String>?>(null) }
    LaunchedEffect(notice) {
        if (notice != null) {
            kotlinx.coroutines.delay(TOP_NOTICE_MS)
            notice = null
        }
    }
    var rowWidthPx by remember { mutableStateOf(0) }
    var stationTileTopPx by remember { mutableStateOf(0f) }
    // Where the menu button's top edge is, so the menu panel opens level with it (0 until it has been placed)
    val sheetTopPx = stationTileTopPx
    val openInfo: () -> Unit = {
        onInfoOpenChange(true)
        scope.launch { infoReveal.animateTo(1f, tween(SHEET_OPEN_MS, easing = FastOutSlowInEasing)) }
    }
    val closeInfo: () -> Unit = {
        onInfoOpenChange(false)
        scope.launch { infoReveal.animateTo(0f, tween(SHEET_CLOSE_MS, easing = FastOutSlowInEasing)) }
    }
    BackHandler(enabled = infoOpen) { closeInfo() }
    // Region Info is a Topography feature: switching to Elevation closes it
    LaunchedEffect(homeTab) { if (homeTab == 1 && infoOpen) closeInfo() }
    run {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // The region name bar is parked for now (the station tile takes the top); the region picker is in the station row
            // A region with no station data has no map or tile, so the bar stays there: it is the only way to change region
            if (SHOW_REGION_BAR || dataset == null) {
                Spacer(modifier = Modifier.height(14.dp))
                HomeRegionBar(region = region, regionOptions = regionOptions, selectedRegionId = selectedRegionId, pickerState = pickerState, onSelectRegion = onSelectRegion)
                Spacer(modifier = Modifier.height(8.dp))
            }
            val shown = if (dataset != null) {
                snapshotStations.firstOrNull { it.id == selectedStationId } ?: snapshotStations.firstOrNull()
            } else null
            // Read here, because the locate button's click handler is not composable
            val centeringMessage = stringResource(
                R.string.map_centering_on,
                shown?.name ?: stringResource(R.string.map_selected_station)
            )
            run {
                Column {
                    // This region has no station data in this build
                    if (dataset == null) {
                        // No map to hold the buttons: the menu and About sit here
                        Spacer(modifier = Modifier.height(6.dp))
                        InfoControlButton(open = false, onClick = { showAboutDialog = true }, label = stringResource(R.string.menu_about))
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    onClickLabel = stringResource(R.string.limited_data_about),
                                    role = Role.Button
                                ) { showLimitedDataDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.limited_data_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "i",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (!hasStationData) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.no_data_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.no_data_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // The map, with small buttons on it to switch between its Topography and Elevation views.
            // The standalone screens are unchanged; these are the same screens without their own app bar.
            if (dataset != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        // Wider than the page by the margin on each side, in step with expandT (the page margin is 5 dp)
                        .layout { measurable, c ->
                            val e = (5.dp * expandT).roundToPx()
                            val w = c.maxWidth + 2 * e
                            val placeable = measurable.measure(c.copy(minWidth = w, maxWidth = w))
                            layout(c.maxWidth, placeable.height) { placeable.place(-e, 0) }
                        }
                        .onGloballyPositioned { mapTopPx = it.positionInRoot().y }
                ) {
                    if (homeTab == 0) {
                        StationMapScreen(
                            dataset = dataset,
                            selectedRegionId = selectedRegionId,
                            onSelectRegion = onSelectRegion,
                            onNavigateBack = {},
                            embedded = true,
                            sharedSelectedStationId = selectedStationId,
                            onStationSelected = onStationSelected,
                            sharedPinScale = pinScale,
                            initialZoom = mapZoom(),
                            onZoomChange = onMapZoomChange,
                            sliderShownShared = sliderShown,
                            onSliderShownChange = onSliderShownChange,
                            // The slider's arrow button sits at the top right of the map, level with the control row at the left
                            sliderToggleTop = 12.dp + HOME_TILE_BAND,
                            expandT = expandT,
                            onSelectedOffscreenChange = { selectedOffscreen = it },
                            recenterTick = recenterTick
                        )
                    } else {
                        ElevationProfileScreen(
                            dataset = dataset,
                            onNavigateBack = {},
                            embedded = true,
                            sharedSelectedStationId = selectedStationId,
                            onStationSelected = onStationSelected,
                            initialZoom = mapZoom(),
                            onZoomChange = onMapZoomChange,
                            sliderShownShared = sliderShown,
                            onSliderShownChange = onSliderShownChange,
                            expandT = expandT,
                            onNotice = { message -> notice = (notice?.first ?: 0) + 1 to message },
                            notice = notice?.second
                        )
                    }
                    // The tile and the buttons keep their place on the screen while the map grows out underneath them
                    Box(modifier = Modifier.matchParentSize().padding(horizontal = 5.dp * expandT)) {
                        // The folder's tab (the region's name) and the first line's height, measured, so the shape fits them
                        var folderTabWidth by remember { mutableStateOf(0f) }
                        var folderTabHeight by remember { mutableStateOf(0f) }
                        // The folder is the green the map shows beyond its terrain, so the header and the map under it read as one
                        // piece of paper with a tab; the space right of the tab is left open
                        // A little see-through (88 percent opaque, just under the buttons' 90 percent), so terrain shows faintly through
                        val folderFill = (if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFE4EBDA) else MaterialTheme.colorScheme.background)
                            .copy(alpha = FOLDER_OPACITY)
                        val folderBorder = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        // The selected station as a tile across the top of the map (it grows into the full card)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp, top = 5.dp)
                                // The header is drawn as a folder: the region's name on the tab at the top left, the station tile and
                                // the picker in the body, and the space right of the tab open to the map (the Map and Profile buttons
                                // sit there). A draw, not a clip, so the card that opens from the tile is never cut off
                                .drawBehind {
                                    drawFolderTab(
                                        fill = folderFill,
                                        border = folderBorder,
                                        tabWidth = folderTabWidth,
                                        tabHeight = folderTabHeight,
                                        radius = 10.dp.toPx(),
                                        join = 12.dp.toPx(),
                                        borderWidth = 1.dp.toPx()
                                    )
                                }
                                // The Region Info sheet opens below this strip, so it never covers the tile
                                .onGloballyPositioned { stationTileTopPx = it.positionInRoot().y + it.size.height + with(mapDensity) { 6.dp.toPx() } }
                                // 4dp at the sides, a little under the tile (5dp: 70 percent less than before); the room above is the first line's
                                .padding(start = 4.dp, top = 0.dp, end = 4.dp, bottom = 5.dp)
                                .onSizeChanged { rowWidthPx = it.width },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // The first line: the folder's tab with the region's name (two lines at most) at the left, and the Map
                                // and Profile buttons at the right, on the map
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { folderTabHeight = it.height.toFloat() }
                                        .padding(top = 6.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // The tab's text is centred on the tab; the buttons sit at the top of the line
                                    Box(
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .align(Alignment.CenterVertically)
                                            .onSizeChanged { folderTabWidth = it.width.toFloat() }
                                    ) {
                                        Text(
                                            text = stringResource(region.nameRes),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 12.dp, end = 36.dp)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.align(Alignment.Top).padding(start = 8.dp, end = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        MapViewButton(label = stringResource(R.string.tab_topography), index = 0, selected = homeTab, onSelect = onHomeTabChange)
                                        MapViewButton(label = stringResource(R.string.tab_elevation), index = 1, selected = homeTab, onSelect = onHomeTabChange)
                                    }
                                }
                                // A margin between the folder's top edge and the station tile, so the tile sits inside the outline
                                Spacer(modifier = Modifier.height(6.dp))
                                // Under it, one row: the station tile at the left, taking all the width the picker leaves, and the
                                // region picker at the right. The tile fills the height it is given, so the row is held to its
                                // content's height (without this the tile fills the whole map)
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        if (shown != null) StationHeroTile(shown, fullWidthPx = rowWidthPx, regionStations = snapshotStations, regionId = region.id, regionName = stringResource(region.nameRes), folded = infoOpen || pickerState.visible, readingFor = readingFor)
                                    }
                                    RegionPickerChip(
                                        state = pickerState,
                                        selectedRegionId = selectedRegionId,
                                        modifier = Modifier.align(Alignment.CenterVertically).height(PICKER_BUTTON_HEIGHT)
                                    )
                                }
                            }
                        }
                        // The Region Info button, under the north indicator at the left (Topography only: Elevation has no Region Info)
                        if (homeTab == 0) {
                            InfoControlButton(
                                open = infoOpen,
                                onClick = { if (infoOpen) closeInfo() else openInfo() },
                                modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp + HOME_TILE_BAND + 40.dp)
                            )
                        }
                        // Below the tile, at the left: the menu, the north indicator and the About button
                        MapControlRow(
                            onElevation = homeTab == 1,
                            stations = snapshotStations,
                            landmarkIds = stateUsa.landmarkStations[region.id] ?: emptyList(),
                            selectedStationId = selectedStationId,
                            onPickStation = onStationSelected,
                            showLocate = selectedOffscreen && homeTab == 0,
                            onLocate = {
                                recenterTick++
                                // A short note at the bottom says what the button just did, so its meaning is learned in use
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = centeringMessage,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp + HOME_TILE_BAND)
                        )
                    }
                }
            }
        }

        InfoSideSheet(reveal = infoReveal, topPx = sheetTopPx, onOpen = openInfo, onClose = closeInfo) {
            RegionInfoContent(
                region = region,
                hasStationData = hasStationData,
                stations = snapshotStations,
                onOpenStationList = { onNavigateTo(AppScreen.STATION_LIST) },
                // About is in the sheet's title band: the dialog opens over the sheet, which stays open until it is closed
                onOpenAbout = { showAboutDialog = true },
                // The region picker is above the floating cards too: they fold while it is showing
                menuOpen = pickerState.visible,
                // The dot-size slider belongs to the map, so only the Topography view has it
                dotScale = if (homeTab == 0) pinScale else 1f,
                legendExtra = { if (homeTab == 0) PinSizeSlider(value = pinScale, onValueChange = onPinScaleChange) }
            )
        }

        // Short messages at the bottom (for example what the locate button just did)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 14.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(8.dp),
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            )
        }

        RegionPickerOverlay(
            state = pickerState,
            selectedRegionId = selectedRegionId,
            onSelectRegion = onSelectRegion
        )
        }
    }

    if (showLimitedDataDialog) {
        AlertDialog(
            onDismissRequest = { showLimitedDataDialog = false },
            title = { Text(stringResource(R.string.limited_data_title)) },
            text = {
                Text(stringResource(R.string.limited_data_body, fullDataRegions, totalRegions))
            },
            confirmButton = {
                TextButton(onClick = { showLimitedDataDialog = false }) { Text(stringResource(R.string.action_ok)) }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(appName = appName, buildLabel = buildLabel, onClose = { showAboutDialog = false })
    }
}

/** How opaque the folder header is: 1 is solid; a little less lets the terrain behind it show faintly. */
private const val FOLDER_OPACITY = 0.88f
