package com.truvideo.sdk.camera.ui.components.rotate_button

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraIos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.button.TruvideoIconButton
import androidx.compose.animation.core.Animatable as AnimatableFloat

@Composable
fun RotateButton(
    enabled: Boolean = true,
    rotation: Float = 0f,
    onPressed: (() -> Unit)? = null,
    size: Float = 40f,
) {
    var rotate by remember { mutableStateOf(false) }
    val rotationAngle = remember { AnimatableFloat(0f) }

    LaunchedEffect(rotate) {
        if (rotate) {
            rotationAngle.animateTo(360f)
            rotate = false
        } else {
            rotationAngle.snapTo(0f)
        }
    }

    Box(modifier = Modifier.rotate(rotationAngle.value)) {
        TruvideoAnimatedRotation(
            rotation = rotation,
        ) {
            TruvideoIconButton(
                enabled = enabled,
                icon = Icons.Default.FlipCameraIos,
                size = size,
                color = Color(0XFF212121),
                onPressed = {
                    rotate = true
                    onPressed?.invoke()
                },
            )
        }
    }
}

@Composable
@Preview
private fun Test() {
    RotateButton(
        onPressed = {

        }
    )
}