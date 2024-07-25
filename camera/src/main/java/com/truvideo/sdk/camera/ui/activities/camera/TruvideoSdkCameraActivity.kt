package com.truvideo.sdk.camera.ui.activities.camera

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.R
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.VersionPropertiesAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraServiceCallback
import com.truvideo.sdk.camera.ui.components.capture_button.CaptureButton
import com.truvideo.sdk.camera.ui.components.exit_panel.ExitPanel
import com.truvideo.sdk.camera.ui.components.media_count_indicator.MediaCountIndicator
import com.truvideo.sdk.camera.ui.components.media_panel.MediaPanel
import com.truvideo.sdk.camera.ui.components.media_preview_panel.MediaPreviewPanel
import com.truvideo.sdk.camera.ui.components.pause_button.PauseButton
import com.truvideo.sdk.camera.ui.components.recording_duration_indicator.RecordingDurationIndicator
import com.truvideo.sdk.camera.ui.components.recording_indicator.RecordingIndicator
import com.truvideo.sdk.camera.ui.components.resolution_panel.ResolutionPanel
import com.truvideo.sdk.camera.ui.components.rotate_button.RotateButton
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicator
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.ui.components.rotated_box.AnimatedFadeRotatedBox
import com.truvideo.sdk.camera.ui.components.toast.ToastContainer
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import com.truvideo.sdk.camera.utils.OrientationLiveData
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
import com.truvideo.sdk.components.animated_value.animateFloat
import com.truvideo.sdk.components.button.TruvideoContinueButton
import com.truvideo.sdk.components.button.TruvideoIconButton
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TruvideoSdkCameraActivity : ComponentActivity() {

    private lateinit var configuration: TruvideoSdkCameraConfiguration

    private lateinit var cameraService: TruvideoSdkCameraService

    private lateinit var getCameraInformationUseCase: GetCameraInformationUseCase

    private lateinit var manipulateResolutionsUseCase: ManipulateResolutionsUseCase

    private lateinit var authAdapter: TruvideoSdkCameraAuthAdapter

    private lateinit var viewModel: TruvideoSdkCameraActivityViewModel

    private lateinit var textureView: TextureView

    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            cameraService.openCamera {
                viewModel.updateIsPreviewVisible(true)
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

    private fun makeFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val view: View = window.decorView
        WindowInsetsControllerCompat(window, view).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private var permissionGranted = false
    private lateinit var tapToFocusGestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
        makeFullScreen()

        tapToFocusGestureDetector = GestureDetector(this, cameraService.tapToFocusListener)
        scaleGestureDetector = ScaleGestureDetector(this, cameraService.scaleGestureListener)

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

        OrientationLiveData(this@TruvideoSdkCameraActivity.applicationContext).apply {
            observe(this@TruvideoSdkCameraActivity, viewModel::updateSensorOrientation)
        }

        setContent {
            // Report authentication error
            LaunchedEffect(isAuthenticated) {
                if (!isAuthenticated) {
                    showFatalErrorDialog(title = "Error", message = "Authentication required")
                }
            }

            var isPermissionGranted by remember { mutableStateOf(false) }

            // Only validate permission when authentication is granted
            if (isAuthenticated) {
                val cameraPermissionResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions(),
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

    override fun onResume() {
        super.onResume()

        if (permissionGranted) {
            Log.d("Camera", "On resume with permissions")

            if (textureView.isAvailable) {
                Log.d("Camera", "Open camera")
                cameraService.openCamera {
                    Log.d("Camera", "Camera ready")
                    viewModel.updateIsPreviewVisible(true)
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
            cameraService.disconnect()
        } else {
            Log.d("Camera", "On pause without permissions")
        }
    }

    override fun onDestroy() {
        viewModel.close()
        super.onDestroy()
    }

    @Composable
    internal fun CameraPreview() {
        var panelResolutionVisible by remember { mutableStateOf(false) }
        var panelMediaVisible by remember { mutableStateOf(false) }
        var panelMediaPreviewIndex by remember { mutableIntStateOf(0) }
        var panelMediaPreviewVisible by remember { mutableStateOf(false) }
        var panelExitVisible by remember { mutableStateOf(false) }

        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
        val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
        val isPreviewVisible by viewModel.isPreviewVisible.collectAsStateWithLifecycle()
        val camera by viewModel.camera.collectAsStateWithLifecycle()
        val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
        val media by viewModel.media.collectAsStateWithLifecycle()
        val orientation by viewModel.orientation.collectAsStateWithLifecycle()
        val screenFlipped = remember(orientation) { orientation.isPortraitReverse }
        val isPortrait by viewModel.isPortrait.collectAsStateWithLifecycle()
        val resolutions by viewModel.resolutions.collectAsStateWithLifecycle()
        val resolution by viewModel.resolution.collectAsStateWithLifecycle()
        val withFlash by viewModel.withFlash.collectAsStateWithLifecycle()
        val zoomFactor by viewModel.zoomFactor.collectAsStateWithLifecycle()
        var zoomIndicatorMode by remember { mutableStateOf(ZoomIndicatorMode.Indicator) }
        var touchPosition by remember { mutableStateOf(IntOffset.Zero) }
        val withMedia = remember(media) { media.isNotEmpty() }
        val continueButtonVisible = withMedia && !isRecording

        val focusState by viewModel.focusState.collectAsStateWithLifecycle()
        val focusStateAlphaAnim = animateFloat(
            value = when (focusState) {
                FocusState.IDLE -> 0f
                FocusState.REQUESTED -> 0.5f
                FocusState.LOCKED -> 1f
            }
        ).coerceIn(0.0f, 1.0f)

        LaunchedEffect(resolution) {
            if (resolution != null) {
                adjustAspectRatio(resolution!!.width, resolution!!.height)
            }
        }

        LaunchedEffect(focusState) {
            if (focusState == FocusState.LOCKED) {
                delay(800)
                viewModel.updateFocusState(FocusState.IDLE)
            }
        }

        fun updateTouchPosition(event: MotionEvent) {
            val offsetSize = (FOCUS_INDICATOR_SIZE)
            val offsetX = (event.x - offsetSize).toInt().coerceAtLeast(0)
            val offsetY = (event.y - offsetSize).toInt().coerceAtLeast(0)
            touchPosition = IntOffset(offsetX, offsetY)
        }

        fun takePicture() {
            val mediaCount = media.size
            val pictureCount = media.filter { it.type == TruvideoSdkCameraMediaType.PICTURE }.size
            val mediaLimit = configuration.mode.mediaLimit
            val pictureLimit = configuration.mode.pictureLimit

            if (mediaLimit != null && mediaCount >= mediaLimit) {
                viewModel.showToast("You have reached the maximum number of pictures for this session")
                return
            }

            if (pictureLimit != null && pictureCount >= pictureLimit) {
                viewModel.showToast("You have reached the maximum number of pictures for this session")
                return
            }

            zoomIndicatorMode = ZoomIndicatorMode.Indicator
            cameraService.takePicture()
        }

        fun toggleRecording() {
            if (isRecording) {
                cameraService.toggleRecording()
            } else {
                val mediaCount = media.size
                val videoCount = media.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }.size
                val mediaLimit = configuration.mode.mediaLimit
                val videoLimit = configuration.mode.videoLimit

                if (mediaLimit != null && mediaCount >= mediaLimit) {
                    viewModel.showToast("You have reached the maximum number of videos for this session")
                    return
                }

                if (videoLimit != null && videoCount >= videoLimit) {
                    viewModel.showToast("You have reached the maximum number of videos for this session")
                    return
                }

                cameraService.toggleRecording()
            }
        }

        fun onMediaCountIndicatorPressed() {
            if (media.isEmpty()) return
            if (media.size == 1) {
                panelMediaPreviewIndex = 0
                panelMediaPreviewVisible = true
            } else {
                panelMediaVisible = true
            }
        }

        fun processContinue() {
            cameraService.disconnect {
                val intent = Intent()
                val list: List<String> = media.map { it.toJson() }.toList()
                intent.putStringArrayListExtra("media", ArrayList(list))
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        fun onClosePressed() {
            zoomIndicatorMode = ZoomIndicatorMode.Indicator

            if (media.isEmpty()) {
                viewModel.updateIsPreviewVisible(false)
                cameraService.disconnect {
                    finish()
                }
            } else {
                panelExitVisible = true
            }
        }

        BackHandler(true) {
            if (isBusy) return@BackHandler
            if (isRecording) return@BackHandler
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
                                enabled = !isRecording && !isBusy,
                                onPressed = { onClosePressed() },
                            )
                        }

                        // Media count indicator
                        AnimatedContent(
                            targetState = isPortrait,
                            label = ""
                        ) { isPortraitTarget ->
                            if (isPortraitTarget) {
                                Row {
                                    Box(modifier = Modifier.width(4.dp))

                                    val videoCount = remember(media) {
                                        media.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }.size
                                    }

                                    val pictureCount = remember(media) {
                                        media.filter { it.type == TruvideoSdkCameraMediaType.PICTURE }.size
                                    }

                                    MediaCountIndicator(
                                        videoCount = videoCount,
                                        pictureCount = pictureCount,
                                        mode = configuration.mode,
                                        enabled = !isBusy && (!isRecording || isPaused),
                                        onPressed = { onMediaCountIndicatorPressed() },
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Resolutions button
                        AnimatedContent(
                            targetState = resolutions.size > 1,
                            label = ""
                        ) { buttonVisibleTarget ->
                            if (buttonVisibleTarget) {
                                Row {
                                    Box(modifier = Modifier.width(4.dp))

                                    TruvideoAnimatedRotation(
                                        rotation = currentOrientation.uiRotation,
                                    ) {
                                        TruvideoIconButton(
                                            icon = Icons.Default.Hd,
                                            small = true,
                                            enabled = !isBusy && !isRecording,
                                            onPressed = {
                                                zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                                panelResolutionVisible = true
                                            },
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }

                        // Flash button
                        AnimatedContent(
                            targetState = withFlash,
                            label = ""
                        ) { withFlashTarget ->
                            if (withFlashTarget) {
                                Row {
                                    Box(modifier = Modifier.width(4.dp))
                                    TruvideoAnimatedRotation(
                                        rotation = currentOrientation.uiRotation,
                                    ) {
                                        TruvideoIconButton(
                                            icon = Icons.Default.FlashOn,
                                            small = true,
                                            enabled = !isBusy,
                                            onPressed = {
                                                zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                                cameraService.toggleFlash()
                                            },
                                            selected = flashMode.isOn
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }

                        // Continue button
                        val portraitContinueButtonVisible = continueButtonVisible && isPortrait
                        AnimatedContent(
                            targetState = portraitContinueButtonVisible,
                            label = ""
                        ) { portraitContinueButtonVisibleTarget ->
                            if (portraitContinueButtonVisibleTarget) {
                                Row {
                                    Box(modifier = Modifier.width(4.dp))
                                    TruvideoContinueButton(
                                        small = true,
                                        enabled = !isBusy,
                                        onPressed = { processContinue() }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }
                    }
                }

                @Composable
                fun bottomBarContent(flipped: Boolean = false) {
                    val currentOrientation = if (flipped) TruvideoSdkCameraOrientation.PORTRAIT else orientation

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        // Take picture
                        val pictureButtonVisible = configuration.mode.canTakeVideo && configuration.mode.canTakePicture
                        TruvideoAnimatedOpacity(
                            opacity = if (pictureButtonVisible) 1f else 0f
                        ) {
                            TruvideoAnimatedRotation(
                                rotation = currentOrientation.uiRotation,
                            ) {
                                TruvideoIconButton(
                                    icon = Icons.Outlined.CameraAlt,
                                    size = 50f,
                                    enabled = pictureButtonVisible && !isBusy,
                                    onPressed = { takePicture() },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Recording
                        CaptureButton(
                            recording = isRecording,
                            enabled = !isBusy,
                            onPressed = {
                                zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                if (configuration.mode.canTakeVideo) {
                                    toggleRecording()
                                } else if (configuration.mode.canTakePicture) {
                                    takePicture()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        AnimatedContent(targetState = isRecording, label = "") { isRecordingTarget ->
                            // Pause or play
                            if (isRecordingTarget) {
                                PauseButton(
                                    isPaused = isPaused,
                                    size = 50f,
                                    enabled = !isBusy,
                                    rotation = currentOrientation.uiRotation,
                                    onPressed = {
                                        if (isPaused) cameraService.resumeRecording()
                                        else cameraService.pauseRecording()
                                        viewModel.updateIsPaused(!isPaused)
                                    },
                                )
                            } else {
                                RotateButton(
                                    size = 50f,
                                    enabled = !isBusy,
                                    rotation = currentOrientation.uiRotation,
                                    onPressed = {
                                        if (viewModel.isBusy.value) return@RotateButton
                                        val c = camera ?: return@RotateButton

                                        zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                        viewModel.updateIsPreviewVisible(false)
                                        viewModel.updateIsBusy(true)
                                        viewModel.viewModelScope.launch {
                                            delay(300)
                                            cameraService.disconnectSuspend(stopRecording = false)
                                            val newLensFacing = when (c.lensFacing) {
                                                TruvideoSdkCameraLensFacing.BACK -> TruvideoSdkCameraLensFacing.FRONT
                                                TruvideoSdkCameraLensFacing.FRONT -> TruvideoSdkCameraLensFacing.BACK
                                            }
                                            cameraService.lensFacing = newLensFacing
                                            cameraService.openCameraSuspend()
                                            viewModel.updateIsPreviewVisible(true)
                                        }
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
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

                // Flipped bottom bar
                AnimatedContent(targetState = screenFlipped, label = "") { screenFlippedTarget ->
                    if (screenFlippedTarget) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationZ = 180f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                        ) {
                            bottomBarContent(flipped = true)
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
                                    textureView.apply {
                                        setOnTouchListener(fun(
                                            _: View, event: MotionEvent
                                        ): Boolean {
                                            updateTouchPosition(event)
                                            if (isBusy || !isPreviewVisible) return false

                                            zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                            tapToFocusGestureDetector.onTouchEvent(event)
                                            scaleGestureDetector.onTouchEvent(event)
                                            return true
                                        })
                                    }
                                },
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(10.dp))
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures {}
                                    },
                            )


                            // Focus indicator
                            Icon(
                                painter = painterResource(id = R.drawable.focus_camera),
                                contentDescription = "",
                                modifier = Modifier
                                    .offset { touchPosition }
                                    .alpha(focusStateAlphaAnim)
                                    .size(80.dp),
                                tint = Color.White
                            )


                            // Recording indicator
                            Box(
                                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp))
                            ) {
                                RecordingIndicator(
                                    isRecording = isRecording,
                                    isPaused = isPaused
                                )
                            }


                            // Zoom indicator port
                            TruvideoAnimatedFadeVisibility(
                                visible = !orientation.isPortraitReverse,
                            ) {
                                ZoomIndicator(zoom = zoomFactor,
                                    orientation = orientation,
                                    mode = zoomIndicatorMode,
                                    onModeChange = { zoomIndicatorMode = it },
                                    onZoomChange = { cameraService.performZoom(it) })
                            }

                            // Zoom indicator port-reverse
                            TruvideoAnimatedFadeVisibility(
                                visible = orientation.isPortraitReverse,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(180f)
                                ) {
                                    ZoomIndicator(zoom = zoomFactor,
                                        orientation = TruvideoSdkCameraOrientation.PORTRAIT,
                                        mode = zoomIndicatorMode,
                                        onModeChange = { zoomIndicatorMode = it },
                                        onZoomChange = { cameraService.performZoom(it) })
                                }
                            }


                            AnimatedFadeRotatedBox(orientation = orientation) {
                                // Recording duration
                                TruvideoAnimatedFadeVisibility(configuration.mode.canTakeVideo) {

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .wrapContentSize()
                                                .padding(8.dp)
                                                .align(Alignment.TopCenter)
                                        ) {
                                            val cameraDuration by cameraService.videoDuration.collectAsStateWithLifecycle()
                                            val time = remember(cameraDuration) { cameraDuration.toDuration(DurationUnit.MILLISECONDS) }
                                            val durationLimit = remember {
                                                cameraService.maxDuration?.toDuration(DurationUnit.MILLISECONDS)
                                            }

                                            val remainingTime = if (durationLimit != null && isRecording && !isBusy) {
                                                durationLimit - time
                                            } else {
                                                null
                                            }

                                            RecordingDurationIndicator(
                                                time = time,
                                                remainingTime = remainingTime,
                                                recording = isRecording
                                            )
                                        }
                                    }
                                }

                                // Toast
                                Box {
                                    val toastVisible by remember { viewModel.toastVisible }.collectAsStateWithLifecycle()
                                    val toastText by remember { viewModel.toastText }.collectAsStateWithLifecycle()
                                    ToastContainer(
                                        text = toastText,
                                        visible = toastVisible,
                                        onPressed = { viewModel.hideToast() }
                                    )
                                }
                            }

                            AnimatedFadeRotatedBox(
                                orientation = orientation,
                                orientations = persistentMapOf(
                                    TruvideoSdkCameraOrientation.LANDSCAPE_RIGHT to true,
                                    TruvideoSdkCameraOrientation.LANDSCAPE_LEFT to true
                                )
                            ) {

                                // Media count
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    MediaCountIndicator(
                                        videoCount = media.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }.size,
                                        pictureCount = media.filter { it.type == TruvideoSdkCameraMediaType.PICTURE }.size,
                                        mode = configuration.mode,
                                        enabled = !isBusy,
                                        onPressed = { onMediaCountIndicatorPressed() },
                                    )
                                }

                                // Continue button
                                TruvideoAnimatedFadeVisibility(
                                    visible = continueButtonVisible,
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            TruvideoContinueButton(
                                                enabled = !isBusy,
                                                small = true,
                                                onPressed = { processContinue() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Normal bottom bar
                AnimatedContent(targetState = screenFlipped, label = "") { screenFlippedTarget ->
                    if (!screenFlippedTarget) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            bottomBarContent(flipped = false)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }

                // Flipped app bar
                AnimatedContent(targetState = screenFlipped, label = "") { screenFlippedTarget ->
                    if (screenFlippedTarget) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    rotationZ = 180f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }
                        ) {
                            appBarContent(flipped = true)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }
            }

            // Resolution panel
            ResolutionPanel(
                visible = panelResolutionVisible,
                resolutions = resolutions,
                close = { panelResolutionVisible = false },
                selectedResolution = resolution,
                orientation = orientation,
                onResolutionPicked = { resolution ->
                    panelResolutionVisible = false

                    val c = camera ?: return@ResolutionPanel

                    viewModel.updateIsPreviewVisible(false)
                    viewModel.updateIsBusy(true)
                    viewModel.viewModelScope.launch {
                        delay(300)
                        cameraService.disconnectSuspend()
                        when (c.lensFacing) {
                            TruvideoSdkCameraLensFacing.BACK -> {
                                viewModel.updateBackResolution(resolution)
                                cameraService.backResolution = resolution
                            }

                            TruvideoSdkCameraLensFacing.FRONT -> {
                                viewModel.updateFrontResolution(resolution)
                                cameraService.frontResolution = resolution
                            }
                        }
                        cameraService.openCameraSuspend()
                        viewModel.updateIsPreviewVisible(true)
                    }
                }
            )

            // Media panel
            MediaPanel(
                visible = panelMediaVisible,
                media = media,
                close = { panelMediaVisible = false },
                onPressed = { item ->
                    val index = media.indexOfFirst { it.filePath == item.filePath }
                    if (index == -1) return@MediaPanel
                    panelMediaPreviewIndex = index
                    panelMediaPreviewVisible = true
                },
                orientation = orientation,
            )

            // Media panel preview
            MediaPreviewPanel(
                visible = panelMediaPreviewVisible,
                media = media,
                orientation = orientation,
                initialIndex = panelMediaPreviewIndex,
                close = {
                    panelMediaPreviewVisible = false
                    if (viewModel.media.value.size <= 1) {
                        panelMediaVisible = false
                    }
                },
                onDelete = {
                    viewModel.removeMedia(it.filePath)
                    if (viewModel.media.value.isEmpty()) {
                        panelMediaPreviewVisible = false
                        panelMediaVisible = false
                    }
                }
            )

            // Exit panel
            ExitPanel(
                visible = panelExitVisible,
                enabled = !isBusy,
                orientation = orientation,
                onDiscardPressed = {
                    cameraService.disconnect {
                        deleteAll()
                        finish()
                    }
                },
                close = { panelExitVisible = false }
            )
        }
    }

    private fun deleteAll() {
        CoroutineScope(Dispatchers.IO).launch {
            val media = viewModel.media.value
            media.forEach {
                try {
                    File(it.filePath).delete()
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun setup() {
        configuration = TruvideoSdkCameraConfiguration.fromJson(intent.getStringExtra("configuration") ?: "")
        authAdapter = TruvideoSdkCameraAuthAdapterImpl(versionPropertiesAdapter = VersionPropertiesAdapter(this))
        viewModel = TruvideoSdkCameraActivityViewModel()
        manipulateResolutionsUseCase = ManipulateResolutionsUseCase()
        getCameraInformationUseCase = GetCameraInformationUseCase(
            context = applicationContext,
            manipulateResolutionsUseCase = manipulateResolutionsUseCase
        )
        textureView = TextureView(this)

        Log.d("TruvideoSdkCamera", "configuration: $configuration")

        val information = getCameraInformationUseCase()
        cameraService = TruvideoSdkCameraService(
            context = this,
            information = information,
            textureView = textureView,
            serviceCallback = object : TruvideoSdkCameraServiceCallback {
                override fun onRecordingStarted() {
                    viewModel.updateIsRecording(true)
                }

                override fun onTakePictureStarted() {
                    viewModel.updateTakingPicture(true)
                }

                override fun onPicture(file: File) {
                    viewModel.updateTakingPicture(false)

                    val camera = viewModel.camera.value ?: return
                    val resolution = viewModel.resolution.value ?: return

                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.PICTURE,
                            filePath = file.path,
                            cameraLensFacing = camera.lensFacing,
                            resolution = resolution,
                            rotation = viewModel.orientation.value,
                            duration = 0L,
                        ),
                    )

                    if (configuration.mode.autoClose) {
                        cameraService.disconnect {
                            finish()
                        }
                    }
                }

                override fun onVideo(file: File, duration: Long, maxDurationReached: Boolean) {
                    val camera = viewModel.camera.value ?: return
                    val resolution = viewModel.resolution.value ?: return

                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.VIDEO,
                            filePath = file.path,
                            cameraLensFacing = camera.lensFacing,
                            resolution = resolution,
                            rotation = viewModel.orientation.value,
                            duration = duration,
                        ),
                    )

                    if (maxDurationReached) {
                        viewModel.showToast("Maximum video duration reached")
                    }

                    viewModel.updateIsRecording(false)

                    if (configuration.mode.autoClose) {
                        cameraService.disconnect {
                            finish()
                        }
                    }
                }

                override fun onCameraDisconnected() {
                    finish()
                }

                override fun updateIsBusy(isBusy: Boolean) {
                    viewModel.updateIsBusy(isBusy)
                }

                override fun updateIsPaused(isPaused: Boolean) {
                    viewModel.updateIsPaused(isPaused)
                }

                override fun updateCamera(camera: TruvideoSdkCameraDevice) {
                    viewModel.updateCamera(camera)
                }

                override fun updateFlashMode(
                    cameraLensFacing: TruvideoSdkCameraLensFacing,
                    flashMode: TruvideoSdkCameraFlashMode
                ) {
                    when (cameraLensFacing) {
                        TruvideoSdkCameraLensFacing.BACK -> viewModel.updateBackFlashMode(flashMode)
                        TruvideoSdkCameraLensFacing.FRONT -> viewModel.updateFrontFlashMode(
                            flashMode
                        )
                    }
                }

                override fun getSensorRotation(): TruvideoSdkCameraOrientation {
                    return viewModel.orientation.value
                }

                override fun onFocusRequest() {
                    viewModel.updateFocusState(FocusState.REQUESTED)
                }

                override fun updateZoomVisibility(visible: Boolean) {
                    viewModel.updateIsZoomVisible(visible)
                }

                override fun updateZoom(value: Float) {
                    viewModel.updateZoomValue(value)
                }

                override fun onFocusLocked() {
                    viewModel.updateFocusState(FocusState.LOCKED)
                }
            },
        )

        val info = cameraService.information
        if (!info.withCameras) {
            throw RuntimeException("No cameras available")
        }

        // Output path
        val outputPath = if (configuration.outputPath.trim().isEmpty()) {
            "${filesDir.path}/truvideo-sdk/camera"
        } else {
            configuration.outputPath
        }
        cameraService.outputPath = outputPath

        // Fixed orientation
        viewModel.updateFixedOrientation(configuration.orientation)

        // Lens facing
        val camera = when (configuration.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> info.backCamera ?: info.frontCamera
            ?: throw RuntimeException("No camera available")

            TruvideoSdkCameraLensFacing.FRONT -> info.frontCamera ?: info.backCamera
            ?: throw RuntimeException("No camera available")
        }
        cameraService.lensFacing = camera.lensFacing

        // Resolutions
        if (info.frontCamera != null) {
            if (configuration.frontResolutions.isNotEmpty()) {
                val resolutions = manipulateResolutionsUseCase.filter(
                    info.frontCamera.resolutions, configuration.frontResolutions
                )
                viewModel.updateFrontResolutions(manipulateResolutionsUseCase.sort(resolutions))
            } else {
                viewModel.updateFrontResolutions(manipulateResolutionsUseCase.sort(info.frontCamera.resolutions))
            }

            val resolution = calculateResolution(
                items = info.frontCamera.resolutions,
                picked = configuration.frontResolutions,
                preferred = configuration.frontResolution
            )
            cameraService.frontResolution = resolution
            viewModel.updateFrontResolution(resolution)
        }

        if (info.backCamera != null) {
            if (configuration.backResolutions.isNotEmpty()) {
                val resolutions = manipulateResolutionsUseCase.filter(
                    info.backCamera.resolutions, configuration.backResolutions
                )
                viewModel.updateBackResolutions(manipulateResolutionsUseCase.sort(resolutions))
            } else {
                viewModel.updateBackResolutions(manipulateResolutionsUseCase.sort(info.backCamera.resolutions))
            }

            val resolution = calculateResolution(
                items = info.backCamera.resolutions,
                picked = configuration.backResolutions,
                preferred = configuration.backResolution
            )
            cameraService.backResolution = resolution
            viewModel.updateBackResolution(resolution)
        }

        // Flash
        cameraService.frontFlashMode = configuration.flashMode
        viewModel.updateFrontFlashMode(configuration.flashMode)

        cameraService.backFlashMode = configuration.flashMode
        viewModel.updateBackFlashMode(configuration.flashMode)

        // Duration limit
        cameraService.maxDuration = configuration.mode.videoDurationLimit
    }

    private fun calculateResolution(
        items: List<TruvideoSdkCameraResolution>,
        picked: List<TruvideoSdkCameraResolution>,
        preferred: TruvideoSdkCameraResolution?
    ): TruvideoSdkCameraResolution {
        if (items.isEmpty()) return TruvideoSdkCameraResolution(0, 0)

        var pickedResolutions = picked.toList()
        if (pickedResolutions.isEmpty()) pickedResolutions = items.toList()

        val validResolutions = manipulateResolutionsUseCase.filter(items, pickedResolutions)
        if (validResolutions.isEmpty()) return items.first()

        if (preferred != null && manipulateResolutionsUseCase.contain(validResolutions, preferred)) {
            return preferred
        }

        return validResolutions.first()
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


    companion object {
        const val FOCUS_INDICATOR_SIZE = 80
    }
}



