package com.truvideo.camera.app.ui.activities.home

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import com.truvideo.camera.app.ui.activities.home.camera_configuration.CameraConfiguration
import com.truvideo.camera.app.ui.activities.home.media_list.MediaList
import com.truvideo.camera.app.ui.activities.viewer.ViewerActivity
import com.truvideo.camera.app.ui.theme.TruvideoSdkAppCameraTheme
import com.truvideo.sdk.camera.TruvideoSdkCamera
import com.truvideo.sdk.camera.model.TruvideoSdkArCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCodeFormat
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerConfiguration
import com.truvideo.sdk.camera.ui.activities.arcamera.TruvideoSdkArCameraContract
import com.truvideo.sdk.camera.ui.activities.camera.TruvideoSdkCameraContract
import com.truvideo.sdk.camera.ui.activities.scanner.TruvideoSdkCameraScannerContract
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val outputPath = "${getExternalFilesDir(Environment.DIRECTORY_DCIM)?.path ?: ""}/truvideo-sdk/camera"

        enableEdgeToEdge()
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
        var files by remember { mutableStateOf(listOf<TruvideoSdkCameraMedia>()) }

        fun addMedia(media: List<TruvideoSdkCameraMedia>) {
            files = files.toMutableList().apply { media.forEach { add(it) } }.toList()
        }

        val cameraLauncher = rememberLauncherForActivityResult(TruvideoSdkCameraContract()) {
            addMedia(it)
        }

        val arCameraLauncher = rememberLauncherForActivityResult(TruvideoSdkArCameraContract()) {
            addMedia(it)
        }

        val scannerLauncher = rememberLauncherForActivityResult(TruvideoSdkCameraScannerContract()) {

        }

        val cameraScannerLauncher = rememberLauncherForActivityResult(TruvideoSdkCameraScannerContract()) {
            Log.d("TruvideoSdkCamera", "scanner result $it")
        }

        var isConfigurationVisible by remember { mutableStateOf(false) }

        var configuration by remember {
            mutableStateOf(
                TruvideoSdkCameraConfiguration(
                    outputPath = outputPath,
                    lensFacing = TruvideoSdkCameraLensFacing.FRONT,
                    mode = TruvideoSdkCameraMode.singleVideo()
                )
            )
        }

        val scannerConfiguration by remember {
            mutableStateOf(
                TruvideoSdkCameraScannerConfiguration(
                    codeFormats = listOf(TruvideoSdkCameraScannerCodeFormat.CODE_39)
                )
            )
        }

        val arConfiguration by remember {
            mutableStateOf(
                TruvideoSdkArCameraConfiguration(
                    outputPath = outputPath,
                    mode = TruvideoSdkCameraMode.singleVideo()
                )
            )
        }

        DisposableEffect(Unit) {
            val observer = Observer<TruvideoSdkCameraEvent> { event ->
                Log.d("TruvideoSdkCameraEvents", "Event type ${event.type}: ${event.data}")
            }
            TruvideoSdkCamera.events.observeForever(observer)

            onDispose {
                Log.d("TruvideoSdkCamera", "Disposing observer")
                TruvideoSdkCamera.events.removeObserver(observer)
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
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
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { cameraLauncher.launch(configuration) }
                        ) {
                            Text(text = "Open camera")
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                arCameraLauncher.launch(arConfiguration)
                            }
                        ) {
                            Text(text = "Open AR camera")
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scannerLauncher.launch(scannerConfiguration)
                            }
                        ) {
                            Text(text = "Open Scanner")
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                cameraScannerLauncher.launch(
                                    TruvideoSdkCameraScannerConfiguration(

                                    )
                                )
                            }
                        ) {
                            Text(text = "Scanner")
                        }
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
                                    open = { media ->
                                        val intent = Intent(context, ViewerActivity::class.java)
                                        intent.putExtra("filePath", media.filePath)
                                        startActivity(intent)
                                    },
                                    delete = { media ->
                                        try {
                                            File(media.filePath).delete()
                                            Log.d("CameraApp", "Deleted file ${media.filePath}")
                                            files = files.filter { it.filePath != media.filePath }.toList()
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
    @Preview(
        showBackground = true,
        showSystemUi = true
    )
    private fun Test() {
        TruvideoSdkAppCameraTheme {
            Content()
        }
    }
}