package com.truvideo.sdk.camera.ui.resolution_panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.animated_fit.AnimatedFit
import com.truvideo.sdk.camera.ui.panel.Panel
import truvideo.sdk.components.button.TruvideoButton
import truvideo.sdk.components.icon_button.TruvideoIconButton


@Composable
fun ResolutionPanel(
    visible: Boolean = true,
    resolutions: List<TruvideoSdkCameraResolution>,
    selectedResolution: TruvideoSdkCameraResolution? = null,
    orientation: TruvideoSdkCameraOrientation,
    close: (() -> Unit)? = null,
    onBackgroundPressed: (() -> Unit)? = null,
    onResolutionPicked: ((TruvideoSdkCameraResolution) -> Unit)? = null
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Panel(
            visible = visible, onBackgroundPressed = onBackgroundPressed
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // List fo resolutions
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedFit(
                        aspectRatio = 1f,
                        rotation = orientation.uiRotation,
                        modifier = Modifier
                            .fillMaxSize(),
                        contentModifier = Modifier
                    ) {

                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "RESOLUTIONS",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier,
                                contentAlignment = Alignment.Center
                            ) {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    resolutions.forEachIndexed { index, resolution ->

                                        Box(
                                            modifier = Modifier
                                                .padding(top = if (index != 0) 8.dp else 0.dp)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center

                                        ) {
                                            Box(modifier = Modifier.width(250.dp)) {
                                                TruvideoButton(
                                                    text = "${resolution.width}x${resolution.height}",
                                                    selected = resolution.width == selectedResolution?.width && resolution.height == selectedResolution.height,
                                                    onPressed = {
                                                        if (onResolutionPicked != null) {
                                                            onResolutionPicked(resolution)
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                    }
                                }
                            }
                        }

                    }

                }


                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp), contentAlignment = Alignment.TopEnd
                ) {
                    TruvideoIconButton(imageVector = Icons.Default.Close, onPressed = {
                        if (close != null) {
                            close()
                        }
                    })
                }


            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var resolutions by remember { mutableStateOf(generateRandomResolutions(4)) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            ResolutionPanel(
                resolutions = resolutions, selectedResolution = resolutions.first(), orientation = orientation, visible = visible
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