import model.RedisConfig
import org.http4k.core.HttpHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.routes
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import server.configureRoutes
import service.RedisCandlestickManager

private val logger = LoggerFactory.getLogger("ServerKt")

suspend fun createHttp4kServer(
    redis: RedisConfig,
    serverPort: Int,
): Http4kServer {
    val jedis =
        try {
            Jedis(redis.url, redis.port)
        } catch (exception: Exception) {
            logger.error("Failed to connect to Redis", exception)
            throw exception
        }
    val candlestickManager = RedisCandlestickManager(jedis)

    val app: HttpHandler =
        ServerFilters.CatchAll().then(
            routes(
                configureRoutes(candlestickManager),
            ),
        )
    logger.info("Starting server on port 9000")
    return app.asServer(Netty(serverPort))
}
