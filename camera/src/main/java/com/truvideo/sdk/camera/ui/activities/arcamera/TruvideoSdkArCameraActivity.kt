package com.truvideo.sdk.camera.ui.activities.arcamera

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ar.core.Config
import com.google.ar.sceneform.rendering.CameraStream
import com.google.ar.sceneform.ux.ArFragment
import com.gorisse.thomas.sceneform.light.LightEstimationConfig
import com.gorisse.thomas.sceneform.lightEstimationConfig
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraLogAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraVersionPropertiesAdapterImpl
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.service.ar.TruvideoSdkArCameraService
import com.truvideo.sdk.camera.service.ar.TruvideoSdkArCameraServiceCallback
import com.truvideo.sdk.camera.ui.components.capture_button.CaptureButton
import com.truvideo.sdk.camera.ui.components.exit_panel.ExitPanel
import com.truvideo.sdk.camera.ui.components.media_count_indicator.MediaCountIndicator
import com.truvideo.sdk.camera.ui.components.media_panel.MediaPanel
import com.truvideo.sdk.camera.ui.components.media_preview_panel.MediaPreviewPanel
import com.truvideo.sdk.camera.ui.components.panel_ar_options.ArOptionsPanel
import com.truvideo.sdk.camera.ui.components.pause_button.PauseButton
import com.truvideo.sdk.camera.ui.components.recording_duration_indicator.RecordingDurationIndicator
import com.truvideo.sdk.camera.ui.components.recording_indicator.RecordingIndicator
import com.truvideo.sdk.camera.ui.components.rotated_box.AnimatedFadeRotatedBox
import com.truvideo.sdk.camera.ui.components.toast.ToastContainer
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.ui.theme.TruVideoSdkCameraTheme
import com.truvideo.sdk.camera.usecase.GetCameraInformationUseCase
import com.truvideo.sdk.camera.usecase.ManipulateResolutionsUseCase
import com.truvideo.sdk.camera.utils.OrientationLiveData
import com.truvideo.sdk.components.animated_fade_visibility.TruvideoAnimatedFadeVisibility
import com.truvideo.sdk.components.animated_opacity.TruvideoAnimatedOpacity
import com.truvideo.sdk.components.animated_rotation.TruvideoAnimatedRotation
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

class TruvideoSdkArCameraActivity : FragmentActivity() {

    private lateinit var configuration: TruvideoSdkCameraConfiguration

    private val textureView by lazy { TextureView(this) }

    private val viewModel by viewModels<TruvideoSdkArCameraViewModel>()

    private lateinit var versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapterImpl

    private lateinit var logAdapter: TruvideoSdkCameraLogAdapter

    private lateinit var authAdapter: TruvideoSdkCameraAuthAdapter

    private lateinit var manipulateResolutionsUseCase: ManipulateResolutionsUseCase

    private lateinit var getCameraInformationUseCase: GetCameraInformationUseCase

    private lateinit var fragmentManager: FragmentManager

    private lateinit var cameraService: TruvideoSdkArCameraService

    private fun makeFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val view: View = window.decorView
        WindowInsetsControllerCompat(window, view).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    val context = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
        makeFullScreen()

        fragmentManager = this.supportFragmentManager
        OrientationLiveData(this@TruvideoSdkArCameraActivity.applicationContext).apply {
            observe(this@TruvideoSdkArCameraActivity, viewModel::updateSensorOrientation)
        }

        fun showFatalErrorDialog(
            title: String,
            message: String
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

        setContent {
            // Report authentication error
            LaunchedEffect(isAuthenticated) {
                if (!isAuthenticated) {
                    showFatalErrorDialog(
                        title = "Error",
                        message = "Authentication required"
                    )
                }
            }

            // Validate permissions
            val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
            val cameraPermissionResultLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissionsMap ->
                    val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
                    if (areGranted) {
                        viewModel.updateIsPermissionGranted(true)
                    } else {
                        showFatalErrorDialog(
                            title = "Error",
                            message = "Camera and microphone permission required"
                        )
                    }
                }
            )

