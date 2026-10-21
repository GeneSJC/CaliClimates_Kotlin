package io.github.genesjc.microclimatefinder.data.remote

import io.github.genesjc.microclimatefinder.testStation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LiveReadingsTest {

    private val scope = CoroutineScope(UnconfinedTestDispatcher())
    private val reading = ObservationResult.Reading(LiveObservation(20.0, null, null, null, null))

    @Test
    fun startsFromTheKnownReadingThenShowsTheFetchedOne() {
        val fetched = mutableListOf<String>()
        val readings = LiveReadings(scope = scope, peek = { ObservationResult.NoReading }, fetch = { fetched += it; reading })
        val flow = readings.flowFor(testStation(id = "K1"))
        assertEquals(reading, flow.value)
        assertEquals(listOf("K1"), fetched)
    }

    @Test
    fun aStationThatCannotReportIsNeverFetched() {
        var fetches = 0
        val readings = LiveReadings(scope = scope, peek = { null }, fetch = { fetches++; reading })
        val flow = readings.flowFor(testStation(id = "KNO", reportsTemp = false))
        assertEquals(ObservationResult.NoReading, flow.value)
        assertEquals(0, fetches)
    }

    @Test
    fun oneValuePerStationAndEachAskRefreshesIt() {
        var fetches = 0
        val readings = LiveReadings(scope = scope, peek = { null }, fetch = { fetches++; reading })
        val station = testStation(id = "K1")
        assertSame(readings.flowFor(station), readings.flowFor(station))
        assertEquals(2, fetches)
    }

    @Test
    fun isNullUntilTheFetchAnswers() {
        val hold = kotlinx.coroutines.CompletableDeferred<ObservationResult>()
        val readings = LiveReadings(scope = scope, peek = { null }, fetch = { hold.await() })
        val flow = readings.flowFor(testStation(id = "K1"))
        assertNull(flow.value)
        hold.complete(reading)
        assertEquals(reading, flow.value)
    }
}
