package io.github.genesjc.climatefinder.data.remote

import android.util.Log
import kotlinx.coroutines.CancellationException

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
