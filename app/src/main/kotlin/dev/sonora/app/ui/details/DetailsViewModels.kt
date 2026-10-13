package dev.sonora.app.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.app.data.CatalogSession
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TrackDetailsUiState(
    val track: Track? = null,
    val isFavorite: Boolean = false,
    val resolved: Boolean = false,
)

/**
 * Resolves a track by id. Results are remembered in the in-memory catalogue
 * session; favourites and history act as a fallback so a details screen keeps
 * working after process death or when opened from a notification-like entry.
 */
class TrackDetailsViewModel(
    private val trackId: Long,
    private val catalogSession: CatalogSession,
    private val favoritesRepository: FavoritesRepository,
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TrackDetailsUiState(track = catalogSession.track(trackId)))
    val state: StateFlow<TrackDetailsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                val favorite = favorites.firstOrNull { it.id == trackId }
                _state.value = _state.value.copy(
                    track = _state.value.track ?: favorite,
                    isFavorite = favorite != null,
                )
            }
        }
        viewModelScope.launch {
            val current = _state.value.track
            if (current == null) {
                val fallback = catalogSession.track(trackId)
                    ?: recentlyPlayedRepository.recentlyPlayed.first().firstOrNull { it.id == trackId }
                _state.value = _state.value.copy(track = fallback, resolved = true)
            } else {
                _state.value = _state.value.copy(resolved = true)
            }
        }
    }

    fun toggleFavorite() {
        val track = _state.value.track ?: return
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }
}

data class VideoDetailsUiState(
    val video: dev.sonora.core.model.VideoItem? = null,
)

class VideoDetailsViewModel(
    videoId: String,
    catalogSession: CatalogSession,
) : ViewModel() {

    private val _state = MutableStateFlow(VideoDetailsUiState(video = catalogSession.video(videoId)))
    val state: StateFlow<VideoDetailsUiState> = _state.asStateFlow()
}
