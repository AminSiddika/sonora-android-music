package dev.sonora.core.domain

import dev.sonora.core.model.YouTubeAvailability

/** Where the effective YouTube key came from. */
enum class YouTubeKeySource {
    /** A key the user saved inside the app. */
    RUNTIME,

    /** A key compiled into the build from local.properties, an env var or a CI secret. */
    BUILD_CONFIG,

    /** No key at all: catalogue-only mode. */
    NONE,
}

/** Result of combining the runtime key with the build-time key. */
data class YouTubeKeyResolution(
    val key: String?,
    val availability: YouTubeAvailability,
    val source: YouTubeKeySource,
)

/**
 * Validates the optional YouTube Data API v3 key.
 *
 * The key is never required: without one Sonora stays fully usable with the
 * iTunes catalogue, and the UI explains how to enable YouTube search. A key
 * saved in the app takes precedence over the one compiled into the build.
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

    /**
     * Combines the runtime key with the build-time key.
     *
     * A blank runtime value falls back to the build-time key; an unusable value
     * (a placeholder such as `YOUR_API_KEY`) is reported as a placeholder so the
     * UI can tell the user what is wrong instead of silently searching the
     * catalogue.
     */
    fun resolve(runtimeKey: String?, buildTimeKey: String?): YouTubeKeyResolution {
        val runtime = runtimeKey?.trim().orEmpty()
        if (runtime.isNotEmpty()) {
            val sanitized = sanitize(runtime)
            return YouTubeKeyResolution(
                key = sanitized,
                availability = if (sanitized == null) YouTubeAvailability.PlaceholderKey else YouTubeAvailability.Available,
                source = if (sanitized == null) YouTubeKeySource.NONE else YouTubeKeySource.RUNTIME,
            )
        }

        val buildTime = buildTimeKey?.trim().orEmpty()
        val sanitized = sanitize(buildTime)
        return YouTubeKeyResolution(
            key = sanitized,
            availability = when {
                buildTime.isEmpty() -> YouTubeAvailability.MissingKey
                sanitized == null -> YouTubeAvailability.PlaceholderKey
                else -> YouTubeAvailability.Available
            },
            source = if (sanitized == null) YouTubeKeySource.NONE else YouTubeKeySource.BUILD_CONFIG,
        )
    }
}
