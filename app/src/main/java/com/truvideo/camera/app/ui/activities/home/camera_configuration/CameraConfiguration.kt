package com.truvideo.camera.app.ui.activities.home.camera_configuration

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import kotlinx.collections.immutable.persistentMapOf

@Composable
fun CameraConfiguration(
    configuration: TruvideoSdkCameraConfiguration,
    onChanged: ((configuration: TruvideoSdkCameraConfiguration) -> Unit) = {},
) {
    val modeOptions = persistentMapOf(
        "Video and images" to TruvideoSdkCameraMode.videoAndImage(),
        "Video and images (max = 3)" to TruvideoSdkCameraMode.videoAndImage(
            maxCount = 3
        ),
        "Video and images (videoMax = 3)" to TruvideoSdkCameraMode.videoAndImage(
            videoMaxCount = 3
        ),
        "Video and images (imageMax = 3)" to TruvideoSdkCameraMode.videoAndImage(
            imageMaxCount = 3
        ),
        "Video and images (video/image max = 3)" to TruvideoSdkCameraMode.videoAndImage(
            imageMaxCount = 3,
            videoMaxCount = 3
        ),
        "Video" to TruvideoSdkCameraMode.video(),
        "Video (max = 3)" to TruvideoSdkCameraMode.video(
            maxCount = 3
        ),
        "Video (duration = 3s)" to TruvideoSdkCameraMode.video(
            durationLimit = 3 * 1000
        ),
        "Video (duration = 10s)" to TruvideoSdkCameraMode.video(
            durationLimit = 10 * 1000
        ),
        "Image" to TruvideoSdkCameraMode.image(),
        "Image (max = 3)" to TruvideoSdkCameraMode.image(
            maxCount = 3
        ),
        "Single video" to TruvideoSdkCameraMode.singleVideo(),
        "Single video (duration = 3s)" to TruvideoSdkCameraMode.singleVideo(durationLimit = 3 * 1000),
        "Single image" to TruvideoSdkCameraMode.singleImage(),
        "Single media" to TruvideoSdkCameraMode.singleVideoOrImage()
    )

    Column {
        Title(text = "Mode")
        modeOptions.entries.forEach {
            Item(
                text = it.key,
                showCheck = true,
                checked = configuration.mode == it.value,
                onPressed = {
                    onChanged(configuration.copy(mode = it.value))
                }
            )
        }

        Box(modifier = Modifier.height(16.dp))

        Title(text = "Lens facing")
        TruvideoSdkCameraLensFacing.entries.forEach {
            Item(
                text = it.name,
                showCheck = true,
                checked = configuration.lensFacing == it,
                onPressed = {
                    onChanged(configuration.copy(lensFacing = it))
                }
            )
        }

        Box(modifier = Modifier.height(16.dp))

        TitleSwitch(
            text = "Flash mode",
            checked = configuration.flashMode.isOn,
            onChanged = {
                if (configuration.flashMode.isOn) {
                    onChanged(configuration.copy(flashMode = TruvideoSdkCameraFlashMode.OFF))
                } else {
                    onChanged(configuration.copy(flashMode = TruvideoSdkCameraFlashMode.ON))
                }
            }
        )


        Box(modifier = Modifier.height(16.dp))
        Title(text = "Orientation")
        Item(
            text = "Any",
            showCheck = true,
            checked = configuration.orientation == null,
            onPressed = {
                onChanged(configuration.copy(orientation = null))
            }
        )
        TruvideoSdkCameraOrientation.entries.forEach {
            Item(
                text = it.name,
                showCheck = true,
                checked = configuration.orientation == it,
                onPressed = {
                    onChanged(configuration.copy(orientation = it))
                }
            )
        }
    }
}

@Composable
private fun Title(
    text: String = "",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Gray.copy(alpha = 0.4f))
            .padding(16.dp)
    ) {
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TitleSwitch(
    text: String = "",
    checked: Boolean = false,
    onChanged: ((checked: Boolean) -> Unit) = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.Gray.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, modifier = Modifier.weight(1f))

        Switch(
            checked = checked,
            onCheckedChange = { onChanged(it) }
        )
    }
}

@Composable
private fun Item(
    text: String = "",
    onPressed: () -> Unit = {},
    checked: Boolean = false,
    showCheck: Boolean = false
) {
    Row(
        modifier = Modifier
            .clickable { onPressed() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, modifier = Modifier.weight(1f))
        if (showCheck) {
            RadioButton(
                selected = checked,
                onClick = { onPressed() }
            )
        }
    }
}

@Composable
@Preview
private fun Test() {
    var configuration by remember { mutableStateOf(TruvideoSdkCameraConfiguration()) }
    CameraConfiguration(configuration = configuration, onChanged = { configuration = it })
}