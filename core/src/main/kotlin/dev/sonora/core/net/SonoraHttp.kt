package dev.sonora.core.net

import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.model.YouTubeAvailability
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

/** Single JSON configuration shared by every provider. */
object SonoraJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }
}

/**
 * Creates the HTTP client used for catalogue and YouTube requests.
 *
 * Tests inject a `MockEngine`, production uses OkHttp. `expectSuccess` is off so
 * that every status code is mapped to a typed [dev.sonora.core.model.SonoraError]
 * instead of throwing generic exceptions.
 */
object SonoraHttp {

    const val ITUNES_BASE_URL: String = "https://itunes.apple.com"
    const val YOUTUBE_BASE_URL: String = "https://www.googleapis.com/youtube/v3"

    const val REQUEST_TIMEOUT_MILLIS: Long = 12_000
    const val CONNECT_TIMEOUT_MILLIS: Long = 8_000

    const val USER_AGENT: String = "Sonora-Android/1.0 (Android music discovery preview player)"

    fun create(
        engine: HttpClientEngine? = null,
        requestTimeoutMillis: Long = REQUEST_TIMEOUT_MILLIS,
    ): HttpClient = if (engine != null) {
        HttpClient(engine) {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
                this.connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                this.socketTimeoutMillis = requestTimeoutMillis
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "application/json")
            }
        }
    } else {
        HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
                this.connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                this.socketTimeoutMillis = requestTimeoutMillis
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "application/json")
            }
        }
    }

    /** Convenience for callers that only have a raw key string. */
    fun youTubeAvailability(rawKey: String?): YouTubeAvailability = YouTubeApiKey.availability(rawKey)
}
