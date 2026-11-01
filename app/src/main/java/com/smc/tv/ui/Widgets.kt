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
import com.smc.tv.data.PrayerId
import com.smc.tv.data.Weather
import com.smc.tv.data.weatherIcon
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val PanelShape = RoundedCornerShape(20.dp)
private val TimeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH).withZone(MALAYSIA_ZONE)

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val message: String?) : Loadable<Nothing>
}

@Composable
fun WidgetPanel(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val frame = if (onClick != null) {
        Modifier.tvFocusable(shape = PanelShape, focusedScale = 1.03f, idleBackground = Ink.Glass, onClick = onClick)
    } else {
        Modifier.background(Ink.Glass, PanelShape).border(1.dp, Gold.Dim.copy(alpha = 0.5f), PanelShape)
    }
    Column(
        modifier
            .then(frame)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(title, color = Gold.Primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = Ink.Muted, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun LoadingOrError(state: Loadable<*>) {
    val s = LocalStrings.current
    val msg = when (state) {
        is Loadable.Failed -> s.loadFailed(state.message)
        else -> s.loading
    }
    Text(msg, color = Ink.Muted, fontSize = 13.sp)
}

@Composable
fun PrayerWidget(state: Loadable<PrayerDay>, now: Instant, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val day = (state as? Loadable.Ready)?.value
    WidgetPanel(
        title = s.prayerTitle,
        subtitle = day?.let { "${s.formatHijri(it.hijri)} · ${s.zone(it.zone)}" },
        modifier = modifier,
    ) {
        if (day == null) {
            LoadingOrError(state)
            return@WidgetPanel
        }
        // Sunrise is shown but is not a prayer, so it never counts as "next".
        val next = day.prayers.filter { it.id != PrayerId.SUNRISE }.firstOrNull { it.time.isAfter(now) }
        if (next != null) {
            val left = Duration.between(now, next.time)
            val hours = left.toHours()
            Text(
                s.nextPrayer(s.prayerName(next.id), hours, if (hours > 0) left.toMinutes() % 60 else left.toMinutes() + 1),
                color = Gold.Light, fontSize = 12.sp,
            )
            Spacer(Modifier.height(4.dp))
        }
        day.prayers.forEach { p ->
            val isNext = p == next
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isNext) Gold.Primary.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val color = when {
                    isNext -> Gold.Primary
                    p.time.isBefore(now) -> Ink.Muted
                    else -> Ink.Text
                }
                val weight = if (isNext) FontWeight.Bold else FontWeight.Normal
                Text(s.prayerName(p.id), color = color, fontSize = 13.sp, fontWeight = weight)
                Text(TimeFmt.format(p.time), color = color, fontSize = 13.sp, fontWeight = weight)
            }
        }
    }
}

@Composable
fun WeatherWidget(
    state: Loadable<Weather>,
    locationName: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val s = LocalStrings.current
    val dayFmt = DateTimeFormatter.ofPattern("EEE", s.locale)
    val w = (state as? Loadable.Ready)?.value
    WidgetPanel(title = s.weatherTitle, subtitle = locationName, modifier = modifier, onClick = onClick) {
        if (w == null) {
            LoadingOrError(state)
            return@WidgetPanel
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(weatherIcon(w.code, w.isDay), fontSize = 32.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("${w.temperature.roundToInt()}°C", color = Ink.Text, fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text(s.weatherLabel(w.code), color = Gold.Light, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            s.weatherDetails(w.feelsLike.roundToInt(), w.humidity, w.windKmh.roundToInt()),
            color = Ink.Muted, fontSize = 10.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            w.daily.drop(1).take(3).forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayFmt.format(d.date), color = Ink.Muted, fontSize = 10.sp)
                    Text(weatherIcon(d.code), fontSize = 16.sp)
                    Text("${d.max.roundToInt()}° / ${d.min.roundToInt()}°", color = Ink.Text, fontSize = 10.sp)
                }
            }
        }
    }
}
