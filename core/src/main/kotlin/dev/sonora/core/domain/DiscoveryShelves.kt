package dev.sonora.core.domain

import dev.sonora.core.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * Small history of what the user listened to, used by the discovery screen so
 * that it has something meaningful to show offline.
 */
interface RecentlyPlayedRepository {

    val recentlyPlayed: Flow<List<Track>>

    suspend fun record(track: Track)

    suspend fun clear()
}

/**
 * A shelf or "mood" on the discovery screen. The iTunes Search API has no
 * browsing or trending endpoint, so discovery is expressed as curated searches
 * that are requested on demand (which also keeps us inside the rate limit).
 */
data class DiscoveryShelf(
    val id: String,
    val title: String,
    val subtitle: String,
    val query: String,
)

object DiscoveryShelves {

    val ALL: List<DiscoveryShelf> = listOf(
        DiscoveryShelf(
            id = "lofi",
            title = "Lo-fi focus",
            subtitle = "Soft beats for deep work",
            query = "lo-fi beats",
        ),
        DiscoveryShelf(
            id = "jazz",
            title = "Late night jazz",
            subtitle = "Standards and smoky horns",
            query = "jazz classics",
        ),
        DiscoveryShelf(
            id = "classical",
            title = "Classical calm",
            subtitle = "Strings, keys and quiet",
            query = "classical piano",
        ),
        DiscoveryShelf(
            id = "synthwave",
            title = "Synthwave drive",
            subtitle = "Neon and retro arpeggios",
            query = "synthwave",
        ),
        DiscoveryShelf(
            id = "soul",
            title = "Soul & funk",
            subtitle = "Grooves with a warm low end",
            query = "soul funk",
        ),
        DiscoveryShelf(
            id = "acoustic",
            title = "Acoustic mornings",
            subtitle = "Guitars and open windows",
            query = "acoustic folk",
        ),
        DiscoveryShelf(
            id = "electronic",
            title = "Electronic pulse",
            subtitle = "Club-leaning electronics",
            query = "electronic dance",
        ),
        DiscoveryShelf(
            id = "world",
            title = "Around the world",
            subtitle = "Rhythms beyond borders",
            query = "world music",
        ),
    )

    /**
     * Deterministic "shelf of the day" so the home screen feels alive without
     * extra network calls or randomness that would break tests.
     */
    fun featuredFor(epochDay: Long): DiscoveryShelf {
        val index = ((epochDay % ALL.size) + ALL.size) % ALL.size
        return ALL[index.toInt()]
    }

    /** Chips shown under the search field. */
    val QUICK_SEARCHES: List<String> = listOf(
        "Top hits",
        "Indie",
        "Hip-hop",
        "Rock",
        "R&B",
        "Soundtrack",
        "K-pop",
        "Country",
        "Metal",
        "Reggae",
    )
}
