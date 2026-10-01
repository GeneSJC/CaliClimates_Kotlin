package io.github.genesjc.microclimatefinder

import io.github.genesjc.microclimatefinder.domain.model.MicroclimateStation
import io.github.genesjc.microclimatefinder.domain.model.TerrainType
import io.github.genesjc.microclimatefinder.domain.model.WeatherReading

/** A station with sensible defaults, so each test names only what it cares about. */
fun testStation(
    id: String = "KTST",
    name: String = "Test Station",
    elevationMeters: Int = 10,
    terrain: TerrainType = TerrainType.VALLEY_BASIN,
    reportsTemp: Boolean? = null,
    isFeatured: Boolean = false
) = MicroclimateStation(
    id = id,
    name = name,
    latitude = 38.5,
    longitude = -121.5,
    elevationMeters = elevationMeters,
    terrain = terrain,
    currentReading = WeatherReading(
        temperatureCelsius = 20.0,
        humidityPercent = 50,
        windSpeedKmh = 10.0,
        windDirectionDegrees = 220,
        solarIrradianceWm2 = 800.0,
        pressureHpa = 1013.0
    ),
    microclimateDescription = "Test",
    reportsTemp = reportsTemp,
    isFeatured = isFeatured
)
