package dev.sonora.app.player

import dev.sonora.core.domain.RepeatMode
import dev.sonora.core.model.Track
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerUiStateTest {

    private val track = Track(id = 1, title = "Track", artist = "Artist", previewUrl = "https://example.com/p.m4a")

    @Test
    fun `progress is a clamped fraction`() {
        assertEquals(0.5f, PlayerUiState(current = track, positionMillis = 15_000, durationMillis = 30_000).progress)
        assertEquals(0f, PlayerUiState(current = track, positionMillis = 0, durationMillis = 0).progress)
        assertEquals(1f, PlayerUiState(current = track, positionMillis = 45_000, durationMillis = 30_000).progress)
    }

    @Test
    fun `remaining time never goes negative`() {
        val state = PlayerUiState(current = track, positionMillis = 31_000, durationMillis = 30_000)
        assertEquals(0L, state.remainingMillis)
    }

    @Test
    fun `a loaded track is active while buffering or playing`() {
        assertTrue(PlayerUiState(current = track, isBuffering = true).isActive)
        assertTrue(PlayerUiState(current = track, isPlaying = true).isActive)
        assertFalse(PlayerUiState(current = track).isActive)
        assertFalse(PlayerUiState().isActive)
    }

    @Test
    fun `hasTrack reflects the queue state`() {
        assertTrue(PlayerUiState(current = track).hasTrack)
        assertFalse(PlayerUiState().hasTrack)
    }

    @Test
    fun `defaults are safe for an empty player`() {
        val state = PlayerUiState()
        assertEquals(RepeatMode.OFF, state.repeatMode)
        assertFalse(state.shuffleEnabled)
        assertEquals(0, state.queueSize)
        assertEquals(null, state.failure)
    }
}
