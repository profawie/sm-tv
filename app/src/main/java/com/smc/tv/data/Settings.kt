package com.smc.tv.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Language { MS, EN }

data class Location(val name: String, val latitude: Double, val longitude: Double)

/** Dahua-style NVR/DVR on the home network, streamed over RTSP. */
data class CctvSettings(
    val host: String = "192.168.1.41",
    val rtspPort: Int = 554,
    val username: String = "syukri",
    val password: String = "",
    val channel: Int = 1,
)

data class AppSettings(
    val tvName: String = DEFAULT_TV_NAME,
    val location: Location = DEFAULT_LOCATION,
    val wallpaperTopic: String = DEFAULT_WALLPAPER_TOPIC,
    /** Bumped to force a new Unsplash photo. */
    val wallpaperSeed: Long = 0L,
    val language: Language = Language.MS,
    val cctv: CctvSettings = CctvSettings(),
) {
    companion object {
        const val DEFAULT_TV_NAME = "Syukri's Family"
        const val DEFAULT_WALLPAPER_TOPIC = "nature"
        val DEFAULT_LOCATION = Location("Puncak Alam, Kuala Selangor", 3.2389, 101.42793)
    }
}

/** Unsplash search keywords offered in Settings; labels come from Strings.wallpaperTopic. */
val WALLPAPER_TOPICS = listOf(
    "nature", "mosque", "mountains", "beach", "forest", "night sky", "city skyline", "malaysia",
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sm_tv_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            tvName = prefs.getString(KEY_TV_NAME, d.tvName) ?: d.tvName,
            location = Location(
                name = prefs.getString(KEY_LOC_NAME, d.location.name) ?: d.location.name,
                latitude = prefs.getFloat(KEY_LOC_LAT, d.location.latitude.toFloat()).toDouble(),
                longitude = prefs.getFloat(KEY_LOC_LON, d.location.longitude.toFloat()).toDouble(),
            ),
            wallpaperTopic = prefs.getString(KEY_WALLPAPER, d.wallpaperTopic) ?: d.wallpaperTopic,
            wallpaperSeed = prefs.getLong(KEY_WALLPAPER_SEED, 0L),
            language = prefs.getString(KEY_LANGUAGE, null)
                ?.let { runCatching { Language.valueOf(it) }.getOrNull() } ?: d.language,
            cctv = CctvSettings(
                host = prefs.getString(KEY_CCTV_HOST, d.cctv.host) ?: d.cctv.host,
                rtspPort = prefs.getInt(KEY_CCTV_PORT, d.cctv.rtspPort),
                username = prefs.getString(KEY_CCTV_USER, d.cctv.username) ?: d.cctv.username,
                password = prefs.getString(KEY_CCTV_PASSWORD, d.cctv.password) ?: d.cctv.password,
                channel = prefs.getInt(KEY_CCTV_CHANNEL, d.cctv.channel),
            ),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putString(KEY_TV_NAME, next.tvName)
            .putString(KEY_LOC_NAME, next.location.name)
            .putFloat(KEY_LOC_LAT, next.location.latitude.toFloat())
            .putFloat(KEY_LOC_LON, next.location.longitude.toFloat())
            .putString(KEY_WALLPAPER, next.wallpaperTopic)
            .putLong(KEY_WALLPAPER_SEED, next.wallpaperSeed)
            .putString(KEY_LANGUAGE, next.language.name)
            .putString(KEY_CCTV_HOST, next.cctv.host)
            .putInt(KEY_CCTV_PORT, next.cctv.rtspPort)
            .putString(KEY_CCTV_USER, next.cctv.username)
            .putString(KEY_CCTV_PASSWORD, next.cctv.password)
            .putInt(KEY_CCTV_CHANNEL, next.cctv.channel)
            .apply()
        _settings.value = next
    }

    private companion object {
        const val KEY_TV_NAME = "tv_name"
        const val KEY_LOC_NAME = "loc_name"
        const val KEY_LOC_LAT = "loc_lat"
        const val KEY_LOC_LON = "loc_lon"
        const val KEY_WALLPAPER = "wallpaper_topic"
        const val KEY_WALLPAPER_SEED = "wallpaper_seed"
        const val KEY_LANGUAGE = "language"
        const val KEY_CCTV_HOST = "cctv_host"
        const val KEY_CCTV_PORT = "cctv_rtsp_port"
        const val KEY_CCTV_USER = "cctv_user"
        const val KEY_CCTV_PASSWORD = "cctv_password"
        const val KEY_CCTV_CHANNEL = "cctv_channel"
    }
}
