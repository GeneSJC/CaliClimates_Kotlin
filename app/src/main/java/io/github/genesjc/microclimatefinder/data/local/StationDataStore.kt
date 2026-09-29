package io.github.genesjc.microclimatefinder.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.genesjc.microclimatefinder.domain.model.CaliforniaRegionalDefinitions
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** A station dataset for one CA region, either imported by the user or the built-in sample. */
data class StationDatasetInfo(
    val regionId: String,
    val fileName: String,
    val isExternal: Boolean,
    val title: String,
    val subtitle: String,
    val stationCount: Int,
    val minElevationMeters: Int,
    val maxElevationMeters: Int
)

data class ImportResult(
    val importedRegionNames: List<String>,
    val skipped: List<String>
)

/**
 * Finds station data per region, first match wins:
 *  1. an imported file (stations_<regionId>.csv in the app's private storage),
 *  2. a full file bundled at build time (stations_<regionId>.csv in assets, from the gitignored
 *     app/src/debug/assets folder),
 *  3. the small built-in sample (stations_<regionId>_sample.csv), Bay Area only.
 */
class StationDataStore(private val context: Context) {

    private val dataDir = File(context.filesDir, DATA_DIR_NAME)
    private val parser = StationCsvParser(context)

    fun fileNameFor(regionId: String) = "stations_$regionId.csv"

    private fun hasExternal(regionId: String): Boolean =
        File(dataDir, fileNameFor(regionId)).let { it.isFile && it.length() > 0 }

    private val assetNames: Set<String> by lazy {
        try { context.assets.list("")?.toSet() ?: emptySet() } catch (e: Exception) { emptySet() }
    }

    private fun hasFullAsset(regionId: String) = fileNameFor(regionId) in assetNames

    private fun sampleFileNameFor(regionId: String) = "stations_${regionId}_sample.csv"

    /** Number of regions with full data (imported or bundled at build time), not just the sample. */
    fun fullDataRegionCount(): Int =
        CaliforniaRegionalDefinitions.ALL_REGIONS.count { hasExternal(it.id) || hasFullAsset(it.id) }

    fun totalRegionCount(): Int = CaliforniaRegionalDefinitions.ALL_REGIONS.size

    fun datasetFor(regionId: String): StationDatasetInfo? {
        val region = CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == regionId }
            ?: return null
        if (hasExternal(regionId)) {
            build(region.id, region.name, fileNameFor(regionId), external = true, sample = false)?.let { return it }
        }
        if (hasFullAsset(regionId)) {
            build(region.id, region.name, fileNameFor(regionId), external = false, sample = false)?.let { return it }
        }
        if (regionId in SAMPLE_REGION_IDS) {
            return build(region.id, region.name, sampleFileNameFor(regionId), external = false, sample = true)
        }
        return null
    }

    /** The always-present Bay Area / Sacramento sample, or the imported file when there is one. */
    fun defaultDataset(): StationDatasetInfo =
        datasetFor(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)
            ?: StationDatasetInfo(
                regionId = CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id,
                fileName = sampleFileNameFor(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id),
                isExternal = false,
                title = CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.name,
                subtitle = "No stations found",
                stationCount = 0,
                minElevationMeters = 0,
                maxElevationMeters = 0
            )

    private fun build(
        regionId: String,
        regionName: String,
        fileName: String,
        external: Boolean,
        sample: Boolean
    ): StationDatasetInfo? {
        val base = StationDatasetInfo(
            regionId = regionId,
            fileName = fileName,
            isExternal = external,
            title = regionName,
            subtitle = "",
            stationCount = 0,
            minElevationMeters = 0,
            maxElevationMeters = 0
        )
        val stations = parser.loadStations(base)
        if (stations.isEmpty()) return null
        val kind = when {
            external -> "Imported dataset"
            sample -> "Built-in sample"
            else -> "Bundled dataset"
        }
        return base.copy(
            subtitle = "$kind • ${stations.size} stations",
            stationCount = stations.size,
            minElevationMeters = stations.minOf { it.elevationMeters },
            maxElevationMeters = stations.maxOf { it.elevationMeters }
        )
    }

    /** Copies chosen stations_<regionId>.csv files (or a zip of them) into private storage. */
    fun importFrom(uris: List<Uri>): ImportResult {
        val imported = linkedSetOf<String>()
        val skipped = mutableListOf<String>()
        dataDir.mkdirs()
        for (uri in uris) {
            val label = displayName(uri)
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream == null) {
                    skipped.add("$label: could not be opened")
                    continue
                }
                stream.use {
                    if (label.endsWith(".zip", ignoreCase = true)) {
                        importZip(it, imported, skipped)
                    } else {
                        importCsv(label, it.readBytes(), imported, skipped, reportUnknown = true)
                    }
                }
            } catch (e: Exception) {
                skipped.add("$label: ${e.message ?: "import failed"}")
            }
        }
        return ImportResult(imported.toList(), skipped)
    }

    private fun importZip(input: InputStream, imported: MutableSet<String>, skipped: MutableList<String>) {
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val base = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && base.startsWith("stations_", ignoreCase = true) &&
                    base.endsWith(".csv", ignoreCase = true)
                ) {
                    importCsv(base, zip.readBytes(), imported, skipped, reportUnknown = true)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun importCsv(
        fileName: String,
        bytes: ByteArray,
        imported: MutableSet<String>,
        skipped: MutableList<String>,
        reportUnknown: Boolean
    ) {
        val base = fileName.substringAfterLast('/')
        val regionId = FILE_PATTERN.matchEntire(base)?.groupValues?.get(1)?.lowercase()
        val region = CaliforniaRegionalDefinitions.ALL_REGIONS.firstOrNull { it.id == regionId }
        if (region == null) {
            if (reportUnknown) skipped.add("$base: name must be stations_<regionId>.csv")
            return
        }
        val count = parser.parse(bytes.inputStream().bufferedReader()).size
        if (count == 0) {
            skipped.add("$base: no valid station rows")
            return
        }
        File(dataDir, fileNameFor(region.id)).writeBytes(bytes)
        imported.add(region.name)
    }

    private fun displayName(uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.let { return it }
            }
        return uri.lastPathSegment ?: "file"
    }

    companion object {
        const val DATA_DIR_NAME = "station_data"

        // Regions whose small sample file ships in app/src/main/assets
        private val SAMPLE_REGION_IDS = setOf(CaliforniaRegionalDefinitions.BAY_AREA_SACRAMENTO.id)

        private val FILE_PATTERN = Regex("stations_([a-z_]+)\\.csv", RegexOption.IGNORE_CASE)
    }
}
