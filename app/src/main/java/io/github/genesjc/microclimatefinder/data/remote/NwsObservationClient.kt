package io.github.genesjc.microclimatefinder.data.remote

import android.util.Log
import kotlinx.coroutines.CancellationException

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

/** Outcome of one fetch: a reading, a station with nothing usable right now, or a failed request. */
sealed interface ObservationResult {
    data class Reading(val observation: LiveObservation) : ObservationResult
    object NoReading : ObservationResult
    object Failed : ObservationResult
}

/** Where a station's latest observation comes from (api.weather.gov; a fake in tests). */
interface ObservationSource {
    suspend fun fetchLatest(stationId: String): ObservationResult
}

/**
 * Fetches one station's latest observation from api.weather.gov (no key needed) through [api]. The answer is read into
 * [ObservationDto] and turned into an [ObservationResult]; a failure of any kind is a result, not an exception
 * (cancellation is the one exception and is passed on).
 */
class NwsObservationClient(private val api: NwsApi) : ObservationSource {

    override suspend fun fetchLatest(stationId: String): ObservationResult = try {
        val response = api.latestObservation(stationId)
        when {
            response.code() == 404 -> ObservationResult.NoReading
            !response.isSuccessful -> ObservationResult.Failed
            else -> response.body()?.toResult() ?: ObservationResult.Failed
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "$stationId failed: ${e.javaClass.simpleName}: ${e.message}")
        ObservationResult.Failed
    }

    companion object {
        private const val TAG = "NwsClient"

        /** One api.weather.gov observation (GeoJSON text) as a result; [ObservationResult.NoReading] when it has no temperature. */
        internal fun parseObservation(body: String): ObservationResult =
            NwsJson.decodeFromString<ObservationDto>(body).toResult()
    }
}
