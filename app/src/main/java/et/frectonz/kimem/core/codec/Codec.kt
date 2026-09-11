package et.frectonz.kimem.core.codec

import java.security.MessageDigest
import java.util.Locale
import kotlin.io.encoding.Base64
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

fun b64Encode(input: String): String = Base64.Default.encode(input.encodeToByteArray())

fun b64Decode(input: String): String = Base64.Default.decode(input.trim()).decodeToString()

fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256").digest(input.encodeToByteArray()).toHexString()

fun ucs2Decode(hex: String): String {
    val bytes = hex.trim().hexToByteArray()
    require(bytes.size % 2 == 0) { "non even length" }
    val chars = CharArray(bytes.size / 2) { i ->
        val hi = bytes[2 * i].toInt() and 0xFF
        val lo = bytes[2 * i + 1].toInt() and 0xFF
        ((hi shl 8) or lo).toChar()
    }
    return String(chars).replace("\r", "")
}

fun ucs2Encode(text: String): String {
    val bytes = ByteArray(text.length * 2)
    text.forEachIndexed { i, c ->
        bytes[2 * i] = (c.code shr 8).toByte()
        bytes[2 * i + 1] = (c.code and 0xFF).toByte()
    }
    return bytes.toHexString()
}

private val DECIMAL_UNITS = listOf("B", "kB", "MB", "GB", "TB", "PB", "EB")
private val BINARY_UNITS = listOf("B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB")

fun humanSize(bytes: Long, binary: Boolean = false): String {
    val base = if (binary) 1024.0 else 1000.0
    val units = if (binary) BINARY_UNITS else DECIMAL_UNITS
    if (bytes < base) return "$bytes B"

    var exponent = floor(ln(bytes.toDouble()) / ln(base)).toInt().coerceIn(1, units.size - 1)
    var value = bytes / base.pow(exponent)
    if (value >= base && exponent < units.size - 1) {
        exponent += 1
        value /= base
    }

    val number = if (value == floor(value)) value.toLong().toString() else String.format(Locale.US, "%.2f", value)
    return "$number ${units[exponent]}"
}

fun fmtNum(value: Double): String =
    if (value == floor(value) && abs(value) < 1e15) value.toLong().toString() else value.toString()

fun fmt2(value: Double): String = String.format(Locale.US, "%.2f", value)

fun fmtSeconds(total: Long): String {
    val days = total / 86_400
    var rest = total % 86_400
    val hours = rest / 3_600
    rest %= 3_600
    val minutes = rest / 60
    val seconds = rest % 60

    val parts = mutableListOf<String>()
    if (days > 0) parts += "${days}d"
    if (hours > 0) parts += "${hours}h"
    if (minutes > 0) parts += "${minutes}m"
    if (seconds > 0 || parts.isEmpty()) parts += "${seconds}s"
    return parts.joinToString(" ")
}

fun truncateChars(text: String, max: Int): String =
    if (text.length <= max) text else text.take(max).trimEnd() + "…"

fun joinMsgIds(ids: Iterable<Int>): String = ids.joinToString("") { "$it;" }
