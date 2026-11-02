@file:OptIn(ExperimentalLayoutApi::class)

package com.smc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.smc.tv.BuildConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smc.tv.data.AppSettings
import com.smc.tv.data.CctvStream
import com.smc.tv.data.rtspUrl
import com.smc.tv.data.Language
import com.smc.tv.data.Location
import com.smc.tv.data.WALLPAPER_TOPICS
import com.smc.tv.data.WeatherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SettingsPage { GENERAL, LOCATION, WALLPAPER, CCTV, SOLAR, ABOUT }

/**
 * Two-pane settings in the Google TV style: a menu on the left, and the selected page on the
 * right. Moving focus through the menu switches the page; D-pad right enters the page.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    hasUnsplashKey: Boolean,
    wallpaperLoading: Boolean,
    wallpaperError: String?,
    weatherRepo: WeatherRepository,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    val s = LocalStrings.current
    var page by remember { mutableStateOf(SettingsPage.GENERAL) }
    val firstItem = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstItem.requestFocus() } }

    Row(Modifier.fillMaxSize().background(Ink.Background.copy(alpha = 0.72f))) {
        Column(
            Modifier
                .width(300.dp)
                .fillMaxHeight()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(s.settings, color = Gold.Primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text(settings.tvName, color = Ink.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            val items = listOf(
                Triple(SettingsPage.GENERAL, Icons.Filled.Tv, s.general),
                Triple(SettingsPage.LOCATION, Icons.Filled.Place, s.locationSection),
                Triple(SettingsPage.WALLPAPER, Icons.Filled.Wallpaper, s.wallpaperSection),
                Triple(SettingsPage.CCTV, Icons.Filled.Videocam, s.cctvSection),
                Triple(SettingsPage.SOLAR, Icons.Filled.SolarPower, s.solarSection),
                Triple(SettingsPage.ABOUT, Icons.Filled.Info, s.about),
            )
            items.forEachIndexed { i, (p, icon, label) ->
                MenuItem(
                    icon = icon,
                    label = label,
                    selected = page == p,
                    modifier = if (i == 0) Modifier.focusRequester(firstItem) else Modifier,
                    onFocus = { page = p },
                )
            }
            Spacer(Modifier.weight(1f))
            MenuItem(icon = Icons.AutoMirrored.Filled.ArrowBack, label = s.back.removePrefix("← "), selected = false, onClick = onBack)
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxHeight().padding(horizontal = 40.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when (page) {
                SettingsPage.GENERAL -> {
                    item { TvNameSection(settings, onUpdate) }
                    item { LanguageSection(settings, onUpdate) }
                }
                SettingsPage.LOCATION -> item { LocationSection(settings, weatherRepo, onUpdate) }
                SettingsPage.WALLPAPER -> item {
                    WallpaperSection(settings, hasUnsplashKey, wallpaperLoading, wallpaperError, onUpdate)
                }
                SettingsPage.CCTV -> item { CctvSection(settings, onUpdate) }
                SettingsPage.SOLAR -> item { SolarSection(settings, onUpdate) }
                SettingsPage.ABOUT -> item { AboutSection() }
            }
        }
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onClick: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocus()
            }
            .background(
                when {
                    focused -> Gold.Primary
                    selected -> Gold.Primary.copy(alpha = 0.15f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(12.dp),
            )
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = when {
            focused -> Color.Black
            selected -> Gold.Primary
            else -> Ink.Text
        }
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = color, fontSize = 14.sp, fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun TvNameSection(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val s = LocalStrings.current
    var tvName by remember { mutableStateOf(settings.tvName) }
    fun save() = onUpdate { it.copy(tvName = tvName.ifBlank { AppSettings.DEFAULT_TV_NAME }) }
    Section(s.tvNameSection) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldTextField(value = tvName, onValueChange = { tvName = it }, onDone = ::save, modifier = Modifier.width(360.dp))
            ActionChip(s.save, onClick = ::save)
            ActionChip(s.reset) {
                tvName = AppSettings.DEFAULT_TV_NAME
                onUpdate { it.copy(tvName = AppSettings.DEFAULT_TV_NAME) }
            }
        }
    }
}

@Composable
private fun LanguageSection(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val s = LocalStrings.current
    Section(s.languageSection) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionChip("Bahasa Melayu", selected = settings.language == Language.MS) {
                onUpdate { it.copy(language = Language.MS) }
            }
            ActionChip("English", selected = settings.language == Language.EN) {
                onUpdate { it.copy(language = Language.EN) }
            }
        }
    }
}

@Composable
private fun LocationSection(
    settings: AppSettings,
    weatherRepo: WeatherRepository,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Location>>(emptyList()) }
    var searchStatus by remember { mutableStateOf<String?>(null) }

    fun search() {
        if (query.isBlank()) return
        searchStatus = s.searching
        scope.launch {
            runCatching { weatherRepo.searchPlaces(query) }
                .onSuccess {
                    results = it
                    searchStatus = if (it.isEmpty()) s.noResults(query) else null
                }
                .onFailure { searchStatus = s.searchFailed(it.message) }
        }
    }

    Section(s.locationSection, s.currentLocation(settings.location.name)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = s.searchHint,
                onDone = ::search,
                modifier = Modifier.width(400.dp),
            )
            ActionChip(s.search, onClick = ::search)
            ActionChip(s.reset) { onUpdate { it.copy(location = AppSettings.DEFAULT_LOCATION) } }
        }
        searchStatus?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Ink.Muted, fontSize = 14.sp)
        }
        results.forEach { loc ->
            Spacer(Modifier.height(10.dp))
            ActionChip(loc.name, selected = loc.name == settings.location.name) {
                onUpdate { it.copy(location = loc) }
                results = emptyList()
                query = ""
            }
        }
    }
}

@Composable
private fun WallpaperSection(
    settings: AppSettings,
    hasUnsplashKey: Boolean,
    wallpaperLoading: Boolean,
    wallpaperError: String?,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    val s = LocalStrings.current
    Section(s.wallpaperSection, if (hasUnsplashKey) s.wallpaperHint else s.wallpaperNoKey) {
        // Wraps into rows instead of scrolling sideways, so every topic is visible.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WALLPAPER_TOPICS.forEach { topic ->
                ActionChip(s.wallpaperTopic(topic), selected = topic == settings.wallpaperTopic) {
                    onUpdate { it.copy(wallpaperTopic = topic, wallpaperSeed = it.wallpaperSeed + 1) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ActionChip(s.changeWallpaperNow) { onUpdate { it.copy(wallpaperSeed = it.wallpaperSeed + 1) } }
            Spacer(Modifier.width(16.dp))
            when {
                wallpaperLoading -> WallpaperLoadingBadge()
                wallpaperError != null -> Text(s.failed(wallpaperError), color = Ink.Muted, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun CctvSection(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val s = LocalStrings.current
    var cctvHost by remember { mutableStateOf(settings.cctv.host) }
    var cctvUser by remember { mutableStateOf(settings.cctv.username) }
    var cctvPassword by remember { mutableStateOf(settings.cctv.password) }

    fun saveCctv() {
        onUpdate {
            it.copy(cctv = it.cctv.copy(host = cctvHost.trim(), username = cctvUser.trim(), password = cctvPassword))
        }
    }

    Section(s.cctvSection, s.cctvHint) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldTextField(
                value = cctvHost,
                onValueChange = { cctvHost = it },
                onDone = ::saveCctv,
                label = s.cctvAddress,
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.width(170.dp),
            )
            GoldTextField(
                value = cctvUser,
                onValueChange = { cctvUser = it },
                onDone = ::saveCctv,
                label = s.cctvUsername,
                modifier = Modifier.width(130.dp),
            )
            GoldTextField(
                value = cctvPassword,
                onValueChange = { cctvPassword = it },
                onDone = ::saveCctv,
                label = s.cctvPassword,
                isPassword = true,
                modifier = Modifier.width(160.dp),
            )
            ActionChip(s.save, onClick = ::saveCctv)
        }
        if (settings.cctv.password.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("✓ ${s.cctvPasswordSet}", color = Gold.Light, fontSize = 13.sp)
        }
        Spacer(Modifier.height(14.dp))
        Text(s.cctvChannel, color = Ink.Muted, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Live preview of the chosen camera, so picking a channel shows it straight away.
            CctvPlayer(
                url = settings.cctv.rtspUrl(CctvStream.SUB),
                modifier = Modifier
                    .width(320.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            FlowRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..CCTV_MAX_CHANNELS).forEach { ch ->
                    ActionChip("$ch", selected = ch == settings.cctv.channel, modifier = Modifier.width(48.dp)) {
                        onUpdate { it.copy(cctv = it.cctv.copy(channel = ch)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SolarSection(settings: AppSettings, onUpdate: ((AppSettings) -> AppSettings) -> Unit) {
    val s = LocalStrings.current
    var token by remember { mutableStateOf(settings.solarToken) }
    fun save() = onUpdate { it.copy(solarToken = token.trim()) }
    Section(s.solarSection, s.solarHint) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoldTextField(
                value = token,
                onValueChange = { token = it },
                onDone = ::save,
                label = s.solarToken,
                isPassword = true,
                modifier = Modifier.width(420.dp),
            )
            ActionChip(s.save, onClick = ::save)
        }
        if (settings.solarToken.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text("✓ ${s.solarTokenSaved}", color = Gold.Light, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AboutSection() {
    val s = LocalStrings.current
    Section(s.about) {
        Text("SM TV", color = Ink.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("${s.version} ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Gold.Light, fontSize = 15.sp)
        Text(BuildConfig.APPLICATION_ID, color = Ink.Muted, fontSize = 12.sp)
    }
}

/** Covers 4, 8 and 16 channel recorders; unused channels simply fail to connect. */
private const val CCTV_MAX_CHANNELS = 16

