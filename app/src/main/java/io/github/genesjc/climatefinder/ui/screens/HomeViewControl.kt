package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

/**
 * Where the small line naming the view you are in goes, with the toggle switch (Gene, 2026-11-04). Two variations, both kept:
 *
 *  - true: UNDER THE TOGGLE, in a column at the right of the first line (the switch on top, the line on a small white chip under
 *    it, so it reads against whatever the map shows behind it). The region's name then has the tab to itself, up to two lines.
 *  - false: UNDER THE REGION'S NAME, on the folder tab, with the name held to one line.
 *
 * Set it to false to go back to the line under the name.
 */
internal const val VIEW_LABEL_UNDER_TOGGLE = true

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
        val flip: (Int) -> Unit = { index ->
            onSelect(index)
            // The view it is going into
            toast.show(if (index == 1) profileWords else mapWords)
        }
        if (VIEW_LABEL_UNDER_TOGGLE) {
            Column(modifier = modifier, horizontalAlignment = Alignment.End) {
                MapViewToggleSwitch(selected = selected, onSelect = flip)
                Spacer(modifier = Modifier.height(3.dp))
                HomeViewLabelChip(selected)
            }
        } else {
            MapViewToggleSwitch(selected = selected, onSelect = flip, modifier = modifier)
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
 * The small line under the region's name on the folder tab, naming the view you are in. Shown with the toggle switch, which has
 * no words of its own for now; the chevron button names the other view itself, so it shows nothing then.
 */
@Composable
internal fun HomeViewLabel(selected: Int) {
    if (!VIEW_SWITCH_AS_TOGGLE || VIEW_LABEL_UNDER_TOGGLE) return
    Text(
        text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
        // 30 percent smaller than the last try (17.6sp), bold, on one line
        fontSize = 12.3.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** The line naming the view, on a small white chip under the toggle: dark ink on white reads against any background behind it. */
@Composable
private fun HomeViewLabelChip(selected: Int) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.90f)) {
        Text(
            text = stringResource(if (selected == 0) R.string.view_label_map else R.string.view_label_profile),
            fontSize = 12.3.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}
