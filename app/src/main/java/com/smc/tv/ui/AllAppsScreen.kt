package com.smc.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Every installed TV app, since Google TV's own app list is gone once SM TV is the home screen. */
@Composable
fun AllAppsScreen(onBack: () -> Unit) {
    val s = LocalStrings.current
    val context = LocalContext.current
    val apps = remember { installedTvApps(context) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink.Background.copy(alpha = 0.78f))
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.allApps, color = Gold.Primary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                s.back,
                color = Ink.Text,
                fontSize = 13.sp,
                modifier = Modifier
                    .tvFocusable(shape = RoundedCornerShape(12.dp), focusedScale = 1.04f, onClick = onBack)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { i, app ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppTile(
                        app = app,
                        modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                        onClick = { launchApp(context, app.packageName) },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(app.label, color = Ink.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
