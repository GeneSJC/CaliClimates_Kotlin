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
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.genesjc.climatefinder.config.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * SANDBOX ONLY (never merged). A tiny ViewModel built the way the real ones are:
 * a KoinComponent that looks its helpers up with `by inject()`, not through the constructor.
 * The one constructor argument, SavedStateHandle, is supplied by the default factory.
 */
class DemoViewModel(private val handle: SavedStateHandle) : ViewModel(), KoinComponent {
    private val config: AppConfig by inject() // a lookup in the container Application.onCreate started

    val buildLabel: String get() = config.buildLabel
    val id: Int = System.identityHashCode(this) // tells instances apart

    private val _plainCount = MutableStateFlow(0) // lives in the ViewModel's memory only
    val plainCount: StateFlow<Int> = _plainCount.asStateFlow()

    val handleCount: StateFlow<Int> = handle.getStateFlow("handleCount", 0) // backed by the saved-state bundle

    init {
        Log.d("Mini", "DemoViewModel created, id=$id")
    }

    fun increment() {
        _plainCount.value = _plainCount.value + 1
        handle["handleCount"] = handleCount.value + 1
    }

    override fun onCleared() {
        Log.d("Mini", "DemoViewModel cleared, id=$id") // the store was cleared: the Activity finished for real
    }
}

/**
 * SANDBOX ONLY (never merged). Chunk 2 demo: viewModel(). Three counters that each survive a different event.
 * Filter Logcat by tag "Mini".
 */
@Composable
fun ViewModelDemo() {
    val a: DemoViewModel = viewModel() // asks the Activity's store: finds one or creates one
    val b: DemoViewModel = viewModel() // asks again: gets the same instance
    val viewModelCount by a.plainCount.collectAsStateWithLifecycle()
    val handleCount by a.handleCount.collectAsStateWithLifecycle()
    var plain by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ViewModel id: ${a.id}")
        Text("Second viewModel() call, same instance: ${a === b}")
        Text("Build label from Koin: ${a.buildLabel}")
        Text("remember: $plain", style = MaterialTheme.typography.titleLarge)
        Text("ViewModel field: $viewModelCount", style = MaterialTheme.typography.titleLarge)
        Text("ViewModel + SavedStateHandle: $handleCount", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { plain++; a.increment() }) { Text("Tap (+1 to all three)") }
        Text("Then try: rotate; Back to menu and reopen; Home, then kill the process (see chat).")
    }
}
