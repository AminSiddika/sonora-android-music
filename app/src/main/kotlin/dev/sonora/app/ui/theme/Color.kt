package dev.sonora.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Sonora's palette: a deep violet core (the "night stage") warmed up by an
 * apricot accent (the "spotlight") and a mint signal colour for playback
 * affordances. Contrast ratios stay above 4.5:1 for body text in both themes.
 */

internal val VioletCore = Color(0xFF5B45E0)
internal val VioletCoreBright = Color(0xFF8C7BFF)
internal val VioletDeep = Color(0xFF2A1F5C)
internal val VioletMist = Color(0xFFE7E2FF)

internal val Apricot = Color(0xFFCE5A22)
internal val ApricotBright = Color(0xFFFFB27A)
internal val ApricotDeep = Color(0xFF55290B)
internal val ApricotMist = Color(0xFFFFE0CC)

internal val Mint = Color(0xFF1E7F6B)
internal val MintBright = Color(0xFF7FE3CB)
internal val MintDeep = Color(0xFF06342B)
internal val MintMist = Color(0xFFC7F1E5)

internal val Ink = Color(0xFF17142A)
internal val InkMuted = Color(0xFF5B5673)
internal val PaperWhite = Color(0xFFFCFAFF)
internal val PaperSurface = Color(0xFFFFFFFF)
internal val PaperSurfaceVariant = Color(0xFFEDE9F7)
internal val PaperOutline = Color(0xFF7C7691)

internal val NightStage = Color(0xFF100D1C)
internal val NightSurface = Color(0xFF1A1628)
internal val NightSurfaceVariant = Color(0xFF2A2440)
internal val NightOutline = Color(0xFF948EAD)

internal val ErrorRed = Color(0xFFB3261E)
internal val ErrorRedDark = Color(0xFFFFB4AB)
internal val ErrorContainerLight = Color(0xFFF9DEDC)
internal val ErrorContainerDark = Color(0xFF8C1D18)
internal val OnErrorContainerLight = Color(0xFF410E0B)
internal val OnErrorContainerDark = Color(0xFFFFDAD6)

internal val SonoraLightColors = lightColorScheme(
    primary = VioletCore,
    onPrimary = Color.White,
    primaryContainer = VioletMist,
    onPrimaryContainer = VioletDeep,
    inversePrimary = VioletCoreBright,
    secondary = Apricot,
    onSecondary = Color.White,
    secondaryContainer = ApricotMist,
    onSecondaryContainer = ApricotDeep,
    tertiary = Mint,
    onTertiary = Color.White,
    tertiaryContainer = MintMist,
    onTertiaryContainer = MintDeep,
    background = PaperWhite,
    onBackground = Ink,
    surface = PaperSurface,
    onSurface = Ink,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = InkMuted,
    surfaceTint = VioletCore,
    inverseSurface = NightSurfaceVariant,
    inverseOnSurface = PaperWhite,
    outline = PaperOutline,
    outlineVariant = VioletMist,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

internal val SonoraDarkColors = darkColorScheme(
    primary = VioletCoreBright,
    onPrimary = VioletDeep,
    primaryContainer = VioletDeep,
    onPrimaryContainer = VioletMist,
    inversePrimary = VioletCore,
    secondary = ApricotBright,
    onSecondary = ApricotDeep,
    secondaryContainer = ApricotDeep,
    onSecondaryContainer = ApricotMist,
    tertiary = MintBright,
    onTertiary = MintDeep,
    tertiaryContainer = MintDeep,
    onTertiaryContainer = MintMist,
    background = NightStage,
    onBackground = Color(0xFFEBE6F7),
    surface = NightSurface,
    onSurface = Color(0xFFEBE6F7),
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFC7C1DB),
    surfaceTint = VioletCoreBright,
    inverseSurface = PaperSurfaceVariant,
    inverseOnSurface = Ink,
    outline = NightOutline,
    outlineVariant = Color(0xFF3C3557),
    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)
