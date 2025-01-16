package com.truvideo.sdk.camera.ui.activities.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraLensFacing
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

internal class TruvideoSdkCameraActivityViewModel : ViewModel() {
    private val _sensorOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _recordingOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _fixedOrientation = MutableStateFlow<TruvideoSdkCameraOrientation?>(null)

    private val _isBusy = MutableStateFlow(true)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _camera = MutableStateFlow<TruvideoSdkCameraDevice?>(null)
    val camera: StateFlow<TruvideoSdkCameraDevice?> = _camera

    private val _frontFlashMode = MutableStateFlow(TruvideoSdkCameraFlashMode.OFF)
    private val _backFlashMode = MutableStateFlow(TruvideoSdkCameraFlashMode.OFF)
    private val _frontResolutions = MutableStateFlow<List<TruvideoSdkCameraResolution>>(listOf())
    private val _frontResolution = MutableStateFlow<TruvideoSdkCameraResolution?>(null)
    private val _backResolutions = MutableStateFlow<List<TruvideoSdkCameraResolution>>(listOf())
    private val _backResolution = MutableStateFlow<TruvideoSdkCameraResolution?>(null)
    private val _focusState = MutableStateFlow(FocusState.IDLE)
    val focusState: StateFlow<FocusState> = _focusState

    private val _zoomFactor = MutableStateFlow(1.0f)
    val zoomFactor: StateFlow<Float> = _zoomFactor

    private val _isZoomVisible = MutableStateFlow(false)
    val isZoomVisible: StateFlow<Boolean> = _isZoomVisible

    private var _previewVisible = MutableStateFlow(false)
    val isPreviewVisible: StateFlow<Boolean> = _previewVisible

    private var _takingImage = MutableStateFlow(false)
    val isTakingImage: StateFlow<Boolean> = _takingImage

    private val _media = MutableStateFlow<ImmutableList<TruvideoSdkCameraMedia>>(persistentListOf())
    val media: StateFlow<ImmutableList<TruvideoSdkCameraMedia>> = _media

    private val _toastVisible = MutableStateFlow(false)
    val toastVisible: StateFlow<Boolean> = _toastVisible

    private val _toastText = MutableStateFlow("")
    val toastText: StateFlow<String> = _toastText

    private var _timerToast: Job? = null

    private fun calculateHasFlash(camera: TruvideoSdkCameraDevice?): Boolean {
        return camera?.withFlash ?: false
    }

