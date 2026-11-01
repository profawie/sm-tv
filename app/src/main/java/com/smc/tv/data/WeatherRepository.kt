package com.smc.tv.data

import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate

data class DailyForecast(val date: LocalDate, val code: Int, val min: Double, val max: Double)

data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windKmh: Double,
    val code: Int,
    val isDay: Boolean,
    val daily: List<DailyForecast>,
)

/**
 * Weather from Open-Meteo (free, no API key).
 *
 * The MSN Weather link in the brief is a Power Automate connector, which only works
 * inside Microsoft Power Platform flows, so it can't be called from an Android app.
 */
class WeatherRepository {
    suspend fun current(location: Location): Weather {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=${location.latitude}&longitude=${location.longitude}" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=Asia%2FKuala_Lumpur&forecast_days=4"
        val json = JSONObject(httpGet(url))
        val cur = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        return Weather(
            temperature = cur.getDouble("temperature_2m"),
            feelsLike = cur.getDouble("apparent_temperature"),
            humidity = cur.getInt("relative_humidity_2m"),
            windKmh = cur.getDouble("wind_speed_10m"),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            daily = (0 until dates.length()).map { i ->
                DailyForecast(
                    date = LocalDate.parse(dates.getString(i)),
                    code = daily.getJSONArray("weather_code").getInt(i),
                    min = daily.getJSONArray("temperature_2m_min").getDouble(i),
                    max = daily.getJSONArray("temperature_2m_max").getDouble(i),
                )
            },
        )
    }

    /** Place search for the Settings location picker. */
    suspend fun searchPlaces(query: String): List<Location> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(
            httpGet("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=ms")
        )
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            val label = listOfNotNull(
                r.getString("name"),
                r.optString("admin2").takeIf { it.isNotBlank() },
                r.optString("admin1").takeIf { it.isNotBlank() },
                r.optString("country").takeIf { it.isNotBlank() },
            ).distinct().joinToString(", ")
            Location(label, r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }
}

/** WMO weather code -> Malay description. */
fun weatherLabel(code: Int): String = when (code) {
    0 -> "Cerah"
    1 -> "Cerah berawan"
    2 -> "Berawan separa"
    3 -> "Mendung"
    45, 48 -> "Berkabus"
    51, 53, 55, 56, 57 -> "Gerimis"
    61, 63, 66, 80, 81 -> "Hujan"
    65, 67, 82 -> "Hujan lebat"
    71, 73, 75, 77, 85, 86 -> "Salji"
    95 -> "Ribut petir"
    96, 99 -> "Ribut petir & hujan batu"
    else -> "—"
}

/** WMO weather code -> emoji glyph for the widget. */
fun weatherIcon(code: Int, isDay: Boolean = true): String = when (code) {
    0 -> if (isDay) "☀️" else "🌙"
    1, 2 -> if (isDay) "🌤️" else "☁️"
    3 -> "☁️"
    45, 48 -> "🌫️"
    51, 53, 55, 56, 57, 61, 63, 66, 80, 81 -> "🌧️"
    65, 67, 82 -> "🌧️"
    71, 73, 75, 77, 85, 86 -> "❄️"
    95, 96, 99 -> "⛈️"
    else -> "🌡️"
}
