package io.github.genesjc.climatefinder.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Makes the element take every touch that lands on it, so that the pan and pinch handlers on the map around it do not
 * also act on those touches. The map's handlers sit on the whole map, which holds the slider and its buttons as children,
 * so a touch on one reaches both unless the child claims it. This claims it after the element's own handlers have used it
 * (the slider's drag, a button's tap), and the map's handlers, which run after, see it as taken and stand back.
 */
fun Modifier.touchShield(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
        }
    }
}
