package server

import createHttp4kServer
import io.mockk.*
import kotlinx.coroutines.runBlocking
import model.RedisConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import redis.clients.jedis.Jedis
import service.RedisCandlestickManager

class ServerTest {
    private lateinit var redisConfig: RedisConfig
    private lateinit var jedis: Jedis
    private lateinit var candlestickManager: RedisCandlestickManager

    @BeforeEach
    fun setUp() {
        redisConfig = RedisConfig(url = "redis://localhost", port = 6379)
        jedis = mockk(relaxed = true)
        candlestickManager = mockk(relaxed = true)

        mockkConstructor(Jedis::class)
        every { anyConstructed<Jedis>().isConnected } returns true
//
//        every { RedisCandlestickManager(any()) } returns candlestickManager
//
//        mockkStatic(ServerFilters::class)
//        mockkStatic(::configureRoutes)
    }

//    @Test
//    fun `test createHttp4kServer initializes server successfully`() =
//        runBlocking {
//            val serverPort = 9000
//
//            val routingHandlerMock = mockk<RoutingHttpHandler>()
//
//            val httpHandlerMock = mockk<HttpHandler>()
//            every { ServerFilters.CatchAll().then(routingHandlerMock) } returns httpHandlerMock
//
//            val http4kServerMock = mockk<Http4kServer>()
//            every { httpHandlerMock.asServer(Netty(serverPort)) } returns http4kServerMock
//            every { http4kServerMock.start() } returns http4kServerMock
//
//            val server = createHttp4kServer(redisConfig, serverPort)
//
//            verify { RedisCandlestickManager(any()) }
//            verify { httpHandlerMock.asServer(Netty(serverPort)) }
//
//            assertEquals(http4kServerMock, server)
//        }

    @Test
    fun `test createHttp4kServer handles Redis connection failure`() {
        mockkConstructor(Jedis::class)

        every { anyConstructed<Jedis>() } throws RuntimeException("Failed to connect to Redis")

        val exception =
            assertThrows<RuntimeException> {
                runBlocking {
                    createHttp4kServer(redisConfig, 9000)
                }
            }

        assertEquals("Failed to connect to Redis", exception.message)
    }
}
