package com.truvideo.sdk.camera.ui.components.media_delete_panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.media3.common.util.UnstableApi
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.components.panel.Panel
import com.truvideo.sdk.components.button.TruvideoButton

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MediaDeletePanel(
    visible: Boolean = false,
    isVideo: Boolean = false,
    onDelete: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation,
    close: (() -> Unit) = {}
) {
    BackHandler(visible) {
        close()
    }

    Panel(
        visible = visible,
        close = close,
        orientation = orientation
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                if (isVideo) "DELETE VIDEO" else "DELETE IMAGE",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.height(8.dp))

            Text(
                text = "Are you sure?",
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Box(modifier = Modifier.height(32.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                TruvideoButton(
                    text = "DELETE",
                    selected = true,
                    selectedColor = Color(0xFFD32F2F),
                    selectedTextColor = Color.White,
                    onPressed = { onDelete() }
                )
            }

            Box(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.widthIn(max = 300.dp)) {
                TruvideoButton(
                    text = "CANCEL",
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
    val orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            MediaDeletePanel(
                orientation = orientation,
                visible = visible,
                close = {
                    visible = false
                }
            )
        }
        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
    }

}