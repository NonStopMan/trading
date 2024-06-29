package service

import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.Candlestick
import model.jackson
import org.slf4j.LoggerFactory
import redis.clients.jedis.Jedis
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class RedisCandlestickManager(
    private val jedis: Jedis,
) : CandlestickManager {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getCandlesticks(isin: String): List<Candlestick> =
        withContext(Dispatchers.IO) {
            val instruments = jedis.hgetAll("instruments")
            instruments[isin] ?: throw IllegalArgumentException("Unknown instrument: $isin")

            val now = Instant.now()
            val currentMinute = now.truncatedTo(ChronoUnit.MINUTES)
            val isinMinuteKey = "$isin:${DateTimeFormatter.ISO_INSTANT.format(currentMinute)}"
            val cachedData = jedis.hgetAll("$isinMinuteKey:cs")
            if (cachedData.isNotEmpty()) {
                val cachedCandlesticks =
                    jackson.readValue<List<Candlestick>>(cachedData["candlesticks"]!!.toString())
                return@withContext cachedCandlesticks
            }

            val candlesticks = constructCandlesticks(isin, now)
            cacheCandlesticks(isinMinuteKey, candlesticks)
            return@withContext candlesticks
        }

    private fun constructCandlesticks(
        isin: String,
        now: Instant,
    ): MutableList<Candlestick> {
        val candlesticks = mutableListOf<Candlestick>()
        var previousCandlestick: Candlestick? = null

        for (i in 30 downTo 1) {
            val minuteInstant = now.minus(i.toLong(), ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)
            val minuteKey = "$isin:${DateTimeFormatter.ISO_INSTANT.format(minuteInstant)}"
            val data = jedis.hgetAll(minuteKey)

            val candlestick =
                if (data.isNotEmpty()) {
                    Candlestick(
                        openTimestamp = data["openTimestamp"]!!,
                        closeTimestamp = data["closeTimestamp"]!!,
                        openPrice = data["openPrice"]!!.toDouble(),
                        highPrice = data["highPrice"]!!.toDouble(),
                        lowPrice = data["lowPrice"]!!.toDouble(),
                        closingPrice = data["closePrice"]!!.toDouble(),
                    )
                } else {
                    previousCandlestick?.let {
                        Candlestick(
                            openTimestamp = DateTimeFormatter.ISO_INSTANT.format(minuteInstant),
                            closeTimestamp = DateTimeFormatter.ISO_INSTANT.format(minuteInstant.plus(1, ChronoUnit.MINUTES)),
                            openPrice = it.openPrice,
                            highPrice = it.highPrice,
                            lowPrice = it.lowPrice,
                            closingPrice = it.closingPrice,
                        )
                    } ?: continue
                }

            candlesticks.add(0, candlestick)
            previousCandlestick = candlestick
        }

        return candlesticks
    }

    private fun cacheCandlesticks(
        isinMinuteKey: String,
        candlesticks: List<Candlestick>,
    ) {
        try {
            jedis.hset("$isinMinuteKey:cs", "candlesticks", jackson.writeValueAsString(candlesticks))
        } catch (e: Exception) {
            logger.error("Failed to cache candlesticks to Redis", e)
        }
    }
}
