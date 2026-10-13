package dev.sonora.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.app.data.CatalogSession
import dev.sonora.app.data.UserPreferences
import dev.sonora.app.ui.LoadStatus
import dev.sonora.core.domain.YouTubeKeyResolution
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.SearchRepository
import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SearchCapabilities
import dev.sonora.core.model.SearchProvider
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem
import dev.sonora.core.model.YouTubeAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val provider: SearchProvider = SearchProvider.default,
    val tracks: List<Track> = emptyList(),
    val videos: List<VideoItem> = emptyList(),
    val status: LoadStatus = LoadStatus.IDLE,
    val error: SonoraError? = null,
    val capabilities: SearchCapabilities = SearchCapabilities(YouTubeAvailability.MissingKey),
    val isOffline: Boolean = false,
    val lastSubmittedQuery: String = "",
)

/**
 * Drives the search screen: query state, provider selection, loading/empty/
 * error handling and favourite bookkeeping.
 */
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val favoritesRepository: FavoritesRepository,
    private val preferences: UserPreferences,
    private val catalogSession: CatalogSession,
    isOnline: StateFlow<Boolean>,
    youTubeKey: StateFlow<YouTubeKeyResolution>,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SearchUiState(
            capabilities = SearchCapabilities(youTube = youTubeKey.value.availability),
            isOffline = !isOnline.value,
        ),
    )
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            isOnline.collect { online -> _state.update { it.copy(isOffline = !online) } }
        }
        viewModelScope.launch {
            preferences.lastProvider.collect { provider -> _state.update { it.copy(provider = provider) } }
        }
        // A key pasted into Settings becomes usable without restarting the app.
        viewModelScope.launch {
            youTubeKey.collect { resolution ->
                _state.update { it.copy(capabilities = SearchCapabilities(youTube = resolution.availability)) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun onProviderSelected(provider: SearchProvider) {
        if (_state.value.provider == provider) return
        _state.update {
            it.copy(
                provider = provider,
                tracks = emptyList(),
                videos = emptyList(),
                error = null,
                status = LoadStatus.IDLE,
            )
        }
        viewModelScope.launch { preferences.setLastProvider(provider) }
        if (_state.value.query.isNotBlank()) submit()
    }

    fun clearQuery() {
        _state.update {
            it.copy(query = "", tracks = emptyList(), videos = emptyList(), error = null, status = LoadStatus.IDLE)
        }
    }

    fun useQuickSearch(term: String) {
        _state.update { it.copy(query = term) }
        submit()
    }

    fun submit(forceRefresh: Boolean = false) {
        val snapshot = _state.value
        val query = snapshot.query
        if (query.isBlank()) {
            _state.update { it.copy(status = LoadStatus.ERROR, error = SonoraError.EmptyQuery) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(status = LoadStatus.LOADING, error = null) }
            when (snapshot.provider) {
                SearchProvider.ITUNES -> {
                    val result = searchRepository.searchTracks(query, forceRefresh = forceRefresh)
                    applyTracks(result, query)
                }

                SearchProvider.YOUTUBE -> {
                    val result = searchRepository.searchVideos(query, forceRefresh = forceRefresh)
                    applyVideos(result, query)
                }
            }
        }
    }

    fun retry() = submit(forceRefresh = true)

    fun switchToCatalogue() {
        _state.update { it.copy(provider = SearchProvider.ITUNES, error = null, status = LoadStatus.IDLE) }
        viewModelScope.launch { preferences.setLastProvider(SearchProvider.ITUNES) }
        if (_state.value.query.isNotBlank()) submit()
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }

    private fun applyTracks(result: ApiResult<List<Track>>, query: String) {
        when (result) {
            is ApiResult.Success -> {
                catalogSession.rememberTracks(result.data)
                _state.update {
                    it.copy(
                        tracks = result.data,
                        videos = emptyList(),
                        status = if (result.data.isEmpty()) LoadStatus.EMPTY else LoadStatus.CONTENT,
                        error = null,
                        lastSubmittedQuery = query.trim(),
                    )
                }
            }

            is ApiResult.Failure -> _state.update {
                it.copy(
                    status = LoadStatus.ERROR,
                    error = result.error,
                    lastSubmittedQuery = query.trim(),
                    tracks = emptyList(),
                )
            }
        }
    }

    private fun applyVideos(result: ApiResult<List<VideoItem>>, query: String) {
        when (result) {
            is ApiResult.Success -> {
                catalogSession.rememberVideos(result.data)
                _state.update {
                    it.copy(
                        videos = result.data,
                        tracks = emptyList(),
                        status = if (result.data.isEmpty()) LoadStatus.EMPTY else LoadStatus.CONTENT,
                        error = null,
                        lastSubmittedQuery = query.trim(),
                    )
                }
            }

            is ApiResult.Failure -> _state.update {
                it.copy(
                    status = LoadStatus.ERROR,
                    error = result.error,
                    lastSubmittedQuery = query.trim(),
                    videos = emptyList(),
                )
            }
        }
    }
}
