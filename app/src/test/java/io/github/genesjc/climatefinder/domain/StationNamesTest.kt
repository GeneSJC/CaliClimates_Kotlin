package io.github.genesjc.climatefinder.domain

import io.github.genesjc.climatefinder.config.StateUsaCA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StationNamesTest {

    private val towns = StationNameFormatter(StateUsaCA.stationTowns)

    @Test
    fun putsTheTownFirstWhenTheStationNameDoesNotSayIt() {
        assertEquals("Jackson (Mount Zion)", placeAndStationName("Jackson", "Mount Zion"))
    }

    @Test
    fun keepsAStationNameThatAlreadyNamesTheTown() {
        assertEquals("Sacramento Executive Airport", placeAndStationName("Sacramento", "Sacramento Executive Airport"))
        assertEquals("Hollister, Ca", placeAndStationName("hollister", "Hollister, Ca"))
    }

    @Test
    fun leavesBlankInputsAlone() {
        assertEquals("Mount Zion", placeAndStationName("", "Mount Zion"))
        assertEquals("", placeAndStationName("Jackson", ""))
    }

    @Test
    fun theTownListGivesTheCombinedNameAndTheShortLabel() {
        assertEquals("Jackson", towns.townOf("MTZC1"))
        assertEquals("Jackson", towns.cleanCityName("MTZC1", "Mount Zion"))
        // the label does not change when the name already carries the town
        assertEquals("Jackson", towns.cleanCityName("MTZC1", "Jackson (Mount Zion)"))
    }

    @Test
    fun aStationOutsideTheTownListKeepsItsOwnName() {
        assertNull(towns.townOf("RSPC1"))
        assertEquals("Rose Peak", towns.cleanCityName("RSPC1", "Rose Peak"))
    }

    @Test
    fun countyIsDroppedFromAFacilityNameButKeptInCountyLine() {
        assertEquals("Yuba", towns.cleanCityName("KXXX", "Yuba County Airport"))
        assertEquals("County Line", towns.cleanCityName("CLNC1", "County Line"))
    }
}
