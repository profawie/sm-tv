package com.smc.tv.data

import com.smc.tv.BuildConfig
import org.json.JSONObject
import java.net.URLEncoder

data class Wallpaper(val imageUrl: String, val photographer: String)

/**
 * Random landscape photo from the Unsplash API. Needs UNSPLASH_ACCESS_KEY in
 * local.properties; without it the home screen falls back to a plain dark gradient.
 */
class WallpaperRepository {
    val hasKey: Boolean get() = BuildConfig.UNSPLASH_ACCESS_KEY.isNotBlank()

    suspend fun random(topic: String): Wallpaper? {
        if (!hasKey) return null
        val q = URLEncoder.encode(topic, "UTF-8")
        val auth = mapOf("Authorization" to "Client-ID ${BuildConfig.UNSPLASH_ACCESS_KEY}")
        val json = JSONObject(
            httpGet(
                "https://api.unsplash.com/photos/random?query=$q&orientation=landscape&content_filter=high",
                auth,
            )
        )
        // Unsplash API guidelines ask apps to ping download_location when a photo is used.
        json.optJSONObject("links")?.optString("download_location")
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { httpGet(it, auth) } }

        val urls = json.getJSONObject("urls")
        // "raw" + sizing params keeps a 1080p image instead of the multi-MB original.
        val image = urls.getString("raw") + "&w=1920&h=1080&fit=crop&q=80&fm=jpg"
        return Wallpaper(image, json.getJSONObject("user").getString("name"))
    }
}
