package com.truvideo.sdk.camera.ui.continue_button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.button.TruvideoContinueButton

@Composable
fun ContinueButtonPanel(
    onPressed: (() -> Unit)? = null,
    enabled: Boolean = true,
    visible: Boolean = true,
    orientation: TruvideoSdkCameraOrientation
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            TruvideoAnimatedFadeVisibility(visible && orientation.isPortrait) {
                TruvideoContinueButton(
                    enabled = enabled,
                    onPressed = onPressed
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = (-30).dp)
        ) {
            TruvideoAnimatedRotation(rotation = 90f) {
                TruvideoAnimatedFadeVisibility(visible && orientation.isLandscapeLeft) {
                    TruvideoContinueButton(
                        enabled = enabled,
                        onPressed = onPressed
                    )
                }
            }
        }


        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            TruvideoAnimatedRotation(rotation = 180f) {
                TruvideoAnimatedFadeVisibility(visible && orientation.isPortraitReverse) {
                    TruvideoContinueButton(
                        enabled = enabled,
                        onPressed = onPressed

                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-30).dp, y = 30.dp)
        ) {
            TruvideoAnimatedRotation(rotation = 270f) {
                TruvideoAnimatedFadeVisibility(visible && orientation.isLandscapeRight) {
                    TruvideoContinueButton(
                        enabled = enabled,
                        onPressed = onPressed
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Box(modifier = Modifier.weight(1f)) {
                ContinueButtonPanel(orientation = orientation)
            }

            Text("Orientation: $orientation", modifier = Modifier.clickable {
                val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
                orientation = TruvideoSdkCameraOrientation.entries[index]
            })

        }


    }

}