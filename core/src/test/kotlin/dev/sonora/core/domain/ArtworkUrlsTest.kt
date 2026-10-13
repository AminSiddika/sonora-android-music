package dev.sonora.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtworkUrlsTest {

    private val realUrl =
        "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/aa/bb/cc/dd.jpg/100x100bb.jpg"

    @Test
    fun `upgrades the size token to the requested size`() {
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/aa/bb/cc/dd.jpg/600x600bb.jpg",
            ArtworkUrls.upgrade(realUrl),
        )
    }

    @Test
    fun `keeps the suffix and extension`() {
        assertEquals(
            "https://example.com/art/300x300ff.png",
            ArtworkUrls.upgrade("https://example.com/art/30x30ff.png", size = 300),
        )
    }

    @Test
    fun `returns null for blank or missing urls`() {
        assertNull(ArtworkUrls.upgrade(null))
        assertNull(ArtworkUrls.upgrade("   "))
    }

    @Test
    fun `returns the original url when there is no size token`() {
        val plain = "https://example.com/artwork.jpg"
        assertEquals(plain, ArtworkUrls.upgrade(plain))
    }
}
