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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.config.StateUsaRegion
import io.github.genesjc.climatefinder.ui.components.RegionPickerChip
import io.github.genesjc.climatefinder.ui.components.RegionPickerState

/**
 * What the Home folder's tab holds: the region picker's icon and the region's name, and the view's chip ("Map" or "Elevation") on
 * the tab's second line at the right. Drawn inside the tab's Box, which sizes the tab's shape.
 *
 * A name of one line rides at the top of the icon's height, so the chip has the second line to itself and does not touch the
 * name; a name of two lines already fills the icon's height.
 */
@Composable
internal fun BoxScope.HomeFolderTabContent(
    region: StateUsaRegion,
    pickerState: RegionPickerState,
    selectedRegionId: String,
    homeTab: Int
) {
    // The view's chip on the tab's second line at the right, on both views: one chip of one size and place, the word in it fading
    // between "Map" and "Elevation". The offset is a draw offset, so the layout does not change
    if (VIEW_CHIP_IN_TAB) {
        HomeViewTabChip(homeTab, region.colorHex, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 1.dp).offset(y = 3.dp))
    }
    // The name's ink: a deep, still-coloured shade of the region's picker colour (see regionInk), so it is dark but not black; in the dark
    // look the theme's own text colour
    val lightLook = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val inkColor = if (lightLook) regionInk(region.colorHex) else MaterialTheme.colorScheme.onSurface
    // How many lines the name takes: one line is lifted to the top of the icon's height while the chip is showing
    var nameLines by remember(region.id) { mutableIntStateOf(1) }
    val lift by animateDpAsState(
        targetValue = if (VIEW_CHIP_IN_TAB && nameLines <= 1) NAME_LIFT else 0.dp,
        label = "tabNameLift"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // The end padding is the room the tab's slope needs. No start padding: the icon lines up with the left edge of the station
        // tile (both FOLDER_MARGIN in). 2dp above the icon on top of the row's own; the bottom leaves 8.4dp under the icon less the
        // trim (FOLDER_TAB_TRIM) that made the tab shorter
        modifier = Modifier.padding(start = 0.dp, end = 28.dp, top = 1.dp, bottom = 8.4.dp - 2.dp - FOLDER_TAB_TRIM / 2 - 1.dp)
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
                fontWeight = FontWeight.Bold,
                // The ink is a deep shade of the region's own colour (see TAB_INK_TOWARD_BLACK), so it blends with the tint and stands out
                color = inkColor,
                lineHeight = 21.sp,
                // Two lines unless the view's name is under the region's name, where one line (ellipsis) keeps the tab's height
                maxLines = if (VIEW_LABEL_UNDER_TOGGLE || VIEW_LABEL_OVER_TOGGLE || HEADER_COMPACT || VIEW_LABEL_ABOVE_NAME) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                // A tight white glow round the letters (no offset, only blur), so the deep ink keeps its edge on any tint
                style = if (TAB_TEXT_GLOW) androidx.compose.ui.text.TextStyle(
                    letterSpacing = 0.2.sp,
                    shadow = Shadow(color = Color.White.copy(alpha = 0.9f), offset = Offset.Zero, blurRadius = 6f)
                ) else androidx.compose.ui.text.TextStyle.Default,
                onTextLayout = { nameLines = it.lineCount }
            )
            if (!VIEW_LABEL_ABOVE_NAME) HomeViewLabel(selected = homeTab, modifier = Modifier.align(Alignment.End))
        }
    }
}

/**
 * Whether the region's name on the folder tab has a tight white glow behind its letters (Gene, 2026-11-04): true is on; false is
 * the plain text. The glow is a blurred white shadow with no offset (6px, tighter than the first try's 10), so it lifts the deep ink
 * off whatever tint is behind it.
 */
private const val TAB_TEXT_GLOW = false

/** How far a one-line name moves up (half of the icon's 44dp less the line's 21dp) to sit at the top of the icon's height. */
private val NAME_LIFT = (-11.5).dp
