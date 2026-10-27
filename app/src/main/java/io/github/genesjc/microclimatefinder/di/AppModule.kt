package io.github.genesjc.microclimatefinder.di

import io.github.genesjc.microclimatefinder.data.local.AppSettings
import io.github.genesjc.microclimatefinder.data.local.AppSettingsPreferences
import io.github.genesjc.microclimatefinder.data.local.AssetStationSource
import io.github.genesjc.microclimatefinder.data.local.AssetTerrainSource
import io.github.genesjc.microclimatefinder.data.local.RegionDatasets
import io.github.genesjc.microclimatefinder.data.local.StationDataStore
import io.github.genesjc.microclimatefinder.data.local.StationRepository
import io.github.genesjc.microclimatefinder.data.local.StationSource
import io.github.genesjc.microclimatefinder.data.local.StationTowns
import io.github.genesjc.microclimatefinder.data.local.TerrainRepository
import io.github.genesjc.microclimatefinder.data.local.TerrainSource
import io.github.genesjc.microclimatefinder.data.remote.LiveReadings
import io.github.genesjc.microclimatefinder.domain.model.StateUsaCA
import io.github.genesjc.microclimatefinder.domain.model.StateUsaDefinition
import io.github.genesjc.microclimatefinder.data.remote.createNwsApi
import io.github.genesjc.microclimatefinder.data.remote.createNwsHttpClient
import io.github.genesjc.microclimatefinder.data.remote.NwsObservationClient
import io.github.genesjc.microclimatefinder.data.remote.ObservationSource
import io.github.genesjc.microclimatefinder.BuildConfig
import io.github.genesjc.microclimatefinder.BuildInfo
import io.github.genesjc.microclimatefinder.viewmodel.AppConfig
import io.github.genesjc.microclimatefinder.viewmodel.ContextStringResolver
import io.github.genesjc.microclimatefinder.viewmodel.StringResolver
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

    // The USA state the app is built for: its regions, geography and artwork (California only today)
    single<StateUsaDefinition> { StateUsaCA }

    // Platform edge: preference stores and data sources
    single<AppSettings> { AppSettingsPreferences(androidContext(), get()) }
    single<StringResolver> { ContextStringResolver(androidContext()) }
    single { StationTowns(get<StateUsaDefinition>().stationTowns) }
    single<StationSource> { AssetStationSource(androidContext(), get()) }
    single<TerrainSource> { AssetTerrainSource(androidContext(), get()) }
    single { createNwsHttpClient() }
    single { createNwsApi(get()) }
    single<ObservationSource> { NwsObservationClient(get()) }

    // Repositories
    single { StationRepository(get()) }
    single { TerrainRepository(get()) }
    single<RegionDatasets> { StationDataStore.fromAssets(androidContext(), get(), get()) }

    // State holders
    single { LiveReadings(get()) }
}
