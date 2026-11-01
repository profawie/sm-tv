package com.smc.tv.data

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val MALAYSIA_ZONE: ZoneId = ZoneId.of("Asia/Kuala_Lumpur")

data class Prayer(val name: String, val time: Instant)

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
            ?: error("Tiada data untuk ${date.dayOfMonth}")

        fun at(field: String) = Instant.ofEpochSecond(day.getLong(field))
        return PrayerDay(
            zone = json.getString("zone"),
            date = date,
            hijri = day.optString("hijri"),
            prayers = listOf(
                Prayer("Subuh", at("fajr")),
                Prayer("Syuruk", at("syuruk")),
                Prayer("Zohor", at("dhuhr")),
                Prayer("Asar", at("asr")),
                Prayer("Maghrib", at("maghrib")),
                Prayer("Isyak", at("isha")),
            ),
        )
    }
}

private val HIJRI_MONTHS = listOf(
    "Muharram", "Safar", "Rabiulawal", "Rabiulakhir", "Jamadilawal", "Jamadilakhir",
    "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah",
)

/** "1448-05-21" -> "21 Jamadilawal 1448H" */
fun formatHijri(raw: String): String {
    val parts = raw.split("-")
    if (parts.size != 3) return raw
    val month = parts[1].toIntOrNull()?.let { HIJRI_MONTHS.getOrNull(it - 1) } ?: return raw
    return "${parts[2].trimStart('0')} $month ${parts[0]}H"
}
