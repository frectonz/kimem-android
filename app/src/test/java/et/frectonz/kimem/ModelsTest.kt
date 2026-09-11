package et.frectonz.kimem

import et.frectonz.kimem.core.model.AirtimeBalance
import et.frectonz.kimem.core.model.ApnConfig
import et.frectonz.kimem.core.model.Info
import et.frectonz.kimem.core.model.MessageStatus
import et.frectonz.kimem.core.model.PppStatus
import et.frectonz.kimem.core.model.Signal
import et.frectonz.kimem.core.model.SmsInbox
import et.frectonz.kimem.core.model.Wifi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private inline fun <reified T> decode(text: String): T = json.decodeFromJsonElement(serializer<T>(), json.parseToJsonElement(text))

    @Test
    fun infoDecodesStringlyFields() {
        val info = decode<Info>(
            """{"msisdn":"","old_sim_num":"00570065006c0063006f006d0065003a0020003200350031003700300030003000300030003000300030",
               "network_type":"LTE","sim_spn":"Safaricom","sim_plmn":"63602","signalbar":"2","ppp_status":"ppp_connected",
               "sms_unread_num":"1","battery_percentage":"81","sta_count":"3","extra":"ignored"}"""
        )
        assertEquals("251700000000", info.phoneNumber())
        assertEquals(2, info.signalBar)
        assertTrue(info.pppStatus is PppStatus.Connected)
    }

    @Test
    fun duplicateKeysKeepTheLastValue() {
        val info = decode<Info>(
            """{"msisdn":"first","msisdn":"second","old_sim_num":"","network_type":"","sim_spn":"","sim_plmn":"",
               "signalbar":"0","ppp_status":"x","sms_unread_num":"0","battery_percentage":"0","sta_count":"0"}"""
        )
        assertEquals("second", info.msisdn)
    }

    @Test
    fun badNumberFailsDecoding() {
        assertThrows(SerializationException::class.java) {
            decode<Info>(
                """{"msisdn":"","old_sim_num":"","network_type":"","sim_spn":"","sim_plmn":"","signalbar":"two",
                   "ppp_status":"x","sms_unread_num":"0","battery_percentage":"0","sta_count":"0"}"""
            )
        }
    }

    @Test
    fun wifiDecodesFlagsAndBase64() {
        val wifi = decode<Wifi>(
            """{"SSID1":"Kimem","HideSSID":"0","wifi_mac":"AA","MAX_Station_num":"10","Channel_cur":"6","wifi_11n_cap":"1",
               "AuthMode":"WPA2PSK","WPAPSK1_encode":"NENDRThDNTc=","m_SSID":"","m_ssid_enable":"0","dhcpStart":"a","dhcpEnd":"b"}"""
        )
        assertEquals("4CCE8C57", wifi.password)
        assertEquals(false, wifi.ssidHidden)
    }

    @Test
    fun signalLeavesEmptyMetricsNull() {
        val signal = decode<Signal>(
            """{"rsrp":"-95","rsrq":"","rssi":"-60.5","sinr":"","band":"3","cell_id":"","enode_id":"","dwCellId":"",
               "phy_cell_id":"","mcs":"","cqi":""}"""
        )
        assertEquals(-95.0, signal.rsrp!!, 0.0)
        assertNull(signal.rsrq)
    }

    @Test
    fun inboxDecodesMessages() {
        val inbox = decode<SmsInbox>(
            """{"messages":[{"id":"898","number":"Safaricom","content":"00480069","tag":"1","date":"26,09,10,23,06,58,+8"}]}"""
        )
        val message = inbox.messages.single()
        assertEquals(898, message.id)
        assertEquals("Hi", message.content)
        assertTrue(message.tag is MessageStatus.Unread)
        assertEquals("2026-09-10 11:06:58 PM", message.date.display())
    }

    @Test
    fun apnProfileSplitsOnMarker() {
        val config = decode<ApnConfig>("""{"APN_config0":"Safaricom(${'$'})internet(${'$'})x(${'$'})x(${'$'})none(${'$'})(${'$'})(${'$'})IP(${'$'})x(${'$'})x(${'$'})auto(${'$'})tail"}""")
        assertEquals("Safaricom", config.profile.profileName)
        assertEquals("IP", config.profile.pdpType)
        assertEquals("auto", config.profile.dnsMode)
    }

    @Test
    fun airtimeAmount() {
        val balance = AirtimeBalance("Airtime balance Br. 2,247.50. SMS will be sent.\n1. 60 Min @20 birr")
        assertEquals(2247.50, balance.balanceBirr()!!, 0.0001)
        assertNull(AirtimeBalance("Your request is being processed").balanceBirr())
    }
}
