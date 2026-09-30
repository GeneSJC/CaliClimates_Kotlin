package io.github.genesjc.microclimatefinder.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StationPackTest {

    // Made by the converter script from SAMPLE_CSV
    private val samplePack =
        "TUNTUAE+h2PYRUgfji20YfkOcP3ClDdUK1aG4BIPs1Gon9mKdReJpj1hSafPvOZmwCKU/45RpiiFBIL3Mj5gzbwDbwi2XB4870Ss9FVj/rO4B/ph6D0Zx0wh6kNfsz/dYyS/cxVF3RUEucArpsaV5Z/kcvqTvpob4bECq7fSwUZOzcmUnpAK8FqpwjDnoNPv+Qxz"

    private val sampleCsv =
        "id,name,latitude,longitude,elevation_meters,tier\n" +
            "KTST,Test Field Airport,38.5,-121.5,12,featured\n" +
            "TST2,Second Station,39.1,-120.9,850,standard\n"

    @Test
    fun decodesAPackBackToItsCsv() {
        assertEquals(sampleCsv, StationPack.decode(samplePack))
    }

    @Test
    fun ignoresLineWrapsAndSurroundingWhitespace() {
        val wrapped = samplePack.chunked(40).joinToString("\n")
        assertEquals(sampleCsv, StationPack.decode("  $wrapped\n"))
    }

    @Test
    fun rejectsAPackWithOneCharacterChanged() {
        val i = samplePack.length / 2
        val changed = if (samplePack[i] == 'A') 'B' else 'A'
        val damaged = samplePack.substring(0, i) + changed + samplePack.substring(i + 1)
        assertNull(StationPack.decode(damaged))
    }

    @Test
    fun rejectsATruncatedPack() {
        assertNull(StationPack.decode(samplePack.substring(0, samplePack.length / 2)))
    }

    @Test
    fun rejectsTextThatIsNotAPack() {
        assertNull(StationPack.decode("not a pack!"))
        assertNull(StationPack.decode(""))
        assertNull(StationPack.decode("id,name\nKTST,Test\n"))
    }

    @Test
    fun decodesTheRealCsvHeaderShape() {
        // The decoded text is plain CSV the existing parser reads unchanged
        val csv = StationPack.decode(samplePack)
        assertNotNull(csv)
        assertEquals("id", csv!!.lineSequence().first().split(",").first())
    }
}
