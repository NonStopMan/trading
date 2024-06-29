
package service

import io.github.serpro69.kfaker.Faker
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import model.Candlestick
import model.jackson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import redis.clients.jedis.Jedis
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RedisCandlestickManagerTest {
    private lateinit var jedis: Jedis
    private lateinit var redisCandlestickManager: RedisCandlestickManager
    private val faker = Faker()

    @BeforeEach
    fun setUp() {
        jedis = mockk()
        redisCandlestickManager = RedisCandlestickManager(jedis)
    }

    @Test
    fun `getCandlesticks should return cached candlesticks if available`(): Unit =
        runBlocking {
            val isin = faker.barcode.issn()
            val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
            val candlestick =
                Candlestick(
                    openTimestamp = now.toString(),
                    closeTimestamp = now.plus(1, ChronoUnit.MINUTES).toString(),
                    openPrice = 100.0,
                    highPrice = 200.0,
                    lowPrice = 50.0,
                    closingPrice = 150.0,
                )
            val cachedCandlesticksJson = jackson.writeValueAsString(listOf(candlestick))
            val candlesticksKey = "$isin:${DateTimeFormatter.ISO_INSTANT.format(now)}:cs"

            every { jedis.hgetAll("instruments") } returns mapOf(isin to "Some Instrument")
            every { jedis.hgetAll(candlesticksKey) } returns mapOf("candlesticks" to cachedCandlesticksJson)

            val result = redisCandlestickManager.getCandlesticks(isin)

            assertEquals(1, result.size)
            assertEquals(candlestick, result[0])
            verify(exactly = 1) { jedis.hgetAll("instruments") }
            verify(exactly = 1) { jedis.hgetAll(candlesticksKey) }
        }

    @Test
    fun `test getCandlesticks with unknown instrument`() =
        runBlocking {
            val isin = "unknown_instrument"

            every { jedis.hgetAll("instruments") } returns emptyMap()

            val exception =
                assertThrows<IllegalArgumentException> {
                    runBlocking {
                        redisCandlestickManager.getCandlesticks(isin)
                    }
                }

            assertEquals("Unknown instrument: $isin", exception.message)
            verify(exactly = 1) { jedis.hgetAll("instruments") }
        }

    @Test
    fun `test getCandlesticks with missing cached data`() =
        runBlocking {
            val isin = faker.barcode.issn()
            val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
            val pastInstant = now.minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
            val candlestick =
                Candlestick(
                    openTimestamp = pastInstant.toString(),
                    closeTimestamp = pastInstant.plus(1, ChronoUnit.MINUTES).toString(),
                    openPrice = 100.0,
                    highPrice = 200.0,
                    lowPrice = 50.0,
                    closingPrice = 150.0,
                )
            val candlestickData =
                mapOf(
                    "openTimestamp" to candlestick.openTimestamp,
                    "closeTimestamp" to candlestick.closeTimestamp,
                    "openPrice" to candlestick.openPrice.toString(),
                    "highPrice" to candlestick.highPrice.toString(),
                    "lowPrice" to candlestick.lowPrice.toString(),
                    "closePrice" to candlestick.closingPrice.toString(),
                )
            val minuteInstant = now.minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
            val minuteKey = "$isin:${DateTimeFormatter.ISO_INSTANT.format(minuteInstant)}"

            every { jedis.hgetAll(any<String>()) } returns emptyMap()
            every { jedis.hgetAll(minuteKey) } returns candlestickData
            every { jedis.hgetAll("instruments") } returns mapOf(isin to "Some Instrument")
            every { jedis.hset(any<String>(), any(), any()) } returns 1

            val result = redisCandlestickManager.getCandlesticks(isin)

            assertEquals(1, result.size)
            assertEquals(candlestick, result[0])
            verify(exactly = 1) { jedis.hgetAll("instruments") }
            verify(atLeast = 2) { jedis.hgetAll(any<String>()) }
            verify(exactly = 1) { jedis.hset(any<String>(), any(), any()) }
        }

    @Test
    fun `test getCandlesticks with no historical data`() =
        runBlocking {
            val isin = faker.barcode.issn()
            every { jedis.hgetAll(any<String>()) } returns emptyMap()
            every { jedis.hgetAll("instruments") } returns mapOf(isin to "Some Instrument")

            val result = redisCandlestickManager.getCandlesticks(isin)

            assertTrue(result.isEmpty())
            verify(exactly = 1) { jedis.hgetAll("instruments") }
            verify(atLeast = 31) { jedis.hgetAll(any<String>()) }
        }

    @Test
    fun `test getCandlesticks with gabs in the historical data`() =
        runBlocking {
            val isin = faker.barcode.issn()
            val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
            val pastInstant = now.minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)

            val candlestick1 =
                Candlestick(
                    openTimestamp = pastInstant.toString(),
                    closeTimestamp = pastInstant.plus(1, ChronoUnit.MINUTES).toString(),
                    openPrice = faker.random.nextDouble(),
                    highPrice = faker.random.nextDouble(),
                    lowPrice = faker.random.nextDouble(),
                    closingPrice = faker.random.nextDouble(),
                )
            val candlestick2 =
                Candlestick(
                    openTimestamp = pastInstant.minus(2, ChronoUnit.MINUTES).toString(),
                    closeTimestamp = pastInstant.minus(1, ChronoUnit.MINUTES).toString(),
                    openPrice = faker.random.nextDouble().toDouble(),
                    highPrice = faker.random.nextDouble(),
                    lowPrice = faker.random.nextDouble(),
                    closingPrice = faker.random.nextDouble(),
                )
            val missingCandlestick =
                Candlestick(
                    openTimestamp = pastInstant.minus(1, ChronoUnit.MINUTES).toString(),
                    closeTimestamp = pastInstant.minus(0, ChronoUnit.MINUTES).toString(),
                    openPrice = candlestick2.openPrice,
                    highPrice = candlestick2.highPrice,
                    lowPrice = candlestick2.lowPrice,
                    closingPrice = candlestick2.closingPrice,
                )
            val candlestickData1 =
                mapOf(
                    "openTimestamp" to candlestick1.openTimestamp,
                    "closeTimestamp" to candlestick1.closeTimestamp,
                    "openPrice" to candlestick1.openPrice.toString(),
                    "highPrice" to candlestick1.highPrice.toString(),
                    "lowPrice" to candlestick1.lowPrice.toString(),
                    "closePrice" to candlestick1.closingPrice.toString(),
                )
            val candlestickData2 =
                mapOf(
                    "openTimestamp" to candlestick2.openTimestamp,
                    "closeTimestamp" to candlestick2.closeTimestamp,
                    "openPrice" to candlestick2.openPrice.toString(),
                    "highPrice" to candlestick2.highPrice.toString(),
                    "lowPrice" to candlestick2.lowPrice.toString(),
                    "closePrice" to candlestick2.closingPrice.toString(),
                )
            val firstMinuteKey = "$isin:${DateTimeFormatter.ISO_INSTANT.format(pastInstant)}"
            val secondMinuteKey = "$isin:${DateTimeFormatter.ISO_INSTANT.format(pastInstant.minus(2, ChronoUnit.MINUTES))}"

            every { jedis.hgetAll(any<String>()) } returns emptyMap()
            every { jedis.hgetAll(firstMinuteKey) } returns candlestickData1
            every { jedis.hgetAll(secondMinuteKey) } returns candlestickData2

            every { jedis.hgetAll("instruments") } returns mapOf(isin to "Some Instrument")
            every { jedis.hset(any<String>(), any(), any()) } returns 1
            val result = redisCandlestickManager.getCandlesticks(isin)

            assertEquals(3, result.size)
            assertEquals(candlestick1, result[0])
            assertEquals(missingCandlestick, result[1])
            assertEquals(candlestick2, result[2])
            verify(exactly = 1) { jedis.hgetAll("instruments") }
            verify(atLeast = 32) { jedis.hgetAll(any<String>()) }
            verify(exactly = 1) { jedis.hset(any<String>(), any(), any()) }
        }
}
