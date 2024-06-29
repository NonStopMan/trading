
@file:Suppress("ktlint:standard:no-wildcard-imports")

package websocket

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.*
import model.InstrumentEvent
import model.QuoteEvent
import model.jackson
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class WebSocketConsumer(
    private val jedis: Jedis,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun connectToWebSocket(uriString: String) {
        coroutineScope {
            while (true) {
                try {
                    val client = WebsocketClient.nonBlocking(Uri.of(uriString))
                    client.onMessage { message ->
                        runBlocking {
                            when {
                                uriString.endsWith("/quotes") ->
                                    launch {
                                        jackson
                                            .readValue<QuoteEvent>(message.bodyString())
                                            .also { processQuote(it) }
                                    }

                                uriString.endsWith("/instruments") ->
                                    launch {
                                        jackson
                                            .readValue<InstrumentEvent>(message.bodyString())
                                            .also { processInstrument(it) }
                                    }
                            }
                        }
                    }

                    client.onClose {
                        logger.error("WebSocket closed, attempting to reconnect...")
                    }

                    break // Break the loop if connection is successful
                } catch (e: Exception) {
                    logger.error("Error connecting to WebSocket: ${e.message}. Retrying in 5 seconds...")
                    delay(5000) // Wait for 5 seconds before retrying
                    connectToWebSocket(uriString)
                }
            }
        }
    }

    suspend fun processQuote(quoteEvent: QuoteEvent) {
        withContext(Dispatchers.IO) {
            try {
                logger.debug("Processing quote event: {} started", quoteEvent)
                val quoteData = quoteEvent.data
                val currentInstant = Instant.now()
                val currentMinute = currentInstant.truncatedTo(ChronoUnit.MINUTES)
                val nextMinute = currentMinute.plus(1, ChronoUnit.MINUTES)
                val minuteKey = "${quoteData.isin}:${DateTimeFormatter.ISO_INSTANT.format(currentMinute)}"

                if (jedis.type(minuteKey) != "hash") {
                    jedis.del(minuteKey)
                }
                val candlestick = jedis.hgetAll(minuteKey)
                if (candlestick.isEmpty()) {
                    // Initialize new candlestick with all the properties in the quote
                    jedis.hmset(
                        minuteKey,
                        mapOf(
                            "openTimestamp" to DateTimeFormatter.ISO_INSTANT.format(currentMinute),
                            "closeTimestamp" to DateTimeFormatter.ISO_INSTANT.format(nextMinute),
                            "openPrice" to quoteData.price.toString(),
                            "highPrice" to quoteData.price.toString(),
                            "lowPrice" to quoteData.price.toString(),
                            "closePrice" to quoteData.price.toString(),
                        ),
                    )
                } else {
                    // Update existing candlestick with the new quote
                    jedis.hset(minuteKey, "closePrice", quoteData.price.toString())
                    if (quoteData.price > candlestick["highPrice"]!!.toDouble()) {
                        jedis.hset(minuteKey, "highPrice", quoteData.price.toString())
                    }
                    if (quoteData.price < candlestick["lowPrice"]!!.toDouble()) {
                        jedis.hset(minuteKey, "lowPrice", quoteData.price.toString())
                    }
                }
                logger.debug("Processing quote event: {} ended", quoteEvent)
            } catch (e: Exception) {
                logger.error("Error processing quote event", e)
            }
        }
    }

    suspend fun processInstrument(instrumentEvent: InstrumentEvent) {
        withContext(Dispatchers.IO) {
            logger.info("Processing instrument event: {}", instrumentEvent)
            if (instrumentEvent.type === InstrumentEvent.Type.DELETE) {
                try {
                    jedis.hdel("instruments", instrumentEvent.data.isin)
                    val keys: Set<String> = jedis.keys("${instrumentEvent.data.isin}:*")
                    for (key in keys) {
                        jedis.del(key)
                    }
                } catch (e: Exception) {
                    logger.error("Error during delete instrument from cache", e)
                }
            } else if (instrumentEvent.type === InstrumentEvent.Type.ADD) {
                val instrumentData = instrumentEvent.data
                jedis.hset("instruments", instrumentData.isin, instrumentData.description)
            } else {
                logger.error("Unknown instrument event type: ${instrumentEvent.type}")
            }
        }
    }
}
