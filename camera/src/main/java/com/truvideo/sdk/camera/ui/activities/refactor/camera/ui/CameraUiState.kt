package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.AutoWhiteBalanceMode
import com.truvideo.sdk.camera.domain.models.CaptureTemplate
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.RecordingConfig
import com.truvideo.sdk.camera.domain.models.enabled
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.activities.camera.intersectOrDefault
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode

data class CameraUiState(
    val mediaState: MediaState = MediaState(),
    val captureState: CaptureState = CaptureState(),
    val cameraConfiguration: CameraConfig = CameraConfig(),
    val controlsState: ControlsState = ControlsState(),
    val permissionState: PermissionState = PermissionState(),
    val error: ErrorState? = null,
    val toastState: ErrorState? = null,
    val cameraInfo: CameraInfo? = null,
    val orientationState: OrientationState = OrientationState(),
    val focusIndicatorState: FocusIndicatorState = FocusIndicatorState(),
    val panelsState: PanelsControlState = PanelsControlState(),
    val cameraConnectionState: CameraConnectionState = CameraConnectionState.Disconnected
)

fun CameraUiState.toVideoStopFailureState() =
    copy(
        captureState = captureState.copy(
            recordingConfig = RecordingConfig(),
            previewConfig = captureState.previewConfig.copy(isBusy = false),
        ),
        mediaState = mediaState.copy(
            tempMedia = emptyList(),
        ),
        orientationState = orientationState.copy(
            fixedOrientation = null
        )
    ).deriveControls()

fun CameraUiState.toVideoStopSuccessfulState(
    newMedia: TruvideoSdkCameraMedia,
    maxVideoCountReached: Boolean,
    maxMediaCountReached: Boolean
) =
    copy(
        captureState = captureState.copy(
            recordingConfig = RecordingConfig(),
            captureConfig = captureState.captureConfig.copy(
                template = CaptureTemplate.Preview,
                autoWhiteBalance = AutoWhiteBalanceMode.Off,
                autoFocus = AutoFocusMode.Off,
                flash =
                    if (captureState.captureConfig.flash.enabled()) FlashMode.Single
                    else FlashMode.Off
            ),
            previewConfig = captureState.previewConfig.copy(isBusy = false),
        ),
        mediaState = mediaState.copy(
            tempMedia = emptyList(),
            media = mediaState.media + listOf(newMedia),
            maxVideoCountReached = maxVideoCountReached,
            maxMediaCountReached = maxMediaCountReached
        ),
        orientationState = orientationState.copy(
            fixedOrientation = null
        ),
    ).deriveControls()

fun CameraUiState.toFlashStateUpdated(
    flashMode: FlashMode
) =
    copy(
        captureState = captureState.copy(
            captureConfig = captureState.captureConfig.copy(
                flash = flashMode,
            ),
            // always restore zoom indicator mode to indicator
            previewConfig = captureState.previewConfig.copy(
                zoomIndicatorMode = ZoomIndicatorMode.Indicator
            )
        ),
    ).deriveControls()

fun CameraUiState.deriveControls() : CameraUiState {
    val config = cameraConfiguration
    val previewConfig = captureState.previewConfig
    val recordingConfig = captureState.recordingConfig
    val recordingState = recordingConfig.recordingState
    val mode = config.cameraMode
    val cameraInfo = cameraInfo?.info
    val media = mediaState.media
    val lensFacing = previewConfig.currentLensFacing

    val currentCameraDevice =
        if (lensFacing == TruvideoSdkCameraLensFacing.BACK) cameraInfo?.backCamera
        else cameraInfo?.frontCamera

    val shouldShowFlashButton = currentCameraDevice?.withFlash == true
    val shouldShowResolutions = !currentCameraDevice?.resolutions.isNullOrEmpty()
    val shouldShowMediaButton = media.isNotEmpty()
    val shouldShowContinueButton = media.isNotEmpty()

    return copy(
        controlsState =
            controlsState
                .copy(
                    isTakingPictureButtonEnabled = !previewConfig.isBusy,
                    isTakingPictureButtonVisible = mode.canTakeImage && mode.canTakeVideo,
                    isLensFacingRotationButtonEnabled = !previewConfig.isBusy && !recordingState.isRecording(),
                    isLensFacingRotationButtonVisible = !recordingState.isRecording(),
                    isPauseButtonEnabled = !previewConfig.isBusy && (!recordingState.isIdle()),
                    isPauseButtonVisible = !recordingState.isIdle(),
                    isCaptureButtonEnabled = !previewConfig.isBusy,
                    isFlashButtonVisible = shouldShowFlashButton,
                    isFlashButtonEnabled = !previewConfig.isBusy,
                    isResolutionsButtonVisible = shouldShowResolutions,
                    isResolutionsButtonEnabled = !previewConfig.isBusy && !recordingState.isRecording(),
                    isMediaCounterButtonVisible =  shouldShowMediaButton,
                    isMediaCounterButtonEnabled = !previewConfig.isBusy && !recordingState.isRecording(),
                    isContinueButtonEnabled =  !previewConfig.isBusy && recordingState.isIdle(),
                    isContinueButtonVisible = shouldShowContinueButton,
                ),
    )
}

fun CameraUiState.deriveOrientationState(): CameraUiState {
    val config = cameraConfiguration
    return copy(
        orientationState = orientationState.copy(
            fixedOrientation = config.fixedOrientation
        )
    )
}

fun CameraUiState.getResolution(lensFacing: TruvideoSdkCameraLensFacing) : TruvideoSdkCameraResolution? {
    if (lensFacing.isFront) {
        val deviceSupportedFrontResolutions = this.cameraInfo?.info?.frontCamera?.resolutions ?: return null
        val configFrontResolutions = this.cameraConfiguration.frontResolutions
        val defaultFrontResolution = this.cameraConfiguration.defaultFrontResolution

        return (deviceSupportedFrontResolutions to configFrontResolutions).intersectOrDefault(defaultFrontResolution)
    }

    val deviceSupportedBackResolutions = this.cameraInfo?.info?.frontCamera?.resolutions ?: return null
    val configBackResolutions = this.cameraConfiguration.frontResolutions
    val defaultBackResolution = this.cameraConfiguration.defaultFrontResolution

    return (deviceSupportedBackResolutions to configBackResolutions).intersectOrDefault(defaultBackResolution)
}

fun CameraUiState.derivePreviewAndMediaState() : CameraUiState {
    val config = cameraConfiguration
    val resolution = getResolution(config.defaultLensFacing)
    return copy(
        captureState = captureState.copy(
            previewConfig = captureState.previewConfig.copy(
                currentLensFacing = config.defaultLensFacing,
                currentResolution = resolution,
            ),
            captureConfig = captureState.captureConfig.copy(
                flash = if (config.flashOnByDefault) FlashMode.Single else FlashMode.Off
            ),
        ),
        mediaState = mediaState.copy(
            mediaOutputDirectory = config.outputPath
        )
    )
}

fun CameraUiState.currentCameraDevice() : TruvideoSdkCameraDevice? {
    val lensFacing = captureState.previewConfig.currentLensFacing

    return if (lensFacing == TruvideoSdkCameraLensFacing.BACK)
            cameraInfo?.info?.backCamera
        else cameraInfo?.info?.frontCamera
}

fun CameraUiState.orientation() : TruvideoSdkCameraOrientation {
    return orientationState.fixedOrientation ?: orientationState.orientation
}

