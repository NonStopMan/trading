package model

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class ServerConfig(
    val port: Int,
)

data class RedisConfig(
    val url: String,
    val port: Int,
)

data class WebSocketConfig(
    val instrumentsWebSocketUri: String,
    val quotesWebSocketUri: String,
)

data class AppConfig(
    val server: ServerConfig,
    val redis: RedisConfig,
    val websockets: WebSocketConfig,
)

val jackson: ObjectMapper =
    jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
