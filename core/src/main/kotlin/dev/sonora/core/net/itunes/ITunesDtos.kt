package dev.sonora.core.net.itunes

import dev.sonora.core.model.Track
import kotlinx.serialization.Serializable

@Serializable
internal data class ITunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ITunesTrackDto> = emptyList(),
)

/**
 * Subset of the fields documented for the iTunes Search API song entity.
 * Everything is nullable because Apple omits fields for some storefronts.
 */
@Serializable
internal data class ITunesTrackDto(
    val wrapperType: String? = null,
    val kind: String? = null,
    val trackId: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val collectionName: String? = null,
    val artworkUrl100: String? = null,
    val previewUrl: String? = null,
    val trackTimeMillis: Long? = null,
    val primaryGenreName: String? = null,
    val releaseDate: String? = null,
    val trackViewUrl: String? = null,
    val collectionViewUrl: String? = null,
    val trackExplicitness: String? = null,
    val contentAdvisoryRating: String? = null,
) {
    /** Returns null when the entry cannot be shown meaningfully. */
    fun toTrackOrNull(): Track? {
        val id = trackId ?: return null
        val name = trackName?.takeIf { it.isNotBlank() } ?: return null
        val artist = artistName?.takeIf { it.isNotBlank() } ?: return null
        val preview = previewUrl?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?: previewUrl?.takeIf { it.isNotBlank() }
        return Track(
            id = id,
            title = name.trim(),
            artist = artist.trim(),
            album = collectionName?.trim()?.takeIf { it.isNotEmpty() },
            artworkUrl = artworkUrl100?.trim()?.takeIf { it.isNotEmpty() },
            previewUrl = preview,
            durationMillis = trackTimeMillis?.takeIf { it > 0 },
            genre = primaryGenreName?.trim()?.takeIf { it.isNotEmpty() },
            releaseDate = releaseDate?.trim()?.takeIf { it.isNotEmpty() },
            storeUrl = (trackViewUrl ?: collectionViewUrl)?.trim()?.takeIf { it.isNotEmpty() },
            explicit = isExplicit(),
        )
    }

    private fun isExplicit(): Boolean =
        contentAdvisoryRating.equals("explicit", ignoreCase = true) ||
            trackExplicitness.equals("explicit", ignoreCase = true)
}
