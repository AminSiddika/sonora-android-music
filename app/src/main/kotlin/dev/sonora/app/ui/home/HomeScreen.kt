package dev.sonora.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sonora.app.R
import dev.sonora.app.ui.LoadStatus
import dev.sonora.app.ui.Provider
import dev.sonora.app.ui.components.AttributionNote
import dev.sonora.app.ui.components.DisclosureCard
import dev.sonora.app.ui.components.EmptyHint
import dev.sonora.app.ui.components.ErrorState
import dev.sonora.app.ui.components.LoadingList
import dev.sonora.app.ui.components.OfflineBanner
import dev.sonora.app.ui.components.SectionHeader
import dev.sonora.app.ui.components.TrackCard
import dev.sonora.app.ui.components.TrackRow
import dev.sonora.app.ui.presentation
import dev.sonora.core.domain.DiscoveryShelves
import dev.sonora.core.model.Track

/**
 * Discovery screen: a rotating curated shelf, quick genre shortcuts, and the
 * user's own favourites and history so the screen is still useful offline.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    favoriteIds: Set<Long>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onOpenTrack: (Track) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onQuickSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(key = "hero") {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.home_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isOffline) {
            item(key = "offline") { OfflineBanner(onRetry = viewModel::refresh) }
        }

        item(key = "shelf-header") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(
                    title = stringResource(R.string.home_shelf_title),
                    subtitle = state.shelf.title,
                    actionLabel = stringResource(R.string.action_refresh),
                    onAction = viewModel::refresh,
                )
            }
        }

        when (state.shelfStatus) {
            LoadStatus.LOADING, LoadStatus.IDLE -> item(key = "shelf-loading") { LoadingList(rows = 3) }

            LoadStatus.ERROR -> item(key = "shelf-error") {
                state.shelfError?.let { error ->
                    ErrorState(
                        presentation = error.presentation(Provider.CATALOGUE),
                        onRetry = { viewModel.loadShelf(forceRefresh = true) },
                    )
                }
            }

            LoadStatus.EMPTY -> item(key = "shelf-empty") {
                EmptyHint(
                    icon = Icons.Filled.Refresh,
                    text = stringResource(R.string.search_empty_title),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            LoadStatus.CONTENT -> item(key = "shelf-content") {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    state.shelfTracks.take(6).forEach { track ->
                        TrackRow(
                            track = track,
                            isFavorite = track.id in favoriteIds,
                            isCurrentTrack = track.id == currentTrackId,
                            isPlaying = isPlaying,
                            onOpen = { onOpenTrack(track) },
                            onPlay = { onPlayTrack(track, state.shelfTracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                        )
                    }
                }
            }
        }

        item(key = "browse") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionHeader(title = stringResource(R.string.home_browse_title))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscoveryShelves.ALL.forEach { shelf ->
                        AssistChip(
                            onClick = { onQuickSearch(shelf.query) },
                            label = { Text(shelf.title) },
                        )
                    }
                    DiscoveryShelves.QUICK_SEARCHES.forEach { term ->
                        FilterChip(
                            selected = false,
                            onClick = { onQuickSearch(term) },
                            label = { Text(term) },
                        )
                    }
                }
            }
        }

        item(key = "favorites-header") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(title = stringResource(R.string.home_favorites_title))
            }
        }

        item(key = "favorites-strip") {
            if (state.favorites.isEmpty()) {
                EmptyHint(
                    icon = Icons.Filled.Refresh,
                    text = stringResource(R.string.home_empty_favorites),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.favorites, key = { it.id }) { track ->
                        TrackCard(track = track, onOpen = { onOpenTrack(track) })
                    }
                }
            }
        }

        item(key = "recent-header") {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader(title = stringResource(R.string.home_recent_title))
            }
        }

        item(key = "recent-strip") {
            if (state.recentlyPlayed.isEmpty()) {
                EmptyHint(
                    icon = Icons.Filled.Refresh,
                    text = stringResource(R.string.home_empty_recent),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.recentlyPlayed, key = { it.id }) { track ->
                        TrackCard(track = track, onOpen = { onOpenTrack(track) })
                    }
                }
            }
        }

        item(key = "disclosures") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DisclosureCard(
                    title = stringResource(R.string.attribution_itunes),
                    message = stringResource(R.string.disclosure_preview),
                )
                AttributionNote(text = stringResource(R.string.attribution_itunes))
            }
        }
    }
}
