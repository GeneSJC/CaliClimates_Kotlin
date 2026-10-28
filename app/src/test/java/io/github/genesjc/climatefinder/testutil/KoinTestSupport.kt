package io.github.genesjc.climatefinder.testutil

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

/**
 * Starts Koin with only the fakes a test registers (no Android context), stopping any container left running first,
 * since `startKoin` throws when called twice. Register everything the ViewModel under test reads while it is built,
 * then construct the ViewModel.
 */
fun restartKoin(registrations: ModuleDeclaration) {
    stopKoin()
    startKoin { modules(module(moduleDeclaration = registrations)) }
}

/** Stops Koin after each test, so the next one starts clean. */
class StopKoinRule : TestWatcher() {
    override fun finished(description: Description) = stopKoin()
}
