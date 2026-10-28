package io.github.genesjc.climatefinder.domain

import io.github.genesjc.climatefinder.data.local.StationTowns
import io.github.genesjc.climatefinder.domain.model.StateUsaCA
import org.junit.Assert.assertEquals
import org.junit.Test

class StationNamesTest {

    private val towns = StationTowns(StateUsaCA.stationTowns)

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
        assertEquals("Jackson (Mount Zion)", towns.displayName("MTZC1", "Mount Zion"))
        assertEquals("Jackson", towns.getCleanCityName("MTZC1", "Mount Zion"))
        // the label does not change when the name already carries the town
        assertEquals("Jackson", towns.getCleanCityName("MTZC1", "Jackson (Mount Zion)"))
    }

    @Test
    fun aStationOutsideTheTownListKeepsItsOwnName() {
        assertEquals("Rose Peak", towns.displayName("RSPC1", "Rose Peak"))
        assertEquals("Rose Peak", towns.getCleanCityName("RSPC1", "Rose Peak"))
    }
}
