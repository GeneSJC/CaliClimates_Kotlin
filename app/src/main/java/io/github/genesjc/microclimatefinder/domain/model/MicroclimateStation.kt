package io.github.genesjc.microclimatefinder.domain.model

enum class TerrainType(val label: String) {
    COASTAL("Coastal Zone"),
    RIDGE("Mountain Ridge"),
    VALLEY_BASIN("Valley Basin"),
    URBAN_CANYON("Urban Corridor")
}

data class WeatherReading(
    val temperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Int,
    val solarIrradianceWm2: Double,
    val pressureHpa: Double
) {
    val temperatureFahrenheit: Double
        get() = (temperatureCelsius * 9.0 / 5.0) + 32.0
}

data class MicroclimateStation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevationMeters: Int,
    val terrain: TerrainType,
    val currentReading: WeatherReading,
    val microclimateDescription: String
)
