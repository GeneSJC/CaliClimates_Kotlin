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

    @Test
    fun comparesWithALandmarkAndCoolsByTheRuleOfThumb() {
        // 1,170 m = 3,839 ft: within 20% of Mt. Diablo (3,850 ft); about 13 F cooler
        assertEquals(
            "About as high as Mt. Diablo (3,850 ft). Typically about 13\u00B0F cooler than at sea level.",
            Elevation.note(1_170, "bay_area_sacramento")
        )
        // 2,950 m = 9,678 ft in Los Angeles: higher than Big Bear Lake, near Mt. Baldy (10,060 ft)
        assertEquals(
            "About as high as Mt. Baldy (10,060 ft). Typically about 34\u00B0F cooler than at sea level.",
            Elevation.note(2_950, "los_angeles")
        )
        // Sea level: no landmark, no cooling
        assertEquals("About sea level, so no cooling from height.", Elevation.note(0, "san_diego"))
    }
}
