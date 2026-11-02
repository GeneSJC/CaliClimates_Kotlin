package io.github.genesjc.climatefinder.data.local

import android.content.Context
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.StationNameFormatter

/** Reads a dataset's stations from the station packs (or a CSV override) in the app's assets. */
class AssetStationSource(context: Context, towns: StationNameFormatter) : StationSource {
    private val parser = StationCsvParser(context.applicationContext, towns)

    override fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation> = parser.loadStations(dataset)
}
