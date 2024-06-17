package com.truvideo.sdk.camera

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal class CameraViewModel : ViewModel() {
    private val _recordingTime: MutableStateFlow<Long> = MutableStateFlow(0L)
    private val _sensorOrientation: MutableStateFlow<TruvideoSdkCameraOrientation> = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _recordingOrientation: MutableStateFlow<TruvideoSdkCameraOrientation> =
        MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _fixedOrientation: MutableStateFlow<TruvideoSdkCameraOrientation?> = MutableStateFlow(null)
    private val _isBusy: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val _isRecording: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _camera: MutableStateFlow<TruvideoSdkCameraDevice?> = MutableStateFlow(null)
    private val _frontFlashMode: MutableStateFlow<TruvideoSdkCameraFlashMode> = MutableStateFlow(TruvideoSdkCameraFlashMode.OFF)
    private val _backFlashMode: MutableStateFlow<TruvideoSdkCameraFlashMode> = MutableStateFlow(TruvideoSdkCameraFlashMode.OFF)
    private val _frontResolutions: MutableStateFlow<List<TruvideoSdkCameraResolution>> = MutableStateFlow(listOf())
    private val _frontResolution: MutableStateFlow<TruvideoSdkCameraResolution?> = MutableStateFlow(null)
    private val _backResolutions: MutableStateFlow<List<TruvideoSdkCameraResolution>> = MutableStateFlow(listOf())
    private val _backResolution: MutableStateFlow<TruvideoSdkCameraResolution?> = MutableStateFlow(null)
    private val _focusState: MutableStateFlow<FocusState> = MutableStateFlow(FocusState.IDLE)
    private val _zoomFactor: MutableStateFlow<Float> = MutableStateFlow(1.0f)
    private val _isZoomVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var _timer: Job? = null
    private var _previewVisible: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _media: MutableStateFlow<List<TruvideoSdkCameraMedia>> = MutableStateFlow(emptyList())
    private val _isPermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isPermissionGranted = _isPermissionGranted.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )

    val focusState = _focusState.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusState.IDLE
    )

    val zoomFactor = _zoomFactor.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        1.0f
    )

    val isPreviewVisible = _previewVisible.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    val isBusy = _isBusy.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true,
    )

    val isRecording = _isRecording.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false,
    )

    val camera = _camera.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val frontFlashMode = _frontFlashMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TruvideoSdkCameraFlashMode.OFF,
    )

    val backFlashMode = _backFlashMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TruvideoSdkCameraFlashMode.OFF,
    )

    val orientation = combine(_fixedOrientation, _sensorOrientation) { a, b -> Pair(a, b) }.map {
        if (it.first != null) {
            it.first!!
        } else {
            it.second
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TruvideoSdkCameraOrientation.PORTRAIT,
    )

    val recordingOrientation = _recordingOrientation.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TruvideoSdkCameraOrientation.PORTRAIT,
    )

    val frontResolution = _frontResolution.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val frontResolutions = _frontResolutions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        listOf(),
    )

    val backResolution = _backResolution.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val backResolutions = _backResolutions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        listOf(),
    )

    val recordingTime = _recordingTime.map {
        it.toDuration(DurationUnit.MILLISECONDS)
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), Duration.ZERO
    )


    val media = _media.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun addMedia(media: TruvideoSdkCameraMedia) {
        val newList = _media.value.toMutableList()
        newList.add(media)
        _media.update { newList }
    }

    fun updateMedia(media: List<TruvideoSdkCameraMedia>) {
        _media.update { media }
    }

    fun updateFixedOrientation(state: TruvideoSdkCameraOrientation?) {
        _fixedOrientation.update { state }
    }

    fun updateSensorOrientation(state: TruvideoSdkCameraOrientation) {
        _sensorOrientation.update { state }
    }

    fun updateRecordingOrientation(value: TruvideoSdkCameraOrientation) {
        _recordingOrientation.update { value }
    }

    fun updateFrontFlashMode(value: TruvideoSdkCameraFlashMode) {
        _frontFlashMode.update { value }
    }

    fun updateBackFlashMode(value: TruvideoSdkCameraFlashMode) {
        _backFlashMode.update { value }
    }

    fun updateCamera(value: TruvideoSdkCameraDevice) {
        _camera.update { value }
    }

    fun updateIsPreviewVisible(value: Boolean) {
        _previewVisible.update { value }
    }

    fun updateIsRecording(value: Boolean) {
        _isRecording.update { value }

        _timer?.cancel()
        if (value) {
            _timer = viewModelScope.launch {
                var time = 0L
                while (true) {
                    delay(200)
                    time += 200
                    _recordingTime.value = time
                }
            }
        } else {
            _recordingTime.value = 0
        }
    }

    fun updateIsBusy(value: Boolean) {
        _isBusy.update { value }
    }

    fun updateFrontResolution(value: TruvideoSdkCameraResolution) {
        _frontResolution.update { value }
    }

    fun updateFrontResolutions(value: List<TruvideoSdkCameraResolution>) {
        _frontResolutions.update { value }
    }

    fun updateBackResolution(value: TruvideoSdkCameraResolution) {
        _backResolution.update { value }
    }

    fun updateFocusState(state: FocusState) {
        _focusState.update { state }
    }

    fun updateBackResolutions(value: List<TruvideoSdkCameraResolution>) {
        _backResolutions.update { value }
    }

    fun updateZoomValue(value: Float) {
        Log.d("[Camera][ViewModel]", "Zoom value $value")
        _zoomFactor.update { value }
    }

    fun updateIsZoomVisible(value: Boolean) {
        Log.d("[Camera][ViewModel]", "Zoom visible $value")
        _isZoomVisible.update { value }
    }

    fun updateIsPermissionGranted(value: Boolean) {
        _isPermissionGranted.update { value }
    }
}

enum class FocusState {
    IDLE, REQUESTED, LOCKED
}