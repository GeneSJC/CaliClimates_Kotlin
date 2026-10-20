package io.github.genesjc.microclimatefinder.viewmodel

import io.github.genesjc.microclimatefinder.BuildInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppBaseViewModelTest {

    private class TestViewModel : AppBaseViewModel()

    @Test
    fun exposesTheBuildLabel() {
        assertEquals(BuildInfo.LABEL, TestViewModel().buildLabel)
    }

    @Test
    fun everyViewModelExtendsTheAppBase() {
        val offenders = File("src/main/java").walk()
            .filter { it.extension == "kt" && it.name != "AppBaseViewModel.kt" }
            .flatMap { file ->
                file.readLines().filter { Regex(""":\s*ViewModel\(\)""").containsMatchIn(it) }.map { file.name }
            }
            .toList()
        assertTrue("ViewModels extending ViewModel directly: $offenders", offenders.isEmpty())
    }
}
