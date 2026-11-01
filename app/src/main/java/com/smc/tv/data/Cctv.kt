package com.smc.tv.data

import java.net.URLEncoder

/** Main stream for full screen, sub stream (lower resolution) for the small widget. */
enum class CctvStream(val subtype: Int) { MAIN(0), SUB(1) }

/**
 * Dahua RTSP URL, e.g. rtsp://user:pass@192.168.1.41:554/cam/realmonitor?channel=1&subtype=1.
 * Returns null until a password is set in Settings.
 */
fun CctvSettings.rtspUrl(stream: CctvStream): String? {
    if (password.isBlank() || host.isBlank()) return null
    fun enc(v: String) = URLEncoder.encode(v, "UTF-8").replace("+", "%20")
    return "rtsp://${enc(username)}:${enc(password)}@$host:$rtspPort" +
        "/cam/realmonitor?channel=$channel&subtype=${stream.subtype}"
}
