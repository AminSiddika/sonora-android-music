package dev.sonora.core.net

import dev.sonora.core.model.ApiResult
import dev.sonora.core.model.SonoraError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/** Thrown internally when a provider answers with a non-success status. */
class ApiHttpException(
    val statusCode: Int,
    val error: SonoraError,
    val body: String? = null,
) : Exception("HTTP $statusCode")

/** Maps transport level failures onto the shared [SonoraError] vocabulary. */
object HttpErrorMapper {

    /** Status mapping for the iTunes Search API (no JSON error envelope). */
    fun fromStatus(statusCode: Int, detail: String? = null): SonoraError = when {
        statusCode == 429 -> SonoraError.RateLimited
        // iTunes answers 403 when a client exceeds the ~20 requests/minute limit.
        statusCode == 403 -> SonoraError.RateLimited
        statusCode == 408 -> SonoraError.Timeout
        statusCode in 500..599 -> SonoraError.ServerError(statusCode, detail)
        statusCode in 400..499 -> SonoraError.ServerError(statusCode, detail)
        else -> SonoraError.Unknown(detail ?: "Unexpected status $statusCode")
    }

    fun fromException(throwable: Throwable): SonoraError = when (throwable) {
        is ApiHttpException -> throwable.error
        is HttpRequestTimeoutException -> SonoraError.Timeout
        is SerializationException -> SonoraError.MalformedResponse
        is java.io.InterruptedIOException -> SonoraError.Timeout
        is java.io.IOException -> SonoraError.NoConnection
        is IllegalArgumentException -> SonoraError.MalformedResponse
        else -> SonoraError.Unknown(throwable.message)
    }
}

/**
 * Runs [block], translating provider failures and parsing problems into
 * [ApiResult]. Coroutine cancellation is always rethrown so structured
 * concurrency keeps working.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    ApiResult.Failure(HttpErrorMapper.fromException(throwable))
}
