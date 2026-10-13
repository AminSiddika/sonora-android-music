package dev.sonora.core.model

import dev.sonora.core.domain.ArtworkUrls
import kotlinx.serialization.Serializable

/**
 * A single song returned by the iTunes Search API.
 *
 * Only the public 30 second [previewUrl] provided by Apple is ever played; see
 * [hasPreview]. Nothing here represents a download or a full-length stream.
 */
@Serializable
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val previewUrl: String? = null,
    val durationMillis: Long? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val storeUrl: String? = null,
    val explicit: Boolean = false,
) {
    /** True when Apple supplied a legally playable 30 second preview. */
    val hasPreview: Boolean
        get() = !previewUrl.isNullOrBlank()

    /** Artwork scaled up for the phone UI, when artwork exists. */
    val displayArtworkUrl: String?
        get() = ArtworkUrls.upgrade(artworkUrl)

    val subtitle: String
        get() = album?.takeIf { it.isNotBlank() && it != title }?.let { "$artist • $it" } ?: artist
}
