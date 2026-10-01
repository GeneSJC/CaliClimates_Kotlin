package io.github.genesjc.microclimatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

class ElevationEdgeCaseTest {
    @Test
    fun bandsChangeExactlyAtTheirFootThresholds() {
        // 30 m = 98 ft, 31 m = 102 ft (threshold 100 ft)
        assertEquals("Near sea level", Elevation.band(30))
        assertEquals("Lowlands", Elevation.band(31))
        // 152 m = 499 ft, 153 m = 502 ft (threshold 500 ft)
        assertEquals("Lowlands", Elevation.band(152))
        assertEquals("Foothills", Elevation.band(153))
        // 1,829 m = 6,001 ft is the first High country; 1,828 m = 5,997 ft is still Mountains
        assertEquals("Mountains", Elevation.band(1_828))
        assertEquals("High country", Elevation.band(1_829))
    }

    @Test
    fun belowSeaLevelStaysNearSeaLevel() {
        // The Central Valley data has stations at -3 m
        assertEquals(-10, Elevation.feet(-3))
        assertEquals("Near sea level", Elevation.band(-3))
        assertEquals("About sea level, so no cooling from height.", Elevation.note(-3, "central_valley"))
    }

    @Test
    fun coolingFollowsTheRuleOfThumb() {
        assertEquals(0, Elevation.coolerFahrenheit(0))
        assertEquals(11, Elevation.coolerFahrenheit(1_000)) // 3,281 ft x 3.5 F per 1,000 ft
    }

    @Test
    fun everyRegionHasLandmarksForTheHeightSentence() {
        val ids = listOf(
            "northern_california", "bay_area_sacramento", "central_coast",
            "central_valley", "los_angeles", "san_diego"
        )
        for (id in ids) {
            // 2,000 m is high enough to be compared with a landmark in every region
            val note = Elevation.note(2_000, id)
            assertTrue("$id: $note", "ft)." in note)
        }
    }

    @Test
    fun anUnknownOrMissingRegionGetsOnlyTheCooling() {
        assertEquals("Typically about 11°F cooler than at sea level.", Elevation.note(1_000, null))
        assertEquals("Typically about 11°F cooler than at sea level.", Elevation.note(1_000, "atlantis"))
    }

    @Test
    fun aHeightFarFromAnyLandmarkIsComparedToTheHighestOneBelow() {
        // 520 m = 1,706 ft in the Bay Area: not within 20% of Twin Peaks (925) or Mt. Tamalpais (2,570)
        assertEquals(
            "Higher than Twin Peaks (925 ft). Typically about 6°F cooler than at sea level.",
            Elevation.note(520, "bay_area_sacramento")
        )
    }
}
