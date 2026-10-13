package dev.sonora.core.domain

import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Local favourites store. The Android implementation is backed by DataStore;
 * [InMemoryFavoritesRepository] keeps unit tests and previews simple.
 */
interface FavoritesRepository {

    /** Favourites, newest first. */
    val favorites: Flow<List<Track>>

    /** Adds or removes [track]. Returns true when the track is now a favourite. */
    suspend fun toggle(track: Track): Boolean

    suspend fun add(track: Track)

    suspend fun remove(trackId: Long)
}

/** Favourites kept in memory: used by tests, and as a safe fallback. */
class InMemoryFavoritesRepository(initial: List<Track> = emptyList()) : FavoritesRepository {

    private val state = kotlinx.coroutines.flow.MutableStateFlow(initial)

    override val favorites: Flow<List<Track>> = state

    override suspend fun toggle(track: Track): Boolean {
        val current = state.value
        val existing = current.any { it.id == track.id }
        state.value = if (existing) {
            current.filterNot { it.id == track.id }
        } else {
            listOf(track) + current.filterNot { it.id == track.id }
        }
        return !existing
    }

    override suspend fun add(track: Track) {
        state.value = listOf(track) + state.value.filterNot { it.id == track.id }
    }

    override suspend fun remove(trackId: Long) {
        state.value = state.value.filterNot { it.id == trackId }
    }
}
