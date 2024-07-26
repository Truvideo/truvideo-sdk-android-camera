package com.truvideo.sdk.camera.ui.components.media_count_indicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.components.TruvideoColors
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity

@Composable
fun MediaCountIndicator(
    videoCount: Int,
    pictureCount: Int,
    enabled: Boolean = true,
    mode: TruvideoSdkCameraMode = TruvideoSdkCameraMode.videoAndPicture(),
    onPressed: (() -> Unit) = {}
) {

    val limitVisible = mode.mediaLimit != null
    val videoVisible = mode.canTakeVideo && if (mode.videoLimit != null) {
        true
    } else {
        videoCount > 0
    }

    val pictureVisible = mode.canTakePicture && if (mode.pictureLimit != null) {
        true
    } else {
        pictureCount > 0
    }

    val visible = (limitVisible || videoVisible || pictureVisible) && !mode.autoClose

    val ripple = rememberRipple(color = Color.White)
    CompositionLocalProvider(LocalIndication provides ripple) {
        TruvideoAnimatedFadeVisibility(visible) {
            TruvideoAnimatedOpacity(
                opacity = if (enabled) 1f else 0.5f
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(TruvideoColors.gray)
                        .height(30.dp)
                        .clickable(enabled = enabled) { onPressed() }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Media limit
                    val mediaLimitVisible = mode.mediaLimit != null
                    AnimatedContent(targetState = mediaLimitVisible, label = "") { mediaLimitVisibleTarget ->
                        if (mediaLimitVisibleTarget) {
                            Box(
                                modifier = Modifier.height(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val totalText = "${videoCount + pictureCount}/${mode.mediaLimit}"
                                AnimatedContent(
                                    targetState = totalText,
                                    label = ""
                                ) { totalTextTarget ->
                                    Text(
                                        totalTextTarget,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Box(Modifier.height(20.dp))
                        }
                    }

                    // Separator
                    Dot(mode.mediaLimit != null && (videoVisible || pictureVisible))

                    // Video count
                    AnimatedContent(targetState = videoVisible, label = "") { videoVisibleTarget ->
                        if (videoVisibleTarget) {
                            Row(
                                modifier = Modifier.height(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Videocam,
                                    contentDescription = "",
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )

                                Box(Modifier.width(4.dp))

                                val text = if (mode.videoLimit != null) "${videoCount}/${mode.videoLimit}" else videoCount.toString()
                                AnimatedContent(targetState = text, label = "") { textTarget ->
                                    Text(
                                        textTarget,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Box(Modifier.height(20.dp))
                        }
                    }

                    // Separator
                    Dot(videoVisible && pictureVisible)

                    // Picture count
                    AnimatedContent(targetState = pictureVisible, label = "") { pictureVisibleTarget ->
                        if (pictureVisibleTarget) {
                            Row(
                                modifier = Modifier.height(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = "",
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )
                                Box(Modifier.width(4.dp))

                                val limit = mode.pictureLimit
                                val text = if (limit != null) "${pictureCount}/${limit}" else "$pictureCount"
                                AnimatedContent(targetState = text, label = "") { textTarget ->
                                    Text(
                                        textTarget,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Box(Modifier.height(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(
    visible: Boolean = true
) {
    AnimatedContent(
        targetState = visible,
        label = ""
    ) { visibleTarget ->
        if (visibleTarget) {
            Box(
                modifier = Modifier.height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }else{
            Box(Modifier.height(20.dp))
        }
    }
}

@Composable
@Preview()
private fun Test() {
    var enabled by remember { mutableStateOf(true) }

    TruVideoSdkCameraTheme {
        Column {
            Column(modifier = Modifier.background(Color.Black)) {
                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 0,
                    pictureCount = 2,
                    mode = TruvideoSdkCameraMode.videoAndPicture(),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 2,
                    mode = TruvideoSdkCameraMode.videoAndPicture(
                        pictureMaxCount = 10,
                        videoMaxCount = 10
                    ),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 2,
                    mode = TruvideoSdkCameraMode.videoAndPicture(
                        pictureMaxCount = 10,
                    ),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 0,
                    mode = TruvideoSdkCameraMode.videoAndPicture(
                        maxCount = 10
                    ),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 2,
                    mode = TruvideoSdkCameraMode.picture(),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 0,
                    mode = TruvideoSdkCameraMode.picture(maxCount = 10),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 2,
                    mode = TruvideoSdkCameraMode.video(),
                    onPressed = {

                    }
                )

                MediaCountIndicator(
                    enabled = enabled,
                    videoCount = 2,
                    pictureCount = 2,
                    mode = TruvideoSdkCameraMode.video(maxCount = 10),
                    onPressed = {

                    }
                )
            }


            Text("Enabled: $enabled", modifier = Modifier.clickable {
                enabled = !enabled
            })
        }
    }
}
