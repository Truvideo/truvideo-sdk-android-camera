package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers

import android.media.Image
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.adapters.ImageCaptureEvent
import com.truvideo.sdk.camera.adapters.TruvideoSdkCameraAdapter
import com.truvideo.sdk.camera.adapters.TruvideoSdkFileManager
import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.AutoWhiteBalanceMode
import com.truvideo.sdk.camera.domain.models.CaptureTemplate
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.RecordingConfig
import com.truvideo.sdk.camera.domain.models.enabled
import com.truvideo.sdk.camera.domain.usecases.ConcatVideoListUseCase
import com.truvideo.sdk.camera.domain.usecases.GetMediaFileUseCase
import com.truvideo.sdk.camera.domain.usecases.GetMediaRotationUseCase
import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.RestartPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.ResumeRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.StartRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.StopRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.TakeImageUseCase
import com.truvideo.sdk.camera.domain.usecases.TakeVideoSnapshotUseCase
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.RecordingSetUp
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEventType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.TruvideoSdkDirectory
import com.truvideo.sdk.camera.model.createImageMediaFromFile
import com.truvideo.sdk.camera.model.createVideoMediaFromFile
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventImageTaken
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventMediaDiscard
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingFinished
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingPaused
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingResumed
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventRecordingStarted
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CaptureState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.RecordingState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ReducedResult
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.currentCameraDevice
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.deriveControls
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isIdle
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isPaused
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.orientation
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.toPausedState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.toRecordingState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.toVideoStopFailureState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.toVideoStopSuccessfulState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel.Companion.TAG
import com.truvideo.sdk.camera.utils.PausableTimer
import com.truvideo.sdk.camera.utils.createFile
import com.truvideo.sdk.camera.utils.getVideoDuration
import com.truvideo.sdk.camera.utils.save
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import truvideo.sdk.common.model.TruvideoSdkLogSeverity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.plus

