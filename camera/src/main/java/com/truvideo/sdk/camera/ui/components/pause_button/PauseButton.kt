package com.truvideo.sdk.camera.ui.components.pause_button

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.button.TruvideoIconButton

@Composable
fun PauseButton(
    isPaused: Boolean = false,
    enabled: Boolean = true,
    rotation: Float = 0f,
    onPressed: (() -> Unit)? = null,
    size: Float = 40f,
) {
    TruvideoAnimatedRotation(
        rotation = rotation,
    ) {

        TruvideoIconButton(
            enabled = enabled,
            icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
            size = size,
            color = Color(0XFF212121),
            onPressed = {
                if (onPressed != null) {
                    onPressed()
                }
            },
        )
    }
}

@Composable
@Preview
private fun Test() {
    PauseButton(
        onPressed = {

        }
    )
}