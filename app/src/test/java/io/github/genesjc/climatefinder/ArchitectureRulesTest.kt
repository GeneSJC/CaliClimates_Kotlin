package io.github.genesjc.climatefinder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The structure rules of the project conventions, as tests, so that new code cannot drift while the old is cleaned up:
 * a class named for a data role lives in the data layer, the data and domain layers hold no screen names, a file holds
 * one public type and is named for it. Files that break a rule on purpose or are waiting to be split are listed in
 * [knownExceptions]; the list must shrink, and a test fails when an entry is no longer needed.
 *
 * Plain file scans in the style of [io.github.genesjc.climatefinder.ui.WordingRulesTest]; they need no library.
 */
class ArchitectureRulesTest {

    private val declaration = Regex(
        "^(?:(?:public|internal|data|sealed|enum|abstract|open|annotation|inline|value|fun) )*(?:class|object|interface)\\s+(\\w+)"
    )
    private val dataRoles = Regex("(Repository|Source|Client|Api|Dto|Catalog)$")
    private val screenNames = Regex("(Screen|ViewModel|UiState)$")

    /** Files that hold a small family of types on purpose, or are waiting for the decomposition of their screen. */
    private val knownExceptions = setOf(
        "HomeViewModel.kt",          // the screen's state, its events and the ViewModel read as one unit
        "StationListViewModel.kt",   // the same
        "NwsObservationDto.kt",      // the three shapes of one JSON answer
        "RegionPicker.kt",           // a composable and the state it is given
        "LandingScreen.kt",          // the screen and the row type of its region list
        "ElevationLabels.kt",        // label placement types (PlacedLabel, LabelHold), one family
        "ElevationScenery.kt",       // scenery types, moves with the Elevation chart decomposition
        "StationMapCanvas.kt"        // label types, moves with the map canvas split
    )

    private class Source(val file: File, val layer: String, val types: List<String>)

    private fun sources(): List<Source> = File("src/main/java").walk()
        .filter { it.extension == "kt" }
        .map { file ->
            val after = file.invariantSeparatorsPath.substringAfter("/climatefinder/", "")
            val layer = if ('/' in after) after.substringBefore('/') else ""
            val types = file.readLines()
                .filter { !it.startsWith("private") }
                .mapNotNull { declaration.find(it)?.groupValues?.get(1) }
            Source(file, layer, types)
        }
        .toList()

    @Test
    fun dataRolesLiveInTheDataLayer() {
        val offenders = sources().flatMap { s ->
            s.types.filter { dataRoles.containsMatchIn(it) && s.layer != "data" }.map { "${s.file.name}: $it" }
        }
        assertEquals("A class named for a data role must live in the data layer", emptyList<String>(), offenders)
    }

    @Test
    fun theDataAndDomainLayersHoldNoScreenNames() {
        val offenders = sources().filter { it.layer == "data" || it.layer == "domain" }.flatMap { s ->
            s.types.filter { screenNames.containsMatchIn(it) }.map { "${s.file.name}: $it" }
        }
        assertEquals("Screen, ViewModel and UiState names belong to the UI", emptyList<String>(), offenders)
    }

    /** Files that break "one public type per file, named for it", without the allowed ones. */
    private fun fileNameOffenders(): Set<String> = sources()
        .filter { s ->
            val stem = s.file.nameWithoutExtension
            s.types.size > 1 || (s.types.size == 1 && s.types.single() != stem)
        }
        .map { it.file.name }
        .toSet()

    @Test
    fun aFileHoldsOnePublicTypeAndIsNamedForIt() {
        val offenders = fileNameOffenders() - knownExceptions
        assertEquals("One public type per file, named for it (or list the file in knownExceptions)", emptySet<String>(), offenders)
    }

    @Test
    fun theListOfKnownExceptionsHasNoStaleEntries() {
        val stale = knownExceptions - fileNameOffenders()
        assertEquals("Remove these from knownExceptions: they now follow the rule", emptySet<String>(), stale)
    }
}
