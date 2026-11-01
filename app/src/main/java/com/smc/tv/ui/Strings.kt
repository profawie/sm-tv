package com.smc.tv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.smc.tv.data.Language
import com.smc.tv.data.PrayerId
import java.util.Locale

/** All user-facing text, in Malay and English. Picked in Settings. */
interface Strings {
    val locale: Locale
    val settings: String
    val back: String
    val save: String
    val reset: String
    val tvNameSection: String
    val locationSection: String
    fun currentLocation(name: String): String
    val searchHint: String
    val search: String
    val searching: String
    fun noResults(query: String): String
    fun searchFailed(reason: String?): String
    val wallpaperSection: String
    val wallpaperHint: String
    val wallpaperNoKey: String
    val changeWallpaperNow: String
    val loadingImage: String
    fun failed(reason: String?): String
    val languageSection: String
    val loading: String
    fun loadFailed(reason: String?): String
    val prayerTitle: String
    fun zone(zone: String): String
    fun nextPrayer(name: String, hours: Long, minutes: Long): String
    fun prayerName(id: PrayerId): String
    val hijriMonths: List<String>
    val weatherTitle: String
    fun weatherDetails(feelsLike: Int, humidity: Int, windKmh: Int): String
    fun weatherLabel(code: Int): String
    val noConnection: String
    val installFromPlayStore: String
    fun photoCredit(name: String): String
    fun wallpaperTopic(topic: String): String

    // Full weather screen
    val today: String
    val next24Hours: String
    val details: String
    val rainRadar: String
    val feelsLike: String
    val humidity: String
    val wind: String
    val pressure: String
    val visibility: String
    val cloudCover: String
    val uvIndex: String
    val sunrise: String
    val sunset: String
    val rainToday: String
    /** 8-point compass, starting at north and going clockwise. */
    val compass: List<String>
    fun weatherSummary(label: String, min: Int, max: Int, rainChance: Int, rainMm: Double): String
    fun radarUpdated(time: String): String

    // CCTV
    val cctvTitle: String
    val cctvNoPassword: String
    val cctvConnecting: String
    val cctvError: String
    val cctvSection: String
    val cctvHint: String
    val cctvAddress: String
    val cctvUsername: String
    val cctvPassword: String
    val cctvPasswordSet: String
    val cctvChannel: String
    fun camera(channel: Int): String
}

