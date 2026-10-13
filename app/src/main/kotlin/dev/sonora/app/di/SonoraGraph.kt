package dev.sonora.app.di

import android.content.Context
import dev.sonora.app.BuildConfig
import dev.sonora.app.data.CatalogSession
import dev.sonora.app.data.DataStoreFavoritesRepository
import dev.sonora.app.data.DataStoreRecentlyPlayedRepository
import dev.sonora.app.data.DataStoreUserPreferences
import dev.sonora.app.data.DataStoreYouTubeKeyStore
import dev.sonora.app.data.NetworkMonitor
import dev.sonora.app.data.UserPreferences
import dev.sonora.app.data.YouTubeKeyStore
import dev.sonora.app.data.YouTubeLauncher
import dev.sonora.app.player.SonoraPlayer
import dev.sonora.core.domain.FavoritesRepository
import dev.sonora.core.domain.RecentlyPlayedRepository
import dev.sonora.core.domain.SearchRepository
import dev.sonora.core.domain.VideoSearch
import dev.sonora.core.domain.YouTubeApiKey
import dev.sonora.core.domain.YouTubeKeyResolution
import dev.sonora.core.model.SonoraError
import dev.sonora.core.net.SonoraHttp
import dev.sonora.core.net.itunes.ITunesCatalog
import dev.sonora.core.net.youtube.RuntimeYouTubeSearch
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Application scoped dependency graph.
 *
 * The YouTube provider is optional and its credential is resolved at runtime:
 * a key saved in Settings wins over the key compiled into the build from
 * `local.properties`, an environment variable or a CI secret. When neither
 * exists the graph reports "missing key" and the catalogue keeps working.
 */
class SonoraGraph(context: Context) {

    private val appContext: Context = context.applicationContext

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val httpClient: HttpClient = SonoraHttp.create()

    val youTubeKeyStore: YouTubeKeyStore = DataStoreYouTubeKeyStore(appContext)

    /** Effective credential plus where it came from, recomputed on every change. */
    val youTubeKey: StateFlow<YouTubeKeyResolution> = youTubeKeyStore.key
        .map { runtimeKey -> YouTubeApiKey.resolve(runtimeKey, BuildConfig.YOUTUBE_API_KEY) }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = YouTubeApiKey.resolve(null, BuildConfig.YOUTUBE_API_KEY),
        )

    /**
     * Always present; it reports [SonoraError.MissingApiKey] until a usable key
     * exists, which is what the UI surfaces.
     */
    private val youTubeSearch: VideoSearch = RuntimeYouTubeSearch(
        client = httpClient,
        keyProvider = { youTubeKey.value.key },
    )

    val searchRepository: SearchRepository = SearchRepository(
        catalog = ITunesCatalog(httpClient),
        videos = youTubeSearch,
        youTubeAvailability = { youTubeKey.value.availability },
    )

    val favoritesRepository: FavoritesRepository = DataStoreFavoritesRepository(appContext)

    val recentlyPlayedRepository: RecentlyPlayedRepository = DataStoreRecentlyPlayedRepository(appContext)

    val preferences: UserPreferences = DataStoreUserPreferences(appContext)

    val catalogSession: CatalogSession = CatalogSession()

    val networkMonitor: NetworkMonitor = NetworkMonitor(appContext)

    val youTubeLauncher: YouTubeLauncher = YouTubeLauncher(appContext)

    val player: SonoraPlayer = SonoraPlayer(appContext)

    val versionName: String = BuildConfig.VERSION_NAME

    fun youTubeApiKeyConfigured(): Boolean = youTubeKey.value.availability.isAvailable
}
