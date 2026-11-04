package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.components.MapViewChevronButton
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

/** The view control at the right of the header's first line, in the variation [VIEW_SWITCH_AS_TOGGLE] picks. */
@Composable
internal fun HomeViewControl(
    selected: Int,
    onSelect: (Int) -> Unit,
    toast: TagsToastState,
    modifier: Modifier = Modifier
) {
    if (VIEW_SWITCH_AS_TOGGLE) {
        val mapWords = stringResource(R.string.view_toast_map)
        val profileWords = stringResource(R.string.view_toast_profile)
        MapViewToggleSwitch(
            selected = selected,
            onSelect = { index ->
                onSelect(index)
                // The view it is going into
                toast.show(if (index == 1) profileWords else mapWords)
            },
            modifier = modifier
        )
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
 * The small line under the region's name on the folder tab, naming the view you are in. Shown with the toggle switch, which has
 * no words of its own for now; the chevron button names the other view itself, so it shows nothing then.
 */
@Composable
internal fun HomeViewLabel(selected: Int) {
    if (!VIEW_SWITCH_AS_TOGGLE) return
    Text(
        text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
        // 60 percent bigger than the first try (11sp)
        fontSize = 17.6.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
