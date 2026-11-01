package com.smc.tv.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val MALAYSIA_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

enum class PrayerId { FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA }

data class Prayer(val id: PrayerId, val time: Instant)

data class PrayerDay(
    val zone: String,
    val date: LocalDate,
    val hijri: String,
    val prayers: List<Prayer>,
)

/** Prayer times from https://api.waktusolat.app (JAKIM data), resolved by GPS to a zone. */
class PrayerRepository {
    private var cacheKey: String? = null
    private var cacheJson: JSONObject? = null

    suspend fun today(location: Location): PrayerDay {
        val date = LocalDate.now(MALAYSIA_ZONE)
        val key = "${location.latitude},${location.longitude},${date.year}-${date.monthValue}"
        val json = cacheJson.takeIf { cacheKey == key } ?: JSONObject(
            httpGet("https://api.waktusolat.app/v2/solat/gps/${location.latitude}/${location.longitude}")
        ).also {
            cacheKey = key
            cacheJson = it
        }

        val days = json.getJSONArray("prayers")
        val day = (0 until days.length()).map { days.getJSONObject(it) }
            .firstOrNull { it.getInt("day") == date.dayOfMonth }
            ?: error("no data for day ${date.dayOfMonth}")

        fun at(field: String) = Instant.ofEpochSecond(day.getLong(field))
        return PrayerDay(
            zone = json.getString("zone"),
            date = date,
            hijri = day.optString("hijri"),
            prayers = listOf(
                Prayer(PrayerId.FAJR, at("fajr")),
                Prayer(PrayerId.SUNRISE, at("syuruk")),
                Prayer(PrayerId.DHUHR, at("dhuhr")),
                Prayer(PrayerId.ASR, at("asr")),
                Prayer(PrayerId.MAGHRIB, at("maghrib")),
                Prayer(PrayerId.ISHA, at("isha")),
            ),
        )
    }
}
