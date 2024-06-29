
package websocket
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis

class WebSocketService(
    private val quotesJedis: Jedis,
    private val instrumentsJedis: Jedis,
) {
    private val logger = LoggerFactory.getLogger(WebSocketService::class.java)

    suspend fun start(
        instrumentsWebSocketUri: String,
        quotesWebSocketUri: String,
    ) {
        WebSocketConsumer(quotesJedis).connectToWebSocket(quotesWebSocketUri).also {
            logger.info("Connected to quotes websocket")
        }
        WebSocketConsumer(instrumentsJedis).connectToWebSocket(instrumentsWebSocketUri).also {
            logger.info("Connected to instruments websocket")
        }
    }
}
