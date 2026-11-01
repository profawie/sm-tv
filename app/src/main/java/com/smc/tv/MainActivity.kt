package com.smc.tv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smc.tv.data.PrayerDay
import com.smc.tv.data.PrayerRepository
import com.smc.tv.data.SettingsStore
import com.smc.tv.data.Wallpaper
import com.smc.tv.data.WallpaperRepository
import com.smc.tv.data.Weather
import com.smc.tv.data.WeatherRepository
import com.smc.tv.ui.HomeScreen
import com.smc.tv.ui.Loadable
import com.smc.tv.ui.SettingsScreen
import com.smc.tv.ui.SmTvTheme
import com.smc.tv.ui.WallpaperLayer
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val prayerRepo = PrayerRepository()
    private val weatherRepo = WeatherRepository()
    private val wallpaperRepo = WallpaperRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(applicationContext)
        requestWifiNamePermission()

        setContent {
            SmTvTheme {
                val settings by store.settings.collectAsStateWithLifecycle()
                var showSettings by remember { mutableStateOf(false) }
                var prayer by remember { mutableStateOf<Loadable<PrayerDay>>(Loadable.Loading) }
                var weather by remember { mutableStateOf<Loadable<Weather>>(Loadable.Loading) }
                var wallpaper by remember { mutableStateOf<Wallpaper?>(null) }
                var wallpaperLoading by remember { mutableStateOf(false) }
                var wallpaperError by remember { mutableStateOf<String?>(null) }

                // Prayer data is per day; re-check every 30 min so it rolls over after midnight.
                LaunchedEffect(settings.location) {
                    prayer = Loadable.Loading
                    while (true) {
                        prayer = runCatching { prayerRepo.today(settings.location) }
                            .fold({ Loadable.Ready(it) }, { (prayer as? Loadable.Ready) ?: Loadable.Failed(it.message ?: "ralat") })
                        delay(30 * 60_000L)
                    }
                }
                LaunchedEffect(settings.location) {
                    weather = Loadable.Loading
                    while (true) {
                        weather = runCatching { weatherRepo.current(settings.location) }
                            .fold({ Loadable.Ready(it) }, { (weather as? Loadable.Ready) ?: Loadable.Failed(it.message ?: "ralat") })
                        delay(15 * 60_000L)
                    }
                }
                // New photo every hour, or immediately when changed in Settings.
                // The image is downloaded before it replaces the old one, so there is no blank flash.
                LaunchedEffect(settings.wallpaperTopic, settings.wallpaperSeed) {
                    while (true) {
                        wallpaperLoading = wallpaperRepo.hasKey
                        wallpaperError = null
                        runCatching {
                            wallpaperRepo.random(settings.wallpaperTopic)?.also { next ->
                                val result = imageLoader.execute(
                                    ImageRequest.Builder(this@MainActivity).data(next.imageUrl).build()
                                )
                                if (result !is SuccessResult) error("gambar tidak dapat dimuat turun")
                            }
                        }
                            .onSuccess { wallpaper = it }
                            .onFailure { wallpaperError = it.message ?: "ralat" }
                        wallpaperLoading = false
                        delay(60 * 60_000L)
                    }
                }

                WallpaperLayer(wallpaper)

                if (showSettings) {
                    BackHandler { showSettings = false }
                    SettingsScreen(
                        settings = settings,
                        hasUnsplashKey = wallpaperRepo.hasKey,
                        wallpaperLoading = wallpaperLoading,
                        wallpaperError = wallpaperError,
                        weatherRepo = weatherRepo,
                        onUpdate = store::update,
                        onBack = { showSettings = false },
                    )
                } else {
                    // As a home screen, Back should stay here instead of closing the launcher.
                    BackHandler {}
                    HomeScreen(
                        settings = settings,
                        prayer = prayer,
                        weather = weather,
                        wallpaper = wallpaper,
                        wallpaperLoading = wallpaperLoading,
                        onOpenSettings = { showSettings = true },
                    )
                }
            }
        }
    }

    /** Android only reveals the WiFi name (SSID) to apps holding location permission. */
    private fun requestWifiNamePermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                0,
            )
        }
    }
}
