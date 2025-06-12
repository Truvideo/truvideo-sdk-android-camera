package com.truvideo.sdk.camera.ui.activities.scanner

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraLogAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraVersionPropertiesAdapterImpl
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraVersionPropertiesAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerConfiguration
import com.truvideo.sdk.camera.service.scanner.TruvideoSdkCameraScannerCallback
import com.truvideo.sdk.camera.service.scanner.TruvideoSdkCameraScannerService
import com.truvideo.sdk.camera.ui.activities.scanner.components.animated_scanner_overlay.CustomAnimatedScannerOverlay
import com.truvideo.sdk.camera.ui.activities.scanner.components.scanner_code_preview_panel.CustomScannerCodePreviewPanel
import com.truvideo.sdk.camera.ui.components.rotated_box.AnimatedFadeRotatedBox
import com.truvideo.sdk.camera.ui.components.toast.ToastContainer
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import com.truvideo.sdk.camera.utils.OrientationLiveData
import com.truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.button.TruvideoIconButton
import kotlin.math.roundToInt


class TruvideoSdkCameraScannerActivity : ComponentActivity() {

    private lateinit var configuration: TruvideoSdkCameraScannerConfiguration

    private lateinit var getCameraInformationUseCase: GetCameraInformationUseCase

    private lateinit var manipulateResolutionsUseCase: ManipulateResolutionsUseCase

    private lateinit var logAdapter: TruvideoSdkCameraLogAdapter

    private lateinit var versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapter

    private lateinit var authAdapter: TruvideoSdkCameraAuthAdapter

    private lateinit var textureView: TextureView

