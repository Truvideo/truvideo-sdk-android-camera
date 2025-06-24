package com.truvideo.sdk.camera.ui.activities.refactor.camera

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEventType
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventMediaContinue
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.utils.EventConstants
import org.koin.androidx.compose.getViewModel
import java.util.Date

class TruvideoSdkNewCameraActivity : ComponentActivity() {

    private lateinit var viewModel: CameraPreviewViewModel

    companion object {
        const val TAG: String = "[TruvideoSdkCamera][TruvideoSdkCameraActivity]"
    }

    private lateinit var configuration: TruvideoSdkCameraConfiguration

    private fun makeFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val view: View = window.decorView
        WindowInsetsControllerCompat(window, view).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configuration = TruvideoSdkCameraConfiguration.fromJson(intent.getStringExtra("configuration") ?: "")
        makeFullScreen()

        fun showFatalErrorDialog(title: String, message: String) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnDismissListener { finish() }
                .setPositiveButton("Accept") { _, _ -> }
                .show()
        }

        setContent {
            viewModel = getViewModel<CameraPreviewViewModel>()

            val permissionState by viewModel.permissionState.collectAsState()
            val isAuthenticated = remember(permissionState.authenticated) { permissionState.authenticated }

            val cameraPermissionResultLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissionsMap ->
                    val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                    if (areGranted) {
                        viewModel.onEvent(CameraUiEvent.Configuration.PermissionsGranted)
                    } else {
                        showFatalErrorDialog(
                            title = "Error",
                            message = "Camera and microphone permission required"
                        )
                    }
                }
            )

            // Report authentication error
            LaunchedEffect(isAuthenticated) {
                if (!isAuthenticated) {
                   viewModel.onEvent(CameraUiEvent.Configuration.ValidateAuthentication)
                    return@LaunchedEffect
                }
                val permissions = mutableListOf(
                    Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
                )
                cameraPermissionResultLauncher.launch(permissions.toTypedArray())
            }

            val config by viewModel.cameraConfig.collectAsState()
            val isConfigured = remember(config){ config.setUp }
            val isPermissionGranted = permissionState.permissionGranted

            TruVideoSdkCameraTheme {
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxSize()
                ) {
                    LaunchedEffect(Unit) {
                        viewModel.onEvent(CameraUiEvent.Configuration.SetUpConfig(configuration))
                    }

                    if (isAuthenticated && isPermissionGranted && isConfigured) {

                        LaunchedEffect(Unit) {
                            viewModel.effect.collect { effect ->
                                when (effect) {
                                    CameraUiEffect.ClosePreview -> finish()
                                    CameraUiEffect.ReportAuthenticationError -> {
                                        showFatalErrorDialog(title = "Error", message = "Authentication required")
                                    }
                                    CameraUiEffect.ReportProperlyAuthenticated -> Unit
                                    is CameraUiEffect.ShowFocusIndicator -> Unit
                                    is CameraUiEffect.ClosePreviewWithResult -> {
                                        val media = effect.media

                                        sendEvent(
                                            TruvideoSdkCameraEvent(
                                                type = TruvideoSdkCameraEventType.CONTINUE,
                                                data = TruvideoSdkCameraEventMediaContinue(media.toList()),
                                                createdAtMillis = Date().time
                                            )
                                        )

                                        val intent = Intent()
                                        val list: List<String> = media.map { it.toJson() }.toList()
                                        intent.putStringArrayListExtra("media", ArrayList(list))
                                        setResult(RESULT_OK, intent)
                                        finish()
                                    }
                                    else -> Unit
                                }
                            }
                        }


                        CameraPreview(viewModel)

                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "OnResume")
        if (::viewModel.isInitialized)
            viewModel.onEvent(CameraUiEvent.Controls.AppForeground)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "OnPause")
        if (::viewModel.isInitialized)
            viewModel.onEvent(CameraUiEvent.Controls.AppBackground)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun sendEvent(data: TruvideoSdkCameraEvent) {
        val intent = Intent(EventConstants.ACTION_NAME)
        intent.putExtra(EventConstants.EVENT_DATA, data.toJson())
        sendBroadcast(intent)
    }
}