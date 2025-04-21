package com.truvideo.sdk.camera.ui.components.camera_mode_toggle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode

@Composable
fun CameraModeToggle(
    selectedTab: TabType,
    media: List<TruvideoSdkCameraMedia>,
    cameraMode: TruvideoSdkCameraMode,
    onTabSelected: (TabType) -> Unit
) {
    val photosCount = media.filter { it.type == TruvideoSdkCameraMediaType.IMAGE }.size
    val videosCount = media.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }.size
    val maxPhotos = cameraMode.imageLimit ?: -1
    val maxVideos = cameraMode.videoLimit ?: -1

    val photosText = if (maxPhotos > 0 ) "Photos $photosCount/$maxPhotos" else "Photos $photosCount"
    val videosText = if (maxVideos > 0 ) "Videos $videosCount/$maxVideos" else "Videos $videosCount"


    val containerBackground = Color(0xFF212121).copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(6.dp))
            .background(containerBackground)
            .padding(2.dp)
    ) {
        TabItem(
            text = photosText,
            isSelected = selectedTab == TabType.PHOTOS,
            onClick = { onTabSelected(TabType.PHOTOS) }
        )
        TabItem(
            text = videosText,
            isSelected = selectedTab == TabType.VIDEOS,
            onClick = { onTabSelected(TabType.VIDEOS) }
        )
    }

}

@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val lightColor = Color(0XFFFAFAFA)
    val darkColor = Color(0xFF212121)
    val contentColor = if (isSelected) darkColor else lightColor

    Box(
        modifier =
            if (isSelected) {
                Modifier
                    .clip(shape = RoundedCornerShape(size = 6.dp))
                    .background(lightColor)
            }else {
                Modifier
            }
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp)
        ,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor
        )
    }
}



enum class TabType {
    PHOTOS, VIDEOS
}