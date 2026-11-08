package io.github.genesjc.climatefinder.domain

/**
 * Station names as the app shows them: the town a station is in, and the cleaned-up station name where there is no
 * town on record. (How the town and the station's own name are put together for the screens is
 * [io.github.genesjc.climatefinder.domain.model.MicroclimateStation.displayName].) The table of towns is the USA state's ([io.github.genesjc.climatefinder.config.StateUsaDefinition.stationTowns]);
 * the container builds this over the registered definition's table. (The local news code that once shared this file is
 * parked; see docs/news-restore.md.)
 */
class StationNameFormatter(private val towns: Map<String, String>) {

    /** The short name the maps' labels use: the station's town when the state has one on record, else its cleaned name. */
    fun cleanCityName(stationId: String, stationName: String): String =
        mappedCity(stationId) ?: placeFromStationName(stationName)

    /** The town the state's table puts the station in; null when it has none on record. */
    fun townOf(stationId: String): String? = mappedCity(stationId)

    private fun mappedCity(stationId: String): String? = towns[stationId]
}
