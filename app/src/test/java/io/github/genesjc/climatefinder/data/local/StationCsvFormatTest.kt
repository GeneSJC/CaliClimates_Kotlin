package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.domain.model.TerrainType
import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import io.github.genesjc.climatefinder.domain.StationNameFormatter

/** The station CSV format: columns, aliases, defaults, flags and the terrain rules. */
class StationCsvFormatTest {

    private val format = StationCsvFormat(StationNameFormatter(StateUsaCA.stationTowns))

    private fun parse(csv: String) = format.parse(BufferedReader(StringReader(csv)))

    @Test
    fun readsTheColumnsOfOneStation() {
        val s = parse(
            "id,display_name,latitude,longitude,elevation_meters,terrain_type,note,tier\n" +
                "KSAC,Sacramento Executive,38.5,-121.49,5,valley,Hot and flat,featured\n"
        ).single()
        assertEquals("KSAC", s.id)
        assertEquals("Sacramento Executive", s.name)
        assertEquals(38.5, s.latitude, 1e-9)
        assertEquals(-121.49, s.longitude, 1e-9)
        assertEquals(5, s.elevationMeters)
        assertEquals(TerrainType.VALLEY_BASIN, s.terrain)
        assertEquals("Hot and flat", s.microclimateNote)
        assertTrue(s.isFeatured)
    }

    @Test
    fun aStationInTheTownListKeepsItsOwnNameAndGainsItsTown() {
        val s = parse("id,display_name,elevation_meters\nMTZC1,Mount Zion,903\n").single()
        assertEquals("Mount Zion", s.name)
        assertEquals("Jackson", s.town)
        // the screens show them together
        assertEquals("Jackson (Mount Zion)", s.displayName)
    }

    @Test
    fun aStationOutsideTheTownListHasNoTown() {
        val s = parse("id,display_name,elevation_meters\nRSPC1,Rose Peak,1000\n").single()
        assertEquals("Rose Peak", s.name)
        assertNull(s.town)
        assertEquals("Rose Peak", s.displayName)
    }

    @Test
    fun acceptsTheAlternativeColumnNames() {
        val s = parse("station_id,station_name,lat,lon,elevation_m\nKXYZ,Alt Names,37.0,-122.0,100\n").single()
        assertEquals("KXYZ", s.id)
        assertEquals("Alt Names", s.name)
        assertEquals(37.0, s.latitude, 1e-9)
        assertEquals(-122.0, s.longitude, 1e-9)
        assertEquals(100, s.elevationMeters)
    }

    @Test
    fun headerNamesIgnoreCaseAndASpreadsheetByteOrderMark() {
        val s = parse("﻿ID,Name,Latitude\nKABC,Cased,36.5\n").single()
        assertEquals("KABC", s.id)
        assertEquals("Cased", s.name)
        assertEquals(36.5, s.latitude, 1e-9)
    }

    @Test
    fun aQuotedFieldMayContainCommas() {
        val s = parse("id,name,elevation_meters\nKQ,\"Smith, Field\",20\n").single()
        assertEquals("Smith, Field", s.name)
        assertEquals(20, s.elevationMeters)
    }

    @Test
    fun skipsBlankLinesAndEmptyInput() {
        assertTrue(parse("").isEmpty())
        assertTrue(parse("id,name\n").isEmpty())
        assertEquals(2, parse("id,name\nA,One\n\n   \nB,Two\n").size)
    }

    @Test
    fun missingValuesFallBackToDefaults() {
        val rows = parse("id,name,latitude,longitude,elevation_meters\n,,,,\nKB,Bad Numbers,north,west,high\n")
        assertEquals("STATION_0", rows[0].id)
        assertEquals("Station 0", rows[0].name)
        assertEquals(38.5816, rows[0].latitude, 1e-9)
        assertEquals(-121.4944, rows[0].longitude, 1e-9)
        assertEquals(15, rows[0].elevationMeters)
        // Text that is not a number is treated as missing, not as an error
        assertEquals("KB", rows[1].id)
        assertEquals(38.5816, rows[1].latitude, 1e-9)
        assertEquals(15, rows[1].elevationMeters)
    }

