package com.truvideo.sdk.camera.ui.media_panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.panel.Panel
import com.truvideo.sdk.components.icon_button.TruvideoIconButton

@Composable
fun MediaPanel(
    visible: Boolean = true,
    media: List<TruvideoSdkCameraMedia> = listOf(),
    onPressed: ((media: TruvideoSdkCameraMedia) -> Unit)? = null,
    orientation: TruvideoSdkCameraOrientation,
    close: (() -> Unit)? = null,
) {
    Panel(
        visible = visible,
        onBackgroundPressed = close,
    ) {
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize()
        ) {
            Box {
                Column {
                    // Close button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        TruvideoIconButton(
                            imageVector = Icons.Default.Close,
                            rotation = orientation.uiRotation,
                            onPressed = {
                                if (close != null) {
                                    close()
                                }
                            }
                        )
                    }

                    // List of media
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
                        ) {
                            items(media.size) {
                                Box(
                                    modifier = Modifier
                                        .padding(
                                            end = (if (it % 2 == 0) 4f else 0f).dp,
                                            start = (if (it % 2 == 0) 0f else 4f).dp,
                                            bottom = 8.dp
                                        )
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center

                                ) {
                                    MediaPanelItem(
                                        media = media[it],
                                        rotation = orientation.uiRotation,
                                        onPressed = {
                                            if (onPressed != null) {
                                                onPressed(media[it])
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf(generate()) }
    val orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(400.dp)
        ) {
            MediaPanel(
                visible = visible,
                orientation = orientation,
                media = media
            )
        }
        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
        Text("Random media", modifier = Modifier.clickable {
            media = generate()
        })
    }
}

private fun generate(): List<TruvideoSdkCameraMedia> {
    val result = mutableListOf<TruvideoSdkCameraMedia>()
    val numberOfResolutions = (1..20).random()
    for (i in 1..numberOfResolutions) {
        val width = (100..4000).random()
        val height = (100..4000).random()
        val lensFacing = TruvideoSdkCameraLensFacing.entries.random()
        val type = TruvideoSdkCameraMediaType.entries.random()

        result.add(
            TruvideoSdkCameraMedia(
                resolution = TruvideoSdkCameraResolution(width, height),
                filePath = "",
                cameraLensFacing = lensFacing,
                type = type,
                createdAt = 0,
                rotation = TruvideoSdkCameraOrientation.PORTRAIT,
                duration = (100..4000).random().toLong(),
            )
        )
    }
    return result
}