@Composable
private fun Section(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Gold.Light, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        if (subtitle != null) Text(subtitle, color = Ink.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ActionChip(
    label: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        label,
        color = if (selected) Gold.Primary else Ink.Text,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        softWrap = false,
        textAlign = TextAlign.Center,
        modifier = modifier
            .tvFocusable(shape = RoundedCornerShape(10.dp), focusedScale = 1.04f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * Text field for a remote: D-pad focus only highlights it (it stays read-only, so no
 * keyboard). OK switches it to editing and opens the keyboard; Done or leaving the field
 * switches back, so moving through Settings doesn't keep popping the keyboard up.
 */
@Composable
private fun GoldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    label: String? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text,
) {
    var editing by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(editing) {
        if (editing) {
            // Newer Android TV (e.g. Chromecast on Android 14) doesn't always open the
            // keyboard by itself when a field becomes editable, so ask for it explicitly.
            delay(100)
            keyboard?.show()
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        readOnly = !editing,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        placeholder = placeholder?.let { { Text(it, color = Ink.Muted, fontSize = 13.sp) } },
        label = label?.let { { Text(it, color = Ink.Muted, fontSize = 12.sp) } },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onDone()
            editing = false
            keyboard?.hide()
        }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold.Primary,
            unfocusedBorderColor = Gold.Dim,
            focusedTextColor = Ink.Text,
            unfocusedTextColor = Ink.Text,
            cursorColor = Gold.Primary,
        ),
        modifier = modifier
            .onFocusChanged { if (!it.isFocused) editing = false }
            .onPreviewKeyEvent { event ->
                val isOk = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!editing && isOk) {
                    if (event.type == KeyEventType.KeyUp) editing = true
                    true
                } else {
                    false
                }
            },
    )
}
