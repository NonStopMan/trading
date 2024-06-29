@file:Suppress("ktlint:standard:no-wildcard-imports")

import kotlinx.coroutines.runBlocking
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import server.configureRoutes
import service.CandlestickManager
import kotlin.test.assertEquals

class RoutesTest {
    @Test
    fun `test GET candlesticks`() =
        runBlocking {
            val candlestickManagerMock = mock(CandlestickManager::class.java)
            `when`(candlestickManagerMock.getCandlesticks(anyString())).thenReturn(emptyList())

            val app = configureRoutes(candlestickManagerMock)
            val request = Request(Method.GET, "/candlesticks").query("isin", "test-isin")
            val response = app(request)

            assertEquals(Status.OK, response.status)
        }

    @Test
    fun `test GET candlesticks with missing isin`() =
        runBlocking {
            val candlestickManagerMock = mock(CandlestickManager::class.java)
            val app = configureRoutes(candlestickManagerMock)
            val expectedBody = "{'reason': 'missing_isin'}"
            val request = Request(Method.GET, "/candlesticks")
            val response = app(request)

            assertEquals(Status.BAD_REQUEST, response.status)

            assertEquals(expectedBody, response.body.toString())
        }

    @Test
    fun `test GET candlesticks with non existing isin`() =
        runBlocking {
            val candlestickManagerMock = mock(CandlestickManager::class.java)
            `when`(candlestickManagerMock.getCandlesticks(anyString())).thenThrow(IllegalArgumentException("Unknown instrument"))
            val app = configureRoutes(candlestickManagerMock)
            val expectedBody = "{'reason': 'unknown_instrument'}"
            val request = Request(Method.GET, "/candlesticks?isin=non-existing-isin")
            val response = app(request)

            assertEquals(Status.BAD_REQUEST, response.status)

            assertEquals(expectedBody, response.body.toString())
        }
}
