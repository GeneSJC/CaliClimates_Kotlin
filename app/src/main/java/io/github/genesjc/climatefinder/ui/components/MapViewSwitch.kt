package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

private val SWITCH_INK = Color(0xFF334155)
private val SWITCH_LINE = Color(0xFFCBD5E1)

/**
 * Variation "chevron button" of the view control at the right of Home's first line (the other is [MapViewToggleSwitch]; Home
 * picks one with VIEW_SWITCH_AS_TOGGLE): one button that names the view a tap goes to. On the Map view
 * (index 0) it reads "Profile" with a solid arrowhead to its right; on the Profile view (index 1) it reads "Map" with
 * the arrowhead to its left. Both labels are laid out in the same space (the one not showing is invisible), so the
 * button never changes size when it flips and the folder tab beside it stays still.
 */
@Composable
fun MapViewChevronButton(
    selected: Int,
    mapLabel: String,
    profileLabel: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val toProfile = selected == 0
    val profileShown by animateFloatAsState(if (toProfile) 1f else 0f, tween(150), label = "viewButtonProfile")
    val targetLabel = if (toProfile) profileLabel else mapLabel
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, SWITCH_LINE),
        modifier = modifier.clickable(
            onClickLabel = stringResource(R.string.map_view_show, targetLabel),
            role = Role.Button
        ) { onSelect(if (toProfile) 1 else 0) }
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.8.dp), contentAlignment = Alignment.Center) {
            ViewButtonLabel(profileLabel, arrowAfter = true, shown = profileShown)
            ViewButtonLabel(mapLabel, arrowAfter = false, shown = 1f - profileShown)
        }
    }
}

/** A label with its solid arrowhead; at [shown] below half it is also hidden from screen readers. */
@Composable
private fun ViewButtonLabel(label: String, arrowAfter: Boolean, shown: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.alpha(shown).then(if (shown < 0.5f) Modifier.clearAndSetSemantics { } else Modifier)
    ) {
        if (!arrowAfter) ViewButtonArrow("\u25C2", Modifier.padding(end = 4.dp))
        Text(
            text = label,
            fontSize = ms(10.125f),
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Bold,
            color = SWITCH_INK
        )
        if (arrowAfter) ViewButtonArrow("\u25B8", Modifier.padding(start = 4.dp))
    }
}

/** The solid arrowhead beside the label: filled, like the drop-down arrows elsewhere in the header. */
@Composable
private fun ViewButtonArrow(glyph: String, modifier: Modifier) {
    Text(text = glyph, fontSize = ms(12f), color = SWITCH_INK, modifier = modifier)
}

/**
 * Variation "toggle switch" of the view control at the right of Home's first line (the other is [MapViewChevronButton]; Home
 * picks one with VIEW_SWITCH_AS_TOGGLE): a plain toggle switch with no label. The two options it toggles: off is the Map view
 * (index 0), on is the Profile view (index 1). A label may be added beside it later; for now Home shows a toast in the middle
 * of the screen naming the view just switched into.
 */
@Composable
fun MapViewToggleSwitch(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    // The track while off (the Map view); a light slate when the switch sits on a white card, where white would vanish
    offTrackColor: Color = Color.White.copy(alpha = 0.90f),
    // The off track's wash (the picker button's blue-to-green), over which the dark on-track fades in; null uses [offTrackColor]
    offTrackBrush: androidx.compose.ui.graphics.Brush? = BlueGreenWash,
    // Stood on end: Map at the top (off), Profile at the bottom (on)
    vertical: Boolean = false
) {
    val on = selected == 1
    val description = stringResource(R.string.view_switch_toggle)
    val length = if (vertical) TOGGLE_VERTICAL_LENGTH else TOGGLE_WIDTH
    val thumbLength = if (vertical) TOGGLE_VERTICAL_THUMB_LENGTH else TOGGLE_THUMB_WIDTH
    val thumbAt by animateDpAsState(if (on) length - thumbLength - TOGGLE_INSET else TOGGLE_INSET, tween(150), label = "viewToggleThumb")
    // With a wash, the track is the wash with the dark colour fading in over it (from transparent dark, so the fade stays dark-toned)
    val track by animateColorAsState(if (on) SWITCH_INK else if (offTrackBrush != null) SWITCH_INK.copy(alpha = 0f) else offTrackColor, tween(150), label = "viewToggleTrack")
    // The thumb is dark on the Map view and, on the Elevation view, the same wash as the Map view's track (the dark fades out over it)
    val thumb by animateColorAsState(if (on) SWITCH_INK.copy(alpha = 0f) else SWITCH_INK, tween(150), label = "viewToggleThumb")
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .toggleable(value = on, role = Role.Switch, onValueChange = { onSelect(if (it) 1 else 0) })
            .size(width = if (vertical) TOGGLE_HEIGHT else length, height = if (vertical) length else TOGGLE_HEIGHT)
            .clip(CircleShape)
            .then(if (offTrackBrush != null) Modifier.background(offTrackBrush) else Modifier)
            .background(track)
            .border(1.dp, if (on) SWITCH_INK else SWITCH_LINE, CircleShape)
    ) {
        Box(
            modifier = (if (vertical) Modifier.offset(y = thumbAt).align(Alignment.TopCenter) else Modifier.offset(x = thumbAt).align(Alignment.CenterStart))
                .size(width = if (vertical) TOGGLE_THUMB else thumbLength, height = if (vertical) thumbLength else TOGGLE_THUMB)
                .clip(CircleShape)
                .background(BlueGreenWash)
                .background(thumb)
        )
    }
}

/** The toggle's size: 70dp wide and 34dp tall (it was 27.2dp, made taller to be easier to grab), the thumb inset 3dp all round. */
private val TOGGLE_WIDTH = 70.dp
private val TOGGLE_HEIGHT = 34.dp
private val TOGGLE_INSET = 3.dp
private val TOGGLE_THUMB = TOGGLE_HEIGHT - TOGGLE_INSET * 2

/** The thumb is a wide pill (the newer desktop-switch look), not a circle; set this equal to the thumb's height (21.2dp) to go back to a circle. */
private val TOGGLE_THUMB_WIDTH = 30.dp

/** Stood on end (the compact header's card): the track is as wide as the toggle is tall and 52dp long, the pill thumb 30dp long. */
private val TOGGLE_VERTICAL_LENGTH = 52.dp
private val TOGGLE_VERTICAL_THUMB_LENGTH = 30.dp
