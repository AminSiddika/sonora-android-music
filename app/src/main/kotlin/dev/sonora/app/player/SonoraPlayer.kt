package dev.sonora.app.player

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import dev.sonora.app.MainActivity
import dev.sonora.core.domain.PlaybackQueue
import dev.sonora.core.domain.RepeatMode
import dev.sonora.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Thin, testable wrapper around ExoPlayer for **preview** playback.
 *
 * Responsibilities:
 *  * only ever plays the 30 second `previewUrl` supplied by the iTunes API,
 *  * owns audio focus (`handleAudioFocus = true`) and pauses when headphones
 *    are unplugged,
 *  * keeps the queue rules (shuffle/repeat/next/previous) in
 *    [PlaybackQueue] so they are unit tested on the JVM,
 *  * publishes a single [PlayerUiState] stream for the mini player and the
 *    full player.
 *
 * Deliberate limitation: previews are short, so Sonora does not run a
 * foreground media service or show playback notifications. Playback stops when
 * the process dies.
 */
class SonoraPlayer(
    context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true,
        )
        setHandleAudioBecomingNoisy(true)
    }

    private val mediaSession: MediaSession = MediaSession.Builder(appContext, exoPlayer)
        .setSessionActivity(
            PendingIntent.getActivity(
                appContext,
                0,
                Intent(appContext, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var queue: PlaybackQueue = PlaybackQueue.empty()
    private var ticker: Job? = null
    private var pendingAutoAdvance: Boolean = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update { current ->
                current.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    isPlaying = exoPlayer.isPlaying,
                )
            }
            when (playbackState) {
                Player.STATE_READY -> _state.update { it.copy(failure = null) }
                Player.STATE_ENDED -> onPreviewCompleted()
                else -> Unit
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { current -> current.copy(isPlaying = false, isBuffering = false, failure = error.toFailure()) }
        }
    }

    init {
        exoPlayer.addListener(listener)
        startTicker()
    }

    /** Starts [track] inside the context of [tracks], replacing the queue. */
    fun play(track: Track, tracks: List<Track> = listOf(track)) {
        if (!track.hasPreview) {
            _state.update { it.copy(failure = PlaybackFailure(PlaybackFailureKind.UNSUPPORTED, "no preview")) }
            return
        }
        val startIndex = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        queue = queue.replace(tracks = tracks.ifEmpty { listOf(track) }, startLibraryIndex = startIndex, random = Random.Default)
        loadCurrent(track)
    }

    /** Continues the existing queue when possible, otherwise starts a new one. */
    fun playOrResume(track: Track, tracks: List<Track> = listOf(track)) {
        val current = _state.value.current
        if (current?.id == track.id && exoPlayer.mediaItemCount > 0) {
            if (!exoPlayer.isPlaying) togglePlayPause()
            return
        }
        play(track, tracks)
    }

    fun togglePlayPause() {
        if (exoPlayer.mediaItemCount == 0) return
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
        }
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
        _state.update { it.copy(isPlaying = exoPlayer.isPlaying) }
    }

    fun pause() {
        if (exoPlayer.isPlaying) exoPlayer.pause()
    }

    fun next() {
        if (queue.isEmpty) return
        if (queue.shouldRepeatCurrent()) {
            replayCurrent()
            return
        }
        val advanced = queue.next()
        if (advanced.current == queue.current) {
            // Nothing to advance to: stop at the end of the queue.
            exoPlayer.pause()
            return
        }
        queue = advanced
        loadCurrent(advanced.current)
    }

    fun previous() {
        if (queue.isEmpty) return
        if (queue.shouldRepeatCurrent()) {
            replayCurrent()
            return
        }
        val rewindToStart = exoPlayer.currentPosition > REWIND_THRESHOLD_MILLIS
        val previous = if (rewindToStart) queue else queue.previous()
        queue = previous
        if (rewindToStart && previous.current?.id == _state.value.current?.id) {
            exoPlayer.seekTo(0L)
            _state.update { it.copy(positionMillis = 0L) }
            return
        }
        loadCurrent(previous.current)
    }

    fun seekToMillis(positionMillis: Long) {
        if (exoPlayer.mediaItemCount == 0) return
        val duration = _state.value.durationMillis.takeIf { it > 0 } ?: exoPlayer.duration.coerceAtLeast(0)
        val clamped = positionMillis.coerceIn(0L, duration)
        exoPlayer.seekTo(clamped)
        _state.update { it.copy(positionMillis = clamped) }
    }

    fun seekToProgress(progress: Float) {
        val duration = _state.value.durationMillis
        if (duration > 0) seekToMillis((duration * progress.coerceIn(0f, 1f)).toLong())
    }

    fun toggleShuffle() {
        queue = queue.toggleShuffle()
        _state.update { it.copy(shuffleEnabled = queue.shuffleEnabled, hasNext = queue.hasNext, hasPrevious = queue.hasPrevious) }
    }

    fun cycleRepeatMode() {
        queue = queue.cycleRepeat()
        _state.update { it.copy(repeatMode = queue.repeatMode, hasNext = queue.hasNext, hasPrevious = queue.hasPrevious) }
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        queue = PlaybackQueue.empty()
        _state.value = PlayerUiState()
    }

    /** Removes a track (for example when it is unfavourited) from the queue. */
    fun removeFromQueue(trackId: Long) {
        val removedCurrent = _state.value.current?.id == trackId
        queue = queue.remove(trackId)
        when {
            queue.isEmpty -> stop()
            removedCurrent -> loadCurrent(queue.current)
            else -> _state.update { it.copy(queueSize = queue.size, hasNext = queue.hasNext, hasPrevious = queue.hasPrevious) }
        }
    }

    fun release() {
        ticker?.cancel()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
        mediaSession.release()
        scope.coroutineContext[Job]?.cancel()
    }

    private fun loadCurrent(track: Track?) {
        if (track == null || !track.hasPreview) {
            _state.update { it.copy(isPlaying = false, isBuffering = false, current = track) }
            return
        }
        exoPlayer.setMediaItem(track.toMediaItem())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        _state.update {
            it.copy(
                current = track,
                isBuffering = true,
                failure = null,
                positionMillis = 0L,
                durationMillis = track.durationMillis ?: PREVIEW_DURATION_MILLIS,
                queueSize = queue.size,
                hasNext = queue.hasNext,
                hasPrevious = queue.hasPrevious,
                repeatMode = queue.repeatMode,
                shuffleEnabled = queue.shuffleEnabled,
            )
        }
    }

    private fun replayCurrent() {
        exoPlayer.seekTo(0L)
        exoPlayer.play()
    }

    private fun onPreviewCompleted() {
        val finishedId = _state.value.current?.id
        val advanced = queue.next()
        val isSameTrack = advanced.current?.id == finishedId
        when {
            queue.shouldRepeatCurrent() || (isSameTrack && queue.repeatMode == RepeatMode.ALL) -> replayCurrent()
            isSameTrack && advanced.repeatMode != RepeatMode.ALL -> {
                queue = advanced
                exoPlayer.pause()
                _state.update { it.copy(isPlaying = false, positionMillis = it.durationMillis) }
            }
            else -> {
                queue = advanced
                loadCurrent(advanced.current)
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying || pendingPositionRefresh()) {
                    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                    val duration = exoPlayer.duration.takeIf { it > 0 } ?: _state.value.durationMillis
                    _state.update { it.copy(positionMillis = position, durationMillis = duration) }
                }
                delay(POSITION_TICK_MILLIS)
            }
        }
    }

    private fun pendingPositionRefresh(): Boolean = _state.value.isBuffering

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setUri(previewUrl)
        .setMediaId(id.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(displayArtworkUrl?.toUri())
                .build(),
        )
        .build()

    private fun PlaybackException.toFailure(): PlaybackFailure {
        val kind = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            -> PlaybackFailureKind.NETWORK

            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            -> PlaybackFailureKind.UNSUPPORTED

            else -> PlaybackFailureKind.UNKNOWN
        }
        return PlaybackFailure(kind, errorCodeName)
    }

    private companion object {
        const val POSITION_TICK_MILLIS = 500L
        const val PREVIEW_DURATION_MILLIS = 30_000L
        const val REWIND_THRESHOLD_MILLIS = 3_000L
    }
}
