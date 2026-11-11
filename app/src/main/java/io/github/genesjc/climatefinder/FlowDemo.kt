package io.github.genesjc.climatefinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * SANDBOX ONLY (never merged). Flow demo: a cold Flow (one private run per collector)
 * and a StateFlow (always holds a current value, a late collector gets it at once).
 */
@Composable
fun FlowDemo() {
    val scope = rememberCoroutineScope()
    val log = remember { mutableStateListOf<String>() }
    var collectors by remember { mutableStateOf(0) }  // numbers the collectors so the log can tell them apart
    var lateCollectors by remember { mutableStateOf(0) }

    // Cold: this block does not run until someone collects, and it runs again from the start for each collector
    val ticker = remember { flow { for (i in 1..5) { delay(500); emit(i) } } }

    // Hot: exists on its own and always holds a current value
    val state = remember { MutableStateFlow(0) }
    val current by state.collectAsStateWithLifecycle() // Flow -> Compose state, as MainActivity does with darkMode

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("1. Cold Flow", style = MaterialTheme.typography.titleMedium)
        Button(onClick = {
            collectors += 1
            val n = collectors
            scope.launch { ticker.collect { log.add("collector $n got $it") } } // runs the block for every value
        }) { Text("Collect the ticker") }

        Text("2. StateFlow (current value: $current)", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { state.value = state.value + 1 }) { Text("state.value + 1") }
        Button(onClick = {
            lateCollectors += 1
            val n = lateCollectors
            scope.launch { state.collect { log.add("late collector $n got $it") } }
        }) { Text("Start a late collector") }

        Button(onClick = { log.clear() }) { Text("Clear log") }
        log.forEach { Text(it, fontFamily = FontFamily.Monospace, fontSize = 13.sp) }
    }
}
