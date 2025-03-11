package com.truvideo.sdk.camera.ui.activities.arcamera

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.ControlCamera
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ar.core.Config
import com.google.ar.sceneform.ux.ArFragment
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAuthAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraLogAdapterImpl
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraVersionPropertiesAdapterImpl
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkArCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
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
import com.truvideo.sdk.camera.usecase.ArCoreUseCase
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
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TruvideoSdkArCameraActivity : FragmentActivity() {

    private lateinit var configuration: TruvideoSdkArCameraConfiguration

    private lateinit var versionPropertiesAdapter: TruvideoSdkCameraVersionPropertiesAdapterImpl

    private lateinit var logAdapter: TruvideoSdkCameraLogAdapter

    private lateinit var authAdapter: TruvideoSdkCameraAuthAdapter

    private lateinit var manipulateResolutionsUseCase: ManipulateResolutionsUseCase

    private lateinit var getCameraInformationUseCase: GetCameraInformationUseCase

    private lateinit var arCoreUseCase: ArCoreUseCase

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
        makeFullScreen()
        setup()

        fun showFatalErrorDialog(
            title: String,
            message: String
        ) {
            AlertDialog.Builder(this).setTitle(title).setMessage(message).setOnDismissListener {
                finish()
            }.setPositiveButton("Accept") { _, _ ->
            }.show()
        }

        fun installArCore() {
            try {
                arCoreUseCase.requestInstall(this@TruvideoSdkArCameraActivity)
            } catch (exception: Exception) {
                Log.d("TruvideoSdkCamera", "Error installing arcore", exception)
                showFatalErrorDialog(
                    title = "Error",
                    message = "Error installing augmented reality services"
                )
            }
        }

        val isAuthenticated = try {
            authAdapter.validateAuthentication()
            true
        } catch (exception: Exception) {
            false
        }

        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            val viewModel: TruvideoSdkArCameraViewModel = viewModel(
                factory = TruvideoSdkArCameraViewModelFactory(
                    arCoreUseCase = arCoreUseCase,
                )
            )

            val cameraService = remember {
                createCameraService(
                    viewModel = viewModel
                )
            }

            LaunchedEffect(viewModel) {
                viewModel.updateFixedOrientation(configuration.orientation)
                viewModel.updateIsBusy(false)
            }

            LaunchedEffect(Unit) {
                OrientationLiveData(context).apply {
                    observe(lifecycleOwner, viewModel::updateSensorOrientation)
                }
            }
            val isPermissionGranted by viewModel.isPermissionGranted.collectAsStateWithLifecycle()
            val isArCoreSupported by viewModel.isAugmentedRealitySupported.collectAsStateWithLifecycle()
            val isArCoreInstalled by viewModel.isAugmentedRealityInstalled.collectAsStateWithLifecycle()
            var isFirstOnResume by rememberSaveable { mutableStateOf(true) }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (isFirstOnResume) {
                            isFirstOnResume = false
                        } else {
                            Log.d("TruvideoSdkCamera", "OnResume")
                            viewModel.refreshAugmentedReality()
                            if (viewModel.isAugmentedRealitySupported.value && !viewModel.isAugmentedRealityInstalled.value) {
                                showFatalErrorDialog(
                                    title = "Error",
                                    message = "Error installing augmented reality services"
                                )
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Validate permissions
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

            // Ask permission
            if (isAuthenticated && isArCoreSupported && isArCoreInstalled) {
                LaunchedEffect(true) {
                    val permissions = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                    cameraPermissionResultLauncher.launch(permissions.toTypedArray())
                }
            }

            // Show fatal error
            LaunchedEffect(Unit) {
                if (!isAuthenticated) {
                    showFatalErrorDialog(
                        title = "Error",
                        message = "Authentication required"
                    )
                } else {
                    if (!isArCoreSupported) {
                        showFatalErrorDialog(
                            title = "Error",
                            message = "Augmented reality not supported"
                        )
                    } else {
                        if (!isArCoreInstalled) {
                            installArCore()
                        }
                    }
                }
            }


            TruVideoSdkCameraTheme {
                Box(
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxSize()
                ) {
                    if (isAuthenticated && isPermissionGranted && isArCoreSupported && isArCoreInstalled) {
                        val arFragment = remember { ArFragment() }
                        val arCamera = remember {
                            ARCamera(
                                context = applicationContext,
                                arFragment = arFragment
                            )
                        }



                        DisposableEffect (arFragment) {
                            val fragmentLifecycleOwner = arFragment.viewLifecycleOwnerLiveData
                            val lifecycleObserver = LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_START -> {
                                        arFragment.apply {
                                            setOnSessionConfigurationListener { session, config ->
                                                config.lightEstimationMode = Config.LightEstimationMode.DISABLED
                                                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                                                config.focusMode = Config.FocusMode.AUTO

                                                if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                                                    Log.d("TruvideoSdkCamera", "Depth mode supported")
                                                    config.depthMode = Config.DepthMode.AUTOMATIC
                                                } else {
                                                    config.depthMode = Config.DepthMode.DISABLED
                                                }

                                                arSceneView.scene.addOnUpdateListener(arCamera)
                                            }

                                            setOnViewCreatedListener { arSceneView ->
                                                Log.d("TruvideoSdkCamera", "Scene view created")
                                                instructionsController?.isEnabled = true
                                                arSceneView.planeRenderer.isVisible = false
                                                cameraService.setUpSceneView(arSceneView)
                                            }
                                        }
                                    }
                                    else -> Unit
                                }
                            }

                            fragmentLifecycleOwner.observeForever { viewLifecycleOwner ->
                                viewLifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
                            }
                            onDispose {
                                arFragment.viewLifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                            }
                        }

                        CameraContent(
                            viewModel = viewModel,
                            cameraService = cameraService,
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                            fragmentManager = supportFragmentManager,
                            arCamera = arCamera,
                            commit = { add(it, arFragment) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    internal fun CameraContent(
        viewModel: TruvideoSdkArCameraViewModel,
        cameraService: TruvideoSdkArCameraService,
        modifier: Modifier = Modifier,
        arCamera: ARCamera,
        fragmentManager: FragmentManager?,
        commit: FragmentTransaction.(containerId: Int) -> Unit,
    ) {
        var animatingTakeImageIndicator by remember { mutableStateOf(false) }
        var takingImageIndicatorVisible by remember { mutableStateOf(false) }

        var panelMediaVisible by remember { mutableStateOf(false) }
        var panelMediaPreviewMedia by remember { mutableStateOf<TruvideoSdkCameraMedia?>(null) }
        var panelMediaPreviewVisible by remember { mutableStateOf(false) }
        var panelMediaPreviewIndex by remember { mutableIntStateOf(0) }
        var panelExitVisible by remember { mutableStateOf(false) }
        var panelAROptionsVisible by remember { mutableStateOf(false) }

        val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
        val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
        val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
        val media by viewModel.media.collectAsStateWithLifecycle()
        val orientation by viewModel.orientation.collectAsStateWithLifecycle()
        val screenFlipped = remember(orientation) { orientation.isPortraitReverse }
        val isPortrait by viewModel.isPortrait.collectAsStateWithLifecycle()
        var zoomIndicatorMode by remember { mutableStateOf(ZoomIndicatorMode.Indicator) }
        val arMode by viewModel.arMode.collectAsStateWithLifecycle()
        val arMeasureUnit by viewModel.arMeasureUnit.collectAsStateWithLifecycle()
        val withMedia = remember(media) { media.isNotEmpty() }
        val continueButtonVisible = withMedia && !isRecording
        val uiOrientation: TruvideoSdkCameraOrientation = orientation

        LaunchedEffect(takingImageIndicatorVisible) {
            if (animatingTakeImageIndicator) return@LaunchedEffect
            if (takingImageIndicatorVisible) {
                animatingTakeImageIndicator = true
                delay(300)
                takingImageIndicatorVisible = false
                animatingTakeImageIndicator = false
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

        fun takeImage() {
            val mediaCount = media.size
            val imageCount = media.filter { it.type == TruvideoSdkCameraMediaType.IMAGE }.size
            val mediaLimit = configuration.mode.mediaLimit
            val imageLimit = configuration.mode.imageLimit

            if (mediaLimit != null && mediaCount >= mediaLimit) {
                viewModel.showToast("You have reached the maximum number of images for this session")
                return
            }

            if (imageLimit != null && imageCount >= imageLimit) {
                viewModel.showToast("You have reached the maximum number of images for this session")
                return
            }

            zoomIndicatorMode = ZoomIndicatorMode.Indicator
            cameraService.takeImage()
            takingImageIndicatorVisible = true
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

                                    val imageCount = remember(media) {
                                        media.filter { it.type == TruvideoSdkCameraMediaType.IMAGE }.size
                                    }

                                    MediaCountIndicator(
                                        videoCount = videoCount,
                                        imageCount = imageCount,
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
                            val icon = remember(arMode) {
                                when (arMode) {
                                    ARModeState.OBJECT -> Icons.Outlined.ViewInAr
                                    ARModeState.RULER -> Icons.Outlined.ControlCamera
                                    ARModeState.RECORD -> Icons.Outlined.Videocam
                                }
                            }

                            TruvideoIconButton(
                                icon = icon,
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

                        // Take image button
                        val imageButtonVisible = configuration.mode.canTakeVideo && configuration.mode.canTakeImage
                        TruvideoAnimatedOpacity(
                            opacity = if (imageButtonVisible) 1f else 0f
                        ) {
                            TruvideoAnimatedRotation(
                                rotation = currentOrientation.uiRotation,
                            ) {
                                TruvideoIconButton(
                                    icon = Icons.Outlined.CameraAlt,
                                    size = 50f,
                                    enabled = imageButtonVisible && !isBusy,
                                    onPressed = { takeImage() },
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
                                } else if (configuration.mode.canTakeImage) {
                                    takeImage()
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
                            } else {
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
                                imageCount = media.filter { it.type == TruvideoSdkCameraMediaType.IMAGE }.size,
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
                    deleteAll(viewModel)
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

    private fun deleteAll(viewModel: TruvideoSdkArCameraViewModel) {
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
        arCoreUseCase = ArCoreUseCase(
            context = applicationContext
        )

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

        configuration = TruvideoSdkArCameraConfiguration.fromJson(intent.getStringExtra("configuration") ?: "")
    }

    private fun createCameraService(
        viewModel: TruvideoSdkArCameraViewModel,
    ): TruvideoSdkArCameraService {
        val information: TruvideoSdkCameraInformation = getCameraInformationUseCase()
        var cameraService: TruvideoSdkArCameraService? = null
        cameraService = TruvideoSdkArCameraService(
            context = this,
            information = information,
            serviceCallback = object : TruvideoSdkArCameraServiceCallback {
                override fun onRecordingStarted() {

                }

                override fun onVideo(
                    file: File,
                    duration: Long,
                    orientation: TruvideoSdkCameraOrientation,
                    resolution: TruvideoSdkCameraResolution,
                    maxVideoDurationReached: Boolean
                ) {
                    Log.d("CameraService", "new video file. ${viewModel.recordingOrientation.value}")

                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            id = UUID.randomUUID().toString(),
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.VIDEO,
                            filePath = file.path,
                            lensFacing = TruvideoSdkCameraLensFacing.BACK,
                            resolution = resolution,
                            orientation = orientation,
                            duration = duration,
                        ),
                    )

                    if (maxVideoDurationReached) {
                        viewModel.showToast("Maximum video duration reached")
                    }

                    viewModel.updateIsRecording(false)

                    if (configuration.mode.autoClose) {
                        cameraService?.disconnect {
                            finish()
                        }
                    }
                }

                override fun onImage(
                    file: File,
                    orientation: TruvideoSdkCameraOrientation,
                    resolution: TruvideoSdkCameraResolution
                ) {
                    Log.d("CameraService", "new image file")
                    viewModel.addMedia(
                        TruvideoSdkCameraMedia(
                            id = UUID.randomUUID().toString(),
                            createdAt = Date().time,
                            type = TruvideoSdkCameraMediaType.IMAGE,
                            filePath = file.path,
                            lensFacing = TruvideoSdkCameraLensFacing.BACK,
                            resolution = resolution,
                            orientation = orientation,
                            duration = 0L,
                        ),
                    )
                    if (configuration.mode.autoClose) {
                        cameraService?.disconnect {
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

        cameraService.maxDuration = configuration.mode.videoDurationLimit

        // Output path
        val outputPath = if (configuration.outputPath.trim().isEmpty()) {
            "${filesDir.path}/truvideo-sdk/camera"
        } else {
            configuration.outputPath
        }
        cameraService.outputPath = outputPath


        return cameraService
    }
}

