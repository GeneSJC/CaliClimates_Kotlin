package io.github.genesjc.climatefinder.ui.screens

import io.github.genesjc.climatefinder.testStation
import org.junit.Assert.assertEquals
import org.junit.Test

class ElevationLabelsTest {
    private fun at(vararg meters: Int) = meters.map { testStation(id = "S$it", elevationMeters = it) }

    @Test
    fun secondFromTheFarEndOfEachSideIsAReference() {
        // Selected at 500 m; below: 400, 300, 200 (far end 200); above: 600, 700, 800 (far end 800)
        val refs = referenceStationIds(at(200, 300, 400, 600, 700, 800), selectedMeters = 500)
        assertEquals(setOf("S300", "S700"), refs)
    }

    @Test
    fun aSideWithOneLabelUsesThatLabel() {
        val refs = referenceStationIds(at(400, 600, 700, 800), selectedMeters = 500)
        assertEquals(setOf("S400", "S700"), refs)
    }

    @Test
    fun aSideWithSixOrMoreAlsoGetsAMiddleReference() {
        // Above: 600..1100 (six labels); far end 1100, second from it 1000, middle index 3 -> 900
        val refs = referenceStationIds(at(400, 600, 700, 800, 900, 1000, 1100), selectedMeters = 500)
        assertEquals(setOf("S400", "S1000", "S900"), refs)
    }

    @Test
    fun noLabelsMeansNoReferences() {
        assertEquals(emptySet<String>(), referenceStationIds(emptyList(), selectedMeters = 500))
    }
}
