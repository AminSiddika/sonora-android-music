package dev.sonora.core.domain

import dev.sonora.core.model.YouTubeAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class YouTubeApiKeyTest {

    private val validKey = "AIzaSyD-1234567890abcdefghijklmnopq"

    @Test
    fun `accepts a realistic key`() {
        assertEquals(validKey, YouTubeApiKey.sanitize(validKey))
        assertIs<YouTubeAvailability.Available>(YouTubeApiKey.availability(validKey))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(validKey, YouTubeApiKey.sanitize("  $validKey \n"))
    }

    @Test
    fun `rejects missing keys`() {
        assertNull(YouTubeApiKey.sanitize(null))
        assertNull(YouTubeApiKey.sanitize("   "))
        assertIs<YouTubeAvailability.MissingKey>(YouTubeApiKey.availability(""))
        assertIs<YouTubeAvailability.MissingKey>(YouTubeApiKey.availability(null))
    }

    @Test
    fun `rejects documentation placeholders`() {
        listOf("YOUR_API_KEY", "your-api-key", "youtube_api_key", "changeme", "none").forEach { placeholder ->
            assertNull(YouTubeApiKey.sanitize(placeholder), "expected $placeholder to be rejected")
            assertIs<YouTubeAvailability.PlaceholderKey>(YouTubeApiKey.availability(placeholder))
        }
    }

    @Test
    fun `rejects keys that are too short`() {
        assertNull(YouTubeApiKey.sanitize("shortkey"))
    }

    @Test
    fun `availability reports whether youtube can be used`() {
        assertTrue(YouTubeApiKey.availability(validKey).isAvailable)
        assertTrue(!YouTubeApiKey.availability(null).isAvailable)
    }
}
