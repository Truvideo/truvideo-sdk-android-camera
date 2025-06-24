package com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.adapters.CameraEvent
import com.truvideo.sdk.camera.adapters.RecordingEvent
import com.truvideo.sdk.camera.domain.usecases.GetCameraInformationUseCase
import com.truvideo.sdk.camera.domain.usecases.ObserveRecordingEventsUseCase
import com.truvideo.sdk.camera.domain.models.CameraCaptureConfig
import com.truvideo.sdk.camera.domain.models.FlashMode
import com.truvideo.sdk.camera.domain.models.RecordingConfig
import com.truvideo.sdk.camera.domain.usecases.ObserveCameraEventsUseCase
import com.truvideo.sdk.camera.interfaces.TruvideoSdkCameraLogAdapter
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraConfig
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraConnectionState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraInfo
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEffect
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiEvent
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CameraUiState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.CaptureState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.ControlsState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.FocusIndicatorState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.MediaState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.OrientationState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.PanelsControlState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.PermissionState
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.PreviewConfig
import com.truvideo.sdk.camera.ui.activities.refactor.camera.ui.currentCameraDevice
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraConfigReducer
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraControlsReducer
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraMediaReducer
import com.truvideo.sdk.camera.ui.activities.refactor.camera.viewmodel.reducers.CameraUIReducer
import com.truvideo.sdk.camera.ui.components.zoom_indicator.ZoomIndicatorMode
import com.truvideo.sdk.camera.utils.PausableTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class CameraPreviewViewModel (
    internal val logAdapter: TruvideoSdkCameraLogAdapter,
    internal val getCameraInformationUseCase: GetCameraInformationUseCase,
    internal val observeRecordingEventsUseCase: ObserveRecordingEventsUseCase,
    internal val observeCameraEventsUseCase: ObserveCameraEventsUseCase,
    internal val controlsReducer: CameraControlsReducer,
    internal val uiReducer: CameraUIReducer,
    internal val mediaReducer: CameraMediaReducer,
    internal val configReducer: CameraConfigReducer,
    internal val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val uiStateMutable: MutableStateFlow<CameraUiState> = MutableStateFlow(
        CameraUiState(
            mediaState = MediaState.fromJson(savedStateHandle[KEY_MEDIA_STATE] ?: ""),
            captureState = CaptureState.fromJson(savedStateHandle[KEY_CAPTURE_STATE] ?: ""),
            cameraConfiguration = CameraConfig.fromJson(savedStateHandle[KEY_CONFIG_STATE] ?: ""),
            controlsState = ControlsState.fromJson(savedStateHandle[KEY_CONTROLS_STATE] ?: ""),
            permissionState = PermissionState.fromJson(savedStateHandle[KEY_PERMISSION_STATE] ?: ""),
            cameraInfo = CameraInfo.fromJson(savedStateHandle[KEY_INFO_STATE] ?: CameraInfo(info = getCameraInformationUseCase()).toJson()),
            orientationState = OrientationState.fromJson(savedStateHandle[KEY_ORIENTATION_STATE] ?: ""),
            focusIndicatorState = FocusIndicatorState.fromJson(savedStateHandle[KEY_FOCUS_STATE] ?: ""),
            panelsState = PanelsControlState.fromJson(savedStateHandle[KEY_PANELS_STATE] ?: "")
        )
    )

    val uiState: StateFlow<CameraUiState> = uiStateMutable.asStateFlow()

    val cameraConfig : StateFlow<CameraConfig> =
        uiState
            .map { it.cameraConfiguration }
            .onEach { savedStateHandle[KEY_CONFIG_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CameraConfig())

    val captureState =
        uiState.map { it.captureState }
            .onEach { savedStateHandle[KEY_CAPTURE_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CaptureState())

    val cameraInfo =
        uiState.map { it.cameraInfo }
            .onEach { savedStateHandle[KEY_INFO_STATE] = it?.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CameraInfo())

    val mediaState =
        uiState
            .map { it.mediaState }
            .onEach {  savedStateHandle[KEY_MEDIA_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, MediaState())

    val controlsState =
        uiState
            .map { it.controlsState }
            .onEach { savedStateHandle[KEY_CONTROLS_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ControlsState())

    val permissionState =
        uiState
            .map { it.permissionState }
            .onEach { savedStateHandle[KEY_PERMISSION_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, PermissionState())

    val panelsControlState =
        uiState.map { it.panelsState }
            .onEach { savedStateHandle[KEY_PANELS_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, PanelsControlState())

    val orientationState : StateFlow<OrientationState> =
        uiState
            .map { it.orientationState }
            .onEach { savedStateHandle[KEY_ORIENTATION_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, OrientationState())

    val focusIndicatorState =
        uiState
            .map { it.focusIndicatorState }
            .onEach { savedStateHandle[KEY_FOCUS_STATE] = it.toJson() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, FocusIndicatorState())

    // -----------------------------------------------------------------------------

    val cameraMode : StateFlow<TruvideoSdkCameraMode> =
        cameraConfig
            .map { it.cameraMode }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TruvideoSdkCameraMode.videoAndImage())

    val previewConfig =
        captureState.map { it.previewConfig }
            .stateIn(viewModelScope, SharingStarted.Eagerly, PreviewConfig())

    val currentLensFacing =
        previewConfig
            .map { it.currentLensFacing }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TruvideoSdkCameraLensFacing.BACK)

    val currentCameraDevice =
        uiState.map {
            it.currentCameraDevice()
        }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val resolutions =
        currentCameraDevice.map { device ->
            device?.resolutions ?: emptyList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val orientation =
        orientationState
            .map { it.fixedOrientation ?: it.orientation }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TruvideoSdkCameraOrientation.PORTRAIT)

    val zoomLevel =
        uiState.map { it.captureState.captureConfig.zoomLevel }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val zoomIndicatorMode =
        previewConfig
            .map { it.zoomIndicatorMode }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ZoomIndicatorMode.Indicator)

    val currentResolution =
        previewConfig
            .map { it.currentResolution }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val resolutionAspectRatio : StateFlow<Float?> =
        uiState
            .map { it.captureState.previewConfig.currentResolution?.aspectRatio }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0f)

    val captureConfig =
        captureState
            .map { it.captureConfig }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CameraCaptureConfig())

    val recordingConfig =
        captureState
            .map { it.recordingConfig }
            .stateIn(viewModelScope, SharingStarted.Eagerly, RecordingConfig())

    val maxRecordingTime  =
        cameraMode
            .map { it.videoDurationLimit }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val recordingTime  =
        recordingConfig
            .map { it.recordingTimer }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val flashButtonSelected =
        captureConfig.map {
            it.flash != FlashMode.Off
        } .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    internal val _effect = MutableSharedFlow<CameraUiEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val effect = _effect.asSharedFlow()

    val recordingTimer : PausableTimer = PausableTimer(interval = 1000) { time ->
        Log.d(TAG, "Timer: $time")
        uiStateMutable.update {
            it.copy(
                captureState = it.captureState.copy(
                    recordingConfig = it.captureState.recordingConfig.copy(
                        recordingTimer =  time,
                    )
                )
            )
        }
    }

    fun onEvent(event: CameraUiEvent) = reduceEvent(event)

    private fun reduceEvent(event: CameraUiEvent) =
        viewModelScope.launch {
            val state = uiState.value
            val reducedResult =
                when(event) {
                    // Configuration events
                    is CameraUiEvent.Configuration -> configReducer.reduceCameraConfigEvents(event,state)
                    // Control events
                    is CameraUiEvent.Controls -> controlsReducer.reduceControlEvents(event,state)
                    // UI events
                    is CameraUiEvent.UI -> uiReducer.reduceUiEvents(event,state)
                    // Media events
                    is CameraUiEvent.Media -> mediaReducer.reduceMediaEvents(event,state, recordingTimer)
                }


            uiStateMutable.update { reducedResult.newState }
            reducedResult.let {
                it.effect?.let { _effect.tryEmit(it) }
                it.onFlowUpdates?.invoke(uiStateMutable, _effect)
            }
        }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared:")
    }

    init {
        observeRecordingEventsUseCase()
            .onEach {
                val state = uiState.value
                val reducedResult = when(it) {
                    is RecordingEvent.Exception -> {
                        it.throwable.printStackTrace()
                        mediaReducer.stopVideo(state = state)
                    }
                    RecordingEvent.MaxDurationReached -> mediaReducer.maxDurationReached(state = state, recordingTimer)
                    RecordingEvent.Paused -> mediaReducer.pauseVideo(state = state)
                    RecordingEvent.Started -> mediaReducer.startVideo(state = state)
                    RecordingEvent.Resumed -> mediaReducer.resumeVideo(state = state)
                    is RecordingEvent.Stopped -> mediaReducer.stopVideo(state = state, maxDurationReached = it.maxDurationReached)
                }

                uiStateMutable.update { reducedResult.newState }
                if (reducedResult.effect != null)
                    _effect.emit(reducedResult.effect)

                reducedResult.onFlowUpdates?.invoke(uiStateMutable, _effect)
            }.launchIn(viewModelScope)

        observeCameraEventsUseCase()
            .onEach {
                when(it) {
                    CameraEvent.Connected -> {
                        Log.d(TAG, "CameraEvent: connected")
                        uiStateMutable.update {
                            it.copy(cameraConnectionState = CameraConnectionState.Connected)
                        }
                    }
                    CameraEvent.Disconnected -> {
                        Log.d(TAG, "CameraEvent: disconnected")
                        uiStateMutable.update {
                            it.copy(cameraConnectionState = CameraConnectionState.Disconnected)
                        }
                    }
                    is CameraEvent.Error -> {
                        Log.d(TAG, "CameraEvent: error")
                        uiStateMutable.update {
                            it.copy(cameraConnectionState = CameraConnectionState.Error)
                        }
                        it.throwable.printStackTrace()
                    }
                }
            }.launchIn(viewModelScope)
    }

    companion object {
        private const val KEY_MEDIA_STATE = "camera_media_state"
        private const val KEY_PERMISSION_STATE = "camera_permission_state"
        private const val KEY_INFO_STATE = "camera_info_state"
        private const val KEY_CONTROLS_STATE = "camera_controls_state"
        private const val KEY_PANELS_STATE = "camera_panels_state"
        private const val KEY_CAPTURE_STATE = "camera_capture_state"
        private const val KEY_CONFIG_STATE = "camera_config_state"
        private const val KEY_ORIENTATION_STATE = "camera_orientation_state"
        private const val KEY_FOCUS_STATE = "camera_focus_state"
        const val TAG = "CameraPreviewViewModel"
    }
}
