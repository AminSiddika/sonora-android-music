package dev.sonora.core.domain

import dev.sonora.core.model.YouTubeAvailability

/**
 * Validates the optional YouTube Data API v3 key.
 *
 * The key is never required: without one Sonora stays fully usable with the
 * iTunes catalogue, and the UI explains how to enable YouTube search.
 */
object YouTubeApiKey {

    private val placeholders = setOf(
        "your_api_key",
        "your-api-key",
        "youtube_api_key",
        "youtube-api-key",
        "api_key",
        "changeme",
        "placeholder",
        "none",
        "null",
    )

    private const val MIN_LENGTH: Int = 20

    /** Returns the usable key, or null when [raw] is blank or clearly a placeholder. */
    fun sanitize(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        if (trimmed.length < MIN_LENGTH) return null
        val normalized = trimmed.lowercase().replace('_', '-')
        if (normalized in placeholders) return null
        if (placeholders.any { trimmed.lowercase().contains(it) }) return null
        return trimmed
    }

    fun availability(rawKey: String?): YouTubeAvailability {
        val trimmed = rawKey?.trim().orEmpty()
        return when {
            trimmed.isEmpty() -> YouTubeAvailability.MissingKey
            sanitize(trimmed) == null -> YouTubeAvailability.PlaceholderKey
            else -> YouTubeAvailability.Available
        }
    }
}
