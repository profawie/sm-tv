package com.smc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smc.tv.data.MALAYSIA_ZONE
import com.smc.tv.data.PrayerDay
import com.smc.tv.data.Weather
import com.smc.tv.data.formatHijri
import com.smc.tv.data.weatherIcon
import com.smc.tv.data.weatherLabel
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PanelShape = RoundedCornerShape(24.dp)
private val TimeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH).withZone(MALAYSIA_ZONE)
private val DayFmt = DateTimeFormatter.ofPattern("EEE", Locale("ms", "MY"))

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String) : Loadable<Nothing>
}

@Composable
fun WidgetPanel(title: String, subtitle: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .background(Ink.Panel, PanelShape)
            .border(1.dp, Gold.Dim.copy(alpha = 0.5f), PanelShape)
            .padding(horizontal = 24.dp, vertical = 18.dp)
    ) {
        Text(title, color = Gold.Primary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = Ink.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun LoadingOrError(state: Loadable<*>) {
    val msg = when (state) {
        is Loadable.Failed -> "Gagal dimuatkan: ${state.message}"
        else -> "Memuatkan…"
    }
    Text(msg, color = Ink.Muted, fontSize = 16.sp)
}

@Composable
fun PrayerWidget(state: Loadable<PrayerDay>, now: Instant, modifier: Modifier = Modifier) {
    val day = (state as? Loadable.Ready)?.value
    WidgetPanel(
        title = "Waktu Solat",
        subtitle = day?.let { "${formatHijri(it.hijri)} · Zon ${it.zone}" },
        modifier = modifier,
    ) {
        if (day == null) {
            LoadingOrError(state)
            return@WidgetPanel
        }
        // Syuruk is shown but is not a prayer, so it never counts as "next".
        val next = day.prayers.filter { it.name != "Syuruk" }.firstOrNull { it.time.isAfter(now) }
        if (next != null) {
            val left = Duration.between(now, next.time)
            Text(
                "${next.name} dalam " + if (left.toHours() > 0) "${left.toHours()} jam ${left.toMinutes() % 60} minit" else "${left.toMinutes() + 1} minit",
                color = Gold.Light, fontSize = 15.sp,
            )
            Spacer(Modifier.height(6.dp))
        }
        day.prayers.forEach { p ->
            val isNext = p == next
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isNext) Gold.Primary.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val color = when {
                    isNext -> Gold.Primary
                    p.time.isBefore(now) -> Ink.Muted
                    else -> Ink.Text
                }
                Text(p.name, color = color, fontSize = 16.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
                Text(TimeFmt.format(p.time), color = color, fontSize = 16.sp, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun WeatherWidget(state: Loadable<Weather>, locationName: String, modifier: Modifier = Modifier) {
    val w = (state as? Loadable.Ready)?.value
    WidgetPanel(title = "Cuaca", subtitle = locationName, modifier = modifier) {
        if (w == null) {
            LoadingOrError(state)
            return@WidgetPanel
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(weatherIcon(w.code, w.isDay), fontSize = 44.sp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("${w.temperature.roundToInt()}°C", color = Ink.Text, fontSize = 40.sp, fontWeight = FontWeight.Light)
                Text(weatherLabel(w.code), color = Gold.Light, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Terasa ${w.feelsLike.roundToInt()}°C · Lembapan ${w.humidity}% · Angin ${w.windKmh.roundToInt()} km/j",
            color = Ink.Muted, fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            w.daily.drop(1).take(3).forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(DayFmt.format(d.date), color = Ink.Muted, fontSize = 12.sp)
                    Text(weatherIcon(d.code), fontSize = 22.sp)
                    Text("${d.max.roundToInt()}° / ${d.min.roundToInt()}°", color = Ink.Text, fontSize = 12.sp)
                }
            }
        }
    }
}

