package dev.sonora.core.domain

import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesRepositoryTest {

    private fun track(id: Long) = Track(id = id, title = "Track $id", artist = "Artist")

    @Test
    fun `new favourites are added to the front`() = runTest {
        val repository = InMemoryFavoritesRepository()

        assertTrue(repository.toggle(track(1)))
        assertTrue(repository.toggle(track(2)))

        assertEquals(listOf(2L, 1L), repository.favorites.first().map { it.id })
    }

    @Test
    fun `toggling an existing favourite removes it`() = runTest {
        val repository = InMemoryFavoritesRepository(listOf(track(1)))

        val isFavorite = repository.toggle(track(1))

        assertFalse(isFavorite)
        assertEquals(emptyList(), repository.favorites.first())
    }

    @Test
    fun `adding twice does not duplicate`() = runTest {
        val repository = InMemoryFavoritesRepository()

        repository.add(track(7))
        repository.add(track(7))

        assertEquals(1, repository.favorites.first().size)
    }

    @Test
    fun `removing by id only drops that entry`() = runTest {
        val repository = InMemoryFavoritesRepository(listOf(track(1), track(2), track(3)))

        repository.remove(2)

        assertEquals(listOf(1L, 3L), repository.favorites.first().map { it.id })
    }

    @Test
    fun `favourites survive a round trip through the json serializer`() = runTest {
        val repository = InMemoryFavoritesRepository()
        val original = Track(
            id = 5,
            title = "Don't Know Why",
            artist = "Norah Jones",
            album = "Come Away With Me",
            artworkUrl = "https://example.com/100x100bb.jpg",
            previewUrl = "https://example.com/preview.m4a",
            durationMillis = 186_000,
            genre = "Jazz",
            releaseDate = "2002-02-26T08:00:00Z",
            storeUrl = "https://music.apple.com/album/5",
            explicit = false,
        )

        repository.add(original)
        val stored = repository.favorites.first().first()
        val encoded = kotlinx.serialization.json.Json.encodeToString(Track.serializer(), stored)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(Track.serializer(), encoded)

        assertEquals(stored, decoded)
        assertTrue(decoded.hasPreview)
    }
}
