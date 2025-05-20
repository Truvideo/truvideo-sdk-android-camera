package com.truvideo.sdk.camera.ui.components.panel_ar_options

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ControlCamera
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewInAr
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
import com.truvideo.sdk.camera.ui.activities.arcamera.ARMeasureState
import com.truvideo.sdk.camera.ui.activities.arcamera.ARModeState
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.components.button.TruvideoButton
import com.truvideo.sdk.components.button.TruvideoIconButton


@Composable
fun ArOptionsPanel(
    visible: Boolean = true,
    mode: ARModeState,
    onModePressed: (mode: ARModeState) -> Unit = {},
    measure: ARMeasureState,
    onMeasureUnitPressed: (measure: ARMeasureState) -> Unit = {},
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
) {

    Panel(
        visible = visible,
        orientation = orientation,
        close = { close() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Modes", color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                Box(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        TruvideoIconButton(
                            size = 50f,
                            icon = Icons.Outlined.ViewInAr,
                            selected = mode == ARModeState.OBJECT,
                            onPressed = { onModePressed(ARModeState.OBJECT) }
                        )
                    }
                    Box(modifier = Modifier.padding(8.dp)) {
                        TruvideoIconButton(
                            size = 50f,
                            icon = Icons.Outlined.ControlCamera,
                            selected = mode == ARModeState.RULER,
                            onPressed = { onModePressed(ARModeState.RULER) }
                        )
                    }
                    Box(modifier = Modifier.padding(8.dp)) {
                        TruvideoIconButton(
                            size = 50f,
                            icon = Icons.Outlined.Videocam,
                            selected = mode == ARModeState.RECORD,
                            onPressed = { onModePressed(ARModeState.RECORD) }
                        )
                    }

                }

                Box(modifier = Modifier.height(16.dp))

                val isRuler = mode == ARModeState.RULER
                AnimatedContent(targetState = isRuler, label = "rule-content") { isRulerTarget ->
                    if (isRulerTarget) {
                        Box(Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Measure Units", color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Row {
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .width(70.dp)
                                    ) {
                                        TruvideoButton(
                                            text = "Cm",
                                            textToUpperCase = false,
                                            selected = measure == ARMeasureState.CM,
                                            onPressed = { onMeasureUnitPressed(ARMeasureState.CM) }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .width(70.dp)
                                    ) {
                                        TruvideoButton(
                                            text = "In",
                                            textToUpperCase = false,
                                            selected = measure == ARMeasureState.IN,
                                            onPressed = { onMeasureUnitPressed(ARMeasureState.IN) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }
    var mode by remember { mutableStateOf(ARModeState.RULER) }
    var measure by remember { mutableStateOf(ARMeasureState.CM) }

    Column {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            ArOptionsPanel(
                visible = visible,
                mode = mode,
                onModePressed = { mode = it },
                measure = measure,
                onMeasureUnitPressed = { measure = it },
                orientation = orientation
            )
        }
        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
    }
}