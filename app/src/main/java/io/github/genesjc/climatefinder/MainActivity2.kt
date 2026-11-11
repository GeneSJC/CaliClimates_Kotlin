package io.github.genesjc.climatefinder

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * SANDBOX ONLY (feature-set/walkthroughSandbox, never merged).
 * Chunk 1 demo: what MainActivity.onCreate + setContent do, in the smallest form.
 * Filter Logcat by tag "Mini".
 */
class MainActivity2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // required; also restores saved state for rememberSaveable
        // null on a fresh launch, non-null when the system recreates us (e.g. rotation)
        Log.d("Mini", "onCreate, savedInstanceState = ${if (savedInstanceState == null) "null (fresh)" else "non-null (recreated)"}")

        setContent { // builds a ComposeView and starts the composition; this lambda re-runs on recomposition
            Log.d("Mini", "setContent lambda ran")
            var plain by remember { mutableStateOf(0) }          // lost when the Activity is recreated
            var saved by rememberSaveable { mutableStateOf(0) }  // survives recreation, like showIntro in MainActivity
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("remember: $plain", style = MaterialTheme.typography.headlineSmall)
                Text("rememberSaveable: $saved", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { plain++; saved++ }) { Text("Tap (+1 to both)") } // state write -> recomposition
                Text("Tap a few times, then rotate the device. Watch which counter resets.")
                ModifierOrderDemo() // modifier-order demo, see ModifierOrderDemo.kt
            }
        }
    }
}
