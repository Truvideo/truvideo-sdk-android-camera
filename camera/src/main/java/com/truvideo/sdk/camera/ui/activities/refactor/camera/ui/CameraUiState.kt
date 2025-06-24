package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import androidx.compose.ui.unit.IntOffset
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.data.serializer.IntOffsetSerializer
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.RecordingConfig
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraImageFormat
import com.truvideo.sdk.camera.model.TruvideoSdkCameraInformation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMediaType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.activities.camera.intersectOrDefault
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

@Serializable
sealed interface CameraConnectionState {
    @Serializable
    data object Connected : CameraConnectionState
    @Serializable
    data object Disconnected : CameraConnectionState
    @Serializable
    data object Error : CameraConnectionState

    fun isDisconnected() : Boolean = this is Disconnected
    fun isNotConnected() : Boolean = this is Disconnected || this is Error
}

@Serializable
data class PanelsControlState(
    val isMediaPanelVisible: Boolean = false,
    val isResolutionsPanelVisible: Boolean = false,
    val isDiscardPanelVisible: Boolean = false,
    val isMediaPanelDetailVisible: Boolean = false,
    val mediaDetailState: MediaDetailState = MediaDetailState(),
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): PanelsControlState {
            if (json.isEmpty()) return PanelsControlState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

@Serializable
data class MediaDetailState(
    val media: TruvideoSdkCameraMedia? = null,
    val index: Int = 0
)

@Serializable
data class ControlsState(
    val isTakingPictureButtonEnabled: Boolean = false,
    val isTakingPictureButtonVisible: Boolean = false,
    val isCaptureButtonEnabled: Boolean = false,
    val isLensFacingRotationButtonEnabled: Boolean = false,
    val isLensFacingRotationButtonVisible: Boolean = false,
    val isPauseButtonEnabled: Boolean = false,
    val isPauseButtonVisible: Boolean = false,
    val isFlashButtonVisible: Boolean = false,
    val isFlashButtonEnabled: Boolean = false,
    val isResolutionsButtonVisible: Boolean = false,
    val isResolutionsButtonEnabled: Boolean = false,
    val isMediaCounterButtonVisible: Boolean = false,
    val isMediaCounterButtonEnabled: Boolean = false,
    val isContinueButtonVisible: Boolean = false,
    val isContinueButtonEnabled: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): ControlsState {
            if (json.isEmpty()) return ControlsState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

@Serializable
data class MediaState(
    val media: List<TruvideoSdkCameraMedia> = emptyList(),
    val tempMedia: List<TruvideoSdkCameraMedia> = emptyList(),
    val mediaOutputDirectory: String? = null,
    val maxVideoCountReached: Boolean = false,
    val maxImageCountReached: Boolean = false,
    val maxMediaCountReached: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): MediaState {
            if (json.isEmpty()) return MediaState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

fun List<TruvideoSdkCameraMedia>.filterByType(type: TruvideoSdkCameraMediaType) : List<TruvideoSdkCameraMedia> {
    return this.filter { it.type == type }
}

val List<TruvideoSdkCameraMedia>.images : List<TruvideoSdkCameraMedia>
    get() = this.filterByType(TruvideoSdkCameraMediaType.IMAGE)

val List<TruvideoSdkCameraMedia>.videos : List<TruvideoSdkCameraMedia>
    get() = this.filterByType(TruvideoSdkCameraMediaType.VIDEO)

@Serializable
data class CaptureState(
    val previewConfig: PreviewConfig = PreviewConfig(),
    val recordingConfig: RecordingConfig = RecordingConfig(),
    val captureConfig: CameraCaptureConfig = CameraCaptureConfig(),
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): CaptureState {
            if (json.isEmpty()) return CaptureState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}


@Serializable
data class FocusIndicatorState(
    val focusState: FocusState = FocusState.Idle,
    @Serializable(with = IntOffsetSerializer::class)
    val position: IntOffset = IntOffset.Zero
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): FocusIndicatorState {
            if (json.isEmpty()) return FocusIndicatorState()
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }

            return jsonConfig.decodeFromString(json)
        }
    }
}

enum class RecordingState { RECORDING, PAUSED, IDLE }

fun RecordingState.isRecording() : Boolean = this == RecordingState.RECORDING

fun RecordingState.isPaused() : Boolean = this == RecordingState.PAUSED

fun RecordingState.isIdle() : Boolean = this == RecordingState.IDLE

@Serializable
data class PreviewConfig(
    val isBusy: Boolean = false,
    val currentResolution : TruvideoSdkCameraResolution? = null,
    val currentLensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
    val viewPortWidth: Int = 0,
    val viewPortHeight: Int = 0,
    val zoomIndicatorMode: ZoomIndicatorMode = ZoomIndicatorMode.Indicator,
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): PreviewConfig {
            if (json.isEmpty()) return PreviewConfig()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

@Serializable
data class CameraConfig(
    val setUp: Boolean = false,
    val outputPath: String? = null,
    val flashOnByDefault: Boolean = false,
    val defaultLensFacing: TruvideoSdkCameraLensFacing = TruvideoSdkCameraLensFacing.BACK,
    val imageFormat: TruvideoSdkCameraImageFormat = TruvideoSdkCameraImageFormat.PNG,
    val fixedOrientation: TruvideoSdkCameraOrientation? = null,
    val defaultBackResolution: TruvideoSdkCameraResolution? = null,
    val defaultFrontResolution: TruvideoSdkCameraResolution? = null,
    val backResolutions: List<TruvideoSdkCameraResolution> = emptyList(),
    val frontResolutions: List<TruvideoSdkCameraResolution> = emptyList(),
    val cameraMode: TruvideoSdkCameraMode = TruvideoSdkCameraMode.videoAndImage()
) {
    fun toJson(): String = Json.encodeToString(this)

    companion object {
        fun fromJson(json: String): CameraConfig {
            if (json.isEmpty()) return CameraConfig()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }

}

@Serializable
data class OrientationState(
    val orientation: TruvideoSdkCameraOrientation = TruvideoSdkCameraOrientation.PORTRAIT,
    val fixedOrientation: TruvideoSdkCameraOrientation? = null,
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): OrientationState {
            if (json.isEmpty()) return OrientationState()
            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

@Serializable
data class CameraInfo(
    val info: TruvideoSdkCameraInformation? = null
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): CameraInfo {
            if (json.isEmpty()) return CameraInfo()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

@Serializable
data class PermissionState(
    val permissionGranted: Boolean = false,
    val authenticated: Boolean = false,
) {
    fun toJson(): String = Json.encodeToString(this)
    companion object {
        fun fromJson(json: String): PermissionState {
            if (json.isEmpty()) return PermissionState()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
            }
            return jsonConfig.decodeFromString(json)
        }
    }
}

data class ErrorState(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

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

data class ReducedResult<S, E>(
    val newState: S,
    val effect: E? = null,
    val onFlowUpdates: (suspend (MutableStateFlow<S>, MutableSharedFlow<E>) -> Unit)? = null
)
