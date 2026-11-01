package com.smc.tv.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smc.tv.data.AppSettings
import com.smc.tv.data.Language
import com.smc.tv.data.Location
import com.smc.tv.data.WALLPAPER_TOPICS
import com.smc.tv.data.WeatherRepository
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    // Start on "Kembali" so the on-screen keyboard only opens when a field is chosen.
    val backButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { backButton.requestFocus() } }

    var tvName by remember { mutableStateOf(settings.tvName) }
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

    LazyColumn(
        Modifier.fillMaxSize().background(Ink.Background.copy(alpha = 0.72f)).padding(horizontal = 56.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.settings, color = Gold.Primary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                ActionChip(s.back, modifier = Modifier.focusRequester(backButton), onClick = onBack)
            }
        }

        item {
            Section(s.tvNameSection) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldTextField(
                        value = tvName,
                        onValueChange = { tvName = it },
                        onDone = { onUpdate { s -> s.copy(tvName = tvName.ifBlank { AppSettings.DEFAULT_TV_NAME }) } },
                        modifier = Modifier.width(420.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    ActionChip(s.save) {
                        onUpdate { s -> s.copy(tvName = tvName.ifBlank { AppSettings.DEFAULT_TV_NAME }) }
                    }
                    Spacer(Modifier.width(12.dp))
                    ActionChip(s.reset) {
                        tvName = AppSettings.DEFAULT_TV_NAME
                        onUpdate { s -> s.copy(tvName = AppSettings.DEFAULT_TV_NAME) }
                    }
                }
            }
        }

        item {
            Section(s.locationSection, s.currentLocation(settings.location.name)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GoldTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = s.searchHint,
                        onDone = ::search,
                        modifier = Modifier.width(420.dp),
                    )
                    Spacer(Modifier.width(16.dp))
                    ActionChip(s.search, onClick = ::search)
                    Spacer(Modifier.width(12.dp))
                    ActionChip(s.reset) {
                        onUpdate { s -> s.copy(location = AppSettings.DEFAULT_LOCATION) }
                    }
                }
                searchStatus?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = Ink.Muted, fontSize = 16.sp)
                }
                results.forEach { loc ->
                    Spacer(Modifier.height(10.dp))
                    ActionChip(loc.name, selected = loc.name == settings.location.name) {
                        onUpdate { s -> s.copy(location = loc) }
                        results = emptyList()
                        query = ""
                    }
                }
            }
        }

        item {
            Section(
                s.wallpaperSection,
                if (hasUnsplashKey) s.wallpaperHint else s.wallpaperNoKey,
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(WALLPAPER_TOPICS) { topic ->
                        ActionChip(s.wallpaperTopic(topic), selected = topic == settings.wallpaperTopic) {
                            onUpdate { s -> s.copy(wallpaperTopic = topic, wallpaperSeed = s.wallpaperSeed + 1) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ActionChip(s.changeWallpaperNow) {
                        onUpdate { s -> s.copy(wallpaperSeed = s.wallpaperSeed + 1) }
                    }
                    Spacer(Modifier.width(16.dp))
                    when {
                        wallpaperLoading -> WallpaperLoadingBadge()
                        wallpaperError != null -> Text(s.failed(wallpaperError), color = Ink.Muted, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
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
    }
}

@Composable
private fun Section(title: String, subtitle: String? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Gold.Light, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        if (subtitle != null) Text(subtitle, color = Ink.Muted, fontSize = 13.sp)
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
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = modifier
            .tvFocusable(shape = RoundedCornerShape(12.dp), focusedScale = 1.04f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun GoldTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = placeholder?.let { { Text(it, color = Ink.Muted) } },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold.Primary,
            unfocusedBorderColor = Gold.Dim,
            focusedTextColor = Ink.Text,
            unfocusedTextColor = Ink.Text,
            cursorColor = Gold.Primary,
        ),
        modifier = modifier,
    )
}
