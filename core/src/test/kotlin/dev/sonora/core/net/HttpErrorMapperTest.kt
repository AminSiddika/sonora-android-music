package dev.sonora.core.net

import dev.sonora.core.model.SonoraError
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpErrorMapperTest {

    @Test
    fun `maps throttling and server statuses`() {
        assertEquals(SonoraError.RateLimited, HttpErrorMapper.fromStatus(429))
        assertEquals(SonoraError.RateLimited, HttpErrorMapper.fromStatus(403))
        assertEquals(SonoraError.Timeout, HttpErrorMapper.fromStatus(408))
        assertEquals(SonoraError.ServerError(500), HttpErrorMapper.fromStatus(500))
        assertEquals(SonoraError.ServerError(503), HttpErrorMapper.fromStatus(503))
        assertEquals(SonoraError.ServerError(400), HttpErrorMapper.fromStatus(400))
    }

    @Test
    fun `unexpected statuses become unknown errors`() {
        val error = HttpErrorMapper.fromStatus(999)
        assertEquals(SonoraError.Unknown("Unexpected status 999"), error)
    }

    @Test
    fun `provider failures keep their typed error`() {
        val error = HttpErrorMapper.fromException(ApiHttpException(429, SonoraError.RateLimited))
        assertEquals(SonoraError.RateLimited, error)
    }

    @Test
    fun `timeouts map to Timeout`() {
        assertEquals(SonoraError.Timeout, HttpErrorMapper.fromException(SocketTimeoutException("read timed out")))
    }

    @Test
    fun `network failures map to NoConnection`() {
        assertEquals(SonoraError.NoConnection, HttpErrorMapper.fromException(UnknownHostException("no dns")))
        assertEquals(SonoraError.NoConnection, HttpErrorMapper.fromException(IOException("broken pipe")))
    }

    @Test
    fun `parsing failures map to MalformedResponse`() {
        assertEquals(
            SonoraError.MalformedResponse,
            HttpErrorMapper.fromException(SerializationException("unexpected token")),
        )
    }

    @Test
    fun `anything else is unknown`() {
        val error = HttpErrorMapper.fromException(IllegalStateException("boom"))
        assertEquals(SonoraError.Unknown("boom"), error)
    }
}
