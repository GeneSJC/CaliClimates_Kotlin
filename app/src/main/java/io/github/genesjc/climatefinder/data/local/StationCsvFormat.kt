package io.github.genesjc.climatefinder.data.local

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.TerrainType
import io.github.genesjc.climatefinder.domain.model.WeatherReading
import java.io.BufferedReader
import io.github.genesjc.climatefinder.domain.StationNameFormatter

/** The station CSV format itself: no Android classes, so it can be tested on the JVM. */
internal class StationCsvFormat(private val towns: StationNameFormatter) {
    fun parse(reader: BufferedReader): List<MicroclimateStation> {
        val stations = mutableListOf<MicroclimateStation>()
        val header = reader.readLine() ?: return emptyList()
        val headerIndices = parseHeaderIndices(header.removePrefix("\uFEFF"))

        var line = reader.readLine()
        var syntheticIndex = 0
        while (line != null) {
            if (line.isNotBlank()) {
                parseLine(line, headerIndices, syntheticIndex++)?.let { stations.add(it) }
            }
            line = reader.readLine()
        }
        return stations
    }

    private fun parseHeaderIndices(headerLine: String): Map<String, Int> {
        val tokens = parseCsvLine(headerLine).map { it.lowercase().trim() }
        val map = mutableMapOf<String, Int>()
        tokens.forEachIndexed { index, token ->
            map[token] = index
        }
        return map
    }

    private fun parseLine(line: String, indices: Map<String, Int>, index: Int): MicroclimateStation? {
        val tokens = parseCsvLine(line)
        if (tokens.isEmpty()) return null

        // Support both schema naming styles: "id" / "station_id", "name" / "station_name", "elevation_meters" / "elevation_m"
        val id = getField(tokens, indices, "id", "station_id") ?: "STATION_$index"
        val stationName = getField(tokens, indices, "display_name", "name", "station_name") ?: "Station $index"
        // The station's own name stays as the file has it; the town is what the state's table says about the station.
        // Putting them together for the screens ("Jackson (Mount Zion)") is MicroclimateStation.displayName
        val town = towns.townOf(id)
        val lat = getField(tokens, indices, "latitude", "lat")?.toDoubleOrNull() ?: 38.5816
        val lon = getField(tokens, indices, "longitude", "lon")?.toDoubleOrNull() ?: -121.4944
        val elev = getField(tokens, indices, "elevation_meters", "elevation_m", "elevation")?.toIntOrNull() ?: 15
        val terrainStr = getField(tokens, indices, "terrain_type", "terrain") ?: ""

        val terrain = deriveTerrainType(terrainStr, elev)

        // Realistic adiabatic lapse rate: ~6.5°C per 1000m elevation
        val baseTemp = 28.0 - (elev * 0.0065)
        val baseHumidity = (34 + (elev / 45)).coerceIn(20, 85)
        val baseWind = (8.0 + (elev / 120.0)).coerceAtLeast(3.0)

        val reading = WeatherReading(
            temperatureCelsius = baseTemp,
            humidityPercent = baseHumidity,
            windSpeedKmh = baseWind,
            windDirectionDegrees = 220,
            solarIrradianceWm2 = 850.0,
            pressureHpa = 1013.25 - (elev / 8.5)
        )

        return MicroclimateStation(
            id = id,
            name = stationName,
            latitude = lat,
            longitude = lon,
            elevationMeters = elev,
            terrain = terrain,
            currentReading = reading,
            microclimateNote = getField(tokens, indices, "note"),
            reportsTemp = parseFlag(getField(tokens, indices, "reports_temp")),
            reportsHumidity = parseFlag(getField(tokens, indices, "reports_humidity")),
            reportsWind = parseFlag(getField(tokens, indices, "reports_wind")),
            isFeatured = getField(tokens, indices, "tier")?.equals("featured", ignoreCase = true) == true,
            town = town
        )
    }

    // TRUE / FALSE in the data files; blank or anything else means unknown
    private fun parseFlag(value: String?): Boolean? = when (value?.trim()?.uppercase()) {
        "TRUE" -> true
        "FALSE" -> false
        else -> null
    }

    private fun deriveTerrainType(terrainStr: String, elevationMeters: Int): TerrainType {
        val clean = terrainStr.trim().lowercase()
        return when {
            clean.contains("ridge") || clean.contains("foothill") || clean.contains("mountain") || elevationMeters > 500 -> TerrainType.RIDGE
            clean.contains("coast") || clean.contains("delta") -> TerrainType.COASTAL
            clean.contains("urban") || clean.contains("city") -> TerrainType.URBAN_CANYON
            else -> TerrainType.VALLEY_BASIN
        }
    }

    private fun getField(tokens: List<String>, indices: Map<String, Int>, vararg keys: String): String? {
        for (key in keys) {
            val idx = indices[key]
            if (idx != null && idx < tokens.size) {
                val value = tokens[idx].trim().trim('\"')
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString().trim())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
