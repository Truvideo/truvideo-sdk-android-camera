package com.truvideo.camera.app.ui.activities.viewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.truvideo.camera.app.ui.theme.TruvideoSdkAppCameraTheme
import java.io.File

class ViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val path = intent.getStringExtra("filePath") ?: ""

        enableEdgeToEdge()
        setContent {
            TruvideoSdkAppCameraTheme {
                Content(path = path)
            }
        }
    }

    @OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun Content(path: String) {
        val context = LocalContext.current
        val isVideo = remember(path) { path.endsWith("mp4") }

        val exoPlayer = remember(path, isVideo) {
            if (!isVideo) return@remember null
            return@remember ExoPlayer.Builder(context).build()
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer?.release()
            }
        }

        val mediaItem = remember(path, isVideo) {
            if (!isVideo) return@remember null
            return@remember MediaItem.fromUri(Uri.fromFile(File(path)))
        }

        LaunchedEffect(exoPlayer, mediaItem) {
            exoPlayer?.apply {
                setMediaItem(mediaItem!!)
                prepare()
                play()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                finish()
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                if (isVideo) {
                    AndroidView(
                        factory = {
                            PlayerView(it).apply {
                                player = exoPlayer
                            }
                        },
                        modifier = Modifier
                            .clipToBounds()
                            .fillMaxSize(),
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val uri = remember(path) { Uri.fromFile(File(path)) }
                        GlideImage(
                            model = uri,
                            contentDescription = "",
                            modifier = Modifier
                                .clipToBounds()
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }


    @Preview
    @Composable
    private fun Test() {
        val path = ""
        TruvideoSdkAppCameraTheme {
            Content(path = path)
        }
    }
}