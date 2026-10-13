package dev.sonora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.app.data.ThemeMode
import dev.sonora.app.data.UserPreferences
import dev.sonora.app.data.YouTubeKeyStore
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.domain.YouTubeKeyResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Why a pasted YouTube key was rejected. */
enum class YouTubeKeyError {
    EMPTY,
    NOT_A_KEY,
}

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false,
    val favoritesCount: Int = 0,
    val historyCount: Int = 0,
    val youTubeKey: YouTubeKeyResolution = YouTubeApiKey.resolve(null, null),
    val keyInput: String = "",
    val keyError: YouTubeKeyError? = null,
    val keySaved: Boolean = false,
    val versionName: String = "",
    val historyCleared: Boolean = false,
)

class SettingsViewModel(
    private val preferences: UserPreferences,
    private val favoritesRepository: FavoritesRepository,
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
    private val youTubeKeyStore: YouTubeKeyStore,
    youTubeKey: StateFlow<YouTubeKeyResolution>,
    versionName: String,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SettingsUiState(youTubeKey = youTubeKey.value, versionName = versionName),
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
        viewModelScope.launch {
            youTubeKey.collect { resolution -> _state.update { it.copy(youTubeKey = resolution) } }
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

    fun onKeyInputChange(value: String) {
        _state.update { it.copy(keyInput = value, keyError = null, keySaved = false) }
    }

    /**
     * Validates the pasted key before storing it. Placeholders and obviously
     * truncated values are rejected here so the app never spends a request on a
     * credential Google will refuse.
     */
    fun saveKey() {
        val raw = _state.value.keyInput.trim()
        when {
            raw.isEmpty() -> _state.update { it.copy(keyError = YouTubeKeyError.EMPTY) }

            YouTubeApiKey.sanitize(raw) == null -> _state.update { it.copy(keyError = YouTubeKeyError.NOT_A_KEY) }

            else -> viewModelScope.launch {
                youTubeKeyStore.save(raw)
                _state.update { it.copy(keyInput = "", keyError = null, keySaved = true) }
            }
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            youTubeKeyStore.clear()
            _state.update { it.copy(keyInput = "", keyError = null, keySaved = false) }
        }
    }
}
