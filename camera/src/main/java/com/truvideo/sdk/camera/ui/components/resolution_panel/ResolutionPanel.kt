package com.truvideo.sdk.camera.ui.components.resolution_panel

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.components.panel.PanelList


@Composable
fun ResolutionPanel(
    visible: Boolean = true,
    resolutions: List<TruvideoSdkCameraResolution>,
    selectedResolution: TruvideoSdkCameraResolution? = null,
    orientation: TruvideoSdkCameraOrientation,
    close: (() -> Unit) = {},
    onResolutionPicked: ((TruvideoSdkCameraResolution) -> Unit) = {}
) {
    BackHandler(visible) {
        close()
    }

    val selectedIndex by remember(resolutions, selectedResolution) {
        derivedStateOf {
            resolutions.indexOfFirst { it == selectedResolution }.takeIf { it >= 0 }
        }
    }

    PanelList(
        title = "Resolutions",
        itemCount = resolutions.size,
        textBuilder = { "${resolutions[it].width}x${resolutions[it].height}" },
        onPressed = { onResolutionPicked(resolutions[it]) },
        orientation = orientation,
        selected = selectedIndex,
        visible = visible,
        close = close,
    )
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var resolutions by remember { mutableStateOf(generateRandomResolutions(4)) }
    var selectedResolution by remember { mutableStateOf<TruvideoSdkCameraResolution?>(null) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.LANDSCAPE_LEFT) }

    Column {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            ResolutionPanel(
                resolutions = resolutions,
                selectedResolution = selectedResolution,
                orientation = orientation,
                visible = visible,
                onResolutionPicked = { selectedResolution = it },
                close = { visible = false }
            )
        }
        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
        Text("Generate Resolutions", modifier = Modifier.clickable {
            resolutions = generateRandomResolutions(4)
        })
        Text("Add 3 Resolutions", modifier = Modifier.clickable {
            val list = resolutions.toMutableList()
            list.addAll(generateRandomResolutions(3))
            resolutions = list.toList()
        })
    }
}

private fun generateRandomResolutions(count: Int): List<TruvideoSdkCameraResolution> {
    val resolutions = mutableListOf<TruvideoSdkCameraResolution>()
    for (i in 1..count) {
        val width = (100..4000).random()
        val height = (100..4000).random()
        resolutions.add(TruvideoSdkCameraResolution(width, height))
    }
    return resolutions
}