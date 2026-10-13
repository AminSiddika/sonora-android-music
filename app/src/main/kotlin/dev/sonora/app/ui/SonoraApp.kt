package dev.sonora.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.sonora.app.di.SonoraGraph
import dev.sonora.app.ui.components.MiniPlayer
import dev.sonora.app.ui.details.TrackDetailsScreen
import dev.sonora.app.ui.details.TrackDetailsViewModel
import dev.sonora.app.ui.details.VideoDetailsScreen
import dev.sonora.app.ui.details.VideoDetailsViewModel
import dev.sonora.app.ui.favorites.FavoritesScreen
import dev.sonora.app.ui.favorites.FavoritesViewModel
import dev.sonora.app.ui.home.HomeScreen
import dev.sonora.app.ui.home.HomeViewModel
import dev.sonora.app.ui.navigation.FavoritesRoute
import dev.sonora.app.ui.navigation.HomeRoute
import dev.sonora.app.ui.navigation.SearchRoute
import dev.sonora.app.ui.navigation.SettingsRoute
import dev.sonora.app.ui.navigation.SonoraTab
import dev.sonora.app.ui.navigation.TrackDetailsRoute
import dev.sonora.app.ui.navigation.VideoDetailsRoute
import dev.sonora.app.ui.player.PlayerScreen
import dev.sonora.app.ui.player.PlayerViewModel
import dev.sonora.app.ui.search.SearchScreen
import dev.sonora.app.ui.search.SearchViewModel
import dev.sonora.app.ui.settings.SettingsScreen
import dev.sonora.app.ui.settings.SettingsViewModel
import dev.sonora.app.ui.theme.LocalReducedMotion
import dev.sonora.app.ui.theme.MotionDurations
import dev.sonora.app.ui.theme.motionSpec
import dev.sonora.core.model.VideoItem

/**
 * Root of the Sonora UI: navigation shell, persistent mini player and the full
 * player overlay.
 *
 * Responsive behaviour: phones get a bottom navigation bar, wider screens and
 * landscape get a navigation rail so the content keeps a comfortable width.
 */
