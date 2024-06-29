package formatter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CustomDateTimeDeserializer : JsonDeserializer<String>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): String {
        val value = p.text
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(value, formatter)
        val instant = localDateTime.atZone(ZoneId.of("UTC")).toInstant()
        return instant.toString()
    }
}
