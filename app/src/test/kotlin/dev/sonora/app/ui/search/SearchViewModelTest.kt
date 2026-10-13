package dev.sonora.app.ui.search

import dev.sonora.app.data.CatalogSession
import dev.sonora.app.testing.FakeUserPreferences
import dev.sonora.app.ui.LoadStatus
import dev.sonora.core.domain.CatalogSearch
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.InMemoryFavoritesRepository
import dev.sonora.core.domain.SearchRepository
import dev.sonora.core.domain.VideoSearch
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.domain.YouTubeKeyResolution
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SearchProvider
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem
import dev.sonora.core.domain.YouTubeKeySource
import dev.sonora.core.model.YouTubeAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val track = Track(
        id = 1L,
        title = "Don't Know Why",
        artist = "Norah Jones",
        album = "Come Away With Me",
        previewUrl = "https://example.com/preview.m4a",
        artworkUrl = "https://example.com/100x100bb.jpg",
        durationMillis = 186_000,
    )

    private class FakeCatalog(
        private val results: ApiResult<List<Track>>,
    ) : CatalogSearch {
        var calls = 0
        var lastQuery: String? = null

        override suspend fun search(query: String, limit: Int): ApiResult<List<Track>> {
            calls++
            lastQuery = query
            return results
        }
    }

    private class FakeVideoSearch(
        private val result: ApiResult<List<VideoItem>> = ApiResult.Success(emptyList()),
    ) : VideoSearch {
        var calls = 0
            private set

        override suspend fun search(query: String, limit: Int): ApiResult<List<VideoItem>> {
            calls++
            return result
        }
    }

    /**
     * A single flow drives both the repository routing and the UI capabilities,
     * exactly like the application graph does.
     */
    private fun resolution(availability: YouTubeAvailability): YouTubeKeyResolution = when (availability) {
        YouTubeAvailability.Available -> YouTubeKeyResolution(
            key = "AIzaSyRuntimeKey1234567890abcdefghij",
            availability = availability,
            source = YouTubeKeySource.RUNTIME,
        )

        else -> YouTubeKeyResolution(key = null, availability = availability, source = YouTubeKeySource.NONE)
    }

    private fun viewModel(
        catalog: FakeCatalog = FakeCatalog(ApiResult.Success(listOf(track))),
        videos: VideoSearch? = null,
        availability: YouTubeAvailability = YouTubeAvailability.MissingKey,
        favorites: FavoritesRepository = InMemoryFavoritesRepository(),
        preferences: FakeUserPreferences = FakeUserPreferences(),
        online: Boolean = true,
        keyFlow: MutableStateFlow<YouTubeKeyResolution> = MutableStateFlow(resolution(availability)),
    ): SearchViewModel = SearchViewModel(
        searchRepository = SearchRepository(catalog, videos) { keyFlow.value.availability },
        favoritesRepository = favorites,
        preferences = preferences,
        catalogSession = CatalogSession(),
        isOnline = MutableStateFlow(online),
        youTubeKey = keyFlow,
    )

    @Test
    fun `successful search exposes results`() = runTest(dispatcher) {
        val catalog = FakeCatalog(ApiResult.Success(listOf(track)))
        val viewModel = viewModel(catalog)
        val rawQuery = "  norah   jones "

        viewModel.onQueryChange(rawQuery)
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LoadStatus.CONTENT, state.status)
        assertEquals(1, state.tracks.size)
        assertEquals("Don't Know Why", state.tracks.first().title)
        assertEquals(rawQuery, state.query, "the raw query is preserved for the text field")
        assertEquals("norah jones", catalog.lastQuery, "the provider receives a normalised query")
    }

    @Test
    fun `empty results surface the empty state`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCatalog(ApiResult.Success(emptyList())))

        viewModel.onQueryChange("zzzz")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoadStatus.EMPTY, viewModel.state.value.status)
        assertEquals("zzzz", viewModel.state.value.lastSubmittedQuery)
    }

    @Test
    fun `provider failures keep their typed error`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCatalog(ApiResult.Failure(SonoraError.RateLimited)))

        viewModel.onQueryChange("jazz")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoadStatus.ERROR, viewModel.state.value.status)
        assertEquals(SonoraError.RateLimited, viewModel.state.value.error)
    }

    @Test
    fun `blank queries are rejected before hitting the network`() = runTest(dispatcher) {
        val catalog = FakeCatalog(ApiResult.Success(listOf(track)))
        val viewModel = viewModel(catalog)

        viewModel.onQueryChange("   ")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoadStatus.ERROR, viewModel.state.value.status)
        assertEquals(SonoraError.EmptyQuery, viewModel.state.value.error)
        assertEquals(0, catalog.calls)
    }

    @Test
    fun `youtube search reports a missing key without calling a provider`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onProviderSelected(SearchProvider.YOUTUBE)
        viewModel.onQueryChange("tiny desk")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(SearchProvider.YOUTUBE, viewModel.state.value.provider)
        assertEquals(LoadStatus.ERROR, viewModel.state.value.status)
        assertEquals(SonoraError.MissingApiKey, viewModel.state.value.error)
        assertTrue(!viewModel.state.value.capabilities.isAvailable(SearchProvider.YOUTUBE))
    }

    @Test
    fun `youtube results are exposed when a key is configured`() = runTest(dispatcher) {
        val videos = object : VideoSearch {
            override suspend fun search(query: String, limit: Int): ApiResult<List<VideoItem>> =
                ApiResult.Success(listOf(VideoItem(videoId = "abc", title = "Tiny Desk", channelTitle = "NPR Music")))
        }
        val viewModel = viewModel(videos = videos, availability = YouTubeAvailability.Available)

        viewModel.onProviderSelected(SearchProvider.YOUTUBE)
        viewModel.onQueryChange("tiny desk")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(LoadStatus.CONTENT, state.status)
        assertEquals("abc", state.videos.first().videoId)
        assertTrue(state.tracks.isEmpty())
    }

    @Test
    fun `switching back to the catalogue clears the error`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onProviderSelected(SearchProvider.YOUTUBE)
        viewModel.onQueryChange("tiny desk")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals(LoadStatus.ERROR, viewModel.state.value.status)

        viewModel.switchToCatalogue()
        advanceUntilIdle()

        assertEquals(SearchProvider.ITUNES, viewModel.state.value.provider)
        assertEquals(LoadStatus.CONTENT, viewModel.state.value.status)
    }

    @Test
    fun `the last used provider is restored from preferences`() = runTest(dispatcher) {
        val viewModel = viewModel(preferences = FakeUserPreferences(initialProvider = SearchProvider.YOUTUBE))
        advanceUntilIdle()

        assertEquals(SearchProvider.YOUTUBE, viewModel.state.value.provider)
    }

    @Test
    fun `provider selection is persisted`() = runTest(dispatcher) {
        val preferences = FakeUserPreferences()
        val viewModel = viewModel(preferences = preferences)

        viewModel.onProviderSelected(SearchProvider.YOUTUBE)
        advanceUntilIdle()

        assertEquals(SearchProvider.YOUTUBE, preferences.lastProvider.first())
    }

    @Test
    fun `offline state is reflected in the ui state`() = runTest(dispatcher) {
        val viewModel = viewModel(online = false)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isOffline)
    }

    @Test
    fun `toggling a favourite updates the local store`() = runTest(dispatcher) {
        val favorites = InMemoryFavoritesRepository()
        val viewModel = viewModel(favorites = favorites)

        viewModel.toggleFavorite(track)
        advanceUntilIdle()

        assertEquals(listOf(track.id), favorites.favorites.first().map { it.id })
    }

    @Test
    fun `saving a key at runtime enables youtube search immediately`() = runTest(dispatcher) {
        val videos = FakeVideoSearch(
            ApiResult.Success(listOf(VideoItem(videoId = "abc", title = "Tiny Desk", channelTitle = "NPR Music"))),
        )
        val keyFlow = MutableStateFlow(resolution(YouTubeAvailability.MissingKey))
        val viewModel = viewModel(videos = videos, keyFlow = keyFlow)
        advanceUntilIdle()

        assertTrue(!viewModel.state.value.capabilities.isAvailable(SearchProvider.YOUTUBE))

        // The user pastes a key in Settings while the app is running.
        keyFlow.value = resolution(YouTubeAvailability.Available)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.capabilities.isAvailable(SearchProvider.YOUTUBE))
        viewModel.onProviderSelected(SearchProvider.YOUTUBE)
        viewModel.onQueryChange("tiny desk")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(LoadStatus.CONTENT, viewModel.state.value.status)
        assertEquals("abc", viewModel.state.value.videos.first().videoId)
        assertEquals(1, videos.calls)
    }
}
