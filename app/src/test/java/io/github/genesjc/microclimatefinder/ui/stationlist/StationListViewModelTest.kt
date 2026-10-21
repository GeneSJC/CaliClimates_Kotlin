package io.github.genesjc.microclimatefinder.ui.stationlist

import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.testLiveReadings
import io.github.genesjc.microclimatefinder.testStation
import io.github.genesjc.microclimatefinder.testutil.FakeStationSource
import io.github.genesjc.microclimatefinder.testutil.MainDispatcherRule
import io.github.genesjc.microclimatefinder.testutil.StopKoinRule
import io.github.genesjc.microclimatefinder.testutil.restartKoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StationListViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", title = "North", subtitle = "Sample",
        stationCount = 3, minElevationMeters = 0, maxElevationMeters = 100
    )
    private val stations = listOf(testStation(id = "K1"), testStation(id = "K2"), testStation(id = "K3"))

    private fun viewModel(list: List<MicroclimateStation> = stations): StationListViewModel {
        restartKoin {
            single { StationRepository(FakeStationSource(list), Dispatchers.Unconfined) }
            single { testLiveReadings(CoroutineScope(Dispatchers.Unconfined)) }
        }
        return StationListViewModel()
    }

    @Test
    fun marksTheStationSelectedOnHomeAndOpensThere() {
        val state = viewModel().stateFor(dataset, stations, "K2")
        assertEquals("K2", state.activeStationId)
        assertEquals(1, state.initialIndex)
        assertEquals("Showing on map", state.selectedLabel)
        assertNull(state.emptyMessage)
    }

    @Test
    fun marksTheFirstStationWhenNothingValidIsSelected() {
        assertEquals("K1", viewModel().stateFor(dataset, stations, null).activeStationId)
        val state = viewModel().stateFor(dataset, stations, "KGONE")
        assertEquals("K1", state.activeStationId)
        assertEquals(0, state.initialIndex)
    }

    @Test
    fun carriesTheDatasetHeader() {
        val state = viewModel().stateFor(dataset, stations, null)
        assertEquals("North", state.title)
        assertEquals("Sample", state.subtitle)
        assertEquals(3, state.stations.size)
    }

    @Test
    fun explainsAnEmptyDataset() {
        val state = viewModel(emptyList()).stateFor(dataset, emptyList(), "K1")
        assertNull(state.activeStationId)
        assertEquals(0, state.initialIndex)
        assertEquals("No stations loaded from: a.txt", state.emptyMessage)
    }

    @Test
    fun readsTheStationsOffTheCallerAndSharesTheFlow() {
        val vm = viewModel()
        val flow = vm.stations(dataset)
        assertEquals(stations, flow.value)
        assertTrue(flow === vm.stations(dataset))
    }

    @Test
    fun showsALoadingStateUntilTheStationsAreRead() {
        val state = viewModel().stateFor(dataset, null, "K1")
        assertTrue(state.isLoading)
        assertNull(state.emptyMessage)
        assertEquals("North", state.title)
        assertTrue(state.stations.isEmpty())
    }
}
