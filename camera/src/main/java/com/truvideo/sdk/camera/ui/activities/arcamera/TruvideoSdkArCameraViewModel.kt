package com.truvideo.sdk.camera.ui.activities.arcamera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.truvideo.sdk.camera.model.TruvideoSdkCameraDevice
import com.truvideo.sdk.camera.model.TruvideoSdkCameraMedia
import com.truvideo.sdk.camera.model.TruvideoSdkCameraOrientation
import com.truvideo.sdk.camera.usecase.ArCoreUseCase
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

internal class TruvideoSdkArCameraViewModelFactory(
    private val arCoreUseCase: ArCoreUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TruvideoSdkArCameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TruvideoSdkArCameraViewModel(
                arCoreUseCase = arCoreUseCase,
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

internal class TruvideoSdkArCameraViewModel(
    private val arCoreUseCase: ArCoreUseCase,
) : ViewModel() {
    private val _sensorOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    private val _fixedOrientation = MutableStateFlow<TruvideoSdkCameraOrientation?>(null)

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted

    private val _isAugmentedRealitySupported = MutableStateFlow(false)
    val isAugmentedRealitySupported: StateFlow<Boolean> = _isAugmentedRealitySupported

    private val _isAugmentedRealityInstalled = MutableStateFlow(false)
    val isAugmentedRealityInstalled: StateFlow<Boolean> = _isAugmentedRealityInstalled

    private val _isBusy = MutableStateFlow(true)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _camera = MutableStateFlow<TruvideoSdkCameraDevice?>(null)
    val camera: StateFlow<TruvideoSdkCameraDevice?> = _camera

    private val _recordingOrientation = MutableStateFlow(TruvideoSdkCameraOrientation.PORTRAIT)
    val recordingOrientation: StateFlow<TruvideoSdkCameraOrientation> = _recordingOrientation

    private val _media = MutableStateFlow<ImmutableList<TruvideoSdkCameraMedia>>(persistentListOf())
    val media: StateFlow<ImmutableList<TruvideoSdkCameraMedia>> = _media

    private val _arMode = MutableStateFlow(ARModeState.RULER)
    val arMode: StateFlow<ARModeState> = _arMode

    private val _arMeasureUnit = MutableStateFlow(ARMeasureState.CM)
    val arMeasureUnit: StateFlow<ARMeasureState> = _arMeasureUnit

    private val _toastVisible = MutableStateFlow(false)
    val toastVisible: StateFlow<Boolean> = _toastVisible

    private val _toastText = MutableStateFlow("")
    val toastText: StateFlow<String> = _toastText

    private val _isFlashOn = MutableStateFlow(false)
    val isFlashOn: StateFlow<Boolean> = _isFlashOn

    private val _withFlash = MutableStateFlow(false)
    val withFlash: StateFlow<Boolean> = _withFlash

    private var _timerToast: Job? = null

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

    init {
        refreshAugmentedReality()
    }

    fun refreshAugmentedReality() {
        _isAugmentedRealitySupported.value = arCoreUseCase.isSupported
        _isAugmentedRealityInstalled.value = arCoreUseCase.isInstalled
    }

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
    }

    fun updateARMeasure(value: ARMeasureState) {
        _arMeasureUnit.value = value
    }

    fun updateFlashState(value: Boolean){
        _isFlashOn.value = !value
    }

    fun updateFlashEnable(value: Boolean){
        _withFlash.value = value
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