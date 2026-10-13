package dev.sonora.app.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
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
import dev.sonora.app.ui.components.EmptyState
import dev.sonora.app.ui.components.LoadingList
import dev.sonora.app.ui.components.TrackRow
import dev.sonora.core.model.Track

/** The user's locally persisted favourites, all stored in DataStore. */
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    currentTrackId: Long?,
    isPlaying: Boolean,
    onOpenTrack: (Track) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onRemove: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingList(rows = 5, modifier = modifier)

        state.favorites.isEmpty() -> EmptyState(
            title = stringResource(R.string.favorites_empty_title),
            message = stringResource(R.string.favorites_empty_message),
            icon = Icons.Filled.Favorite,
            modifier = modifier,
        )

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(R.string.favorites_count, state.favorites.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val playable = state.favorites.filter { it.hasPreview }
                    if (playable.isNotEmpty()) {
                        Button(onClick = { onPlayTrack(playable.first(), playable) }) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 6.dp),
                            )
                            Text(text = stringResource(R.string.action_play_all))
                        }
                    }
                }
            }

            items(items = state.favorites, key = { it.id }) { track ->
                TrackRow(
                    track = track,
                    isFavorite = true,
                    isCurrentTrack = track.id == currentTrackId,
                    isPlaying = isPlaying,
                    onOpen = { onOpenTrack(track) },
                    onPlay = { onPlayTrack(track, state.favorites) },
                    onToggleFavorite = { onRemove(track) },
                )
            }
        }
    }
}
