package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import io.github.genesjc.climatefinder.ui.components.RegionHaptics
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.components.MapViewChevronButton
import io.github.genesjc.climatefinder.ui.components.ms
import io.github.genesjc.climatefinder.ui.components.MapViewToggleSwitch

/**
 * Which control switches Home between the Map and Profile views (Gene, 2026-11-04). Two variations, both kept:
 *
 *  - true: a TOGGLE SWITCH with no label. Off is the Map view, on is the Profile view. Each flip shows a dark green toast in the
 *    middle of the screen naming the view just switched into. (A label beside the switch may follow.)
 *  - false: the CHEVRON BUTTON. One button that names the other view with a solid arrowhead: "Profile \u25B8" on the Map view and
 *    "\u25C2 Map" on the Profile view.
 *
 * Set it to false to go back to the chevron button.
 */
internal const val VIEW_SWITCH_AS_TOGGLE = true

/**
 * Where the small line naming the view you are in goes, with the toggle switch (Gene, 2026-11-04). Two variations, both kept:
 *
 *  - true: UNDER THE TOGGLE, in a column at the right of the first line (the switch on top, the line on a small white chip under
 *    it, so it reads against whatever the map shows behind it). The region's name then has the tab to itself, up to two lines.
 *  - false: UNDER THE REGION'S NAME, on the folder tab, with the name held to one line.
 *
 * Set it to false to go back to the line under the name.
 */
internal const val VIEW_LABEL_UNDER_TOGGLE = false

/**
 * The compact header (Gene, 2026-11-04): the toggle and the view's name are ONE white card at the top right, over the folder's
 * outline; the folder's tab holds only the region row, and the station tile sits right under it, in the card's lower row, beside the
 * card. Needs the toggle variation. Two variations, both kept:
 *
 *  - true: the compact header with the combined card ([HomeViewWidget]), with the toggle stood on end. Tried in build 149.23 and
 *    set aside (Gene, 2026-11-04); it is still here to look at.
 *  - false: the header as in build 149.21: the toggle (with its chip, see [VIEW_LABEL_UNDER_TOGGLE]) on the first line and the
 *    tile under the tab.
 *
 * Set it to false to go back.
 */
internal const val VIEW_WIDGET_COMBINED = false

/**
 * Where the view's name goes on the folder tab when the toggle floats alone (that is, when neither VIEW_WIDGET_COMBINED nor
 * VIEW_LABEL_UNDER_TOGGLE is on): true puts it ABOVE the region's name, false puts it BELOW it (Gene, 2026-11-04). The region's
 * name has up to two lines and the toggle floats on the map without a card behind it.
 */
internal const val VIEW_LABEL_ABOVE_NAME = true

/**
 * Where the view's name goes when the toggle floats alone and VIEW_WIDGET_COMBINED is off (Gene, 2026-11-04): true puts it on a
 * small pastel chip stacked ABOVE the toggle at the right of the first line, with a margin between them (not joined), so the
 * region's name has the tab to itself and sits centred in it. Takes the place of VIEW_LABEL_ABOVE_NAME when on.
 */
internal const val VIEW_LABEL_OVER_TOGGLE = true

/**
 * Where the view chip is (Gene, 2026-11-04), with VIEW_CHIP_IN_CONTROLS on: true puts it on the folder tab, on the tab's second
 * line at the right, and only while the Profile view is showing (the Map view describes itself, so no chip there); false keeps
 * it at the right under the header on both views.
 */
internal const val VIEW_CHIP_IN_TAB = true

/**
 * With VIEW_CHIP_IN_CONTROLS on and VIEW_CHIP_IN_TAB off (Gene, 2026-11-04): true puts the chip ABOVE the toggle at the right of the
 * header, only while the Elevation view is showing, and the toggle sits lower to make room without making the header any taller
 * (the room is held on the Map view too, so the toggle does not jump). false puts the chip at the right under the header.
 */
internal const val VIEW_CHIP_ABOVE_TOGGLE = false

/** The chip's height above the toggle, small enough that chip and toggle together fit the first row without making it taller. */
internal val VIEW_TOP_CHIP_HEIGHT = 17.dp

/** The chip's height on the folder tab, where it shares the tab's second line. */
internal val VIEW_TAB_CHIP_HEIGHT = 22.dp

