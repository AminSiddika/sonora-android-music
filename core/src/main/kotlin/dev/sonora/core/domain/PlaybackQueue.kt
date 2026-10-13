package dev.sonora.core.domain

import dev.sonora.core.model.Track

/** Repeat behaviour for the local playback queue. */
enum class RepeatMode {
    OFF,
    ONE,
    ALL,
    ;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }
}

/**
 * Immutable playback queue with the shuffle/repeat rules factored out of the
 * UI and the ExoPlayer wrapper so they can be unit tested on the JVM.
 */
class PlaybackQueue private constructor(
    val tracks: List<Track>,
    /** Library indices in playback order. */
    val order: List<Int>,
    /** Position inside [order]. */
    private val cursor: Int,
    val shuffleEnabled: Boolean,
    val repeatMode: RepeatMode,
) {

    val size: Int get() = tracks.size

    val isEmpty: Boolean get() = tracks.isEmpty()

    val currentIndex: Int get() = order.getOrElse(cursor) { 0 }

    val current: Track? get() = tracks.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = !isEmpty && (repeatMode == RepeatMode.ALL || repeatMode == RepeatMode.ONE || cursor < order.lastIndex)

    val hasPrevious: Boolean
        get() = !isEmpty && (repeatMode == RepeatMode.ALL || repeatMode == RepeatMode.ONE || cursor > 0)

    /** True when the caller should keep playing the same item (repeat one). */
    fun shouldRepeatCurrent(): Boolean = repeatMode == RepeatMode.ONE && !isEmpty

    fun next(): PlaybackQueue {
        if (isEmpty) return this
        if (repeatMode == RepeatMode.ONE) return this
        if (cursor < order.lastIndex) return copy(cursor = cursor + 1)
        return if (repeatMode == RepeatMode.ALL) copy(cursor = 0) else this
    }

    fun previous(): PlaybackQueue {
        if (isEmpty) return this
        if (repeatMode == RepeatMode.ONE) return this
        if (cursor > 0) return copy(cursor = cursor - 1)
        return if (repeatMode == RepeatMode.ALL) copy(cursor = order.lastIndex) else this
    }

    fun seekToLibraryIndex(index: Int): PlaybackQueue {
        if (isEmpty) return this
        val clamped = index.coerceIn(0, tracks.lastIndex)
        val position = order.indexOf(clamped)
        return copy(cursor = if (position >= 0) position else cursor)
    }

    fun toggleShuffle(random: kotlin.random.Random = kotlin.random.Random.Default): PlaybackQueue {
        if (isEmpty) return copy(shuffleEnabled = !shuffleEnabled)
        if (shuffleEnabled) {
            val restored = order[cursor]
            return PlaybackQueue(tracks, tracks.indices.toList(), restored, shuffleEnabled = false, repeatMode = repeatMode)
        }
        val currentLibraryIndex = currentIndex
        val rest = tracks.indices.filter { it != currentLibraryIndex }.shuffled(random)
        return PlaybackQueue(
            tracks = tracks,
            order = listOf(currentLibraryIndex) + rest,
            cursor = 0,
            shuffleEnabled = true,
            repeatMode = repeatMode,
        )
    }

    fun cycleRepeat(): PlaybackQueue = copy(repeatMode = repeatMode.next())

    fun remove(trackId: Long): PlaybackQueue {
        val removedLibraryIndex = tracks.indexOfFirst { it.id == trackId }
        if (removedLibraryIndex < 0) return this
        val removedPosition = order.indexOf(removedLibraryIndex)
        val newTracks = tracks.filterIndexed { index, _ -> index != removedLibraryIndex }
        if (newTracks.isEmpty()) return empty()
        val newOrder = order
            .filter { it != removedLibraryIndex }
            .map { if (it > removedLibraryIndex) it - 1 else it }
        val newCursor = when {
            removedPosition < cursor -> cursor - 1
            removedPosition == cursor -> cursor.coerceAtMost(newOrder.lastIndex)
            else -> cursor
        }
        return PlaybackQueue(
            tracks = newTracks,
            order = newOrder,
            cursor = newCursor.coerceIn(0, newOrder.lastIndex),
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
        )
    }

    /** Replaces the contents, starting playback at [startLibraryIndex]. */
    fun replace(
        tracks: List<Track>,
        startLibraryIndex: Int = 0,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): PlaybackQueue = of(
        tracks = tracks,
        startLibraryIndex = startLibraryIndex,
        shuffle = shuffleEnabled,
        repeatMode = repeatMode,
        random = random,
    )

    private fun copy(
        tracks: List<Track> = this.tracks,
        order: List<Int> = this.order,
        cursor: Int = this.cursor,
        shuffleEnabled: Boolean = this.shuffleEnabled,
        repeatMode: RepeatMode = this.repeatMode,
    ): PlaybackQueue = PlaybackQueue(
        tracks = tracks,
        order = order,
        cursor = cursor.coerceIn(0, (order.lastIndex).coerceAtLeast(0)),
        shuffleEnabled = shuffleEnabled,
        repeatMode = repeatMode,
    )

    override fun toString(): String =
        "PlaybackQueue(size=$size, cursor=$cursor, shuffle=$shuffleEnabled, repeat=$repeatMode)"

    companion object {

        fun empty(): PlaybackQueue = PlaybackQueue(
            tracks = emptyList(),
            order = emptyList(),
            cursor = 0,
            shuffleEnabled = false,
            repeatMode = RepeatMode.OFF,
        )

        fun of(
            tracks: List<Track>,
            startLibraryIndex: Int = 0,
            shuffle: Boolean = false,
            repeatMode: RepeatMode = RepeatMode.OFF,
            random: kotlin.random.Random = kotlin.random.Random.Default,
        ): PlaybackQueue {
            if (tracks.isEmpty()) {
                return PlaybackQueue(emptyList(), emptyList(), 0, shuffle, repeatMode)
            }
            val start = startLibraryIndex.coerceIn(0, tracks.lastIndex)
            val order = if (shuffle) {
                listOf(start) + tracks.indices.filter { it != start }.shuffled(random)
            } else {
                tracks.indices.toList()
            }
            val cursor = if (shuffle) 0 else start
            return PlaybackQueue(tracks, order, cursor, shuffle, repeatMode)
        }
    }
}
