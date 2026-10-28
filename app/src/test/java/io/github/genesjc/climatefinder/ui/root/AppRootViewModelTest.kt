package io.github.genesjc.climatefinder.ui.root

import io.github.genesjc.climatefinder.data.local.AppSettings
import io.github.genesjc.climatefinder.data.local.TerrainRepository
import io.github.genesjc.climatefinder.testutil.FakeAppSettings
import io.github.genesjc.climatefinder.testutil.FakeTerrainSource
import io.github.genesjc.climatefinder.testutil.MainDispatcherRule
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
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

    private fun viewModel(store: AppSettings): AppRootViewModel {
        restartKoin {
            single { store }
            single { TerrainRepository(terrain, Dispatchers.Unconfined) }
        }
        return AppRootViewModel()
    }

    @Test
    fun startsWithTheSavedLook() {
        assertFalse(viewModel(FakeAppSettings(dark = false)).darkMode.value)
        assertTrue(viewModel(FakeAppSettings(dark = true)).darkMode.value)
    }

    @Test
    fun switchingTheLookSavesIt() {
        val store = FakeAppSettings()
        val vm = viewModel(store)
        vm.setDarkMode(true)
        assertTrue(vm.darkMode.value)
        assertTrue(store.dark)
    }

    @Test
    fun loadsTheBackdropTextureOnceAtStart() {
        val vm = viewModel(FakeAppSettings())
        assertEquals(1, terrain.backdropLoads)
        assertNull(vm.backdropTexture.value)
    }
}
