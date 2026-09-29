package io.github.genesjc.microclimatefinder.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

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

/** Fetches one station's latest observation from api.weather.gov (no key needed). */
object NwsObservationClient {
    private const val TAG = "NwsClient"
    private const val USER_AGENT =
        "California Climate Finder (https://github.com/GeneSJC/Android_MicroClimateFinder)"

    suspend fun fetchLatest(stationId: String): ObservationResult = withContext(Dispatchers.IO) {
        try {
            fetch(stationId)
        } catch (e: Exception) {
            Log.w(TAG, "$stationId failed: ${e.javaClass.simpleName}: ${e.message}")
            ObservationResult.Failed
        }
    }

    private fun fetch(stationId: String): ObservationResult {
        val url = URL("https://api.weather.gov/stations/${URLEncoder.encode(stationId, "UTF-8")}/observations/latest")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/geo+json")
        try {
            val code = conn.responseCode
            if (code == 404) return ObservationResult.NoReading
            if (code !in 200..299) return ObservationResult.Failed
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val props = JSONObject(body).getJSONObject("properties")
            // Values can be null and carry a unit code; convert by unit code instead of assuming.
            val tempC = number(props.optJSONObject("temperature"))?.let {
                if (unit(props, "temperature").endsWith("degF")) (it - 32.0) * 5.0 / 9.0 else it
            } ?: return ObservationResult.NoReading
            val wind = number(props.optJSONObject("windSpeed"))?.let {
                if (unit(props, "windSpeed").endsWith("m_s-1")) it * 3.6 else it
            }
            val pressure = number(props.optJSONObject("barometricPressure"))?.let {
                if (unit(props, "barometricPressure").endsWith("Pa")) it / 100.0 else it
            }
            return ObservationResult.Reading(
                LiveObservation(
                    temperatureCelsius = tempC,
                    humidityPercent = number(props.optJSONObject("relativeHumidity")),
                    windSpeedKmh = wind,
                    pressureHpa = pressure,
                    observedAtMillis = parseTime(props.optString("timestamp"))
                )
            )
        } finally {
            conn.disconnect()
        }
    }

    private fun number(obj: JSONObject?): Double? =
        if (obj == null || obj.isNull("value")) null else obj.getDouble("value")

    private fun unit(props: JSONObject, key: String): String =
        props.optJSONObject(key)?.optString("unitCode").orEmpty()

    private fun parseTime(timestamp: String): Long? {
        if (timestamp.isBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(timestamp)?.time
        } catch (e: Exception) {
            null
        }
    }
}
