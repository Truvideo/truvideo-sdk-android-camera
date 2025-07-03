package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers

import android.util.Log
import android.view.Surface
import androidx.compose.ui.unit.IntOffset
import com.truvideo.sdk.camera.adapters.FocusState
import com.truvideo.sdk.camera.domain.models.AutoFocusMode
import com.truvideo.sdk.camera.domain.models.toTruvideoSdkFlashMode
import com.truvideo.sdk.camera.domain.models.toggled
import com.truvideo.sdk.camera.domain.usecases.ChangeCameraUseCase
import com.truvideo.sdk.camera.domain.usecases.PauseRecordingUseCase
import com.truvideo.sdk.camera.domain.usecases.RequestFocusOnPositionUseCase
import com.truvideo.sdk.camera.domain.usecases.RestartPreviewUseCase
import com.truvideo.sdk.camera.domain.usecases.SetFlashUseCase
import com.truvideo.sdk.camera.domain.usecases.SetZoomLevelUseCase
import com.truvideo.sdk.camera.domain.usecases.StartPreviewUseCase
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEvent
import com.truvideo.sdk.camera.model.TruvideoSdkCameraEventType
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventFlashModeChanged
import com.truvideo.sdk.camera.model.events.TruvideoSdkCameraEventZoomChanged
import com.truvideo.sdk.camera.ui.activities.camera.TruvideoSdkCameraActivity.Companion.FOCUS_INDICATOR_SIZE
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.FocusIndicatorState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ReducedResult
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.currentCameraDevice
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.deriveControls
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.getResolution
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.toFlashStateUpdated
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.CameraPreviewViewModel.Companion.TAG
import com.truvideo.sdk.camera.utils.getFocusRectangle
import com.truvideo.sdk.camera.utils.getZoomRectangle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import truvideo.sdk.common.model.TruvideoSdkLogSeverity
import java.util.Date
import kotlin.math.log

internal class CameraControlsReducer(
    private val startPreviewUseCase: StartPreviewUseCase,
    private val requestFocusOnPositionUseCase: RequestFocusOnPositionUseCase,
    private val setFlashUseCase: SetFlashUseCase,
    private val changeCameraUseCase: ChangeCameraUseCase,
    private val setZoomLevelUseCase: SetZoomLevelUseCase,
    private val pauseRecordingUseCase: PauseRecordingUseCase,
    private val restartPreviewUseCase: RestartPreviewUseCase,
    private val logAdapter: TruvideoSdkCameraLogAdapter
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
        logAdapter.addLog(
            eventName = "event_camera_recording_resume",
            message = "Resume recording",
            severity = TruvideoSdkLogSeverity.INFO
        )

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
        logAdapter.addLog(
            eventName = "event_camera_button_close_pressed",
            message = "Button close pressed",
            severity = TruvideoSdkLogSeverity.INFO,
        )

        if (state.mediaState.media.isNotEmpty()) {
            logAdapter.addLog(
                eventName = "event_camera_panel_discard_open",
                message = "Open panel discard",
                severity = TruvideoSdkLogSeverity.INFO,
            )
            return ReducedResult(
                state.copy(
                    panelsState = state.panelsState.copy(isDiscardPanelVisible = true)
                ).deriveControls()
            )
        }

        return ReducedResult(state, effect = CameraUiEffect.ClosePreview)
    }

    private fun handleToggleFlash(state: CameraUiState): ReducedResult<CameraUiState, CameraUiEffect> {
        val flashMode = state.captureState.captureConfig.flash
        val cameraMode = state.cameraConfiguration.cameraMode

        val canOnlyTakeImages = cameraMode.canTakeImage && !cameraMode.canTakeVideo

        val newState = state.toFlashStateUpdated(
            flashMode.toggled(canOnlyTakeImages)
        )

        logAdapter.addLog(
            eventName = "event_camera_flash",
            message = "Toggle flash. Current: ${flashMode.name}",
            severity = TruvideoSdkLogSeverity.INFO
        )

        if (canOnlyTakeImages)
            return ReducedResult(newState)

        return ReducedResult(
            state,
            onFlowUpdates = { state, effect ->
                setFlashUseCase(newState.captureState.captureConfig).collect {
                    if (!it) return@collect
                    state.update { newState }
                }

                effect.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.FLASH_MODE_CHANGED,
                            data = TruvideoSdkCameraEventFlashModeChanged(flashMode.toTruvideoSdkFlashMode()),
                            createdAtMillis = Date().time
                        )
                    )
                )
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
            autoFocus = AutoFocusMode.Auto,
            focusArea = focusArea
        )

        logAdapter.addLog(
            eventName = "event_camera_focus",
            message = "Focus requested",
            severity = TruvideoSdkLogSeverity.INFO
        )

        return ReducedResult(
            newState = state.copy(
                captureState = state.captureState.copy(
                    captureConfig = newCaptureConfig
                )
            ),
            onFlowUpdates = { uiState, effect ->
                requestFocusOnPositionUseCase(newCaptureConfig)
                    .onEach { focus ->
                        Log.d(TAG, "requestFocusOnPosition: focus state: ${focus}")
                        if (focus == FocusState.FocusedLocked) {
                            logAdapter.addLog(
                                eventName = "event_camera_focus",
                                message = "Focus locked",
                                severity = TruvideoSdkLogSeverity.INFO
                            )
                        }


                        uiState.update {
                            it.copy(
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

        logAdapter.addLog(
            eventName = "event_camera_zoom",
            message = "New value: $level",
            severity = TruvideoSdkLogSeverity.INFO
        )

        return ReducedResult(
            state.copy(
                captureState = state.captureState.copy(
                    captureConfig = newCaptureConfig
                )
            ),
            onFlowUpdates = { uiState, effect ->
                setZoomLevelUseCase(newCaptureConfig)
                    .collect()

                effect.tryEmit(
                    CameraUiEffect.SendEvent(
                        TruvideoSdkCameraEvent(
                            type = TruvideoSdkCameraEventType.ZOOM_CHANGED,
                            data = TruvideoSdkCameraEventZoomChanged(level),
                            createdAtMillis = Date().time
                        )
                    )
                )
            }
        )
    }

    private fun handleZoomLevelScaled(state: CameraUiState, scaleFactor: Float): ReducedResult<CameraUiState, CameraUiEffect> {
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
            state.copy(
                captureState = state.captureState.copy(
                    captureConfig = newCaptureConfig
                )
            ),
            onFlowUpdates = { uiState, effect ->
                setZoomLevelUseCase(newCaptureConfig)
                    .collect()
            }
        )

    }
}