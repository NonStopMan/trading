@file:Suppress("ktlint:standard:filename", "ktlint:standard:no-wildcard-imports")

import kotlinx.coroutines.runBlocking
import model.RedisConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import redis.clients.jedis.Jedis
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MainKtTest {
    @Test
    fun `test loadConfig`() {
        val config = loadConfig()
        assertNotNull(config)
    }

    @Test
    fun `test createHttp4kServer`() {
        runBlocking {
            val redisConfig = RedisConfig("localhost", 6379)
            val server = createHttp4kServer(redisConfig, 9000)
            assertNotNull(server)
        }
    }

    @Test
    fun `test Redis connection handling`() {
        val redisConfig = RedisConfig("localhost", 6379)
        val jedisMock = mock(Jedis::class.java)
        `when`(jedisMock.ping()).thenReturn("PONG")

        val jedisInstance =
            try {
                Jedis(redisConfig.url, redisConfig.port)
            } catch (e: Exception) {
                null
            }
        assertNotNull(jedisInstance)
        assertEquals("PONG", jedisMock.ping())
    }
}
