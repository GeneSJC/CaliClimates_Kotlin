package io.github.genesjc.microclimatefinder.ui.regionpicker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionPickerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun loadsTheStatewidePictureOnceAsSoonAsItIsCreated() {
        var loads = 0
        val vm = RegionPickerViewModel(loadStateTerrain = { loads++; null }, ioDispatcher = dispatcher)
        assertEquals(1, loads)
        assertNull(vm.stateTerrain.value)
    }
}
