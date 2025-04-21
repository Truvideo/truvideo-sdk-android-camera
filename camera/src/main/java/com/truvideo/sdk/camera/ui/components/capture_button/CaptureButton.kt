package com.truvideo.sdk.camera.ui.components.capture_button

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.components.animated_value.animateColor
import com.truvideo.sdk.components.animated_value.animateFloat
import com.truvideo.sdk.components.animated_value.springAnimationFloatSpec

private fun calculateCircleSize(recording: Boolean): Float {
    if (recording) {
        return 50f
    }

    return 50f
}

private fun calculateCircleRadius(recording: Boolean): Float {
    if (recording) {
        return 30f
    }

    return 30f
}

private fun calculateCircleColor(recording: Boolean): Color {
    if (recording) {
        return Color.Red
    }

    return Color.White
}

@Composable
fun CaptureButton(
    recording: Boolean = false,
    enabled: Boolean = true,
    onPressed: (() -> Unit)? = null,
) {
    val borderColor = Color.White

    val size = 58f
    val br = 2.0f
    val circleSizeAnim = animateFloat(
        value = calculateCircleSize(recording),
        spec = springAnimationFloatSpec
    ).coerceAtLeast(0f)
    val circleColorAnim = animateColor(value = calculateCircleColor(recording))
    val circleRadiusAnim = animateFloat(
        value = calculateCircleRadius(recording),
        spec = springAnimationFloatSpec
    ).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .width(size.dp)
            .height(size.dp)
            .border(
                width = br.dp, color = borderColor, shape = CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(circleSizeAnim.dp)
                .height(circleSizeAnim.dp)
                .clip(RoundedCornerShape(circleRadiusAnim.dp))
                .background(circleColorAnim)
                .clickable(
                    enabled = enabled
                ) {
                    onPressed?.invoke()
                }
        )
    }
}

@Composable
@Preview
fun CaptureButtonTest() {
    var recording by remember { mutableStateOf(true) }
    var enabled by remember { mutableStateOf(false) }

    Column {
        Box(modifier = Modifier.background(color = Color.Black)) {
            CaptureButton(
                recording = recording,
                enabled = enabled,
                onPressed = {}
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