package service

import model.Candlestick

interface CandlestickManager {
    suspend fun getCandlesticks(isin: String): List<Candlestick>
}
