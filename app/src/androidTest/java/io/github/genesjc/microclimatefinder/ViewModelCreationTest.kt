package io.github.genesjc.microclimatefinder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.genesjc.microclimatefinder.data.local.DarkModeStore
import io.github.genesjc.microclimatefinder.data.local.ElevationHintStore
import io.github.genesjc.microclimatefinder.data.local.MapZoomStore
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.SelectedStationStore
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.StationSource
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.ObservationSource
import io.github.genesjc.microclimatefinder.ui.home.HomeViewModel
import io.github.genesjc.microclimatefinder.ui.map.ElevationViewModel
import io.github.genesjc.microclimatefinder.ui.map.StationMapViewModel
import io.github.genesjc.microclimatefinder.ui.regionpicker.RegionPickerViewModel
import io.github.genesjc.microclimatefinder.ui.root.AppRootViewModel
import io.github.genesjc.microclimatefinder.ui.stationlist.StationListViewModel
import io.github.genesjc.microclimatefinder.viewmodel.AppConfig
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import kotlin.reflect.KClass

/**
 * Proves the real container can build every ViewModel with the ordinary factory, and that every shared object the
 * module registers can be created. A missing registration fails here instead of when a screen first opens. Runs on a
 * device or emulator, where the real Application has started Koin.
 */
@RunWith(AndroidJUnit4::class)
class ViewModelCreationTest {

    private fun <T : ViewModel> create(type: Class<T>): T =
        ViewModelProvider(ViewModelStore(), ViewModelProvider.NewInstanceFactory())[type]

    @Test
    fun everyViewModelCanBeCreated() {
        assertNotNull(create(AppRootViewModel::class.java))
        assertNotNull(create(HomeViewModel::class.java))
        assertNotNull(create(StationListViewModel::class.java))
        assertNotNull(create(StationMapViewModel::class.java))
        assertNotNull(create(ElevationViewModel::class.java))
        assertNotNull(create(RegionPickerViewModel::class.java))
    }

    @Test
    fun everyRegisteredObjectCanBeBuilt() {
        val koin = GlobalContext.get()
        val types: List<KClass<*>> = listOf(
            AppConfig::class, SelectedRegionStore::class, SelectedStationStore::class, MapZoomStore::class,
            DarkModeStore::class, ElevationHintStore::class, StationSource::class, TerrainSource::class,
            ObservationSource::class, StationRepository::class, TerrainRepository::class, RegionDatasets::class,
            LiveReadings::class
        )
        types.forEach { type -> assertNotNull("Not registered: ${type.simpleName}", koin.get<Any>(type, null, null)) }
    }
}
