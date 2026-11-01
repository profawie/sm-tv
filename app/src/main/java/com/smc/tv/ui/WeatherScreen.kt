package com.smc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smc.tv.data.Location
import com.smc.tv.data.Weather
import com.smc.tv.data.weatherIcon
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.tan

private val CardShape = RoundedCornerShape(18.dp)
private val HourFmt = DateTimeFormatter.ofPattern("h a", Locale.ENGLISH)
private val ClockFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

/** Full-screen weather: summary, details, next 24 hours and a rain radar map. */
@Composable
fun WeatherScreen(
    state: Loadable<Weather>,
    location: Location,
    radarTileTemplate: String?,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    val backButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backButton.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.Background.copy(alpha = 0.72f))
            .padding(horizontal = 40.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.weatherTitle, color = Gold.Primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(location.name, color = Ink.Muted, fontSize = 11.sp)
            }
            Text(
                s.back,
                color = Ink.Text,
                fontSize = 14.sp,
                modifier = Modifier
                    .focusRequester(backButton)
                    .tvFocusable(shape = RoundedCornerShape(12.dp), focusedScale = 1.04f, onClick = onBack)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(10.dp))

        val w = (state as? Loadable.Ready)?.value
        if (w == null) {
            Text(if (state is Loadable.Failed) s.loadFailed(state.message) else s.loading, color = Ink.Muted, fontSize = 14.sp)
            return@Column
        }

        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.width(440.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryCard(w)
                DetailsCard(w, Modifier.weight(1f))
            }
            Card(s.rainRadar, Modifier.weight(1f).fillMaxHeight()) {
                RadarMap(location, radarTileTemplate, Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
            }
        }
        Spacer(Modifier.height(10.dp))
        Card(s.next24Hours, Modifier.fillMaxWidth()) {
            // Every second hour, so the whole day fits without scrolling.
            Row(Modifier.fillMaxWidth()) {
                w.hourly.filterIndexed { i, _ -> i % 2 == 0 }.forEach { h ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(HourFmt.format(h.time), color = Ink.Muted, fontSize = 9.sp)
                        Text(weatherIcon(h.code, h.isDay), fontSize = 14.sp)
                        Text("${h.temperature.roundToInt()}°", color = Ink.Text, fontSize = 12.sp)
                        Text("💧${h.rainChance}%", color = if (h.rainChance >= 50) Gold.Light else Ink.Muted, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .background(Ink.Panel, CardShape)
            .border(1.dp, Gold.Dim.copy(alpha = 0.5f), CardShape)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(title, color = Gold.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        content()
    }
}

@Composable
private fun SummaryCard(w: Weather) {
    val s = LocalStrings.current
    val today = w.daily.firstOrNull()
    Card(s.today, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(weatherIcon(w.code, w.isDay), fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Text("${w.temperature.roundToInt()}°C", color = Ink.Text, fontSize = 28.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(s.weatherLabel(w.code), color = Gold.Light, fontSize = 13.sp)
                if (today != null) {
                    Text("↑ ${today.max.roundToInt()}°  ↓ ${today.min.roundToInt()}°", color = Ink.Muted, fontSize = 11.sp)
                }
            }
        }
        if (today != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                s.weatherSummary(
                    s.weatherLabel(today.code), today.min.roundToInt(), today.max.roundToInt(),
                    today.rainChance, today.rainMm,
                ),
                color = Ink.Text, fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun DetailsCard(w: Weather, modifier: Modifier) {
    val s = LocalStrings.current
    val today = w.daily.firstOrNull()
    val compass = s.compass[((w.windDirection % 360) / 45.0).roundToInt() % 8]
    val rows = listOfNotNull(
        s.feelsLike to "${w.feelsLike.roundToInt()}°C",
        s.humidity to "${w.humidity}%",
        s.wind to "${w.windKmh.roundToInt()} km/h $compass",
        s.pressure to "${w.pressureHpa.roundToInt()} hPa",
        s.visibility to "${"%.1f".format(w.visibilityKm)} km",
        s.cloudCover to "${w.cloudCover}%",
        s.uvIndex to "${"%.1f".format(w.uvIndex)} (max ${"%.1f".format(today?.uvMax ?: 0.0)})",
        today?.let { s.rainToday to "${"%.1f".format(it.rainMm)} mm · ${it.rainChance}%" },
        today?.let { s.sunrise to ClockFmt.format(it.sunrise) },
        today?.let { s.sunset to ClockFmt.format(it.sunset) },
    )
    Card(s.details, modifier.fillMaxWidth()) {
        // Two columns of label/value pairs.
        val half = (rows.size + 1) / 2
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(rows.take(half), rows.drop(half)).forEach { column ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    column.forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = Ink.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                            Text(value, color = Ink.Text, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

/** RainViewer's free radar tiles stop at this zoom, so the base map matches it. */
private const val MAP_ZOOM = 7
private val TileSize = 150.dp

/**
 * Esri dark grey base map with the latest RainViewer radar on top, centred on [location].
 * Built from a 5x5 grid of web-mercator tiles shifted so the location sits in the middle.
 */
@Composable
private fun RadarMap(location: Location, radarTemplate: String?, modifier: Modifier) {
    val s = LocalStrings.current
    val n = 1 shl MAP_ZOOM
    val tx = (location.longitude + 180) / 360 * n
    val latRad = location.latitude * PI / 180
    val ty = (1 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2 * n
    val cx = floor(tx).toInt()
    val cy = floor(ty).toInt()
    val fx = (tx - cx).toFloat()
    val fy = (ty - cy).toFloat()
    val radius = 2

    var updated by remember(radarTemplate) { mutableStateOf<String?>(null) }
    LaunchedEffect(radarTemplate) { if (radarTemplate != null) updated = ClockFmt.format(LocalTime.now()) }

    BoxWithConstraints(modifier.background(Color(0xFF0E0E0E))) {
        val gridSize = TileSize * (radius * 2 + 1)
        // Offset that puts the location's pixel at the centre of this box.
        val offsetX = maxWidth / 2 - TileSize * (radius + fx)
        val offsetY = maxHeight / 2 - TileSize * (radius + fy)
        Box(
            Modifier
                .requiredSize(gridSize)
                .align(Alignment.TopStart)
                .offset(x = offsetX + (gridSize - maxWidth) / 2, y = offsetY + (gridSize - maxHeight) / 2)
        ) {
            for (dy in -radius..radius) for (dx in -radius..radius) {
                val x = ((cx + dx) % n + n) % n
                val y = cy + dy
                if (y !in 0 until n) continue
                val tileModifier = Modifier
                    .offset(x = TileSize * (dx + radius), y = TileSize * (dy + radius))
                    .size(TileSize)
                AsyncImage(
                    model = "https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/$MAP_ZOOM/$y/$x",
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = tileModifier,
                )
                if (radarTemplate != null) {
                    AsyncImage(
                        model = radarTemplate.replace("{z}", "$MAP_ZOOM").replace("{x}", "$x").replace("{y}", "$y"),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        alpha = 0.75f,
                        modifier = tileModifier,
                    )
                }
            }
        }
        // Location marker.
        Box(
            Modifier
                .align(Alignment.Center)
                .size(14.dp)
                .background(Gold.Primary, CircleShape)
                .border(3.dp, Color.Black.copy(alpha = 0.6f), CircleShape)
        )
        Text(
            listOfNotNull(updated?.let { s.radarUpdated(it) }, "Esri, HERE, Garmin · RainViewer").joinToString(" · "),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 9.sp,
            modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.5f)).padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
