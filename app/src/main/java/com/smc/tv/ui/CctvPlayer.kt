package com.smc.tv.ui

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.smc.tv.data.CctvSettings
import com.smc.tv.data.CctvStream
import com.smc.tv.data.rtspUrl
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private enum class CctvState { CONNECTING, PLAYING, ERROR }

/**
 * Live RTSP view of one CCTV channel. Plays only while the activity is started, so the
 * decoder is released when Netflix/YouTube take over, and retries after a dropped stream.
 */
@OptIn(UnstableApi::class)
@Composable
fun CctvPlayer(url: String?, modifier: Modifier = Modifier, muted: Boolean = true) {
    val s = LocalStrings.current
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (url == null) {
            Text(s.cctvNoPassword, color = Ink.Muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
            return@Box
        }

        val context = LocalContext.current
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        var state by remember(url) { mutableStateOf(CctvState.CONNECTING) }
        var retry by remember(url) { mutableStateOf(0) }

        val player = remember(url) {
            ExoPlayer.Builder(context).build().apply {
                volume = if (muted) 0f else 1f
                playWhenReady = true
            }
        }

        DisposableEffect(player, retry) {
            val source = RtspMediaSource.Factory()
                // TCP is more reliable than UDP across the 192.168.1.x / 2.x subnets.
                .setForceUseRtpTcp(true)
                .createMediaSource(MediaItem.fromUri(url))
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) state = CctvState.PLAYING
                }

                override fun onPlayerError(error: PlaybackException) {
                    state = CctvState.ERROR
                }
            }
            player.addListener(listener)

            // Adding the observer replays ON_START if we're already started, which starts playback.
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        state = CctvState.CONNECTING
                        player.setMediaSource(source)
                        player.prepare()
                    }
                    Lifecycle.Event.ON_STOP -> player.stop()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                player.removeListener(listener)
                player.stop()
            }
        }
        DisposableEffect(player) { onDispose { player.release() } }

        // Reconnect a few seconds after an error (camera reboot, network blip).
        LaunchedEffect(state) {
            if (state == CctvState.ERROR) {
                delay(5_000)
                state = CctvState.CONNECTING
                retry++
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        when (state) {
            CctvState.CONNECTING -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Gold.Primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(6.dp))
                Text(s.cctvConnecting, color = Ink.Muted, fontSize = 11.sp)
            }
            CctvState.ERROR -> Text(s.cctvError, color = Ink.Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
            CctvState.PLAYING -> Unit
        }
    }
}

@Composable
fun CctvWidget(cctv: CctvSettings, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val s = LocalStrings.current
    WidgetPanel(title = s.cctvTitle, subtitle = s.camera(cctv.channel), modifier = modifier, onClick = onClick) {
        CctvPlayer(
            url = cctv.rtspUrl(CctvStream.SUB),
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
        )
    }
}

/** Full-screen live view of the camera chosen in Settings, using the main (HD) stream. */
@Composable
fun CctvScreen(cctv: CctvSettings, onBack: () -> Unit) {
    val s = LocalStrings.current
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CctvPlayer(url = cctv.rtspUrl(CctvStream.MAIN), modifier = Modifier.fillMaxSize())
        Text(
            "${s.cctvTitle} · ${s.camera(cctv.channel)}",
            color = Gold.Primary,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        val back = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { back.requestFocus() } }
        Text(
            s.back,
            color = Ink.Text,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
                .focusRequester(back)
                .tvFocusable(shape = RoundedCornerShape(12.dp), focusedScale = 1.04f, onClick = onBack)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
