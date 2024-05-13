package com.truvideo.sdk.camera.ui.media_panel_preview

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.animated_fit.AnimatedFit
import com.truvideo.sdk.camera.ui.panel.Panel
import com.truvideo.sdk.camera.ui.video_player.VideoPlayer
import truvideo.sdk.components.icon_button.TruvideoIconButton
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaPanelPreview(
    visible: Boolean = false,
    media: TruvideoSdkCameraMedia? = null,
    onDelete: (() -> Unit)? = null,
    orientation: TruvideoSdkCameraOrientation,
    close: (() -> Unit)? = null
) {
    val videoAspectRatio = media?.fixedResolution?.aspectRatio ?: 1f

    Panel(
        visible = visible,
        onBackgroundPressed = close
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {

                }
        ) {

            if (media != null) {
                AnimatedFit(
                    aspectRatio = videoAspectRatio,
                    rotation = orientation.uiRotation,
                    modifier = Modifier.fillMaxSize(),
                    contentModifier = Modifier.fillMaxSize()
                ) {
                    if (media.type.isVideo) {
                        VideoPlayer(
                            uri = Uri.fromFile(
                                File(media.filePath)
                            )
                        )
                    } else {
                        GlideImage(
                            model = Uri.fromFile(File(media.filePath)),
                            contentDescription = "",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            }

            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Row {
                    TruvideoIconButton(
                        imageVector = Icons.Default.DeleteOutline,
                        rotation = orientation.uiRotation,
                        onPressed = {
                            if (onDelete != null) {
                                onDelete()
                            }
                        }
                    )
                    Box(modifier = Modifier.width(8.dp))
                    TruvideoIconButton(
                        imageVector = Icons.Default.Close,
                        rotation = orientation.uiRotation,
                        onPressed = {
                            if (close != null) close()
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    val orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    val media = TruvideoSdkCameraMedia(
        createdAt = 0L,
        type = TruvideoSdkCameraMediaType.VIDEO,
        cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
        filePath = "",
        resolution = TruvideoSdkCameraResolution(100, 100),
        rotation = TruvideoSdkCameraOrientation.PORTRAIT,
        duration = 1000L
    )

    Column {

        Box(
            modifier = Modifier
                .width(300.dp)
                .height(500.dp)
        ) {
            MediaPanelPreview(orientation = orientation, visible = visible, media = media, close = {
                visible = false
            })
        }
        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
    }

}