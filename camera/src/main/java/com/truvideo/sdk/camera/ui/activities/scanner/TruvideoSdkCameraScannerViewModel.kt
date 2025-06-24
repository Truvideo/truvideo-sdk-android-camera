package com.truvideo.sdk.camera.ui.activities.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraFlashMode
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.model.TruvideoSdkCameraResolution
import com.truvideo.sdk.camera.model.TruvideoSdkCameraScannerCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


internal class TruvideoSdkCameraScannerViewModelFactory(
    private val fixedOrientation: TruvideoSdkCameraOrientation?,
    private val flashMode: TruvideoSdkCameraFlashMode,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TruvideoSdkCameraScannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TruvideoSdkCameraScannerViewModel(
                fixedOrientation = fixedOrientation,
                flashMode = flashMode
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

internal class TruvideoSdkCameraScannerViewModel(
    fixedOrientation: TruvideoSdkCameraOrientation?,
    flashMode: TruvideoSdkCameraFlashMode,
) : ViewModel() {
    private var _timerToast: Job? = null
    private val _sensorOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _fixedOrientation = MutableStateFlow(fixedOrientation)

    private val _isBusy = MutableStateFlow(true)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _camera = MutableStateFlow<TruvideoSdkCameraDevice?>(null)
    val camera: StateFlow<TruvideoSdkCameraDevice?> = _camera

    private val _flashMode = MutableStateFlow(flashMode)
    val flashMode: StateFlow<TruvideoSdkCameraFlashMode> = _flashMode

    private var _isPreviewVisible = MutableStateFlow(false)
    val isPreviewVisible: StateFlow<Boolean> = _isPreviewVisible

    private val _code = MutableStateFlow<TruvideoSdkCameraScannerCode?>(null)
    val code: StateFlow<TruvideoSdkCameraScannerCode?> = _code

    private val _resolution = MutableStateFlow<TruvideoSdkCameraResolution?>(null)
    val resolution: StateFlow<TruvideoSdkCameraResolution?> = _resolution

    private val _isCodeVisible = MutableStateFlow(false)
    val isCodeVisible: StateFlow<Boolean> = _isCodeVisible

    private val _toastVisible = MutableStateFlow(false)
    val toastVisible: StateFlow<Boolean> = _toastVisible

    private val _toastText = MutableStateFlow("")
    val toastText: StateFlow<String> = _toastText

    private fun calculateOrientation(
        fixedOrientation: TruvideoSdkCameraOrientation?,
        sensorOrientation: TruvideoSdkCameraOrientation,
    ) = fixedOrientation ?: sensorOrientation

    val orientation = combine(
        _fixedOrientation,
        _sensorOrientation,
    ) { fixedOrientation, sensorOrientation ->
        calculateOrientation(
            fixedOrientation = fixedOrientation,
            sensorOrientation = sensorOrientation,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = calculateOrientation(
            fixedOrientation = _fixedOrientation.value,
            sensorOrientation = _sensorOrientation.value,
        ),
    )

    fun updateSensorOrientation(value: TruvideoSdkCameraOrientation) {
        _sensorOrientation.value = value
    }

    fun updateFlashMode(value: TruvideoSdkCameraFlashMode) {
        _flashMode.value = value
    }

    fun updateCamera(value: TruvideoSdkCameraDevice) {
        _camera.value = value
    }

    fun updateIsPreviewVisible(value: Boolean) {
        _isPreviewVisible.value = value
    }

    fun updateCode(value: TruvideoSdkCameraScannerCode?) {
        _code.value = value
    }

    fun updateIsCodeVisible(value: Boolean) {
        _isCodeVisible.value = value
    }

    fun updateIsBusy(value: Boolean) {
        _isBusy.value = value
    }

    fun updateResolution(value: TruvideoSdkCameraResolution?) {
        _resolution.value = value
    }

    fun showToast(value: String, duration: Long = 5000L) {
        _timerToast?.cancel()
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

    fun close() {
        viewModelScope.cancel()

        _timerToast?.cancel()
        _timerToast = null
    }
}