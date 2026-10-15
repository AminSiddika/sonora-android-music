package dev.sonora.core.domain

import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SearchCapabilities
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem
import dev.sonora.core.model.YouTubeAvailability

/**
 * Single entry point the UI uses for search: validates the query, routes it to
 * the right provider, serves repeat searches from a short lived cache and turns
 * every failure into a typed [SonoraError].
 */
class SearchRepository(
    private val catalog: CatalogSearch,
    private val videos: VideoSearch?,
    private val cacheTtlMillis: Long = DEFAULT_CACHE_TTL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Evaluated on every call so a key entered at runtime is picked up immediately. */
    private val youTubeAvailability: () -> YouTubeAvailability,
) {

    private val trackCache = ExpiringCache<String, List<Track>>(
        ttlMillis = cacheTtlMillis,
        maxEntries = MAX_CACHE_ENTRIES,
        clock = clock,
    )

    private val videoCache = ExpiringCache<String, List<VideoItem>>(
        ttlMillis = cacheTtlMillis,
        maxEntries = MAX_CACHE_ENTRIES,
        clock = clock,
    )

    fun capabilities(): SearchCapabilities = SearchCapabilities(youTube = youTubeAvailability())

    suspend fun searchTracks(
        rawQuery: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        forceRefresh: Boolean = false,
    ): ApiResult<List<Track>> {
        val query = validated(rawQuery) ?: return ApiResult.Failure(SonoraError.EmptyQuery)
        val key = "itunes|$limit|$query"
        if (!forceRefresh) {
            trackCache.get(key)?.let { return ApiResult.Success(it) }
        }
        val result = catalog.search(query, limit)
        if (result is ApiResult.Success) trackCache.put(key, result.data)
        return result
    }

    suspend fun searchVideos(
        rawQuery: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        forceRefresh: Boolean = false,
    ): ApiResult<List<VideoItem>> {
        val query = validated(rawQuery) ?: return ApiResult.Failure(SonoraError.EmptyQuery)
        val provider = videos.takeIf { youTubeAvailability().isAvailable } ?: return ApiResult.Failure(
            when (youTubeAvailability()) {
                YouTubeAvailability.PlaceholderKey -> SonoraError.InvalidApiKey
                else -> SonoraError.MissingApiKey
            },
        )
        val key = "youtube|$limit|$query"
        if (!forceRefresh) {
            videoCache.get(key)?.let { return ApiResult.Success(it) }
        }
        val result = provider.search(query, limit)
        if (result is ApiResult.Success) videoCache.put(key, result.data)
        return result
    }

    fun clearCaches() {
        trackCache.clear()
        videoCache.clear()
    }

    private fun validated(rawQuery: String): String? = when (val validation = SearchQueryValidator.validate(rawQuery)) {
        is QueryValidation.Valid -> validation.query
        else -> null
    }

    companion object {
        /** Long enough to make navigation feel instant, short enough to stay fresh. */
        const val DEFAULT_CACHE_TTL_MILLIS: Long = 5 * 60 * 1000

        const val MAX_CACHE_ENTRIES: Int = 24
    }
}
