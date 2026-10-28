package io.github.genesjc.climatefinder.data.remote

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

/** The part of an api.weather.gov observation (GeoJSON) the app reads. Values can be null and carry a unit code. */
@Serializable
data class ObservationDto(val properties: ObservationPropertiesDto)

@Serializable
data class ObservationPropertiesDto(
    val timestamp: String? = null,
    val temperature: QuantityDto? = null,
    val windSpeed: QuantityDto? = null,
    val barometricPressure: QuantityDto? = null,
    val relativeHumidity: QuantityDto? = null
)

@Serializable
data class QuantityDto(val value: Double? = null, val unitCode: String? = null)

/**
 * The observation as a result, converting by unit code instead of assuming one: Fahrenheit to Celsius, metres per
 * second to km/h, pascals to hectopascals. [ObservationResult.NoReading] when it has no temperature.
 */
fun ObservationDto.toResult(): ObservationResult {
    val tempC = properties.temperature.valueOrNull()?.let {
        if (properties.temperature.unit().endsWith("degF")) (it - 32.0) * 5.0 / 9.0 else it
    } ?: return ObservationResult.NoReading
    val wind = properties.windSpeed.valueOrNull()?.let {
        if (properties.windSpeed.unit().endsWith("m_s-1")) it * 3.6 else it
    }
    val pressure = properties.barometricPressure.valueOrNull()?.let {
        // Pascals only: a unit already in hectopascals ("wmoUnit:hPa") also ends in "Pa"
        if (properties.barometricPressure.unit().endsWith(":Pa")) it / 100.0 else it
    }
    return ObservationResult.Reading(
        LiveObservation(
            temperatureCelsius = tempC,
            humidityPercent = properties.relativeHumidity.valueOrNull(),
            windSpeedKmh = wind,
            pressureHpa = pressure,
            observedAtMillis = parseTime(properties.timestamp)
        )
    )
}

private fun QuantityDto?.valueOrNull(): Double? = this?.value

private fun QuantityDto?.unit(): String = this?.unitCode.orEmpty()

private fun parseTime(timestamp: String?): Long? {
    if (timestamp.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(timestamp)?.time
    } catch (e: Exception) {
        null
    }
}
