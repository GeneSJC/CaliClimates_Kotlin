package io.github.genesjc.climatefinder.data.remote

import io.github.genesjc.climatefinder.testStation
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import io.github.genesjc.climatefinder.domain.model.LiveObservation
import io.github.genesjc.climatefinder.domain.model.ObservationResult

@OptIn(ExperimentalCoroutinesApi::class)
class LiveReadingsRepositoryTest {

    private val minute = 60_000L
    private var clock = 0L
    private val calls = AtomicInteger(0)
    private val fetched = mutableListOf<String>()
    private val reading = ObservationResult.Reading(LiveObservation(20.0, 50.0, 10.0, 1013.0, 0L))
    private var next: ObservationResult = reading
    private var onFetch: suspend (String) -> Unit = {}

    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private lateinit var readings: LiveReadingsRepository

    @Before
    fun setUp() {
        readings = LiveReadingsRepository(
            source = object : ObservationSource {
                override suspend fun fetchLatest(stationId: String): ObservationResult {
                    calls.incrementAndGet()
                    synchronized(fetched) { fetched.add(stationId) }
                    onFetch(stationId)
                    return next
                }
            },
            scope = scope,
            now = { clock }
        )
    }

    @Test
    fun nothingIsKnownBeforeTheFirstFetch() {
        assertNull(readings.peek("KTST"))
    }

    @Test
    fun aFetchedReadingIsKeptAndPeekable() = runBlocking {
        assertSame(reading, readings.get("KTST"))
        assertSame(reading, readings.peek("KTST"))
    }

    @Test
    fun aReadingIsReusedForFifteenMinutes() = runBlocking {
        readings.get("KTST")
        clock = 14 * minute + 59_000
        readings.get("KTST")
        assertEquals(1, calls.get())
        clock = 15 * minute
        readings.get("KTST")
        assertEquals(2, calls.get())
    }

    @Test
    fun aStationWithNoReadingIsNotRetriedForFiveMinutes() = runBlocking {
        next = ObservationResult.NoReading
        readings.get("KTST")
        clock = 4 * minute + 59_000
        readings.get("KTST")
        assertEquals(1, calls.get())
        clock = 5 * minute
        readings.get("KTST")
        assertEquals(2, calls.get())
    }

    @Test
    fun aFailedRequestIsNotRetriedForFiveMinutesEither() = runBlocking {
        next = ObservationResult.Failed
        assertSame(ObservationResult.Failed, readings.get("KTST"))
        clock = 4 * minute
        readings.get("KTST")
        assertEquals(1, calls.get())
        // ... and a later success replaces it
        next = reading
        clock = 5 * minute
        assertSame(reading, readings.get("KTST"))
        assertSame(reading, readings.peek("KTST"))
    }

    @Test
    fun stationsAreHeldIndependently() = runBlocking {
        readings.get("KAAA")
        readings.get("KBBB")
        readings.get("KAAA")
        assertEquals(2, calls.get())
    }

    @Test
    fun twoCallersForTheSameStationShareOneRequest() = runBlocking {
        val hold = CompletableDeferred<Unit>()
        onFetch = { hold.await() }
        val first = async { readings.get("KTST") }
        val second = async { readings.get("KTST") }
        hold.complete(Unit)
        assertSame(reading, first.await())
        assertSame(reading, second.await())
        assertEquals(1, calls.get())
    }

    @Test
    fun overlappingFetchesOfDifferentStationsAreBothKept() = runBlocking {
        val hold = CompletableDeferred<Unit>()
        onFetch = { hold.await() }
        val a = async { readings.get("KAAA") }
        val b = async { readings.get("KBBB") }
        hold.complete(Unit)
        a.await(); b.await()
        assertSame(reading, readings.peek("KAAA"))
        assertSame(reading, readings.peek("KBBB"))
    }

