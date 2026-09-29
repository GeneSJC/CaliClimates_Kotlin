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

/**
 * Dev proof: fetches one station's latest observation from the National Weather Service and describes
 * it in one line. It answers three questions on a real device: does the feed return readings for our
 * station ids, in what units, and how fresh. The raw response goes to Logcat under "NwsProbe".
 * Not used by the real screens yet.
 */
object NwsObservationProbe {
    private const val TAG = "NwsProbe"
    private const val USER_AGENT =
        "California Climate Finder (https://github.com/GeneSJC/Android_MicroClimateFinder)"

    /** One line about the station's latest observation, or why there is none. */
    suspend fun describe(stationId: String): String = withContext(Dispatchers.IO) {
        try {
            fetch(stationId)
        } catch (e: Exception) {
            Log.w(TAG, "$stationId failed", e)
            "error (${e.javaClass.simpleName}: ${e.message})"
        }
    }

    private fun fetch(stationId: String): String {
        val cached = LiveObservationCache.peek(stationId)
        if (cached is ObservationResult.Reading) {
            val o = cached.observation
            val age = o.ageMinutes()?.let { "$it min ago" } ?: "no timestamp"
            return "%.1f°C / %.1f°F • %s • prefetched (cache)".format(Locale.US, o.temperatureCelsius, o.temperatureFahrenheit, age)
        }
        val url = URL("https://api.weather.gov/stations/${URLEncoder.encode(stationId, "UTF-8")}/observations/latest")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "application/geo+json")
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            Log.d(TAG, "$stationId HTTP $code: ${body.take(1500)}")
            if (code == 404) return "station not found in the NWS list"
            if (code !in 200..299) return "HTTP $code"

            val props = JSONObject(body).getJSONObject("properties")
            val age = ageText(props.optString("timestamp"))
            val tempC = celsius(props.optJSONObject("temperature"))
                ?: return "no temperature in the latest observation ($age)"
            val tempF = tempC * 9.0 / 5.0 + 32.0
            val humidity = props.optJSONObject("relativeHumidity")?.takeUnless { it.isNull("value") }
                ?.getDouble("value")
            val windKmh = kmh(props.optJSONObject("windSpeed"))
            val parts = mutableListOf("%.1f°C / %.1f°F".format(Locale.US, tempC, tempF), age)
            if (humidity != null) parts.add("humidity %.0f%%".format(Locale.US, humidity))
            if (windKmh != null) parts.add("wind %.1f km/h".format(Locale.US, windKmh))
            return parts.joinToString(" • ")
        } finally {
            conn.disconnect()
        }
    }

    // Values can be null and carry a unit code; convert by the unit code instead of assuming.
    private fun celsius(obj: org.json.JSONObject?): Double? {
        if (obj == null || obj.isNull("value")) return null
        val value = obj.getDouble("value")
        return if (obj.optString("unitCode").endsWith("degF")) (value - 32.0) * 5.0 / 9.0 else value
    }

    private fun kmh(obj: org.json.JSONObject?): Double? {
        if (obj == null || obj.isNull("value")) return null
        val value = obj.getDouble("value")
        return if (obj.optString("unitCode").endsWith("m_s-1")) value * 3.6 else value
    }

    private fun ageText(timestamp: String): String {
        if (timestamp.isBlank()) return "no timestamp"
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(timestamp)
            val minutes = ((System.currentTimeMillis() - parsed!!.time) / 60_000).coerceAtLeast(0)
            if (minutes < 90) "$minutes min ago" else "${minutes / 60} h ago"
        } catch (e: Exception) {
            timestamp
        }
    }
}
