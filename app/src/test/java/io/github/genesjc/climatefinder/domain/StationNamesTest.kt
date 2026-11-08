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
        assertEquals("Sacramento Mather", placeAndStationName("Sacramento", "Sacramento Mather"))
        assertEquals("Hollister, Ca", placeAndStationName("hollister", "Hollister, Ca"))
        // A town that already holds the station's name stands alone
        assertEquals("West Santa Monica", placeAndStationName("West Santa Monica", "Santa Monica"))
        assertEquals("Mid-Wilshire", stationDisplayName("Mid-Wilshire", "Mid-Wilshire"))
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
    fun aFacilityNameShowsThePlaceAlone() {
        // With a town on record: the town
        assertEquals("Burbank", stationDisplayName("Burbank", "Burbank Bob Hope Airport"))
        // Without one: the place part of the name, cut at a slash, a comma, a dash or "Near", and without the facility words
        assertEquals("Santa Maria", stationDisplayName(null, "Santa Maria Public Airport/capt G Allan Hancock"))
        assertEquals("Santa Barbara", stationDisplayName(null, "Santa Barbara, Santa Barbara Municipal Airport"))
        assertEquals("Lake Kaweah", stationDisplayName(null, "Lake Kaweah Weather Station Near Lemoncove 3 S"))
        // A name that does not describe a facility is put with its town as before
        assertEquals("Jackson (Mount Zion)", stationDisplayName("Jackson", "Mount Zion"))
        assertEquals("Rose Peak", stationDisplayName(null, "Rose Peak"))
    }

    @Test
    fun anAgencyCodeOrASiteNumberShowsThePlaceAlone() {
        assertEquals("Camarillo", stationDisplayName("Camarillo", "Vnc Station 54"))
        assertEquals("Apple Valley", stationDisplayName(null, "Apple Valley 2"))
        assertEquals("Crystal Lake", stationDisplayName(null, "Crystal Lake #3324"))
        assertEquals("Arnold", stationDisplayName("Arnold", "Btaws"))
    }

    @Test
    fun theTownTableNamesThePlacesOfTheBusiestFields() {
        assertEquals("Burbank", StateUsaCA.stationTowns["KBUR"])
        assertEquals("Oakland", StateUsaCA.stationTowns["KOAK"])
        // Two Sacramento stations are told apart by their towns
        assertEquals("Sacramento", StateUsaCA.stationTowns["KSMF"])
        assertEquals("South Sacramento", StateUsaCA.stationTowns["KSAC"])
    }

    @Test
    fun countyIsDroppedFromAFacilityNameButKeptInCountyLine() {
        assertEquals("Yuba", towns.cleanCityName("KXXX", "Yuba County Airport"))
        assertEquals("County Line", towns.cleanCityName("CLNC1", "County Line"))
    }
}
