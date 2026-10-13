package dev.sonora.core.net.youtube

import dev.sonora.core.domain.VideoSearch
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.VideoItem
import dev.sonora.core.net.SonoraHttp
import io.ktor.client.HttpClient

/**
 * YouTube search that can pick up a key *after* the app was built.
 *
 * Sonora supports two credential sources, in priority order: a key the user
 * saved in the app, and the optional key compiled into the build from
 * `local.properties`, an environment variable or a CI secret. Either way the
 * catalogue keeps working when no key exists at all.
 *
 * The delegate [YouTubeCatalog] is rebuilt only when the effective key changes,
 * so changing the key does not leak an old client.
 */
class RuntimeYouTubeSearch(
    private val client: HttpClient,
    private val keyProvider: () -> String?,
    private val baseUrl: String = SonoraHttp.YOUTUBE_BASE_URL,
) : VideoSearch {

    private var activeKey: String? = null
    private var delegate: YouTubeCatalog? = null
    private val lock = Any()

    /** True when a usable key is currently available. */
    val isConfigured: Boolean
        get() = YouTubeApiKey.sanitize(keyProvider()) != null

    override suspend fun search(query: String, limit: Int): ApiResult<List<VideoItem>> {
        val catalog = catalogForCurrentKey()
            ?: return ApiResult.Failure(SonoraError.MissingApiKey)
        return catalog.search(query, limit)
    }

    /** Synchronised so a key change can never race with an in-flight request. */
    private fun catalogForCurrentKey(): YouTubeCatalog? = synchronized(lock) {
        val key = YouTubeApiKey.sanitize(keyProvider()) ?: return@synchronized null
        val existing = delegate
        if (existing == null || key != activeKey) {
            activeKey = key
            return@synchronized YouTubeCatalog(client, key, baseUrl).also { delegate = it }
        }
        existing
    }
}
