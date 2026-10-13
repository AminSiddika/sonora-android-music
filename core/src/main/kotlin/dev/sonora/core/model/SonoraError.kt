package dev.sonora.core.model

/**
 * Every failure mode Sonora can surface, expressed independently of Android so
 * that UI, data and tests share one vocabulary. The Android layer maps each
 * case to a localized message plus a recovery action.
 */
sealed interface SonoraError {

    /** The query was empty after normalisation. */
    data object EmptyQuery : SonoraError

    /** No usable network connection. */
    data object NoConnection : SonoraError

    /** The request took too long. */
    data object Timeout : SonoraError

    /** Provider throttled us (HTTP 403/429 on the iTunes endpoint). */
    data object RateLimited : SonoraError

    /** The optional provider needs an API key that was not configured. */
    data object MissingApiKey : SonoraError

    /** The configured API key was rejected by the provider. */
    data object InvalidApiKey : SonoraError

    /** The provider quota for the API key is exhausted. */
    data object QuotaExceeded : SonoraError

    /** The response body could not be parsed. */
    data object MalformedResponse : SonoraError

    /** The provider answered with an unexpected status code. */
    data class ServerError(val statusCode: Int, val detail: String? = null) : SonoraError

    /** Anything unexpected. */
    data class Unknown(val detail: String? = null) : SonoraError
}

/** True when retrying the same request could plausibly succeed. */
val SonoraError.isRetryable: Boolean
    get() = when (this) {
        SonoraError.EmptyQuery, SonoraError.MissingApiKey, SonoraError.InvalidApiKey -> false
        else -> true
    }
