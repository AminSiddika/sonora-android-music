package dev.sonora.core.domain

/**
 * Tiny TTL cache used to keep the app well below the iTunes Search API rate
 * limit (~20 requests/minute per IP) while still feeling instant when a user
 * bounces between screens.
 */
class ExpiringCache<K : Any, V : Any>(
    private val ttlMillis: Long,
    private val maxEntries: Int = 32,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private data class Entry<V>(val value: V, val storedAt: Long)

    private val entries = LinkedHashMap<K, Entry<V>>()
    private val lock = Any()

    val size: Int
        get() = synchronized(lock) { entries.size }

    fun get(key: K): V? = synchronized(lock) {
        val entry = entries[key] ?: return null
        if (clock() - entry.storedAt > ttlMillis) {
            entries.remove(key)
            return null
        }
        // Refresh recency for the LRU eviction below.
        entries.remove(key)
        entries[key] = entry
        entry.value
    }

    fun put(key: K, value: V) = synchronized(lock) {
        entries.remove(key)
        entries[key] = Entry(value, clock())
        while (entries.size > maxEntries) {
            val oldest = entries.keys.firstOrNull() ?: break
            entries.remove(oldest)
        }
    }

    fun clear() = synchronized(lock) { entries.clear() }
}
