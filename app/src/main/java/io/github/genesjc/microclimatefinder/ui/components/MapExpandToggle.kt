package io.github.genesjc.microclimatefinder.ui.components

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
import io.github.genesjc.microclimatefinder.R

/** The small "Expand / Shrink" chip on the maps: stretches the map to fill the screen and back. */
@Composable
fun MapExpandToggle(expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.90f),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier.clickable(
            onClickLabel = stringResource(if (expanded) R.string.map_shrink_label else R.string.map_expand_label),
            role = Role.Button,
            onClick = onToggle
        )
    ) {
        Text(
            text = stringResource(if (expanded) R.string.map_shrink_button else R.string.map_expand_button),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
