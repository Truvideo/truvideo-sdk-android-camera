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
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraLogAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraVersionPropertiesAdapterImpl
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEventType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventCameraFlipped
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventFlashModeChanged
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventMediaContinue
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventMediaDeleted
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventMediaDiscard
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventPictureTaken
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingFinished
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingPaused
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingResumed
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingStarted
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventResolutionChanged
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventZoomChanged
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
import com.truvideo.sdk.camera.ui.components.rotated_box.AnimatedFadeRotatedBox
import com.truvideo.sdk.camera.ui.components.toast.ToastContainer
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicator
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import com.truvideo.sdk.camera.utils.EventConstants
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
import truvideo.sdk.common.model.TruvideoSdkLogSeverity
import java.io.File
import java.util.Date
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class TruvideoSdkCameraActivity : ComponentActivity() {

    private lateinit var configuration: TruvideoSdkCameraConfiguration

    private lateinit var cameraService: TruvideoSdkCameraService

    private lateinit var getCameraInformationUseCase: GetCameraInformationUseCase

    private lateinit var manipulateResolutionsUseCase: ManipulateResolutionsUseCase

    private lateinit var logAdapter: TruvideoSdkCameraLogAdapter

    private lateinit var authAdapter: TruvideoSdkCameraAuthAdapter

    private lateinit var versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapterImpl

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
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
            title: String, message: String
        ) {
            AlertDialog.Builder(this).setTitle(title).setMessage(message).setOnDismissListener {
                finish()
            }.setPositiveButton("Accept") { _, _ ->
            }.show()
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
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
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
            logAdapter.addLog(
                "event_camera_take_picture",
                "Take picture",
                TruvideoSdkLogSeverity.INFO,
            )

            val mediaCount = media.size
            val pictureCount = media.filter { it.type == TruvideoSdkCameraMediaType.PICTURE }.size
            val mediaLimit = configuration.mode.mediaLimit
            val pictureLimit = configuration.mode.pictureLimit

            if (mediaLimit != null && mediaCount >= mediaLimit) {
                logAdapter.addLog(
                    "event_camera_take_picture",
                    "Media limit reached",
                    TruvideoSdkLogSeverity.INFO,
                )

                viewModel.showToast("You have reached the maximum number of pictures for this session")
                return
            }

            if (pictureLimit != null && pictureCount >= pictureLimit) {
                logAdapter.addLog(
                    "event_camera_take_picture",
                    "Picture limit reached",
                    TruvideoSdkLogSeverity.INFO,
                )


                viewModel.showToast("You have reached the maximum number of pictures for this session")
                return
            }

            zoomIndicatorMode = ZoomIndicatorMode.Indicator
            cameraService.takePicture()
        }

        fun toggleRecording() {
            if (isRecording) {
                logAdapter.addLog(
                    "event_camera_recording_stop",
                    "Stop recording",
                    TruvideoSdkLogSeverity.INFO,
                )
                cameraService.toggleRecording()
            } else {
                logAdapter.addLog(
                    "event_camera_recording_start",
                    "Start recording",
                    TruvideoSdkLogSeverity.INFO,
                )

                val mediaCount = media.size
                val videoCount = media.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }.size
                val mediaLimit = configuration.mode.mediaLimit
                val videoLimit = configuration.mode.videoLimit

                if (mediaLimit != null && mediaCount >= mediaLimit) {
                    logAdapter.addLog(
                        "event_camera_recording_start",
                        "Media limit reached",
                        TruvideoSdkLogSeverity.INFO,
                    )
                    viewModel.showToast("You have reached the maximum number of videos for this session")
                    return
                }

                if (videoLimit != null && videoCount >= videoLimit) {
                    logAdapter.addLog(
                        "event_camera_recording_start",
                        "Video limit reached",
                        TruvideoSdkLogSeverity.INFO,
                    )
                    viewModel.showToast("You have reached the maximum number of videos for this session")
                    return
                }

                cameraService.toggleRecording()
            }
        }

        fun onMediaCountIndicatorPressed() {
            if (media.isEmpty()) return

            if (media.size == 1) {
                logAdapter.addLog(
                    eventName = "event_camera_panel_media_detail_open",
                    message = "Open panel media detail. Media: ${media.first().toJson()}",
                    severity = TruvideoSdkLogSeverity.INFO,
                )

                panelMediaPreviewIndex = 0
                panelMediaPreviewVisible = true
            } else {
                logAdapter.addLog(
                    eventName = "event_camera_panel_media_open",
                    message = "Open panel media",
                    severity = TruvideoSdkLogSeverity.INFO,
                )
                panelMediaVisible = true
            }
        }

        fun processContinue() {
            val list: List<String> = media.map { it.toJson() }.toList()

            logAdapter.addLog(
                eventName = "event_camera_button_continue_pressed",
                message = "Button continue pressed. Result: ${list.joinToString(", ")}",
                severity = TruvideoSdkLogSeverity.INFO,
            )

            // Report event
            sendEvent(
                TruvideoSdkCameraEvent(
                    type = TruvideoSdkCameraEventType.Continue,
                    data = TruvideoSdkCameraEventMediaContinue(media.toList()),
                    createdAtMillis = Date().time
                )
            )

            cameraService.disconnect {
                reportResult()
                finish()
            }
        }

        fun onClosePressed() {
            logAdapter.addLog(
                eventName = "event_camera_button_close_pressed",
                message = "Button close pressed",
                severity = TruvideoSdkLogSeverity.INFO,
            )

            zoomIndicatorMode = ZoomIndicatorMode.Indicator

            if (media.isEmpty()) {
                viewModel.updateIsPreviewVisible(false)
                cameraService.disconnect {
                    finish()
                }
            } else {
                logAdapter.addLog(
                    eventName = "event_camera_panel_discard_open",
                    message = "Open panel discard",
                    severity = TruvideoSdkLogSeverity.INFO,
                )
                panelExitVisible = true
            }
        }

        fun onButtonResolutionPressed() {
            logAdapter.addLog(
                eventName = "event_camera_panel_resolution_open",
                message = "Open resolution panel",
                severity = TruvideoSdkLogSeverity.INFO,
            )
            zoomIndicatorMode = ZoomIndicatorMode.Indicator
            panelResolutionVisible = true
        }

        fun onButtonFlashPressed() {
            logAdapter.addLog(
                eventName = "event_camera_flash",
                message = "Toggle flash. Current: ${flashMode.name}",
                severity = TruvideoSdkLogSeverity.INFO
            )
            zoomIndicatorMode = ZoomIndicatorMode.Indicator
            cameraService.toggleFlash()
        }

        fun onButtonPauseRecordingPressed() {
            if (isPaused) {
                logAdapter.addLog(
                    eventName = "event_camera_recording_resume",
                    message = "Resume recording",
                    severity = TruvideoSdkLogSeverity.INFO
                )
                cameraService.resumeRecording()

                // Report event
                sendEvent(
                    TruvideoSdkCameraEvent(
                        type = TruvideoSdkCameraEventType.RecordingResumed,
                        data = TruvideoSdkCameraEventRecordingResumed(
                            resolution = viewModel.resolution.value ?: TruvideoSdkCameraResolution(
                                width = 0,
                                height = 0
                            ),
                            lensFacing = viewModel.camera.value?.lensFacing ?: TruvideoSdkCameraLensFacing.BACK,
                            orientation = viewModel.orientation.value
                        ),
                        createdAtMillis = Date().time
                    )
                )
            } else {
                logAdapter.addLog(
                    eventName = "event_camera_recording_pause",
                    message = "Pause recording",
                    severity = TruvideoSdkLogSeverity.INFO
                )
                cameraService.pauseRecording()

                // Report event
                sendEvent(
                    TruvideoSdkCameraEvent(
                        type = TruvideoSdkCameraEventType.RecordingPaused,
                        data = TruvideoSdkCameraEventRecordingPaused(
                            resolution = viewModel.resolution.value ?: TruvideoSdkCameraResolution(
                                width = 0,
                                height = 0
                            ),
                            lensFacing = viewModel.camera.value?.lensFacing ?: TruvideoSdkCameraLensFacing.BACK,
                            orientation = viewModel.orientation.value
                        ),
                        createdAtMillis = Date().time
                    )
                )
            }

            viewModel.updateIsPaused(!isPaused)
        }

        fun onButtonFlipCameraPressed() {
            logAdapter.addLog(
                eventName = "event_camera_flip",
                message = "Flip camera. Current: ${camera?.lensFacing?.name}",
                severity = TruvideoSdkLogSeverity.INFO,
            )

            if (viewModel.isBusy.value) return
            val c = camera ?: return

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

                // Report event
                sendEvent(
                    TruvideoSdkCameraEvent(
                        type = TruvideoSdkCameraEventType.CameraFlipped,
                        data = TruvideoSdkCameraEventCameraFlipped(newLensFacing),
                        createdAtMillis = Date().time
                    )
                )
            }
        }

        BackHandler(true) {
            logAdapter.addLog(
                eventName = "event_camera_button_back_pressed",
                message = "Button back pressed",
                severity = TruvideoSdkLogSeverity.INFO,
            )

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
                    val currentOrientation =
                        if (flipped) TruvideoSdkCameraOrientation.PORTRAIT else orientation

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
                            targetState = isPortrait, label = ""
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
                            targetState = resolutions.size > 1, label = ""
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
                                            onPressed = { onButtonResolutionPressed() },
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }

                        // Flash button
                        AnimatedContent(
                            targetState = withFlash, label = ""
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
                                            onPressed = { onButtonFlashPressed() },
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
                            targetState = portraitContinueButtonVisible, label = ""
                        ) { portraitContinueButtonVisibleTarget ->
                            if (portraitContinueButtonVisibleTarget) {
                                Row {
                                    Box(modifier = Modifier.width(4.dp))
                                    TruvideoContinueButton(small = true,
                                        enabled = !isBusy,
                                        onPressed = { processContinue() })
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }
                    }
                }

                @Composable
                fun bottomBarContent(flipped: Boolean = false) {
                    val currentOrientation =
                        if (flipped) TruvideoSdkCameraOrientation.PORTRAIT else orientation

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        // Take picture
                        val mode = configuration.mode
                        val pictureButtonVisible = mode.canTakeVideo && mode.canTakePicture
                        val isRemainingCountEnough = if (mode.mediaLimit == null) true else (mode.mediaLimit - media.size) > 1
                        val isPictureButtonEnabledWhileRecording = (!mode.isSingleMediaMode && isRemainingCountEnough)

                        TruvideoAnimatedOpacity(
                            opacity = if (pictureButtonVisible) 1f else 0f
                        ) {
                            TruvideoAnimatedRotation(
                                rotation = currentOrientation.uiRotation,
                            ) {
                                TruvideoIconButton(
                                    icon = Icons.Outlined.CameraAlt,
                                    size = 50f,
                                    enabled = (!isBusy && !isRecording) || (isRecording && isPictureButtonEnabledWhileRecording),
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

                        AnimatedContent(
                            targetState = isRecording,
                            label = ""
                        ) { isRecordingTarget ->
                            // Pause or play
                            if (isRecordingTarget) {
                                PauseButton(
                                    isPaused = isPaused,
                                    size = 50f,
                                    enabled = !isBusy,
                                    rotation = currentOrientation.uiRotation,
                                    onPressed = { onButtonPauseRecordingPressed() },
                                )
                            } else {
                                RotateButton(
                                    size = 50f,
                                    enabled = !isBusy,
                                    rotation = currentOrientation.uiRotation,
                                    onPressed = { onButtonFlipCameraPressed() },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Normal app bar
                AnimatedContent(
                    targetState = screenFlipped,
                    label = ""
                ) { screenFlippedTarget ->
                    if (!screenFlippedTarget) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            appBarContent(flipped = false)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }

                // Flipped bottom bar
                AnimatedContent(
                    targetState = screenFlipped,
                    label = ""
                ) { screenFlippedTarget ->
                    if (screenFlippedTarget) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationZ = 180f
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }) {
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
                            Icon(painter = painterResource(id = R.drawable.focus_camera),
                                contentDescription = "",
                                modifier = Modifier
                                    .offset { touchPosition }
                                    .alpha(focusStateAlphaAnim)
                                    .size(80.dp),
                                tint = Color.White)


                            // Recording indicator
                            Box(
                                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp))
                            ) {
                                RecordingIndicator(
                                    isRecording = isRecording, isPaused = isPaused
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
                                            val time = remember(cameraDuration) {
                                                cameraDuration.toDuration(DurationUnit.MILLISECONDS)
                                            }
                                            val durationLimit = remember {
                                                cameraService.maxDuration?.toDuration(DurationUnit.MILLISECONDS)
                                            }

                                            val remainingTime =
                                                if (durationLimit != null && isRecording && !isBusy) {
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
                                    ToastContainer(text = toastText,
                                        visible = toastVisible,
                                        onPressed = { viewModel.hideToast() })
                                }
                            }

                            AnimatedFadeRotatedBox(
                                orientation = orientation, orientations = persistentMapOf(
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
                AnimatedContent(
                    targetState = screenFlipped,
                    label = ""
                ) { screenFlippedTarget ->
                    if (!screenFlippedTarget) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            bottomBarContent(flipped = false)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }

                // Flipped app bar
                AnimatedContent(
                    targetState = screenFlipped,
                    label = ""
                ) { screenFlippedTarget ->
                    if (screenFlippedTarget) {
                        Box(modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationZ = 180f
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }) {
                            appBarContent(flipped = true)
                        }
                    } else {
                        Box(Modifier.fillMaxWidth())
                    }
                }
            }

            // Resolution panel
            fun changeResolution(resolution: TruvideoSdkCameraResolution) {
                logAdapter.addLog(
                    eventName = "event_camera_resolution_change",
                    message = "Change resolution. ${resolution.width}x${resolution.height}",
                    severity = TruvideoSdkLogSeverity.INFO,
                )
                panelResolutionVisible = false

                // Report event
                sendEvent(
                    TruvideoSdkCameraEvent(
                        type = TruvideoSdkCameraEventType.ResolutionChanged,
                        data = TruvideoSdkCameraEventResolutionChanged(resolution),
                        createdAtMillis = Date().time
                    )
                )

                val c = camera ?: return

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

            ResolutionPanel(
                visible = panelResolutionVisible,
                resolutions = resolutions,
                close = { panelResolutionVisible = false },
                selectedResolution = resolution,
                orientation = orientation,
                onResolutionPicked = { changeResolution(it) }
            )

            // Media panel
            fun openMediaDetail(item: TruvideoSdkCameraMedia) {
                val index = media.indexOfFirst { it.filePath == item.filePath }
                if (index == -1) return

                val m = media[index]
                logAdapter.addLog(
                    eventName = "event_camera_panel_media_detail_open",
                    message = "Open panel media detail. Media: ${m.toJson()}",
                    severity = TruvideoSdkLogSeverity.INFO,
                )

                panelMediaPreviewIndex = index
                panelMediaPreviewVisible = true
            }

            MediaPanel(
                visible = panelMediaVisible,
                media = media,
                close = { panelMediaVisible = false },
                onPressed = { openMediaDetail(it) },
                orientation = orientation,
            )

            // Media panel delete
            fun deleteMedia(media: TruvideoSdkCameraMedia) {
                logAdapter.addLog(
                    eventName = "event_camera_media_delete",
                    message = "Delete media: ${media.toJson()}",
                    severity = TruvideoSdkLogSeverity.INFO,
                )
                viewModel.removeMedia(media.filePath)
                if (viewModel.media.value.isEmpty()) {
                    panelMediaPreviewVisible = false
                    panelMediaVisible = false
                }

                // Report event
                sendEvent(
                    TruvideoSdkCameraEvent(
                        type = TruvideoSdkCameraEventType.MediaDeleted,
                        data = TruvideoSdkCameraEventMediaDeleted(media),
                        createdAtMillis = Date().time
                    )
                )
            }

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
                onDelete = { deleteMedia(it) }
            )

            // Exit panel
            fun discard() {
                logAdapter.addLog(
                    eventName = "event_camera_discard",
                    message = "Discard all media",
                    severity = TruvideoSdkLogSeverity.INFO,
                )

                // Report event
                sendEvent(
                    TruvideoSdkCameraEvent(
                        type = TruvideoSdkCameraEventType.MediaDiscard,
                        data = TruvideoSdkCameraEventMediaDiscard(media = viewModel.media.value.toList()),
                        createdAtMillis = Date().time
                    )
                )

                cameraService.disconnect {
                    deleteAll()
                    finish()
                }
            }

            ExitPanel(
                visible = panelExitVisible,
                enabled = !isBusy,
                orientation = orientation,
                onDiscardPressed = { discard() },
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
        versionPropertiesAdapter = TruvideoSdkCameraVersionPropertiesAdapterImpl(
            context = applicationContext
        )
        logAdapter = TruvideoSdkCameraLogAdapterImpl(
            versionPropertiesAdapter = versionPropertiesAdapter
        )
        authAdapter = TruvideoSdkCameraAuthAdapterImpl(
            versionPropertiesAdapter = versionPropertiesAdapter,
            logAdapter = logAdapter
        )
        viewModel = TruvideoSdkCameraActivityViewModel()
        manipulateResolutionsUseCase = ManipulateResolutionsUseCase()
        getCameraInformationUseCase = GetCameraInformationUseCase(
            context = applicationContext,
            manipulateResolutionsUseCase = manipulateResolutionsUseCase,
        )
        textureView = TextureView(this)


        logAdapter.addLog(
            eventName = "event_camera_open",
            message = "The camera screen has opened. Configuration: ${configuration.toJson()}",
            severity = TruvideoSdkLogSeverity.INFO
        )

        val information = getCameraInformationUseCase()
        cameraService = TruvideoSdkCameraService(
            context = this,
            information = information,
            textureView = textureView,
            serviceCallback = object : TruvideoSdkCameraServiceCallback {
                override fun onRecordingStarted() {
                    viewModel.updateIsRecording(true)

                    // Report event
                    sendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.RecordingStarted,
                            data = TruvideoSdkCameraEventRecordingStarted(
                                lensFacing = viewModel.camera.value?.lensFacing ?: TruvideoSdkCameraLensFacing.BACK,
                                orientation = viewModel.orientation.value,
                                resolution = viewModel.resolution.value ?: TruvideoSdkCameraResolution(
                                    width = 0,
                                    height = 0
                                )
                            ),
                            createdAtMillis = Date().time
                        )
                    )
                }

                override fun onTakePictureStarted() {
                    viewModel.updateTakingPicture(true)
                }

                override fun onPicture(file: File) {
                    viewModel.updateTakingPicture(false)

                    val camera = viewModel.camera.value ?: return
                    val resolution = viewModel.resolution.value ?: return

                    val media = TruvideoSdkCameraMedia(
                        id = UUID.randomUUID().toString(),
                        createdAt = Date().time,
                        type = TruvideoSdkCameraMediaType.PICTURE,
                        filePath = file.path,
                        cameraLensFacing = camera.lensFacing,
                        resolution = resolution,
                        rotation = viewModel.orientation.value,
                        duration = 0L,
                    )
                    viewModel.addMedia(media)

                    logAdapter.addLog(
                        eventName = "event_camera_media",
                        message = "New media: ${media.toJson()}",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

                    // Report event
                    sendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.PictureTaken,
                            data = TruvideoSdkCameraEventPictureTaken(media),
                            createdAtMillis = Date().time
                        )
                    )

                    if (configuration.mode.autoClose) {
                        cameraService.disconnect {
                            reportResult()
                            finish()
                        }
                    }
                }

                override fun onVideo(file: File, duration: Long, maxDurationReached: Boolean) {
                    val camera = viewModel.camera.value ?: return
                    val resolution = viewModel.resolution.value ?: return

                    val media = TruvideoSdkCameraMedia(
                        id = UUID.randomUUID().toString(),
                        createdAt = Date().time,
                        type = TruvideoSdkCameraMediaType.VIDEO,
                        filePath = file.path,
                        cameraLensFacing = camera.lensFacing,
                        resolution = resolution,
                        rotation = viewModel.orientation.value,
                        duration = duration,
                    )

                    viewModel.addMedia(media)

                    if (maxDurationReached) {
                        logAdapter.addLog(
                            eventName = "event_camera_recording_duration_limit",
                            message = "Video duration limit reached. ${configuration.mode.videoDurationLimit}",
                            severity = TruvideoSdkLogSeverity.INFO
                        )

                        viewModel.showToast("Maximum video duration reached")
                    }

                    logAdapter.addLog(
                        eventName = "event_camera_media",
                        message = "New media: ${media.toJson()}",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

                    viewModel.updateIsRecording(false)

                    // Report event
                    sendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.RecordingFinished,
                            data = TruvideoSdkCameraEventRecordingFinished(media),
                            createdAtMillis = Date().time
                        )
                    )

                    if (configuration.mode.autoClose) {
                        cameraService.disconnect {
                            reportResult()
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
                    logAdapter.addLog(
                        eventName = "event_camera_device_info",
                        message = "Camera: ${camera.toJson()}",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

                    viewModel.updateCamera(camera)
                }

                override fun updateFlashMode(
                    cameraLensFacing: TruvideoSdkCameraLensFacing,
                    flashMode: TruvideoSdkCameraFlashMode
                ) {
                    logAdapter.addLog(
                        eventName = "event_camera_flash",
                        message = "New flash mode: ${flashMode.name}",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

                    when (cameraLensFacing) {
                        TruvideoSdkCameraLensFacing.BACK -> viewModel.updateBackFlashMode(flashMode)
                        TruvideoSdkCameraLensFacing.FRONT -> viewModel.updateFrontFlashMode(
                            flashMode
                        )
                    }

                    // Report event
                    sendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.FlashModeChanged,
                            data = TruvideoSdkCameraEventFlashModeChanged(flashMode),
                            createdAtMillis = Date().time
                        )
                    )
                }

                override fun getSensorRotation(): TruvideoSdkCameraOrientation {
                    return viewModel.orientation.value
                }

                override fun onFocusRequest() {
                    logAdapter.addLog(
                        eventName = "event_camera_focus",
                        message = "Focus requested",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

                    viewModel.updateFocusState(FocusState.REQUESTED)
                }

                override fun updateZoomVisibility(visible: Boolean) {
                    viewModel.updateIsZoomVisible(visible)
                }

                override fun updateZoom(value: Float) {
                    logAdapter.addLog(
                        eventName = "event_camera_zoom",
                        message = "New value: $value",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

                    viewModel.updateZoomValue(value)

                    // Report event
                    sendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.ZoomChanged,
                            data = TruvideoSdkCameraEventZoomChanged(value),
                            createdAtMillis = Date().time
                        )
                    )
                }

                override fun onFocusLocked() {
                    logAdapter.addLog(
                        eventName = "event_camera_focus",
                        message = "Focus locked",
                        severity = TruvideoSdkLogSeverity.INFO
                    )

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

        if (preferred != null && manipulateResolutionsUseCase.contain(
                validResolutions, preferred
            )
        ) {
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

    private fun reportResult() {
        val intent = Intent()
        val media = viewModel.media.value
        val list: List<String> = media.map { it.toJson() }.toList()
        intent.putStringArrayListExtra("media", ArrayList(list))
        setResult(RESULT_OK, intent)
    }

    companion object {
        const val FOCUS_INDICATOR_SIZE = 80
    }

    private fun sendEvent(data: TruvideoSdkCameraEvent) {
        val intent = Intent(EventConstants.ACTION_NAME)
        intent.putExtra(EventConstants.EVENT_DATA, data.toJson())
        sendBroadcast(intent)
    }
}



