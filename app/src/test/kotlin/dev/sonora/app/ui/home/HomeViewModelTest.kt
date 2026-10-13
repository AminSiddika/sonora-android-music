package dev.sonora.app.ui.home

import dev.sonora.app.data.CatalogSession
import dev.sonora.app.ui.LoadStatus
import dev.sonora.core.domain.CatalogSearch
import dev.sonora.core.domain.DiscoveryShelves
import dev.sonora.core.domain.InMemoryFavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.domain.SearchRepository
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import dev.sonora.core.model.YouTubeAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val track = Track(id = 7L, title = "Late Night", artist = "Sonora Test")

    private class FakeRecentlyPlayed(initial: List<Track> = emptyList()) : RecentlyPlayedRepository {
        private val state = MutableStateFlow(initial)
        override val recentlyPlayed: Flow<List<Track>> = state
        override suspend fun record(track: Track) {
            state.value = listOf(track) + state.value.filterNot { it.id == track.id }
        }

        override suspend fun clear() {
            state.value = emptyList()
        }
    }

    private class FakeCatalog(private val results: ApiResult<List<Track>>) : CatalogSearch {
        var calls = 0
        override suspend fun search(query: String, limit: Int): ApiResult<List<Track>> {
            calls++
            return results
        }
    }

    private fun viewModel(
        catalog: FakeCatalog = FakeCatalog(ApiResult.Success(listOf(track))),
        favorites: InMemoryFavoritesRepository = InMemoryFavoritesRepository(),
        recent: FakeRecentlyPlayed = FakeRecentlyPlayed(),
        online: Boolean = true,
    ) = HomeViewModel(
        searchRepository = SearchRepository(catalog, videos = null, youTubeAvailability = YouTubeAvailability.MissingKey),
        favoritesRepository = favorites,
        recentlyPlayedRepository = recent,
        catalogSession = CatalogSession(),
        isOnline = MutableStateFlow(online),
        epochDayProvider = { 3L },
    )

    @Test
    fun `the featured shelf rotates by day and loads tracks`() = runTest(dispatcher) {
        val catalog = FakeCatalog(ApiResult.Success(listOf(track)))
        val viewModel = viewModel(catalog)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(DiscoveryShelves.featuredFor(3L).id, state.shelf.id)
        assertEquals(LoadStatus.CONTENT, state.shelfStatus)
        assertEquals(1, state.shelfTracks.size)
        assertTrue(catalog.calls >= 1)
    }

    @Test
    fun `a shelf failure is reported without crashing the screen`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeCatalog(ApiResult.Failure(SonoraError.NoConnection)))
        advanceUntilIdle()

        assertEquals(LoadStatus.ERROR, viewModel.state.value.shelfStatus)
        assertEquals(SonoraError.NoConnection, viewModel.state.value.shelfError)
    }

    @Test
    fun `favourites and history power the offline shelves`() = runTest(dispatcher) {
        val favorites = InMemoryFavoritesRepository()
        val recent = FakeRecentlyPlayed()
        val viewModel = viewModel(favorites = favorites, recent = recent)
        favorites.add(track)
        recent.record(track)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf(track.id), state.favorites.map { it.id })
        assertEquals(listOf(track.id), state.recentlyPlayed.map { it.id })
        assertEquals(setOf(track.id), state.favoriteIds)
    }

    @Test
    fun `offline state comes from the network monitor`() = runTest(dispatcher) {
        val viewModel = viewModel(online = false)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isOffline)
    }

    @Test
    fun `toggling a favourite writes through the repository`() = runTest(dispatcher) {
        val favorites = InMemoryFavoritesRepository()
        val viewModel = viewModel(favorites = favorites)
        advanceUntilIdle()

        viewModel.toggleFavorite(track)
        advanceUntilIdle()

        assertEquals(listOf(track.id), favorites.favorites.first().map { it.id })
    }
}
