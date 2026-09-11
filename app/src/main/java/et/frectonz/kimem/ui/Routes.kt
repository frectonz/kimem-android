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
enum class ReportKind(@StringRes val title: Int, @StringRes val description: Int, val command: String) {
    Info(R.string.kind_info, R.string.desc_info, "get › info"),
    System(R.string.kind_system, R.string.desc_system, "get › system"),
    Signal(R.string.kind_signal, R.string.desc_signal, "get › signal"),
    Internet(R.string.kind_internet, R.string.desc_internet, "get › internet"),
    Apn(R.string.kind_apn, R.string.desc_apn, "get › apn"),
    Device(R.string.kind_device, R.string.desc_device, "get › device"),
    Wifi(R.string.kind_wifi, R.string.desc_wifi, "get › wifi"),
    Devices(R.string.kind_devices, R.string.desc_devices, "get › devices"),
    SmsInfo(R.string.kind_sms_info, R.string.desc_sms_info, "get › sms › info"),
    Syslog(R.string.kind_syslog, R.string.desc_syslog, "get › syslog"),
    Airtime(R.string.kind_airtime, R.string.desc_airtime, "get › airtime"),
    Power(R.string.kind_power, R.string.desc_power, "get › power"),
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
