package dev.sonora.core.net.itunes

import dev.sonora.core.domain.CatalogSearch
import dev.sonora.core.domain.DEFAULT_SEARCH_LIMIT
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.Track
import dev.sonora.core.net.ApiHttpException
import dev.sonora.core.net.HttpErrorMapper
import dev.sonora.core.net.SonoraHttp
import dev.sonora.core.net.SonoraJson
import dev.sonora.core.net.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * iTunes Search API client. Public, key-free and rate limited to roughly
 * 20 requests per minute per IP address, which is why
 * [dev.sonora.core.domain.SearchRepository] adds a short lived cache.
 *
 * Only `previewUrl` values returned by Apple are ever played by the app.
 */
class ITunesCatalog(
    private val client: HttpClient,
    private val country: String = DEFAULT_COUNTRY,
    private val baseUrl: String = SonoraHttp.ITUNES_BASE_URL,
) : CatalogSearch {

    override suspend fun search(query: String, limit: Int): ApiResult<List<Track>> = apiCall {
        val response = client.get("$baseUrl/search") {
            parameter("term", query)
            parameter("media", "music")
            parameter("entity", "song")
            parameter("limit", limit.coerceIn(1, MAX_LIMIT))
            parameter("country", country)
        }
        if (!response.status.isSuccess()) {
            throw ApiHttpException(
                statusCode = response.status.value,
                error = HttpErrorMapper.fromStatus(response.status.value),
                body = runCatching { response.bodyAsText() }.getOrNull(),
            )
        }
        val payload = SonoraJson.instance.decodeFromString<ITunesSearchResponse>(response.bodyAsText())
        payload.results.mapNotNull { it.toTrackOrNull() }
    }

    companion object {
        const val MAX_LIMIT: Int = 50
        const val DEFAULT_COUNTRY: String = "US"

        /** Documented behaviour of the endpoint; kept here for tests and docs. */
        const val RATE_LIMIT_PER_MINUTE: Int = 20

        fun defaultLimit(): Int = DEFAULT_SEARCH_LIMIT
    }
}
