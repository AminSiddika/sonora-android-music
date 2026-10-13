package dev.sonora.core.net.youtube

import dev.sonora.core.domain.VideoSearch
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.VideoItem
import dev.sonora.core.net.ApiHttpException
import dev.sonora.core.net.SonoraHttp
import dev.sonora.core.net.SonoraJson
import dev.sonora.core.net.apiCall
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

/**
 * YouTube Data API v3 search client.
 *
 * This class only ever performs a `search.list` request. Sonora deliberately
 * never requests stream URLs, never downloads media and never plays YouTube
 * content in the background: results are handed to the official YouTube
 * application or website.
 */
class YouTubeCatalog(
    private val client: HttpClient,
    private val apiKey: String,
    private val baseUrl: String = SonoraHttp.YOUTUBE_BASE_URL,
) : VideoSearch {

    override suspend fun search(query: String, limit: Int): ApiResult<List<VideoItem>> = apiCall {
        val response = client.get("$baseUrl/search") {
            parameter("part", "snippet")
            parameter("type", "video")
            parameter("maxResults", limit.coerceIn(1, MAX_LIMIT))
            parameter("q", query)
            parameter("safeSearch", "moderate")
            // Only items that the official embedded player may show are returned.
            parameter("videoEmbeddable", "true")
            parameter("fields", "items(id(videoId),snippet(title,description,channelId,channelTitle,publishedAt,thumbnails))")
            parameter("key", apiKey)
        }
        if (!response.status.isSuccess()) {
            val body = runCatching { response.bodyAsText() }.getOrNull()
            throw ApiHttpException(
                statusCode = response.status.value,
                error = YouTubeErrorMapper.map(response.status.value, body),
                body = body,
            )
        }
        val payload = SonoraJson.instance.decodeFromString<YouTubeSearchResponse>(response.bodyAsText())
        payload.items.mapNotNull { it.toVideoOrNull() }
    }

    companion object {
        const val MAX_LIMIT: Int = 50
    }
}
