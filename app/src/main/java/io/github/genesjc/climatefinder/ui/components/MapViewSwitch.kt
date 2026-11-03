package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

/**
 * One of the two view buttons on the map (Topography is index 0, Elevation is index 1). The selected one is dark,
 * the other white. Home puts them side by side at the right of the header's first line.
 */
@Composable
fun MapViewButton(label: String, index: Int, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val on = index == selected
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (on) Color(0xFF334155) else Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.map_view_show, label), role = Role.Tab) { onSelect(index) }
    ) {
        Text(
            text = label,
            fontSize = ms(9f),
            fontWeight = FontWeight.Bold,
            color = if (on) Color.White else Color(0xFF334155),
            // Snug at the sides (4dp), so the two buttons take less of the header
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp)
        )
    }
}
