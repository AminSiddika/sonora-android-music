package dev.sonora.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.sonora.app.R
import dev.sonora.core.model.SonoraError

/** What a user can do after a failure. */
enum class RecoveryAction {
    RETRY,
    OPEN_YOUTUBE_GUIDE,
    SWITCH_TO_CATALOGUE,
    NONE,
}

/** Localized title/message plus the recovery affordance for a failure. */
data class ErrorPresentation(
    val title: String,
    val message: String,
    val recovery: RecoveryAction = RecoveryAction.RETRY,
    val secondaryRecovery: RecoveryAction = RecoveryAction.NONE,
)

/**
 * Maps the typed errors produced by the core module onto user facing copy.
 * Keeping this in one place means search, home and playback all explain
 * failures the same way.
 */
@Composable
fun SonoraError.presentation(provider: Provider = Provider.CATALOGUE): ErrorPresentation = when (this) {
    SonoraError.EmptyQuery -> ErrorPresentation(
        title = stringResource(R.string.error_title_empty_query),
        message = stringResource(R.string.error_message_empty_query),
        recovery = RecoveryAction.NONE,
    )

    SonoraError.NoConnection -> ErrorPresentation(
        title = stringResource(R.string.error_title_offline),
        message = stringResource(R.string.error_message_offline),
        recovery = RecoveryAction.RETRY,
    )

    SonoraError.Timeout -> ErrorPresentation(
        title = stringResource(R.string.error_title_timeout),
        message = stringResource(R.string.error_message_timeout),
    )

    SonoraError.RateLimited -> ErrorPresentation(
        title = stringResource(R.string.error_title_rate_limited),
        message = stringResource(R.string.error_message_rate_limited),
    )

    SonoraError.MissingApiKey -> ErrorPresentation(
        title = stringResource(R.string.error_title_missing_key),
        message = stringResource(R.string.error_message_missing_key),
        recovery = RecoveryAction.OPEN_YOUTUBE_GUIDE,
        secondaryRecovery = RecoveryAction.SWITCH_TO_CATALOGUE,
    )

    SonoraError.InvalidApiKey -> ErrorPresentation(
        title = stringResource(R.string.error_title_invalid_key),
        message = stringResource(R.string.error_message_invalid_key),
        recovery = RecoveryAction.OPEN_YOUTUBE_GUIDE,
        secondaryRecovery = RecoveryAction.SWITCH_TO_CATALOGUE,
    )

    SonoraError.QuotaExceeded -> ErrorPresentation(
        title = stringResource(R.string.error_title_quota),
        message = stringResource(R.string.error_message_quota),
        secondaryRecovery = if (provider == Provider.YOUTUBE) RecoveryAction.SWITCH_TO_CATALOGUE else RecoveryAction.NONE,
    )

    SonoraError.MalformedResponse -> ErrorPresentation(
        title = stringResource(R.string.error_title_malformed),
        message = stringResource(R.string.error_message_malformed),
    )

    is SonoraError.ServerError -> ErrorPresentation(
        title = stringResource(R.string.error_title_server),
        message = stringResource(R.string.error_message_server, statusCode),
    )

    is SonoraError.Unknown -> ErrorPresentation(
        title = stringResource(R.string.error_title_unknown),
        message = stringResource(R.string.error_message_unknown),
    )
}

/** Which provider produced a failure, used to tailor recovery actions. */
enum class Provider {
    CATALOGUE,
    YOUTUBE,
}