            if (isAuthenticated) {
                LaunchedEffect(true) {
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
                        val arFragment = remember { ArFragment() }
                        val arCamera = remember {
                            ARCamera(
                                context = applicationContext,
                                arFragment = arFragment
                            )
                        }

                        arFragment.apply {
                            setOnSessionConfigurationListener { session, config ->
                                session.resume()
                                session.pause()
                                session.resume()

                                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                                config.focusMode = Config.FocusMode.AUTO
                                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                    config.depthMode = Config.DepthMode.RAW_DEPTH_ONLY
                                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                                }

                                arSceneView.scene.addOnUpdateListener(arCamera)
                            }
                            setOnViewCreatedListener { arSceneView ->
                                // Available modes: DEPTH_OCCLUSION_DISABLED, DEPTH_OCCLUSION_ENABLED
                                arSceneView.setMaxFramesPerSeconds(240)
                                arSceneView.lightEstimationConfig = LightEstimationConfig.DISABLED
                                instructionsController?.isEnabled = true
                                arSceneView.planeRenderer.isVisible = false
                                arSceneView.cameraStream.depthOcclusionMode = CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_DISABLED
                                cameraService.setUpSceneView(arSceneView)
                            }
                        }

                        CameraContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(0.dp)),
                            fragmentManager = supportFragmentManager,
                            arCamera = arCamera,
                            commit = { add(it, arFragment) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun CameraContent(
        modifier: Modifier = Modifier,
        arCamera: ARCamera,
        fragmentManager: FragmentManager?,
        commit: FragmentTransaction.(containerId: Int) -> Unit,
        viewModel: TruvideoSdkArCameraViewModel
    ) {
        var animatingTakePictureIndicator by remember { mutableStateOf(false) }
        var takingPictureIndicatorVisible by remember { mutableStateOf(false) }

        var panelMediaVisible by remember { mutableStateOf(false) }
        var panelMediaPreviewMedia by remember { mutableStateOf<TruvideoSdkCameraMedia?>(null) }
        var panelMediaPreviewVisible by remember { mutableStateOf(false) }
        var panelMediaPreviewIndex by remember { mutableIntStateOf(0) }
        var panelExitVisible by remember { mutableStateOf(false) }
        var panelAROptionsVisible by remember { mutableStateOf(false) }

        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
        val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
        val camera: TruvideoSdkCameraDevice? by viewModel.camera.collectAsStateWithLifecycle()
        val media by viewModel.media.collectAsStateWithLifecycle()
        val orientation by viewModel.orientation.collectAsStateWithLifecycle()
        val screenFlipped = remember(orientation) { orientation.isPortraitReverse }
        val isPortrait by viewModel.isPortrait.collectAsStateWithLifecycle()
        val backResolution by viewModel.backResolution.collectAsStateWithLifecycle()
        val frontResolution by viewModel.frontResolution.collectAsStateWithLifecycle()
        var zoomIndicatorMode by remember { mutableStateOf(ZoomIndicatorMode.Indicator) }
        val arMode by viewModel.arMode.collectAsStateWithLifecycle()
        val arMeasureUnit by viewModel.arMeasureUnit.collectAsStateWithLifecycle()
        val arModeImage by viewModel.arModeImage.collectAsStateWithLifecycle()
        var resolution: TruvideoSdkCameraResolution? = null
        if (camera != null) {
            resolution = when (camera!!.lensFacing) {
                TruvideoSdkCameraLensFacing.BACK -> backResolution
                TruvideoSdkCameraLensFacing.FRONT -> frontResolution
            }
        }
        val withMedia = remember(media) { media.isNotEmpty() }
        val continueButtonVisible = withMedia && !isRecording
        val uiOrientation: TruvideoSdkCameraOrientation = orientation

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

        val localView = LocalView.current
        val containerId = rememberSaveable {
            mutableIntStateOf(View.generateViewId())
        }
        val container = remember {
            mutableStateOf<FragmentContainerView?>(null)
        }
        val viewBlock: (Context) -> View = remember(localView) {
            { context ->
                FragmentContainerView(context)
                    .apply {
                        id = containerId.intValue
                    }
                    .also {
                        fragmentManager?.commit { commit(it.id) }
                        container.value = it
                    }
            }
        }

        val localContext = LocalContext.current
        DisposableEffect(localView, localContext, container) {
            onDispose {
                val existingFragment = fragmentManager?.findFragmentById(container.value?.id ?: 0)
                if (existingFragment != null &&
                    !fragmentManager.isStateSaved
                ) {
                    fragmentManager.commit {
                        detach(existingFragment)
                        remove(existingFragment)
                    }
                }
            }
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
            takingPictureIndicatorVisible = true
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

        fun onButtonPauseRecordingPressed() {
            if (isPaused) {
                logAdapter.addLog(
                    eventName = "event_camera_recording_resume",
                    message = "Resume recording",
                    severity = TruvideoSdkLogSeverity.INFO
                )
                cameraService.resumeRecording()
            } else {
                logAdapter.addLog(
                    eventName = "event_camera_recording_pause",
                    message = "Pause recording",
                    severity = TruvideoSdkLogSeverity.INFO
                )
                cameraService.pauseRecording()
            }

            viewModel.updateIsPaused(!isPaused)
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
                // Top App bar composable
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
                                        enabled = !isBusy && !isRecording,
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
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // AR Options
                        TruvideoAnimatedRotation(
                            rotation = currentOrientation.uiRotation,
                        ) {
                            TruvideoIconButton(
                                icon = arModeImage,
                                small = true,
                                enabled = !isBusy,
                                onPressed = {
                                    panelAROptionsVisible = true
                                },
                            )
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
                                        onPressed = {
                                            cameraService.disconnect {
                                                val intent = Intent()
                                                val list: List<String> = media.map { it.toJson() }.toList()
                                                intent.putStringArrayListExtra("media", ArrayList(list))
                                                setResult(RESULT_OK, intent)
                                                finish()
                                            }
                                        }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.height(30.dp))
                            }
                        }
                    }

                }

