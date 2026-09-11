package et.frectonz.kimem.core.model

import et.frectonz.kimem.R
import et.frectonz.kimem.core.codec.BuildTime
import et.frectonz.kimem.core.codec.RouterDateTime
import et.frectonz.kimem.core.codec.fmt2
import et.frectonz.kimem.core.codec.fmtSeconds
import et.frectonz.kimem.core.codec.humanSize
import et.frectonz.kimem.core.codec.truncateChars
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetRandomLogin(@SerialName("random_login") val randomLogin: String)

@Serializable
class LoginReply(@Serializable(with = LoginResultSerializer::class) val result: LoginResult)

@Serializable
class ActionReply(val result: String)

@Serializable
class Info(
    val msisdn: String,
    @SerialName("old_sim_num") @Serializable(with = Ucs2::class) val oldSimNum: String,
    @SerialName("network_type") val networkType: String,
    @SerialName("sim_spn") val simSpn: String,
    @SerialName("sim_plmn") val simPlmn: String,
    @SerialName("signalbar") @Serializable(with = StringInt::class) val signalBar: Int,
    @SerialName("ppp_status") @Serializable(with = PppStatusSerializer::class) val pppStatus: PppStatus,
    @SerialName("sms_unread_num") @Serializable(with = StringInt::class) val smsUnreadNum: Int,
    @SerialName("battery_percentage") @Serializable(with = StringInt::class) val batteryPercentage: Int,
    @SerialName("sta_count") @Serializable(with = StringInt::class) val staCount: Int,
) : Show {
    fun phoneNumber(): String? {
        if (msisdn.isNotEmpty()) return msisdn
        return oldSimNum.split(Regex("[^0-9]")).firstOrNull { it.length >= 9 }
    }

    private fun plmn(): UiText =
        if (simSpn.isEmpty()) plain(simPlmn) else res(R.string.plmn_with_operator, simSpn, simPlmn)

    override fun table() = Report.KeyValues(
        res(R.string.kind_info), rows(
            R.string.info_phone_number to orDash(phoneNumber() ?: ""),
            R.string.info_network_type to plain(networkType),
            R.string.info_plmn to plmn(),
            R.string.info_signal to res(R.string.signal_bars, signalBar),
            R.string.info_internet to pppStatus.label,
            R.string.info_unread_sms to plain(smsUnreadNum.toString()),
            R.string.info_battery to res(R.string.percent, batteryPercentage),
            R.string.info_connected_devices to plain(staCount.toString()),
        )
    )

    companion object {
        val CMDS = listOf(
            "msisdn", "old_sim_num", "network_type", "sim_spn", "sim_plmn", "signalbar",
            "ppp_status", "sms_unread_num", "battery_percentage", "sta_count",
        )
    }
}

@Serializable
class SystemInfo(
    @SerialName("mem_total") @Serializable(with = Kibibytes::class) val memTotalKib: Long,
    @SerialName("mem_free") @Serializable(with = Kibibytes::class) val memFreeKib: Long,
    @SerialName("mem_cached") @Serializable(with = Kibibytes::class) val memCachedKib: Long,
    @SerialName("mem_active") @Serializable(with = Kibibytes::class) val memActiveKib: Long,
    @SerialName("tz_cpu_usage") @Serializable(with = Percent::class) val cpuUsagePercent: Double,
    @SerialName("tz_flash_use") @Serializable(with = StringDouble::class) val flashUseMb: Double,
    @SerialName("tz_flash_total") @Serializable(with = StringDouble::class) val flashTotalMb: Double,
) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_system), rows(
            R.string.system_memory_total to plain(humanSize(memTotalKib * 1024, binary = true)),
            R.string.system_memory_free to plain(humanSize(memFreeKib * 1024, binary = true)),
            R.string.system_memory_cached to plain(humanSize(memCachedKib * 1024, binary = true)),
            R.string.system_memory_active to plain(humanSize(memActiveKib * 1024, binary = true)),
            R.string.system_cpu_usage to res(R.string.percent_2f, fmt2(cpuUsagePercent)),
            R.string.system_flash_usage to res(
                R.string.of,
                res(R.string.megabytes, fmt2(flashUseMb)),
                res(R.string.megabytes, fmt2(flashTotalMb)),
            ),
        )
    )

    companion object {
        val CMDS = listOf(
            "mem_total", "mem_free", "mem_cached", "mem_active",
            "tz_cpu_usage", "tz_flash_use", "tz_flash_total",
        )
    }
}

