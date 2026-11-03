package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.climatefinder.R
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_GAP_DP
import io.github.genesjc.climatefinder.ui.components.BAND_ARC_SIZE_DP
import io.github.genesjc.climatefinder.ui.components.LAYER_LEGEND_HEIGHT_DP
import kotlinx.coroutines.delay

/** How long the toast in the middle of the chart stays after the switch is tapped. */
private const val TOAST_MS = 1800L

private val TOAST_GREEN = Color(0xFF14532D)

/**
 * The switch for how tags behave while the dial turns, to the left of the dial and above the key (with the bands on): with
 * [followView] the tags stay on the same stations (see [TAGS_FOLLOW_VIEW]); without, they are picked afresh at every angle.
 * Tapping it calls [onToggle] and says which mode is now on, in a dark green toast in the middle of the chart.
 */
@Composable
internal fun BoxScope.ElevationTagsToggle(followView: Boolean, onToggle: () -> Unit) {
    var toast by remember { mutableStateOf<String?>(null) }
    var toastTick by remember { mutableIntStateOf(0) }
    val onWords = stringResource(R.string.tags_follow_on)
    val offWords = stringResource(R.string.tags_follow_off)
    LaunchedEffect(toastTick) {
        if (toastTick > 0) {
            delay(TOAST_MS)
            toast = null
        }
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = (LEGEND_MARGIN_DP + BAND_ARC_SIZE_DP + 8f).dp, bottom = (LEGEND_MARGIN_DP + LAYER_LEGEND_HEIGHT_DP + BAND_ARC_GAP_DP).dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = stringResource(R.string.tags_toggle_action), role = Role.Switch) {
                onToggle()
                // The mode it is going into
                toast = if (followView) offWords else onWords
                toastTick++
            }
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            // Green while the tags follow the view, grey while they are picked afresh
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (followView) Color(0xFF4ADE80) else Color(0xFF94A3B8)))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.tags_toggle_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = TOAST_GREEN, shadowElevation = 6.dp) {
            Text(
                text = toast ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
            )
        }
    }
}
