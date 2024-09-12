package com.truvideo.sdk.camera.ui.components.exit_panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.components.button.TruvideoButton


@Composable
fun ExitPanel(
    visible: Boolean = true,
    onDiscardPressed: (() -> Unit) = {},
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
    enabled: Boolean = true
) {
    BackHandler(visible) {
        if (enabled) {
            close()
        }
    }

    Panel(
        visible = visible,
        orientation = orientation,
        close = { close() }
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

            Box(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                TruvideoButton(
                    text = "DISCARD",
                    selected = true,
                    selectedColor = Color(0xFFD32F2F),
                    selectedTextColor = Color.White,
                    enabled = enabled,
                    onPressed = { onDiscardPressed() }
                )
            }

            Box(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                TruvideoButton(
                    text = "CANCEL",
                    enabled = enabled,
                    onPressed = { close() }
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