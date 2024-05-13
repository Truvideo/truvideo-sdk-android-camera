package com.truvideo.sdk.camera.ui.capture_button

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import truvideo.sdk.components.scale_button.TruvideoScaleButton
import androidx.compose.animation.Animatable as AnimatableColor
import androidx.compose.animation.core.Animatable as AnimatableFloat

private fun calculateCircleSize(recording: Boolean): Float {
    if (recording) {
        return 25f
    }

    return 55f
}

private fun calculateCircleRadius(recording: Boolean): Float {
    if (recording) {
        return 2f
    }

    return 30f
}

private fun calculateCircleColor(recording: Boolean): Color {
    if (recording) {
        return Color.Red
    }

    return Color.White
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CaptureButton(
    recording: Boolean = false,
    enabled: Boolean = true,
    onPressed: (() -> Unit)? = null
) {
    val borderColor = Color.White

    val size = 70.0f
    val br = 2.0f
    val circleSize = remember { AnimatableFloat(calculateCircleSize(recording)) }
    val circleColor = remember { AnimatableColor(calculateCircleColor(recording)) }
    val circleRadius = remember { AnimatableFloat(calculateCircleRadius(recording)) }

    LaunchedEffect(recording) {
        circleColor.animateTo(calculateCircleColor(recording), tween(durationMillis = 500))
    }

    LaunchedEffect(recording) {
        circleSize.animateTo(calculateCircleSize(recording), tween(durationMillis = 500))
    }

    LaunchedEffect(recording) {
        circleRadius.animateTo(calculateCircleRadius(recording), tween(durationMillis = 500))
    }

    Box(
        modifier = Modifier
            .width(size.dp)
            .height(size.dp)
            .border(
                width = br.dp,
                color = borderColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
    )
    {
        TruvideoScaleButton(
            enabled = enabled,
            onPressed = onPressed
        ) {
            Box(
                modifier = Modifier
                    .width(circleSize.value.dp)
                    .height(circleSize.value.dp)
                    .background(
                        color = circleColor.value,
                        shape = RoundedCornerShape(circleRadius.value.dp) // Ajusta el radio según tus necesidades
                    ),
            )
        }
    }
}

@Composable
@Preview
fun CaptureButtonTest() {
    var recording by remember { mutableStateOf(false) }
    var enabled by remember { mutableStateOf(false) }


    Column {
        Box(modifier = Modifier.background(color = Color.Black)) {
            CaptureButton(
                recording = recording,
                enabled = enabled,
                onPressed = {
                }
            )
        }
        Text("Enabled: $enabled", modifier = Modifier.clickable {
            enabled = !enabled
        })
        Text("Recording: $recording", modifier = Modifier.clickable {
            recording = !recording
        })
    }

}