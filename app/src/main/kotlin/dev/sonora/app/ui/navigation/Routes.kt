package dev.sonora.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import dev.sonora.app.R
import kotlinx.serialization.Serializable

/*
 * Type safe navigation routes (Navigation Compose 2.8+). Details screens take
 * ids only: the payload is resolved from the in-memory catalogue session or the
 * local favourites/history stores, which keeps deep links and process restores
 * predictable.
 */

@Serializable
data object HomeRoute

@Serializable
data class SearchRoute(val initialQuery: String? = null)

@Serializable
data object FavoritesRoute

@Serializable
data object SettingsRoute

@Serializable
data class TrackDetailsRoute(val trackId: Long)

@Serializable
data class VideoDetailsRoute(val videoId: String)

/** Bottom bar / navigation rail entries. */
enum class SonoraTab(
    val route: Any,
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(HomeRoute, R.string.nav_home, Icons.Filled.Home),
    SEARCH(SearchRoute(), R.string.nav_search, Icons.Filled.Search),
    FAVORITES(FavoritesRoute, R.string.nav_favorites, Icons.Filled.Favorite),
    SETTINGS(SettingsRoute, R.string.nav_settings, Icons.Filled.Settings),
}
