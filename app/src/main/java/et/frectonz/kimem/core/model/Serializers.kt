package et.frectonz.kimem.core.model

import et.frectonz.kimem.core.codec.BuildTime
import et.frectonz.kimem.core.codec.RouterDateTime
import et.frectonz.kimem.core.codec.b64Decode
import et.frectonz.kimem.core.codec.ucs2Decode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

abstract class StringMapped<T>(name: String, private val map: (String) -> T) : KSerializer<T> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): T {
        val raw = decoder.decodeString()
        return try {
            map(raw)
        } catch (e: SerializationException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw SerializationException("cannot parse \"$raw\" as ${descriptor.serialName}: ${e.message}", e)
        }
    }

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.toString())
}

private fun fail(raw: String, what: String): Nothing = throw SerializationException("cannot parse \"$raw\" as $what")

object StringInt : StringMapped<Int>("StringInt", { it.trim().toIntOrNull() ?: fail(it, "int") })
object StringLong : StringMapped<Long>("StringLong", { it.trim().toLongOrNull() ?: fail(it, "long") })
object StringDouble : StringMapped<Double>("StringDouble", { it.trim().toDoubleOrNull() ?: fail(it, "double") })

object OptionalDouble : StringMapped<Double?>("OptionalDouble", { raw ->
    raw.trim().let { if (it.isEmpty()) null else it.toDoubleOrNull() ?: fail(raw, "double") }
})

object Flag : StringMapped<Boolean>("Flag", {
    when (it) {
        "1" -> true
        "0" -> false
        else -> fail(it, "flag")
    }
})

object Ucs2 : StringMapped<String>("Ucs2", ::ucs2Decode)
object B64 : StringMapped<String>("B64", ::b64Decode)

object Kibibytes : StringMapped<Long>("Kibibytes", {
    it.trim().removeSuffix("kB").trim().toLongOrNull() ?: fail(it, "kibibytes")
})

object Percent : StringMapped<Double>("Percent", {
    it.trim().removeSuffix("%").toDoubleOrNull() ?: fail(it, "percent")
})

object DnsList : StringMapped<List<String>>("DnsList", { raw ->
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
})

object PppStatusSerializer : StringMapped<PppStatus>("PppStatus", PppStatus::parse)
object ChannelBandwidthSerializer : StringMapped<ChannelBandwidth>("ChannelBandwidth", ChannelBandwidth::parse)
object MessageStatusSerializer : StringMapped<MessageStatus>("MessageStatus", MessageStatus::parse)
object SmsStoreSerializer : StringMapped<SmsStore>("SmsStore", SmsStore::parse)
object UssdFlagSerializer : StringMapped<UssdFlag>("UssdFlag", UssdFlag::parse)
object LoginResultSerializer : StringMapped<LoginResult>("LoginResult", LoginResult::parse)

object RouterDateTimeSerializer : StringMapped<RouterDateTime>("RouterDateTime", {
    RouterDateTime.parse(it) ?: fail(it, "timestamp")
})

object BuildTimeSerializer : StringMapped<BuildTime>("BuildTime", {
    BuildTime.parse(it) ?: fail(it, "build time")
})

object ApnProfileSerializer : StringMapped<ApnProfile>("ApnProfile", { raw ->
    val f = raw.split("(\$)")
    if (f.size < 11) fail(raw, "APN config")
    ApnProfile(
        profileName = f[0].trim(),
        apn = f[1].trim(),
        authMode = f[4].trim(),
        username = f[5].trim(),
        password = f[6].trim(),
        pdpType = f[7].trim(),
        dnsMode = f[10].trim(),
    )
})
