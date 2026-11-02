package io.github.genesjc.climatefinder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * The size limit of the project conventions as a test: no source file passes [LIMIT] lines. Files waiting for a split
 * are listed in [allowed]; the list must shrink, and a test fails when an entry is no longer needed.
 */
class FileSizeRulesTest {

    // Empty: every file is within the limit. A file that needs more room is listed here only until it is split.
    private val allowed = emptySet<String>()

    private fun lineCounts(): Map<String, Int> = File("src/main/java").walk()
        .filter { it.extension == "kt" }
        .associate { it.name to it.readLines().size }

    @Test
    fun noFileIsOverTheLimitUnlessListed() {
        val over = lineCounts().filter { it.value > LIMIT && it.key !in allowed }.keys.sorted()
        assertEquals("Files over $LIMIT lines:", emptyList<String>(), over)
    }

    @Test
    fun theAllowanceListHoldsNoStaleEntries() {
        val counts = lineCounts()
        val stale = allowed.filter { (counts[it] ?: 0) <= LIMIT }.sorted()
        assertEquals("Entries that are no longer over the limit:", emptyList<String>(), stale)
    }

    private companion object {
        const val LIMIT = 600
    }
}
