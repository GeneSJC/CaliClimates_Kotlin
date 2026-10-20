package io.github.genesjc.microclimatefinder.ui.stationlist

import io.github.genesjc.microclimatefinder.data.local.StationDatasetInfo
import io.github.genesjc.microclimatefinder.testLiveReadings
import io.github.genesjc.microclimatefinder.testStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationListViewModelTest {

    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", title = "North", subtitle = "Sample",
        stationCount = 3, minElevationMeters = 0, maxElevationMeters = 100
    )
    private val stations = listOf(testStation(id = "K1"), testStation(id = "K2"), testStation(id = "K3"))

    private fun viewModel(list: List<io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation> = stations) =
        StationListViewModel(stationsFor = { list }, readings = testLiveReadings(CoroutineScope(UnconfinedTestDispatcher())))

    @Test
    fun marksTheStationSelectedOnHomeAndOpensThere() {
        val state = viewModel().stateFor(dataset, "K2")
        assertEquals("K2", state.activeStationId)
        assertEquals(1, state.initialIndex)
        assertEquals("Showing on map", state.selectedLabel)
        assertNull(state.emptyMessage)
    }

    @Test
    fun marksTheFirstStationWhenNothingValidIsSelected() {
        assertEquals("K1", viewModel().stateFor(dataset, null).activeStationId)
        val state = viewModel().stateFor(dataset, "KGONE")
        assertEquals("K1", state.activeStationId)
        assertEquals(0, state.initialIndex)
    }

    @Test
    fun carriesTheDatasetHeader() {
        val state = viewModel().stateFor(dataset, null)
        assertEquals("North", state.title)
        assertEquals("Sample", state.subtitle)
        assertEquals(3, state.stations.size)
    }

    @Test
    fun explainsAnEmptyDataset() {
        val state = viewModel(emptyList()).stateFor(dataset, "K1")
        assertNull(state.activeStationId)
        assertEquals(0, state.initialIndex)
        assertEquals("No stations loaded from: a.txt", state.emptyMessage)
    }
}
