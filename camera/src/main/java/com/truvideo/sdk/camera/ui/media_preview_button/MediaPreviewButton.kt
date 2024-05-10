package com.truvideo.sdk.camera.ui.media_preview_button

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.CrossFade
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import truvideo.sdk.components.scale_button.TruvideoScaleButton
import java.io.File

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaPreviewButton(
    media: List<TruvideoSdkCameraMedia>,
    onPressed: (() -> Unit)? = null
) {
    var previewFile: File? = null
    if (media.isNotEmpty()) {
        previewFile = File(media.last().filePath)
    }

    AnimatedVisibility(
        visible = previewFile != null,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 0.7f)
    ) {

        TruvideoScaleButton(
            enabled = previewFile != null,
            onPressed = onPressed
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = CircleShape)
                        .background(color = Color.Black)
                ) {
                    if (previewFile != null)
                        GlideImage(
                            modifier = Modifier.fillMaxSize(),
                            model = Uri.fromFile(previewFile),
                            contentScale = ContentScale.Crop,
                            contentDescription = "",
                            transition = CrossFade
                        )
                }

                Box(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(4.dp))
                        .background(color = Color(0xFFFFC107))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .align(Alignment.BottomEnd),
                ) {
                    Text(
                        "${media.size}",
                        color = Color.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun Test() {
    var media by remember { mutableStateOf(listOf<TruvideoSdkCameraMedia>()) }

    Column {
        MediaPreviewButton(media)
    }
}