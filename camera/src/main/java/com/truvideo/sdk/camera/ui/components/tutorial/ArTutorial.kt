package com.truvideo.sdk.camera.ui.components.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.R
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.activities.arcamera.ARModeState
import com.truvideo.sdk.camera.ui.components.panel.Panel


@Composable
fun ArTutorial(
    visible: Boolean = true,
    mode: ARModeState,
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
) {
    val modeTitle = when (mode) {
        ARModeState.RULER -> "Ruler Mode"
        ARModeState.OBJECT -> "Object Mode"
        ARModeState.RECORD  -> "Record Mode"
    }

    val modeDescription = when (mode) {
        ARModeState.RULER -> "Use this mode to measure distances."
        ARModeState.OBJECT -> "Use this mode to place directional arrows."
        ARModeState.RECORD  -> "Use this mode to hide the AR Marker and record a video."
    }

    val modeIcon = when (mode) {
        ARModeState.RULER -> R.drawable.sceneform_hand_phone
        ARModeState.OBJECT -> R.drawable.sceneform_hand_object
        ARModeState.RECORD  -> R.drawable.sceneform_record
    }

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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modeTitle, color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )
                Box(modifier = Modifier.height(8.dp))

                Text(
                    modeDescription, color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )

                Image(
                    painter = painterResource(id = modeIcon),
                    contentDescription = "Discovery prompt",
                    modifier = Modifier.size(150.dp)
                )

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
                                    "Move your device slowly to detect surfaces", color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                )
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

    Column {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            ArTutorial(
                visible = visible,
                mode = mode,
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