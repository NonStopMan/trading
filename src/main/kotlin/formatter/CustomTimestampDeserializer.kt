package formatter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CustomTimestampDeserializer : JsonDeserializer<String>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): String {
        val value = p.text
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val localTime = LocalTime.parse(value, formatter)
        val instant = localTime.atDate(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()).toInstant(ZoneOffset.UTC)
        return instant.toString()
    }
}
