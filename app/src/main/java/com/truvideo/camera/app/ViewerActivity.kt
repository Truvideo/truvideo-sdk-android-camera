package com.truvideo.camera.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePath = intent.getStringExtra("filePath") ?: ""
        val isVideo = filePath.endsWith("mp4")

        setContent {
            val context = LocalContext.current

            var exoPlayer: ExoPlayer? = null
            if (isVideo) {
                exoPlayer = remember(context) {
                    ExoPlayer.Builder(this)
                        .build()
                        .apply {
                            val mediaItem = MediaItem.fromUri(Uri.fromFile(File(filePath)))
                            setMediaItem(mediaItem)
                            prepare()
                            play()
                        }
                }
            }


            TruvideoSdkAppCameraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isVideo) {
                        AndroidView(
                            factory = {
                                PlayerView(it).apply {
                                    player = exoPlayer
                                }
                            },
                            modifier = Modifier
                                .clip(shape = RoundedCornerShape(0.dp))
                                .fillMaxSize(),
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            GlideImage(
                                model = Uri.fromFile(File(filePath)),
                                contentDescription = ""
                            )
                        }
                    }
                }
            }
        }
    }

}