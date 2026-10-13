package dev.sonora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.app.data.ThemeMode
import dev.sonora.app.data.UserPreferences
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.model.YouTubeAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false,
    val favoritesCount: Int = 0,
    val historyCount: Int = 0,
    val youTubeAvailability: YouTubeAvailability = YouTubeAvailability.MissingKey,
    val versionName: String = "",
    val historyCleared: Boolean = false,
)

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val favoritesRepository: FavoritesRepository,
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
    youTubeAvailability: YouTubeAvailability,
    versionName: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(youTubeAvailability = youTubeAvailability, versionName = versionName),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.themeMode.collect { mode -> _state.update { it.copy(themeMode = mode) } }
        }
        viewModelScope.launch {
            preferences.reduceMotion.collect { enabled -> _state.update { it.copy(reduceMotion = enabled) } }
        }
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                _state.update { it.copy(favoritesCount = favorites.size) }
            }
        }
        viewModelScope.launch {
            recentlyPlayedRepository.recentlyPlayed.collect { recent ->
                _state.update { it.copy(historyCount = recent.size) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            recentlyPlayedRepository.clear()
            _state.update { it.copy(historyCleared = true) }
        }
    }

    fun acknowledgeHistoryCleared() {
        _state.update { it.copy(historyCleared = false) }
    }
}
