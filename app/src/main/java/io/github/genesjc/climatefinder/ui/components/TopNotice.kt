package io.github.genesjc.climatefinder.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** How far down the screen the note sits, as a share of the screen's height. */
const val TOP_NOTICE_MARGIN = 0.15f

/** How long a note stays before it fades out. */
const val TOP_NOTICE_MS = 3200L

/**
 * A short note that fades in near the top of the screen and fades out when [message] goes back to null (the caller
 * decides how long it stays). It is a toast in the app's own layer: Android ignores where a toast is placed on newer
 * versions, and this one has to sit clear of the controls. It takes no touches.
 */
@Composable
fun TopNotice(message: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.fillMaxHeight(TOP_NOTICE_MARGIN))
        Crossfade(targetState = message, animationSpec = tween(350), label = "topNotice") { text ->
            Box {
                if (text != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BandsButtonDark.copy(alpha = 0.94f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = text,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}
