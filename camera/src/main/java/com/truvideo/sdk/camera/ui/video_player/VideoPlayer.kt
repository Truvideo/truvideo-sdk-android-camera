package com.truvideo.sdk.camera.ui.video_player

import android.net.Uri
import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import truvideo.sdk.components.scale_button.TruvideoScaleButton
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Composable
fun VideoPlayer(
    uri: Uri, modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var videoReady by remember { mutableStateOf(false) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var error by remember { mutableStateOf(false) }
    var videoIsPlaying by remember { mutableStateOf(false) }
    var playerPosition by remember { mutableLongStateOf(0L) }
    var playerDuration by remember { mutableLongStateOf(0L) }
    val sliderInteractionSource = remember { MutableInteractionSource() }
    val isPressed by sliderInteractionSource.collectIsPressedAsState()
    val isDragged by sliderInteractionSource.collectIsDraggedAsState()
    val sliderIsInteracting by remember { derivedStateOf { isPressed || isDragged } }
    var sliderTempValue by remember { mutableFloatStateOf(0f) }
    val playerPercentage by remember(exoPlayer, videoReady, playerDuration, sliderIsInteracting, sliderTempValue, playerPosition) {
        derivedStateOf {
            if (exoPlayer == null || !videoReady || playerDuration == 0L) {
                0f
            } else {
                if (sliderIsInteracting) {
                    sliderTempValue
                } else {
                    playerPosition.toFloat() / playerDuration.toFloat()
                }
            }
        }
    }

    LaunchedEffect(uri) {
        error = false

        if (exoPlayer != null) {
            Log.d("CameraService", "Disposing previous exoplayer")
            exoPlayer?.release()
            exoPlayer = null
        }

        try {
            Log.d("CameraService", "creating new exoplayer")
            val player = ExoPlayer.Builder(context).build()
            val mediaItem = MediaItem.fromUri(uri)
            player.setMediaItem(mediaItem)
            player.prepare()

            player.addListener(object : Player.Listener {

                override fun onPlaybackStateChanged(playbackState: Int) {
                    super.onPlaybackStateChanged(playbackState)
                    videoReady = true
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    super.onIsPlayingChanged(isPlaying)
                    videoIsPlaying = isPlaying
                }
            })

            player.play()
            exoPlayer = player
        } catch (exception: Exception) {
            Log.d("CameraService", "Error creating exoplayer", exception)
            exception.printStackTrace()
            exoPlayer = null
            error = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            playerPosition = exoPlayer?.currentPosition ?: 0L
            playerDuration = exoPlayer?.duration ?: 0L
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            error = false
            if (exoPlayer != null) {
                Log.d("CameraService", "Disposing exoplayer")
                exoPlayer?.release()
                exoPlayer = null
            }
        }
    }

    Box(
        modifier = modifier
            .background(color = Color.Black)
            .fillMaxSize()
    ) {
        if (exoPlayer != null) {
            TruvideoAnimatedFadeVisibility(visible = videoReady) {
                Box(
                    modifier = Modifier
                        .background(color = Color.Transparent)
                        .clip(RoundedCornerShape(0.dp))
                ) {

                    VideoSurface(
                        player = exoPlayer!!,
                        modifier = Modifier
                            .clip(shape = RoundedCornerShape(0.dp))
                            .align(Alignment.Center)
                            .fillMaxSize(),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .clip(CircleShape)
                .background(color = Color.Black.copy(0.7f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .align(Alignment.BottomCenter)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                TruvideoScaleButton(
                    onPressed = {
                        val player = exoPlayer ?: return@TruvideoScaleButton

                        if (player.isPlaying) {
                            player.pause()
                        } else {
                            val duration = player.duration
                            val position = player.currentPosition
                            if ((duration - position).absoluteValue < 50) {
                                Log.d("CameraService", "Seeking to 0")
                                player.seekTo(0)
                            }

                            player.play()
                        }
                    }
                ) {

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color = Color.Black.copy(0.7f))
                    ) {
                        Icon(
                            imageVector = if (videoIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "",
                            tint = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Box(modifier = Modifier.width(8.dp))



                Slider(
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFC107),
                        activeTrackColor = Color(0xFFFFC107),
                    ),
                    value = playerPercentage,
                    onValueChange = {
                        val player = exoPlayer ?: return@Slider
                        val position = player.duration * it
                        sliderTempValue = it

                        if (player.isPlaying) {
                            player.pause()
                        }

                        player.seekTo(position.toLong())
                    },
                    interactionSource = sliderInteractionSource
                )

                Box(modifier = Modifier.width(8.dp))

                Text(
                    formatDuration(playerPosition.toInt().toDuration(DurationUnit.MILLISECONDS)),
                    color = Color.White,
                    fontSize = 12.sp
                )
                Box(modifier = Modifier.width(8.dp))

            }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    Log.d("CameraService", "Format duration $duration ${duration.inWholeMilliseconds}")

    if (duration == ZERO) return "00:00"

    val hours = duration.inWholeHours
    val minutes = (duration - hours.hours).inWholeMinutes
    val seconds = (duration - hours.hours - minutes.minutes).inWholeSeconds
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

@Composable
private fun VideoSurface(
    player: Player, modifier: Modifier
) {
    val context = LocalContext.current

    val videoView = remember {
        val view = TextureView(context)
        view.tag = player
        player.setVideoTextureView(view)
        view
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier,
    )
    DisposableEffect(Unit) {
        onDispose {
            player.clearVideoTextureView(videoView)
        }
    }

}

@Composable
@Preview
private fun Test() {
    var uri by remember { mutableStateOf(Uri.parse("")) }

    Box(modifier = Modifier.fillMaxSize()) {

        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(0.dp))
                    .weight(1f)
            ) {
                VideoPlayer(
                    uri, modifier = Modifier
                )
            }

            Text("Uri: $uri", modifier = Modifier
                .background(color = Color.White)
                .fillMaxWidth()
                .clickable {
                    uri = Uri.parse("https://file-examples.com/storage/fedf16213165ce2d096e19a/2017/04/file_example_MP4_480_1_5MG.mp4")
                }
                .padding(16.dp)

            )

        }
    }


}