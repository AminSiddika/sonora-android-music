package dev.sonora.app.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import dev.sonora.core.domain.ExternalLinks

/**
 * Opens YouTube content the compliant way.
 *
 * Sonora never resolves stream URLs and never embeds ads or background audio:
 * YouTube results always continue in the official YouTube application when it
 * is installed, and otherwise in the user's browser. Both routes are full
 * official YouTube experiences with their own ads, attribution and policies.
 */
class YouTubeLauncher(private val context: Context) {

    fun openVideo(videoId: String): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, ExternalLinks.youTubeAppUri(videoId).toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (context.packageManager.resolveActivity(appIntent, 0) != null) {
            if (startSafely(appIntent)) return true
        }
        return openUrl(ExternalLinks.youTubeWatchUrl(videoId))
    }

    fun openChannel(channelId: String): Boolean = openUrl(ExternalLinks.youTubeChannelUrl(channelId))

    fun openUrl(url: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(intent)
    }

    fun openPlayStoreListing(): Boolean =
        openUrl("market://details?id=com.google.android.youtube")

    private fun startSafely(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (notFound: ActivityNotFoundException) {
        false
    }
}
