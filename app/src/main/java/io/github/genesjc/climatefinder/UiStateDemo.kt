package io.github.genesjc.climatefinder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** SANDBOX ONLY (never merged). Everything the screen shows, as one immutable value (the real one is HomeUiState). */
data class UiState(
    val region: String = "None picked",
    val loading: Boolean = false,
    val stations: List<String> = emptyList()
)

/** SANDBOX ONLY (never merged). A small ViewModel with the same shape as HomeViewModel: private state in, read-only state out. */
class RegionViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var load: Job? = null // the load in progress, so a new pick can cancel it (HomeViewModel has regionLoad)

    /** The one event the screen can send: "the user picked this region". */
    fun pick(region: String) {
        load?.cancel()
        _state.update { it.copy(region = region, loading = true, stations = emptyList()) } // copy: a new state, the old one is untouched
        load = viewModelScope.launch {
            delay(1000) // pretend to load stations
            _state.update { it.copy(loading = false, stations = listOf("$region North", "$region South")) }
        }
    }
}

/** SANDBOX ONLY (never merged). MVVM demo: state flows down into the screen, events flow up to the ViewModel. */
@Composable
fun UiStateDemo(vm: RegionViewModel = viewModel()) {
    val ui by vm.state.collectAsStateWithLifecycle() // state down

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Coast", "Sierra", "Desert").forEach { region ->
                Button(onClick = { vm.pick(region) }) { Text(region) } // event up
            }
        }
        Text("Region: ${ui.region}")
        if (ui.loading) CircularProgressIndicator() else ui.stations.forEach { Text(it) }
        Text("UiState = $ui", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}