@Serializable
class Internet(
    @SerialName("ppp_status") @Serializable(with = PppStatusSerializer::class) val pppStatus: PppStatus,
    @SerialName("wan_ipaddr") val wanIpAddr: String,
    @SerialName("LocalDomain") val lanDomain: String,
    @SerialName("sim_imsi") val simImsi: String,
    @SerialName("realtime_rx_thrpt") @Serializable(with = StringLong::class) val realtimeRxThrpt: Long,
    @SerialName("realtime_tx_thrpt") @Serializable(with = StringLong::class) val realtimeTxThrpt: Long,
    @SerialName("realtime_rx_bytes") @Serializable(with = StringLong::class) val realtimeRxBytes: Long,
    @SerialName("realtime_tx_bytes") @Serializable(with = StringLong::class) val realtimeTxBytes: Long,
    @SerialName("monthly_rx_bytes") @Serializable(with = StringLong::class) val monthlyRxBytes: Long,
    @SerialName("monthly_tx_bytes") @Serializable(with = StringLong::class) val monthlyTxBytes: Long,
    @SerialName("monthly_time") @Serializable(with = StringLong::class) val monthlyTime: Long,
) {
    companion object {
        val CMDS = listOf(
            "ppp_status", "wan_ipaddr", "LocalDomain", "sim_imsi",
            "realtime_rx_thrpt", "realtime_tx_thrpt", "realtime_rx_bytes", "realtime_tx_bytes",
            "monthly_rx_bytes", "monthly_tx_bytes", "monthly_time",
        )
    }
}

@Serializable
class WanDetails(
    val gateway: String,
    val netmask: String,
    @Serializable(with = DnsList::class) val dns: List<String>,
) {
    companion object {
        const val CMD = "home_get"
    }
}

class InternetReport(val internet: Internet, val wan: WanDetails) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_internet), rows(
            R.string.internet_status to internet.pppStatus.label,
            R.string.internet_wan_ip to orDash(internet.wanIpAddr),
            R.string.internet_gateway to orDash(wan.gateway),
            R.string.internet_netmask to orDash(wan.netmask),
            R.string.internet_dns to fmtDns(wan.dns),
            R.string.internet_lan_domain to plain(internet.lanDomain),
            R.string.internet_imsi to plain(internet.simImsi),
            R.string.internet_download_speed to res(R.string.per_second, humanSize(internet.realtimeRxThrpt)),
            R.string.internet_upload_speed to res(R.string.per_second, humanSize(internet.realtimeTxThrpt)),
            R.string.internet_realtime_rx to plain(humanSize(internet.realtimeRxBytes)),
            R.string.internet_realtime_tx to plain(humanSize(internet.realtimeTxBytes)),
            R.string.internet_monthly_rx to plain(humanSize(internet.monthlyRxBytes)),
            R.string.internet_monthly_tx to plain(humanSize(internet.monthlyTxBytes)),
            R.string.internet_monthly_online to plain(fmtSeconds(internet.monthlyTime)),
        )
    )
}

