package com.truvideo.sdk.camera.ui.activities.refactor.camera.ui

import android.view.Surface
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import truvideo.sdk.common.model.TruvideoSdkLogSeverity

sealed interface CameraUiEvent {

    sealed class Controls : CameraUiEvent {
        class StartPreview(val surface: Surface, val width: Int, val height: Int) : Controls()
        data object ClosePreview: Controls()
        data object OnFlashButtonPressed: Controls()
        data object OnFlipCameraLensFacingButtonPressed : Controls()
        data class OnTapToFocus(val x: Float, val y: Float) : Controls()
        data class OnZoomLevelChange(val level: Float) : Controls()
        data class OnZoomLevelScaled(val scaleFactor: Float) : Controls()
        data object AppBackground : Controls()
        data object AppForeground : Controls()
    }

    sealed class UI : CameraUiEvent {
        data class OnZoomIndicatorModeChange(val mode: ZoomIndicatorMode) : UI()
        data class OnCurrentResolutionChanged(val resolution: TruvideoSdkCameraResolution): UI()
        data class OnOrientationChanged(val orientation: TruvideoSdkCameraOrientation): UI()
        data object OnResolutionsButtonPressed: UI()
        data object OnResolutionsPanelCloseButtonPressed: UI()
        data object OnContinueButtonPressed : UI()
        data object OnMediaCounterButtonPressed : UI()
        data object OnMediaCounterCloseButtonPressed : UI()
        data class OnMediaDetailPressed(val media: TruvideoSdkCameraMedia) : UI()
        data object OnMediaDetailDismiss : UI()
        data object OnShowDiscardPanel : UI()
        data object OnDismissDiscardPanel : UI()
    }

    sealed class Media: CameraUiEvent {
        data object OnTakeImageButtonPressed : Media()
        data object OnCapturedButtonPressed : Media()
        data object OnPauseButtonPressed : Media()
        data class OnDeleteMediaButtonPressed(val media: TruvideoSdkCameraMedia) : Media()
        data object OnDiscardAllMedia : Media()
    }

    sealed class Configuration: CameraUiEvent {
        data class SetUpConfig(val config: TruvideoSdkCameraConfiguration): Configuration()
        data object ValidateAuthentication: Configuration()
        data object PermissionsGranted: Configuration()
        data class Log(val eventName: String, val msg: String, val severity: TruvideoSdkLogSeverity = TruvideoSdkLogSeverity.INFO): Configuration()
    }

}
