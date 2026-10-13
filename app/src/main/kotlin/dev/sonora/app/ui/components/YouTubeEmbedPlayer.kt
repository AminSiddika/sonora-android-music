package dev.sonora.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * In-app YouTube playback through Google's **official IFrame Player API**, the
 * same embedded player a browser would run.
 *
 * Why this and not a stream extractor: Sonora deliberately does not resolve or
 * re-stream media URLs, does not download videos, does not touch ads and does not
 * play YouTube audio in the background. The IFrame player keeps every one of
 * those guarantees — YouTube serves its own content, ads and controls — while
 * still playing inside the app.
 *
 * Behaviour notes:
 *  * the video is *cued*, not autoplayed, so the official player shows its own
 *    play button and WebView autoplay policies stay satisfied,
 *  * the view forwards lifecycle events to the IFrame player, which is why audio
 *    stops when the app goes to the background,
 *  * [YouTubePlayerView.release] runs in `onRelease` so no WebView leaks.
 *
 * No API key is involved: this is the IFrame player, not the Data API.
 */
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    modifier: Modifier = Modifier,
    onError: (PlayerConstants.PlayerError) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            YouTubePlayerView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addYouTubePlayerListener(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.cueVideo(videoId, 0f)
                        }

                        override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            currentOnError(error)
                        }
                    },
                )
                lifecycleOwner.lifecycle.addObserver(this)
            }
        },
        onRelease = { view ->
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        },
    )
}
