package com.smc.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Samples are trimmed from the examples in the Growatt OpenAPI v4 docs. */
class SolarParsingTest {
    @Test
    fun `device list keeps the first device type`() {
        val body = """{"code":0,"data":{"pages":1,"count":3,"data":[
            {"deviceType":"sph","deviceSn":"AAA"},
            {"deviceType":"inv","deviceSn":"BBB"},
            {"deviceType":"sph","deviceSn":"CCC"}]},"message":"SUCCESSFUL_OPERATION"}"""
        assertEquals(SolarDevices("sph", listOf("AAA", "CCC")), parseDeviceList(body))
    }

    @Test
    fun `empty device list is an error`() {
        val body = """{"code":0,"data":{"count":0,"data":[]},"message":"ok"}"""
        assertThrows(IllegalStateException::class.java) { parseDeviceList(body) }
    }

    @Test
    fun `reads an sph hybrid from the type keyed shape`() {
        val body = """{"code":0,"data":{"sph":[{"serialNum":"OZD0849010","ppv":1318.9,"pac":1258.1,
            "eacToday":8.6,"eacTotal":1714.4,"epvTotal":1953.4,"soc":100,"bmsSOC":0,
            "time":"2024-05-25 14:35:31","statusText":"PV Bat Online"}]},"message":"ok"}"""
        val status = summarize(parseLastData(body, "sph"))
        assertEquals(1318.9, status.currentPowerW, 0.001)
        assertEquals(8.6, status.todayKwh, 0.001)
        assertEquals(1714.4, status.totalKwh, 0.001)
        assertEquals(100, status.batteryPercent)
        assertEquals("PV Bat Online", status.statusText)
        assertEquals("2024-05-25 14:35:31", status.lastUpdate)
    }

    @Test
    fun `adds up inverters from the serial keyed shape`() {
        val body = """{"error_msg":"","data":{
            "UMJ0CC4025":{"ppv":29441.1,"pac":28952.7,"eacToday":286.1,"eacTotal":276507.5,"powerToday":0,
                "time":"2024-02-29 15:31:03","statusText":"Normal"},
            "UMJ0CC4022":{"ppv":34983.7,"pac":34260.4,"eacToday":265.4,"eacTotal":255544.3,"powerToday":0,
                "time":"2024-02-29 15:33:04","statusText":"Normal"}},"error_code":0}"""
        val status = summarize(parseLastData(body, "max"))
        assertEquals(64424.8, status.currentPowerW, 0.001)
        assertEquals(551.5, status.todayKwh, 0.001)
        assertNull(status.batteryPercent)
        assertEquals("2024-02-29 15:33:04", status.lastUpdate)
    }

    @Test
    fun `inv and storage use their own field names`() {
        val inv = """{"code":0,"data":{"inv":[{"ppv":1111.9,"pac":936.6,"powerToday":15.7,"powerTotal":130.3,
            "epvTotal":139.3}]},"message":"ok"}"""
        val invStatus = summarize(parseLastData(inv, "inv"))
        assertEquals(15.7, invStatus.todayKwh, 0.001)
        assertEquals(130.3, invStatus.totalKwh, 0.001)

        val storage = """{"code":0,"data":{"storage":[{"ppv":0.0,"capacity":90,"epvToday":12.9,"epvTotal":729.6,
            "etoday":12.9,"etotal":729.6}]},"message":"ok"}"""
        val storageStatus = summarize(parseLastData(storage, "storage"))
        assertEquals(12.9, storageStatus.todayKwh, 0.001)
        assertEquals(729.6, storageStatus.totalKwh, 0.001)
        assertEquals(90, storageStatus.batteryPercent)
    }

    @Test
    fun `numbers sent as strings still parse`() {
        val body = """{"code":0,"data":{"min":[{"ppv":"112.9","eacToday":"2.0","eacTotal":"25799.8"}]},"message":"ok"}"""
        assertEquals(112.9, summarize(parseLastData(body, "min")).currentPowerW, 0.001)
    }

    @Test
    fun `frequency limit is told apart from other errors`() {
        val limited = """{"code":102,"data":null,"message":"Access Frequency Limit"}"""
        assertThrows(SolarRateLimited::class.java) { parseLastData(limited, "sph") }

        val badToken = """{"code":2,"data":null,"message":"Invalid Secret Token"}"""
        val e = assertThrows(IllegalStateException::class.java) { parseDeviceList(badToken) }
        assertEquals("Invalid Secret Token", e.message)
    }
}
