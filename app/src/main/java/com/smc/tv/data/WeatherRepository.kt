package com.smc.tv.data

import org.json.JSONObject
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime

data class DailyForecast(
    val date: LocalDate,
    val code: Int,
    val min: Double,
    val max: Double,
    val sunrise: LocalDateTime,
    val sunset: LocalDateTime,
    val uvMax: Double,
    val rainMm: Double,
    val rainChance: Int,
)

data class HourlyForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val code: Int,
    val rainChance: Int,
    val isDay: Boolean,
)

data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windKmh: Double,
    val windDirection: Int,
    val pressureHpa: Double,
    val visibilityKm: Double,
    val cloudCover: Int,
    val uvIndex: Double,
    val code: Int,
    val isDay: Boolean,
    val hourly: List<HourlyForecast>,
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
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,is_day," +
            "wind_speed_10m,wind_direction_10m,surface_pressure,visibility,cloud_cover,uv_index" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,is_day&forecast_hours=24" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
            "precipitation_sum,precipitation_probability_max" +
            "&timezone=Asia%2FKuala_Lumpur&forecast_days=4"
        val json = JSONObject(httpGet(url))
        val cur = json.getJSONObject("current")
        val hourly = json.getJSONObject("hourly")
        val daily = json.getJSONObject("daily")
        fun JSONObject.ints(key: String, i: Int) = getJSONArray(key).optInt(i)
        fun JSONObject.doubles(key: String, i: Int) = getJSONArray(key).optDouble(i, 0.0)
        return Weather(
            temperature = cur.getDouble("temperature_2m"),
            feelsLike = cur.getDouble("apparent_temperature"),
            humidity = cur.getInt("relative_humidity_2m"),
            windKmh = cur.getDouble("wind_speed_10m"),
            windDirection = cur.optInt("wind_direction_10m"),
            pressureHpa = cur.optDouble("surface_pressure", 0.0),
            visibilityKm = cur.optDouble("visibility", 0.0) / 1000,
            cloudCover = cur.optInt("cloud_cover"),
            uvIndex = cur.optDouble("uv_index", 0.0),
            code = cur.getInt("weather_code"),
            isDay = cur.getInt("is_day") == 1,
            hourly = (0 until hourly.getJSONArray("time").length()).map { i ->
                HourlyForecast(
                    time = LocalDateTime.parse(hourly.getJSONArray("time").getString(i)),
                    temperature = hourly.doubles("temperature_2m", i),
                    code = hourly.ints("weather_code", i),
                    rainChance = hourly.ints("precipitation_probability", i),
                    isDay = hourly.ints("is_day", i) == 1,
                )
            },
            daily = (0 until daily.getJSONArray("time").length()).map { i ->
                DailyForecast(
                    date = LocalDate.parse(daily.getJSONArray("time").getString(i)),
                    code = daily.ints("weather_code", i),
                    min = daily.doubles("temperature_2m_min", i),
                    max = daily.doubles("temperature_2m_max", i),
                    sunrise = LocalDateTime.parse(daily.getJSONArray("sunrise").getString(i)),
                    sunset = LocalDateTime.parse(daily.getJSONArray("sunset").getString(i)),
                    uvMax = daily.doubles("uv_index_max", i),
                    rainMm = daily.doubles("precipitation_sum", i),
                    rainChance = daily.ints("precipitation_probability_max", i),
                )
            },
        )
    }

    /**
     * Latest rain radar frame from RainViewer, as a tile URL template with {z}/{x}/{y}.
     * Their free tiles stop at zoom 7, so the map uses that zoom.
     */
    suspend fun radarTileTemplate(): String? = runCatching {
        val json = JSONObject(httpGet("https://api.rainviewer.com/public/weather-maps.json"))
        val past = json.getJSONObject("radar").getJSONArray("past")
        val latest = past.getJSONObject(past.length() - 1)
        json.getString("host") + latest.getString("path") + "/256/{z}/{x}/{y}/2/1_1.png"
    }.getOrNull()

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
