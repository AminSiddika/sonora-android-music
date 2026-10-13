package dev.sonora.app.player

import dev.sonora.core.domain.RepeatMode
import dev.sonora.core.model.Track

/** Why local preview playback stopped. */
enum class PlaybackFailureKind {
    NETWORK,
    UNSUPPORTED,
    UNKNOWN,
}

data class PlaybackFailure(
    val kind: PlaybackFailureKind,
    /** Technical detail from Media3, useful when reporting an issue. */
    val detail: String? = null,
)

/**
 * Everything the UI needs to render the mini player and the full player.
 *
 * Positions are milliseconds; [progress] is a 0..1 fraction that is safe to
 * feed straight into a slider.
 */
data class PlayerUiState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val queueSize: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val failure: PlaybackFailure? = null,
) {
    val hasTrack: Boolean get() = current != null

    val progress: Float
        get() {
            if (durationMillis <= 0L) return 0f
            return (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
        }

    val remainingMillis: Long
        get() = (durationMillis - positionMillis).coerceAtLeast(0L)

    /** True while the current preview is being prepared or played. */
    val isActive: Boolean get() = hasTrack && (isPlaying || isBuffering)
}
