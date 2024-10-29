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

private fun formatDuration(
    duration: Duration,
    roundUp: Boolean = false,
): String {
    var totalSeconds = duration.inWholeSeconds
    if (roundUp && duration.inWholeMilliseconds % 1000 != 0L) {
        totalSeconds += 1
    }

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

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
            val text = remember(currentRemainingTime) {
                formatDuration(
                    duration = currentRemainingTime ?: Duration.ZERO,
                    roundUp = true
                )
            }

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
    val d = 10000
    val td = 10000
    var duration by remember { mutableStateOf(d.toDuration(DurationUnit.MILLISECONDS)) }
    var remainingDuration by remember { mutableStateOf((td - d).toDuration(DurationUnit.MILLISECONDS)) }
    var remainingDurationVisible by remember { mutableStateOf(true) }

    TruVideoSdkCameraTheme {
        Column {
            RecordingDurationIndicator(
                time = duration,
                recording = recording,
                remainingTime = if (remainingDurationVisible) remainingDuration else null
            )
            Text(
                text = "Recording: $recording",
                modifier = Modifier.clickable {
                    recording = !recording
                }
            )
            Text("Duration: $duration")
            Text(
                text = "Remaining duration visible: $remainingDurationVisible",
                modifier = Modifier.clickable {
                    remainingDurationVisible = !remainingDurationVisible
                }
            )


            Row {
                Text(
                    text = "Hour",
                    modifier = Modifier.clickable {
                        duration += 1.toDuration(DurationUnit.HOURS)
                    }
                )
                Text(
                    text = "Min",
                    modifier = Modifier.clickable {
                        duration += 1.toDuration(DurationUnit.MINUTES)
                    }
                )
                Text(
                    text = "Sec",
                    modifier = Modifier.clickable {
                        duration += 1.toDuration(DurationUnit.SECONDS)
                    }
                )
                Text(
                    text = "Clear",
                    modifier = Modifier.clickable {
                        duration = 0.toDuration(DurationUnit.SECONDS)
                    }
                )
            }
        }
    }
}