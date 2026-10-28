package io.github.genesjc.climatefinder.ui.regionpicker

import io.github.genesjc.climatefinder.data.local.TerrainRepository
import io.github.genesjc.climatefinder.testutil.FakeTerrainSource
import io.github.genesjc.climatefinder.testutil.MainDispatcherRule
import io.github.genesjc.climatefinder.testutil.StopKoinRule
import io.github.genesjc.climatefinder.testutil.restartKoin
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class RegionPickerViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val stopKoin = StopKoinRule()

    @Test
    fun loadsTheStatewidePictureOnceAsSoonAsItIsCreated() {
        val terrain = FakeTerrainSource()
        restartKoin { single { TerrainRepository(terrain, Dispatchers.Unconfined) } }
        val vm = RegionPickerViewModel()
        assertEquals(1, terrain.stateLoads)
        assertNull(vm.stateTerrain.value)
    }
}
