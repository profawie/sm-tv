package com.smc.tv

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import kotlin.coroutines.cancellation.CancellationException
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import com.smc.tv.data.SolarRepository
import com.smc.tv.data.SolarStatus
import com.smc.tv.ui.AllAppsScreen
import androidx.compose.runtime.mutableIntStateOf
import com.smc.tv.data.Wallpaper
import com.smc.tv.data.WallpaperRepository
import com.smc.tv.data.Weather
import com.smc.tv.data.WeatherRepository
import com.smc.tv.ui.HomeScreen
import com.smc.tv.ui.Loadable
import com.smc.tv.ui.SettingsScreen
import com.smc.tv.ui.SmTvTheme
import com.smc.tv.ui.LocalStrings
import com.smc.tv.ui.stringsFor
import androidx.compose.runtime.CompositionLocalProvider
import com.smc.tv.ui.WallpaperLayer
import com.smc.tv.ui.WeatherScreen
import com.smc.tv.ui.CctvScreen
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.delay

private const val RETRY_MS = 30_000L

/**
 * Like runCatching, but lets coroutine cancellation through, so a loader restarted by a
 * settings or network change doesn't record "the coroutine scope left the composition"
 * as a load error.
 */
private inline fun <T> attempt(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

private enum class Screen { HOME, SETTINGS, WEATHER, CCTV, ALL_APPS }

class MainActivity : ComponentActivity() {
    private val prayerRepo = PrayerRepository()
    private val weatherRepo = WeatherRepository()
    private val solarRepo = SolarRepository()
    /** Bumped when the Home button is pressed while SM TV is already open. */
    private val homePresses = mutableIntStateOf(0)
    private val wallpaperRepo = WallpaperRepository()
    /** Bumped each time the TV gets a working internet connection (e.g. WiFi coming back). */
    private val networkUps = mutableIntStateOf(0)
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // Capabilities change often (signal strength etc.), so only count offline -> online.
        // Starts false so a TV that boots before WiFi is ready reloads once it connects.
        private var online = false

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (validated && !online) runOnUiThread { networkUps.intValue++ }
            online = validated
        }

        override fun onLost(network: Network) {
            online = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = SettingsStore(applicationContext)
        requestWifiNamePermission()
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)

        setContent {
            val settings by store.settings.collectAsStateWithLifecycle()
            CompositionLocalProvider(LocalStrings provides stringsFor(settings.language)) {
                SmTvTheme {
                    var screen by remember { mutableStateOf(Screen.HOME) }
                    LaunchedEffect(homePresses.intValue) { screen = Screen.HOME }
                    var solar by remember { mutableStateOf<Loadable<SolarStatus>?>(null) }
                    val networkUp = networkUps.intValue
                    LaunchedEffect(settings.solarToken, networkUp) {
                        if (settings.solarToken.isBlank()) {
                            solar = null
                            return@LaunchedEffect
                        }
                        if (solar == null) solar = Loadable.Loading
                        while (true) {
                            val result = attempt { solarRepo.status(settings.solarToken) }
                            solar = result.fold({ Loadable.Ready(it) }, { (solar as? Loadable.Ready) ?: Loadable.Failed(it.message) })
                            // Growatt updates plant data about every 5 minutes.
                            delay(if (result.isSuccess) 5 * 60_000L else RETRY_MS)
                        }
                    }
                    var radarTiles by remember { mutableStateOf<String?>(null) }
                    var prayer by remember { mutableStateOf<Loadable<PrayerDay>>(Loadable.Loading) }
                    var weather by remember { mutableStateOf<Loadable<Weather>>(Loadable.Loading) }
                    var wallpaper by remember { mutableStateOf<Wallpaper?>(null) }
                    var wallpaperLoading by remember { mutableStateOf(false) }
                    var wallpaperError by remember { mutableStateOf<String?>(null) }

                    // Prayer data is per day; re-check every 30 min so it rolls over after midnight.
                    // Each loader also restarts when the connection comes back, and retries
                    // after 30 s on failure instead of waiting for the next regular refresh.
                    LaunchedEffect(settings.location, networkUp) {
                        while (true) {
                            val result = attempt { prayerRepo.today(settings.location) }
                            prayer = result.fold({ Loadable.Ready(it) }, { (prayer as? Loadable.Ready) ?: Loadable.Failed(it.message) })
                            delay(if (result.isSuccess) 30 * 60_000L else RETRY_MS)
                        }
                    }
                    LaunchedEffect(settings.location, networkUp) {
                        while (true) {
                            val result = attempt { weatherRepo.current(settings.location) }
                            weather = result.fold({ Loadable.Ready(it) }, { (weather as? Loadable.Ready) ?: Loadable.Failed(it.message) })
                            delay(if (result.isSuccess) 15 * 60_000L else RETRY_MS)
                        }
                    }
                    // New photo every hour, or immediately when changed in Settings.
                    // The image is downloaded before it replaces the old one, so there is no blank flash.
                    // A reconnect only refetches when there's no photo yet, so it doesn't swap the current one.
                    var wallpaperRetry by remember { mutableIntStateOf(0) }
                    LaunchedEffect(networkUp) { if (wallpaper == null && wallpaperRepo.hasKey) wallpaperRetry++ }
                    LaunchedEffect(settings.wallpaperTopic, settings.wallpaperSeed, wallpaperRetry) {
                        while (true) {
                            wallpaperLoading = wallpaperRepo.hasKey
                            wallpaperError = null
                            attempt {
                                wallpaperRepo.random(settings.wallpaperTopic)?.also { next ->
                                    val result = imageLoader.execute(
                                        ImageRequest.Builder(this@MainActivity).data(next.imageUrl).build()
                                    )
                                    if (result !is SuccessResult) error("download failed")
                                }
                            }
                                .onSuccess { wallpaper = it }
                                .onFailure { wallpaperError = it.message ?: "error" }
                            wallpaperLoading = false
                            delay(if (wallpaperError == null) 60 * 60_000L else RETRY_MS)
                        }
                    }

                    WallpaperLayer(wallpaper)

                    // Radar frames update every 10 minutes; only fetch while the weather screen is open.
                    LaunchedEffect(screen) {
                        while (screen == Screen.WEATHER) {
                            weatherRepo.radarTileTemplate()?.let { radarTiles = it }
                            delay(10 * 60_000L)
                        }
                    }

                    when (screen) {
                        Screen.SETTINGS -> {
                            BackHandler { screen = Screen.HOME }
                            SettingsScreen(
                                settings = settings,
                                hasUnsplashKey = wallpaperRepo.hasKey,
                                wallpaperLoading = wallpaperLoading,
                                wallpaperError = wallpaperError,
                                weatherRepo = weatherRepo,
                                onUpdate = store::update,
                                onBack = { screen = Screen.HOME },
                            )
                        }
                        Screen.WEATHER -> {
                            BackHandler { screen = Screen.HOME }
                            WeatherScreen(
                                state = weather,
                                location = settings.location,
                                radarTileTemplate = radarTiles,
                                onBack = { screen = Screen.HOME },
                            )
                        }
                        Screen.CCTV -> {
                            BackHandler { screen = Screen.HOME }
                            CctvScreen(settings.cctv, onBack = { screen = Screen.HOME })
                        }
                        Screen.ALL_APPS -> {
                            BackHandler { screen = Screen.HOME }
                            AllAppsScreen(onBack = { screen = Screen.HOME })
                        }
                        Screen.HOME -> {
                            // As a home screen, Back should stay here instead of closing the launcher.
                            BackHandler {}
                            HomeScreen(
                                settings = settings,
                                prayer = prayer,
                                weather = weather,
                                solar = solar,
                                wallpaper = wallpaper,
                                wallpaperLoading = wallpaperLoading,
                                onOpenSettings = { screen = Screen.SETTINGS },
                                onOpenWeather = { screen = Screen.WEATHER },
                                onOpenAllApps = { screen = Screen.ALL_APPS },
                                onOpenCctv = { screen = Screen.CCTV },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback) }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
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
