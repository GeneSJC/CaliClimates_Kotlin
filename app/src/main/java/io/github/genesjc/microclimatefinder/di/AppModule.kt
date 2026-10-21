package io.github.genesjc.microclimatefinder.di

import io.github.genesjc.microclimatefinder.data.local.AssetStationSource
import io.github.genesjc.microclimatefinder.data.local.AssetTerrainSource
import io.github.genesjc.microclimatefinder.data.local.DarkModeStore
import io.github.genesjc.microclimatefinder.data.local.ElevationHintPreferences
import io.github.genesjc.microclimatefinder.data.local.ElevationHintStore
import io.github.genesjc.microclimatefinder.data.local.MapZoomPreferences
import io.github.genesjc.microclimatefinder.data.local.MapZoomStore
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.RegionPreferences
import io.github.genesjc.microclimatefinder.data.local.SelectedRegionStore
import io.github.genesjc.microclimatefinder.data.local.SelectedStationStore
import io.github.genesjc.microclimatefinder.data.local.StationDataStore
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.StationSelectionPreferences
import io.github.genesjc.microclimatefinder.data.local.StationSource
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.data.local.ThemePreferences
import io.github.genesjc.microclimatefinder.data.remote.LiveObservationCache
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.data.remote.NwsObservationClient
import io.github.genesjc.microclimatefinder.data.remote.ObservationSource
import io.github.genesjc.microclimatefinder.BuildConfig
import io.github.genesjc.microclimatefinder.BuildInfo
import io.github.genesjc.microclimatefinder.viewmodel.AppConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * How the app's shared objects are built, in one place, bottom up: the platform edge (preference stores and data
 * sources), then repositories, then state holders. Every definition is a `single`. The edge pieces are registered under
 * their interface, which is the seam a test replaces with a fake; the classes above them are registered as themselves.
 * Clocks, dispatchers and the app scope are default constructor arguments, so they are not listed here.
 */
val appModule = module {
    // Build constants, read once into one immutable object
    single { AppConfig(isCommercial = BuildConfig.IS_COMMERCIAL, buildLabel = BuildInfo.LABEL) }

    // Platform edge: preference stores and data sources
    single<SelectedRegionStore> { RegionPreferences(androidContext()) }
    single<SelectedStationStore> { StationSelectionPreferences(androidContext()) }
    single<MapZoomStore> { MapZoomPreferences(androidContext()) }
    single<DarkModeStore> { ThemePreferences(androidContext()) }
    single<ElevationHintStore> { ElevationHintPreferences(androidContext()) }
    single<StationSource> { AssetStationSource(androidContext()) }
    single<TerrainSource> { AssetTerrainSource(androidContext()) }
    single<ObservationSource> { NwsObservationClient() }

    // Repositories
    single { StationRepository(get()) }
    single { TerrainRepository(get()) }
    single<RegionDatasets> { StationDataStore(androidContext(), get()) }
    single { LiveObservationCache(get()) }

    // State holders
    single {
        val cache = get<LiveObservationCache>()
        LiveReadings(peek = { cache.peek(it) }, fetch = { cache.get(it) })
    }
}
