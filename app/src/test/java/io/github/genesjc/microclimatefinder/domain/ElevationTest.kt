package io.github.genesjc.microclimatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationTest {
    @Test
    fun convertsMetersToFeet() {
        assertEquals(0, Elevation.feet(0))
        assertEquals(3_281, Elevation.feet(1_000))
        assertEquals("1,640 ft", Elevation.formatFeet(500))
    }

    @Test
    fun picksTheBandByFeet() {
        assertEquals("Near sea level", Elevation.band(20))   // 66 ft
        assertEquals("Lowlands", Elevation.band(100))        // 328 ft
        assertEquals("Foothills", Elevation.band(340))       // 1,115 ft
        assertEquals("Hills", Elevation.band(700))           // 2,297 ft
        assertEquals("Mountains", Elevation.band(1_500))     // 4,921 ft
        assertEquals("High country", Elevation.band(2_000))  // 6,562 ft
    }

    @Test
    fun describesAndRanges() {
        assertEquals("1,115 ft · Foothills", Elevation.describe(340))
        assertEquals("66–9,678 ft", Elevation.rangeFeet(20, 2_950))
    }
}
