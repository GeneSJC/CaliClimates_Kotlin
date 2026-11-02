package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.testStation
import io.github.genesjc.climatefinder.testutil.FakeStationSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext
import io.github.genesjc.climatefinder.domain.model.DatasetKind

class AssetDatasetCatalogTest {

    private val north = StateUsaCA.NORTHERN_CALIFORNIA.id
    private val bay = StateUsaCA.BAY_AREA_SACRAMENTO.id
    private val stations = listOf(testStation(id = "K1", elevationMeters = 10), testStation(id = "K2", elevationMeters = 900))

    /** Counts how often work is handed to it, then runs it where it stands. */
    private class CountingDispatcher : CoroutineDispatcher() {
        var dispatched = 0
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatched++
            block.run()
        }
    }

    private fun store(assets: Set<String>, dispatcher: CoroutineDispatcher = Dispatchers.Unconfined) =
        AssetDatasetCatalog(
            listAssets = { assets },
            repository = StationRepository(FakeStationSource(stations), Dispatchers.Unconfined),
            stateUsa = StateUsaCA,
            ioDispatcher = dispatcher
        )

    @Test
    fun readsARegionsStationsFromItsPack() = runBlocking {
        assertEquals(stations, store(setOf("a.txt")).stationsFor(north))
    }

    @Test
    fun aRegionWithoutDataHasNoStations() = runBlocking {
        val store = store(setOf("a.txt"))
        assertTrue(store.stationsFor(bay).isEmpty())
        assertNull(store.datasetFor(bay))
    }

    @Test
    fun anUnknownRegionHasNoStations() = runBlocking {
        assertTrue(store(setOf("a.txt")).stationsFor("nowhere").isEmpty())
    }

    @Test
    fun readsOnTheStoresOwnIoDispatcher() = runBlocking {
        val io = CountingDispatcher()
        store(setOf("a.txt"), io).stationsFor(north)
        assertEquals(1, io.dispatched)
    }

    @Test
    fun summarisesADatasetFromItsStations() = runBlocking {
        val dataset = store(setOf("a.txt")).datasetFor(north)
        assertNotNull(dataset)
        assertEquals(2, dataset!!.stationCount)
        assertEquals(10, dataset.minElevationMeters)
        assertEquals(900, dataset.maxElevationMeters)
        assertEquals("a.txt", dataset.packFileName)
    }

    @Test
    fun readsADatasetOnTheStoresOwnIoDispatcher() = runBlocking {
        val io = CountingDispatcher()
        store(setOf("a.txt"), io).datasetFor(north)
        assertEquals(1, io.dispatched)
    }

    @Test
    fun aDatasetIsOnlyCachedOnceItHasBeenRead() = runBlocking {
        val store = store(setOf("a.txt"))
        assertNull(store.cachedDatasetFor(north))
        val read = store.datasetFor(north)
        assertEquals(read, store.cachedDatasetFor(north))
    }

    @Test
    fun theDefaultDatasetFallsBackToAnEmptyOneWhenItsRegionHasNoData() = runBlocking {
        val store = store(emptySet())
        val dataset = store.defaultDataset()
        assertEquals(DatasetKind.NONE, dataset.kind)
        assertEquals(0, dataset.stationCount)
        assertEquals(store.emptyDefaultDataset(), dataset)
    }

    @Test
    fun countsTheRegionsThatHaveData() {
        assertEquals(2, store(setOf("a.txt", "b.txt")).fullDataRegionCount())
        assertEquals(StateUsaCA.regions.size, store(emptySet()).totalRegionCount())
    }

    @Test
    fun aCsvOverrideWinsOverThePack() = runBlocking {
        val dataset = store(setOf("a.txt", "stations_$north.csv")).datasetFor(north)
        assertEquals("stations_$north.csv", dataset!!.fileName)
        assertNull(dataset.packFileName)
    }
}
