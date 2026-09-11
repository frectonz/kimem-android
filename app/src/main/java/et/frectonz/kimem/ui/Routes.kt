package et.frectonz.kimem.ui

import androidx.annotation.Keep
import androidx.annotation.StringRes
import et.frectonz.kimem.R
import et.frectonz.kimem.core.model.Show
import et.frectonz.kimem.core.model.TextShow
import et.frectonz.kimem.core.model.res
import et.frectonz.kimem.core.network.Router
import kotlinx.serialization.Serializable

@Keep
enum class ReportKind(@StringRes val title: Int, @StringRes val description: Int, val path: List<String>) {
    Info(R.string.kind_info, R.string.desc_info, listOf("get", "info")),
    System(R.string.kind_system, R.string.desc_system, listOf("get", "system")),
    Signal(R.string.kind_signal, R.string.desc_signal, listOf("get", "signal")),
    Internet(R.string.kind_internet, R.string.desc_internet, listOf("get", "internet")),
    Apn(R.string.kind_apn, R.string.desc_apn, listOf("get", "apn")),
    Device(R.string.kind_device, R.string.desc_device, listOf("get", "device")),
    Wifi(R.string.kind_wifi, R.string.desc_wifi, listOf("get", "wifi")),
    Devices(R.string.kind_devices, R.string.desc_devices, listOf("get", "devices")),
    SmsInfo(R.string.kind_sms_info, R.string.desc_sms_info, listOf("get", "sms", "info")),
    Syslog(R.string.kind_syslog, R.string.desc_syslog, listOf("get", "syslog")),
    Airtime(R.string.kind_airtime, R.string.desc_airtime, listOf("get", "airtime")),
    Power(R.string.kind_power, R.string.desc_power, listOf("get", "power")),
}

suspend fun Router.fetchReport(kind: ReportKind): Show = when (kind) {
    ReportKind.Info -> info()
    ReportKind.System -> system()
    ReportKind.Signal -> signal()
    ReportKind.Internet -> internet()
    ReportKind.Apn -> apn()
    ReportKind.Device -> device()
    ReportKind.Wifi -> wifi()
    ReportKind.Devices -> devices()
    ReportKind.SmsInfo -> smsInfo()
    ReportKind.Syslog -> TextShow(res(R.string.kind_syslog), syslog())
    ReportKind.Airtime -> airtime()
    ReportKind.Power -> power()
}

@Keep
enum class SmsAction(@StringRes val label: Int, val command: String) {
    Delete(R.string.delete, "delete"),
    Mark(R.string.mark_read, "mark"),
}

sealed interface Route {
    @Serializable
    data class Menu(val path: List<String> = emptyList()) : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class Report(val kind: ReportKind) : Route

    @Serializable
    data object SmsInbox : Route

    @Serializable
    data object SmsShow : Route

    @Serializable
    data class SmsMessage(val id: Int) : Route

    @Serializable
    data class SendSms(val number: String = "") : Route

    @Serializable
    data class SmsSelect(val action: SmsAction) : Route

    @Serializable
    data class Ussd(val code: String = "") : Route
}

data class Crumb(val label: String, val target: Route? = null)

fun menuCrumbs(path: List<String>): List<Crumb> =
    listOf(Crumb("kimem", Route.Menu())) + path.indices.map { i -> Crumb(path[i], Route.Menu(path.take(i + 1))) }

fun leafCrumbs(path: List<String>): List<Crumb> =
    menuCrumbs(path.dropLast(1)) + Crumb(path.last())
