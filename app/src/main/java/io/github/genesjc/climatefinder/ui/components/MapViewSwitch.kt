package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

private val SWITCH_INK = Color(0xFF334155)
private val SWITCH_LINE = Color(0xFFCBD5E1)

/**
 * The view button at the right of Home's first line: one button that names the view a tap goes to. On the Map view
 * (index 0) it reads "Profile" with a solid arrowhead to its right; on the Profile view (index 1) it reads "Map" with
 * the arrowhead to its left. Both labels are laid out in the same space (the one not showing is invisible), so the
 * button never changes size when it flips and the folder tab beside it stays still.
 */
@Composable
fun MapViewSwitch(
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