    private var globalCameraService: TruvideoSdkCameraScannerService? = null
    private var globalViewModel: TruvideoSdkCameraScannerViewModel? = null

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            globalCameraService?.openCamera {
                globalViewModel?.updateIsPreviewVisible(true)
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        }
    }


    private fun setup() {
        configuration = TruvideoSdkCameraScannerConfiguration.fromJson(intent.getStringExtra(TruvideoSdkCameraScannerContract.INPUT) ?: "")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        versionPropertiesAdapter = TruvideoSdkCameraVersionPropertiesAdapterImpl(
            context = this
        )
        logAdapter = TruvideoSdkCameraLogAdapterImpl(
            versionPropertiesAdapter = versionPropertiesAdapter
        )
        authAdapter = TruvideoSdkCameraAuthAdapterImpl(
            versionPropertiesAdapter = versionPropertiesAdapter,
            logAdapter = logAdapter
        )
        manipulateResolutionsUseCase = ManipulateResolutionsUseCase()
        getCameraInformationUseCase = GetCameraInformationUseCase(
            context = applicationContext,
            manipulateResolutionsUseCase = manipulateResolutionsUseCase
        )
        textureView = TextureView(this)
    }

    private fun makeFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val view: View = window.decorView
        WindowInsetsControllerCompat(window, view).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private var permissionGranted = false

    override fun onResume() {
        super.onResume()

        if (permissionGranted) {
            Log.d("Camera", "On resume with permissions")

            if (textureView.isAvailable) {
                Log.d("Camera", "Open camera")
                globalCameraService?.openCamera {
                    Log.d("Camera", "Camera ready")
                    globalViewModel?.updateIsPreviewVisible(true)
                }
            } else {
                Log.d("Camera", "Waiting for texture to be ready")
                textureView.surfaceTextureListener = textureListener
            }
        } else {
            Log.d("Camera", "On resume without permissions")
        }
    }

    override fun onPause() {
        super.onPause()

        if (permissionGranted) {
            globalCameraService?.disconnect()
        } else {
            Log.d("Camera", "On pause without permissions")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
        makeFullScreen()

        fun showFatalErrorDialog(
            title: String,
            message: String
        ) {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnDismissListener {
                    finish()
                }
                .setPositiveButton("Accept") { _, _ ->
                }
                .show()
        }

        val isAuthenticated = try {
            authAdapter.validateAuthentication()
            true
        } catch (exception: Exception) {
            false
        }

        setContent {
            // Report authentication error
            LaunchedEffect(isAuthenticated) {
                if (!isAuthenticated) {
                    showFatalErrorDialog(title = "Error", message = "Authentication required")
                }
            }

            var isPermissionGranted by remember { mutableStateOf(false) }
            val cameraPermissionResultLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissionsMap ->
                    val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                    if (areGranted) {
                        textureView.surfaceTextureListener = textureListener
                        isPermissionGranted = true
                        permissionGranted = true
                    } else {
                        showFatalErrorDialog(
                            title = "Error",
                            message = "Camera and microphone permission required"
                        )
                    }
                }
            )

            // Only validate permission when authentication is granted
            if (isAuthenticated) {
                LaunchedEffect(Unit) {
                    val permissions = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    cameraPermissionResultLauncher.launch(permissions.toTypedArray())
                }
            }

            TruVideoSdkCameraTheme {
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxSize()
                ) {
                    if (isAuthenticated && isPermissionGranted) {
                        CameraPreview()
                    }
                }
            }
        }
    }

    @Composable
    internal fun CameraPreview() {
        val context = LocalContext.current
        val owner = LocalLifecycleOwner.current
        val viewModel = viewModel<TruvideoSdkCameraScannerViewModel>(
            factory = TruvideoSdkCameraScannerViewModelFactory(
                fixedOrientation = configuration.orientation,
                flashMode = configuration.flashMode
            )
        )
        globalViewModel = viewModel

        val cameraService = remember { createCameraService(viewModel) }
        globalCameraService = cameraService

        val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
        val orientation by viewModel.orientation.collectAsStateWithLifecycle()
        val isPreviewVisible by viewModel.isPreviewVisible.collectAsStateWithLifecycle()
        val screenFlipped = remember(orientation) { orientation.isPortraitReverse }
        val resolution by viewModel.resolution.collectAsStateWithLifecycle()
        val isCodeVisible by viewModel.isCodeVisible.collectAsStateWithLifecycle()
        val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()

        // Sensor orientation
        LaunchedEffect(Unit) {
            OrientationLiveData(context).apply {
                observe(owner, viewModel::updateSensorOrientation)
            }
        }

        LaunchedEffect(resolution) {
            if (resolution != null) {
                adjustAspectRatio(resolution!!.width, resolution!!.height)
            }
        }

        fun onClosePressed() {
            viewModel.updateIsPreviewVisible(false)
            cameraService.disconnect {
                finish()
            }
        }

        BackHandler(true) {
            if (isBusy) return@BackHandler
            onClosePressed()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {

                @Composable
                fun appBarContent(flipped: Boolean = false) {
                    val currentOrientation = if (flipped) TruvideoSdkCameraOrientation.PORTRAIT else orientation

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Close button
                        TruvideoAnimatedRotation(
                            rotation = currentOrientation.uiRotation,
                        ) {
                            TruvideoIconButton(
                                icon = Icons.Default.Close,
                                small = true,
                                enabled = !isBusy,
                                onPressed = { onClosePressed() },
                            )
                        }

                        Box(Modifier.weight(1f))

                        TruvideoAnimatedRotation(
                            rotation = orientation.uiRotation,
                        ) {
                            TruvideoIconButton(
                                icon = Icons.Default.FlashOn,
                                small = true,
                                enabled = !isBusy,
                                selected = flashMode == TruvideoSdkCameraFlashMode.ON,
                                onPressed = { cameraService.toggleFlash() },
                            )
                        }
                    }
                }


                // Normal app bar
                AnimatedContent(targetState = screenFlipped, label = "") { screenFlippedTarget ->
                    if (!screenFlippedTarget) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            appBarContent(flipped = false)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    TruvideoAnimatedOpacity(opacity = if (isPreviewVisible) 1.0f else 0.0f) {
                        Box {

                            // Camera Preview
                            AndroidView(
                                factory = {
                                    textureView
                                },
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(10.dp))
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures {}
                                    },
                            )

                            // Overlay
                            this@Column.AnimatedVisibility(
                                visible = !isCodeVisible,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CustomAnimatedScannerOverlay()
                                }
                            }

                            AnimatedFadeRotatedBox(
                                orientation = orientation
                            ) {
                                // Toast
                                Box {
                                    val toastVisible by remember { viewModel.toastVisible }.collectAsStateWithLifecycle()
                                    val toastText by remember { viewModel.toastText }.collectAsStateWithLifecycle()
                                    ToastContainer(text = toastText,
                                        visible = toastVisible,
                                        onPressed = { viewModel.hideToast() })
                                }
                            }
                        }
                    }
                }

                // Flipped app bar
                AnimatedContent(targetState = screenFlipped, label = "") { screenFlippedTarget ->
                    if (screenFlippedTarget) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            appBarContent(flipped = true)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }

            }

            // Barcode panel
            val code by viewModel.code.collectAsStateWithLifecycle()
            CustomScannerCodePreviewPanel(
                visible = isCodeVisible,
                enabled = !isBusy,
                orientation = orientation,
                code = code,
                onConfirm = {
                    confirmBarcode(
                        service = cameraService,
                        viewModel = viewModel
                    )
                },
                close = {
                    cameraService.enableScanning()
                    viewModel.updateIsCodeVisible(false)
                },
            )
        }
    }

    private fun adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
        val width = textureView.width
        val height = textureView.height
        val newWidth: Int
        val newHeight: Int
        val aspectRatio = videoWidth.toFloat() / videoHeight.toFloat()
        val actualRatio = if (width > height) aspectRatio else 1f / aspectRatio
        if (width < height * actualRatio) {
            newHeight = height
            newWidth = (height * actualRatio).roundToInt()
        } else {
            newWidth = width
            newHeight = (width / actualRatio).roundToInt()
        }

        val xOff = (width - newWidth) / 2
        val yOff = (height - newHeight) / 2
        val matrix = Matrix()
        textureView.getTransform(matrix)
        matrix.setScale(newWidth.toFloat() / width, newHeight.toFloat() / height)
        matrix.postTranslate(xOff.toFloat(), yOff.toFloat())
        textureView.setTransform(matrix)
    }

    private fun confirmBarcode(
        service: TruvideoSdkCameraScannerService,
        viewModel: TruvideoSdkCameraScannerViewModel
    ) {
        service.disconnect {
            val intent = Intent()
            intent.putExtra(TruvideoSdkCameraScannerContract.RESULT, viewModel.code.value?.toJson())
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    private fun createCameraService(
        viewModel: TruvideoSdkCameraScannerViewModel
    ): TruvideoSdkCameraScannerService {
        val information = getCameraInformationUseCase()

        var service: TruvideoSdkCameraScannerService? = null
        service = TruvideoSdkCameraScannerService(
            context = this,
            information = information,
            textureView = textureView,
            serviceCallback = object : TruvideoSdkCameraScannerCallback {
                override fun onCodeScanned(code: TruvideoSdkCameraScannerCode) {
                    val validator = TruvideoSdkCameraScannerContract.validator
                    if (validator != null) {
                        val result = validator.validate(code)
                        if (!result.accept) {
                            viewModel.showToast(result.message ?: "Invalid code")
                            return
                        }
                    }

                    if (configuration.autoClose) {
                        viewModel.updateCode(code)
                        confirmBarcode(
                            service = service!!,
                            viewModel = viewModel
                        )
                        return
                    }

                    if (!viewModel.isCodeVisible.value) {
                        viewModel.updateCode(code)
                        viewModel.updateIsCodeVisible(true)
                        service?.disableScanning()
                    }
                }

                override fun onCameraDisconnected() {
                    finish()
                }

                override fun updateResolution(resolution: TruvideoSdkCameraResolution) {
                    viewModel.updateResolution(resolution)
                }

                override fun updateIsBusy(isBusy: Boolean) {
                    viewModel.updateIsBusy(isBusy)
                }

                override fun updateCamera(camera: TruvideoSdkCameraDevice) {
                    viewModel.updateCamera(camera)
                }

                override fun getSensorRotation(): TruvideoSdkCameraOrientation {
                    return viewModel.orientation.value
                }

                override fun updateFlashMode(flashMode: TruvideoSdkCameraFlashMode) {
                    viewModel.updateFlashMode(flashMode)
                }
            },
        )

        service.buildCodeScanner(configuration.codeFormats)
        service.flashMode = configuration.flashMode
        return service
    }
}



