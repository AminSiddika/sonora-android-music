package dev.sonora.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExternalLinksTest {

    @Test
    fun `builds watch urls from video ids`() {
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", ExternalLinks.youTubeWatchUrl("dQw4w9WgXcQ"))
    }

    @Test
    fun `builds the official app uri`() {
        assertEquals("vnd.youtube:dQw4w9WgXcQ", ExternalLinks.youTubeAppUri("dQw4w9WgXcQ"))
    }

    @Test
    fun `falls back to a thumbnail url`() {
        assertEquals("https://i.ytimg.com/vi/abc/mqdefault.jpg", ExternalLinks.youTubeThumbnail("abc"))
    }

    @Test
    fun `exposes the policy links required for youtube attribution`() {
        val required = listOf(
            ExternalLinks.YOUTUBE_TERMS_OF_SERVICE,
            ExternalLinks.GOOGLE_PRIVACY_POLICY,
            ExternalLinks.YOUTUBE_API_SERVICES_TERMS,
            ExternalLinks.YOUTUBE_DEVELOPER_POLICIES,
        )
        assertTrue(required.all { it.startsWith("https://") })
    }
}
