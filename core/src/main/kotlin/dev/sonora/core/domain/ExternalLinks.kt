package dev.sonora.core.domain

/**
 * Canonical external links. Keeping them here means the UI, the README and the
 * tests all agree on the same destinations, and that YouTube attribution is
 * never hard-coded in more than one place.
 */
object ExternalLinks {

    const val ITUNES_SEARCH_API_DOCS: String = "https://performance-partners.apple.com/search-api"

    const val YOUTUBE_WATCH_BASE: String = "https://www.youtube.com/watch"

    const val YOUTUBE_APP_SCHEME: String = "vnd.youtube:"

    const val YOUTUBE_API_KEY_GUIDE: String = "https://developers.google.com/youtube/registering_an_application"

    /** Required by the YouTube API Services Terms of Service. */
    const val YOUTUBE_TERMS_OF_SERVICE: String = "https://www.youtube.com/t/terms"

    /** Required by the YouTube API Services Terms of Service. */
    const val GOOGLE_PRIVACY_POLICY: String = "https://policies.google.com/privacy"

    const val YOUTUBE_API_SERVICES_TERMS: String =
        "https://developers.google.com/youtube/terms/api-services-terms-of-service"

    const val YOUTUBE_DEVELOPER_POLICIES: String =
        "https://developers.google.com/youtube/terms/developer-policies"

    fun youTubeWatchUrl(videoId: String): String = "$YOUTUBE_WATCH_BASE?v=$videoId"

    fun youTubeAppUri(videoId: String): String = "$YOUTUBE_APP_SCHEME$videoId"

    fun youTubeChannelUrl(channelId: String): String = "https://www.youtube.com/channel/$channelId"

    /** Thumbnail fallback when the API response omits one. */
    fun youTubeThumbnail(videoId: String): String = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"
}
