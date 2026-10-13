package dev.sonora.core.model

import kotlinx.serialization.Serializable

/**
 * A YouTube video returned by the optional YouTube Data API v3 search provider.
 *
 * Sonora never extracts stream URLs for these items. Playback always happens in
 * the official YouTube application or website.
 */
@Serializable
data class VideoItem(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val channelId: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
)
