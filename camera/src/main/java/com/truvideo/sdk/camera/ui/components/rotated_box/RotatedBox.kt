package com.truvideo.sdk.camera.ui.components.rotated_box

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_value.animateFloat
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun AnimatedFadeRotatedBox(
    orientation: TruvideoSdkCameraOrientation,
    orientations: ImmutableMap<TruvideoSdkCameraOrientation, Boolean> = persistentMapOf(
        TruvideoSdkCameraOrientation.PORTRAIT to true,
        TruvideoSdkCameraOrientation.LANDSCAPE_LEFT to true,
        TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT to true,
        TruvideoSdkCameraOrientation.PORTRAIT_REVERSE to true
    ),
    content: @Composable (orientation: TruvideoSdkCameraOrientation) -> Unit = {}
) {
    TruvideoSdkCameraOrientation.entries.forEach {
        key(it) {
            val visible = remember(orientations, it) { orientations[it] ?: false }

            TruvideoAnimatedFadeVisibility(it == orientation && visible) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxW = if (it.isPortrait || it.isPortraitReverse) maxWidth else maxHeight
                    val maxH = if (it.isPortrait || it.isPortraitReverse) maxHeight else maxWidth
                    val rotation = it.uiRotation

                    Box(
                        modifier = Modifier.wrapContentSize(
                            unbounded = true,
                            align = Alignment.TopStart
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .width(maxW)
                                .height(maxH)
                                .graphicsLayer {
                                    val x = when (it) {
                                        TruvideoSdkCameraOrientation.PORTRAIT -> 0f
                                        TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> -(size.width - size.height) * 0.5f
                                        TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> -(size.width - size.height) * 0.5f
                                        TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> 0f
                                    }
                                    val y = when (it) {
                                        TruvideoSdkCameraOrientation.PORTRAIT -> 0f
                                        TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> (size.width - size.height) * 0.5f
                                        TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> (size.width - size.height) * 0.5f
                                        TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> 0f
                                    }

                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    rotationZ = rotation
                                    translationX = x
                                    translationY = y
                                }
                        ) {
                            content(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedRotatedBox(
    orientation: TruvideoSdkCameraOrientation,
    content: @Composable () -> Unit = {}
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        var maxW by remember { mutableStateOf(if (orientation.isPortrait || orientation.isPortraitReverse) maxWidth else maxHeight) }
        val maxWAnim = animateFloat(value = maxW.value)

        var maxH by remember { mutableStateOf(if (orientation.isPortrait || orientation.isPortraitReverse) maxHeight else maxWidth) }
        val maxHAnim = animateFloat(value = maxH.value)

        var rotation by remember { mutableFloatStateOf(orientation.uiRotation) }
        val rotationAnim = animateFloat(value = rotation)

        val x = when (orientation) {
            TruvideoSdkCameraOrientation.PORTRAIT -> 0f
            TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> 0f
            TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> 0f
            TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> 0f
        }
        val xAnim = animateFloat(value = x)
        val y = when (orientation) {
            TruvideoSdkCameraOrientation.PORTRAIT -> 0f
            TruvideoSdkCameraOrientation.LANDSCAPE_LEFT -> 0.5f
            TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT -> 0.5f
            TruvideoSdkCameraOrientation.PORTRAIT_REVERSE -> 0f
        }
        val yAnim = animateFloat(value = y)

        LaunchedEffect(orientation) {
            maxW = if (orientation.isPortrait || orientation.isPortraitReverse) maxWidth else maxHeight
            maxH = if (orientation.isPortrait || orientation.isPortraitReverse) maxHeight else maxWidth
            rotation = orientation.uiRotation
        }

        Box(
            modifier = Modifier
                .wrapContentSize(
                    unbounded = true,
                    align = Alignment.Center
                )
        ) {
            Box(
                modifier = Modifier
                    .width(maxWAnim.dp)
                    .height(maxHAnim.dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0.5f)

                        rotationZ = rotationAnim
                        translationX = (size.width - size.height) * xAnim
                        translationY = (size.width - size.height) * yAnim
                    }
            ) {
                content()
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT) }
    val orientations = remember {
        persistentMapOf(
            TruvideoSdkCameraOrientation.PORTRAIT to false,
            TruvideoSdkCameraOrientation.LANDSCAPE_LEFT to true,
            TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT to true,
            TruvideoSdkCameraOrientation.PORTRAIT_REVERSE to true
        )
    }

    TruVideoSdkCameraTheme {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedRotatedBox(
                    orientation = orientation
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Red
                            )
                    ) {
                        Text("TopStart", modifier = Modifier.align(Alignment.TopStart))
                        Text("TopEnd", modifier = Modifier.align(Alignment.TopEnd))
                        Text("BottomStart", modifier = Modifier.align(Alignment.BottomStart))
                        Text("BottomEnd", modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
            }

            Box(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                AnimatedFadeRotatedBox(
                    orientation = orientation,
                    orientations = orientations
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Color.Red
                            )
                    ) {
                        Text("TopStart", modifier = Modifier.align(Alignment.TopStart))
                        Text("TopEnd", modifier = Modifier.align(Alignment.TopEnd))
                        Text("BottomStart", modifier = Modifier.align(Alignment.BottomStart))
                        Text("BottomEnd", modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
            }

            Text("Orientation: $orientation",
                modifier = Modifier.clickable {
                    val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
                    orientation = TruvideoSdkCameraOrientation.entries[index]
                }
            )
        }
    }

}