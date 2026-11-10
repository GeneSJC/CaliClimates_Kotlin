package io.github.genesjc.climatefinder

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The FOSS build's violet-mountain launcher icon is an overlay: files in `app/src/foss/res` that Android's resource merge puts
 * over the green ones in `app/src/main/res`. These checks keep the overlay exactly what the generator
 * (`docs/images/make_icon.py --variant foss`) writes, so a missing, extra or stale file fails a test and not a release.
 * Plain file scans, like the other rule tests; they run from the app module directory.
 */
class IconVariantFilesTest {

    private val main = File("src/main/res")
    private val overlay = File("src/foss/res")

    /** The files whose pixels or colours change with the mountain's colour, and nothing else. */
    private val expected = setOf(
        "drawable/ic_launcher_foreground.xml",
        "drawable/ic_launcher_vector.xml"
    ) + listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").flatMap {
        listOf("mipmap-$it/ic_launcher.webp", "mipmap-$it/ic_launcher_round.webp")
    }

    private fun overlayFiles(): Set<String> =
        overlay.walk().filter { it.isFile }.map { it.relativeTo(overlay).invariantSeparatorsPath }.toSet()

    @Test
    fun theFossOverlayHoldsExactlyTheFilesThatChange() {
        assertEquals("Files in app/src/foss/res", expected, overlayFiles())
    }

    @Test
    fun noOverlayFileIsACopyOfTheMainOne() {
        val copies = overlayFiles().filter { File(main, it).readBytes().contentEquals(File(overlay, it).readBytes()) }
        assertEquals("Overlay files identical to main (copy only what differs)", emptyList<String>(), copies)
    }

    @Test
    fun everyOverlayFileReplacesAMainFile() {
        // A file with no main counterpart (or a different extension) would be an extra resource, not a replacement
        val orphans = overlayFiles().filter { !File(main, it).isFile }
        assertEquals("Overlay files with no file of the same name in main", emptyList<String>(), orphans)
    }

    @Test
    fun theFossForegroundHasTheVioletMountainAndMainKeepsTheGreen() {
        val violet = listOf("#FF9B6BE0", "#FF6B45B0")
        val green = listOf("#FF35B57A", "#FF1F865C")
        val foss = File(overlay, "drawable/ic_launcher_foreground.xml").readText()
        val base = File(main, "drawable/ic_launcher_foreground.xml").readText()
        violet.forEach { assertTrue("foss foreground lacks $it", it in foss) }
        green.forEach { assertFalse("foss foreground still has $it", it in foss) }
        green.forEach { assertTrue("main foreground lacks $it", it in base) }
        violet.forEach { assertFalse("main foreground has $it", it in base) }
    }
}
