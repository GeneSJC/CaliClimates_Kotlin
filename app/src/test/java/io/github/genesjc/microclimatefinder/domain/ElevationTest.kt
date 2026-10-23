package io.github.genesjc.microclimatefinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationTest {
    @Test
    fun convertsMetersToFeet() {
        assertEquals(0, Elevation.feet(0))
        assertEquals(3_281, Elevation.feet(1_000))
        assertEquals(1_640, Elevation.feet(500))
    }

    @Test
    fun picksTheBandByFeet() {
        assertEquals(ElevationBand.NEAR_SEA_LEVEL, Elevation.band(20))   // 66 ft
        assertEquals(ElevationBand.LOWLANDS, Elevation.band(100))        // 328 ft
        assertEquals(ElevationBand.FOOTHILLS, Elevation.band(340))       // 1,115 ft
        assertEquals(ElevationBand.HILLS, Elevation.band(700))           // 2,297 ft
        assertEquals(ElevationBand.MOUNTAINS, Elevation.band(1_500))     // 4,921 ft
        assertEquals(ElevationBand.HIGH_COUNTRY, Elevation.band(2_000))  // 6,562 ft
    }

    @Test
    fun everyBandHasItsOwnWording() {
        val ids = ElevationBand.values().map { it.labelRes }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun comparesWithALandmarkAndCoolsByTheRuleOfThumb() {
        // 1,170 m = 3,839 ft: within 20% of Mt. Diablo (3,850 ft); about 13 F cooler
        val diablo = Elevation.note(1_170, "bay_area_sacramento")
        assertEquals(LandmarkComparison("Mt. Diablo", 3_850, isNear = true), diablo.landmark)
        assertEquals(13, diablo.coolerFahrenheit)
        // 2,950 m = 9,678 ft in Los Angeles: near Mt. Baldy (10,060 ft)
        val baldy = Elevation.note(2_950, "los_angeles")
        assertEquals(LandmarkComparison("Mt. Baldy", 10_060, isNear = true), baldy.landmark)
        assertEquals(34, baldy.coolerFahrenheit)
        // Sea level: no landmark, no cooling
        val seaLevel = Elevation.note(0, "san_diego")
        assertNull(seaLevel.landmark)
        assertEquals(0, seaLevel.coolerFahrenheit)
    }
}

class ElevationEdgeCaseTest {
    @Test
    fun bandsChangeExactlyAtTheirFootThresholds() {
        // 30 m = 98 ft, 31 m = 102 ft (threshold 100 ft)
        assertEquals(ElevationBand.NEAR_SEA_LEVEL, Elevation.band(30))
        assertEquals(ElevationBand.LOWLANDS, Elevation.band(31))
        // 152 m = 499 ft, 153 m = 502 ft (threshold 500 ft)
        assertEquals(ElevationBand.LOWLANDS, Elevation.band(152))
        assertEquals(ElevationBand.FOOTHILLS, Elevation.band(153))
        // 1,829 m = 6,001 ft is the first High country; 1,828 m = 5,997 ft is still Mountains
        assertEquals(ElevationBand.MOUNTAINS, Elevation.band(1_828))
        assertEquals(ElevationBand.HIGH_COUNTRY, Elevation.band(1_829))
    }

    @Test
    fun belowSeaLevelStaysNearSeaLevel() {
        // The Central Valley data has stations at -3 m
        assertEquals(-10, Elevation.feet(-3))
        assertEquals(ElevationBand.NEAR_SEA_LEVEL, Elevation.band(-3))
        val note = Elevation.note(-3, "central_valley")
        assertNull(note.landmark)
        assertEquals(0, note.coolerFahrenheit)
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
            assertNotNull("$id has a landmark", Elevation.note(2_000, id).landmark)
        }
    }

    @Test
    fun anUnknownOrMissingRegionGetsOnlyTheCooling() {
        for (regionId in listOf(null, "atlantis")) {
            val note = Elevation.note(1_000, regionId)
            assertNull(note.landmark)
            assertEquals(11, note.coolerFahrenheit)
        }
    }

    @Test
    fun aHeightFarFromAnyLandmarkIsComparedToTheHighestOneBelow() {
        // 520 m = 1,706 ft in the Bay Area: not within 20% of Twin Peaks (925) or Mt. Tamalpais (2,570)
        val note = Elevation.note(520, "bay_area_sacramento")
        assertEquals(LandmarkComparison("Twin Peaks", 925, isNear = false), note.landmark)
        assertEquals(6, note.coolerFahrenheit)
        assertTrue(!note.landmark!!.isNear)
    }
}
