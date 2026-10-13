package dev.sonora.app.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import android.provider.Settings

/**
 * Motion in Sonora is subtle: short fades and small slides. When the user asks
 * the system (or the in-app setting) to reduce motion, durations collapse to
 * zero so screens change instantly while remaining functional.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

object MotionDurations {
    const val SHORT = 140
    const val MEDIUM = 220
    const val LONG = 320
}

/** Combines the in-app preference with the platform animator scale. */
@Composable
fun rememberReducedMotion(preferenceEnabled: Boolean): Boolean {
    if (LocalInspectionMode.current) return preferenceEnabled
    val context = LocalContext.current
    val systemScale = runCatching {
        ValueAnimator.areAnimatorsEnabled() &&
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
    }.getOrDefault(false)
    return preferenceEnabled || systemScale
}

/** Zero-duration spec when motion is reduced. */
fun <T> motionSpec(reduced: Boolean, durationMillis: Int = MotionDurations.MEDIUM): FiniteAnimationSpec<T> =
    if (reduced) snap() else tween(durationMillis = durationMillis)
