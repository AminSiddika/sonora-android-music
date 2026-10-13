package dev.sonora.app.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.sonora.app.R
import dev.sonora.app.ui.components.AttributionNote
import dev.sonora.app.ui.components.DisclosureCard
import dev.sonora.app.ui.components.MissingContentState
import dev.sonora.app.ui.components.WatchOnYouTubeButton
import dev.sonora.app.ui.components.YouTubeEmbedPlayer

/**
 * YouTube result details.
 *
 * Playback policy: Sonora never resolves stream URLs, downloads media or plays
 * YouTube audio in the background. The only playback route offered is the
 * official YouTube application (or the browser when it is not installed).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailsScreen(
    viewModel: VideoDetailsViewModel,
    onBack: () -> Unit,
    onWatchOnYouTube: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onSearchAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val video = state.video

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.video_details_title)) },
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
        if (video == null) {
            MissingContentState(
                title = stringResource(R.string.video_missing_title),
                message = stringResource(R.string.video_missing_message),
                modifier = Modifier.padding(padding),
                actionLabel = stringResource(R.string.nav_search),
                onAction = onSearchAgain,
            )
            return@Scaffold
        }

        var showEmbeddedPlayer by remember(video.videoId) { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = stringResource(R.string.cd_youtube_video),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium),
            )

            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.video_details_channel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = video.channelTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            video.publishedAt?.let { published ->
                Text(
                    text = "${stringResource(R.string.video_details_published)}: ${published.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            WatchOnYouTubeButton(onClick = { onWatchOnYouTube(video.videoId) })

            if (showEmbeddedPlayer) {
                YouTubeEmbedPlayer(
                    videoId = video.videoId,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.medium),
                )
                Text(
                    text = stringResource(R.string.video_details_embedded_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = { showEmbeddedPlayer = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.video_details_play_here),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            video.channelId?.let { channelId ->
                OutlinedButton(
                    onClick = { onOpenChannel(channelId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.video_details_open_channel))
                }
            }

            video.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DisclosureCard(
                title = stringResource(R.string.video_details_playback_title),
                message = stringResource(R.string.video_details_playback_message),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onOpenUrl(dev.sonora.core.domain.ExternalLinks.YOUTUBE_TERMS_OF_SERVICE) }) {
                    Text(text = stringResource(R.string.youtube_terms_label), style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = { onOpenUrl(dev.sonora.core.domain.ExternalLinks.GOOGLE_PRIVACY_POLICY) }) {
                    Text(text = stringResource(R.string.youtube_privacy_label), style = MaterialTheme.typography.labelMedium)
                }
            }

            AttributionNote(text = stringResource(R.string.attribution_youtube))
        }
    }
}
