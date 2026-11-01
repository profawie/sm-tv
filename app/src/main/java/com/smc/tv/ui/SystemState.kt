package com.smc.tv.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.time.LocalDateTime

enum class LinkType { WIFI, ETHERNET, NONE }

data class NetworkStatus(val type: LinkType, val ssid: String?)

@SuppressLint("MissingPermission")
fun readNetworkStatus(context: Context): NetworkStatus {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    return when {
        caps == null -> NetworkStatus(LinkType.NONE, null)
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkStatus(LinkType.ETHERNET, "Ethernet")
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
            @Suppress("DEPRECATION")
            val ssid = runCatching { wifi.connectionInfo?.ssid }.getOrNull()
                ?.trim('"')
                ?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID.trim('<', '>') && it != "<unknown ssid>" }
            NetworkStatus(LinkType.WIFI, ssid ?: "WiFi")
        }
        else -> NetworkStatus(LinkType.NONE, null)
    }
}

/** Polls network status; cheap enough to do every few seconds on a TV. */
@Composable
fun rememberNetworkStatus(): NetworkStatus {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readNetworkStatus(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            status = readNetworkStatus(context)
            delay(5_000)
        }
    }
    return status
}

/** Ticks on each minute boundary for the header clock and prayer countdown. */
@Composable
fun rememberNow(): LocalDateTime {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L * (60 - now.second).coerceAtLeast(1))
        }
    }
    return now
}

data class TvApp(val label: String, val packageName: String)

val PINNED_APPS = listOf(
    TvApp("Netflix", "com.netflix.ninja"),
    TvApp("YouTube", "com.google.android.youtube.tv"),
    TvApp("Disney+", "com.disney.disneyplus"),
)

/** Opens a TV app, or its Play Store page when it isn't installed. */
fun launchApp(context: Context, packageName: String) {
    val pm = context.packageManager
    val intent = pm.getLeanbackLaunchIntentForPackage(packageName)
        ?: pm.getLaunchIntentForPackage(packageName)
    try {
        if (intent != null) {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    } catch (_: ActivityNotFoundException) {
    }
}

private val STOCK_LAUNCHERS = listOf(
    "com.google.android.apps.tv.launcherx", // Google TV
    "com.google.android.tvlauncher",        // Android TV
)

/** "Back to Google TV": opens the stock home screen. */
fun openGoogleTvHome(context: Context) {
    val pm = context.packageManager
    for (pkg in STOCK_LAUNCHERS) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setPackage(pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(pm) != null) {
            context.startActivity(intent)
            return
        }
        pm.getLaunchIntentForPackage(pkg)?.let {
            context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
    }
}
