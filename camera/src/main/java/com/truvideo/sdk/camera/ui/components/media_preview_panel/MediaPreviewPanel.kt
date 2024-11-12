package com.truvideo.sdk.camera.ui.components.media_preview_panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.components.media_delete_panel.MediaDeletePanel
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.components.button.TruvideoIconButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewPanel(
    visible: Boolean = false,
    initialIndex: Int = 0,
    media: ImmutableList<TruvideoSdkCameraMedia> = persistentListOf(),
    onDelete: ((media: TruvideoSdkCameraMedia) -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
    close: (() -> Unit) = {}
) {
    BackHandler(visible) {
        close()
    }

    var deleteVisible by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { media.size })
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }
    val currentItem = remember(currentPage, media) {
        try {
            media[currentPage]
        } catch (_: Exception) {
            null
        }
    }
    val currentIsVideo = remember(currentPage) { currentItem?.type?.isVideo ?: false }

    LaunchedEffect(visible, initialIndex) {
        if (visible) {
            pagerState.scrollToPage(initialIndex)
        }
    }

    Panel(
        visible = visible,
        orientation = orientation,
        close = { close() },
        portraitActions = {
            Box(modifier = Modifier.weight(1f))
            TruvideoIconButton(
                icon = Icons.Outlined.DeleteOutline,
                small = true,
                onPressed = {
                    if (currentItem == null) return@TruvideoIconButton
                    deleteVisible = true
                }
            )
        },
        landscapeSecondaryActions = {
            TruvideoIconButton(
                icon = Icons.Outlined.DeleteOutline,
                small = true,
                onPressed = {
                    if (currentItem == null) return@TruvideoIconButton
                    deleteVisible = true
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                {

                }
        ) {
            CompositionLocalProvider(
                LocalOverscrollConfiguration provides OverscrollConfiguration(
                    glowColor = Color.White
                )
            ) {
                HorizontalPager(
                    state = pagerState
                ) { page ->
                    // Our page content
                    key(media[page].filePath) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MediaPreviewItemPanel(media = media[page])
                        }
                    }
                }
            }
        }
    }

    MediaDeletePanel(
        visible = deleteVisible,
        orientation = orientation,
        isVideo = currentIsVideo,
        onDelete = {
            deleteVisible = false

            if (currentItem == null) return@MediaDeletePanel
            onDelete(currentItem)
        },
        close = { deleteVisible = false },
    )
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }
    var media by remember {
        mutableStateOf<ImmutableList<TruvideoSdkCameraMedia>>(
            persistentListOf(
                TruvideoSdkCameraMedia(
                    id = UUID.randomUUID().toString(),
                    createdAt = 0L,
                    type = TruvideoSdkCameraMediaType.PICTURE,
                    cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
                    filePath = "1",
                    resolution = TruvideoSdkCameraResolution(100, 500),
                    rotation = TruvideoSdkCameraOrientation.PORTRAIT,
                    duration = 1000L
                ),
                TruvideoSdkCameraMedia(
                    id = UUID.randomUUID().toString(),
                    createdAt = 0L,
                    type = TruvideoSdkCameraMediaType.PICTURE,
                    cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
                    filePath = "2",
                    resolution = TruvideoSdkCameraResolution(100, 500),
                    rotation = TruvideoSdkCameraOrientation.PORTRAIT,
                    duration = 1000L
                ),
                TruvideoSdkCameraMedia(
                    id = UUID.randomUUID().toString(),
                    createdAt = 0L,
                    type = TruvideoSdkCameraMediaType.PICTURE,
                    cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
                    filePath = "3",
                    resolution = TruvideoSdkCameraResolution(100, 500),
                    rotation = TruvideoSdkCameraOrientation.PORTRAIT,
                    duration = 1000L
                )
            )
        )
    }


    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            MediaPreviewPanel(
                orientation = orientation,
                visible = visible,
                media = media,
                close = { visible = false },
                onDelete = { item ->
                    media = media.filter { it.filePath != item.filePath }.toImmutableList()
                }
            )
        }

        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
    }

}