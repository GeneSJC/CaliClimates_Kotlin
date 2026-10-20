package io.github.genesjc.microclimatefinder.ui.root

import io.github.genesjc.microclimatefinder.data.local.DarkModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRootViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeDarkModeStore(var dark: Boolean = false) : DarkModeStore {
        override fun darkMode() = dark
        override fun saveDarkMode(dark: Boolean) {
            this.dark = dark
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(store: DarkModeStore, loads: MutableList<String> = mutableListOf()) =
        AppRootViewModel(store, loadBackdropTexture = { loads += "load"; null }, ioDispatcher = dispatcher)

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
        val loads = mutableListOf<String>()
        val vm = viewModel(FakeDarkModeStore(), loads)
        assertEquals(1, loads.size)
        assertNull(vm.backdropTexture.value)
    }
}