/**
 * Whether a toast in the middle of the screen names the view just switched into (Gene, 2026-11-04). Off: the chip on the folder tab
 * already names the view, so it glows for a moment when the view changes and says it itself (see [VIEW_CHIP_GLOW_ON_CHANGE]). Turn it
 * on to get the toast back.
 */
internal const val VIEW_CHANGE_TOAST = false

/**
 * Whether the tab chip gives a small pop and a shimmer when the view changes (the toast's job, see [VIEW_CHANGE_TOAST]). The first try
 * also had a gold glow and outline; it was not visible enough and was taken out (Gene, 2026-11-04).
 */
internal const val VIEW_CHIP_GLOW_ON_CHANGE = true

/** Whether the phone gives a short click when the view changes, together with the chip's pop and shimmer. */
private const val VIEW_CHANGE_HAPTIC = true

/** How long the shimmer takes to cross the chip (milliseconds): it was 700 and read as too fast, so it is drawn out. */
private const val VIEW_SHIMMER_MS = 1600

/** The shimmer band's width as a share of the chip's width (it was 0.55), and its strongest opacity (it was 0.6): a bigger, bolder sweep. */
private const val VIEW_SHIMMER_BAND = 0.8f
private const val VIEW_SHIMMER_STRENGTH = 0.8f

/** The shimmer's colour: a pale silver-blue sweep, visible on the chip's white (a white sheen would not show on it). */
private val VIEW_CHIP_SHIMMER = Color(0xFF8FA8D6)

/** The room between the header and the controls under it (the view chip first): the controls start this far lower. */
internal val CONTROLS_GAP = 20.dp

/** The view chip's height: 32dp like the Stations button and the other map controls it lines up with. */
internal val VIEW_CHIP_HEIGHT = 32.dp

/** The chip's background: white, a little see-through (the toggle's own light colour), gentle on both views' backgrounds. */
private val VIEW_CHIP_COLOR = Color.White.copy(alpha = 0.85f)

/** The green of the folder tab, for the view chip and for the floating toggle's off track; the same colour in both. */
internal val VIEW_FOLDER_GREEN = Color(0xFFE4EBDA)

/** The chip's letters: a dark slate on the green. */
private val VIEW_CHIP_INK = Color(0xFF1F2937)

/**
 * Where the view's name chip goes with the toggle switch (Gene, 2026-11-04): true puts it with the map's controls under the
 * header, so it stays put as the view changes: on the Map view it is the first item of the column under the station tile (then
 * the north indicator, then Info); on the Profile view it comes after the Stations drop-down, on the row with the slider's arrow
 * button. The toggle then floats alone at the right of the header. Takes the place of VIEW_LABEL_OVER_TOGGLE when on.
 */
internal const val VIEW_CHIP_IN_CONTROLS = true

/** Whether the chip sits at the right under the header on both views: the older of the chip's places. */
internal const val VIEW_CHIP_UNDER_HEADER = VIEW_CHIP_IN_CONTROLS && !VIEW_CHIP_IN_TAB && !VIEW_CHIP_ABOVE_TOGGLE

/** Whether the compact header is in use: the toggle variation and the combined card. */
internal const val HEADER_COMPACT = VIEW_SWITCH_AS_TOGGLE && VIEW_WIDGET_COMBINED

/** The combined card's width: the name on the left (two short lines), the switch stood on end at the right; the tab leaves room for it. */
internal val VIEW_WIDGET_WIDTH = 108.dp

