package com.truvideo.sdk.camera.ui.components.toast

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.TruvideoColors

@Composable
fun Toast(
    text: String = "",
    enabled: Boolean = true,
    onPressed: () -> Unit = {}
) {
    val ripple = rememberRipple(color = Color.White)
    CompositionLocalProvider(LocalIndication provides ripple) {
        Box(
            modifier = Modifier
                .shadow(16.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(TruvideoColors.gray)
                .clickable(enabled = enabled) { onPressed() }
                .padding(8.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun Test() {
    val text by remember { mutableStateOf("hola klajsdlksajdas lkdjaskld jsakdjlkasjdasd aslkdj asldjaslkdjalksdjlkasjdlaksdjas a skjdlkas jdlkasjd") }
    TruVideoSdkCameraTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)
        ) {
            Toast(
                text = text
            )
        }
    }
}