package io.github.genesjc.climatefinder.domain.model

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
