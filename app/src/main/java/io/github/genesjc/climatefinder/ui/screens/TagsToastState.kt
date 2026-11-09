package io.github.genesjc.climatefinder.ui.screens

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
