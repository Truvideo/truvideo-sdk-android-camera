package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers

import android.util.Log
import android.view.Surface
import androidx.compose.ui.unit.IntOffset
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.enabled
import com.truvideo.sdk.camera.domain.usecases.ChangeCameraUseCase
import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.RequestFocusOnPositionUseCase
import com.truvideo.sdk.camera.domain.usecases.RestartPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.SetFlashUseCase
import com.truvideo.sdk.camera.domain.usecases.SetZoomLevelUseCase
import com.truvideo.sdk.camera.domain.usecases.StartPreviewUseCase
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.ui.activities.camera.TruvideoSdkCameraActivity.Companion.FOCUS_INDICATOR_SIZE
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.FocusIndicatorState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ReducedResult
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.currentCameraDevice
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.deriveControls
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.getResolution
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel.Companion.TAG
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.utils.getFocusRectangle
import com.truvideo.sdk.camera.utils.getZoomRectangle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

class CameraControlsReducer(
    private val startPreviewUseCase: StartPreviewUseCase,
    private val requestFocusOnPositionUseCase: RequestFocusOnPositionUseCase,
    private val setFlashUseCase: SetFlashUseCase,
    private val changeCameraUseCase: ChangeCameraUseCase,
    private val setZoomLevelUseCase: SetZoomLevelUseCase,
    private val pauseRecordingUseCase: PauseRecordingUseCase,
    private val restartPreviewUseCase: RestartPreviewUseCase
) {

    fun reduceControlEvents(event: CameraUiEvent.Controls, state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> =
        when (event) {
            is CameraUiEvent.Controls.StartPreview -> handlePreviewStart(state, event.surface, event.width, event.height)
            CameraUiEvent.Controls.ClosePreview -> handleClosePreview(state)
            CameraUiEvent.Controls.OnFlashButtonPressed -> handleToggleFlash(state)
            CameraUiEvent.Controls.OnFlipCameraLensFacingButtonPressed -> handleFlipCameraLensFacing(state)
            is CameraUiEvent.Controls.OnTapToFocus -> requestFocusOnPosition(state, event.x, event.y)
            is CameraUiEvent.Controls.OnZoomLevelChange -> handleZoomLevelChange(state, event.level)
            is CameraUiEvent.Controls.OnZoomLevelScaled -> handleZoomLevelScaled(state, event.scaleFactor)
            CameraUiEvent.Controls.AppBackground -> handleAppBackground(state)
            CameraUiEvent.Controls.AppForeground -> handleAppForeground(state)
        }

    private fun handleAppBackground(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        pauseRecordingUseCase()
        return ReducedResult(state)
    }

    private fun handleAppForeground(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        if (state.cameraConnectionState.isNotConnected()) {
            val cameraId = state.currentCameraDevice()?.id ?: return ReducedResult(state, null) // TODO: Handle error
            val captureConfig = state.captureState.captureConfig
            val resolution = state.captureState.previewConfig.currentResolution ?: return ReducedResult(state, null) // TODO: Handle error
            restartPreviewUseCase(restartCamera = true, cameraId, resolution, captureConfig)
        }
        return ReducedResult(state)
    }


    private fun handlePreviewStart(state: CameraUiState, surface: Surface, width: Int, height: Int) : ReducedResult<CameraUiState, CameraUiEffect> {
        val resolution = state.captureState.previewConfig.currentResolution ?: return ReducedResult(state, null) // TODO: Handle error
        val cameraId = state.currentCameraDevice()?.id ?: return ReducedResult(state, null) // TODO: Handle error
        val captureConfig = state.captureState.captureConfig
        val currentLensFacing = state.captureState.previewConfig.currentLensFacing

        startPreviewUseCase(cameraId, surface, resolution, captureConfig)

        val newState =  state.
            copy(
                captureState = state.captureState.copy(
                    previewConfig = state.captureState.previewConfig.copy(
                        currentResolution = resolution,
                        currentLensFacing = currentLensFacing,
                        viewPortWidth = width,
                        viewPortHeight = height,
                    )
                )
            )

        return ReducedResult(newState)
    }

    private fun handleClosePreview(state: CameraUiState) : ReducedResult<CameraUiState, CameraUiEffect> {
        return ReducedResult(state, effect = CameraUiEffect.ClosePreview)
    }

    private fun handleToggleFlash(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        val cameraMode = state.cameraConfiguration.cameraMode

        if (cameraMode.canTakeImage && !cameraMode.canTakeVideo) {
            val newState = state.copy(
                captureState = state.captureState.copy(
                    captureConfig = state.captureState.captureConfig.copy(
                        flash = if (state.captureState.captureConfig.flash.enabled())
                            FlashMode.Off
                        else FlashMode.Single,
                    ),
                    // always restore zoom indicator mode to indicator
                    previewConfig = state.captureState.previewConfig.copy(
                        zoomIndicatorMode = ZoomIndicatorMode.Indicator
                    )
                ),
            )

            return ReducedResult(newState)
        }

        val newFlashState =
            if (state.captureState.captureConfig.flash.enabled()) FlashMode.Off
            else FlashMode.Torch

        val newCameraCaptureConfig = state.captureState.captureConfig.copy(flash = newFlashState)


        return ReducedResult(
            state,
            onFlowUpdates = { state, effect ->
                setFlashUseCase(newCameraCaptureConfig).collect {
                    if (!it) {
                        return@collect
                    }

                    state.update { st ->
                        st.copy(
                            captureState = st.captureState.copy(
                                captureConfig = newCameraCaptureConfig,
                                previewConfig = st.captureState.previewConfig.copy(
                                    zoomIndicatorMode = ZoomIndicatorMode.Indicator
                                )
                            ),
                        )
                    }

                }
            }
        )
    }

    private fun requestFocusOnPosition(state: CameraUiState, x: Float, y: Float) : ReducedResult<CameraUiState, CameraUiEffect> {
        val currentCameraDevice = state.currentCameraDevice() ?: return ReducedResult(state)
        val hasTapToFocus = currentCameraDevice.isTapToFocusEnabled

        if (!hasTapToFocus) return ReducedResult(state)

        val previewConfig = state.captureState.previewConfig
        val viewPortWidth = previewConfig.viewPortWidth
        val viewPortHeight = previewConfig.viewPortHeight
        val sensorSize = currentCameraDevice.sensorSize
        val sensorOrientation = currentCameraDevice.sensorOrientation

        val focusArea =
            getFocusRectangle(
                touchX = x,
                touchY = y,
                viewWidth = viewPortWidth.toFloat(),
                viewHeight = viewPortHeight.toFloat(),
                sensorSize = sensorSize,
                orientation = sensorOrientation
            )

        val offsetSize = (FOCUS_INDICATOR_SIZE)
        val offsetX = (x - offsetSize).toInt().coerceAtLeast(0)
        val offsetY = (y - offsetSize).toInt().coerceAtLeast(0)
        val touchPosition = IntOffset(offsetX, offsetY)


        val newCaptureConfig = state.captureState.captureConfig.copy(
            focusArea = focusArea
        )

        var newState = state

        return ReducedResult(
            newState = newState,
            onFlowUpdates = { uiState, effect ->
                requestFocusOnPositionUseCase(newCaptureConfig)
                    .onEach { focus ->
                        Log.d(TAG, "requestFocusOnPosition: focus state: ${focus}")
                        uiState.update {
                            it.copy(
                                captureState = it.captureState.copy(
                                    captureConfig = it.captureState.captureConfig.copy(
                                        focusArea = focusArea
                                    ),
                                ),
                                focusIndicatorState = FocusIndicatorState(
                                    focusState = focus,
                                    position = touchPosition
                                )
                            )
                        }
                    }.collect()
            }
        )
    }

    private fun handleFlipCameraLensFacing(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect>  {
        val lensFacing = state.captureState.previewConfig.currentLensFacing.reversed
        val device = state.cameraInfo?.info?.let {
            if (lensFacing == TruvideoSdkCameraLensFacing.BACK) it.backCamera
            else it.frontCamera
        } ?: return ReducedResult(state)

        val cameraId = device.id
        val resolution = state.getResolution(lensFacing) ?:  return ReducedResult(state)

        changeCameraUseCase(cameraId, resolution, state.captureState.captureConfig)

        val newState = state.copy(
                captureState = state.captureState.copy(
                    previewConfig = state.captureState.previewConfig.copy(
                        currentResolution = resolution,
                        currentLensFacing = lensFacing
                    )
                ),
            ).deriveControls()

        return ReducedResult(newState)
    }

    private fun handleZoomLevelChange(state: CameraUiState, level: Float): ReducedResult<CameraUiState, CameraUiEffect> {
        val currentDevice = state.currentCameraDevice()

        val sensorSize = currentDevice?.sensorSize ?: run {
            Log.d(TAG, "requestFocusOnPosition: currentCameraDevice is ${currentDevice}")
            return ReducedResult(state)
        }

        val cropArea = getZoomRectangle(level, sensorSize)

        val newCaptureConfig = state.captureState.captureConfig.copy(
            zoomArea = cropArea,
            zoomLevel = level
        )

        return ReducedResult(
            state,
            onFlowUpdates = { uiState, effect  ->
                setZoomLevelUseCase(newCaptureConfig)
                    .collectLatest { focus ->
                        uiState.update { state ->
                            state.copy(
                                captureState = state.captureState.copy(
                                    captureConfig = newCaptureConfig
                                )
                            )
                        }
                    }
            }
        )
    }

    private fun handleZoomLevelScaled(state: CameraUiState, scaleFactor: Float): ReducedResult<CameraUiState, CameraUiEffect> {
        if (state.captureState.previewConfig.isBusy) return ReducedResult(state)

        val captureConfig = state.captureState.captureConfig
        val cameraDevice = state.currentCameraDevice()

        val currentZoomValue = captureConfig.zoomLevel
        val newZoomValue = (currentZoomValue * scaleFactor).coerceIn(1.0f, 10.0f)

        val sensorSize = cameraDevice?.sensorSize ?: run {
            return ReducedResult(state)
        }

        val cropArea = getZoomRectangle(newZoomValue, sensorSize)

        val newCaptureConfig = captureConfig.copy(
            zoomArea = cropArea,
            zoomLevel = newZoomValue
        )

        return ReducedResult(
            state,
            onFlowUpdates = { uiState, effect ->
                setZoomLevelUseCase(newCaptureConfig)
                    .onStart {
                        uiState.update { state ->
                            state.copy(
                                captureState = state.captureState.copy(
                                    previewConfig = state.captureState.previewConfig.copy(
                                        isBusy = true
                                    ),
                                )
                            )
                        }
                    }
                    .onEach { focus ->
                        uiState.update { state ->
                            state.copy(
                                captureState = state.captureState.copy(
                                    captureConfig = newCaptureConfig
                                )
                            )
                        }
                    }
                    .onCompletion { uiState.update { state ->
                        state.copy(
                            captureState = state.captureState.copy(
                                previewConfig = state.captureState.previewConfig.copy(
                                    isBusy = false
                                ),
                            )
                        )
                    } }
                    .collect()
            }
        )

    }
}