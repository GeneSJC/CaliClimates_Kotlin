package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.testStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import io.github.genesjc.climatefinder.domain.model.DatasetKind
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo

class StationRepositoryTest {

    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", kind = DatasetKind.BUNDLED_PACK,
        stationCount = 1, minElevationMeters = 0, maxElevationMeters = 1, packFileName = "a.txt"
    )
    private var reads = 0
    private var answer: List<MicroclimateStation> = listOf(testStation(id = "K1"))

    private val repository = StationRepository(object : StationSource {
        override fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation> {
            reads++
            return answer
        }
    }, Dispatchers.Unconfined)

    @Test
    fun readsADatasetOnceAndThenKeepsIt() {
        val first = repository.stations(dataset)
        assertSame(first, repository.stations(dataset))
        assertEquals(1, reads)
        assertSame(first, repository.peek(dataset))
    }

    @Test
    fun peekNeverReads() {
        assertNull(repository.peek(dataset))
        assertEquals(0, reads)
    }

    @Test
    fun anEmptyReadIsNotKeptSoItIsTriedAgain() {
        answer = emptyList()
        repository.stations(dataset)
        repository.stations(dataset)
        assertEquals(2, reads)
        assertNull(repository.peek(dataset))
    }

    @Test
    fun loadReadsOnceAndThenIsALookup() = runBlocking {
        val first = repository.load(dataset)
        assertSame(first, repository.load(dataset))
        assertEquals(1, reads)
        assertSame(first, repository.peek(dataset))
    }

    @Test
    fun loadOfAnEmptyReadIsTriedAgain() = runBlocking {
        answer = emptyList()
        repository.load(dataset)
        repository.load(dataset)
        assertEquals(2, reads)
    }
}
