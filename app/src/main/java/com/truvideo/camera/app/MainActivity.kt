package com.truvideo.camera.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truvideo.camera.app.ui.camera_configuration.CameraConfiguration
import com.truvideo.camera.app.ui.media_list.MediaList
import com.truvideo.camera.app.ui.theme.TruvideoSdkAppCameraTheme
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.ui.activities.camera.TruvideoSdkCameraContract
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outputPath = "${getExternalFilesDir(Environment.DIRECTORY_DCIM)?.path ?: ""}/truvideo-sdk/camera"

        setContent {
            TruvideoSdkAppCameraTheme {
                Content(outputPath)
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content(outputPath: String = "") {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var files by remember { mutableStateOf(listOf<File>()) }
        fun refresh() {
            coroutineScope.launch {
                val directory = File(outputPath)
                if (directory.exists()) {
                    files = directory.listFiles()?.toList() ?: listOf()
                }
            }
        }

        val cameraLauncher = rememberLauncherForActivityResult(TruvideoSdkCameraContract()) {
            it.forEach { media ->
                Log.d("TruvideoSdkCamera", media.toJson())
            }

            refresh()
        }

        var isConfigurationVisible by remember { mutableStateOf(false) }

        var configuration by remember {
            mutableStateOf(
                TruvideoSdkCameraConfiguration(
                    outputPath = outputPath,
                    mode = TruvideoSdkCameraMode.videoAndPicture(
                        videoMaxCount = 10,
                        pictureMaxCount = 2,
                        durationLimit = 3 * 1000
                    )
                )
            )
        }

        LaunchedEffect(Unit) {
            refresh()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    TopAppBar(
                        title = {
                            Text("Truvideo SDK Camera")
                        },
                        actions = {
                            IconButton(
                                onClick = { isConfigurationVisible = !isConfigurationVisible }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = ""
                                )
                            }
                            IconButton(
                                onClick = { refresh() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = ""
                                )
                            }
                        }
                    )
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = { cameraLauncher.launch(configuration) }
                    ) {
                        Text(text = "Open camera")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AnimatedContent(
                            targetState = isConfigurationVisible,
                            modifier = Modifier.fillMaxSize(), label = ""
                        ) { isConfigurationVisibleTarget ->
                            if (isConfigurationVisibleTarget) {
                                Box(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    CameraConfiguration(
                                        configuration = configuration,
                                        onChanged = { configuration = it }
                                    )
                                }
                            } else {
                                MediaList(
                                    files = files,
                                    open = { file ->
                                        val intent = Intent(context, ViewerActivity::class.java)
                                        intent.putExtra("filePath", file.path)
                                        startActivity(intent)
                                    },
                                    delete = { file ->
                                        try {
                                            file.delete()
                                            Log.d("CameraApp", "Deleted file $file")
                                            refresh()
                                        } catch (exception: Exception) {
                                            exception.printStackTrace()
                                        }
                                    },
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    @Composable
    @Preview(showBackground = true)
    private fun Test() {
        TruvideoSdkAppCameraTheme {
            Content()
        }
    }
}