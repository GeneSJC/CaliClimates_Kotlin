package io.github.genesjc.microclimatefinder.data.local

import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.testStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

class StationRepositoryTest {

    private val dataset = StationDatasetInfo(
        regionId = "a", fileName = "a.txt", title = "North", subtitle = "",
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
