package com.truvideo.camera.app.ui.media_list

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import java.io.File

@Composable
fun MediaList(
    files: List<File> = listOf(),
    delete: ((file: File) -> Unit)? = null,
    open: ((file: File) -> Unit)? = null
) {
    LazyColumn {
        items(files.size) {
            VideoItem(
                file = files[it],
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
    file: File,
    onPlayPressed: (() -> Unit)? = null,
    onDeletePressed: (() -> Unit)? = null
) {
    val isVideo = file.path.endsWith("mp4")

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
                    .width(100.dp)
                    .height(100.dp),
                model = Uri.fromFile(file),
                contentScale = ContentScale.Crop,
                contentDescription = "",
            )
            Box(modifier = Modifier.width(16.dp))

            Text(file.path ?: "", modifier = Modifier.weight(1f))

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
    var files by remember { mutableStateOf(listOf<File>()) }
    Column {
        MediaList(
            files = files,
        )
    }
}