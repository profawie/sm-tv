package com.smc.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

object Gold {
    val Primary = Color(0xFFD4AF37)
    val Light = Color(0xFFF1D78A)
    val Dim = Color(0xFF8C7325)
}

object Ink {
    val Background = Color(0xFF0A0A0A)
    val Panel = Color(0xCC111111)
    /** Lighter panel for the home widgets so the wallpaper shows through. */
    val Glass = Color(0xB30D0D0D)
    val PanelFocused = Color(0xE6221B0B)
    val Text = Color(0xFFF5F0E1)
    val Muted = Color(0xFFA8A090)
}

private val SmTvColors = darkColorScheme(
    primary = Gold.Primary,
    onPrimary = Color.Black,
    secondary = Gold.Light,
    background = Ink.Background,
    onBackground = Ink.Text,
    surface = Color(0xFF151515),
    onSurface = Ink.Text,
    onSurfaceVariant = Ink.Muted,
    outline = Gold.Dim,
)

@Composable
fun SmTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SmTvColors, content = content)
}

/**
 * D-pad friendly clickable: gold border + slight zoom when focused.
 * Compose's clickable already treats DPAD_CENTER / Enter as a click.
 */
fun Modifier.tvFocusable(
    shape: Shape = RoundedCornerShape(16.dp),
    focusedScale: Float = 1.06f,
    onFocus: (Boolean) -> Unit = {},
    onClick: () -> Unit,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) focusedScale else 1f, label = "focusScale")
    this
        .scale(scale)
        .onFocusChanged {
            focused = it.isFocused
            onFocus(it.isFocused)
        }
        .clip(shape)
        .background(if (focused) Ink.PanelFocused else Ink.Panel, shape)
        .border(if (focused) 3.dp else 1.dp, if (focused) Gold.Primary else Gold.Dim.copy(alpha = 0.4f), shape)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
}
