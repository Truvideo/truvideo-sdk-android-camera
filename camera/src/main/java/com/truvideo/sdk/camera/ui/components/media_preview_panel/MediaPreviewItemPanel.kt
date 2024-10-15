package com.truvideo.sdk.camera.ui.components.media_preview_panel

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.ui.components.video_player.VideoPlayer
import java.io.File
import java.util.UUID

@Composable
fun MediaPreviewItemPanel(
    media: TruvideoSdkCameraMedia,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (media.type.isVideo) {
            Video(media)
        } else if (media.type.isPicture) {
            Image(media)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BoxScope.Image(media: TruvideoSdkCameraMedia) {
    val view = LocalView.current
    val aspectRatio = remember(media) { media.fixedResolution.aspectRatio }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .align(Alignment.Center)
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) {
        if (!view.isInEditMode) {
            val uri = remember(media) { Uri.fromFile(File(media.filePath)) }
            GlideImage(
                model = uri,
                contentDescription = "",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Video(media: TruvideoSdkCameraMedia) {
    val view = LocalView.current
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    val uri = remember(media) { Uri.fromFile(File(media.filePath)) }
    var controlVisible by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        if (exoPlayer != null) {
            Log.d("TruvideoSdkCamera", "Disposing exoplayer. Uri changed")
            exoPlayer?.release()
            exoPlayer = null
        }

        if (!view.isInEditMode) {
            try {
                Log.d("TruvideoSdkCamera", "Creating exoplayer")
                val player = ExoPlayer.Builder(context).build()
                val mediaItem = MediaItem.fromUri(uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                exoPlayer = player
            } catch (exception: Exception) {
                Log.d("TruvideoSdkCamera", "Error creating exoplayer", exception)
                exception.printStackTrace()
                exoPlayer = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (exoPlayer != null) {
                Log.d("TruvideoSdkCamera", "Disposing exoplayer. Disposed component")
                exoPlayer?.release()
                exoPlayer = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() })
            {
                controlVisible = !controlVisible
            }
    ) {
        VideoPlayer(
            exoPlayer = exoPlayer,
            controlVisible = controlVisible
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    val media = TruvideoSdkCameraMedia(
        id = UUID.randomUUID().toString(),
        createdAt = 0L,
        type = TruvideoSdkCameraMediaType.PICTURE,
        cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
        filePath = "",
        resolution = TruvideoSdkCameraResolution(100, 500),
        rotation = TruvideoSdkCameraOrientation.PORTRAIT,
        duration = 1000L
    )

    TruVideoSdkCameraTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            MediaPreviewItemPanel(
                media = media
            )
        }
    }
}