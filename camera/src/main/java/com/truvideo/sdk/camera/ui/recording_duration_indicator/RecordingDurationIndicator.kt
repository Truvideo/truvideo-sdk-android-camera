package com.truvideo.sdk.camera.ui.recording_duration_indicator

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    time: Duration
) {
    val color = remember { Animatable(calculateColor(recording)) }
    val textColor = remember { Animatable(calculateTextColor(recording)) }

    LaunchedEffect(recording) {
        color.animateTo(calculateColor(recording), tween(durationMillis = 500))
    }

    LaunchedEffect(recording) {
        textColor.animateTo(calculateTextColor(recording), tween(durationMillis = 500))
    }

    Box(
        modifier = Modifier
            .clip(
                shape = RoundedCornerShape(4.dp)
            )
            .background(
                color = color.value
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = formatDuration(time),
            color = textColor.value
        )
    }
}


@Composable
@Preview
private fun Preview() {
    var recording by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(1.toDuration(DurationUnit.SECONDS)) }

    Column {
        RecordingDurationIndicator(
            time = duration,
            recording = recording
        )
        Text("Recording: $recording", modifier = Modifier.clickable {
            recording = !recording
        })
        Text("Duration: $duration")

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