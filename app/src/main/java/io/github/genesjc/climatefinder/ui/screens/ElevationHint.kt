package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.ui.res.stringResource
import io.github.genesjc.climatefinder.R
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The hint in the ocean: fades away after a few seconds or at the first tap on the line. [hintAlpha] is read as it fades. */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.ElevationHint(hintAlpha: () -> Float, heightDp: androidx.compose.ui.unit.Dp) {
    if (hintAlpha() > 0.01f) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xCC0F172A),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = heightDp * 0.045f + 6.dp)
                .graphicsLayer { this.alpha = hintAlpha() }
        ) {
            Text(
                text = stringResource(R.string.elevation_hint_tap_line),
                color = Color.White,
                // A third smaller than before; fixed, not tied to the font slider
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }
}
