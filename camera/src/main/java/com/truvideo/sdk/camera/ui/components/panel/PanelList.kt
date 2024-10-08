package com.truvideo.sdk.camera.ui.components.panel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.button.TruvideoButton

@Composable
fun PanelList(
    visible: Boolean = true,
    itemCount: Int = 0,
    selected: Int? = null,
    textBuilder: (Int) -> String = { "" },
    onPressed: (Int) -> Unit = {},
    close: (() -> Unit) = {},
    orientation: TruvideoSdkCameraOrientation = TruvideoSdkCameraOrientation.PORTRAIT,
    title: String = "",
    closeVisible: Boolean = true,
) {
    Panel(
        closeVisible = closeVisible,
        visible = visible,
        close = close,
        orientation = orientation,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (title.trim().isNotEmpty()) {
                    Text(
                        "RESOLUTIONS",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(modifier = Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 0 until itemCount) {
                        Box(modifier = Modifier.widthIn(max = 300.dp)) {
                            TruvideoButton(
                                selected = selected == i,
                                text = textBuilder(i),
                                onPressed = { onPressed(i) }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    TruVideoSdkCameraTheme {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                PanelList(
                    orientation = orientation,
                    itemCount = 10,
                    textBuilder = { "$it" }
                )
            }
            Text("Orientation: $orientation", modifier = Modifier.clickable {
                orientation = TruvideoSdkCameraOrientation.entries[(orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size]
            })
        }
    }
}