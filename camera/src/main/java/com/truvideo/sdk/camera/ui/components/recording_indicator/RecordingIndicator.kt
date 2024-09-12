package com.truvideo.sdk.camera.ui.components.recording_indicator

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.animated_value.animateFloat

@Composable
fun RecordingIndicator(
    isRecording: Boolean = false,
    isPaused: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val blinkingOpacity by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = ""
    )
    val fadeOpacity = animateFloat(
        value = if (isRecording) 1f else 0f
    ).coerceIn(0f, 1f)

    val opacity = if (isRecording && isPaused) {
        blinkingOpacity
    } else {
        fadeOpacity
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = opacity
            }
    ) {
        TruvideoAnimatedRotation {
            RecordingCorner()
        }

        Box(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            TruvideoAnimatedRotation(rotation = 90f) {
                RecordingCorner()
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            TruvideoAnimatedRotation(rotation = 180f) {
                RecordingCorner()
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            TruvideoAnimatedRotation(rotation = 270f) {
                RecordingCorner()
            }
        }
    }
}


@Composable
private fun RecordingCorner() {
    val w = 60f
    val br = 10f

    Box(
        modifier = Modifier
            .width(w.dp)
            .height(w.dp)
    ) {
        Box(
            modifier = Modifier
                .width(w.dp)
                .height(br.dp)
                .clip(shape = RoundedCornerShape(br.dp))
                .background(color = Color.Red)
        )
        Box(
            modifier = Modifier
                .width(br.dp)
                .height(w.dp)
                .clip(shape = RoundedCornerShape(br.dp))
                .background(color = Color.Red)
        )
    }
}

@Composable
@Preview
private fun Preview() {
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
                .background(color = Color.Black)
        ) {
            RecordingIndicator(
                isRecording = isRecording,
                isPaused = isPaused
            )
        }
        Text("Recording: $isRecording", modifier = Modifier.clickable {
            isRecording = !isRecording
        })
        Text("Paused: $isPaused", modifier = Modifier.clickable {
            isPaused = !isPaused
        })
    }
}