    val withFlash = _camera.map { calculateHasFlash(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = calculateHasFlash(_camera.value)
        )

    private fun calculateFlashMode(
        camera: TruvideoSdkCameraDevice?,
        backFlashMode: TruvideoSdkCameraFlashMode,
        frontFlashMode: TruvideoSdkCameraFlashMode
    ): TruvideoSdkCameraFlashMode {
        return if (camera != null) {
            when (camera.lensFacing) {
                TruvideoSdkCameraLensFacing.BACK -> backFlashMode
                TruvideoSdkCameraLensFacing.FRONT -> frontFlashMode
            }
        } else {
            TruvideoSdkCameraFlashMode.OFF
        }
    }

    val flashMode = combine(_camera, _frontFlashMode, _backFlashMode) { camera, frontFlashMode, backFlashMode ->
        calculateFlashMode(
            camera = camera,
            backFlashMode = backFlashMode,
            frontFlashMode = frontFlashMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = calculateFlashMode(
            camera = _camera.value,
            backFlashMode = _backFlashMode.value,
            frontFlashMode = _frontFlashMode.value
        ),
    )

    private fun calculateOrientation(
        fixedOrientation: TruvideoSdkCameraOrientation?,
        sensorOrientation: TruvideoSdkCameraOrientation,
        recordingOrientation: TruvideoSdkCameraOrientation,
        isRecording: Boolean
    ): TruvideoSdkCameraOrientation {
        if (isRecording) return recordingOrientation
        return fixedOrientation ?: sensorOrientation
    }

    val orientation = combine(
        _fixedOrientation,
        _sensorOrientation,
        _recordingOrientation,
        _isRecording
    ) { fixedOrientation, sensorOrientation, recordingOrientation, isRecording ->
        calculateOrientation(
            fixedOrientation = fixedOrientation,
            sensorOrientation = sensorOrientation,
            recordingOrientation = recordingOrientation,
            isRecording = isRecording
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = calculateOrientation(
            fixedOrientation = _fixedOrientation.value,
            sensorOrientation = _sensorOrientation.value,
            recordingOrientation = _recordingOrientation.value,
            isRecording = _isRecording.value
        ),
    )

    val isPortrait = orientation.map { it.isPortrait || it.isPortraitReverse }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = orientation.value.isPortrait || orientation.value.isPortraitReverse
    )

    private fun calculateResolutions(
        camera: TruvideoSdkCameraDevice?,
        frontResolutions: List<TruvideoSdkCameraResolution>,
        backResolutions: List<TruvideoSdkCameraResolution>
    ): List<TruvideoSdkCameraResolution> {
        if (camera == null) return listOf()
        return when (camera.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> backResolutions
            TruvideoSdkCameraLensFacing.FRONT -> frontResolutions
        }
    }

    private fun calculateResolution(
        camera: TruvideoSdkCameraDevice?,
        frontResolution: TruvideoSdkCameraResolution?,
        backResolution: TruvideoSdkCameraResolution?
    ): TruvideoSdkCameraResolution? {
        if (camera == null) return null
        return when (camera.lensFacing) {
            TruvideoSdkCameraLensFacing.BACK -> backResolution
            TruvideoSdkCameraLensFacing.FRONT -> frontResolution
        }
    }

    val resolutions = combine(_camera, _frontResolutions, _backResolutions) { camera, frontResolutions, backResolutions ->
        calculateResolutions(
            camera = camera,
            frontResolutions = frontResolutions,
            backResolutions = backResolutions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = calculateResolutions(
            camera = _camera.value,
            frontResolutions = _frontResolutions.value,
            backResolutions = _backResolutions.value
        ),
    )

    val resolution = combine(_camera, _frontResolution, _backResolution) { camera, frontResolution, backResolution ->
        calculateResolution(
            camera = camera,
            frontResolution = frontResolution,
            backResolution = backResolution
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = calculateResolution(
            camera = _camera.value,
            frontResolution = _frontResolution.value,
            backResolution = _backResolution.value
        ),
    )

    fun addMedia(value: TruvideoSdkCameraMedia) {
        _media.value = _media.value.toMutableList().apply { add(value) }.toPersistentList()
    }

    fun removeMedia(path: String) {
        try {
            File(path).delete()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }

        _media.value = _media.value.filter { it.filePath != path }.toPersistentList()
    }

    fun updateSensorOrientation(value: TruvideoSdkCameraOrientation) {
        _sensorOrientation.value = value
    }

    fun updateFixedOrientation(value: TruvideoSdkCameraOrientation?) {
        _fixedOrientation.value = value
    }

    fun updateFrontFlashMode(value: TruvideoSdkCameraFlashMode) {
        _frontFlashMode.value = value
    }

    fun updateBackFlashMode(value: TruvideoSdkCameraFlashMode) {
        _backFlashMode.value = value
    }

    fun updateCamera(value: TruvideoSdkCameraDevice) {
        _camera.value = value
    }

    fun updateIsPreviewVisible(value: Boolean) {
        _previewVisible.value = value
    }

    fun updateIsRecording(value: Boolean) {
        if (value) {
            if (_isRecording.value) return

            _isRecording.value = true
            val orientation = _fixedOrientation.value ?: _sensorOrientation.value
            _recordingOrientation.value = orientation
        } else {
            if (!_isRecording.value) return
            _isRecording.value = false
        }
    }

    fun updateIsPaused(value: Boolean) {
        _isPaused.value = value
    }

    fun updateIsBusy(value: Boolean) {
        _isBusy.value = value
    }

    fun updateFrontResolution(value: TruvideoSdkCameraResolution) {
        _frontResolution.value = value
    }

    fun updateFrontResolutions(value: List<TruvideoSdkCameraResolution>) {
        _frontResolutions.value = value
    }

    fun updateBackResolution(value: TruvideoSdkCameraResolution) {
        _backResolution.value = value
    }

    fun updateFocusState(value: FocusState) {
        _focusState.value = value
    }

    fun updateBackResolutions(value: List<TruvideoSdkCameraResolution>) {
        _backResolutions.value = value
    }

    fun updateZoomValue(value: Float) {
        _zoomFactor.value = value
    }

    fun updateIsZoomVisible(value: Boolean) {
        _isZoomVisible.value = value
    }

    fun showToast(value: String, duration: Long = 5000L) {
        _timerToast?.cancel()
        _timerToast = null

        _toastText.value = value
        _toastVisible.value = true
        _timerToast = viewModelScope.launch {
            delay(duration)
            _toastVisible.value = false
        }
    }

    fun hideToast() {
        _timerToast?.cancel()
        _timerToast = null

        _toastVisible.value = false
    }

    fun updateTakingImage(value: Boolean) {
        _takingImage.value = value
    }

    fun close() {
        viewModelScope.cancel()

        _timerToast?.cancel()
        _timerToast = null
    }
}