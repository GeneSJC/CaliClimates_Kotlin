package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Two small buttons on the map that switch between its Topography and Elevation views. */
@Composable
fun MapViewSwitch(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Topography", "Elevation").forEachIndexed { index, label ->
            val on = index == selected
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (on) Color(0xFF334155) else Color.White.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.clickable(onClickLabel = "Show the $label view", role = Role.Tab) { onSelect(index) }
            ) {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (on) Color.White else Color(0xFF334155),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
                )
            }
        }
    }
}
