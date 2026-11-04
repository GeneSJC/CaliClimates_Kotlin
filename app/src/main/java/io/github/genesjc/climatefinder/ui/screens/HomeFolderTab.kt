package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.config.StateUsaRegion
import io.github.genesjc.climatefinder.ui.components.RegionPickerChip
import io.github.genesjc.climatefinder.ui.components.RegionPickerState

/**
 * What the Home folder's tab holds: the region picker's icon and the region's name, and, on the Elevation view, the view's chip on
 * the tab's second line at the right (the Map view shows none). Drawn inside the tab's Box, which sizes the tab's shape.
 *
 * A name of one line rides at the top of the icon's height while the chip is showing, so the chip has the second line to itself
 * and does not touch the name; a name of two lines already fills the icon's height.
 */
@Composable
internal fun BoxScope.HomeFolderTabContent(
    region: StateUsaRegion,
    pickerState: RegionPickerState,
    selectedRegionId: String,
    homeTab: Int
) {
    // The Elevation view's chip on the tab's second line at the right; the Map view shows none. The offset is a draw offset, so the
    // layout does not change
    if (VIEW_CHIP_IN_TAB) {
        androidx.compose.animation.AnimatedVisibility(
            visible = homeTab == 1,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 1.dp).offset(y = 3.dp)
        ) { HomeViewChip(1, height = VIEW_TAB_CHIP_HEIGHT) }
    }
    // How many lines the name takes: one line is lifted to the top of the icon's height while the chip is showing
    var nameLines by remember(region.id) { mutableIntStateOf(1) }
    val lift by animateDpAsState(
        targetValue = if (VIEW_CHIP_IN_TAB && homeTab == 1 && nameLines <= 1) NAME_LIFT else 0.dp,
        label = "tabNameLift"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // The end padding is the room the tab's slope needs. No start padding: the icon lines up with the left edge of the station
        // tile (both FOLDER_MARGIN in). 2dp above the icon on top of the row's own; the bottom leaves 8.4dp under the icon less the
        // trim (FOLDER_TAB_TRIM) that made the tab shorter
        modifier = Modifier.padding(start = 0.dp, end = 28.dp, top = 2.dp, bottom = 8.4.dp - 2.dp - FOLDER_TAB_TRIM / 2)
    ) {
        // A little bigger than the other picker buttons (40dp), with more room inside its outline
        RegionPickerChip(
            state = pickerState,
            selectedRegionId = selectedRegionId,
            modifier = Modifier.height(HEADER_PICKER_HEIGHT),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        )
        // The gap between the picker icon and the name: 12dp
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).offset(y = lift)) {
            // The view's name above the region's name, or below it (VIEW_LABEL_ABOVE_NAME)
            if (VIEW_LABEL_ABOVE_NAME) HomeViewLabel(selected = homeTab, modifier = Modifier.align(Alignment.End))
            Text(
                // The region's shorter header name where it has one (the tab has little room), else its name
                text = stringResource(if (region.headerNameRes != 0) region.headerNameRes else region.nameRes),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 21.sp,
                // Two lines unless the view's name is under the region's name, where one line (ellipsis) keeps the tab's height
                maxLines = if (VIEW_LABEL_UNDER_TOGGLE || VIEW_LABEL_OVER_TOGGLE || HEADER_COMPACT || VIEW_LABEL_ABOVE_NAME) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { nameLines = it.lineCount }
            )
            if (!VIEW_LABEL_ABOVE_NAME) HomeViewLabel(selected = homeTab, modifier = Modifier.align(Alignment.End))
        }
    }
}

/** How far a one-line name moves up (half of the icon's 40dp less the line's 21dp) to sit at the top of the icon's height. */
private val NAME_LIFT = (-9.5).dp
