package io.github.genesjc.climatefinder.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.genesjc.climatefinder.domain.model.LiveObservation
import io.github.genesjc.climatefinder.domain.model.ObservationResult

class ReadingCacheTest {

    private val minute = 60_000L
    private var clock = 0L
    private val cache = ReadingCache(now = { clock })
    private val reading = ObservationResult.Reading(LiveObservation(20.0, 50.0, 10.0, 1013.0, 0L))

    @Test
    fun nothingIsKnownUntilSomethingIsStored() {
        assertNull(cache.peek("KTST"))
        assertNull(cache.fresh("KTST"))
    }

    @Test
    fun aStoredResultCanBePeekedAndIsFresh() {
        cache.put("KTST", reading)
        assertSame(reading, cache.peek("KTST"))
        assertSame(reading, cache.fresh("KTST"))
    }

    @Test
    fun aReadingExpiresAfterFifteenMinutesButStaysPeekable() {
        cache.put("KTST", reading)
        clock = 14 * minute + 59_000
        assertSame(reading, cache.fresh("KTST"))
        clock = 15 * minute
        assertNull(cache.fresh("KTST"))
        assertSame(reading, cache.peek("KTST"))
    }

    @Test
    fun aFailureExpiresAfterFiveMinutes() {
        cache.put("KTST", ObservationResult.Failed)
        clock = 4 * minute + 59_000
        assertSame(ObservationResult.Failed, cache.fresh("KTST"))
        clock = 5 * minute
        assertNull(cache.fresh("KTST"))
    }

    @Test
    fun storingOneStationLeavesTheOthers() {
        cache.put("KONE", reading)
        cache.put("KTWO", ObservationResult.NoReading)
        assertSame(reading, cache.peek("KONE"))
        assertSame(ObservationResult.NoReading, cache.peek("KTWO"))
    }

    @Test
    fun changesStartsNullAndFollowsTheStoredResult() = runBlocking {
        assertNull(cache.changes("KTST").first())
        cache.put("KTST", reading)
        assertSame(reading, cache.changes("KTST").first())
    }

    @Test
    fun theFreshnessRuleIsTheSameAsTheCacheUses() {
        assertTrue(ReadingFreshness.isFresh(reading, ReadingFreshness.READING_MS - 1))
        assertFalse(ReadingFreshness.isFresh(reading, ReadingFreshness.READING_MS))
        assertTrue(ReadingFreshness.isFresh(ObservationResult.NoReading, ReadingFreshness.NEGATIVE_MS - 1))
        assertFalse(ReadingFreshness.isFresh(ObservationResult.NoReading, ReadingFreshness.NEGATIVE_MS))
        assertEquals(15 * minute, ReadingFreshness.READING_MS)
    }
}
