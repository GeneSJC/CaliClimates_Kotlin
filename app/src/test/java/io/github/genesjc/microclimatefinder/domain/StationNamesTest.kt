package io.github.genesjc.microclimatefinder.domain

import io.github.genesjc.microclimatefinder.data.local.StationTowns
import org.junit.Assert.assertEquals
import org.junit.Test

class StationNamesTest {

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
        assertEquals("Jackson (Mount Zion)", StationTowns.displayName("MTZC1", "Mount Zion"))
        assertEquals("Jackson", StationTowns.getCleanCityName("MTZC1", "Mount Zion"))
        // the label does not change when the name already carries the town
        assertEquals("Jackson", StationTowns.getCleanCityName("MTZC1", "Jackson (Mount Zion)"))
    }

    @Test
    fun aStationOutsideTheTownListKeepsItsOwnName() {
        assertEquals("Rose Peak", StationTowns.displayName("RSPC1", "Rose Peak"))
        assertEquals("Rose Peak", StationTowns.getCleanCityName("RSPC1", "Rose Peak"))
    }
}
