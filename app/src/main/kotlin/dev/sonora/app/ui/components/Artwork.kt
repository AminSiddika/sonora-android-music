package dev.sonora.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import dev.sonora.app.R
import dev.sonora.app.ui.theme.LocalReducedMotion
import dev.sonora.app.ui.theme.MotionDurations
import dev.sonora.app.ui.theme.motionSpec

/**
 * Deterministic gradient pairs used while artwork loads or when a track has no
 * artwork at all. Deriving the pair from the track id keeps the UI stable and
 * gives every track a recognisable colour without shipping image assets.
 */
object ArtworkPalette {

    private val pairs: List<Pair<Color, Color>> = listOf(
        Color(0xFF5B45E0) to Color(0xFFB27AFF),
        Color(0xFF1E7F6B) to Color(0xFF7FE3CB),
        Color(0xFFCE5A22) to Color(0xFFFFB27A),
        Color(0xFF3B4CCA) to Color(0xFF7FA9FF),
        Color(0xFF8E2DA8) to Color(0xFFFF9AD5),
        Color(0xFF16697A) to Color(0xFF82C0CC),
    )

    fun pairFor(seed: Long): Pair<Color, Color> {
        val index = ((seed % pairs.size) + pairs.size) % pairs.size
        return pairs[index.toInt()]
    }

    fun brushFor(seed: Long): Brush {
        val (start, end) = pairFor(seed)
        return Brush.linearGradient(listOf(start, end))
    }

    val size: Int get() = pairs.size
}

/**
 * Album artwork with a graceful fallback: a brand gradient with a note icon.
 * The image fades in only when motion is allowed.
 */
@Composable
fun ArtworkImage(
    url: String?,
    contentDescription: String?,
    seed: Long,
    modifier: Modifier = Modifier,
    size: Dp? = 56.dp,
    shape: Shape = MaterialTheme.shapes.small,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val brush = remember(seed) { ArtworkPalette.brushFor(seed) }
    val reducedMotion = LocalReducedMotion.current
    val sizeModifier = if (size != null) Modifier.size(size) else Modifier

    Box(
        modifier = modifier
            .then(sizeModifier)
            .clip(shape)
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            ArtworkImageContent(
                url = url,
                contentDescription = contentDescription,
                contentScale = contentScale,
                reducedMotion = reducedMotion,
            )
        } else {
            FallbackNote()
        }
    }
}

@Composable
private fun BoxScope.ArtworkImageContent(
    url: String,
    contentDescription: String?,
    contentScale: ContentScale,
    reducedMotion: Boolean,
) {
    var loaded by remember(url) { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = motionSpec(reducedMotion, MotionDurations.MEDIUM),
        label = "artworkAlpha",
    )

    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha },
        onSuccess = { loaded = true },
    )
    if (!loaded) {
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun FallbackNote() {
    Icon(
        imageVector = Icons.Filled.MusicNote,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.85f),
        modifier = Modifier.size(24.dp),
    )
}

/** Convenience wrapper that derives the accessibility label from a title. */
@Composable
fun artworkDescription(title: String): String = stringResource(R.string.cd_artwork, title)
