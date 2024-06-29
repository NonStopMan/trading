package formatter

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CustomTimestampSerializer : JsonSerializer<String>() {
    override fun serialize(
        value: String?,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        value?.let {
            val instant = Instant.parse(it)
            val formatter =
                DateTimeFormatter
                    .ofPattern("HH:mm:ss")
                    .withZone(ZoneId.of("UTC")) // or use any other ZoneId if needed
            val formatted = formatter.format(instant)
            gen.writeString(formatted)
        }
    }
}
