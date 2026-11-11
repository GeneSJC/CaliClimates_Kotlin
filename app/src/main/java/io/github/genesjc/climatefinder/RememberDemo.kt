package io.github.genesjc.climatefinder

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

/**
 * SANDBOX ONLY (never merged). Chunk 1 demo: remember vs rememberSaveable.
 * Tap, then rotate the device while on this page. Filter Logcat by tag "Mini".
 */
@Composable
fun RememberDemo() {
    Log.d("Mini", "RememberDemo composed") // prints on first composition and again on every recomposition (each tap)
    var plain by remember { mutableStateOf(0) }          // lost when the Activity is recreated
    var saved by rememberSaveable { mutableStateOf(0) }  // survives recreation, like showIntro in MainActivity
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("remember: $plain", style = MaterialTheme.typography.headlineSmall)
        Text("rememberSaveable: $saved", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = { plain++; saved++ }) { Text("Tap (+1 to both)") } // state write -> recomposition
        Text("Tap a few times, then rotate the device. Watch which counter resets.")
    }
}
