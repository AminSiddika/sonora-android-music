package dev.sonora.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sonora.app.data.CatalogSession
import dev.sonora.app.data.RecentlyPlayedRepository
import dev.sonora.app.player.PlayerUiState
import dev.sonora.app.player.SonoraPlayer
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Bridges the app scoped [SonoraPlayer] to Compose. A single instance is shared
 * by the mini player, the full player and every list that starts playback.
 */
class PlayerViewModel(
    private val player: SonoraPlayer,
    private val favoritesRepository: FavoritesRepository,
    private val recentlyPlayedRepository: RecentlyPlayedRepository,
    private val catalogSession: CatalogSession,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = player.state

    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteIds: StateFlow<Set<Long>> = _favoriteIds.asStateFlow()

    init {
        viewModelScope.launch {
            favoritesRepository.favorites.collect { favorites ->
                _favoriteIds.value = favorites.map { it.id }.toSet()
            }
        }
        viewModelScope.launch {
            player.state
                .map { it.current }
                .distinctUntilChanged()
                .collect { track ->
                    if (track != null) recentlyPlayedRepository.record(track)
                }
        }
    }

    /**
     * Plays [track] as part of [queue]. The queue is also remembered so that
     * opening the detail screen straight from the player keeps working.
     */
    fun play(track: Track, queue: List<Track> = listOf(track)) {
        val effectiveQueue = if (queue.any { it.id == track.id }) queue else listOf(track)
        catalogSession.rememberTracks(effectiveQueue)
        player.playOrResume(track, effectiveQueue)
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun next() = player.next()

    fun previous() = player.previous()

    fun seekToProgress(progress: Float) = player.seekToProgress(progress)

    fun toggleShuffle() = player.toggleShuffle()

    fun cycleRepeatMode() = player.cycleRepeatMode()

    fun stop() = player.stop()

    fun removeFromQueue(trackId: Long) = player.removeFromQueue(trackId)

    fun toggleFavorite(track: Track) {
        viewModelScope.launch { favoritesRepository.toggle(track) }
    }

    fun toggleFavoriteOfCurrent() {
        val current = state.value.current ?: return
        toggleFavorite(current)
    }
}
