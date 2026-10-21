package io.github.genesjc.microclimatefinder.data.remote

import io.github.genesjc.microclimatefinder.testStation
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
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

    private lateinit var cache: LiveObservationCache

    @Before
    fun setUp() {
        cache = LiveObservationCache(
            source = object : ObservationSource {
                override suspend fun fetchLatest(stationId: String): ObservationResult {
                    calls.incrementAndGet()
                    synchronized(fetched) { fetched.add(stationId) }
                    return next
                }
            },
            now = { clock }
        )
    }

    @Test
    fun nothingIsKnownBeforeTheFirstFetch() {
        assertNull(cache.peek("KTST"))
    }

    @Test
    fun aFetchedReadingIsKeptAndPeekable() = runBlocking {
        assertSame(reading, cache.get("KTST"))
        assertSame(reading, cache.peek("KTST"))
    }

    @Test
    fun aReadingIsReusedForFifteenMinutes() = runBlocking {
        cache.get("KTST")
        clock = 14 * minute + 59_000
        cache.get("KTST")
        assertEquals(1, calls.get())
        clock = 15 * minute
        cache.get("KTST")
        assertEquals(2, calls.get())
    }

    @Test
    fun aStationWithNoReadingIsNotRetriedForFiveMinutes() = runBlocking {
        next = ObservationResult.NoReading
        cache.get("KTST")
        clock = 4 * minute + 59_000
        cache.get("KTST")
        assertEquals(1, calls.get())
        clock = 5 * minute
        cache.get("KTST")
        assertEquals(2, calls.get())
    }

    @Test
    fun aFailedRequestIsNotRetriedForFiveMinutesEither() = runBlocking {
        next = ObservationResult.Failed
        assertSame(ObservationResult.Failed, cache.get("KTST"))
        clock = 4 * minute
        cache.get("KTST")
        assertEquals(1, calls.get())
        // ... and a later success replaces it
        next = reading
        clock = 5 * minute
        assertSame(reading, cache.get("KTST"))
        assertSame(reading, cache.peek("KTST"))
    }

    @Test
    fun stationsAreCachedIndependently() = runBlocking {
        cache.get("KAAA")
        cache.get("KBBB")
        cache.get("KAAA")
        assertEquals(2, calls.get())
    }

    @Test
    fun prefetchFetchesOnlyFeaturedStationsThatCanReportTemperature() = runBlocking {
        cache.prefetch(
            listOf(
                testStation(id = "FEAT1", isFeatured = true),
                testStation(id = "FEAT2", isFeatured = true, reportsTemp = true),
                testStation(id = "PLAIN", isFeatured = false),
                testStation(id = "NOTEMP", isFeatured = true, reportsTemp = false)
            )
        )
        assertEquals(setOf("FEAT1", "FEAT2"), fetched.toSet())
        assertNull(cache.peek("PLAIN"))
        assertNull(cache.peek("NOTEMP"))
    }

    @Test
    fun prefetchStopsAtTheLimit() = runBlocking {
        val stations = (1..LiveObservationCache.PREFETCH_LIMIT + 10).map { testStation(id = "S$it", isFeatured = true) }
        cache.prefetch(stations)
        assertEquals(LiveObservationCache.PREFETCH_LIMIT, calls.get())
        // The first ones in the list are the ones fetched
        assertSame(reading, cache.peek("S1"))
        assertNull(cache.peek("S${LiveObservationCache.PREFETCH_LIMIT + 1}"))
    }

    @Test
    fun prefetchWithNothingToFetchDoesNothing() = runBlocking {
        cache.prefetch(emptyList())
        cache.prefetch(listOf(testStation(isFeatured = false)))
        assertEquals(0, calls.get())
    }

    @Test
    fun prefetchReusesFreshReadings() = runBlocking {
        val stations = listOf(testStation(id = "KTST", isFeatured = true))
        cache.prefetch(stations)
        cache.prefetch(stations)
        assertEquals(1, calls.get())
    }
}
