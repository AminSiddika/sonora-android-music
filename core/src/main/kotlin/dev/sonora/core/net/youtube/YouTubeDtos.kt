package dev.sonora.core.net.youtube

import dev.sonora.core.domain.ExternalLinks
import dev.sonora.core.model.VideoItem
import kotlinx.serialization.Serializable

@Serializable
internal data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItemDto> = emptyList(),
)

@Serializable
internal data class YouTubeSearchItemDto(
    val id: YouTubeIdDto? = null,
    val snippet: YouTubeSnippetDto? = null,
) {
    fun toVideoOrNull(): VideoItem? {
        val videoId = id?.videoId?.takeIf { it.isNotBlank() } ?: return null
        val title = snippet?.title?.takeIf { it.isNotBlank() } ?: return null
        return VideoItem(
            videoId = videoId,
            title = title.trim(),
            channelTitle = snippet.channelTitle?.trim()?.takeIf { it.isNotEmpty() } ?: "YouTube",
            channelId = snippet.channelId?.trim()?.takeIf { it.isNotEmpty() },
            thumbnailUrl = snippet.thumbnails?.best() ?: ExternalLinks.youTubeThumbnail(videoId),
            description = snippet.description?.trim()?.takeIf { it.isNotEmpty() },
            publishedAt = snippet.publishedAt?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

@Serializable
internal data class YouTubeIdDto(
    val kind: String? = null,
    val videoId: String? = null,
)

@Serializable
internal data class YouTubeSnippetDto(
    val title: String? = null,
    val description: String? = null,
    val channelId: String? = null,
    val channelTitle: String? = null,
    val publishedAt: String? = null,
    val thumbnails: YouTubeThumbnailsDto? = null,
)

@Serializable
internal data class YouTubeThumbnailsDto(
    val default: YouTubeThumbnailDto? = null,
    val medium: YouTubeThumbnailDto? = null,
    val high: YouTubeThumbnailDto? = null,
) {
    fun best(): String? = listOf(high, medium, default)
        .firstNotNullOfOrNull { it?.url?.takeIf { url -> url.isNotBlank() } }
}

@Serializable
internal data class YouTubeThumbnailDto(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
internal data class YouTubeErrorEnvelope(
    val error: YouTubeErrorDto? = null,
)

@Serializable
internal data class YouTubeErrorDto(
    val code: Int? = null,
    val message: String? = null,
    val errors: List<YouTubeErrorDetailDto> = emptyList(),
)

@Serializable
internal data class YouTubeErrorDetailDto(
    val domain: String? = null,
    val reason: String? = null,
    val message: String? = null,
)
