package dev.sonora.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.app.data.CatalogSession
import dev.sonora.app.ui.LoadStatus
import dev.sonora.core.domain.DiscoveryShelves
import dev.sonora.core.domain.DiscoveryShelf
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.domain.SearchRepository
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val shelf: DiscoveryShelf = DiscoveryShelves.featuredFor(0),
    val shelfTracks: List<Track> = emptyList(),
    val shelfStatus: LoadStatus = LoadStatus.IDLE,
    val shelfError: SonoraError? = null,
    val favorites: List<Track> = emptyList(),
    val recentlyPlayed: List<Track> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val isOffline: Boolean = false,
)

/**
 * Discovery screen state. iTunes has no browsing endpoint, so the screen is
 * built from local data (favourites, history) plus one curated search that
 * rotates daily, which also keeps the app inside the documented rate limit.
 */
class HomeViewModel(
    private val searchRepository: SearchRepository,
    private val favoritesRepository: FavoritesRepository,
    recentlyPlayedRepository: RecentlyPlayedRepository,
    private val catalogSession: CatalogSession,
    isOnline: StateFlow<Boolean>,
    epochDayProvider: () -> Long = { java.time.LocalDate.now().toEpochDay() },
) : ViewModel() {

    private val _state = MutableStateFlow(
        HomeUiState(
            shelf = DiscoveryShelves.featuredFor(epochDayProvider()),
            isOffline = !isOnline.value,
        ),
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                _state.update {
                    it.copy(
                        favorites = favorites,
                        favoriteIds = favorites.map { track -> track.id }.toSet(),
                    )
                }
            }
        }
        viewModelScope.launch {
            recentlyPlayedRepository.recentlyPlayed.collect { recent ->
                _state.update { it.copy(recentlyPlayed = recent) }
            }
        }
        viewModelScope.launch {
            isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        loadShelf()
    }

    /** Loads the featured shelf; repeated calls are cheap thanks to the cache. */
    fun loadShelf(forceRefresh: Boolean = false) {
        val shelf = _state.value.shelf
        viewModelScope.launch {
            _state.update { it.copy(shelfStatus = LoadStatus.LOADING, shelfError = null) }
            when (val result = searchRepository.searchTracks(shelf.query, limit = 10, forceRefresh = forceRefresh)) {
                is ApiResult.Success -> {
                    catalogSession.rememberTracks(result.data)
                    _state.update {
                        it.copy(
                            shelfTracks = result.data,
                            shelfStatus = if (result.data.isEmpty()) LoadStatus.EMPTY else LoadStatus.CONTENT,
                            shelfError = null,
                        )
                    }
                }

                is ApiResult.Failure -> _state.update {
                    it.copy(shelfStatus = LoadStatus.ERROR, shelfError = result.error, shelfTracks = emptyList())
                }
            }
        }
    }

    fun refresh() = loadShelf(forceRefresh = true)

    fun toggleFavorite(track: Track) {
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }
}
