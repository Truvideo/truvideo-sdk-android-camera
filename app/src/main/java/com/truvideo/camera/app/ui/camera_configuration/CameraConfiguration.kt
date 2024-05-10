package com.truvideo.camera.app.ui.camera_configuration

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
import com.truvideo.sdk.camera.model.TruvideoSdkCameraVideoCodec

@Composable
fun CameraConfiguration(
    configuration: TruvideoSdkCameraConfiguration,
    onChanged: ((configuration: TruvideoSdkCameraConfiguration) -> Unit)? = null,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Text("Mode")
        }
        TruvideoSdkCameraMode.entries.forEach {
            Row(
                modifier = Modifier
                    .clickable {
                        if (onChanged != null) {
                            onChanged(configuration.copy(mode = it))
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(it.name)
                Box(modifier = Modifier.weight(1f))
                RadioButton(
                    selected = configuration.mode == it,
                    onClick = {
                        if (onChanged != null) {
                            onChanged(configuration.copy(mode = it))
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Text("Lens facing")
        }
        TruvideoSdkCameraLensFacing.entries.forEach {
            Row(
                modifier = Modifier
                    .clickable {
                        if (onChanged != null) {
                            onChanged(configuration.copy(lensFacing = it))
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(it.name)
                Box(modifier = Modifier.weight(1f))
                RadioButton(
                    selected = configuration.lensFacing == it,
                    onClick = {
                        if (onChanged != null) {
                            onChanged(configuration.copy(lensFacing = it))
                        }
                    }
                )
            }
        }

        Box(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray.copy(alpha = 0.4f))
                .clickable {
                    if (onChanged != null) {
                        if(configuration.flashMode.isOn){
                            onChanged(configuration.copy(flashMode = TruvideoSdkCameraFlashMode.OFF))
                        }else{
                            onChanged(configuration.copy(flashMode = TruvideoSdkCameraFlashMode.ON))
                        }
                    }
                }
                .padding(horizontal = 16.dp)

        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Flash Mode")
                Box(modifier = Modifier.weight(1f))
                Switch(
                    checked = configuration.flashMode.isOn,
                    onCheckedChange = {
                        if (onChanged != null) {
                            if(configuration.flashMode.isOn){
                                onChanged(configuration.copy(flashMode = TruvideoSdkCameraFlashMode.OFF))
                            }else{
                                onChanged(configuration.copy(flashMode = TruvideoSdkCameraFlashMode.ON))
                            }
                        }
                    }
                )
            }

        }

        Box(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Text("Orientation")
        }
        Row(
            modifier = Modifier
                .clickable {
                    if (onChanged != null) {
                        onChanged(configuration.copy(orientation = null))
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ANY")
            Box(modifier = Modifier.weight(1f))
            RadioButton(
                selected = configuration.orientation == null,
                onClick = {
                    if (onChanged != null) {
                        onChanged(configuration.copy(orientation = null))
                    }
                }
            )
        }
        TruvideoSdkCameraOrientation.entries.forEach {
            Row(
                modifier = Modifier
                    .clickable {
                        if (onChanged != null) {
                            onChanged(configuration.copy(orientation = it))
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(it.name)
                Box(modifier = Modifier.weight(1f))
                RadioButton(
                    selected = configuration.orientation == it,
                    onClick = {
                        if (onChanged != null) {
                            onChanged(configuration.copy(orientation = it))
                        }
                    }
                )
            }
        }

        // Video codec
        Box(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Text("Video codec")
        }
        TruvideoSdkCameraVideoCodec.entries.forEach {
            Row(
                modifier = Modifier
                    .clickable {
                        if (onChanged != null) {
                            onChanged(configuration.copy(videoCodec = it))
                        }
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(it.name)
                Box(modifier = Modifier.weight(1f))
                RadioButton(
                    selected = configuration.videoCodec == it,
                    onClick = {
                        if (onChanged != null) {
                            onChanged(configuration.copy(videoCodec = it))
                        }
                    }
                )
            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var configuration by remember { mutableStateOf(TruvideoSdkCameraConfiguration()) }
    CameraConfiguration(configuration = configuration, onChanged = { configuration = it })
}