package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers

import android.media.Image
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.adapters.ImageCaptureEvent
import com.truvideo.sdk.camera.adapters.TruvideoSdkFileManager
import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.AutoWhiteBalanceMode
import com.truvideo.sdk.camera.domain.models.CaptureTemplate
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.RecordingConfig
import com.truvideo.sdk.camera.domain.models.enabled
import com.truvideo.sdk.camera.domain.usecases.ConcatVideoListUseCase
import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.RestartPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.StartRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.StopRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.TakeImageUseCase
import com.truvideo.sdk.camera.domain.usecases.TakeVideoSnapshotUseCase
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.createImageMediaFromFile
import com.truvideo.sdk.camera.model.createVideoMediaFromFile
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.RecordingState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ReducedResult
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.currentCameraDevice
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.deriveControls
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isIdle
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.isPaused
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.orientation
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.plus

internal class CameraMediaReducer(
    private val fileManager: TruvideoSdkFileManager,
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase,
    private val resumeRecordingUseCase: StartRecordingUseCase,
    private val pauseRecordingUseCase: PauseRecordingUseCase,
    private val concatVideoListUseCase: ConcatVideoListUseCase,
    private val restartPreviewUseCase: RestartPreviewUseCase,
    private val takeVideoSnapshotUseCase: TakeVideoSnapshotUseCase,
    private val takeImageUseCase: TakeImageUseCase,
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
        }

    private fun handleCapturedButtonPressed(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> =
        if (state.cameraConfiguration.cameraMode.canTakeVideo) takeVideo(state, timer)
        else takePreviewImageWithFocus(state)

    private fun handleDeleteMediaButtonPressed(state: CameraUiState, media: TruvideoSdkCameraMedia) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaList = state.mediaState.media
        val newList = mediaList.toMutableList().apply { remove(media) }
        return ReducedResult(
            state
                .copy(mediaState = state.mediaState.copy(media = newList))
                .deriveControls()
        )
    }

    private fun handlePauseVideoRecording(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> {
        val captureState = state.captureState
        val cameraMode = state.cameraConfiguration.cameraMode
        val previewConfig = state.captureState.previewConfig
        val captureConfig = state.captureState.captureConfig
        val recordingState = captureState.recordingConfig.recordingState

        if (recordingState.isPaused()) {

            val filesDirectory = fileManager.filesDirectory
            val outputPath = "${filesDirectory}/truvideo-sdk/camera/temp"
            val tempFile = createVideoFile(
                outputPath
            ) ?: run {
                Log.d(TAG, "takeVideo: temp file is null")
                return ReducedResult(state)
            }
            val orientationHint = state.orientation()
            val durationLimit = cameraMode.videoDurationLimit
            val resolution = previewConfig.currentResolution ?: return ReducedResult(state)

            val sensorRotation = state.currentCameraDevice()?.sensorOrientation ?: 0
            val lensFacing = previewConfig.currentLensFacing

            val rotation = calculateRotation(
                sensorRotation = sensorRotation,
                isFront = lensFacing.isFront,
                orientation = orientationHint.rotation
            )

            val newCaptureConfig = captureConfig.copy(
                template = CaptureTemplate.Record,
                autoWhiteBalance = AutoWhiteBalanceMode.Auto,
                autoFocus = AutoFocusMode.Auto,
            )

            val newCaptureState = captureState.copy(
                recordingConfig = captureState.recordingConfig.copy(
                    recordingState = RecordingState.RECORDING,
                    recordingPath = tempFile.path,
                    recordingResolution = resolution,
                    recordingOrientation = orientationHint,
                    recordingLensFacing = lensFacing,
                ),
                previewConfig = previewConfig.copy(
                    isBusy = true,
                ),
                captureConfig = newCaptureConfig
            )

            timer.resume()
            resumeRecordingUseCase(
                cameraConfig = newCaptureConfig,
                outputFile = tempFile,
                durationLimit = durationLimit,
                resolution = resolution,
                orientation = rotation,
            )

            return ReducedResult(
                state.copy(
                    captureState = newCaptureState,
                    orientationState = state.orientationState.copy(fixedOrientation = orientationHint)
                ).deriveControls()
            )
        }

        timer.pause()
        pauseRecordingUseCase()
        return ReducedResult(state)
    }

    private fun handleTakeImageButtonPressed(state: CameraUiState) :  ReducedResult<CameraUiState, CameraUiEffect> =
        takeVideoSnapshot(state)

    fun pauseVideo(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        val captureState = state.captureState
        val recordingConfig = captureState.recordingConfig
        val captureConfig = captureState.captureConfig
        val currentTempVideoPath = captureState.recordingConfig.recordingPath
        val media = saveVideo(recordingConfig, currentTempVideoPath) ?: run {
            return ReducedResult(state)
        }

        val newCaptureConfig = captureConfig.copy(
            template = CaptureTemplate.Preview,
            autoWhiteBalance = AutoWhiteBalanceMode.Off,
            autoFocus = AutoFocusMode.Off,
            flash =
                if (captureConfig.flash.enabled()) FlashMode.Single
                else FlashMode.Off
        )

        restartPreviewUseCase(captureConfig = newCaptureConfig)

        return ReducedResult(state.copy(
            captureState = state.captureState.copy(
                previewConfig = state.captureState.previewConfig.copy(isBusy = false),
                recordingConfig = state.captureState.recordingConfig.copy(
                    recordingState = RecordingState.PAUSED
                ),
                captureConfig = newCaptureConfig
            ),
            mediaState = state.mediaState.copy(
                tempMedia = state.mediaState.tempMedia + listOf(media)
            )
        ))
    }

    fun startVideo(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                captureState = state.captureState.copy(
                    previewConfig = state.captureState.previewConfig.copy(isBusy = false),
                )
            ).deriveControls()
        )

    fun resumeVideo(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
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
        val captureConfig = captureState.captureConfig
        val config = state.cameraConfiguration

        val currentTempVideoPath = recordingConfig.recordingPath ?: return ReducedResult(state)
        val media = saveVideo(captureState.recordingConfig, currentTempVideoPath) ?: run {
            return ReducedResult(
                state.copy(
                    captureState = state.captureState.copy(
                        recordingConfig = RecordingConfig(),
                        previewConfig = captureState.previewConfig.copy(isBusy = false),
                    ),
                    mediaState = state.mediaState.copy(
                        tempMedia = emptyList(),
                        media = state.mediaState.media
                    ),
                    orientationState = state.orientationState.copy(fixedOrientation = null)
                )
            )
        }

        val mediaState = state.mediaState
        val mediaList = mediaState.tempMedia + listOf(media)
        val fileList = mediaList.map { File(it.filePath) }
        val outputFile = File(getOutputVideoPath(config.outputPath))

        return ReducedResult(
            state,
            effect = if (config.cameraMode.autoClose)
                CameraUiEffect.ClosePreviewWithResult(listOf(media))
            else null,
            onFlowUpdates = { uiState, effects ->
                val outputPath = withContext(Dispatchers.IO) { concatVideoListUseCase(fileList, outputFile) }
                val newMedia = saveVideo(recordingConfig = recordingConfig, outputPath) ?: run {
                    Log.d(TAG, "handleRecordingEvents: couldn't create media from file ")
                    uiState.update {
                        it.copy(
                            captureState = it.captureState.copy(
                                recordingConfig = RecordingConfig(),
                                previewConfig = it.captureState.previewConfig.copy(isBusy = false),
                            ),
                            mediaState = it.mediaState.copy(
                                tempMedia = emptyList(),
                                media = it.mediaState.media
                            ),
                            orientationState = it.orientationState.copy(
                                fixedOrientation = null
                            )
                        ).deriveControls()
                    }
                    return@ReducedResult
                }

                val newCaptureConfig = captureConfig.copy(
                    template = CaptureTemplate.Preview,
                    autoWhiteBalance = AutoWhiteBalanceMode.Off,
                    autoFocus = AutoFocusMode.Off,
                    flash =
                        if (captureConfig.flash.enabled()) FlashMode.Single
                        else FlashMode.Off
                )


                val newList = mediaState.media + listOf(media)
                val videoList = newList.filter { it.type == TruvideoSdkCameraMediaType.VIDEO }
                val maxVideoCount = config.cameraMode.videoLimit ?: Int.MAX_VALUE
                val maxMediaCount = config.cameraMode.mediaLimit ?: Int.MAX_VALUE
                val maxVideoCountReached = videoList.size >= maxVideoCount
                val maxMediaCountReached = newList.size >= maxMediaCount

                uiState.update {
                    it.copy(
                        captureState = it.captureState.copy(
                            recordingConfig = RecordingConfig(),
                            captureConfig = newCaptureConfig,
                            previewConfig = it.captureState.previewConfig.copy(isBusy = false),
                        ),
                        mediaState = it.mediaState.copy(
                            tempMedia = emptyList(),
                            media = it.mediaState.media + listOf(newMedia),
                            maxVideoCountReached = maxVideoCountReached,
                            maxMediaCountReached = maxMediaCountReached
                        ),
                        orientationState = it.orientationState.copy(
                            fixedOrientation = null
                        ),
                    ).deriveControls()
                }

                restartPreviewUseCase(captureConfig = newCaptureConfig)

                if (maxDurationReached) {
                    effects.tryEmit(CameraUiEffect.ShowToastMessage("Maximum video duration reached"))
                    delay(5000)
                    effects.tryEmit(CameraUiEffect.DismissToast)
                }

                if (state.cameraConfiguration.cameraMode.isSingleMediaMode)
                    effects.tryEmit(CameraUiEffect.ClosePreviewWithResult(listOf(media)))

            }
        )
    }

    fun maxDurationReached(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> {
        stopRecordingUseCase(maxDurationReached = true)
        timer.stop()
        return ReducedResult(state)
    }

    private fun takeVideo(state: CameraUiState, timer: PausableTimer) : ReducedResult<CameraUiState, CameraUiEffect> {

        val mediaState = state.mediaState
        val captureState = state.captureState
        val previewConfig = state.captureState.previewConfig
        val captureConfig = state.captureState.captureConfig
        val cameraMode = state.cameraConfiguration.cameraMode
        val currentCameraDevice = state.currentCameraDevice()
        val currentLensFacing = state.captureState.previewConfig.currentLensFacing

        if (mediaState.maxVideoCountReached
            || mediaState.maxMediaCountReached) {
//            showToast("You have reached the maximum number of videos for this session")
            return ReducedResult(state) { state, effects ->
                effects.tryEmit(CameraUiEffect.ShowToastMessage("Maximum video duration reached"))
                delay(5000)
                effects.tryEmit(CameraUiEffect.DismissToast)
            }
        }


        if (!captureState.recordingConfig.recordingState.isIdle()) {
            timer.stop()
            if (captureState.recordingConfig.recordingState.isPaused()){
                return stopVideo(state)
            }

            // stop recording
            stopRecordingUseCase()
            return ReducedResult(state)
        }

        val filesDirectory = fileManager.filesDirectory
        val outputPath = "${filesDirectory}/truvideo-sdk/camera/temp"
        val tempFile = createVideoFile(outputPath) ?: run {
            Log.d(TAG, "takeVideo: temp file is null")
            return ReducedResult(state)
        }


        val orientationHint = state.orientation()
        val durationLimit = cameraMode.videoDurationLimit
        val resolution = previewConfig.currentResolution ?: return ReducedResult(state)

        val sensorRotation = currentCameraDevice?.sensorOrientation ?: 0
        val lensFacing = currentLensFacing

        val rotation = calculateRotation(
            sensorRotation = sensorRotation,
            isFront = lensFacing.isFront,
            orientation = orientationHint.rotation
        )

        val newCaptureConfig = captureConfig.copy(
            template = CaptureTemplate.Record,
            autoWhiteBalance = AutoWhiteBalanceMode.Auto,
            autoFocus = AutoFocusMode.Auto,
        )

        val newCaptureState = captureState.copy(
            recordingConfig = RecordingConfig(
                recordingState = RecordingState.RECORDING,
                recordingPath = tempFile.path,
                recordingResolution = resolution,
                recordingOrientation = orientationHint,
                recordingLensFacing = lensFacing
            ),
            previewConfig = previewConfig.copy(
                isBusy = true,
            ),
            captureConfig = newCaptureConfig
        )


        startRecordingUseCase(
            cameraConfig = newCaptureConfig,
            outputFile = tempFile,
            durationLimit = durationLimit,
            resolution = resolution,
            orientation = rotation,
        )

        timer.start()

        return ReducedResult(
            state.copy(
                captureState = newCaptureState,
                orientationState = state.orientationState.copy(fixedOrientation = orientationHint)
            ).deriveControls()
        )
    }

    private fun takePreviewImageWithFocus(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaState = state.mediaState
        val captureConfig = state.captureState.captureConfig

        if (mediaState.maxImageCountReached
            || mediaState.maxMediaCountReached) {
//            showToast("You have reached the maximum number of images for this session")
            return ReducedResult(state)
        }

        return ReducedResult(
            state,
            onFlowUpdates = { uiStateMutable, effects ->
                takeImageUseCase(captureConfig)
                    .collectLatest { event ->
                        when(event) {
                            ImageCaptureEvent.CaptureFailed -> Unit
                            ImageCaptureEvent.CaptureFocusFailed -> Unit
                            ImageCaptureEvent.CaptureFocusLocked -> Unit
                            ImageCaptureEvent.CaptureFocusStarted -> Unit
                            ImageCaptureEvent.CaptureStarted -> Unit
                            is ImageCaptureEvent.Captured -> {
                                Log.d(TAG, "TakeImageUseCase: onEvent -> Captured ")
                                val newState = saveImage(state, event.image)
                                uiStateMutable.update { newState }
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

    private fun takeVideoSnapshot(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaState = state.mediaState
        val captureConfig = state.captureState.captureConfig
        if (mediaState.maxImageCountReached
            || mediaState.maxMediaCountReached) {
//            showToast("You have reached the maximum number of images for this session")
            return ReducedResult(state)
        }

        return ReducedResult(
            state,
            onFlowUpdates = { uiStateMutable, effects ->
                takeVideoSnapshotUseCase(captureConfig).collectLatest { event ->
                    when(event) {
                        is ImageCaptureEvent.Captured -> {
                            Log.d(TAG, "TakeImageUseCase: onEvent -> Captured ")
                            val newState = saveImage(state, event.image)
                            uiStateMutable.update { newState }
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

    private suspend fun saveImage(state: CameraUiState, image: Image) : CameraUiState {
        val imageOrientation = state.orientation()
        val imageSensorRotation = state.currentCameraDevice()?.sensorOrientation ?: 0
        val imageLensFacing = state.captureState.previewConfig.currentLensFacing
        val imageFormat = state.cameraConfiguration.imageFormat

        val mediaState = state.mediaState
        val cameraConfig = state.cameraConfiguration

        val rotation = calculateRotation(
            sensorRotation = imageSensorRotation,
            isFront = imageLensFacing.isFront,
            orientation = imageOrientation.rotation
        )

        val fixedResolution = when (rotation) {
            90, 270 -> TruvideoSdkCameraResolution(image.height, image.width)
            else -> TruvideoSdkCameraResolution(image.width, image.height)
        }

        val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())
        val outputPath = getOutputPath(cameraConfig.outputPath)

        val result = image.save(
            path = outputPath,
            name = name,
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

        return state.copy(
            mediaState = state.mediaState.copy(
                media = state.mediaState.media + listOf(media),
                maxImageCountReached = maxImageCountReached,
                maxMediaCountReached = maxMediaCountReached
            )
        ).deriveControls()

    }

    private fun getOutputPath(defaultOutputPath: String? = null) : String {
        val filesDirectory = fileManager.filesDirectory
        return defaultOutputPath ?: "${filesDirectory}/truvideo-sdk/camera"
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

//        logAdapter.addLog(
//            eventName = "event_camera_media",
//            message = "New media: ${media.toJson()}",
//            severity = TruvideoSdkLogSeverity.INFO
//        )

        // Report event
//            sendEvent(
//                TruvideoSdkCameraEvent(
//                    type = TruvideoSdkCameraEventType.IMAGE_TAKEN,
//                    data = TruvideoSdkCameraEventImageTaken(media),
//                    createdAtMillis = Date().time
//                )
//            )

        return media
    }

    private fun getOutputVideoPath(outputPath: String? = null) : String {
        val filesDirectory = fileManager.filesDirectory
        return (outputPath ?: "${filesDirectory}/truvideo-sdk/camera") + "/${System.currentTimeMillis()}.mp4"
    }

    private fun createVideoFile(outputPath: String): File? {
        val name = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US).format(Date())
        return createFile(outputPath, name, "mp4")
    }

    private fun calculateRotation(
        sensorRotation: Int,
        isFront: Boolean,
        orientation: Int
    ): Int {
        val result = if (isFront) {
            sensorRotation + orientation
        } else {
            sensorRotation - orientation + 360
        }
        return (result % 360) % 360
    }
}