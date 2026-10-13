package dev.sonora.core.net.itunes

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
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ITunesCatalogTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private suspend fun searchWith(
        limit: Int = 25,
        handler: MockRequestHandler,
    ) = ITunesCatalog(SonoraHttp.create(MockEngine(handler))).search("norah jones", limit)

    @Test
    fun `parses songs, upgrades artwork and skips unusable entries`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("itunes-search-songs.json"), HttpStatusCode.OK, jsonHeaders)
        }

        val tracks = result.getOrNull()
        assertNotNull(tracks)
        assertEquals(3, tracks.size)

        val first = tracks.first()
        assertEquals(1440857788L, first.id)
        assertEquals("Don't Know Why", first.title)
        assertEquals("Norah Jones", first.artist)
        assertEquals("Come Away With Me", first.album)
        assertEquals(186_000L, first.durationMillis)
        assertEquals("Jazz", first.genre)
        assertTrue(first.hasPreview)
        assertEquals("2002", dev.sonora.core.domain.DurationFormat.year(first.releaseDate))
        assertFalse(first.explicit)
        assertTrue(first.displayArtworkUrl!!.endsWith("600x600bb.jpg"))
        assertTrue(first.storeUrl!!.startsWith("https://music.apple.com"))
    }

    @Test
    fun `tracks without a preview are still listed but not playable`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("itunes-search-songs.json"), HttpStatusCode.OK, jsonHeaders)
        }

        val withoutPreview = result.getOrNull()!!.first { it.title == "Untitled Drift" }
        assertFalse(withoutPreview.hasPreview)
        assertNull(withoutPreview.previewUrl)
        assertTrue(withoutPreview.displayArtworkUrl!!.endsWith("600x600bb.png"))
    }

    @Test
    fun `explicit content is flagged from the advisory rating`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("itunes-search-songs.json"), HttpStatusCode.OK, jsonHeaders)
        }

        assertTrue(result.getOrNull()!!.first { it.title == "Turn It Up" }.explicit)
    }

    @Test
    fun `an empty result set is a success`() = runTest {
        val result = searchWith {
            respond(TestFixtures.text("itunes-search-empty.json"), HttpStatusCode.OK, jsonHeaders)
        }

        assertEquals(emptyList(), result.getOrNull())
        assertNull(result.errorOrNull())
    }

    @Test
    fun `sends the documented search parameters`() = runTest {
        var capturedUrl: String? = null
        searchWith(limit = 10) { request ->
            capturedUrl = request.url.toString()
            respond(TestFixtures.text("itunes-search-empty.json"), HttpStatusCode.OK, jsonHeaders)
        }

        val url = assertNotNull(capturedUrl)
        assertTrue(url.startsWith("https://itunes.apple.com/search?"), url)
        assertTrue(url.contains("term=norah"), url)
        assertTrue(url.contains("media=music"), url)
        assertTrue(url.contains("entity=song"), url)
        assertTrue(url.contains("limit=10"), url)
        assertTrue(url.contains("country=US"), url)
    }

    @Test
    fun `limit is clamped to the provider maximum`() = runTest {
        var capturedUrl: String? = null
        searchWith(limit = 500) { request ->
            capturedUrl = request.url.toString()
            respond(TestFixtures.text("itunes-search-empty.json"), HttpStatusCode.OK, jsonHeaders)
        }

        assertTrue(assertNotNull(capturedUrl).contains("limit=${ITunesCatalog.MAX_LIMIT}"))
    }

    @Test
    fun `http 429 is reported as rate limited`() = runTest {
        val result = searchWith { respond("slow down", HttpStatusCode.TooManyRequests, jsonHeaders) }
        assertEquals(SonoraError.RateLimited, result.errorOrNull())
    }

    @Test
    fun `http 403 is reported as rate limited`() = runTest {
        val result = searchWith { respond("forbidden", HttpStatusCode.Forbidden, jsonHeaders) }
        assertEquals(SonoraError.RateLimited, result.errorOrNull())
    }

    @Test
    fun `http 500 keeps the status code`() = runTest {
        val result = searchWith { respond("kaboom", HttpStatusCode.InternalServerError, jsonHeaders) }
        assertEquals(SonoraError.ServerError(500), result.errorOrNull())
    }

    @Test
    fun `malformed json becomes a typed error instead of a crash`() = runTest {
        val result = searchWith { respond("{not json at all", HttpStatusCode.OK, jsonHeaders) }
        assertEquals(SonoraError.MalformedResponse, result.errorOrNull())
    }

    @Test
    fun `offline failures are reported as no connection`() = runTest {
        val result = searchWith { throw UnknownHostException("offline") }
        assertEquals(SonoraError.NoConnection, result.errorOrNull())
    }

    @Test
    fun `socket timeouts are reported as timeouts`() = runTest {
        val result = searchWith { throw SocketTimeoutException("timeout") }
        assertEquals(SonoraError.Timeout, result.errorOrNull())
    }
}