/** The view control at the right of the header's first line, in the variation [VIEW_SWITCH_AS_TOGGLE] picks. */
@Composable
internal fun HomeViewControl(
    selected: Int,
    onSelect: (Int) -> Unit,
    toast: TagsToastState,
    modifier: Modifier = Modifier
) {
    if (VIEW_SWITCH_AS_TOGGLE) {
        val flip = rememberViewFlip(onSelect, toast)
        if (VIEW_CHIP_IN_CONTROLS) {
            if (VIEW_CHIP_ABOVE_TOGGLE) {
                // The chip above the toggle on the Elevation view; its room is held on the Map view too, so the toggle stays put
                Column(modifier = modifier, horizontalAlignment = Alignment.End) {
                    Box(modifier = Modifier.height(VIEW_TOP_CHIP_HEIGHT)) {
                        androidx.compose.animation.AnimatedVisibility(visible = selected == 1) { HomeViewChip(1, height = VIEW_TOP_CHIP_HEIGHT) }
                    }
                    MapViewToggleSwitch(selected = selected, onSelect = flip)
                }
            } else {
                // The chip is with the map's controls: the toggle floats alone
                MapViewToggleSwitch(selected = selected, onSelect = flip, modifier = modifier)
            }
        } else if (VIEW_LABEL_OVER_TOGGLE) {
            // The chip above the switch, a margin between them
            Column(modifier = modifier, horizontalAlignment = Alignment.End) {
                HomeViewLabelChip(selected)
                Spacer(modifier = Modifier.height(5.dp))
                MapViewToggleSwitch(selected = selected, onSelect = flip, offTrackColor = VIEW_FOLDER_GREEN)
            }
        } else if (VIEW_LABEL_UNDER_TOGGLE) {
            Column(modifier = modifier, horizontalAlignment = Alignment.End) {
                MapViewToggleSwitch(selected = selected, onSelect = flip, offTrackColor = VIEW_FOLDER_GREEN)
                Spacer(modifier = Modifier.height(3.dp))
                HomeViewLabelChip(selected)
            }
        } else {
            MapViewToggleSwitch(selected = selected, onSelect = flip, modifier = modifier, offTrackColor = VIEW_FOLDER_GREEN)
        }
    } else {
        MapViewChevronButton(
            selected = selected,
            mapLabel = stringResource(R.string.tab_topography),
            profileLabel = stringResource(R.string.tab_elevation),
            onSelect = onSelect,
            modifier = modifier
        )
    }
}

/**
 * The small chip on the folder tab naming the view you are in, at the right of the tab (the caller aligns it) with a white chip
 * and bold dark letters for contrast. Shown with the toggle switch, which has no words of its own for now, when it is not in the
 * combined card or under the toggle; the chevron button names the other view itself, so it shows nothing then.
 */
