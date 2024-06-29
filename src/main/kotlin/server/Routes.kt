package server

import kotlinx.coroutines.runBlocking
import model.jackson
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.slf4j.LoggerFactory
import service.CandlestickManager

private val logger = LoggerFactory.getLogger("configureRoutes")

suspend fun configureRoutes(candlestickManager: CandlestickManager): RoutingHttpHandler =
    routes(
        "/candlesticks" bind Method.GET to { request: Request ->
            runBlocking {
                val isin =
                    request.query("isin")
                        ?: return@runBlocking Response(Status.BAD_REQUEST).body("{'reason': 'missing_isin'}")
                try {
                    val candlesticks = candlestickManager.getCandlesticks(isin)
                    val body = jackson.writeValueAsBytes(candlesticks)
                    Response(Status.OK).body(body.inputStream())
                } catch (e: IllegalArgumentException) {
                    logger.error("Unknown instrument: $isin", e)
                    Response(Status.BAD_REQUEST).body("{'reason': 'unknown_instrument'}")
                } catch (exception: Exception) {
                    logger.error("Unknown error raised while getting instrument: $isin", exception)
                    Response(Status.INTERNAL_SERVER_ERROR).body("{'reason': 'internal_error'}")
                }
            }
        },
    )
