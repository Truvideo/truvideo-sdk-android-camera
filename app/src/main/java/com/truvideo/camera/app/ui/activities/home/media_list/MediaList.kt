package com.truvideo.camera.app.ui.activities.home.media_list

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import java.io.File

@Composable
fun MediaList(
    files: List<TruvideoSdkCameraMedia> = listOf(),
    delete: ((file: TruvideoSdkCameraMedia) -> Unit)? = null,
    open: ((file: TruvideoSdkCameraMedia) -> Unit)? = null
) {
    LazyColumn {
        items(files.size) {
            VideoItem(
                model = files[it],
                onPlayPressed = {
                    if (open != null) {
                        open(files[it])
                    }
                },
                onDeletePressed = {
                    if (delete != null) {
                        delete(files[it])
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun VideoItem(
    model: TruvideoSdkCameraMedia,
    onPlayPressed: (() -> Unit)? = null,
    onDeletePressed: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {

            GlideImage(
                modifier = Modifier
                    .width(56.dp)
                    .height(56.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
                model = model.getVideoThumbnail() ?: Uri.fromFile(File(model.filePath)),
                contentScale = ContentScale.Crop,
                contentDescription = "",
            )
            Box(modifier = Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    model.filePath,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Rotation: ${model.orientation.name}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Resolution: ${model.resolution.width}x${model.resolution.height}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = {
                    if (onPlayPressed != null) {
                        onPlayPressed()
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
            }
            IconButton(
                onClick = {
                    if (onDeletePressed != null) {
                        onDeletePressed()
                    }
                }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null
                )
            }
        }
    }
}

@Preview
@Composable
private fun Test() {
    val files by remember { mutableStateOf(listOf<TruvideoSdkCameraMedia>()) }
    Column {
        MediaList(
            files = files,
        )
    }
}