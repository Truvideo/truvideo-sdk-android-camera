package com.truvideo.sdk.camera.ui.exit_panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.animated_fit.AnimatedFit
import com.truvideo.sdk.camera.ui.panel.Panel
import truvideo.sdk.components.button.TruvideoButton
import truvideo.sdk.components.icon_button.TruvideoIconButton


@Composable
fun ExitPanel(
    visible: Boolean = true,
    onDiscardPressed: (() -> Unit)? = null,
    close: (() -> Unit)? = null,
    orientation: TruvideoSdkCameraOrientation,
    enabled: Boolean = true
) {
    Panel(
        visible = visible,
        onBackgroundPressed = close
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                AnimatedFit(
                    aspectRatio = 1f,
                    rotation = orientation.uiRotation,
                    modifier = Modifier.fillMaxSize(),
                    contentModifier = Modifier
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            "EXIT", color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Would you like to discard all videos and images?",
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Box(modifier = Modifier.height(16.dp))


                        Box(modifier = Modifier.width(250.dp)) {
                            TruvideoButton(
                                text = "DISCARD",
                                selected = true,
                                selectedColor = Color(0xFFD32F2F),
                                selectedTextColor = Color.White,
                                enabled = enabled,
                                onPressed = {
                                    if (onDiscardPressed != null) {
                                        onDiscardPressed()
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.height(8.dp))

                        Box(modifier = Modifier.width(250.dp)) {
                            TruvideoButton(
                                text = "CANCEL",
                                enabled = enabled,
                                onPressed = {
                                    if (close != null) {
                                        close()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Close button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TruvideoIconButton(
                    imageVector = Icons.Default.Close,
                    enabled = enabled,
                    rotation = orientation.uiRotation,
                    onPressed = {
                        if (close != null) {
                            close()
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
    var visible by remember { mutableStateOf(true) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        Box(
            modifier = Modifier
                .width(400.dp)
                .height(800.dp)
        ) {
            ExitPanel(
                visible = visible,
                orientation = orientation,
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