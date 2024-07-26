package com.truvideo.sdk.camera.ui.components.media_panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.camera.utils.dpToPx
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.min

@Composable
fun MediaPanel(
    visible: Boolean = true,
    media: ImmutableList<TruvideoSdkCameraMedia> = persistentListOf(),
    onPressed: (media: TruvideoSdkCameraMedia) -> Unit = {},
    orientation: TruvideoSdkCameraOrientation = TruvideoSdkCameraOrientation.PORTRAIT,
    close: (() -> Unit) = {},
) {
    val context = LocalContext.current

    Panel(
        visible = visible,
        orientation = orientation,
        close = { close() },
        content = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val calculatedCount = (context.dpToPx(maxWidth.value) / context.dpToPx(150f)).toInt()
                var count = min(media.size, calculatedCount)
                if (count == 0) count = 1

                LazyVerticalGrid(
                    columns = GridCells.Fixed(count),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    items(media.size) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center

                        ) {
                            MediaPanelItem(
                                media = media[it],
                                onPressed = { onPressed(media[it]) }
                            )
                        }
                    }
                }
            }
        }
    )
}


@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var media by remember { mutableStateOf(generate()) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
        Text("Random media", modifier = Modifier.clickable {
            media = generate()
        })
    }
}

private fun generate(): ImmutableList<TruvideoSdkCameraMedia> {
    val result = mutableListOf<TruvideoSdkCameraMedia>()
    val numberOfResolutions = 10
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
    return result.toPersistentList()
}