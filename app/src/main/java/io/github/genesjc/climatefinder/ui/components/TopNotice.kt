package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** How long a note stays before it fades out. */
const val TOP_NOTICE_MS = 3200L

/**
 * A short note that fades in and out as [message] comes and goes (the caller decides how long it stays), such as which view the
 * bands button just switched to. It sits where the caller puts it (the chart keeps it beside the controls it describes, above
 * the key and left of the dial), in the dark slate of the view buttons, and takes no touches.
 */
@Composable
fun BandsNotice(message: String?, modifier: Modifier = Modifier) {
    Crossfade(targetState = message, animationSpec = tween(350), label = "bandsNotice", modifier = modifier) { text ->
        Box {
            if (text != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BandsButtonDark.copy(alpha = 0.94f)
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
