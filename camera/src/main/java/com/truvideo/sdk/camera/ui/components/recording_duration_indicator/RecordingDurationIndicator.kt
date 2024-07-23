package com.truvideo.sdk.camera.ui.components.recording_duration_indicator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.animated_collapse_visibility.TruvideoAnimatedCollapseVisibility
import com.truvideo.sdk.components.animated_value.animateColor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private fun calculateColor(recording: Boolean): Color {
    if (recording) {
        return Color.Red
    }

    return Color.Black.copy(0.7f)
}

private fun calculateTextColor(recording: Boolean): Color {
    if (recording) {
        return Color.White
    }

    return Color.White
}

private fun formatDuration(duration: Duration): String {
    val hours = duration.inWholeHours
    val minutes = (duration - hours.hours).inWholeMinutes
    val seconds = (duration - hours.hours - minutes.minutes).inWholeSeconds
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
fun RecordingDurationIndicator(
    recording: Boolean = false,
    time: Duration,
    remainingTime: Duration? = null
) {
    val colorAnim = animateColor(value = calculateColor(recording))
    val textColorAnim = animateColor(value = calculateTextColor(recording))

    var currentRemainingTime by remember { mutableStateOf(remainingTime) }
    LaunchedEffect(remainingTime) {
        if (remainingTime != null) {
            currentRemainingTime = remainingTime
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(colorAnim)
                .padding(horizontal = 8.dp)
        ) {
            val text = remember(time) { formatDuration(time) }
            Text(
                text = text,
                color = textColorAnim,
                fontSize = 14.sp
            )
        }

        TruvideoAnimatedCollapseVisibility(remainingTime != null) {
            val text = remember(currentRemainingTime) { formatDuration(currentRemainingTime ?: Duration.ZERO) }
            Text(
                text = text,
                modifier = Modifier.padding(8.dp),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    shadow = Shadow(
                        color = Color.Black,
                        offset = Offset.Zero,
                        blurRadius = 10f
                    )
                )
            )
        }
    }

}


@Composable
@Preview
private fun Preview() {
    var recording by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(1.toDuration(DurationUnit.SECONDS)) }
    var remainingDuration by remember { mutableStateOf(1.toDuration(DurationUnit.SECONDS)) }
    var remainingDurationVisible by remember { mutableStateOf(false) }

    TruVideoSdkCameraTheme {
        Column {
            RecordingDurationIndicator(
                time = duration,
                recording = recording,
                remainingTime = if (remainingDurationVisible) remainingDuration else null
            )
            Text("Recording: $recording", modifier = Modifier.clickable {
                recording = !recording
            })
            Text("Duration: $duration")
            Text("Remaining duration visible: $remainingDurationVisible", modifier = Modifier.clickable {
                remainingDurationVisible = !remainingDurationVisible
            })


            Row {
                Text("Hour", modifier = Modifier.clickable {
                    duration += 1.toDuration(DurationUnit.HOURS)
                })
                Text("Min", modifier = Modifier.clickable {
                    duration += 1.toDuration(DurationUnit.MINUTES)
                })
                Text("Sec", modifier = Modifier.clickable {
                    duration += 1.toDuration(DurationUnit.SECONDS)
                })
                Text("Clear", modifier = Modifier.clickable {
                    duration = 0.toDuration(DurationUnit.SECONDS)
                })
            }
        }
    }
}