package et.frectonz.kimem

import et.frectonz.kimem.core.codec.BuildTime
import et.frectonz.kimem.core.codec.RouterDateTime
import et.frectonz.kimem.core.codec.b64Decode
import et.frectonz.kimem.core.codec.b64Encode
import et.frectonz.kimem.core.codec.fmtNum
import et.frectonz.kimem.core.codec.fmtSeconds
import et.frectonz.kimem.core.codec.humanSize
import et.frectonz.kimem.core.codec.joinMsgIds
import et.frectonz.kimem.core.codec.sha256Hex
import et.frectonz.kimem.core.codec.truncateChars
import et.frectonz.kimem.core.codec.ucs2Decode
import et.frectonz.kimem.core.codec.ucs2Encode
import et.frectonz.kimem.core.model.MsgSelector
import et.frectonz.kimem.core.model.resolveUssdCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CodecTest {
    @Test
    fun ucs2RoundTrip() {
        assertEquals("Hi", ucs2Decode("00480069"))
        assertEquals("00480069", ucs2Encode("Hi"))
        assertEquals("ሰላም", ucs2Decode(ucs2Encode("ሰላም")))
    }

    @Test
    fun ucs2DropsCarriageReturns() {
        assertEquals("a\nb", ucs2Decode(ucs2Encode("a\r\nb")))
    }

    @Test
    fun ucs2RejectsOddLength() {
        assertThrows(IllegalArgumentException::class.java) { ucs2Decode("004") }
    }

    @Test
    fun base64AndSha256() {
        assertEquals("YWRtaW4=", b64Encode("admin"))
        assertEquals("4CCE8C57", b64Decode("NENDRThDNTc="))
        assertEquals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", sha256Hex("admin"))
    }

    @Test
    fun humanSizes() {
        assertEquals("476 B", humanSize(476))
        assertEquals("1.24 GB", humanSize(1_237_026_476))
        assertEquals("1 MB", humanSize(1_000_000))
        assertEquals("23.83 MiB", humanSize(24_988_672, binary = true))
    }

    @Test
    fun seconds() {
        assertEquals("2h 54m 36s", fmtSeconds(10_476))
        assertEquals("0s", fmtSeconds(0))
        assertEquals("1d 1s", fmtSeconds(86_401))
    }

    @Test
    fun numbersLikeRust() {
        assertEquals("-95", fmtNum(-95.0))
        assertEquals("-9.5", fmtNum(-9.5))
    }

    @Test
    fun routerTimestampsIgnoreTheirTimezone() {
        assertEquals("2026-06-05 01:06:24 PM", RouterDateTime.parse("26,06,05,13,06,24,+8")!!.display())
        assertEquals("2026-06-05 01:06:24 PM", RouterDateTime.parse("26,06,05,13,06,24,+12")!!.display())
        assertNull(RouterDateTime.parse("garbage"))
    }

    @Test
    fun buildTime() {
        assertEquals("2025-02-19 13:59", BuildTime.parse("2025-02-19_13:59")!!.display())
        assertNull(BuildTime.parse("nope"))
    }

    @Test
    fun ussdCodes() {
        assertEquals("*704#", resolveUssdCode("balance"))
        assertEquals("*777#", resolveUssdCode("MENU"))
        assertEquals("*123*4#", resolveUssdCode("*123*4#"))
        assertNull(resolveUssdCode("nonsense"))
    }

    @Test
    fun selectors() {
        assertEquals(MsgSelector.All, MsgSelector.parse("ALL"))
        assertEquals(MsgSelector.Id(715), MsgSelector.parse(" 715 "))
        assertNull(MsgSelector.parse("seven"))
    }

    @Test
    fun misc() {
        assertEquals("1;2;3;", joinMsgIds(listOf(1, 2, 3)))
        assertEquals("hello…", truncateChars("hello world", 6))
        assertEquals("hi", truncateChars("hi", 6))
    }
}
