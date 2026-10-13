package dev.sonora.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sonora.app.R
import dev.sonora.app.ui.LoadStatus
import dev.sonora.app.ui.Provider
import dev.sonora.app.ui.components.DisclosureCard
import dev.sonora.app.ui.components.EmptyState
import dev.sonora.app.ui.components.ErrorState
import dev.sonora.app.ui.components.LoadingList
import dev.sonora.app.ui.components.OfflineBanner
import dev.sonora.app.ui.components.SectionHeader
import dev.sonora.app.ui.components.TrackRow
import dev.sonora.app.ui.components.VideoRow
import dev.sonora.app.ui.presentation
import dev.sonora.core.domain.DiscoveryShelves
import dev.sonora.core.model.SearchProvider
import dev.sonora.core.model.SonoraError
import dev.sonora.core.model.Track
import dev.sonora.core.model.VideoItem
import dev.sonora.core.model.YouTubeAvailability

/**
 * Search screen with an explicit provider switch: the iTunes catalogue (no key
 * required, 30 second previews) or YouTube (optional, opens the official app).
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    favoriteIds: Set<Long>,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onOpenTrack: (Track) -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onWatchVideo: (VideoItem) -> Unit,
    onOpenYouTubeGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                placeholder = { Text(text = stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.action_clear),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        viewModel.submit()
                    },
                ),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.search_source_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilterChip(
                    selected = state.provider == SearchProvider.ITUNES,
                    onClick = { viewModel.onProviderSelected(SearchProvider.ITUNES) },
                    label = { Text(stringResource(R.string.search_provider_itunes)) },
                )
                FilterChip(
                    selected = state.provider == SearchProvider.YOUTUBE,
                    onClick = { viewModel.onProviderSelected(SearchProvider.YOUTUBE) },
                    label = { Text(stringResource(R.string.search_provider_youtube)) },
                )
            }
        }

        if (state.isOffline) {
            OfflineBanner(onRetry = { viewModel.retry() })
        }

        val provider = if (state.provider == SearchProvider.YOUTUBE) Provider.YOUTUBE else Provider.CATALOGUE

        val unavailableError = when (state.capabilities.youTube) {
            YouTubeAvailability.Available -> null
            YouTubeAvailability.PlaceholderKey -> SonoraError.InvalidApiKey
            YouTubeAvailability.MissingKey -> SonoraError.MissingApiKey
        }

        when {
            state.provider == SearchProvider.YOUTUBE && unavailableError != null -> {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ErrorState(
                        presentation = unavailableError.presentation(Provider.YOUTUBE),
                        onOpenYouTubeGuide = onOpenYouTubeGuide,
                        onSwitchToCatalogue = viewModel::switchToCatalogue,
                    )
                }
            }

            state.status == LoadStatus.IDLE -> IdleSearchContent(onQuickSearch = viewModel::useQuickSearch)

            state.status == LoadStatus.LOADING -> LoadingList(rows = 6)

            state.status == LoadStatus.ERROR -> state.error?.let { error ->
                ErrorState(
                    presentation = error.presentation(provider),
                    onRetry = viewModel::retry,
                    onOpenYouTubeGuide = onOpenYouTubeGuide,
                    onSwitchToCatalogue = viewModel::switchToCatalogue,
                )
            }

            state.status == LoadStatus.EMPTY -> EmptyState(
                title = stringResource(R.string.search_empty_title),
                message = stringResource(R.string.search_empty_message, state.lastSubmittedQuery),
                icon = Icons.Filled.Search,
                actionLabel = stringResource(R.string.action_clear),
                onAction = viewModel::clearQuery,
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item(key = "result-count") {
                    val resultCount = if (provider == Provider.YOUTUBE) state.videos.size else state.tracks.size
                    Text(
                        text = pluralStringResource(R.plurals.search_results_count, resultCount, resultCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }

                if (state.provider == SearchProvider.ITUNES) {
                    items(items = state.tracks, key = { "track-${it.id}" }) { track ->
                        TrackRow(
                            track = track,
                            isFavorite = track.id in favoriteIds,
                            isCurrentTrack = track.id == currentTrackId,
                            isPlaying = isPlaying,
                            onOpen = { onOpenTrack(track) },
                            onPlay = { onPlayTrack(track, state.tracks) },
                            onToggleFavorite = { onToggleFavorite(track) },
                        )
                    }
                } else {
                    items(items = state.videos, key = { "video-${it.videoId}" }) { video ->
                        VideoRow(
                            video = video,
                            onOpenDetails = { onOpenVideo(video) },
                            onWatchOnYouTube = { onWatchVideo(video) },
                        )
                    }
                    item(key = "youtube-disclosure") {
                        Column(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DisclosureCard(
                                title = stringResource(R.string.attribution_youtube),
                                message = stringResource(R.string.disclosure_youtube),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleSearchContent(onQuickSearch: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionHeader(
            title = stringResource(R.string.search_idle_title),
            subtitle = stringResource(R.string.search_idle_message),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.search_quick_searches),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscoveryShelves.QUICK_SEARCHES.forEach { term ->
                    FilterChip(
                        selected = false,
                        onClick = { onQuickSearch(term) },
                        label = { Text(term) },
                    )
                }
            }
        }
        DisclosureCard(
            title = stringResource(R.string.attribution_itunes),
            message = stringResource(R.string.disclosure_preview),
        )
    }
}
