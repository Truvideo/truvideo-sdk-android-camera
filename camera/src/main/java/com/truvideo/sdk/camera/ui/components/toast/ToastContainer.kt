package com.truvideo.sdk.camera.ui.components.toast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.animated_value.animateFloat
import com.truvideo.sdk.components.animated_value.springAnimationFloatSpec

@Composable
fun ToastContainer(
    text: String = "",
    visible: Boolean = true,
    onPressed: () -> Unit = {}
) {
    val opacityAnim = animateFloat(value = if (visible) 1f else 0f).coerceIn(0f, 1f)
    val translationAnim = animateFloat(
        value = if (visible) 0f else 1f,
        spec = springAnimationFloatSpec
    )

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = opacityAnim
                    translationY = translationAnim * size.height
                }
                .align(Alignment.BottomCenter)
                .padding(16.dp)

        ) {
            Toast(
                text = text,
                enabled = visible,
                onPressed = onPressed
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    val text by remember { mutableStateOf("klasjdlkasjd salk asdjlksaj dlkasjdlkas jdksajd") }
    var visible by remember { mutableStateOf(true) }
    TruVideoSdkCameraTheme {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ToastContainer(
                    text = text,
                    visible = visible,
                    onPressed = { visible = false }
                )
            }
            Text("Visible: $visible",
                modifier = Modifier.clickable {
                    visible = !visible
                }
            )
        }
    }
}