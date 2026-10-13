package dev.sonora.core.domain

import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem
import dev.sonora.core.model.YouTubeAvailability
import dev.sonora.core.model.errorOrNull
import dev.sonora.core.model.getOrNull
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SearchRepositoryTest {

    private fun track(id: Long, title: String = "Track $id") =
        Track(id = id, title = title, artist = "Artist")

    private class RecordingCatalog(
        private val result: ApiResult<List<Track>> = ApiResult.Success(emptyList()),
    ) : CatalogSearch {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String, limit: Int): ApiResult<List<Track>> {
            queries += query
            return result
        }
    }

    private class RecordingVideoSearch(
        private val result: ApiResult<List<VideoItem>> = ApiResult.Success(emptyList()),
    ) : VideoSearch {
        val queries = mutableListOf<String>()

        override suspend fun search(query: String, limit: Int): ApiResult<List<VideoItem>> {
            queries += query
            return result
        }
    }

    @Test
    fun `blank queries never reach the network`() = runTest {
        val catalog = RecordingCatalog()
        val repository = SearchRepository(catalog, videos = null, youTubeAvailability = { YouTubeAvailability.MissingKey })

        val result = repository.searchTracks("   ")

        assertEquals(SonoraError.EmptyQuery, result.errorOrNull())
        assertTrue(catalog.queries.isEmpty())
    }

    @Test
    fun `queries are normalised before being sent`() = runTest {
        val catalog = RecordingCatalog()
        val repository = SearchRepository(catalog, videos = null, youTubeAvailability = { YouTubeAvailability.MissingKey })

        repository.searchTracks("  norah    jones ")

        assertEquals(listOf("norah jones"), catalog.queries)
    }

    @Test
    fun `repeat searches are served from the cache`() = runTest {
        val catalog = RecordingCatalog(ApiResult.Success(listOf(track(1))))
        val repository = SearchRepository(catalog, videos = null, youTubeAvailability = { YouTubeAvailability.MissingKey })

        val first = repository.searchTracks("jazz")
        val second = repository.searchTracks("jazz")

        assertEquals(1, catalog.queries.size)
        assertEquals(first.getOrNull(), second.getOrNull())
    }

    @Test
    fun `forceRefresh bypasses the cache`() = runTest {
        val catalog = RecordingCatalog(ApiResult.Success(listOf(track(1))))
        val repository = SearchRepository(catalog, videos = null, youTubeAvailability = { YouTubeAvailability.MissingKey })

        repository.searchTracks("jazz")
        repository.searchTracks("jazz", forceRefresh = true)

        assertEquals(2, catalog.queries.size)
    }

    @Test
    fun `errors are not cached`() = runTest {
        val catalog = RecordingCatalog(ApiResult.Failure(SonoraError.RateLimited))
        val repository = SearchRepository(catalog, videos = null, youTubeAvailability = { YouTubeAvailability.MissingKey })

        repository.searchTracks("jazz")
        repository.searchTracks("jazz")

        assertEquals(2, catalog.queries.size)
    }

    @Test
    fun `the cache expires after the ttl`() = runTest {
        var now = 0L
        val catalog = RecordingCatalog(ApiResult.Success(listOf(track(1))))
        val repository = SearchRepository(
            catalog = catalog,
            videos = null,
            youTubeAvailability = { YouTubeAvailability.MissingKey },
            cacheTtlMillis = 1_000,
            clock = { now },
        )

        repository.searchTracks("jazz")
        now += 2_000
        repository.searchTracks("jazz")

        assertEquals(2, catalog.queries.size)
    }

    @Test
    fun `youtube search reports a missing key instead of failing silently`() = runTest {
        val repository = SearchRepository(
            catalog = RecordingCatalog(),
            videos = null,
            youTubeAvailability = { YouTubeAvailability.MissingKey },
        )

        assertEquals(SonoraError.MissingApiKey, repository.searchVideos("tiny desk").errorOrNull())
    }

    @Test
    fun `a placeholder key is reported as an invalid key`() = runTest {
        val repository = SearchRepository(
            catalog = RecordingCatalog(),
            videos = null,
            youTubeAvailability = { YouTubeAvailability.PlaceholderKey },
        )

        assertEquals(SonoraError.InvalidApiKey, repository.searchVideos("tiny desk").errorOrNull())
    }

    @Test
    fun `youtube search works when a key is configured`() = runTest {
        val videos = RecordingVideoSearch(
            ApiResult.Success(listOf(VideoItem(videoId = "abc", title = "A video", channelTitle = "Channel"))),
        )
        val repository = SearchRepository(
            catalog = RecordingCatalog(),
            videos = videos,
            youTubeAvailability = { YouTubeAvailability.Available },
        )

        val result = repository.searchVideos("tiny desk")

        assertEquals(listOf("tiny desk"), videos.queries)
        assertEquals(1, result.getOrNull()?.size)
        assertTrue(repository.capabilities().isAvailable(dev.sonora.core.model.SearchProvider.YOUTUBE))
    }

    @Test
    fun `catalogue search always stays available`() {
        val repository = SearchRepository(
            catalog = RecordingCatalog(),
            videos = null,
            youTubeAvailability = { YouTubeAvailability.MissingKey },
        )

        val capabilities = repository.capabilities()

        assertTrue(capabilities.isAvailable(dev.sonora.core.model.SearchProvider.ITUNES))
        assertTrue(!capabilities.isAvailable(dev.sonora.core.model.SearchProvider.YOUTUBE))
        assertNull(capabilities.youTube.let { if (it is YouTubeAvailability.Available) it else null })
    }

    @Test
    fun `capabilities follow the availability provider at runtime`() = runTest {
        var availability: YouTubeAvailability = YouTubeAvailability.MissingKey
        val videos = RecordingVideoSearch(
            ApiResult.Success(listOf(VideoItem(videoId = "abc", title = "A video", channelTitle = "Channel"))),
        )
        val repository = SearchRepository(
            catalog = RecordingCatalog(),
            videos = videos,
            youTubeAvailability = { availability },
        )

        assertTrue(!repository.capabilities().isAvailable(dev.sonora.core.model.SearchProvider.YOUTUBE))
        assertEquals(SonoraError.MissingApiKey, repository.searchVideos("tiny desk").errorOrNull())
        assertTrue(videos.queries.isEmpty(), "an unavailable provider must not be called")

        // The user saves a key in Settings while the app is running.
        availability = YouTubeAvailability.Available

        assertTrue(repository.capabilities().isAvailable(dev.sonora.core.model.SearchProvider.YOUTUBE))
        assertEquals(1, repository.searchVideos("tiny desk").getOrNull()?.size)
        assertEquals(listOf("tiny desk"), videos.queries)
    }
}
