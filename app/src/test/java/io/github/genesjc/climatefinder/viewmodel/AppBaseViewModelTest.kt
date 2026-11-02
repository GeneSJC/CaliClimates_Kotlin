package io.github.genesjc.climatefinder.viewmodel

import io.github.genesjc.climatefinder.config.StateUsaCA
import io.github.genesjc.climatefinder.config.StateUsaDefinition
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class AppBaseViewModelTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    private class TestViewModel : AppBaseViewModel()

    @Test
    fun exposesTheInjectedBuildConfig() {
        restartKoin { single { AppConfig(isCommercial = true, buildLabel = "9.9") } }
        val vm = TestViewModel()
        assertEquals("9.9", vm.buildLabel)
        assertTrue(vm.isCommercial)
    }

    @Test
    fun exposesTheInjectedStateUsaDefinition() {
        restartKoin { single<StateUsaDefinition> { StateUsaCA } }
        assertEquals(StateUsaCA, TestViewModel().stateUsa)
    }

    @Test
    fun readsTheConfigOnlyWhenAskedFor() {
        // Nothing is registered: building the ViewModel must not need the config until a getter is used
        restartKoin { }
        assertFalse(TestViewModel().toString().isEmpty())
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

    @Test
    fun noViewModelTakesConstructorArguments() {
        val offenders = File("src/main/java").walk()
            .filter { it.extension == "kt" && it.name.endsWith("ViewModel.kt") && it.name != "AppBaseViewModel.kt" }
            .flatMap { file ->
                file.readLines()
                    .filter { Regex("""class \w+ViewModel\(""").containsMatchIn(it) }
                    .map { file.name }
            }
            .toList()
        assertTrue("ViewModels with constructor arguments: $offenders", offenders.isEmpty())
    }

    @Test
    fun onlyTheModuleReadsTheBuildConstants() {
        val offenders = File("src/main/java").walk()
            .filter { it.extension == "kt" && it.name != "AppModule.kt" && it.name != "BuildInfo.kt" }
            .flatMap { file ->
                file.readLines()
                    .filter { Regex("""\b(BuildInfo|BuildConfig)\.""").containsMatchIn(it) }
                    .map { file.name }
            }
            .toList()
        assertTrue("Files reading the build constants directly: $offenders", offenders.isEmpty())
    }
}
