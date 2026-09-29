package io.github.genesjc.microclimatefinder.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.genesjc.microclimatefinder.data.remote.NwsObservationProbe

/** Dev proof line: the station's real latest reading from the National Weather Service. */
@Composable
fun NwsProbeLine(stationId: String, modifier: Modifier = Modifier) {
    val prefix = "NWS live check (dev): "
    var text by remember(stationId) { mutableStateOf(prefix + "checking...") }
    LaunchedEffect(stationId) {
        text = prefix + NwsObservationProbe.describe(stationId)
    }
    Text(
        text = text,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 6.dp)
    )
}
