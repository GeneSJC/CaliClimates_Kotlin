package io.github.genesjc.microclimatefinder.data.remote

import io.github.genesjc.microclimatefinder.testStation
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class LiveObservationCacheTest {

    private val minute = 60_000L
    private var clock = 0L
    private val calls = AtomicInteger(0)
    private val fetched = mutableListOf<String>()
    private val reading = ObservationResult.Reading(
        LiveObservation(20.0, 50.0, 10.0, 1013.0, 0L)
    )
    private var next: ObservationResult = reading

    @Before
    fun setUp() {
        LiveObservationCache.clear()
        LiveObservationCache.now = { clock }
        LiveObservationCache.fetcher = { id ->
            calls.incrementAndGet()
            synchronized(fetched) { fetched.add(id) }
            next
        }
    }

    @After
    fun tearDown() {
        LiveObservationCache.clear()
        LiveObservationCache.now = { System.currentTimeMillis() }
        LiveObservationCache.fetcher = { NwsObservationClient.fetchLatest(it) }
    }

    @Test
    fun nothingIsKnownBeforeTheFirstFetch() {
        assertNull(LiveObservationCache.peek("KTST"))
    }

    @Test
    fun aFetchedReadingIsKeptAndPeekable() = runBlocking {
        assertSame(reading, LiveObservationCache.get("KTST"))
        assertSame(reading, LiveObservationCache.peek("KTST"))
    }

    @Test
    fun aReadingIsReusedForFifteenMinutes() = runBlocking {
        LiveObservationCache.get("KTST")
        clock = 14 * minute + 59_000
        LiveObservationCache.get("KTST")
        assertEquals(1, calls.get())
        clock = 15 * minute
        LiveObservationCache.get("KTST")
        assertEquals(2, calls.get())
    }

    @Test
    fun aStationWithNoReadingIsNotRetriedForFiveMinutes() = runBlocking {
        next = ObservationResult.NoReading
        LiveObservationCache.get("KTST")
        clock = 4 * minute + 59_000
        LiveObservationCache.get("KTST")
        assertEquals(1, calls.get())
        clock = 5 * minute
        LiveObservationCache.get("KTST")
        assertEquals(2, calls.get())
    }

    @Test
    fun aFailedRequestIsNotRetriedForFiveMinutesEither() = runBlocking {
        next = ObservationResult.Failed
        assertSame(ObservationResult.Failed, LiveObservationCache.get("KTST"))
        clock = 4 * minute
        LiveObservationCache.get("KTST")
        assertEquals(1, calls.get())
        // ... and a later success replaces it
        next = reading
        clock = 5 * minute
        assertSame(reading, LiveObservationCache.get("KTST"))
        assertSame(reading, LiveObservationCache.peek("KTST"))
    }

    @Test
    fun stationsAreCachedIndependently() = runBlocking {
        LiveObservationCache.get("KAAA")
        LiveObservationCache.get("KBBB")
        LiveObservationCache.get("KAAA")
        assertEquals(2, calls.get())
    }

    @Test
    fun prefetchFetchesOnlyFeaturedStationsThatCanReportTemperature() = runBlocking {
        LiveObservationCache.prefetch(
            listOf(
                testStation(id = "FEAT1", isFeatured = true),
                testStation(id = "FEAT2", isFeatured = true, reportsTemp = true),
                testStation(id = "PLAIN", isFeatured = false),
                testStation(id = "NOTEMP", isFeatured = true, reportsTemp = false)
            )
        )
        assertEquals(setOf("FEAT1", "FEAT2"), fetched.toSet())
        assertNull(LiveObservationCache.peek("PLAIN"))
        assertNull(LiveObservationCache.peek("NOTEMP"))
    }

    @Test
    fun prefetchStopsAtTheLimit() = runBlocking {
        val stations = (1..LiveObservationCache.PREFETCH_LIMIT + 10).map { testStation(id = "S$it", isFeatured = true) }
        LiveObservationCache.prefetch(stations)
        assertEquals(LiveObservationCache.PREFETCH_LIMIT, calls.get())
        // The first ones in the list are the ones fetched
        assertSame(reading, LiveObservationCache.peek("S1"))
        assertNull(LiveObservationCache.peek("S${LiveObservationCache.PREFETCH_LIMIT + 1}"))
    }

    @Test
    fun prefetchWithNothingToFetchDoesNothing() = runBlocking {
        LiveObservationCache.prefetch(emptyList())
        LiveObservationCache.prefetch(listOf(testStation(isFeatured = false)))
        assertEquals(0, calls.get())
    }

    @Test
    fun prefetchReusesFreshReadings() = runBlocking {
        val stations = listOf(testStation(id = "KTST", isFeatured = true))
        LiveObservationCache.prefetch(stations)
        LiveObservationCache.prefetch(stations)
        assertEquals(1, calls.get())
    }
}
