package dev.sonora.core.domain

import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem

/** Default page size for both providers. */
const val DEFAULT_SEARCH_LIMIT: Int = 25

/** Searches the iTunes Search API catalogue (songs with 30 second previews). */
interface CatalogSearch {
    suspend fun search(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): ApiResult<List<Track>>
}

/** Searches the optional YouTube Data API v3 provider. */
interface VideoSearch {
    suspend fun search(query: String, limit: Int = DEFAULT_SEARCH_LIMIT): ApiResult<List<VideoItem>>
}
