package dev.sonora.app.testing

import dev.sonora.app.data.YouTubeKeyStore
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory listening history used by unit tests. */
class FakeRecentlyPlayedRepository(initial: List<Track> = emptyList()) : RecentlyPlayedRepository {

    private val state = MutableStateFlow(initial)

    override val recentlyPlayed: Flow<List<Track>> = state

    override suspend fun record(track: Track) {
        state.value = listOf(track) + state.value.filterNot { it.id == track.id }
    }

    override suspend fun clear() {
        state.value = emptyList()
    }
}

/** In-memory YouTube key store, mirroring the DataStore backed implementation. */
class FakeYouTubeKeyStore(initial: String? = null) : YouTubeKeyStore {

    private val state = MutableStateFlow(initial)

    override val key: StateFlow<String?> = state

    override suspend fun save(raw: String) {
        state.value = raw.trim().ifEmpty { null }
    }

    override suspend fun clear() {
        state.value = null
    }
}
