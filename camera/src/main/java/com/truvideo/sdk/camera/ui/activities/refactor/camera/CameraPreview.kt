package com.truvideo.sdk.camera.ui.activities.refactor.camera

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.truvideo.sdk.camera.R
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent.UI.OnContinueButtonPressed
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.images
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isIdle
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isPaused
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.videos
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel
import com.truvideo.sdk.camera.ui.components.media_count_indicator.MediaCountIndicator
import com.truvideo.sdk.camera.ui.components.media_panel.MediaPanel
import com.truvideo.sdk.camera.ui.components.media_preview_panel.MediaPreviewPanel
import com.truvideo.sdk.camera.ui.components.recording_duration_indicator.RecordingDurationIndicator
import com.truvideo.sdk.camera.ui.components.recording_indicator.RecordingIndicator
import com.truvideo.sdk.camera.ui.components.resolution_panel.ResolutionPanel
import com.truvideo.sdk.camera.ui.components.rotated_box.AnimatedFadeRotatedBox
import com.truvideo.sdk.camera.ui.components.toast.ToastContainer
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicator
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.utils.OrientationLiveData
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_value.animateFloat
import com.truvideo.sdk.components.button.TruvideoContinueButton
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@SuppressLint("FlowOperatorInvokedInComposition")
@Composable
internal fun CameraPreview(
    viewModel: CameraPreviewViewModel
) {
    val tapToFocusGestureDetector = GestureDetector(
        LocalContext.current,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(event: MotionEvent): Boolean {
                viewModel.onEvent(CameraUiEvent.Controls.OnTapToFocus(event.x, event.y))
                return true
            }
        }
    )

    val scaleGestureDetector = ScaleGestureDetector(
        LocalContext.current,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                viewModel.onEvent(CameraUiEvent.Controls.OnZoomLevelScaled(scaleFactor))
                return true
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        OrientationLiveData(context).apply {
            observe(lifecycleOwner){ orientation ->
                viewModel.onEvent(CameraUiEvent.UI.OnOrientationChanged(orientation))
            }
        }
    }

    var toastMessage: String by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CameraUiEffect.ShowToastMessage -> {
                    toastMessage = effect.msg
                }
                CameraUiEffect.DismissToast -> {
                    toastMessage = ""
                }
                else -> Unit
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.Black)
            .pointerInput(Unit) {
                detectTapGestures {
                    // Tapped outside
                    viewModel.onEvent(CameraUiEvent.UI.OnZoomIndicatorModeChange(mode = ZoomIndicatorMode.Indicator))
                }
            }
    ) {
        val aspectRatio by viewModel.resolutionAspectRatio.collectAsState()
        val resolution by viewModel.currentResolution.collectAsState()
        val resolutions by viewModel.resolutions.collectAsState()
        val orientation by viewModel.orientation.collectAsState()
        val zoomLevel by viewModel.zoomLevel.collectAsState()
        val zoomIndicatorMode by viewModel.zoomIndicatorMode.collectAsState()
        val captureState by viewModel.captureState.collectAsState()
        val isRecording = !captureState.recordingConfig.recordingState.isIdle()
        val isPaused = captureState.recordingConfig.recordingState.isPaused()
        val previewConfig by viewModel.previewConfig.collectAsState()
        val isBusy = remember(previewConfig) { previewConfig.isBusy }
        val time by viewModel.recordingTime.collectAsState()
        val maxTime by viewModel.maxRecordingTime.collectAsState()
        val cameraMode by viewModel.cameraMode.collectAsState()
        val panelsControlState by viewModel.panelsControlState.collectAsState()
        val mediaState by viewModel.mediaState.collectAsState()
        val controlsState by viewModel.controlsState.collectAsState()
        val videoCount = remember(mediaState.media) { mediaState.media.videos.size }
        val imageCount = remember(mediaState.media) { mediaState.media.images.size }
        val isMediaCounterButtonEnabled = controlsState.isMediaCounterButtonEnabled

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            CameraScreen(
                viewModel
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
//                TruvideoAnimatedOpacity(opacity = if (isPreviewVisible) 1.0f else 0.0f) {
                    Box(Modifier.fillMaxSize()) {
                        // Camera Preview

                        Box(
                            modifier =
                                Modifier
                                    .aspectRatio(1 / aspectRatio!!)
                                    .align(Alignment.Center)
                        ) {
                            val focusIndicatorState by viewModel.focusIndicatorState.collectAsState()
                            val focusStateAlphaAnim = animateFloat(
                                value = when (focusIndicatorState.focusState) {
                                    FocusState.Failed -> 0.0f
                                    FocusState.FocusedLocked -> 1.0f
                                    FocusState.Idle -> 0.0f
                                    FocusState.Started -> 0.5f
                                }
                            ).coerceIn(0.0f, 1.0f)

                            AndroidView(
                                factory = { context ->
                                    TextureView(context).apply {
                                        surfaceTextureListener =
                                            object : TextureView.SurfaceTextureListener {
                                                override fun onSurfaceTextureAvailable(
                                                    surfaceTexture: SurfaceTexture,
                                                    width: Int,
                                                    height: Int
                                                ) {

                                                    val resolution = resolution ?: return
                                                    surfaceTexture.setDefaultBufferSize(resolution.width, resolution.height)
                                                    val surface = Surface(surfaceTexture)
                                                    viewModel.onEvent(CameraUiEvent.Controls.StartPreview(surface, width, height))
                                                }

                                                override fun onSurfaceTextureSizeChanged(
                                                    surface: SurfaceTexture,
                                                    width: Int,
                                                    height: Int
                                                ) = Unit

                                                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
                                                    true

                                                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
                                                    Unit
                                            }
                                        setOnTouchListener(fun(
                                            _: View, event: MotionEvent
                                        ): Boolean {
                                            viewModel.onEvent(CameraUiEvent.UI.OnZoomIndicatorModeChange(mode = ZoomIndicatorMode.Indicator))
                                            tapToFocusGestureDetector.onTouchEvent(event)
                                            scaleGestureDetector.onTouchEvent(event)
                                            return true
                                        })
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(shape = RoundedCornerShape(10.dp))
                                    .pointerInput(Unit) { detectTapGestures {} },
                            )

                            // Focus indicator
                            Icon(
                                painter = painterResource(id = R.drawable.focus_camera),
                                contentDescription = "",
                                modifier = Modifier
                                    .offset { focusIndicatorState.position }
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
                                ZoomIndicator(
                                    zoom = zoomLevel,
                                    orientation = orientation,
                                    mode = zoomIndicatorMode,
                                    onModeChange = {
                                        viewModel.onEvent(CameraUiEvent.UI.OnZoomIndicatorModeChange(it))
                                    },
                                    onZoomChange = {
                                        viewModel.onEvent(CameraUiEvent.Controls.OnZoomLevelChange(it))
                                    })
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
                                    ZoomIndicator(
                                        zoom = zoomLevel,
                                        orientation = orientation,
                                        mode = zoomIndicatorMode,
                                        onModeChange = { viewModel.onEvent(CameraUiEvent.UI.OnZoomIndicatorModeChange(it)) },
                                        onZoomChange = { viewModel.onEvent(CameraUiEvent.Controls.OnZoomLevelChange(it)) })
                                }
                            }

                            AnimatedFadeRotatedBox(orientation = orientation) {
                                // Recording duration
                                TruvideoAnimatedFadeVisibility(cameraMode.canTakeVideo) {

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .wrapContentSize()
                                                .padding(8.dp)
                                                .align(Alignment.TopCenter)
                                        ) {
                                             val time = remember(time) {
                                                time.toDuration(DurationUnit.MILLISECONDS)
                                            }
                                            val durationLimit = remember(maxTime) {
                                                maxTime?.toDuration(DurationUnit.MILLISECONDS)
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
                                    val toastVisible = toastMessage.isNotEmpty()
                                    ToastContainer(
                                        text = toastMessage,
                                        visible = toastVisible,
                                        onPressed = {
                                            toastMessage = ""
                                        })
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
                                        videoCount = videoCount,
                                        imageCount = imageCount,
                                        mode = cameraMode,
                                        enabled = isMediaCounterButtonEnabled,
                                        onPressed = {
                                            viewModel.onEvent(CameraUiEvent.UI.OnMediaCounterButtonPressed)
                                        },
                                    )
                                }

                                // Continue button
                                TruvideoAnimatedFadeVisibility(
                                    visible = controlsState.isContinueButtonVisible,
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            TruvideoContinueButton(
                                                enabled = controlsState.isContinueButtonEnabled,
                                                small = true,
                                                onPressed = {
                                                    viewModel.onEvent(OnContinueButtonPressed)
                                                }
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

        MediaPanel(
            visible = panelsControlState.isMediaPanelVisible,
            media = mediaState.media.toImmutableList(),
            close = {
                viewModel.onEvent(CameraUiEvent.UI.OnMediaCounterCloseButtonPressed)
            },
            onPressed = {
                viewModel.onEvent(CameraUiEvent.UI.OnMediaDetailPressed(it))
            },
            orientation = orientation,
        )

        ResolutionPanel(
            visible = panelsControlState.isResolutionsPanelVisible,
            resolutions = resolutions,
            close = { viewModel.onEvent(CameraUiEvent.UI.OnResolutionsPanelCloseButtonPressed)},
            selectedResolution = resolution,
            orientation = orientation,
            onResolutionPicked = {
                viewModel.onEvent(CameraUiEvent.UI.OnCurrentResolutionChanged(it))
            }
        )

        MediaPreviewPanel(
            visible = panelsControlState.isMediaPanelDetailVisible,
            media = mediaState.media.toImmutableList(),
            orientation = orientation,
            initialIndex = panelsControlState.mediaDetailState.index,
            close = {
                viewModel.onEvent(CameraUiEvent.UI.OnMediaDetailDismiss)
            },
            onDelete = { viewModel.onEvent(CameraUiEvent.Media.OnDeleteMediaButtonPressed(it)) }
        )
    }
}

@Composable
internal fun CameraScreen(
    viewModel: CameraPreviewViewModel,
    cameraPreview: @Composable ColumnScope.() -> Unit
) {

    val orientation by viewModel.orientation.collectAsState()
    val screenFlipped = remember(orientation) { orientation.isPortraitReverse }

    Column(modifier = Modifier.fillMaxSize()) {

        // normal app bar
        AnimatedContent(
            targetState = screenFlipped,
            label = ""
        ) { screenFlippedTarget ->
            if (!screenFlippedTarget) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AppBarView(viewModel)
                }
            } else {
                Box(Modifier.fillMaxWidth())
            }
        }

        // flipped bottom bar
        AnimatedContent(
            targetState = screenFlipped,
            label = ""
        ) { screenFlippedTarget ->
            if (screenFlippedTarget) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BottomBarView(viewModel)
                }
            } else {
                Box(Modifier.fillMaxWidth())
            }
        }


        cameraPreview()

        // flipped app bar
        AnimatedContent(
            targetState = screenFlipped,
            label = ""
        ) { screenFlippedTarget ->
            if (screenFlippedTarget) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AppBarView(viewModel)
                }
            } else {
                Box(Modifier.fillMaxWidth())
            }
        }

        // regular bottom bar
        AnimatedContent(
            targetState = screenFlipped,
            label = ""
        ) { screenFlippedTarget ->
            if (!screenFlippedTarget) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BottomBarView(viewModel)
                }
            } else {
                Box(Modifier.fillMaxWidth())
            }
        }
    }
}