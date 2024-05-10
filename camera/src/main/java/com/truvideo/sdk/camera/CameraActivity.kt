package com.truvideo.sdk.camera

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraService
import com.truvideo.sdk.camera.service.camera.TruvideoSdkCameraServiceCallback
import com.truvideo.sdk.camera.ui.capture_button.CaptureButton
import com.truvideo.sdk.camera.ui.continue_button.ContinueButtonPanel
import com.truvideo.sdk.camera.ui.exit_panel.ExitPanel
import com.truvideo.sdk.camera.ui.media_count_indicator.MediaCountIndicator
import com.truvideo.sdk.camera.ui.media_panel.MediaPanel
import com.truvideo.sdk.camera.ui.media_panel_delete.MediaPanelDelete
import com.truvideo.sdk.camera.ui.media_panel_preview.MediaPanelPreview
import com.truvideo.sdk.camera.ui.recording_duration_indicator.RecordingDurationIndicator
import com.truvideo.sdk.camera.ui.recording_indicator.RecordingIndicator
import com.truvideo.sdk.camera.ui.resolution_panel.ResolutionPanel
import com.truvideo.sdk.camera.ui.rotate_button.RotateButton
import com.truvideo.sdk.camera.ui.theme.TruVideoSDKCameraTheme
import com.truvideo.sdk.camera.ui.zoom_indicator.ZoomIndicator
import com.truvideo.sdk.camera.ui.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import com.truvideo.sdk.camera.utils.OrientationLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import truvideo.sdk.components.animated_collapse_visibility.CollapseDirection
import truvideo.sdk.components.animated_collapse_visibility.TruvideoAnimatedCollapseVisibility
import truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity
import truvideo.sdk.components.icon_button.TruvideoIconButton
import java.io.File
import java.util.Date
import kotlin.math.roundToInt

class CameraActivity : ComponentActivity() {

    private lateinit var configuration: TruvideoSdkCameraConfiguration

    private val textureView by lazy { TextureView(this) }

    private val viewModel by viewModels<CameraViewModel>()

    private val manipulateResolutionsUseCase by lazy { ManipulateResolutionsUseCase() }

    private val getCameraInformationUseCase by lazy { GetCameraInformationUseCase(manipulateResolutionsUseCase) }

