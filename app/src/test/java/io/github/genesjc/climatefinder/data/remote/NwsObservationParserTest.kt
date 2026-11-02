package io.github.genesjc.climatefinder.data.remote

import java.time.Instant
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.genesjc.climatefinder.domain.model.LiveObservation
import io.github.genesjc.climatefinder.domain.model.ObservationResult

/** Reads api.weather.gov observation text: unit conversion, missing values, odd timestamps. */
class NwsObservationParserTest {

    private fun body(
        temperature: String = """{"unitCode":"wmoUnit:degC","value":18.3}""",
        wind: String = """{"unitCode":"wmoUnit:km_h-1","value":11.16}""",
        pressure: String = """{"unitCode":"wmoUnit:Pa","value":101530}""",
        humidity: String = """{"unitCode":"wmoUnit:percent","value":64.2}""",
        timestamp: String = "\"2026-10-01T05:53:00+00:00\""
    ) = """{"properties":{"timestamp":$timestamp,"temperature":$temperature,"windSpeed":$wind,""" +
        """"barometricPressure":$pressure,"relativeHumidity":$humidity}}"""

    private fun reading(text: String): LiveObservation {
        val result = NwsObservationClient.parseObservation(text)
        assertTrue("expected a reading, got $result", result is ObservationResult.Reading)
        return (result as ObservationResult.Reading).observation
    }

    @Test
    fun readsAFullObservation() {
        val o = reading(body())
        assertEquals(18.3, o.temperatureCelsius, 1e-9)
        assertEquals(64.2, o.humidityPercent!!, 1e-9)
        assertEquals(11.16, o.windSpeedKmh!!, 1e-9)
        assertEquals(1015.3, o.pressureHpa!!, 1e-9)
        assertEquals(Instant.parse("2026-10-01T05:53:00Z").toEpochMilli(), o.observedAtMillis)
    }

    @Test
    fun convertsAFahrenheitTemperature() {
        val o = reading(body(temperature = """{"unitCode":"wmoUnit:degF","value":68}"""))
        assertEquals(20.0, o.temperatureCelsius, 1e-9)
    }

    @Test
    fun convertsWindFromMetresPerSecond() {
        val o = reading(body(wind = """{"unitCode":"wmoUnit:m_s-1","value":10}"""))
        assertEquals(36.0, o.windSpeedKmh!!, 1e-9)
    }

    @Test
    fun leavesPressureAloneWhenAlreadyInHectopascals() {
        val o = reading(body(pressure = """{"unitCode":"wmoUnit:hPa","value":1015.3}"""))
        assertEquals(1015.3, o.pressureHpa!!, 1e-9)
    }

    @Test
    fun aMissingTemperatureIsNoReading() {
        val nullValue = body(temperature = """{"unitCode":"wmoUnit:degC","value":null}""")
        assertSame(ObservationResult.NoReading, NwsObservationClient.parseObservation(nullValue))
        val absent = """{"properties":{"timestamp":"2026-10-01T05:53:00+00:00"}}"""
        assertSame(ObservationResult.NoReading, NwsObservationClient.parseObservation(absent))
    }

    @Test
    fun otherMissingValuesStayNull() {
        val o = reading(
            body(
                wind = """{"unitCode":"wmoUnit:km_h-1","value":null}""",
                pressure = """{"unitCode":"wmoUnit:Pa","value":null}""",
                humidity = """{"unitCode":"wmoUnit:percent","value":null}"""
            )
        )
        assertEquals(18.3, o.temperatureCelsius, 1e-9)
        assertNull(o.windSpeedKmh)
        assertNull(o.pressureHpa)
        assertNull(o.humidityPercent)
    }

    @Test
    fun aMissingOrUnreadableTimestampIsUnknownNotAnError() {
        assertNull(reading(body(timestamp = "null")).observedAtMillis)
        assertNull(reading(body(timestamp = "\"\"")).observedAtMillis)
        assertNull(reading(body(timestamp = "\"yesterday afternoon\"")).observedAtMillis)
    }

    @Test
    fun readsATimestampWithAnOffset() {
        val o = reading(body(timestamp = "\"2026-10-01T05:53:00-07:00\""))
        assertEquals(Instant.parse("2026-10-01T12:53:00Z").toEpochMilli(), o.observedAtMillis)
    }

    @Test(expected = SerializationException::class)
    fun textThatIsNotAnObservationThrowsForTheClientToCatch() {
        NwsObservationClient.parseObservation("<html>Service unavailable</html>")
    }

    @Test(expected = SerializationException::class)
    fun anObservationWithoutPropertiesThrowsForTheClientToCatch() {
        NwsObservationClient.parseObservation("""{"type":"Feature"}""")
    }
}
