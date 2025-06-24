package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers

import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCase
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraAuthAdapter
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraConfiguration
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraConfig
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ReducedResult
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.deriveControls
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.deriveOrientationState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.derivePreviewAndMediaState
import truvideo.sdk.common.exceptions.TruvideoSdkException
import truvideo.sdk.common.model.TruvideoSdkLogSeverity

internal class CameraConfigReducer(
    val authAdapter: TruvideoSdkCameraAuthAdapter,
    val logAdapter: TruvideoSdkCameraLogAdapter,
    val pauseRecordingUseCase: PauseRecordingUseCase
) {

    fun reduceCameraConfigEvents(event: CameraUiEvent.Configuration, state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> =
        when(event) {
            is CameraUiEvent.Configuration.Log -> handleLog(state, event.eventName, event.msg, event.severity)
            CameraUiEvent.Configuration.PermissionsGranted -> handlePermissionsGranted(state)
            is CameraUiEvent.Configuration.SetUpConfig -> handleSetUpConfig(state, event.config)
            CameraUiEvent.Configuration.ValidateAuthentication -> handleValidateAuthentication(state)
        }

    private fun handleLog(
        state: CameraUiState,
        eventName: String,
        msg: String,
        severity: TruvideoSdkLogSeverity = TruvideoSdkLogSeverity.INFO
    ): ReducedResult<CameraUiState, CameraUiEffect> {
        logAdapter.addLog(eventName, msg, severity)
        return ReducedResult(state)
    }

    private fun handlePermissionsGranted(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        return ReducedResult(state.copy(permissionState = state.permissionState.copy(permissionGranted = true)))
    }

    private fun handleSetUpConfig(state: CameraUiState, config: TruvideoSdkCameraConfiguration): ReducedResult<CameraUiState, CameraUiEffect> {
        return ReducedResult(
            state.copy(
                cameraConfiguration =
                        CameraConfig(
                            setUp = true,
                            outputPath = config.outputPath,
                            defaultLensFacing = config.lensFacing,
                            flashOnByDefault = config.flashMode.isOn,
                            fixedOrientation = config.orientation,
                            defaultBackResolution = config.backResolution,
                            defaultFrontResolution = config.frontResolution,
                            backResolutions = config.backResolutions,
                            frontResolutions = config.frontResolutions,
                            imageFormat = config.imageFormat,
                            cameraMode = config.mode
                        )
            ).derivePreviewAndMediaState()
            .deriveControls()
            .deriveOrientationState()
        )
    }

    private fun handleValidateAuthentication(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        return try {
            authAdapter.validateAuthentication()
            ReducedResult(
                state.copy(permissionState = state.permissionState.copy(authenticated = true))
            )
        }catch (e: TruvideoSdkException){
            ReducedResult(
                state,
                CameraUiEffect.ReportAuthenticationError
            )
        }
    }

}