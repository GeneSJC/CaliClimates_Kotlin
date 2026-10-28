package io.github.genesjc.climatefinder.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The string resource conventions, as tests: no user-facing wording as a literal in the screens or the ViewModels,
 * and the resource files themselves written the agreed way. Wording is reviewed in the strings files, not frozen in tests.
 */
class WordingRulesTest {

    private val literal = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"")
    private val templates = listOf(Regex("\\$\\{[^}]*\\}"), Regex("\\$[A-Za-z_]\\w*"), Regex("\\\\u[0-9A-Fa-f]{4}"))
    private val skippedLines = listOf("//", "*", "/*", "import", "package", "@Suppress", "@OptIn", "@file")

    private fun uiAndViewModelFiles() = File("src/main/java").walk()
        .filter { it.extension == "kt" }
        .filter { val p = it.invariantSeparatorsPath; "/ui/" in p || "/viewmodel/" in p }

    /** A literal is wording when, ignoring templates and symbols, it has a word of two letters or more and reads as text. */
    private fun isWording(raw: String): Boolean {
        var text = raw
        templates.forEach { text = it.replace(text, "") }
        text = text.replace("\\n", " ")
        if (!Regex("[A-Za-z]{2,}").containsMatchIn(text)) return false
        return text.trim().contains(' ') || text.first().isUpperCase()
    }

    @Test
    fun noUserFacingWordingIsWrittenInTheScreensOrViewModels() {
        val offenders = uiAndViewModelFiles().flatMap { file ->
            file.readLines().withIndex().flatMap { (index, line) ->
                val trimmed = line.trim()
                if (skippedLines.any { trimmed.startsWith(it) } || "Suppress(" in trimmed) {
                    emptyList()
                } else {
                    val code = line.replace(Regex("//.*$"), "")
                    literal.findAll(code).map { it.groupValues[1] }.filter(::isWording)
                        .map { "${file.name}:${index + 1}: $it" }.toList()
                }
            }
        }.toList()
        assertTrue("User-facing wording written as a literal (use a string resource):\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    private fun resourceFiles() = File("src/main/res/values").listFiles { f -> f.name.startsWith("strings") && f.extension == "xml" }
        .orEmpty().sortedBy { it.name }

    private val entry = Regex("<(string|plurals|string-array)\\s+name=\"([^\"]+)\"[^>]*>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)

    @Test
    fun everyResourceNameIsUniqueAcrossTheStringsFiles() {
        val names = resourceFiles().flatMap { f -> entry.findAll(f.readText()).map { it.groupValues[2] to f.name }.toList() }
        val duplicates = names.groupBy { it.first }.filter { it.value.size > 1 }.map { "${it.key} in ${it.value.map { v -> v.second }}" }
        assertTrue("Duplicate resource names: $duplicates", duplicates.isEmpty())
        assertTrue("Expected the strings files to hold resources", names.size > 100)
    }

    @Test
    fun formatSpecifiersArePositional() {
        val offenders = resourceFiles().flatMap { f ->
            entry.findAll(f.readText()).filter { it.groupValues[1] != "string-array" }.mapNotNull { m ->
                val withoutEscapedPercent = m.groupValues[3].replace("%%", "")
                val bad = Regex("%(?!\\d+\\$)[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]").find(withoutEscapedPercent)
                if (bad != null) "${f.name}: ${m.groupValues[2]}: ${bad.value}" else null
            }.toList()
        }
        assertTrue("Format specifiers must be positional (%1\$s):\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    @Test
    fun everyPluralsResourceHasOneAndOther() {
        val offenders = resourceFiles().flatMap { f ->
            entry.findAll(f.readText()).filter { it.groupValues[1] == "plurals" }.filter { m ->
                "quantity=\"one\"" !in m.groupValues[3] || "quantity=\"other\"" !in m.groupValues[3]
            }.map { "${f.name}: ${it.groupValues[2]}" }.toList()
        }
        assertEquals("Plurals without one and other: $offenders", 0, offenders.size)
    }

    @Test
    fun aStringsFileExistsForEachCategory() {
        val names = resourceFiles().map { it.name }
        listOf(
            "strings.xml", "strings_ui.xml", "strings_messages.xml", "strings_accessibility.xml",
            "strings_text.xml", "strings_vocabulary.xml", "strings_region_ca.xml"
        ).forEach { assertTrue("Missing $it in $names", it in names) }
    }
}
