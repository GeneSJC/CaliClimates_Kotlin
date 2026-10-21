package io.github.genesjc.microclimatefinder.ui.root

import io.github.genesjc.microclimatefinder.data.local.DarkModeStore
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.testutil.FakeDarkModeStore
import io.github.genesjc.microclimatefinder.testutil.FakeTerrainSource
import io.github.genesjc.microclimatefinder.testutil.MainDispatcherRule
import io.github.genesjc.microclimatefinder.testutil.StopKoinRule
import io.github.genesjc.microclimatefinder.testutil.restartKoin
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppRootViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    private val terrain = FakeTerrainSource()

    private fun viewModel(store: DarkModeStore): AppRootViewModel {
        restartKoin {
            single { store }
            single { TerrainRepository(terrain, Dispatchers.Unconfined) }
        }
        return AppRootViewModel()
    }

    @Test
    fun startsWithTheSavedLook() {
        assertFalse(viewModel(FakeDarkModeStore(dark = false)).darkMode.value)
        assertTrue(viewModel(FakeDarkModeStore(dark = true)).darkMode.value)
    }

    @Test
    fun switchingTheLookSavesIt() {
        val store = FakeDarkModeStore()
        val vm = viewModel(store)
        vm.setDarkMode(true)
        assertTrue(vm.darkMode.value)
        assertTrue(store.dark)
    }

    @Test
    fun loadsTheBackdropTextureOnceAtStart() {
        val vm = viewModel(FakeDarkModeStore())
        assertEquals(1, terrain.backdropLoads)
        assertNull(vm.backdropTexture.value)
    }
}