@Composable
fun SonoraApp(graph: SonoraGraph) {
    val navController = rememberNavController()
    val playerViewModel = sonoraViewModel(graph) { container ->
        PlayerViewModel(
            player = container.player,
            favoritesRepository = container.favoritesRepository,
            recentlyPlayedRepository = container.recentlyPlayedRepository,
            catalogSession = container.catalogSession,
        )
    }
    val playerState by playerViewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by playerViewModel.favoriteIds.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(SonoraTab.HOME) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    val reducedMotion = LocalReducedMotion.current

    val openUrl: (String) -> Unit = remember(graph) { { url -> graph.youTubeLauncher.openUrl(url) } }
    val watchVideo: (VideoItem) -> Unit = remember(graph) { { video -> graph.youTubeLauncher.openVideo(video.videoId) } }

    fun selectTab(tab: SonoraTab) {
        selectedTab = tab
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            val miniPlayer: @Composable () -> Unit = {
                AnimatedVisibility(
                    visible = playerState.hasTrack && !showPlayer,
                    enter = slideInVertically(animationSpec = motionSpec(reducedMotion, MotionDurations.MEDIUM)),
                    exit = slideOutVertically(animationSpec = motionSpec(reducedMotion, MotionDurations.MEDIUM)),
                ) {
                    MiniPlayer(
                        state = playerState,
                        onTogglePlay = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::next,
                        onExpand = { showPlayer = true },
                        onClose = playerViewModel::stop,
                    )
                }
            }

            val navContent: @Composable (Modifier) -> Unit = { navModifier ->
                NavHost(
                    navController = navController,
                    startDestination = HomeRoute,
                    modifier = navModifier,
                    enterTransition = { fadeIn(animationSpec = motionSpec(reducedMotion, MotionDurations.MEDIUM)) },
                    exitTransition = { fadeOut(animationSpec = motionSpec(reducedMotion, MotionDurations.SHORT)) },
                    popEnterTransition = { fadeIn(animationSpec = motionSpec(reducedMotion, MotionDurations.MEDIUM)) },
                    popExitTransition = { fadeOut(animationSpec = motionSpec(reducedMotion, MotionDurations.SHORT)) },
                ) {
                    composable<HomeRoute> {
                        val homeViewModel = sonoraViewModel(graph) { container ->
                            HomeViewModel(
                                searchRepository = container.searchRepository,
                                favoritesRepository = container.favoritesRepository,
                                recentlyPlayedRepository = container.recentlyPlayedRepository,
                                catalogSession = container.catalogSession,
                                isOnline = container.networkMonitor.isOnline,
                            )
                        }
                        HomeScreen(
                            viewModel = homeViewModel,
                            favoriteIds = favoriteIds,
                            currentTrackId = playerState.current?.id,
                            isPlaying = playerState.isPlaying,
                            onOpenTrack = { track -> navController.navigate(TrackDetailsRoute(track.id)) },
                            onPlayTrack = playerViewModel::play,
                            onToggleFavorite = playerViewModel::toggleFavorite,
                            onQuickSearch = { term ->
                                selectedTab = SonoraTab.SEARCH
                                navController.navigate(SearchRoute(term)) { launchSingleTop = true }
                            },
                        )
                    }

                    composable<SearchRoute> { entry ->
                        val route = entry.toRoute<SearchRoute>()
                        val searchViewModel = sonoraViewModel(graph) { container ->
                            SearchViewModel(
                                searchRepository = container.searchRepository,
                                favoritesRepository = container.favoritesRepository,
                                preferences = container.preferences,
                                catalogSession = container.catalogSession,
                                isOnline = container.networkMonitor.isOnline,
                                youTubeKey = container.youTubeKey,
                            )
                        }
                        var appliedInitialQuery by rememberSaveable { mutableStateOf(false) }
                        if (!appliedInitialQuery && !route.initialQuery.isNullOrBlank()) {
                            appliedInitialQuery = true
                            searchViewModel.useQuickSearch(route.initialQuery)
                        }
                        SearchScreen(
                            viewModel = searchViewModel,
                            favoriteIds = favoriteIds,
                            currentTrackId = playerState.current?.id,
                            isPlaying = playerState.isPlaying,
                            onOpenTrack = { track -> navController.navigate(TrackDetailsRoute(track.id)) },
                            onOpenVideo = { video -> navController.navigate(VideoDetailsRoute(video.videoId)) },
                            onPlayTrack = playerViewModel::play,
                            onToggleFavorite = playerViewModel::toggleFavorite,
                            onWatchVideo = watchVideo,
                            onOpenYouTubeGuide = { openUrl(dev.sonora.core.domain.ExternalLinks.YOUTUBE_API_KEY_GUIDE) },
                        )
                    }

                    composable<FavoritesRoute> {
                        val favoritesViewModel = sonoraViewModel(graph) { container ->
                            FavoritesViewModel(container.favoritesRepository)
                        }
                        FavoritesScreen(
                            viewModel = favoritesViewModel,
                            currentTrackId = playerState.current?.id,
                            isPlaying = playerState.isPlaying,
                            onOpenTrack = { track -> navController.navigate(TrackDetailsRoute(track.id)) },
                            onPlayTrack = playerViewModel::play,
                            onRemove = { track -> favoritesViewModel.remove(track) },
                        )
                    }

                    composable<SettingsRoute> {
                        val settingsViewModel = sonoraViewModel(graph) { container ->
                            SettingsViewModel(
                                preferences = container.preferences,
                                favoritesRepository = container.favoritesRepository,
                                recentlyPlayedRepository = container.recentlyPlayedRepository,
                                youTubeKeyStore = container.youTubeKeyStore,
                                youTubeKey = container.youTubeKey,
                                versionName = container.versionName,
                            )
                        }
                        SettingsScreen(viewModel = settingsViewModel, onOpenUrl = openUrl)
                    }

                    composable<TrackDetailsRoute> { entry ->
                        val route = entry.toRoute<TrackDetailsRoute>()
                        val detailsViewModel = sonoraViewModel(graph, key = "track-${route.trackId}") { container ->
                            TrackDetailsViewModel(
                                trackId = route.trackId,
                                catalogSession = container.catalogSession,
                                favoritesRepository = container.favoritesRepository,
                                recentlyPlayedRepository = container.recentlyPlayedRepository,
                            )
                        }
                        TrackDetailsScreen(
                            viewModel = detailsViewModel,
                            isCurrentTrack = playerState.current?.id == route.trackId,
                            isPlaying = playerState.isPlaying,
                            isBuffering = playerState.isBuffering,
                            onBack = { navController.popBackStack() },
                            onPlay = { track -> playerViewModel.play(track) },
                            onToggleFavorite = { playerViewModel.toggleFavorite(it) },
                            onOpenStore = openUrl,
                            onSearchAgain = {
                                navController.popBackStack()
                                selectedTab = SonoraTab.SEARCH
                                navController.navigate(SearchRoute()) { launchSingleTop = true }
                            },
                        )
                    }

                    composable<VideoDetailsRoute> { entry ->
                        val route = entry.toRoute<VideoDetailsRoute>()
                        val videoViewModel = sonoraViewModel(graph, key = "video-${route.videoId}") { container ->
                            VideoDetailsViewModel(videoId = route.videoId, catalogSession = container.catalogSession)
                        }
                        VideoDetailsScreen(
                            viewModel = videoViewModel,
                            onBack = { navController.popBackStack() },
                            onWatchOnYouTube = { videoId -> graph.youTubeLauncher.openVideo(videoId) },
                            onOpenChannel = { channelId -> graph.youTubeLauncher.openChannel(channelId) },
                            onOpenUrl = openUrl,
                            onSearchAgain = {
                                navController.popBackStack()
                                selectedTab = SonoraTab.SEARCH
                                navController.navigate(SearchRoute()) { launchSingleTop = true }
                            },
                        )
                    }
                }
            }

            if (useNavigationRail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    SonoraNavigationRail(selected = selectedTab, onSelect = ::selectTab)
                    Column(modifier = Modifier.fillMaxHeight()) {
                        Box(modifier = Modifier.weight(1f)) {
                            navContent(Modifier.fillMaxSize())
                        }
                        miniPlayer()
                    }
                }
            } else {
                Scaffold(
                    bottomBar = {
                        Column {
                            miniPlayer()
                            SonoraNavigationBar(selected = selectedTab, onSelect = ::selectTab)
                        }
                    },
                ) { innerPadding ->
                    navContent(Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(
                animationSpec = motionSpec(reducedMotion, MotionDurations.LONG),
                initialOffsetY = { fullHeight -> fullHeight },
            ),
            exit = slideOutVertically(
                animationSpec = motionSpec(reducedMotion, MotionDurations.MEDIUM),
                targetOffsetY = { fullHeight -> fullHeight },
            ),
        ) {
            PlayerScreen(
                state = playerState,
                isFavorite = playerState.current?.id in favoriteIds,
                onClose = { showPlayer = false },
                onTogglePlay = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onPrevious = playerViewModel::previous,
                onSeekToProgress = playerViewModel::seekToProgress,
                onToggleShuffle = playerViewModel::toggleShuffle,
                onCycleRepeat = playerViewModel::cycleRepeatMode,
                onToggleFavorite = playerViewModel::toggleFavoriteOfCurrent,
                onOpenStore = openUrl,
            )
        }
    }

        BackHandler(enabled = showPlayer) { showPlayer = false }
}

@Composable
private fun SonoraNavigationBar(
    selected: SonoraTab,
    onSelect: (SonoraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        SonoraTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}

@Composable
private fun SonoraNavigationRail(
    selected: SonoraTab,
    onSelect: (SonoraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SonoraTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(tab.labelRes)) },
            )
        }
    }
}
