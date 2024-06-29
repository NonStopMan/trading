package model

data class QuoteEvent(
    val data: Quote,
)


data class Quote(
    val isin: ISIN,
    val price: Price,
)
typealias Price = Double