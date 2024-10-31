package com.truvideo.sdk.camera.ui.activities.arcamera

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ControlCamera
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

internal class TruvideoSdkArCameraViewModel : ViewModel() {
    private val _sensorOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _recordingOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _fixedOrientation = MutableStateFlow<TruvideoSdkCameraOrientation?>(null)
    private val _isBusy = MutableStateFlow(true)
    private val _isPaused = MutableStateFlow(false)
    private val _isRecording = MutableStateFlow(false)
    private val _camera = MutableStateFlow(null)
    private val _frontResolution = MutableStateFlow(null)
    private val _backResolution = MutableStateFlow(null)
    private var _previewVisible = MutableStateFlow(false)
    private val _media = MutableStateFlow<ImmutableList<TruvideoSdkCameraMedia>>(persistentListOf())
    private val _isPermissionGranted = MutableStateFlow(false)
    private val _arMode = MutableStateFlow(ARModeState.RULER)
    private val _arModeImage = MutableStateFlow(Icons.Outlined.ControlCamera)
    private val _arMeasureUnit = MutableStateFlow(ARMeasureState.CM)
    private val _toastVisible = MutableStateFlow(false)
    private val _toastText = MutableStateFlow("")

    private var _timerToast: Job? = null

    val isPermissionGranted = _isPermissionGranted.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), false
    )

    val isBusy = _isBusy.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        true,
    )

    val isPaused = _isPaused.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _isPaused.value,
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

    val backResolution = _backResolution.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val media = _media.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _media.value
    )

    val toastVisible = _toastVisible.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _toastVisible.value
    )

    val toastText = _toastText.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _toastText.value
    )

    val arMode = _arMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ARModeState.RULER,
    )

    val arModeImage = _arModeImage.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Icons.Outlined.ControlCamera
    )

    val arMeasureUnit = _arMeasureUnit.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ARMeasureState.CM
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

    fun updateFixedOrientation(value: TruvideoSdkCameraOrientation?) {
        _fixedOrientation.value = value
    }

    fun updateSensorOrientation(value: TruvideoSdkCameraOrientation) {
        _sensorOrientation.value = value
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

        _isRecording.value = value
    }

    fun updateIsBusy(value: Boolean) {
        _isBusy.value = value
    }

    fun updateIsPaused(value: Boolean) {
        _isPaused.value = value
    }

    fun updateIsPermissionGranted(value: Boolean) {
        _isPermissionGranted.value = value
    }

    fun updateARMode(value: ARModeState) {
        _arMode.value = value
        _arModeImage.value = when (value) {
            ARModeState.OBJECT -> Icons.Outlined.ViewInAr
            ARModeState.RULER -> Icons.Outlined.ControlCamera
            ARModeState.RECORD -> Icons.Outlined.Videocam
        }
    }

    fun updateARMeasure(value: ARMeasureState) {
        _arMeasureUnit.value = value
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
}

enum class ARModeState {
    OBJECT, RULER, RECORD
}

enum class ARMeasureState {
    CM, IN
}