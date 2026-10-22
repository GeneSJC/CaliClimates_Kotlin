package io.github.genesjc.microclimatefinder.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/** The one api.weather.gov call the app makes. The response is returned whole so the client can tell a 404 from a failure. */
interface NwsApi {
    @Headers("Accept: application/geo+json")
    @GET("stations/{stationId}/observations/latest")
    suspend fun latestObservation(@Path("stationId") stationId: String): Response<ObservationDto>
}

/** Reads the answers: fields the app does not use are ignored. */
internal val NwsJson = Json { ignoreUnknownKeys = true }

const val NWS_BASE_URL = "https://api.weather.gov/"

private const val USER_AGENT = "Cali Climates (https://github.com/GeneSJC/Android_MicroClimateFinder)"
private const val TIMEOUT_SECONDS = 10L

/** The HTTP client for the NWS: 10-second timeouts and an identifying User-Agent, which the NWS asks for. */
fun createNwsHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
    }
    .build()

/** The [NwsApi] over [client]; [baseUrl] must end with a slash (tests point it at a local server). */
fun createNwsApi(client: OkHttpClient, baseUrl: String = NWS_BASE_URL): NwsApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(NwsJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(NwsApi::class.java)