    @Test
    fun theLiveReportFlagsReadTrueFalseOrUnknown() {
        val rows = parse(
            "id,name,reports_temp,reports_humidity,reports_wind\n" +
                "A,One,TRUE,false,\n" +
                "B,Two,maybe,TRUE,FALSE\n" +
                "C,Three,,,\n"
        )
        assertEquals(true, rows[0].reportsTemp)
        assertEquals(false, rows[0].reportsHumidity)
        assertNull(rows[0].reportsWind)
        assertNull(rows[1].reportsTemp)
        assertEquals(true, rows[1].reportsHumidity)
        assertEquals(false, rows[1].reportsWind)
        assertNull(rows[2].reportsTemp)
    }

    @Test
    fun featuredIsReadFromTheTierColumnIgnoringCase() {
        val rows = parse("id,name,tier\nA,One,featured\nB,Two,Featured\nC,Three,standard\nD,Four,\n")
        assertEquals(listOf(true, true, false, false), rows.map { it.isFeatured })
        // No tier column at all: nobody is featured
        assertFalse(parse("id,name\nA,One\n").single().isFeatured)
    }

    @Test
    fun terrainComesFromTheTextAndTheHeight() {
        fun terrain(text: String, elevation: Int) =
            parse("id,terrain_type,elevation_meters\nT,$text,$elevation\n").single().terrain
        assertEquals(TerrainType.RIDGE, terrain("mountain ridge", 300))
        assertEquals(TerrainType.RIDGE, terrain("Sierra foothill", 300))
        assertEquals(TerrainType.COASTAL, terrain("coast", 10))
        assertEquals(TerrainType.COASTAL, terrain("Delta", 3))
        assertEquals(TerrainType.URBAN_CANYON, terrain("urban core", 40))
        assertEquals(TerrainType.URBAN_CANYON, terrain("City", 40))
        assertEquals(TerrainType.VALLEY_BASIN, terrain("plain", 40))
        // Above 500 m is a ridge whatever the text says
        assertEquals(TerrainType.RIDGE, terrain("coast", 501))
        assertEquals(TerrainType.COASTAL, terrain("coast", 500))
    }

    @Test
    fun withoutANoteTheStationHasNoNoteAndTheScreensWriteOneFromTheTerrain() {
        // The data layer reports the missing note as a fact; the wording is a string resource chosen by the screen
        val ridge = parse("id,name,elevation_meters\nR,Ridge,1000\n").single()
        assertNull(ridge.microclimateNote)
        assertEquals(TerrainType.RIDGE, ridge.terrain)
        val coast = parse("id,name,terrain_type,elevation_meters\nC,Coast,coast,10\n").single()
        assertNull(coast.microclimateNote)
        assertEquals(TerrainType.COASTAL, coast.terrain)
    }

    @Test
    fun theSampleReadingFallsWithHeight() {
        val low = parse("id,name,elevation_meters\nL,Low,0\n").single().currentReading
        val high = parse("id,name,elevation_meters\nH,High,1000\n").single().currentReading
        // About 6.5 C cooler per 1,000 m, and lower pressure
        assertEquals(28.0, low.temperatureCelsius, 1e-9)
        assertEquals(21.5, high.temperatureCelsius, 1e-9)
        assertTrue(high.pressureHpa < low.pressureHpa)
        assertEquals(1013.25, low.pressureHpa, 1e-9)
    }

    @Test
    fun theSampleHumidityAndWindStayInTheirLimits() {
        val extreme = parse("id,name,elevation_meters\nX,Tall,9000\nN,Deep,-3\n")
        assertEquals(85, extreme[0].currentReading.humidityPercent)   // 34 + 200 capped at 85
        assertTrue(extreme[1].currentReading.humidityPercent >= 20)
        assertTrue(extreme[1].currentReading.windSpeedKmh >= 3.0)
    }
}
