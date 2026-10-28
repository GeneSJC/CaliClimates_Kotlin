package io.github.genesjc.climatefinder.testutil

import io.github.genesjc.climatefinder.di.appModule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class KoinTestSupportTest {

    @get:Rule
    val stopKoin = StopKoinRule()

    @Test
    fun registersFakesThatCanBeLookedUp() {
        restartKoin { single { "fake" } }
        assertEquals("fake", GlobalContext.get().get<String>())
    }

    @Test
    fun restartingReplacesWhatWasRegisteredBefore() {
        restartKoin { single { "first" } }
        restartKoin { single { "second" } }
        assertEquals("second", GlobalContext.get().get<String>())
    }

    @Test
    fun theAppModuleLoadsWithoutAnAndroidContextBecauseDefinitionsAreLazy() {
        stopKoinIfRunning()
        startKoin { modules(appModule, module { single { 1 } }) }
        assertEquals(1, GlobalContext.get().get<Int>())
    }

    private fun stopKoinIfRunning() = org.koin.core.context.stopKoin()
}
