package io.github.genesjc.microclimatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    @Test
    fun convertsCelsiusToFahrenheit() {
        assertEquals(32.0, Units.fahrenheit(0.0), 1e-9)
        assertEquals(212.0, Units.fahrenheit(100.0), 1e-9)
        assertEquals(-40.0, Units.fahrenheit(-40.0), 1e-9)
        assertEquals(68.0, Units.fahrenheit(20.0), 1e-9)
    }

    @Test
    fun convertsKilometresPerHourToMph() {
        assertEquals(0.0, Units.mph(0.0), 1e-9)
        assertEquals(62.1371, Units.mph(100.0), 1e-4)
    }

    @Test
    fun convertsHectopascalsToInchesOfMercury() {
        // Standard sea-level pressure
        assertEquals(29.92, Units.inHg(1013.25), 0.01)
    }
}