                // Bottom bar composable
                @Composable
                fun bottomBarContent(flipped: Boolean = false) {
                    val currentOrientation = if (flipped) TruvideoSdkCameraOrientation.PORTRAIT else orientation

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))

                        // Take picture button
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

                        // Recording button
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

                        // Play-Pause Button
                        AnimatedContent(
                            targetState = isRecording,
                            label = ""
                        ) { isRecordingTarget ->
                            if (isRecordingTarget) {
                                TruvideoAnimatedRotation(
                                    rotation = currentOrientation.uiRotation,
                                ) {
                                    PauseButton(
                                        isPaused = isPaused,
                                        size = 50f,
                                        enabled = !isBusy,
                                        rotation = currentOrientation.uiRotation,
                                        onPressed = {
                                            onButtonPauseRecordingPressed()
                                        },
                                    )
                                }
                            }
                            else {
                                Spacer(modifier = Modifier.width(50.dp))
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

                //Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(shape = RoundedCornerShape(10.dp))
                        .background(color = Color.Gray)
                        .fillMaxWidth()
                ) {

                    AndroidView(
                        modifier = modifier,
                        factory = viewBlock,
                        update = {},
                    )

                    // Recording indicator
                    Box(
                        modifier = Modifier.clip(shape = RoundedCornerShape(10.dp))
                    ) {
                        RecordingIndicator(
                            isRecording = isRecording
                        )
                    }

                    // Media count flipped and continue button
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
                                        onPressed = {
                                            cameraService.disconnect {
                                                val intent = Intent()
                                                val list: List<String> = media.map { it.toJson() }.toList()
                                                intent.putStringArrayListExtra("media", ArrayList(list))
                                                setResult(RESULT_OK, intent)
                                                finish()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Undo AR Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) {
                        val currentOrientation = orientation
                        if (arMode != ARModeState.RECORD) {

                            // Undo Single Object
                            TruvideoAnimatedRotation(
                                rotation = currentOrientation.uiRotation,
                            ) {
                                TruvideoIconButton(
                                    icon = Icons.AutoMirrored.Outlined.Undo,
                                    onPressed = { arCamera.undo() },
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Undo All AR items
                            TruvideoAnimatedRotation(
                                rotation = currentOrientation.uiRotation,
                            ) {
                                TruvideoIconButton(
                                    icon = Icons.Default.DeleteOutline,
                                    onPressed = { arCamera.undoAll() },
                                )
                            }

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
                                        cameraDuration.toDuration(
                                            DurationUnit.MILLISECONDS
                                        )
                                    }
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
        }


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

        // AR Options Panel
        ArOptionsPanel(
            visible = panelAROptionsVisible,
            orientation = orientation,
            mode = arMode,
            onModePressed = {
                viewModel.updateARMode(it)
                arCamera.updateMode(it)
            },
            measure = arMeasureUnit,
            onMeasureUnitPressed = {
                viewModel.updateARMeasure(it)
                arCamera.updateMeasure(it)
            },
            close = { panelAROptionsVisible = false }
        )
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

    private fun setup() {
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

        manipulateResolutionsUseCase = ManipulateResolutionsUseCase()

        getCameraInformationUseCase = GetCameraInformationUseCase(
            context = applicationContext,
            manipulateResolutionsUseCase = manipulateResolutionsUseCase,
        )
        val information = getCameraInformationUseCase()

        configuration = TruvideoSdkCameraConfiguration.fromJson(intent.getStringExtra("configuration") ?: "")
        cameraService = TruvideoSdkArCameraService(
            context = this,
            information = information,
            serviceCallback = object : TruvideoSdkArCameraServiceCallback {
                override fun onRecordingStarted() {

                }

                override fun onVideo(file: File, duration: Long, maxVideoDurationReached: Boolean) {
                    Log.d("CameraService", "new video file. ${viewModel.recordingOrientation.value}")

                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            id = UUID.randomUUID().toString(),
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.VIDEO,
                            filePath = file.path,
                            cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
                            resolution = TruvideoSdkCameraResolution(
                                width = cameraService.videoSize.width,
                                height = cameraService.videoSize.height
                            ),
                            rotation = viewModel.recordingOrientation.value,
                            duration = duration,
                        ),
                    )

                    if (maxVideoDurationReached) {
                        viewModel.showToast("Maximum video duration reached")
                    }

                    viewModel.updateIsRecording(false)

                    if (configuration.mode.autoClose) {
                        cameraService.disconnect {
                            finish()
                        }
                    }
                }

                override fun onPicture(file: File) {
                    Log.d("CameraService", "new picture file")
                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            id = UUID.randomUUID().toString(),
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.PICTURE,
                            filePath = file.path,
                            cameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
                            resolution = TruvideoSdkCameraResolution(1920, 1080),
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

                override fun onCameraDisconnected() {}

                override fun updateIsBusy(isBusy: Boolean) = viewModel.updateIsBusy(isBusy)

                override fun updateIsPaused(isPaused: Boolean) {
                    viewModel.updateIsPaused(isPaused)
                }

                override fun updateIsRecording(isRecording: Boolean) = viewModel.updateIsRecording(isRecording)

                override fun getSensorRotation() = viewModel.orientation.value
            }
        )

        // Duration limit
        cameraService.maxDuration = configuration.mode.videoDurationLimit

        // Output path
        val outputPath = if (configuration.outputPath.trim().isEmpty()) {
            "${filesDir.path}/truvideo-sdk/camera"
        } else {
            configuration.outputPath
        }
        cameraService.outputPath = outputPath

        // Fixed orientation
        viewModel.updateFixedOrientation(configuration.orientation)
        viewModel.updateIsBusy(false)
    }
}

