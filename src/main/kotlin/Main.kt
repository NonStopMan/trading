import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import model.AppConfig
import org.http4k.server.Http4kServer
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import websocket.WebSocketService
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("MainKt")

suspend fun main() {
    logger.info("starting up")
    loadConfig()
        .also { config -> createHttpServer(config) }
        .also { config ->
            createRedisInstances(config).let {
                val (instrumentsJedisInstance, quotesJedisInstance) = it.getOrThrow()
                WebSocketService(quotesJedis = quotesJedisInstance, instrumentsJedis = instrumentsJedisInstance).start(
                    instrumentsWebSocketUri = config.websockets.instrumentsWebSocketUri,
                    quotesWebSocketUri = config.websockets.quotesWebSocketUri,
                )
            }
        }
}

fun loadConfig(): AppConfig =
    runCatching { ConfigFactory.load().extract<AppConfig>() }
        .getOrElse {
            logger.error("Failed to load configuration", it)
            exitProcess(1)
        }

private suspend fun createHttpServer(config: AppConfig): Http4kServer =
    runCatching {
        val server = createHttp4kServer(config.redis, config.server.port)
        server.start()
        server
    }.getOrElse {
        logger.error("Failed to create http server", it)
        exitProcess(1)
    }

private fun createRedisInstances(config: AppConfig): Result<Pair<Jedis, Jedis>> =
    runCatching {
        Jedis(config.redis.url, config.redis.port) to Jedis(config.redis.url, config.redis.port)
    }.onFailure { e ->
        logger.error("Error occurred while creating Redis instances: ${e.message}")
        exitProcess(1)
    }
