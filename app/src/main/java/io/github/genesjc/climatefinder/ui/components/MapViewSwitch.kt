package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SWITCH_DARK = Color(0xFF334155)
private val SWITCH_LINE = Color(0xFFCBD5E1)

/**
 * The view switch at the right of Home's first line: one pill with both views in it (Map is index 0, Profile is
 * index 1), the one you are in dark and the other light, so nothing has to be remembered and a tap on the other
 * half switches. Home puts it level with the folder tab's name.
 */
@Composable
fun MapViewSwitch(
    selected: Int,
    mapLabel: String,
    profileLabel: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        shape = shape,
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, SWITCH_LINE),
        modifier = modifier
    ) {
        Row {
            MapViewSwitchSegment(label = mapLabel, on = selected == 0, onClick = { onSelect(0) })
            MapViewSwitchSegment(label = profileLabel, on = selected == 1, onClick = { onSelect(1) })
        }
    }
}

@Composable
private fun MapViewSwitchSegment(label: String, on: Boolean, onClick: () -> Unit) {
    val fill = animateColorAsState(if (on) SWITCH_DARK else Color.Transparent, tween(180), label = "viewSwitchFill").value
    val ink = animateColorAsState(if (on) Color.White else SWITCH_DARK, tween(180), label = "viewSwitchInk").value
    Box(
        modifier = Modifier
            .background(fill, RoundedCornerShape(7.dp))
            .selectable(selected = on, role = Role.Tab, onClick = onClick)
            .widthIn(min = 52.dp)
            .padding(horizontal = 8.dp, vertical = 4.8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            // 10 percent smaller than before (11.25 to 10.125)
            fontSize = ms(10.125f),
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Bold,
            color = ink
        )
    }
}
