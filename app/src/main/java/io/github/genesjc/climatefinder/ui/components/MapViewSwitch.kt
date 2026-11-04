package io.github.genesjc.climatefinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R

/**
 * One of the two view buttons on the map (Map is index 0, Profile is index 1). The selected one is dark,
 * the other white. Home stacks them at the right of the header's second line, in a column of one width.
 */
@Composable
fun MapViewButton(label: String, index: Int, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, centered: Boolean = false) {
    val on = index == selected
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (on) Color(0xFF334155) else Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier.clickable(onClickLabel = stringResource(R.string.map_view_show, label), role = Role.Tab) { onSelect(index) }
    ) {
        Text(
            text = label,
            // 25 percent larger than before (9 to 11.25), with a little room between letters so short words do not blur
            fontSize = ms(11.25f),
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Bold,
            color = if (on) Color.White else Color(0xFF334155),
            // In a stack of buttons of one width (centered), the word is centred in the button
            textAlign = if (centered) TextAlign.Center else null,
            modifier = (if (centered) Modifier.fillMaxWidth() else Modifier).padding(horizontal = 9.dp, vertical = if (centered) 3.dp else 6.dp)
        )
    }
}