@Composable
internal fun HomeViewLabel(selected: Int, modifier: Modifier = Modifier) {
    if (!VIEW_SWITCH_AS_TOGGLE || VIEW_CHIP_IN_CONTROLS || VIEW_LABEL_UNDER_TOGGLE || VIEW_LABEL_OVER_TOGGLE || HEADER_COMPACT) return
    Surface(shape = RoundedCornerShape(6.dp), color = VIEW_FOLDER_GREEN, modifier = modifier) {
        Text(
            text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
            fontSize = 12.3.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = VIEW_CHIP_INK,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/** The line naming the view, on a small white chip under the toggle (VIEW_LABEL_UNDER_TOGGLE): dark ink on white reads against any background. */
@Composable
private fun HomeViewLabelChip(selected: Int) {
    Surface(shape = RoundedCornerShape(6.dp), color = VIEW_FOLDER_GREEN) {
        Text(
            text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
            fontSize = 12.3.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = VIEW_CHIP_INK,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/** What a flip of the toggle does: choose the view, and show the toast naming the view just entered. */
@Composable
private fun rememberViewFlip(onSelect: (Int) -> Unit, toast: TagsToastState): (Int) -> Unit {
    val mapWords = stringResource(R.string.view_toast_map)
    val profileWords = stringResource(R.string.view_toast_profile)
    return { index ->
        onSelect(index)
        // The view it is going into (the chip's glow now says it; the toast is off, see VIEW_CHANGE_TOAST)
        if (VIEW_CHANGE_TOAST) toast.show(if (index == 1) profileWords else mapWords)
    }
}

/**
 * The compact header's one white card: the name of the view you are in at the left and the toggle, stood on end, at the right,
 * both on the same white so the words read against whatever the map shows behind. It sits at the top right, lifted a little over
 * the folder's top edge, and is no taller than the tab, so the station tile under it has the whole width.
 */
@Composable
internal fun HomeViewWidget(selected: Int, onSelect: (Int) -> Unit, toast: TagsToastState, modifier: Modifier = Modifier) {
    val flip = rememberViewFlip(onSelect, toast)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.94f),
        shadowElevation = 3.dp,
        modifier = modifier.width(VIEW_WIDGET_WIDTH)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 5.dp, bottom = 5.dp)) {
            // The name of the view you are in, to the left of the switch (two short lines when it needs them)
            Text(
                text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
                fontSize = 12.3.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = Color(0xFF334155),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            // The switch stood on end: Map at the top, Profile at the bottom
            MapViewToggleSwitch(selected = selected, onSelect = flip, offTrackColor = Color(0xFFE2E8F0), vertical = true)
        }
    }
}

/**
 * The chip naming the view you are in, with the map's controls (VIEW_CHIP_IN_CONTROLS): dark slate, a little see-through, with
 * white bold letters in the same type as the Stations button. It stands on its own against both views' backgrounds and stays in
 * the same place when the view changes. [VIEW_CHIP_HEIGHT] tall.
 */
@Composable
internal fun HomeViewChip(selected: Int, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = VIEW_CHIP_HEIGHT) {
    Surface(
        // The shape, padding and type of the Stations button, so the chip reads as one of the family
        shape = RoundedCornerShape(6.dp),
        color = VIEW_CHIP_COLOR,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier.height(height)
    ) {
        Box(modifier = Modifier.padding(horizontal = 7.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
                // The Stations button's size (9, through the shared text scale), in a regular weight; bring bold back if it is too faint
                fontSize = ms(9f),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                color = VIEW_CHIP_INK
            )
        }
    }
}

/**
 * The chip on the folder tab: ONE chip, as wide as the longer word ("Elevation"), with "Map" and "Elevation" laid in the same
 * space and centred, one fading into the other. The chip never changes size or place when the view changes, so only the word
 * fades; "Map" has more room on each side than it needs, which keeps the two views looking alike.
 */
@Composable
internal fun HomeViewTabChip(selected: Int, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = VIEW_TAB_CHIP_HEIGHT) {
    val elevationShown by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected == 1) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "tabChipWords"
    )
    // A pulse (0 to 1 and back to 0) drives the pop, and a sweep (0 to 1) drives the shimmer; both run each time the view changes
    // (not on the first look), with a click from the phone's vibrator
    val context = androidx.compose.ui.platform.LocalContext.current
    val sweep = remember { androidx.compose.animation.core.Animatable(1f) }
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }
    var armed by remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(selected) {
        if (!armed) {
            armed = true
            return@LaunchedEffect
        }
        if (VIEW_CHANGE_HAPTIC) RegionHaptics.tick(context)
        if (!VIEW_CHIP_GLOW_ON_CHANGE) return@LaunchedEffect
        pulse.snapTo(0f)
        sweep.snapTo(0f)
        // The shimmer crosses the chip while it pops and settles
        launch { sweep.animateTo(1f, androidx.compose.animation.core.tween(VIEW_SHIMMER_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
        pulse.animateTo(1f, androidx.compose.animation.core.tween(180))
        pulse.animateTo(0f, androidx.compose.animation.core.tween(420))
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = VIEW_CHIP_COLOR,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .height(height)
            .graphicsLayer { val pop = 1f + 0.08f * pulse.value; scaleX = pop; scaleY = pop }
    ) {
        Box(
            modifier = Modifier
                // The shimmer: a soft diagonal band that crosses the chip from left to right (the Surface clips it to the chip's shape)
                .drawWithContent {
                    drawContent()
                    val t = sweep.value
                    if (t < 1f) {
                        val band = size.width * VIEW_SHIMMER_BAND
                        val x = -band + (size.width + band * 2f) * t
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(Color.Transparent, VIEW_CHIP_SHIMMER.copy(alpha = VIEW_SHIMMER_STRENGTH), Color.Transparent),
                                start = Offset(x, 0f),
                                end = Offset(x + band, size.height)
                            )
                        )
                    }
                }
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            TabChipWord(R.string.view_label_map, 1f - elevationShown)
            TabChipWord(R.string.view_label_profile, elevationShown)
        }
    }
}

/** One of the tab chip's two words, at [alpha]; a word that is not showing is also hidden from screen readers. */
@Composable
private fun TabChipWord(@androidx.annotation.StringRes res: Int, alpha: Float) {
    Text(
        text = stringResource(res),
        fontSize = ms(9f),
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        color = VIEW_CHIP_INK,
        modifier = Modifier
            .alpha(alpha)
            .then(if (alpha < 0.5f) Modifier.clearAndSetSemantics { } else Modifier)
    )
}
