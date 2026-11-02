package io.github.genesjc.climatefinder.data.local

/** A station dataset for one region: its bundled station pack, or a CSV override. */
data class StationDatasetInfo(
    val regionId: String,
    val fileName: String,
    val kind: DatasetKind,
    val stationCount: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int,
    /** Set when the stations come from a station pack (a.txt to f.txt) instead of a CSV file. */
    val packFileName: String? = null
)
