@file:Suppress("ktlint:standard:no-wildcard-imports")

package websocket

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import redis.clients.jedis.Jedis

class WebSocketServiceTest {
    private lateinit var quotesJedis: Jedis
    private lateinit var instrumentsJedis: Jedis
    private lateinit var webSocketService: WebSocketService

    @BeforeEach
    fun setUp() {
        quotesJedis = mockk(relaxed = true)
        instrumentsJedis = mockk(relaxed = true)
        webSocketService = WebSocketService(quotesJedis, instrumentsJedis)
    }

    @Test
    fun `test start method connects to web sockets`() {
        val quotesUri = "ws://quotes-uri"
        val instrumentsUri = "ws://instruments-uri"

        val webSocketConsumerMock = mockk<WebSocketConsumer>(relaxed = true)
        mockkConstructor(WebSocketConsumer::class)
        runBlocking {
            coEvery { anyConstructed<WebSocketConsumer>().connectToWebSocket(any()) } returns Unit
            webSocketService.start(instrumentsUri, quotesUri)
        }

        coVerify { anyConstructed<WebSocketConsumer>().connectToWebSocket(quotesUri) }
        coVerify { anyConstructed<WebSocketConsumer>().connectToWebSocket(instrumentsUri) }
    }
}
