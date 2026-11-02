package io.github.genesjc.climatefinder.domain.model

/** A real reading from the National Weather Service. Only temperature is guaranteed. */
data class LiveObservation(
    val temperatureCelsius: Double,
    val humidityPercent: Double?,
    val windSpeedKmh: Double?,
    val pressureHpa: Double?,
    val observedAtMillis: Long?
) {
    val temperatureFahrenheit: Double
        get() = temperatureCelsius * 9.0 / 5.0 + 32.0

    /** Minutes since the station observed this, or null when the timestamp is unknown. */
    fun ageMinutes(now: Long = System.currentTimeMillis()): Long? =
        observedAtMillis?.let { ((now - it) / 60_000).coerceAtLeast(0) }
}
