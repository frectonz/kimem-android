package et.frectonz.kimem.core.model

import androidx.annotation.StringRes
import et.frectonz.kimem.R

sealed interface PppStatus {
    val label: UiText

    data object Connected : PppStatus {
        override val label = res(R.string.ppp_connected)
    }

    data object Disconnected : PppStatus {
        override val label = res(R.string.ppp_disconnected)
    }

    data class Other(val raw: String) : PppStatus {
        override val label = plain(raw)
    }

    companion object {
        fun parse(raw: String): PppStatus = when (raw) {
            "ppp_connected" -> Connected
            "ppp_disconnected" -> Disconnected
            else -> Other(raw)
        }
    }
}

enum class ChannelBandwidth(@StringRes val labelRes: Int) {
    Mhz20(R.string.bandwidth_20), Mhz20Or40(R.string.bandwidth_20_40), Mhz40(R.string.bandwidth_40);

    val label: UiText get() = res(labelRes)

    companion object {
        fun parse(raw: String): ChannelBandwidth = when (raw) {
            "0" -> Mhz20
            "1" -> Mhz20Or40
            else -> Mhz40
        }
    }
}

sealed interface MessageStatus {
    val label: UiText

    data object Read : MessageStatus {
        override val label = res(R.string.status_read)
    }

    data object Unread : MessageStatus {
        override val label = res(R.string.status_unread)
    }

    data object Sent : MessageStatus {
        override val label = res(R.string.status_sent)
    }

    data class Unknown(val tag: String) : MessageStatus {
        override val label = res(R.string.status_unknown, tag)
    }

    companion object {
        fun parse(raw: String): MessageStatus = when (raw) {
            "0" -> Read
            "1" -> Unread
            "2" -> Sent
            else -> Unknown(raw)
        }
    }
}

sealed interface SmsStore {
    val label: UiText

    data object Device : SmsStore {
        override val label = res(R.string.store_device)
    }

    data object Sim : SmsStore {
        override val label = res(R.string.store_sim)
    }

    data class Other(val raw: String) : SmsStore {
        override val label = plain(raw)
    }

    companion object {
        fun parse(raw: String): SmsStore = when (raw) {
            "nv" -> Device
            "sim" -> Sim
            else -> Other(raw)
        }
    }
}

sealed interface UssdFlag {
    data object Pending : UssdFlag
    data object Ready : UssdFlag
    data class Failed(val reason: UiText) : UssdFlag

    companion object {
        fun parse(raw: String): UssdFlag = when (raw) {
            "15" -> Pending
            "16" -> Ready
            "1" -> Failed(res(R.string.ussd_no_service))
            "2" -> Failed(res(R.string.ussd_terminated))
            "3", "4", "unknown" -> Failed(res(R.string.ussd_timed_out))
            "10" -> Failed(res(R.string.ussd_retry))
            "41" -> Failed(res(R.string.ussd_op_not_supported))
            "99" -> Failed(res(R.string.ussd_not_supported))
            else -> Failed(res(R.string.ussd_unexpected, raw))
        }
    }
}

sealed interface LoginResult {
    data object Success : LoginResult
    data object InvalidCredentials : LoginResult
    data object MalformedRequest : LoginResult
    data class Other(val code: String) : LoginResult

    companion object {
        fun parse(raw: String): LoginResult = when (raw) {
            "0" -> Success
            "3" -> InvalidCredentials
            "1" -> MalformedRequest
            else -> Other(raw)
        }
    }
}

sealed interface MsgSelector {
    data object All : MsgSelector
    data class Id(val id: Int) : MsgSelector

    companion object {
        fun parse(raw: String): MsgSelector? {
            val trimmed = raw.trim()
            if (trimmed.equals("all", ignoreCase = true)) return All
            return trimmed.toIntOrNull()?.let(::Id)
        }
    }
}

val USSD_CODES: List<Pair<String, String>> = listOf(
    "menu" to "*777#",
    "balance" to "*704#",
    "bundles" to "*777*02#",
    "mpesa" to "*733#",
)

fun resolveUssdCode(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.contains('*') || trimmed.contains('#')) return trimmed
    return USSD_CODES.firstOrNull { (name, _) -> name.equals(trimmed, ignoreCase = true) }?.second
}

fun ussdBuiltinsHelp(): String = USSD_CODES.joinToString(", ") { (name, code) -> "$name ($code)" }

fun fmtDbm(value: Double?): UiText = value?.let { res(R.string.dbm, et.frectonz.kimem.core.codec.fmtNum(it)) } ?: res(R.string.dash)
fun fmtDecibels(value: Double?): UiText = value?.let { res(R.string.db, et.frectonz.kimem.core.codec.fmtNum(it)) } ?: res(R.string.dash)
fun fmtDns(servers: List<String>): UiText = if (servers.isEmpty()) res(R.string.dash) else plain(servers.joinToString(", "))
