package io.github.genesjc.climatefinder.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveObservationTest {
    private fun observation(observedAtMillis: Long?) = LiveObservation(
        temperatureCelsius = 20.0,
        humidityPercent = null,
        windSpeedKmh = null,
        pressureHpa = null,
        observedAtMillis = observedAtMillis
    )

    @Test
    fun convertsItsTemperatureToFahrenheit() {
        assertEquals(68.0, observation(null).temperatureFahrenheit, 1e-9)
    }

    @Test
    fun ageIsWholeMinutesSinceTheObservation() {
        assertEquals(90L, observation(0L).ageMinutes(now = 90 * 60_000L))
        assertEquals(1L, observation(0L).ageMinutes(now = 119_999L))
        assertEquals(0L, observation(0L).ageMinutes(now = 59_999L))
    }

    @Test
    fun ageIsNeverNegative() {
        // A station clock a little ahead of the phone's
        assertEquals(0L, observation(10 * 60_000L).ageMinutes(now = 0L))
    }

    @Test
    fun ageIsUnknownWithoutATimestamp() {
        assertNull(observation(null).ageMinutes(now = 1_000L))
    }
}