@Serializable
class Wifi(
    @SerialName("SSID1") val ssid: String,
    @SerialName("HideSSID") @Serializable(with = Flag::class) val ssidHidden: Boolean,
    @SerialName("wifi_mac") val wifiMac: String,
    @SerialName("MAX_Station_num") @Serializable(with = StringInt::class) val maxStationNum: Int,
    @SerialName("Channel_cur") @Serializable(with = StringInt::class) val channel: Int,
    @SerialName("wifi_11n_cap") @Serializable(with = ChannelBandwidthSerializer::class) val channelBandwidth: ChannelBandwidth,
    @SerialName("AuthMode") val authMode: String,
    @SerialName("WPAPSK1_encode") @Serializable(with = B64::class) val password: String,
    @SerialName("m_SSID") val guestSsid: String,
    @SerialName("m_ssid_enable") @Serializable(with = Flag::class) val guestSsidEnabled: Boolean,
    @SerialName("dhcpStart") val dhcpStart: String,
    @SerialName("dhcpEnd") val dhcpEnd: String,
) : Show {
    private fun guest(): UiText {
        if (guestSsid.isEmpty()) return res(R.string.dash)
        val state = res(if (guestSsidEnabled) R.string.enabled else R.string.disabled)
        return res(R.string.plmn_with_operator, guestSsid, state)
    }

    override fun table() = Report.KeyValues(
        res(R.string.kind_wifi), rows(
            R.string.wifi_ssid to plain(ssid),
            R.string.wifi_ssid_visible to yesNo(!ssidHidden),
            R.string.wifi_mac to plain(wifiMac),
            R.string.wifi_max_stations to plain(maxStationNum.toString()),
            R.string.wifi_channel to plain(channel.toString()),
            R.string.wifi_channel_bandwidth to channelBandwidth.label,
            R.string.wifi_auth_mode to plain(authMode),
            R.string.wifi_password to plain(password),
            R.string.wifi_guest_ssid to guest(),
            R.string.wifi_dhcp_start to plain(dhcpStart),
            R.string.wifi_dhcp_end to plain(dhcpEnd),
        )
    )

    companion object {
        val CMDS = listOf(
            "SSID1", "HideSSID", "wifi_mac", "MAX_Station_num", "Channel_cur", "wifi_11n_cap",
            "AuthMode", "WPAPSK1_encode", "m_SSID", "m_ssid_enable", "dhcpStart", "dhcpEnd",
        )
    }
}

@Serializable
class Power(
    @SerialName("battery_exist") @Serializable(with = Flag::class) val batteryExist: Boolean,
    @SerialName("battery_percentage") @Serializable(with = StringInt::class) val batteryPercentage: Int,
    @SerialName("battery_value") @Serializable(with = StringInt::class) val batteryValue: Int,
    @SerialName("power_exist") @Serializable(with = Flag::class) val powerExist: Boolean,
) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_power), rows(
            R.string.power_battery_present to yesNo(batteryExist),
            R.string.power_battery_percentage to res(R.string.percent, batteryPercentage),
            R.string.power_battery_power to plain(batteryValue.toString()),
            R.string.power_connected to yesNo(powerExist),
        )
    )

    companion object {
        val CMDS = listOf("battery_exist", "battery_percentage", "battery_value", "power_exist")
    }
}

@Serializable
class CellExtras(
    @SerialName("lte_tac") val lteTac: String,
    @SerialName("nv_arfcn") val nvArfcn: String,
) {
    companion object {
        val CMDS = listOf("lte_tac", "nv_arfcn")
    }
}

@Serializable
class ConnectedDevice(
    val hostname: String,
    @SerialName("ip_addr") val ipAddr: String,
    @SerialName("mac_addr") val macAddr: String,
    @SerialName("dev_type") val devType: String,
    @SerialName("ip_type") val ipType: String,
)

@Serializable
class StationList(@SerialName("station_list") val devices: List<ConnectedDevice>) : Show {
    override fun table() = Report.Records(
        res(R.string.kind_devices),
        listOf(
            res(R.string.devices_hostname),
            res(R.string.devices_ip),
            res(R.string.devices_mac),
            res(R.string.devices_type),
            res(R.string.devices_ip_type),
        ),
        devices.map { listOf(orDash(it.hostname), plain(it.ipAddr), plain(it.macAddr), plain(it.devType), plain(it.ipType)) },
    )

    companion object {
        const val CMD = "station_list"
    }
}

@Serializable
class Message(
    @Serializable(with = StringInt::class) val id: Int,
    val number: String,
    @Serializable(with = Ucs2::class) val content: String,
    @Serializable(with = MessageStatusSerializer::class) val tag: MessageStatus,
    @Serializable(with = RouterDateTimeSerializer::class) val date: RouterDateTime,
) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.message_title), rows(
            R.string.message_id to plain(id.toString()),
            R.string.message_number to plain(number),
            R.string.message_status to tag.label,
            R.string.message_date to plain(date.display()),
            R.string.message_content to plain(content.trim()),
        )
    )
}

