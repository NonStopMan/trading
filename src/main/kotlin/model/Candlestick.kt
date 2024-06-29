
package model

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import formatter.CustomDateTimeDeserializer
import formatter.CustomDateTimeSerializer
import formatter.CustomTimestampDeserializer
import formatter.CustomTimestampSerializer

data class Candlestick(
    @JsonSerialize(using = CustomDateTimeSerializer::class)
    @JsonDeserialize(using = CustomDateTimeDeserializer::class)
    val openTimestamp: String,
    @JsonSerialize(using = CustomTimestampSerializer::class)
    @JsonDeserialize(using = CustomTimestampDeserializer::class)
    var closeTimestamp: String,
    val openPrice: Price,
    var highPrice: Price,
    var lowPrice: Price,
    var closingPrice: Price,
)
