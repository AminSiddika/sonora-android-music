package dev.sonora.app.di

import android.content.Context
import dev.sonora.app.BuildConfig
import dev.sonora.app.data.CatalogSession
import dev.sonora.app.data.DataStoreFavoritesRepository
import dev.sonora.app.data.DataStoreRecentlyPlayedRepository
import dev.sonora.app.data.DataStoreUserPreferences
import dev.sonora.app.data.NetworkMonitor
import dev.sonora.app.data.UserPreferences
import dev.sonora.app.data.YouTubeLauncher
import dev.sonora.app.player.SonoraPlayer
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.domain.SearchRepository
import dev.sonora.core.domain.VideoSearch
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.model.YouTubeAvailability
import dev.sonora.core.net.SonoraHttp
import dev.sonora.core.net.itunes.ITunesCatalog
import dev.sonora.core.net.youtube.YouTubeCatalog
import io.ktor.client.HttpClient

/**
 * Application scoped dependency graph.
 *
 * The YouTube provider is optional: when no usable key is compiled in, the
 * graph simply has no [VideoSearch] implementation and the UI explains how to
 * enable it. Nothing else in the app depends on YouTube being available.
 */
class SonoraGraph(context: Context) {

    private val appContext: Context = context.applicationContext

    val httpClient: HttpClient = SonoraHttp.create()

    /** Where the optional YouTube key came from; drives the UI messaging. */
    val youTubeAvailability: YouTubeAvailability = YouTubeApiKey.availability(BuildConfig.YOUTUBE_API_KEY)

    private val youTubeSearch: VideoSearch? = YouTubeApiKey
        .sanitize(BuildConfig.YOUTUBE_API_KEY)
        ?.let { key -> YouTubeCatalog(httpClient, key) }

    val searchRepository: SearchRepository = SearchRepository(
        catalog = ITunesCatalog(httpClient),
        videos = youTubeSearch,
        youTubeAvailability = youTubeAvailability,
    )

    val favoritesRepository: FavoritesRepository = DataStoreFavoritesRepository(appContext)

    val recentlyPlayedRepository: RecentlyPlayedRepository = DataStoreRecentlyPlayedRepository(appContext)

    val preferences: UserPreferences = DataStoreUserPreferences(appContext)

    val catalogSession: CatalogSession = CatalogSession()

    val networkMonitor: NetworkMonitor = NetworkMonitor(appContext)

    val youTubeLauncher: YouTubeLauncher = YouTubeLauncher(appContext)

    val player: SonoraPlayer = SonoraPlayer(appContext)

    val versionName: String = BuildConfig.VERSION_NAME

    fun youTubeApiKeyConfigured(): Boolean = youTubeAvailability.isAvailable
}
