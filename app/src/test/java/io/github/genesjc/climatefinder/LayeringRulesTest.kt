package io.github.genesjc.climatefinder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The layering rules of the project conventions, as tests: layers depend in one direction. The screens depend on the
 * ViewModels and the domain; the ViewModels on the repositories; the data layer on the domain; the domain on nothing.
 *
 * - **domain** imports no Android, no resource ids and nothing from the other layers.
 * - **data** imports no Compose and nothing from the UI.
 * - **screens, components, text and theme** import no data-layer type, except the two terrain picture holders the
 *   terrain repository hands to the maps ([uiMayImportFromData]).
 * - **ViewModels** import no Compose.
 *
 * Plain file scans in the style of [io.github.genesjc.climatefinder.ui.WordingRulesTest].
 */
class LayeringRulesTest {

    private val base = "io.github.genesjc.climatefinder"

    /** The only data-layer types the UI may import: plain picture holders, which carry bitmaps, not data logic. */
    private val uiMayImportFromData = setOf("$base.data.local.TerrainImage", "$base.data.local.StateUsaTerrain")

    private class Source(val file: File, val area: String, val imports: List<String>)

    private fun sources(): List<Source> = File("src/main/java").walk()
        .filter { it.extension == "kt" }
        .map { file ->
            val after = file.invariantSeparatorsPath.substringAfter("/climatefinder/", "")
            val area = after.split('/').dropLast(1).take(2).joinToString("/")
            val imports = file.readLines().filter { it.startsWith("import ") }.map { it.removePrefix("import ").trim() }
            Source(file, area, imports)
        }
        .toList()

    private fun offenders(inArea: (String) -> Boolean, forbidden: (String) -> Boolean): List<String> =
        sources().filter { inArea(it.area) }.flatMap { s ->
            s.imports.filter(forbidden).map { "${s.file.name}: $it" }
        }

    @Test
    fun theDomainKnowsNoAndroidAndNoOtherLayer() {
        val layers = listOf("data", "ui", "viewmodel", "config", "di").map { "$base.$it." }
        val bad = offenders({ it == "domain" || it.startsWith("domain/") }) { imp ->
            imp.startsWith("android.") || imp.startsWith("androidx.") || imp == "$base.R" || layers.any { imp.startsWith(it) }
        }
        assertEquals("The domain must not depend on Android or on another layer", emptyList<String>(), bad)
    }

    @Test
    fun theDataLayerKnowsNoComposeAndNoUi() {
        val bad = offenders({ it == "data" || it.startsWith("data/") }) { imp ->
            imp.startsWith("androidx.compose") || imp.startsWith("$base.ui.") || imp.startsWith("$base.viewmodel.")
        }
        assertEquals("The data layer must not depend on Compose or the UI", emptyList<String>(), bad)
    }

    @Test
    fun screensAndComponentsImportNoDataLayerTypes() {
        val views = setOf("ui/screens", "ui/components", "ui/text", "ui/theme")
        val bad = offenders({ it in views }) { imp -> imp.startsWith("$base.data.") && imp !in uiMayImportFromData }
        assertEquals("Screens depend on the domain and on ViewModels, not on the data layer", emptyList<String>(), bad)
    }

    @Test
    fun viewModelsHoldNoComposeTypes() {
        val bad = sources().filter { it.file.name.endsWith("ViewModel.kt") }.flatMap { s ->
            s.imports.filter { it.startsWith("androidx.compose") }.map { "${s.file.name}: $it" }
        }
        assertEquals("A ViewModel must not depend on Compose", emptyList<String>(), bad)
    }
}
