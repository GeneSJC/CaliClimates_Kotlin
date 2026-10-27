package io.github.genesjc.microclimatefinder.ui.ads

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The isolation rule for advertising as a test: nothing outside the commercial source set may import the ads or consent
 * libraries, so the open build can have none of them. The foss-clean script proves it for the built app; this proves it for
 * the source.
 */
class AdsBoundaryTest {

    private val forbidden = Regex("""^\s*import\s+com\.google\.android\.(gms\.ads|ump)\b""")

    private fun offenders(folder: String): List<String> =
        File("src/$folder").walk()
            .filter { it.extension == "kt" }
            .filter { file -> file.readLines().any { forbidden.containsMatchIn(it) } }
            .map { it.path }
            .toList()

    @Test
    fun onlyTheCommercialModuleReadsTheAdsSwitch() {
        val found = listOf("main", "foss", "core", "demo", "test").flatMap { folder ->
            File("src/$folder").walk().filter { it.extension == "kt" && it.name != "AdsBoundaryTest.kt" }
                .filter { file -> file.readLines().any { it.contains("ADS_ENABLED") && !it.trimStart().startsWith("*") && !it.trimStart().startsWith("//") } }
                .map { it.path }.toList()
        }
        assertTrue("The ads switch is read outside the commercial module: $found", found.isEmpty())
    }

    @Test
    fun noAdsLibraryIsImportedByTheSharedOrOpenSource() {
        val found = listOf("main", "foss", "core", "demo", "test").flatMap { offenders(it) }
        assertTrue("Ads imports outside src/commercial: $found", found.isEmpty())
    }
}
