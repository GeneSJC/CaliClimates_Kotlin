package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo

/** Where a dataset's stations are read from (the app's assets; a fake in tests). */
interface StationSource {
    fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation>
}
