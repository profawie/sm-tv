package com.smc.tv.ui

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.smc.tv.data.AppSettings
import com.smc.tv.data.MALAYSIA_ZONE
import com.smc.tv.data.PrayerDay
import com.smc.tv.data.SolarStatus
import com.smc.tv.data.Wallpaper
import com.smc.tv.data.Weather
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ClockFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.ENGLISH)

@Composable
fun HomeScreen(
    settings: AppSettings,
    prayer: Loadable<PrayerDay>,
    weather: Loadable<Weather>,
    solar: Loadable<SolarStatus>?,
    wallpaper: Wallpaper?,
    wallpaperLoading: Boolean,
    onOpenSettings: () -> Unit,
    onOpenWeather: () -> Unit,
    onOpenCctv: () -> Unit,
    onOpenAllApps: () -> Unit,
) {
    val context = LocalContext.current
    val now = rememberNow()
    val firstApp = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstApp.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 20.dp)) {
            Header(
                tvName = settings.tvName,
                clock = ClockFmt.format(now),
                onOpenSettings = onOpenSettings,
                onOpenAllApps = onOpenAllApps,
                onGoogleTv = { openGoogleTvHome(context) },
            )

            Spacer(Modifier.height(10.dp))
            // Row 1: prayer times across the full width.
            PrayerWidget(prayer, now.atZone(MALAYSIA_ZONE).toInstant(), Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            // Row 2: weather, solar and CCTV side by side.
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WeatherWidget(weather, settings.location.name, Modifier.weight(1f).fillMaxHeight(), onClick = onOpenWeather)
                // Hidden until a Growatt API token is set, so weather and CCTV get the space.
                if (solar != null) SolarWidget(solar, Modifier.weight(1f).fillMaxHeight())
                CctvWidget(settings.cctv, Modifier.weight(1.2f).fillMaxHeight(), onClick = onOpenCctv)
            }
            Spacer(Modifier.height(14.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            ) {
                PINNED_APPS.forEachIndexed { i, app ->
                    AppTile(
                        app = app,
                        modifier = if (i == 0) Modifier.focusRequester(firstApp) else Modifier,
                        onClick = { launchApp(context, app.packageName) },
                    )
                }
            }
        }

        if (wallpaperLoading) {
            WallpaperLoadingBadge(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 8.dp))
        } else if (wallpaper != null) {
            Text(
                LocalStrings.current.photoCredit(wallpaper.photographer),
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 8.dp),
            )
        }
    }
}

/** Shared backdrop for Home and Settings, so a wallpaper change shows straight away. */
@Composable
fun WallpaperLayer(wallpaper: Wallpaper?) {
    Box(Modifier.fillMaxSize().background(Ink.Background))
    if (wallpaper != null) {
        // The image is already in Coil's cache (prefetched before the switch), so this is instant.
        AsyncImage(
            model = wallpaper.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(Color(0xFF050505), Color(0xFF1C160A), Color(0xFF050505)))
            )
        )
    }
    // Darken edges so header and app row stay readable on bright photos.
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.7f),
                0.25f to Color.Black.copy(alpha = 0.25f),
                0.7f to Color.Black.copy(alpha = 0.25f),
                1f to Color.Black.copy(alpha = 0.8f),
            )
        )
    )
}

@Composable
private fun Header(
    tvName: String,
    clock: String,
    onOpenSettings: () -> Unit,
    onOpenAllApps: () -> Unit,
    onGoogleTv: () -> Unit,
) {
    val net = rememberNetworkStatus()
    val context = LocalContext.current
    val hasGoogleTv = remember { isGoogleTvHomeAvailable(context) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(tvName, color = Gold.Primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

        val (icon, label) = when (net.type) {
            LinkType.WIFI -> Icons.Filled.Wifi to (net.ssid ?: "WiFi")
            LinkType.ETHERNET -> Icons.Filled.Cable to "Ethernet"
            LinkType.NONE -> Icons.Filled.WifiOff to LocalStrings.current.noConnection
        }
        Icon(icon, contentDescription = null, tint = if (net.type == LinkType.NONE) Ink.Muted else Gold.Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Ink.Text, fontSize = 14.sp)
        Spacer(Modifier.width(20.dp))
        Text(clock, color = Ink.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(18.dp))
        HeaderButton(Icons.Filled.Apps, LocalStrings.current.allApps, onOpenAllApps)
        Spacer(Modifier.width(10.dp))
        HeaderButton(Icons.Filled.Settings, LocalStrings.current.settings, onOpenSettings)
        // Hidden once Google TV's launcher is disabled to make SM TV the home screen.
        if (hasGoogleTv) {
            Spacer(Modifier.width(10.dp))
            HeaderButton(Icons.Filled.Home, "Google TV", onGoogleTv)
        }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .tvFocusable(shape = RoundedCornerShape(50), focusedScale = 1.08f, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Gold.Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Ink.Text, fontSize = 13.sp)
    }
}

@Composable
fun AppTile(app: TvApp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val banner: Drawable? = remember(app.packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationBanner(app.packageName) ?: pm.getApplicationIcon(app.packageName)
        }.getOrNull()
    }
    Box(
        modifier
            .tvFocusable(onClick = onClick)
            .size(width = 144.dp, height = 81.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (banner != null) {
            Image(
                bitmap = remember(banner) { banner.toBitmap(320, 180).asImageBitmap() },
                contentDescription = app.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Not installed: show the name; clicking opens its Play Store page.
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(10.dp).background(Gold.Primary, CircleShape))
                Spacer(Modifier.height(8.dp))
                Text(app.label, color = Ink.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(LocalStrings.current.installFromPlayStore, color = Ink.Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun WallpaperLoadingBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Ink.Panel, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(color = Gold.Primary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(LocalStrings.current.loadingImage, color = Ink.Text, fontSize = 12.sp)
    }
}