object MalayStrings : Strings {
    override val locale: Locale = Locale("ms", "MY")
    override val settings = "Tetapan"
    override val back = "← Kembali"
    override val save = "Simpan"
    override val reset = "Lalai"
    override val tvNameSection = "1. Nama TV"
    override val locationSection = "2. Lokasi"
    override fun currentLocation(name: String) = "Semasa: $name"
    override val searchHint = "Cari bandar, cth. Bandar Puncak Alam"
    override val search = "Cari"
    override val searching = "Mencari…"
    override fun noResults(query: String) = "Tiada hasil untuk \"$query\""
    override fun searchFailed(reason: String?) = "Carian gagal: ${reason ?: "ralat"}"
    override val wallpaperSection = "3. Wallpaper (Unsplash)"
    override val wallpaperHint = "Pilih tema gambar latar"
    override val wallpaperNoKey = "Kunci Unsplash belum ditetapkan, latar gelap digunakan"
    override val changeWallpaperNow = "Tukar gambar sekarang"
    override val loadingImage = "Memuatkan gambar…"
    override fun failed(reason: String?) = "Gagal: ${reason ?: "ralat"}"
    override val languageSection = "4. Bahasa / Language"
    override val loading = "Memuatkan…"
    override fun loadFailed(reason: String?) = "Gagal dimuatkan: ${reason ?: "ralat"}"
    override val prayerTitle = "Waktu Solat"
    override fun zone(zone: String) = "Zon $zone"
    override fun nextPrayer(name: String, hours: Long, minutes: Long) =
        if (hours > 0) "$name dalam $hours jam $minutes minit" else "$name dalam $minutes minit"
    override fun prayerName(id: PrayerId) = when (id) {
        PrayerId.FAJR -> "Subuh"
        PrayerId.SUNRISE -> "Syuruk"
        PrayerId.DHUHR -> "Zohor"
        PrayerId.ASR -> "Asar"
        PrayerId.MAGHRIB -> "Maghrib"
        PrayerId.ISHA -> "Isyak"
    }
    override val hijriMonths = listOf(
        "Muharram", "Safar", "Rabiulawal", "Rabiulakhir", "Jamadilawal", "Jamadilakhir",
        "Rejab", "Syaaban", "Ramadan", "Syawal", "Zulkaedah", "Zulhijjah",
    )
    override val weatherTitle = "Cuaca"
    override fun weatherDetails(feelsLike: Int, humidity: Int, windKmh: Int) =
        "Terasa $feelsLike°C · Lembapan $humidity% · Angin $windKmh km/j"
    override fun weatherLabel(code: Int) = when (code) {
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
    override val noConnection = "Tiada sambungan"
    override val installFromPlayStore = "Pasang dari Play Store"
    override fun photoCredit(name: String) = "Foto: $name / Unsplash"
    override fun wallpaperTopic(topic: String) = when (topic) {
        "nature" -> "Alam semula jadi"
        "mosque" -> "Masjid"
        "mountains" -> "Gunung"
        "beach" -> "Pantai"
        "forest" -> "Hutan"
        "night sky" -> "Langit malam"
        "city skyline" -> "Bandar"
        else -> topic.replaceFirstChar { it.uppercase() }
    }
    override val today = "Hari ini"
    override val next24Hours = "24 jam akan datang"
    override val details = "Butiran"
    override val rainRadar = "Radar hujan"
    override val feelsLike = "Terasa seperti"
    override val humidity = "Kelembapan"
    override val wind = "Angin"
    override val pressure = "Tekanan"
    override val visibility = "Jarak penglihatan"
    override val cloudCover = "Litupan awan"
    override val uvIndex = "Indeks UV"
    override val sunrise = "Matahari terbit"
    override val sunset = "Matahari terbenam"
    override val rainToday = "Hujan hari ini"
    override val compass = listOf("U", "TL", "T", "TG", "S", "BD", "B", "BL")
    override fun weatherSummary(label: String, min: Int, max: Int, rainChance: Int, rainMm: Double) =
        "Hari ini ${label.lowercase(locale)}, suhu $min°C hingga $max°C. " +
            "Peluang hujan $rainChance% (anggaran ${"%.1f".format(rainMm)} mm)."
    override fun radarUpdated(time: String) = "Dikemas kini $time"
    override val cctvTitle = "CCTV"
    override val cctvNoPassword = "Masukkan kata laluan CCTV di Tetapan"
    override val cctvConnecting = "Menyambung…"
    override val cctvError = "Kamera tidak dapat disambung. Cuba semula…"
    override val cctvSection = "5. CCTV"
    override val cctvHint = "Rangkaian rumah sahaja (RTSP)"
    override val cctvAddress = "Alamat IP"
    override val cctvUsername = "Nama pengguna"
    override val cctvPassword = "Kata laluan"
    override val cctvPasswordSet = "Kata laluan disimpan"
    override val cctvChannel = "Kamera untuk widget"
    override fun camera(channel: Int) = "Kamera $channel"
}

object EnglishStrings : Strings {
    override val locale: Locale = Locale.ENGLISH
    override val settings = "Settings"
    override val back = "← Back"
    override val save = "Save"
    override val reset = "Default"
    override val tvNameSection = "1. TV name"
    override val locationSection = "2. Location"
    override fun currentLocation(name: String) = "Current: $name"
    override val searchHint = "Search a town, e.g. Bandar Puncak Alam"
    override val search = "Search"
    override val searching = "Searching…"
    override fun noResults(query: String) = "No results for \"$query\""
    override fun searchFailed(reason: String?) = "Search failed: ${reason ?: "error"}"
    override val wallpaperSection = "3. Wallpaper (Unsplash)"
    override val wallpaperHint = "Choose a background theme"
    override val wallpaperNoKey = "Unsplash key not set, using a dark background"
    override val changeWallpaperNow = "Change photo now"
    override val loadingImage = "Loading photo…"
    override fun failed(reason: String?) = "Failed: ${reason ?: "error"}"
    override val languageSection = "4. Language / Bahasa"
    override val loading = "Loading…"
    override fun loadFailed(reason: String?) = "Couldn't load: ${reason ?: "error"}"
    override val prayerTitle = "Prayer Times"
    override fun zone(zone: String) = "Zone $zone"
    override fun nextPrayer(name: String, hours: Long, minutes: Long) =
        if (hours > 0) "$name in ${hours}h ${minutes}m" else "$name in $minutes min"
    override fun prayerName(id: PrayerId) = when (id) {
        PrayerId.FAJR -> "Fajr"
        PrayerId.SUNRISE -> "Sunrise"
        PrayerId.DHUHR -> "Dhuhr"
        PrayerId.ASR -> "Asr"
        PrayerId.MAGHRIB -> "Maghrib"
        PrayerId.ISHA -> "Isha"
    }
    override val hijriMonths = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qa'dah", "Dhu al-Hijjah",
    )
    override val weatherTitle = "Weather"
    override fun weatherDetails(feelsLike: Int, humidity: Int, windKmh: Int) =
        "Feels $feelsLike°C · Humidity $humidity% · Wind $windKmh km/h"
    override fun weatherLabel(code: Int) = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 66, 80, 81 -> "Rain"
        65, 67, 82 -> "Heavy rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm & hail"
        else -> "—"
    }
    override val noConnection = "No connection"
    override val installFromPlayStore = "Install from Play Store"
    override fun photoCredit(name: String) = "Photo: $name / Unsplash"
    override fun wallpaperTopic(topic: String) = when (topic) {
        "night sky" -> "Night sky"
        "city skyline" -> "City"
        else -> topic.replaceFirstChar { it.uppercase() }
    }
    override val today = "Today"
    override val next24Hours = "Next 24 hours"
    override val details = "Details"
    override val rainRadar = "Rain radar"
    override val feelsLike = "Feels like"
    override val humidity = "Humidity"
    override val wind = "Wind"
    override val pressure = "Pressure"
    override val visibility = "Visibility"
    override val cloudCover = "Cloud cover"
    override val uvIndex = "UV index"
    override val sunrise = "Sunrise"
    override val sunset = "Sunset"
    override val rainToday = "Rain today"
    override val compass = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    override fun weatherSummary(label: String, min: Int, max: Int, rainChance: Int, rainMm: Double) =
        "${label.replaceFirstChar { it.uppercase() }} today, $min°C to $max°C. " +
            "$rainChance% chance of rain (about ${"%.1f".format(rainMm)} mm)."
    override fun radarUpdated(time: String) = "Updated $time"
    override val cctvTitle = "CCTV"
    override val cctvNoPassword = "Enter the CCTV password in Settings"
    override val cctvConnecting = "Connecting…"
    override val cctvError = "Can't reach the camera. Retrying…"
    override val cctvSection = "5. CCTV"
    override val cctvHint = "Home network only (RTSP)"
    override val cctvAddress = "IP address"
    override val cctvUsername = "Username"
    override val cctvPassword = "Password"
    override val cctvPasswordSet = "Password saved"
    override val cctvChannel = "Camera for the widget"
    override fun camera(channel: Int) = "Camera $channel"
}

fun stringsFor(language: Language): Strings = when (language) {
    Language.MS -> MalayStrings
    Language.EN -> EnglishStrings
}

val LocalStrings = staticCompositionLocalOf<Strings> { MalayStrings }

/** "1448-05-21" -> "21 Jamadilawal 1448H" */
fun Strings.formatHijri(raw: String): String {
    val parts = raw.split("-")
    if (parts.size != 3) return raw
    val month = parts[1].toIntOrNull()?.let { hijriMonths.getOrNull(it - 1) } ?: return raw
    return "${parts[2].trimStart('0')} $month ${parts[0]}H"
}
