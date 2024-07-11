package com.truvideo.camera.app

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.truvideo.camera.app.ui.camera_configuration.CameraConfiguration
import com.truvideo.camera.app.ui.media_list.MediaList
import com.truvideo.camera.app.ui.theme.TruvideosdkcameraTheme
import com.truvideo.sdk.camera.TruvideoSdkCamera
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.usecase.TruvideoSdkCameraScreen
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var cameraScreen: TruvideoSdkCameraScreen

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraScreen = TruvideoSdkCamera.initCameraScreen(this)
        val outputPath = "${getExternalFilesDir(Environment.DIRECTORY_DCIM)?.path ?: ""}/truvideo-sdk/camera"

        setContent {
            val context = LocalContext.current
            var isConfigurationVisible by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            var configuration by remember {
                mutableStateOf(
                    TruvideoSdkCameraConfiguration(
                        outputPath = outputPath,
                        pauseButtonVisible = true
                    )
                )
            }

            fun refresh(): List<File> {
                val directory = File(outputPath)
                if (directory.exists()) {
                    return directory.listFiles()?.toList() ?: listOf()
                }

                return listOf()
            }


            var files by remember { mutableStateOf(listOf<File>()) }

            LaunchedEffect(true) {
                files = refresh()
            }


            fun openCameraScreen() {
                scope.launch {
                    val info = TruvideoSdkCamera.getInformation()
                    Log.d("TruvideoSdkCamera", "Info $info")
                    cameraScreen.open(configuration)
                    files = refresh()
                }
            }

            TruvideosdkcameraTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            TopAppBar(
                                title = {
                                    Text("Truvideo SDK Camera")
                                },
                                actions = {
                                    IconButton(onClick = { isConfigurationVisible = !isConfigurationVisible }) {
                                        Icon(imageVector = Icons.Default.Settings, contentDescription = "")
                                    }
                                    IconButton(onClick = { files = refresh() }) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "")
                                    }
                                }
                            )
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                onClick = { openCameraScreen() }
                            ) {
                                Text(text = "Open camera")
                            }

                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isConfigurationVisible) {
                                    Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                                                files = refresh()
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
    }
}