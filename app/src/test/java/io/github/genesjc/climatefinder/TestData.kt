package io.github.genesjc.climatefinder

import io.github.genesjc.climatefinder.domain.model.MicroclimateStation
import io.github.genesjc.climatefinder.domain.model.TerrainType
import io.github.genesjc.climatefinder.domain.model.WeatherReading

/** A station with sensible defaults, so each test names only what it cares about. */
fun testStation(
    id: String = "KTST",
    name: String = "Test Station",
    elevationMeters: Int = 10,
    terrain: TerrainType = TerrainType.VALLEY_BASIN,
    reportsTemp: Boolean? = null,
    isFeatured: Boolean = false,
    latitude: Double = 38.5,
    longitude: Double = -121.5
) = MicroclimateStation(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
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
    microclimateNote = "Test",
    reportsTemp = reportsTemp,
    isFeatured = isFeatured
)

/** Live readings over a fake source (every fetch answers Failed and is recorded) and a clock that stands still. */
fun testLiveReadings(
    scope: kotlinx.coroutines.CoroutineScope,
    source: io.github.genesjc.climatefinder.data.remote.ObservationSource =
        io.github.genesjc.climatefinder.testutil.FakeObservationSource(),
    now: () -> Long = { 0L }
) = io.github.genesjc.climatefinder.data.remote.LiveReadings(source, scope, now)
