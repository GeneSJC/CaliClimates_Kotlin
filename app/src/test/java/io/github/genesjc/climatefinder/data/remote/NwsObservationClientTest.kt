package io.github.genesjc.climatefinder.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.github.genesjc.climatefinder.domain.model.ObservationResult

/** The client over a local web server: the request it makes and what it does with each kind of answer. */
class NwsObservationClientTest {

    private val server = MockWebServer()
    private lateinit var client: NwsObservationClient

    private val observation = """{"properties":{"timestamp":"2026-10-01T05:53:00+00:00",""" +
        """"temperature":{"unitCode":"wmoUnit:degC","value":18.3},"windSpeed":{"unitCode":"wmoUnit:km_h-1","value":11.16},""" +
        """"barometricPressure":{"unitCode":"wmoUnit:Pa","value":101530},"relativeHumidity":{"unitCode":"wmoUnit:percent","value":64.2}}}"""

    @Before
    fun setUp() {
        server.start()
        client = NwsObservationClient(createNwsApi(createNwsHttpClient(), server.url("/").toString()))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun asksForTheStationsLatestObservationWithIdentifyingHeaders() = runBlocking {
        server.enqueue(MockResponse().setBody(observation))
        client.fetchLatest("KSFO")
        val request = server.takeRequest()
        assertEquals("/stations/KSFO/observations/latest", request.path)
        assertEquals("application/geo+json", request.getHeader("Accept"))
        assertTrue(request.getHeader("User-Agent")!!.startsWith("Cali Climates"))
    }

    @Test
    fun encodesAStationIdInThePath() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        client.fetchLatest("A B")
        assertEquals("/stations/A%20B/observations/latest", server.takeRequest().path)
    }

    @Test
    fun aGoodAnswerIsAReading() = runBlocking {
        server.enqueue(MockResponse().setBody(observation))
        val result = client.fetchLatest("KSFO")
        assertTrue("expected a reading, got $result", result is ObservationResult.Reading)
        assertEquals(18.3, (result as ObservationResult.Reading).observation.temperatureCelsius, 1e-9)
    }

    @Test
    fun anAnswerWithoutATemperatureIsNoReading() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"properties":{"timestamp":"2026-10-01T05:53:00+00:00"}}"""))
        assertSame(ObservationResult.NoReading, client.fetchLatest("KSFO"))
    }

    @Test
    fun aMissingStationIsNoReading() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertSame(ObservationResult.NoReading, client.fetchLatest("KNONE"))
    }

    @Test
    fun aServerErrorIsFailed() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503).setBody("busy"))
        assertSame(ObservationResult.Failed, client.fetchLatest("KSFO"))
    }

    @Test
    fun textThatIsNotAnObservationIsFailed() = runBlocking {
        server.enqueue(MockResponse().setBody("<html>Service unavailable</html>"))
        assertSame(ObservationResult.Failed, client.fetchLatest("KSFO"))
    }

    @Test
    fun aDroppedConnectionIsFailed() = runBlocking {
        // OkHttp retries a dropped connection once, so the second try is dropped too
        repeat(2) { server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)) }
        assertSame(ObservationResult.Failed, client.fetchLatest("KSFO"))
    }

    @Test
    fun anUnreachableServerIsFailed() = runBlocking {
        server.shutdown()
        assertSame(ObservationResult.Failed, client.fetchLatest("KSFO"))
    }
}
