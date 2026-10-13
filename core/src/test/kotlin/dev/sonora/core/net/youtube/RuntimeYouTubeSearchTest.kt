package dev.sonora.core.net.youtube

import dev.sonora.core.TestFixtures
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.getOrNull
import dev.sonora.core.model.errorOrNull
import dev.sonora.core.net.SonoraHttp
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies the runtime credential path: the same search call must work with no
 * key, with the build-time key, and with a key the user typed in later.
 */
class RuntimeYouTubeSearchTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val buildTimeKey = "AIzaSyBuildTimeKey1234567890abcdefg"
    private val runtimeKey = "AIzaSyRuntimeKey1234567890abcdefghij"

    private class Recorder {
        val urls = mutableListOf<String>()
        var requests = 0
    }

    private fun search(
        rawKeyProvider: () -> String?,
        recorder: Recorder,
    ): RuntimeYouTubeSearch {
        val engine = MockEngine { request ->
            recorder.requests++
            recorder.urls += request.url.toString()
            respond(TestFixtures.text("youtube-search.json"), HttpStatusCode.OK, jsonHeaders)
        }
        return RuntimeYouTubeSearch(SonoraHttp.create(engine), rawKeyProvider)
    }

    @Test
    fun `without any key search fails before touching the network`() = runTest {
        val recorder = Recorder()
        val search = search({ null }, recorder)

        val result = search.search("tiny desk", 10)

        assertEquals(SonoraError.MissingApiKey, result.errorOrNull())
        assertEquals(0, recorder.requests)
        assertFalse(search.isConfigured)
    }

    @Test
    fun `a placeholder key is treated as missing`() = runTest {
        val recorder = Recorder()
        val search = search({ "YOUR_API_KEY" }, recorder)

        assertEquals(SonoraError.MissingApiKey, search.search("tiny desk", 10).errorOrNull())
        assertEquals(0, recorder.requests)
    }

    @Test
    fun `the build-time key is used when the user has not saved one`() = runTest {
        val recorder = Recorder()
        val search = search({ buildTimeKey }, recorder)

        val result = search.search("tiny desk", 10)

        assertEquals(2, result.getOrNull()?.size)
        assertTrue(assertNotNull(recorder.urls.firstOrNull()).contains("key=$buildTimeKey"))
        assertTrue(search.isConfigured)
    }

    @Test
    fun `a key saved at runtime is picked up without rebuilding`() = runTest {
        val recorder = Recorder()
        var currentKey: String? = buildTimeKey
        val search = search({ currentKey }, recorder)

        search.search("tiny desk", 5)
        assertEquals(1, recorder.requests)

        // The user pastes a different key in Settings while the app is running.
        currentKey = runtimeKey
        val second = search.search("tiny desk", 5)

        assertEquals(2, recorder.requests)
        assertTrue(second.getOrNull() != null)
        assertTrue(recorder.urls[1].contains("key=$runtimeKey"), recorder.urls[1])
        assertFalse(recorder.urls[1].contains(buildTimeKey))
    }

    @Test
    fun `the delegate is reused while the key does not change`() = runTest {
        val recorder = Recorder()
        val search = search({ buildTimeKey }, recorder)

        search.search("one", 5)
        search.search("two", 5)

        assertEquals(2, recorder.requests)
        assertEquals(2, recorder.urls.size)
        assertTrue(recorder.urls.all { it.contains("key=$buildTimeKey") })
    }

    @Test
    fun `removing the key at runtime stops using YouTube`() = runTest {
        val recorder = Recorder()
        var currentKey: String? = runtimeKey
        val search = search({ currentKey }, recorder)

        search.search("tiny desk", 5)
        assertEquals(1, recorder.requests)

        currentKey = null
        assertEquals(SonoraError.MissingApiKey, search.search("tiny desk", 5).errorOrNull())
        assertEquals(1, recorder.requests)
    }

    @Test
    fun `resolve prefers the runtime key over the build-time key`() {
        val resolution = YouTubeApiKey.resolve(runtimeKey, buildTimeKey)

        assertEquals(runtimeKey, resolution.key)
        assertEquals(dev.sonora.core.domain.YouTubeKeySource.RUNTIME, resolution.source)
        assertTrue(resolution.availability.isAvailable)
    }

    @Test
    fun `resolve falls back to the build-time key`() {
        val resolution = YouTubeApiKey.resolve(null, buildTimeKey)

        assertEquals(buildTimeKey, resolution.key)
        assertEquals(dev.sonora.core.domain.YouTubeKeySource.BUILD_CONFIG, resolution.source)
    }

    @Test
    fun `resolve reports a placeholder typed by the user`() {
        val resolution = YouTubeApiKey.resolve("YOUR_API_KEY", buildTimeKey)

        assertEquals(null, resolution.key)
        assertEquals(dev.sonora.core.domain.YouTubeKeySource.NONE, resolution.source)
        assertFalse(resolution.availability.isAvailable)
    }

    @Test
    fun `resolve reports when no credential exists at all`() {
        val resolution = YouTubeApiKey.resolve(null, "")

        assertEquals(null, resolution.key)
        assertEquals(dev.sonora.core.domain.YouTubeKeySource.NONE, resolution.source)
        assertEquals(dev.sonora.core.model.YouTubeAvailability.MissingKey, resolution.availability)
    }
}