@Serializable
class SmsInbox(val messages: List<Message>) : Show {
    override fun table() = Report.Records(
        res(R.string.kind_sms_info),
        listOf(
            res(R.string.message_id),
            res(R.string.message_number),
            res(R.string.message_content),
            res(R.string.message_status),
            res(R.string.message_date),
        ),
        messages.map {
            listOf(
                plain(it.id.toString()),
                plain(it.number),
                plain(truncateChars(it.content.trim().replace('\n', ' '), 24)),
                it.tag.label,
                plain(it.date.display()),
            )
        },
    )

    companion object {
        const val CMD = "sms_data_total"
        val PARAMS = mapOf(
            "page" to "0",
            "data_per_page" to "500",
            "mem_store" to "1",
            "tags" to "10",
            "order_by" to "order by id asc",
        )
    }
}

@Serializable
class SmsCapacity(
    @SerialName("sms_nv_total") @Serializable(with = StringInt::class) val nvTotal: Int,
    @SerialName("sms_nv_rev_total") @Serializable(with = StringInt::class) val nvRevTotal: Int,
    @SerialName("sms_nv_send_total") @Serializable(with = StringInt::class) val nvSendTotal: Int,
    @SerialName("sms_nv_draftbox_total") @Serializable(with = StringInt::class) val nvDraftboxTotal: Int,
) {
    fun used(): Int = nvRevTotal + nvSendTotal + nvDraftboxTotal

    companion object {
        const val CMD = "sms_capacity_info"
    }
}

@Serializable
class SmsParameters(
    @SerialName("sms_para_sca") val sca: String,
    @SerialName("sms_para_status_report") @Serializable(with = Flag::class) val statusReport: Boolean,
    @SerialName("default_store") @Serializable(with = SmsStoreSerializer::class) val defaultStore: SmsStore,
) {
    companion object {
        const val CMD = "sms_parameter_info"
    }
}

class SmsSettings(val capacity: SmsCapacity, val parameters: SmsParameters) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_sms_info), rows(
            R.string.sms_messages to res(R.string.of, capacity.used().toString(), capacity.nvTotal.toString()),
            R.string.sms_smsc to orDash(parameters.sca),
            R.string.sms_delivery_reports to yesNo(parameters.statusReport),
            R.string.sms_default_store to parameters.defaultStore.label,
        )
    )
}

@Serializable
class AirtimeBalance(@SerialName("airtime_balance") @Serializable(with = Ucs2::class) val text: String) : Show {
    private fun amount(): String? {
        val index = text.indexOf("Br.")
        if (index < 0) return null
        val after = text.substring(index + 3).trimStart()
        val end = after.indexOfFirst { !(it in '0'..'9' || it == ',' || it == '.') }.let { if (it < 0) after.length else it }
        return after.substring(0, end).trimEnd('.').ifEmpty { null }
    }

    fun balance(): UiText? = amount()?.let { res(R.string.birr, it) }

    fun balanceBirr(): Double? = amount()?.replace(",", "")?.toDoubleOrNull()

    override fun table() = Report.KeyValues(
        res(R.string.kind_airtime),
        rows(R.string.airtime_balance to (balance() ?: orDash(text.trim()))),
    )

    companion object {
        const val CMD = "airtime_balance"
    }
}

@Serializable
class Signal(
    @Serializable(with = OptionalDouble::class) val rsrp: Double?,
    @Serializable(with = OptionalDouble::class) val rsrq: Double?,
    @Serializable(with = OptionalDouble::class) val rssi: Double?,
    @Serializable(with = OptionalDouble::class) val sinr: Double?,
    val band: String,
    @SerialName("cell_id") val cellId: String,
    @SerialName("enode_id") val enodeId: String,
    @SerialName("dwCellId") val fullCellId: String,
    @SerialName("phy_cell_id") val phyCellId: String,
    val mcs: String,
    val cqi: String,
) {
    companion object {
        const val CMD = "system_status"
    }
}