    private val cameraService by lazy {
        val information = getCameraInformationUseCase(this)
        TruvideoSdkCameraService(
            this, information, configuration, textureView,
            object : TruvideoSdkCameraServiceCallback {

                override fun onRecordingStarted() = viewModel.updateRecordingOrientation(viewModel.orientation.value)

                override fun onPicture(file: File) {
                    val camera = viewModel.camera.value ?: return
                    val resolution = when (camera.lensFacing) {
                        TruvideoSdkCameraLensFacing.BACK -> viewModel.backResolution.value
                        TruvideoSdkCameraLensFacing.FRONT -> viewModel.frontResolution.value
                    } ?: return

                    Log.d("CameraService", "new picture file")
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
                }

                override fun onVideo(file: File, duration: Long) {
                    val camera = viewModel.camera.value ?: return
                    val resolution = when (camera.lensFacing) {
                        TruvideoSdkCameraLensFacing.BACK -> viewModel.backResolution.value
                        TruvideoSdkCameraLensFacing.FRONT -> viewModel.frontResolution.value
                    } ?: return

                    Log.d("CameraService", "new video file")
                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.VIDEO,
                            filePath = file.path,
                            cameraLensFacing = camera.lensFacing,
                            resolution = resolution,
                            rotation = viewModel.recordingOrientation.value,
                            duration = duration,
                        ),
                    )
                }

                override fun onCameraDisconnected() {}

                override fun updateIsBusy(isBusy: Boolean) = viewModel.updateIsBusy(isBusy)

                override fun updateIsRecording(isRecording: Boolean) = viewModel.updateIsRecording(isRecording)

                override fun updateCamera(camera: TruvideoSdkCameraDevice) = viewModel.updateCamera(camera)

                override fun updateFlashMode(cameraLensFacing: TruvideoSdkCameraLensFacing, flashMode: TruvideoSdkCameraFlashMode) {
                    when(cameraLensFacing){
                        TruvideoSdkCameraLensFacing.BACK -> viewModel.updateBackFlashMode(flashMode)
                        TruvideoSdkCameraLensFacing.FRONT -> viewModel.updateFrontFlashMode(flashMode)
                    }
                }

                override fun getSensorRotation() = viewModel.orientation.value

                override fun onFocusRequest() = viewModel.updateFocusState(FocusState.REQUESTED)

                override fun updateZoomVisibility(visible: Boolean) = viewModel.updateIsZoomVisible(visible)

                override fun updateZoom(value: Float) = viewModel.updateZoomValue(value)

                override fun onFocusLocked() = viewModel.updateFocusState(FocusState.LOCKED)
            },
        )
    }

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

    private lateinit var tapToFocusGestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadInitialValues()
        makeFullScreen()
        tapToFocusGestureDetector = GestureDetector(this, cameraService.tapToFocusListener)
        scaleGestureDetector = ScaleGestureDetector(this, cameraService.scaleGestureListener)

        OrientationLiveData(this@CameraActivity.applicationContext).apply {
            observe(this@CameraActivity, viewModel::updateSensorOrientation)
        }

        setContent {
            val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()

            val cameraPermissionResultLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissionsMap ->
                    val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                    if (areGranted) {
                        textureView.surfaceTextureListener = textureListener
                        viewModel.updateIsPermissionGranted(true)
                    } else {
                        finish()
                    }
                }
            )

            LaunchedEffect(true) {
                cameraPermissionResultLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                    )
                )
            }

            TruVideoSDKCameraTheme {
                Box(
                    modifier = Modifier
                        .background(color = Color.Black)
                        .fillMaxSize()
                ) {
                    if (isPermissionGranted) {
                        CameraPreview(
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onResume() {
        super.onResume()

        if (viewModel.isPermissionGranted.value) {
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

        if (viewModel.isPermissionGranted.value) {
            cameraService.disconnect()
        } else {
            Log.d("Camera", "On pause without permissions")
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Composable
    internal fun CameraPreview(viewModel: CameraViewModel) {
        var animatingTakePictureIndicator by remember { mutableStateOf(false) }
        var takingPictureIndicatorVisible by remember { mutableStateOf(false) }

        var panelResolutionVisible by remember { mutableStateOf(false) }
        var panelMediaVisible by remember { mutableStateOf(false) }
        var panelMediaPreviewMedia by remember { mutableStateOf<TruvideoSdkCameraMedia?>(null) }
        var panelMediaPreviewVisible by remember { mutableStateOf(false) }
        var panelMediaDeleteVisible by remember { mutableStateOf(false) }
        var panelExitVisible by remember { mutableStateOf(false) }

        val isRecording by viewModel.isRecording.collectAsState()
        val isBusy by viewModel.isBusy.collectAsState()
        val isPreviewVisible by viewModel.isPreviewVisible.collectAsState()
        val camera: TruvideoSdkCameraDevice? by viewModel.camera.collectAsState()
        val frontFlashMode by viewModel.frontFlashMode.collectAsState()
        val backFlashMode by viewModel.backFlashMode.collectAsState()
        val flashMode = if(camera!=null){
            when(camera!!.lensFacing){
                TruvideoSdkCameraLensFacing.BACK -> backFlashMode
                TruvideoSdkCameraLensFacing.FRONT -> frontFlashMode
            }
        }else{
            TruvideoSdkCameraFlashMode.OFF
        }

        val media by viewModel.media.collectAsState()
        val orientation by viewModel.orientation.collectAsState()
        val recordingOrientation by viewModel.recordingOrientation.collectAsState()
        val backResolutions by viewModel.backResolutions.collectAsState()
        val backResolution by viewModel.backResolution.collectAsState()
        val frontResolutions by viewModel.frontResolutions.collectAsState()
        val frontResolution by viewModel.frontResolution.collectAsState()
        var withFlash = false
        var cameraResolutionList = listOf<TruvideoSdkCameraResolution>()
        val focusState by viewModel.focusState.collectAsState()
        val zoomFactor by viewModel.zoomFactor.collectAsState()
        var zoomIndicatorMode by remember { mutableStateOf(ZoomIndicatorMode.Indicator) }
        var touchPosition by remember { mutableStateOf(IntOffset.Zero) }
        val time by viewModel.recordingTime.collectAsState()

        val fadeInOutAlpha by animateFloatAsState(
            targetValue = when (focusState) {
                FocusState.IDLE -> 0f
                FocusState.REQUESTED -> 0.5f
                FocusState.LOCKED -> 1f
            },
            animationSpec = TweenSpec(durationMillis = 500),
            label = ""
        )

        var resolution: TruvideoSdkCameraResolution? = null
        if (camera != null) {
            withFlash = camera!!.withFlash
            cameraResolutionList = when (camera!!.lensFacing) {
                TruvideoSdkCameraLensFacing.BACK -> backResolutions
                TruvideoSdkCameraLensFacing.FRONT -> frontResolutions
            }
            resolution = when (camera!!.lensFacing) {
                TruvideoSdkCameraLensFacing.BACK -> backResolution
                TruvideoSdkCameraLensFacing.FRONT -> frontResolution
            }
        }

        val uiOrientation: TruvideoSdkCameraOrientation = if (isRecording) {
            recordingOrientation
        } else {
            orientation
        }

        LaunchedEffect(resolution) {
            if (resolution != null) {
                adjustAspectRatio(resolution.width, resolution.height)
            }
        }

        LaunchedEffect(takingPictureIndicatorVisible) {
            if (animatingTakePictureIndicator) return@LaunchedEffect
            if (takingPictureIndicatorVisible) {
                animatingTakePictureIndicator = true
                delay(300)
                takingPictureIndicatorVisible = false
                animatingTakePictureIndicator = false
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black)
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {

                // App bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(16.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    MediaCountIndicator(
                        media = media,
                        orientation = uiOrientation,
                        onPressed = {
                            if (media.isEmpty()) return@MediaCountIndicator
                            if (media.size == 1) {
                                panelMediaPreviewMedia = media[0]
                                panelMediaPreviewVisible = true
                            } else {
                                panelMediaVisible = true
                            }
                        },
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Resolutions
                    TruvideoAnimatedCollapseVisibility(
                        visible = cameraResolutionList.size > 1,
                        direction = CollapseDirection.HORIZONTAL
                    ) {
                        Row {
                            TruvideoIconButton(
                                imageVector = Icons.Default.Hd,
                                enabled = !isBusy && !isRecording,
                                rotation = uiOrientation.uiRotation,
                                onPressed = {
                                    zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                    panelResolutionVisible = true
                                },
                            )
                            Box(modifier = Modifier.width(8.0.dp))
                        }
                    }

                    // Flash button
                    TruvideoAnimatedCollapseVisibility(
                        visible = withFlash,
                        direction = CollapseDirection.HORIZONTAL
                    ) {
                        Row {
                            TruvideoIconButton(
                                imageVector = Icons.Default.FlashOn,
                                enabled = !isBusy,
                                rotation = uiOrientation.uiRotation,
                                onPressed = {
                                    zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                    cameraService.toggleFlash()
                                },
                                selected = flashMode.isOn
                            )
                            Box(modifier = Modifier.width(8.0.dp))
                        }
                    }

                    // Close button
                    TruvideoIconButton(
                        imageVector = Icons.Default.Close,
                        rotation = uiOrientation.uiRotation,
                        enabled = !isRecording && !isBusy,
                        onPressed = {
                            zoomIndicatorMode = ZoomIndicatorMode.Indicator

                            if (media.isEmpty()) {
                                viewModel.updateIsPreviewVisible(false)
                                cameraService.disconnect {
                                    finish()
                                }
                            } else {
                                panelExitVisible = true
                            }
                        },
                    )
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
                                        setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
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
                                    .alpha(fadeInOutAlpha)
                                    .size(80.dp),
                                tint = Color.White
                            )


                            // Recording indicator
                            Box(
                                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp))
                            ) {
                                RecordingIndicator(
                                    recording = isRecording
                                )
                            }


                            // Zoom indicator port
                            TruvideoAnimatedFadeVisibility(
                                visible = !uiOrientation.isPortraitReverse,
                            ) {
                                ZoomIndicator(
                                    zoom = zoomFactor,
                                    orientation = uiOrientation,
                                    mode = zoomIndicatorMode,
                                    onModeChange = { zoomIndicatorMode = it },
                                    onZoomChange = { cameraService.performZoom(it) }
                                )
                            }

                            // Zoom indicator port-reverse
                            TruvideoAnimatedFadeVisibility(
                                visible = uiOrientation.isPortraitReverse,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(180f)
                                ) {
                                    ZoomIndicator(
                                        zoom = zoomFactor,
                                        orientation = TruvideoSdkCameraOrientation.PORTRAIT,
                                        mode = zoomIndicatorMode,
                                        onModeChange = { zoomIndicatorMode = it },
                                        onZoomChange = { cameraService.performZoom(it) }
                                    )
                                }
                            }


                            // Recording time (portrait)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(top = 16.dp)
                                    .align(Alignment.TopCenter),
                            ) {
                                TruvideoAnimatedFadeVisibility(configuration.mode.withVideo && uiOrientation.isPortrait) {
                                    RecordingDurationIndicator(
                                        time = time, recording = isRecording
                                    )
                                }
                            }

                            // Recording time (portrait-reverse)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(bottom = 16.dp)
                                    .align(Alignment.BottomCenter)
                                    .rotate(180f),
                            ) {
                                TruvideoAnimatedFadeVisibility(configuration.mode.withVideo && uiOrientation.isPortraitReverse) {
                                    RecordingDurationIndicator(
                                        time = time, recording = isRecording
                                    )
                                }
                            }

                            // Recording time (land-left)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.CenterEnd)
                                    .offset(x = 16.dp)
                                    .rotate(90f),
                            ) {
                                TruvideoAnimatedFadeVisibility(configuration.mode.withVideo && uiOrientation.isLandscapeLeft) {
                                    RecordingDurationIndicator(
                                        time = time, recording = isRecording
                                    )
                                }
                            }

                            // Recording time (land-right)
                            Box(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .align(Alignment.CenterStart)
                                    .offset(x = (-16).dp)
                                    .rotate(270f),
                            ) {
                                TruvideoAnimatedFadeVisibility(configuration.mode.withVideo && uiOrientation.isLandscapeRight) {
                                    RecordingDurationIndicator(
                                        time = time, recording = isRecording
                                    )
                                }
                            }

                            ContinueButtonPanel(
                                orientation = uiOrientation,
                                onPressed = {
                                    cameraService.disconnect {
                                        val intent = Intent()
                                        val list: List<String> = media.map { it.toJson() }.toList()
                                        intent.putStringArrayListExtra("media", ArrayList(list))
                                        setResult(RESULT_OK, intent)
                                        finish()
                                    }
                                },
                                visible = media.isNotEmpty() && !isRecording,
                                enabled = !isBusy
                            )

                            // Taking Picture indicator
                            TruvideoAnimatedFadeVisibility(takingPictureIndicatorVisible) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(shape = RoundedCornerShape(10.dp))
                                        .background(color = Color.White)
                                )
                            }
                        }
                    }
                }

                // Bottom bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                        .padding(top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))

                    // Take picture
                    TruvideoAnimatedOpacity(opacity = if (configuration.mode.isVideoAndPicture) 1f else 0f) {
                        TruvideoIconButton(
                            imageVector = Icons.Outlined.CameraAlt,
                            size = 50f,
                            enabled = configuration.mode.isVideoAndPicture && !isBusy,
                            rotation = uiOrientation.uiRotation,
                            onPressed = {
                                zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                cameraService.takePicture()
                                takingPictureIndicatorVisible = true
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Recording
                    CaptureButton(
                        recording = isRecording,
                        enabled = !isBusy,
                        onPressed = {
                            zoomIndicatorMode = ZoomIndicatorMode.Indicator

                            when (configuration.mode) {
                                TruvideoSdkCameraMode.VIDEO_AND_PICTURE -> cameraService.toggleRecording()
                                TruvideoSdkCameraMode.VIDEO -> cameraService.toggleRecording()
                                TruvideoSdkCameraMode.PICTURE -> {
                                    cameraService.takePicture()
                                    takingPictureIndicatorVisible = true
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Rotate
                    RotateButton(
                        size = 50f,
                        enabled = !isBusy,
                        rotation = uiOrientation.uiRotation,
                        onPressed = {
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

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Resolution panel
            ResolutionPanel(
                visible = panelResolutionVisible,
                resolutions = cameraResolutionList,
                close = { panelResolutionVisible = false },
                selectedResolution = resolution,
                onBackgroundPressed = { panelResolutionVisible = false },
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
                onPressed = {
                    panelMediaPreviewMedia = it
                    panelMediaPreviewVisible = true
                },
                orientation = uiOrientation,
            )

            // Media panel preview
            MediaPanelPreview(
                visible = panelMediaPreviewVisible && panelMediaPreviewMedia != null,
                media = panelMediaPreviewMedia,
                orientation = uiOrientation,
                close = { panelMediaPreviewVisible = false },
                onDelete = { panelMediaDeleteVisible = true }
            )

            // Media panel delete
            MediaPanelDelete(
                visible = panelMediaPreviewVisible && panelMediaPreviewMedia != null && panelMediaDeleteVisible,
                orientation = uiOrientation,
                close = { panelMediaDeleteVisible = false },
                onCancel = { panelMediaDeleteVisible = false },
                onDelete = { ->
                    val m = panelMediaPreviewMedia ?: return@MediaPanelDelete

                    try {
                        File(m.filePath).delete()
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }

                    val newMediaList = viewModel.media.value.filter { it.filePath != m.filePath }.toList()
                    viewModel.updateMedia(newMediaList)

                    panelMediaPreviewVisible = false
                    panelMediaDeleteVisible = false

                    if (newMediaList.isEmpty()) {
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

    private fun loadInitialValues() {
        configuration = TruvideoSdkCameraConfiguration.fromJson(intent.getStringExtra("configuration") ?: "")

        val info = cameraService.information
        if (!info.withCameras) {
            throw RuntimeException("No cameras available")
        }

        val configuration = cameraService.configuration

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
            TruvideoSdkCameraLensFacing.BACK -> info.backCamera ?: info.frontCamera ?: throw RuntimeException("No camera available")
            TruvideoSdkCameraLensFacing.FRONT -> info.frontCamera ?: info.backCamera ?: throw RuntimeException("No camera available")
        }
        cameraService.lensFacing = camera.lensFacing

        // Resolutions
        if (info.frontCamera != null) {
            if (configuration.frontResolutions.isNotEmpty()) {
                val resolutions = manipulateResolutionsUseCase.filter(info.frontCamera.resolutions, configuration.frontResolutions)
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
                val resolutions = manipulateResolutionsUseCase.filter(info.backCamera.resolutions, configuration.backResolutions)
                viewModel.updateBackResolutions(manipulateResolutionsUseCase.sort(resolutions))
            } else {
                viewModel.updateBackResolutions(manipulateResolutionsUseCase.sort(info.backCamera.resolutions))
            }

            val resolution = calculateResolution(
                items = info.backCamera.resolutions, picked = configuration.backResolutions, preferred = configuration.backResolution
            )
            cameraService.backResolution = resolution
            viewModel.updateBackResolution(resolution)
        }

        // Flash
        cameraService.frontFlashMode = configuration.flashMode
        viewModel.updateFrontFlashMode(configuration.flashMode)

        cameraService.backFlashMode = configuration.flashMode
        viewModel.updateBackFlashMode(configuration.flashMode)
    }

    private fun calculateResolution(
        items: List<TruvideoSdkCameraResolution>, picked: List<TruvideoSdkCameraResolution>, preferred: TruvideoSdkCameraResolution?
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



