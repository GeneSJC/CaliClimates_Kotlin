package io.github.genesjc.climatefinder.data.local

import android.content.Context
import android.util.Log
import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import java.io.BufferedReader
import java.io.StringReader
import io.github.genesjc.climatefinder.domain.StationNameFormatter
import io.github.genesjc.climatefinder.domain.model.StationDatasetInfo

class StationCsvParser(private val context: Context, private val towns: StationNameFormatter) {

    fun loadStationsFromCsv(fileName: String): List<MicroclimateStation> =
        try {
            context.assets.open(fileName).use { parse(it.bufferedReader()) }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

    fun loadStations(dataset: StationDatasetInfo): List<MicroclimateStation> =
        dataset.packFileName?.let { loadStationsFromPack(it) } ?: loadStationsFromCsv(dataset.fileName)

    /** Stations from a station pack in assets; empty (and logged) if the pack is missing or damaged. */
    fun loadStationsFromPack(packFileName: String): List<MicroclimateStation> =
        try {
            val packText = context.assets.open(packFileName).use { it.bufferedReader().readText() }
            val csv = StationPack.decode(packText)
            if (csv == null) {
                Log.w("StationPack", "$packFileName is damaged or not a station pack")
                emptyList()
            } else {
                parse(BufferedReader(StringReader(csv)))
            }
        } catch (e: Exception) {
            Log.w("StationPack", "Could not read $packFileName", e)
            emptyList()
        }

    fun parse(reader: BufferedReader): List<MicroclimateStation> = StationCsvFormat(towns).parse(reader)
}