    @Test
    fun prefetchFetchesOnlyFeaturedStationsThatCanReportTemperature() = runBlocking {
        readings.prefetch(
            listOf(
                testStation(id = "FEAT1", isFeatured = true),
                testStation(id = "FEAT2", isFeatured = true, reportsTemp = true),
                testStation(id = "PLAIN", isFeatured = false),
                testStation(id = "NOTEMP", isFeatured = true, reportsTemp = false)
            )
        )
        assertEquals(setOf("FEAT1", "FEAT2"), fetched.toSet())
        assertNull(readings.peek("PLAIN"))
        assertNull(readings.peek("NOTEMP"))
    }

    @Test
    fun prefetchStopsAtTheLimit() = runBlocking {
        val stations = (1..LiveReadingsRepository.PREFETCH_LIMIT + 10).map { testStation(id = "S$it", isFeatured = true) }
        readings.prefetch(stations)
        assertEquals(LiveReadingsRepository.PREFETCH_LIMIT, calls.get())
        // The first ones in the list are the ones fetched
        assertSame(reading, readings.peek("S1"))
        assertNull(readings.peek("S${LiveReadingsRepository.PREFETCH_LIMIT + 1}"))
    }

    @Test
    fun prefetchWithNothingToFetchDoesNothing() = runBlocking {
        readings.prefetch(emptyList())
        readings.prefetch(listOf(testStation(isFeatured = false)))
        assertEquals(0, calls.get())
    }

    @Test
    fun prefetchReusesFreshReadings() = runBlocking {
        val stations = listOf(testStation(id = "KTST", isFeatured = true))
        readings.prefetch(stations)
        readings.prefetch(stations)
        assertEquals(1, calls.get())
    }

    @Test
    fun openingAVisitFetchesTheRegionsFeaturedStations() {
        readings.open(listOf(testStation(id = "K1", isFeatured = true), testStation(id = "K2", isFeatured = false)))
        assertEquals(listOf("K1"), fetched)
        assertSame(reading, readings.peek("K1"))
    }

    @Test
    fun openingTheNextVisitCancelsTheLastVisitsRequests() {
        val cancelledFor = mutableListOf<String>()
        onFetch = { id ->
            try {
                awaitCancellation()
            } finally {
                cancelledFor += id
            }
        }
        readings.open(listOf(testStation(id = "A1", isFeatured = true)))
        assertEquals(listOf("A1"), fetched)
        readings.open(listOf(testStation(id = "B1", isFeatured = true)))
        assertEquals(listOf("A1"), cancelledFor)
        assertEquals(listOf("A1", "B1"), fetched)
    }

    @Test
    fun readingsSurviveAcrossVisits() {
        val stations = listOf(testStation(id = "K1", isFeatured = true))
        readings.open(stations)
        readings.open(listOf(testStation(id = "K9", isFeatured = true)))
        readings.open(stations)
        assertEquals(listOf("K1", "K9"), fetched)
    }

    @Test
    fun startsFromTheKnownReadingThenShowsTheFetchedOne() = runBlocking {
        readings.get("K1")
        val flow = readings.flowFor(testStation(id = "K1"))
        assertEquals(reading, flow.value)
    }

    @Test
    fun aStationThatCannotReportIsNeverFetched() {
        val flow = readings.flowFor(testStation(id = "KNO", reportsTemp = false))
        assertEquals(ObservationResult.NoReading, flow.value)
        assertEquals(0, calls.get())
    }

    @Test
    fun oneValuePerStationAndEachAskRefreshesOnlyWhenStale() {
        val station = testStation(id = "K1")
        assertSame(readings.flowFor(station), readings.flowFor(station))
        assertEquals(1, calls.get())
        clock = 15 * minute
        readings.flowFor(station)
        assertEquals(2, calls.get())
    }

    @Test
    fun aCardsReadingIsNullUntilTheFetchAnswers() {
        val hold = CompletableDeferred<Unit>()
        onFetch = { hold.await() }
        val flow = readings.flowFor(testStation(id = "K1"))
        assertNull(flow.value)
        hold.complete(Unit)
        assertEquals(reading, flow.value)
    }
}
