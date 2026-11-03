package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import io.github.genesjc.climatefinder.ui.components.TAGS_CELL_WIDTH_DP
import kotlinx.coroutines.delay

/** How long the toast in the middle of the chart stays after the switch is tapped. */
private const val TOAST_MS = 1800L

private val TOAST_GREEN = Color(0xFF14532D)

/** The words of the toast in the middle of the chart, and when it goes away. */
@Stable
internal class TagsToastState {
    var message by mutableStateOf<String?>(null)
    var tick by mutableIntStateOf(0)

    fun show(text: String) {
        message = text
        tick++
    }
}

@Composable
internal fun rememberTagsToast(): TagsToastState = remember { TagsToastState() }

/**
 * The switch for how tags behave while the dial turns, as the first segment of the key bar (with the bands on): with [followView]
 * the tags stay on the same stations (see [TAGS_FOLLOW_VIEW]); without, they are picked afresh at every angle. Tapping it calls
 * [onToggle] and tells [toast] which mode is now on. A thin divider separates it from the angle reading beside it.
 */
@Composable
internal fun TagsSwitch(followView: Boolean, toast: TagsToastState, onToggle: () -> Unit) {
    val onWords = stringResource(R.string.tags_follow_on)
    val offWords = stringResource(R.string.tags_follow_off)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .width(TAGS_CELL_WIDTH_DP.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable(onClickLabel = stringResource(R.string.tags_toggle_action), role = Role.Switch) {
                    onToggle()
                    // The mode it is going into
                    toast.show(if (followView) offWords else onWords)
                }
        ) {
            // Green while the tags follow the view, grey while they are picked afresh
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (followView) Color(0xFF4ADE80) else Color(0xFF94A3B8)))
            Spacer(modifier = Modifier.width(7.dp))
            Text(text = stringResource(R.string.tags_toggle_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.95f))
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.18f)))
    }
}

/** The toast in the middle of the chart naming the mode just switched to: white on dark green, gone after a moment. */
@Composable
internal fun BoxScope.TagsToast(toast: TagsToastState) {
    LaunchedEffect(toast.tick) {
        if (toast.tick > 0) {
            delay(TOAST_MS)
            toast.message = null
        }
    }
    AnimatedVisibility(
        visible = toast.message != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Surface(shape = RoundedCornerShape(16.dp), color = TOAST_GREEN, shadowElevation = 6.dp) {
            Text(
                text = toast.message ?: "",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp)
            )
        }
    }
}
