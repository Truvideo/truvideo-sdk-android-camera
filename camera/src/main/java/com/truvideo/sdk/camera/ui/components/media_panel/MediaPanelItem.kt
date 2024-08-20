package com.truvideo.sdk.camera.ui.components.media_panel

import android.net.Uri
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.TruvideoColors
import java.io.File
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaPanelItem(
    media: TruvideoSdkCameraMedia,
    onPressed: (() -> Unit) = {},
) {
    val ripple = rememberRipple(color = Color.White)

    CompositionLocalProvider(LocalIndication provides ripple) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .clip(shape = RoundedCornerShape(8.dp))
                .background(color = TruvideoColors.gray)
                .clickable { onPressed() }
        ) {
            GlideImage(
                model = Uri.fromFile(File(media.filePath)),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (media.type.isVideo)
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(shape = RoundedCornerShape(4.dp))
                        .background(color = Color(0xFF616161))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Text(
                        formatDuration(media.duration.toDuration(DurationUnit.MILLISECONDS)),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
        }
    }
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.inWholeHours
    val minutes = (duration - hours.hours).inWholeMinutes
    val seconds = (duration - hours.hours - minutes.minutes).inWholeSeconds
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
@Preview
private fun Test() {
    TruVideoSdkCameraTheme {
        MediaPanelItem(
            media = TruvideoSdkCameraMedia(
                id = UUID.randomUUID().toString(),
                createdAt = 0L,
                type = TruvideoSdkCameraMediaType.VIDEO,
                cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
                filePath = "",
                resolution = TruvideoSdkCameraResolution(100, 100),
                rotation = TruvideoSdkCameraOrientation.PORTRAIT,
                duration = 1000L
            ),
        )
    }
}