package com.truvideo.sdk.camera.ui.components.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.components.rotated_box.AnimatedFadeRotatedBox
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.button.TruvideoIconButton

@Composable
fun Panel(
    visible: Boolean = true,
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation = TruvideoSdkCameraOrientation.PORTRAIT,
    closeVisible: Boolean = true,
    portraitActions: @Composable RowScope.(orientation: TruvideoSdkCameraOrientation) -> Unit = {},
    landscapeActions: @Composable ColumnScope.(orientation: TruvideoSdkCameraOrientation) -> Unit = {},
    portraitSecondaryActions: (@Composable RowScope.(orientation: TruvideoSdkCameraOrientation) -> Unit)? = null,
    landscapeSecondaryActions: (@Composable ColumnScope.(orientation: TruvideoSdkCameraOrientation) -> Unit)? = null,
    content: @Composable BoxScope.(orientation: TruvideoSdkCameraOrientation) -> Unit = {}
) {
    BackHandler(visible) {
        close()
    }

    TruvideoAnimatedFadeVisibility(visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(0.9f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() })
                {
                    close()
                }
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedFadeRotatedBox(orientation = orientation) { orientationTarget ->
                if (orientationTarget.isPortrait || orientationTarget.isPortraitReverse) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            if (closeVisible) {
                                TruvideoIconButton(
                                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                    small = true,
                                    onPressed = { close() }
                                )
                            }
                            portraitActions(orientationTarget)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()){
                                content(orientationTarget)
                            }
                        }
                        if (portraitSecondaryActions != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                portraitSecondaryActions(orientationTarget)
                            }
                        }
                    }
                } else {
                    Row {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(16.dp)
                        ) {
                            if (closeVisible) {
                                TruvideoIconButton(
                                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                    small = true,
                                    onPressed = { close() }
                                )
                            }
                            landscapeActions(orientationTarget)
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()){
                                content(orientationTarget)
                            }
                        }
                        if (landscapeSecondaryActions != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(16.dp)
                            ) {
                                landscapeSecondaryActions(orientationTarget)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    TruVideoSdkCameraTheme {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                Panel(
                    orientation = orientation,
                )
                {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Red)
                    )
                }
            }

            Text(
                text = "Orientation: $orientation",
                modifier = Modifier.clickable {
                    val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
                    orientation = TruvideoSdkCameraOrientation.entries[index]
                }
            )
        }
    }
}