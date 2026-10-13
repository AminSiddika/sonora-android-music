package dev.sonora.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesUiState())
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                _state.update { it.copy(favorites = favorites, isLoading = false) }
            }
        }
    }

    fun remove(track: Track) {
        viewModelScope.launch { favoritesRepository.remove(track.id) }
    }

    /** Tracks with a playable preview, in list order. */
    fun playable(): List<Track> = _state.value.favorites.filter { it.hasPreview }
}
