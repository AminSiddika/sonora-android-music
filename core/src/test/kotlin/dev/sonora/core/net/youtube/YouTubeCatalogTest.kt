package dev.sonora.core.net.youtube

import dev.sonora.core.TestFixtures
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.errorOrNull
import dev.sonora.core.model.getOrNull
import dev.sonora.core.net.SonoraHttp
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class YouTubeCatalogTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val key = "AIzaSyD-1234567890abcdefghijklmnopq"

    private suspend fun searchWith(
        limit: Int = 25,
        handler: MockRequestHandler,
    ) = YouTubeCatalog(SonoraHttp.create(MockEngine(handler)), apiKey = key).search("tiny desk", limit)

    @Test
    fun `parses videos and skips entries without a video id`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("youtube-search.json"), HttpStatusCode.OK, jsonHeaders)
        }

        val videos = result.getOrNull()
        assertNotNull(videos)
        assertEquals(2, videos.size)

        val first = videos.first()
        assertEquals("dQw4w9WgXcQ", first.videoId)
        assertEquals("Never Gonna Give You Up (Official Video)", first.title)
        assertEquals("Rick Astley", first.channelTitle)
        assertEquals("UCuAXFkgsw1L7xaCfnd5JJOw", first.channelId)
        assertEquals("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg", first.thumbnailUrl)
    }

    @Test
    fun `thumbnails fall back when the api omits them`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("youtube-search.json"), HttpStatusCode.OK, jsonHeaders)
        }

        val withoutThumbs = result.getOrNull()!!.first { it.videoId == "abcdefghijk" }
        assertEquals("https://i.ytimg.com/vi/abcdefghijk/mqdefault.jpg", withoutThumbs.thumbnailUrl)
    }

    @Test
    fun `requests stay inside the compliant parameter set`() = runTest {
        var capturedUrl: String? = null
        searchWith(limit = 5) { request ->
            capturedUrl = request.url.toString()
            respond(TestFixtures.text("youtube-search.json"), HttpStatusCode.OK, jsonHeaders)
        }

        val url = assertNotNull(capturedUrl)
        assertTrue(url.startsWith("https://www.googleapis.com/youtube/v3/search?"), url)
        assertTrue(url.contains("part=snippet"), url)
        assertTrue(url.contains("type=video"), url)
        assertTrue(url.contains("safeSearch=moderate"), url)
        assertTrue(url.contains("videoEmbeddable=true"), url)
        assertTrue(url.contains("maxResults=5"), url)
        assertTrue(url.contains("key=$key"), url)
        // Sonora never asks for stream URLs or embedded player responses.
        assertTrue(!url.contains("part=contentDetails") && !url.contains("videos?"))
    }

    @Test
    fun `quota exhaustion is reported separately from other 403s`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("youtube-error-quota.json"), HttpStatusCode.Forbidden, jsonHeaders)
        }
        assertEquals(SonoraError.QuotaExceeded, result.errorOrNull())
    }

    @Test
    fun `an invalid key is reported as such`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("youtube-error-key.json"), HttpStatusCode.BadRequest, jsonHeaders)
        }
        assertEquals(SonoraError.InvalidApiKey, result.errorOrNull())
    }

    @Test
    fun `unknown failures keep the status code`() = runTest {
        val result = searchWith { respond("nope", HttpStatusCode.ServiceUnavailable, jsonHeaders) }
        assertEquals(SonoraError.ServerError(503), result.errorOrNull())
    }

    @Test
    fun `malformed json does not crash the app`() = runTest {
        val result = searchWith { respond("<html>maintenance</html>", HttpStatusCode.OK, jsonHeaders) }
        assertEquals(SonoraError.MalformedResponse, result.errorOrNull())
    }
}
