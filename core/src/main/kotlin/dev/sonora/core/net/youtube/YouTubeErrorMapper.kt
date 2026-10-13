package dev.sonora.core.net.youtube

import dev.sonora.core.model.SonoraError
import dev.sonora.core.net.SonoraJson

/**
 * Translates YouTube Data API v3 error payloads into [SonoraError].
 *
 * The API reports problems such as an invalid key, an exhausted daily quota or
 * a disabled API through `error.errors[].reason`, so the reason is inspected
 * before falling back to the HTTP status.
 */
object YouTubeErrorMapper {

    fun map(statusCode: Int, body: String?): SonoraError {
        val dto = body?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { SonoraJson.instance.decodeFromString<YouTubeErrorEnvelope>(raw).error }.getOrNull()
        }
        val reason = dto?.errors?.firstNotNullOfOrNull { it.reason }
        val detail = dto?.message
        return when (reason) {
            "quotaExceeded", "dailyLimitExceeded" -> SonoraError.QuotaExceeded
            "rateLimitExceeded", "userRateLimitExceeded" -> SonoraError.RateLimited
            "keyInvalid", "keyMissing", "badRequest" -> SonoraError.InvalidApiKey
            "ipRefererBlocked", "accessNotConfigured", "forbidden", "accountDelegationForbidden" ->
                SonoraError.InvalidApiKey
            null -> fallback(statusCode, detail)
            else -> fallback(statusCode, detail)
        }
    }

    private fun fallback(statusCode: Int, detail: String?): SonoraError = when {
        statusCode == 429 -> SonoraError.RateLimited
        statusCode == 401 -> SonoraError.InvalidApiKey
        statusCode == 403 -> SonoraError.QuotaExceeded
        statusCode == 404 -> SonoraError.ServerError(statusCode, detail)
        statusCode in 500..599 -> SonoraError.ServerError(statusCode, detail)
        statusCode in 400..499 -> SonoraError.ServerError(statusCode, detail)
        else -> SonoraError.Unknown(detail ?: "Unexpected status $statusCode")
    }
}
