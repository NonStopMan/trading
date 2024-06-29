import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.runBlocking
import model.InstrumentEvent
import model.QuoteEvent
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import redis.clients.jedis.Jedis
import websocket.WebSocketConsumer

class WebSocketConsumerTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `test processQuote`(): Unit =
        runBlocking {
            val jedisMock = mock(Jedis::class.java)
            val webSocketConsumer = WebSocketConsumer(jedisMock)

            val quoteEventJson = """{"data":{"isin":"test-isin","price":100.0}}"""
            val quoteEvent: QuoteEvent = objectMapper.readValue(quoteEventJson)

            `when`(jedisMock.type(anyString())).thenReturn("none")
            webSocketConsumer.processQuote(quoteEvent)

            verify(jedisMock).hmset(anyString(), anyMap())
        }

    @Test
    fun `test processInstrument ADD`(): Unit =
        runBlocking {
            val jedisMock = mock(Jedis::class.java)
            val webSocketConsumer = WebSocketConsumer(jedisMock)

            val instrumentEventJson = """{"type":"ADD","data":{"isin":"test-isin","description":"Test Instrument"}}"""
            val instrumentEvent: InstrumentEvent = objectMapper.readValue(instrumentEventJson)

            webSocketConsumer.processInstrument(instrumentEvent)

            verify(jedisMock).hset("instruments", "test-isin", "Test Instrument")
        }

    @Test
    fun `test processInstrument DELETE`(): Unit =
        runBlocking {
            val jedisMock = mock(Jedis::class.java)
            val webSocketConsumer = WebSocketConsumer(jedisMock)

            val instrumentEventJson = """{"type":"DELETE","data":{"isin":"test-isin","description":"Test Instrument"}}"""
            val instrumentEvent: InstrumentEvent = objectMapper.readValue(instrumentEventJson)

            `when`(jedisMock.keys(anyString())).thenReturn(setOf("test-isin:12345"))

            webSocketConsumer.processInstrument(instrumentEvent)

            verify(jedisMock).hdel("instruments", "test-isin")
            verify(jedisMock).del("test-isin:12345")
        }
}
