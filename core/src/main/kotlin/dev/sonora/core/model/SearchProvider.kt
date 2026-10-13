package dev.sonora.core.model

/** The two search sources Sonora can query. */
enum class SearchProvider {
    /** Apple's public iTunes Search API: no key required, 30 second previews. */
    ITUNES,

    /** Optional YouTube Data API v3 search: requires a user supplied key. */
    YOUTUBE,
    ;

    companion object {
        val default: SearchProvider = ITUNES
    }
}

/** Why the optional YouTube provider is (un)available. */
sealed interface YouTubeAvailability {
    data object Available : YouTubeAvailability

    /** No key was compiled in or entered at runtime. */
    data object MissingKey : YouTubeAvailability

    /** A key was provided but looks like a documentation placeholder. */
    data object PlaceholderKey : YouTubeAvailability

    val isAvailable: Boolean
        get() = this is Available
}

/** Result of asking the repository which providers can be used. */
data class SearchCapabilities(
    val youTube: YouTubeAvailability,
) {
    fun isAvailable(provider: SearchProvider): Boolean = when (provider) {
        SearchProvider.ITUNES -> true
        SearchProvider.YOUTUBE -> youTube.isAvailable
    }
}
