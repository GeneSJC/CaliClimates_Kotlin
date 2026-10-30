package io.github.genesjc.climatefinder.ui.screens

import androidx.annotation.StringRes
import io.github.genesjc.climatefinder.ui.components.regionPickerColors
import io.github.genesjc.climatefinder.ui.components.HOME_TILE_BAND
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import io.github.genesjc.climatefinder.ui.components.RegionPickerButton
import io.github.genesjc.climatefinder.ui.components.MapViewButton
import io.github.genesjc.climatefinder.ui.components.RegionPickerOverlay
import io.github.genesjc.climatefinder.ui.components.rememberRegionPickerState
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import io.github.genesjc.climatefinder.ui.components.TOP_NOTICE_MS
import io.github.genesjc.climatefinder.ui.components.TopNotice
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import io.github.genesjc.climatefinder.data.remote.ObservationResult
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import io.github.genesjc.climatefinder.EditionConfig
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.data.local.StationDatasetInfo
import io.github.genesjc.climatefinder.ui.theme.LocalStateUsa
import io.github.genesjc.climatefinder.ui.components.AboutDialog
import io.github.genesjc.climatefinder.ui.components.MenuDrawerContent
import io.github.genesjc.climatefinder.ui.navigation.AppScreen
import kotlinx.coroutines.launch

