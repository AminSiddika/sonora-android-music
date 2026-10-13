package dev.sonora.app.data

import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Remembers the last search results so that detail screens can be opened by id
 * and re-rendered after a configuration change without another network call.
 */
class CatalogSession {

    private val tracks = MutableStateFlow<Map<Long, Track>>(emptyMap())
    private val videos = MutableStateFlow<Map<String, VideoItem>>(emptyMap())

    fun rememberTracks(newTracks: List<Track>) {
        if (newTracks.isEmpty()) return
        tracks.update { current -> current + newTracks.associateBy(Track::id) }
    }

    fun rememberVideos(newVideos: List<VideoItem>) {
        if (newVideos.isEmpty()) return
        videos.update { current -> current + newVideos.associateBy(VideoItem::videoId) }
    }

    fun track(id: Long): Track? = tracks.value[id]

    fun video(videoId: String): VideoItem? = videos.value[videoId]

    fun clear() {
        tracks.value = emptyMap()
        videos.value = emptyMap()
    }
}
