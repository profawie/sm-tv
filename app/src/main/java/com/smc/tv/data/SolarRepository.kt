package com.smc.tv.data

import com.smc.tv.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

data class SolarStatus(
    val currentPowerW: Double,
    val todayKwh: Double,
    val totalKwh: Double,
    /** Battery charge, or null when the system has no battery reporting one. */
    val batteryPercent: Int?,
    val statusText: String?,
    val lastUpdate: String?,
)

/** Growatt refused the call because it came too soon after the last one (codes 100 and 102). */
class SolarRateLimited(message: String) : Exception(message)

/**
 * Growatt device data from the OpenAPI v4 "new-api", documented at
 * https://www.showdoc.com.cn/2540838290984246/0. It authenticates with a token applied for
 * through a Growatt OSS account, and works on device serial numbers rather than plants, so no
 * plant name or monthly energy is available. Malaysia is on the international server.
 */
class SolarRepository {
    private var cachedDevices: Pair<String, SolarDevices>? = null // token -> devices

    suspend fun status(token: String): SolarStatus {
        // Growatt answers 403 to Android's default "Dalvik" agent, so name the app instead.
        val headers = mapOf("token" to token.trim(), "User-Agent" to "SM-TV/${BuildConfig.VERSION_NAME}")
        val devices = cachedDevices?.takeIf { it.first == token }?.second
            ?: deviceList(headers).also { cachedDevices = token to it }

        val form = mapOf("deviceType" to devices.type, "deviceSn" to devices.serials.joinToString(","))
        val records = parseLastData(httpPostForm("$BASE/queryLastData", form, headers), devices.type)
        return summarize(records)
    }

    private suspend fun deviceList(headers: Map<String, String>): SolarDevices =
        parseDeviceList(httpPostForm("$BASE/queryDeviceList", mapOf("page" to "1"), headers))

    private companion object {
        const val BASE = "https://openapi.growatt.com/v4/new-api"
    }
}

/**
 * The inverters to read. queryLastData takes one device type per call and is limited to once
 * every 5 minutes, so a system mixing device types only reports the first type.
 */
data class SolarDevices(val type: String, val serials: List<String>)

internal fun parseDeviceList(body: String): SolarDevices {
    val list = checked(body).getJSONObject("data").getJSONArray("data").objects()
    val type = list.firstOrNull()?.getString("deviceType") ?: error("no devices on this account")
    return SolarDevices(
        type = type,
        serials = list.filter { it.getString("deviceType") == type }.map { it.getString("deviceSn") }.take(100),
    )
}

/** The docs show both `data.<type>: [..]` and `data.<serial>: {..}`, so accept either. */
internal fun parseLastData(body: String, deviceType: String): List<JSONObject> {
    val data = checked(body).optJSONObject("data") ?: return emptyList()
    data.optJSONArray(deviceType)?.let { return it.objects() }
    return data.keys().asSequence().mapNotNull(data::optJSONObject).toList()
}

/** Field names differ between device types, so each value tries the names the docs list. */
internal fun summarize(records: List<JSONObject>): SolarStatus {
    if (records.isEmpty()) error("no data from Growatt")
    return SolarStatus(
        currentPowerW = records.sumOf { it.number("ppv", "pac") ?: 0.0 },
        todayKwh = records.sumOf { it.number("eacToday", "powerToday", "epvToday", "etoday") ?: 0.0 },
        totalKwh = records.sumOf { it.number("eacTotal", "powerTotal", "epvTotal", "etotal") ?: 0.0 },
        // A missing battery reads 0, and types differ in which of these fields they fill in.
        batteryPercent = records.firstNotNullOfOrNull { r ->
            listOf("soc", "bmsSoc", "bmsSOC", "capacity").firstNotNullOfOrNull { name ->
                r.number(name)?.takeIf { it > 0 }
            }
        }?.toInt(),
        statusText = records.firstNotNullOfOrNull { r -> r.optString("statusText").takeIf { it.isNotBlank() } },
        lastUpdate = records.mapNotNull { r -> r.optString("time").takeIf { it.isNotBlank() } }.maxOrNull(),
    )
}

/** Failures come back as HTTP 200 with a non-zero `code`, or `error_code` on some calls. */
private fun checked(body: String): JSONObject {
    val json = JSONObject(body)
    val code = if (json.has("code")) json.optInt("code") else json.optInt("error_code")
    if (code == 0) return json
    val message = json.optString("message").ifBlank { json.optString("error_msg") }.ifBlank { "error $code" }
    if (code == 100 || code == 102) throw SolarRateLimited(message)
    error(message)
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull(::optJSONObject)

private fun JSONObject.number(vararg names: String): Double? =
    names.firstNotNullOfOrNull { name ->
        if (isNull(name)) null else optDouble(name).takeUnless(Double::isNaN)
    }
