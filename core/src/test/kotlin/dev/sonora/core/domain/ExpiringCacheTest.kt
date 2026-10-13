package dev.sonora.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpiringCacheTest {

    private var now = 1_000L

    private fun cache(ttl: Long, maxEntries: Int = 3) = ExpiringCache<String, String>(
        ttlMillis = ttl,
        maxEntries = maxEntries,
        clock = { now },
    )

    @Test
    fun `stores and returns values inside the ttl`() {
        val cache = cache(ttl = 100)
        cache.put("a", "one")
        assertEquals("one", cache.get("a"))
    }

    @Test
    fun `expires values after the ttl`() {
        val cache = cache(ttl = 100)
        cache.put("a", "one")
        now += 101
        assertNull(cache.get("a"))
        assertEquals(0, cache.size)
    }

    @Test
    fun `evicts the least recently used entry`() {
        val cache = cache(ttl = 10_000, maxEntries = 2)
        cache.put("a", "1")
        cache.put("b", "2")
        // touch "a" so that "b" becomes the oldest entry
        assertEquals("1", cache.get("a"))
        cache.put("c", "3")

        assertEquals("1", cache.get("a"))
        assertEquals("3", cache.get("c"))
        assertNull(cache.get("b"))
    }

    @Test
    fun `missing keys return null`() {
        assertNull(cache(ttl = 100).get("nope"))
    }

    @Test
    fun `clear removes everything`() {
        val cache = cache(ttl = 100)
        cache.put("a", "1")
        cache.clear()
        assertEquals(0, cache.size)
    }
}
