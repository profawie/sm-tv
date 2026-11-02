package com.smc.tv.data

import org.json.JSONObject

data class SolarStatus(
    val plantName: String,
    val currentPowerW: Double,
    val todayKwh: Double,
    val monthKwh: Double,
    val totalKwh: Double,
    val lastUpdate: String?,
)

/**
 * Growatt plant data from the official OpenAPI (https://openapi.growatt.com/v1), which
 * authenticates with an API token from the ShineServer website. The old username/password
 * endpoints used by ShinePhone reject third-party clients, so they aren't used here.
 */
class SolarRepository {
    private var cachedPlant: Pair<String, Pair<String, String>>? = null // token -> (id, name)

    suspend fun status(token: String): SolarStatus {
        val headers = mapOf("token" to token.trim())
        val (plantId, plantName) = cachedPlant?.takeIf { it.first == token }?.second
            ?: firstPlant(headers).also { cachedPlant = token to it }

        val data = checked(httpGet("$BASE/plant/data?plant_id=$plantId", headers)).getJSONObject("data")
        return SolarStatus(
            plantName = plantName,
            currentPowerW = data.optDouble("current_power", 0.0),
            todayKwh = data.optDouble("today_energy", 0.0),
            monthKwh = data.optDouble("monthly_energy", 0.0),
            totalKwh = data.optDouble("total_energy", 0.0),
            lastUpdate = data.optString("last_update_time").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    private suspend fun firstPlant(headers: Map<String, String>): Pair<String, String> {
        val plants = checked(httpGet("$BASE/plant/list", headers))
            .getJSONObject("data").getJSONArray("plants")
        if (plants.length() == 0) error("no plants on this account")
        val plant = plants.getJSONObject(0)
        return plant.getString("plant_id") to plant.optString("name", "Solar")
    }

    /** Growatt returns HTTP 200 with error_code != 0 on failure (e.g. 10011 = bad token). */
    private fun checked(body: String): JSONObject {
        val json = JSONObject(body)
        val code = json.optInt("error_code", 0)
        if (code != 0) error(json.optString("error_msg", "error $code"))
        return json
    }

    private companion object {
        const val BASE = "https://openapi.growatt.com/v1"
    }
}
