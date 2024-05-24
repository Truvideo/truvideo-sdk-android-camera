package com.truvideo.sdk.camera.ui.media_count_indicator

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.animated_scale.TruvideoAnimatedScale
import com.truvideo.sdk.components.scale_button.TruvideoScaleButton
import java.io.File

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MediaCountIndicator(
    media: List<TruvideoSdkCameraMedia>,
    orientation: TruvideoSdkCameraOrientation,
    onPressed: (() -> Unit)? = null
) {
    val visible = media.isNotEmpty()
    val previewFile = if (media.isNotEmpty()) {
        File(media.last().filePath)
    } else {
        File("")
    }

    TruvideoAnimatedScale(
        scale = if (visible) 1f else 0.7f,
    ) {
        TruvideoAnimatedOpacity(
            opacity = if (visible) 1f else 0.0f,
        ) {
            TruvideoScaleButton(
                enabled = media.isNotEmpty(),
                onPressed = onPressed,
            ) {
                TruvideoAnimatedRotation(orientation.uiRotation) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(shape = CircleShape)
                                .background(color = Color.Black)
                        ) {
                            GlideImage(
                                modifier = Modifier.size(40.dp),
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
                                .align(Alignment.BottomEnd)
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
    }
}

@Composable
@Preview
private fun Test() {
    var media by remember { mutableStateOf(generate()) }
    var orientation by remember { mutableStateOf(TruvideoSdkCameraOrientation.PORTRAIT) }

    Column {
        MediaCountIndicator(
            media = media,
            orientation = orientation,
            onPressed = {

            }
        )
        Text("Random media", modifier = Modifier.clickable {
            media = generate()
        })
        Text("Clear media", modifier = Modifier.clickable {
            media = listOf()
        })
        Text("Orientation: $orientation", modifier = Modifier.clickable {
            val index = (orientation.ordinal + 1) % TruvideoSdkCameraOrientation.entries.size
            orientation = TruvideoSdkCameraOrientation.entries[index]
        })
    }
}

private fun generate(): List<TruvideoSdkCameraMedia> {
    val result = mutableListOf<TruvideoSdkCameraMedia>()
    val numberOfResolutions = (1..20).random()
    for (i in 1..numberOfResolutions) {
        val width = (100..4000).random()
        val height = (100..4000).random()
        val lensFacing = TruvideoSdkCameraLensFacing.entries.random()
        val type = TruvideoSdkCameraMediaType.entries.random()

        result.add(
            TruvideoSdkCameraMedia(
                resolution = TruvideoSdkCameraResolution(width, height),
                filePath = "",
                cameraLensFacing = lensFacing,
                type = type,
                createdAt = 0,
                rotation = TruvideoSdkCameraOrientation.PORTRAIT,
                duration = (100..4000).random().toLong(),
            )
        )
    }
    return result
}