class SignalReport(val signal: Signal, val cell: CellExtras) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_signal), rows(
            R.string.signal_rsrp to fmtDbm(signal.rsrp),
            R.string.signal_rsrq to fmtDecibels(signal.rsrq),
            R.string.signal_rssi to fmtDbm(signal.rssi),
            R.string.signal_sinr to fmtDecibels(signal.sinr),
            R.string.signal_band to orDash(signal.band),
            R.string.signal_earfcn to orDash(cell.nvArfcn),
            R.string.signal_tac to orDash(cell.lteTac),
            R.string.signal_cell_id to orDash(signal.cellId),
            R.string.signal_enodeb_id to orDash(signal.enodeId),
            R.string.signal_full_cell_id to orDash(signal.fullCellId),
            R.string.signal_pci to orDash(signal.phyCellId),
            R.string.signal_mcs to orDash(signal.mcs),
            R.string.signal_cqi to orDash(signal.cqi),
        )
    )
}

@Serializable
class Device(
    @SerialName("device_version") val deviceVersion: String,
    @SerialName("real_device_version") val realDeviceVersion: String,
    @SerialName("build_time") @Serializable(with = BuildTimeSerializer::class) val buildTime: BuildTime,
    @SerialName("platform_version") val platformVersion: String,
    val sn: String,
    val imei: String,
    @SerialName("eth0_mac") val eth0Mac: String,
    @SerialName("online_time") @Serializable(with = StringLong::class) val onlineTime: Long,
) {
    companion object {
        const val CMD = "home_get"
    }
}

@Serializable
class SimIccid(val ziccid: String) {
    companion object {
        const val CMD = "ziccid"
    }
}

class DeviceReport(val device: Device, val iccid: String) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_device), rows(
            R.string.device_firmware to plain(device.deviceVersion),
            R.string.device_build to plain(device.realDeviceVersion),
            R.string.device_build_time to plain(device.buildTime.display()),
            R.string.device_platform to plain(device.platformVersion),
            R.string.device_sn to plain(device.sn),
            R.string.device_imei to plain(device.imei),
            R.string.device_eth_mac to plain(device.eth0Mac),
            R.string.device_iccid to orDash(iccid),
            R.string.device_uptime to plain(fmtSeconds(device.onlineTime)),
        )
    )
}

@Serializable
class ApnStatus(@SerialName("apn_mode") val apnMode: String) {
    companion object {
        const val CMD = "apn_mode"
    }
}

class ApnProfile(
    val profileName: String,
    val apn: String,
    val pdpType: String,
    val authMode: String,
    val username: String,
    val password: String,
    val dnsMode: String,
)

@Serializable
class ApnConfig(@SerialName("APN_config0") @Serializable(with = ApnProfileSerializer::class) val profile: ApnProfile) {
    companion object {
        const val CMD = "APN_config0"
    }
}

class ApnReport(val mode: String, val profile: ApnProfile) : Show {
    override fun table() = Report.KeyValues(
        res(R.string.kind_apn), rows(
            R.string.apn_mode to plain(mode),
            R.string.apn_profile to orDash(profile.profileName),
            R.string.apn_apn to orDash(profile.apn),
            R.string.apn_pdp_type to orDash(profile.pdpType),
            R.string.apn_auth_mode to orDash(profile.authMode),
            R.string.apn_username to orDash(profile.username),
            R.string.apn_password to orDash(profile.password),
            R.string.apn_dns_mode to orDash(profile.dnsMode),
        )
    )
}

@Serializable
class UssdWriteFlag(@SerialName("ussd_write_flag") @Serializable(with = UssdFlagSerializer::class) val flag: UssdFlag) {
    companion object {
        const val CMD = "ussd_write_flag"
    }
}

@Serializable
class UssdData(@SerialName("ussd_data") @Serializable(with = Ucs2::class) val text: String) {
    companion object {
        const val CMD = "ussd_data_info"
    }
}
