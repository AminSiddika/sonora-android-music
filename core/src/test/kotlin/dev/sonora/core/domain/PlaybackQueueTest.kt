package dev.sonora.core.domain

import dev.sonora.core.model.Track
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaybackQueueTest {

    private fun track(id: Long) = Track(id = id, title = "Track $id", artist = "Artist")

    private val tracks = listOf(track(1), track(2), track(3), track(4))

    @Test
    fun `empty queue exposes no current item`() {
        val queue = PlaybackQueue.empty()
        assertTrue(queue.isEmpty)
        assertNull(queue.current)
        assertFalse(queue.hasNext)
        assertFalse(queue.hasPrevious)
    }

    @Test
    fun `starts at the requested index`() {
        val queue = PlaybackQueue.of(tracks, startLibraryIndex = 2)
        assertEquals(2, queue.currentIndex)
        assertEquals(3L, queue.current?.id)
        assertTrue(queue.hasNext)
        assertTrue(queue.hasPrevious)
    }

    @Test
    fun `next stops at the end when repeat is off`() {
        val queue = PlaybackQueue.of(tracks, startLibraryIndex = tracks.lastIndex)
        assertFalse(queue.hasNext)
        assertEquals(queue.current, queue.next().current)
    }

    @Test
    fun `next wraps around when repeat all is on`() {
        var queue = PlaybackQueue.of(tracks, startLibraryIndex = tracks.lastIndex, repeatMode = RepeatMode.ALL)
        queue = queue.next()
        assertEquals(0, queue.currentIndex)
    }

    @Test
    fun `previous wraps backwards when repeat all is on`() {
        var queue = PlaybackQueue.of(tracks, repeatMode = RepeatMode.ALL)
        queue = queue.previous()
        assertEquals(tracks.lastIndex, queue.currentIndex)
    }

    @Test
    fun `repeat one keeps the current item`() {
        val queue = PlaybackQueue.of(tracks, startLibraryIndex = 1, repeatMode = RepeatMode.ONE)
        assertTrue(queue.shouldRepeatCurrent())
        assertEquals(queue.current, queue.next().current)
        assertEquals(queue.current, queue.previous().current)
    }

    @Test
    fun `repeat mode cycles off all one`() {
        var queue = PlaybackQueue.of(tracks)
        assertEquals(RepeatMode.OFF, queue.repeatMode)
        queue = queue.cycleRepeat()
        assertEquals(RepeatMode.ALL, queue.repeatMode)
        queue = queue.cycleRepeat()
        assertEquals(RepeatMode.ONE, queue.repeatMode)
        queue = queue.cycleRepeat()
        assertEquals(RepeatMode.OFF, queue.repeatMode)
    }

    @Test
    fun `shuffle keeps every track exactly once and starts on the current item`() {
        val queue = PlaybackQueue.of(tracks, startLibraryIndex = 2, shuffle = true, random = Random(7))
        assertTrue(queue.shuffleEnabled)
        assertEquals(3L, queue.current?.id)
        assertEquals(tracks.indices.toSet(), queue.order.toSet())
        assertEquals(tracks.size, queue.order.size)
    }

    @Test
    fun `shuffle walks the shuffled order without repeating`() {
        var queue = PlaybackQueue.of(tracks, startLibraryIndex = 0, shuffle = true, random = Random(3))
        val visited = mutableListOf(queue.currentIndex)
        repeat(tracks.size - 1) {
            queue = queue.next()
            visited += queue.currentIndex
        }
        assertEquals(tracks.indices.toSet(), visited.toSet())
        assertFalse(queue.hasNext)
    }

    @Test
    fun `turning shuffle off restores the current track`() {
        val shuffled = PlaybackQueue.of(tracks, startLibraryIndex = 3, shuffle = true, random = Random(11))
        val current = shuffled.current
        val restored = shuffled.toggleShuffle()
        assertFalse(restored.shuffleEnabled)
        assertEquals(current, restored.current)
        assertEquals(tracks.indices.toList(), restored.order)
    }

    @Test
    fun `removing the current track keeps the queue playable`() {
        val queue = PlaybackQueue.of(tracks, startLibraryIndex = 1)
        val updated = queue.remove(trackId = 2)
        assertEquals(tracks.size - 1, updated.size)
        assertEquals(3L, updated.current?.id)
        assertFalse(updated.tracks.any { it.id == 2L })
    }

    @Test
    fun `removing the last remaining track empties the queue`() {
        val queue = PlaybackQueue.of(listOf(track(42)))
        assertTrue(queue.remove(42).isEmpty)
    }

    @Test
    fun `replacing the queue keeps shuffle and repeat preferences`() {
        val queue = PlaybackQueue.of(tracks, shuffle = true, repeatMode = RepeatMode.ALL, random = Random(5))
        val replacement = queue.replace(listOf(track(90), track(91)), startLibraryIndex = 1, random = Random(5))
        assertTrue(replacement.shuffleEnabled)
        assertEquals(RepeatMode.ALL, replacement.repeatMode)
        assertEquals(91L, replacement.current?.id)
        assertEquals(2, replacement.size)
    }
}