internal class CameraMediaReducer(
    private val fileManager: TruvideoSdkFileManager,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val resumeRecordingUseCase: ResumeRecordingUseCase,
    private val pauseRecordingUseCase: PauseRecordingUseCase,
    private val concatVideoListUseCase: ConcatVideoListUseCase,
    private val restartPreviewUseCase: RestartPreviewUseCase,
    private val takeVideoSnapshotUseCase: TakeVideoSnapshotUseCase,

    // new
    private val getMediaRotationUseCase: GetMediaRotationUseCase,
    private val getMediaFileUseCase: GetMediaFileUseCase,
    private val takeImageUseCase: TakeImageUseCase,
    private val logAdapter: TruvideoSdkCameraLogAdapter

) {

    fun reduceMediaEvents(
        event: CameraUiEvent.Media,
        state: CameraUiState,
        recordingTimer: PausableTimer,
    ): ReducedResult<CameraUiState, CameraUiEffect> =
        when(event) {
            is CameraUiEvent.Media.OnCapturedButtonPressed -> handleCapturedButtonPressed(state, recordingTimer)
            is CameraUiEvent.Media.OnDeleteMediaButtonPressed -> handleDeleteMediaButtonPressed(state, media = event.media)
            CameraUiEvent.Media.OnPauseButtonPressed -> handlePauseVideoRecording(state, recordingTimer)
            CameraUiEvent.Media.OnTakeImageButtonPressed -> handleTakeImageButtonPressed(state)
            CameraUiEvent.Media.OnDiscardAllMedia -> handleDiscardAllMedia(state)
        }

    private fun handleDiscardAllMedia(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        return ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(
                    isDiscardPanelVisible = false
                )
            ), onFlowUpdates = { _, effects ->
                logAdapter.addLog(
                    eventName = "event_camera_discard",
                    message = "Discard all media",
                    severity = TruvideoSdkLogSeverity.INFO,
                )

                // Report event
                effects.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.MEDIA_DISCARDED,
                            data = TruvideoSdkCameraEventMediaDiscard(media = state.mediaState.media.toList()),
                            createdAtMillis = Date().time
                        )
                    )
                )
                effects.tryEmit(CameraUiEffect.ClosePreview)
            }
        )
    }

    // Start - Stop logic
    private fun handleCapturedButtonPressed(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> =
        if (state.cameraConfiguration.cameraMode.canTakeVideo) takeVideo(state, timer) // take video
        else takePreviewImageWithFocus(state) // take image

    private fun takeVideo(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> {

        val mediaState = state.mediaState
        val captureState = state.captureState

        val cameraMode = state.cameraConfiguration.cameraMode
        val previewConfig = captureState.previewConfig
        val screenOrientation = state.orientation()
        val screenRotation = screenOrientation.rotation
        val durationLimit = cameraMode.videoDurationLimit
        val resolution = previewConfig.currentResolution ?: return ReducedResult(state)

        if (
            mediaState.maxVideoCountReached
            || mediaState.maxMediaCountReached
        ) {

            if (mediaState.maxMediaCountReached) {

                logAdapter.addLog(
                    "event_camera_recording_start",
                    "Media limit reached",
                    TruvideoSdkLogSeverity.INFO,
                )
            }

            if (mediaState.maxVideoCountReached) {
                logAdapter.addLog(
                    "event_camera_recording_start",
                    "Video limit reached",
                    TruvideoSdkLogSeverity.INFO,
                )
            }

            // do not take video
            return ReducedResult(state) { _, effects ->
                effects.tryEmit(CameraUiEffect.ShowToastMessage("You have reached the maximum number of videos for this session"))
                delay(5000)
                effects.tryEmit(CameraUiEffect.DismissToast)
            }
        }


        if (!captureState.recordingConfig.recordingState.isIdle()) {
            // Video is Paused or Stopped

            logAdapter.addLog(
                "event_camera_recording_start",
                "Start recording",
                TruvideoSdkLogSeverity.INFO,
            )

            if (captureState.recordingConfig.recordingState.isPaused()){
                // already stopped - just handle video merging and confirmation
                return stopVideo(state)
            }

            // stop media recording
            stopRecordingUseCase()
            timer.stop()
            return ReducedResult(state)
        }

        // create temp file
        val tempFile = getMediaFileUseCase(
            directory = TruvideoSdkDirectory.Files(
                fileManager.filesDirectory,
                temp = true
            ),
            extension = "mp4"
        ) ?: run {
            Log.d(TAG, "takeVideo: temp file is null")
            return ReducedResult(state)
        }

        // calculate video rotation
        val mediaRotationAngle = getMediaRotationUseCase(
            sensorRotation = state.currentCameraDevice()?.sensorOrientation ?: 0,
            isFront = previewConfig.currentLensFacing.isFront,
            orientation = screenRotation
        )

        // set up class for creating new video
        val recordingSetUp = RecordingSetUp(
            videoPath = tempFile.path,
            lensFacing = previewConfig.currentLensFacing,
            orientation = screenOrientation,
            rotationAngle = mediaRotationAngle,
            resolution = resolution,
            maxDuration = durationLimit,
        )

        // modified ui state
        val newCaptureState = captureState.toRecordingState(recordingSetUp)

        logAdapter.addLog(
            "event_camera_recording_start",
            "Start recording",
            TruvideoSdkLogSeverity.INFO,
        )
        
        return ReducedResult(
            state.copy(
                captureState = newCaptureState,
                orientationState = state.orientationState.copy(fixedOrientation = screenOrientation)
            ).deriveControls(),
            onFlowUpdates = { _, effects ->
                // go ahead and create video
                startRecordingUseCase(
                    captureConfig = newCaptureState.captureConfig,
                    recordingSetUp = recordingSetUp,
                )

                timer.start()

                effects.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.RECORDING_STARTED,
                            data = TruvideoSdkCameraEventRecordingStarted(
                                lensFacing = recordingSetUp.lensFacing,
                                orientation = recordingSetUp.orientation,
                                resolution = recordingSetUp.resolution
                            ),
                            createdAtMillis = Date().time
                        )
                    )
                )
            }
        )
    }

    private fun takePreviewImageWithFocus(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaState = state.mediaState
        val captureConfig = state.captureState.captureConfig

        if (mediaState.maxImageCountReached
            || mediaState.maxMediaCountReached) {
            if (mediaState.maxMediaCountReached) {

                logAdapter.addLog(
                    "event_camera_take_image",
                    "Media limit reached",
                    TruvideoSdkLogSeverity.INFO,
                )
            }

            if (mediaState.maxImageCountReached) {
                logAdapter.addLog(
                    "event_camera_take_image",
                    "Image limit reached",
                    TruvideoSdkLogSeverity.INFO,
                )
            }

            ReducedResult(state) { _, effects ->
                effects.tryEmit(CameraUiEffect.ShowToastMessage("You have reached the maximum number of images for this session"))
                delay(5000)
                effects.tryEmit(CameraUiEffect.DismissToast)
            }
        }

        return ReducedResult(
            state,
            onFlowUpdates = { uiStateMutable, effects ->
                logAdapter.addLog(
                    "event_camera_take_image",
                    "Take image",
                    TruvideoSdkLogSeverity.INFO,
                )
                takeImageUseCase(captureConfig)
                    .collectLatest { event ->
                        when(event) {
                            ImageCaptureEvent.CaptureFailed -> Unit
                            ImageCaptureEvent.CaptureFocusFailed -> Unit
                            ImageCaptureEvent.CaptureFocusLocked -> Unit
                            ImageCaptureEvent.CaptureFocusStarted -> Unit
                            ImageCaptureEvent.CaptureStarted -> Unit
                            is ImageCaptureEvent.Captured -> {
                                logAdapter.addLog(
                                    "event_camera_capture_image",
                                    "Capture image",
                                    TruvideoSdkLogSeverity.INFO,
                                )
                                val reducedResult = saveImage(state, event.image)

                                uiStateMutable.update { reducedResult.newState }

                                reducedResult.onFlowUpdates?.invoke(uiStateMutable, effects)
                            }
                            is ImageCaptureEvent.Exception -> {
                                event.throwable.printStackTrace()
                            }

                            ImageCaptureEvent.CaptureFocusIdle -> Unit
                        }
                    }

            }
        )
    }
    // ==========================================

    // Pause - Resume Logic
    private fun resumeRecording(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        // resume video
        val captureState = state.captureState
        val cameraMode = state.cameraConfiguration.cameraMode
        val previewConfig = captureState.previewConfig
        val screenOrientation = state.orientation()
        val screenRotation = screenOrientation.rotation
        val durationLimit = cameraMode.videoDurationLimit
        val resolution = previewConfig.currentResolution ?: return ReducedResult(state)

        // create temp file
        val tempFile = getMediaFileUseCase(
            directory = TruvideoSdkDirectory.Files(
                fileManager.filesDirectory,
                temp = true,
            ),
            extension = "mp4"
        ) ?: run {
            Log.d(TAG, "takeVideo: temp file is null")
            return ReducedResult(state)
        }

        // calculate video rotation
        val mediaRotationAngle = getMediaRotationUseCase(
            sensorRotation = state.currentCameraDevice()?.sensorOrientation ?: 0,
            isFront = previewConfig.currentLensFacing.isFront,
            orientation = screenRotation
        )

        // set up class for creating new video
        val recordingSetUp = RecordingSetUp(
            videoPath = tempFile.path,
            lensFacing = previewConfig.currentLensFacing,
            orientation = screenOrientation,
            rotationAngle = mediaRotationAngle,
            resolution = resolution,
            maxDuration = durationLimit,
        )

        // modified ui state
        val newCaptureState = captureState.toRecordingState(recordingSetUp)

        // go ahead and create video
        resumeRecordingUseCase(
            captureConfig = newCaptureState.captureConfig,
            recordingSetUp = recordingSetUp,
        )

        return ReducedResult(
            state.copy(
                captureState = newCaptureState,
                orientationState = state.orientationState.copy(fixedOrientation = screenOrientation)
            ).deriveControls(),
            onFlowUpdates = { _, effects ->
                effects.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.RECORDING_RESUMED,
                            data = TruvideoSdkCameraEventRecordingResumed(
                                resolution = recordingSetUp.resolution,
                                lensFacing = recordingSetUp.lensFacing,
                                orientation = recordingSetUp.orientation
                            ),
                            createdAtMillis = Date().time
                        )
                    )
                )

            }
        )
    }

    private fun handlePauseVideoRecording(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> {
        val captureState = state.captureState
        val recordingState = captureState.recordingConfig.recordingState

        if (!recordingState.isPaused()) {
            // pause video
            logAdapter.addLog(
                eventName = "event_camera_recording_pause",
                message = "Pause recording",
                severity = TruvideoSdkLogSeverity.INFO
            )
            Log.d(TAG, "handlePauseVideoRecording: is pausing")

            timer.pause()
            pauseRecordingUseCase()
            return ReducedResult(state.copy(captureState = captureState.toPausedState()))
        }

        logAdapter.addLog(
            eventName = "event_camera_recording_resume",
            message = "Resume recording",
            severity = TruvideoSdkLogSeverity.INFO
        )

        timer.resume()
        return resumeRecording(state)
    }
    // ================================================

    private fun handleDeleteMediaButtonPressed(state: CameraUiState, media: TruvideoSdkCameraMedia) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaList = state.mediaState.media
        val newList = mediaList.toMutableList().apply { remove(media) }
        return ReducedResult(
            state
                .copy(mediaState = state.mediaState.copy(media = newList))
                .deriveControls()
        )
    }

    // Take Image Button logic ---------
    private fun handleTakeImageButtonPressed(state: CameraUiState) :  ReducedResult<CameraUiState, CameraUiEffect> =
        takeVideoSnapshot(state)

    private fun takeVideoSnapshot(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaState = state.mediaState
        val captureConfig = state.captureState.captureConfig
        if (mediaState.maxImageCountReached
            || mediaState.maxMediaCountReached) {
            ReducedResult(state) { _, effects ->
                effects.tryEmit(CameraUiEffect.ShowToastMessage("You have reached the maximum number of images for this session"))
                delay(5000)
                effects.tryEmit(CameraUiEffect.DismissToast)
            }
        }

        return ReducedResult(
            state,
            onFlowUpdates = { uiStateMutable, effects ->

                takeVideoSnapshotUseCase(captureConfig).collectLatest { event ->
                    when(event) {
                        is ImageCaptureEvent.Captured -> {
                            Log.d(TAG, "TakeImageUseCase: onEvent -> Captured ")
                            val result = saveImage(state, event.image)
                            uiStateMutable.update { result.newState }

                            result.onFlowUpdates?.invoke(uiStateMutable, effects)

                        }
                        is ImageCaptureEvent.Exception -> {
                            event.throwable.printStackTrace()
                        }

                        ImageCaptureEvent.CaptureFocusIdle -> Unit
                        else -> Unit
                    }
                }
            }
        )
    }
    // ----------------------------

    fun pauseVideo(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        val captureState = state.captureState
        val recordingConfig = captureState.recordingConfig
        val currentTempVideoPath = captureState.recordingConfig.recordingPath

        val media = saveVideo(
            recordingConfig,
            currentTempVideoPath
        ) ?: run {
            Log.d(TAG, "pauseVideo: could not save file ")
            return ReducedResult(state)
        }

        Log.d(TAG, "pauseVideo: current Media ${media.filePath}")

        val captureConfig = captureState.captureConfig.copy(
            template = CaptureTemplate.Preview,
            autoWhiteBalance = AutoWhiteBalanceMode.Off,
            autoFocus = AutoFocusMode.Auto,
            flash = if (captureState.captureConfig.flash.enabled()) FlashMode.Single else FlashMode.Off
        )

        restartPreviewUseCase(captureConfig = captureConfig)

        return ReducedResult(
            state.copy(
                captureState = captureState.copy(
                    previewConfig = captureState.previewConfig.copy(isBusy = false),
                    recordingConfig = recordingConfig.copy(
                        recordingState = RecordingState.PAUSED
                    ),
                    captureConfig = captureConfig
                )
            )
            , onFlowUpdates = { _, effects ->
                effects.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.RECORDING_PAUSED,
                            data = TruvideoSdkCameraEventRecordingPaused(
                                resolution = recordingConfig.recordingResolution ?: TruvideoSdkCameraResolution(
                                    width = 0,
                                    height = 0
                                ),
                                lensFacing = recordingConfig.recordingLensFacing ?: TruvideoSdkCameraLensFacing.BACK,
                                orientation = recordingConfig.recordingOrientation ?: TruvideoSdkCameraOrientation.PORTRAIT
                            ),
                            createdAtMillis = Date().time
                        )
                    )
                )
            }
        )
    }

    fun setPreviewReady(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                captureState = state.captureState.copy(
                    previewConfig = state.captureState.previewConfig.copy(isBusy = false),
                )
            ).deriveControls()
        )

    fun stopVideo(state: CameraUiState, maxDurationReached: Boolean = false) : ReducedResult<CameraUiState, CameraUiEffect> {

        val captureState = state.captureState
        val recordingConfig = captureState.recordingConfig
        val config = state.cameraConfiguration

        val currentTempVideoPath = recordingConfig.recordingPath ?: return ReducedResult(state)
        val media = saveVideo(captureState.recordingConfig, currentTempVideoPath) ?: run {
            return ReducedResult(state.toVideoStopFailureState())
        }

        val outputFile = getMediaFileUseCase(
            directory = TruvideoSdkDirectory.Files(fileManager.filesDirectory,),
            extension = "mp4"
        ) ?: run {
            return ReducedResult(state)
        }

        val mediaState = state.mediaState
        val mediaList = mediaState.tempMedia + listOf(media)
        val fileList = mediaList.map { File(it.filePath) }

        return ReducedResult(
            state,
            effect = if (config.cameraMode.autoClose)
                CameraUiEffect.ClosePreviewWithResult(listOf(media))
            else null,
            onFlowUpdates = { uiState, effects ->
                val outputPath = withContext(Dispatchers.IO) { concatVideoListUseCase(fileList, outputFile) }
                val newMedia = saveVideo(recordingConfig = recordingConfig, outputPath) ?: run {
                    Log.d(TAG, "handleRecordingEvents: couldn't create media from file ")
                    uiState.update { it.toVideoStopFailureState() }
                    return@ReducedResult
                }

                val newList = mediaState.media + listOf(newMedia)
                val videoList = newList.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }
                val maxVideoCount = config.cameraMode.videoLimit ?: Int.MAX_VALUE
                val maxMediaCount = config.cameraMode.mediaLimit ?: Int.MAX_VALUE
                val maxVideoCountReached = videoList.size >= maxVideoCount
                val maxMediaCountReached = newList.size >= maxMediaCount

                uiState.update {
                    state.toVideoStopSuccessfulState(
                        newMedia = newMedia,
                        maxVideoCountReached = maxVideoCountReached,
                        maxMediaCountReached = maxMediaCountReached
                    )
                }

                effects.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.RECORDING_FINISHED,
                            data = TruvideoSdkCameraEventRecordingFinished(newMedia),
                            createdAtMillis = Date().time
                        )
                    )
                )

                restartPreviewUseCase(captureConfig = state.captureState.captureConfig)

                if (maxDurationReached) {
                    effects.tryEmit(CameraUiEffect.ShowToastMessage("Maximum video duration reached"))
                    delay(5000)
                    effects.tryEmit(CameraUiEffect.DismissToast)
                }

                if (state.cameraConfiguration.cameraMode.isSingleMediaMode)
                    effects.tryEmit(CameraUiEffect.ClosePreviewWithResult(newList))

            }
        )
    }

    fun maxDurationReached(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> {
        stopRecordingUseCase(maxDurationReached = true)
        timer.stop()
        return ReducedResult(state)
    }

    private fun getOutputPath(defaultOutputPath: String? = null) : String {
        val filesDirectory = fileManager.filesDirectory
        return defaultOutputPath ?: "${filesDirectory}/truvideo-sdk/camera"
    }

    private suspend fun saveImage(state: CameraUiState, image: Image) : ReducedResult<CameraUiState, CameraUiEffect> {
        val imageOrientation = state.orientation()
        val imageSensorRotation = state.currentCameraDevice()?.sensorOrientation ?: 0
        val imageLensFacing = state.captureState.previewConfig.currentLensFacing
        val imageFormat = state.cameraConfiguration.imageFormat

        val mediaState = state.mediaState
        val cameraConfig = state.cameraConfiguration

        val rotation = getMediaRotationUseCase(
            sensorRotation = imageSensorRotation,
            isFront = imageLensFacing.isFront,
            orientation = imageOrientation.rotation
        )

        val fixedResolution = when (rotation) {
            90, 270 -> TruvideoSdkCameraResolution(image.height, image.width)
            else -> TruvideoSdkCameraResolution(image.width, image.height)
        }

        val outputPath = getOutputPath()

        val outputFile = getMediaFileUseCase(
            directory =
                if (cameraConfig.outputPath.isNullOrEmpty())
                    TruvideoSdkDirectory.Files(
                        fileManager.filesDirectory,
                        temp = true
                    )
                else TruvideoSdkDirectory.Custom(cameraConfig.outputPath),
            extension = imageFormat.code.toString()
        )

        val outputFileName = outputFile?.nameWithoutExtension ?: return ReducedResult(state)

        val result = image.save(
            path = outputPath,
            name = outputFileName,
            extension = imageFormat.code.toString(),
            rotation = rotation
        ).also { image.close() }

        val media = createImageMediaFromFile(
            path = File(result).path,
            resolution = fixedResolution,
            lensFacing = imageLensFacing,
            orientation = imageOrientation
        )

        val newList = mediaState.media + listOf(media)
        val newImageList = newList.filter { it.type == TruvideoSdkCameraMediaType.IMAGE }
        val maxImageCount = cameraConfig.cameraMode.imageLimit ?: Int.MAX_VALUE
        val maxMediaCount = cameraConfig.cameraMode.mediaLimit ?: Int.MAX_VALUE
        val maxImageCountReached = newImageList.size >= maxImageCount
        val maxMediaCountReached = newList.size >= maxMediaCount

        return ReducedResult(
            state.copy(
                mediaState = state.mediaState.copy(
                    media = state.mediaState.media + listOf(media),
                    maxImageCountReached = maxImageCountReached,
                    maxMediaCountReached = maxMediaCountReached
                )
            ).deriveControls(),
            onFlowUpdates = { _, effects ->
                effects.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.IMAGE_TAKEN,
                            data = TruvideoSdkCameraEventImageTaken(media),
                            createdAtMillis = Date().time
                        )
                    )
                )

            }
        )

    }

    private fun saveVideo(recordingConfig: RecordingConfig, videoPath: String? = null) : TruvideoSdkCameraMedia? {

        val filePath = videoPath ?: return null
        val duration = getVideoDuration(File(filePath))
        val orientation = recordingConfig.recordingOrientation ?: return null
        val resolution = recordingConfig.recordingResolution ?: return null
        val lensFacing = recordingConfig.recordingLensFacing ?: return null

        val media = createVideoMediaFromFile(
            path = filePath,
            resolution = resolution,
            lensFacing = lensFacing,
            orientation = orientation,
            duration = duration
        )

        logAdapter.addLog(
            eventName = "event_camera_media",
            message = "New media: ${media.toJson()}",
            severity = TruvideoSdkLogSeverity.INFO
        )


        return media
    }

}