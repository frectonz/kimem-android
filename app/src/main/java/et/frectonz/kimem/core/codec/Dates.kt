package et.frectonz.kimem.core.codec

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val ADDIS: ZoneId = ZoneId.of("Africa/Addis_Ababa")

private fun pattern(p: String): DateTimeFormatter = DateTimeFormatter.ofPattern(p, Locale.US)

data class RouterDateTime(val value: ZonedDateTime) {
    fun display(): String = value.format(pattern("yyyy-MM-dd hh:mm:ss a"))

    companion object {
        private val PARSE = pattern("uu,MM,dd,HH,mm,ss")
        private val ROUTER_TIME = pattern("uu;MM;dd;HH;mm;ss")

        fun parse(raw: String): RouterDateTime? {
            val parts = raw.trim().split(',')
            if (parts.size < 6) return null
            return try {
                RouterDateTime(LocalDateTime.parse(parts.take(6).joinToString(","), PARSE).atZone(ADDIS))
            } catch (_: DateTimeParseException) {
                null
            }
        }

        fun routerTimeNow(): String = ZonedDateTime.now().format(ROUTER_TIME) + ";+3"
    }
}

data class BuildTime(val value: LocalDateTime) {
    fun display(): String = value.format(pattern("yyyy-MM-dd HH:mm"))

    companion object {
        private val PARSE = pattern("uuuu-MM-dd_HH:mm")

        fun parse(raw: String): BuildTime? = try {
            BuildTime(LocalDateTime.parse(raw.trim(), PARSE))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
