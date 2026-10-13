package dev.sonora.app.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sonora.app.R
import dev.sonora.app.ui.components.ArtworkImage
import dev.sonora.app.ui.components.AttributionNote
import dev.sonora.app.ui.components.DisclosureCard
import dev.sonora.app.ui.components.ExplicitBadge
import dev.sonora.app.ui.components.MissingContentState
import dev.sonora.app.ui.components.NoPreviewBadge
import dev.sonora.app.ui.components.PreviewBadge
import dev.sonora.core.domain.DurationFormat
import dev.sonora.core.model.Track

/**
 * Catalogue track details. The primary action always plays the 30 second
 * preview Apple provides; when there is none the screen explains why instead
 * of hiding the track.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsScreen(
    viewModel: TrackDetailsViewModel,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onBack: () -> Unit,
    onPlay: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onOpenStore: (String) -> Unit,
    onSearchAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val track = state.track

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.track_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_previous),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (track == null) {
            MissingContentState(
                title = stringResource(R.string.track_missing_title),
                message = stringResource(R.string.track_missing_message),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.nav_search),
                onAction = onSearchAgain,
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtworkImage(
                url = track.displayArtworkUrl,
                contentDescription = stringResource(R.string.cd_artwork, track.title),
                seed = track.id,
                modifier = Modifier.fillMaxWidth(0.78f).aspectRatio(1f),
                size = null,
                shape = MaterialTheme.shapes.large,
            )

            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (track.hasPreview) {
                    PreviewBadge(durationMillis = track.durationMillis)
                } else {
                    NoPreviewBadge()
                }
                if (track.explicit) {
                    ExplicitBadge()
                }
            }

            if (track.hasPreview) {
                Button(
                    onClick = { onPlay(track) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrentTrack && (isPlaying || isBuffering)) {
                            stringResource(R.string.player_now_playing)
                        } else {
                            stringResource(R.string.track_details_play)
                        },
                    )
                }
            } else {
                DisclosureCard(
                    title = stringResource(R.string.preview_unavailable),
                    message = stringResource(R.string.preview_unavailable_detail),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.isFavorite) {
                            stringResource(R.string.cd_favorite_remove, track.title)
                        } else {
                            stringResource(R.string.cd_favorite_add, track.title)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val storeUrl = track.storeUrl
                if (storeUrl != null) {
                    OutlinedButton(
                        onClick = { onOpenStore(storeUrl) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_open_store),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            MetadataRow(label = stringResource(R.string.track_details_album), value = track.album)
            MetadataRow(label = stringResource(R.string.track_details_genre), value = track.genre)
            MetadataRow(label = stringResource(R.string.track_details_released), value = DurationFormat.year(track.releaseDate))
            MetadataRow(label = stringResource(R.string.track_details_length), value = DurationFormat.format(track.durationMillis))
            MetadataRow(label = stringResource(R.string.track_details_source), value = stringResource(R.string.attribution_itunes))

            DisclosureCard(
                title = stringResource(R.string.preview_badge),
                message = stringResource(R.string.disclosure_preview),
            )
            AttributionNote(text = stringResource(R.string.attribution_itunes))
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
