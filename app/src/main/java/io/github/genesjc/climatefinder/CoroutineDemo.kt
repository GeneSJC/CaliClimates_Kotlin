package io.github.genesjc.climatefinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SANDBOX ONLY (never merged). Coroutines demo: blocking the main thread vs suspending vs a background thread.
 * The spinner only keeps spinning while the main thread is free, so it shows what each button does to the UI.
 */
@Composable
fun CoroutineDemo() {
    val scope = rememberCoroutineScope() // a coroutine scope that lasts as long as this page
    var taps by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("idle") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator() // animates on the main thread: it freezes if the main thread is blocked
        Button(onClick = { taps++ }) { Text("Tap me: $taps") }

        // 1. Blocks the main thread: nothing can draw or react until the sleep ends
        Button(onClick = {
            Thread.sleep(3000)
            status = "Blocked 3 s, then finished"
        }) { Text("Block main thread 3 s") }

        // 2. Suspends: the coroutine pauses, the main thread stays free
        Button(onClick = {
            scope.launch {
                status = "Waiting 3 s (suspended)..."
                delay(3000)
                status = "Suspended 3 s, then finished"
            }
        }) { Text("delay 3 s in a coroutine") }

        // 3. Switches to a background thread for the slow part, then comes back
        Button(onClick = {
            scope.launch {
                status = "Started on: ${Thread.currentThread().name}"
                val result = withContext(Dispatchers.Default) {
                    Thread.sleep(3000) // blocking a background thread is fine: the UI is not on it
                    "Work ran on: ${Thread.currentThread().name}"
                }
                status = "$result; back on: ${Thread.currentThread().name}"
            }
        }) { Text("Work on a background thread") }

        Text(status, style = MaterialTheme.typography.titleMedium)
    }
}
