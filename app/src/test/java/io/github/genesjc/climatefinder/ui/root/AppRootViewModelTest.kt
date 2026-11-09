package io.github.genesjc.climatefinder.ui.root

import io.github.genesjc.climatefinder.data.local.AppearanceSettings
import io.github.genesjc.climatefinder.data.local.TerrainRepository
import io.github.genesjc.climatefinder.testutil.FakeAppSettings
import io.github.genesjc.climatefinder.testutil.FakeTerrainSource
import io.github.genesjc.climatefinder.testutil.MainDispatcherRule
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
import io.github.genesjc.climatefinder.ui.theme.SHOW_RELIEF_TEXTURE
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

    private fun viewModel(store: AppearanceSettings): AppRootViewModel {
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
    fun loadsTheBackdropTextureOnceAtStartWhenItIsSwitchedOn() {
        val vm = viewModel(FakeAppSettings())
        // The relief texture is behind a switch (SHOW_RELIEF_TEXTURE, off while the map fills the screen): no load while it is off
        assertEquals(if (SHOW_RELIEF_TEXTURE) 1 else 0, terrain.backdropLoads)
        assertNull(vm.backdropTexture.value)
    }
}
