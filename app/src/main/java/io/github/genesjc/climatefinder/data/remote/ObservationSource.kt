package io.github.genesjc.climatefinder.data.remote

/** Where a station's latest observation comes from (api.weather.gov; a fake in tests). */
interface ObservationSource {
    suspend fun fetchLatest(stationId: String): ObservationResult
}