/** One row in the quick region picker. */
/** The height of the three header widgets on Home. */
private val HEADER_HEIGHT = 42.dp

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // The menu drawer is the top layer: while it is opening or open, the floating cards fold
    val menuDrawerShowing = drawerState.targetValue != DrawerValue.Closed
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLimitedDataDialog by remember { mutableStateOf(false) }
    var showRegionMenu by remember { mutableStateOf(false) }

    val pickerState = rememberRegionPickerState()

    // Region info sheet: slides in from the right over the start screen
    // The open state lives with the caller, so the sheet is still open when Back returns from the Station List
    val infoReveal = remember { Animatable(if (infoOpen) 1f else 0f) }
    // Where the sheet starts: the top of the map (or, with no map, just under the link)
    var mapTopPx by remember { mutableStateOf(0f) }
    // The Topography / Elevation buttons' height (it follows the text size), measured
    val mapDensity = LocalDensity.current
    // On the Elevation view the map grows out into the page margins (west, east and south; never up), so the water
    // reaches the screen edges; eased both ways when switching views
    val expandT by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (homeTab == 1) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "mapExpand"
    )
    var viewSwitchHeight by remember { mutableStateOf(34.dp) }
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
    var menuButtonTopPx by remember { mutableStateOf(0f) }
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
    ModalNavigationDrawer(
        drawerState = drawerState,
        // The menu button opens it; a swipe must not compete with the map's drag to pan (it still closes by swipe)
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            // A narrow side panel only as tall as its content, starting level with the menu button's top edge (the Region Info
            // sheet comes in lower, from the right edge, so the two stagger). The page shows behind it; a tap there,
            // a swipe to the left, or Back closes it.
            val menuWidth = (LocalConfiguration.current.screenWidthDp * 0.66f).dp
            Surface(
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                modifier = (
                    if (menuButtonTopPx > 0f) Modifier.offset { IntOffset(0, menuButtonTopPx.roundToInt()) }
                    else Modifier.statusBarsPadding().padding(top = 14.dp)
                ).width(menuWidth)
            ) {
              Box {
                Box(modifier = Modifier.background(sheetBrush())) {
                MenuDrawerContent(
                    appName = appName,
                    buildLabel = buildLabel,
                    // Only the demo edition has the Demos & Previews hub
                    showDemos = EditionConfig.HAS_DEMOS,
                    darkMode = darkMode,
                    onDarkModeChange = onDarkModeChange,
                    onOpenDemos = {
                        scope.launch { drawerState.close() }
                        onNavigateTo(AppScreen.DEMOS_AND_PREVIEWS)
                    },
                    onOpenAbout = {
                        scope.launch { drawerState.close() }
                        showAboutDialog = true
                    }
                )
                }
                // A small grip at the right edge: the cue that the panel can be swiped closed
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 5.dp)
                        .size(width = 4.dp, height = 36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                )
              }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            run {
            Column {
            run {
                // App header: two separate widgets with the backdrop showing between them, each with the
                // About-dialog gradient: (1) the region (its name, and under it the menu and About buttons at the left
                // and the Region Info link at the right), (2) the region picker.
                val headerShape = RoundedCornerShape(8.dp)
                val headerFill = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    .compositeOver(MaterialTheme.colorScheme.background)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    // A wider gap between the region widget and the picker widget, so more terrain shows between them (8dp, then 16dp)
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Widget 2: the region name with a chevron (tap for the region list), and the Region Info link
                    // on the line below, in the same card. The Box is the list's anchor.
                    Box(modifier = Modifier.weight(1f).height(HEADER_HEIGHT)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(headerShape)
                                // The tile's flat tint and 1 dp outline, so the name and Info link read clearly
                                .background(headerFill)
                                .border(1.dp, MaterialTheme.colorScheme.primary, headerShape)
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClickLabel = stringResource(R.string.region_choose), role = Role.Button) {
                                        showRegionMenu = true
                                    }
                                    .padding(vertical = 0.dp)
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(region.nameRes),
                                        style = MaterialTheme.typography.titleLarge,
                                        // 25% smaller than the title style, so more of a long name shows
                                        fontSize = 20.6.sp,
                                        lineHeight = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Text(
                                        text = "\u25BE",
                                        fontSize = 18.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                                    )
                                }
                                // The region picker, at the right of the name's row (it belongs with the drop-down)
                                Spacer(modifier = Modifier.width(8.dp))
                                // In a small button of its own, so it reads as tappable
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.6f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RegionPickerButton(
                                        state = pickerState,
                                        selectedRegionId = selectedRegionId,
                                        background = Color.Transparent,
                                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                                        border = Color.Transparent,
                                        size = 34.dp,
                                        iconSize = 28.dp
                                    )
                                }
                            }

                        }
                            // The region list, dressed like the rest of the app: a warm sand panel with a fine
                            // outline, each region marked with its own map colour, the current one tinted
                            val menuColors = regionPickerColors()
                            val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
                            DropdownMenu(
                                expanded = showRegionMenu,
                                onDismissRequest = { showRegionMenu = false },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = if (lightLook) Color(0xFFF1E2C4) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (lightLook) Color(0xFF3A2A1E).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.25f)
                                ),
                                tonalElevation = 0.dp,
                                shadowElevation = 8.dp
                            ) {
                                regionOptions.forEach { option ->
                                    val isCurrent = option.id == selectedRegionId
                                    val regionColor = menuColors[option.id] ?: MaterialTheme.colorScheme.primary
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) regionColor.copy(alpha = 0.22f) else Color.Transparent),
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.widthIn(min = 200.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(regionColor)
                                                        .border(1.dp, Color(0xFF3A2A1E).copy(alpha = 0.35f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = stringResource(option.nameRes),
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCurrent) {
                                                    Text(
                                                        text = "\u2713",
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(start = 12.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            showRegionMenu = false
                                            if (!isCurrent) onSelectRegion(option.id)
                                        }
                                    )
                                }
                            }
                    }

                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            }
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
                        MapControlRow(onElevation = false, onMenu = { scope.launch { drawerState.open() } }, onMenuTop = { menuButtonTopPx = it })
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
                            // The slider's arrow button sits 4 dp under the view buttons, whatever their height
                            sliderToggleTop = 21.dp + HOME_TILE_BAND + viewSwitchHeight + 4.dp,
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
                            onNotice = { message -> notice = (notice?.first ?: 0) + 1 to message }
                        )
                    }
                    // The tile and the buttons keep their place on the screen while the map grows out underneath them
                    Box(modifier = Modifier.matchParentSize().padding(horizontal = 5.dp * expandT)) {
                        // The selected station as a tile across the top of the map (it grows into the full card)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(start = 6.dp, end = 6.dp, top = 5.dp)
                                .height(IntrinsicSize.Min)
                                .onSizeChanged { rowWidthPx = it.width },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    // The Region Info sheet opens below this row (the tile and the Info link / X beside it), so it never covers them
                                    .onGloballyPositioned { stationTileTopPx = it.positionInRoot().y + it.size.height + with(mapDensity) { 6.dp.toPx() } }
                            ) {
                                if (shown != null) StationHeroTile(shown, fullWidthPx = rowWidthPx, regionStations = snapshotStations, regionId = region.id, folded = infoOpen || menuDrawerShowing || pickerState.visible, readingFor = readingFor)
                            }
                            // The info link, on its own to the right of the station tile; the sheet slides in from the right, the X takes
                            // its place. Topography only: the Elevation view has no Region Info, so the tile takes the whole width there
                            if (homeTab == 0) {
                                val handleColor = MaterialTheme.colorScheme.primary
                                // The link text always takes its space (hidden while the X shows), so the station tile beside it
                                // keeps one width and does not grow when the link becomes the X
                                Box(
                                    modifier = Modifier
                                        .clickable(
                                            onClickLabel = stringResource(if (infoOpen) R.string.region_info_close else R.string.region_info_open),
                                            role = Role.Button
                                        ) { if (infoOpen) closeInfo() else openInfo() }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.region_info_link),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontSize = 15.84.sp,
                                        lineHeight = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = handleColor,
                                        modifier = Modifier.alpha(if (infoOpen) 0f else 1f)
                                    )
                                    if (infoOpen) {
                                        // A bold X, drawn so its weight and size are exact
                                        Canvas(modifier = Modifier.size(13.dp)) {
                                            val stroke = 2.2.dp.toPx()
                                            val inset = stroke / 2f
                                            drawLine(
                                                color = handleColor,
                                                start = Offset(inset, inset),
                                                end = Offset(size.width - inset, size.height - inset),
                                                strokeWidth = stroke,
                                                cap = StrokeCap.Round
                                            )
                                            drawLine(
                                                color = handleColor,
                                                start = Offset(size.width - inset, inset),
                                                end = Offset(inset, size.height - inset),
                                                strokeWidth = stroke,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Below the tile, at the left: the menu, the north indicator and the About button
                        MapControlRow(
                            onElevation = homeTab == 1,
                            stations = snapshotStations,
                            selectedStationId = selectedStationId,
                            onPickStation = onStationSelected,
                            onMenu = { scope.launch { drawerState.open() } },
                            onMenuTop = { menuButtonTopPx = it },
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
                        // The two view buttons sit at the top right: Topography first, level with the row of controls, then Elevation at
                        // the edge, 9 dp lower, a small offset (they traded places in build 117.15)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 12.dp, top = 12.dp + HOME_TILE_BAND),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            MapViewButton(label = stringResource(R.string.tab_topography), index = 0, selected = homeTab, onSelect = onHomeTabChange)
                            MapViewButton(
                                label = stringResource(R.string.tab_elevation),
                                index = 1,
                                selected = homeTab,
                                onSelect = onHomeTabChange,
                                modifier = Modifier
                                    .padding(top = 9.dp)
                                    .onSizeChanged { viewSwitchHeight = with(mapDensity) { it.height.toDp() } }
                            )
                        }
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
                // The region picker is above the floating cards too: they fold while it is showing
                menuOpen = menuDrawerShowing || pickerState.visible,
                // The dot-size slider belongs to the map, so only the Topography view has it
                dotScale = if (homeTab == 0) pinScale else 1f,
                legendExtra = { if (homeTab == 0) PinSizeSlider(value = pinScale, onValueChange = onPinScaleChange) }
            )
        }

        TopNotice(message = notice?.second)

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
