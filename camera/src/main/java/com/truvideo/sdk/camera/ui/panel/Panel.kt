package com.truvideo.sdk.camera.ui.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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

@Composable
fun Panel(
    visible: Boolean = false,
    onBackgroundPressed: (() -> Unit)? = null,
    content: @Composable() (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(0.9f))
                .clickable(
                    enabled = onBackgroundPressed != null,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {
                    if (onBackgroundPressed != null) {
                        onBackgroundPressed()
                    }
                }
        )
        {
            if (content != null)
                content()
        }
    }
}

@Composable
@Preview
private fun Test() {
    var visible by remember { mutableStateOf(false) }

    Column {
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(100.dp)
                .background(color = Color.Black)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Component on the background", color = Color.White)
            }
            Panel(visible = visible) {
                Text("Component on the foreground", color = Color.White)
            }
        }

        Text("Visible: $visible", modifier = Modifier.clickable {
            visible = !visible
        })
    }
}

