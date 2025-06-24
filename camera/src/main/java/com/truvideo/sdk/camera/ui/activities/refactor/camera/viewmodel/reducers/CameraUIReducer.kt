package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers

import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.MediaDetailState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ReducedResult
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode

class CameraUIReducer {

    fun reduceUiEvents(
        event: CameraUiEvent.UI,
        state: CameraUiState
    ): ReducedResult<CameraUiState, CameraUiEffect> =
        when (event) {
            CameraUiEvent.UI.OnContinueButtonPressed -> handleContinueButtonPressed(state)
            is CameraUiEvent.UI.OnCurrentResolutionChanged -> handleCurrentResolutionChanged(state, event.resolution)
            CameraUiEvent.UI.OnDismissDiscardPanel -> handleDismissDiscardPanel(state)
            CameraUiEvent.UI.OnMediaCounterButtonPressed -> handleMediaCounterButtonPressed(state)
            CameraUiEvent.UI.OnMediaCounterCloseButtonPressed -> handleMediaCounterCloseButtonPressed(state)
            CameraUiEvent.UI.OnMediaDetailDismiss -> handleDismissMediaDetail(state)
            is CameraUiEvent.UI.OnMediaDetailPressed -> handleShowMediaDetail(state, event.media)
            is CameraUiEvent.UI.OnOrientationChanged -> handleOrientationChanged(state, event.orientation)
            CameraUiEvent.UI.OnResolutionsButtonPressed -> handleResolutionsButtonPressed(state)
            CameraUiEvent.UI.OnResolutionsPanelCloseButtonPressed -> handleResolutionsPanelCloseButtonPressed(state)
            CameraUiEvent.UI.OnShowDiscardPanel -> handleShowDiscardPanel(state)
            is CameraUiEvent.UI.OnZoomIndicatorModeChange -> handleZoomIndicatorModeChange(state, event.mode)
        }

    fun handleResolutionsPanelCloseButtonPressed(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(isResolutionsPanelVisible = false)
            )
        )

    fun handleMediaCounterButtonPressed(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(isMediaPanelVisible = true)
            )
        )

    fun handleMediaCounterCloseButtonPressed(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(isMediaPanelVisible = false)
            )
        )

    fun handleShowDiscardPanel(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(isDiscardPanelVisible = true)
            )
        )

    fun handleDismissDiscardPanel(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(isDiscardPanelVisible = false)
            )
        )

    fun handleOrientationChanged(state: CameraUiState, orientation: TruvideoSdkCameraOrientation) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                orientationState = state.orientationState.copy(orientation = orientation)
            )
        )

    fun handleZoomIndicatorModeChange(state: CameraUiState, mode: ZoomIndicatorMode) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                captureState = state.captureState.copy(
                    previewConfig = state.captureState.previewConfig.copy(
                        zoomIndicatorMode = mode
                    ),
                )
            )
        )

    fun handleDismissMediaDetail(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(
                    isMediaPanelDetailVisible = false,
                    mediaDetailState = MediaDetailState(),
                )
            )
        )

    fun handleShowMediaDetail(state: CameraUiState, media: TruvideoSdkCameraMedia) : ReducedResult<CameraUiState, CameraUiEffect> {
        val mediaList = state.mediaState.media
        val index = mediaList.indexOfFirst { it.filePath == media.filePath }
        if (index == -1) return ReducedResult(state)
//        logAdapter.addLog(
//            eventName = "event_camera_panel_media_detail_open",
//            message = "Open panel media detail. Media: ${m.toJson()}",
//            severity = TruvideoSdkLogSeverity.INFO,
//        )

        return ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(
                    isMediaPanelDetailVisible = true,
                    mediaDetailState = MediaDetailState(
                        index = index,
                        media = media
                    ),
                )
            )
        )
    }

    fun handleResolutionsButtonPressed(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                panelsState = state.panelsState.copy(isResolutionsPanelVisible = true)
            )
        )

    fun handleCurrentResolutionChanged(state: CameraUiState, resolution: TruvideoSdkCameraResolution) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state.copy(
                captureState = state.captureState.copy(
                    previewConfig = state.captureState.previewConfig.copy(
                        currentResolution = resolution
                    )
                ),
                panelsState = state.panelsState.copy(
                    isResolutionsPanelVisible = false
                )
            )
        )

    fun handleContinueButtonPressed(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> =
        ReducedResult(
            state,
            effect =
                if (!state.mediaState.media.isEmpty())
                    CameraUiEffect.ClosePreviewWithResult(state.mediaState.media)
                else CameraUiEffect.ClosePreview
        )

}